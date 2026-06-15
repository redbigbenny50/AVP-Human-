package com.human.common.gameplay.entity.living.human.marine.ai.break_fall.sensor;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.BreakFallSensors;
import com.just.ai.goap.state.ReadableWorldState;
import com.just.core.functional.option.Option;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public class IsCloseToLandingSensor {

    public static @NotNull Boolean sense(Marine marine, ReadableWorldState worldState) {
        var landingPosOption = worldState.getOrDefault(BreakFallSensors.LANDING_BLOCK_POS.key(), Option.<BlockPos>none());

        if (landingPosOption.isNone()) {
            return false;
        }

        var landingPos = landingPosOption.unwrap();
        var distance = Math.sqrt(marine.distanceToSqr(landingPos.getCenter()));

        return distance <= BreakFallSensors.WATER_PLACEMENT_DISTANCE;
    }
}
