package com.human.fabric.data.lang.en_us.provider;

import com.human.common.registry.init.HumanEntityTypes;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.world.entity.EntityType;

import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EnUsEntityProvider {

    private static final HashSet<EntityType<?>> TOUCHED_ENTRIES = new HashSet<>();

    public static final Consumer<FabricLanguageProvider.TranslationBuilder> CONSUMER = builder -> {
        addEntity(builder, HumanEntityTypes.FLAMETHROW, "Flamethrow");
        addEntity(builder, HumanEntityTypes.CHEM_CLOUD, "Chem Cloud");
        addEntity(builder, HumanEntityTypes.GRENADE_THROWN, "Grenade");
        addEntity(builder, HumanEntityTypes.MARINE_DOG, "Marine Dog");
        addEntity(builder, HumanEntityTypes.MARINE, "Marine");
        addEntity(builder, HumanEntityTypes.TACTICAL_MARINE, "Tactical Marine");
        addEntity(builder, HumanEntityTypes.WY_COMMANDO, "W-Y Commando");
        addEntity(builder, HumanEntityTypes.WY_ELITE, "W-Y Elite");
        addEntity(builder, HumanEntityTypes.WY_SPEC_OPS_COMMANDO, "W-Y Spec Ops Commando");
        addEntity(builder, HumanEntityTypes.WY_SPEC_OPS_ELITE, "W-Y Spec Ops Elite");
        addEntity(builder, HumanEntityTypes.WY_APE_TROOPER, "W-Y APE Trooper");
        addEntity(builder, HumanEntityTypes.COLONIST, "Colonist");
        // The trade screen title and name of a colonist holding the job - see ElectricianProfession.
        builder.add("entity.avp_human.colonist.electrician", "Electrician");
        addEntity(builder, HumanEntityTypes.MUSHROOM_CLOUD, "Mushroom Cloud");
        addEntity(builder, HumanEntityTypes.NUKE, "Nuke");
        addEntity(builder, HumanEntityTypes.ROCKET, "Rocket");
        addEntity(builder, HumanEntityTypes.SENTRY_TURRET, "Sentry Turret");
    };

    private static void addEntity(
        FabricLanguageProvider.TranslationBuilder translationBuilder,
        Supplier<? extends EntityType<?>> entityTypeSupplier,
        String value
    ) {
        addEntity(translationBuilder, entityTypeSupplier.get(), value);
    }

    private static void addEntity(FabricLanguageProvider.TranslationBuilder translationBuilder, EntityType<?> entityType, String value) {
        TOUCHED_ENTRIES.add(entityType);
        translationBuilder.add(entityType, value);
    }
}
