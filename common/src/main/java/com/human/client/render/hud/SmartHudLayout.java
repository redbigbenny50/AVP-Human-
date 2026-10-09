package com.human.client.render.hud;

import com.human.common.gameplay.item.gun.targeting.GunTargetingProfile;

/**
 * The frame's geometry, computed in ONE place so the drawing and the acquisition test can never disagree.
 * <p>
 * ⚠⚠ THIS EXISTS BECAUSE THE FRAME IS THE ACQUISITION RULE, NOT A PICTURE OF ONE. If the renderer clamped the frame to
 * the screen and the targeting scan did not, a target could sit visibly inside the drawn frame and refuse to acquire -
 * and that is the first bug report you would get. Both callers ask this class, so there is nothing to keep in sync.
 * </p>
 */
public record SmartHudLayout(
    int left,
    int top,
    int right,
    int bottom,
    int centerX,
    int centerY
) {

    public static SmartHudLayout of(GunTargetingProfile profile, int guiWidth, int guiHeight) {
        var centerX = (guiWidth - 1) / 2;
        var centerY = (guiHeight - 1) / 2;

        // Clamped to leave room for the side panels and scales; a frame running off the edge reads as a bug.
        var frameWidth = Math.min(profile.frameWidth(), Math.max(80, guiWidth - 170));
        var frameHeight = Math.min(profile.frameHeight(), Math.max(60, guiHeight - 90));

        return new SmartHudLayout(
            centerX - frameWidth / 2,
            centerY - frameHeight / 2,
            centerX + frameWidth / 2,
            centerY + frameHeight / 2,
            centerX,
            centerY
        );
    }

    public boolean contains(float x, float y) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    /** Distance from the reticle, in pixels - the score used to pick the most central contact. */
    public double distanceFromCenter(float x, float y) {
        var deltaX = x - centerX;
        var deltaY = y - centerY;

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
}
