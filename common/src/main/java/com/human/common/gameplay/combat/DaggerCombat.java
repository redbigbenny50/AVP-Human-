package com.human.common.gameplay.combat;

import com.human.common.registry.tag.HumanItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * The rules for fighting with a dagger, shared by the client's input handling and the server's resolution so the two
 * can never disagree about which hand is allowed to swing.
 */
public class DaggerCombat {

    /** Ceiling on a hand's recovery time, so a nonsensical attack-speed modifier cannot freeze a hand forever. */
    public static final float MAXIMUM_ATTACK_STRENGTH_DELAY_IN_TICKS = 40.0F;

    public static boolean isDagger(ItemStack itemStack) {
        return !itemStack.isEmpty() && itemStack.is(HumanItemTags.DAGGERS);
    }

    /**
     * Whether the player is holding a dagger in BOTH hands, which is the only state that unlocks alternating attacks.
     */
    public static boolean isDualWielding(Player player) {
        return isDagger(player.getMainHandItem()) && isDagger(player.getOffhandItem());
    }

    /**
     * Whether a right-click with the off-hand dagger should stab.
     * <p>
     * ⚠ FALSE WHILE DUAL WIELDING, and that is his rule: with a dagger in each hand the off-hand right-click stops
     * responding and both blades are driven from the left button instead. Right-click stabbing exists so a player with
     * a pickaxe or a torch in the main hand is still armed - once the main hand holds a dagger too, it has nothing left
     * to solve.
     */
    public static boolean canStabWithOffHand(Player player) {
        return isDagger(player.getOffhandItem()) && !isDualWielding(player);
    }

    /**
     * The attack damage the OFF-HAND stack is worth.
     * <p>
     * ⚠⚠ COMPUTED FROM THE STACK, NOT FROM THE PLAYER'S ATTRIBUTES. The obvious implementation - swap the two stacks,
     * call vanilla's attack, swap back - silently uses the MAIN hand's damage, because held-item attribute modifiers
     * are only applied in {@code LivingEntity.detectEquipmentUpdates} once per tick and a mid-tick swap never reaches
     * it. Two identical knives hide that completely; two DIFFERENT daggers would hit for the wrong number, which is
     * exactly the case he asked to be handled properly.
     * <p>
     * So the base attack damage attribute is taken WITHOUT any held-item contribution, and the off-hand stack's own
     * modifiers are applied to it by hand.
     */
    public static float computeOffHandAttackDamage(Player player) {
        var itemStack = player.getOffhandItem();
        var damage = player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        var modifiers = itemStack.getOrDefault(
            net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
            ItemAttributeModifiers.EMPTY
        );

        var addValue = 0.0;
        var multiplyBase = 0.0;
        var multiplyTotal = 1.0;

        for (var entry : modifiers.modifiers()) {
            if (!entry.attribute().equals(Attributes.ATTACK_DAMAGE) || !entry.slot().test(EquipmentSlot.MAINHAND)) {
                continue;
            }

            switch (entry.modifier().operation()) {
                case ADD_VALUE -> addValue += entry.modifier().amount();
                case ADD_MULTIPLIED_BASE -> multiplyBase += entry.modifier().amount();
                case ADD_MULTIPLIED_TOTAL -> multiplyTotal *= 1.0 + entry.modifier().amount();
            }
        }

        // ⚠ The stack declares its modifiers against MAINHAND - that is where a sword's attributes live and it is true
        // of every dagger too - so the slot test above asks for MAINHAND even though we are resolving an off-hand
        // swing. Asking for OFFHAND would match nothing and every dagger would hit for bare-fist damage.
        var total = (damage + addValue) * (1.0 + multiplyBase) * multiplyTotal;

        return (float) Math.max(0.0, total);
    }

    /**
     * Whether the target is close enough for an off-hand stab, using the same reach the player's own attacks use.
     */
    public static boolean isWithinReach(Player player, LivingEntity target) {
        var reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        // A little slack for the round trip: the client decided this a tick or two ago and the target has moved since.
        var allowedReach = reach + 1.0;

        return player.distanceToSqr(target) <= allowedReach * allowedReach;
    }

    private DaggerCombat() {
        throw new UnsupportedOperationException();
    }
}
