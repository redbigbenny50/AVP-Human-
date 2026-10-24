package com.human.fabric.data.tag;

import com.blib.api.common.tag.v1.BLibItemTags;
import com.blib.api.common.tag.v1.CommonItemTags;
import com.human.Human;
import com.human.common.gameplay.item.GunItem;
import com.human.common.registry.init.block.CoreBlocks;
import com.human.common.registry.init.block.HumanPaddingBlocks;
import com.human.common.registry.init.block.HumanPlasticBlocks;
import com.human.common.registry.init.item.HumanArmorItems;
import com.human.common.registry.init.item.HumanBlockItems;
import com.human.common.registry.init.item.HumanItems;
import com.human.common.registry.init.item.block.HumanFerroaluminumBlockItems;
import com.human.common.registry.init.item.block.HumanIndustrialGlassBlockItems;
import com.human.common.registry.init.item.block.HumanSteelBlockItems;
import com.human.common.registry.init.item.block.HumanTitaniumBlockItems;
import com.human.common.registry.tag.HumanItemTags;
import com.human.compatibility.HumanCommonItemTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class HumanItemTagProvider extends FabricTagProvider.ItemTagProvider {

    public HumanItemTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        getOrCreateTagBuilder(HumanCommonItemTags.LATEX).add(HumanItems.LATEX_SHEET.get());
        // Oct 7 - [stated] the APE suit mask is ignored by Gigeresque facehuggers.
        getOrCreateTagBuilder(com.human.common.registry.tag.HumanItemTags.FACEHUGGER_PROOF_HELMETS)
            .add(com.human.common.registry.init.item.HumanArmorItems.WY_APE_HELMET.get());
        // [stated] Sep 29/30: what the electronics workstation makes. It only ever crafts a member of this tag.
        // Empty on purpose: the grinder decrafts anything with a crafting recipe; pack makers list exceptions here.
        getOrCreateTagBuilder(com.human.common.registry.tag.HumanItemTags.NO_DECRAFT);

        getOrCreateTagBuilder(com.human.common.registry.tag.HumanItemTags.ELECTRONICS)
            .add(HumanItems.BATTERY_PACK.get())
            .add(HumanItems.CAPACITOR.get())
            .add(HumanItems.CPU.get())
            .add(HumanItems.FAN.get())
            .add(HumanItems.HEAT_SINK.get())
            .add(HumanItems.COPPER_COIL.get())
            .add(HumanItems.RAM.get())
            .add(HumanItems.SSD.get())
            .add(HumanItems.MOTHERBOARD.get())
            .add(HumanItems.POWER_SUPPLY.get())
            .add(HumanItems.DIODE.get())
            .add(HumanItems.INTEGRATED_CIRCUIT.get())
            .add(HumanItems.LED.get())
            .add(HumanItems.LED_DISPLAY.get())
            .add(HumanItems.NEODYMIUM_MAGNET.get())
            .add(HumanItems.RECEIVER.get())
            .add(HumanItems.REGULATOR.get())
            .add(HumanItems.RESISTOR.get())
            .add(HumanItems.SERVO.get())
            // [stated] Oct 20 - the speaker and the resonator are made at the electronics workstation too.
            .add(HumanItems.SPEAKER.get())
            .add(com.human.common.registry.init.HumanBlocks.RESONATOR_BLOCK.get().asItem())
            .add(HumanItems.SMART_RECEIVER.get())
            .add(HumanItems.TRANSISTOR.get())
            .add(com.human.common.registry.init.item.HumanBlockItems.DESK_TERMINAL_BLOCK.get())
            .add(com.human.common.registry.init.item.HumanBlockItems.MACHINE_BLOCK.get());
        getOrCreateTagBuilder(HumanCommonItemTags.RUBBER).add(HumanItems.RUBBER_GLOBULE.get());

        addArmors();
        addRadioactiveItems();
        addCommonItems();
        addCompatibilityItems();
        addAutomatedTagItems();
        addArmorRepairIngredientItems();

        getOrCreateTagBuilder(HumanItemTags.AMMO_ITEMS)
            .add(
                HumanItems.CASELESS_BULLET.get(),
                HumanItems.HEAVY_BULLET.get(),
                HumanItems.SMALL_BULLET.get(),
                HumanItems.MEDIUM_BULLET.get(),
                HumanItems.SHOTGUN_SHELL.get(),
                HumanItems.ROCKET.get(),
                HumanItems.FUEL_TANK.get()
            );

        getOrCreateTagBuilder(BLibItemTags.IRON_BLOCK_LIKE)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_ALUMINUM)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_FERROALUMINUM)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_STEEL)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_ZINC);

        getOrCreateTagBuilder(BLibItemTags.IRON_INGOT_LIKE)
            .addTag(HumanCommonItemTags.INGOTS_ALUMINUM)
            .addTag(HumanCommonItemTags.INGOTS_FERROALUMINUM)
            .addTag(HumanCommonItemTags.INGOTS_STEEL)
            .addTag(HumanCommonItemTags.INGOTS_ZINC);

        var paddingBlocksTagBuilder = getOrCreateTagBuilder(HumanItemTags.PADDING_BLOCKS);

        HumanPaddingBlocks.DYE_COLOR_TO_PADDING
            .values()
            .stream()
            .map(Supplier::get)
            .map(Block::asItem)
            .forEach(paddingBlocksTagBuilder::add);

        var plasticTagBuilder = getOrCreateTagBuilder(HumanItemTags.PLASTIC);

        TagProviderUtil.getPlasticBlockStream()
            .map(Block::asItem)
            .forEach(plasticTagBuilder::add);

        var plasticBlocksTagBuilder = getOrCreateTagBuilder(HumanItemTags.PLASTIC_BLOCKS);

        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC
            .values()
            .stream()
            .map(Supplier::get)
            .map(Block::asItem)
            .forEach(plasticBlocksTagBuilder::add);

        var industrialGlassBlockTagBuilder = getOrCreateTagBuilder(HumanItemTags.INDUSTRIAL_GLASS_BLOCK);

        industrialGlassBlockTagBuilder.add(HumanIndustrialGlassBlockItems.INDUSTRIAL_GLASS.get());
        HumanIndustrialGlassBlockItems.DYE_COLOR_TO_INDUSTRIAL_GLASS.forEach(
            ($, blockItemSupplier) -> industrialGlassBlockTagBuilder.add(blockItemSupplier.get())
        );

        var industrialGlassPaneTagBuilder = getOrCreateTagBuilder(HumanItemTags.INDUSTRIAL_GLASS_PANE);

        industrialGlassPaneTagBuilder.add(HumanIndustrialGlassBlockItems.INDUSTRIAL_GLASS_PANE.get());
        HumanIndustrialGlassBlockItems.DYE_COLOR_TO_INDUSTRIAL_GLASS_PANE.forEach(
            ($, blockItemSupplier) -> industrialGlassPaneTagBuilder.add(blockItemSupplier.get())
        );

        var industrialGlassTagBuilder = getOrCreateTagBuilder(HumanItemTags.INDUSTRIAL_GLASS)
            .addTag(HumanItemTags.INDUSTRIAL_GLASS_BLOCK)
            .addTag(HumanItemTags.INDUSTRIAL_GLASS_PANE);

        industrialGlassTagBuilder.add(HumanIndustrialGlassBlockItems.INDUSTRIAL_GLASS_DOOR.get());
        industrialGlassTagBuilder.add(HumanIndustrialGlassBlockItems.INDUSTRIAL_GLASS_SLAB.get());
        industrialGlassTagBuilder.add(HumanIndustrialGlassBlockItems.INDUSTRIAL_GLASS_STAIRS.get());
        industrialGlassTagBuilder.add(HumanIndustrialGlassBlockItems.INDUSTRIAL_GLASS_TRAP_DOOR.get());
        industrialGlassTagBuilder.add(HumanIndustrialGlassBlockItems.INDUSTRIAL_GLASS_WALL.get());
        Stream.of(
            HumanIndustrialGlassBlockItems.DYE_COLOR_TO_INDUSTRIAL_GLASS_STAIRS,
            HumanIndustrialGlassBlockItems.DYE_COLOR_TO_INDUSTRIAL_GLASS_SLAB,
            HumanIndustrialGlassBlockItems.DYE_COLOR_TO_INDUSTRIAL_GLASS_DOOR,
            HumanIndustrialGlassBlockItems.DYE_COLOR_TO_INDUSTRIAL_GLASS_TRAP_DOOR,
            HumanIndustrialGlassBlockItems.DYE_COLOR_TO_INDUSTRIAL_GLASS_WALL
        )
            .flatMap(map -> map.values().stream())
            .map(Supplier::get)
            .forEach(industrialGlassTagBuilder::add);

        getOrCreateTagBuilder(HumanItemTags.LITHIUM)
            .add(
                CoreBlocks.LITHIUM_BLOCK.get().asItem(),
                CoreBlocks.LITHIUM_ORE.get().asItem(),
                HumanItems.LITHIUM_DUST.get()
            );

        getOrCreateTagBuilder(ItemTags.DYEABLE)
            .addTag(HumanItemTags.MK50_ARMOR)
            .addTag(HumanItemTags.PRESSURE_ARMOR)
            .addTag(HumanItemTags.WY_COMMANDO_ARMOR)
            .addTag(HumanItemTags.WY_ELITE_ARMOR);

        getOrCreateTagBuilder(BLibItemTags.RANGED_WEAPONS)
            .addTag(HumanItemTags.GUNS);
    }

    private void addArmorRepairIngredientItems() {
        getOrCreateTagBuilder(HumanItemTags.WY_APE_ARMOR_REPAIR_INGREDIENTS)
            .addTag(HumanCommonItemTags.INGOTS_TITANIUM)
            .addTag(HumanItemTags.PADDING_BLOCKS)
            .addTag(HumanItemTags.PLASTIC_BLOCKS);

        getOrCreateTagBuilder(HumanItemTags.MK50_ARMOR_REPAIR_INGREDIENTS)
            .addTag(HumanCommonItemTags.INGOTS_LEAD);

        getOrCreateTagBuilder(HumanItemTags.PRESSURE_ARMOR_REPAIR_INGREDIENTS)
            .addTag(HumanCommonItemTags.INGOTS_ALUMINUM);

        getOrCreateTagBuilder(HumanItemTags.STEEL_ARMOR_REPAIR_INGREDIENTS)
            .addTag(HumanCommonItemTags.INGOTS_STEEL);

        getOrCreateTagBuilder(HumanItemTags.TACTICAL_ARMOR_REPAIR_INGREDIENTS)
            .addTag(HumanCommonItemTags.INGOTS_STEEL);

        getOrCreateTagBuilder(HumanItemTags.TITANIUM_ARMOR_REPAIR_INGREDIENTS)
            .addTag(HumanCommonItemTags.INGOTS_TITANIUM);

        getOrCreateTagBuilder(HumanItemTags.WY_COMMANDO_ARMOR_REPAIR_INGREDIENTS)
            .addTag(HumanCommonItemTags.INGOTS_STEEL)
            .addTag(HumanItemTags.PLASTIC_BLOCKS);

        getOrCreateTagBuilder(HumanItemTags.WY_ELITE_ARMOR_REPAIR_INGREDIENTS)
            .addTag(HumanCommonItemTags.INGOTS_STEEL)
            .addTag(HumanItemTags.PLASTIC_BLOCKS);
    }

    private void addArmors() {
        getOrCreateTagBuilder(HumanItemTags.WY_APE_ARMOR)
            .add(
                HumanArmorItems.WY_APE_BOOTS.get(),
                HumanArmorItems.WY_APE_CHESTPLATE.get(),
                HumanArmorItems.WY_APE_HELMET.get(),
                HumanArmorItems.WY_APE_LEGGINGS.get()
            );

        getOrCreateTagBuilder(HumanItemTags.RADIATION_RESISTANT_ARMORS)
            .add(
                HumanArmorItems.MK50_HELMET.get(),
                HumanArmorItems.MK50_CHESTPLATE.get(),
                HumanArmorItems.MK50_LEGGINGS.get(),
                HumanArmorItems.MK50_BOOTS.get()
            );

        getOrCreateTagBuilder(HumanItemTags.MK50_ARMOR)
            .add(
                HumanArmorItems.MK50_BOOTS.get(),
                HumanArmorItems.MK50_CHESTPLATE.get(),
                HumanArmorItems.MK50_HELMET.get(),
                HumanArmorItems.MK50_LEGGINGS.get()
            );

        getOrCreateTagBuilder(HumanItemTags.PRESSURE_ARMOR)
            .add(
                HumanArmorItems.PRESSURE_BOOTS.get(),
                HumanArmorItems.PRESSURE_CHESTPLATE.get(),
                HumanArmorItems.PRESSURE_HELMET.get(),
                HumanArmorItems.PRESSURE_LEGGINGS.get()
            );

        getOrCreateTagBuilder(HumanItemTags.WY_COMMANDO_ARMOR)
            .add(
                HumanArmorItems.WY_COMMANDO_BOOTS.get(),
                HumanArmorItems.WY_COMMANDO_CHESTPLATE.get(),
                HumanArmorItems.WY_COMMANDO_HELMET.get(),
                HumanArmorItems.WY_COMMANDO_LEGGINGS.get()
            );

        getOrCreateTagBuilder(HumanItemTags.WY_ELITE_ARMOR)
            .add(
                HumanArmorItems.WY_ELITE_BOOTS.get(),
                HumanArmorItems.WY_ELITE_CHESTPLATE.get(),
                HumanArmorItems.WY_ELITE_HELMET.get(),
                HumanArmorItems.WY_ELITE_LEGGINGS.get()
            );
    }

    private void addRadioactiveItems() {
        getOrCreateTagBuilder(HumanItemTags.RADIATION_CURE_ITEMS)
            .add(
                Items.GOLDEN_APPLE,
                Items.ENCHANTED_GOLDEN_APPLE
            );

        getOrCreateTagBuilder(HumanItemTags.RADIOACTIVE_ITEMS)
            .add(
                HumanItems.AUTUNITE_DUST.get(),
                HumanItems.URANIUM_NUGGET.get(),
                HumanItems.URANIUM_INGOT.get(),
                HumanBlockItems.AUTUNITE_BLOCK.get(),
                HumanBlockItems.AUTUNITE_ORE.get(),
                HumanBlockItems.URANIUM_BLOCK.get(),
                HumanBlockItems.TRINITITE_BLOCK.get()
            )
            .addTag(HumanCommonItemTags.INGOTS_URANIUM);

        // Potency tiers layered on top of the master list above. Dust and nuggets stay at baseline strength; refined
        // and raw material is worse to carry; a solid block of the stuff is worse again.
        getOrCreateTagBuilder(HumanItemTags.HIGHLY_RADIOACTIVE_ITEMS)
            .add(
                HumanItems.URANIUM_INGOT.get(),
                HumanBlockItems.AUTUNITE_ORE.get()
            )
            .addTag(HumanCommonItemTags.INGOTS_URANIUM);

        getOrCreateTagBuilder(HumanItemTags.EXTREMELY_RADIOACTIVE_ITEMS)
            .add(
                HumanBlockItems.AUTUNITE_BLOCK.get(),
                HumanBlockItems.URANIUM_BLOCK.get(),
                HumanBlockItems.TRINITITE_BLOCK.get()
            );

        getOrCreateTagBuilder(HumanItemTags.URANIUM_NUGGET_LIKE)
            .addOptionalTag(HumanCommonItemTags.NUGGETS_URANIUM);
    }

    private void addCommonItems() {
        getOrCreateTagBuilder(HumanCommonItemTags.DUSTS_AUTUNITE)
            .setReplace(false)
            .add(HumanItems.AUTUNITE_DUST.get());

        getOrCreateTagBuilder(CommonItemTags.DUSTS_COAL)
            .setReplace(false)
            .add(HumanItems.CARBON_DUST.get());

        getOrCreateTagBuilder(HumanCommonItemTags.DUSTS_LITHIUM)
            .setReplace(false)
            .add(HumanItems.LITHIUM_DUST.get());

        // [stated] Sep 29: the metal dusts, each under its metal's c: tag so other mods' grinders and furnaces agree.
        getOrCreateTagBuilder(HumanCommonItemTags.DUSTS_IRON)
            .setReplace(false)
            .add(HumanItems.IRON_DUST.get());

        getOrCreateTagBuilder(HumanCommonItemTags.DUSTS_GOLD)
            .setReplace(false)
            .add(HumanItems.GOLD_DUST.get());

        getOrCreateTagBuilder(HumanCommonItemTags.DUSTS_COPPER)
            .setReplace(false)
            .add(HumanItems.COPPER_DUST.get());

        getOrCreateTagBuilder(HumanCommonItemTags.DUSTS_TITANIUM)
            .setReplace(false)
            .add(HumanItems.TITANIUM_DUST.get());

        getOrCreateTagBuilder(HumanCommonItemTags.DUSTS_ALUMINUM)
            .setReplace(false)
            .add(HumanItems.ALUMINUM_DUST.get());

        getOrCreateTagBuilder(HumanCommonItemTags.DUSTS_FERROALUMINUM)
            .setReplace(false)
            .add(HumanItems.FERROALUMINUM_DUST.get());

        getOrCreateTagBuilder(HumanCommonItemTags.DUSTS_LEAD)
            .setReplace(false)
            .add(HumanItems.LEAD_DUST.get());

        getOrCreateTagBuilder(HumanCommonItemTags.DUSTS_ZINC)
            .setReplace(false)
            .add(HumanItems.ZINC_DUST.get());

        getOrCreateTagBuilder(HumanCommonItemTags.DUSTS_BRASS)
            .setReplace(false)
            .add(HumanItems.BRASS_DUST.get());

        getOrCreateTagBuilder(HumanCommonItemTags.DUSTS_STEEL)
            .setReplace(false)
            .add(HumanItems.STEEL_DUST.get());

        getOrCreateTagBuilder(HumanCommonItemTags.DUSTS_NEODYMIUM)
            .setReplace(false)
            .add(HumanItems.MAGNETIC_DUST.get());

        getOrCreateTagBuilder(CommonItemTags.INGOTS)
            .setReplace(false)
            .addTag(HumanCommonItemTags.INGOTS_ALUMINUM)
            .addTag(HumanCommonItemTags.INGOTS_BRASS)
            .addTag(HumanCommonItemTags.INGOTS_FERROALUMINUM)
            .addTag(HumanCommonItemTags.INGOTS_COMPOSITE)
            .addTag(HumanCommonItemTags.INGOTS_LEAD)
            .addTag(HumanCommonItemTags.INGOTS_STEEL)
            .addTag(HumanCommonItemTags.INGOTS_TITANIUM)
            .addTag(HumanCommonItemTags.INGOTS_URANIUM)
            .addTag(HumanCommonItemTags.INGOTS_ZINC);

        getOrCreateTagBuilder(HumanCommonItemTags.INGOTS_ALUMINUM)
            .setReplace(false)
            .add(HumanItems.ALUMINUM_INGOT.get());

        getOrCreateTagBuilder(HumanCommonItemTags.INGOTS_BRASS)
            .setReplace(false)
            .add(HumanItems.BRASS_INGOT.get());

        getOrCreateTagBuilder(HumanCommonItemTags.INGOTS_LEAD)
            .setReplace(false)
            .add(HumanItems.LEAD_INGOT.get());

        getOrCreateTagBuilder(HumanCommonItemTags.INGOTS_FERROALUMINUM)
            .setReplace(false)
            .add(HumanItems.FERROALUMINUM_INGOT.get());

        getOrCreateTagBuilder(HumanCommonItemTags.INGOTS_STEEL)
            .setReplace(false)
            .add(HumanItems.STEEL_INGOT.get());

        getOrCreateTagBuilder(HumanCommonItemTags.INGOTS_COMPOSITE)
            .setReplace(false)
            .add(HumanItems.COMPOSITE_INGOT.get());

        getOrCreateTagBuilder(HumanCommonItemTags.INGOTS_TITANIUM)
            .setReplace(false)
            .add(HumanItems.TITANIUM_INGOT.get());

        getOrCreateTagBuilder(HumanCommonItemTags.INGOTS_URANIUM)
            .setReplace(false)
            .add(HumanItems.URANIUM_INGOT.get());

        getOrCreateTagBuilder(HumanCommonItemTags.INGOTS_ZINC)
            .setReplace(false)
            .add(HumanItems.ZINC_INGOT.get());

        getOrCreateTagBuilder(CommonItemTags.NUGGETS)
            .setReplace(false)
            .addTag(HumanCommonItemTags.NUGGETS_ALUMINUM)
            .addTag(HumanCommonItemTags.NUGGETS_BRASS)
            .addTag(HumanCommonItemTags.NUGGETS_FERROALUMINUM)
            .addTag(HumanCommonItemTags.NUGGETS_LEAD)
            .addTag(HumanCommonItemTags.NUGGETS_STEEL)
            .addTag(HumanCommonItemTags.NUGGETS_TITANIUM)
            .addTag(HumanCommonItemTags.NUGGETS_URANIUM)
            .addTag(HumanCommonItemTags.NUGGETS_ZINC);

        // Oct 21 - the composite nugget, and every metal chain in the shared chain tag (the capture chain recipe reads
        // it).
        var compositeNuggets = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "nuggets/composite")
        );

        getOrCreateTagBuilder(compositeNuggets)
            .setReplace(false)
            .add(com.human.common.registry.init.HumanMetalFixtures.COMPOSITE_NUGGET.get());

        getOrCreateTagBuilder(CommonItemTags.NUGGETS)
            .setReplace(false)
            .addTag(compositeNuggets);

        getOrCreateTagBuilder(
            net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "chains")
            )
        )
            .setReplace(false)
            .add(
                com.human.common.registry.init.HumanMetalFixtures.STEEL_CHAIN_ITEM.get(),
                com.human.common.registry.init.HumanMetalFixtures.TITANIUM_CHAIN_ITEM.get(),
                com.human.common.registry.init.HumanMetalFixtures.FERROALUMINUM_CHAIN_ITEM.get(),
                com.human.common.registry.init.HumanMetalFixtures.ALUMINUM_CHAIN_ITEM.get(),
                com.human.common.registry.init.HumanMetalFixtures.COMPOSITE_CHAIN_ITEM.get()
            );

        // [stated] Oct 23 - the metal signs in vanilla's sign item tags.
        var signItems = getOrCreateTagBuilder(net.minecraft.tags.ItemTags.SIGNS);
        var hangingSignItems = getOrCreateTagBuilder(net.minecraft.tags.ItemTags.HANGING_SIGNS);

        for (var set : com.human.common.registry.init.HumanSigns.all()) {
            signItems.add(set.signItem().get());
            hangingSignItems.add(set.hangingSignItem().get());
        }

        getOrCreateTagBuilder(HumanCommonItemTags.NUGGETS_ALUMINUM)
            .setReplace(false)
            .add(HumanItems.ALUMINUM_NUGGET.get());

        getOrCreateTagBuilder(HumanCommonItemTags.NUGGETS_BRASS)
            .setReplace(false)
            .add(HumanItems.BRASS_NUGGET.get());

        getOrCreateTagBuilder(HumanCommonItemTags.NUGGETS_FERROALUMINUM)
            .setReplace(false)
            .add(HumanItems.FERROALUMINUM_NUGGET.get());

        getOrCreateTagBuilder(HumanCommonItemTags.NUGGETS_LEAD)
            .setReplace(false)
            .add(HumanItems.LEAD_NUGGET.get());

        getOrCreateTagBuilder(HumanCommonItemTags.NUGGETS_STEEL)
            .setReplace(false)
            .add(HumanItems.STEEL_NUGGET.get());

        getOrCreateTagBuilder(HumanCommonItemTags.NUGGETS_TITANIUM)
            .setReplace(false)
            .add(HumanItems.TITANIUM_NUGGET.get());

        getOrCreateTagBuilder(HumanCommonItemTags.NUGGETS_URANIUM)
            .setReplace(false)
            .add(HumanItems.URANIUM_NUGGET.get());

        getOrCreateTagBuilder(HumanCommonItemTags.NUGGETS_ZINC)
            .setReplace(false)
            .add(HumanItems.ZINC_NUGGET.get());

        getOrCreateTagBuilder(HumanCommonItemTags.NUGGETS_COPPER)
            .setReplace(false)
            .add(HumanItems.COPPER_NUGGET.get());

        getOrCreateTagBuilder(CommonItemTags.ORES)
            .setReplace(false)
            .addTag(HumanCommonItemTags.ORES_ALUMINUM)
            .addTag(HumanCommonItemTags.ORES_AUTUNITE)
            .addTag(HumanCommonItemTags.ORES_LEAD)
            .addTag(HumanCommonItemTags.ORES_LITHIUM)
            .addTag(HumanCommonItemTags.ORES_MONAZITE)
            .addTag(HumanCommonItemTags.ORES_TITANIUM)
            .addTag(HumanCommonItemTags.ORES_ZINC);

        getOrCreateTagBuilder(HumanCommonItemTags.ORES_ALUMINUM)
            .setReplace(false)
            .addTag(HumanCommonItemTags.ORES_BAUXITE);

        getOrCreateTagBuilder(HumanCommonItemTags.ORES_AUTUNITE)
            .setReplace(false)
            .add(HumanBlockItems.AUTUNITE_ORE.get());

        getOrCreateTagBuilder(HumanCommonItemTags.ORES_BAUXITE)
            .setReplace(false)
            .add(HumanBlockItems.BAUXITE_ORE.get());

        getOrCreateTagBuilder(HumanCommonItemTags.ORES_GALENA)
            .setReplace(false)
            .add(HumanBlockItems.GALENA_ORE.get());

        getOrCreateTagBuilder(HumanCommonItemTags.ORES_LEAD)
            .setReplace(false)
            .addTag(HumanCommonItemTags.ORES_GALENA);

        getOrCreateTagBuilder(HumanCommonItemTags.ORES_LITHIUM)
            .setReplace(false)
            .add(HumanBlockItems.LITHIUM_ORE.get());

        getOrCreateTagBuilder(HumanCommonItemTags.ORES_MONAZITE)
            .setReplace(false)
            .add(HumanBlockItems.MONAZITE_ORE.get());

        getOrCreateTagBuilder(HumanCommonItemTags.ORES_TITANIUM)
            .setReplace(false)
            .add(HumanBlockItems.DEEPSLATE_TITANIUM_ORE.get());

        getOrCreateTagBuilder(HumanCommonItemTags.ORES_ZINC)
            .setReplace(false)
            .add(HumanBlockItems.ZINC_ORE.get())
            .add(HumanBlockItems.DEEPSLATE_ZINC_ORE.get());

        getOrCreateTagBuilder(CommonItemTags.RAW_MATERIALS)
            .setReplace(false)
            .addTag(HumanCommonItemTags.RAW_MATERIALS_ALUMINUM)
            .addTag(HumanCommonItemTags.RAW_MATERIALS_LEAD)
            .addTag(HumanCommonItemTags.RAW_MATERIALS_STEEL)
            .addTag(HumanCommonItemTags.RAW_MATERIALS_TITANIUM)
            .addTag(HumanCommonItemTags.RAW_MATERIALS_ZINC);

        getOrCreateTagBuilder(HumanCommonItemTags.RAW_MATERIALS_ALUMINUM)
            .setReplace(false)
            .add(HumanItems.RAW_BAUXITE.get());

        getOrCreateTagBuilder(HumanCommonItemTags.RAW_MATERIALS_LEAD)
            .setReplace(false)
            .add(HumanItems.RAW_GALENA.get());

        getOrCreateTagBuilder(HumanCommonItemTags.RAW_MATERIALS_MONAZITE)
            .setReplace(false)
            .add(HumanItems.RAW_MONAZITE.get());

        getOrCreateTagBuilder(HumanCommonItemTags.RAW_MATERIALS_STEEL)
            .setReplace(false)
            .add(HumanItems.RAW_CRUDE_IRON.get());

        getOrCreateTagBuilder(HumanCommonItemTags.RAW_MATERIALS_TITANIUM)
            .setReplace(false)
            .add(HumanItems.RAW_TITANIUM.get());

        getOrCreateTagBuilder(HumanCommonItemTags.RAW_MATERIALS_ZINC)
            .setReplace(false)
            .add(HumanItems.RAW_ZINC.get());

        getOrCreateTagBuilder(HumanCommonItemTags.SILICON)
            .setReplace(false)
            .add(HumanItems.SILICON.get());

        getOrCreateTagBuilder(CommonItemTags.STORAGE_BLOCKS)
            .setReplace(false)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_ALUMINUM)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_BRASS)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_FERROALUMINUM)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_LEAD)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_RAW_ALUMINUM)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_RAW_LEAD)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_RAW_TITANIUM)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_RAW_ZINC)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_STEEL)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_TITANIUM)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_URANIUM)
            .addTag(HumanCommonItemTags.STORAGE_BLOCKS_ZINC);

        getOrCreateTagBuilder(HumanCommonItemTags.STORAGE_BLOCKS_ALUMINUM)
            .setReplace(false)
            .add(HumanBlockItems.ALUMINUM_BLOCK.get());

        getOrCreateTagBuilder(HumanCommonItemTags.STORAGE_BLOCKS_BRASS)
            .setReplace(false)
            .add(HumanBlockItems.BRASS_BLOCK.get());

        getOrCreateTagBuilder(HumanCommonItemTags.STORAGE_BLOCKS_FERROALUMINUM)
            .setReplace(false)
            .add(HumanFerroaluminumBlockItems.FERROALUMINUM_BLOCK.get());

        getOrCreateTagBuilder(HumanCommonItemTags.STORAGE_BLOCKS_LEAD)
            .setReplace(false)
            .add(HumanBlockItems.LEAD_BLOCK.get());

        getOrCreateTagBuilder(HumanCommonItemTags.STORAGE_BLOCKS_RAW_ALUMINUM)
            .setReplace(false)
            .add(HumanBlockItems.RAW_BAUXITE_BLOCK.get());

        getOrCreateTagBuilder(HumanCommonItemTags.STORAGE_BLOCKS_RAW_LEAD)
            .setReplace(false)
            .add(HumanBlockItems.RAW_GALENA_BLOCK.get());

        getOrCreateTagBuilder(HumanCommonItemTags.STORAGE_BLOCKS_RAW_TITANIUM)
            .setReplace(false)
            .add(HumanBlockItems.RAW_TITANIUM_BLOCK.get());

        getOrCreateTagBuilder(HumanCommonItemTags.STORAGE_BLOCKS_RAW_ZINC)
            .setReplace(false)
            .add(HumanBlockItems.RAW_ZINC_BLOCK.get());

        getOrCreateTagBuilder(HumanCommonItemTags.STORAGE_BLOCKS_STEEL)
            .setReplace(false)
            .add(HumanSteelBlockItems.STEEL_BLOCK.get());

        getOrCreateTagBuilder(HumanCommonItemTags.STORAGE_BLOCKS_TITANIUM)
            .setReplace(false)
            .add(HumanTitaniumBlockItems.TITANIUM_BLOCK.get());

        getOrCreateTagBuilder(HumanCommonItemTags.STORAGE_BLOCKS_URANIUM)
            .setReplace(false)
            .add(HumanBlockItems.URANIUM_BLOCK.get());

        getOrCreateTagBuilder(HumanCommonItemTags.STORAGE_BLOCKS_ZINC)
            .setReplace(false)
            .add(HumanBlockItems.ZINC_BLOCK.get());
    }

    private void addCompatibilityItems() {
        getOrCreateTagBuilder(foreignItemTag("avp_alien", "facehugger_resistant_helmets"))
            .add(HumanArmorItems.WY_APE_HELMET.get());

        // avp_alien has a resin_blocks BLOCK tag holding exactly the four base strain blocks, but no ITEM equivalent,
        // and a smelting ingredient needs an item tag. The ids are added by raw ResourceLocation because avp_alien is
        // compile-only here -- see foreignItemTag below -- and as optional entries so the tag is simply empty, and the
        // recipe simply never matches, when avp_alien is absent. The optional tag reference means that if avp_alien
        // ever does ship an item tag of its own, its contents merge in without a change here.
        // The two sibling mods' music-disc fragments, each in its own tag so marine_chest_personal can offer them
        // without avp_human requiring either mod. Added as OPTIONAL raw ResourceLocations for the same two reasons as
        // the resin blocks below: avp_alien/avp_predator are modCompileOnly, so touching their item classes here would
        // throw NoClassDefFoundError inside datagen and take the entire provider down with it; and an optional entry
        // leaves the tag simply EMPTY when the mod is absent, which is exactly the wanted behaviour - the loot entry
        // rolls to nothing instead of breaking the table.
        getOrCreateTagBuilder(HumanItemTags.ALIEN_MUSIC_DISC_FRAGMENTS)
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "alien_music_disc_1_fragment"));

        getOrCreateTagBuilder(HumanItemTags.PREDATOR_MUSIC_DISC_FRAGMENTS)
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_predator", "predator_music_disc_1_fragment"));

        getOrCreateTagBuilder(HumanItemTags.RESIN_BLOCKS)
            .addOptionalTag(ResourceLocation.fromNamespaceAndPath("avp_alien", "resin_blocks"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "resin"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "aberrant_resin"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "irradiated_resin"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("avp_alien", "nether_resin"));

        // Stellaris judges whether an entity can breathe in a vacuum by checking that all four equipment slots hold
        // items in its own stellaris:oxygenated_armor tag, which it declares but deliberately ships empty for other
        // mods to fill. Topping up vanilla air supply, which is what these suits do, has no bearing on that check --
        // Stellaris deals its own damage type directly and never reads the air supply.
        getOrCreateTagBuilder(foreignItemTag("stellaris", "oxygenated_armor"))
            .addTag(HumanItemTags.MK50_ARMOR)
            .addTag(HumanItemTags.PRESSURE_ARMOR);

        // Ad Astra checks a FULL SET against each of these (SpaceSuitItem.hasFullSet walks all four slots; read from
        // the 1.16.26 jar). Sep 21 rulings: "pressure suit is for oxygen, mk50 is all four, no tank needed its a built
        // in recycling system." Acid rain has no armor tag in Ad Astra - that one is handled at the damage, see
        // MixinLivingEntity_SuitHazardImmunity.
        getOrCreateTagBuilder(foreignItemTag("ad_astra", "space_resistant_armor"))
            .addTag(HumanItemTags.MK50_ARMOR)
            .addTag(HumanItemTags.PRESSURE_ARMOR);

        getOrCreateTagBuilder(foreignItemTag("ad_astra", "oxygen_supplying_armor"))
            .addTag(HumanItemTags.MK50_ARMOR)
            .addTag(HumanItemTags.PRESSURE_ARMOR);

        getOrCreateTagBuilder(foreignItemTag("ad_astra", "heat_resistant_armor"))
            .addTag(HumanItemTags.MK50_ARMOR);

        getOrCreateTagBuilder(foreignItemTag("ad_astra", "freeze_resistant_armor"))
            .addTag(HumanItemTags.MK50_ARMOR);

        // [stated] Oct 1: only proper suits keep cryofluid out - the MK50 or any other environmental hazard suit, as a
        // full set. Ad Astra's freeze tag is OPTIONAL so this still loads without Ad Astra installed.
        getOrCreateTagBuilder(HumanItemTags.CRYO_PROTECTIVE_ARMOR)
            .addTag(HumanItemTags.MK50_ARMOR)
            .addOptionalTag(foreignItemTag("ad_astra", "freeze_resistant_armor"));
    }

    private void addAutomatedTagItems() {
        // Armor
        var headArmorTagProvider = getOrCreateTagBuilder(ItemTags.HEAD_ARMOR);
        var chestArmorTagProvider = getOrCreateTagBuilder(ItemTags.CHEST_ARMOR);
        var legArmorTagProvider = getOrCreateTagBuilder(ItemTags.LEG_ARMOR);
        var footArmorTagProvider = getOrCreateTagBuilder(ItemTags.FOOT_ARMOR);

        // Blocks
        var buttonTagProvider = getOrCreateTagBuilder(ItemTags.BUTTONS);
        var doorTagProvider = getOrCreateTagBuilder(ItemTags.DOORS);
        var fenceTagProvider = getOrCreateTagBuilder(ItemTags.FENCES);
        var slabTagProvider = getOrCreateTagBuilder(ItemTags.SLABS);
        var stairsTagProvider = getOrCreateTagBuilder(ItemTags.STAIRS);
        var trapdoorTagProvider = getOrCreateTagBuilder(ItemTags.TRAPDOORS);
        var wallTagBuilder = getOrCreateTagBuilder(ItemTags.WALLS);

        // Tools
        var axeTagProvider = getOrCreateTagBuilder(ItemTags.AXES);
        var hoeTagProvider = getOrCreateTagBuilder(ItemTags.HOES);
        var pickaxeTagProvider = getOrCreateTagBuilder(ItemTags.PICKAXES);
        var shovelTagProvider = getOrCreateTagBuilder(ItemTags.SHOVELS);

        // Weapons
        // Daggers. The behaviour keys off this tag rather than the item class, so a dagger from another mod joins in
        // by being tagged and one added here never needs code changes.
        getOrCreateTagBuilder(HumanItemTags.DAGGERS).add(HumanItems.TACTICAL_KNIFE.get());

        var gunTagProvider = getOrCreateTagBuilder(HumanItemTags.GUNS);
        var swordTagProvider = getOrCreateTagBuilder(ItemTags.SWORDS);

        Human.MOD.registries()
            .getAllHolders(BuiltInRegistries.ITEM)
            .forEach(holder -> {
                var item = holder.get();

                if (item instanceof ArmorItem armorItem) {
                    switch (armorItem.getType()) {
                        case HELMET -> headArmorTagProvider.add(item);
                        case CHESTPLATE -> chestArmorTagProvider.add(item);
                        case LEGGINGS -> legArmorTagProvider.add(item);
                        case BOOTS -> footArmorTagProvider.add(item);
                        case BODY -> { /* NO-OP */ }
                    }
                }

                if (item instanceof BlockItem blockItem) {
                    var block = blockItem.getBlock();

                    if (block instanceof ButtonBlock) {
                        buttonTagProvider.add(item);
                    }

                    if (block instanceof DoorBlock) {
                        doorTagProvider.add(item);
                    }

                    if (block instanceof FenceBlock) {
                        fenceTagProvider.add(item);
                    }

                    if (block instanceof SlabBlock) {
                        slabTagProvider.add(item);
                    }

                    if (block instanceof StairBlock) {
                        stairsTagProvider.add(item);
                    }

                    if (block instanceof TrapDoorBlock) {
                        trapdoorTagProvider.add(item);
                    }

                    if (block instanceof WallBlock) {
                        wallTagBuilder.add(item);
                    }
                }

                if (item instanceof AxeItem) {
                    axeTagProvider.add(item);
                }

                if (item instanceof GunItem) {
                    gunTagProvider.add(item);
                }

                if (item instanceof HoeItem) {
                    hoeTagProvider.add(item);
                }

                if (item instanceof PickaxeItem) {
                    pickaxeTagProvider.add(item);
                }

                if (item instanceof ShovelItem) {
                    shovelTagProvider.add(item);
                }

                if (item instanceof SwordItem) {
                    swordTagProvider.add(item);
                }
            });
    }

    /**
     * Builds a TagKey owned by ANOTHER mod from its raw id, instead of importing that mod's tag class.
     * <p>
     * Datagen only ever needs the tag's IDENTITY to write a JSON file, never the foreign class - and importing it made
     * this provider fail with NoClassDefFoundError whenever the sibling mod was absent from the DATAGEN runtime
     * classpath (it is compile-only here). Raw ids keep these compatibility tags working no matter which siblings are
     * present, and an unused tag file for an absent mod is simply inert data.
     * </p>
     */
    private static TagKey<Item> foreignItemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
