package com.human.mixin;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Oct 5 - [stated] "the marines do die way to easy to the pred can we increase damage vs predators for them maybe like
 * 200% damage vs preds and 50% more towards aliens."
 * <p>
 * Everything a marine deals - bullets, rockets, grenades, its knife - lands at {@value Marine#DAMAGE_VS_YAUTJA}x on a
 * yautja and {@value Marine#DAMAGE_VS_XENOS}x on any avp_alien creature. Applied at the top of
 * {@code LivingEntity.hurt}, before armour, toughness and the alien caliber rules, so those still scale the bigger hit
 * the same way they scaled the old one. The victim is recognised by registry id, so neither sibling mod has to be
 * installed. Same pattern as avp_predator's {@code MixinPlayer_YautjaDamageToPlayers}.
 * </p>
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity_MarineDamageBonus {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float avp_human$marineDamageBonus(float amount, DamageSource source, float original) {
        if (!(source.getEntity() instanceof Marine)) {
            return amount;
        }

        var key = BuiltInRegistries.ENTITY_TYPE.getKey(((LivingEntity) (Object) this).getType());
        var namespace = key.getNamespace();

        if ("avp_predator".equals(namespace) && key.getPath().contains("yautja")) {
            return amount * Marine.DAMAGE_VS_YAUTJA;
        }

        if ("avp_alien".equals(namespace)) {
            return amount * Marine.DAMAGE_VS_XENOS;
        }

        return amount;
    }
}
