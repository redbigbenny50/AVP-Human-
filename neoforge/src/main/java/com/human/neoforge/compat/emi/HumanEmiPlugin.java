package com.human.neoforge.compat.emi;

import com.human.client.compat.BlueprintSources;
import com.human.client.compat.BlueprintTableRecipe;
import com.human.client.compat.DryingRecipe;
import com.human.client.compat.VaporizingRecipe;
import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.HumanChemicals;
import com.human.common.registry.init.HumanFabrication;
import com.human.common.registry.init.HumanRecipes;
import com.human.common.registry.init.HumanVaporizer;
import com.human.common.registry.init.item.HumanItems;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * avp_human in EMI - its own plugin, so EMI shows our tabs even with no JEI installed.
 * <p>
 * [stated] Oct 24 - "will these jei fixes also work with emi?" EMI only reads JEI plugins when JEI itself is installed
 * beside it, and many packs run EMI alone. This gives EMI the same tabs as JEI and REI - the machines, Blueprint
 * Locations, the Blueprint Table and Drying - from the SAME shared lists (com.human.client.compat), so the three
 * viewers can never disagree. NeoForge only: that is where the build has EMI; Fabric players use REI, or EMI with JEI.
 * </p>
 * <p>
 * Fluid amounts are given in millibuckets, NeoForge's own unit, which is what EMI uses on NeoForge.
 * </p>
 */
@EmiEntrypoint
public final class HumanEmiPlugin implements EmiPlugin {

    private static EmiRecipeCategory category(String id, ItemLike icon) {
        return category(id, icon, null);
    }

    /**
     * A category titled by {@code title}, or by its icon's name when null (the machine's own name). EMI would otherwise
     * look for a lang key built from the id, and show the raw key when there is none.
     */
    private static EmiRecipeCategory category(String id, ItemLike icon, Component title) {
        var stack = EmiStack.of(icon);
        var name = title != null ? title : new ItemStack(icon).getHoverName();
        return new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("avp_human", id), stack, stack) {

            @Override
            public Component getName() {
                return name;
            }
        };
    }

    @Override
    public void register(EmiRegistry registry) {
        var grinding = category("grinding", HumanBlocks.ELECTRIC_GRINDER.get());
        var pressing = category("pressing", HumanBlocks.ELECTRIC_PRESS.get());
        var freezing = category("freezing", HumanBlocks.ELECTRIC_FREEZER.get());
        var hydrating = category("hydrating", HumanBlocks.ELECTRIC_HYDRATOR.get());
        var fabricating = category("fabricating", HumanFabrication.FABRICATOR.get());
        var vaporizing = category("vaporizing", HumanVaporizer.VAPORIZER.get());
        var sources = category(
            "blueprint_sources",
            HumanItems.BLUEPRINT_M41A_PULSE_RIFLE.get(),
            Component.literal("Blueprints & Rare Finds")
        );
        var table = category("blueprint_table", HumanBlocks.BLUEPRINT_BLOCK.get());
        var drying = category("drying", Blocks.HONEY_BLOCK, Component.literal("Drying"));

        try {
            for (var category : List.of(grinding, pressing, freezing, hydrating, fabricating, vaporizing, sources, table, drying)) {
                registry.addCategory(category);
            }
        } catch (RuntimeException exception) {
            // One bad recipe (a data pack's, another mod's) costs only this section, never the whole plugin.
            com.human.Human.LOGGER.warn("avp_human EMI: skipped a section that failed to build", exception);
        }

        registry.addWorkstation(grinding, EmiStack.of(HumanBlocks.ELECTRIC_GRINDER.get()));
        registry.addWorkstation(pressing, EmiStack.of(HumanBlocks.ELECTRIC_PRESS.get()));
        registry.addWorkstation(freezing, EmiStack.of(HumanBlocks.ELECTRIC_FREEZER.get()));
        registry.addWorkstation(hydrating, EmiStack.of(HumanBlocks.ELECTRIC_HYDRATOR.get()));
        registry.addWorkstation(fabricating, EmiStack.of(HumanFabrication.FABRICATOR.get()));
        registry.addWorkstation(fabricating, EmiStack.of(HumanFabrication.WY_FABRICATOR.get()));
        registry.addWorkstation(fabricating, EmiStack.of(HumanFabrication.USCM_FABRICATOR.get()));
        registry.addWorkstation(vaporizing, EmiStack.of(HumanVaporizer.VAPORIZER.get()));
        registry.addWorkstation(vaporizing, EmiStack.of(HumanVaporizer.WY_VAPORIZER.get()));
        registry.addWorkstation(vaporizing, EmiStack.of(HumanVaporizer.USCM_VAPORIZER.get()));
        registry.addWorkstation(table, EmiStack.of(HumanBlocks.BLUEPRINT_BLOCK.get()));
        registry.addWorkstation(drying, EmiStack.of(Blocks.CAULDRON));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(HumanBlocks.ELECTRIC_FURNACE.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMOKING, EmiStack.of(HumanBlocks.ELECTRIC_SMOKER.get()));
        registry.addWorkstation(VanillaEmiRecipeCategories.BLASTING, EmiStack.of(HumanBlocks.ELECTRIC_BLAST_FURNACE.get()));

        var recipes = registry.getRecipeManager();
        var level = Minecraft.getInstance().level;

        try {
            for (var holder : recipes.getAllRecipesFor(HumanRecipes.GRINDING_TYPE.get())) {
                var recipe = holder.value();
                var outputs = new ArrayList<EmiStack>();
                for (var result : recipe.getResults()) {
                    outputs.add(EmiStack.of(result.stack()).setChance(Math.min(1.0F, result.chance())));
                }
                registry.addRecipe(
                    new Page(
                        grinding,
                        holder.id(),
                        List.of(items(recipe.getIngredient(), recipe.getInputCount())),
                        outputs,
                        recipe.getTime(),
                        null
                    )
                );
            }
        } catch (RuntimeException exception) {
            // One bad recipe (a data pack's, another mod's) costs only this section, never the whole plugin.
            com.human.Human.LOGGER.warn("avp_human EMI: skipped a section that failed to build", exception);
        }

        try {
            for (var holder : recipes.getAllRecipesFor(HumanRecipes.PRESSING_TYPE.get())) {
                var recipe = holder.value();
                var outputs = new ArrayList<EmiStack>();
                if (!recipe.getResult().isEmpty()) {
                    outputs.add(EmiStack.of(recipe.getResult()));
                }
                if (!recipe.getFluid().isEmpty()) {
                    outputs.add(fluid(recipe.getFluid()));
                }
                registry.addRecipe(
                    new Page(
                        pressing,
                        holder.id(),
                        List.of(items(recipe.getIngredient(), recipe.getInputCount())),
                        outputs,
                        recipe.getTime(),
                        null
                    )
                );
            }
        } catch (RuntimeException exception) {
            // One bad recipe (a data pack's, another mod's) costs only this section, never the whole plugin.
            com.human.Human.LOGGER.warn("avp_human EMI: skipped a section that failed to build", exception);
        }

        try {
            for (var holder : recipes.getAllRecipesFor(HumanRecipes.FREEZING_TYPE.get())) {
                var recipe = holder.value();
                var inputs = new ArrayList<EmiIngredient>();
                if (!recipe.getIngredient().isEmpty()) {
                    inputs.add(items(recipe.getIngredient(), recipe.getInputCount()));
                }
                if (!recipe.getFluid().isEmpty()) {
                    inputs.add(fluid(recipe.getFluid()));
                }
                registry.addRecipe(
                    new Page(freezing, holder.id(), inputs, List.of(EmiStack.of(recipe.getResult())), recipe.getTime(), null)
                );
            }
        } catch (RuntimeException exception) {
            // One bad recipe (a data pack's, another mod's) costs only this section, never the whole plugin.
            com.human.Human.LOGGER.warn("avp_human EMI: skipped a section that failed to build", exception);
        }

        try {
            for (var holder : recipes.getAllRecipesFor(HumanRecipes.HYDRATING_TYPE.get())) {
                var recipe = holder.value();
                var inputs = new ArrayList<EmiIngredient>();
                inputs.add(items(recipe.getIngredient(), recipe.getInputCount()));
                if (recipe.getWater() > 0) {
                    inputs.add(EmiStack.of(Fluids.WATER, recipe.getWater()));
                }
                registry.addRecipe(
                    new Page(hydrating, holder.id(), inputs, List.of(EmiStack.of(recipe.getResult())), recipe.getTime(), null)
                );
            }
        } catch (RuntimeException exception) {
            // One bad recipe (a data pack's, another mod's) costs only this section, never the whole plugin.
            com.human.Human.LOGGER.warn("avp_human EMI: skipped a section that failed to build", exception);
        }

        try {
            for (var holder : recipes.getAllRecipesFor(HumanFabrication.FABRICATING_TYPE.get())) {
                var recipe = holder.value();
                var inputs = new ArrayList<EmiIngredient>();
                for (var part : recipe.parts()) {
                    inputs.add(items(part.ingredient(), part.count()));
                }
                var result = level == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
                registry.addRecipe(new Page(fabricating, holder.id(), inputs, List.of(EmiStack.of(result)), 100, null));
            }
        } catch (RuntimeException exception) {
            // One bad recipe (a data pack's, another mod's) costs only this section, never the whole plugin.
            com.human.Human.LOGGER.warn("avp_human EMI: skipped a section that failed to build", exception);
        }

        var n = 0;
        try {
            for (var recipe : VaporizingRecipe.all()) {
                registry.addRecipe(
                    new Page(
                        vaporizing,
                        synthetic("vaporizing", n++),
                        List.of(EmiStack.of(recipe.liquid(), FluidContent.BUCKET), EmiStack.of(HumanChemicals.ALUMINUM_TANK.get())),
                        List.of(EmiStack.of(recipe.tank())),
                        VaporizingRecipe.TIME_TICKS,
                        null
                    )
                );
            }
        } catch (RuntimeException exception) {
            // One bad recipe (a data pack's, another mod's) costs only this section, never the whole plugin.
            com.human.Human.LOGGER.warn("avp_human EMI: skipped a section that failed to build", exception);
        }

        n = 0;
        try {
            for (var source : BlueprintSources.all()) {
                var outputs = new ArrayList<EmiStack>();
                source.blueprints().forEach((item, chance) -> outputs.add(EmiStack.of(item).setChance(Math.min(1.0F, chance))));
                registry.addRecipe(
                    new Page(sources, synthetic("blueprint_sources", n++), List.of(EmiStack.of(source.icon())), outputs, 0, source.name())
                );
            }
        } catch (RuntimeException exception) {
            // One bad recipe (a data pack's, another mod's) costs only this section, never the whole plugin.
            com.human.Human.LOGGER.warn("avp_human EMI: skipped a section that failed to build", exception);
        }

        n = 0;
        try {
            for (var recipe : BlueprintTableRecipe.all()) {
                var inputs = EmiIngredient.of(recipe.inputs().stream().map(EmiStack::of).toList());
                var outputs = recipe.outputs().stream().map(EmiStack::of).toList();
                // One output slot cycling every chip, on the same clock as the input slot, so the chip shown always
                // matches
                // the item shown; every chip is still an output, so looking any of them up finds this page.
                var page = new Page(
                    table,
                    synthetic("blueprint_table", n++),
                    List.of(inputs, EmiStack.of(recipe.siliconStack())),
                    outputs,
                    40,
                    Component.literal(recipe.silicon() == 1 ? "Item + 1 silicon = its blueprint chip" : "Chip + 2 silicon = 2 of that chip")
                );
                page.cycleOutputs = true;
                registry.addRecipe(page);
            }
        } catch (RuntimeException exception) {
            // One bad recipe (a data pack's, another mod's) costs only this section, never the whole plugin.
            com.human.Human.LOGGER.warn("avp_human EMI: skipped a section that failed to build", exception);
        }

        n = 0;
        try {
            for (var recipe : DryingRecipe.all()) {
                var inputs = new ArrayList<EmiIngredient>();
                inputs.add(EmiStack.of(recipe.liquid(), FluidContent.BUCKET));
                if (!recipe.vessel().isEmpty()) {
                    inputs.add(EmiStack.of(recipe.vessel()));
                }
                registry.addRecipe(
                    new Page(drying, synthetic("drying", n++), inputs, List.of(EmiStack.of(recipe.result())), 200, recipe.how())
                );
            }
        } catch (RuntimeException exception) {
            // One bad recipe (a data pack's, another mod's) costs only this section, never the whole plugin.
            com.human.Human.LOGGER.warn("avp_human EMI: skipped a section that failed to build", exception);
        }
    }

    private static ResourceLocation synthetic(String kind, int index) {
        return ResourceLocation.fromNamespaceAndPath("avp_human", "/" + kind + "/" + index);
    }

    private static EmiIngredient items(Ingredient ingredient, int count) {
        return EmiIngredient.of(
            Arrays.stream(ingredient.getItems()).map(stack -> EmiStack.of(stack.copyWithCount(Math.max(1, count)))).toList()
        );
    }

    private static EmiStack fluid(FluidContent content) {
        return EmiStack.of(content.fluid(), content.amount());
    }

    /** One EMI page: inputs left, arrow, outputs right (rows of three), an optional line of text on top. */
    private static final class Page extends BasicEmiRecipe {

        private final int time;

        private final Component note;

        /** Show the outputs as ONE cycling slot instead of a slot each. */
        boolean cycleOutputs;

        Page(
            EmiRecipeCategory category,
            ResourceLocation id,
            List<? extends EmiIngredient> inputs,
            List<EmiStack> outputs,
            int time,
            Component note
        ) {
            super(category, id, 150, note == null ? 56 : 68);
            this.inputs.addAll(inputs);
            this.outputs.addAll(outputs);
            this.time = time;
            this.note = note;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            var top = 0;
            if (note != null) {
                var font = Minecraft.getInstance().font;
                var lines = font.split(note, 150);
                for (int i = 0; i < lines.size() && i < 1; i++) {
                    widgets.addText(lines.get(i), 0, 0, 0x404040, false);
                }
                top = 12;
            }

            for (int i = 0; i < inputs.size(); i++) {
                widgets.addSlot(inputs.get(i), (i % 3) * 18, top + (i / 3) * 18);
            }

            if (time > 0) {
                widgets.addFillingArrow(58, top + 18, time * 50);
            } else {
                widgets.addTexture(dev.emi.emi.api.render.EmiTexture.EMPTY_ARROW, 58, top + 18);
            }

            if (cycleOutputs) {
                widgets.addSlot(EmiIngredient.of(outputs), 88, top + 18).recipeContext(this);
                return;
            }

            for (int i = 0; i < outputs.size(); i++) {
                widgets.addSlot(outputs.get(i), 88 + (i % 3) * 18, top + (i / 3) * 18).recipeContext(this);
            }
        }
    }
}
