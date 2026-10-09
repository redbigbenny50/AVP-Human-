package com.human.mixin.client;

import com.blib.api.client.render.v1.AzRendererConfig;
import com.human.client.render.cryo.CryoIcon;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A CRYOLINK icon copy is drawn with the flat-green twin of its own texture (see {@link CryoIcon#flatFor}): the texture
 * the config would have used is swapped on the way out, and the render type is built from that twin. Every other entity
 * is untouched.
 */
@Mixin(value = AzRendererConfig.class, remap = false)
public abstract class MixinAzRendererConfig_CryoIcon {

    @Inject(
        method = "textureLocation(Lnet/minecraft/world/entity/Entity;Ljava/lang/Object;)Lnet/minecraft/resources/ResourceLocation;",
        at = @At("RETURN"), cancellable = true
    )
    private void avp_human$iconTexture(Entity entity, Object animatable, CallbackInfoReturnable<ResourceLocation> callback) {
        if (avp_human$isIcon(entity, animatable)) {
            callback.setReturnValue(CryoIcon.flatFor(callback.getReturnValue()));
        }
    }

    @Inject(
        method = "getRenderType(Lnet/minecraft/world/entity/Entity;Ljava/lang/Object;)Lnet/minecraft/client/renderer/RenderType;", at = @At(
            "HEAD"
        ), cancellable = true
    )
    @SuppressWarnings("unchecked")
    private void avp_human$iconRenderType(Entity entity, Object animatable, CallbackInfoReturnable<RenderType> callback) {
        if (avp_human$isIcon(entity, animatable)) {
            // textureLocation is itself swapped above, so this is already the flat twin.
            var texture = ((AzRendererConfig<Object, Object>) (Object) this).textureLocation(entity, animatable);
            callback.setReturnValue(RenderType.entityCutoutNoCull(texture));
        }
    }

    /**
     * [stated] Oct 3: a frozen marine's portrait showed only its eyes. For the humans BLib passes the ENTITY as null
     * and the marine as the ANIMATABLE, so a check on the entity alone never saw the icon: the texture was not
     * flattened and only human/base's own four eye pixels drew. The layer hook already looks at the animatable; this
     * matches it.
     */
    private static boolean avp_human$isIcon(Entity entity, Object animatable) {
        var target = entity != null ? entity : animatable instanceof Entity animatableEntity ? animatableEntity : null;
        return target != null && CryoIcon.isIcon(target);
    }
}
