package com.human.fabric.data.lang.en_us.provider;

import com.human.common.registry.tag.HumanEntityTypeTags;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import java.util.function.Consumer;

public class EnUsEntityTypeTagProvider {

    public static final Consumer<FabricLanguageProvider.TranslationBuilder> CONSUMER = builder -> {
        builder.add(HumanEntityTypeTags.HATED_BY_MARINES, "Hated By Marines");
        builder.add(HumanEntityTypeTags.IGNORED_BY_MARINES, "Ignored By Marines");
        builder.add(HumanEntityTypeTags.RADIATION_RESISTANT, "Radiation Resistant");
    };
}
