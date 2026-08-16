package com.human.common.gameplay.recipe;

import com.human.common.gameplay.item.GunItem;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.HumanRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Strips the loaded ammunition out of any gun placed alone in a crafting grid.
 * <p>
 * The gun comes back otherwise untouched — enchantments, calibration, heat, every other component survives. Only
 * {@code AMMUNITION} is cleared, and the rounds are NOT returned: this is for emptying a weapon before storing or
 * handing it on, not for recovering ammunition. A drum swallowed by a reload is spent.
 * <p>
 * Written as a custom recipe rather than a JSON one because a shaped recipe cannot copy the input's components to its
 * output; the same reason vanilla implements armour dyeing and shulker colouring this way.
 */
public class GunUnloadRecipe extends CustomRecipe {

    public GunUnloadRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, @NotNull Level level) {
        var gun = findSingleLoadedGun(input);

        return gun != null;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.@NotNull Provider registries) {
        var gun = findSingleLoadedGun(input);

        if (gun == null) {
            return ItemStack.EMPTY;
        }

        var unloaded = gun.copy();

        unloaded.set(HumanDataComponents.AMMUNITION.get(), 0);

        return unloaded;
    }

    /**
     * The grid must hold exactly one item, and it must be a gun with something still in it. Requiring a LOADED gun
     * means an already-empty one does not sit in the crafting output looking like a valid recipe.
     */
    private static ItemStack findSingleLoadedGun(CraftingInput input) {
        ItemStack found = null;

        for (var slot = 0; slot < input.size(); slot++) {
            var itemStack = input.getItem(slot);

            if (itemStack.isEmpty()) {
                continue;
            }

            if (found != null) {
                return null;
            }

            found = itemStack;
        }

        if (found == null || !(found.getItem() instanceof GunItem)) {
            return null;
        }

        return found.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0) > 0 ? found : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return HumanRecipes.GUN_UNLOAD_RECIPE_SERIALIZER.get();
    }
}
