package com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy.impl;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.human.common.gameplay.entity.ai.goap.HumanGOAPSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy.WBIStrategy;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy.WBIStrategyUtil;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.ReadableWorldState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Objects;

/**
 * Thrown straight down at the feet. Only valid on the ground: a splash bottle lobbed while treading water drifts and
 * bursts somewhere else, so this is for the marine standing on the bottom or on the shore about to go in.
 */
public class SplashPotionWBIStrategy implements WBIStrategy {

    private static final double HANDLING_PENALTY = 0.05;

    @Override
    public boolean isValidItemStack(ItemStack itemStack) {
        return itemStack.is(Items.SPLASH_POTION) && WBIStrategyUtil.waterBreathingTicksFromStack(itemStack) > 0;
    }

    @Override
    public boolean isValidWorldState(LivingEntity livingEntity, ReadableWorldState worldState) {
        return worldState.getOrDefault(GOAPSensors.IS_ON_GROUND.key(), false);
    }

    @Override
    public double score(LivingEntity livingEntity, ReadableWorldState worldState, ItemStack itemStack) {
        var ticks = (int) Math.round(WBIStrategyUtil.waterBreathingTicksFromStack(itemStack) * 0.75);

        if (ticks <= 0) {
            return Double.NEGATIVE_INFINITY;
        }

        return WBIStrategyUtil.score(
            worldState.getOrDefault(GOAPSensors.IS_UNDERWATER.key(), false),
            worldState.getOrDefault(HumanGOAPSensors.AIR_RATIO.key(), 1.0F),
            worldState.getOrDefault(HumanGOAPSensors.WATER_BREATHING_REMAINING_TICKS.key(), 0),
            ticks,
            itemStack.getUseDuration(livingEntity),
            HANDLING_PENALTY
        );
    }

    @Override
    public Action.Signal execute(Action.Context<? extends LivingEntity> context) {
        var livingEntity = context.getActor();
        var itemStack = livingEntity.getMainHandItem();

        if (!itemStack.is(Items.SPLASH_POTION)) {
            return Action.Signal.ABORT;
        }

        var potionContents = Objects.requireNonNull(itemStack.get(DataComponents.POTION_CONTENTS));
        var level = livingEntity.level();
        var thrownPotion = new ThrownPotion(level, livingEntity);
        thrownPotion.setItem(itemStack);
        thrownPotion.shoot(0.0, -1.0, 0.0, 0.5F, 1.0F);
        level.addFreshEntity(thrownPotion);
        potionContents.getAllEffects().forEach(livingEntity::addEffect);
        itemStack.shrink(1);

        return Action.Signal.CONTINUE;
    }
}
