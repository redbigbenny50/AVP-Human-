package com.human.mixin;

import com.human.common.gameplay.cosmetic.CosmeticBunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bunk beds as beds, for players and NPCs alike ([stated] Oct 31). Vanilla asks a sleeper's bed four questions through
 * LivingEntity, all of them answered only for a BedBlock; a bunk is a cosmetic multiblock, so each is answered here:
 * <ul>
 * <li>{@code checkBedExists} - is the bed still there (else the sleeper is woken every tick);</li>
 * <li>{@code getBedOrientation} - which way the sleeper lies (head at the pillow);</li>
 * <li>{@code setPosToBed} - how high the sleeper lies (on the right mattress);</li>
 * <li>{@code stopSleeping} - where they stand up (in front of the bunk, not inside it).</li>
 * </ul>
 * Only bunks are touched; every other bed goes the vanilla (or NeoForge) way. All four are hooked at their start or
 * end, which NeoForge's own patches to these methods leave in place. Method names checked against the 1.21.1 jar.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity_CosmeticBunk {

    @Unique
    private Vec3 avp_human$bunkStandUp;

    @Inject(method = "checkBedExists", at = @At("HEAD"), cancellable = true)
    private void avp_human$bunkExists(CallbackInfoReturnable<Boolean> callbackInfo) {
        var self = (LivingEntity) (Object) this;
        var pos = self.getSleepingPos();

        if (pos.isPresent() && CosmeticBunks.isBunkBed(self.level(), pos.get())) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method = "getBedOrientation", at = @At("HEAD"), cancellable = true)
    private void avp_human$bunkOrientation(CallbackInfoReturnable<Direction> callbackInfo) {
        var self = (LivingEntity) (Object) this;
        var pos = self.getSleepingPos();

        if (pos.isPresent()) {
            var direction = CosmeticBunks.orientation(self.level(), pos.get());

            if (direction != null) {
                callbackInfo.setReturnValue(direction);
            }
        }
    }

    @Inject(method = "setPosToBed", at = @At("TAIL"))
    private void avp_human$bunkHeight(BlockPos pos, CallbackInfo callbackInfo) {
        var self = (LivingEntity) (Object) this;

        if (CosmeticBunks.isBunkBed(self.level(), pos)) {
            self.setPos(self.getX(), CosmeticBunks.lyingHeight(self.level(), pos), self.getZ());
        }
    }

    @Inject(method = "stopSleeping", at = @At("HEAD"))
    private void avp_human$bunkWakeStart(CallbackInfo callbackInfo) {
        var self = (LivingEntity) (Object) this;
        avp_human$bunkStandUp = self.getSleepingPos().map(pos -> CosmeticBunks.standUp(self.level(), pos, self)).orElse(null);
    }

    @Inject(method = "stopSleeping", at = @At("TAIL"))
    private void avp_human$bunkWakeEnd(CallbackInfo callbackInfo) {
        if (avp_human$bunkStandUp != null) {
            var self = (LivingEntity) (Object) this;
            self.setPos(avp_human$bunkStandUp);
            avp_human$bunkStandUp = null;
        }
    }
}
