package com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.strategy.impl;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.impl.ConsumeItemAction;
import com.blib.api.common.inventory.v1.BLibInventory;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.strategy.FRIStrategy;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.strategy.FRIStrategyUtil;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategyUtil;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import com.just.ai.goap.state.ReadableWorldState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collection;

public class EnchantedGoldenAppleFRIStrategy implements FRIStrategy {

    private static final int TICK_DURATION = Foods.ENCHANTED_GOLDEN_APPLE
        .effects()
        .stream()
        .map(FoodProperties.PossibleEffect::effect)
        .filter(effect -> effect.is(MobEffects.FIRE_RESISTANCE))
        .mapToInt(MobEffectInstance::getDuration)
        .findFirst()
        .orElse(0);

    @Override
    public boolean isValidItemStack(ItemStack itemStack) {
        return itemStack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    @Override
    public boolean isValidWorldState(LivingEntity livingEntity, ReadableWorldState worldState) {
        return true;
    }

    @Override
    public Collection<BLibInventory.Entry> selectEntriesFromInventory(BLibInventory inventory) {
        return inventory.selectEntries(Items.ENCHANTED_GOLDEN_APPLE);
    }

    @Override
    public double score(LivingEntity livingEntity, ReadableWorldState worldState, ItemStack itemStack) {
        var healthRatio = worldState.getOrDefault(GOAPSensors.HEALTH_RATIO.key(), 1.0F);
        var isOnFire = worldState.getOrDefault(GOAPSensors.IS_ON_FIRE.key(), false);
        var fireResTicksRemaining = worldState.getOrDefault(GOAPSensors.FIRE_RESISTANCE_REMAINING_TICKS.key(), 0);
        var weights = FRIStrategy.Weights.DEFAULT;

        var U = FRIStrategyUtil.urgencyTerm(isOnFire, healthRatio);
        var D = FRIStrategyUtil.durationTermSeconds(TICK_DURATION);

        // Side-benefit is bigger if low HP.
        var side = FRIStrategyUtil.clamp01((1.0 - healthRatio) * 0.6);

        var use = FRIStrategyUtil.timePenalty(itemStack.getUseDuration(livingEntity));
        var waste = FRIStrategyUtil.overlapWasteTerm(fireResTicksRemaining, TICK_DURATION);

        // Apply rarity penalty unless highly urgent.
        return weights.urgency() * U
            + weights.duration() * D
            + weights.sideBenefit() * side
            - weights.useTimePenalty() * use
            - weights.overlapWastePenalty() * waste
            - weights.rarityPenalty() * (1.0 - U);
    }

    @Override
    public Action.Signal execute(Action.Context<? extends LivingEntity> context) {
        var livingEntity = context.getActor();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        return ConsumeItemAction.perform(SoundEvents.GENERIC_EAT, livingEntity, blackboard, () -> HealingStrategyUtil.eat(livingEntity));
    }
}
