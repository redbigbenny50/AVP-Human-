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
        addEntity(builder, HumanEntityTypes.GRENADE_THROWN, "Grenade");
        addEntity(builder, HumanEntityTypes.MARINE_DOG, "Marine Dog");
        addEntity(builder, HumanEntityTypes.MARINE, "Marine");
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
