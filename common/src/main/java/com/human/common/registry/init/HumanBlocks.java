package com.human.common.registry.init;

import com.blib.api.common.block.v1.BlockPropertyBuilder;
import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block.AmmoChestBlock;
import com.human.common.gameplay.block.ElectronicsWorkstationBlock;
import com.human.common.gameplay.block.IndustrialFurnaceBlock;
import com.human.common.gameplay.block.LeadChestBlock;
import com.human.common.gameplay.block.NukeBlock;
import com.human.common.gameplay.block.RazorWireBlock;
import com.human.common.gameplay.block.RedstoneGeneratorBlock;
import com.human.common.gameplay.block.SentryTurretBlock;
import com.human.common.gameplay.block.power.AerokineticGeneratorBlock;
import com.human.common.gameplay.block.power.BatteryBlock;
import com.human.common.gameplay.block.power.CableBlock;
import com.human.common.gameplay.block.power.ChargingHubBlock;
import com.human.common.gameplay.block.power.DeskTerminalBlock;
import com.human.common.gameplay.block.power.HydroelectricGeneratorBlock;
import com.human.common.gameplay.block.power.InfinitePowerGeneratorBlock;
import com.human.common.gameplay.block.power.ResonatorBlock;
import com.human.common.gameplay.block.power.SolarPanelBlock;
import com.human.common.gameplay.block.power.ThermoelectricGeneratorBlock;
import com.human.common.gameplay.block.power.machine.ElectricMachineBlock;
import com.human.common.gameplay.block.property.HumanBlockProperties;
import com.human.common.gameplay.machine.ElectricMachineKind;
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

    /**
     * ⭐ [stated] Sep 29: the NEW full-block battery at 1.5M. The block that used to hold this id - the ESU with the
     * charging plate - is now {@link #CHARGING_HUB}. No jar had shipped with the ESU, so the id moves without a
     * migration. A plain full cube, so it keeps occlusion.
     */
    public static final BLibHolder<Block> BATTERY = create(
        "battery",
        () -> new BatteryBlock(HumanBlockProperties.STEEL.build())
    );

    /** The old ESU: its model, art and charging plate, at 500k. */
    public static final BLibHolder<Block> CHARGING_HUB = create(
        "charging_hub",
        () -> new ChargingHubBlock(HumanBlockProperties.STEEL.build().noOcclusion())
    );

    public static final BLibHolder<Block> BLUEPRINT_BLOCK = create("blueprint_block", HumanBlockProperties.STEEL);

    /**
     * [stated] Oct 1: the cryoconduit - connection only, like the power cable, from a cryotube controller to its
     * cryotanks. No block entity and no tick.
     */
    public static final BLibHolder<Block> CRYOCONDUIT = create(
        "cryoconduit",
        () -> new com.human.common.gameplay.block.cryo.CryoConduitBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).strength(1.5F, 6.0F).noOcclusion()
        )
    );

    /**
     * [stated] Oct 1-2: the cryotank block. Stack into a solid box for a bigger tank - 8 per craft, aluminum over
     * industrial glass over steel.
     */
    public static final BLibHolder<Block> CRYOTANK = create(
        "cryotank",
        () -> new com.human.common.gameplay.block.cryo.CryotankBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .sound(net.minecraft.world.level.block.SoundType.GLASS)
                // [stated] Oct 2: glows while a specimen is inside.
                .lightLevel(state -> state.getValue(com.human.common.gameplay.block.cryo.CryotankBlock.LIT) ? 10 : 0)
                .noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
        )
    );

    /** [stated] Oct 1: the four editions share one block class and one block entity; only the art differs. */
    public static final BLibHolder<Block> CRYOTUBE_CONTROLLER = createCryotubeController("cryotube_controller");

    public static final BLibHolder<Block> STEEL_CRYOTUBE_CONTROLLER = createCryotubeController("steel_cryotube_controller");

    /** [stated] Oct 1: W-Y and USCM come from the fabricator only - no crafting recipe. */
    public static final BLibHolder<Block> WY_CRYOTUBE_CONTROLLER = createCryotubeController("wy_cryotube_controller");

    public static final BLibHolder<Block> USCM_CRYOTUBE_CONTROLLER = createCryotubeController("uscm_cryotube_controller");

    private static BLibHolder<Block> createCryotubeController(String path) {
        return create(
            path,
            () -> new com.human.common.gameplay.block.cryo.CryotubeControllerBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
            )
        );
    }

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

    // ---- Tier-1 machines. [stated] Sep 27: uniform single blocks built on the machine block. ----

    /** The bare machine block - the casing every tier-1 machine is built on. No function of its own. */
    public static final BLibHolder<Block> MACHINE_BLOCK = create("machine_block", HumanBlockProperties.STEEL);

    public static final BLibHolder<Block> ELECTRIC_FURNACE = create(
        "electric_furnace",
        () -> new ElectricMachineBlock(HumanBlockProperties.STEEL.build(), ElectricMachineKind.FURNACE)
    );

    public static final BLibHolder<Block> ELECTRIC_SMOKER = create(
        "electric_smoker",
        () -> new ElectricMachineBlock(HumanBlockProperties.STEEL.build(), ElectricMachineKind.SMOKER)
    );

    public static final BLibHolder<Block> ELECTRIC_BLAST_FURNACE = create(
        "electric_blast_furnace",
        () -> new ElectricMachineBlock(HumanBlockProperties.STEEL.build(), ElectricMachineKind.BLAST_FURNACE)
    );

    public static final BLibHolder<Block> ELECTRIC_INDUSTRIAL_FURNACE = create(
        "electric_industrial_furnace",
        () -> new ElectricMachineBlock(HumanBlockProperties.STEEL.build(), ElectricMachineKind.INDUSTRIAL_FURNACE)
    );

    /** [stated] Sep 29: ores to two dusts, the sediment chain, coal to carbon. */
    public static final BLibHolder<Block> ELECTRIC_GRINDER = create(
        "electric_grinder",
        () -> new ElectricMachineBlock(HumanBlockProperties.STEEL.build(), ElectricMachineKind.GRINDER)
    );

    /** [stated] Sep 29: ores to two dusts, the sediment chain, coal to carbon. */
    public static final BLibHolder<Block> ELECTRIC_PRESS = create(
        "electric_press",
        () -> new ElectricMachineBlock(HumanBlockProperties.STEEL.build(), ElectricMachineKind.PRESS)
    );

    /** [stated] Sep 29: ores to two dusts, the sediment chain, coal to carbon. */
    public static final BLibHolder<Block> ELECTRIC_HYDRATOR = create(
        "electric_hydrator",
        () -> new ElectricMachineBlock(HumanBlockProperties.STEEL.build(), ElectricMachineKind.HYDRATOR)
    );

    /** [stated] Sep 29: ores to two dusts, the sediment chain, coal to carbon. */
    public static final BLibHolder<Block> ELECTRIC_FREEZER = create(
        "electric_freezer",
        () -> new ElectricMachineBlock(HumanBlockProperties.STEEL.build(), ElectricMachineKind.FREEZER)
    );

    /**
     * A universal canister placed in the world (shift + left-click). No block item: the canister item IS its item. ⚠
     * Deliberately no requiresCorrectToolForDrops - see {@link com.human.common.gameplay.block.CanisterBlock}.
     */
    public static final BLibHolder<Block> CANISTER_BLOCK = create(
        "canister_block",
        () -> new com.human.common.gameplay.block.CanisterBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .mapColor(net.minecraft.world.level.material.MapColor.COLOR_YELLOW)
                .strength(1.0F)
                .sound(net.minecraft.world.level.block.SoundType.METAL)
                .noOcclusion()
        )
    );

    // Multiblock fluid tanks - one block per edition, identical stats, merge only with their own edition.
    // ⚠ PushReaction.BLOCK: a piston moving one block would tear a tank's controller away from its members.
    // noOcclusion: the windows are see-through, so neighbours must not cull the faces behind them.
    public static final BLibHolder<Block> STEEL_FLUID_TANK = create(
        "steel_fluid_tank",
        () -> new com.human.common.gameplay.block.tank.FluidTankBlock(
            com.human.common.gameplay.block.tank.FluidTankEdition.STEEL,
            HumanBlockProperties.STEEL.build()
                .noOcclusion()
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
        )
    );

    public static final BLibHolder<Block> FERROALUMINUM_FLUID_TANK = create(
        "ferroaluminum_fluid_tank",
        () -> new com.human.common.gameplay.block.tank.FluidTankBlock(
            com.human.common.gameplay.block.tank.FluidTankEdition.FERROALUMINUM,
            HumanBlockProperties.STEEL.build()
                .noOcclusion()
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
        )
    );

    public static final BLibHolder<Block> ALUMINUM_FLUID_TANK = create(
        "aluminum_fluid_tank",
        () -> new com.human.common.gameplay.block.tank.FluidTankBlock(
            com.human.common.gameplay.block.tank.FluidTankEdition.ALUMINUM,
            HumanBlockProperties.STEEL.build()
                .noOcclusion()
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
        )
    );

    public static final BLibHolder<Block> TITANIUM_FLUID_TANK = create(
        "titanium_fluid_tank",
        () -> new com.human.common.gameplay.block.tank.FluidTankBlock(
            com.human.common.gameplay.block.tank.FluidTankEdition.TITANIUM,
            HumanBlockProperties.STEEL.build()
                .noOcclusion()
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
        )
    );

    public static final BLibHolder<Block> WY_FLUID_TANK = create(
        "wy_fluid_tank",
        () -> new com.human.common.gameplay.block.tank.WyFluidTankBlock(
            HumanBlockProperties.STEEL.build()
                .noOcclusion()
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
        )
    );

    public static final BLibHolder<Block> USCM_FLUID_TANK = create(
        "uscm_fluid_tank",
        () -> new com.human.common.gameplay.block.tank.FluidTankBlock(
            com.human.common.gameplay.block.tank.FluidTankEdition.USCM,
            HumanBlockProperties.STEEL.build()
                .noOcclusion()
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
        )
    );

    /** [stated] Sep 29/30: the unpowered electronics workstation - see {@link ElectronicsWorkstationBlock}. */
    /** [stated] Sep 29/30: the fuse panel - the wireless hub. See FusePanelBlockEntity. */
    /** [stated] Sep 29/30: the fluid pipe - see FluidPipeBlock. Copper, like his texture. */
    public static final BLibHolder<Block> FLUID_PIPE = create(
        "fluid_pipe",
        () -> new com.human.common.gameplay.block.fluid.FluidPipeBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COPPER_BLOCK)
                .noOcclusion()
        )
    );

    /** [stated] Sep 30: the item pipe - see ItemPipeBlock. Iron, like his grey texture. */
    public static final BLibHolder<Block> ITEM_PIPE = create(
        "item_pipe",
        () -> new com.human.common.gameplay.block.item.ItemPipeBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_BLOCK)
                .noOcclusion()
        )
    );

    public static final BLibHolder<Block> FUSE_PANEL = create(
        "fuse_panel",
        () -> new com.human.common.gameplay.block.power.FusePanelBlock(HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> ELECTRONICS_WORKSTATION = create(
        "electronics_workstation",
        () -> new ElectronicsWorkstationBlock(HumanBlockProperties.STEEL.build())
    );

    public static final BLibHolder<Block> HYDROELECTRIC_GENERATOR = create(
        "hydroelectric_generator",
        () -> new HydroelectricGeneratorBlock(HumanBlockProperties.STEEL.build().noOcclusion())
    );

    public static final BLibHolder<Block> SOLAR_PANEL = create(
        "solar_panel",
        // TODO: Use custom properties here.
        () -> new SolarPanelBlock(HumanBlockProperties.STEEL.build().noOcclusion())
    );

    public static final BLibHolder<Block> THERMOELECTRIC_GENERATOR = create(
        "thermoelectric_generator",
        () -> new ThermoelectricGeneratorBlock(HumanBlockProperties.STEEL.build().noOcclusion())
    );

    public static final BLibHolder<Block> AEROKINETIC_GENERATOR = create(
        "aerokinetic_generator",
        () -> new AerokineticGeneratorBlock(HumanBlockProperties.STEEL.build().noOcclusion())
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
