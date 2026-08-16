package com.human.common.registry.init.creative_mode_tab.initializer;

import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.block.HumanIndustrialConcreteBlocks;
import com.human.common.registry.init.block.HumanIndustrialGlassBlocks;
import com.human.common.registry.init.block.HumanPaddingBlocks;
import com.human.common.registry.init.block.HumanPlasticBlocks;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;

import java.util.Arrays;
import java.util.function.Consumer;

public class ColoredBlocksCreativeModeTabInitializer {

    public static final Consumer<CreativeModeTab.Output> OUTPUT_CONSUMER = output -> {
        Arrays.stream(DyeColor.values()).forEach(dyeColor -> {
            output.accept(HumanBlocks.DYE_COLOR_TO_CONCRETE_STAIRS.get(dyeColor).get());
            output.accept(HumanBlocks.DYE_COLOR_TO_CONCRETE_SLAB.get(dyeColor).get());

            output.accept(HumanIndustrialConcreteBlocks.DYE_COLOR_TO_INDUSTRIAL_CONCRETE.get(dyeColor).get());
            output.accept(HumanIndustrialConcreteBlocks.DYE_COLOR_TO_INDUSTRIAL_CONCRETE_STAIRS.get(dyeColor).get());
            output.accept(HumanIndustrialConcreteBlocks.DYE_COLOR_TO_INDUSTRIAL_CONCRETE_SLAB.get(dyeColor).get());
            output.accept(HumanIndustrialConcreteBlocks.DYE_COLOR_TO_INDUSTRIAL_CONCRETE_WALL.get(dyeColor).get());
        });

        CreativeModeTabUtil.accept(output, HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS);
        Arrays.stream(DyeColor.values())
            .forEach(dyeColor -> output.accept(HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS.get(dyeColor).get()));

        Arrays.stream(DyeColor.values()).forEach(dyeColor -> {
            output.accept(HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_STAIRS.get(dyeColor).get());
            output.accept(HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_SLAB.get(dyeColor).get());
            output.accept(HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_DOOR.get(dyeColor).get());
            output.accept(HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_TRAP_DOOR.get(dyeColor).get());
            output.accept(HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_WALL.get(dyeColor).get());
        });
        CreativeModeTabUtil.accept(output, HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_PANE);
        Arrays.stream(DyeColor.values())
            .forEach(dyeColor -> output.accept(HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_PANE.get(dyeColor).get()));

        Arrays.stream(DyeColor.values()).forEach(dyeColor -> {
            output.accept(HumanPaddingBlocks.DYE_COLOR_TO_PADDING.get(dyeColor).get());
            output.accept(HumanPaddingBlocks.DYE_COLOR_TO_PADDING_STAIRS.get(dyeColor).get());
            output.accept(HumanPaddingBlocks.DYE_COLOR_TO_PADDING_SLAB.get(dyeColor).get());
        });

        Arrays.stream(DyeColor.values()).forEach(dyeColor -> {
            output.accept(HumanPaddingBlocks.DYE_COLOR_TO_PANEL_PADDING.get(dyeColor).get());
            output.accept(HumanPaddingBlocks.DYE_COLOR_TO_PANEL_PADDING_STAIRS.get(dyeColor).get());
            output.accept(HumanPaddingBlocks.DYE_COLOR_TO_PANEL_PADDING_SLAB.get(dyeColor).get());
        });

        Arrays.stream(DyeColor.values()).forEach(dyeColor -> {
            output.accept(HumanPaddingBlocks.DYE_COLOR_TO_PIPE_PADDING.get(dyeColor).get());
            output.accept(HumanPaddingBlocks.DYE_COLOR_TO_PIPE_PADDING_STAIRS.get(dyeColor).get());
            output.accept(HumanPaddingBlocks.DYE_COLOR_TO_PIPE_PADDING_SLAB.get(dyeColor).get());
        });

        Arrays.stream(DyeColor.values()).forEach(dyeColor -> {
            output.accept(HumanPaddingBlocks.DYE_COLOR_TO_TILE_PADDING.get(dyeColor).get());
            output.accept(HumanPaddingBlocks.DYE_COLOR_TO_TILE_PADDING_STAIRS.get(dyeColor).get());
            output.accept(HumanPaddingBlocks.DYE_COLOR_TO_TILE_PADDING_SLAB.get(dyeColor).get());
        });

        Arrays.stream(DyeColor.values()).forEach(dyeColor -> {
            output.accept(HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(dyeColor).get());
            output.accept(HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_STAIRS.get(dyeColor).get());
            output.accept(HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_SLAB.get(dyeColor).get());
            output.accept(HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_WALL.get(dyeColor).get());
        });

        Arrays.stream(DyeColor.values()).forEach(dyeColor -> {
            output.accept(HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC.get(dyeColor).get());
            output.accept(HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC_STAIRS.get(dyeColor).get());
            output.accept(HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC_SLAB.get(dyeColor).get());
        });

        Arrays.stream(DyeColor.values())
            .forEach(dyeColor -> output.accept(HumanPlasticBlocks.DYE_COLOR_TO_FRAMED_PLASTIC.get(dyeColor).get()));

        Arrays.stream(DyeColor.values()).forEach(dyeColor -> {
            output.accept(HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE.get(dyeColor).get());
            output.accept(HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_STAIRS.get(dyeColor).get());
            output.accept(HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_SLAB.get(dyeColor).get());
        });

        Arrays.stream(DyeColor.values()).forEach(dyeColor -> {
            output.accept(HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC.get(dyeColor).get());
            output.accept(HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC_STAIRS.get(dyeColor).get());
            output.accept(HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC_SLAB.get(dyeColor).get());
        });

        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.AISLE_HAZARD_PLASTIC);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_STAIRS);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_SLAB);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_WALL);

        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_STAIRS);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_SLAB);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_WALL);

        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.FIRE_HAZARD_PLASTIC);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_STAIRS);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_SLAB);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_WALL);

        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.HAZARD_PLASTIC);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.HAZARD_PLASTIC_STAIRS);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.HAZARD_PLASTIC_SLAB);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.HAZARD_PLASTIC_WALL);

        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_STAIRS);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_SLAB);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_WALL);

        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_STAIRS);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_SLAB);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_WALL);

        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.SAFETY_PLASTIC);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.SAFETY_PLASTIC_STAIRS);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.SAFETY_PLASTIC_SLAB);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.SAFETY_PLASTIC_WALL);

        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_STAIRS);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_SLAB);
        CreativeModeTabUtil.accept(output, HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_WALL);
    };
}
