package com.human.mixin.client;

import com.human.client.render.cryo.CryoIcon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cryo specimen portraits for mobs drawn by GECKOLIB. [stated] Oct 3: other mods' mobs should get the flat green icon
 * too; vanilla-style and BLib renderers already did, GeckoLib's own entity renderer did not.
 * <p>
 * Checked against GeckoLib 4.9.3 (the one in BLib's library): its {@code defaultRender} gets the texture, calls
 * {@code getRenderType(animatable, texture, buffers, partialTick)}, draws, then {@code applyRenderLayers(...)}. An icon
 * gets the flat green twin of that texture and no layers.
 * </p>
 * <h2>Why it is built this way</h2>
 * <ul>
 * <li>{@code @Pseudo}: GeckoLib is optional. Without it this mixin is simply skipped.</li>
 * <li>{@code require = 0}: a different GeckoLib version that renamed a method loses the green icon, never crashes.</li>
 * <li>Methods targeted by NAME, GeckoLib types taken as {@code @Coerce Object}: both are GeckoLib's own methods, so
 * their names are the same on Fabric and NeoForge, and nothing here needs GeckoLib to compile.</li>
 * </ul>
 * ⚠ Per-BONE layers (held items, mostly) are left alone: switching those off would need a full vanilla-typed
 * descriptor, which differs between the loaders. At worst an icon shows a held item in its own colours.
 */
@Pseudo
@Mixin(targets = "software.bernie.geckolib.renderer.GeoEntityRenderer", remap = false)
public abstract class MixinGeoEntityRenderer_CryoIcon {

    /** The model's render type: an icon draws the flat green twin of its texture, cut out, like every other icon. */
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true, require = 0)
    private void avp_human$iconRenderType(
        @Coerce Object animatable,
        ResourceLocation texture,
        MultiBufferSource bufferSource,
        float partialTick,
        CallbackInfoReturnable<RenderType> callback
    ) {
        if (animatable instanceof Entity entity && CryoIcon.isIcon(entity)) {
            callback.setReturnValue(RenderType.entityCutoutNoCull(CryoIcon.flatFor(texture)));
        }
    }

    /** No render layers on an icon - a clean silhouette, as for vanilla and BLib mobs. */
    @Inject(method = "applyRenderLayers", at = @At("HEAD"), cancellable = true, require = 0)
    private void avp_human$noLayersOnIcons(
        PoseStack poseStack,
        @Coerce Object animatable,
        @Coerce Object model,
        RenderType renderType,
        MultiBufferSource bufferSource,
        VertexConsumer buffer,
        float partialTick,
        int packedLight,
        int packedOverlay,
        CallbackInfo callback
    ) {
        if (animatable instanceof Entity entity && CryoIcon.isIcon(entity)) {
            callback.cancel();
        }
    }
}
