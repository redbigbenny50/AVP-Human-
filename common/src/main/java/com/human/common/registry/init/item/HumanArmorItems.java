package com.human.common.registry.init.item;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.item.ApeArmorItem;
import com.human.common.gameplay.item.MK50ArmorItem;
import com.human.common.gameplay.item.PressureSuitArmorItem;
import com.human.common.registry.init.HumanArmorMaterials;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class HumanArmorItems {

    private static final BLibRegistry<Item> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final int STEEL_DURABILITY_MULTIPLIER = 21;

    private static final int TACTICAL_DURABILITY_MULTIPLIER = 18;

    private static final int TITANIUM_DURABILITY_MULTIPLIER = 27;

    private static final int WY_COMMANDO_DURABILITY_MULTIPLIER = 18;

    private static final int WY_ELITE_DURABILITY_MULTIPLIER = 18;

    public static final BLibHolder<ArmorItem> WY_APE_BOOTS = create("ape_boots", () -> new ApeArmorItem(ArmorItem.Type.BOOTS));

    public static final BLibHolder<ArmorItem> WY_APE_CHESTPLATE = create(
        "ape_chestplate",
        () -> new ApeArmorItem(ArmorItem.Type.CHESTPLATE)
    );

    public static final BLibHolder<ArmorItem> WY_APE_HELMET = create("ape_helmet", () -> new ApeArmorItem(ArmorItem.Type.HELMET));

    public static final BLibHolder<ArmorItem> WY_APE_LEGGINGS = create(
        "ape_leggings",
        () -> new ApeArmorItem(ArmorItem.Type.LEGGINGS)
    );

    public static final BLibHolder<ArmorItem> MK50_BOOTS = create("mk50_boots", () -> new MK50ArmorItem(ArmorItem.Type.BOOTS));

    /**
     * [stated] Sep 30: the electrician colonist's work clothes - a hard hat and a hi-vis jacket, worn over his normal
     * clothes. Leather's protection: they are work clothes, not armour.
     */
    public static final BLibHolder<ArmorItem> ELECTRICIAN_HARD_HAT = create(
        "electrician_hard_hat",
        () -> new ArmorItem(
            net.minecraft.world.item.ArmorMaterials.LEATHER,
            ArmorItem.Type.HELMET,
            new net.minecraft.world.item.Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(5))
        )
    );

    public static final BLibHolder<ArmorItem> ELECTRICIAN_JACKET = create(
        "electrician_jacket",
        () -> new ArmorItem(
            net.minecraft.world.item.ArmorMaterials.LEATHER,
            ArmorItem.Type.CHESTPLATE,
            new net.minecraft.world.item.Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(5))
        )
    );

    public static final BLibHolder<ArmorItem> MK50_CHESTPLATE = create(
        "mk50_chestplate",
        () -> new MK50ArmorItem(ArmorItem.Type.CHESTPLATE)
    );

    public static final BLibHolder<ArmorItem> MK50_HELMET = create("mk50_helmet", () -> new MK50ArmorItem(ArmorItem.Type.HELMET));

    public static final BLibHolder<ArmorItem> MK50_LEGGINGS = create(
        "mk50_leggings",
        () -> new MK50ArmorItem(ArmorItem.Type.LEGGINGS)
    );

    public static final BLibHolder<ArmorItem> PRESSURE_BOOTS = create(
        "pressure_boots",
        () -> new PressureSuitArmorItem(ArmorItem.Type.BOOTS)
    );

    public static final BLibHolder<ArmorItem> PRESSURE_CHESTPLATE = create(
        "pressure_chestplate",
        () -> new PressureSuitArmorItem(ArmorItem.Type.CHESTPLATE)
    );

    public static final BLibHolder<ArmorItem> PRESSURE_HELMET = create(
        "pressure_helmet",
        () -> new PressureSuitArmorItem(ArmorItem.Type.HELMET)
    );

    public static final BLibHolder<ArmorItem> PRESSURE_LEGGINGS = create(
        "pressure_leggings",
        () -> new PressureSuitArmorItem(ArmorItem.Type.LEGGINGS)
    );

    public static final BLibHolder<ArmorItem> STEEL_BOOTS = create(
        "steel_boots",
        HumanArmorMaterials.STEEL,
        ArmorItem.Type.BOOTS,
        STEEL_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> STEEL_CHESTPLATE = create(
        "steel_chestplate",
        HumanArmorMaterials.STEEL,
        ArmorItem.Type.CHESTPLATE,
        STEEL_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> STEEL_HELMET = create(
        "steel_helmet",
        HumanArmorMaterials.STEEL,
        ArmorItem.Type.HELMET,
        STEEL_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> STEEL_LEGGINGS = create(
        "steel_leggings",
        HumanArmorMaterials.STEEL,
        ArmorItem.Type.LEGGINGS,
        STEEL_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> TACTICAL_BOOTS = create(
        "tactical_boots",
        HumanArmorMaterials.TACTICAL,
        ArmorItem.Type.BOOTS,
        TACTICAL_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> TACTICAL_CHESTPLATE = create(
        "tactical_chestplate",
        HumanArmorMaterials.TACTICAL,
        ArmorItem.Type.CHESTPLATE,
        TACTICAL_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> TACTICAL_HELMET = create(
        "tactical_helmet",
        HumanArmorMaterials.TACTICAL,
        ArmorItem.Type.HELMET,
        TACTICAL_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> TACTICAL_LEGGINGS = create(
        "tactical_leggings",
        HumanArmorMaterials.TACTICAL,
        ArmorItem.Type.LEGGINGS,
        TACTICAL_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> TACTICAL_CAMO_BOOTS = create(
        "tactical_camo_boots",
        HumanArmorMaterials.TACTICAL,
        ArmorItem.Type.BOOTS,
        TACTICAL_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> TACTICAL_CAMO_CHESTPLATE = create(
        "tactical_camo_chestplate",
        HumanArmorMaterials.TACTICAL,
        ArmorItem.Type.CHESTPLATE,
        TACTICAL_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> TACTICAL_CAMO_HELMET = create(
        "tactical_camo_helmet",
        HumanArmorMaterials.TACTICAL,
        ArmorItem.Type.HELMET,
        TACTICAL_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> TACTICAL_CAMO_LEGGINGS = create(
        "tactical_camo_leggings",
        HumanArmorMaterials.TACTICAL,
        ArmorItem.Type.LEGGINGS,
        TACTICAL_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> TITANIUM_BOOTS = create(
        "titanium_boots",
        HumanArmorMaterials.TITANIUM,
        ArmorItem.Type.BOOTS,
        TITANIUM_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> TITANIUM_CHESTPLATE = create(
        "titanium_chestplate",
        HumanArmorMaterials.TITANIUM,
        ArmorItem.Type.CHESTPLATE,
        TITANIUM_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> TITANIUM_HELMET = create(
        "titanium_helmet",
        HumanArmorMaterials.TITANIUM,
        ArmorItem.Type.HELMET,
        TITANIUM_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> TITANIUM_LEGGINGS = create(
        "titanium_leggings",
        HumanArmorMaterials.TITANIUM,
        ArmorItem.Type.LEGGINGS,
        TITANIUM_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> WY_COMMANDO_BOOTS = create(
        "wy_commando_boots",
        HumanArmorMaterials.WY_COMMANDO,
        ArmorItem.Type.BOOTS,
        WY_COMMANDO_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> WY_COMMANDO_CHESTPLATE = create(
        "wy_commando_chestplate",
        HumanArmorMaterials.WY_COMMANDO,
        ArmorItem.Type.CHESTPLATE,
        WY_COMMANDO_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> WY_COMMANDO_HELMET = create(
        "wy_commando_helmet",
        HumanArmorMaterials.WY_COMMANDO,
        ArmorItem.Type.HELMET,
        WY_COMMANDO_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> WY_COMMANDO_LEGGINGS = create(
        "wy_commando_leggings",
        HumanArmorMaterials.WY_COMMANDO,
        ArmorItem.Type.LEGGINGS,
        WY_COMMANDO_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> WY_ELITE_BOOTS = create(
        "wy_elite_boots",
        HumanArmorMaterials.WY_ELITE,
        ArmorItem.Type.BOOTS,
        WY_ELITE_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> WY_ELITE_CHESTPLATE = create(
        "wy_elite_chestplate",
        HumanArmorMaterials.WY_ELITE,
        ArmorItem.Type.CHESTPLATE,
        WY_ELITE_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> WY_ELITE_HELMET = create(
        "wy_elite_helmet",
        HumanArmorMaterials.WY_ELITE,
        ArmorItem.Type.HELMET,
        WY_ELITE_DURABILITY_MULTIPLIER
    );

    public static final BLibHolder<ArmorItem> WY_ELITE_LEGGINGS = create(
        "wy_elite_leggings",
        HumanArmorMaterials.WY_ELITE,
        ArmorItem.Type.LEGGINGS,
        WY_ELITE_DURABILITY_MULTIPLIER
    );

    private static BLibHolder<ArmorItem> create(
        String path,
        BLibHolder<ArmorMaterial> holder,
        ArmorItem.Type type,
        int durabilityMultiplier
    ) {
        return create(path, holder, type, durabilityMultiplier, new Item.Properties());
    }

    private static BLibHolder<ArmorItem> create(
        String path,
        BLibHolder<ArmorMaterial> holder,
        ArmorItem.Type type,
        int durabilityMultiplier,
        Item.Properties properties
    ) {
        return create(path, () -> createArmorItem(holder, type, durabilityMultiplier, properties));
    }

    private static <T extends ArmorItem> BLibHolder<T> create(String path, Supplier<T> itemSupplier) {
        return REGISTRY.createHolder(path, itemSupplier);
    }

    private static ArmorItem createArmorItem(
        Holder<ArmorMaterial> holder,
        ArmorItem.Type type,
        int durabilityMultiplier,
        Item.Properties properties
    ) {
        var durability = type.getDurability(durabilityMultiplier);
        properties = properties.durability(durability);
        return new ArmorItem(holder, type, properties);
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
