package com.human.fabric.data.lang.en_us.provider;

import com.human.common.registry.init.item.HumanArmorItems;
import com.human.common.registry.init.item.HumanGunItems;
import com.human.common.registry.init.item.HumanItems;
import com.human.common.registry.init.item.HumanSpawnEggItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EnUsItemProvider {

    private static final HashSet<Item> TOUCHED_ENTRIES = new HashSet<>();

    public static final Consumer<FabricLanguageProvider.TranslationBuilder> CONSUMER = builder -> {
        // Combat Items
        addItem(builder, HumanItems.GRENADE, "Grenade");
        addItem(builder, HumanItems.GRENADE_INCENDIARY, "Incendiary Grenade");
        addItem(builder, HumanItems.GRENADE_IRRADIATED, "Irradiated Grenade");
        addItem(builder, HumanItems.CASELESS_BULLET, "Caseless Bullet");
        addItem(builder, HumanGunItems.F903WE_RIFLE, "F903WE Rifle");
        addItem(builder, HumanGunItems.FLAMETHROWER_SEVASTOPOL, "Flamethrower (Sevastopol)");
        addItem(builder, HumanItems.FUEL_TANK, "Fuel Tank");
        addItem(builder, HumanItems.HEAVY_BULLET, "Heavy Bullet");
        addItem(builder, HumanGunItems.M37_12_SHOTGUN, "M37-12 Shotgun");
        addItem(builder, HumanGunItems.M41A_PULSE_RIFLE, "M41A Pulse Rifle");
        addItem(builder, HumanGunItems.M42A3_SNIPER_RIFLE, "M42A3 Sniper Rifle");
        addItem(builder, HumanGunItems.M4RA_BATTLE_RIFLE, "M4RA Battle Rifle");
        addItem(builder, HumanGunItems.M56_SMARTGUN, "M56 Smartgun");
        addItem(builder, HumanGunItems.M6B_ROCKET_LAUNCHER, "M6B Rocket Launcher");
        addItem(builder, HumanGunItems.M88MOD4_COMBAT_PISTOL, "88 Mod 4 Combat Pistol");
        addItem(builder, HumanItems.MEDIUM_BULLET, "Medium Bullet");
        addItem(builder, HumanArmorItems.MK50_BOOTS, "MK50 Boots");
        addItem(builder, HumanArmorItems.MK50_CHESTPLATE, "MK50 Chestplate");
        addItem(builder, HumanArmorItems.MK50_HELMET, "MK50 Helmet");
        addItem(builder, HumanArmorItems.MK50_LEGGINGS, "MK50 Leggings");
        addItem(builder, HumanGunItems.OLD_PAINLESS, "Old Painless");
        addItem(builder, HumanArmorItems.PRESSURE_BOOTS, "Pressure Boots");
        addItem(builder, HumanArmorItems.PRESSURE_CHESTPLATE, "Pressure Chestplate");
        addItem(builder, HumanArmorItems.PRESSURE_HELMET, "Pressure Helmet");
        addItem(builder, HumanArmorItems.PRESSURE_LEGGINGS, "Pressure Leggings");
        addItem(builder, HumanItems.ROCKET, "Rocket");
        addItem(builder, HumanItems.SHOTGUN_SHELL, "Shotgun Shell");
        addItem(builder, HumanItems.SMALL_BULLET, "Small Bullet");
        addItem(builder, HumanArmorItems.STEEL_BOOTS, "Steel Boots");
        addItem(builder, HumanArmorItems.STEEL_CHESTPLATE, "Steel Chestplate");
        addItem(builder, HumanArmorItems.STEEL_HELMET, "Steel Helmet");
        addItem(builder, HumanArmorItems.STEEL_LEGGINGS, "Steel Leggings");
        addItem(builder, HumanArmorItems.TACTICAL_BOOTS, "Tactical Boots");
        addItem(builder, HumanArmorItems.TACTICAL_CHESTPLATE, "Tactical Chestplate");
        addItem(builder, HumanArmorItems.TACTICAL_HELMET, "Tactical Helmet");
        addItem(builder, HumanArmorItems.TACTICAL_LEGGINGS, "Tactical Leggings");
        addItem(builder, HumanArmorItems.TACTICAL_CAMO_BOOTS, "Tactical Camo Boots");
        addItem(builder, HumanArmorItems.TACTICAL_CAMO_CHESTPLATE, "Tactical Camo Chestplate");
        addItem(builder, HumanArmorItems.TACTICAL_CAMO_HELMET, "Tactical Camo Helmet");
        addItem(builder, HumanArmorItems.TACTICAL_CAMO_LEGGINGS, "Tactical Camo Leggings");
        addItem(builder, HumanArmorItems.TITANIUM_BOOTS, "Titanium Boots");
        addItem(builder, HumanArmorItems.TITANIUM_CHESTPLATE, "Titanium Chestplate");
        addItem(builder, HumanArmorItems.TITANIUM_HELMET, "Titanium Helmet");
        addItem(builder, HumanArmorItems.TITANIUM_LEGGINGS, "Titanium Leggings");
        addItem(builder, HumanArmorItems.WY_APE_BOOTS, "WY Ape Boots");
        addItem(builder, HumanArmorItems.WY_APE_CHESTPLATE, "WY Ape Chestplate");
        addItem(builder, HumanArmorItems.WY_APE_HELMET, "WY Ape Helmet");
        addItem(builder, HumanArmorItems.WY_APE_LEGGINGS, "WY Ape Leggings");
        addItem(builder, HumanArmorItems.WY_COMMANDO_BOOTS, "WY Commando Boots");
        addItem(builder, HumanArmorItems.WY_COMMANDO_CHESTPLATE, "WY Commando Chestplate");
        addItem(builder, HumanArmorItems.WY_COMMANDO_HELMET, "WY Commando Helmet");
        addItem(builder, HumanArmorItems.WY_COMMANDO_LEGGINGS, "WY Commando Leggings");
        addItem(builder, HumanArmorItems.WY_ELITE_BOOTS, "WY Elite Boots");
        addItem(builder, HumanArmorItems.WY_ELITE_CHESTPLATE, "WY Elite Chestplate");
        addItem(builder, HumanArmorItems.WY_ELITE_HELMET, "WY Elite Helmet");
        addItem(builder, HumanArmorItems.WY_ELITE_LEGGINGS, "WY Elite Leggings");
        addItem(builder, HumanGunItems.ZX_76_SHOTGUN, "ZX-76 Shotgun");

        // Ingredient Items
        addItem(builder, HumanItems.NUCLEAR_BATTERY, "Nuclear Battery");
        addItem(builder, HumanItems.REDSTONE_CRYSTAL, "Redstone Crystal");
        addItem(builder, HumanItems.SERVO, "Servo");
        addItem(builder, HumanItems.SPEAKER, "Speaker");
        addItem(builder, HumanItems.ALUMINUM_INGOT, "Aluminum Ingot");
        addItem(builder, HumanItems.AUTUNITE_DUST, "Autunite Dust");
        addItem(builder, HumanItems.BARREL, "Barrel");
        addItem(builder, HumanItems.BATTERY_PACK, "Battery Pack");
        addItem(builder, HumanItems.BLUEPRINT_F903WE_RIFLE, "F903WE Rifle Blueprint");
        addItem(builder, HumanItems.BLUEPRINT_FLAMETHROWER_SEVASTOPOL, "Flamethrower (Sevastopol) Blueprint");
        addItem(builder, HumanItems.BLUEPRINT_M37_12_SHOTGUN, "M37-12 Shotgun Blueprint");
        addItem(builder, HumanItems.BLUEPRINT_M41A_PULSE_RIFLE, "M41A Pulse Rifle Blueprint");
        addItem(builder, HumanItems.BLUEPRINT_M42A3_SNIPER_RIFLE, "M42A3 Sniper Rifle Blueprint");
        addItem(builder, HumanItems.BLUEPRINT_M4RA_BATTLE_RIFLE, "M4RA Battle Rifle Blueprint");
        addItem(builder, HumanItems.BLUEPRINT_M56_SMARTGUN, "M56 Smartgun Blueprint");
        addItem(builder, HumanItems.BLUEPRINT_M6B_ROCKET_LAUNCHER, "M6B Rocket Launcher Blueprint");
        addItem(builder, HumanItems.BLUEPRINT_M88MOD4_COMBAT_PISTOL, "M88 Mod 4 Combat Pistol Blueprint");
        addItem(builder, HumanItems.BLUEPRINT_OLD_PAINLESS, "Old Painless Blueprint");
        addItem(builder, HumanItems.BLUEPRINT_ZX_76_SHOTGUN, "ZX-76 Shotgun Blueprint");
        addItem(builder, HumanItems.BRASS_INGOT, "Brass Ingot");
        addItem(builder, HumanItems.CAPACITOR, "Capacitor");
        addItem(builder, HumanItems.CARBON_DUST, "Carbon Dust");
        addItem(builder, HumanItems.CORNBREAD, "Cornbread");
        addItem(builder, HumanItems.CPU, "CPU");
        addItem(builder, HumanItems.DIODE, "Diode");
        addItem(builder, HumanItems.DOG_TAG, "Dog Tag");
        addItem(builder, HumanItems.FERROALUMINUM_INGOT, "Ferroaluminum Ingot");
        addItem(builder, HumanItems.GRIP, "Grip");
        addItem(builder, HumanItems.INTEGRATED_CIRCUIT, "Integrated Circuit");
        addItem(builder, HumanItems.LEAD_INGOT, "Lead Ingot");
        addItem(builder, HumanItems.LED, "LED");
        addItem(builder, HumanItems.LED_DISPLAY, "LED Display");
        addItem(builder, HumanItems.LITHIUM_DUST, "Lithium Dust");
        addItem(builder, HumanItems.MINIGUN_BARREL, "Minigun Barrel");
        addItem(builder, HumanItems.NEODYMIUM_MAGNET, "Neodymium Magnet");
        addItem(builder, HumanItems.POLYMER, "Polymer");
        addItem(builder, HumanItems.RAW_BAUXITE, "Raw Bauxite");
        addItem(builder, HumanItems.RAW_BRASS, "Raw Brass");
        addItem(builder, HumanItems.RAW_CRUDE_IRON, "Raw Crude Iron");
        addItem(builder, HumanItems.RAW_FERROBAUXITE, "Raw Ferrobauxite");
        addItem(builder, HumanItems.RAW_GALENA, "Raw Galena");
        addItem(builder, HumanItems.RAW_MONAZITE, "Raw Monazite");
        addItem(builder, HumanItems.SILICON, "Silicon");
        addItem(builder, HumanItems.RAW_TITANIUM, "Raw Titanium");
        addItem(builder, HumanItems.RAW_ZINC, "Raw Zinc");
        addItem(builder, HumanItems.RECEIVER, "Receiver");
        addItem(builder, HumanItems.REGULATOR, "Regulator");
        addItem(builder, HumanItems.RESISTOR, "Resistor");
        addItem(builder, HumanItems.ROCKET_BARREL, "Rocket Barrel");
        addItem(builder, HumanItems.SMART_BARREL, "Smart Barrel");
        addItem(builder, HumanItems.SMART_RECEIVER, "Smart Receiver");
        addItem(builder, HumanItems.STEEL_INGOT, "Steel Ingot");
        addItem(builder, HumanItems.STOCK, "Stock");
        addItem(builder, HumanItems.TITANIUM_INGOT, "Titanium Ingot");
        addItem(builder, HumanItems.TRANSISTOR, "Transistor");
        addItem(builder, HumanItems.URANIUM_INGOT, "Uranium Ingot");
        addItem(builder, HumanItems.ZINC_INGOT, "Zinc Ingot");
        addItem(builder, HumanItems.ALUMINUM_NUGGET, "Aluminum Nugget");
        addItem(builder, HumanItems.BRASS_NUGGET, "Brass Nugget");
        addItem(builder, HumanItems.FERROALUMINUM_NUGGET, "Ferroaluminum Nugget");
        addItem(builder, HumanItems.LEAD_NUGGET, "Lead Nugget");
        addItem(builder, HumanItems.STEEL_NUGGET, "Steel Nugget");
        addItem(builder, HumanItems.TITANIUM_NUGGET, "Titanium Nugget");
        addItem(builder, HumanItems.URANIUM_NUGGET, "Uranium Nugget");
        addItem(builder, HumanItems.ZINC_NUGGET, "Zinc Nugget");

        // Tools & Utilities Items
        addItem(builder, HumanItems.ARMOR_CASE, "Armor Case");
        addItem(builder, HumanItems.CANISTER, "Canister");
        addItem(builder, HumanItems.GENE_READER, "Gene Reader");
        addItem(builder, HumanItems.WATER_CANISTER, "Water Canister");
        addItem(builder, HumanItems.LAVA_CANISTER, "Lava Canister");
        addItem(builder, HumanItems.MILK_CANISTER, "Milk Canister");
        addItem(builder, HumanItems.POWDER_SNOW_CANISTER, "Powder Snow Canister");
        addItem(builder, HumanItems.STEEL_AXE, "Steel Axe");
        addItem(builder, HumanItems.STEEL_HOE, "Steel Hoe");
        addItem(builder, HumanItems.STEEL_PICKAXE, "Steel Pickaxe");
        addItem(builder, HumanItems.STEEL_SHOVEL, "Steel Shovel");
        addItem(builder, HumanItems.STEEL_SWORD, "Steel Sword");
        addItem(builder, HumanItems.SYRINGE, "Syringe");
        addItem(builder, HumanItems.TACTICAL_KNIFE, "Tactical Knife");
        addItem(builder, HumanItems.TITANIUM_AXE, "Titanium Axe");
        addItem(builder, HumanItems.TITANIUM_HOE, "Titanium Hoe");
        addItem(builder, HumanItems.TITANIUM_PICKAXE, "Titanium Pickaxe");
        addItem(builder, HumanItems.TITANIUM_SHOVEL, "Titanium Shovel");
        addItem(builder, HumanItems.TITANIUM_SWORD, "Titanium Sword");

        // Spawn Egg Items
        addItem(builder, HumanSpawnEggItems.MARINE_DOG_SPAWN_EGG, "Marine Dog Spawn Egg");
        addItem(builder, HumanSpawnEggItems.MARINE_SPAWN_EGG, "Marine Spawn Egg");
    };

    private static void addItem(
        FabricLanguageProvider.TranslationBuilder translationBuilder,
        Supplier<? extends Item> itemSupplier,
        String value
    ) {
        addItem(translationBuilder, itemSupplier.get(), value);
    }

    private static void addItem(FabricLanguageProvider.TranslationBuilder translationBuilder, Item item, String value) {
        TOUCHED_ENTRIES.add(item);
        translationBuilder.add(item, value);
    }

}
