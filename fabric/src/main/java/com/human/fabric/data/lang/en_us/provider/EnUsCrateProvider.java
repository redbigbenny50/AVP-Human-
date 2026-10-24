package com.human.fabric.data.lang.en_us.provider;

import com.human.common.gameplay.storage.CrateType;
import com.human.common.registry.init.HumanCrates;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import java.util.function.Consumer;

/** Names for all 168 storage crates and containers, plus the crate screen, tooltip and message text. */
public final class EnUsCrateProvider {

    public static final Consumer<FabricLanguageProvider.TranslationBuilder> CONSUMER = builder -> {
        for (var type : CrateType.ALL) {
            builder.add(HumanCrates.BLOCK_BY_TYPE.get(type).get(), type.displayName());
        }

        builder.add("creativeModeTab.avp_human.storage", "Storage");
        builder.add("message.avp_human.crate.no_room", "There isn't room to place this here");
        builder.add("tooltip.avp_human.crate.capacity", "Holds %s slots");
        builder.add("tooltip.avp_human.crate.contents", "%s items in %s / %s slots");
        builder.add("gui.avp_human.crate.search", "Search...");
        builder.add("gui.avp_human.crate.sort.name", "Name");
        builder.add("gui.avp_human.crate.sort.count", "Count");
        builder.add("gui.avp_human.crate.sort.mod", "Mod");
        builder.add("gui.avp_human.crate.group", "%s versions, %s in total");
        builder.add("gui.avp_human.crate.expand", "Click to show each version");
        builder.add("gui.avp_human.crate.collapse", "Click to collapse");
        builder.add("gui.avp_human.crate.stored", "Stored: %s");
        builder.add("gui.avp_human.crate.slots", "%s / %s slots");
    };

    private EnUsCrateProvider() {}
}
