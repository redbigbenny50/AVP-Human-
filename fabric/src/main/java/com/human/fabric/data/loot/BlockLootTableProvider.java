package com.human.fabric.data.loot;

import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.block.CoreBlocks;
import com.human.common.registry.init.block.HumanCompositeBlocks;
import com.human.common.registry.init.block.HumanFerroaluminumBlocks;
import com.human.common.registry.init.block.HumanIndustrialConcreteBlocks;
import com.human.common.registry.init.block.HumanIndustrialGlassBlocks;
import com.human.common.registry.init.block.HumanPaddingBlocks;
import com.human.common.registry.init.block.HumanPlasticBlocks;
import com.human.common.registry.init.block.HumanSteelBlocks;
import com.human.common.registry.init.block.HumanTitaniumBlocks;
import com.human.common.registry.init.item.HumanItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class BlockLootTableProvider extends FabricBlockLootTableProvider {

    private static final Set<Block> TOUCHED_ENTRIES = new HashSet<>();

    public BlockLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    /**
     * The battery's table: the storage table plus its UPGRADE SLOTS ({@code minecraft:container}), so a broken battery
     * keeps its upgrades the way it keeps its charge. Kept separate from {@link #createPowerStorageTable} so the
     * generators sharing that one are untouched.
     */
    private net.minecraft.world.level.storage.loot.LootTable.Builder createBatteryTable(
        net.minecraft.world.level.block.Block block
    ) {
        return net.minecraft.world.level.storage.loot.LootTable.lootTable()
            .withPool(
                applyExplosionCondition(
                    block,
                    net.minecraft.world.level.storage.loot.LootPool.lootPool()
                        .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1.0F))
                        .add(
                            net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(block)
                                .apply(
                                    net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction
                                        .copyComponents(
                                            net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source.BLOCK_ENTITY
                                        )
                                        .include(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
                                        .include(com.human.common.registry.init.HumanDataComponents.STORED_POWER.get())
                                        .include(com.human.common.registry.init.HumanDataComponents.POWER_PORTS.get())
                                        .include(net.minecraft.core.component.DataComponents.CONTAINER)
                                )
                        )
                )
            );
    }

    /** Oct 2: the cryotube controller - its name, breakers, cryofluid buffer and hub chip all ride on the item. */
    /**
     * Blocks that hold items: everything the block entity carries goes onto the ONE dropped item - name, breaker ports,
     * fluid and container contents.
     * <p>
     * ⚠⚠ NO applyExplosionCondition. That condition lets an explosion drop NOTHING, and for a block holding items it
     * would destroy them: the cryotube controller's backup batteries, a drive bay's SSDs and blueprints. Oct 13 quality
     * check - the controller used it until then.
     * </p>
     */
    /**
     * The fabricator: four blocks, one block id. ONLY the core drops, and it carries everything - name, breaker ports
     * and all 35 slots. Without the part check, one fabricator would drop four.
     */
    /** Only the CORE drops the terminal: the other five blocks are part of the same machine and drop nothing. */
    private net.minecraft.world.level.storage.loot.LootTable.Builder createQuarryTerminalTable(
        net.minecraft.world.level.block.Block block
    ) {
        return net.minecraft.world.level.storage.loot.LootTable.lootTable()
            .withPool(
                net.minecraft.world.level.storage.loot.LootPool.lootPool()
                    .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1.0F))
                    .when(
                        net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                            .setProperties(
                                net.minecraft.advancements.critereon.StatePropertiesPredicate.Builder.properties()
                                    .hasProperty(
                                        com.human.common.gameplay.quarry.QuarryTerminalBlock.PART,
                                        com.human.common.gameplay.quarry.QuarryTerminalPart.CORE
                                    )
                            )
                    )
                    .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(block))
                    .when(net.minecraft.world.level.storage.loot.predicates.ExplosionCondition.survivesExplosion())
            );
    }

    private net.minecraft.world.level.storage.loot.LootTable.Builder createFabricatorTable(
        net.minecraft.world.level.block.Block block
    ) {
        return net.minecraft.world.level.storage.loot.LootTable.lootTable()
            .withPool(
                net.minecraft.world.level.storage.loot.LootPool.lootPool()
                    .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1.0F))
                    .when(
                        net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                            .setProperties(
                                net.minecraft.advancements.critereon.StatePropertiesPredicate.Builder.properties()
                                    .hasProperty(
                                        com.human.common.gameplay.fabrication.FabricatorBlock.PART,
                                        com.human.common.gameplay.fabrication.FabricatorPart.CORE
                                    )
                            )
                    )
                    .add(
                        net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(block)
                            .apply(
                                net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction
                                    .copyComponents(
                                        net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source.BLOCK_ENTITY
                                    )
                                    .include(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
                                    .include(com.human.common.registry.init.HumanDataComponents.POWER_PORTS.get())
                                    .include(net.minecraft.core.component.DataComponents.CONTAINER)
                            )
                    )
            );
    }

    private net.minecraft.world.level.storage.loot.LootTable.Builder createCryotubeControllerTable(
        net.minecraft.world.level.block.Block block
    ) {
        return net.minecraft.world.level.storage.loot.LootTable.lootTable()
            .withPool(
                net.minecraft.world.level.storage.loot.LootPool.lootPool()
                    .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1.0F))
                    .add(
                        net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(block)
                            .apply(
                                net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction
                                    .copyComponents(
                                        net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source.BLOCK_ENTITY
                                    )
                                    .include(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
                                    .include(com.human.common.registry.init.HumanDataComponents.POWER_PORTS.get())
                                    .include(com.human.common.registry.init.HumanDataComponents.FLUID_CONTENT.get())
                                    .include(net.minecraft.core.component.DataComponents.CONTAINER)
                            )
                    )
            );
    }

    /** Drops the block carrying both its name and its stored charge. */
    private net.minecraft.world.level.storage.loot.LootTable.Builder createPowerStorageTable(
        net.minecraft.world.level.block.Block block
    ) {
        return net.minecraft.world.level.storage.loot.LootTable.lootTable()
            .withPool(
                applyExplosionCondition(
                    block,
                    net.minecraft.world.level.storage.loot.LootPool.lootPool()
                        .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1.0F))
                        .add(
                            net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(block)
                                .apply(
                                    net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction
                                        .copyComponents(
                                            net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source.BLOCK_ENTITY
                                        )
                                        .include(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
                                        .include(com.human.common.registry.init.HumanDataComponents.STORED_POWER.get())
                                        .include(com.human.common.registry.init.HumanDataComponents.POWER_PORTS.get())
                                )
                        )
                )
            );
    }

    @Override
    public void generate() {
        generateSelfDrops();
        generateAshDrops();
        generateSlabDrops();
        generateCustomDrops();
        generateOtherDrops();
    }

    /**
     * Ash layers drop ASH BALLS, one per layer - the snow model exactly.
     * <p>
     * Previously this was {@code dropSelf}, which handed out a whole {@code ash_block} item for a SINGLE layer. Placing
     * that back gives eight layers, so a shovel was an ash duplicator. Scaling the drop to the layer count closes that
     * and gives ash a real unit.
     * </p>
     * <p>
     * Silk touch still returns the layers themselves, again matching snow, so a careful player can relocate a drift
     * intact instead of reconstituting it.
     * </p>
     */
    private void generateAshDrops() {
        add(
            CoreBlocks.ASH_BLOCK.get(),
            block -> LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .add(
                            AlternativesEntry.alternatives(
                                AlternativesEntry.alternatives(
                                    SnowLayerBlock.LAYERS.getPossibleValues(),
                                    layers -> LootItem.lootTableItem(HumanItems.ASH_BALL.get())
                                        .when(layersAre(block, layers))
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(layers.floatValue())))
                                )
                                    .when(doesNotHaveSilkTouch()),
                                AlternativesEntry.alternatives(
                                    SnowLayerBlock.LAYERS.getPossibleValues(),
                                    layers -> LootItem.lootTableItem(CoreBlocks.ASH_BLOCK.get())
                                        .when(layersAre(block, layers))
                                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(layers.floatValue())))
                                )
                            )
                        )
                )
        );
    }

    private static LootItemBlockStatePropertyCondition.Builder layersAre(Block block, Integer layers) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SnowLayerBlock.LAYERS, layers.intValue()));
    }

    private void generateSelfDrops() {
        dropSelf(CoreBlocks.ALUMINUM_BLOCK);
        dropSelf(CoreBlocks.AUTUNITE_BLOCK);
        dropSelf(HumanBlocks.CABLE);
        dropSelf(HumanBlocks.CRYOCONDUIT);
        dropSelf(HumanBlocks.CRYOTANK);
        // The controller keeps its cryofluid, hub chip and breaker on a break, like every power machine.
        add(HumanBlocks.CRYOTUBE_CONTROLLER.get(), this::createCryotubeControllerTable);
        add(HumanBlocks.STEEL_CRYOTUBE_CONTROLLER.get(), this::createCryotubeControllerTable);
        add(HumanBlocks.WY_CRYOTUBE_CONTROLLER.get(), this::createCryotubeControllerTable);
        add(HumanBlocks.USCM_CRYOTUBE_CONTROLLER.get(), this::createCryotubeControllerTable);
        // [stated] Oct 13: a broken drive bay keeps its SSDs and blueprints - same keep-everything table.
        add(com.human.common.registry.init.HumanFabrication.DRIVE_BAY.get(), this::createCryotubeControllerTable);
        add(com.human.common.registry.init.HumanFabrication.WY_DRIVE_BAY.get(), this::createCryotubeControllerTable);
        add(com.human.common.registry.init.HumanFabrication.USCM_DRIVE_BAY.get(), this::createCryotubeControllerTable);
        add(com.human.common.registry.init.HumanFabrication.FABRICATOR.get(), this::createFabricatorTable);
        add(com.human.common.registry.init.HumanFabrication.WY_FABRICATOR.get(), this::createFabricatorTable);
        add(com.human.common.registry.init.HumanFabrication.USCM_FABRICATOR.get(), this::createFabricatorTable);
        add(com.human.common.registry.init.HumanQuarry.QUARRY_TERMINAL.get(), this::createQuarryTerminalTable);
        add(com.human.common.registry.init.HumanQuarry.WY_QUARRY_TERMINAL.get(), this::createQuarryTerminalTable);
        add(com.human.common.registry.init.HumanQuarry.USCM_QUARRY_TERMINAL.get(), this::createQuarryTerminalTable);
        dropSelf(com.human.common.registry.init.HumanQuarry.QUARRY_FRAME);
        dropSelf(HumanBlocks.BLUEPRINT_BLOCK);
        dropSelf(CoreBlocks.BRASS_BLOCK);
        dropSelf(HumanFerroaluminumBlocks.CHISELED_FERROALUMINUM);
        dropSelf(HumanSteelBlocks.CHISELED_STEEL);
        dropSelf(HumanTitaniumBlocks.CHISELED_TITANIUM);
        dropSelf(HumanFerroaluminumBlocks.CUT_FERROALUMINUM);
        dropSelf(HumanFerroaluminumBlocks.CUT_FERROALUMINUM_STAIRS);
        dropSelf(HumanSteelBlocks.CUT_STEEL);
        dropSelf(HumanSteelBlocks.CUT_STEEL_STAIRS);
        dropSelf(HumanTitaniumBlocks.CUT_TITANIUM);
        dropSelf(HumanTitaniumBlocks.CUT_TITANIUM_STAIRS);
        dropSelf(HumanBlocks.DESK_TERMINAL_BLOCK);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_BLOCK);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_BUTTON);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_CHAIN_FENCE);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_COLUMN);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_SIDING);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_SIDING_STAIRS);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_STANDING);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_STANDING_STAIRS);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_GRATE);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_GRATE_STAIRS);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_PLATING);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_PLATING_STAIRS);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_PRESSURE_PLATE);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_SIDING);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_SIDING_STAIRS);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_STAIRS);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_STANDING);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_STANDING_STAIRS);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_TRAP_DOOR);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_TREAD);
        dropSelf(HumanFerroaluminumBlocks.FERROALUMINUM_TREAD_STAIRS);
        dropSelf(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS);
        dropSelf(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_PANE);
        dropSelf(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_STAIRS);
        dropSelf(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_WALL);
        dropSelf(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_TRAP_DOOR);
        dropSelf(CoreBlocks.LEAD_BLOCK);
        dropSelf(CoreBlocks.LITHIUM_BLOCK);
        dropSelf(HumanBlocks.NUKE_BLOCK);
        dropSelf(CoreBlocks.RAW_BAUXITE_BLOCK);
        dropSelf(CoreBlocks.RAW_GALENA_BLOCK);
        dropSelf(CoreBlocks.RAW_MONAZITE_BLOCK);
        dropSelf(CoreBlocks.RAW_TITANIUM_BLOCK);
        dropSelf(CoreBlocks.RAW_ZINC_BLOCK);
        dropSelf(HumanBlocks.RAZOR_WIRE);
        dropSelf(HumanBlocks.REDSTONE_GENERATOR);
        dropSelf(HumanBlocks.RESONATOR_BLOCK);
        dropSelf(HumanBlocks.SENTRY_TURRET);
        dropSelf(CoreBlocks.SILICA_GRAVEL);
        dropSelf(CoreBlocks.SILICON_BLOCK);
        dropSelf(HumanSteelBlocks.STEEL_BARS);

        // Oct 21 - metal bars, chains and ladders.
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.STEEL_CHAIN);
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.TITANIUM_CHAIN);
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.FERROALUMINUM_CHAIN);
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.ALUMINUM_CHAIN);
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.COMPOSITE_CHAIN);
        // Oct 23 - metal signs. The wall and wall-hanging blocks drop like their standing/ceiling block (dropsLike),
        // so only those two have tables, as with vanilla's signs.
        for (var set : com.human.common.registry.init.HumanSigns.all()) {
            dropSelf(set.sign());
            dropSelf(set.hangingSign());
        }
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.STEEL_LADDER);
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.TITANIUM_LADDER);
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.FERROALUMINUM_LADDER);
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.ALUMINUM_LADDER);
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.COMPOSITE_LADDER);
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.TITANIUM_BARS);
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.FERROALUMINUM_BARS);
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.ALUMINUM_BARS);
        dropSelf(com.human.common.registry.init.HumanMetalFixtures.COMPOSITE_BARS);
        dropSelf(HumanSteelBlocks.STEEL_BLOCK);
        dropSelf(HumanSteelBlocks.STEEL_BUTTON);
        dropSelf(HumanSteelBlocks.STEEL_CHAIN_FENCE);
        dropSelf(HumanSteelBlocks.STEEL_COLUMN);
        dropSelf(HumanSteelBlocks.STEEL_FASTENED_SIDING);
        dropSelf(HumanSteelBlocks.STEEL_FASTENED_SIDING_STAIRS);
        dropSelf(HumanSteelBlocks.STEEL_FASTENED_STANDING);
        dropSelf(HumanSteelBlocks.STEEL_FASTENED_STANDING_STAIRS);
        dropSelf(HumanSteelBlocks.STEEL_GRATE);
        dropSelf(HumanSteelBlocks.STEEL_GRATE_STAIRS);
        dropSelf(HumanSteelBlocks.STEEL_PLATING);
        dropSelf(HumanSteelBlocks.STEEL_PLATING_STAIRS);
        dropSelf(HumanSteelBlocks.STEEL_PRESSURE_PLATE);
        dropSelf(HumanSteelBlocks.STEEL_SIDING);
        dropSelf(HumanSteelBlocks.STEEL_SIDING_STAIRS);
        dropSelf(HumanSteelBlocks.STEEL_STAIRS);
        dropSelf(HumanSteelBlocks.STEEL_STANDING);
        dropSelf(HumanSteelBlocks.STEEL_STANDING_STAIRS);
        dropSelf(HumanSteelBlocks.STEEL_TRAP_DOOR);
        dropSelf(HumanSteelBlocks.STEEL_TREAD);
        dropSelf(HumanSteelBlocks.STEEL_TREAD_STAIRS);
        dropSelf(HumanTitaniumBlocks.TITANIUM_BLOCK);
        dropSelf(HumanTitaniumBlocks.TITANIUM_BUTTON);
        dropSelf(HumanTitaniumBlocks.TITANIUM_CHAIN_FENCE);
        dropSelf(HumanTitaniumBlocks.TITANIUM_COLUMN);
        dropSelf(HumanTitaniumBlocks.TITANIUM_FASTENED_SIDING);
        dropSelf(HumanTitaniumBlocks.TITANIUM_FASTENED_SIDING_STAIRS);
        dropSelf(HumanTitaniumBlocks.TITANIUM_FASTENED_STANDING);
        dropSelf(HumanTitaniumBlocks.TITANIUM_FASTENED_STANDING_STAIRS);
        dropSelf(HumanTitaniumBlocks.TITANIUM_GRATE);
        dropSelf(HumanTitaniumBlocks.TITANIUM_GRATE_STAIRS);
        dropSelf(HumanTitaniumBlocks.TITANIUM_PLATING);
        dropSelf(HumanTitaniumBlocks.TITANIUM_PLATING_STAIRS);
        dropSelf(HumanTitaniumBlocks.TITANIUM_PRESSURE_PLATE);
        dropSelf(HumanTitaniumBlocks.TITANIUM_SIDING);
        dropSelf(HumanTitaniumBlocks.TITANIUM_SIDING_STAIRS);
        dropSelf(HumanTitaniumBlocks.TITANIUM_STAIRS);
        dropSelf(HumanTitaniumBlocks.TITANIUM_STANDING);
        dropSelf(HumanTitaniumBlocks.TITANIUM_STANDING_STAIRS);
        dropSelf(HumanTitaniumBlocks.TITANIUM_TRAP_DOOR);
        dropSelf(HumanTitaniumBlocks.TITANIUM_TREAD);
        dropSelf(HumanTitaniumBlocks.TITANIUM_TREAD_STAIRS);
        dropSelf(CoreBlocks.TRINITITE_BLOCK);
        dropSelf(CoreBlocks.URANIUM_BLOCK);
        dropSelf(CoreBlocks.ZINC_BLOCK);

        dropSelf(HumanPlasticBlocks.AISLE_HAZARD_PLASTIC);
        dropSelf(HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_STAIRS);
        dropSelf(HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_WALL);
        dropSelf(HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC);
        dropSelf(HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_STAIRS);
        dropSelf(HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_WALL);
        dropSelf(HumanPlasticBlocks.FIRE_HAZARD_PLASTIC);
        dropSelf(HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_STAIRS);
        dropSelf(HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_WALL);
        dropSelf(HumanPlasticBlocks.HAZARD_PLASTIC);
        dropSelf(HumanPlasticBlocks.HAZARD_PLASTIC_STAIRS);
        dropSelf(HumanPlasticBlocks.HAZARD_PLASTIC_WALL);
        dropSelf(HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC);
        dropSelf(HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_STAIRS);
        dropSelf(HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_WALL);
        dropSelf(HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC);
        dropSelf(HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_STAIRS);
        dropSelf(HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_WALL);
        dropSelf(HumanPlasticBlocks.SAFETY_PLASTIC);
        dropSelf(HumanPlasticBlocks.SAFETY_PLASTIC_STAIRS);
        dropSelf(HumanPlasticBlocks.SAFETY_PLASTIC_WALL);
        dropSelf(HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC);
        dropSelf(HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_STAIRS);
        dropSelf(HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_WALL);

        Stream.of(
            HumanBlocks.DYE_COLOR_TO_CONCRETE_STAIRS,
            HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC,
            HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC_STAIRS,
            HumanPlasticBlocks.DYE_COLOR_TO_FRAMED_PLASTIC,
            HumanIndustrialConcreteBlocks.DYE_COLOR_TO_INDUSTRIAL_CONCRETE,
            HumanIndustrialConcreteBlocks.DYE_COLOR_TO_INDUSTRIAL_CONCRETE_STAIRS,
            HumanIndustrialConcreteBlocks.DYE_COLOR_TO_INDUSTRIAL_CONCRETE_WALL,
            HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS,
            HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_PANE,
            HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_STAIRS,
            HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_TRAP_DOOR,
            HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_WALL,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_WALL,
            HumanPaddingBlocks.DYE_COLOR_TO_PADDING,
            HumanPaddingBlocks.DYE_COLOR_TO_PADDING_STAIRS,
            HumanPaddingBlocks.DYE_COLOR_TO_PANEL_PADDING,
            HumanPaddingBlocks.DYE_COLOR_TO_PANEL_PADDING_STAIRS,
            HumanPaddingBlocks.DYE_COLOR_TO_PIPE_PADDING,
            HumanPaddingBlocks.DYE_COLOR_TO_PIPE_PADDING_STAIRS,
            HumanPaddingBlocks.DYE_COLOR_TO_TILE_PADDING,
            HumanPaddingBlocks.DYE_COLOR_TO_TILE_PADDING_STAIRS,
            HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC,
            HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC_STAIRS,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_STAIRS,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_STAIRS
        )
            .map(Map::values)
            .flatMap(Collection::stream)
            .forEach(this::dropSelf);
    }

    private void generateSlabDrops() {
        dropSlab(HumanFerroaluminumBlocks.CUT_FERROALUMINUM_SLAB);
        dropSlab(HumanSteelBlocks.CUT_STEEL_SLAB);
        dropSlab(HumanTitaniumBlocks.CUT_TITANIUM_SLAB);
        dropSlab(HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_SIDING_SLAB);
        dropSlab(HumanFerroaluminumBlocks.FERROALUMINUM_FASTENED_STANDING_SLAB);
        dropSlab(HumanFerroaluminumBlocks.FERROALUMINUM_GRATE_SLAB);
        dropSlab(HumanFerroaluminumBlocks.FERROALUMINUM_PLATING_SLAB);
        dropSlab(HumanFerroaluminumBlocks.FERROALUMINUM_SIDING_SLAB);
        dropSlab(HumanFerroaluminumBlocks.FERROALUMINUM_SLAB);
        dropSlab(HumanFerroaluminumBlocks.FERROALUMINUM_STANDING_SLAB);
        dropSlab(HumanFerroaluminumBlocks.FERROALUMINUM_TREAD_SLAB);
        dropSlab(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_SLAB);
        // ⚠ Slabs and doors need their own tables, not dropSelf: a double slab must yield two, and a door is two
        // blocks that must yield ONE item rather than one per half.
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_SLAB.values().forEach(this::dropSlab);
        dropSlab(HumanSteelBlocks.STEEL_FASTENED_SIDING_SLAB);
        dropSlab(HumanSteelBlocks.STEEL_FASTENED_STANDING_SLAB);
        dropSlab(HumanSteelBlocks.STEEL_GRATE_SLAB);
        dropSlab(HumanSteelBlocks.STEEL_PLATING_SLAB);
        dropSlab(HumanSteelBlocks.STEEL_SIDING_SLAB);
        dropSlab(HumanSteelBlocks.STEEL_SLAB);
        dropSlab(HumanSteelBlocks.STEEL_STANDING_SLAB);
        dropSlab(HumanSteelBlocks.STEEL_TREAD_SLAB);
        dropSlab(HumanTitaniumBlocks.TITANIUM_FASTENED_SIDING_SLAB);
        dropSlab(HumanTitaniumBlocks.TITANIUM_FASTENED_STANDING_SLAB);
        dropSlab(HumanTitaniumBlocks.TITANIUM_GRATE_SLAB);
        dropSlab(HumanTitaniumBlocks.TITANIUM_PLATING_SLAB);
        dropSlab(HumanTitaniumBlocks.TITANIUM_SIDING_SLAB);
        dropSlab(HumanTitaniumBlocks.TITANIUM_SLAB);
        dropSlab(HumanTitaniumBlocks.TITANIUM_STANDING_SLAB);
        dropSlab(HumanTitaniumBlocks.TITANIUM_TREAD_SLAB);

        dropSlab(HumanPlasticBlocks.AISLE_HAZARD_PLASTIC_SLAB);
        dropSlab(HumanPlasticBlocks.ALIEN_HAZARD_PLASTIC_SLAB);
        dropSlab(HumanPlasticBlocks.FIRE_HAZARD_PLASTIC_SLAB);
        dropSlab(HumanPlasticBlocks.HAZARD_PLASTIC_SLAB);
        dropSlab(HumanPlasticBlocks.MACHINE_HAZARD_PLASTIC_SLAB);
        dropSlab(HumanPlasticBlocks.RADIATION_HAZARD_PLASTIC_SLAB);
        dropSlab(HumanPlasticBlocks.SAFETY_PLASTIC_SLAB);
        dropSlab(HumanPlasticBlocks.TRAFFIC_HAZARD_PLASTIC_SLAB);

        Stream.of(
            HumanBlocks.DYE_COLOR_TO_CONCRETE_SLAB,
            HumanPlasticBlocks.DYE_COLOR_TO_CUT_PLASTIC_SLAB,
            HumanIndustrialConcreteBlocks.DYE_COLOR_TO_INDUSTRIAL_CONCRETE_SLAB,
            HumanPaddingBlocks.DYE_COLOR_TO_PADDING_SLAB,
            HumanPaddingBlocks.DYE_COLOR_TO_PANEL_PADDING_SLAB,
            HumanPaddingBlocks.DYE_COLOR_TO_PIPE_PADDING_SLAB,
            HumanPaddingBlocks.DYE_COLOR_TO_TILE_PADDING_SLAB,
            HumanPlasticBlocks.DYE_COLOR_TO_PITTED_PLASTIC_SLAB,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_SLAB,
            HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_SLAB
        )
            .map(Map::values)
            .flatMap(Collection::stream)
            .forEach(this::dropSlab);
    }

    private void generateCustomDrops() {
        add(CoreBlocks.AUTUNITE_ORE, block -> createOreMultiDrop(block, HumanItems.AUTUNITE_DUST.get(), 2, 4));
        add(CoreBlocks.BAUXITE_ORE, block -> createOreDrop(block, HumanItems.RAW_BAUXITE.get()));
        add(CoreBlocks.DEEPSLATE_TITANIUM_ORE, block -> createOreDrop(block, HumanItems.RAW_TITANIUM.get()));
        add(CoreBlocks.DEEPSLATE_ZINC_ORE, block -> createOreMultiDrop(block, HumanItems.RAW_ZINC.get(), 2, 5));
        add(CoreBlocks.GALENA_ORE, block -> createOreDrop(block, HumanItems.RAW_GALENA.get()));
        add(HumanBlocks.LEAD_CHEST, this::createShulkerBoxDrop);
        add(HumanBlocks.AMMO_CHEST, this::createShulkerBoxDrop);
        add(CoreBlocks.LITHIUM_ORE, block -> createOreMultiDrop(block, HumanItems.LITHIUM_DUST.get(), 2, 4));
        add(CoreBlocks.MONAZITE_ORE, block -> createOreDrop(block, HumanItems.RAW_MONAZITE.get()));
        add(CoreBlocks.ZINC_ORE, block -> createOreMultiDrop(block, HumanItems.RAW_ZINC.get(), 2, 5));
        add(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_DOOR, this::createDoorTable);
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_DOOR.values()
            .forEach(blockSupplier -> add(blockSupplier, this::createDoorTable));
        add(HumanFerroaluminumBlocks.FERROALUMINUM_DOOR, this::createDoorTable);
        add(HumanSteelBlocks.STEEL_DOOR, this::createDoorTable);

        dropSelf(HumanCompositeBlocks.CHISELED_COMPOSITE);
        dropSelf(HumanCompositeBlocks.CUT_COMPOSITE);
        dropSelf(HumanCompositeBlocks.CUT_COMPOSITE_STAIRS);
        dropSelf(HumanCompositeBlocks.COMPOSITE_BLOCK);
        dropSelf(HumanCompositeBlocks.COMPOSITE_BUTTON);
        dropSelf(HumanCompositeBlocks.COMPOSITE_CHAIN_FENCE);
        dropSelf(HumanCompositeBlocks.COMPOSITE_COLUMN);
        dropSelf(HumanCompositeBlocks.COMPOSITE_FASTENED_SIDING);
        dropSelf(HumanCompositeBlocks.COMPOSITE_FASTENED_SIDING_STAIRS);
        dropSelf(HumanCompositeBlocks.COMPOSITE_FASTENED_STANDING);
        dropSelf(HumanCompositeBlocks.COMPOSITE_FASTENED_STANDING_STAIRS);
        dropSelf(HumanCompositeBlocks.COMPOSITE_GRATE);
        dropSelf(HumanCompositeBlocks.COMPOSITE_GRATE_STAIRS);
        dropSelf(HumanCompositeBlocks.COMPOSITE_PLATING);
        dropSelf(HumanCompositeBlocks.COMPOSITE_PLATING_STAIRS);
        dropSelf(HumanCompositeBlocks.COMPOSITE_PRESSURE_PLATE);
        dropSelf(HumanCompositeBlocks.COMPOSITE_SIDING);
        dropSelf(HumanCompositeBlocks.COMPOSITE_SIDING_STAIRS);
        dropSelf(HumanCompositeBlocks.COMPOSITE_STAIRS);
        dropSelf(HumanCompositeBlocks.COMPOSITE_STANDING);
        dropSelf(HumanCompositeBlocks.COMPOSITE_STANDING_STAIRS);
        dropSelf(HumanCompositeBlocks.COMPOSITE_TRAP_DOOR);
        dropSelf(HumanCompositeBlocks.COMPOSITE_TREAD);
        dropSelf(HumanCompositeBlocks.COMPOSITE_TREAD_STAIRS);
        // [stated] Sep 28 - every USCM block drops itself (the lockers also spill their contents, from the block).
        com.human.common.registry.init.block.HumanUscmBlocks.ALL.forEach(block -> dropSelf(block));
        // [stated] Oct 20 - every W-Y block drops itself (the locker also spills its contents, from the block).
        com.human.common.registry.init.block.HumanWyBlocks.ALL.forEach(block -> dropSelf(block));
        dropSlab(HumanCompositeBlocks.CUT_COMPOSITE_SLAB);
        dropSlab(HumanCompositeBlocks.COMPOSITE_FASTENED_SIDING_SLAB);
        dropSlab(HumanCompositeBlocks.COMPOSITE_FASTENED_STANDING_SLAB);
        dropSlab(HumanCompositeBlocks.COMPOSITE_GRATE_SLAB);
        dropSlab(HumanCompositeBlocks.COMPOSITE_PLATING_SLAB);
        dropSlab(HumanCompositeBlocks.COMPOSITE_SIDING_SLAB);
        dropSlab(HumanCompositeBlocks.COMPOSITE_SLAB);
        dropSlab(HumanCompositeBlocks.COMPOSITE_STANDING_SLAB);
        dropSlab(HumanCompositeBlocks.COMPOSITE_TREAD_SLAB);
        add(HumanCompositeBlocks.COMPOSITE_DOOR, this::createDoorTable);
        add(HumanTitaniumBlocks.TITANIUM_DOOR, this::createDoorTable);
        // ⭐ NOT createNameableBlockEntityTable: that copies CUSTOM_NAME and nothing else, so a full ESU would drop
        // empty and destroy everything stored. Tier-2 recipes consume the tier-1 block, so breaking one to upgrade is
        // the intended path - the charge has to survive it.
        add(HumanBlocks.BATTERY.get(), this::createBatteryTable);
        add(HumanBlocks.CHARGING_HUB.get(), this::createPowerStorageTable);
        // Machines keep their custom name; their contents spill when broken, as a furnace's do.
        add(HumanBlocks.ELECTRIC_FURNACE, this::createNameableBlockEntityTable);
        add(HumanBlocks.ELECTRIC_SMOKER, this::createNameableBlockEntityTable);
        add(HumanBlocks.ELECTRIC_BLAST_FURNACE, this::createNameableBlockEntityTable);
        add(HumanBlocks.ELECTRIC_INDUSTRIAL_FURNACE, this::createNameableBlockEntityTable);
        add(HumanBlocks.ELECTRIC_GRINDER, this::createNameableBlockEntityTable);
        add(HumanBlocks.FUSE_PANEL, this::createNameableBlockEntityTable);
        dropSelf(HumanBlocks.FLUID_PIPE);
        dropSelf(HumanBlocks.ITEM_PIPE);
        add(HumanBlocks.ELECTRIC_PRESS, this::createNameableBlockEntityTable);
        add(HumanBlocks.ELECTRIC_HYDRATOR, this::createNameableBlockEntityTable);
        add(HumanBlocks.ELECTRIC_FREEZER, this::createNameableBlockEntityTable);
        // [stated] Oct 17 - the vaporizer, like every machine. Its top block and the acid liquids drop nothing.
        add(com.human.common.registry.init.HumanVaporizer.VAPORIZER, this::createNameableBlockEntityTable);
        add(com.human.common.registry.init.HumanVaporizer.WY_VAPORIZER, this::createNameableBlockEntityTable);
        add(com.human.common.registry.init.HumanVaporizer.USCM_VAPORIZER, this::createNameableBlockEntityTable);
        // [stated] Oct 17 - the distributor drops itself; its slot, cards and chamber spill/empty like the other
        // machines.
        add(com.human.common.registry.init.HumanDistributor.DISTRIBUTOR, this::createNameableBlockEntityTable);
        add(com.human.common.registry.init.HumanDistributor.WY_DISTRIBUTOR, this::createNameableBlockEntityTable);
        add(com.human.common.registry.init.HumanDistributor.USCM_DISTRIBUTOR, this::createNameableBlockEntityTable);
        dropSelf(HumanBlocks.MACHINE_BLOCK);
        dropSelf(HumanBlocks.ELECTRONICS_WORKSTATION);

        // Fluid tanks drop as plain blocks - the item form does not keep fluid, as in Create.
        dropSelf(HumanBlocks.STEEL_FLUID_TANK);
        dropSelf(HumanBlocks.FERROALUMINUM_FLUID_TANK);
        dropSelf(HumanBlocks.ALUMINUM_FLUID_TANK);
        dropSelf(HumanBlocks.TITANIUM_FLUID_TANK);
        dropSelf(HumanBlocks.WY_FLUID_TANK);
        dropSelf(HumanBlocks.USCM_FLUID_TANK);
        add(HumanBlocks.INDUSTRIAL_FURNACE, this::createNameableBlockEntityTable);
        add(HumanBlocks.INFINITE_POWER_GENERATOR, this::createNameableBlockEntityTable);
        // [stated] Oct 20 - the creative test blocks drop themselves.
        dropSelf(com.human.common.registry.init.HumanCreativeBlocks.CREATIVE_BATTERY.get());
        dropSelf(com.human.common.registry.init.HumanCreativeBlocks.CREATIVE_FLUID_TANK.get());
        dropSelf(CoreBlocks.LATEX_BLOCK.get());
        dropSelf(CoreBlocks.KELP_BLOCK.get());
        dropSelf(CoreBlocks.LATEX_TAP.get());
        // Breaking a latex cauldron gives back the cauldron; the latex inside is lost, as with every vanilla cauldron.
        add(CoreBlocks.LATEX_CAULDRON.get(), createSingleItemTable(net.minecraft.world.level.block.Blocks.CAULDRON));
        dropSelf(CoreBlocks.RUBBER_BLOCK.get());
        // ⚠ THE SAME TABLE AS THE BATTERY, and not because generators hold charge - they do not. They hold a BREAKER
        // LAYOUT, and vanilla's nameable table copies CUSTOM_NAME only, so every port a player closed would reopen
        // the moment they moved the block. A machine should come back configured the way it left.
        add(HumanBlocks.HYDROELECTRIC_GENERATOR.get(), this::createPowerStorageTable);
        add(HumanBlocks.SOLAR_PANEL.get(), this::createPowerStorageTable);
        add(HumanBlocks.THERMOELECTRIC_GENERATOR.get(), this::createPowerStorageTable);
        add(HumanBlocks.AEROKINETIC_GENERATOR.get(), this::createPowerStorageTable);
    }

    private void generateOtherDrops() {}

    public void add(Supplier<? extends Block> blockSupplier, Function<Block, LootTable.Builder> factory) {
        var block = blockSupplier.get();
        add(block, factory);
        TOUCHED_ENTRIES.add(block);
    }

    public void dropOther(Supplier<? extends Block> blockSupplier, Supplier<? extends ItemLike> itemLikeSupplier) {
        var block = blockSupplier.get();
        dropOther(block, itemLikeSupplier.get());
        TOUCHED_ENTRIES.add(block);
    }

    public void dropSelf(Supplier<? extends Block> blockSupplier) {
        var block = blockSupplier.get();
        dropSelf(block);
        TOUCHED_ENTRIES.add(block);
    }

    public void dropSlab(Supplier<? extends Block> blockSupplier) {
        var block = blockSupplier.get();
        add(block, createSlabItemTable(block));
        TOUCHED_ENTRIES.add(block);
    }

    public LootTable.Builder createOreMultiDrop(Block block, Item item, int min, int max) {
        var registryLookup = registries.lookupOrThrow(Registries.ENCHANTMENT);

        return createSilkTouchDispatchTable(
            block,
            applyExplosionDecay(
                block,
                LootItem.lootTableItem(item)
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))
                    .apply(ApplyBonusCount.addUniformBonusCount(registryLookup.getOrThrow(Enchantments.FORTUNE)))
            )
        );
    }
}
