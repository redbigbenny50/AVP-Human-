package com.human.client.compat;

import com.human.common.gameplay.fabrication.BlueprintScanning;
import com.human.common.registry.init.HumanFabrication;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * The blueprint table's two jobs, for JEI and REI.
 * <p>
 * [stated] Oct 24 - "blueprint chips should show the blueprint table and its screen shows blocks being changing in the
 * input slot along with 1 silicon to make the blueprint of that block. Also it should show 1 blueprint chip being
 * consumed with 2 silicon to make 2 of that chip."
 * </p>
 * <ul>
 * <li><b>Scan</b>: any item the table accepts + 1 raw silicon = a blueprint chip for it. The input slot cycles through
 * every scannable item and the output cycles in step, always showing the chip for the item on screen.</li>
 * <li><b>Copy</b>: a written chip + 2 raw silicon = 2 of that chip, cycling the same way.</li>
 * </ul>
 * <p>
 * "Scannable" is decided by the table's own rule ({@link BlueprintScanning#check}) against the client's recipe book, so
 * the viewers can never show a scan the table would refuse. avp_human's own items come first in the cycle.
 * </p>
 */
public record BlueprintTableRecipe(
    List<ItemStack> inputs,
    int silicon,
    List<ItemStack> outputs
) {

    public static List<BlueprintTableRecipe> all() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }

        var items = new LinkedHashSet<Item>();
        for (var holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            try {
                var result = holder.value().getResultItem(level.registryAccess());
                if (!result.isEmpty()) {
                    items.add(result.getItem());
                }
            } catch (RuntimeException ignored) {
                // Another mod's special recipe that cannot answer outside a real craft - nothing to scan from it.
            }
        }

        var scannable = new ArrayList<Item>();
        for (var item : items) {
            try {
                if (BlueprintScanning.check(level, new ItemStack(item)) == BlueprintScanning.Result.OK) {
                    scannable.add(item);
                }
            } catch (RuntimeException ignored) {
                // An item whose own checks throw outside the world is simply not listed.
            }
        }
        scannable.sort(
            Comparator.comparing((Item item) -> !"avp_human".equals(BuiltInRegistries.ITEM.getKey(item).getNamespace()))
                .thenComparing(item -> BuiltInRegistries.ITEM.getKey(item).toString())
        );

        if (scannable.isEmpty()) {
            return List.of();
        }

        var scanInputs = new ArrayList<ItemStack>(scannable.size());
        var chips = new ArrayList<ItemStack>(scannable.size());
        var copies = new ArrayList<ItemStack>(scannable.size());
        for (var item : scannable) {
            scanInputs.add(new ItemStack(item));
            var chip = BlueprintScanning.chipFor(item);
            chips.add(chip);
            copies.add(chip.copyWithCount(BlueprintScanning.COPY_COUNT));
        }

        return List.of(
            new BlueprintTableRecipe(scanInputs, 1, chips),
            new BlueprintTableRecipe(chips, BlueprintScanning.COPY_SILICON, copies)
        );
    }

    public ItemStack siliconStack() {
        return new ItemStack(HumanItems.SILICON.get(), silicon);
    }

    /** The blueprint table itself, for the tab icon and its workstation. */
    public static ItemStack table() {
        return new ItemStack(com.human.common.registry.init.HumanBlocks.BLUEPRINT_BLOCK.get());
    }

    /** A blank chip, kept for viewers that want it. */
    public static ItemStack blankChip() {
        return new ItemStack(HumanFabrication.BLUEPRINT_CHIP.get());
    }
}
