package com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.impl;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.inventory.v1.BLibInventory;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategy;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategyUtil;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.ReadableWorldState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collection;
import java.util.Objects;

/**
 * Strategy for using splash potions of regeneration.
 */
public class SplashRegenPotionStrategy implements HealingStrategy {

    @Override
    public boolean isValidItemStack(ItemStack itemStack) {
        return itemStack.is(Items.SPLASH_POTION) && HealingStrategyUtil.hasMobEffect(itemStack, MobEffects.REGENERATION);
    }

    @Override
    public Collection<BLibInventory.Entry> selectEntriesFromInventory(BLibInventory inventory) {
        return inventory.selectEntries(Items.SPLASH_POTION);
    }

    @Override
    public boolean isValidWorldState(LivingEntity livingEntity, ReadableWorldState worldState) {
        var healthRatio = worldState.getOrDefault(GOAPSensors.HEALTH_RATIO.key(), 1.0F);
        var isOnGround = worldState.getOrDefault(GOAPSensors.IS_ON_GROUND.key(), false);
        return healthRatio < HealingStrategyUtil.HEAL_THRESHOLD && isOnGround;
    }

    @Override
    public double score(LivingEntity livingEntity, ReadableWorldState worldState, ItemStack itemStack) {
        var healthRatio = worldState.getOrDefault(GOAPSensors.HEALTH_RATIO.key(), 1.0F);
        // Splash potions are 75% effective when thrown at self.
        var healing = HealingStrategyUtil.estimateRegenHealing(itemStack) * 0.75f;
        var weights = HealingStrategy.Weights.DEFAULT;

        if (healing <= 0) {
            return Double.NEGATIVE_INFINITY;
        }

        var U = HealingStrategyUtil.urgencyTerm(healthRatio);
        var H = HealingStrategyUtil.healingTerm(healing);
        var waste = HealingStrategyUtil.overhealWasteTerm(
            livingEntity.getHealth(),
            livingEntity.getMaxHealth(),
            healing
        );

        // Slight handling penalty (aiming, spread).
        var handlingPenalty = 0.05;
        // Regen takes time to heal, so slightly penalize for urgent situations.
        var regenDelayPenalty = 0.1 * U;

        return weights.urgency() * U
            + weights.healingAmount() * H
            - weights.wasteOverhealPenalty() * waste
            - handlingPenalty
            - regenDelayPenalty;
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
