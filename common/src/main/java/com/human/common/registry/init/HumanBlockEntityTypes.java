package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block.entity.AmmoChestBlockEntity;
import com.human.common.gameplay.block.entity.IndustrialFurnaceBlockEntity;
import com.human.common.gameplay.block.entity.LatexTapBlockEntity;
import com.human.common.gameplay.block.entity.LeadChestBlockEntity;
import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.block.entity.power.impl.AerokineticGeneratorBlockEntity;
import com.human.common.gameplay.block.entity.power.impl.BatteryBlockEntity;
import com.human.common.gameplay.block.entity.power.impl.ChargingHubBlockEntity;
import com.human.common.gameplay.block.entity.power.impl.DeskTerminalBlockEntity;
import com.human.common.gameplay.block.entity.power.impl.HydroelectricGeneratorBlockEntity;
import com.human.common.gameplay.block.entity.power.impl.InfinitePowerGeneratorBlockEntity;
import com.human.common.gameplay.block.entity.power.impl.ResonatorBlockEntity;
import com.human.common.gameplay.block.entity.power.impl.SolarPanelBlockEntity;
import com.human.common.gameplay.block.entity.power.impl.ThermoelectricGeneratorBlockEntity;
import com.human.common.gameplay.machine.ElectricMachineKind;
import com.human.common.registry.init.block.CoreBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class HumanBlockEntityTypes {

    private static final BLibRegistry<BlockEntityType<?>> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    public static final BLibHolder<BlockEntityType<IndustrialFurnaceBlockEntity>> INDUSTRIAL_FURNACE = create(
        "industrial_furnace",
        () -> BlockEntityType.Builder.of(IndustrialFurnaceBlockEntity::new, HumanBlocks.INDUSTRIAL_FURNACE.get())
    );

    public static final BLibHolder<BlockEntityType<InfinitePowerGeneratorBlockEntity>> INFINITE_POWER_GENERATOR = create(
        "infinite_power_generator",
        () -> BlockEntityType.Builder.of(InfinitePowerGeneratorBlockEntity::new, HumanBlocks.INFINITE_POWER_GENERATOR.get())
    );

    /** [stated] Oct 3: the redstone generator - 16 FE a tick, forever. */
    public static final BLibHolder<BlockEntityType<com.human.common.gameplay.block.entity.power.impl.RedstoneGeneratorBlockEntity>> REDSTONE_GENERATOR =
        create(
            "redstone_generator",
            () -> BlockEntityType.Builder.of(
                com.human.common.gameplay.block.entity.power.impl.RedstoneGeneratorBlockEntity::new,
                HumanBlocks.REDSTONE_GENERATOR.get()
            )
        );

    public static final BLibHolder<BlockEntityType<LeadChestBlockEntity>> LEAD_CHEST = create(
        "lead_chest",
        () -> BlockEntityType.Builder.of(LeadChestBlockEntity::new, HumanBlocks.LEAD_CHEST.get())
    );

    public static final BLibHolder<BlockEntityType<AmmoChestBlockEntity>> AMMO_CHEST = create(
        "ammo_chest",
        () -> BlockEntityType.Builder.of(AmmoChestBlockEntity::new, HumanBlocks.AMMO_CHEST.get())
    );

    public static final BLibHolder<BlockEntityType<BatteryBlockEntity>> BATTERY = create(
        "battery",
        () -> BlockEntityType.Builder.of(BatteryBlockEntity::new, HumanBlocks.BATTERY.get())
    );

    public static final BLibHolder<BlockEntityType<com.human.common.gameplay.block.entity.CanisterBlockEntity>> CANISTER = create(
        "canister",
        () -> BlockEntityType.Builder.of(com.human.common.gameplay.block.entity.CanisterBlockEntity::new, HumanBlocks.CANISTER_BLOCK.get())
    );

    /** One type for all six tank editions: the block entity is the same, only the block (and its look) differs. */
    public static final BLibHolder<BlockEntityType<com.human.common.gameplay.block.tank.FluidTankBlockEntity>> FLUID_TANK = create(
        "fluid_tank",
        () -> BlockEntityType.Builder.of(
            com.human.common.gameplay.block.tank.FluidTankBlockEntity::new,
            HumanBlocks.STEEL_FLUID_TANK.get(),
            HumanBlocks.FERROALUMINUM_FLUID_TANK.get(),
            HumanBlocks.ALUMINUM_FLUID_TANK.get(),
            HumanBlocks.TITANIUM_FLUID_TANK.get(),
            HumanBlocks.WY_FLUID_TANK.get(),
            HumanBlocks.USCM_FLUID_TANK.get()
        )
    );

    public static final BLibHolder<BlockEntityType<com.human.common.gameplay.block.cryo.CryotankBlockEntity>> CRYOTANK = create(
        "cryotank",
        () -> BlockEntityType.Builder.of(com.human.common.gameplay.block.cryo.CryotankBlockEntity::new, HumanBlocks.CRYOTANK.get())
    );

    public static final BLibHolder<BlockEntityType<com.human.common.gameplay.block.cryo.CryotubeControllerBlockEntity>> CRYOTUBE_CONTROLLER =
        create(
            "cryotube_controller",
            () -> BlockEntityType.Builder.of(
                com.human.common.gameplay.block.cryo.CryotubeControllerBlockEntity::new,
                HumanBlocks.CRYOTUBE_CONTROLLER.get(),
                HumanBlocks.STEEL_CRYOTUBE_CONTROLLER.get(),
                HumanBlocks.WY_CRYOTUBE_CONTROLLER.get(),
                HumanBlocks.USCM_CRYOTUBE_CONTROLLER.get()
            )
        );

    public static final BLibHolder<BlockEntityType<ChargingHubBlockEntity>> CHARGING_HUB = create(
        "charging_hub",
        () -> BlockEntityType.Builder.of(ChargingHubBlockEntity::new, HumanBlocks.CHARGING_HUB.get())
    );

    public static final BLibHolder<BlockEntityType<ElectricMachineBlockEntity>> ELECTRIC_FURNACE = createMachine(
        ElectricMachineKind.FURNACE,
        HumanBlocks.ELECTRIC_FURNACE
    );

    public static final BLibHolder<BlockEntityType<ElectricMachineBlockEntity>> ELECTRIC_SMOKER = createMachine(
        ElectricMachineKind.SMOKER,
        HumanBlocks.ELECTRIC_SMOKER
    );

    public static final BLibHolder<BlockEntityType<ElectricMachineBlockEntity>> ELECTRIC_BLAST_FURNACE = createMachine(
        ElectricMachineKind.BLAST_FURNACE,
        HumanBlocks.ELECTRIC_BLAST_FURNACE
    );

    public static final BLibHolder<BlockEntityType<ElectricMachineBlockEntity>> ELECTRIC_INDUSTRIAL_FURNACE = createMachine(
        ElectricMachineKind.INDUSTRIAL_FURNACE,
        HumanBlocks.ELECTRIC_INDUSTRIAL_FURNACE
    );

    public static final BLibHolder<BlockEntityType<ElectricMachineBlockEntity>> ELECTRIC_GRINDER = createMachine(
        ElectricMachineKind.GRINDER,
        HumanBlocks.ELECTRIC_GRINDER
    );

    /** [stated] Sep 30: a cable that meets a block - its flange colours. */
    public static final BLibHolder<BlockEntityType<com.human.common.gameplay.block.entity.power.CableBlockEntity>> CABLE = create(
        "cable",
        () -> BlockEntityType.Builder.of(com.human.common.gameplay.block.entity.power.CableBlockEntity::new, HumanBlocks.CABLE.get())
    );

    /** An item pipe touching a port - its flange colours and end settings, and how its network finds it on load. */
    public static final BLibHolder<BlockEntityType<com.human.common.gameplay.block.item.ItemPipeBlockEntity>> ITEM_PIPE = create(
        "item_pipe",
        () -> BlockEntityType.Builder.of(com.human.common.gameplay.block.item.ItemPipeBlockEntity::new, HumanBlocks.ITEM_PIPE.get())
    );

    /** A pipe touching a port - its flange colours, and how the pipe network finds it on load. */
    public static final BLibHolder<BlockEntityType<com.human.common.gameplay.block.fluid.FluidPipeBlockEntity>> FLUID_PIPE = create(
        "fluid_pipe",
        () -> BlockEntityType.Builder.of(com.human.common.gameplay.block.fluid.FluidPipeBlockEntity::new, HumanBlocks.FLUID_PIPE.get())
    );

    public static final BLibHolder<BlockEntityType<com.human.common.gameplay.power.fuse.FusePanelBlockEntity>> FUSE_PANEL = create(
        "fuse_panel",
        () -> BlockEntityType.Builder.of(com.human.common.gameplay.power.fuse.FusePanelBlockEntity::new, HumanBlocks.FUSE_PANEL.get())
    );

    /** [stated] Sep 29/30: the unpowered electronics workstation's storage. */
    public static final BLibHolder<BlockEntityType<com.human.common.gameplay.block.entity.workstation.ElectronicsWorkstationBlockEntity>> ELECTRONICS_WORKSTATION =
        create(
            "electronics_workstation",
            () -> BlockEntityType.Builder.of(
                com.human.common.gameplay.block.entity.workstation.ElectronicsWorkstationBlockEntity::new,
                HumanBlocks.ELECTRONICS_WORKSTATION.get()
            )
        );

    public static final BLibHolder<BlockEntityType<ElectricMachineBlockEntity>> ELECTRIC_PRESS = createMachine(
        ElectricMachineKind.PRESS,
        HumanBlocks.ELECTRIC_PRESS
    );

    public static final BLibHolder<BlockEntityType<ElectricMachineBlockEntity>> ELECTRIC_HYDRATOR = createMachine(
        ElectricMachineKind.HYDRATOR,
        HumanBlocks.ELECTRIC_HYDRATOR
    );

    public static final BLibHolder<BlockEntityType<ElectricMachineBlockEntity>> ELECTRIC_FREEZER = createMachine(
        ElectricMachineKind.FREEZER,
        HumanBlocks.ELECTRIC_FREEZER
    );

    public static final BLibHolder<BlockEntityType<DeskTerminalBlockEntity>> DESK_TERMINAL = create(
        "desk_terminal",
        () -> BlockEntityType.Builder.of(DeskTerminalBlockEntity::new, HumanBlocks.DESK_TERMINAL_BLOCK.get())
    );

    public static final BLibHolder<BlockEntityType<ResonatorBlockEntity>> RESONATOR = create(
        "resonator",
        () -> BlockEntityType.Builder.of(ResonatorBlockEntity::new, HumanBlocks.RESONATOR_BLOCK.get())
    );

    public static final BLibHolder<BlockEntityType<LatexTapBlockEntity>> LATEX_TAP = create(
        "latex_tap",
        () -> BlockEntityType.Builder.of(LatexTapBlockEntity::new, CoreBlocks.LATEX_TAP.get())
    );

    public static final BLibHolder<BlockEntityType<HydroelectricGeneratorBlockEntity>> HYDROELECTRIC_GENERATOR = create(
        "hydroelectric_generator",
        () -> BlockEntityType.Builder.of(
            HydroelectricGeneratorBlockEntity::new,
            HumanBlocks.HYDROELECTRIC_GENERATOR.get()
        )
    );

    public static final BLibHolder<BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL = create(
        "solar_panel",
        () -> BlockEntityType.Builder.of(SolarPanelBlockEntity::new, HumanBlocks.SOLAR_PANEL.get())
    );

    public static final BLibHolder<BlockEntityType<ThermoelectricGeneratorBlockEntity>> THERMOELECTRIC_GENERATOR = create(
        "thermoelectric_generator",
        () -> BlockEntityType.Builder.of(
            ThermoelectricGeneratorBlockEntity::new,
            HumanBlocks.THERMOELECTRIC_GENERATOR.get()
        )
    );

    public static final BLibHolder<BlockEntityType<AerokineticGeneratorBlockEntity>> AEROKINETIC_GENERATOR = create(
        "aerokinetic_generator",
        () -> BlockEntityType.Builder.of(AerokineticGeneratorBlockEntity::new, HumanBlocks.AEROKINETIC_GENERATOR.get())
    );

    private static <T extends BlockEntity> BLibHolder<BlockEntityType<T>> create(
        String path,
        Supplier<BlockEntityType.Builder<T>> builder
    ) {
        return REGISTRY.createHolder(path, () -> builder.get().build(null));
    }

    /** One block entity type per machine: the id matches the block, so each kind saves and loads as itself. */
    private static BLibHolder<BlockEntityType<ElectricMachineBlockEntity>> createMachine(
        ElectricMachineKind kind,
        Supplier<? extends net.minecraft.world.level.block.Block> block
    ) {
        return create(
            kind.id(),
            () -> BlockEntityType.Builder.of((pos, state) -> new ElectricMachineBlockEntity(kind, pos, state), block.get())
        );
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
