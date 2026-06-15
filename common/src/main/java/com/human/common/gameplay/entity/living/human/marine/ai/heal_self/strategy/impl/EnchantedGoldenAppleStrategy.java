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
 * Strategy for using enchanted golden apples. Enchanted golden apples provide powerful healing and defensive effects.
 */
public class EnchantedGoldenAppleStrategy implements HealingStrategy {

    // Estimate effective healing from enchanted golden apple.
    // Regen V for 30 seconds = ~30 HP, Absorption IV for 2 minutes = 16 absorption hearts.
    private static final float EFFECTIVE_HEALING = 30.0f;

    @Override
    public boolean isValidItemStack(ItemStack itemStack) {
        return itemStack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    @Override
    public Collection<BLibInventory.Entry> selectEntriesFromInventory(BLibInventory inventory) {
        return inventory.selectEntries(Items.ENCHANTED_GOLDEN_APPLE);
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

        // Side-benefit is significant due to fire resistance and resistance effects.
        var sideBenefit = HealingStrategyUtil.clamp01((1.0 - healthRatio) * 0.8);

        // Apply significant rarity penalty unless highly urgent (enchanted golden apples are rare).
        return weights.urgency() * U
            + weights.healingAmount() * H
            + weights.sideBenefit() * sideBenefit
            - weights.useTimePenalty() * use
            - weights.wasteOverhealPenalty() * waste
            - weights.rarityPenalty() * (1.0 - U);
    }

    @Override
    public Action.Signal execute(Action.Context<? extends LivingEntity> context) {
        var livingEntity = context.getActor();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        return ConsumeItemAction.perform(SoundEvents.GENERIC_EAT, livingEntity, blackboard, () -> HealingStrategyUtil.eat(livingEntity));
    }
}
