package com.human.client.compat;

import com.human.common.gameplay.block.HoneyLiquidBlock;
import com.human.common.gameplay.block.LatexCauldronBlock;
import com.human.common.gameplay.block.LatexLiquidBlock;
import com.human.common.registry.init.HumanFluids;
import com.human.common.registry.init.block.CoreBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/**
 * Liquids that dry or harden into something on their own, for JEI and REI.
 * <p>
 * [stated] Oct 24 - "there should be a tab for drying liquids like for example latex and honey or any block that turns
 * into something else when left exposed or in a cauldron." Every such change in the AVP mods, found by searching every
 * block that ticks on its own:
 * </p>
 * <ul>
 * <li>a liquid LATEX source block left standing hardens into a latex block ({@link LatexLiquidBlock});</li>
 * <li>a cauldron FULL of latex cures, and a right-click then gives a latex block ({@link LatexCauldronBlock});</li>
 * <li>a liquid HONEY source block left standing hardens into a honey block ({@link HoneyLiquidBlock}).</li>
 * </ul>
 * <p>
 * Only source blocks harden - flowing liquid just drains away when its source does, which each page says. The times are
 * the averages at the default random tick speed: one random tick about every 68 seconds per block, each with the
 * blocks' own 50% chance, so a little over two minutes. (Yautja blood also "dries", but it simply disappears, so it has
 * no page.)
 * </p>
 */
public record DryingRecipe(
    Fluid liquid,
    ItemStack vessel,
    Component how,
    ItemStack result
) {

    public static List<DryingRecipe> all() {
        return List.of(
            new DryingRecipe(
                HumanFluids.LATEX.get(),
                ItemStack.EMPTY,
                Component.literal("Left standing as a source block: hardens in about 2 minutes"),
                new ItemStack(CoreBlocks.LATEX_BLOCK.get())
            ),
            new DryingRecipe(
                HumanFluids.LATEX.get(),
                new ItemStack(Items.CAULDRON),
                Component.literal("A full cauldron cures in about 2 minutes, then right-click it"),
                new ItemStack(CoreBlocks.LATEX_BLOCK.get())
            ),
            new DryingRecipe(
                HumanFluids.HONEY.get(),
                ItemStack.EMPTY,
                Component.literal("Left standing as a source block: hardens in about 2 minutes"),
                new ItemStack(Items.HONEY_BLOCK)
            )
        );
    }
}
