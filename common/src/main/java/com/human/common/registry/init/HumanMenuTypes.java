package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.machine.ElectricMachineKind;
import com.human.common.gameplay.menu.IndustrialFurnaceMenu;
import com.human.common.gameplay.menu.armor_case.ArmorCaseMenu;
import com.human.common.gameplay.menu.machine.ElectricMachineMenu;
import com.human.common.gameplay.menu.marine.MarineInventoryMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class HumanMenuTypes {

    private static final BLibRegistry<MenuType<?>> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.MENU);

    public static final BLibHolder<MenuType<ArmorCaseMenu>> ARMOR_CASE = register("armor_case", ArmorCaseMenu::new);

    public static final BLibHolder<MenuType<MarineInventoryMenu>> MARINE_INVENTORY = register(
        "marine_inventory",
        MarineInventoryMenu::new
    );

    public static final BLibHolder<MenuType<IndustrialFurnaceMenu>> INDUSTRIAL_FURNACE_MENU = register(
        "industrial_furnace_menu",
        IndustrialFurnaceMenu::new
    );

    public static final BLibHolder<MenuType<com.human.common.gameplay.power.menu.BatteryMenu>> BATTERY = register(
        "battery",
        com.human.common.gameplay.power.menu.BatteryMenu::new
    );

    /** [stated] Sep 30: the sentry turret's power screen - battery slot, or its tank while a fuse panel feeds it. */
    public static final BLibHolder<MenuType<com.human.common.gameplay.menu.sentry.SentryTurretMenu>> SENTRY_TURRET = register(
        "sentry_turret",
        com.human.common.gameplay.menu.sentry.SentryTurretMenu::new
    );

    /** [stated] Oct 2: the desk terminal - the turrets on the network, and their cameras. */
    public static final BLibHolder<MenuType<com.human.common.gameplay.menu.terminal.DeskTerminalMenu>> DESK_TERMINAL = register(
        "desk_terminal",
        com.human.common.gameplay.menu.terminal.DeskTerminalMenu::new
    );

    /** [stated] Sep 30: a cable's port screen - how each face treats another mod's energy block. */
    public static final BLibHolder<MenuType<com.human.common.gameplay.power.external.CablePortMenu>> CABLE_PORTS = register(
        "cable_ports",
        com.human.common.gameplay.power.external.CablePortMenu::new
    );

    /** [stated] Sep 30: one pipe end's screen - mode, filter and search. */
    public static final BLibHolder<MenuType<com.human.common.gameplay.block.pipe.PipeEndMenu>> PIPE_END = register(
        "pipe_end",
        com.human.common.gameplay.block.pipe.PipeEndMenu::new
    );

    public static final BLibHolder<MenuType<com.human.common.gameplay.menu.cryo.CryotubeControllerMenu>> CRYOTUBE_CONTROLLER =
        register("cryotube_controller", com.human.common.gameplay.menu.cryo.CryotubeControllerMenu::new);

    public static final BLibHolder<MenuType<com.human.common.gameplay.power.fuse.FusePanelMenu>> FUSE_PANEL = register(
        "fuse_panel",
        com.human.common.gameplay.power.fuse.FusePanelMenu::new
    );

    public static final BLibHolder<MenuType<com.human.common.gameplay.menu.workstation.ElectronicsWorkstationMenu>> ELECTRONICS_WORKSTATION =
        register(
            "electronics_workstation",
            com.human.common.gameplay.menu.workstation.ElectronicsWorkstationMenu::new
        );

    // One menu type per machine, all the same menu class: the type is how the client knows which art to draw.
    public static final BLibHolder<MenuType<ElectricMachineMenu>> ELECTRIC_FURNACE = register(
        "electric_furnace",
        (id, inventory) -> new ElectricMachineMenu(ElectricMachineKind.FURNACE, id, inventory)
    );

    public static final BLibHolder<MenuType<ElectricMachineMenu>> ELECTRIC_SMOKER = register(
        "electric_smoker",
        (id, inventory) -> new ElectricMachineMenu(ElectricMachineKind.SMOKER, id, inventory)
    );

    public static final BLibHolder<MenuType<ElectricMachineMenu>> ELECTRIC_BLAST_FURNACE = register(
        "electric_blast_furnace",
        (id, inventory) -> new ElectricMachineMenu(ElectricMachineKind.BLAST_FURNACE, id, inventory)
    );

    public static final BLibHolder<MenuType<ElectricMachineMenu>> ELECTRIC_INDUSTRIAL_FURNACE = register(
        "electric_industrial_furnace",
        (id, inventory) -> new ElectricMachineMenu(ElectricMachineKind.INDUSTRIAL_FURNACE, id, inventory)
    );

    public static final BLibHolder<MenuType<ElectricMachineMenu>> ELECTRIC_GRINDER = register(
        "electric_grinder",
        (id, inventory) -> new ElectricMachineMenu(ElectricMachineKind.GRINDER, id, inventory)
    );

    public static final BLibHolder<MenuType<ElectricMachineMenu>> ELECTRIC_PRESS = register(
        "electric_press",
        (id, inventory) -> new ElectricMachineMenu(ElectricMachineKind.PRESS, id, inventory)
    );

    public static final BLibHolder<MenuType<ElectricMachineMenu>> ELECTRIC_HYDRATOR = register(
        "electric_hydrator",
        (id, inventory) -> new ElectricMachineMenu(ElectricMachineKind.HYDRATOR, id, inventory)
    );

    public static final BLibHolder<MenuType<ElectricMachineMenu>> ELECTRIC_FREEZER = register(
        "electric_freezer",
        (id, inventory) -> new ElectricMachineMenu(ElectricMachineKind.FREEZER, id, inventory)
    );

    public static <T extends AbstractContainerMenu> BLibHolder<MenuType<T>> register(String id, MenuType.MenuSupplier<T> supplier) {
        return REGISTRY.createHolder(id, () -> new MenuType<>(supplier, FeatureFlagSet.of()));
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
