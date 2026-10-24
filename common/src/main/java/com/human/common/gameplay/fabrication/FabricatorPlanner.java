package com.human.common.gameplay.fabrication;

import com.human.HumanResources;
import com.human.common.gameplay.storage.ContentsRules;
import com.human.common.gameplay.storage.CrateContents;
import com.human.common.gameplay.storage.CrateItem;
import com.human.common.registry.init.HumanCrates;
import com.human.common.registry.init.HumanFabrication;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * How a fabricator prints one item from its own storage plus the storage connected to it.
 * <h2>RULES</h2>
 * <ul>
 * <li>Internal storage first, then connected storage; finished parts are shortcuts. Only what is missing is made, all
 * the way down.</li>
 * <li>RAW MATERIALS ({@code #avp_human:fabricator_raw}) are only ever taken, never crafted.</li>
 * <li>THE CHIP IS THE BLUEPRINT ([stated] Oct 16): the printed item's own recipe never asks for a paper blueprint
 * ({@code #avp_human:fabricator_chip_replaces} - the gun blueprints). The gun was broken down on the blueprint table to
 * make the chip, so the chip stands in for it. Only for the item being printed: a gun needed as a PART of something
 * else still needs its paper blueprint, so no chip unlocks a gun it was not made from.</li>
 * <li>A fabricating recipe beats a crafting recipe for the same item; otherwise the lowest crafting recipe id
 * wins.</li>
 * <li>Anything made along the way, and anything left over, goes back into INTERNAL storage - never into a connected
 * chest.</li>
 * <li>⚠ Nothing that HOLDS something ({@link ContentsRules}) is ever used up - its contents would vanish. The one
 * exception is the item a print converts: the first ingredient of a fabricating recipe (a tank becoming its W-Y
 * edition) or the smaller crate of an upgrade. Then its contents CARRY OVER to the result.</li>
 * <li>Nothing is committed here: this works on copies, and the fabricator commits only a printable plan.</li>
 * </ul>
 */
public final class FabricatorPlanner {

    public static final TagKey<Item> RAW = TagKey.create(Registries.ITEM, HumanResources.location("fabricator_raw"));

    /** Paper blueprints a blueprint chip stands in for, in the printed item's own recipe (the gun blueprints). */
    public static final TagKey<Item> CHIP_REPLACES = TagKey.create(Registries.ITEM, HumanResources.location("fabricator_chip_replaces"));

    /** Every item this ingredient accepts is a paper blueprint the chip replaces. */
    private static boolean chipReplaces(Ingredient ingredient) {
        var options = ingredient.getItems();

        if (options.length == 0) {
            return false;
        }

        for (var option : options) {
            if (!option.is(CHIP_REPLACES)) {
                return false;
            }
        }

        return true;
    }

    private static final int MAX_DEPTH = 12;

    private static final Map<RecipeManager, Map<Item, Source>> INDEX = new WeakHashMap<>();

    /** One way to make an item: what it takes, how many one batch gives, and whether it is a fabricating recipe. */
    public record Source(
        List<FabricatingRecipe.Part> parts,
        int yield,
        boolean fabricating
    ) {}

    /**
     * @param usedInternal what internal storage loses, per item
     * @param usedNetwork  what connected storage loses, per item
     * @param missing      what is not there and cannot be made
     * @param internal     internal storage afterwards (valid when {@link #printable()})
     * @param taken        how many to take from each connected entry, by index
     * @param carried      the item whose contents carry over to the result, or empty
     * @param steps        how many crafts the print involves - sets its time
     */
    public record Plan(
        Map<Item, Integer> usedInternal,
        Map<Item, Integer> usedNetwork,
        Map<Item, Integer> missing,
        List<ItemStack> internal,
        int[] taken,
        boolean fits,
        ItemStack carried,
        int steps,
        int count
    ) {

        public boolean printable() {
            return missing.isEmpty() && fits;
        }

        /**
         * The finished items - ⚠ Oct 21: the WHOLE batch its recipe makes ({@link #count}), never just one - with any
         * carried contents moved onto them.
         */
        public ItemStack result(Item item) {
            var result = new ItemStack(item, Math.max(1, count));

            if (!carried.isEmpty()) {
                result.applyComponents(carried.getComponentsPatch());

                // A crate's summary records its slot total; the bigger crate has more.
                var contents = result.get(HumanCrates.CRATE_CONTENTS.get());

                if (contents != null && result.getItem() instanceof CrateItem crate) {
                    result.set(
                        HumanCrates.CRATE_CONTENTS.get(),
                        new CrateContents(
                            contents.id(),
                            contents.items(),
                            contents.usedSlots(),
                            crate.type().size().slots(),
                            contents.preview()
                        )
                    );
                }
            }

            return result;
        }
    }

    private FabricatorPlanner() {}

    public static boolean canMake(Level level, Item item) {
        return index(level).containsKey(item);
    }

    /**
     * How many one print of {@code item} makes: its recipe's own output count (1 if it has none).
     * <p>
     * ⚠ Oct 21 (tester: ammo "consumes the amount a crafting table does but only makes 1"). [stated] "It should never
     * make less than its crafted recipe for the same resources ... each craft is that stack." A print used to plan ONE
     * item: it still had to run the whole recipe for it, and the rest of the batch went quietly into the fabricator's
     * own storage (or nowhere, if that was full). One print now makes the recipe's full batch, and all of it lands in
     * the output.
     * </p>
     */
    public static int batchSize(Level level, Item item) {
        var source = index(level).get(item);

        return source == null ? 1 : Math.max(1, source.yield());
    }

    /**
     * Plans ONE PRINT of {@code target}: one full batch of its recipe ({@link #batchSize}).
     *
     * @param internal the fabricator's own storage (copied)
     * @param network  what connected storage holds, one entry per inventory and item (copied)
     */
    public static Plan plan(Level level, Item target, List<ItemStack> internal, List<ItemStack> network) {
        var solver = new Solver(level, index(level), internal, network, target);

        var count = batchSize(level, target);

        solver.make(target, count, new HashSet<>(), 0);

        var internalAfter = new ArrayList<ItemStack>(internal.size());
        var taken = new int[network.size()];
        var usedNetwork = new LinkedHashMap<Item, Integer>();

        for (int slot = 0; slot < internal.size(); slot++) {
            internalAfter.add(solver.storage.get(slot));
        }

        for (int entry = 0; entry < network.size(); entry++) {
            var after = solver.storage.get(internal.size() + entry);

            taken[entry] = network.get(entry).getCount() - (after.isEmpty() ? 0 : after.getCount());

            if (taken[entry] > 0) {
                usedNetwork.merge(network.get(entry).getItem(), taken[entry], Integer::sum);
            }
        }

        return new Plan(
            netUse(internal, internalAfter),
            usedNetwork,
            solver.missing,
            internalAfter,
            taken,
            solver.fits,
            solver.carried,
            solver.steps,
            count
        );
    }

    private static synchronized Map<Item, Source> index(Level level) {
        return INDEX.computeIfAbsent(level.getRecipeManager(), manager -> {
            var index = new LinkedHashMap<Item, Source>();
            var crafting = new ArrayList<>(manager.getAllRecipesFor(RecipeType.CRAFTING));

            crafting.sort((a, b) -> a.id().toString().compareTo(b.id().toString()));

            for (var holder : crafting) {
                var result = holder.value().getResultItem(level.registryAccess());

                if (result.isEmpty() || holder.value().getIngredients().isEmpty() || index.containsKey(result.getItem())) {
                    continue;
                }

                var parts = new ArrayList<FabricatingRecipe.Part>();

                for (var ingredient : holder.value().getIngredients()) {
                    if (!ingredient.isEmpty()) {
                        parts.add(new FabricatingRecipe.Part(ingredient, 1));
                    }
                }

                index.put(result.getItem(), new Source(parts, result.getCount(), false));
            }

            for (var holder : manager.getAllRecipesFor(HumanFabrication.FABRICATING_TYPE.get())) {
                var result = holder.value().getResultItem(level.registryAccess());

                if (!result.isEmpty()) {
                    index.put(result.getItem(), new Source(holder.value().parts(), result.getCount(), true));
                }
            }

            return index;
        });
    }

    private static Map<Item, Integer> netUse(List<ItemStack> before, List<ItemStack> after) {
        var counts = new LinkedHashMap<Item, Integer>();

        before.forEach(stack -> {
            if (!stack.isEmpty()) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        });
        after.forEach(stack -> {
            if (!stack.isEmpty()) {
                counts.merge(stack.getItem(), -stack.getCount(), Integer::sum);
            }
        });
        counts.values().removeIf(amount -> amount <= 0);

        return counts;
    }

    private static final class Solver {

        private final Map<Item, Source> index;

        /** Internal storage first, then the connected entries. Only internal slots ever receive anything. */
        private final List<ItemStack> storage = new ArrayList<>();

        private final int internalSlots;

        private final Map<Item, Integer> missing = new LinkedHashMap<>();

        private final boolean crateTarget;

        private final Item target;

        private boolean fits = true;

        private ItemStack carried = ItemStack.EMPTY;

        private int steps;

        Solver(Level level, Map<Item, Source> index, List<ItemStack> internal, List<ItemStack> network, Item target) {
            this.index = index;
            this.internalSlots = internal.size();
            this.crateTarget = new ItemStack(target).getItem() instanceof CrateItem;
            this.target = target;

            internal.forEach(stack -> storage.add(stack.copy()));
            network.forEach(stack -> storage.add(stack.copy()));
        }

        void make(Item item, int count, Set<Item> path, int depth) {
            var source = index.get(item);

            if (source == null || depth > MAX_DEPTH || path.contains(item) || (depth > 0 && new ItemStack(item).is(RAW))) {
                missing.merge(item, count, Integer::sum);
                return;
            }

            var batches = (count + source.yield() - 1) / source.yield();

            path.add(item);
            steps += batches;

            for (int part = 0; part < source.parts().size(); part++) {
                var entry = source.parts().get(part);

                // The printed item's paper blueprint: the chip is the blueprint, so it is not needed (see the rules).
                if (depth == 0 && chipReplaces(entry.ingredient())) {
                    continue;
                }

                // The converted item: the first part of the PRINTED item's fabricating recipe, or a crate upgrade's
                // crate.
                var carries = depth == 0 && (source.fabricating() ? part == 0 : crateTarget);

                need(entry.ingredient(), entry.count() * batches, path, depth + 1, carries);
            }

            path.remove(item);

            var surplus = batches * source.yield() - count;

            if (surplus > 0) {
                store(new ItemStack(item, surplus));
            }
        }

        void need(Ingredient ingredient, int count, Set<Item> path, int depth, boolean carries) {
            var remaining = count - take(ingredient, count, carries);

            if (remaining > 0) {
                for (var option : ingredient.getItems()) {
                    var item = option.getItem();

                    if (index.containsKey(item) && !path.contains(item) && !option.is(RAW)) {
                        make(item, remaining, path, depth);
                        remaining -= take(ingredient, remaining, carries);
                        break;
                    }
                }
            }

            if (remaining > 0) {
                var shown = ingredient.getItems().length > 0 ? ingredient.getItems()[0].getItem() : net.minecraft.world.item.Items.BARRIER;

                missing.merge(shown, remaining, Integer::sum);
            }
        }

        /** Empty stacks first; one stack holding contents only where it carries over, and only one per print. */
        int take(Ingredient ingredient, int count, boolean carries) {
            var taken = takeMatching(ingredient, count, false);

            if (taken < count && carries && carried.isEmpty()) {
                taken += takeMatching(ingredient, 1, true);
            }

            return taken;
        }

        private int takeMatching(Ingredient ingredient, int count, boolean holding) {
            var taken = 0;

            for (int slot = 0; slot < storage.size() && taken < count; slot++) {
                var stack = storage.get(slot);

                if (stack.isEmpty() || !ingredient.test(stack) || ContentsRules.holdsContents(stack) != holding) {
                    continue;
                }

                // ⚠ Contents only ever carry between the SAME KIND of block (crate to crate, tank to tank). A filled
                // chest
                // is the first ingredient of a faction crate, and its items would be copied onto a crate that never
                // reads them - lost. So a holding stack of a different kind is never used at all.
                if (holding && !sameKind(stack.getItem(), target)) {
                    continue;
                }

                var moved = Math.min(count - taken, stack.getCount());

                if (holding) {
                    carried = stack.copyWithCount(1);
                }

                var leftover = stack.getItem().getCraftingRemainingItem();

                stack.shrink(moved);
                taken += moved;

                if (stack.isEmpty()) {
                    storage.set(slot, ItemStack.EMPTY);
                }

                // A crafting table hands back the empty bucket a recipe used; so does the fabricator.
                if (leftover != null) {
                    store(new ItemStack(leftover, moved));
                }
            }

            return taken;
        }

        /**
         * Same item class, and for block items related block classes: a tank to its W-Y edition, never a chest to a
         * crate.
         */
        private static boolean sameKind(Item source, Item result) {
            if (source.getClass() != result.getClass()) {
                return false;
            }

            if (source instanceof net.minecraft.world.item.BlockItem from && result instanceof net.minecraft.world.item.BlockItem to) {
                // An edition may subclass its standard block (WyFluidTankBlock extends FluidTankBlock): one class
                // extending
                // the other is the same kind. A chest and a crate are unrelated, so a filled chest never qualifies.
                var a = from.getBlock().getClass();
                var b = to.getBlock().getClass();

                return a.isAssignableFrom(b) || b.isAssignableFrom(a);
            }

            return true;
        }

        /** Into INTERNAL storage only: merged first, then empty slots. No room = the plan does not fit. */
        void store(ItemStack stack) {
            for (int slot = 0; slot < internalSlots && !stack.isEmpty(); slot++) {
                var held = storage.get(slot);

                if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, stack)) {
                    var moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());

                    if (moved > 0) {
                        held.grow(moved);
                        stack.shrink(moved);
                    }
                }
            }

            for (int slot = 0; slot < internalSlots && !stack.isEmpty(); slot++) {
                if (storage.get(slot).isEmpty()) {
                    storage.set(slot, stack.split(stack.getMaxStackSize()));
                }
            }

            if (!stack.isEmpty()) {
                fits = false;
            }
        }
    }
}
