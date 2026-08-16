package com.human.fabric.data.model;

import com.human.common.registry.init.item.HumanArmorItems;
import com.human.common.registry.init.item.HumanItems;
import com.human.common.registry.init.item.HumanSpawnEggItems;
import com.human.common.registry.init.item.block.HumanFerroaluminumBlockItems;
import com.human.common.registry.init.item.block.HumanSteelBlockItems;
import com.human.common.registry.init.item.block.HumanTitaniumBlockItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ItemModelProvider extends FabricModelProvider {

    public ItemModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generators) {}

    @Override
    public void generateItemModels(ItemModelGenerators generators) {
        generateStandardItem(generators, HumanItems.GRENADE);
        generateStandardItem(generators, HumanItems.GRENADE_INCENDIARY);
        generateStandardItem(generators, HumanItems.GRENADE_IRRADIATED);
        generateStandardItem(generators, HumanItems.CASELESS_BULLET);
        generateStandardItem(generators, HumanItems.HEAVY_BULLET);
        generateStandardItem(generators, HumanItems.SMALL_BULLET);
        generateStandardItem(generators, HumanItems.MEDIUM_BULLET);
        generateStandardItem(generators, HumanItems.SHOTGUN_SHELL);
        generateStandardItem(generators, HumanItems.FUEL_TANK);
        generateStandardItem(generators, HumanItems.DRUM_CARTRIDGE);
        generateStandardItem(generators, HumanItems.DRUM_CANNISTER);
        generateStandardItem(generators, HumanArmorItems.WY_APE_BOOTS);
        generateStandardItem(generators, HumanArmorItems.WY_APE_CHESTPLATE);
        generateStandardItem(generators, HumanArmorItems.WY_APE_HELMET);
        generateStandardItem(generators, HumanArmorItems.WY_APE_LEGGINGS);
        generateTwoLayeredItem(generators, HumanArmorItems.MK50_BOOTS);
        generateTwoLayeredItem(generators, HumanArmorItems.MK50_CHESTPLATE);
        generateTwoLayeredItem(generators, HumanArmorItems.MK50_HELMET);
        generateTwoLayeredItem(generators, HumanArmorItems.MK50_LEGGINGS);
        generateTwoLayeredItem(generators, HumanArmorItems.PRESSURE_BOOTS);
        generateTwoLayeredItem(generators, HumanArmorItems.PRESSURE_CHESTPLATE);
        generateTwoLayeredItem(generators, HumanArmorItems.PRESSURE_HELMET);
        generateTwoLayeredItem(generators, HumanArmorItems.PRESSURE_LEGGINGS);
        generateStandardItem(generators, HumanItems.ROCKET);
        generateStandardItem(generators, HumanArmorItems.STEEL_BOOTS);
        generateStandardItem(generators, HumanArmorItems.STEEL_CHESTPLATE);
        generateStandardItem(generators, HumanArmorItems.STEEL_HELMET);
        generateStandardItem(generators, HumanArmorItems.STEEL_LEGGINGS);
        generateStandardItem(generators, HumanArmorItems.TACTICAL_BOOTS);
        generateStandardItem(generators, HumanArmorItems.TACTICAL_CHESTPLATE);
        generateStandardItem(generators, HumanArmorItems.TACTICAL_HELMET);
        generateStandardItem(generators, HumanArmorItems.TACTICAL_LEGGINGS);
        generateStandardItem(generators, HumanArmorItems.TACTICAL_CAMO_BOOTS);
        generateStandardItem(generators, HumanArmorItems.TACTICAL_CAMO_CHESTPLATE);
        generateStandardItem(generators, HumanArmorItems.TACTICAL_CAMO_HELMET);
        generateStandardItem(generators, HumanArmorItems.TACTICAL_CAMO_LEGGINGS);
        generateStandardItem(generators, HumanArmorItems.TITANIUM_BOOTS);
        generateStandardItem(generators, HumanArmorItems.TITANIUM_CHESTPLATE);
        generateStandardItem(generators, HumanArmorItems.TITANIUM_HELMET);
        generateStandardItem(generators, HumanArmorItems.TITANIUM_LEGGINGS);
        generateTwoLayeredItem(generators, HumanArmorItems.WY_COMMANDO_BOOTS);
        generateTwoLayeredItem(generators, HumanArmorItems.WY_COMMANDO_CHESTPLATE);
        generateTwoLayeredItem(generators, HumanArmorItems.WY_COMMANDO_HELMET);
        generateTwoLayeredItem(generators, HumanArmorItems.WY_COMMANDO_LEGGINGS);
        generateTwoLayeredItem(generators, HumanArmorItems.WY_ELITE_BOOTS);
        generateTwoLayeredItem(generators, HumanArmorItems.WY_ELITE_CHESTPLATE);
        generateTwoLayeredItem(generators, HumanArmorItems.WY_ELITE_HELMET);
        generateTwoLayeredItem(generators, HumanArmorItems.WY_ELITE_LEGGINGS);

        generateStandardItem(generators, HumanItems.ALUMINUM_INGOT);
        generateStandardItem(generators, HumanItems.AUTUNITE_DUST);
        generateStandardItem(generators, HumanItems.BARREL);
        generateStandardItem(generators, HumanItems.BATTERY_PACK);
        generateStandardItem(generators, HumanItems.BLUEPRINT_M37_12_SHOTGUN);
        generateStandardItem(generators, HumanItems.BLUEPRINT_F903WE_RIFLE);
        generateStandardItem(generators, HumanItems.BLUEPRINT_FLAMETHROWER_SEVASTOPOL);
        generateStandardItem(generators, HumanItems.BLUEPRINT_M41A_PULSE_RIFLE);
        generateStandardItem(generators, HumanItems.BLUEPRINT_M56_SMARTGUN);
        generateStandardItem(generators, HumanItems.BLUEPRINT_M6B_ROCKET_LAUNCHER);
        generateStandardItem(generators, HumanItems.BLUEPRINT_M88MOD4_COMBAT_PISTOL);
        generateStandardItem(generators, HumanItems.BLUEPRINT_OLD_PAINLESS);
        generateStandardItem(generators, HumanItems.BLUEPRINT_M42A3_SNIPER_RIFLE);
        generateStandardItem(generators, HumanItems.BLUEPRINT_M4RA_BATTLE_RIFLE);
        generateStandardItem(generators, HumanItems.BLUEPRINT_ZX_76_SHOTGUN);
        generateStandardItem(generators, HumanItems.BRASS_INGOT);
        generateStandardItem(generators, HumanItems.CANISTER);
        generateStandardItem(generators, HumanItems.WATER_CANISTER);
        generateStandardItem(generators, HumanItems.LAVA_CANISTER);
        generateStandardItem(generators, HumanItems.MILK_CANISTER);
        generateStandardItem(generators, HumanItems.POWDER_SNOW_CANISTER);
        generateStandardItem(generators, HumanItems.CAPACITOR);
        generateStandardItem(generators, HumanItems.NUCLEAR_BATTERY);
        generateStandardItem(generators, HumanItems.REDSTONE_CRYSTAL);
        generateStandardItem(generators, HumanItems.SERVO);
        generateStandardItem(generators, HumanItems.SPEAKER);
        generateStandardItem(generators, HumanItems.ASH_BALL);
        generateStandardItem(generators, HumanItems.CARBON_DUST);
        generateStandardItem(generators, HumanItems.CORNBREAD);
        generateStandardItem(generators, HumanItems.CPU);
        generateStandardItem(generators, HumanItems.DIODE);
        generateStandardItem(generators, HumanItems.DOG_TAG);
        generateStandardItem(generators, HumanItems.FERROALUMINUM_INGOT);
        generateStandardItem(generators, HumanItems.GRIP);
        generateStandardItem(generators, HumanItems.INTEGRATED_CIRCUIT);
        generateStandardItem(generators, HumanItems.LEAD_INGOT);
        generateStandardItem(generators, HumanItems.LED);
        generateStandardItem(generators, HumanItems.LED_DISPLAY);
        generateStandardItem(generators, HumanItems.LITHIUM_DUST);
        generateStandardItem(generators, HumanItems.MINIGUN_BARREL);
        generateStandardItem(generators, HumanItems.NEODYMIUM_MAGNET);
        generateStandardItem(generators, HumanItems.POLYMER);
        generateStandardItem(generators, HumanItems.RAW_BAUXITE);
        generateStandardItem(generators, HumanItems.RAW_BRASS);
        generateStandardItem(generators, HumanItems.RAW_CRUDE_IRON);
        generateStandardItem(generators, HumanItems.RAW_FERROBAUXITE);
        generateStandardItem(generators, HumanItems.RAW_GALENA);
        generateStandardItem(generators, HumanItems.RAW_MONAZITE);
        generateStandardItem(generators, HumanItems.RAW_TITANIUM);
        generateStandardItem(generators, HumanItems.RAW_ZINC);
        generateStandardItem(generators, HumanItems.RECEIVER);
        generateStandardItem(generators, HumanItems.REGULATOR);
        generateStandardItem(generators, HumanItems.RESISTOR);
        generateStandardItem(generators, HumanItems.ROCKET_BARREL);
        generateStandardItem(generators, HumanItems.SILICON);
        generateStandardItem(generators, HumanItems.SMART_BARREL);
        generateStandardItem(generators, HumanItems.SMART_RECEIVER);
        generateHandheldItem(generators, HumanItems.STEEL_AXE);
        generateHandheldItem(generators, HumanItems.STEEL_HOE);
        generateStandardItem(generators, HumanItems.STEEL_INGOT);
        generateHandheldItem(generators, HumanItems.STEEL_PICKAXE);
        generateHandheldItem(generators, HumanItems.STEEL_SHOVEL);
        generateHandheldItem(generators, HumanItems.STEEL_SWORD);
        generateHandheldItem(generators, HumanItems.TACTICAL_KNIFE);
        generateStandardItem(generators, HumanItems.STOCK);
        generateHandheldItem(generators, HumanItems.TITANIUM_AXE);
        generateHandheldItem(generators, HumanItems.TITANIUM_HOE);
        generateStandardItem(generators, HumanItems.TITANIUM_INGOT);
        generateHandheldItem(generators, HumanItems.TITANIUM_PICKAXE);
        generateHandheldItem(generators, HumanItems.TITANIUM_SHOVEL);
        generateHandheldItem(generators, HumanItems.TITANIUM_SWORD);
        generateStandardItem(generators, HumanItems.TRANSISTOR);
        generateStandardItem(generators, HumanItems.URANIUM_INGOT);
        generateStandardItem(generators, HumanItems.ZINC_INGOT);

        generateStandardItem(generators, HumanItems.FERROALUMINUM_NUGGET);
        generateStandardItem(generators, HumanItems.STEEL_NUGGET);
        generateStandardItem(generators, HumanItems.GENE_READER);
        generateStandardItem(generators, HumanItems.SYRINGE);
        generateStandardItem(generators, HumanItems.BRASS_NUGGET);
        generateStandardItem(generators, HumanItems.TITANIUM_NUGGET);
        generateStandardItem(generators, HumanItems.ZINC_NUGGET);
        generateStandardItem(generators, HumanItems.LEAD_NUGGET);
        generateStandardItem(generators, HumanItems.URANIUM_NUGGET);
        generateStandardItem(generators, HumanItems.ALUMINUM_NUGGET);

        generateHandheldItem(generators, HumanFerroaluminumBlockItems.FERROALUMINUM_DOOR);
        generateHandheldItem(generators, HumanSteelBlockItems.STEEL_DOOR);
        generateHandheldItem(generators, HumanTitaniumBlockItems.TITANIUM_DOOR);

        HumanSpawnEggItems.REGISTRY.getAll()
            .forEach(spawnEggItem -> generateStandardItem(generators, spawnEggItem.get()));
    }

    private void generateHandheldItem(ItemModelGenerators generators, Supplier<? extends Item> itemSupplier) {
        generateHandheldItem(generators, itemSupplier.get());
    }

    private void generateHandheldItem(ItemModelGenerators generators, Item item) {
        generateStandardItem(generators, item, ModelTemplates.FLAT_HANDHELD_ITEM);
    }

    private void generateStandardItem(ItemModelGenerators generators, Supplier<? extends Item> itemSupplier) {
        generateStandardItem(generators, itemSupplier.get());
    }

    private void generateStandardItem(ItemModelGenerators generators, Item item) {
        generateStandardItem(generators, item, ModelTemplates.FLAT_ITEM);
    }

    private void generateStandardItem(ItemModelGenerators generators, Item item, ModelTemplate modelTemplate) {
        generators.generateFlatItem(item, modelTemplate);
    }

    private void generateTwoLayeredItem(ItemModelGenerators generators, Supplier<? extends Item> itemSupplier) {
        var item = itemSupplier.get();
        var modelLocation = ModelLocationUtils.getModelLocation(item);
        var layer0 = TextureMapping.getItemTexture(item);
        var layer1 = TextureMapping.getItemTexture(item, "_overlay");

        ModelTemplates.TWO_LAYERED_ITEM.create(modelLocation, TextureMapping.layered(layer0, layer1), generators.output);
    }

    @Override
    public @NotNull String getName() {
        return "Item Model Definitions";
    }
}
