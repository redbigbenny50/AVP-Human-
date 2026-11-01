package com.human.mixin;

import com.human.common.gameplay.cosmetic.CosmeticBunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bunk beds ([stated] Oct 31): vanilla refuses a bed with a solid block over it - and the bottom bunk has the top bunk
 * over it by design. The bunk is never "obstructed"; every other bed keeps vanilla's check. (Respawning needs no hook:
 * sleeping in a bunk sets a FORCED respawn point on the floor in front of it, vanilla's own /spawnpoint route.)
 */
@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer_CosmeticBunk {

    @Inject(method = "bedBlocked", at = @At("HEAD"), cancellable = true)
    private void avp_human$bunkNeverBlocked(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (CosmeticBunks.isBunkBed(((ServerPlayer) (Object) this).level(), pos)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
