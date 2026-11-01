package com.human.common.gameplay.cosmetic;

import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * What a terrarium, tank or crystal case shows inside - [stated] Oct 31: picked in game by right-clicking the piece
 * with an item. Each value is one interchangeable model group ({@code gContents...}); a design only accepts the values
 * its model actually has (see {@link CosmeticModelTables#CONTENTS}). The item is not used up.
 */
public enum CosmeticContents implements StringRepresentable {

    PLANT("plant", "gContentsPlant", stack -> stack.is(ItemTags.SAPLINGS)),
    FUNGUS("fungus", "gContentsFungus", stack -> stack.is(Items.RED_MUSHROOM) || stack.is(Items.BROWN_MUSHROOM) || stack.is(
        Items.CRIMSON_FUNGUS
    ) || stack.is(Items.WARPED_FUNGUS)),
    CRYSTAL_PURPLE("crystal_purple", "gContentsCrystalPurple", stack -> stack.is(Items.AMETHYST_SHARD)),
    CRYSTAL_BLUE("crystal_blue", "gContentsCrystalBlue", stack -> stack.is(Items.PRISMARINE_CRYSTALS)),
    CRYSTAL_GREEN("crystal_green", "gContentsCrystalGreen", stack -> stack.is(Items.EMERALD)),
    CRYSTAL_RED("crystal_red", "gContentsCrystalRed", stack -> stack.is(Items.REDSTONE)),
    BONE("bone", "gContentsBone", stack -> stack.is(Items.BONE)),
    TISSUE("tissue", "gContentsTissue", stack -> stack.is(Items.ROTTEN_FLESH)),
    ORGANISM("organism", "gContentsOrganism", stack -> stack.is(Items.FERMENTED_SPIDER_EYE));

    private final String id;

    private final String bone;

    private final Predicate<ItemStack> item;

    CosmeticContents(String id, String bone, Predicate<ItemStack> item) {
        this.id = id;
        this.bone = bone;
        this.item = item;
    }

    @Override
    public @NotNull String getSerializedName() {
        return id;
    }

    /** The model group this value shows. */
    public String bone() {
        return bone;
    }

    public String translationKey() {
        return "gui.avp_human.cosmetic.contents." + id;
    }

    /** The contents an item selects, or null when it selects none. */
    public static @Nullable CosmeticContents fromItem(ItemStack stack) {
        for (var contents : values()) {
            if (contents.item.test(stack)) {
                return contents;
            }
        }

        return null;
    }

    public static @Nullable CosmeticContents fromBone(String bone) {
        for (var contents : values()) {
            if (contents.bone.equals(bone)) {
                return contents;
            }
        }

        return null;
    }
}
