package com.human.fabric.data.lang.en_us.provider;

import com.human.common.registry.init.HumanFabrication;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import java.util.function.Consumer;

/** Names and screen text for the fabrication system: blueprint chips, faction licenses and the blueprint table. */
public final class EnUsFabricationProvider {

    public static final Consumer<FabricLanguageProvider.TranslationBuilder> CONSUMER = builder -> {
        builder.add(HumanFabrication.BLUEPRINT_CHIP.get(), "Blueprint Chip");
        builder.add(HumanFabrication.USCM_FACTION_LICENSE.get(), "USCM Faction License");
        builder.add(HumanFabrication.WY_FACTION_LICENSE.get(), "Weyland-Yutani Faction License");

        builder.add("tooltip.avp_human.blueprint_chip.prints", "Prints: %s");
        builder.add("tooltip.avp_human.blueprint_chip.blank", "Blank");
        builder.add("tooltip.avp_human.blueprint_chip.missing", "Prints %s - not installed");
        builder.add("tooltip.avp_human.faction_license.uscm", "Unlocks every USCM fabricator print");
        builder.add("tooltip.avp_human.faction_license.wy", "Unlocks every Weyland-Yutani fabricator print");

        builder.add("container.avp_human.blueprint_table", "Blueprint Table");
        builder.add(HumanFabrication.DRIVE_BAY.get(), "Drive Bay");
        builder.add(HumanFabrication.WY_DRIVE_BAY.get(), "Weyland-Yutani Drive Bay");
        builder.add(HumanFabrication.USCM_DRIVE_BAY.get(), "USCM Drive Bay");
        builder.add("tooltip.avp_human.drive_bay.contents", "%s SSDs, %s blueprints inside");
        builder.add("gui.avp_human.drive_bay.capacity", "%s / %s");
        builder.add("gui.avp_human.drive_bay.greyed", "Greyed out - install another SSD to use this slot");
        builder.add(HumanFabrication.FABRICATOR.get(), "Fabricator");
        builder.add(HumanFabrication.WY_FABRICATOR.get(), "Weyland-Yutani Fabricator");
        builder.add(HumanFabrication.USCM_FABRICATOR.get(), "USCM Fabricator");
        builder.add("tooltip.avp_human.fabricator.contents", "%s items stored, %s blueprints inside");
        builder.add("gui.avp_human.fabricator.subtitle", "FABRICATION TERMINAL");
        builder.add("gui.avp_human.fabricator.brand.bare", "INDUSTRIAL");
        builder.add("gui.avp_human.fabricator.brand.wy", "WEYLAND-YUTANI");
        builder.add("gui.avp_human.fabricator.brand.uscm", "USCM ENGINEERING");
        builder.add("gui.avp_human.fabricator.search", "Search, or @mod");
        // Oct 16 layout: PRINT / BLUEPRINTS / STORAGE / SYSTEM. Every label fits its box at full size.
        builder.add("gui.avp_human.fabricator.tab.print", "PRINT");
        builder.add("gui.avp_human.fabricator.tab.blueprints", "BLUEPRINTS");
        builder.add("gui.avp_human.fabricator.tab.storage", "STORAGE");
        builder.add("gui.avp_human.fabricator.tab.system", "SYSTEM");
        builder.add("gui.avp_human.fabricator.catalogue", "CATALOGUE");
        builder.add("gui.avp_human.fabricator.filter.all", "ALL");
        builder.add("gui.avp_human.fabricator.filter.local", "LOCAL");
        builder.add("gui.avp_human.fabricator.filter.network", "NET");
        builder.add("gui.avp_human.fabricator.empty", "NO BLUEPRINTS. ADD CHIPS OR A LICENSE ON BLUEPRINTS, OR CABLE A DRIVE BAY.");
        builder.add("gui.avp_human.fabricator.no_match", "NOTHING MATCHES.");
        builder.add("gui.avp_human.fabricator.catalogue_count", "%s PRINTS");
        builder.add("gui.avp_human.fabricator.materials", "MATERIALS");
        builder.add("gui.avp_human.fabricator.pick", "SELECT A PRINT.");
        builder.add("gui.avp_human.fabricator.calculating", "CALCULATING...");
        builder.add("gui.avp_human.fabricator.need", "NEED");
        builder.add("gui.avp_human.fabricator.have", "HAVE");
        builder.add("gui.avp_human.fabricator.short", "%s SHORT");
        builder.add("gui.avp_human.fabricator.all_here", "ALL MATERIALS HERE");
        builder.add("gui.avp_human.fabricator.print", "PRINT");
        builder.add("gui.avp_human.fabricator.qty", "QTY");
        builder.add("gui.avp_human.fabricator.stop", "STOP");
        builder.add("gui.avp_human.fabricator.left", "%s LEFT");
        builder.add("gui.avp_human.fabricator.idle_line", "READY");
        builder.add("gui.avp_human.fabricator.blueprints_title", "LOCAL BLUEPRINTS");
        builder.add("gui.avp_human.fabricator.blueprints_help", "CHIPS AND LICENSES HERE ARE THIS FABRICATOR'S OWN.");
        builder.add("gui.avp_human.fabricator.blueprints_shared", "DRIVE BAYS ON THE SAME CABLE SHARE THEIRS WITH EVERY FABRICATOR.");
        builder.add("gui.avp_human.fabricator.storage", "STORAGE");
        builder.add("gui.avp_human.fabricator.internal", "INTERNAL");
        builder.add("gui.avp_human.fabricator.connected", "CONNECTED");
        builder.add("gui.avp_human.fabricator.slots", "%s SLOTS");
        builder.add("gui.avp_human.fabricator.internal_hint", "SHIFT-CLICK ITEMS IN. PRINTS USE THESE FIRST.");
        builder.add("gui.avp_human.fabricator.types", "%s TYPES");
        builder.add("gui.avp_human.fabricator.connected_hint", "CONNECTED CRATES AND CHESTS. VIEW ONLY: TAKE THEM OUT AT THE CRATE.");
        builder.add("gui.avp_human.fabricator.storage_empty", "PLACE CRATES OR CHESTS AGAINST IT, OR PIPE THEM IN.");
        builder.add("gui.avp_human.fabricator.inventory", "INVENTORY");
        builder.add("gui.avp_human.fabricator.upgrades_title", "UPGRADES");
        builder.add("gui.avp_human.fabricator.power_note", "%s FE/t WHILE PRINTING.");
        builder.add("gui.avp_human.fabricator.speed_note", "SPEED CARDS PRINT FASTER.");
        builder.add("gui.avp_human.fabricator.control", "CONTROL");
        builder.add("gui.avp_human.fabricator.priority", "PRIORITY");
        builder.add("gui.avp_human.fabricator.priority.critical", "CRITICAL");
        builder.add("gui.avp_human.fabricator.priority.default", "NORMAL");
        builder.add("gui.avp_human.fabricator.priority.deferrable", "LOW");
        builder.add("gui.avp_human.fabricator.redstone", "REDSTONE");
        builder.add("gui.avp_human.fabricator.redstone.ignore", "IGNORE");
        builder.add("gui.avp_human.fabricator.redstone.high", "RUNS");
        builder.add("gui.avp_human.fabricator.redstone.low", "STOPS");
        builder.add("gui.avp_human.fabricator.redstone_card", "NEEDS A REDSTONE CARD.");
        builder.add("gui.avp_human.fabricator.status.idle", "IDLE");
        builder.add("gui.avp_human.fabricator.status.printing", "PRINTING");
        builder.add("gui.avp_human.fabricator.status.no_power", "NO POWER");
        builder.add("gui.avp_human.fabricator.status.output_full", "OUTPUT FULL");
        builder.add("gui.avp_human.fabricator.status.missing", "MISSING MATERIALS");
        builder.add("gui.avp_human.fabricator.status.no_blueprint", "BLUEPRINT UNAVAILABLE");
        builder.add("gui.avp_human.fabricator.status.redstone", "HELD BY REDSTONE");
        // Oct 16: one CONFIRM button; every message fits the window (three short lines, full size). The item is
        // "Silicon".
        builder.add("gui.avp_human.blueprint_table.confirm", "CONFIRM");
        builder.add("gui.avp_human.blueprint_table.status.ok", "ITEM WILL BE BROKEN DOWN.");
        builder.add("gui.avp_human.blueprint_table.status.copy", "CHIP WILL BE COPIED.");
        builder.add("gui.avp_human.blueprint_table.status.empty", "ADD AN ITEM OR A CHIP.");
        builder.add("gui.avp_human.blueprint_table.status.no_silicon", "NOT ENOUGH SILICON.");
        builder.add("gui.avp_human.blueprint_table.status.not_enough_silicon", "NOT ENOUGH SILICON.");
        builder.add("gui.avp_human.blueprint_table.status.blank_chip", "BLANK CHIP. NOTHING TO COPY.");
        builder.add("gui.avp_human.blueprint_table.status.unscannable", "INVALID ITEM.");
        builder.add("gui.avp_human.blueprint_table.status.faction", "INVALID ITEM. NEEDS A LICENSE.");
        builder.add("gui.avp_human.blueprint_table.status.has_contents", "EMPTY THIS ITEM FIRST.");
        builder.add("gui.avp_human.blueprint_table.status.no_recipe", "INVALID ITEM. NO RECIPE.");
        builder.add("gui.avp_human.blueprint_table.status.output_full", "TAKE THE CHIPS OUT FIRST.");
        builder.add("gui.avp_human.blueprint_table.hint.item", "Item to break down, or a blueprint chip to copy");
        builder.add("gui.avp_human.blueprint_table.hint.silicon", "Silicon only");
    };

    private EnUsFabricationProvider() {}
}
