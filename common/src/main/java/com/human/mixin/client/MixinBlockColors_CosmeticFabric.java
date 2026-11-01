package com.human.mixin.client;

import com.human.common.gameplay.cosmetic.CosmeticBlock;
import com.human.common.registry.init.HumanCosmetics;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

/**
 * Dyed cosmetic fabric ([stated] Oct 31: only cushions and fabrics take dye). The fabric faces of a dyeable piece's
 * baked model carry tint index 0 over a greyscale texture, so the colour returned here IS the colour the player sees:
 * the dye's, or the faction's own fabric colour while undyed.
 * <p>
 * Registers a REAL colour provider when vanilla builds its colour table ({@code BlockColors.createDefault}) - BLib has
 * no block-colour registration. A provider in the table is what Sodium and Embeddium read: they look providers up
 * directly instead of calling {@code getColor}, so post-processing that call (as the nuked-foliage tint does) would
 * leave dyed fabric grey under them. Vanilla's own registration is equally a plain {@code register} on this table.
 * </p>
 */
@Mixin(BlockColors.class)
public class MixinBlockColors_CosmeticFabric {

    @Inject(method = "createDefault", at = @At("RETURN"))
    private static void avp_human$registerCosmeticFabric(CallbackInfoReturnable<BlockColors> callbackInfo) {
        var dyeable = new ArrayList<Block>();

        for (var holder : HumanCosmetics.DECOR_BLOCKS.values()) {
            if (holder.get() instanceof CosmeticBlock block && CosmeticBlock.dyeable(block.design())) {
                dyeable.add(block);
            }
        }

        if (dyeable.isEmpty()) {
            return;
        }

        callbackInfo.getReturnValue()
            .register(
                (state, level, pos, tintIndex) -> tintIndex == 0 && state.getBlock() instanceof CosmeticBlock block ? block.fabricTint(state) : -1,
                dyeable.toArray(Block[]::new)
            );
    }
}
