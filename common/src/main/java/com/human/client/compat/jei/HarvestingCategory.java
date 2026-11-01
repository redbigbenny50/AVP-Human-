package com.human.client.compat.jei;

import com.human.client.compat.HarvestRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

/** JEI's "Harvesting" tab - the harvest genes and the substance bucket. Laid out like the Drying tab. */
public final class HarvestingCategory implements IRecipeCategory<HarvestRecipe> {

    public static final RecipeType<HarvestRecipe> TYPE = RecipeType.create("avp_human", "harvesting", HarvestRecipe.class);

    private static final int WIDTH = 150;

    private static final int SLOT_Y = 24;

    private final IGuiHelper gui;

    public HarvestingCategory(IGuiHelper gui) {
        this.gui = gui;
    }

    @Override
    public @NotNull RecipeType<HarvestRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.literal("Harvesting");
    }

    @Override
    public IDrawable getIcon() {
        return gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.BUCKET));
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return 44;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HarvestRecipe recipe, @NotNull IFocusGroup focuses) {
        for (int i = 0; i < recipe.inputs().size() && i < 2; i++) {
            builder.addInputSlot(1 + i * 20, SLOT_Y).setStandardSlotBackground().addItemStack(recipe.inputs().get(i));
        }
        for (int i = 0; i < recipe.outputs().size() && i < 2; i++) {
            builder.addOutputSlot(84 + i * 20, SLOT_Y).setOutputSlotBackground().addItemStack(recipe.outputs().get(i));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, @NotNull HarvestRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addAnimatedRecipeArrow(200).setPosition(46, SLOT_Y - 1);
    }

    @Override
    public void draw(HarvestRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        var lines = font.split(recipe.how(), WIDTH);
        for (int i = 0; i < lines.size() && i < 2; i++) {
            graphics.drawString(font, lines.get(i), 0, i * 10, 0x404040, false);
        }
    }
}
