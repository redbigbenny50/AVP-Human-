package com.human.common.gameplay.fabrication;

import com.human.HumanResources;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The two factions whose blocks are printed only in a fabricator.
 * <p>
 * [stated] Oct 13: a faction license unlocks its faction's versions of blocks and its own blocks - "faction specific
 * blueprint collections". What a faction owns is its item tag, so a pack maker can add to it with a data pack.
 */
public enum Faction {

    USCM("uscm"),
    WY("wy");

    private final String id;

    private final TagKey<Item> items;

    Faction(String id) {
        this.id = id;
        this.items = TagKey.create(Registries.ITEM, HumanResources.location("faction/" + id));
    }

    public String id() {
        return id;
    }

    /** Everything this faction's license unlocks in a fabricator. */
    public TagKey<Item> items() {
        return items;
    }

    /** The faction that owns this item, or null for an ordinary item. */
    public static Faction owning(net.minecraft.world.item.ItemStack stack) {
        for (var faction : values()) {
            if (stack.is(faction.items)) {
                return faction;
            }
        }

        return null;
    }
}
