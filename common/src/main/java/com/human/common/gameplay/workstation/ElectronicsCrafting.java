package com.human.common.gameplay.workstation;

import com.human.common.registry.tag.HumanItemTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the electronics workstation can make, and the plan for making one.
 * <h2>ONLY THE ELECTRONICS TAG</h2> [stated] Sep 29: it makes the items in {@code #avp_human:electronics}, using their
 * ordinary crafting-table recipes - and it only ever CRAFTS a tagged item. Anything else a recipe needs must already be
 * in its storage. A cheap recipe another mod adds for a non-electronics item, or a loop between recipes, can therefore
 * never turn it into a duplicator.
 * <h2>HOW A CRAFT IS RESOLVED</h2> For each ingredient: take matching items from storage first - finished components
 * included - and for any shortfall, craft a tagged item that satisfies it, the same way, all the way down the tree.
 * What a nested craft makes beyond what was needed (a diode recipe makes eight) goes back into storage. The result is a
 * {@link Plan}: everything taken from storage, everything missing, and what storage holds afterwards. A craft is
 * committed only when nothing is missing - it completes entirely or uses nothing.
 * <h2>SAME ANSWER ON BOTH SIDES</h2> The server plans to craft; the client plans the same thing, from the same synced
 * storage and recipes, to draw the material list. Everything here is deterministic: recipes are picked by id order,
 * candidates by registry order.
 * <p>
 * ⚠ Container remainders (a recipe that hands back a bucket) are not returned - no electronics recipe has one.
 * </p>
 */
public final class ElectronicsCrafting {

    /** Deeper than any real tree (smart receiver is five levels) - a guard against a pathological pack. */
    private static final int MAX_DEPTH = 12;

    private ElectronicsCrafting() {}

    /** One craftable entry: the tagged item and the crafting recipe the workstation uses for it. */
    public record Craftable(
        Item item,
        RecipeHolder<CraftingRecipe> recipe
    ) {

        public ItemStack result(Level level) {
            return recipe.value().getResultItem(level.registryAccess()).copy();
        }
    }

    /**
     * The plan for one craft.
     *
     * @param used    what storage loses, per item - raw materials and finished components alike, net of anything
     *                crafted and used inside the same plan
     * @param missing items that storage lacks and that cannot be crafted here
     * @param after   what storage would hold afterwards; valid only when {@link #craftable()}
     */
    public record Plan(
        ItemStack result,
        Map<Item, Integer> used,
        Map<Item, Integer> missing,
        List<ItemStack> after,
        boolean fits
    ) {

        public boolean craftable() {
            return missing.isEmpty() && fits;
        }
    }

    /** Every tagged item with a crafting recipe, in registry-id order - the list on the screen. */
    public static List<Craftable> craftables(Level level) {
        var index = recipeIndex(level.getRecipeManager(), level);
        var list = new ArrayList<Craftable>();

        for (var entry : index.entrySet()) {
            list.add(new Craftable(entry.getKey(), entry.getValue()));
        }

        list.sort(Comparator.comparing(craftable -> BuiltInRegistries.ITEM.getKey(craftable.item()).toString()));

        return list;
    }

    /** Tagged item -> the recipe used for it: the lowest recipe id among the crafting recipes that make it. */
    private static Map<Item, RecipeHolder<CraftingRecipe>> recipeIndex(RecipeManager manager, Level level) {
        var index = new LinkedHashMap<Item, RecipeHolder<CraftingRecipe>>();

        for (var holder : manager.getAllRecipesFor(RecipeType.CRAFTING)) {
            var result = holder.value().getResultItem(level.registryAccess());

            if (result.isEmpty() || !result.is(HumanItemTags.ELECTRONICS) || holder.value().getIngredients().isEmpty()) {
                continue;
            }

            index.merge(
                result.getItem(),
                holder,
                (existing, candidate) -> existing.id().toString().compareTo(candidate.id().toString()) <= 0 ? existing : candidate
            );
        }

        return index;
    }

    /** Plans ONE batch of the craftable (its recipe's result count) against a copy of the storage. */
    public static Plan plan(Level level, Craftable target, List<ItemStack> storage, int maxStackSize) {
        var solver = new Solver(level, recipeIndex(level.getRecipeManager(), level), storage, maxStackSize);

        solver.craft(target.item(), target.result(level).getCount(), new HashSet<>(), 0);

        return new Plan(target.result(level), netUse(storage, solver.storage), solver.missing, solver.storage, solver.fits);
    }

    /**
     * What storage actually LOSES: before minus after, per item. Intermediates crafted and used inside the same plan
     * net to nothing, and a surplus left over (the other five diodes) is not a loss - so this is the honest material
     * list.
     */
    private static Map<Item, Integer> netUse(List<ItemStack> before, List<ItemStack> after) {
        var counts = new LinkedHashMap<Item, Integer>();

        for (var stack : before) {
            if (!stack.isEmpty()) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }

        for (var stack : after) {
            if (!stack.isEmpty()) {
                counts.merge(stack.getItem(), -stack.getCount(), Integer::sum);
            }
        }

        counts.values().removeIf(amount -> amount <= 0);

        return counts;
    }

    private static final class Solver {

        private final Level level;

        private final Map<Item, RecipeHolder<CraftingRecipe>> index;

        private final List<ItemStack> storage;

        private final int maxStackSize;

        private final Map<Item, Integer> missing = new LinkedHashMap<>();

        private boolean fits = true;

        Solver(Level level, Map<Item, RecipeHolder<CraftingRecipe>> index, List<ItemStack> storage, int maxStackSize) {
            this.level = level;
            this.index = index;
            this.maxStackSize = maxStackSize;
            this.storage = new ArrayList<>(storage.size());

            for (var stack : storage) {
                this.storage.add(stack.copy());
            }
        }

        /** Makes at least {@code count} of a tagged item; any surplus goes into storage. */
        void craft(Item item, int count, Set<Item> path, int depth) {
            var holder = index.get(item);

            if (holder == null || depth > MAX_DEPTH || path.contains(item)) {
                missing.merge(item, count, Integer::sum);
                return;
            }

            var recipe = holder.value();
            var perCraft = Math.max(1, recipe.getResultItem(level.registryAccess()).getCount());
            var crafts = (count + perCraft - 1) / perCraft;

            path.add(item);

            for (var ingredient : recipe.getIngredients()) {
                if (!ingredient.isEmpty()) {
                    need(ingredient, crafts, path, depth + 1);
                }
            }

            path.remove(item);

            var surplus = crafts * perCraft - count;

            if (surplus > 0) {
                store(new ItemStack(item, surplus));
            }
        }

        /** Satisfies {@code count} of an ingredient: storage first, then crafting a tagged item that matches it. */
        void need(Ingredient ingredient, int count, Set<Item> path, int depth) {
            var remaining = count - take(ingredient, count);

            if (remaining <= 0) {
                return;
            }

            for (var option : ingredient.getItems()) {
                var item = option.getItem();

                if (option.is(HumanItemTags.ELECTRONICS) && index.containsKey(item) && !path.contains(item)) {
                    craft(item, remaining, path, depth);
                    // The craft left what it made in storage (plus any surplus); take the part this ingredient needs.
                    remaining -= take(ingredient, remaining);
                    break;
                }
            }

            if (remaining > 0) {
                var shown = ingredient.getItems().length > 0 ? ingredient.getItems()[0].getItem() : net.minecraft.world.item.Items.BARRIER;

                missing.merge(shown, remaining, Integer::sum);
            }
        }

        /** Takes up to {@code count} items matching the ingredient from storage. @return how many were taken */
        int take(Ingredient ingredient, int count) {
            var taken = 0;

            for (var index = 0; index < storage.size() && taken < count; index++) {
                var stack = storage.get(index);

                if (stack.isEmpty() || !ingredient.test(stack)) {
                    continue;
                }

                var moved = Math.min(count - taken, stack.getCount());

                stack.shrink(moved);
                taken += moved;

                if (stack.isEmpty()) {
                    storage.set(index, ItemStack.EMPTY);
                }
            }

            return taken;
        }

        /** Puts a crafted surplus back: merged first, then into empty slots. No room = the plan does not fit. */
        void store(ItemStack stack) {
            for (var slot : storage) {
                if (stack.isEmpty()) {
                    return;
                }

                if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                    var moved = Math.min(stack.getCount(), Math.min(slot.getMaxStackSize(), maxStackSize) - slot.getCount());

                    if (moved > 0) {
                        slot.grow(moved);
                        stack.shrink(moved);
                    }
                }
            }

            for (var index = 0; index < storage.size() && !stack.isEmpty(); index++) {
                if (storage.get(index).isEmpty()) {
                    storage.set(index, stack.split(Math.min(stack.getMaxStackSize(), maxStackSize)));
                }
            }

            if (!stack.isEmpty()) {
                fits = false;
            }
        }
    }
}
