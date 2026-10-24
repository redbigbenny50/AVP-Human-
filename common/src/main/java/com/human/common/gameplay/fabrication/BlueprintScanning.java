package com.human.common.gameplay.fabrication;

import com.human.HumanResources;
import com.human.common.registry.init.HumanFabrication;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * What the blueprint table may scan, and the chip it makes.
 * <p>
 * The table dissolves the item for good, so every refusal below exists to stop a scan losing something the player did
 * not mean to give up, or skipping a rule elsewhere:
 * <ul>
 * <li>Anything still holding something ({@link com.human.common.gameplay.storage.ContentsRules}) - its contents would
 * be lost.</li>
 * <li>Faction blocks - those are unlocked by a faction license, and a blueprint must not be a way round it.</li>
 * <li>Licenses, blank chips, and anything in {@code #avp_human:blueprint_unscannable}.</li>
 * <li>[stated] Oct 16: a written blueprint CHIP is not scanned but COPIED - 1 chip + 2 raw silicon = 2 of that chip
 * (see {@link #isCopy}). Faction licenses never go in at all.</li>
 * <li>Anything with no crafting recipe - a fabricator could never print it, so the scan would only waste the item.</li>
 * </ul>
 */
public final class BlueprintScanning {

    public static final TagKey<Item> UNSCANNABLE = TagKey.create(Registries.ITEM, HumanResources.location("blueprint_unscannable"));

    /** Items some crafting recipe makes, per recipe manager - rebuilt only when datapacks reload. */
    private static final Map<RecipeManager, Set<Item>> CRAFTABLE = new WeakHashMap<>();

    public enum Result {

        OK,
        EMPTY,
        NO_SILICON,

        /** A copy needs two raw silicon. */
        NOT_ENOUGH_SILICON,

        /** A chip with nothing written on it. */
        BLANK_CHIP,
        UNSCANNABLE,
        FACTION,
        HAS_CONTENTS,
        NO_RECIPE,
        OUTPUT_FULL;

        public String key() {
            return "gui.avp_human.blueprint_table.status." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private BlueprintScanning() {}

    /** Whether {@code stack} may be scanned at all, ignoring the silicon and the output slot. */
    public static Result check(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return Result.EMPTY;
        }

        if (stack.getItem() instanceof BlueprintChipItem) {
            return isCopy(stack) ? Result.OK : Result.BLANK_CHIP;
        }

        if (stack.is(UNSCANNABLE) || stack.getItem() instanceof FactionLicenseItem) {
            return Result.UNSCANNABLE;
        }

        if (Faction.owning(stack) != null) {
            return Result.FACTION;
        }

        if (com.human.common.gameplay.storage.ContentsRules.holdsContents(stack)) {
            return Result.HAS_CONTENTS;
        }

        if (!craftable(level).contains(stack.getItem())) {
            return Result.NO_RECIPE;
        }

        return Result.OK;
    }

    /** Raw silicon a copy takes (it makes {@link #COPY_COUNT} chips from one). */
    public static final int COPY_SILICON = 2;

    public static final int COPY_COUNT = 2;

    /** A written blueprint chip: the table copies it instead of scanning it. */
    public static boolean isCopy(ItemStack stack) {
        return stack.getItem() instanceof BlueprintChipItem && stack.has(HumanFabrication.BLUEPRINT.get());
    }

    /** May this go in the item slot? Never raw silicon (it has its own slot) and never a faction license. */
    public static boolean mayInsert(ItemStack stack) {
        return !stack.is(com.human.common.registry.init.item.HumanItems.SILICON.get()) && !(stack.getItem() instanceof FactionLicenseItem);
    }

    /** A blueprint chip for {@code item}. */
    public static ItemStack chipFor(Item item) {
        var chip = new ItemStack(HumanFabrication.BLUEPRINT_CHIP.get());

        chip.set(HumanFabrication.BLUEPRINT.get(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));

        return chip;
    }

    private static synchronized Set<Item> craftable(Level level) {
        var manager = level.getRecipeManager();

        return CRAFTABLE.computeIfAbsent(manager, recipes -> {
            var items = new HashSet<Item>();

            for (var holder : recipes.getAllRecipesFor(RecipeType.CRAFTING)) {
                var result = holder.value().getResultItem(level.registryAccess());

                if (!result.isEmpty()) {
                    items.add(result.getItem());
                }
            }

            return items;
        });
    }
}
