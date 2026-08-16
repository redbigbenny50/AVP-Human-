package com.human.fabric.data.tag;

import com.human.common.registry.init.block.HumanPlasticBlocks;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class TagProviderUtil {

    static @NotNull Stream<Block> getPlasticBlockStream() {
        var dyeColorPlasticStream = Stream.of(
            HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC,
            HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC_SLAB,
            HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC_STAIRS,
            HumanPlasticBlocks.DYE_COLOR_TO_FRAMED_PLASTIC,
            HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC,
            HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC_SLAB,
            HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC_STAIRS,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_SLAB,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_STAIRS,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_SLAB,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_STAIRS,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_WALL
        )
            .map(Map::values)
            .flatMap(Collection::stream)
            .map(Supplier::get);

        var uniquePlasticStream = Stream.of(
            HumanPlasticBlocks.AISLE_HAZARD_PLASTIC,
            HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_STAIRS,
            HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_SLAB,
            HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_WALL,
            HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC,
            HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_STAIRS,
            HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_SLAB,
            HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_WALL,
            HumanPlasticBlocks.FIRE_HAZARD_PLASTIC,
            HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_STAIRS,
            HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_SLAB,
            HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_WALL,
            HumanPlasticBlocks.HAZARD_PLASTIC,
            HumanPlasticBlocks.HAZARD_PLASTIC_STAIRS,
            HumanPlasticBlocks.HAZARD_PLASTIC_SLAB,
            HumanPlasticBlocks.HAZARD_PLASTIC_WALL,
            HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC,
            HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_STAIRS,
            HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_SLAB,
            HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_WALL,
            HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC,
            HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_STAIRS,
            HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_SLAB,
            HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_WALL,
            HumanPlasticBlocks.SAFETY_PLASTIC,
            HumanPlasticBlocks.SAFETY_PLASTIC_STAIRS,
            HumanPlasticBlocks.SAFETY_PLASTIC_SLAB,
            HumanPlasticBlocks.SAFETY_PLASTIC_WALL,
            HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC,
            HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_STAIRS,
            HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_SLAB,
            HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_WALL
        ).map(Supplier::get);

        return Stream.concat(dyeColorPlasticStream, uniquePlasticStream);
    }
}
