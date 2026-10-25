package com.human.client.compat.jei;

import com.human.client.compat.DryingRecipe;
import com.human.common.gameplay.fluid.FluidContent;
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

/** JEI's "Drying" tab - liquids that harden on their own. See {@link DryingRecipe}. */
public final class DryingCategory implements IRecipeCategory<DryingRecipe> {

    public static final RecipeType<DryingRecipe> TYPE = RecipeType.create("avp_human", "drying", DryingRecipe.class);

    private static final int WIDTH = 150;

    private static final int SLOT_Y = 24;

    private final IGuiHelper gui;

    private final long bucketVolume;

    public DryingCategory(IGuiHelper gui, long bucketVolume) {
        this.gui = gui;
        this.bucketVolume = bucketVolume;
    }

    @Override
    public @NotNull RecipeType<DryingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.literal("Drying");
    }

    @Override
    public IDrawable getIcon() {
        return gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.HONEY_BLOCK));
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
    public void setRecipe(IRecipeLayoutBuilder builder, DryingRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addInputSlot(1, SLOT_Y)
            .setStandardSlotBackground()
            .setFluidRenderer(bucketVolume, false, 16, 16)
            .addFluidStack(recipe.liquid(), (long) FluidContent.BUCKET * bucketVolume / 1000L);
        if (!recipe.vessel().isEmpty()) {
            builder.addInputSlot(21, SLOT_Y).setStandardSlotBackground().addItemStack(recipe.vessel());
        }
        builder.addOutputSlot(84, SLOT_Y).setOutputSlotBackground().addItemStack(recipe.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, @NotNull DryingRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addAnimatedRecipeArrow(200).setPosition(46, SLOT_Y - 1);
    }

    @Override
    public void draw(DryingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        var lines = font.split(recipe.how(), WIDTH);
        for (int i = 0; i < lines.size() && i < 2; i++) {
            graphics.drawString(font, lines.get(i), 0, i * 10, 0x404040, false);
        }
    }
}
