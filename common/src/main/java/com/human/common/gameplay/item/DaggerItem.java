package com.human.common.gameplay.item;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * A short blade that fights from either hand.
 * <p>
 * ⚠ EXTENDS {@link SwordItem} ON PURPOSE. Every other mod, and vanilla itself, asks "is this a sword" in a dozen places
 * - cobweb breaking, mob drops, enchantment applicability, villager trades. Subclassing keeps all of that working for
 * free and confines the dagger behaviour to the places that explicitly look for it, rather than making the knife a
 * stranger to every system that already understood it.
 * <p>
 * The behaviour itself is driven by the {@code avp_human:daggers} tag, not by this class. A dagger from another mod, or
 * one added later, joins in by being tagged and never needs to extend anything.
 */
public class DaggerItem extends SwordItem {

    public DaggerItem(Tier tier, Properties properties) {
        super(tier, properties);
    }
}
