package com.human.mixin.client;

import com.blib.api.client.posteffect.v1.BLibPostEffectFramework;
import com.human.client.render.TintedMultiBufferSource;
import com.human.client.targeting.SmartTargetingClient;
import com.human.common.gameplay.item.gun.targeting.SmartTargeting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Swaps in a tinting buffer source for the smart tracker's locked target.
 * <p>
 * ⚠⚠ AT THE DISPATCHER, NOT AT A RENDERER. Every entity renderer receives its buffer source from here, so one hook
 * covers vanilla mobs and BLib's azurelib entities alike. Hooking {@code LivingEntityRenderer} instead would have
 * missed every xenomorph, because {@code AzEntityRenderer} extends {@code EntityRenderer} directly.
 * </p>
 * <p>
 * ⚠ CLIENT-SIDE AND VIEWER-SPECIFIC. Nothing is written to the entity and nothing is synced - this colours one creature
 * in one player's eyes, the same principle avp_alien's xeno vision uses. Another player in the same room sees an
 * ordinary xenomorph.
 * </p>
 * <p>
 * ⚠ THE GUN IS RE-CHECKED HERE, NOT ASSUMED FROM THE TARGET. A stale target could otherwise keep glowing for a frame
 * after the weapon is put away, since the targeting scan runs on the game tick and this runs per frame.
 * </p>
 */
@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher_SmartHighlight {

    /**
     * ⭐ THE MARK IS 50% WHITE FROM THE OVERLAY'S NON-HURT ROW.
     * <p>
     * The overlay's white rows fade with u - {@code (1 - u/15 * 0.75)} - so coverage runs 0% at u=0 to 75% at u=15. u=4
     * is 20%, walked down in play from 75 -> 60 -> 45 -> 35 -> 20. Just enough to lift a near-black xenomorph off the
     * ground; its own colour and shape carry the rest, and the brackets and lock circle do the marking.
     * <p>
     * ⚠ EACH STEP OF THE FIRST NUMBER IS 5% WHITE, and the ramp tops out at u=15 (75%). There is no finer control than
     * 5% - the overlay is a 16-pixel-wide texture, so u is an index into it, not a continuous value.
     * </p>
     * </p>
     * <p>
     * ⚠⚠ THE OUTLINE USED TO SWALLOW THIS. When an entity glows, LevelRenderer swaps the buffer source for an
     * OutlineBufferSource (line 1027), whose EntityOutlineGenerator OVERRIDES setUv1 and setUv2 to do NOTHING - overlay
     * and lightmap are discarded by design, because an outline is a flat silhouette. Wrapping that source meant the
     * mark was being written into a consumer that throws it away. With the outline gone, the buffer is the ordinary one
     * again and the overlay lands.
     * </p>
     * <p>
     * ⚠ THE BODY IS DELIBERATELY NEUTRAL NOW. The colour comes from the OUTLINE, which owes nothing to the texture and
     * can therefore be any hue at all - so the body does not need to carry the colour and should not fight it. A red
     * body against a green outline read as two unrelated effects.
     * </p>
     */
    private static final int MARK_OVERLAY = OverlayTexture.pack(4, OverlayTexture.v(false));

    /**
     * ⭐ HIT FEEDBACK IS THE VANILLA RED FLASH AGAIN. Marking with white frees the red rows, so a hit shows exactly what
     * a hit has always shown - which is one less thing for a player to learn.
     */
    private static final int HURT_OVERLAY = OverlayTexture.pack(0, OverlayTexture.RED_OVERLAY_V);

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private MultiBufferSource avp_human$tintLockedTarget(MultiBufferSource bufferSource, Entity entity) {
        // ⚠⚠ NEVER TINT DURING A CLASSIFICATION PASS. The predator's vision modes re-render the world into a
        // classification buffer to decide what each surface IS - heat tier, background, and so on - and a colour
        // effect layered on top of that pass corrupts what the vision reads back. A highlight is a thing the PLAYER
        // sees; it has no business in a pass whose whole job is to describe the scene to a shader.
        if (BLibPostEffectFramework.isClassificationPassActive()) {
            return bufferSource;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;

        if (player == null || !(entity instanceof LivingEntity target) || entity != SmartTargetingClient.target()) {
            return bufferSource;
        }

        var profile = SmartTargeting.resolve(player.getMainHandItem());

        // ⚠⚠ DO NOT GATE THIS ON highlightStrength. It used to read
        // `profile == null || profile.highlightStrength() <= 0.0F`, and the moment the vertex TINT was turned off -
        // strength 0, because the overlay does the marking now - that guard switched off the ENTIRE highlight:
        // overlay, brightness, hit flash, everything. The wrapper was never installed at all.
        // ⚠ STRENGTH IS ONE INGREDIENT, NOT AN ON SWITCH. A strength of zero is a legitimate setting meaning "do not
        // multiply the body colour"; it says nothing about whether the target should be marked. The profile existing
        // is what decides that.
        if (profile == null) {
            return bufferSource;
        }

        // ⚠ ONLY THE LOCKED TARGET IS TINTED, not every boxed contact. Colouring the whole crowd removes the very
        // distinction the lock exists to make, and the brackets already mark the others.
        if (!SmartTargetingClient.isLocked()) {
            return bufferSource;
        }

        return new TintedMultiBufferSource(
            bufferSource,
            profile.highlightColor(),
            profile.highlightStrength(),
            overlayFor(target)
        );
    }

    /**
     * Red normally; white for the moment after a hit.
     * <p>
     * ⚠ {@code hurtTime} IS ALREADY ON THE CLIENT - it counts down from 10 and is what drives vanilla's own red flash,
     * so this needs no packet and no state of its own.
     * </p>
     * <p>
     * ⚠ A HARD SWAP, NOT A FADE. The two rows are different colours rather than different strengths of one, so there is
     * nothing meaningful to interpolate between - and at twenty rounds a second a white blink on a red body is legible
     * in a way a gradient would not be.
     * </p>
     */
    private static int overlayFor(LivingEntity target) {
        return target.hurtTime > 0 ? HURT_OVERLAY : MARK_OVERLAY;
    }
}
