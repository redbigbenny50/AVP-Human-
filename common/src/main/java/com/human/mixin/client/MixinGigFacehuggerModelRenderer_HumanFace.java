package com.human.mixin.client;

import com.human.common.gameplay.entity.living.dog.MarineDog;
import com.human.common.gameplay.entity.living.human.AbstractHuman;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Oct 7 - GIGERESQUE FACEHUGGERS ON MARINES, COLONISTS AND THE MARINE DOG SAT ON TOP OF THEM.
 * <p>
 * Since the Gigeresque host tags (avp_combined__12) its facehuggers latch onto our humans and the marine dog, but
 * Gigeresque's renderer only moves a latched hugger onto the face for host types in its own head-data table (vanilla
 * mobs only, read from the 0.8.16 jar). For anything else it returns early and the hugger stays where riding puts it:
 * on top of the head, or on the dog's back (tester screenshot).
 * <p>
 * For our hosts this does the placement itself with Gigeresque's own face math and the head numbers avp_alien uses for
 * the same bodies: the player-shaped human model (head pivot 24 px - avp_human's HumanEntityHeadData / avp_alien's
 * player profile) and the vanilla wolf model the marine dog renders with (avp_alien's wolf profile). Numbers are
 * copied, not referenced, so it works with avp_alien absent. A Gigeresque hugger is a little taller than avp_alien's,
 * so it sits that much further out, exactly as avp_alien's own renderer does for larger huggers.
 * <p>
 * {@code @Pseudo}, {@code require = 0}: without Gigeresque, or with a version that renamed the method, nothing happens.
 * The typed method and its bridge both match by name; whichever runs first places the hugger and cancels.
 */
@Pseudo
@Mixin(targets = "mods.cybercat.gigeresque.client.entity.model.FacehuggerModelRenderer", remap = false)
public abstract class MixinGigFacehuggerModelRenderer_HumanFace {

    /** avp_alien's standard facehugger height; profiles are authored against it. */
    private static final double REFERENCE_HUGGER_HEIGHT = 0.25;

    /** Human head: pivot x/y/z, vertical offset, face offset - in blocks (pixels / 16). */
    private static final double[] HUMAN = { 0.0, 24.0 / 16.0, 0.0, -10.0 / 16.0, 4.0 / 16.0 };

    /** Vanilla wolf head (avp_alien wolf profile), used by the marine dog. */
    private static final double[] DOG = { 1.0 / 16.0, 10.5 / 16.0, -7.0 / 16.0, -9.1579 / 16.0, 11.6 / 16.0 };

    @Inject(method = "applyRotations", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void avp_human$placeOnFace(
        @Coerce Entity facehugger,
        PoseStack poseStack,
        float ageInTicks,
        float rotationYaw,
        float partialTick,
        float nativeScale,
        CallbackInfo ci
    ) {
        if (!facehugger.isPassenger() || !(facehugger.getVehicle() instanceof LivingEntity host)) {
            return;
        }

        double[] head;

        if (host instanceof AbstractHuman) {
            head = HUMAN;
        } else if (host instanceof MarineDog) {
            head = DOG;
        } else {
            return;
        }

        // Profiles scale with the host's real size (a baby or a scaled mob), as avp_alien's data profiles do.
        var scale = Mth.clamp((double) host.getScale(), 0.25, 4.0);
        var pivotX = head[0] * scale;
        var pivotY = head[1] * scale;
        var pivotZ = head[2] * scale;

        var bodyYaw = Mth.rotLerp(partialTick, host.yBodyRotO, host.yBodyRot);
        var headYaw = Mth.rotLerp(partialTick, host.yHeadRotO, host.yHeadRot) - bodyYaw;
        var headPitch = Mth.rotLerp(partialTick, host.getXRot(), host.xRotO);

        poseStack.mulPose(Axis.YN.rotationDegrees(bodyYaw));
        poseStack.translate(pivotX, pivotY - host.getBbHeight(), -pivotZ);
        poseStack.mulPose(Axis.YN.rotationDegrees(headYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(headPitch));
        poseStack.translate(-pivotX, -pivotY + host.getBbHeight(), pivotZ);
        poseStack.translate(
            0.0,
            head[3] * scale,
            head[4] * scale + facehugger.getBbHeight() - REFERENCE_HUGGER_HEIGHT
        );

        ci.cancel();
    }
}
