package com.human.fabric.data.lang.en_us;

import com.human.common.registry.init.HumanMobEffects;
import com.human.fabric.data.lang.en_us.provider.EnUsAdvancementProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsBiomeTagProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsBlockProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsBlockTagProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsConfigProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsCreativeModeTabProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsDamageTypeTagProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsEnchantmentTagProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsEntityProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsEntityTypeTagProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsGeneProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsItemProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsItemTagProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsKeybindProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsSoundEventProvider;
import com.human.fabric.data.lang.en_us.provider.EnUsTooltipProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

public class EnglishLanguageProvider extends FabricLanguageProvider {

    public EnglishLanguageProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, "en_us", registryLookup);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder builder) {
        // Advancements
        EnUsAdvancementProvider.CONSUMER.accept(builder);

        // Blocks
        EnUsBlockProvider.CONSUMER.accept(builder);
        com.human.fabric.data.lang.en_us.provider.EnUsCrateProvider.CONSUMER.accept(builder);
        com.human.fabric.data.lang.en_us.provider.EnUsFabricationProvider.CONSUMER.accept(builder);
        com.human.fabric.data.lang.en_us.provider.EnUsChemicalsProvider.CONSUMER.accept(builder);
        com.human.fabric.data.lang.en_us.provider.EnUsQuarryProvider.CONSUMER.accept(builder);

        // Configs
        EnUsConfigProvider.CONSUMER.accept(builder);

        // Containers
        builder.add("container.ammo_chest", "Ammo Chest");
        builder.add("avp.industrialfurnace.displayName", "Industrial Furnace");
        builder.add("container.lead_chest", "Lead Chest");

        // Creative Mode Tabs
        EnUsCreativeModeTabProvider.CONSUMER.accept(builder);

        // Biomes
        // Oct 14 FIX - the nuked biome never had a name: minimaps and the F3 screen showed the raw key
        // "biome.avp_human.nuked_biome". Built from the biome's own key, so it can never drift from the real id.
        var nukedBiome = com.human.common.registry.key.HumanBiomeKeys.NUKED_BIOME.location();
        builder.add("biome." + nukedBiome.getNamespace() + "." + nukedBiome.getPath(), "Nuclear Wasteland");

        // Death messages
        builder.add("death.attack.bullet", "%1$s was shot to death");
        builder.add("death.attack.radiation", "%1$s surrendered to radiation");
        builder.add("death.attack.razor_wire", "%1$s got tangled in razor wire");
        builder.add("death.attack.cryofluid", "%1$s froze solid in cryofluid");
        // The vapors. The plain key is used with or without a shooter, so it names only the victim.
        builder.add("death.attack.cryo_vapor", "%1$s froze in a cloud of cryomist");
        builder.add("death.attack.cryo_vapor.player", "%1$s froze in cryomist while fighting %2$s");
        builder.add("death.attack.cryo_vapor.item", "%1$s was frozen by %2$s using %3$s");
        builder.add("death.attack.infernium_vapor", "%1$s was burned alive by infernium vapor");
        builder.add("death.attack.infernium_vapor.player", "%1$s was burned alive by infernium vapor while fighting %2$s");
        builder.add("death.attack.infernium_vapor.item", "%1$s was burned alive by %2$s using %3$s");
        builder.add("death.attack.acid_vapor", "%1$s dissolved in acidic vapor");
        builder.add("death.attack.acid_vapor.player", "%1$s dissolved in acidic vapor while fighting %2$s");
        builder.add("death.attack.acid_vapor.item", "%1$s was dissolved by %2$s using %3$s");

        // Entities
        EnUsEntityProvider.CONSUMER.accept(builder);

        // Genes
        EnUsGeneProvider.CONSUMER.accept(builder);

        // Items
        EnUsItemProvider.CONSUMER.accept(builder);

        // Keybinds
        EnUsKeybindProvider.CONSUMER.accept(builder);

        // Misc.
        builder.add("display.avp.low_ammunition_warning", "Low Ammo");
        builder.add("display.avp.no_ammunition_warning", "Out of Ammo");

        // Mob Effects
        builder.add(HumanMobEffects.getRadiationHolder().value(), "Radiation");
        builder.add(HumanMobEffects.getFrozenSolidHolder().value(), "Frozen Solid");

        // Sounds
        EnUsSoundEventProvider.CONSUMER.accept(builder);

        // Tags
        EnUsBiomeTagProvider.CONSUMER.accept(builder);
        EnUsBlockTagProvider.CONSUMER.accept(builder);
        EnUsDamageTypeTagProvider.CONSUMER.accept(builder);
        EnUsEnchantmentTagProvider.CONSUMER.accept(builder);
        EnUsEntityTypeTagProvider.CONSUMER.accept(builder);
        EnUsItemTagProvider.CONSUMER.accept(builder);

        // Tooltips
        EnUsTooltipProvider.CONSUMER.accept(builder);

        // Villagers
        builder.add("entity.minecraft.villager.commissary", "Commissary Villager");
        builder.add("entity.minecraft.villager.avp.commissary", "Commissary Villager");
        // NeoForge names a modded profession with its namespace: entity.minecraft.villager.<namespace>.<path>.
        // Fabric uses vanilla's path-only key above. Without this the NeoForge trade screen and Jade showed the raw
        // key.
        builder.add("entity.minecraft.villager.avp_human.commissary", "Commissary Villager");
    }
}
