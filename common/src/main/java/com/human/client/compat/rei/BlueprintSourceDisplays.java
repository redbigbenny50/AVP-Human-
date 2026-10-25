package com.human.client.compat.rei;

import com.human.Human;
import com.human.client.compat.BlueprintSources;
import com.human.common.registry.init.item.HumanItems;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** REI's "Blueprint Locations" tab - the same pages as the JEI one (see {@link BlueprintSources}). */
public final class BlueprintSourceDisplays {

    public static final CategoryIdentifier<SourceDisplay> SOURCES = CategoryIdentifier.of(Human.MOD_ID, "blueprint_sources");

    private BlueprintSourceDisplays() {}

    public static final class SourceDisplay extends BasicDisplay {

        private final Component name;

        SourceDisplay(BlueprintSources.Source source) {
            super(List.of(EntryIngredient.of(EntryStacks.of(source.icon()))), outputs(source));
            this.name = source.name();
        }

        private static List<EntryIngredient> outputs(BlueprintSources.Source source) {
            var outputs = new ArrayList<EntryIngredient>();
            for (var entry : source.blueprints().entrySet()) {
                var stack = EntryStacks.of(new ItemStack(entry.getKey()))
                    .tooltip(BlueprintSources.chanceText(entry.getValue()).withStyle(ChatFormatting.GOLD));
                outputs.add(EntryIngredient.of(stack));
            }
            return outputs;
        }

        Component name() {
            return name;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return SOURCES;
        }
    }

    public static final class SourceCategory implements DisplayCategory<SourceDisplay> {

        @Override
        public CategoryIdentifier<? extends SourceDisplay> getCategoryIdentifier() {
            return SOURCES;
        }

        @Override
        public Component getTitle() {
            return Component.literal("Blueprints & Rare Finds");
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(HumanItems.BLUEPRINT_M41A_PULSE_RIFLE.get());
        }

        @Override
        public List<Widget> setupDisplay(SourceDisplay display, Rectangle bounds) {
            var widgets = new ArrayList<Widget>();
            widgets.add(Widgets.createRecipeBase(bounds));

            var left = bounds.getX() + 6;
            widgets.add(
                Widgets.createLabel(new Point(left, bounds.getY() + 6), display.name()).leftAligned().noShadow().color(0x404040, 0xBBBBBB)
            );

            var slotY = bounds.getY() + 22;
            widgets.add(Widgets.createSlot(new Point(left, slotY)).entries(display.getInputEntries().get(0)).markInput());

            var x = left + 26;
            for (var output : display.getOutputEntries()) {
                widgets.add(Widgets.createSlot(new Point(x, slotY)).entries(output).markOutput());
                x += 18;
            }

            return widgets;
        }

        @Override
        public int getDisplayHeight() {
            return 46;
        }

        @Override
        public int getDisplayWidth(SourceDisplay display) {
            return 180;
        }
    }

    public static List<SourceDisplay> all() {
        return BlueprintSources.all().stream().map(SourceDisplay::new).toList();
    }
}
