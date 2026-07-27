package com.human.fabric.data.tag;

import com.blib.api.common.tag.v1.BLibEntityTypeTags;
import com.human.common.registry.init.HumanEntityTypes;
import com.human.common.registry.tag.HumanEntityTypeTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.concurrent.CompletableFuture;

public class HumanEntityTypeTagProvider extends FabricTagProvider.EntityTypeTagProvider {

    public HumanEntityTypeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        addHatedByMarines();
        addHumanoids();
        addRadiationResistant();

        // Compatibility
        addHatedByXenomorphs();
        addHosts();
    }

    private void addHatedByMarines() {
        getOrCreateTagBuilder(HumanEntityTypeTags.HATED_BY_MARINES)
            .addOptionalTag(EntityTypeTags.ILLAGER)
            .addOptionalTag(EntityTypeTags.ILLAGER_FRIENDS)
            .addOptionalTag(EntityTypeTags.UNDEAD)
            .addOptionalTag(EntityTypeTags.WITHER_FRIENDS)
            .add(EntityType.CAVE_SPIDER)
            .add(EntityType.CREEPER)
            .add(EntityType.BLAZE)
            .add(EntityType.BREEZE)
            .add(EntityType.ELDER_GUARDIAN)
            .add(EntityType.GHAST)
            .add(EntityType.GUARDIAN)
            .add(EntityType.HOGLIN)
            .add(EntityType.MAGMA_CUBE)
            .add(EntityType.PIGLIN)
            .add(EntityType.PIGLIN_BRUTE)
            .add(EntityType.SHULKER)
            .add(EntityType.SILVERFISH)
            .add(EntityType.SLIME)
            .add(EntityType.SPIDER)
            .add(EntityType.VEX)
            .add(EntityType.WARDEN);
    }

    private void addHumanoids() {
        getOrCreateTagBuilder(BLibEntityTypeTags.HUMANOIDS)
            .add(
                HumanEntityTypes.MARINE.get()
            );
    }

    private void addRadiationResistant() {
        getOrCreateTagBuilder(HumanEntityTypeTags.RADIATION_RESISTANT)
            .addOptionalTag(EntityTypeTags.UNDEAD)
            .add(EntityType.CREEPER);
    }

    private void addHatedByXenomorphs() {
        getOrCreateTagBuilder(foreignEntityTypeTag("avp_alien", "hated_by_xenomorphs"))
            .add(HumanEntityTypes.MARINE.get());
    }

    private void addHosts() {
        getOrCreateTagBuilder(foreignEntityTypeTag("avp_alien", "hosts"))
            .add(HumanEntityTypes.MARINE.get());
    }

    /**
     * Builds a TagKey owned by ANOTHER mod from its raw id, instead of importing that mod's tag class.
     * <p>
     * Datagen only ever needs the tag's IDENTITY to write a JSON file, never the foreign class - and importing it made
     * this provider fail with NoClassDefFoundError whenever the sibling mod was absent from the DATAGEN runtime
     * classpath (it is compile-only here). Raw ids keep these compatibility tags working no matter which siblings are
     * present, and an unused tag file for an absent mod is simply inert data.
     * </p>
     */
    private static TagKey<EntityType<?>> foreignEntityTypeTag(String namespace, String path) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
