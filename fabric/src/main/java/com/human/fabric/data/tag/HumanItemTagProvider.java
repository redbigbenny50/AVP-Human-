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

public class HumanItemTagProvider extends FabricTagProvider.ItemTagProvider {

    public HumanItemTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
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

        getOrCreateTagBuilder(HumanItemTags.INDUSTRIAL_GLASS)
            .addTag(HumanItemTags.INDUSTRIAL_GLASS_BLOCK)
            .addTag(HumanItemTags.INDUSTRIAL_GLASS_PANE);

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

        getOrCreateTagBuilder(CommonItemTags.INGOTS)
            .setReplace(false)
            .addTag(HumanCommonItemTags.INGOTS_ALUMINUM)
            .addTag(HumanCommonItemTags.INGOTS_BRASS)
            .addTag(HumanCommonItemTags.INGOTS_FERROALUMINUM)
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
