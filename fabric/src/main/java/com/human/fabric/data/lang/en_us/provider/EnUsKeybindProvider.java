package com.human.fabric.data.lang.en_us.provider;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import java.util.function.Consumer;

public class EnUsKeybindProvider {

    public static final Consumer<FabricLanguageProvider.TranslationBuilder> CONSUMER = builder -> {
        builder.add("key.avp_human.crawl", "Crawl");
        builder.add("key.avp_human.reload", "Reload");
        builder.add("keybind.category.avp_human.movement", "AVP Movement");
        builder.add("keybind.category.avp_human.weapons", "AVP Weapons");
    };
}
