package com.human.mixin;

import com.human.common.gameplay.combat.DaggerCombat;
import com.human.common.gameplay.combat.OffHandAttacker;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives the player a second attack-strength cooldown belonging to the off hand.
 * <p>
 * Ticked alongside vanilla's own, so the two refill on the same clock and neither can drift.
 */
@Mixin(Player.class)
public abstract class MixinPlayer_OffHandAttackStrength implements OffHandAttacker {

    /**
     * A hand at or above this fraction of its cooldown counts as recovered. Matched to the point vanilla stops calling
     * a hit weak - below it a swing is a tap, and letting the off hand strike there would turn dual wielding into a
     * flurry of chip damage rather than two proper strikes.
     */
    @Unique
    private static final float AVP_HUMAN$READY_THRESHOLD = 0.9F;

    @Unique
    private int avp_human$offHandAttackStrengthTicker;

    @Inject(method = "tick", at = @At("HEAD"))
    private void avp_human$tickOffHand(CallbackInfo callbackInfo) {
        avp_human$tickOffHandAttackStrength();
    }

    @Override
    public void avp_human$tickOffHandAttackStrength() {
        avp_human$offHandAttackStrengthTicker++;
    }

    @Override
    public void avp_human$resetOffHandAttackStrength() {
        avp_human$offHandAttackStrengthTicker = 0;
    }

    @Override
    public boolean avp_human$isOffHandAttackReady() {
        return avp_human$getOffHandAttackStrengthScale() >= AVP_HUMAN$READY_THRESHOLD;
    }

    @Override
    public float avp_human$getOffHandAttackStrengthScale() {
        return Mth.clamp(avp_human$offHandAttackStrengthTicker / avp_human$offHandAttackStrengthDelay(), 0.0F, 1.0F);
    }

    /**
     * The off hand's own recovery time, read from the OFF-HAND stack's attack-speed modifier rather than vanilla's
     * {@code getCurrentItemAttackStrengthDelay}, which only ever looks at the main hand.
     */
    @Unique
    private float avp_human$offHandAttackStrengthDelay() {
        var player = (Player) (Object) this;
        var attackSpeed = player.getAttributeBaseValue(Attributes.ATTACK_SPEED);
        var itemStack = player.getOffhandItem();
        var modifiers = itemStack.getOrDefault(
            net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
            net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY
        );

        for (var entry : modifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_SPEED) && entry.slot().test(EquipmentSlot.MAINHAND)) {
                switch (entry.modifier().operation()) {
                    case ADD_VALUE -> attackSpeed += entry.modifier().amount();
                    case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> attackSpeed *= 1.0 + entry.modifier().amount();
                }
            }
        }

        if (attackSpeed <= 0.0) {
            return DaggerCombat.MAXIMUM_ATTACK_STRENGTH_DELAY_IN_TICKS;
        }

        return (float) Mth.clamp(
            20.0 / attackSpeed,
            1.0,
            DaggerCombat.MAXIMUM_ATTACK_STRENGTH_DELAY_IN_TICKS
        );
    }
}
