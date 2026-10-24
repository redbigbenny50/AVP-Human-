package com.human.common.gameplay.block;

import com.human.common.gameplay.fluid.CryoFreezing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.NotNull;

/** Placed cryofluid. Everything it does to an entity lives in {@link CryoFreezing}. */
public class CryofluidLiquidBlock extends LiquidBlock {

    public CryofluidLiquidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Entity entity
    ) {
        super.entityInside(state, level, pos, entity);

        // Extinguishes like water: it is a freezing liquid, not lava.
        if (entity.isOnFire()) {
            entity.clearFire();
        }

        if (!level.isClientSide && entity instanceof LivingEntity living && isCountedBlock(level, pos, living)) {
            CryoFreezing.tick(living);
        }
    }

    @Override
    protected void onPlace(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        freezeNeighbours(level, pos);
    }

    @Override
    protected void neighborChanged(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull net.minecraft.world.level.block.Block neighborBlock,
        @NotNull BlockPos neighborPos,
        boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        freezeNeighbours(level, pos);
    }

    /**
     * [stated] Oct 20 - cryofluid freezes the nether resin it touches into netherrack, in its own shape (as powder snow
     * does). Only on placing and on a neighbour changing - never every tick.
     */
    private static void freezeNeighbours(Level level, BlockPos pos) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        for (var direction : net.minecraft.core.Direction.values()) {
            var side = pos.relative(direction);

            if (serverLevel.isLoaded(side)) {
                com.human.compatibility.avp_alien.HumanResinConversion.freeze(serverLevel, side, serverLevel.getBlockState(side));
            }
        }
    }

    /**
     * ⚠ {@code entityInside} runs once for EVERY block the entity overlaps, so a player standing in fluid two blocks
     * deep would be frozen twice a tick. Only one block counts: the one at the feet, or, when the feet are out of it,
     * the one at the eyes.
     */
    private boolean isCountedBlock(Level level, BlockPos pos, LivingEntity living) {
        var feet = living.blockPosition();

        if (level.getBlockState(feet).is(this)) {
            return pos.equals(feet);
        }

        return pos.equals(BlockPos.containing(living.getEyePosition()));
    }
}
