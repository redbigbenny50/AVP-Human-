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
        // [stated] Oct 28 - the structures read their biomes from these tags (they used to carry their own vanilla-only
        // lists, so with Terralith or another biome mod they had almost nowhere to spawn - the badlands outpost worst).
        // Each keeps exactly its old vanilla biomes, plus the shared "c" convention tag for that biome family, which
        // biome mods add their own biomes to. Optional, so a pack without the tag loses nothing.
        getOrCreateTagBuilder(HumanBiomeTags.HAS_MARINE_CAMP_GRASS)
            .add(Biomes.BIRCH_FOREST)
            .add(Biomes.FLOWER_FOREST)
            .add(Biomes.FOREST)
            .add(Biomes.OLD_GROWTH_BIRCH_FOREST)
            .add(Biomes.PLAINS)
            .add(Biomes.SUNFLOWER_PLAINS)
            .add(Biomes.TAIGA)
            .addOptionalTag(common("is_plains"))
            .addOptionalTag(common("is_birch_forest"))
            .addOptionalTag(common("is_flower_forest"));

        applyStandardMarineBiomes(HumanBiomeTags.HAS_MARINE_PATROLS)
            .addOptionalTag(BiomeTags.IS_SAVANNA);

        getOrCreateTagBuilder(HumanBiomeTags.HAS_MOBILE_LAB)
            .add(Biomes.BADLANDS)
            .add(Biomes.DESERT)
            .add(Biomes.CHERRY_GROVE)
            .add(Biomes.ERODED_BADLANDS)
            .add(Biomes.PLAINS)
            .add(Biomes.SAVANNA)
            .add(Biomes.SNOWY_PLAINS)
            .add(Biomes.WINDSWEPT_SAVANNA)
            .addOptionalTag(common("is_badlands"))
            .addOptionalTag(common("is_desert"))
            .addOptionalTag(common("is_plains"))
            .addOptionalTag(common("is_snowy_plains"))
            .addOptionalTag(common("is_savanna"));

        // No convention tag fits meadow + savanna plateau without pulling in badlands and cherry groves, so the
        // communications outpost keeps its two vanilla biomes only.
        getOrCreateTagBuilder(HumanBiomeTags.HAS_OUTPOST_COMMS)
            .add(Biomes.MEADOW)
            .add(Biomes.SAVANNA_PLATEAU);

        getOrCreateTagBuilder(HumanBiomeTags.HAS_OUTPOST_MUNITION)
            .add(Biomes.OLD_GROWTH_PINE_TAIGA)
            .add(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
            .add(Biomes.SNOWY_TAIGA)
            .addOptionalTag(common("is_taiga"));

        getOrCreateTagBuilder(HumanBiomeTags.HAS_OUTPOST_SUPPLY_BADLAND)
            .add(Biomes.BADLANDS)
            .add(Biomes.WOODED_BADLANDS)
            .addOptionalTag(BiomeTags.IS_BADLANDS)
            .addOptionalTag(common("is_badlands"));

        getOrCreateTagBuilder(HumanBiomeTags.HAS_OUTPOST_SUPPLY_DESERT)
            .add(Biomes.DESERT)
            .addOptionalTag(common("is_desert"));

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

    /** A shared "c" convention biome tag (both loaders define the same set in 1.21.1). */
    private static TagKey<Biome> common(String path) {
        return TagKey.create(Registries.BIOME, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", path));
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
