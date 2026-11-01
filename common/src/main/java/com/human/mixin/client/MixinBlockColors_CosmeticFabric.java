package com.human.mixin.client;

import com.human.client.CosmeticFabricColors;
import net.minecraft.client.color.block.BlockColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Puts the dyed cosmetic fabric colour in vanilla's colour table as it is built - see {@link CosmeticFabricColors}.
 * <p>
 * ⚠ Nov 1: this used to resolve every cosmetic block holder right here, and on NeoForge they were not registered yet at
 * this point - the game crashed on startup. {@link CosmeticFabricColors#register} now resolves nothing unless every
 * holder is bound, and joining a world ({@link MixinClientLevel_CosmeticFabric}) registers it if this was too early.
 * </p>
 */
@Mixin(BlockColors.class)
public class MixinBlockColors_CosmeticFabric {

    @Inject(method = "createDefault", at = @At("RETURN"))
    private static void avp_human$registerCosmeticFabric(CallbackInfoReturnable<BlockColors> callbackInfo) {
        CosmeticFabricColors.register(callbackInfo.getReturnValue());
    }
}
