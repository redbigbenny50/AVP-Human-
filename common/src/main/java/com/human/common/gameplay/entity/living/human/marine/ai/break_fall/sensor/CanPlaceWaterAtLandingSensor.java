package com.human.common.gameplay.entity.living.human.marine.ai.break_fall.sensor;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.BreakFallSensors;
import com.just.ai.goap.state.ReadableWorldState;
import com.just.core.functional.option.Option;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public class CanPlaceWaterAtLandingSensor {

    public static @NotNull Boolean sense(Marine marine, ReadableWorldState worldState) {
        // Can't place water in ultra warm dimensions (like the Nether).
        if (marine.level().dimensionType().ultraWarm()) {
            return false;
        }

        var landingPosOption = worldState.getOrDefault(BreakFallSensors.LANDING_BLOCK_POS.key(), Option.<BlockPos>none());

        if (landingPosOption.isNone()) {
            return false;
        }

        var landingPos = landingPosOption.unwrap();
        var level = marine.level();
        var landingState = level.getBlockState(landingPos);
        var belowState = level.getBlockState(landingPos.below());

        // The landing position must be replaceable.
        if (!landingState.canBeReplaced()) {
            return false;
        }

        // Can't place water on top of lava or other fluids.
        var belowFluidState = belowState.getFluidState();

        return belowFluidState.isEmpty();
    }
}
