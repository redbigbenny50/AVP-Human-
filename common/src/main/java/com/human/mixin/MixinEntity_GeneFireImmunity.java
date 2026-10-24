package com.human.mixin;

import com.human.common.util.GeneImmunity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [stated] Oct 19 - a mob whose fire resistance gene is at 100% is fire immune, like a blaze: never set alight, no fire
 * or lava damage, infernium vapor passes it by. Vanilla asks {@code fireImmune()} for all of it, so the answer is given
 * here, for living things only.
 */
@Mixin(Entity.class)
public abstract class MixinEntity_GeneFireImmunity {

    @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
    private void avp_human$fullFireResistanceIsFireImmunity(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity living && GeneImmunity.fireImmune(living)) {
            cir.setReturnValue(true);
        }
    }
}
