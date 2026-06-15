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
 * Strategy for consuming honey bottles. Honey bottles provide food saturation and remove poison effects.
 */
public class HoneyBottleStrategy implements HealingStrategy {

    // Honey bottle: 6 nutrition, 1.2 saturation.
    private static final float EFFECTIVE_HEALING = 6.0f * 0.5f + 1.2f * 2.0f;

    @Override
    public boolean isValidItemStack(ItemStack itemStack) {
        return itemStack.is(Items.HONEY_BOTTLE);
    }

    @Override
    public Collection<BLibInventory.Entry> selectEntriesFromInventory(BLibInventory inventory) {
        return inventory.selectEntries(Items.HONEY_BOTTLE);
    }

    @Override
    public boolean isValidWorldState(LivingEntity livingEntity, ReadableWorldState worldState) {
        var healthRatio = worldState.getOrDefault(GOAPSensors.HEALTH_RATIO.key(), 1.0F);
        return healthRatio < HealingStrategyUtil.HEAL_THRESHOLD;
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

        // Side-benefit: honey removes poison effect.
        var poisonCureBenefit = livingEntity.hasEffect(net.minecraft.world.effect.MobEffects.POISON) ? 0.3 : 0.0;

        // Food provides indirect healing through saturation, so add delay penalty.
        var indirectHealingPenalty = 0.15;

        return weights.urgency() * U
            + weights.healingAmount() * H
            + weights.sideBenefit() * poisonCureBenefit
            - weights.useTimePenalty() * use
            - weights.wasteOverhealPenalty() * waste
            - indirectHealingPenalty;
    }

    @Override
    public Action.Signal execute(Action.Context<? extends LivingEntity> context) {
        var livingEntity = context.getActor();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        return ConsumeItemAction.perform(SoundEvents.GENERIC_DRINK, livingEntity, blackboard, () -> HealingStrategyUtil.eat(livingEntity));
    }
}
