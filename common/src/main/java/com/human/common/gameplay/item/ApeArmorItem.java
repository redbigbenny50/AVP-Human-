package com.human.common.gameplay.item;

import com.human.common.registry.init.HumanArmorMaterials;
import net.minecraft.world.item.ArmorItem;

public class ApeArmorItem extends ArmorItem {

    /**
     * Durability is set per piece rather than through vanilla's multiplier.
     * <p>
     * A material multiplier cannot express these numbers: vanilla scales each slot by a fixed factor (11 for a helmet,
     * 16 a chestplate, 15 leggings, 13 boots), so any single multiplier locks the four pieces into that ratio. The
     * values wanted here do not sit on it, so they are stated outright. {@code ArmorItem} passes the properties
     * straight to {@code Item}, so what is set here is what the piece gets.
     * <p>
     * For scale, netherite works out at 407/592/555/481, so a full ape set now sits a little under it.
     */
    private static final int HELMET_DURABILITY = 350;

    private static final int CHESTPLATE_DURABILITY = 480;

    private static final int LEGGINGS_DURABILITY = 460;

    private static final int BOOTS_DURABILITY = 440;

    /**
     * Only reachable through {@code Type.BODY}, the animal-armour slot, which has no ape piece. Kept so the switch
     * stays total rather than throwing if one is ever added.
     */
    private static final int FALLBACK_DURABILITY_MULTIPLIER = 14;

    public ApeArmorItem(Type type) {
        super(HumanArmorMaterials.APE, type, new Properties().durability(durabilityFor(type)));
    }

    private static int durabilityFor(Type type) {
        return switch (type) {
            case HELMET -> HELMET_DURABILITY;
            case CHESTPLATE -> CHESTPLATE_DURABILITY;
            case LEGGINGS -> LEGGINGS_DURABILITY;
            case BOOTS -> BOOTS_DURABILITY;
            default -> type.getDurability(FALLBACK_DURABILITY_MULTIPLIER);
        };
    }
}
