package com.human.mixin.client;

import com.human.client.render.cryo.FrostOverlayLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives every vanilla-model mob the frost layer, which only ever draws on a cryotank display copy. */
@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer_CryoFrost<T extends LivingEntity, M extends EntityModel<T>> {

    @Shadow
    protected abstract boolean addLayer(RenderLayer<T, M> layer);

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void avp_human$addFrostOverlay(
        EntityRendererProvider.Context context,
        M model,
        float shadowRadius,
        CallbackInfo callback
    ) {
        this.addLayer(new FrostOverlayLayer<>((RenderLayerParent<T, M>) this));
    }
}
