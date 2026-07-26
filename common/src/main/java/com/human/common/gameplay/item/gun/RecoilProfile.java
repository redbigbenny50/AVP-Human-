package com.human.common.gameplay.item.gun;

/**
 * Per-fire-mode recoil and accuracy data. Values are deliberately expressed in camera degrees and degrees of cone
 * spread so they can be tuned independently from damage and fire rate.
 */
public record RecoilProfile(
    float verticalKick,
    float horizontalKick,
    float spreadPerShot,
    float maximumSpread,
    float recoveryPerTick,
    float aimedMultiplier,
    float[] horizontalPattern
) {

    public static RecoilProfile fromLegacy(float recoil) {
        var kick = Math.max(recoil * 0.35F, 0.05F);
        return new RecoilProfile(kick, kick * 0.35F, 0.08F, Math.max(kick * 0.8F, 0.25F), 0.12F, 0.55F, new float[] { 0.0F });
    }

    public float horizontalOffset(int shotIndex) {
        if (horizontalPattern.length == 0) {
            return 0.0F;
        }

        return horizontalPattern[Math.floorMod(shotIndex, horizontalPattern.length)] * horizontalKick;
    }
}
