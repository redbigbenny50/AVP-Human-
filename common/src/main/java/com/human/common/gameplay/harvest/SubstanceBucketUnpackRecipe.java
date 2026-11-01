package com.human.common.gameplay.harvest;

import com.human.common.registry.init.HumanHarvest;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * A substance bucket alone in a crafting grid gives back 9 of its item; the empty bucket stays in the grid as the
 * item's crafting remainder. A custom recipe because a JSON one cannot read the bucket's component (same reason as
 * GunUnloadRecipe).
 */
public class SubstanceBucketUnpackRecipe extends CustomRecipe {

    public SubstanceBucketUnpackRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        return single(input) != null;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input, HolderLookup.@NotNull Provider registries) {
        var bucket = single(input);
        return bucket == null
            ? ItemStack.EMPTY
            : new ItemStack(SubstanceBucketItem.substanceOf(bucket), SubstanceBucketItem.ITEMS_PER_BUCKET);
    }

    /** Exactly one filled substance bucket and nothing else. */
    private static ItemStack single(CraftingInput input) {
        ItemStack found = null;

        for (var slot = 0; slot < input.size(); slot++) {
            var stack = input.getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            if (found != null || !stack.is(HumanHarvest.SUBSTANCE_BUCKET.get()) || SubstanceBucketItem.substanceOf(stack) == Items.AIR) {
                return null;
            }

            found = stack;
        }

        return found;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return HumanHarvest.UNPACK_SERIALIZER.get();
    }
}
