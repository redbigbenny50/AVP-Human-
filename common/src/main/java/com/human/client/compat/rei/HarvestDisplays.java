package com.human.client.compat.rei;

import com.human.Human;
import com.human.client.compat.HarvestRecipe;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** REI's "Harvesting" tab - the harvest genes and the substance bucket (see {@link HarvestRecipe}). */
public final class HarvestDisplays {

    public static final CategoryIdentifier<HumanMachineDisplays.MachineDisplay> HARVESTING = CategoryIdentifier.of(
        Human.MOD_ID,
        "harvesting"
    );

    private HarvestDisplays() {}

    public static HumanMachineDisplays.MachineDisplay display(HarvestRecipe recipe) {
        var inputs = new ArrayList<EntryIngredient>();
        for (var stack : recipe.inputs()) {
            inputs.add(EntryIngredient.of(EntryStacks.of(stack).tooltip(recipe.how())));
        }
        var outputs = new ArrayList<EntryIngredient>();
        for (var stack : recipe.outputs()) {
            outputs.add(EntryIngredient.of(EntryStacks.of(stack).tooltip(recipe.how())));
        }
        return new HumanMachineDisplays.MachineDisplay(HARVESTING, inputs, outputs);
    }

    /** Same layout as the machine tabs, with its own title and a bucket for an icon. */
    public static final class HarvestCategory implements DisplayCategory<HumanMachineDisplays.MachineDisplay> {

        @Override
        public CategoryIdentifier<? extends HumanMachineDisplays.MachineDisplay> getCategoryIdentifier() {
            return HARVESTING;
        }

        @Override
        public Component getTitle() {
            return Component.literal("Harvesting");
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(Items.BUCKET);
        }

        @Override
        public List<Widget> setupDisplay(HumanMachineDisplays.MachineDisplay display, Rectangle bounds) {
            var widgets = new ArrayList<Widget>();
            widgets.add(Widgets.createRecipeBase(bounds));

            var inputs = display.getInputEntries();
            var outputs = display.getOutputEntries();
            var top = bounds.getCenterY() - 9;
            var left = bounds.getCenterX() - 62;

            for (int i = 0; i < inputs.size(); i++) {
                widgets.add(Widgets.createSlot(new Point(left + i * 18, top)).entries(inputs.get(i)).markInput());
            }

            widgets.add(Widgets.createArrow(new Point(left + 56, bounds.getCenterY() - 8)).animationDurationTicks(100));

            for (int i = 0; i < outputs.size(); i++) {
                widgets.add(Widgets.createSlot(new Point(left + 84 + i * 18, top)).entries(outputs.get(i)).markOutput());
            }

            return widgets;
        }

        @Override
        public int getDisplayHeight() {
            return 66;
        }

        @Override
        public int getDisplayWidth(HumanMachineDisplays.MachineDisplay display) {
            return 150;
        }
    }
}
