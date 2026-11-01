package com.human.mixin.client;

import com.human.client.CosmeticFabricColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Joining a world: the dyed cosmetic fabric colour goes in now if it could not when the colour table was built (the
 * blocks were not registered yet - see {@link CosmeticFabricColors}). Every block is registered by now, and no chunk
 * has been meshed yet, so the tint is in place for the first frame. Does nothing once registered.
 */
@Mixin(ClientLevel.class)
public class MixinClientLevel_CosmeticFabric {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void avp_human$registerCosmeticFabric(CallbackInfo callbackInfo) {
        CosmeticFabricColors.register(Minecraft.getInstance().getBlockColors());
    }
}
