package com.human.mixin.client;

import com.human.common.registry.init.HumanGeneSync;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Echolocation (advanced, players): creatures within {@value #RANGE} blocks are outlined through walls - only on the
 * carrier's own screen (vanilla's glowing outline, without the Glowing effect anyone else would see).
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft_GeneEcholocation {

    @Unique
    private static final double RANGE = 16.0;

    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void avp_human$echolocation(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || !(entity instanceof LivingEntity) || !entity.isAlive()) {
            return;
        }

        var player = ((Minecraft) (Object) this).player;

        if (
            player != null && entity != player && entity.distanceToSqr(player) <= RANGE * RANGE
                && HumanGeneSync.echolocation(HumanGeneSync.flags(player))
        ) {
            cir.setReturnValue(true);
        }
    }
}
