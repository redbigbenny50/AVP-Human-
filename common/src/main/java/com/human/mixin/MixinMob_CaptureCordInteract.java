package com.human.mixin;

import com.human.common.gameplay.item.CaptureCordTether;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [stated] Oct 3: right-clicking the mob you hold on a capture cord lets it go and gives the cord back - what a lead
 * does, now that the cord is its own tether rather than vanilla's leash.
 */
@Mixin(Mob.class)
public abstract class MixinMob_CaptureCordInteract {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void avp_human$releaseCaptureCord(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callbackInfo) {
        var self = (Mob) (Object) this;

        if (!CaptureCordTether.isHeldBy(self, player)) {
            return;
        }

        if (!self.level().isClientSide) {
            CaptureCordTether.release(self, player);
        }

        callbackInfo.setReturnValue(InteractionResult.sidedSuccess(self.level().isClientSide));
    }
}
