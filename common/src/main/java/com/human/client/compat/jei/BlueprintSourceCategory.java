package com.human.client.compat.jei;

import com.human.client.compat.BlueprintSources;
import com.human.common.registry.init.item.HumanItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * JEI's "Blueprint Locations" tab: one page per chest that can hold blueprints, with the chance per chest on each.
 * Every blueprint is an OUTPUT here, so looking up how to get one (R) lands on the places it is found. See
 * {@link BlueprintSources}.
 */
public final class BlueprintSourceCategory implements IRecipeCategory<BlueprintSources.Source> {

    public static final RecipeType<BlueprintSources.Source> TYPE = RecipeType.create(
        "avp_human",
        "blueprint_sources",
        BlueprintSources.Source.class
    );

    private static final int WIDTH = 162;

    private static final int HEIGHT = 46;

    private static final int SLOT_Y = 26;

    private final IGuiHelper gui;

    public BlueprintSourceCategory(IGuiHelper gui) {
        this.gui = gui;
    }

    @Override
    public @NotNull RecipeType<BlueprintSources.Source> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.literal("Blueprints & Rare Finds");
    }

    @Override
    public IDrawable getIcon() {
        return gui.createDrawableIngredient(
            VanillaTypes.ITEM_STACK,
            new ItemStack(HumanItems.BLUEPRINT_M41A_PULSE_RIFLE.get())
        );
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BlueprintSources.Source source, @NotNull IFocusGroup focuses) {
        builder.addInputSlot(1, SLOT_Y).setStandardSlotBackground().addItemStack(source.icon());

        var x = 28;
        for (var entry : source.blueprints().entrySet()) {
            if (x + 18 > WIDTH) {
                break;
            }
            var chance = BlueprintSources.chanceText(entry.getValue()).withStyle(ChatFormatting.GOLD);
            builder.addOutputSlot(x, SLOT_Y)
                .setOutputSlotBackground()
                .addItemStack(new ItemStack(entry.getKey()))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(chance));
            x += 18;
        }
    }

    @Override
    public void draw(
        BlueprintSources.Source source,
        IRecipeSlotsView slots,
        GuiGraphics graphics,
        double mouseX,
        double mouseY
    ) {
        var font = Minecraft.getInstance().font;
        var lines = font.split(source.name(), WIDTH);
        for (int i = 0; i < lines.size() && i < 2; i++) {
            graphics.drawString(font, lines.get(i), 0, i * 10, 0x404040, false);
        }
    }
}
