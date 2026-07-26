package com.human.common.registry.init;

import com.blib.api.common.block.v1.BlockPropertyBuilder;
import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block.AmmoChestBlock;
import com.human.common.gameplay.block.IndustrialFurnaceBlock;
import com.human.common.gameplay.block.LeadChestBlock;
import com.human.common.gameplay.block.NukeBlock;
import com.human.common.gameplay.block.RazorWireBlock;
import com.human.common.gameplay.block.RedstoneGeneratorBlock;
import com.human.common.gameplay.block.SentryTurretBlock;
import com.human.common.gameplay.block.power.BatteryBlock;
import com.human.common.gameplay.block.power.CableBlock;
import com.human.common.gameplay.block.power.DeskTerminalBlock;
import com.human.common.gameplay.block.power.InfinitePowerGeneratorBlock;
import com.human.common.gameplay.block.power.ResonatorBlock;
import com.human.common.gameplay.block.power.SolarPanelBlock;
import com.human.common.gameplay.block.power.ThermalGeneratorBlock;
import com.human.common.gameplay.block.power.WindTurbineBlock;
import com.human.common.gameplay.block.property.HumanBlockProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HumanBlocks {

    public static final BLibRegistry<Block> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    public static final BLibHolder<Block> AMMO_CHEST = create(
        "ammo_chest",
        () -> new AmmoChestBlock(HumanBlockProperties.LEAD.build())
    );

    public static final BLibHolder<Block> BATTERY = create(
        "battery",
        // TODO: Use custom properties here.
        () -> new BatteryBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE))
    );

    public static final BLibHolder<Block> BLUEPRINT_BLOCK = create("blueprint_block", HumanBlockProperties.STEEL);

    public static final BLibHolder<Block> CABLE = create(
        "cable",
        // TODO: Use custom properties here.
        () -> new CableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE).noOcclusion())
    );

    public static final BLibHolder<Block> DESK_TERMINAL_BLOCK = create(
        "desk_terminal",
        () -> new DeskTerminalBlock(HumanBlockProperties.STEEL.build().noOcclusion())
    );

    public static final BLibHolder<Block> INDUSTRIAL_FURNACE = create(
        "industrial_furnace_block",
        // TODO: Use custom properties here.
        () -> new IndustrialFurnaceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLAST_FURNACE))
    );

    public static final BLibHolder<Block> INFINITE_POWER_GENERATOR = create(
        "infinite_power_generator",
        // TODO: Use custom properties here.
        () -> new InfinitePowerGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE))
    );

    public static final BLibHolder<Block> LEAD_CHEST = create(
        "lead_chest",
        () -> new LeadChestBlock(HumanBlockProperties.LEAD.build())
    );

    public static final BLibHolder<Block> NUKE_BLOCK = create("nuke", () -> new NukeBlock(HumanBlockProperties.NUKE.build()));

    public static final BLibHolder<Block> RAZOR_WIRE = create(
        "razor_wire",
        () -> new RazorWireBlock(HumanBlockProperties.RAZOR_WIRE.build())
    );

    public static final BLibHolder<Block> REDSTONE_GENERATOR = create(
        "redstone_generator",
        () -> new RedstoneGeneratorBlock(HumanBlockProperties.STEEL.build().randomTicks())
    );

    public static final BLibHolder<Block> RESONATOR_BLOCK = create(
        "resonator",
        () -> new ResonatorBlock(HumanBlockProperties.STEEL.build().noOcclusion())
    );

    public static final BLibHolder<Block> SENTRY_TURRET = create("sentry_turret", SentryTurretBlock::new);

    public static final BLibHolder<Block> SOLAR_PANEL = create(
        "solar_panel",
        // TODO: Use custom properties here.
        () -> new SolarPanelBlock(HumanBlockProperties.STEEL.build().noOcclusion())
    );

    public static final BLibHolder<Block> THERMAL_GENERATOR = create(
        "thermal_generator",
        // TODO: Use custom properties here.
        () -> new ThermalGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE))
    );

    public static final BLibHolder<Block> WIND_TURBINE = create(
        "wind_turbine",
        // TODO: Use custom properties here.
        () -> new WindTurbineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE))
    );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_CONCRETE_SLAB =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_concrete_slab",
                            () -> new SlabBlock(HumanBlockProperties.DYE_COLOR_TO_CONCRETE_PROPERTIES.get(dyeColor).build())
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    public static final Map<DyeColor, BLibHolder<Block>> DYE_COLOR_TO_CONCRETE_STAIRS =
        Collections.unmodifiableMap(
            Arrays.stream(DyeColor.values())
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        dyeColor -> create(
                            dyeColor.getName() + "_concrete_stairs",
                            () -> new StairBlock(
                                HumanBlockProperties.DYE_COLOR_TO_CONCRETE_BLOCKS.get(dyeColor).defaultBlockState(),
                                HumanBlockProperties.DYE_COLOR_TO_CONCRETE_PROPERTIES.get(dyeColor).build()
                            )
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                    )
                )
        );

    private static BLibHolder<Block> create(String path, BlockPropertyBuilder blockPropertyBuilder) {
        return create(path, () -> new Block(blockPropertyBuilder.build()));
    }

    private static <T extends Block> BLibHolder<T> create(String path, Supplier<T> blockSupplier) {
        return REGISTRY.createHolder(path, blockSupplier);
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
