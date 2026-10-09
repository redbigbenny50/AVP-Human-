package com.human.fabric.data;

import com.human.fabric.data.advancement.AdvancementProvider;
import com.human.fabric.data.damage_type.DamageTypeBootstrapper;
import com.human.fabric.data.damage_type.DamageTypeProvider;
import com.human.fabric.data.gene_bonus_data.GeneBonusDataSubProvider;
import com.human.fabric.data.lang.en_us.EnglishLanguageProvider;
import com.human.fabric.data.loot.BlockLootTableProvider;
import com.human.fabric.data.loot.ChestLootTableProvider;
import com.human.fabric.data.loot.VillagerLootTableProvider;
import com.human.fabric.data.model.BlockModelProvider;
import com.human.fabric.data.model.ItemModelProvider;
import com.human.fabric.data.recipe.RecipeProvider;
import com.human.fabric.data.tag.HumanBiomeTagProvider;
import com.human.fabric.data.tag.HumanBlockTagProvider;
import com.human.fabric.data.tag.HumanDamageTypeTagProvider;
import com.human.fabric.data.tag.HumanEnchantmentTagProvider;
import com.human.fabric.data.tag.HumanEntityTypeTagProvider;
import com.human.fabric.data.tag.HumanFluidTagProvider;
import com.human.fabric.data.tag.HumanItemTagProvider;
import com.human.fabric.data.tag.HumanPoiTagProvider;
import com.human.fabric.data.worldgen.HumanBiomeProvider;
import com.human.fabric.data.worldgen.HumanCaveConfigurations;
import com.human.fabric.data.worldgen.HumanCavePlacements;
import com.human.fabric.data.worldgen.HumanOreConfigurations;
import com.human.fabric.data.worldgen.HumanOrePlacements;
import com.human.fabric.data.worldgen.HumanStructureSets;
import com.human.fabric.data.worldgen.HumanStructureTemplatePools;
import com.human.fabric.data.worldgen.HumanStructures;
import com.human.fabric.data.worldgen.HumanWorldGenProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public class HumanDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        var pack = fabricDataGenerator.createPack();
        // Advancement providers
        pack.addProvider(AdvancementProvider::new);

        // Damage Type providers
        pack.addProvider(DamageTypeProvider::new);

        // Language providers
        pack.addProvider(EnglishLanguageProvider::new);

        // Model providers
        pack.addProvider(BlockModelProvider::new);
        pack.addProvider(ItemModelProvider::new);

        // Recipe providers
        pack.addProvider(RecipeProvider::new);

        // Tag providers
        pack.addProvider(HumanBlockTagProvider::new);
        pack.addProvider(HumanBiomeTagProvider::new);
        pack.addProvider(HumanDamageTypeTagProvider::new);
        pack.addProvider(HumanEnchantmentTagProvider::new);
        pack.addProvider(HumanEntityTypeTagProvider::new);
        pack.addProvider(HumanItemTagProvider::new);
        pack.addProvider(HumanPoiTagProvider::new);
        pack.addProvider(HumanFluidTagProvider::new);

        // Loot providers
        pack.addProvider(BlockLootTableProvider::new);
        pack.addProvider(ChestLootTableProvider::new);
        pack.addProvider(VillagerLootTableProvider::new);

        // Worldgen providers
        pack.addProvider(HumanWorldGenProvider::new);
        pack.addProvider(HumanBiomeProvider::new);

        // Custom Providers
        pack.addProvider(GeneBonusDataSubProvider::new);
    }

    @Override
    public void buildRegistry(RegistrySetBuilder registryBuilder) {
        registryBuilder.add(Registries.CONFIGURED_FEATURE, HumanCaveConfigurations::bootstrap);
        registryBuilder.add(Registries.CONFIGURED_FEATURE, HumanOreConfigurations::bootstrap);

        registryBuilder.add(Registries.DAMAGE_TYPE, DamageTypeBootstrapper::bootstrap);

        registryBuilder.add(Registries.PLACED_FEATURE, HumanCavePlacements::bootstrap);
        registryBuilder.add(Registries.PLACED_FEATURE, HumanOrePlacements::bootstrap);

        registryBuilder.add(Registries.STRUCTURE, HumanStructures::bootstrap);
        registryBuilder.add(Registries.STRUCTURE_SET, HumanStructureSets::bootstrap);
        registryBuilder.add(Registries.TEMPLATE_POOL, HumanStructureTemplatePools::bootstrap);
    }
}
