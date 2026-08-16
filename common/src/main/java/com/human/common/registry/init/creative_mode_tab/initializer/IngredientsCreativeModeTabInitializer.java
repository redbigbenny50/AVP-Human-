package com.human.common.registry.init.creative_mode_tab.initializer;

import com.human.common.registry.init.item.HumanItems;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Consumer;

public class IngredientsCreativeModeTabInitializer {

    public static final Consumer<CreativeModeTab.Output> OUTPUT_CONSUMER = output -> {
        // Raw materials
        CreativeModeTabUtil.accept(output, HumanItems.AUTUNITE_DUST);
        CreativeModeTabUtil.accept(output, HumanItems.ASH_BALL);
        CreativeModeTabUtil.accept(output, HumanItems.CARBON_DUST);
        CreativeModeTabUtil.accept(output, HumanItems.RAW_BAUXITE);
        CreativeModeTabUtil.accept(output, HumanItems.RAW_BRASS);
        CreativeModeTabUtil.accept(output, HumanItems.RAW_CRUDE_IRON);
        CreativeModeTabUtil.accept(output, HumanItems.RAW_FERROBAUXITE);
        CreativeModeTabUtil.accept(output, HumanItems.RAW_GALENA);
        CreativeModeTabUtil.accept(output, HumanItems.RAW_MONAZITE);
        CreativeModeTabUtil.accept(output, HumanItems.SILICON);
        CreativeModeTabUtil.accept(output, HumanItems.RAW_TITANIUM);
        CreativeModeTabUtil.accept(output, HumanItems.RAW_ZINC);
        CreativeModeTabUtil.accept(output, HumanItems.ALUMINUM_NUGGET);
        CreativeModeTabUtil.accept(output, HumanItems.BRASS_NUGGET);
        CreativeModeTabUtil.accept(output, HumanItems.FERROALUMINUM_NUGGET);
        CreativeModeTabUtil.accept(output, HumanItems.LEAD_NUGGET);
        CreativeModeTabUtil.accept(output, HumanItems.STEEL_NUGGET);
        CreativeModeTabUtil.accept(output, HumanItems.TITANIUM_NUGGET);
        CreativeModeTabUtil.accept(output, HumanItems.URANIUM_NUGGET);
        CreativeModeTabUtil.accept(output, HumanItems.ZINC_NUGGET);
        // Refined materials
        CreativeModeTabUtil.accept(output, HumanItems.ALUMINUM_INGOT);
        CreativeModeTabUtil.accept(output, HumanItems.BRASS_INGOT);
        CreativeModeTabUtil.accept(output, HumanItems.FERROALUMINUM_INGOT);
        CreativeModeTabUtil.accept(output, HumanItems.LEAD_INGOT);
        CreativeModeTabUtil.accept(output, HumanItems.LITHIUM_DUST);
        CreativeModeTabUtil.accept(output, HumanItems.NEODYMIUM_MAGNET);
        CreativeModeTabUtil.accept(output, HumanItems.STEEL_INGOT);
        CreativeModeTabUtil.accept(output, HumanItems.TITANIUM_INGOT);
        CreativeModeTabUtil.accept(output, HumanItems.URANIUM_INGOT);
        CreativeModeTabUtil.accept(output, HumanItems.ZINC_INGOT);
        CreativeModeTabUtil.accept(output, HumanItems.POLYMER);
        CreativeModeTabUtil.accept(output, HumanItems.REDSTONE_CRYSTAL);
        // Electronic materials
        CreativeModeTabUtil.accept(output, HumanItems.BATTERY_PACK);
        CreativeModeTabUtil.accept(output, HumanItems.CAPACITOR);
        CreativeModeTabUtil.accept(output, HumanItems.CPU);
        CreativeModeTabUtil.accept(output, HumanItems.DIODE);
        CreativeModeTabUtil.accept(output, HumanItems.INTEGRATED_CIRCUIT);
        CreativeModeTabUtil.accept(output, HumanItems.LED);
        CreativeModeTabUtil.accept(output, HumanItems.LED_DISPLAY);
        CreativeModeTabUtil.accept(output, HumanItems.REGULATOR);
        CreativeModeTabUtil.accept(output, HumanItems.RESISTOR);
        CreativeModeTabUtil.accept(output, HumanItems.TRANSISTOR);
        CreativeModeTabUtil.accept(output, HumanItems.SERVO);
        CreativeModeTabUtil.accept(output, HumanItems.SPEAKER);
        CreativeModeTabUtil.accept(output, HumanItems.NUCLEAR_BATTERY);

        // Blueprint materials
        CreativeModeTabUtil.accept(output, HumanItems.BLUEPRINT_F903WE_RIFLE);
        CreativeModeTabUtil.accept(output, HumanItems.BLUEPRINT_FLAMETHROWER_SEVASTOPOL);
        CreativeModeTabUtil.accept(output, HumanItems.BLUEPRINT_M37_12_SHOTGUN);
        CreativeModeTabUtil.accept(output, HumanItems.BLUEPRINT_M41A_PULSE_RIFLE);
        CreativeModeTabUtil.accept(output, HumanItems.BLUEPRINT_M42A3_SNIPER_RIFLE);
        CreativeModeTabUtil.accept(output, HumanItems.BLUEPRINT_M4RA_BATTLE_RIFLE);
        CreativeModeTabUtil.accept(output, HumanItems.BLUEPRINT_M56_SMARTGUN);
        CreativeModeTabUtil.accept(output, HumanItems.BLUEPRINT_M6B_ROCKET_LAUNCHER);
        CreativeModeTabUtil.accept(output, HumanItems.BLUEPRINT_M88MOD4_COMBAT_PISTOL);
        CreativeModeTabUtil.accept(output, HumanItems.BLUEPRINT_OLD_PAINLESS);
        CreativeModeTabUtil.accept(output, HumanItems.BLUEPRINT_ZX_76_SHOTGUN);

        // Gun part materials
        CreativeModeTabUtil.accept(output, HumanItems.BARREL);
        CreativeModeTabUtil.accept(output, HumanItems.GRIP);
        CreativeModeTabUtil.accept(output, HumanItems.MINIGUN_BARREL);
        CreativeModeTabUtil.accept(output, HumanItems.RECEIVER);
        CreativeModeTabUtil.accept(output, HumanItems.ROCKET_BARREL);
        CreativeModeTabUtil.accept(output, HumanItems.SMART_BARREL);
        CreativeModeTabUtil.accept(output, HumanItems.SMART_RECEIVER);
        CreativeModeTabUtil.accept(output, HumanItems.STOCK);
    };
}
