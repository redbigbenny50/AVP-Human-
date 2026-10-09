package com.human.common.registry.init.item;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.item.ArmorCaseItem;
import com.human.common.gameplay.item.DaggerItem;
import com.human.common.gameplay.item.GeneReaderItem;
import com.human.common.gameplay.item.SyringeItem;
import com.human.common.gameplay.item.WiringToolItem;
import com.human.common.gameplay.item.canister.CanisterItem;
import com.human.common.gameplay.item.canister.MilkCanisterItem;
import com.human.common.gameplay.item.canister.SolidCanisterItem;
import com.human.common.gameplay.item.canister.UniversalCanisterItem;
import com.human.common.gameplay.item.grenade.GrenadeItem;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.HumanFluids;
import com.human.common.registry.init.HumanTiers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

import java.util.function.Supplier;

public class HumanItems {

    public static final BLibRegistry<Item> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    public static final BLibHolder<Item> ALUMINUM_INGOT = create("aluminum_ingot");

    public static final BLibHolder<Item> ALUMINUM_NUGGET = create("aluminum_nugget");

    public static final BLibHolder<Item> ARMOR_CASE = create(
        "armor_case",
        () -> new ArmorCaseItem(new Item.Properties().stacksTo(1))
    );

    public static final BLibHolder<Item> AUTUNITE_DUST = create("autunite_dust");

    public static final BLibHolder<Item> ASH_BALL = create("ash_ball");

    public static final BLibHolder<Item> BARREL = create("barrel");

    /**
     * [stated] Sep 29/30: the lithium battery pack - 100,000 FE, rechargeable. See
     * {@link com.human.common.gameplay.item.power.BatteryItem}.
     */
    public static final BLibHolder<com.human.common.gameplay.item.power.BatteryItem> BATTERY_PACK = create(
        "battery_pack",
        () -> new com.human.common.gameplay.item.power.BatteryItem(
            new Item.Properties(),
            com.human.common.gameplay.item.power.BatteryItem.PACK_CAPACITY,
            true
        )
    );

    public static final BLibHolder<Item> BLUEPRINT_F903WE_RIFLE = create("blueprint_f903we_rifle");

    public static final BLibHolder<Item> BLUEPRINT_FLAMETHROWER_SEVASTOPOL = create("blueprint_flamethrower_sevastopol");

    public static final BLibHolder<Item> BLUEPRINT_M37_12_SHOTGUN = create("blueprint_m37_12_shotgun");

    public static final BLibHolder<Item> BLUEPRINT_M41A_PULSE_RIFLE = create("blueprint_m41a_pulse_rifle");

    public static final BLibHolder<Item> BLUEPRINT_M42A3_SNIPER_RIFLE = create("blueprint_m42a3_sniper_rifle");

    public static final BLibHolder<Item> BLUEPRINT_M4RA_BATTLE_RIFLE = create("blueprint_m4ra_battle_rifle");

    public static final BLibHolder<Item> BLUEPRINT_M56_SMARTGUN = create("blueprint_m56_smartgun");

    public static final BLibHolder<Item> BLUEPRINT_M6B_ROCKET_LAUNCHER = create("blueprint_m6b_rocket_launcher");

    public static final BLibHolder<Item> BLUEPRINT_M88MOD4_COMBAT_PISTOL = create("blueprint_m88mod4_combat_pistol");

    public static final BLibHolder<Item> BLUEPRINT_OLD_PAINLESS = create("blueprint_old_painless");

    public static final BLibHolder<Item> BLUEPRINT_ZX_76_SHOTGUN = create("blueprint_zx_76_shotgun");

    public static final BLibHolder<Item> BRASS_INGOT = create("brass_ingot");

    public static final BLibHolder<Item> BRASS_NUGGET = create("brass_nugget");

    /**
     * The universal canister - any fluid, from any mod, up to 8 buckets. Same id as the old empty canister, so every
     * empty canister already in the world simply becomes this. The water, lava and latex canisters below are LEGACY and
     * convert into this one in a player's inventory.
     */
    public static final BLibHolder<Item> CANISTER = create(
        "canister",
        () -> new UniversalCanisterItem(new Item.Properties().stacksTo(16))
    );

    public static final BLibHolder<Item> CAPACITOR = create("capacitor");

    public static final BLibHolder<Item> CARBON_DUST = create("carbon_dust");

    // Metal dusts. [stated] Sep 29: the grinder turns each raw ore into two, and one dust smelts to one ingot.
    public static final BLibHolder<Item> IRON_DUST = create("iron_dust");

    public static final BLibHolder<Item> GOLD_DUST = create("gold_dust");

    public static final BLibHolder<Item> COPPER_DUST = create("copper_dust");

    public static final BLibHolder<Item> TITANIUM_DUST = create("titanium_dust");

    public static final BLibHolder<Item> ALUMINUM_DUST = create("aluminum_dust");

    public static final BLibHolder<Item> FERROALUMINUM_DUST = create("ferroaluminum_dust");

    public static final BLibHolder<Item> LEAD_DUST = create("lead_dust");

    public static final BLibHolder<Item> ZINC_DUST = create("zinc_dust");

    public static final BLibHolder<Item> BRASS_DUST = create("brass_dust");

    public static final BLibHolder<Item> STEEL_DUST = create("steel_dust");

    public static final BLibHolder<Item> MAGNETIC_DUST = create("magnetic_dust");

    public static final BLibHolder<Item> CASELESS_BULLET = create("caseless_bullet");

    /**
     * Field rations, and deliberately better than a loaf of bread.
     * <p>
     * Vanilla BREAD is nutrition 5 / modifier 0.6, i.e. 6.0 saturation. Cornbread was 3 / 0.3 = 1.8 saturation, which
     * made it WORSE than bread despite costing wheat, an egg, a bucket of milk and a dye. At 6 / 0.8 it restores one
     * more hunger point than bread and 9.6 saturation - 60% more staying power - which lands it level with cooked
     * mutton and still short of cooked beef, so it is worth carrying without displacing real meat.
     */
    public static final BLibHolder<Item> CORNBREAD = create(
        "cornbread",
        new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build())
    );

    public static final BLibHolder<Item> CPU = create("cpu");

    // [stated] Oct 2: computer parts for the terminal and MUTHUR, made at the electronics workstation.
    public static final BLibHolder<Item> RAM = create("ram");

    public static final BLibHolder<Item> SSD = create("ssd");

    public static final BLibHolder<Item> MOTHERBOARD = create("motherboard");

    public static final BLibHolder<Item> POWER_SUPPLY = create("power_supply");

    public static final BLibHolder<Item> DIODE = create("diode");

    public static final BLibHolder<Item> DOG_TAG = create("dog_tag");

    /** Old Painless's belt drum. One cannister is one full 1500-round load; it is consumed whole on reload. */
    public static final BLibHolder<Item> DRUM_CANNISTER = create("drum_cannister", new Item.Properties().stacksTo(1));

    /** The M56 Smartgun's drum. One cartridge is one full 800-round load; it is consumed whole on reload. */
    public static final BLibHolder<Item> DRUM_CARTRIDGE = create("drum_cartridge", new Item.Properties().stacksTo(1));

    public static final BLibHolder<Item> FERROALUMINUM_INGOT = create("ferroaluminum_ingot");

    public static final BLibHolder<Item> FERROALUMINUM_NUGGET = create("ferroaluminum_nugget");

    public static final BLibHolder<Item> FUEL_TANK = create("fuel_tank", new Item.Properties().stacksTo(1));

    public static final BLibHolder<Item> GENE_READER = create("gene_reader", GeneReaderItem::new);

    public static final BLibHolder<Item> GRENADE = create("grenade_standard", () -> new GrenadeItem(false, false));

    public static final BLibHolder<Item> GRENADE_INCENDIARY = create("grenade_incendiary", () -> new GrenadeItem(true, false));

    public static final BLibHolder<Item> GRENADE_IRRADIATED = create("grenade_irradiated", () -> new GrenadeItem(false, true));

    public static final BLibHolder<Item> GRIP = create("grip");

    public static final BLibHolder<Item> HEAVY_BULLET = create("heavy_bullet");

    public static final BLibHolder<Item> INTEGRATED_CIRCUIT = create("integrated_circuit");

    public static final BLibHolder<Item> LAVA_CANISTER = create(
        "lava_canister",
        () -> new CanisterItem(
            Fluids.LAVA,
            new Item.Properties().stacksTo(1).craftRemainder(CANISTER.get()).component(HumanDataComponents.CANISTER_CAPACITY.get(), 1)
        )
    );

    public static final BLibHolder<Item> LEAD_INGOT = create("lead_ingot");

    public static final BLibHolder<Item> LEAD_NUGGET = create("lead_nugget");

    public static final BLibHolder<Item> LED = create("led");

    public static final BLibHolder<Item> LED_DISPLAY = create("led_display");

    public static final BLibHolder<Item> LITHIUM_DUST = create("lithium_dust");

    public static final BLibHolder<Item> MEDIUM_BULLET = create("medium_bullet");

    public static final BLibHolder<Item> MILK_CANISTER = create(
        "milk_canister",
        () -> new MilkCanisterItem(
            new Item.Properties().craftRemainder(CANISTER.get()).stacksTo(1).component(HumanDataComponents.CANISTER_CAPACITY.get(), 1)
        )
    );

    public static final BLibHolder<Item> MINIGUN_BARREL = create("minigun_barrel");

    public static final BLibHolder<Item> NEODYMIUM_MAGNET = create("neodymium_magnet");

    /** [stated] Sep 29/30: 10x the pack, NOT rechargeable, consumed when drained. See BatteryItem. */
    public static final BLibHolder<com.human.common.gameplay.item.power.BatteryItem> NUCLEAR_BATTERY = create(
        "nuclear_battery",
        () -> new com.human.common.gameplay.item.power.BatteryItem(
            new Item.Properties(),
            com.human.common.gameplay.item.power.BatteryItem.NUCLEAR_CAPACITY,
            false
        )
    );

    public static final BLibHolder<Item> LATEX_BUCKET = create(
        "latex_bucket",
        () -> new BucketItem(
            HumanFluids.LATEX.get(),
            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
        )
    );

    /** [stated] Oct 1: slimeball + blue ice. The press turns it into cryofluid; it also goes into cryo gear. */
    public static final BLibHolder<Item> CRYOGEL = create("cryogel", new Item.Properties());

    public static final BLibHolder<Item> CRYOFLUID_BUCKET = create(
        "cryofluid_bucket",
        () -> new BucketItem(
            HumanFluids.CRYOFLUID.get(),
            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
        )
    );

    public static final BLibHolder<Item> HONEY_BUCKET = create(
        "honey_bucket",
        () -> new BucketItem(
            HumanFluids.HONEY.get(),
            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
        )
    );

    /**
     * ⚠ MODELLED EXACTLY ON {@link #LAVA_CANISTER}, not on the latex BUCKET. A canister stacks to 1, leaves an empty
     * {@link #CANISTER} behind when used in a recipe, and carries the capacity component - a bucket does none of that,
     * so copying the bucket would have produced something that looks like a canister and behaves like a bucket.
     */
    public static final BLibHolder<Item> LATEX_CANISTER = create(
        "latex_canister",
        () -> new CanisterItem(
            HumanFluids.LATEX.get(),
            new Item.Properties().stacksTo(1).craftRemainder(CANISTER.get()).component(HumanDataComponents.CANISTER_CAPACITY.get(), 1)
        )
    );

    public static final BLibHolder<Item> LATEX_SHEET = create("latex_sheet");

    public static final BLibHolder<Item> POLYMER = create("polymer");

    public static final BLibHolder<Item> RUBBER_GLOBULE = create("rubber_globule");

    public static final BLibHolder<Item> POWDER_SNOW_CANISTER = create(
        "powder_snow_canister",
        () -> new SolidCanisterItem(
            Blocks.POWDER_SNOW,
            SoundEvents.BUCKET_EMPTY_POWDER_SNOW,
            new Item.Properties().stacksTo(1).component(HumanDataComponents.CANISTER_CAPACITY.get(), 1)
        )
    );

    public static final BLibHolder<Item> RAW_BAUXITE = create("raw_bauxite");

    public static final BLibHolder<Item> RAW_BRASS = create("raw_brass");

    public static final BLibHolder<Item> RAW_CRUDE_IRON = create("raw_crude_iron");

    public static final BLibHolder<Item> RAW_FERROBAUXITE = create("raw_ferrobauxite");

    public static final BLibHolder<Item> RAW_GALENA = create("raw_galena");

    public static final BLibHolder<Item> RAW_MONAZITE = create("raw_monazite");

    public static final BLibHolder<Item> RAW_TITANIUM = create("raw_titanium");

    public static final BLibHolder<Item> RAW_ZINC = create("raw_zinc");

    public static final BLibHolder<Item> RECEIVER = create("receiver");

    public static final BLibHolder<Item> REDSTONE_CRYSTAL = create("redstone_crystal");

    public static final BLibHolder<Item> REGULATOR = create("regulator");

    public static final BLibHolder<Item> RESISTOR = create("resistor");

    public static final BLibHolder<Item> ROCKET = create("rocket");

    public static final BLibHolder<Item> ROCKET_BARREL = create("rocket_barrel");

    public static final BLibHolder<Item> SERVO = create("servo");

    // Power-system components (Sep 24): the parts the tier-1 generators are built from.
    public static final BLibHolder<Item> TURBINE = create("turbine");

    public static final BLibHolder<Item> ELECTRIC_MOTOR = create("electric_motor");

    public static final BLibHolder<Item> PHOTOVOLTAIC_ARRAY = create("photovoltaic_array");

    public static final BLibHolder<Item> FAN = create("fan");

    public static final BLibHolder<Item> HEAT_SINK = create("heat_sink");

    public static final BLibHolder<Item> THERMAL_PLATE = create("thermal_plate");

    public static final BLibHolder<Item> COPPER_COIL = create("copper_coil");

    /** [stated] Sep 29 readme - inspects grids, switches machines on and off, opens and closes breakers. */
    public static final BLibHolder<WiringToolItem> WIRING_TOOL = create(
        "wiring_tool",
        () -> new WiringToolItem(new Item.Properties().stacksTo(1))
    );

    // Battery upgrade cards. [stated] Sep 29: red redstone, grey armoured, green capacity, purple emergency, blue
    // transfer, white alarm.
    /** [stated] Sep 30: the teal card - a machine with it binds to a fuse panel and needs no wire. */
    public static final BLibHolder<com.human.common.gameplay.item.power.HubChipItem> HUB_UPGRADE = create(
        "hub_upgrade",
        () -> new com.human.common.gameplay.item.power.HubChipItem(new Item.Properties().stacksTo(16))
    );

    public static final BLibHolder<Item> CAPACITY_UPGRADE = create(
        "capacity_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.BatteryUpgradeItem(
            com.human.common.gameplay.power.upgrade.BatteryUpgrade.CAPACITY,
            new Item.Properties().stacksTo(16)
        )
    );

    public static final BLibHolder<Item> TRANSFER_UPGRADE = create(
        "transfer_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.BatteryUpgradeItem(
            com.human.common.gameplay.power.upgrade.BatteryUpgrade.TRANSFER,
            new Item.Properties().stacksTo(16)
        )
    );

    public static final BLibHolder<Item> EMERGENCY_UPGRADE = create(
        "emergency_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.BatteryUpgradeItem(
            com.human.common.gameplay.power.upgrade.BatteryUpgrade.EMERGENCY,
            new Item.Properties().stacksTo(16)
        )
    );

    public static final BLibHolder<Item> REDSTONE_UPGRADE = create(
        "redstone_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.BatteryUpgradeItem(
            com.human.common.gameplay.power.upgrade.BatteryUpgrade.REDSTONE,
            new Item.Properties().stacksTo(16)
        )
    );

    public static final BLibHolder<Item> ALARM_UPGRADE = create(
        "alarm_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.BatteryUpgradeItem(
            com.human.common.gameplay.power.upgrade.BatteryUpgrade.ALARM,
            new Item.Properties().stacksTo(16)
        )
    );

    public static final BLibHolder<Item> ARMORED_UPGRADE = create(
        "armored_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.BatteryUpgradeItem(
            com.human.common.gameplay.power.upgrade.BatteryUpgrade.ARMORED,
            new Item.Properties().stacksTo(16)
        )
    );

    // [stated] Oct 2: machine cards (speed, efficiency) and the sentry's sideways cards. Rules and numbers:
    // UpgradeCard.
    public static final BLibHolder<Item> SPEED_UPGRADE = create(
        "speed_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.UpgradeCardItem(
            com.human.common.gameplay.power.upgrade.UpgradeCard.SPEED,
            new Item.Properties().stacksTo(16)
        )
    );

    public static final BLibHolder<Item> EFFICIENCY_UPGRADE = create(
        "efficiency_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.UpgradeCardItem(
            com.human.common.gameplay.power.upgrade.UpgradeCard.EFFICIENCY,
            new Item.Properties().stacksTo(16)
        )
    );

    public static final BLibHolder<Item> RANGE_UPGRADE = create(
        "range_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.UpgradeCardItem(
            com.human.common.gameplay.power.upgrade.UpgradeCard.RANGE,
            new Item.Properties().stacksTo(16)
        )
    );

    public static final BLibHolder<Item> WIDE_CONE_UPGRADE = create(
        "wide_cone_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.UpgradeCardItem(
            com.human.common.gameplay.power.upgrade.UpgradeCard.WIDE_CONE,
            new Item.Properties().stacksTo(16)
        )
    );

    // [stated] Oct 2: the sentry's sensor lenses (one sensor slot) and its network card (MUTHUR link slot).
    public static final BLibHolder<Item> MOTION_SENSOR = create("motion_sensor", () -> new Item(new Item.Properties().stacksTo(16)));

    public static final BLibHolder<Item> EM_SENSOR = create("em_sensor", () -> new Item(new Item.Properties().stacksTo(16)));

    public static final BLibHolder<Item> CAMERA_SENSOR = create("camera_sensor", () -> new Item(new Item.Properties().stacksTo(16)));

    public static final BLibHolder<Item> NETWORK_CARD = create("network_card", () -> new Item(new Item.Properties().stacksTo(16)));

    /** [stated] Oct 2: one Skin Kit item for every skin; the skin rides on it as a component. */
    public static final BLibHolder<Item> SKIN_KIT = create(
        "skin_kit",
        () -> new com.human.common.gameplay.item.SkinKitItem(new Item.Properties().stacksTo(16))
    );

    public static final BLibHolder<Item> DEFENSE_UPGRADE = create(
        "defense_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.UpgradeCardItem(
            com.human.common.gameplay.power.upgrade.UpgradeCard.DEFENSE,
            new Item.Properties().stacksTo(16)
        )
    );

    public static final BLibHolder<Item> COOLING_UPGRADE = create(
        "cooling_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.UpgradeCardItem(
            com.human.common.gameplay.power.upgrade.UpgradeCard.COOLING,
            new Item.Properties().stacksTo(16)
        )
    );

    public static final BLibHolder<Item> CASELESS_AMMO_UPGRADE = create(
        "caseless_ammo_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.UpgradeCardItem(
            com.human.common.gameplay.power.upgrade.UpgradeCard.CASELESS_AMMO,
            new Item.Properties().stacksTo(16)
        )
    );

    public static final BLibHolder<Item> ROCKET_AMMO_UPGRADE = create(
        "rocket_ammo_upgrade",
        () -> new com.human.common.gameplay.power.upgrade.UpgradeCardItem(
            com.human.common.gameplay.power.upgrade.UpgradeCard.ROCKET_AMMO,
            new Item.Properties().stacksTo(16)
        )
    );

    public static final BLibHolder<Item> SHOTGUN_SHELL = create("shotgun_shell");

    // TODO: Change this to "silicon" with 0.2.0.
    public static final BLibHolder<Item> SILICON = create("raw_silica");

    public static final BLibHolder<Item> SMALL_BULLET = create("small_bullet");

    public static final BLibHolder<Item> SMART_BARREL = create("smart_barrel");

    public static final BLibHolder<Item> SMART_RECEIVER = create("smart_receiver");

    public static final BLibHolder<Item> SPEAKER = create("speaker");

    public static final BLibHolder<Item> STOCK = create("stock");

    public static final BLibHolder<Item> STEEL_AXE = create(
        "steel_axe",
        () -> new AxeItem(HumanTiers.STEEL, new Item.Properties().attributes(AxeItem.createAttributes(HumanTiers.STEEL, 6.0F, -3.1F)))
    );

    public static final BLibHolder<Item> STEEL_HOE = create(
        "steel_hoe",
        () -> new HoeItem(HumanTiers.STEEL, new Item.Properties().attributes(HoeItem.createAttributes(HumanTiers.STEEL, -2.0F, -1.0F)))
    );

    public static final BLibHolder<Item> STEEL_INGOT = create("steel_ingot");

    /**
     * [stated] Oct 3: composite - steel and plastic. 2 polymer and a steel ingot make one; 9 make a composite block.
     */
    public static final BLibHolder<Item> COMPOSITE_INGOT = create("composite_ingot");

    public static final BLibHolder<Item> STEEL_NUGGET = create("steel_nugget");

    public static final BLibHolder<Item> STEEL_PICKAXE = create(
        "steel_pickaxe",
        () -> new PickaxeItem(
            HumanTiers.STEEL,
            new Item.Properties().attributes(PickaxeItem.createAttributes(HumanTiers.STEEL, 1.0F, -2.8F))
        )
    );

    public static final BLibHolder<Item> STEEL_SHOVEL = create(
        "steel_shovel",
        () -> new ShovelItem(HumanTiers.STEEL, new Item.Properties().attributes(ShovelItem.createAttributes(HumanTiers.STEEL, 1.5F, -3.0F)))
    );

    public static final BLibHolder<Item> STEEL_SWORD = create(
        "steel_sword",
        () -> new SwordItem(HumanTiers.STEEL, new Item.Properties().attributes(SwordItem.createAttributes(HumanTiers.STEEL, 3, -2.4F)))
    );

    public static final BLibHolder<Item> SYRINGE = create("syringe", SyringeItem::new);

    public static final BLibHolder<Item> TACTICAL_KNIFE = create(
        "tactical_knife",
        () -> new DaggerItem(HumanTiers.STEEL, new Item.Properties().attributes(SwordItem.createAttributes(HumanTiers.STEEL, 1, -1.8F)))
    );

    public static final BLibHolder<Item> TITANIUM_AXE = create(
        "titanium_axe",
        () -> new AxeItem(HumanTiers.TITANIUM, new Item.Properties().attributes(AxeItem.createAttributes(HumanTiers.TITANIUM, 6.0F, -3.1F)))
    );

    public static final BLibHolder<Item> TITANIUM_HOE = create(
        "titanium_hoe",
        () -> new HoeItem(
            HumanTiers.TITANIUM,
            new Item.Properties().attributes(HoeItem.createAttributes(HumanTiers.TITANIUM, -2.0F, -1.0F))
        )
    );

    public static final BLibHolder<Item> TITANIUM_INGOT = create("titanium_ingot");

    /** [stated] Oct 1: a lead that holds any mob, to 16 blocks. */
    public static final BLibHolder<Item> TITANIUM_CAPTURE_CORD = create(
        "titanium_capture_cord",
        () -> new com.human.common.gameplay.item.TitaniumCaptureCordItem(new Item.Properties())
    );

    public static final BLibHolder<Item> TITANIUM_NUGGET = create("titanium_nugget");

    public static final BLibHolder<Item> TITANIUM_PICKAXE = create(
        "titanium_pickaxe",
        () -> new PickaxeItem(
            HumanTiers.TITANIUM,
            new Item.Properties().attributes(PickaxeItem.createAttributes(HumanTiers.TITANIUM, 1.0F, -2.8F))
        )
    );

    public static final BLibHolder<Item> TITANIUM_SHOVEL = create(
        "titanium_shovel",
        () -> new ShovelItem(
            HumanTiers.TITANIUM,
            new Item.Properties().attributes(ShovelItem.createAttributes(HumanTiers.TITANIUM, 1.5F, -3.0F))
        )
    );

    public static final BLibHolder<Item> TITANIUM_SWORD = create(
        "titanium_sword",
        () -> new SwordItem(
            HumanTiers.TITANIUM,
            new Item.Properties().attributes(SwordItem.createAttributes(HumanTiers.TITANIUM, 3, -2.4F))
        )
    );

    public static final BLibHolder<Item> TRANSISTOR = create("transistor");

    public static final BLibHolder<Item> URANIUM_INGOT = create("uranium_ingot");

    public static final BLibHolder<Item> URANIUM_NUGGET = create("uranium_nugget");

    public static final BLibHolder<Item> WATER_CANISTER = create(
        "water_canister",
        () -> new CanisterItem(
            Fluids.WATER,
            new Item.Properties().stacksTo(1).craftRemainder(CANISTER.get()).component(HumanDataComponents.CANISTER_CAPACITY.get(), 1)
        )
    );

    public static final BLibHolder<Item> ZINC_INGOT = create("zinc_ingot");

    public static final BLibHolder<Item> ZINC_NUGGET = create("zinc_nugget");

    /** [stated] Oct 3: the heat sink's recipe needs copper nuggets, and neither vanilla nor this mod had one. */
    public static final BLibHolder<Item> COPPER_NUGGET = create("copper_nugget");

    public static BLibHolder<Item> create(String name) {
        return create(name, new Item.Properties());
    }

    private static BLibHolder<Item> create(String name, Item.Properties properties) {
        return create(name, () -> new Item(properties));
    }

    private static <T extends Item> BLibHolder<T> create(String name, Supplier<T> itemSupplier) {
        return REGISTRY.createHolder(name, itemSupplier);
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
