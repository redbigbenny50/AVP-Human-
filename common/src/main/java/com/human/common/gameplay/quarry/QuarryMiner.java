package com.human.common.gameplay.quarry;

import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.registry.init.HumanQuarry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The quarry's digging ([stated] Oct 5: "mines an entire chunk layer by layer, then moves to the next ... a whole chunk
 * before coming back up and moving to the next chunk").
 * <h2>COST - [stated] Oct 15: "we want to keep things low cost"</h2>
 * <ul>
 * <li><b>A fixed budget per tick.</b> A layer is 256 positions spread evenly over its {@code secondsPerLayer}: 0.64 a
 * tick at the base 20 s, 3.2 at the fastest 4 s. Nothing ever scans a layer or a chunk in one go.</li>
 * <li><b>No neighbour updates.</b> Mined blocks are removed with {@code UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE} - clients
 * see it, but no block-update cascade runs through the pit. Water at the edge of the hole stays put instead of flooding
 * it (the containment field of phase 4b makes that deliberate).</li>
 * <li><b>Air costs one state read.</b> The claim check (a break event) and the loot roll run only for real blocks.</li>
 * <li><b>Structures are looked up once per chunk</b>, from the chunk's own structure references, never per block.</li>
 * <li><b>Clients hear about it once per layer</b> (every 4-20 s), not per block; the renderer glides between.</li>
 * <li><b>Never loads a chunk.</b> An unloaded work chunk just waits.</li>
 * </ul>
 */
final class QuarryMiner {

    /** Positions in one chunk layer. */
    static final int LAYER = 16 * 16;

    /** Blocks a second the cutter rises at, and the head travels at, between chunks - the renderer uses the same. */
    static final double RISE_PER_TICK = 0.5D;

    static final double TRAVEL_PER_TICK = 0.25D;

    /**
     * Most positions LOOKED AT in one tick. Empty ones (air, liquid, nothing to dig) cost no time, so this is what
     * bounds the work instead: one layer's worth, so an all-air layer is passed in a single tick.
     */
    static final int MAX_SCAN_PER_TICK = LAYER;

    /** The most mining time that can be saved up while passing empty space - stops a burst on the first solid block. */
    private static final double MAX_CARRY = 4.0D;

    private final QuarryTerminalBlockEntity terminal;

    /** Which chunk of the area, the layer's Y, and the next position in that layer (0-255). */
    private int chunk;

    private int layerY = Integer.MIN_VALUE;

    private int cursor;

    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

    /** Fractional positions owed this tick - the budget's remainder, so the pace is exact at any speed. */
    private double carry;

    /** Ticks still to wait while the cutter rises and the head moves to the next chunk. */
    private int travelTicks;

    /** Blocks actually dug in this area (the PROGRESS tab's BLOCKS MINED). */
    private int mined;

    /** True once the whole area is done; START begins again from the top. */
    private boolean finished;

    /** The drops that last did not fit (not saved: worked out again from the block on load). Null = not stuck. */
    private @org.jetbrains.annotations.Nullable List<ItemStack> stuckDrops;

    /** The liquid that last did not fit in the tank. Null = not stuck. */
    private net.minecraft.world.level.material.@org.jetbrains.annotations.Nullable Fluid stuckFluid;

    /** Drops that did not fit; mining waits until they do ([stated] Oct 15: "stops until space is made"). */
    private final List<ItemStack> overflow = new ArrayList<>();

    /** Highest Y of any structure piece in the current chunk, or MIN_VALUE for none. Looked up once per chunk. */
    /** Whether the chunk being dug is ocean or river - looked up once per chunk, like the structures. */
    private boolean waterChunk;

    private int waterChunkIndex = -1;

    QuarryMiner(QuarryTerminalBlockEntity terminal) {
        this.terminal = terminal;
    }

    // ------------------------------------------------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------------------------------------------------

    boolean finished() {
        return finished;
    }

    int mined() {
        return mined;
    }

    int chunkIndex() {
        return chunk;
    }

    int layerY() {
        return layerY == Integer.MIN_VALUE ? terminal.startY() : layerY;
    }

    /** Layers still to dig in the whole area, counting the current one - what TIME LEFT and AREA DONE are built on. */
    int layersLeft(int chunks) {
        if (finished || chunks == 0) {
            return 0;
        }

        var perChunk = layersPerChunk();
        var here = Math.max(0, layerY() - terminal.bottomY() + 1);

        return Math.max(0, chunks - chunk - 1) * perChunk + Math.min(perChunk, here);
    }

    int layersPerChunk() {
        return Math.max(0, terminal.startY() - terminal.bottomY() + 1);
    }

    /** Starts the area over - a new area was confirmed, or the finished one was started again. */
    void reset() {
        chunk = 0;
        layerY = Integer.MIN_VALUE;
        cursor = 0;
        areaCleared = false;
        clearChunk = 0;
        clearColumn = 0;
        verifying = false;
        rechecks = 0;
        sectionSkips = 0;
        refusedBlocks = 0;
        carry = 0.0D;
        travelTicks = 0;
        finished = false;
        structureBoxesChunk = null;
        waterChunkIndex = -1;
        stuckDrops = null;
        stuckFluid = null;
        mined = 0;
        releaseFields();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The dig
    // ------------------------------------------------------------------------------------------------------------

    /** One powered tick of mining. Returns the status to show. */
    QuarryTerminalBlockEntity.Status tick(ServerLevel level, List<ChunkPos> chunks) {
        areaSize = chunks.size();

        if (finished) {
            return QuarryTerminalBlockEntity.Status.READY;
        }

        if (!flushOverflow()) {
            return QuarryTerminalBlockEntity.Status.STORAGE_FULL;
        }

        // ⚠ Oct 16 fix - stay FULL until there is really room. Before, on ticks where the dig budget had not yet built
        // up to a whole block the scan stopped early and reported MINING, then the next tick hit the same block and
        // reported FULL again - so the status, and the terminal's screen with it, flipped every tick. While stuck, only
        // the drops that did not fit are checked (no block is read or rolled) until they would.
        if (stuckDrops != null) {
            if (!fits(stuckDrops)) {
                return QuarryTerminalBlockEntity.Status.STORAGE_FULL;
            }

            stuckDrops = null;
        }

        // Same for a full tank (DRAIN LIQUIDS = COLLECT): stay TANK FULL until a bucket of that liquid fits.
        if (stuckFluid != null) {
            if (terminal.tank().insert(stuckFluid, FluidContent.BUCKET, true) < FluidContent.BUCKET) {
                return QuarryTerminalBlockEntity.Status.TANK_FULL;
            }

            stuckFluid = null;
        }

        if (travelTicks > 0) {
            travelTicks--;
            return QuarryTerminalBlockEntity.Status.MINING;
        }

        if (chunk >= chunks.size()) {
            finish();
            return QuarryTerminalBlockEntity.Status.READY;
        }

        // Oct 17 - the whole area inside the frame is cleared by the lasers once, before the first cut.
        if (!areaCleared) {
            return clearArea(level, chunks);
        }

        var target = chunks.get(chunk);

        if (!level.hasChunk(target.x, target.z)) {
            return QuarryTerminalBlockEntity.Status.WAITING;
        }

        if (layerY == Integer.MIN_VALUE) {
            layerY = terminal.startY();
            cursor = 0;
            terminal.miningMoved();
        }

        // Oct 17 - the bottom was reached: the chunk is checked before the drill may leave it.
        if (verifying) {
            return verify(level, target, chunks);
        }

        carry = Math.min(MAX_CARRY, carry + LAYER / (terminal.secondsPerLayer() * 20.0D));

        if (waterChunkIndex != chunk) {
            waterChunkIndex = chunk;
            waterChunk = isOceanOrRiver(level, target);
        }

        var drain = terminal.settings().get(QuarrySettings.Option.DRAIN_LIQUIDS);

        // Oct 18: keep liquid out of this chunk while it is dug, however it arrives (QuarryDigSeal) - except an ocean
        // or
        // river chunk with DRAIN OFF, where the sea is meant to carry on down the hole.
        if (waterChunk && drain == DRAIN_OFF) {
            QuarryDigSeal.clear(this);
        } else {
            QuarryDigSeal.set(this, level, target, terminal.bottomY(), terminal.startY());
        }

        // [stated] Oct 15: "if a layer is all air it should skip to the next layer". Empty positions cost no time -
        // only real digging spends the budget - so an empty layer goes by in one tick, and a whole empty chunk
        // SECTION (16 layers of nothing: sky, a big cave) is skipped in one step from the section's own air flag.
        for (int scanned = 0; scanned < MAX_SCAN_PER_TICK; scanned++) {
            if (cursor == 0 && skipEmptySection(level, target)) {
                sectionSkips++;

                if (layerY < terminal.bottomY()) {
                    startVerify();
                    break;
                }

                continue;
            }

            // One reused position for the scan (up to a layer's 256 reads a tick through empty space); a fresh, fixed
            // one is made only for a spot that is actually worked.
            probe.set(target.getMinBlockX() + (cursor & 15), layerY, target.getMinBlockZ() + (cursor >> 4));

            var state = level.getBlockState(probe);

            // [stated] Oct 18: PRESERVE STRUCTURES on = anything inside a structure's box is left alone, and the drill
            // carries on past it to the bottom. Skipped like air: no time spent, nothing removed.
            if (!isEmpty(state, drain) && !preserved(level, probe, target)) {
                if (carry < 1.0D) {
                    break;
                }

                var at = probe.immutable();

                var result = state.getBlock() instanceof LiquidBlock ? drainLiquid(level, at, state, drain) : mine(level, at, state);

                if (result != Result.DONE) {
                    // Drops or liquid did not fit: the block is kept for the next try, and the clock stops with it.
                    carry = 0.0D;
                    return result == Result.TANK_FULL
                        ? QuarryTerminalBlockEntity.Status.TANK_FULL
                        : QuarryTerminalBlockEntity.Status.STORAGE_FULL;
                }

                carry -= 1.0D;

                if (!(state.getBlock() instanceof LiquidBlock)) {
                    mined++;
                }
            }

            if (++cursor >= LAYER) {
                cursor = 0;
                layerY--;

                if (layerY < terminal.bottomY()) {
                    startVerify();
                    break;
                }

                terminal.miningMoved();
            }
        }

        return QuarryTerminalBlockEntity.Status.MINING;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Clearing the frame - Oct 17 FIX: [stated] "the laser is supposed to clear out any blocks along the frame or the
    // internal area" ... "everything in the frame"
    // ------------------------------------------------------------------------------------------------------------

    /*
     * The dig starts at the terminal's own level and works DOWN, and the frame build only atomized the blocks standing
     * where a frame block went (the Oct 15 ruling, now reversed) - so nothing inside the frame above the dig was ever
     * touched: trees stood in the middle of the area, and grass was left floating once the ground under it was cut (the
     * drill removes blocks without neighbour updates - a full update per block would cost far too much - so a plant is
     * never told its ground went). Now, before the first cut, the lasers sweep every column of the whole selected area:
     * - INSIDE THE FRAME - from the terminal's level up to the top of the frame and the 2 air blocks above it (the 7
     * blocks the gantry and the head travel through) - EVERYTHING is atomized, whatever it is. - ABOVE THE FRAME, up to
     * the top of what is there - only plants and trees, so a tall tree goes whole while a hill or a build higher than
     * the frame is left alone. Atomized like the frame build's: gone, nothing dropped. Never touched, anywhere:
     * anything with a block entity, unbreakable blocks, the quarry itself, liquids, and anything a claim or protection
     * mod refuses the owner. It is done ONCE per job (a new area, or a finished one started again), across every chunk,
     * before any digging - so the gantry never travels through anything. A quarry already running when this lands
     * sweeps its area once when it next loads, then carries on where it was. COST: a few columns a tick, each a handful
     * of reads (7 inside the frame, and down from the top of what is there above it); at most ZAPS_PER_TICK blocks go a
     * tick and one laser is drawn a tick.
     */

    /** The frame's inside: the 4-block gap, the frame's own level and the 2 air blocks above it. */
    private static final int FRAME_HEIGHT = 7;

    /** Columns looked at per tick, and blocks atomized per tick at most. */
    private static final int CLEAR_COLUMNS_PER_TICK = 4;

    private static final int ZAPS_PER_TICK = 12;

    private boolean areaCleared;

    private int clearChunk;

    private int clearColumn;

    /** One tick of the sweep. Returns the status to show; flips {@link #areaCleared} when the whole area is done. */
    private QuarryTerminalBlockEntity.Status clearArea(ServerLevel level, List<ChunkPos> chunks) {
        var zapped = 0;
        var lasered = false;
        var at = new BlockPos.MutableBlockPos();
        var floor = terminal.startY();
        var frameTop = floor + FRAME_HEIGHT;

        for (int columns = 0; columns < CLEAR_COLUMNS_PER_TICK && clearChunk < chunks.size(); columns++) {
            var target = chunks.get(clearChunk);

            // Never load a chunk to clear it.
            if (!level.hasChunk(target.x, target.z)) {
                return QuarryTerminalBlockEntity.Status.WAITING;
            }

            var x = target.getMinBlockX() + (clearColumn & 15);
            var z = target.getMinBlockZ() + (clearColumn >> 4);
            var top = Math.max(frameTop, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z));
            var columnDone = true;

            for (int y = top; y > floor; y--) {
                at.set(x, y, z);

                var state = level.getBlockState(at);
                var inFrame = y <= frameTop;

                if (state.isAir() || !state.getFluidState().isEmpty()) {
                    continue;
                }

                if (!inFrame && !isVegetation(state) || QuarryTerminalBlockEntity.isProtected(level, at, state)) {
                    continue;
                }

                // PRESERVE STRUCTURES: a structure standing inside the frame (a village house, a ruin) is left too.
                if (preserved(level, at, target)) {
                    continue;
                }

                if (!QuarryProtection.check(level, at, terminal.ownerId())) {
                    continue;
                }

                if (zapped >= ZAPS_PER_TICK) {
                    columnDone = false;
                    break;
                }

                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                zapped++;

                if (!lasered) {
                    terminal.fireLaser(level, at.immutable());
                    lasered = true;
                }
            }

            if (!columnDone) {
                return QuarryTerminalBlockEntity.Status.MINING;
            }

            if (++clearColumn >= LAYER) {
                clearColumn = 0;
                clearChunk++;
            }
        }

        if (clearChunk >= chunks.size()) {
            areaCleared = true;
        }

        return QuarryTerminalBlockEntity.Status.MINING;
    }

    /** Plants and trees - what the lasers clear ABOVE the frame. Never liquids, never solid terrain. */
    private static boolean isVegetation(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }

        var block = state.getBlock();

        return state.canBeReplaced()
            || state.is(net.minecraft.tags.BlockTags.LEAVES)
            || state.is(net.minecraft.tags.BlockTags.LOGS)
            || state.is(net.minecraft.tags.BlockTags.FLOWERS)
            || state.is(net.minecraft.tags.BlockTags.SAPLINGS)
            || state.is(net.minecraft.tags.BlockTags.CROPS)
            || block instanceof net.minecraft.world.level.block.BushBlock
            || block instanceof net.minecraft.world.level.block.VineBlock
            || block instanceof net.minecraft.world.level.block.CactusBlock
            || block instanceof net.minecraft.world.level.block.SugarCaneBlock
            || block instanceof net.minecraft.world.level.block.BambooStalkBlock
            || block instanceof net.minecraft.world.level.block.HugeMushroomBlock
            || block instanceof net.minecraft.world.level.block.CocoaBlock;
    }

    /** Nothing to do here: air, or a liquid while DRAIN LIQUIDS is OFF (it is left standing, held by the field). */
    private static boolean isEmpty(BlockState state, int drain) {
        return state.isAir() || drain == DRAIN_OFF && state.getBlock() instanceof LiquidBlock;
    }

    /** DRAIN LIQUIDS: 0 OFF (leave liquids, hold them back), 1 COLLECT (into the tank), 2 VOID. */
    static final int DRAIN_OFF = 0;

    static final int DRAIN_COLLECT = 1;

    enum Result {
        DONE,
        STORAGE_FULL,
        TANK_FULL
    }

    /**
     * DRAIN LIQUIDS on: a liquid in the dig is taken out. A SOURCE goes into the tank as a bucket (COLLECT) or is
     * deleted (VOID); flowing liquid is just cleared. With COLLECT and no room in the tank - full, or holding a
     * different liquid - it waits, like a full storage.
     */
    private Result drainLiquid(ServerLevel level, BlockPos at, BlockState state, int drain) {
        var fluid = state.getFluidState();

        if (drain == DRAIN_COLLECT && fluid.isSource()) {
            var tank = terminal.tank();
            var source = fluid.getType();

            if (tank.insert(source, FluidContent.BUCKET, true) < FluidContent.BUCKET) {
                stuckFluid = source;
                return Result.TANK_FULL;
            }

            tank.insert(source, FluidContent.BUCKET, false);
        }

        clear(level, at);

        return Result.DONE;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Containment field
    // ------------------------------------------------------------------------------------------------------------

    /** Sides liquid could pour in from: the four around, and above (liquid never flows up). */
    private static final net.minecraft.core.Direction[] POUR_SIDES = {
        net.minecraft.core.Direction.NORTH,
        net.minecraft.core.Direction.SOUTH,
        net.minecraft.core.Direction.EAST,
        net.minecraft.core.Direction.WEST,
        net.minecraft.core.Direction.UP
    };

    /** Field blocks raised in the chunk being dug - taken down when the drill moves on. */
    private long[] fields = new long[0];

    private int fieldCount;

    /** Field blocks of chunks already finished, still being taken down a few a tick. */
    private long[] clearing = new long[0];

    private int clearingCount;

    /** Field blocks taken down per tick: bounded, because each lets liquid pour in and flow. */
    private static final int CLEAR_PER_TICK = 16;

    /**
     * Empties a dug spot. If liquid touches it ([stated] Oct 15: "edge fluids held back by a CONTAINMENT FIELD that
     * clears when the drill moves on"), a field block takes its place instead of air, so the sea, a river or the lake
     * beside the hole cannot pour in while the drill works. Five fluid reads per dug block - nothing per tick.
     */
    private void clear(ServerLevel level, BlockPos at) {
        // [stated] Oct 15: "Ocean or river chunks should just pass through the water without draining it. Basically
        // treating it like air. Unless the player selects drain or void." So there, with DRAIN OFF, a dug spot that
        // water touches simply becomes water - the sea carries on down the hole as the drill goes, as if it were air
        // the drill was passing through. No field, nothing to take down later, and no flowing: a still water block
        // is set straight in, one write like the air it replaces.
        if (waterChunk && terminal.settings().get(QuarrySettings.Option.DRAIN_LIQUIDS) == DRAIN_OFF) {
            for (var side : POUR_SIDES) {
                if (level.getFluidState(at.relative(side)).is(net.minecraft.tags.FluidTags.WATER)) {
                    level.setBlock(at, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    return;
                }
            }
        }

        for (var side : POUR_SIDES) {
            if (!level.getFluidState(at.relative(side)).isEmpty()) {
                level.setBlock(
                    at,
                    HumanQuarry.QUARRY_CONTAINMENT_FIELD.get().defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
                );

                if (fieldCount == fields.length) {
                    fields = java.util.Arrays.copyOf(fields, Math.max(64, fields.length * 2));
                }

                fields[fieldCount++] = at.asLong();
                return;
            }
        }

        level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    /**
     * A field block sealed by QuarryDigSeal when liquid tried to spread into the chunk being dug: it joins this chunk's
     * fields, so it comes down with them.
     */
    void registerField(BlockPos pos) {
        if (fieldCount == fields.length) {
            fields = java.util.Arrays.copyOf(fields, Math.max(64, fields.length * 2));
        }

        fields[fieldCount++] = pos.asLong();
    }

    /** The chunk is done: its field blocks join the queue to come down, a few a tick. */
    void releaseFields() {
        // The drill is leaving this chunk (moving on, finished, retracted, broken): it no longer holds liquid out.
        QuarryDigSeal.clear(this);

        if (fieldCount == 0) {
            return;
        }

        if (clearingCount + fieldCount > clearing.length) {
            clearing = java.util.Arrays.copyOf(clearing, clearingCount + fieldCount);
        }

        System.arraycopy(fields, 0, clearing, clearingCount, fieldCount);
        clearingCount += fieldCount;
        fieldCount = 0;
        fields = new long[0];
    }

    /**
     * Takes down queued field blocks, {@link #CLEAR_PER_TICK} at a time, with a full block update so liquid flows in
     * naturally. Runs every tick the terminal ticks, whether or not it is mining. Only removes blocks that are still
     * the field - anything a player put there since is left alone. Skips unloaded spots for later.
     */
    void tickClearing(ServerLevel level) {
        if (clearingCount == 0) {
            return;
        }

        var field = HumanQuarry.QUARRY_CONTAINMENT_FIELD.get();

        for (int done = 0; done < CLEAR_PER_TICK && clearingCount > 0; done++) {
            var pos = BlockPos.of(clearing[--clearingCount]);

            if (!level.isLoaded(pos)) {
                // Put it back at the front and stop: it waits for its chunk, nothing is lost.
                clearingCount++;
                break;
            }

            if (level.getBlockState(pos).is(field)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        terminal.contentsChanged();
    }

    /**
     * Every field block this quarry raised, down now - the terminal was broken, so nothing would otherwise ever take
     * them down. One-off, so it is done in one go.
     */
    void dropAllFields(ServerLevel level) {
        releaseFields();

        while (clearingCount > 0) {
            var before = clearingCount;

            tickClearing(level);

            if (clearingCount == before) {
                // The rest are in unloaded chunks; they are plain blocks that break by hand.
                break;
            }
        }
    }

    /**
     * At the start of a layer: if the whole 16-high chunk section it is in holds only air, drop straight to the layer
     * under that section - one flag read instead of thousands of block reads. True if it skipped.
     */
    private boolean skipEmptySection(ServerLevel level, ChunkPos target) {
        var chunk = level.getChunk(target.x, target.z);
        var index = chunk.getSectionIndex(layerY);

        if (index < 0 || index >= chunk.getSectionsCount() || !chunk.getSection(index).hasOnlyAir()) {
            return false;
        }

        var sectionBottom = net.minecraft.core.SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));

        layerY = sectionBottom - 1;
        terminal.miningMoved();

        return true;
    }

    /**
     * Mines one position. Returns false only when its drops would not fit - the block then stays exactly as it was.
     * Skipped (returns true): air, liquids (phase 4b), unbreakable blocks, the quarry's own frame and terminal, and
     * anything a claim or protection mod refuses to the quarry's owner.
     */
    private Result mine(ServerLevel level, BlockPos at, BlockState state) {
        if (state.isAir() || state.getDestroySpeed(level, at) < 0.0F) {
            return Result.DONE;
        }

        if (
            state.getBlock() instanceof QuarryFrameBlock || state.getBlock() instanceof QuarryTerminalBlock
                || state.getBlock() instanceof QuarryContainmentBlock
        ) {
            return Result.DONE;
        }

        if (!QuarryProtection.check(level, at, terminal.ownerId())) {
            // Oct 18 diagnostic: counted, so the chunk's log line shows if a claim or protection mod refused blocks.
            refusedBlocks++;
            return Result.DONE;
        }

        var drops = drops(level, at, state);

        if (!fits(drops)) {
            stuckDrops = drops;
            return Result.STORAGE_FULL;
        }

        var blockEntity = level.getBlockEntity(at);

        // Containers give up what is in them to the quarry first, so nothing spills into the pit when the block goes.
        if (blockEntity instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                var stack = container.removeItemNoUpdate(slot);

                if (!stack.isEmpty()) {
                    store(stack);
                }
            }

            // ⚠ Final-pass DUPLICATION fix: some containers drop WITH their contents inside (a shulker box's loot
            // copies them into the item). The drops above were rolled while it was still full, so storing them as
            // well as the emptied contents gave everything twice. Rolled again now it is empty.
            drops = drops(level, at, state);
        }

        clear(level, at);

        for (var drop : drops) {
            store(drop);
        }

        return Result.DONE;
    }

    /** What a block gives: its loot with a diamond pickaxe, or - with PRESERVE SPAWNERS on - the spawner itself. */
    private List<ItemStack> drops(ServerLevel level, BlockPos at, BlockState state) {
        var blockEntity = level.getBlockEntity(at);

        // [stated] Oct 15: "spawners collected keeping their type".
        if (state.is(Blocks.SPAWNER) && terminal.settings().get(QuarrySettings.Option.PRESERVE_SPAWNERS) == 0 && blockEntity != null) {
            var spawner = new ItemStack(Items.SPAWNER);

            BlockItem.setBlockEntityData(spawner, BlockEntityType.MOB_SPAWNER, blockEntity.saveWithoutMetadata(level.registryAccess()));

            // Marked as the quarry's, so it can be placed back WITH its mob in survival
            // (MixinBlockItem_QuarriedSpawner;
            // vanilla only allows that for operators). Other spawners with data still need an operator.
            var marker = new CompoundTag();

            marker.putBoolean("avp_human_quarried", true);
            spawner.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(marker));

            return List.of(spawner);
        }

        // A diamond pickaxe carrying whatever FORTUNE / SILK TOUCH books are in the terminal - built once, cached.
        return Block.getDrops(state, level, at, blockEntity, null, terminal.miningTool());
    }

    /** Whether all of {@code drops} would go into storage right now. Cheap: drops are one or two stacks. */
    private boolean fits(List<ItemStack> drops) {
        if (drops.isEmpty()) {
            return true;
        }

        var storage = terminal.storage();
        var free = 0;

        for (int slot = 0; slot < storage.getContainerSize() && free < drops.size(); slot++) {
            var held = storage.getItem(slot);

            if (held.isEmpty()) {
                free++;
            } else {
                for (var drop : drops) {
                    if (ItemStack.isSameItemSameComponents(held, drop) && held.getCount() + drop.getCount() <= held.getMaxStackSize()) {
                        free++;
                        break;
                    }
                }
            }
        }

        return free >= drops.size();
    }

    /**
     * Into storage, or held over if it does not quite fit (container contents can be larger than a check allows for).
     */
    private void store(ItemStack stack) {
        var left = terminal.storage().addItem(stack);

        if (!left.isEmpty()) {
            overflow.add(left);
        }

        terminal.contentsChanged();
    }

    /** Pushes held-over drops into storage; true when nothing is left waiting. */
    private boolean flushOverflow() {
        if (overflow.isEmpty()) {
            return true;
        }

        for (var iterator = overflow.listIterator(); iterator.hasNext();) {
            var left = terminal.storage().addItem(iterator.next());

            if (left.isEmpty()) {
                iterator.remove();
            } else {
                iterator.set(left);
            }
        }

        terminal.contentsChanged();

        return overflow.isEmpty();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The completion check - Oct 17: [stated] "quarries when having all speed cards installed start a chunk even when
    // the first isnt finished" ... yes to the check and the log
    // ------------------------------------------------------------------------------------------------------------

    /*
     * Reaching the bottom no longer moves the drill on by itself. It first re-reads the chunk, from the top of the dig
     * down to the bottom, and if anything it should have dug is still there - for ANY reason: a section skip that leapt
     * over blocks, sand or gravel that fell in, something placed into the pit - it goes back up to the highest such
     * layer and digs on from there. Only a chunk that passes moves the drill on. What counts as left over is exactly
     * what the drill digs: not air, not a liquid it was told to leave (DRAIN OFF), not unbreakable, not the quarry's
     * own blocks, not claimed or protected from the owner. Liquids it WAS told to drain count only as source blocks -
     * flowing liquid that keeps arriving from outside could send it back forever. For the same reason it is sent back
     * at most MAX_RECHECKS times per chunk; after that it moves on and says so. COST: nothing is removed during the
     * check - it only reads, VERIFY_READS_PER_TICK positions a tick, so a deep chunk (256 x ~130 layers, about 33,000
     * reads) is checked in a handful of ticks.
     */

    private static final int VERIFY_READS_PER_TICK = 8192;

    private static final int MAX_RECHECKS = 3;

    private boolean verifying;

    private int verifyY;

    private int verifyCursor;

    private int rechecks;

    /** The area's chunk count, kept from the last tick - for the log line. */
    private int areaSize;

    /** How many blocks a claim or protection mod refused in this chunk - for the log line. */
    private int refusedBlocks;

    /** How many all-air sections were skipped in this chunk - for the log line. */
    private int sectionSkips;

    private void startVerify() {
        verifying = true;
        verifyY = terminal.startY();
        verifyCursor = 0;
    }

    private QuarryTerminalBlockEntity.Status verify(ServerLevel level, ChunkPos target, List<ChunkPos> chunks) {
        var drain = terminal.settings().get(QuarrySettings.Option.DRAIN_LIQUIDS);
        var at = new BlockPos.MutableBlockPos();

        for (int reads = 0; reads < VERIFY_READS_PER_TICK; reads++) {
            if (verifyY < terminal.bottomY()) {
                verifying = false;
                logMove(target, "finished: reached the bottom and the check found nothing left");
                nextChunk(chunks);
                return QuarryTerminalBlockEntity.Status.MINING;
            }

            at.set(target.getMinBlockX() + (verifyCursor & 15), verifyY, target.getMinBlockZ() + (verifyCursor >> 4));

            if (isLeftOver(level, at, level.getBlockState(at), drain)) {
                verifying = false;

                if (rechecks >= MAX_RECHECKS) {
                    logMove(target, "moved on after " + MAX_RECHECKS + " re-checks still found blocks (last at y " + verifyY + ")");
                    nextChunk(chunks);
                    return QuarryTerminalBlockEntity.Status.MINING;
                }

                rechecks++;
                com.human.Human.LOGGER.info(
                    "[Quarry {}] chunk {},{}: check found blocks left at y {} ({}), going back to dig them (re-check {} of {})",
                    terminal.getBlockPos().toShortString(),
                    target.x,
                    target.z,
                    verifyY,
                    level.getBlockState(at).getBlock().getDescriptionId(),
                    rechecks,
                    MAX_RECHECKS
                );
                layerY = verifyY;
                cursor = 0;
                terminal.miningMoved();
                return QuarryTerminalBlockEntity.Status.MINING;
            }

            if (++verifyCursor >= LAYER) {
                verifyCursor = 0;
                verifyY--;
            }
        }

        return QuarryTerminalBlockEntity.Status.MINING;
    }

    /** Whether the drill should still dig this - the same rules as mine() and drainLiquid(). */
    private boolean isLeftOver(ServerLevel level, BlockPos at, BlockState state, int drain) {
        if (isEmpty(state, drain) || preserved(level, at, new ChunkPos(at))) {
            return false;
        }

        if (state.getBlock() instanceof LiquidBlock) {
            return state.getFluidState().isSource();
        }

        if (state.getDestroySpeed(level, at) < 0.0F) {
            return false;
        }

        if (
            state.getBlock() instanceof QuarryFrameBlock || state.getBlock() instanceof QuarryTerminalBlock
                || state.getBlock() instanceof QuarryContainmentBlock
        ) {
            return false;
        }

        return QuarryProtection.check(level, at, terminal.ownerId());
    }

    /** [stated] Oct 17: one log line every time the drill leaves a chunk, saying why. */
    private void logMove(ChunkPos target, String reason) {
        com.human.Human.LOGGER.info(
            "[Quarry {}] chunk {},{} ({} of {}): {} - dig at y {}, {} empty section(s) skipped, {} re-check(s), {} block(s) refused by protection",
            terminal.getBlockPos().toShortString(),
            target.x,
            target.z,
            chunk + 1,
            areaSize,
            reason,
            layerY,
            sectionSkips,
            rechecks,
            refusedBlocks
        );
    }

    /** The cutter rises, the head travels to the next chunk's centre, and digging starts again from the top there. */
    private void nextChunk(List<ChunkPos> chunks) {
        var depth = terminal.startY() + 1.0D - layerY();

        // The drill moves on: this chunk's containment comes down behind it.
        releaseFields();

        chunk++;
        layerY = Integer.MIN_VALUE;
        cursor = 0;
        verifying = false;
        rechecks = 0;
        sectionSkips = 0;
        refusedBlocks = 0;
        carry = 0.0D;
        travelTicks = (int) Math.ceil(Math.max(0.0D, depth) / RISE_PER_TICK) + (int) Math.ceil(16.0D / TRAVEL_PER_TICK);

        if (chunk >= chunks.size()) {
            travelTicks = (int) Math.ceil(Math.max(0.0D, depth) / RISE_PER_TICK);
        }

        terminal.miningMoved();
    }

    private void finish() {
        releaseFields();
        finished = true;
        layerY = Integer.MIN_VALUE;
        terminal.miningFinished();
    }

    /**
     * Ocean or river, judged by the biome at the chunk's centre at sea level - one lookup per chunk. Deep, lukewarm,
     * frozen oceans and frozen rivers are all in vanilla's (and modded biomes') ocean/river tags.
     */
    private static boolean isOceanOrRiver(ServerLevel level, ChunkPos target) {
        var biome = level.getBiome(new BlockPos(target.getMiddleBlockX(), level.getSeaLevel(), target.getMiddleBlockZ()));

        return biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN) || biome.is(net.minecraft.tags.BiomeTags.IS_RIVER);
    }

    /**
     * The top of the highest structure piece reaching into this chunk's column, from the chunk's own structure
     * references - looked up once when a chunk starts, never per block.
     */
    /**
     * [stated] Oct 18 - PRESERVE STRUCTURES: whether {@code at} is inside the box of any structure piece in its chunk.
     * Off, nothing is preserved. The boxes are looked up once per chunk and kept, so the per-block test is a check
     * against the few boxes of that chunk (most chunks have none, and the test is then free).
     * <p>
     * The boxes are rectangular: some plain stone inside a box but outside the build itself is left as well - a
     * mineshaft corridor stays as a tunnel of stone standing in the pit.
     * </p>
     */
    private boolean preserved(ServerLevel level, BlockPos at, ChunkPos target) {
        if (terminal.settings().get(QuarrySettings.Option.PRESERVE_STRUCTURES) != 0) {
            return false;
        }

        for (var box : structureBoxes(level, target)) {
            if (box.isInside(at)) {
                return true;
            }
        }

        return false;
    }

    private ChunkPos structureBoxesChunk;

    private List<net.minecraft.world.level.levelgen.structure.BoundingBox> structureBoxes = List.of();

    private List<net.minecraft.world.level.levelgen.structure.BoundingBox> structureBoxes(ServerLevel level, ChunkPos target) {
        if (target.equals(structureBoxesChunk)) {
            return structureBoxes;
        }

        structureBoxesChunk = target;

        var column = new net.minecraft.world.level.levelgen.structure.BoundingBox(
            target.getMinBlockX(),
            level.getMinBuildHeight(),
            target.getMinBlockZ(),
            target.getMaxBlockX(),
            level.getMaxBuildHeight(),
            target.getMaxBlockZ()
        );
        var boxes = new java.util.ArrayList<net.minecraft.world.level.levelgen.structure.BoundingBox>();

        for (var start : level.structureManager().startsForStructure(target, structure -> true)) {
            for (var piece : start.getPieces()) {
                var box = piece.getBoundingBox();

                if (box.intersects(column)) {
                    boxes.add(box);
                }
            }
        }

        structureBoxes = boxes;
        return structureBoxes;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------------------------------------------------

    CompoundTag save(HolderLookup.Provider registries) {
        var tag = new CompoundTag();

        tag.putInt("Chunk", chunk);
        tag.putInt("LayerY", layerY);
        tag.putInt("Cursor", cursor);
        tag.putBoolean("AreaCleared", areaCleared);
        tag.putBoolean("Verifying", verifying);
        tag.putInt("VerifyY", verifyY);
        tag.putInt("VerifyCursor", verifyCursor);
        tag.putInt("Rechecks", rechecks);
        tag.putInt("ClearChunk", clearChunk);
        tag.putInt("ClearColumn", clearColumn);
        tag.putInt("Travel", travelTicks);
        tag.putBoolean("Finished", finished);
        tag.putInt("Mined", mined);
        tag.putLongArray("Fields", java.util.Arrays.copyOf(fields, fieldCount));
        tag.putLongArray("Clearing", java.util.Arrays.copyOf(clearing, clearingCount));

        if (!overflow.isEmpty()) {
            var list = new ListTag();

            for (var stack : overflow) {
                list.add(stack.save(registries));
            }

            tag.put("Overflow", list);
        }

        return tag;
    }

    void load(CompoundTag tag, HolderLookup.Provider registries) {
        chunk = tag.getInt("Chunk");
        layerY = tag.contains("LayerY") ? tag.getInt("LayerY") : Integer.MIN_VALUE;
        cursor = Math.clamp(tag.getInt("Cursor"), 0, LAYER - 1);
        // A save from before this fix has no flag: its area is swept once, then the dig carries on.
        areaCleared = tag.getBoolean("AreaCleared");
        verifying = tag.getBoolean("Verifying");
        verifyY = tag.getInt("VerifyY");
        verifyCursor = Math.clamp(tag.getInt("VerifyCursor"), 0, LAYER - 1);
        rechecks = tag.getInt("Rechecks");
        clearChunk = Math.max(0, tag.getInt("ClearChunk"));
        clearColumn = Math.clamp(tag.getInt("ClearColumn"), 0, LAYER - 1);
        travelTicks = tag.getInt("Travel");
        finished = tag.getBoolean("Finished");
        mined = tag.getInt("Mined");
        waterChunkIndex = -1;
        fields = tag.getLongArray("Fields");
        fieldCount = fields.length;
        clearing = tag.getLongArray("Clearing");
        clearingCount = clearing.length;
        structureBoxesChunk = null;
        overflow.clear();

        for (var entry : tag.getList("Overflow", Tag.TAG_COMPOUND)) {
            ItemStack.parse(registries, entry).ifPresent(overflow::add);
        }
    }

    /** What the renderer needs, small: where the head is and how deep the cutter is. */
    void writeClient(CompoundTag tag) {
        tag.putInt("Chunk", chunk);
        tag.putInt("LayerY", layerY);
        tag.putBoolean("Finished", finished);
    }

    void readClient(CompoundTag tag) {
        chunk = tag.getInt("Chunk");
        layerY = tag.contains("LayerY") ? tag.getInt("LayerY") : Integer.MIN_VALUE;
        finished = tag.getBoolean("Finished");
    }
}
