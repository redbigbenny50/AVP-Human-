package com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy.impl;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.impl.ConsumeItemAction;
import com.human.common.gameplay.entity.ai.goap.HumanGOAPSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy.WBIStrategy;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy.WBIStrategyUtil;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import com.just.ai.goap.state.ReadableWorldState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;

public class DrinkablePotionWBIStrategy implements WBIStrategy {

    @Override
    public boolean isValidItemStack(ItemStack itemStack) {
        return itemStack.is(Items.POTION) && WBIStrategyUtil.waterBreathingTicksFromStack(itemStack) > 0;
    }

    @Override
    public boolean isValidWorldState(LivingEntity livingEntity, ReadableWorldState worldState) {
        return true;
    }

    @Override
    public double score(LivingEntity livingEntity, ReadableWorldState worldState, ItemStack itemStack) {
        var ticks = WBIStrategyUtil.waterBreathingTicksFromStack(itemStack);

        if (ticks <= 0) {
            return Double.NEGATIVE_INFINITY;
        }

        return WBIStrategyUtil.score(
            worldState.getOrDefault(GOAPSensors.IS_UNDERWATER.key(), false),
            worldState.getOrDefault(HumanGOAPSensors.AIR_RATIO.key(), 1.0F),
            worldState.getOrDefault(HumanGOAPSensors.WATER_BREATHING_REMAINING_TICKS.key(), 0),
            ticks,
            itemStack.getUseDuration(livingEntity),
            0.0
        );
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
