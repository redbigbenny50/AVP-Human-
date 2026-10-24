package com.human.common.gameplay.quarry;

import com.human.common.registry.init.HumanQuarry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps liquid out of the chunk a quarry is digging, however it arrives.
 * <p>
 * Oct 18 - [stated] a tester: "not sure how it happened but the water broke through the containment field blocks and
 * then flooded and then more containment fields came about". The cause: when the drill finishes a chunk, that chunk's
 * containment comes down (as ruled) and the lake pours into its pit - and the next chunk, already dug down beside it,
 * has no field where the water now arrives, because a field is only placed at the moment a spot is dug, and only if
 * liquid already touched it then. The water flowed into those dug spots, spread over the flat floor and, with two
 * sources beside each gap, made new source blocks. His ruling: "Just seal any flow that reaches the dig (keep per-chunk
 * release)".
 * <h2>How</h2> Each quarry registers the chunk it is digging (its full dug height). When any liquid tries to SPREAD
 * into a cell of that volume (FlowingFluid.spreadTo, every downward and sideways flow - see
 * MixinFlowingFluid_QuarryDigSeal), the spread is refused and the cell becomes a containment field, registered with the
 * quarry so it comes down with that chunk's other fields, exactly as before. No liquid enters, so no new sources form.
 * <p>
 * ⚠ Not registered for an ocean or river chunk with DRAIN LIQUIDS off: there the sea is ruled to carry on down the hole
 * as if it were air.
 */
public final class QuarryDigSeal {

    private record Active(
        ResourceKey<Level> dimension,
        int minX,
        int minZ,
        int minY,
        int maxY
    ) {

        boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() < minX + 16
                && pos.getZ() >= minZ && pos.getZ() < minZ + 16
                && pos.getY() >= minY && pos.getY() <= maxY;
        }
    }

    /** ⚠ Weak keys: a quarry that unloads or is broken is dropped, never kept alive by this map. */
    private static final Map<QuarryMiner, Active> ACTIVE = new WeakHashMap<>();

    private QuarryDigSeal() {
        throw new UnsupportedOperationException();
    }

    /** The quarry is digging this chunk, from {@code minY} up to {@code maxY}. */
    static void set(QuarryMiner miner, ServerLevel level, ChunkPos chunk, int minY, int maxY) {
        var active = new Active(level.dimension(), chunk.getMinBlockX(), chunk.getMinBlockZ(), minY, maxY);

        if (!active.equals(ACTIVE.get(miner))) {
            ACTIVE.put(miner, active);
        }
    }

    /** The quarry is not digging (moving on, finished, retracted, broken, or a pass-through water chunk). */
    static void clear(QuarryMiner miner) {
        ACTIVE.remove(miner);
    }

    /**
     * Called as a liquid is about to spread into {@code pos}. {@return true if the spread must be refused}: the cell is
     * in a chunk being dug - and it is then sealed with a containment field if nothing solid is there. Server side; one
     * map check when no quarry is digging, so ordinary water costs nothing.
     */
    public static boolean refuseSpread(LevelAccessor accessor, BlockPos pos) {
        if (ACTIVE.isEmpty() || !(accessor instanceof ServerLevel level)) {
            return false;
        }

        for (var entry : ACTIVE.entrySet()) {
            var active = entry.getValue();

            if (active.dimension() != level.dimension() || !active.contains(pos)) {
                continue;
            }

            var state = level.getBlockState(pos);

            // Air, or flowing liquid already in the cell: replaced by the field. A solid block is left as it is - the
            // refused spread already keeps the liquid out.
            if (state.isAir() || state.canBeReplaced()) {
                level.setBlock(
                    pos,
                    HumanQuarry.QUARRY_CONTAINMENT_FIELD.get().defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
                );
                entry.getKey().registerField(pos.immutable());
            }

            return true;
        }

        return false;
    }
}
