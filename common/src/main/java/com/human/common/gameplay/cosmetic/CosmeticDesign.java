package com.human.common.gameplay.cosmetic;

/**
 * One cosmetic design from the concept sheets. GENERATED from the design master table (master.py) together with the
 * models, so a design's cells, set, mount and glow always match its assets - edit the table, not this file.
 * <h2>CELLS</h2> The blocks it covers as {cx, cy, cz} from its CORE, in model space: +x is the facing's CLOCKWISE side,
 * +y up, +z BACK (away from the front). The core is always first. 3-wide pieces are anchored on their middle block.
 * <h2>BAKED or ANIMATED</h2> [stated] Oct 31: anything with a screen or LEDs has a glow layer, so it draws through an
 * AzureLib model with a block entity. Everything else is a plain baked block model - meshed into the chunk, no
 * per-frame cost.
 * <h2>MOUNT</h2> FLOOR pieces stand on the block below. WALL pieces are modelled flush against the wall behind them and
 * are placed by clicking a wall. CEILING pieces hang from the block above.
 */
public enum CosmeticDesign {

    // ---- LABORATORY
    LAB_BENCH("lab_bench", CosmeticSet.LABORATORY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    LAB_CHAIR("lab_chair", CosmeticSet.LABORATORY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    STORAGE_CABINET("storage_cabinet", CosmeticSet.LABORATORY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    LAB_TERMINAL("lab_terminal", CosmeticSet.LABORATORY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    EQUIPMENT_CART("equipment_cart", CosmeticSet.LABORATORY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    MICROSCOPE("microscope", CosmeticSet.LABORATORY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    CENTRIFUGE("centrifuge", CosmeticSet.LABORATORY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SPECIMEN_JAR("specimen_jar", CosmeticSet.LABORATORY, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SAMPLE_ANALYZER("sample_analyzer", CosmeticSet.LABORATORY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SAMPLE_RACK("sample_rack", CosmeticSet.LABORATORY, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    EXAM_BED("exam_bed", CosmeticSet.LABORATORY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    SURGICAL_LIGHT("surgical_light", CosmeticSet.LABORATORY, true, false, false, Mount.CEILING, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    PRIVACY_SCREEN("privacy_screen", CosmeticSet.LABORATORY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 1, 0, 0 } }),
    WALL_CABINET("wall_cabinet", CosmeticSet.LABORATORY, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    MEDICAL_STATION("medical_station", CosmeticSet.LABORATORY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    CRYO_SUPPORT_UNIT("cryo_support_unit", CosmeticSet.LABORATORY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),

    // ---- HABITAT
    BUNK_BED("bunk_bed", CosmeticSet.HABITAT, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    DINING_SET("dining_set", CosmeticSet.HABITAT, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    HABITAT_LOCKER("habitat_locker", CosmeticSet.HABITAT, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    SOFA("sofa", CosmeticSet.HABITAT, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    REFRIGERATOR("refrigerator", CosmeticSet.HABITAT, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    CONTROL_DESK("control_desk", CosmeticSet.HABITAT, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 1, 0, 0 } }),
    OFFICE_CHAIR("office_chair", CosmeticSet.HABITAT, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SHELVING_UNIT("shelving_unit", CosmeticSet.HABITAT, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    VENDING_MACHINE("vending_machine", CosmeticSet.HABITAT, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    SMALL_TABLE("small_table", CosmeticSet.HABITAT, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    LONG_TABLE("long_table", CosmeticSet.HABITAT, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    LARGE_TABLE("large_table", CosmeticSet.HABITAT, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 } }),
    COUNTER_UNIT("counter_unit", CosmeticSet.HABITAT, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    PLAIN_DESK("plain_desk", CosmeticSet.HABITAT, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    BARE_LAB_BENCH("bare_lab_bench", CosmeticSet.HABITAT, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    SIDE_TABLE("side_table", CosmeticSet.HABITAT, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),

    // ---- UTILITY
    JUNCTION_BOX("junction_box", CosmeticSet.UTILITY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    PIPE_SECTION("pipe_section", CosmeticSet.UTILITY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    VENT_PANEL("vent_panel", CosmeticSet.UTILITY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    CABLE_TRAY("cable_tray", CosmeticSet.UTILITY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    DATA_PANEL("data_panel", CosmeticSet.UTILITY, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    CONDUIT_SECTION("conduit_section", CosmeticSet.UTILITY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    CEILING_RAIL("ceiling_rail", CosmeticSet.UTILITY, false, false, false, Mount.CEILING, new int[][] { { 0, 0, 0 } }),
    ARMORED_PANEL("armored_panel", CosmeticSet.UTILITY, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    VENT_FAN("vent_fan", CosmeticSet.UTILITY, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    SUPPORT_BRACKET("support_bracket", CosmeticSet.UTILITY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    CABLE_PANEL("cable_panel", CosmeticSet.UTILITY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    SPLIT_PANEL("split_panel", CosmeticSet.UTILITY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    PIPE_CLUSTER("pipe_cluster", CosmeticSet.UTILITY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    CABLE_BUNDLE("cable_bundle", CosmeticSet.UTILITY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    BEAM_SECTION("beam_section", CosmeticSet.UTILITY, false, false, false, Mount.CEILING, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    HANGING_CONDUITS("hanging_conduits", CosmeticSet.UTILITY, false, false, false, Mount.CEILING, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    JUNCTION_HUB("junction_hub", CosmeticSet.UTILITY, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    FILTER_BOX("filter_box", CosmeticSet.UTILITY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    ACCESS_PANEL("access_panel", CosmeticSet.UTILITY, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    STOWAGE_RACK("stowage_rack", CosmeticSet.UTILITY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    TOOL_MOUNT("tool_mount", CosmeticSet.UTILITY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    TRASH_BIN("trash_bin", CosmeticSet.UTILITY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    RECYCLING_BIN("recycling_bin", CosmeticSet.UTILITY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    BIOHAZARD_BIN("biohazard_bin", CosmeticSet.UTILITY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    JANITOR_CART("janitor_cart", CosmeticSet.UTILITY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    MAINTENANCE_KIT("maintenance_kit", CosmeticSet.UTILITY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    UTILITY_CART("utility_cart", CosmeticSet.UTILITY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    WORK_LIGHT("work_light", CosmeticSet.UTILITY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),

    // ---- KITCHEN
    STOVE("stove", CosmeticSet.KITCHEN, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SINK_UNIT("sink_unit", CosmeticSet.KITCHEN, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    TOILET("toilet", CosmeticSet.KITCHEN, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SHOWER("shower", CosmeticSet.KITCHEN, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    FOOD_DISPENSER("food_dispenser", CosmeticSet.KITCHEN, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    COFFEE_MACHINE("coffee_machine", CosmeticSet.KITCHEN, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    WATER_COOLER("water_cooler", CosmeticSet.KITCHEN, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    MICROWAVE("microwave", CosmeticSet.KITCHEN, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    WATER_DISPENSER("water_dispenser", CosmeticSet.KITCHEN, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    HEATER_UNIT("heater_unit", CosmeticSet.KITCHEN, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    RATION_WARMER("ration_warmer", CosmeticSet.KITCHEN, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),

    // ---- SECURITY
    CARGO_CRATE("cargo_crate", CosmeticSet.SECURITY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    PALLET("pallet", CosmeticSet.SECURITY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    MAINTENANCE_WORKBENCH("maintenance_workbench", CosmeticSet.SECURITY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    TOOL_CHEST("tool_chest", CosmeticSet.SECURITY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SECURITY_DESK("security_desk", CosmeticSet.SECURITY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    CHECKPOINT_SCANNER("checkpoint_scanner", CosmeticSet.SECURITY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    TOOL_CART("tool_cart", CosmeticSet.SECURITY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    AMMO_RACK("ammo_rack", CosmeticSet.SECURITY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    ARMORY_LOCKER("armory_locker", CosmeticSet.SECURITY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    SECURITY_CHECKPOINT("security_checkpoint", CosmeticSet.SECURITY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SECURITY_GATE("security_gate", CosmeticSet.SECURITY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    TURNSTILE("turnstile", CosmeticSet.SECURITY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    FIELD_SUPPLY_CASE("field_supply_case", CosmeticSet.SECURITY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 0, 1 } }),

    // ---- RECREATION
    HYDRO_TRAY("hydro_tray", CosmeticSet.RECREATION, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    GROW_RACK("grow_rack", CosmeticSet.RECREATION, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    ARCADE_MACHINE("arcade_machine", CosmeticSet.RECREATION, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    EXERCISE_BENCH("exercise_bench", CosmeticSet.RECREATION, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    HYDROPONIC_UNIT("hydroponic_unit", CosmeticSet.RECREATION, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    PLANT_CABINET("plant_cabinet", CosmeticSet.RECREATION, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    ENTERTAINMENT_CONSOLE("entertainment_console", CosmeticSet.RECREATION, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    ARMOR_STAND("armor_stand", CosmeticSet.RECREATION, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    BOTANICAL_TABLE("botanical_table", CosmeticSet.RECREATION, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    RECREATION_TABLE("recreation_table", CosmeticSet.RECREATION, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),

    // ---- PROPS
    DESKTOP_TERMINAL("desktop_terminal", CosmeticSet.PROPS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    KEYBOARD("keyboard", CosmeticSet.PROPS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    PAPERWORK_STACK("paperwork_stack", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    CLIPBOARD("clipboard", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    BOOK_STACK("book_stack", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    COFFEE_MUG("coffee_mug", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    FOOD_TRAY("food_tray", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    TOOL_TRAY("tool_tray", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    BOTTLES("bottles", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    TEST_TUBES("test_tubes", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SAMPLE_CASE("sample_case", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    FIRST_AID_KIT("first_aid_kit", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    FIRE_EXTINGUISHER("fire_extinguisher", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SPARE_HARD_HAT("spare_hard_hat", CosmeticSet.PROPS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    BOOT_PAIR("boot_pair", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    DUFFEL_BAG("duffel_bag", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    TOOLBOX("toolbox", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    TAPE_ROLL("tape_roll", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    FACILITY_SIGN("facility_sign", CosmeticSet.PROPS, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    HANDHELD_DEVICE("handheld_device", CosmeticSet.PROPS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    COMMS_RADIO("comms_radio", CosmeticSet.PROPS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    FILE_STACK("file_stack", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    ALARM_SIREN("alarm_siren", CosmeticSet.PROPS, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    INTERCOM("intercom", CosmeticSet.PROPS, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    WALL_SPEAKER("wall_speaker", CosmeticSet.PROPS, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    SECURITY_CAMERA("security_camera", CosmeticSet.PROPS, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    WALL_CLOCK("wall_clock", CosmeticSet.PROPS, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    WARNING_SIGN("warning_sign", CosmeticSet.PROPS, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    MAP_PANEL("map_panel", CosmeticSet.PROPS, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    ELECTRICAL_BOX("electrical_box", CosmeticSet.PROPS, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    SWITCH_PANEL("switch_panel", CosmeticSet.PROPS, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    KEYPAD("keypad", CosmeticSet.PROPS, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    WALL_HANDSET("wall_handset", CosmeticSet.PROPS, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    SCREEN_PANEL("screen_panel", CosmeticSet.PROPS, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    SCREEN_BOARD("screen_board", CosmeticSet.PROPS, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    PIPE_MOUNT("pipe_mount", CosmeticSet.PROPS, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    CABLE_HOOK("cable_hook", CosmeticSet.PROPS, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    TOOL_RACK("tool_rack", CosmeticSet.PROPS, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    COAT_HOOK("coat_hook", CosmeticSet.PROPS, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    WALL_SHELF("wall_shelf", CosmeticSet.PROPS, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    PETRI_DISHES("petri_dishes", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    INSTRUMENT_TRAY("instrument_tray", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    DATA_PAD("data_pad", CosmeticSet.PROPS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    DESK_LAMP("desk_lamp", CosmeticSet.PROPS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    WALL_MONITOR("wall_monitor", CosmeticSet.PROPS, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    SAMPLE_CAROUSEL("sample_carousel", CosmeticSet.PROPS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    PORTABLE_LIGHT("portable_light", CosmeticSet.PROPS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),

    // ---- BOTANICAL
    DESKTOP_TERRARIUM("desktop_terrarium", CosmeticSet.BOTANICAL, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    TALL_GROWTH_CHAMBER("tall_growth_chamber", CosmeticSet.BOTANICAL, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    WIDE_GROW_RACK("wide_grow_rack", CosmeticSet.BOTANICAL, true, false, true, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    HYDROPONIC_CABINET("hydroponic_cabinet", CosmeticSet.BOTANICAL, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    CLIMATE_GROW_CHAMBER("climate_grow_chamber", CosmeticSet.BOTANICAL, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 }, { 0, 1, 0 }, { 1, 1, 0 }, { 0, 1, 1 }, { 1, 1, 1 } }),
    AUTOMATED_PLANTER("automated_planter", CosmeticSet.BOTANICAL, true, false, true, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    HANGING_BOTANICAL_POD("hanging_botanical_pod", CosmeticSet.BOTANICAL, true, true, true, Mount.CEILING, new int[][] { { 0, 0, 0 } }),
    WALL_PLANT_CHAMBER("wall_plant_chamber", CosmeticSet.BOTANICAL, true, true, true, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    AQUATIC_TERRARIUM("aquatic_terrarium", CosmeticSet.BOTANICAL, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    FUNGAL_GROW_CHAMBER("fungal_grow_chamber", CosmeticSet.BOTANICAL, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    VERTICAL_BIO_DOME("vertical_bio_dome", CosmeticSet.BOTANICAL, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 }, { 0, 1, 0 }, { 1, 1, 0 }, { 0, 1, 1 }, { 1, 1, 1 }, { 0, 2, 0 }, { 1, 2, 0 }, { 0, 2, 1 }, { 1, 2, 1 } }),
    ROOT_OBSERVATION_TANK("root_observation_tank", CosmeticSet.BOTANICAL, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SEED_GERMINATION_INCUBATOR("seed_germination_incubator", CosmeticSet.BOTANICAL, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    BOTANICAL_EXAMINATION_BENCH("botanical_examination_bench", CosmeticSet.BOTANICAL, true, false, true, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    NUTRIENT_RESERVOIR("nutrient_reservoir", CosmeticSet.BOTANICAL, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),

    // ---- GENETICS
    GENETICS_WORKBENCH("genetics_workbench", CosmeticSet.GENETICS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    DNA_SEQUENCER("dna_sequencer", CosmeticSet.GENETICS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    SPLICING_WORKSTATION("splicing_workstation", CosmeticSet.GENETICS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    PCR_MACHINE("pcr_machine", CosmeticSet.GENETICS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    GEL_ELECTROPHORESIS_UNIT("gel_electrophoresis_unit", CosmeticSet.GENETICS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    TISSUE_CULTURE_STATION("tissue_culture_station", CosmeticSet.GENETICS, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    CRYOGENIC_SAMPLE_STORAGE("cryogenic_sample_storage", CosmeticSet.GENETICS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    CLONE_SAMPLE_POD("clone_sample_pod", CosmeticSet.GENETICS, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    MICROARRAY_STATION("microarray_station", CosmeticSet.GENETICS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    GENE_ANALYSIS_TERMINAL("gene_analysis_terminal", CosmeticSet.GENETICS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SPECIMEN_FREEZER("specimen_freezer", CosmeticSet.GENETICS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    REAGENT_MIXER("reagent_mixer", CosmeticSet.GENETICS, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),

    // ---- VIROLOGY
    BIOSAFETY_CABINET("biosafety_cabinet", CosmeticSet.VIROLOGY, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    BIOHAZARD_WORKBENCH("biohazard_workbench", CosmeticSet.VIROLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    VIRUS_INCUBATOR("virus_incubator", CosmeticSet.VIROLOGY, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    CONTAINMENT_GLOVEBOX("containment_glovebox", CosmeticSet.VIROLOGY, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    ISOLATION_CHAMBER("isolation_chamber", CosmeticSet.VIROLOGY, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 }, { 0, 1, 0 }, { 1, 1, 0 }, { 0, 1, 1 }, { 1, 1, 1 } }),
    DECONTAMINATION_SHOWER("decontamination_shower", CosmeticSet.VIROLOGY, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    AUTOCLAVE("autoclave", CosmeticSet.VIROLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    HAZMAT_STORAGE("hazmat_storage", CosmeticSet.VIROLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    FLUID_STORAGE_TANK("fluid_storage_tank", CosmeticSet.VIROLOGY, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SAMPLE_FREEZER("sample_freezer", CosmeticSet.VIROLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    PATHOGEN_ANALYZER("pathogen_analyzer", CosmeticSet.VIROLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    WASTE_DISPOSAL_UNIT("waste_disposal_unit", CosmeticSet.VIROLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    QUARANTINE_SPECIMEN_CASE("quarantine_specimen_case", CosmeticSet.VIROLOGY, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),

    // ---- GEOLOGY
    CRYSTAL_GROWTH_CHAMBER("crystal_growth_chamber", CosmeticSet.GEOLOGY, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    CRYSTAL_SAMPLE("crystal_sample", CosmeticSet.GEOLOGY, true, false, true, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    GEOLOGY_WORKBENCH("geology_workbench", CosmeticSet.GEOLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    ROCK_ANALYZER("rock_analyzer", CosmeticSet.GEOLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    CRYSTAL_DISPLAY_CASE("crystal_display_case", CosmeticSet.GEOLOGY, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    GEODE_CUTTER("geode_cutter", CosmeticSet.GEOLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    MINERAL_PRESS("mineral_press", CosmeticSet.GEOLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SPECTROMETER("spectrometer", CosmeticSet.GEOLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    THIN_SECTION_STATION("thin_section_station", CosmeticSet.GEOLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    ROCK_SAW("rock_saw", CosmeticSet.GEOLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    CORE_SAMPLE_RACK("core_sample_rack", CosmeticSet.GEOLOGY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    SEISMIC_MONITOR("seismic_monitor", CosmeticSet.GEOLOGY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    CRYSTAL_CLUSTER_SMALL("crystal_cluster_small", CosmeticSet.GEOLOGY, true, false, true, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    GEODE_FORMATION("geode_formation", CosmeticSet.GEOLOGY, true, false, true, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    MINERAL_PEDESTAL("mineral_pedestal", CosmeticSet.GEOLOGY, true, false, true, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    WALL_MINERAL_COLLECTION("wall_mineral_collection", CosmeticSet.GEOLOGY, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    FOSSIL_DISPLAY("fossil_display", CosmeticSet.GEOLOGY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),

    // ---- SPECIMENS
    SPECIMEN_TANK("specimen_tank", CosmeticSet.SPECIMENS, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    VERTICAL_SPECIMEN_CHAMBER("vertical_specimen_chamber", CosmeticSet.SPECIMENS, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    SKELETON_DISPLAY("skeleton_display", CosmeticSet.SPECIMENS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    ORGAN_DISPLAY_CASE("organ_display_case", CosmeticSet.SPECIMENS, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    XENOMORPH_SPECIMEN_TANK("xenomorph_specimen_tank", CosmeticSet.SPECIMENS, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 }, { 0, 1, 0 }, { 1, 1, 0 }, { 0, 1, 1 }, { 1, 1, 1 } }),
    ANATOMY_TABLE("anatomy_table", CosmeticSet.SPECIMENS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    PRESERVATION_POD("preservation_pod", CosmeticSet.SPECIMENS, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SPECIMEN_SHELF("specimen_shelf", CosmeticSet.SPECIMENS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    JAR_RACK("jar_rack", CosmeticSet.SPECIMENS, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    TISSUE_STORAGE("tissue_storage", CosmeticSet.SPECIMENS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SPECIMEN_LOCKER("specimen_locker", CosmeticSet.SPECIMENS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),

    // ---- SUPPLIES
    POTTED_PLANT_SET("potted_plant_set", CosmeticSet.SUPPLIES, true, false, true, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    HANGING_VINE_CLUSTER("hanging_vine_cluster", CosmeticSet.SUPPLIES, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    SEED_TRAY("seed_tray", CosmeticSet.SUPPLIES, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    PLANT_SAMPLE("plant_sample", CosmeticSet.SUPPLIES, true, true, true, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    FUNGUS_CULTURE_JAR("fungus_culture_jar", CosmeticSet.SUPPLIES, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    GROWTH_MEDIA_JAR("growth_media_jar", CosmeticSet.SUPPLIES, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SAMPLE_VIAL_RACK("sample_vial_rack", CosmeticSet.SUPPLIES, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    DNA_SAMPLE_CANISTER("dna_sample_canister", CosmeticSet.SUPPLIES, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    VIRUS_SAMPLE_CANISTER("virus_sample_canister", CosmeticSet.SUPPLIES, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    ROCK_SAMPLE("rock_sample", CosmeticSet.SUPPLIES, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    CORE_SAMPLE("core_sample", CosmeticSet.SUPPLIES, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    PIPETTE_RACK("pipette_rack", CosmeticSet.SUPPLIES, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SCALPEL_TRAY("scalpel_tray", CosmeticSet.SUPPLIES, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    SPECIMEN_NOTEBOOK("specimen_notebook", CosmeticSet.SUPPLIES, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    RESEARCH_WHITEBOARD("research_whiteboard", CosmeticSet.SUPPLIES, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    BIOHAZARD_SIGN("biohazard_sign", CosmeticSet.SUPPLIES, false, false, false, Mount.WALL, new int[][] { { 0, 0, 0 } }),
    GEOLOGICAL_FIELD_CASE("geological_field_case", CosmeticSet.SUPPLIES, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),

    // ---- ARMORY
    VERTICAL_WEAPON_RACK("vertical_weapon_rack", CosmeticSet.ARMORY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    DUAL_WEAPON_RACK("dual_weapon_rack", CosmeticSet.ARMORY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    WALL_WEAPON_RACK("wall_weapon_rack", CosmeticSet.ARMORY, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    MODULAR_WEAPON_RACK("modular_weapon_rack", CosmeticSet.ARMORY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    ARMORY_DISPLAY_CASE("armory_display_case", CosmeticSet.ARMORY, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    GEAR_CAGE("gear_cage", CosmeticSet.ARMORY, false, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 1, 0, 0 }, { -1, 1, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    AMMO_GEAR_SHELF("ammo_gear_shelf", CosmeticSet.ARMORY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    WEAPON_MAINTENANCE_BENCH("weapon_maintenance_bench", CosmeticSet.ARMORY, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 } }),

    // ---- OPERATIONS
    MAPPING_TABLE("mapping_table", CosmeticSet.OPERATIONS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 } }),
    OPS_CONSOLE("ops_console", CosmeticSet.OPERATIONS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    MULTI_MONITOR_STATION("multi_monitor_station", CosmeticSet.OPERATIONS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    TACTICAL_CONSOLE("tactical_console", CosmeticSet.OPERATIONS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    SECURITY_CONSOLE("security_console", CosmeticSet.OPERATIONS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    COMMS_CONSOLE("comms_console", CosmeticSet.OPERATIONS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    SURVEILLANCE_STATION("surveillance_station", CosmeticSet.OPERATIONS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    HOLO_PROJECTOR("holo_projector", CosmeticSet.OPERATIONS, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    COMMAND_TERMINAL("command_terminal", CosmeticSet.OPERATIONS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    BRIEFING_TABLE("briefing_table", CosmeticSet.OPERATIONS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 1, 0, 0 }, { -1, 0, 1 }, { 0, 0, 1 }, { 1, 0, 1 } }),
    WALL_DISPLAY_ARRAY("wall_display_array", CosmeticSet.OPERATIONS, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 1, 0, 0 }, { -1, 1, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    PLANNING_BOARD("planning_board", CosmeticSet.OPERATIONS, true, false, false, Mount.WALL, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    DATA_SERVER_RACK("data_server_rack", CosmeticSet.OPERATIONS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    COMMS_RACK("comms_rack", CosmeticSet.OPERATIONS, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),

    // ---- ENGINEERING
    LATHE("lathe", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    CNC_MILL("cnc_mill", CosmeticSet.ENGINEERING, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 }, { 0, 1, 0 }, { 1, 1, 0 }, { 0, 1, 1 }, { 1, 1, 1 } }),
    DRILL_PRESS("drill_press", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    WELDER("welder", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    PARTS_WASHER("parts_washer", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    HYDRAULIC_PRESS("hydraulic_press", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    ASSEMBLY_RIG("assembly_rig", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 1, 0, 0 }, { -1, 0, 1 }, { 0, 0, 1 }, { 1, 0, 1 }, { -1, 1, 0 }, { 0, 1, 0 }, { 1, 1, 0 }, { -1, 1, 1 }, { 0, 1, 1 }, { 1, 1, 1 } }),
    FRAME_WELDER("frame_welder", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 1, 0, 0 }, { -1, 0, 1 }, { 0, 0, 1 }, { 1, 0, 1 }, { -1, 1, 0 }, { 0, 1, 0 }, { 1, 1, 0 }, { -1, 1, 1 }, { 0, 1, 1 }, { 1, 1, 1 } }),
    PARTS_FABRICATOR("parts_fabricator", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    TABLE_SAW("table_saw", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    BAND_SAW("band_saw", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } }),
    COLD_SAW("cold_saw", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    BENCH_GRINDER("bench_grinder", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 } }),
    CYBERNETICS_BENCH("cybernetics_bench", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    PROSTHETIC_ASSEMBLY("prosthetic_assembly", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    NEURAL_INTERFACE("neural_interface", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 0, 1 } }),
    LIMB_FABRICATOR("limb_fabricator", CosmeticSet.ENGINEERING, true, true, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 }, { 0, 1, 0 }, { 1, 1, 0 }, { 0, 1, 1 }, { 1, 1, 1 } }),
    EXOSKELETON_RIG("exoskeleton_rig", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 }, { 0, 1, 0 }, { 1, 1, 0 }, { 0, 1, 1 }, { 1, 1, 1 }, { 0, 2, 0 }, { 1, 2, 0 }, { 0, 2, 1 }, { 1, 2, 1 } }),
    SYNTH_REPAIR_STATION("synth_repair_station", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    ROBOT_ASSEMBLY_BAY("robot_assembly_bay", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { -1, 0, 0 }, { 1, 0, 0 }, { -1, 0, 1 }, { 0, 0, 1 }, { 1, 0, 1 }, { -1, 1, 0 }, { 0, 1, 0 }, { 1, 1, 0 }, { -1, 1, 1 }, { 0, 1, 1 }, { 1, 1, 1 } }),
    DRONE_WORKSTATION("drone_workstation", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    SENTRY_TURRET_BUILD("sentry_turret_build", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 } }),
    UTILITY_DRONE_PAD("utility_drone_pad", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 }, { 1, 0, 1 } }),
    ROBOT_FRAME_RACK("robot_frame_rack", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }),
    CHARGING_STATION("charging_station", CosmeticSet.ENGINEERING, true, false, false, Mount.FLOOR, new int[][] { { 0, 0, 0 }, { 0, 1, 0 } });

    public enum Mount {
        FLOOR,
        WALL,
        CEILING
    }

    private final String id;

    private final CosmeticSet set;

    private final boolean animated;

    private final boolean translucent;

    private final boolean contents;

    private final Mount mount;

    private final int[][] cells;

    CosmeticDesign(String id, CosmeticSet set, boolean animated, boolean translucent, boolean contents, Mount mount, int[][] cells) {
        this.id = id;
        this.set = set;
        this.animated = animated;
        this.translucent = translucent;
        this.contents = contents;
        this.mount = mount;
        this.cells = cells;
    }

    /** True when its gGlass group renders see-through. */
    public boolean translucent() {
        return translucent;
    }

    /** True when it has a swappable interior ({@link CosmeticSpecimenBlock}). */
    public boolean hasContents() {
        return contents;
    }

    public String id() {
        return id;
    }

    public CosmeticSet set() {
        return set;
    }

    /** True for the AzureLib designs (block entity + glow layer); false for baked block models. */
    public boolean animated() {
        return animated;
    }

    public Mount mount() {
        return mount;
    }

    public int[][] cells() {
        return cells;
    }

    /** The registry name of this design in {@code style}. */
    public String blockName(CosmeticStyle style) {
        return style.prefix() + id;
    }

    /** Footprint for the screen: "W x D", plus " x H" when it is taller than one block. */
    public String footprint() {
        int minX = 0, maxX = 0, maxY = 0, minZ = 0, maxZ = 0;

        for (var cell : cells) {
            minX = Math.min(minX, cell[0]);
            maxX = Math.max(maxX, cell[0]);
            maxY = Math.max(maxY, cell[1]);
            minZ = Math.min(minZ, cell[2]);
            maxZ = Math.max(maxZ, cell[2]);
        }

        var text = (maxX - minX + 1) + "x" + (maxZ - minZ + 1);

        return maxY > 0 ? text + "x" + (maxY + 1) : text;
    }
}
