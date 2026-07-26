package com.human.common.gameplay.effect;

import com.blib.api.common.color.v1.Color;
import com.human.common.registry.key.HumanDamageTypeKeys;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.concurrent.TimeUnit;

/**
 * Radiation status effect - a DISPLAY of accumulated exposure, not the thing that drives it.
 *
 * <p>The amplifier mirrors the entity's {@link RadiationLevel} (amplifier 0 = Radiation I). All of the behaviour -
 * accumulating exposure while a source is present, decaying it once clear, and applying each level's damage and
 * afflictions - lives in {@code MixinLivingEntity_RadiationDamage} and {@link RadiationLevel}.</p>
 *
 * <p>The duration constants below are what INSTANT sources hand out; sustained sources instead mark exposure every
 * tick they are present.</p>
 */
public class RadiationStatusEffect extends MobEffect {

    public static final int SHORT_EFFECT_DURATION_IN_TICKS = (int) TimeUnit.MINUTES.toSeconds(1) * 20;

    public static final int MEDIUM_EFFECT_DURATION_IN_TICKS = (int) (TimeUnit.MINUTES.toSeconds(2) + 30) * 20;

    public static final int LONG_EFFECT_DURATION_IN_TICKS = (int) TimeUnit.MINUTES.toSeconds(5) * 20;

    public static final float INCUBATION_RATIO = 0.20f;

    public static final float PEAK_DAMAGE_RATIO = 0.80f;

    public static final float BASE_DAMAGE = 1.0f;

    public static final float DAMAGE_PER_AMPLIFIER = 0.5f;

    public static final int MAX_DAMAGE_INTERVAL_TICKS = 4 * 20;

    public static final int MIN_DAMAGE_INTERVAL_TICKS = 20;

    public RadiationStatusEffect() {
        super(MobEffectCategory.HARMFUL, Color.GREEN.getColor());
    }

    public static DamageSource createRadiationDamageSource(LivingEntity entity) {
        return new DamageSource(
            entity.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(HumanDamageTypeKeys.RADIATION)
        );
    }
}
