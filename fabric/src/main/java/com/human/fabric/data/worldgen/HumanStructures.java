package com.human.fabric.data.worldgen;

import com.human.common.gameplay.worldgen.structure.HumanCommunicationsOutpostStructure;
import com.human.common.gameplay.worldgen.structure.HumanMarineCampStructure;
import com.human.common.gameplay.worldgen.structure.HumanMobileLabStructure;
import com.human.common.gameplay.worldgen.structure.HumanMunitionsOutpostStructure;
import com.human.common.gameplay.worldgen.structure.HumanSupplyOutpostBadlandsStructure;
import com.human.common.gameplay.worldgen.structure.HumanSupplyOutpostDesertStructure;
import com.human.common.gameplay.worldgen.structure.HumanSupplyOutpostStructure;
import com.human.common.registry.key.HumanStructureKeys;
import com.human.common.registry.key.HumanStructureTemplatePoolKeys;
import com.human.common.registry.tag.HumanBiomeTags;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiFunction;

public class HumanStructures {

    public static void bootstrap(BootstrapContext<Structure> registry) {
        registry.register(HumanStructureKeys.COMMUNICATIONS_OUTPOST, createHumanCommunicationsOutpostStructure(registry));
        registry.register(HumanStructureKeys.MARINE_CAMP_GRASS, createHumanMarineCampStructure(registry));
        registry.register(HumanStructureKeys.MOBILE_LAB, createHumanMobileLabStructure(registry));
        registry.register(HumanStructureKeys.MUNITIONS_OUTPOST, createHumanMunitionsOutpostStructure(registry));
        registry.register(
            HumanStructureKeys.SUPPLY_OUTPOST_BADLANDS,
            createHumanSupplyOutpostStructure(
                registry,
                HumanBiomeTags.HAS_OUTPOST_SUPPLY_BADLAND,
                HumanStructureTemplatePoolKeys.SUPPLY_OUTPOST_BADLANDS,
                HumanSupplyOutpostBadlandsStructure::new
            )
        );
        registry.register(
            HumanStructureKeys.SUPPLY_OUTPOST_DESERT,
            createHumanSupplyOutpostStructure(
                registry,
                HumanBiomeTags.HAS_OUTPOST_SUPPLY_DESERT,
                HumanStructureTemplatePoolKeys.SUPPLY_OUTPOST_DESERT,
                HumanSupplyOutpostDesertStructure::new
            )
        );
    }

    private static @NotNull HumanCommunicationsOutpostStructure createHumanCommunicationsOutpostStructure(
        BootstrapContext<Structure> registry
    ) {
        var biomeLookup = registry.lookup(Registries.BIOME);
        var templatePoolLookup = registry.lookup(Registries.TEMPLATE_POOL);

        var biomes = biomeLookup.getOrThrow(HumanBiomeTags.HAS_OUTPOST_COMMS);

        var structureSettings = new Structure.StructureSettings.Builder(biomes)
            .generationStep(GenerationStep.Decoration.TOP_LAYER_MODIFICATION)
            .spawnOverrides(Map.of())
            .terrainAdapation(TerrainAdjustment.BEARD_BOX)
            .build();

        var startPool = templatePoolLookup.getOrThrow(HumanStructureTemplatePoolKeys.COMMUNICATIONS_OUTPOST_BOTTOM);

        return new HumanCommunicationsOutpostStructure(structureSettings, startPool);
    }

    private static @NotNull HumanMarineCampStructure createHumanMarineCampStructure(BootstrapContext<Structure> registry) {
        var biomeLookup = registry.lookup(Registries.BIOME);
        var templatePoolLookup = registry.lookup(Registries.TEMPLATE_POOL);

        var biomes = biomeLookup.getOrThrow(HumanBiomeTags.HAS_MARINE_CAMP_GRASS);

        var structureSettings = new Structure.StructureSettings.Builder(biomes)
            .generationStep(GenerationStep.Decoration.TOP_LAYER_MODIFICATION)
            .spawnOverrides(Map.of())
            .terrainAdapation(TerrainAdjustment.BEARD_BOX)
            .build();

        var startPool = templatePoolLookup.getOrThrow(HumanStructureTemplatePoolKeys.MARINE_CAMP_GRASS);

        return new HumanMarineCampStructure(structureSettings, startPool);
    }

    private static @NotNull HumanMobileLabStructure createHumanMobileLabStructure(BootstrapContext<Structure> registry) {
        var biomeLookup = registry.lookup(Registries.BIOME);
        var templatePoolLookup = registry.lookup(Registries.TEMPLATE_POOL);

        var biomes = biomeLookup.getOrThrow(HumanBiomeTags.HAS_MOBILE_LAB);

        var structureSettings = new Structure.StructureSettings.Builder(biomes)
            .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
            .spawnOverrides(Map.of())
            .terrainAdapation(TerrainAdjustment.BEARD_BOX)
            .build();

        var startPool = templatePoolLookup.getOrThrow(HumanStructureTemplatePoolKeys.MOBILE_LAB);

        return new HumanMobileLabStructure(structureSettings, startPool);
    }

    private static @NotNull HumanMunitionsOutpostStructure createHumanMunitionsOutpostStructure(
        BootstrapContext<Structure> registry
    ) {
        var biomeLookup = registry.lookup(Registries.BIOME);
        var templatePoolLookup = registry.lookup(Registries.TEMPLATE_POOL);

        var biomes = biomeLookup.getOrThrow(HumanBiomeTags.HAS_OUTPOST_MUNITION);

        var structureSettings = new Structure.StructureSettings.Builder(biomes)
            .generationStep(GenerationStep.Decoration.TOP_LAYER_MODIFICATION)
            .spawnOverrides(Map.of())
            .terrainAdapation(TerrainAdjustment.BEARD_BOX)
            .build();

        var startPool = templatePoolLookup.getOrThrow(HumanStructureTemplatePoolKeys.MUNITIONS_OUTPOST);

        return new HumanMunitionsOutpostStructure(structureSettings, startPool);
    }

    private static @NotNull HumanSupplyOutpostStructure createHumanSupplyOutpostStructure(
        BootstrapContext<Structure> registry,
        net.minecraft.tags.TagKey<Biome> biomeTag,
        ResourceKey<StructureTemplatePool> templatePoolResourceKey,
        BiFunction<Structure.StructureSettings, Holder.Reference<StructureTemplatePool>, HumanSupplyOutpostStructure> structureFactory
    ) {
        var biomeLookup = registry.lookup(Registries.BIOME);
        var templatePoolLookup = registry.lookup(Registries.TEMPLATE_POOL);

        // [stated] Oct 28 - biomes come from the structure's biome tag (vanilla biomes + the matching "c" tag).
        var biomesHolder = biomeLookup.getOrThrow(biomeTag);

        var structureSettings = new Structure.StructureSettings.Builder(biomesHolder)
            .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
            .spawnOverrides(Map.of())
            .terrainAdapation(TerrainAdjustment.BEARD_BOX)
            .build();

        var startPool = templatePoolLookup.getOrThrow(templatePoolResourceKey);

        return structureFactory.apply(structureSettings, startPool);
    }
}
