package com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy;

import com.human.common.gameplay.entity.ai.utility.item.ItemStrategy;
import com.just.ai.goap.action.Action;
import net.minecraft.world.entity.LivingEntity;

/**
 * A way of getting Water Breathing onto a marine - the water counterpart of {@code FRIStrategy}, and the same shape so
 * the utility sensors and the pick-up / equip / use actions work unchanged. Scoring reuses the fire-resistance terms
 * with "underwater and short of air" standing in for "on fire and low on health".
 */
public interface WBIStrategy extends ItemStrategy {

    Action.Signal execute(Action.Context<? extends LivingEntity> context);
}
