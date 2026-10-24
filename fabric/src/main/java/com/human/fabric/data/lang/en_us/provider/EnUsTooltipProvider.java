package com.human.fabric.data.lang.en_us.provider;

import com.human.common.data.HumanTooltipTranslationKeys;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import java.util.function.Consumer;

public class EnUsTooltipProvider {

    public static final Consumer<FabricLanguageProvider.TranslationBuilder> CONSUMER = builder -> {
        builder.add("message.avp.reload_too_early", "Your training tells you it's too early to reload.");

        // Power breakers. ⚠ The direction is a separate key so the message reads naturally in languages that inflect
        // it - concatenating "Port closed: " + "north" would not translate.
        builder.add("message.avp_human.port_closed", "%s port closed");
        // [stated] Oct 20 - the creative test blocks.
        builder.add("tooltip.avp_human.creative_only", "Creative only - cannot be crafted");
        builder.add("message.avp_human.creative_fluid_tank.set", "Creative Fluid Tank set to %s");
        builder.add("message.avp_human.creative_fluid_tank.cleared", "Creative Fluid Tank cleared");
        builder.add("message.avp_human.soldier.contract_only", "Weyland-Yutani personnel only work under a company contract.");
        builder.add("message.avp_human.port_opened", "%s port opened");
        // Oct 21 - the resonator's right-click status.
        builder.add("message.avp_human.resonator.active", "Resonator running - field reaches %s blocks (sneak + right-click to show it)");
        builder.add("message.avp_human.resonator.no_alien", "Resonator: no effect - AVP Alien is not installed");
        builder.add("message.avp_human.resonator.panel_unpowered", "Resonator: no power from its fuse panel (needs %s FE/t)");
        builder.add("message.avp_human.resonator.nothing_below", "Resonator: no power - feed it from UNDERNEATH (needs %s FE/t)");
        builder.add(
            "message.avp_human.resonator.underpowered",
            "Resonator: not enough power - needs %s FE/t from below (check the breaker on its underside)"
        );
        builder.add("message.avp_human.direction.north", "North");
        builder.add("message.avp_human.direction.south", "South");
        builder.add("message.avp_human.direction.east", "East");
        builder.add("message.avp_human.direction.west", "West");
        builder.add("message.avp_human.direction.up", "Top");
        builder.add("message.avp_human.direction.down", "Bottom");

        // Fluid names - NeoForge shows these wherever a fluid is named (tank readouts, JEI/Jade). Neither fluid had
        // one.
        builder.add("fluid_type.avp_human.latex", "Latex");
        builder.add("fluid_type.avp_human.honey", "Honey");
        builder.add("fluid_type.avp_human.cryofluid", "Cryofluid");
        // The placed liquid blocks' names - used to name a fluid wherever vanilla has no fluid name of its own, such as
        // "Latex Canister" on the universal canister. Water and lava already have theirs from vanilla.
        builder.add("block.avp_human.latex", "Latex");
        builder.add("block.avp_human.honey", "Honey");
        builder.add("block.avp_human.cryofluid", "Cryofluid");

        // Cryotube controller screen and capture messages.
        builder.add("gui.avp_human.cryo.subtitle", "CRYOGENIC SUPPORT SYSTEMS");
        builder.add("gui.avp_human.cryo.controller", "CRYOTUBE CONTROLLER");
        builder.add("gui.avp_human.cryo.tab.overview", "OVERVIEW");
        builder.add("gui.avp_human.cryo.tab.cryotubes", "CRYOTUBES");
        builder.add("gui.avp_human.cryo.tab.specimens", "SPECIMENS");
        builder.add("gui.avp_human.cryo.tab.diagnostics", "DIAGNOSTICS");
        builder.add("gui.avp_human.cryo.panel.system", "SYSTEM STATUS");
        builder.add("gui.avp_human.cryo.panel.network", "NETWORK");
        builder.add("gui.avp_human.cryo.panel.events", "RECENT EVENTS");
        builder.add("gui.avp_human.cryo.panel.tubes", "TUBES");
        builder.add("gui.avp_human.cryo.panel.specimens", "SPECIMENS");
        builder.add("gui.avp_human.cryo.panel.specimen_data", "SPECIMEN DATA");
        builder.add("gui.avp_human.cryo.panel.genetics", "GENETIC DATA");
        builder.add("gui.avp_human.cryo.panel.alerts", "ALERTS");
        builder.add("gui.avp_human.cryo.panel.details", "DETAILS");
        builder.add("gui.avp_human.cryo.panel.inventory", "BACKUP POWER // NETWORK // INVENTORY");
        builder.add("gui.avp_human.cryo.label.status", "STATUS");
        builder.add("gui.avp_human.cryo.label.connected", "CONNECTED TUBES");
        builder.add("gui.avp_human.cryo.label.active", "ACTIVE");
        builder.add("gui.avp_human.cryo.label.standby", "STANDBY");
        builder.add("gui.avp_human.cryo.label.battery", "BACKUP BATTERY");
        builder.add("gui.avp_human.cryo.label.draw", "POWER DRAW");
        builder.add("gui.avp_human.cryo.label.grid", "GRID");
        builder.add("gui.avp_human.cryo.label.cryofluid", "CRYOFLUID");
        builder.add("gui.avp_human.cryo.label.usage", "USAGE");
        builder.add("gui.avp_human.cryo.label.time_left", "TIME LEFT");
        builder.add("gui.avp_human.cryo.label.size", "SIZE");
        builder.add("gui.avp_human.cryo.label.specimen", "SPECIMEN");
        builder.add("gui.avp_human.cryo.label.power", "POWER");
        builder.add("gui.avp_human.cryo.label.fuel", "FUEL");
        builder.add("gui.avp_human.cryo.label.thaw", "THAW");
        builder.add("gui.avp_human.cryo.label.priority", "PRIORITY");
        builder.add("gui.avp_human.cryo.label.species", "SPECIES");
        builder.add("gui.avp_human.cryo.label.tube", "TUBE");
        builder.add("gui.avp_human.cryo.label.health", "HEALTH");
        builder.add("gui.avp_human.cryo.label.armor", "ARMOR");
        builder.add("gui.avp_human.cryo.label.attack", "ATTACK");
        builder.add("gui.avp_human.cryo.label.speed", "SPEED");
        builder.add("gui.avp_human.cryo.label.strain", "STRAIN");
        builder.add("gui.avp_human.cryo.label.stage", "STAGE");
        builder.add("gui.avp_human.cryo.label.hive", "HIVE");
        builder.add("gui.avp_human.cryo.label.default_genes", "DEFAULT GENES");
        builder.add("gui.avp_human.cryo.label.bonus_genes", "BONUS GENES");
        builder.add("gui.avp_human.cryo.label.temperature", "TEMPERATURE");
        builder.add("gui.avp_human.cryo.label.batteries", "BATTERIES");
        builder.add("gui.avp_human.cryo.label.hub", "HUB CHIP");
        builder.add("gui.avp_human.cryo.label.inventory", "INVENTORY");
        builder.add("gui.avp_human.cryo.label.hotbar", "HOTBAR");
        builder.add("gui.avp_human.cryo.grid.supplied", "SUPPLIED");
        builder.add("gui.avp_human.cryo.grid.partly", "PARTLY UNLOADED");
        builder.add("gui.avp_human.cryo.grid.none", "NO POWER");
        builder.add("gui.avp_human.cryo.tube.active", "ACTIVE");
        builder.add("gui.avp_human.cryo.tube.thawing", "THAWING");
        builder.add("gui.avp_human.cryo.tube.deactivated", "DEACTIVATED");
        builder.add("gui.avp_human.cryo.tube.paused", "PAUSED");
        builder.add("gui.avp_human.cryo.tube.unloaded", "UNLOADED");
        builder.add("gui.avp_human.cryo.tube.standby", "STANDBY");
        builder.add("gui.avp_human.cryo.none", "NONE");
        builder.add("gui.avp_human.cryo.no_tubes", "No tubes connected");
        builder.add("gui.avp_human.cryo.no_specimens", "No specimens stored");
        builder.add("gui.avp_human.cryo.no_genes", "No genetic data");
        builder.add("gui.avp_human.cryo.priority_hint", "1 = kept longest");
        builder.add("gui.avp_human.cryo.deactivate_hint", "Thaws and releases");
        builder.add("gui.avp_human.cryo.copied", "COPIED");
        builder.add("gui.avp_human.cryo.action.deactivate", "DEACTIVATE");
        builder.add("gui.avp_human.cryo.action.confirm_deactivate", "CONFIRM?");
        builder.add("gui.avp_human.cryo.action.reactivate", "REACTIVATE");
        builder.add("gui.avp_human.cryo.action.standby", "STANDBY - READY");
        builder.add("gui.avp_human.cryo.event.frozen", "%s: %s FROZEN");
        builder.add("gui.avp_human.cryo.event.deactivated", "%s: DEACTIVATED (%s)");
        builder.add("gui.avp_human.cryo.event.thawing", "%s: THAWING (%s)");
        builder.add("gui.avp_human.cryo.event.released", "%s: %s RELEASED");
        builder.add("gui.avp_human.cryo.event.broke_out", "%s: %s BROKE OUT");
        builder.add("gui.avp_human.cryo.event.escaped", "%s: %s ESCAPED");
        builder.add("gui.avp_human.cryo.event.reactivated", "%s: %s REACTIVATED");
        builder.add("gui.avp_human.cryo.event.on_battery", "ON BATTERY");
        builder.add("gui.avp_human.cryo.event.power_restored", "POWER RESTORED");
        builder.add("gui.avp_human.cryo.event.fuel_low", "CRYOFLUID LOW");
        builder.add("gui.avp_human.cryo.event.fuel_empty", "CRYOFLUID EMPTY");
        builder.add("gui.avp_human.cryo.event.broken_by", "%s BROKEN BY %s AT %s");
        builder.add("gui.avp_human.cryo.event.broken_near", "%1$s BROKEN AT %3$s (%2$s NEARBY)");
        builder.add("gui.avp_human.cryo.event.broken", "%s BROKEN AT %s");
        builder.add("gui.avp_human.cryo.alert.nominal", "SYSTEMS NOMINAL");
        builder.add("gui.avp_human.cryo.alert.fuel_low", "LOW CRYOFLUID");
        builder.add("gui.avp_human.cryo.alert.fuel_empty", "CRYOFLUID EMPTY");
        builder.add("gui.avp_human.cryo.alert.on_battery", "RUNNING ON BATTERY");
        builder.add("gui.avp_human.cryo.alert.grid_partly", "GRID PARTLY UNLOADED");
        builder.add("gui.avp_human.cryo.alert.tube_thawing", "TUBE THAWING");
        builder.add("gui.avp_human.cryo.alert.at_capacity", "AT TUBE CAPACITY");
        builder.add("gui.avp_human.cryo.alert.tube_broken", "TUBE BROKEN");
        builder.add("gui.avp_human.cryo.alert.nominal.detail", "All systems operating within normal limits.");
        builder.add("gui.avp_human.cryo.alert.fuel_low.detail", "Cryofluid below 10%. Refill through the top port.");
        builder.add("gui.avp_human.cryo.alert.fuel_empty.detail", "No cryofluid. Occupied tubes are thawing.");
        builder.add("gui.avp_human.cryo.alert.on_battery.detail", "Grid power lost. Backup batteries are keeping the tubes frozen.");
        builder.add("gui.avp_human.cryo.alert.grid_partly.detail", "Part of the power grid is unloaded. Tubes are paused, not thawing.");
        builder.add("gui.avp_human.cryo.alert.tube_thawing.detail", "%s is thawing: %s%%.");
        builder.add("gui.avp_human.cryo.alert.at_capacity.detail", "This controller serves its maximum number of tubes.");
        builder.add("gui.avp_human.cryo.status.idle", "IDLE");
        builder.add("gui.avp_human.cryo.status.running", "NOMINAL");
        builder.add("gui.avp_human.cryo.status.paused", "PAUSED");
        builder.add("gui.avp_human.cryo.status.thawing", "THAWING");
        builder.add("gui.avp_human.cryo.status.battery", "ON BATTERY");
        builder.add("gui.avp_human.cryo.empty", "EMPTY");
        builder.add("message.avp_human.cryo.occupied", "This cryotank already holds a specimen");
        builder.add("message.avp_human.cryo.no_controller", "This cryotank is not connected to a cryotube controller");
        builder.add("message.avp_human.cryo.no_fuel", "The cryotube controller has no cryofluid");
        builder.add("message.avp_human.cryo.no_power", "The cryotube controller has no power");
        builder.add(
            "message.avp_human.cryo.over_capacity",
            "This cryotube controller already serves its maximum of 8 tanks or 3,375 tank blocks"
        );
        builder.add("message.avp_human.cryo.too_big", "The specimen does not fit in this cryotank");
        builder.add("message.avp_human.cryo.cannot_freeze", "The specimen could not be frozen");
        builder.add("message.avp_human.cryo.frozen", "Specimen frozen");
        builder.add("message.avp_human.cryo.extracted", "Specimen pulled out - frozen solid until it thaws");
        builder.add("message.avp_human.cryo.extract_failed", "There is no room to pull the specimen out");
        builder.add("message.avp_human.cryo.refused", "That cannot be frozen in a cryotank");
        builder.add("message.avp_human.cryo.occupied_resize", "Release the specimen before changing this cryotank's size");
        builder.add("gui.avp_human.cryo.unknown", "Unknown specimen");
        builder.add("message.avp_human.cryo.info", "Cryotank %s - %s");
        builder.add("message.avp_human.cryo.info_unconnected", "Cryotank %s - %s - not connected to a controller");

        // The universal canister.
        builder.add("item.avp_human.canister.filled", "%s Canister");
        builder.add("tooltip.avp_human.canister.amount", "%s / %s buckets");
        // [stated] Oct 23 - the Weyland-Yutani music disc.
        builder.add("item.avp_human.wy_music_disc_1", "Music Disc");
        builder.add("item.avp_human.wy_music_disc_1_fragment", "Disc Fragment");
        builder.add("item.avp_human.wy_music_disc_1_fragment.desc", "Music Disc - Building Better Worlds");
        builder.add("jukebox_song.avp_human.building_better_worlds", "Rotch Gwylt - Building Better Worlds");
        builder.add("subtitles.jukebox_sounds.building_better_worlds", "Building Better Worlds plays");
        // [stated] Oct 23 - how to use a canister, one line per click.
        builder.add("tooltip.avp_human.canister.use.fill", "Right-click a fluid: fill it");
        builder.add("tooltip.avp_human.canister.use.tank", "Right-click a tank: pour in or draw out");
        builder.add("tooltip.avp_human.canister.use.pour", "Shift + right-click: pour it out");
        builder.add("tooltip.avp_human.canister.use.place", "Shift + left-click: place as a block");
        builder.add("message.avp_human.canister.empty", "Empty");

        // Fluid tanks.
        builder.add("message.avp_human.fluid_tank.windows_on", "Windows open");
        builder.add("message.avp_human.fluid_tank.windows_off", "Windows closed");
        builder.add("message.avp_human.fluid_tank.side_input", "Side port: input");
        builder.add("message.avp_human.fluid_tank.side_output", "Side port: output");
        builder.add("message.avp_human.fluid_tank.fixed_port", "The top always takes fluid in and the bottom always gives it out.");

        // Battery upgrades and the battery's screen.
        builder.add("tooltip.avp_human.battery_upgrade.capacity", "Battery: adds 1.5M FE of storage.");
        builder.add("tooltip.avp_human.battery_upgrade.transfer", "Battery: doubles charge and discharge rate.");
        builder.add("tooltip.avp_human.battery_upgrade.emergency", "Battery: keeps a reserve for Critical machines only.");
        builder.add("tooltip.avp_human.battery_upgrade.redstone", "Battery: lets redstone control charging and discharging.");
        builder.add("tooltip.avp_human.battery_upgrade.alarm", "Battery: warns its owner when charge runs low.");
        builder.add("tooltip.avp_human.battery_upgrade.armored", "Battery: obsidian hardness and blast resistance.");
        builder.add("tooltip.avp_human.battery_upgrade.stackable", "Stacks: more slots, stronger effect.");
        builder.add("tooltip.avp_human.battery_upgrade.single", "One per battery.");
        builder.add("tooltip.avp_human.upgrade_card.speed", "Machine: works 1.5x faster per card (4 cards = 6x), drawing more power.");
        builder.add("tooltip.avp_human.upgrade_card.efficiency", "Machine: draws 25% less power per card.");
        builder.add("tooltip.avp_human.upgrade_card.range", "Sentry: range 32 -> 64 blocks.");
        builder.add("tooltip.avp_human.upgrade_card.wide_cone", "Sentry: field of fire 90 -> 180 degrees.");
        builder.add("tooltip.avp_human.upgrade_card.cooling", "Sentry: fires twice as long before overheating.");
        builder.add(
            "tooltip.avp_human.upgrade_card.defense",
            "Sentry: as tough as obsidian - immune to explosions, half damage from the rest."
        );
        builder.add("tooltip.avp_human.upgrade_card.caseless_ammo", "Sentry: fires caseless bullets for 1.5x damage.");
        builder.add("tooltip.avp_human.upgrade_card.rocket_ammo", "Sentry: fires rockets. Its blasts never hurt it or break blocks.");
        builder.add("tooltip.avp_human.upgrade_card.fits_machine", "Fits any machine's upgrade slots.");
        builder.add("tooltip.avp_human.upgrade_card.fits_sentry", "Fits a sentry turret's upgrade slots.");
        builder.add("tooltip.avp_human.upgrade_card.stackable", "Stacks: more slots, stronger effect.");
        builder.add("tooltip.avp_human.upgrade_card.single", "One of each per turret.");
        builder.add("tooltip.avp_human.upgrade_card.one_ammo", "One ammo card at a time.");
        builder.add("gui.avp_human.battery.stored", "%s / %s FE");
        builder.add("gui.avp_human.battery.rate", "Rate: %s FE/t");
        // [stated] Oct 2: the battery's side buttons.
        builder.add("gui.avp_human.battery.side.mode", "Mode");
        builder.add("gui.avp_human.battery.side.reserve", "Emergency Reserve");
        builder.add("gui.avp_human.battery.side.redstone", "Redstone Control");
        builder.add("gui.avp_human.battery.side.alarm", "Low Charge Alarm");
        builder.add("gui.avp_human.battery.mode.primary", "Primary");
        builder.add("gui.avp_human.battery.mode.reserve", "Reserve");
        builder.add("gui.avp_human.battery.mode.storage_only", "Storage");
        builder.add("gui.avp_human.battery.mode.tooltip.primary", "Primary: gives power out first.");
        builder.add("gui.avp_human.battery.mode.tooltip.reserve", "Reserve: used only after Primary batteries.");
        builder.add("gui.avp_human.battery.mode.tooltip.storage_only", "Storage: charges but never gives power out.");
        builder.add("gui.avp_human.battery.reserve", "R %s%%");
        builder.add("gui.avp_human.battery.reserve.tooltip", "Emergency reserve: the last %s%% is kept for Critical machines only.");
        builder.add("gui.avp_human.battery.redstone.ignored", "RS -");
        builder.add("gui.avp_human.battery.redstone.backup", "RS B");
        builder.add("gui.avp_human.battery.redstone.hold_when_powered", "RS H");
        builder.add("gui.avp_human.battery.redstone.off_when_powered", "RS O");
        builder.add("gui.avp_human.battery.redstone.tooltip.ignored", "Redstone: ignored.");
        builder.add("gui.avp_human.battery.redstone.tooltip.backup", "Redstone: backup - gives power out only while powered.");
        builder.add("gui.avp_human.battery.redstone.tooltip.hold_when_powered", "Redstone: holds its charge while powered.");
        builder.add("gui.avp_human.battery.redstone.tooltip.off_when_powered", "Redstone: switched off while powered.");
        builder.add("gui.avp_human.battery.alarm", "A %s%%");
        builder.add("gui.avp_human.battery.alarm.tooltip", "Alarm: warns the owner below %s%%.");
        builder.add("gui.avp_human.battery.needs_module", "Install the %s to use this.");
        builder.add("gui.avp_human.battery.capacity_locked", "Can’t remove: the battery holds more than it could without this upgrade.");
        builder.add("message.avp_human.battery.alarm", "%s below %s%% charge!");
        builder.add("message.avp_human.canister.contents", "%s: %s / %s buckets");

        // Machines and the wiring tool.
        builder.add("message.avp_human.machine_activated", "Machine activated");
        builder.add("message.avp_human.machine_deactivated", "Machine deactivated");
        builder.add("message.avp_human.no_port_here", "No power port on this face");
        builder.add("message.avp_human.wiring.machine_working", "Machine: working, drawing %s FE/t");
        builder.add("message.avp_human.wiring.machine_idle", "Machine: powered, idle (draws %s FE/t while working)");
        builder.add("message.avp_human.wiring.machine_no_power", "Machine: no power (needs %s FE/t)");
        builder.add("message.avp_human.wiring.machine_disabled", "Machine: switched off (draws %s FE/t when on)");
        builder.add("message.avp_human.wiring.no_grid", "Not connected to a power grid");
        builder.add("message.avp_human.wiring.grid", "Grid: %s producers, %s consumers, %s stores");
        builder.add("message.avp_human.wiring.flow", "Generating %s FE/t, demand %s FE/t");
        builder.add("message.avp_human.wiring.stored", "Stored %s / %s FE");
        builder.add("gui.avp_human.machine.priority.critical", "Priority: Critical");
        builder.add("gui.avp_human.machine.priority.critical.desc", "Fed first when power runs short.");
        builder.add("gui.avp_human.machine.priority.default", "Priority: Normal");
        builder.add("gui.avp_human.machine.priority.default.desc", "Fed after critical machines when power runs short.");
        builder.add("gui.avp_human.machine.priority.deferrable", "Priority: Low");
        builder.add("gui.avp_human.machine.priority.deferrable.desc", "Waits until everything else is fed when power runs short.");
        builder.add("gui.avp_human.machine.priority.cycle", "Click to change");
        builder.add("gui.avp_human.machine.filter", "Input Filter");
        builder.add("gui.avp_human.pipe_end.title", "%s End");
        builder.add("gui.avp_human.pipe_end.search", "Search...");
        builder.add("gui.avp_human.pipe_end.allow", "Allow");
        builder.add("gui.avp_human.pipe_end.block", "Block");
        builder.add(
            "gui.avp_human.pipe_end.toggle.desc",
            "Allow: only the listed entries pass. Block: everything except them. No entries: everything passes."
        );
        builder.add("gui.avp_human.pipe_end.empty.item", "Click with an item to add it, or pick one from the search");
        builder.add("gui.avp_human.pipe_end.empty.fluid", "Click with a filled bucket or canister, or pick a fluid from the search");
        builder.add("gui.avp_human.pipe_end.clear", "Click with an empty hand to clear");
        builder.add("gui.avp_human.pipe_end.mode.auto.desc", "Whatever the block's face does");
        builder.add("gui.avp_human.pipe_end.mode.pull.desc", "Output: the pipe takes out of this block (connect to its output)");
        builder.add("gui.avp_human.pipe_end.mode.push.desc", "Input: the pipe puts into this block (connect to its input)");
        builder.add("gui.avp_human.pipe_end.mode.both.desc", "A buffer: fills from Output ends, empties into Input ends");
        builder.add("gui.avp_human.pipe_end.mode.off.desc", "This end does nothing");
        // Oct 8: the filter page.
        builder.add("gui.avp_human.pipe_end.search.hint", "Item, #tag or @mod");
        builder.add("gui.avp_human.pipe_end.filter", "Filter (%s)");
        builder.add("gui.avp_human.pipe_end.filter.desc", "Open the full filter: search, tags, mods and as many lines as you need");
        builder.add("gui.avp_human.pipe_end.filter_title", "%s End Filter");
        builder.add("gui.avp_human.pipe_end.priority.auto", "Auto");
        builder.add("gui.avp_human.pipe_end.priority.low", "Low");
        builder.add("gui.avp_human.pipe_end.priority.normal", "Normal");
        builder.add("gui.avp_human.pipe_end.priority.high", "High");
        builder.add(
            "gui.avp_human.pipe_end.priority.desc",
            "Priority: higher ends are offered items first. Auto = Normal for machines, Low for storage. Right-click cycles back"
        );
        builder.add("gui.avp_human.pipe_end.copy", "Copy");
        builder.add("gui.avp_human.pipe_end.paste", "Paste");
        builder.add("gui.avp_human.pipe_end.clear_all", "Clear");
        builder.add("gui.avp_human.pipe_end.done", "Done");
        builder.add("gui.avp_human.pipe_end.lines", "%s lines");
        // Oct 23 - shortened: the filter editor draws it beside the Done button, with 102 px of room (it was 139 px).
        builder.add("gui.avp_human.pipe_end.empty_filter", "No lines: all pass");
        builder.add("gui.avp_human.pipe_end.any", "Any");
        builder.add("gui.avp_human.pipe_end.not_enchanted.short", "Plain");
        builder.add(
            "gui.avp_human.pipe_end.not_enchanted.desc",
            "Any: matches enchanted and plain. Plain: enchanted items do not match this line"
        );
        builder.add("gui.avp_human.pipe_end.remove", "Remove this line");
        builder.add("gui.avp_human.pipe_end.hot.ores", "Ores");
        builder.add("gui.avp_human.pipe_end.hot.ingots", "Ingots");
        builder.add("gui.avp_human.pipe_end.hot.gems", "Gems");
        builder.add("gui.avp_human.pipe_end.hot.logs", "Logs");
        builder.add("gui.avp_human.pipe_end.hot.stone", "Stone");
        builder.add("gui.avp_human.pipe_end.hot.dirt", "Dirt");
        builder.add("gui.avp_human.pipe_end.hot.crops", "Crops");
        builder.add("gui.avp_human.pipe_end.hot.seeds", "Seeds");
        builder.add("gui.avp_human.pipe_end.hot.food", "Food");
        builder.add("message.avp_human.pipe_end", "%s end: %s");
        builder.add("message.avp_human.pipe_end.auto", "Auto");
        // [stated] Oct 14 - named from the block's side: Extract takes out of the block, Input puts into it. The ids
        // stay "pull" / "push", so saved pipes and every flange setting in existing worlds are unchanged.
        // [stated] Oct 23 - "change the name extract back to output so it fits": the pull mode is Output everywhere -
        // the end's button, its tooltip and the chat message - and fits its 31 px button.
        builder.add("message.avp_human.pipe_end.pull", "Output");
        builder.add("message.avp_human.pipe_end.push", "Input");
        builder.add("message.avp_human.pipe_end.both", "Both");
        builder.add("message.avp_human.pipe_end.off", "Off");
        builder.add("gui.avp_human.cable_ports", "Cable Ports");
        builder.add("gui.avp_human.cable_ports.none", "-");
        builder.add("gui.avp_human.cable_ports.click", "Click to change");
        builder.add("gui.avp_human.cable_ports.mode.auto", "Auto");
        builder.add(
            "gui.avp_human.cable_ports.mode.auto.desc",
            "Follows the other block: powers a machine, takes from a generator, uses a battery both ways"
        );
        builder.add("gui.avp_human.cable_ports.mode.out", "Out");
        builder.add("gui.avp_human.cable_ports.mode.out.desc", "Our grid powers this block");
        builder.add("gui.avp_human.cable_ports.mode.in", "In");
        builder.add("gui.avp_human.cable_ports.mode.in.desc", "This block feeds our grid");
        builder.add("gui.avp_human.cable_ports.mode.both", "Both");
        builder.add("gui.avp_human.cable_ports.mode.both.desc", "Used as a battery - charged from surplus, drawn on when short");
        builder.add("gui.avp_human.cable_ports.mode.off", "Off");
        builder.add("gui.avp_human.cable_ports.mode.off.desc", "Not connected");
        builder.add("tooltip.avp_human.requires_sentry_power", "Power - a battery or a fuse panel");
        builder.add("tooltip.avp_human.use.open_turret", "Right-click to open its power and status screen");
        builder.add("tooltip.avp_human.sentry.needs_power", "Needs power - a battery, or a fuse panel");
        builder.add("gui.avp_human.sentry.charge", "Charge");
        builder.add("gui.avp_human.sentry.charge.amount", "%s / %s FE");
        builder.add("gui.avp_human.sentry.charge.cost", "%s FE a shot");
        builder.add("gui.avp_human.sentry.ready", "Ready");
        builder.add("gui.avp_human.sentry.no_power", "No power");
        builder.add("gui.avp_human.sentry.no_chest", "No ammo chest nearby");
        builder.add("gui.avp_human.sentry.no_bullets", "Out of ammo");
        // Oct 23 - the turret screen's own line; the tooltip's "Overheated - cooling down" is wider than its display.
        builder.add("gui.avp_human.sentry.overheated", "Overheated: cooling");
        builder.add("gui.avp_human.sentry.wired", "Wired to a fuse panel");
        builder.add("gui.avp_human.sentry.battery", "Battery");
        builder.add("tooltip.avp_human.hub_upgrade", "In a machine: lets it be wired to a fuse panel - no cable needed, only range");
        builder.add("gui.avp_human.fuse_panel.devices", "%s / %s devices");
        // Oct 23 - shortened to fit the 144 px device list ("Bind devices with the wiring tool" was 159 px).
        builder.add("gui.avp_human.fuse_panel.empty", "Bind with the wiring tool");
        builder.add("gui.avp_human.fuse_panel.unknown", "Unknown device");
        builder.add("gui.avp_human.fuse_panel.unbind", "Unbind");
        builder.add("gui.avp_human.fuse_panel.out_of_range", "Out of range - not powered");
        builder.add("gui.avp_human.machine.upgrade", "Upgrade Slot");
        builder.add(
            "gui.avp_human.machine.upgrade.desc",
            "Speed and efficiency cards (they stack), a hub chip for in-wall wiring to a fuse panel, or a redstone card"
        );
        // [stated] Oct 2: the side buttons and their pop-ups.
        builder.add("gui.avp_human.machine.side.priority", "Power Priority");
        builder.add("gui.avp_human.machine.side.filter", "Input Filter");
        builder.add("gui.avp_human.machine.side.redstone", "Redstone Control");
        builder.add("gui.avp_human.machine.side.outputs", "Outputs");
        builder.add("gui.avp_human.machine.side.click", "Click to change");
        builder.add("gui.avp_human.machine.redstone.ignore", "Ignore redstone");
        builder.add("gui.avp_human.machine.redstone.high", "Run with a signal");
        builder.add("gui.avp_human.machine.redstone.low", "Run without a signal");
        builder.add("gui.avp_human.machine.filter.hint", "Left: set  Right: clear");
        builder.add("gui.avp_human.machine.side.ports", "Ports");
        builder.add("gui.avp_human.machine.port.items", "Items");
        builder.add("gui.avp_human.machine.port.fluids", "Fluids");
        builder.add("gui.avp_human.machine.port.mode.in", "In");
        builder.add("gui.avp_human.machine.port.mode.out", "Out");
        builder.add("gui.avp_human.machine.port.mode.both", "Both");
        builder.add("gui.avp_human.machine.port.mode.off", "Off");
        builder.add("gui.avp_human.machine.port.face.front", "Front");
        builder.add("gui.avp_human.machine.port.face.back", "Back");
        builder.add("gui.avp_human.machine.port.face.left", "Left");
        builder.add("gui.avp_human.machine.port.face.right", "Right");
        builder.add("gui.avp_human.machine.port.face.top", "Top");
        builder.add("gui.avp_human.machine.port.face.bottom", "Bottom");
        builder.add(
            "message.avp_human.fuse.selected",
            "Fuse panel selected (%s/%s devices) - please right-click the device you wish to connect"
        );
        // Oct 7 - wiring tool tooltip, and the desk terminal's power messages.
        builder.add("tooltip.avp_human.wiring_tool.selected", "Selected fuse panel: %s, %s, %s");
        builder.add("tooltip.avp_human.wiring_tool.readout", "Right-click a power block or cable: show its power and grid");
        builder.add(
            "tooltip.avp_human.wiring_tool.select_panel",
            "Right-click a fuse panel, then a device: connect them (again to disconnect)"
        );
        builder.add("tooltip.avp_human.wiring_tool.bind", "Or right-click the device first, then the fuse panel");
        builder.add("tooltip.avp_human.wiring_tool.toggle", "Sneak + right-click: switch a machine on/off, or open/close a power port");
        builder.add("tooltip.avp_human.wiring_tool.clear", "Sneak + right-click the air: clear the selection");
        builder.add("message.avp_human.terminal.booting", "Terminal starting up...");
        builder.add("message.avp_human.terminal.not_wired", "No power - wire it to a fuse panel, or place it on a redstone generator");
        builder.add("message.avp_human.terminal.no_power", "No power - its power source has no power");
        builder.add("message.avp_human.fuse.cleared", "Wiring selection cleared");
        builder.add("message.avp_human.fuse.bound", "Connection successful (%s/%s devices)");
        builder.add("message.avp_human.fuse.unbound", "Disconnected from the fuse panel (%s/%s devices)");
        builder.add("message.avp_human.fuse.needs_hub_chip", "Connection failed: this machine needs a hub chip first");
        builder.add("message.avp_human.fuse.cannot_bind", "Connection failed: that cannot be wired to a fuse panel");
        builder.add("message.avp_human.fuse.out_of_range", "Connection failed: out of range - %s blocks away, the fuse panel reaches %s");
        builder.add("message.avp_human.fuse.full", "Connection failed: the fuse panel is full (%s devices)");
        builder.add("message.avp_human.fuse.other_dimension", "Connection failed: the selected fuse panel is in another dimension");
        builder.add("message.avp_human.fuse.gone", "The selected fuse panel is gone - select one again");
        // [stated] Oct 14 - wiring feedback: device-first prompts, the no-power warning, the device-side failures.
        builder.add(
            "message.avp_human.fuse.device_selected",
            "Device selected - please right-click the fuse panel you wish to connect it to"
        );
        builder.add(
            "message.avp_human.fuse.sentry_selected",
            "Turret selected - please right-click the fuse panel you wish to connect it to"
        );
        builder.add(
            "message.avp_human.fuse.bound_no_power",
            "Connection successful, but the fuse panel has no power - run a powered cable into its back (%s/%s devices)"
        );
        builder.add("message.avp_human.fuse.device_other_dimension", "Connection failed: the selected device is in another dimension");
        builder.add("message.avp_human.fuse.device_gone", "The selected device is gone - select it again");
        builder.add("tooltip.avp_human.wiring_tool.selected_device", "Selected device: %s, %s, %s");
        builder.add("tooltip.avp_human.wiring_tool.selected_sentry", "Selected device: a sentry turret");
        builder.add("tooltip.avp_human.battery.charge", "Charge: %s / %s FE");
        builder.add("tooltip.avp_human.battery.rechargeable", "Rechargeable");
        builder.add("tooltip.avp_human.battery.single_use", "Not rechargeable - used up when drained");
        builder.add("gui.avp_human.workstation.storage.rows", "STORAGE  ROWS %s-%s OF %s");
        builder.add("gui.avp_human.workstation.pick", "PICK WHAT TO MAKE.");
        builder.add("gui.avp_human.workstation.missing", "MISSING MATERIALS.");
        builder.add("gui.avp_human.workstation.full", "STORAGE TOO FULL.");
        builder.add("gui.avp_human.workstation.ready", "READY. TAKE IT.");
        builder.add("gui.avp_human.workstation.material.uses", "Needs %s in all");
        builder.add("gui.avp_human.workstation.material.missing", "%s short - not enough in storage");
        builder.add("gui.avp_human.machine.keep_items", "Item Output");
        builder.add("gui.avp_human.machine.keep_items.on", "Kept - pressed blocks go to the output slot");
        builder.add("gui.avp_human.machine.keep_items.off", "Thrown away - only the fluid is kept");
        builder.add("gui.avp_human.machine.keep_fluid", "Fluid Output");
        builder.add("gui.avp_human.machine.keep_fluid.on", "Kept - squeezed fluid goes to the tank");
        builder.add("gui.avp_human.machine.keep_fluid.off", "Thrown away - only the blocks are kept");
        builder.add("gui.avp_human.machine.keep.toggle", "Click to switch");
        builder.add("gui.avp_human.machine.tank.empty", "Empty");
        builder.add("gui.avp_human.machine.tank.amount", "%s / %s mB");
        builder.add("gui.avp_human.machine.fill", "Fill Slot");
        builder.add("gui.avp_human.machine.fill.desc", "Put an empty bucket or a canister here to fill it from the tank");
        builder.add(
            "gui.avp_human.machine.filter.desc",
            "Only these items can be piped in. Empty means anything. Click with an item to add, empty-handed to clear."
        );
        builder.add("gui.avp_human.machine.status.working", "Working");
        builder.add("gui.avp_human.machine.status.idle", "Powered, idle");
        builder.add("gui.avp_human.machine.status.no_power", "No power");
        builder.add("gui.avp_human.machine.status.disabled", "Switched off");
        builder.add("gui.avp_human.machine.power", "Uses %s FE/t while working");

        builder.add("gui.avp.marine.mode.follow", "Following");
        builder.add("gui.avp.marine.mode.follow.tooltip", "This marine is following you. Click to make it hold position.");
        builder.add("gui.avp.marine.mode.hold", "Holding");
        builder.add("gui.avp.marine.mode.hold.tooltip", "This marine is holding position. Click to make it follow you.");

        builder.add("gui.avp.marine.sentry.on", "Sentry: On");
        builder.add("gui.avp.marine.sentry.off", "Sentry: Off");
        builder.add("gui.avp.marine.sentry.on.tooltip", "Guarding this position. Click to stand down and follow you again.");
        builder.add("gui.avp.marine.sentry.off.tooltip", "Post this marine here to guard the area. It will stay within 16 blocks.");
        builder.add("gui.avp.marine.sentry.configure", "\u2699");
        builder.add("gui.avp.marine.sentry.configure.tooltip", "Choose what this marine shoots while on sentry.");
        builder.add("gui.avp.marine.sentry.title", "Sentry Targets");
        builder.add("gui.avp.marine.sentry.whitelist", "Only shoot these");
        builder.add("gui.avp.marine.sentry.blacklist", "Shoot all but these");
        builder.add(
            "gui.avp.marine.sentry.whitelist.tooltip",
            "Ticked groups are the only things this marine will engage. Click to invert."
        );
        builder.add(
            "gui.avp.marine.sentry.blacklist.tooltip",
            "Ticked groups are left alone; everything else is engaged. Click to invert."
        );
        builder.add("gui.avp.marine.sentry.apply", "Apply");
        builder.add("gui.avp.marine.sentry.cancel", "Back");
        builder.add("gui.avp.marine.sentry.category.monsters", "Monsters");
        builder.add("gui.avp.marine.sentry.category.animals", "Animals");
        builder.add("gui.avp.marine.sentry.category.ambient", "Ambient");
        builder.add("gui.avp.marine.sentry.category.water_animals", "Water animals");
        builder.add("gui.avp.marine.sentry.category.water_ambient", "Water ambient");
        builder.add("gui.avp.marine.sentry.category.underground_water", "Underground water");
        builder.add("gui.avp.marine.sentry.category.players", "Players");
        builder.add("gui.avp.marine.sentry.search", "Search mobs...");
        builder.add("gui.avp.marine.sentry.named", "Named mobs");
        builder.add("gui.avp.marine.sentry.favorites", "Favourites");
        builder.add("gui.avp.marine.sentry.favorite.empty", "Empty. Shift-click to save the current setup here.");
        builder.add("gui.avp.marine.sentry.favorite.overwrite", "\nShift-click to overwrite.");
        builder.add("gui.avp.marine.end_contract", "Dismiss");
        builder.add("gui.avp.marine.end_contract.tooltip", "End this marine's contract. It keeps its issued gear and returns to patrol.");
        builder.add("gui.avp.marine.end_contract.blocked.tooltip", "Take your own items out of the pack before dismissing this marine.");
        builder.add("tooltip.avp.accuracy", "Accuracy: ");
        builder.add("tooltip.avp.ammunition", "Ammo: ");
        builder.add("tooltip.avp.ammunition_type", "Fires: ");
        builder.add("tooltip.avp.damage", "Damage: ");
        builder.add("tooltip.avp.fire_mode", "Fire Mode: ");
        builder.add("tooltip.avp.fire_rate", "Fire Rate: ");
        builder.add("tooltip.avp.knockback", "Knockback: ");
        builder.add("tooltip.avp.recoil", "Recoil: ");
        builder.add("tooltip.avp.capacity", "Capacity: ");

        builder.add(HumanTooltipTranslationKeys.EFFECT_AUTO_EQUIP_ARMOR_SET, "Auto-Equips Armor Sets");
        builder.add(HumanTooltipTranslationKeys.EFFECT_AUTO_EQUIP_ARMOR_STAND_ARMOR_SET, "Auto-Equips Armor Stand Armor Sets");
        builder.add(HumanTooltipTranslationKeys.EFFECT_AUTO_STORE_IRRADIATED_ITEMS, "Irradiated Items Auto-Stored in Chest");
        builder.add(HumanTooltipTranslationKeys.EFFECT_GUNS_AUTO_RELOAD_FROM_CHEST, "Guns Auto-Reload Ammo from Chest");
        builder.add(HumanTooltipTranslationKeys.EFFECT_NEARBY_TURRETS_USE_AMMO_FROM_CHEST, "Nearby Turrets use Ammo from Chest");
        builder.add(HumanTooltipTranslationKeys.EFFECT_RADIATION_RESISTANCE, "Radiation Resistance");

        builder.add(HumanTooltipTranslationKeys.REQUIRES_REDSTONE_POWER, "Redstone Power");
        // [stated] Oct 2: the turret fires only from its own ammo slots now (loose rounds or filled ammo chests).
        builder.add(
            HumanTooltipTranslationKeys.REQUIRES_NEARBY_AMMO_CHEST_WITH_AMMO,
            "Ammo in its ammo slots - rounds or a filled Ammo Chest"
        );
        builder.add("tooltip.avp_human.sentry.needs_ammo", "Needs %s in its ammo slots");
        builder.add("tooltip.avp_human.sentry.overheated", "Overheated - cooling down");
        builder.add("gui.avp_human.sentry.heat", "Heat");
        builder.add("gui.avp_human.sentry.side.targeting", "Targeting");
        builder.add("item.avp_human.skin_kit.named", "%s Skin Kit");
        builder.add("tooltip.avp_human.skin_kit", "Goes in a Sentry Turret's skin slot");
        builder.add("skin.avp_human.weyland_yutani", "Weyland-Yutani");
        builder.add("skin.avp_human.gold", "Gold");
        builder.add("skin.avp_human.xeno", "Xeno");
        builder.add("gui.avp_human.sentry.side.view", "View Camera");
        builder.add("gui.avp_human.sentry.camera.title", "SENTRY CAM");
        builder.add("gui.avp_human.sentry.camera.controls", "Attack: fire   Sneak: exit");
        builder.add("gui.avp_human.terminal.title", "MU/TH/UR - Defence Network");
        builder.add("gui.avp_human.terminal.empty", "No turrets on the network. Fit a turret with a Network Card.");
        builder.add("gui.avp_human.terminal.camera", "CAM");
        builder.add("gui.avp_human.terminal.hint", "Click a CAM turret to look through it");
        builder.add("message.avp_human.sentry.camera.needs_parts", "That turret needs a Camera Lens and a Network Card");
        builder.add("message.avp_human.sentry.camera.not_owner", "That is not your turret");
        builder.add("message.avp_human.sentry.camera.no_power", "That turret has no power");
        builder.add("message.avp_human.sentry.camera.too_far", "That turret is out of camera range (beyond your render distance)");
        builder.add("gui.avp_human.sentry.slot.card", "Upgrade Card");
        builder.add("gui.avp_human.sentry.slot.skin", "Skin");
        builder.add("gui.avp_human.sentry.slot.network", "Network Card (MUTHUR link)");
        builder.add("gui.avp_human.sentry.slot.sensor", "Sensor Lens");
        builder.add("gui.avp_human.sentry.slot.battery", "Battery");
        builder.add("gui.avp_human.sentry.slot.ammo", "Ammo - rounds or a filled Ammo Chest");
        builder.add(HumanTooltipTranslationKeys.STATUS_NEEDS_REDSTONE, "Needs a redstone signal");
        builder.add(HumanTooltipTranslationKeys.STATUS_NEEDS_AMMO_CHEST, "Needs an Ammo Chest within %s blocks");
        builder.add(HumanTooltipTranslationKeys.STATUS_NEEDS_MEDIUM_BULLETS, "Ammo Chest has no Medium Bullets");
        builder.add(HumanTooltipTranslationKeys.STATUS_READY, "Powered and supplied");
        builder.add(HumanTooltipTranslationKeys.USE_CHECK_TURRET_STATUS, "Right-click to check what it needs");
        builder.add(HumanTooltipTranslationKeys.USE_PICK_UP_TURRET, "Sneak + right-click to pick it back up");
    };
}
