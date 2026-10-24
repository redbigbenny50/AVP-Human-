package com.human.common.gameplay.quarry;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * REFILL HOLE ([stated] Oct 15): fills the dug area back in, chunk by chunk, from MIN Y up to MAX Y, "at the same speed
 * it dug", taking blocks from the 3 x 3 FILL WITH grid in slot order - slot 1 first, then 2, and so on. Mining and
 * refilling never run together; the terminal enforces that.
 * <h2>COST</h2> The same shape as {@link QuarryMiner}: a fixed budget per tick (one layer per {@code secondsPerLayer}),
 * spots that are already solid cost no time so a layer with nothing to fill goes by in a tick, block writes without
 * neighbour updates, nothing per block but a state read and a claim check, and one small client update per layer at
 * most.
 */
final class QuarryFiller {

    private final QuarryTerminalBlockEntity terminal;

    private int chunk;

    private int layerY = Integer.MIN_VALUE;

    private int cursor;

    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

    private double carry;

    private int travelTicks;

    private int placed;

    /** The grid ran dry; stays NO FILL BLOCKS until a block is there. */
    private boolean outOfBlocks;

    private boolean finished;

    /** The fill slot used last (0-8), or -1 - for the PROGRESS tab's FILLING WITH. */
    private int lastSlot = -1;

    QuarryFiller(QuarryTerminalBlockEntity terminal) {
        this.terminal = terminal;
    }

    boolean finished() {
        return finished;
    }

    int chunkIndex() {
        return chunk;
    }

    int placed() {
        return placed;
    }

    int lastSlot() {
        return lastSlot;
    }

    int layerY() {
        return layerY == Integer.MIN_VALUE ? terminal.refillMin() : layerY;
    }

    int layersPerChunk() {
        return Math.max(0, terminal.refillMax() - terminal.refillMin() + 1);
    }

    /** Layers still to fill, counting the current one - TIME LEFT and AREA FILLED come from this. */
    int layersLeft(int chunks) {
        if (finished || chunks == 0) {
            return 0;
        }

        var perChunk = layersPerChunk();
        var here = Math.max(0, terminal.refillMax() - layerY() + 1);

        return Math.max(0, chunks - chunk - 1) * perChunk + Math.min(perChunk, here);
    }

    void reset() {
        chunk = 0;
        layerY = Integer.MIN_VALUE;
        cursor = 0;
        carry = 0.0D;
        travelTicks = 0;
        placed = 0;
        finished = false;
        lastSlot = -1;
        outOfBlocks = false;
    }

    /**
     * Whether a stack can go in the FILL WITH grid: a plain placeable block - no chests, machines or anything with a
     * block entity, and none of the quarry's own blocks.
     */
    static boolean isFill(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item
            && !(item.getBlock() instanceof EntityBlock)
            && !(item.getBlock() instanceof QuarryFrameBlock)
            && !(item.getBlock() instanceof QuarryContainmentBlock)
            && !item.getBlock().defaultBlockState().isAir();
    }

    /** One powered tick of refilling. Returns the status to show. */
    QuarryTerminalBlockEntity.Status tick(ServerLevel level, List<ChunkPos> chunks) {
        if (finished) {
            return QuarryTerminalBlockEntity.Status.FILLED;
        }

        if (travelTicks > 0) {
            travelTicks--;
            return QuarryTerminalBlockEntity.Status.REFILLING;
        }

        if (chunk >= chunks.size()) {
            finished = true;
            layerY = Integer.MIN_VALUE;
            terminal.refillFinished();
            return QuarryTerminalBlockEntity.Status.FILLED;
        }

        var target = chunks.get(chunk);

        if (!level.hasChunk(target.x, target.z)) {
            return QuarryTerminalBlockEntity.Status.WAITING;
        }

        if (layerY == Integer.MIN_VALUE) {
            layerY = terminal.refillMin();
            cursor = 0;
            terminal.miningMoved();

            // Down to MIN Y first, at the speed the renderer lowers the bit, so it is at the bottom when the first
            // block goes in.
            travelTicks = (int) Math.ceil(Math.max(0.0D, terminal.startY() + 1.0D - layerY) / QuarryMiner.RISE_PER_TICK);

            return QuarryTerminalBlockEntity.Status.REFILLING;
        }

        // ⚠ Oct 16 fix - out of fill blocks stays NO FILL BLOCKS until some arrive, instead of flipping to REFILLING
        // on every tick the budget had not yet built up to a whole block.
        if (outOfBlocks) {
            if (nextFillSlot() < 0) {
                return QuarryTerminalBlockEntity.Status.NO_FILL;
            }

            outOfBlocks = false;
        }

        carry = Math.min(4.0D, carry + QuarryMiner.LAYER / (terminal.secondsPerLayer() * 20.0D));

        for (int scanned = 0; scanned < QuarryMiner.MAX_SCAN_PER_TICK; scanned++) {
            // One reused position for the scan (up to a layer's 256 reads a tick through empty space); a fresh, fixed
            // one is made only for a spot that is actually worked.
            probe.set(target.getMinBlockX() + (cursor & 15), layerY, target.getMinBlockZ() + (cursor >> 4));

            var state = level.getBlockState(probe);

            if (needsFill(state)) {
                if (carry < 1.0D) {
                    break;
                }

                var at = probe.immutable();

                var slot = nextFillSlot();

                if (slot < 0) {
                    carry = 0.0D;
                    outOfBlocks = true;
                    return QuarryTerminalBlockEntity.Status.NO_FILL;
                }

                // A claim made since the area was confirmed still stops it, block by block - same as digging.
                if (QuarryProtection.check(level, at, terminal.ownerId())) {
                    var stack = terminal.fills().getItem(slot);
                    var block = ((BlockItem) stack.getItem()).getBlock();

                    level.setBlock(at, block.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    terminal.fills().removeItem(slot, 1);
                    lastSlot = slot;
                    placed++;
                    terminal.contentsChanged();
                }

                carry -= 1.0D;
            }

            if (++cursor >= QuarryMiner.LAYER) {
                cursor = 0;
                layerY++;

                if (layerY > terminal.refillMax()) {
                    nextChunk(chunks);
                    break;
                }

                terminal.miningMoved();
            }
        }

        return QuarryTerminalBlockEntity.Status.REFILLING;
    }

    /**
     * What gets filled: air, liquid, anything replaceable (grass, snow layers), and the quarry's own containment field.
     * Anything solid is already "full" and costs no time.
     */
    private static boolean needsFill(BlockState state) {
        return state.isAir()
            || state.canBeReplaced()
            || !state.getFluidState().isEmpty() && state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock
            || state.getBlock() instanceof QuarryContainmentBlock;
    }

    /** Slot 1 first, then 2, and so on ([stated] Oct 15). -1 when the grid is empty. */
    private int nextFillSlot() {
        var fills = terminal.fills();

        for (int slot = 0; slot < fills.getContainerSize(); slot++) {
            if (isFill(fills.getItem(slot))) {
                return slot;
            }
        }

        return -1;
    }

    /** Up and over to the next chunk's bottom. */
    private void nextChunk(List<ChunkPos> chunks) {
        chunk++;
        layerY = Integer.MIN_VALUE;
        cursor = 0;
        carry = 0.0D;
        // Over to the next chunk (the drop to its MIN Y is waited for when that chunk starts).
        travelTicks = (int) Math.ceil(16.0D / QuarryMiner.TRAVEL_PER_TICK);
        terminal.miningMoved();
    }

    CompoundTag save() {
        var tag = new CompoundTag();

        tag.putInt("Chunk", chunk);
        tag.putInt("LayerY", layerY);
        tag.putInt("Cursor", cursor);
        tag.putInt("Travel", travelTicks);
        tag.putInt("Placed", placed);
        tag.putBoolean("Finished", finished);
        tag.putInt("LastSlot", lastSlot);

        return tag;
    }

    void load(CompoundTag tag) {
        chunk = tag.getInt("Chunk");
        layerY = tag.contains("LayerY") ? tag.getInt("LayerY") : Integer.MIN_VALUE;
        cursor = Math.clamp(tag.getInt("Cursor"), 0, QuarryMiner.LAYER - 1);
        travelTicks = tag.getInt("Travel");
        placed = tag.getInt("Placed");
        finished = tag.getBoolean("Finished");
        lastSlot = tag.contains("LastSlot") ? tag.getInt("LastSlot") : -1;
    }

    /** What the renderer needs: which chunk and which layer the head is filling. */
    void writeClient(CompoundTag tag) {
        tag.putInt("Chunk", chunk);
        tag.putInt("LayerY", layerY);
        tag.putBoolean("Finished", finished);
    }
}
