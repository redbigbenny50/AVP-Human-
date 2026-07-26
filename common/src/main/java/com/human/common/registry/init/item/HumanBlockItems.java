package com.human.common.registry.init.item;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.blib.api.common.registry.v1.impl.BLibAzureLibIdentityRegistry;
import com.human.Human;
import com.human.common.gameplay.block_item.AmmoChestBlockItem;
import com.human.common.gameplay.block_item.LeadChestBlockItem;
import com.human.common.gameplay.block_item.SentryTurretBlockItem;
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

    public static final BLibHolder<BlockItem> AUTUNITE_BLOCK = create("autunite_block", CoreBlocks.AUTUNITE_BLOCK);

    public static final BLibHolder<BlockItem> AUTUNITE_ORE = create("autunite_ore", CoreBlocks.AUTUNITE_ORE);

    public static final BLibHolder<BlockItem> BATTERY = create(
        "battery",
        HumanBlocks.BATTERY
    );

    public static final BLibHolder<BlockItem> BAUXITE_ORE = create("bauxite_ore", CoreBlocks.BAUXITE_ORE);

    public static final BLibHolder<BlockItem> BLUEPRINT_BLOCK = create("blueprint_block", HumanBlocks.BLUEPRINT_BLOCK);

    public static final BLibHolder<BlockItem> CABLE = create("cable", HumanBlocks.CABLE);

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

    public static final BLibHolder<BlockItem> SOLAR_PANEL = create(
        "solar_panel",
        HumanBlocks.SOLAR_PANEL
    );

    public static final BLibHolder<BlockItem> THERMAL_GENERATOR = create(
        "thermal_generator",
        HumanBlocks.THERMAL_GENERATOR
    );

    public static final BLibHolder<BlockItem> TRINITITE_BLOCK = create("trinitite_block", CoreBlocks.TRINITITE_BLOCK);

    public static final BLibHolder<BlockItem> URANIUM_BLOCK = create("uranium_block", CoreBlocks.URANIUM_BLOCK);

    public static final BLibHolder<BlockItem> WIND_TURBINE = create(
        "wind_turbine",
        HumanBlocks.WIND_TURBINE
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
