package com.human.mixin;

import com.human.common.gameplay.gene.MobIntelligence;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [stated] Nov 1: the Intelligence gene raises the experience a MOB drops (see {@link MobIntelligence}).
 * <p>
 * {@code getExperienceReward} is the one number {@code dropExperience} awards as orbs - after the mob's base reward and
 * enchantment effects - and NeoForge's experience-drop event starts from it too, so one hook covers both loaders.
 * </p>
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity_IntelligenceExperience {

    @Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
    private void avp_human$intelligenceExperience(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> callbackInfo) {
        int amount = callbackInfo.getReturnValue();
        var raised = MobIntelligence.scaleDrop((LivingEntity) (Object) this, amount);
        if (raised != amount) {
            callbackInfo.setReturnValue(raised);
        }
    }
}
