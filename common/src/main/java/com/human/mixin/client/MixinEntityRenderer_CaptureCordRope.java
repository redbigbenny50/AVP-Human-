package com.human.mixin.client;

import com.human.client.render.cord.CaptureCordRopeRenderer;
import com.human.common.gameplay.item.CaptureCordTether;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the braided capture cord from a held mob to whoever holds it. [stated] Oct 3 - the clean route: the cord is not
 * a vanilla leash any more, so there is no vanilla rope to cancel. This only ADDS a draw - nothing is suppressed -
 * which is why it is reliable where the old cancel was not.
 * <p>
 * {@code EntityRenderer.render} is where vanilla draws a leash, with the pose stack at the entity's origin; every
 * renderer reaches it (LivingEntityRenderer and BLib's Az renderer both call {@code super.render}).
 * </p>
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer_CaptureCordRope {

    @Inject(method = "render", at = @At("HEAD"))
    private void avp_human$drawCaptureCord(
        Entity entity,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        CallbackInfo callbackInfo
    ) {
        if (!(entity instanceof Mob mob)) {
            return;
        }

        var holderId = CaptureCordTether.holderId(mob);

        if (holderId == CaptureCordTether.NO_HOLDER) {
            return;
        }

        var holder = mob.level().getEntity(holderId);

        if (holder != null) {
            CaptureCordRopeRenderer.render(mob, partialTick, poseStack, bufferSource, holder);
        }
    }
}
