package com.human.mixin;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A dropped FULL crate can carry thousands of items, so it must never be lost: it never despawns and nothing can hurt
 * it - explosions, fire, lava, cactus, acid. (Fire and lava are also covered by the fire-resistant component the full
 * crate carries.) Only damage that bypasses invulnerability, such as /kill or the void, still applies.
 */
@Mixin(ItemEntity.class)
public abstract class MixinItemEntity_CrateProtection {

    @Inject(at = @At("HEAD"), method = "tick")
    private void avp_human$keepFullCrate(CallbackInfo callbackInfo) {
        var self = ItemEntity.class.cast(this);

        if (com.human.common.gameplay.storage.StoredContents.isProtected(self.getItem())) {
            self.setUnlimitedLifetime();
        }
    }

    @Inject(at = @At("HEAD"), method = "hurt", cancellable = true)
    private void avp_human$protectFullCrate(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callbackInfo) {
        var self = ItemEntity.class.cast(this);

        if (
            com.human.common.gameplay.storage.StoredContents.isProtected(self.getItem()) && !source.is(
                DamageTypeTags.BYPASSES_INVULNERABILITY
            )
        ) {
            callbackInfo.setReturnValue(false);
        }
    }
}
