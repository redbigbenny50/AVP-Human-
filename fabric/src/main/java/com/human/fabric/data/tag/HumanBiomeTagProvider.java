package com.human.fabric.data.tag;

import com.human.common.registry.key.HumanBiomeKeys;
import com.human.common.registry.tag.HumanBiomeTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.concurrent.CompletableFuture;

public class HumanBiomeTagProvider extends FabricTagProvider<Biome> {

    public HumanBiomeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, Registries.BIOME, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        getOrCreateTagBuilder(HumanBiomeTags.HAS_MARINE_CAMP_GRASS)
            .add(Biomes.BIRCH_FOREST)
            .add(Biomes.FOREST)
            .add(Biomes.MEADOW)
            .add(Biomes.PLAINS);

        applyStandardMarineBiomes(HumanBiomeTags.HAS_MARINE_PATROLS)
            .addOptionalTag(BiomeTags.IS_SAVANNA);

        getOrCreateTagBuilder(HumanBiomeTags.HAS_MOBILE_LAB)
            .add(Biomes.BADLANDS)
            .add(Biomes.CHERRY_GROVE)
            .add(Biomes.DESERT)
            .add(Biomes.SAVANNA)
            .add(Biomes.STONY_SHORE)
            .add(Biomes.SNOWY_PLAINS);

        getOrCreateTagBuilder(HumanBiomeTags.HAS_OUTPOST_COMMS)
            .add(Biomes.FLOWER_FOREST)
            .add(Biomes.SUNFLOWER_PLAINS)
            .add(Biomes.WINDSWEPT_HILLS)
            .add(Biomes.WINDSWEPT_SAVANNA);

        getOrCreateTagBuilder(HumanBiomeTags.HAS_OUTPOST_MUNITION)
            .add(Biomes.SNOWY_TAIGA)
            .add(Biomes.TAIGA);

        getOrCreateTagBuilder(HumanBiomeTags.HAS_OUTPOST_SUPPLY_BADLAND)
            .add(Biomes.BADLANDS);

        getOrCreateTagBuilder(HumanBiomeTags.HAS_OUTPOST_SUPPLY_DESERT)
            .add(Biomes.DESERT);

        getOrCreateTagBuilder(HumanBiomeTags.HAS_TACTICAL_MARINE_PATROLS)
            .addOptionalTag(BiomeTags.IS_JUNGLE)
            .add(Biomes.DARK_FOREST)
            .add(Biomes.MANGROVE_SWAMP)
            .add(Biomes.SWAMP);

        // [stated] Oct 13 - "space out the biomes": one family per W-Y unit, none shared between them, and dark forest
        // left to the tactical marine. The APE also turns up in avp_alien hive territory (ApePatrolSpawnHandle).
        getOrCreateTagBuilder(HumanBiomeTags.HAS_WY_APE_PATROLS)
            .addOptionalTag(BiomeTags.IS_BADLANDS);
        getOrCreateTagBuilder(HumanBiomeTags.HAS_WY_COMMANDO_PATROLS)
            .addOptionalTag(BiomeTags.IS_TAIGA);
        getOrCreateTagBuilder(HumanBiomeTags.HAS_WY_ELITE_PATROLS)
            .add(Biomes.BIRCH_FOREST)
            .add(Biomes.OLD_GROWTH_BIRCH_FOREST);
        getOrCreateTagBuilder(HumanBiomeTags.HAS_WY_SPEC_OPS_COMMANDO_PATROLS)
            .add(Biomes.SNOWY_PLAINS)
            .add(Biomes.ICE_SPIKES)
            .add(Biomes.SNOWY_SLOPES)
            .add(Biomes.GROVE);
        getOrCreateTagBuilder(HumanBiomeTags.HAS_WY_SPEC_OPS_ELITE_PATROLS)
            .add(Biomes.WINDSWEPT_HILLS)
            .add(Biomes.WINDSWEPT_GRAVELLY_HILLS)
            .add(Biomes.WINDSWEPT_FOREST)
            .add(Biomes.STONY_PEAKS)
            .add(Biomes.JAGGED_PEAKS);

        getOrCreateTagBuilder(HumanBiomeTags.IS_IRRADIATED)
            .addOptional(HumanBiomeKeys.NUKED_BIOME);
    }

    private FabricTagProvider<Biome>.FabricTagBuilder applyStandardMarineBiomes(TagKey<Biome> biomeTag) {
        return getOrCreateTagBuilder(biomeTag)
            .addOptionalTag(BiomeTags.IS_TAIGA)
            .add(Biomes.BADLANDS)
            .add(Biomes.BIRCH_FOREST)
            .add(Biomes.DESERT)
            .add(Biomes.FLOWER_FOREST)
            .add(Biomes.FOREST)
            .add(Biomes.GROVE)
            .add(Biomes.MEADOW)
            .add(Biomes.OLD_GROWTH_BIRCH_FOREST)
            .add(Biomes.PLAINS)
            .add(Biomes.SUNFLOWER_PLAINS)
            .add(Biomes.WINDSWEPT_HILLS)
            .add(Biomes.WINDSWEPT_SAVANNA);
    }
}
