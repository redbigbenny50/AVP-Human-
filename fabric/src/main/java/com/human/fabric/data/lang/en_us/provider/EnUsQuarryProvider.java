package com.human.fabric.data.lang.en_us.provider;

import com.human.common.registry.init.HumanQuarry;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import java.util.function.Consumer;

/** Names and messages for the quarry: the terminal editions, the frame block and the placement messages. */
public final class EnUsQuarryProvider {

    public static final Consumer<FabricLanguageProvider.TranslationBuilder> CONSUMER = builder -> {
        builder.add(HumanQuarry.QUARRY_TERMINAL.get(), "Quarry Terminal");
        builder.add(HumanQuarry.WY_QUARRY_TERMINAL.get(), "Weyland-Yutani Quarry Terminal");
        builder.add(HumanQuarry.USCM_QUARRY_TERMINAL.get(), "USCM Quarry Terminal");
        builder.add(HumanQuarry.QUARRY_FRAME.get(), "Quarry Frame");
        builder.add(HumanQuarry.QUARRY_CONTAINMENT_FIELD.get(), "Containment Field");
        builder.add("gui.avp_human.quarry.status.tank_full", "TANK FULL");

        builder.add("message.avp_human.quarry.place.no_room", "There isn't room for the quarry terminal here.");
        builder.add(
            "message.avp_human.quarry.place.move_toward",
            "The quarry terminal goes just outside a chunk edge, back to the chunk. Move it %s block(s) further in."
        );
        builder.add(
            "message.avp_human.quarry.place.move_away",
            "The quarry terminal goes just outside a chunk edge, back to the chunk. Move it %s block(s) back toward you."
        );

        // The Quarry-Com screen.
        builder.add("gui.avp_human.quarry.title", "/// Quarry-Com");
        builder.add("gui.avp_human.quarry.subtitle", "VERTICAL BORE CONTROL");
        builder.add("gui.avp_human.quarry.start", "START");
        builder.add("gui.avp_human.quarry.stop", "STOP");
        builder.add("gui.avp_human.quarry.brand.bare", "INDUSTRIAL");
        builder.add("gui.avp_human.quarry.brand.wy", "WEYLAND-YUTANI");
        builder.add("gui.avp_human.quarry.brand.uscm", "USCM ENGINEERING");
        builder.add("gui.avp_human.quarry.terminal", "QUARRY TERMINAL");
        builder.add("gui.avp_human.quarry.tab.work_area", "WORK AREA");
        builder.add("gui.avp_human.quarry.tab.settings", "SETTINGS");
        builder.add("gui.avp_human.quarry.tab.progress", "PROGRESS");
        builder.add("gui.avp_human.quarry.tab.storage", "STORAGE");
        builder.add("gui.avp_human.quarry.operation", "OPERATION");
        builder.add("gui.avp_human.quarry.save", "SAVE");
        builder.add("gui.avp_human.quarry.load", "LOAD");
        builder.add("gui.avp_human.quarry.saved", "SETTINGS SAVED");
        builder.add("gui.avp_human.quarry.loaded", "SETTINGS LOADED");
        builder.add("gui.avp_human.quarry.nothing_saved", "NOTHING SAVED YET");
        builder.add("gui.avp_human.quarry.save_failed", "COULD NOT SAVE / LOAD");
        builder.add("gui.avp_human.quarry.refill", "REFILL");
        builder.add("gui.avp_human.quarry.refill.min_y", "MIN Y");
        builder.add("gui.avp_human.quarry.refill.max_y", "MAX Y");
        builder.add("gui.avp_human.quarry.refill.fill_with", "FILL WITH");
        builder.add("gui.avp_human.quarry.refill.note", "SAME SPEED AS DIGGING. SLOT 1 FIRST.");
        builder.add("gui.avp_human.quarry.progress.refill", "PROGRESS - REFILL");
        builder.add("gui.avp_human.quarry.blocks_placed", "BLOCKS PLACED");
        builder.add("gui.avp_human.quarry.area_filled", "AREA FILLED");
        builder.add("gui.avp_human.quarry.filling_with", "FILLING WITH");
        builder.add("gui.avp_human.quarry.on_hand", "ON HAND");
        builder.add("gui.avp_human.quarry.status.filled", "AREA FILLED");
        builder.add("gui.avp_human.quarry.status.no_fill", "NO FILL BLOCKS");
        builder.add("gui.avp_human.quarry.connections", "CONNECTIONS");
        builder.add("gui.avp_human.quarry.port.item", "ITEM");
        builder.add("gui.avp_human.quarry.port.item.where", "TOP LEFT");
        builder.add("gui.avp_human.quarry.port.liquid", "LIQUID");
        builder.add("gui.avp_human.quarry.port.liquid.where", "TOP RIGHT");
        builder.add("gui.avp_human.quarry.port.power", "POWER");
        builder.add("gui.avp_human.quarry.port.power.where", "TOP MIDDLE");
        builder.add("gui.avp_human.quarry.status", "STATUS");
        builder.add("gui.avp_human.quarry.status.stopped", "STOPPED");
        builder.add("gui.avp_human.quarry.status.no_power", "NO POWER");
        builder.add("gui.avp_human.quarry.status.idle", "READY");
        builder.add("gui.avp_human.quarry.status.mining", "MINING");
        builder.add("gui.avp_human.quarry.status.refilling", "REFILLING");
        builder.add("gui.avp_human.quarry.status.storage_full", "STORAGE FULL");
        builder.add("gui.avp_human.quarry.status.no_area", "NO WORK AREA");
        builder.add("gui.avp_human.quarry.time_left", "TIME LEFT");
        builder.add("gui.avp_human.quarry.time_left.none", "-");
        builder.add("gui.avp_human.quarry.frames", "FRAMES");
        builder.add("gui.avp_human.quarry.internal", "STORAGE");
        builder.add("gui.avp_human.quarry.upgrades", "UPGRADES");
        builder.add("gui.avp_human.quarry.upgrades.note", "EACH SPEED CARD TAKES 4 S OFF A LAYER.");
        builder.add("gui.avp_human.quarry.internal_storage", "INTERNAL STORAGE");
        builder.add("gui.avp_human.quarry.slots", "SLOTS");
        builder.add("gui.avp_human.quarry.storage.note", "ONLY USED WHEN NO CONNECTED STORAGE HAS ROOM.");
        builder.add("gui.avp_human.quarry.frames.only", "FRAMES ONLY");
        builder.add("gui.avp_human.quarry.tank", "TANK");
        builder.add("gui.avp_human.quarry.tank.empty", "EMPTY");
        builder.add("gui.avp_human.quarry.setting.redstone", "REDSTONE");
        builder.add("gui.avp_human.quarry.setting.redstone.0", "IGNORE");
        builder.add("gui.avp_human.quarry.setting.redstone.1", "RUNS");
        builder.add("gui.avp_human.quarry.setting.redstone.2", "STOPS");
        builder.add("gui.avp_human.quarry.setting.redstone.0.desc", "RUNS WHENEVER IT IS STARTED.");
        builder.add("gui.avp_human.quarry.setting.redstone.1.desc", "RUNS ONLY WHILE IT HAS A REDSTONE SIGNAL.");
        builder.add("gui.avp_human.quarry.setting.redstone.2.desc", "A REDSTONE SIGNAL STOPS IT.");
        builder.add("gui.avp_human.quarry.setting.when_finished", "FRAME WHEN FINISHED");
        builder.add("gui.avp_human.quarry.setting.when_finished.0", "RETRACT");
        builder.add("gui.avp_human.quarry.setting.when_finished.1", "LEAVE BUILT");
        builder.add("gui.avp_human.quarry.setting.when_finished.0.desc", "THE FRAME IS TAKEN DOWN AND RETURNED TO THE TERMINAL.");
        builder.add("gui.avp_human.quarry.setting.when_finished.1.desc", "THE FRAME STAYS UP AFTER THE LAST CHUNK.");
        builder.add("gui.avp_human.quarry.setting.atomizer", "ATOMIZER SWEEP");
        builder.add("gui.avp_human.quarry.setting.atomizer.0", "ON");
        builder.add("gui.avp_human.quarry.setting.atomizer.1", "OFF");
        builder.add(
            "gui.avp_human.quarry.setting.atomizer.0.desc",
            "CLEARS ONLY WHERE THE FRAME WILL STAND. CABLES, PIPES AND STORAGE ARE SKIPPED AND MARKED RED."
        );
        builder.add(
            "gui.avp_human.quarry.setting.atomizer.1.desc",
            "NOTHING IS CLEARED. ANYTHING IN THE FRAME'S WAY STOPS IT BEING BUILT."
        );
        builder.add("gui.avp_human.quarry.setting.drain_liquids", "DRAIN LIQUIDS");
        builder.add("gui.avp_human.quarry.setting.drain_liquids.0", "OFF");
        builder.add("gui.avp_human.quarry.setting.drain_liquids.1", "COLLECT");
        builder.add("gui.avp_human.quarry.setting.drain_liquids.2", "VOID");
        builder.add("gui.avp_human.quarry.setting.drain_liquids.0.desc", "WATER IN THE AREA IS LEFT. THE DRILL WORKS THROUGH IT.");
        builder.add("gui.avp_human.quarry.setting.drain_liquids.1.desc", "LIQUIDS GO TO THE PORT, THEN THE 128 BUCKET TANK.");
        builder.add("gui.avp_human.quarry.setting.drain_liquids.2.desc", "LIQUIDS ARE DESTROYED AS THEY ARE FOUND.");
        builder.add("gui.avp_human.quarry.setting.preserve_structures", "PRESERVE STRUCTURES");
        builder.add("gui.avp_human.quarry.setting.preserve_structures.0", "ON");
        builder.add("gui.avp_human.quarry.setting.preserve_structures.1", "OFF");
        builder.add("gui.avp_human.quarry.setting.preserve_structures.0.desc", "THE DRILL DIGS AROUND STRUCTURES, NOT INTO THEM.");
        builder.add("gui.avp_human.quarry.setting.preserve_structures.1.desc", "THE DRILL DIGS STRAIGHT THROUGH STRUCTURES.");
        builder.add("gui.avp_human.quarry.setting.preserve_spawners", "PRESERVE SPAWNERS");
        builder.add("gui.avp_human.quarry.setting.preserve_spawners.0", "ON");
        builder.add("gui.avp_human.quarry.setting.preserve_spawners.1", "OFF");
        builder.add("gui.avp_human.quarry.setting.preserve_spawners.0.desc", "SPAWNERS ARE COLLECTED AND KEEP THEIR MOB TYPE.");
        builder.add("gui.avp_human.quarry.setting.preserve_spawners.1.desc", "SPAWNERS ARE MINED LIKE ANY OTHER BLOCK.");
        builder.add("gui.avp_human.quarry.setting.refill", "REFILL HOLE");
        builder.add("gui.avp_human.quarry.setting.refill.0", "ON");
        builder.add("gui.avp_human.quarry.setting.refill.1", "OFF");
        builder.add("gui.avp_human.quarry.setting.refill.0.desc", "FILLS THE HOLE BACK IN USING THE REFILL HOLE SETTINGS.");
        builder.add("gui.avp_human.quarry.setting.refill.1.desc", "THE HOLE IS LEFT OPEN.");

        // WORK AREA and the frame.
        builder.add("gui.avp_human.quarry.site_map", "SITE MAP");
        builder.add("gui.avp_human.quarry.legend.selected", "SELECTED");
        builder.add("gui.avp_human.quarry.legend.available", "AVAILABLE");
        builder.add("gui.avp_human.quarry.legend.claimed", "CLAIMED");
        builder.add("gui.avp_human.quarry.legend.xeno", "XENO HIVE");
        builder.add("gui.avp_human.quarry.legend.quarry", "QUARRY");
        builder.add("gui.avp_human.quarry.legend.out_of_range", "TOO FAR");
        builder.add("gui.avp_human.quarry.legend.terminal", "TERMINAL");
        builder.add("gui.avp_human.quarry.size", "SIZE");
        builder.add("gui.avp_human.quarry.size.value", "%s x %s CHUNKS");
        builder.add("gui.avp_human.quarry.footprint", "FOOTPRINT");
        builder.add("gui.avp_human.quarry.frames_needed", "FRAMES NEEDED");
        builder.add("gui.avp_human.quarry.frames_in_place", "IN PLACE");
        builder.add("gui.avp_human.quarry.frames_held_short", "IN TERMINAL");
        builder.add("gui.avp_human.quarry.problem.ready", "READY - CONFIRM THE AREA");
        builder.add("gui.avp_human.quarry.problem.nothing", "NOTHING SELECTED");
        builder.add("gui.avp_human.quarry.problem.rectangle", "MUST BE A RECTANGLE");
        builder.add("gui.avp_human.quarry.problem.too_big", "MAX 3 x 3 CHUNKS");
        builder.add("gui.avp_human.quarry.problem.front", "MUST INCLUDE THE CHUNK IN FRONT");
        builder.add("gui.avp_human.quarry.problem.frame_claimed", "CLAIMED LAND OR TOO NEAR ANOTHER QUARRY.");
        builder.add("gui.avp_human.quarry.frame.none", "AREA CONFIRMED - PRESS START TO BUILD THE FRAME");
        builder.add("gui.avp_human.quarry.frame.building", "BUILDING THE FRAME");
        builder.add("gui.avp_human.quarry.frame.built", "FRAME BUILT");
        builder.add("gui.avp_human.quarry.frame.retracting", "TAKING THE FRAME DOWN");
        builder.add("gui.avp_human.quarry.frame.blocked", "%s BLOCKED - SHOWN IN RED");
        builder.add("gui.avp_human.quarry.depth", "DEPTH");
        builder.add("gui.avp_human.quarry.starting_y", "STARTING Y");
        builder.add("gui.avp_human.quarry.bottom_y", "BOTTOM Y");
        builder.add("gui.avp_human.quarry.bottom_y.shift", "SHIFT: 10 AT A TIME");
        builder.add("gui.avp_human.quarry.layers", "LAYERS");
        builder.add("gui.avp_human.quarry.confirm", "CONFIRM");
        builder.add("gui.avp_human.quarry.edit", "EDIT AREA");
        builder.add("gui.avp_human.quarry.retract", "RETRACT");
        builder.add("gui.avp_human.quarry.cell.available", "AVAILABLE - CLICK TO ADD");
        builder.add("gui.avp_human.quarry.cell.selected", "SELECTED - CLICK TO REMOVE");
        builder.add("gui.avp_human.quarry.cell.out_of_range", "OUT OF RANGE: UP TO 3 x 3 CHUNKS IN FRONT OF THE TERMINAL");
        builder.add("gui.avp_human.quarry.cell.claimed", "CLAIMED: MINING NOT AUTHORIZED BY OWNER");
        builder.add("gui.avp_human.quarry.cell.xeno", "XENO HIVE: MINING TOO DANGEROUS, HOSTILE ALIEN LIFEFORMS DETECTED");
        builder.add("gui.avp_human.quarry.cell.near_quarry", "TOO CLOSE TO ANOTHER QUARRY: KEEP 1 CHUNK BETWEEN QUARRIES");
        builder.add("gui.avp_human.quarry.cell.other_quarry", "ANOTHER QUARRY IS ALREADY WORKING THIS CHUNK");
        builder.add("gui.avp_human.quarry.cell.terminal", "THE TERMINAL. THE WORK AREA STARTS AT THE CHUNK IN FRONT OF IT.");
        builder.add("gui.avp_human.quarry.cell.behind", "BEHIND THE TERMINAL: THE AREA ONLY RUNS IN FRONT OF IT.");
        builder.add("gui.avp_human.quarry.status.building_frame", "BUILDING FRAME");
        builder.add("gui.avp_human.quarry.status.need_frames", "NEED FRAMES");
        builder.add("gui.avp_human.quarry.status.frame_blocked", "FRAME BLOCKED");
        builder.add("gui.avp_human.quarry.status.waiting", "CHUNK UNLOADED");
        builder.add("gui.avp_human.quarry.status.retracting", "RETRACTING");
        builder.add("gui.avp_human.quarry.status.frame_slots_full", "NO FRAME ROOM");
        builder.add("gui.avp_human.quarry.status.ready", "FRAME READY");
        builder.add("gui.avp_human.quarry.status.finished", "AREA FINISHED");
        builder.add("gui.avp_human.quarry.status.redstone", "REDSTONE HOLD");
        builder.add("gui.avp_human.quarry.chunk", "CHUNK");
        builder.add("gui.avp_human.quarry.layer", "LAYER");
        builder.add("gui.avp_human.quarry.blocks_left", "BLOCKS LEFT");
        builder.add("gui.avp_human.quarry.area_done", "AREA DONE");
        builder.add("gui.avp_human.quarry.progress.mining", "PROGRESS - MINING");
        builder.add("gui.avp_human.quarry.blocks_mined", "BLOCKS MINED");
        builder.add("gui.avp_human.quarry.energy", "ENERGY");
        builder.add("gui.avp_human.quarry.upgrades.place", "PLACE UPGRADES HERE");
        builder.add("gui.avp_human.quarry.enchant", "ENCHANTMENT");
        builder.add("gui.avp_human.quarry.enchant.takes", "Fortune (any level) or Silk Touch book");
    };

    private EnUsQuarryProvider() {}
}
