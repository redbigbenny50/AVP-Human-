package com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy;

import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.strategy.FRIStrategyUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/** Water Breathing duration on a potion stack, plus the scoring terms shared with fire resistance. */
public class WBIStrategyUtil {

    public static int waterBreathingTicksFromStack(ItemStack itemStack) {
        var potionContents = itemStack.get(DataComponents.POTION_CONTENTS);

        if (potionContents == null) {
            return 0;
        }

        for (MobEffectInstance mobEffectInstance : potionContents.getAllEffects()) {
            if (mobEffectInstance.getEffect() == MobEffects.WATER_BREATHING) {
                return mobEffectInstance.getDuration();
            }
        }

        return 0;
    }

    /**
     * Higher means "drink something now". Being under the water is the fire; the air bar draining is the low health.
     */
    public static double urgencyTerm(boolean isUnderwater, double airRatio) {
        return FRIStrategyUtil.urgencyTerm(isUnderwater, airRatio);
    }

    public static double score(
        boolean isUnderwater,
        double airRatio,
        int waterBreathingTicksRemaining,
        int newTicks,
        int useTicks,
        double handlingPenalty
    ) {
        var weights = com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.strategy.FRIStrategy.Weights.DEFAULT;
        var urgency = urgencyTerm(isUnderwater, airRatio);
        var duration = FRIStrategyUtil.durationTermSeconds(newTicks);
        var use = FRIStrategyUtil.timePenalty(useTicks);
        var waste = FRIStrategyUtil.overlapWasteTerm(waterBreathingTicksRemaining, newTicks);

        return weights.urgency() * urgency
            + weights.duration() * duration
            - weights.useTimePenalty() * use
            - weights.overlapWastePenalty() * waste
            - handlingPenalty;
    }

    private WBIStrategyUtil() {
        throw new UnsupportedOperationException();
    }
}
