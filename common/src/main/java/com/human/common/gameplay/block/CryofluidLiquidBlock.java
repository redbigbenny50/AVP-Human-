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
