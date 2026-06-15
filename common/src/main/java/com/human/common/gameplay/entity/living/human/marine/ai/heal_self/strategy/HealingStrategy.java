package com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy;

import com.human.common.gameplay.entity.ai.utility.item.ItemStrategy;
import com.just.ai.goap.action.Action;
import net.minecraft.world.entity.LivingEntity;

public interface HealingStrategy extends ItemStrategy {

    Action.Signal execute(Action.Context<? extends LivingEntity> context);

    /**
     * @param urgency              Higher means "heal now" (low HP).
     * @param healingAmount        Reward more healing.
     * @param useTimePenalty       Penalize long use time (entity is vulnerable while using).
     * @param sideEffectPenalty    Penalize negative side effects (hunger from rotten flesh, poison from potato).
     * @param sideBenefit          Reward side buffs (e.g., golden apple effects).
     * @param rarityPenalty        Make rare/valuable items harder to spend unless urgent.
     * @param wasteOverhealPenalty Penalize waste if healing would exceed max health.
     */
    record Weights(
        double urgency,
        double healingAmount,
        double useTimePenalty,
        double sideEffectPenalty,
        double sideBenefit,
        double rarityPenalty,
        double wasteOverhealPenalty
    ) {

        public static final Weights DEFAULT = new Weights(
            // urgency
            2.0,
            // healingAmount
            1.5,
            // useTimePenalty
            0.8,
            // sideEffectPenalty
            1.5,
            // sideBenefit
            0.6,
            // rarityPenalty
            1.2,
            // wasteOverhealPenalty
            0.5
        );
    }
}
