package com.human.common.registry.tag;

import com.human.HumanResources;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class HumanItemTags {

    public static final TagKey<Item> AMMO_ITEMS = create("ammo_items");

    /**
     * Potency tiers for carried radiation sources. Everything hot belongs to {@code radioactive_items} (that is what
     * the lead chest intercepts); these two sub-tags mark the things that are WORSE to carry. A member of neither is
     * baseline strength, so refined and raw materials can be told apart from solid blocks of the stuff.
     */
    public static final TagKey<Item> HIGHLY_RADIOACTIVE_ITEMS = create("highly_radioactive_items");

    public static final TagKey<Item> EXTREMELY_RADIOACTIVE_ITEMS = create("extremely_radioactive_items");

    public static final TagKey<Item> GUNS = create("guns");

    public static final TagKey<Item> INDUSTRIAL_GLASS = create("industrial_glass");

    public static final TagKey<Item> INDUSTRIAL_GLASS_BLOCK = create("industrial_glass_block");

    public static final TagKey<Item> INDUSTRIAL_GLASS_PANE = create("industrial_glass_pane");

    public static final TagKey<Item> LITHIUM = create("lithium");

    public static final TagKey<Item> MK50_ARMOR = create("mk50_armor");

    public static final TagKey<Item> MK50_ARMOR_REPAIR_INGREDIENTS = create("mk50_armor_repair_ingredients");

    public static final TagKey<Item> PADDING_BLOCKS = create("padding_blocks");

    public static final TagKey<Item> PLASTIC = create("plastic");

    public static final TagKey<Item> PLASTIC_BLOCKS = create("plastic_blocks");

    public static final TagKey<Item> PRESSURE_ARMOR = create("pressure_armor");

    public static final TagKey<Item> PRESSURE_ARMOR_REPAIR_INGREDIENTS = create("pressure_armor_repair_ingredients");

    public static final TagKey<Item> RADIATION_CURE_ITEMS = create("radiation_cure_items");

    public static final TagKey<Item> RADIATION_RESISTANT_ARMORS = create("radiation_resistant_armors");

    public static final TagKey<Item> RADIOACTIVE_ITEMS = create("radioactive_items");

    public static final TagKey<Item> STEEL_ARMOR_REPAIR_INGREDIENTS = create("steel_armor_repair_ingredients");

    public static final TagKey<Item> TACTICAL_ARMOR_REPAIR_INGREDIENTS = create("tactical_armor_repair_ingredients");

    public static final TagKey<Item> TITANIUM_ARMOR_REPAIR_INGREDIENTS = create("titanium_armor_repair_ingredients");

    public static final TagKey<Item> URANIUM_NUGGET_LIKE = create("uranium_nugget_like");

    public static final TagKey<Item> WY_APE_ARMOR = create("wy_ape_armor");

    public static final TagKey<Item> WY_APE_ARMOR_REPAIR_INGREDIENTS = create("wy_ape_armor_repair_ingredients");

    public static final TagKey<Item> WY_COMMANDO_ARMOR = create("wy_commando_armor");

    public static final TagKey<Item> WY_COMMANDO_ARMOR_REPAIR_INGREDIENTS = create("wy_commando_armor_repair_ingredients");

    public static final TagKey<Item> WY_ELITE_ARMOR = create("wy_elite_armor");

    public static final TagKey<Item> WY_ELITE_ARMOR_REPAIR_INGREDIENTS = create("wy_elite_armor_repair_ingredients");

    private static TagKey<Item> create(String name) {
        return TagKey.create(Registries.ITEM, HumanResources.location(name));
    }
}
