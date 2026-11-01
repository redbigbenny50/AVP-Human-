package com.human.client.compat;

import com.human.common.gameplay.harvest.HarvestGenes;
import com.human.common.gameplay.harvest.SubstanceBucketItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * The harvest genes for JEI, REI and EMI (one shared list, like DryingRecipe): what each harvest gene gives, the
 * substance bucket emptying back into items in a crafting grid, and a slime bucket placing its block. [stated] Oct 24 -
 * every new item, drop and conversion gets its recipe-viewer page in the same build.
 */
public record HarvestRecipe(
    List<ItemStack> inputs,
    Component how,
    List<ItemStack> outputs
) {

    public static List<HarvestRecipe> all() {
        var slime = SubstanceBucketItem.of(Items.SLIME_BALL);
        var ink = SubstanceBucketItem.of(Items.INK_SAC);

        return List.of(
            new HarvestRecipe(
                List.of(new ItemStack(Items.BUCKET)),
                Component.literal("Bucket on a Milkable mob (Milk, or no substance)"),
                List.of(HarvestGenes.Substance.MILK.result())
            ),
            new HarvestRecipe(
                List.of(new ItemStack(Items.BUCKET)),
                Component.literal("Bucket on a Milkable mob with Slime"),
                List.of(slime)
            ),
            new HarvestRecipe(
                List.of(new ItemStack(Items.BUCKET)),
                Component.literal("Bucket on a Milkable mob with Honey"),
                List.of(HarvestGenes.Substance.HONEY.result())
            ),
            new HarvestRecipe(
                List.of(new ItemStack(Items.BUCKET)),
                Component.literal("Bucket on a Milkable mob with Ink"),
                List.of(ink)
            ),
            new HarvestRecipe(
                List.of(new ItemStack(Items.BOWL)),
                Component.literal("Bowl on a Bowl-feeding mob"),
                List.of(new ItemStack(Items.MUSHROOM_STEW))
            ),
            new HarvestRecipe(
                List.of(new ItemStack(Items.SHEARS)),
                Component.literal("Shears on a Shearable mob"),
                List.of(new ItemStack(Items.WHITE_WOOL, 2))
            ),
            new HarvestRecipe(List.of(), Component.literal("An Egg-laying mob lays on its own"), List.of(new ItemStack(Items.EGG))),
            new HarvestRecipe(
                List.of(slime),
                Component.literal("Alone in a crafting grid"),
                List.of(new ItemStack(Items.SLIME_BALL, 9), new ItemStack(Items.BUCKET))
            ),
            new HarvestRecipe(
                List.of(ink),
                Component.literal("Alone in a crafting grid"),
                List.of(new ItemStack(Items.INK_SAC, 9), new ItemStack(Items.BUCKET))
            ),
            new HarvestRecipe(
                List.of(slime),
                Component.literal("Use on a block to place it"),
                List.of(new ItemStack(Items.SLIME_BLOCK))
            )
        );
    }
}
