package com.human.common.gameplay.combat;

/**
 * A player that tracks a SECOND attack-strength cooldown for its off hand.
 * <p>
 * ⚠⚠ THIS IS THE WHOLE REASON ALTERNATING IS FASTER, and it is not a tweak to an existing value. Vanilla has exactly
 * one {@code attackStrengthTicker} for the whole player: swing, and the bar refills over
 * {@code getCurrentItemAttackStrengthDelay} ticks before the next hit is worth full damage. Alternating hands against a
 * single shared timer would just mean two weak hits instead of one strong one.
 * <p>
 * With a ticker per hand each blade recovers on its own clock, so by the time the main hand has swung and the off hand
 * has swung, the main hand is most of the way back to full. Nothing is exempted from the cooldown - there are simply
 * two of them, which is exactly what holding two knives ought to buy.
 */
public interface OffHandAttacker {

    /**
     * Whether the off hand has recovered enough for a full-strength strike.
     */
    boolean avp_human$isOffHandAttackReady();

    /**
     * How far the off hand's cooldown has refilled, 0.0 to 1.0, mirroring {@code Player.getAttackStrengthScale}.
     */
    float avp_human$getOffHandAttackStrengthScale();

    void avp_human$resetOffHandAttackStrength();

    void avp_human$tickOffHandAttackStrength();
}
