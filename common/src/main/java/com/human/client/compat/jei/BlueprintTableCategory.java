package com.human.client.compat.jei;

import com.human.client.compat.BlueprintTableRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * JEI's blueprint table tab: the item (or chip) and its silicon on the left, the chip(s) on the right, the input and
 * output cycling IN STEP (a focus link), so the chip shown is always the one for the item shown. See
 * {@link BlueprintTableRecipe}.
 */
public final class BlueprintTableCategory implements IRecipeCategory<BlueprintTableRecipe> {

    public static final RecipeType<BlueprintTableRecipe> TYPE = RecipeType.create(
        "avp_human",
        "blueprint_table",
        BlueprintTableRecipe.class
    );

    private final IGuiHelper gui;

    public BlueprintTableCategory(IGuiHelper gui) {
        this.gui = gui;
    }

    @Override
    public @NotNull RecipeType<BlueprintTableRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return BlueprintTableRecipe.table().getHoverName();
    }

    @Override
    public IDrawable getIcon() {
        return gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, BlueprintTableRecipe.table());
    }

    @Override
    public int getWidth() {
        return 104;
    }

    @Override
    public int getHeight() {
        return 26;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BlueprintTableRecipe recipe, @NotNull IFocusGroup focuses) {
        var input = builder.addInputSlot(1, 5).setStandardSlotBackground().addItemStacks(recipe.inputs());
        builder.addInputSlot(21, 5).setStandardSlotBackground().addItemStack(recipe.siliconStack());
        var output = builder.addOutputSlot(84, 5).setOutputSlotBackground().addItemStacks(recipe.outputs());
        builder.createFocusLink(input, output);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, @NotNull BlueprintTableRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addAnimatedRecipeArrow(40).setPosition(46, 4);
    }
}
