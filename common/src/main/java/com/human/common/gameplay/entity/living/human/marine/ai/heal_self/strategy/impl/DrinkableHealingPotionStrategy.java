package com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.impl;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.impl.ConsumeItemAction;
import com.blib.api.common.inventory.v1.BLibInventory;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategy;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategyUtil;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import com.just.ai.goap.state.ReadableWorldState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collection;
import java.util.Objects;

/**
 * Strategy for using drinkable potions of instant healing.
 */
public class DrinkableHealingPotionStrategy implements HealingStrategy {

    @Override
    public boolean isValidItemStack(ItemStack itemStack) {
        return itemStack.is(Items.POTION) && HealingStrategyUtil.hasMobEffect(itemStack, MobEffects.HEAL);
    }

    @Override
    public Collection<BLibInventory.Entry> selectEntriesFromInventory(BLibInventory inventory) {
        return inventory.selectEntries(Items.POTION);
    }

    @Override
    public boolean isValidWorldState(LivingEntity livingEntity, ReadableWorldState worldState) {
        var healthRatio = worldState.getOrDefault(GOAPSensors.HEALTH_RATIO.key(), 1.0F);
        return healthRatio < HealingStrategyUtil.HEAL_THRESHOLD;
    }

    @Override
    public double score(LivingEntity livingEntity, ReadableWorldState worldState, ItemStack itemStack) {
        var healthRatio = worldState.getOrDefault(GOAPSensors.HEALTH_RATIO.key(), 1.0F);
        var healing = HealingStrategyUtil.getInstantHealthHealing(itemStack);
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

        return weights.urgency() * U
            + weights.healingAmount() * H
            - weights.useTimePenalty() * use
            - weights.wasteOverhealPenalty() * waste;
    }

    @Override
    public Action.Signal execute(Action.Context<? extends LivingEntity> context) {
        var livingEntity = context.getActor();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        return ConsumeItemAction.perform(SoundEvents.GENERIC_DRINK, livingEntity, blackboard, () -> onConsume(livingEntity));
    }

    private static Action.Signal onConsume(LivingEntity livingEntity) {
        var itemStack = livingEntity.getMainHandItem();

        if (!itemStack.is(Items.POTION)) {
            return Action.Signal.ABORT;
        }

        var potionContents = Objects.requireNonNull(itemStack.get(DataComponents.POTION_CONTENTS));

        potionContents.getAllEffects().forEach(livingEntity::addEffect);

        itemStack.shrink(1);

        return Action.Signal.CONTINUE;
    }
}
