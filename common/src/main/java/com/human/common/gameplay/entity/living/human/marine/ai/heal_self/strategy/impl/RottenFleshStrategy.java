package com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.impl;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.impl.ConsumeItemAction;
import com.blib.api.common.inventory.v1.BLibInventory;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategy;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategyUtil;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import com.just.ai.goap.state.ReadableWorldState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collection;

/**
 * Strategy for eating rotten flesh. Rotten flesh provides food but has a high chance of causing hunger effect. Only
 * used as a last resort when health is critically low.
 */
public class RottenFleshStrategy implements HealingStrategy {

    // Rotten flesh: 4 nutrition, 0.1 saturation, 80% chance of 30 seconds hunger.
    private static final float EFFECTIVE_HEALING = 4.0f * 0.5f + 0.1f * 2.0f;

    // Only use rotten flesh when health is critically low (below 25%).
    private static final float CRITICAL_THRESHOLD = 0.25f;

    @Override
    public boolean isValidItemStack(ItemStack itemStack) {
        return itemStack.is(Items.ROTTEN_FLESH);
    }

    @Override
    public Collection<BLibInventory.Entry> selectEntriesFromInventory(BLibInventory inventory) {
        return inventory.selectEntries(Items.ROTTEN_FLESH);
    }

    @Override
    public boolean isValidWorldState(LivingEntity livingEntity, ReadableWorldState worldState) {
        var healthRatio = worldState.getOrDefault(GOAPSensors.HEALTH_RATIO.key(), 1.0F);
        // Only consider rotten flesh when critically low on health.
        return healthRatio < CRITICAL_THRESHOLD;
    }

    @Override
    public double score(LivingEntity livingEntity, ReadableWorldState worldState, ItemStack itemStack) {
        var healthRatio = worldState.getOrDefault(GOAPSensors.HEALTH_RATIO.key(), 1.0F);
        var weights = HealingStrategy.Weights.DEFAULT;

        var U = HealingStrategyUtil.urgencyTerm(healthRatio);
        var H = HealingStrategyUtil.healingTerm(EFFECTIVE_HEALING);
        var use = HealingStrategyUtil.timePenalty(itemStack.getUseDuration(livingEntity));
        var waste = HealingStrategyUtil.overhealWasteTerm(
            livingEntity.getHealth(),
            livingEntity.getMaxHealth(),
            EFFECTIVE_HEALING
        );

        // Significant side-effect penalty for hunger effect (80% chance).
        var sideEffectPenalty = 0.8;

        // Food provides indirect healing through saturation, so add delay penalty.
        var indirectHealingPenalty = 0.15;

        return weights.urgency() * U
            + weights.healingAmount() * H
            - weights.useTimePenalty() * use
            - weights.wasteOverhealPenalty() * waste
            - weights.sideEffectPenalty() * sideEffectPenalty
            - indirectHealingPenalty;
    }

    @Override
    public Action.Signal execute(Action.Context<? extends LivingEntity> context) {
        var livingEntity = context.getActor();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        return ConsumeItemAction.perform(SoundEvents.GENERIC_EAT, livingEntity, blackboard, () -> HealingStrategyUtil.eat(livingEntity));
    }
}
