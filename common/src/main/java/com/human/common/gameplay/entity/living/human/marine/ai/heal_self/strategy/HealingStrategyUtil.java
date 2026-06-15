package com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy;

import com.just.ai.goap.action.Action;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HealingStrategyUtil {

    /**
     * Health threshold below which healing becomes urgent (50% health).
     */
    public static final float HEAL_THRESHOLD = 0.5f;

    /**
     * Maximum healing value used for normalization (10 hearts = 20 HP).
     */
    private static final float MAX_HEALING_NORMALIZATION = 20.0f;

    /**
     * Calculates urgency based on current health ratio. Lower health = higher urgency.
     */
    public static double urgencyTerm(float healthRatio) {
        // 0 health = 1.0 urgency, full health = 0.0 urgency
        return clamp01(1.0 - healthRatio);
    }

    /**
     * Calculates healing value term normalized to [0, 1].
     */
    public static double healingTerm(float healingAmount) {
        return clamp01(healingAmount / MAX_HEALING_NORMALIZATION);
    }

    /**
     * Calculates overheal waste penalty.
     */
    public static double overhealWasteTerm(float currentHealth, float maxHealth, float healingAmount) {
        var missingHealth = maxHealth - currentHealth;

        if (healingAmount <= missingHealth) {
            return 0.0;
        }

        var wasted = healingAmount - missingHealth;
        return clamp01(wasted / healingAmount);
    }

    /**
     * Calculates time penalty for item use duration.
     */
    public static double timePenalty(int useTicks) {
        // Normalize against 32-tick baseline.
        return clamp01((useTicks - 16) / 32.0);
    }

    /**
     * Gets healing amount from instant health potion effects.
     */
    public static float getInstantHealthHealing(ItemStack itemStack) {
        var effect = getMobEffectInstanceOrNull(itemStack, MobEffects.HEAL);

        if (effect == null) {
            return 0.0f;
        }

        // Instant Health heals 4 HP (2 hearts) per level
        var amplifier = effect.getAmplifier() + 1;
        return 4.0f * amplifier;
    }

    /**
     * Gets regeneration ticks from potion effects.
     */
    public static int getRegenerationTicks(ItemStack itemStack) {
        var effect = getMobEffectInstanceOrNull(itemStack, MobEffects.REGENERATION);
        return effect == null ? 0 : effect.getDuration();
    }

    /**
     * Estimates healing from regeneration effect. Regen heals 1 HP every 50/25/12 ticks for level I/II/III.
     */
    public static float estimateRegenHealing(ItemStack itemStack) {
        var effect = getMobEffectInstanceOrNull(itemStack, MobEffects.REGENERATION);

        if (effect == null) {
            return 0.0f;
        }

        var duration = effect.getDuration();
        var amplifier = effect.getAmplifier();

        // Ticks between each heal: 50 >> amplifier (50, 25, 12, 6...)
        var ticksPerHeal = Math.max(1, 50 >> amplifier);
        var healCount = duration / ticksPerHeal;

        return healCount;
    }

    /**
     * Gets healing from food saturation. Higher saturation = more healing potential. Returns saturation as healing
     * equivalent (saturation * 2 as rough healing value).
     */
    public static float getFoodHealing(ItemStack itemStack) {
        var foodProps = itemStack.get(DataComponents.FOOD);

        if (foodProps == null) {
            return 0.0f;
        }

        // Nutrition provides direct food bar restoration
        // Saturation provides passive healing when food bar is full
        // Use a weighted combination for scoring
        return foodProps.nutrition() * 0.5f + foodProps.saturation() * 2.0f;
    }

    /**
     * Gets the nutrition value from food.
     */
    public static int getFoodNutrition(ItemStack itemStack) {
        var foodProps = itemStack.get(DataComponents.FOOD);
        return foodProps == null ? 0 : foodProps.nutrition();
    }

    /**
     * Gets the saturation value from food.
     */
    public static float getFoodSaturation(ItemStack itemStack) {
        var foodProps = itemStack.get(DataComponents.FOOD);
        return foodProps == null ? 0.0f : foodProps.saturation();
    }

    /**
     * Checks if an item is a food item.
     */
    public static boolean isFood(ItemStack itemStack) {
        return itemStack.has(DataComponents.FOOD);
    }

    /**
     * Checks if the item has the specified mob effect.
     */
    public static boolean hasMobEffect(ItemStack itemStack, Holder<MobEffect> effectHolder) {
        return getMobEffectInstanceOrNull(itemStack, effectHolder) != null;
    }

    /**
     * Gets food possible effects (for checking side effects like poison).
     */
    public static @Nullable FoodProperties getFoodProperties(ItemStack itemStack) {
        return itemStack.get(DataComponents.FOOD);
    }

    public static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static @Nullable MobEffectInstance getMobEffectInstanceOrNull(ItemStack itemStack, Holder<MobEffect> mobEffectHolder) {
        var potionContents = itemStack.get(DataComponents.POTION_CONTENTS);

        if (potionContents != null) {
            for (var mobEffectInstance : potionContents.getAllEffects()) {
                if (mobEffectInstance.getEffect() == mobEffectHolder) {
                    return mobEffectInstance;
                }
            }
        }

        return null;
    }

    public static Action.@NotNull Signal eat(LivingEntity livingEntity) {
        var itemStack = livingEntity.getMainHandItem();
        var foodProperties = itemStack.get(DataComponents.FOOD);

        if (foodProperties != null) {
            livingEntity.eat(livingEntity.level(), itemStack);
            livingEntity.heal(foodProperties.nutrition());
        }

        return Action.Signal.CONTINUE;
    }
}
