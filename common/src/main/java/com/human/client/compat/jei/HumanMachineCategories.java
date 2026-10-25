package com.human.client.compat.jei;

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
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * JEI tabs for every avp_human machine that has its own recipes.
 * <p>
 * [stated] Oct 24 - "Jei seems to not be showing all recipes. For example how to make cryofuel". The plugin only ever
 * registered the industrial furnace, so nothing made in the grinder, press, freezer, hydrator, fabricator or vaporizer
 * could be looked up at all - cryofluid (pressed from cryogel) among them.
 * </p>
 * <p>
 * Every tab: inputs on the left, an animated arrow timed to the machine, outputs on the right; fluids are drawn as
 * fluid slots, so pressing R or U on a fluid (or its bucket) finds the recipe. Amounts are given in the platform's own
 * fluid units (millibuckets on NeoForge, droplets on Fabric) through {@code bucketVolume}, from the recipes'
 * millibuckets.
 * </p>
 */
public final class HumanMachineCategories {

    public static final RecipeType<GrinderRecipe> GRINDING = RecipeType.create("avp_human", "grinding", GrinderRecipe.class);

    public static final RecipeType<PressRecipe> PRESSING = RecipeType.create("avp_human", "pressing", PressRecipe.class);

    public static final RecipeType<FreezerRecipe> FREEZING = RecipeType.create("avp_human", "freezing", FreezerRecipe.class);

    public static final RecipeType<HydratorRecipe> HYDRATING = RecipeType.create("avp_human", "hydrating", HydratorRecipe.class);

    public static final RecipeType<FabricatingRecipe> FABRICATING = RecipeType.create(
        "avp_human",
        "fabricating",
        FabricatingRecipe.class
    );

    public static final RecipeType<VaporizingRecipe> VAPORIZING = RecipeType.create(
        "avp_human",
        "vaporizing",
        VaporizingRecipe.class
    );

    private static final int SLOT = 18;

    private HumanMachineCategories() {}

    /** Shared frame: title and icon from the machine block, an arrow timed to the recipe. */
    private abstract static class Machine<T> implements IRecipeCategory<T> {

        protected final IGuiHelper gui;

        protected final long bucketVolume;

        private final RecipeType<T> type;

        private final Block icon;

        private final int width;

        private final int height;

        Machine(IGuiHelper gui, long bucketVolume, RecipeType<T> type, Block icon, int width, int height) {
            this.gui = gui;
            this.bucketVolume = bucketVolume;
            this.type = type;
            this.icon = icon;
            this.width = width;
            this.height = height;
        }

        @Override
        public @NotNull RecipeType<T> getRecipeType() {
            return type;
        }

        @Override
        public @NotNull Component getTitle() {
            return icon.getName();
        }

        @Override
        public IDrawable getIcon() {
            return gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(icon));
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        /** The recipe's processing time in ticks, for the arrow. */
        abstract int time(T recipe);

        /** Where the arrow sits. */
        abstract int arrowX();

        int arrowY() {
            return (height - 17) / 2;
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, @NotNull T recipe, @NotNull IFocusGroup focuses) {
            builder.addAnimatedRecipeArrow(Math.max(1, time(recipe))).setPosition(arrowX(), arrowY());
        }

        /** An item input showing the stack size the recipe actually takes. */
        void itemInput(IRecipeLayoutBuilder builder, int x, int y, Ingredient ingredient, int count) {
            builder.addInputSlot(x, y).setStandardSlotBackground().addItemStacks(counted(ingredient, count));
        }

        /** A fluid slot holding {@code fluid} in the recipe's millibuckets. */
        IRecipeSlotBuilder fluid(IRecipeLayoutBuilder builder, boolean input, int x, int y, FluidContent fluid) {
            return fluid(builder, input, x, y, fluid.fluid(), fluid.amount());
        }

        IRecipeSlotBuilder fluid(IRecipeLayoutBuilder builder, boolean input, int x, int y, Fluid fluid, int millibuckets) {
            var slot = input ? builder.addInputSlot(x, y) : builder.addOutputSlot(x, y);
            var amount = Math.max(1L, millibuckets * bucketVolume / 1000L);
            return slot.setStandardSlotBackground()
                .setFluidRenderer(Math.max(amount, bucketVolume), false, 16, 16)
                .addFluidStack(fluid, amount);
        }
    }

    static List<ItemStack> counted(Ingredient ingredient, int count) {
        return Arrays.stream(ingredient.getItems()).map(stack -> stack.copyWithCount(Math.max(1, count))).toList();
    }

    /** Grinder: one input, up to nine outputs with their chances. */
    public static final class Grinding extends Machine<GrinderRecipe> {

        public Grinding(IGuiHelper gui, long bucketVolume) {
            super(gui, bucketVolume, GRINDING, HumanBlocks.ELECTRIC_GRINDER.get(), 116, 54);
        }

        @Override
        int time(GrinderRecipe recipe) {
            return recipe.getTime();
        }

        @Override
        int arrowX() {
            return 24;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, GrinderRecipe recipe, @NotNull IFocusGroup focuses) {
            itemInput(builder, 1, 19, recipe.getIngredient(), recipe.getInputCount());

            var results = recipe.getResults();
            for (int i = 0; i < results.size() && i < GrinderRecipe.MAX_RESULTS; i++) {
                var result = results.get(i);
                var percent = Math.round(result.chance() * 100.0F);
                var slot = builder.addOutputSlot(60 + (i % 3) * SLOT, 1 + (i / 3) * SLOT)
                    .setOutputSlotBackground()
                    .addItemStack(result.stack());
                if (percent < 100) {
                    slot.addRichTooltipCallback(
                        (view, tooltip) -> tooltip.add(
                            Component.literal(percent + "% chance").withStyle(ChatFormatting.GOLD)
                        )
                    );
                }
            }
        }
    }

    /** Press: one input, an item and/or a fluid out (cryofluid from cryogel, latex, ...). */
    public static final class Pressing extends Machine<PressRecipe> {

        public Pressing(IGuiHelper gui, long bucketVolume) {
            super(gui, bucketVolume, PRESSING, HumanBlocks.ELECTRIC_PRESS.get(), 104, 26);
        }

        @Override
        int time(PressRecipe recipe) {
            return recipe.getTime();
        }

        @Override
        int arrowX() {
            return 24;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, PressRecipe recipe, @NotNull IFocusGroup focuses) {
            itemInput(builder, 1, 5, recipe.getIngredient(), recipe.getInputCount());

            var x = 60;
            if (!recipe.getResult().isEmpty()) {
                builder.addOutputSlot(x, 5).setOutputSlotBackground().addItemStack(recipe.getResult());
                x += 22;
            }
            if (!recipe.getFluid().isEmpty()) {
                fluid(builder, false, x, 5, recipe.getFluid());
            }
        }
    }

    /** Freezer: an item and/or a fluid in, one item out. */
    public static final class Freezing extends Machine<FreezerRecipe> {

        public Freezing(IGuiHelper gui, long bucketVolume) {
            super(gui, bucketVolume, FREEZING, HumanBlocks.ELECTRIC_FREEZER.get(), 104, 26);
        }

        @Override
        int time(FreezerRecipe recipe) {
            return recipe.getTime();
        }

        @Override
        int arrowX() {
            return 46;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, FreezerRecipe recipe, @NotNull IFocusGroup focuses) {
            var x = 1;
            if (!recipe.getIngredient().isEmpty()) {
                itemInput(builder, x, 5, recipe.getIngredient(), recipe.getInputCount());
                x += 20;
            }
            if (!recipe.getFluid().isEmpty()) {
                fluid(builder, true, x, 5, recipe.getFluid());
            }
            builder.addOutputSlot(84, 5).setOutputSlotBackground().addItemStack(recipe.getResult());
        }
    }

    /** Hydrator: one item plus water, one item out. */
    public static final class Hydrating extends Machine<HydratorRecipe> {

        public Hydrating(IGuiHelper gui, long bucketVolume) {
            super(gui, bucketVolume, HYDRATING, HumanBlocks.ELECTRIC_HYDRATOR.get(), 104, 26);
        }

        @Override
        int time(HydratorRecipe recipe) {
            return recipe.getTime();
        }

        @Override
        int arrowX() {
            return 46;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, HydratorRecipe recipe, @NotNull IFocusGroup focuses) {
            itemInput(builder, 1, 5, recipe.getIngredient(), recipe.getInputCount());
            if (recipe.getWater() > 0) {
                fluid(builder, true, 21, 5, Fluids.WATER, recipe.getWater());
            }
            builder.addOutputSlot(84, 5).setOutputSlotBackground().addItemStack(recipe.getResult());
        }
    }

    /** Fabricator: the part list (3 per row), then the result. */
    public static final class Fabricating extends Machine<FabricatingRecipe> {

        public Fabricating(IGuiHelper gui, long bucketVolume) {
            super(gui, bucketVolume, FABRICATING, HumanFabrication.FABRICATOR.get(), 116, 54);
        }

        @Override
        int time(FabricatingRecipe recipe) {
            return 100;
        }

        @Override
        int arrowX() {
            return 60;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, FabricatingRecipe recipe, @NotNull IFocusGroup focuses) {
            var parts = recipe.parts();
            for (int i = 0; i < parts.size() && i < 9; i++) {
                var part = parts.get(i);
                builder.addInputSlot(1 + (i % 3) * SLOT, 1 + (i / 3) * SLOT)
                    .setStandardSlotBackground()
                    .addItemStacks(counted(part.ingredient(), part.count()));
            }
            var level = net.minecraft.client.Minecraft.getInstance().level;
            var result = level == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
            builder.addOutputSlot(96, 19).setOutputSlotBackground().addItemStack(result);
        }
    }

    /** Vaporizer: one bucket of liquid plus an empty aluminum tank, one filled tank out. */
    public static final class Vaporizing extends Machine<VaporizingRecipe> {

        public Vaporizing(IGuiHelper gui, long bucketVolume) {
            super(gui, bucketVolume, VAPORIZING, HumanVaporizer.VAPORIZER.get(), 104, 26);
        }

        @Override
        int time(VaporizingRecipe recipe) {
            return VaporizingRecipe.TIME_TICKS;
        }

        @Override
        int arrowX() {
            return 46;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, VaporizingRecipe recipe, @NotNull IFocusGroup focuses) {
            fluid(builder, true, 1, 5, recipe.liquid(), FluidContent.BUCKET);
            builder.addInputSlot(21, 5).setStandardSlotBackground().addItemStack(new ItemStack(HumanChemicals.ALUMINUM_TANK.get()));
            builder.addOutputSlot(84, 5).setOutputSlotBackground().addItemStack(new ItemStack(recipe.tank()));
        }
    }
}
