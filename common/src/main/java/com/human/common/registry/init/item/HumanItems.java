package com.human.common.registry.init.item;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.item.ArmorCaseItem;
import com.human.common.gameplay.item.DaggerItem;
import com.human.common.gameplay.item.GeneReaderItem;
import com.human.common.gameplay.item.SyringeItem;
import com.human.common.gameplay.item.canister.CanisterItem;
import com.human.common.gameplay.item.canister.MilkCanisterItem;
import com.human.common.gameplay.item.canister.SolidCanisterItem;
import com.human.common.gameplay.item.grenade.GrenadeItem;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.HumanTiers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
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

    public static final BLibHolder<Item> BATTERY_PACK = create("battery_pack");

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

    public static final BLibHolder<Item> CANISTER = create(
        "canister",
        () -> new CanisterItem(Fluids.EMPTY, new Item.Properties().stacksTo(16))
    );

    public static final BLibHolder<Item> CAPACITOR = create("capacitor");

    public static final BLibHolder<Item> CARBON_DUST = create("carbon_dust");

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

    public static final BLibHolder<Item> NUCLEAR_BATTERY = create("nuclear_battery");

    public static final BLibHolder<Item> POLYMER = create("polymer");

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
