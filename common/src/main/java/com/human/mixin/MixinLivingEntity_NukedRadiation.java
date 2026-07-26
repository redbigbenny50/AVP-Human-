package com.human.mixin;

import com.human.common.gameplay.effect.RadiationStatusEffect;
import com.human.common.registry.init.HumanMobEffects;
import com.human.common.registry.key.HumanBiomeKeys;
import com.human.util.HumanPredicates;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity_NukedRadiation extends Entity {

    public MixinLivingEntity_NukedRadiation(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo callbackInfo) {
        var self = LivingEntity.class.cast(this);

        if (!HumanPredicates.canBeIrradiated(self)) {
            return;
        }

        if (!isIsEntityInAnIrradiatedBiome(self)) {
            // Entity is not in an irradiated biome, can't possibly irradiate them. Abort.
            return;
        }

        // Apply radiation effect.
        self.addEffect(new MobEffectInstance(HumanMobEffects.getRadiationHolder(), RadiationStatusEffect.LONG_EFFECT_DURATION_IN_TICKS, 0));
    }

    @Unique
    private boolean isIsEntityInAnIrradiatedBiome(LivingEntity self) {
        return self.level().getBiome(self.blockPosition()).is(HumanBiomeKeys.NUKED_BIOME);
    }
}
