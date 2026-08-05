package com.human.common.gameplay.effect;

import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The five stages of radiation sickness, in order of severity.
 * <p>
 * Radiation is NOT a fixed-duration debuff - it is a level driven by how long you have been standing in it. An entity
 * accumulates exposure while a source is present (a radioactive item in the inventory, an irradiated location, a hit
 * from a radioactive attack) and sheds it once clear. Each level is reached after {@code ESCALATION_TICKS} of
 * continuous exposure and lost after the slower {@code DECAY_TICKS} of being clean, so getting contaminated is faster
 * than getting clean.
 * </p>
 * <p>
 * The ladder is tuned against natural regeneration, which restores roughly 1 HP every 4 seconds for a well-fed player:
 * </p>
 * <ul>
 * <li>{@link #MILD} - well under the regeneration line. A warning; you can outlast it if you eat.</li>
 * <li>{@link #CONTAMINATION} - still beatable, but hunger makes you burn supplies to stay even.</li>
 * <li>{@link #SICKNESS} - damage exactly cancels natural regeneration. Healing stops. Leave.</li>
 * <li>{@link #ACUTE} - net loss. Roughly eighty seconds from full health.</li>
 * <li>{@link #FATAL} - roughly twenty seconds. Cure it or die.</li>
 * </ul>
 */
public enum RadiationLevel {

    NONE(0, 0.0F, 0),
    MILD(1, 1.0F, 10 * 20),
    CONTAMINATION(2, 1.0F, 6 * 20),
    SICKNESS(3, 1.0F, 4 * 20),
    ACUTE(4, 2.0F, 4 * 20),
    FATAL(5, 2.0F, 2 * 20);

    /** Exposure units that make up one level. */
    public static final int UNITS_PER_LEVEL = 3600;

    /** Exposure gained per tick while a baseline source is present: one level per 60 seconds. */
    public static final int GAIN_PER_TICK = 3;

    /** Exposure shed per tick while clean: one level per 90 seconds - recovery is slower than contamination. */
    public static final int DECAY_PER_TICK = 2;

    public static final int MAX_LEVEL = 5;

    /** Exposure at which {@link #FATAL} is reached; the counter never climbs past this. */
    public static final int MAX_EXPOSURE = UNITS_PER_LEVEL * (MAX_LEVEL - 1);

    private static final RadiationLevel[] BY_LEVEL = values();

    /** Side effects are refreshed well before they lapse, so they never flicker between ticks. */
    private static final int SIDE_EFFECT_DURATION_TICKS = 5 * 20;

    private final int level;

    private final float damage;

    private final int damageIntervalTicks;

    RadiationLevel(int level, float damage, int damageIntervalTicks) {
        this.level = level;
        this.damage = damage;
        this.damageIntervalTicks = damageIntervalTicks;
    }

    /**
     * The level for an exposure counter. ANY exposure at all is {@link #MILD} - contact is immediate - and each further
     * {@link #UNITS_PER_LEVEL} of accumulated exposure adds a level, to a ceiling of {@link #FATAL}.
     */
    public static RadiationLevel byExposure(int exposure) {
        if (exposure <= 0) {
            return NONE;
        }
        return BY_LEVEL[Mth.clamp(1 + exposure / UNITS_PER_LEVEL, 1, MAX_LEVEL)];
    }

    public int level() {
        return level;
    }

    /** The effect amplifier that displays this level (Radiation I is amplifier 0). */
    public int amplifier() {
        return Math.max(0, level - 1);
    }

    public float damage() {
        return damage;
    }

    public int damageIntervalTicks() {
        return damageIntervalTicks;
    }

    public boolean isHarmful() {
        return level > 0 && damageIntervalTicks > 0;
    }

    /**
     * Applies this level's accompanying afflictions. Deliberately escalates in KIND, not just in numbers: hunger
     * arrives first (you burn supplies), then weakness (you cannot fight your way out), then mining fatigue (you cannot
     * work), and finally the loss of natural healing at {@link #FATAL}.
     */
    public void applySideEffects(LivingEntity entity) {
        switch (this) {
            case NONE, MILD -> {
                // A warning only. Nothing punishing lives here on purpose - picking up a hot rock should not
                // immediately tax the player.
            }
            case CONTAMINATION -> refresh(entity, MobEffects.HUNGER, 0);
            case SICKNESS -> {
                refresh(entity, MobEffects.HUNGER, 0);
                refresh(entity, MobEffects.WEAKNESS, 0);
            }
            case ACUTE -> {
                refresh(entity, MobEffects.HUNGER, 1);
                refresh(entity, MobEffects.WEAKNESS, 0);
                refresh(entity, MobEffects.DIG_SLOWDOWN, 0);
            }
            case FATAL -> {
                refresh(entity, MobEffects.HUNGER, 1);
                refresh(entity, MobEffects.WEAKNESS, 1);
                refresh(entity, MobEffects.DIG_SLOWDOWN, 1);

                // The body stops repairing itself. Saturation is held at zero so no fast regeneration can tick.
                if (entity instanceof Player player) {
                    player.getFoodData().setSaturation(0.0F);
                }
            }
        }
    }

    /**
     * Applies an affliction, or upgrades one that is already present at a weaker amplifier. Never DOWNGRADES an effect
     * the entity got from somewhere else, and never fights a stronger dose from another source.
     */
    private static void refresh(LivingEntity entity, Holder<MobEffect> effect, int amplifier) {
        var current = entity.getEffect(effect);

        if (current != null && current.getAmplifier() > amplifier) {
            return;
        }

        if (current == null || current.getAmplifier() < amplifier || current.getDuration() < SIDE_EFFECT_DURATION_TICKS / 2) {
            entity.addEffect(new MobEffectInstance(effect, SIDE_EFFECT_DURATION_TICKS, amplifier, true, false, true));
        }
    }
}
