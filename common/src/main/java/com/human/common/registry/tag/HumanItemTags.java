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

    public static final TagKey<Item> DAGGERS = create("daggers");

    /**
     * Oct 7 - helmets a Gigeresque facehugger will not go for: a sealed face mask leaves nothing to grip. See
     * {@code MixinGigEntityUtils_FacehuggerProofMask}. Filled with the APE helmet; packs can add more.
     */
    public static final TagKey<Item> FACEHUGGER_PROOF_HELMETS = create("facehugger_proof_helmets");

    /**
     * [stated] Oct 1: armour that keeps cryofluid out, worn as a FULL set - the MK50, and through Ad Astra's
     * {@code freeze_resistant_armor} tag any other mod's cold suit.
     */
    public static final TagKey<Item> CRYO_PROTECTIVE_ARMOR = create("cryo_protective_armor");

    /**
     * [stated] Sep 29: what the electronics workstation makes - components, the terminal, the machine block. It only
     * ever CRAFTS a member of this tag (see ElectronicsCrafting), so adding an item here is how a pack teaches it more.
     */
    public static final TagKey<Item> ELECTRONICS = create("electronics");

    /** [stated] Sep 30: items the grinder will NOT decraft - empty by default, for pack makers to fill. */
    public static final TagKey<Item> NO_DECRAFT = create("no_decraft");

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

    /**
     * The sibling mods' music-disc fragments, one tag each, so a chest can offer them WITHOUT avp_human depending on
     * either mod.
     * <p>
     * ⚠⚠ THIS EXISTS BECAUSE A LOOT TABLE CANNOT NAME AN ITEM THAT MIGHT NOT BE REGISTERED. {@code LootItem} parses its
     * {@code name} through {@code BuiltInRegistries.ITEM.holderByNameCodec()}, so one unknown id fails the codec and
     * the WHOLE table is dropped at load - not just that entry. avp_alien and avp_predator are OPTIONAL dependencies,
     * so naming their items directly meant marine_chest_personal (16 per outpost) silently vanished for anyone running
     * avp_human on its own. A tag entry has no such problem: an item id inside a tag can be marked
     * {@code required: false} and simply drops out.
     * </p>
     * <p>
     * ⚠ ONE TAG PER MOD, NOT ONE SHARED TAG. A single tag holding both would make the two discs a single loot entry
     * competing for one weight, changing the odds. Separate tags keep each fragment's own weight exactly as authored.
     * </p>
     */
    public static final TagKey<Item> ALIEN_MUSIC_DISC_FRAGMENTS = create("loot/alien_music_disc_fragments");

    public static final TagKey<Item> PREDATOR_MUSIC_DISC_FRAGMENTS = create("loot/predator_music_disc_fragments");

    /** Base resin blocks from every alien strain, the industrial furnace's shortcut into plastic. */
    public static final TagKey<Item> RESIN_BLOCKS = create("resin_blocks");

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
