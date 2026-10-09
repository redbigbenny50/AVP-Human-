package com.human.common.registry.init.item;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.blib.api.common.registry.v1.impl.BLibAzureLibIdentityRegistry;
import com.human.Human;
import com.human.common.gameplay.block_item.AmmoChestBlockItem;
import com.human.common.gameplay.block_item.LeadChestBlockItem;
import com.human.common.gameplay.block_item.SentryTurretBlockItem;
import com.human.common.gameplay.item.HydroelectricGeneratorBlockItem;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.block.CoreBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HumanBlockItems {

    private static final BLibRegistry<Item> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibAzureLibIdentityRegistry IDENTITY_REGISTRY = Human.MOD.registries().createAzureLibIdentityRegistry();

    public static final BLibHolder<BlockItem> ALUMINUM_BLOCK = create("aluminum_block", CoreBlocks.ALUMINUM_BLOCK);

    public static final BLibHolder<BlockItem> AMMO_CHEST = createWithSupplier("ammo_chest", AmmoChestBlockItem::new);

    public static final BLibHolder<BlockItem> ASH_BLOCK = create("ash_block", CoreBlocks.ASH_BLOCK);

    public static final BLibHolder<BlockItem> LATEX_BLOCK = create("latex_block", CoreBlocks.LATEX_BLOCK);

    public static final BLibHolder<BlockItem> KELP_BLOCK = create("kelp_block", CoreBlocks.KELP_BLOCK);

    /**
     * ⚠ WITHOUT THIS THE TAP IS UNOBTAINABLE AND ITS RECIPE CRASHES DATAGEN. A Block with no BlockItem resolves
     * {@code asItem()} to AIR, so {@code new ItemStack(block, 1)} becomes air with count 0 and the recipe codec rejects
     * it: "Value must be within range [1;99]: 0; Item must not be minecraft:air".
     */
    public static final BLibHolder<BlockItem> LATEX_TAP = create("latex_tap", CoreBlocks.LATEX_TAP);

    public static final BLibHolder<BlockItem> RUBBER_BLOCK = create("rubber_block", CoreBlocks.RUBBER_BLOCK);

    public static final BLibHolder<BlockItem> AUTUNITE_BLOCK = create("autunite_block", CoreBlocks.AUTUNITE_BLOCK);

    public static final BLibHolder<BlockItem> AUTUNITE_ORE = create("autunite_ore", CoreBlocks.AUTUNITE_ORE);

    /**
     * ⚠⚠ STACKS TO ONE, AND THAT IS A CORRECTNESS RULE NOT A FLAVOUR ONE. A store carries its charge on the item, and
     * two stacks whose components differ only by a number are still two different stacks - but a player holding a full
     * one and an empty one would expect them to merge, and any code path that DID merge them would silently destroy one
     * battery's charge. One per stack removes the question. Same for the charging hub.
     */
    public static final BLibHolder<BlockItem> BATTERY = create(
        "battery",
        HumanBlocks.BATTERY,
        new Item.Properties().stacksTo(1)
    );

    public static final BLibHolder<BlockItem> CHARGING_HUB = create(
        "charging_hub",
        HumanBlocks.CHARGING_HUB,
        new Item.Properties().stacksTo(1)
    );

    public static final BLibHolder<BlockItem> MACHINE_BLOCK = create("machine_block", HumanBlocks.MACHINE_BLOCK);

    public static final BLibHolder<BlockItem> ELECTRIC_FURNACE = create("electric_furnace", HumanBlocks.ELECTRIC_FURNACE);

    public static final BLibHolder<BlockItem> ELECTRIC_SMOKER = create("electric_smoker", HumanBlocks.ELECTRIC_SMOKER);

    public static final BLibHolder<BlockItem> ELECTRIC_BLAST_FURNACE = create(
        "electric_blast_furnace",
        HumanBlocks.ELECTRIC_BLAST_FURNACE
    );

    public static final BLibHolder<BlockItem> ELECTRIC_INDUSTRIAL_FURNACE = create(
        "electric_industrial_furnace",
        HumanBlocks.ELECTRIC_INDUSTRIAL_FURNACE
    );

    public static final BLibHolder<BlockItem> ELECTRIC_GRINDER = create("electric_grinder", HumanBlocks.ELECTRIC_GRINDER);

    public static final BLibHolder<BlockItem> ELECTRIC_PRESS = create("electric_press", HumanBlocks.ELECTRIC_PRESS);

    public static final BLibHolder<BlockItem> ELECTRIC_HYDRATOR = create("electric_hydrator", HumanBlocks.ELECTRIC_HYDRATOR);

    public static final BLibHolder<BlockItem> ELECTRIC_FREEZER = create("electric_freezer", HumanBlocks.ELECTRIC_FREEZER);

    public static final BLibHolder<BlockItem> STEEL_FLUID_TANK = create("steel_fluid_tank", HumanBlocks.STEEL_FLUID_TANK);

    public static final BLibHolder<BlockItem> FERROALUMINUM_FLUID_TANK = create(
        "ferroaluminum_fluid_tank",
        HumanBlocks.FERROALUMINUM_FLUID_TANK
    );

    public static final BLibHolder<BlockItem> ALUMINUM_FLUID_TANK = create("aluminum_fluid_tank", HumanBlocks.ALUMINUM_FLUID_TANK);

    public static final BLibHolder<BlockItem> TITANIUM_FLUID_TANK = create("titanium_fluid_tank", HumanBlocks.TITANIUM_FLUID_TANK);

    public static final BLibHolder<BlockItem> WY_FLUID_TANK = create("wy_fluid_tank", HumanBlocks.WY_FLUID_TANK);

    public static final BLibHolder<BlockItem> USCM_FLUID_TANK = create("uscm_fluid_tank", HumanBlocks.USCM_FLUID_TANK);

    public static final BLibHolder<BlockItem> FUSE_PANEL = create("fuse_panel", HumanBlocks.FUSE_PANEL);

    public static final BLibHolder<BlockItem> FLUID_PIPE = create("fluid_pipe", HumanBlocks.FLUID_PIPE);

    public static final BLibHolder<BlockItem> ITEM_PIPE = create("item_pipe", HumanBlocks.ITEM_PIPE);

    public static final BLibHolder<BlockItem> ELECTRONICS_WORKSTATION = create(
        "electronics_workstation",
        HumanBlocks.ELECTRONICS_WORKSTATION
    );

    public static final BLibHolder<BlockItem> BAUXITE_ORE = create("bauxite_ore", CoreBlocks.BAUXITE_ORE);

    public static final BLibHolder<BlockItem> BLUEPRINT_BLOCK = create("blueprint_block", HumanBlocks.BLUEPRINT_BLOCK);

    public static final BLibHolder<BlockItem> CABLE = create("cable", HumanBlocks.CABLE);

    public static final BLibHolder<BlockItem> CRYOCONDUIT = create("cryoconduit", HumanBlocks.CRYOCONDUIT);

    public static final BLibHolder<BlockItem> CRYOTANK = create("cryotank", HumanBlocks.CRYOTANK);

    public static final BLibHolder<BlockItem> CRYOTUBE_CONTROLLER = create("cryotube_controller", HumanBlocks.CRYOTUBE_CONTROLLER);

    public static final BLibHolder<BlockItem> STEEL_CRYOTUBE_CONTROLLER = create(
        "steel_cryotube_controller",
        HumanBlocks.STEEL_CRYOTUBE_CONTROLLER
    );

    public static final BLibHolder<BlockItem> WY_CRYOTUBE_CONTROLLER = create("wy_cryotube_controller", HumanBlocks.WY_CRYOTUBE_CONTROLLER);

    public static final BLibHolder<BlockItem> USCM_CRYOTUBE_CONTROLLER = create(
        "uscm_cryotube_controller",
        HumanBlocks.USCM_CRYOTUBE_CONTROLLER
    );

    public static final BLibHolder<BlockItem> BRASS_BLOCK = create("brass_block", CoreBlocks.BRASS_BLOCK);

    public static final BLibHolder<BlockItem> DEEPSLATE_TITANIUM_ORE = create(
        "deepslate_titanium_ore",
        CoreBlocks.DEEPSLATE_TITANIUM_ORE
    );

    public static final BLibHolder<BlockItem> DEEPSLATE_ZINC_ORE = create("deepslate_zinc_ore", CoreBlocks.DEEPSLATE_ZINC_ORE);

    public static final BLibHolder<BlockItem> DESK_TERMINAL_BLOCK = create("desk_terminal", HumanBlocks.DESK_TERMINAL_BLOCK);

    public static final BLibHolder<BlockItem> GALENA_ORE = create("galena_ore", CoreBlocks.GALENA_ORE);

    public static final BLibHolder<BlockItem> INDUSTRIAL_FURNACE_BLOCK = create(
        "industrial_furnace_block",
        HumanBlocks.INDUSTRIAL_FURNACE
    );

    public static final BLibHolder<BlockItem> INFINITE_POWER_GENERATOR = create(
        "infinite_power_generator",
        HumanBlocks.INFINITE_POWER_GENERATOR
    );

    public static final BLibHolder<BlockItem> LEAD_BLOCK = create("lead_block", CoreBlocks.LEAD_BLOCK);

    public static final BLibHolder<BlockItem> LEAD_CHEST = createWithSupplier("lead_chest", LeadChestBlockItem::new);

    public static final BLibHolder<BlockItem> LITHIUM_BLOCK = create("lithium_block", CoreBlocks.LITHIUM_BLOCK);

    public static final BLibHolder<BlockItem> LITHIUM_ORE = create("lithium_ore", CoreBlocks.LITHIUM_ORE);

    public static final BLibHolder<BlockItem> MONAZITE_ORE = create("monazite_ore", CoreBlocks.MONAZITE_ORE);

    public static final BLibHolder<BlockItem> NUKE_BLOCK = create("nuke", HumanBlocks.NUKE_BLOCK);

    public static final BLibHolder<BlockItem> RAW_BAUXITE_BLOCK = create("raw_bauxite_block", CoreBlocks.RAW_BAUXITE_BLOCK);

    public static final BLibHolder<BlockItem> RAW_GALENA_BLOCK = create("raw_galena_block", CoreBlocks.RAW_GALENA_BLOCK);

    public static final BLibHolder<BlockItem> RAW_MONAZITE_BLOCK = create("raw_monazite_block", CoreBlocks.RAW_MONAZITE_BLOCK);

    public static final BLibHolder<BlockItem> RAW_TITANIUM_BLOCK = create("raw_titanium_block", CoreBlocks.RAW_TITANIUM_BLOCK);

    public static final BLibHolder<BlockItem> RAW_ZINC_BLOCK = create("raw_zinc_block", CoreBlocks.RAW_ZINC_BLOCK);

    public static final BLibHolder<BlockItem> RAZOR_WIRE = create("razor_wire", HumanBlocks.RAZOR_WIRE);

    public static final BLibHolder<BlockItem> REDSTONE_GENERATOR = create("redstone_generator", HumanBlocks.REDSTONE_GENERATOR);

    public static final BLibHolder<BlockItem> RESONATOR_BLOCK = create("resonator", HumanBlocks.RESONATOR_BLOCK);

    public static final BLibHolder<BlockItem> SENTRY_TURRET = createWithSupplier("sentry_turret", SentryTurretBlockItem::new);

    public static final BLibHolder<BlockItem> SILICA_GRAVEL = create("silica_gravel", CoreBlocks.SILICA_GRAVEL);

    // TODO: Change this to "silicon_block" with 0.2.0.
    public static final BLibHolder<BlockItem> SILICON_BLOCK = create("raw_silica_block", CoreBlocks.SILICON_BLOCK);

    /**
     * ⚠ Custom BlockItem: the hub - which is the block itself, and so the CABLE PORT - sits a full block above the
     * surface clicked. See HydroelectricGeneratorBlockItem.
     */
    public static final BLibHolder<BlockItem> HYDROELECTRIC_GENERATOR = createWithSupplier(
        "hydroelectric_generator",
        HydroelectricGeneratorBlockItem::new
    );

    public static final BLibHolder<BlockItem> SOLAR_PANEL = create(
        "solar_panel",
        HumanBlocks.SOLAR_PANEL
    );

    public static final BLibHolder<BlockItem> THERMOELECTRIC_GENERATOR = create(
        "thermoelectric_generator",
        HumanBlocks.THERMOELECTRIC_GENERATOR
    );

    public static final BLibHolder<BlockItem> TRINITITE_BLOCK = create("trinitite_block", CoreBlocks.TRINITITE_BLOCK);

    public static final BLibHolder<BlockItem> URANIUM_BLOCK = create("uranium_block", CoreBlocks.URANIUM_BLOCK);

    public static final BLibHolder<BlockItem> AEROKINETIC_GENERATOR = create(
        "aerokinetic_generator",
        HumanBlocks.AEROKINETIC_GENERATOR
    );

    public static final BLibHolder<BlockItem> ZINC_BLOCK = create("zinc_block", CoreBlocks.ZINC_BLOCK);

    public static final BLibHolder<BlockItem> ZINC_ORE = create("zinc_ore", CoreBlocks.ZINC_ORE);

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_CONCRETE_SLAB =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_concrete_slab",
                            HumanBlocks.DYE_COLOR_TO_CONCRETE_SLAB.get(dyeColor)
                        )
                    )
                )
        );

    public static final Map<DyeColor, Supplier<BlockItem>> DYE_COLOR_TO_CONCRETE_STAIRS =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_concrete_stairs",
                            HumanBlocks.DYE_COLOR_TO_CONCRETE_STAIRS.get(dyeColor)
                        )
                    )
                )
        );

    private static BLibHolder<BlockItem> create(String path, Supplier<? extends Block> blockSupplier) {
        return create(path, blockSupplier, new Item.Properties());
    }

    private static BLibHolder<BlockItem> create(String path, Supplier<? extends Block> blockSupplier, Item.Properties properties) {
        return createBlockItem(path, () -> new BlockItem(blockSupplier.get(), properties));
    }

    private static BLibHolder<BlockItem> createWithSupplier(String id, Supplier<BlockItem> blockItemSupplier) {
        return createBlockItem(id, blockItemSupplier);
    }

    private static BLibHolder<BlockItem> createBlockItem(String path, Supplier<BlockItem> blockItemSupplier) {
        return REGISTRY.createHolder(path, blockItemSupplier);
    }

    public static void initialize() {
        REGISTRY.registerAll();
        IDENTITY_REGISTRY.register(RESONATOR_BLOCK);
        IDENTITY_REGISTRY.register(SENTRY_TURRET);
    }
}
