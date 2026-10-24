package com.human.neoforge.mixin.client;

import com.human.client.render.OversizedRender;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge culls even "render off screen" block entities by their renderer's bounding box, one block by default. For
 * our oversized renderers ({@link OversizedRender}) test their real size instead, so a quarry's drill, a fabricator or
 * a big crate stays drawn while any of it is in view.
 */
@Mixin(ClientHooks.class)
public class MixinClientHooks_OversizedRenders {

    @Inject(method = "isBlockEntityRendererVisible", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("unchecked")
    private static void avp_human$oversizedRenders(
        BlockEntityRenderDispatcher dispatcher,
        BlockEntity blockEntity,
        Frustum frustum,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (dispatcher.getRenderer(blockEntity) instanceof OversizedRender<?> oversized) {
            callback.setReturnValue(frustum.isVisible(((OversizedRender<BlockEntity>) oversized).renderBox(blockEntity)));
        }
    }
}
