package com.human.mixin;

import com.human.common.gameplay.item.CaptureCordTether;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ticks a mob's capture cord on the server - the pull, the snap, the holder sync. [stated] Oct 3: the cord is its own
 * tether now, not vanilla's leash, so it ticks here rather than beside vanilla's leash tick.
 */
@Mixin(Entity.class)
public abstract class MixinEntity_CaptureCordTick {

    @Inject(method = "baseTick", at = @At("HEAD"))
    private void avp_human$tickCaptureCord(CallbackInfo callbackInfo) {
        // ⚡ One instanceof and one level check for every other entity; the tether's own first check is the cord tag.
        if ((Object) this instanceof Mob mob && !mob.level().isClientSide) {
            CaptureCordTether.tick(mob);
        }
    }
}
