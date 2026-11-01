package com.human.common.gameplay.cosmetic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Bunk beds that work as beds ([stated] Oct 31): two sleepers, one per bunk, players or NPCs. Sleeping through the night
 * and sleeping itself are vanilla's own; this class answers the questions vanilla asks of a bed, which the bed mixins
 * route here because the bunk is a cosmetic multiblock, not a BedBlock.
 * <ul>
 * <li>Each bunk's sleeping block is the PILLOW END of its level (cell {1, 0, 0} bottom, {1, 1, 0} top) - two different
 * positions, so two sleepers never share one.</li>
 * <li>Sleepers lie along the bunk, head at the pillow (orientation = the facing's clockwise side, the model's +X).</li>
 * <li>Lying height is the mattress top + 2 px, as vanilla does for its own bed (mattress 9 px, body at 11 px): the
 * lower mattress tops out at 7 px, the upper at 20 px (4 px into its own block).</li>
 * <li>Getting up puts the sleeper on the floor in front of the bunk, when that spot is clear.</li>
 * </ul>
 */
public final class CosmeticBunks {

    /** Cell indices of the two sleeping blocks in the bunk's cell list. */
    private static final int BOTTOM_CELL = 1;

    private static final int TOP_CELL = 3;

    private CosmeticBunks() {}

    public static boolean isBunk(CosmeticDesign design) {
        return design == CosmeticDesign.BUNK_BED;
    }

    /** The bunk block at {@code pos} if it is one of the two sleeping blocks, else null. */
    private static @Nullable CosmeticBlock sleepingBlock(BlockGetter level, BlockPos pos) {
        var state = level.getBlockState(pos);

        if (state.getBlock() instanceof CosmeticBlock block && isBunk(block.design())) {
            var cell = state.getValue(CosmeticMultiblock.CELL);

            if (cell == BOTTOM_CELL || cell == TOP_CELL) {
                return block;
            }
        }

        return null;
    }

    public static boolean isBunkBed(BlockGetter level, BlockPos pos) {
        return sleepingBlock(level, pos) != null;
    }

    /** Head direction of a sleeper in the bunk at {@code pos}: the facing's clockwise side (the pillow end). */
    public static @Nullable Direction orientation(BlockGetter level, BlockPos pos) {
        if (sleepingBlock(level, pos) == null) {
            return null;
        }

        return level.getBlockState(pos).getValue(CosmeticMultiblock.FACING).getClockWise();
    }

    /** Where a sleeper's body lies above {@code pos}: the mattress top + 2 px. */
    public static double lyingHeight(BlockGetter level, BlockPos pos) {
        var top = level.getBlockState(pos).getValue(CosmeticMultiblock.CELL) == TOP_CELL;

        return pos.getY() + (top ? 6.0 : 9.0) / 16.0;
    }

    /** Where a sleeper stands after getting up: the floor in front of the bunk, or null to keep vanilla's choice. */
    public static @Nullable Vec3 standUp(Level level, BlockPos pos, LivingEntity sleeper) {
        var state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof CosmeticBlock block) || !isBunk(block.design())) {
            return null;
        }

        var facing = state.getValue(CosmeticMultiblock.FACING);
        var core = block.corePos(pos, state);
        var floor = new BlockPos(pos.getX(), core.getY(), pos.getZ()).relative(facing);
        var at = Vec3.atBottomCenterOf(floor);

        return level.noCollision(sleeper, sleeper.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(at)) ? at : null;
    }

    /** The sleeping block for a click: the top bunk when the click is on the upper block, otherwise the bottom one. */
    public static BlockPos sleepingPos(CosmeticBlock block, BlockPos core, Direction facing, double hitY) {
        var top = hitY >= core.getY() + 1;

        return block.cellPos(core, facing, block.cells()[top ? TOP_CELL : BOTTOM_CELL]);
    }

    /** Someone (player or NPC) already sleeps at {@code sleepingPos}. */
    public static boolean occupied(Level level, BlockPos sleepingPos) {
        return !level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(sleepingPos).inflate(1),
            entity -> entity.isSleeping() && entity.getSleepingPos().map(sleepingPos::equals).orElse(false)
        ).isEmpty();
    }

    // ------------------------------------------------------------------------------------------------------------
    // NPC claims - [stated] Oct 31: colonists, marines and W-Y troops sleep in free bunks at night
    // ------------------------------------------------------------------------------------------------------------

    /** How far an NPC looks for a free bunk, horizontally and vertically (blocks). */
    public static final int SEARCH_RADIUS = 32;

    public static final int SEARCH_HEIGHT = 16;

    private static boolean isSleepingCell(net.minecraft.world.level.block.state.BlockState state) {
        if (state.getBlock() instanceof CosmeticBlock block && isBunk(block.design())) {
            var cell = state.getValue(CosmeticMultiblock.CELL);
            return cell == BOTTOM_CELL || cell == TOP_CELL;
        }

        return false;
    }

    /**
     * The nearest bunk sleeping block around {@code origin} that no other NPC has claimed and nobody sleeps in, or
     * null. Cheap by construction: only LOADED chunks are read, and a section's palette is checked first
     * ({@code maybeHas}) - a section without a bunk in it is skipped without reading a single block.
     */
    public static @Nullable BlockPos findFree(net.minecraft.server.level.ServerLevel level, BlockPos origin, LivingEntity claimant) {
        // Collect the bunk blocks first (cheap: palette check, then block reads), then test them nearest-first and stop
        // at the first free one - so a barracks of fifty bunks costs one claims lookup and usually one occupancy check,
        // not an entity search per bunk.
        var candidates = new java.util.ArrayList<BlockPos>();
        int minChunkX = (origin.getX() - SEARCH_RADIUS) >> 4, maxChunkX = (origin.getX() + SEARCH_RADIUS) >> 4;
        int minChunkZ = (origin.getZ() - SEARCH_RADIUS) >> 4, maxChunkZ = (origin.getZ() + SEARCH_RADIUS) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);

                if (chunk == null) {
                    continue;
                }

                var sections = chunk.getSections();

                for (int index = 0; index < sections.length; index++) {
                    var section = sections[index];
                    var sectionY = chunk.getSectionYFromSectionIndex(index) << 4;

                    if (section.hasOnlyAir() || Math.abs(sectionY + 8 - origin.getY()) > SEARCH_HEIGHT + 8 || !section.maybeHas(CosmeticBunks::isSleepingCell)) {
                        continue;
                    }

                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                if (!isSleepingCell(section.getBlockState(x, y, z))) {
                                    continue;
                                }

                                candidates.add(new BlockPos((chunkX << 4) + x, sectionY + y, (chunkZ << 4) + z));
                            }
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(java.util.Comparator.comparingDouble(pos -> pos.distSqr(origin)));

        // Every claim that could matter, in ONE entity search: a claimant is within its own search radius of its bunk,
        // and the bunk within ours, so it is within twice that of us.
        var claimed = new java.util.HashSet<BlockPos>();

        for (var human : level.getEntitiesOfClass(
            com.human.common.gameplay.entity.living.human.AbstractHuman.class,
            new AABB(origin).inflate(SEARCH_RADIUS * 2),
            human -> human != claimant && human.getBunkPos() != null
        )) {
            claimed.add(human.getBunkPos());
        }

        for (var pos : candidates) {
            if (!claimed.contains(pos) && !occupied(level, pos)) {
                return pos;
            }
        }

        return null;
    }

    /** Another NPC near the bunk has already claimed it. */
    public static boolean claimedByAnother(Level level, BlockPos pos, LivingEntity claimant) {
        return !level.getEntitiesOfClass(
            com.human.common.gameplay.entity.living.human.AbstractHuman.class,
            new AABB(pos).inflate(SEARCH_RADIUS * 2),
            human -> human != claimant && pos.equals(human.getBunkPos())
        ).isEmpty();
    }

    /** Where an NPC walks to before lying down: the floor in front of its bunk. */
    public static @Nullable BlockPos approach(Level level, BlockPos sleepingPos) {
        var state = level.getBlockState(sleepingPos);

        if (!(state.getBlock() instanceof CosmeticBlock block) || !isBunk(block.design())) {
            return null;
        }

        return respawnPoint(sleepingPos, block.corePos(sleepingPos, state), state.getValue(CosmeticMultiblock.FACING));
    }

    /** The floor in front of a bunk - the forced respawn point a sleeper is given. */
    public static BlockPos respawnPoint(BlockPos sleepingPos, BlockPos core, Direction facing) {
        return new BlockPos(sleepingPos.getX(), core.getY(), sleepingPos.getZ()).relative(facing);
    }
}
