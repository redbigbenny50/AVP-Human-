package com.human.client.compat.rei;

import com.human.Human;
import com.human.client.compat.VaporizingRecipe;
import com.human.common.gameplay.fabrication.FabricatingRecipe;
import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.recipe.FreezerRecipe;
import com.human.common.gameplay.recipe.GrinderRecipe;
import com.human.common.gameplay.recipe.HydratorRecipe;
import com.human.common.gameplay.recipe.PressRecipe;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.HumanChemicals;
import com.human.common.registry.init.HumanFabrication;
import com.human.common.registry.init.HumanVaporizer;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * REI tabs for every avp_human machine with its own recipes - the same set as the JEI side
 * ({@code HumanMachineCategories}). [stated] Oct 24 - "Jei seems to not be showing all recipes"; the REI plugin had the
 * same gap (industrial furnace only), so both viewers are fixed together.
 * <p>
 * One display class serves every machine: inputs and outputs as REI entries (items, and fluids in the platform's own
 * units: droplets on Fabric, millibuckets on NeoForge), plus the category it belongs to. One category class lays any of
 * them out: inputs left, arrow, outputs right, wrapping to rows of three.
 * </p>
 */
public final class HumanMachineDisplays {

    public static final CategoryIdentifier<MachineDisplay> GRINDING = CategoryIdentifier.of(Human.MOD_ID, "grinding");

    public static final CategoryIdentifier<MachineDisplay> PRESSING = CategoryIdentifier.of(Human.MOD_ID, "pressing");

    public static final CategoryIdentifier<MachineDisplay> FREEZING = CategoryIdentifier.of(Human.MOD_ID, "freezing");

    public static final CategoryIdentifier<MachineDisplay> HYDRATING = CategoryIdentifier.of(Human.MOD_ID, "hydrating");

    public static final CategoryIdentifier<MachineDisplay> FABRICATING = CategoryIdentifier.of(Human.MOD_ID, "fabricating");

    public static final CategoryIdentifier<MachineDisplay> VAPORIZING = CategoryIdentifier.of(Human.MOD_ID, "vaporizing");

    public static final CategoryIdentifier<MachineDisplay> BLUEPRINT_TABLE = CategoryIdentifier.of(Human.MOD_ID, "blueprint_table");

    public static final CategoryIdentifier<MachineDisplay> DRYING = CategoryIdentifier.of(Human.MOD_ID, "drying");

    private HumanMachineDisplays() {}

    /** A recipe as REI sees it: what goes in, what comes out, and which tab it belongs on. */
    public static final class MachineDisplay extends BasicDisplay {

        private final CategoryIdentifier<MachineDisplay> category;

        MachineDisplay(CategoryIdentifier<MachineDisplay> category, List<EntryIngredient> inputs, List<EntryIngredient> outputs) {
            super(inputs, outputs);
            this.category = category;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return category;
        }
    }

    /** One layout for every machine tab. */
    public static final class MachineCategory implements DisplayCategory<MachineDisplay> {

        private final CategoryIdentifier<MachineDisplay> id;

        private final Block icon;

        public MachineCategory(CategoryIdentifier<MachineDisplay> id, Block icon) {
            this.id = id;
            this.icon = icon;
        }

        @Override
        public CategoryIdentifier<? extends MachineDisplay> getCategoryIdentifier() {
            return id;
        }

        @Override
        public Component getTitle() {
            return icon.getName();
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(icon);
        }

        @Override
        public List<Widget> setupDisplay(MachineDisplay display, Rectangle bounds) {
            var widgets = new ArrayList<Widget>();
            widgets.add(Widgets.createRecipeBase(bounds));

            var inputs = display.getInputEntries();
            var outputs = display.getOutputEntries();
            var rows = Math.max(rows(inputs.size()), rows(outputs.size()));
            var top = bounds.getCenterY() - rows * 9;
            var left = bounds.getCenterX() - 62;

            for (int i = 0; i < inputs.size(); i++) {
                widgets.add(
                    Widgets.createSlot(new Point(left + (i % 3) * 18, top + (i / 3) * 18)).entries(inputs.get(i)).markInput()
                );
            }

            widgets.add(Widgets.createArrow(new Point(left + 56, bounds.getCenterY() - 8)).animationDurationTicks(100));

            for (int i = 0; i < outputs.size(); i++) {
                widgets.add(
                    Widgets.createSlot(new Point(left + 84 + (i % 3) * 18, top + (i / 3) * 18))
                        .entries(outputs.get(i))
                        .markOutput()
                );
            }

            return widgets;
        }

        private static int rows(int slots) {
            return Math.max(1, (slots + 2) / 3);
        }

        @Override
        public int getDisplayHeight() {
            return 66;
        }

        @Override
        public int getDisplayWidth(MachineDisplay display) {
            return 150;
        }
    }

    // ---- recipe -> display ----

    static EntryIngredient items(Ingredient ingredient, int count) {
        return EntryIngredients.ofItemStacks(
            Arrays.stream(ingredient.getItems()).map(stack -> stack.copyWithCount(Math.max(1, count))).toList()
        );
    }

    static EntryIngredient fluid(Fluid fluid, int millibuckets) {
        var amount = Math.max(
            1L,
            millibuckets * (com.blib.api.BLibAPI.getModLoaderType() == com.blib.api.common.mod.v1.model.loader.ModLoaderType.FABRIC
                ? 81_000L
                : 1_000L) / 1000L
        );
        return EntryIngredient.of(EntryStacks.of(fluid, amount));
    }

    static EntryIngredient fluid(FluidContent content) {
        return fluid(content.fluid(), content.amount());
    }

    public static MachineDisplay grinding(RecipeHolder<GrinderRecipe> holder) {
        var recipe = holder.value();
        var outputs = new ArrayList<EntryIngredient>();
        for (var result : recipe.getResults()) {
            var stack = EntryStacks.of(result.stack());
            var percent = Math.round(result.chance() * 100.0F);
            if (percent < 100) {
                stack = stack.tooltip(Component.literal(percent + "% chance").withStyle(ChatFormatting.GOLD));
            }
            outputs.add(EntryIngredient.of(stack));
        }
        return new MachineDisplay(GRINDING, List.of(items(recipe.getIngredient(), recipe.getInputCount())), outputs);
    }

    public static MachineDisplay pressing(RecipeHolder<PressRecipe> holder) {
        var recipe = holder.value();
        var outputs = new ArrayList<EntryIngredient>();
        if (!recipe.getResult().isEmpty()) {
            outputs.add(EntryIngredients.of(recipe.getResult()));
        }
        if (!recipe.getFluid().isEmpty()) {
            outputs.add(fluid(recipe.getFluid()));
        }
        return new MachineDisplay(PRESSING, List.of(items(recipe.getIngredient(), recipe.getInputCount())), outputs);
    }

    public static MachineDisplay freezing(RecipeHolder<FreezerRecipe> holder) {
        var recipe = holder.value();
        var inputs = new ArrayList<EntryIngredient>();
        if (!recipe.getIngredient().isEmpty()) {
            inputs.add(items(recipe.getIngredient(), recipe.getInputCount()));
        }
        if (!recipe.getFluid().isEmpty()) {
            inputs.add(fluid(recipe.getFluid()));
        }
        return new MachineDisplay(FREEZING, inputs, List.of(EntryIngredients.of(recipe.getResult())));
    }

    public static MachineDisplay hydrating(RecipeHolder<HydratorRecipe> holder) {
        var recipe = holder.value();
        var inputs = new ArrayList<EntryIngredient>();
        inputs.add(items(recipe.getIngredient(), recipe.getInputCount()));
        if (recipe.getWater() > 0) {
            inputs.add(fluid(Fluids.WATER, recipe.getWater()));
        }
        return new MachineDisplay(HYDRATING, inputs, List.of(EntryIngredients.of(recipe.getResult())));
    }

    public static MachineDisplay fabricating(RecipeHolder<FabricatingRecipe> holder) {
        var recipe = holder.value();
        var inputs = new ArrayList<EntryIngredient>();
        for (var part : recipe.parts()) {
            inputs.add(items(part.ingredient(), part.count()));
        }
        var level = net.minecraft.client.Minecraft.getInstance().level;
        var result = level == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
        return new MachineDisplay(FABRICATING, inputs, List.of(EntryIngredients.of(result)));
    }

    public static MachineDisplay vaporizing(VaporizingRecipe recipe) {
        return new MachineDisplay(
            VAPORIZING,
            List.of(fluid(recipe.liquid(), FluidContent.BUCKET), EntryIngredients.of(HumanChemicals.ALUMINUM_TANK.get())),
            List.of(EntryIngredients.of(recipe.tank()))
        );
    }

    /**
     * The blueprint table: scan (item + 1 silicon = its chip) and copy (chip + 2 silicon = 2 chips). The item and chip
     * lists are the same length and in the same order, and REI cycles every slot on one clock, so the chip shown always
     * matches the item shown. See {@link com.human.client.compat.BlueprintTableRecipe}.
     */
    public static MachineDisplay blueprintTable(com.human.client.compat.BlueprintTableRecipe recipe) {
        return new MachineDisplay(
            BLUEPRINT_TABLE,
            List.of(EntryIngredients.ofItemStacks(recipe.inputs()), EntryIngredients.of(recipe.siliconStack())),
            List.of(EntryIngredients.ofItemStacks(recipe.outputs()))
        );
    }

    /**
     * A liquid that hardens on its own (see {@link com.human.client.compat.DryingRecipe}); its how-to rides as a
     * tooltip.
     */
    public static MachineDisplay drying(com.human.client.compat.DryingRecipe recipe) {
        var inputs = new ArrayList<EntryIngredient>();
        inputs.add(EntryIngredient.of(fluidStack(recipe.liquid(), FluidContent.BUCKET).tooltip(recipe.how())));
        if (!recipe.vessel().isEmpty()) {
            inputs.add(EntryIngredients.of(recipe.vessel()));
        }
        var result = EntryStacks.of(recipe.result()).tooltip(recipe.how());
        return new MachineDisplay(DRYING, inputs, List.of(EntryIngredient.of(result)));
    }

    private static me.shedaniel.rei.api.common.entry.EntryStack<?> fluidStack(Fluid fluid, int millibuckets) {
        return EntryStacks.of(
            fluid,
            Math.max(
                1L,
                millibuckets * (com.blib.api.BLibAPI.getModLoaderType() == com.blib.api.common.mod.v1.model.loader.ModLoaderType.FABRIC
                    ? 81_000L
                    : 1_000L) / 1000L
            )
        );
    }

    /** The machines, for the categories and their workstations. */
    static Block grinder() {
        return HumanBlocks.ELECTRIC_GRINDER.get();
    }

    static Block press() {
        return HumanBlocks.ELECTRIC_PRESS.get();
    }

    static Block freezer() {
        return HumanBlocks.ELECTRIC_FREEZER.get();
    }

    static Block hydrator() {
        return HumanBlocks.ELECTRIC_HYDRATOR.get();
    }

    static List<Block> fabricators() {
        return List.of(HumanFabrication.FABRICATOR.get(), HumanFabrication.WY_FABRICATOR.get(), HumanFabrication.USCM_FABRICATOR.get());
    }

    static List<Block> vaporizers() {
        return List.of(HumanVaporizer.VAPORIZER.get(), HumanVaporizer.WY_VAPORIZER.get(), HumanVaporizer.USCM_VAPORIZER.get());
    }
}
