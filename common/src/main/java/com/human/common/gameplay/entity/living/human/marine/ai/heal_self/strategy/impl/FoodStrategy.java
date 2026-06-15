package com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.impl;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.impl.ConsumeItemAction;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategy;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategyUtil;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import com.just.ai.goap.state.ReadableWorldState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Strategy for eating general food items. Uses saturation as a proxy for healing value. Excludes special food items
 * (golden apples, rotten flesh, poisonous potato, honey bottle).
 */
public class FoodStrategy implements HealingStrategy {

    @Override
    public boolean isValidItemStack(ItemStack itemStack) {
        // Exclude special cases handled by other strategies.
        if (
            itemStack.is(Items.GOLDEN_APPLE)
                || itemStack.is(Items.ENCHANTED_GOLDEN_APPLE)
                || itemStack.is(Items.ROTTEN_FLESH)
                || itemStack.is(Items.POISONOUS_POTATO)
                || itemStack.is(Items.HONEY_BOTTLE)
        ) {
            return false;
        }

        return HealingStrategyUtil.isFood(itemStack);
    }

    @Override
    public boolean isValidWorldState(LivingEntity livingEntity, ReadableWorldState worldState) {
        var healthRatio = worldState.getOrDefault(GOAPSensors.HEALTH_RATIO.key(), 1.0F);
        return healthRatio < HealingStrategyUtil.HEAL_THRESHOLD;
    }

    @Override
    public double score(LivingEntity livingEntity, ReadableWorldState worldState, ItemStack itemStack) {
        var healthRatio = worldState.getOrDefault(GOAPSensors.HEALTH_RATIO.key(), 1.0F);
        var healing = HealingStrategyUtil.getFoodHealing(itemStack);
        var weights = HealingStrategy.Weights.DEFAULT;

        if (healing <= 0) {
            return Double.NEGATIVE_INFINITY;
        }

        var U = HealingStrategyUtil.urgencyTerm(healthRatio);
        var H = HealingStrategyUtil.healingTerm(healing);
        var use = HealingStrategyUtil.timePenalty(itemStack.getUseDuration(livingEntity));
        var waste = HealingStrategyUtil.overhealWasteTerm(
            livingEntity.getHealth(),
            livingEntity.getMaxHealth(),
            healing
        );

        // Food provides indirect healing through saturation, so add delay penalty.
        var indirectHealingPenalty = 0.15;

        return weights.urgency() * U
            + weights.healingAmount() * H
            - weights.useTimePenalty() * use
            - weights.wasteOverhealPenalty() * waste
            - indirectHealingPenalty;
    }

    @Override
    public Action.Signal execute(Action.Context<? extends LivingEntity> context) {
        var livingEntity = context.getActor();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        return ConsumeItemAction.perform(SoundEvents.GENERIC_EAT, livingEntity, blackboard, () -> HealingStrategyUtil.eat(livingEntity));
    }
}
