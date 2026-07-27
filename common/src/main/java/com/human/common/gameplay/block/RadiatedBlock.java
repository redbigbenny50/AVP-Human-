package com.human.common.gameplay.block;

import com.human.common.model.RadiationExposure;
import com.human.util.HumanPredicates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class RadiatedBlock extends Block {

    public RadiatedBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull BlockState blockState, @NotNull Entity entity) {
        if (HumanPredicates.canBeIrradiated(entity) && entity instanceof LivingEntity livingEntity) {
            // Standing on the hot stuff marks exposure while contact lasts, at the same strength as irradiated
            // ground - step off and the level starts falling again.
            ((RadiationExposure) livingEntity).avp_human$markRadiationSource(2);
        }

        super.stepOn(level, blockPos, blockState, entity);
    }
}
