package com.human.common.gameplay.machine;

import com.human.common.registry.tag.HumanItemTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The grinder as a decrafting table. [stated] Sep 30: "the grinder acts as a decrafting table but gives less items when
 * durability goes down" - back to the ORIGINAL INGREDIENTS (ingots, not dust), netherite included.
 * <h2>WHAT IT TAKES</h2> Anything with a crafting-table recipe that has no grinding recipe of its own (grinding recipes
 * win: ores still give two dusts), and netherite gear through its smithing upgrade. Anything else - raw materials,
 * loot-only items - is not accepted.
 * <h2>LIMITING EXPLOITS</h2> [stated] "default to the cheaper recipe if there's multiple ... to try and limit it":
 * <ul>
 * <li>Of every recipe that makes the item, the one with the FEWEST ingredients per item made - so another mod's cheap
 * recipe can never be reversed into something dearer. Ties go to the lowest recipe id, so the choice never varies.</li>
 * <li>A recipe that makes several is decrafted only as that whole batch: four planks back to one log.</li>
 * <li>Where an ingredient slot accepts several items (any plank, any wool) the first the game lists comes back.</li>
 * <li>Recipes that leave something behind (empty buckets from cake) are skipped - the classic duplication.</li>
 * <li>{@code #avp_human:no_decraft} excludes anything a pack maker wants out.</li>
 * <li>A netherite item gives its netherite ingot and what its diamond base decrafts into; the smithing template is NOT
 * returned - that would make templates reusable forever.</li>
 * </ul>
 * <h2>DURABILITY</h2> Every output is scaled by the durability left; the fraction is rolled as a chance, so on average
 * it is exact. [stated] "At least one material returns": the MAIN material - the ingredient the recipe uses most of -
 * never drops below one. Enchantments are lost.
 * <h2>COST</h2> Plans are worked out once per item and cached until the recipes reload, so a working grinder pays a map
 * lookup.
 */
public final class GrinderDecrafting {

    /** What decrafting one batch of an item gives back at full durability. */
    public record Plan(
        int inputCount,
        List<ItemStack> outputs,
        Item mainMaterial
    ) {}

    /**
     * Plans per recipe manager. In single-player the client and the server each have their own, and a /reload makes a
     * new one - keyed like this, neither ever wipes the other's plans, and old managers are dropped with their plans.
     */
    private static final Map<RecipeManager, Map<Item, Optional<Plan>>> CACHE = new java.util.WeakHashMap<>();

    private GrinderDecrafting() {}

    /** The plan for this item, or null when it cannot be decrafted. */
    public static synchronized @Nullable Plan plan(Level level, ItemStack stack) {
        if (stack.isEmpty() || stack.is(HumanItemTags.NO_DECRAFT)) {
            return null;
        }

        var plans = CACHE.computeIfAbsent(level.getRecipeManager(), manager -> new HashMap<>());

        return plans.computeIfAbsent(stack.getItem(), item -> Optional.ofNullable(build(level, item, new HashSet<>()))).orElse(null);
    }

    private static @Nullable Plan build(Level level, Item item, Set<Item> visiting) {
        if (!visiting.add(item)) {
            return null;
        }

        var crafting = cheapestCrafting(level, item);

        return crafting != null ? crafting : smithing(level, item, visiting);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The crafting table
    // ------------------------------------------------------------------------------------------------------------

    private static @Nullable Plan cheapestCrafting(Level level, Item item) {
        Plan best = null;
        var bestCost = Double.MAX_VALUE;
        String bestId = null;

        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            var recipe = holder.value();
            var result = recipe.getResultItem(level.registryAccess());

            if (result.isEmpty() || result.getItem() != item || recipe.isSpecial()) {
                continue;
            }

            var counts = new LinkedHashMap<Item, Integer>();
            var usable = true;

            for (var ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) {
                    continue;
                }

                var options = ingredient.getItems();

                if (options.length == 0) {
                    usable = false;
                    break;
                }

                var first = options[0].getItem();

                // ⚠ A recipe that hands something back (a bucket) would give it again here - the cake duplication.
                if (first.hasCraftingRemainingItem()) {
                    usable = false;
                    break;
                }

                counts.merge(first, 1, Integer::sum);
            }

            if (!usable || counts.isEmpty()) {
                continue;
            }

            var total = counts.values().stream().mapToInt(Integer::intValue).sum();
            var cost = total / (double) result.getCount();
            var id = holder.id().toString();

            if (cost < bestCost || cost == bestCost && id.compareTo(bestId) < 0) {
                bestCost = cost;
                bestId = id;
                best = toPlan(result.getCount(), counts);
            }
        }

        return best;
    }

    private static Plan toPlan(int inputCount, Map<Item, Integer> counts) {
        var outputs = new ArrayList<ItemStack>();
        Item main = null;
        var mainCount = -1;

        for (var entry : counts.entrySet()) {
            outputs.add(new ItemStack(entry.getKey(), entry.getValue()));

            if (entry.getValue() > mainCount) {
                mainCount = entry.getValue();
                main = entry.getKey();
            }
        }

        return new Plan(inputCount, List.copyOf(outputs), main);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The smithing table - netherite
    // ------------------------------------------------------------------------------------------------------------

    /**
     * [stated] "As for netherite allow it": the smithing upgrade that makes this item gives back its addition (the
     * netherite ingot) plus what its base (the diamond item) decrafts into. The template stays consumed.
     */
    private static @Nullable Plan smithing(Level level, Item item, Set<Item> visiting) {
        for (var holder : level.getRecipeManager().getAllRecipesFor(RecipeType.SMITHING)) {
            if (!(holder.value() instanceof SmithingTransformRecipe recipe)) {
                continue;
            }

            if (recipe.getResultItem(level.registryAccess()).getItem() != item) {
                continue;
            }

            Item base = null;
            Item addition = null;

            // The recipe keeps its ingredients private; it answers "is this the base / the addition?" instead.
            for (var candidate : BuiltInRegistries.ITEM) {
                if (candidate == Items.AIR) {
                    continue;
                }

                var probe = new ItemStack(candidate);

                if (base == null && recipe.isBaseIngredient(probe)) {
                    base = candidate;
                } else if (addition == null && recipe.isAdditionIngredient(probe)) {
                    addition = candidate;
                }

                if (base != null && addition != null) {
                    break;
                }
            }

            if (base == null || addition == null) {
                continue;
            }

            var counts = new LinkedHashMap<Item, Integer>();

            counts.put(addition, 1);

            var basePlan = build(level, base, visiting);

            if (basePlan != null) {
                for (var output : basePlan.outputs()) {
                    counts.merge(output.getItem(), output.getCount(), Integer::sum);
                }
            } else {
                counts.merge(base, 1, Integer::sum);
            }

            return toPlan(1, counts);
        }

        return null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Rolling the outputs
    // ------------------------------------------------------------------------------------------------------------

    /**
     * What one decraft of {@code input} gives: every output scaled by the durability left, the fraction rolled as a
     * chance, and at least one of the main material.
     */
    public static List<ItemStack> roll(Plan plan, ItemStack input, RandomSource random) {
        var fraction = 1.0D;

        if (input.isDamageableItem() && input.getMaxDamage() > 0) {
            fraction = Math.max(0.0D, 1.0D - input.getDamageValue() / (double) input.getMaxDamage());
        }

        var results = new ArrayList<ItemStack>();

        for (var output : plan.outputs()) {
            var expected = output.getCount() * fraction;
            var count = (int) Math.floor(expected);

            if (random.nextDouble() < expected - count) {
                count++;
            }

            // [stated] "At least one material returns."
            if (output.getItem() == plan.mainMaterial()) {
                count = Math.max(1, count);
            }

            if (count > 0) {
                results.add(new ItemStack(output.getItem(), count));
            }
        }

        return results;
    }
}
