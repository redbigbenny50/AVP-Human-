package com.human.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Lets a gene sample play the target's own hurt sound without hurting it (see {@code GeneSamplePrick}).
 */
@Mixin(LivingEntity.class)
public interface MixinLivingEntity_HurtSoundInvoker {

    @Invoker("playHurtSound")
    void avp_human$playHurtSound(DamageSource source);
}
