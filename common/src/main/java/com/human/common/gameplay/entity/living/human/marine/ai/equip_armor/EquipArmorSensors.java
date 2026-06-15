package com.human.common.gameplay.entity.living.human.marine.ai.equip_armor;

import com.alien.common.registry.init.item.AlienArmorItems;
import com.human.common.gameplay.entity.ai.utility.sensor.BestItemSensor;
import com.human.common.gameplay.entity.ai.utility.sensor.EquippedItemSensor;
import com.human.common.gameplay.entity.ai.utility.sensor.ItemInInventorySensor;
import com.human.common.gameplay.entity.ai.utility.sensor.ItemInWorldSensor;
import com.human.common.gameplay.entity.living.human.ai.ItemSenseUtil;
import com.human.common.gameplay.entity.living.human.ai.model.ArmorSet;
import com.human.common.gameplay.entity.living.human.ai.model.ArmorSetTarget;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.sensor.BestArmorSetTargetSensor;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.strategy.ArmorStrategy;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.strategy.ArmorStrategyResult;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.strategy.ArmorStrategySet;
import com.human.common.registry.init.item.HumanArmorItems;
import com.human.compatibility.avp_alien.AVPAlien;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;

public class EquipArmorSensors {

    private static final ArmorSet MK50_ARMOR_SET = new ArmorSet(
        HumanArmorItems.MK50_HELMET,
        HumanArmorItems.MK50_CHESTPLATE,
        HumanArmorItems.MK50_LEGGINGS,
        HumanArmorItems.MK50_BOOTS
    );

    private static final ArmorSet PRESSURE_SUIT_ARMOR_SET = new ArmorSet(
        HumanArmorItems.PRESSURE_HELMET,
        HumanArmorItems.PRESSURE_CHESTPLATE,
        HumanArmorItems.PRESSURE_LEGGINGS,
        HumanArmorItems.PRESSURE_BOOTS
    );

    private static final Option<ArmorSet> NETHER_CHITIN_ARMOR_SET_OPTION = Option.ofNullable(
        AVPAlien.MOD.isLoaded()
            ? new ArmorSet(
                AlienArmorItems.NETHER_CHITIN_HELMET,
                AlienArmorItems.NETHER_CHITIN_CHESTPLATE,
                AlienArmorItems.NETHER_CHITIN_LEGGINGS,
                AlienArmorItems.NETHER_CHITIN_BOOTS
            )
            : null
    );

    private static final Option<ArmorSet> PLATED_NETHER_CHITIN_ARMOR_SET_OPTION = Option.ofNullable(
        AVPAlien.MOD.isLoaded()
            ? new ArmorSet(
                AlienArmorItems.PLATED_NETHER_CHITIN_HELMET,
                AlienArmorItems.PLATED_NETHER_CHITIN_CHESTPLATE,
                AlienArmorItems.PLATED_NETHER_CHITIN_LEGGINGS,
                AlienArmorItems.PLATED_NETHER_CHITIN_BOOTS
            )
            : null
    );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<ItemTarget.Equipped>>> BEST_HELMET_EQUIPPED = Sensors.lazyCompose(
        StateKey.sensed("best_helmet_equipped"),
        EquippedItemSensor.builder(() -> ArmorStrategySet.INSTANCE.getForType(ArmorItem.Type.HELMET), ArmorStrategyResult::new)
            .withEquipmentSlots(new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND })
            .build()::sense
    );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<ItemTarget.Inventory>>> BEST_HELMET_IN_INVENTORY = Sensors
        .lazyCompose(
            StateKey.sensed("best_helmet_in_inventory"),
            ItemInInventorySensor.builder(() -> ArmorStrategySet.INSTANCE.getForType(ArmorItem.Type.HELMET), ArmorStrategyResult::new)
                .build()::sense
        );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<ItemTarget.World>>> BEST_HELMET_IN_WORLD = Sensors.lazyCompose(
        StateKey.sensed("best_helmet_in_world"),
        ItemInWorldSensor.builder(() -> ArmorStrategySet.INSTANCE.getForType(ArmorItem.Type.HELMET), ArmorStrategyResult::new)
            .build()::sense
    );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<? extends ItemTarget>>> BEST_HELMET = Sensors.lazyCompose(
        StateKey.sensed("best_helmet"),
        BestItemSensor.<ArmorStrategy, ArmorStrategyResult<? extends ItemTarget>>builder()
            .withSensor(BEST_HELMET_EQUIPPED)
            .withSensor(BEST_HELMET_IN_INVENTORY)
            .withSensor(BEST_HELMET_IN_WORLD)
            .build()::sense
    );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<ItemTarget.Equipped>>> BEST_CHESTPLATE_EQUIPPED = Sensors
        .lazyCompose(
            StateKey.sensed("best_chestplate_equipped"),
            EquippedItemSensor.builder(() -> ArmorStrategySet.INSTANCE.getForType(ArmorItem.Type.CHESTPLATE), ArmorStrategyResult::new)
                .withEquipmentSlots(new EquipmentSlot[] { EquipmentSlot.CHEST, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND })
                .build()::sense
        );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<ItemTarget.Inventory>>> BEST_CHESTPLATE_IN_INVENTORY = Sensors
        .lazyCompose(
            StateKey.sensed("best_chestplate_in_inventory"),
            ItemInInventorySensor.builder(() -> ArmorStrategySet.INSTANCE.getForType(ArmorItem.Type.CHESTPLATE), ArmorStrategyResult::new)
                .build()::sense
        );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<ItemTarget.World>>> BEST_CHESTPLATE_IN_WORLD = Sensors
        .lazyCompose(
            StateKey.sensed("best_chestplate_in_world"),
            ItemInWorldSensor.builder(() -> ArmorStrategySet.INSTANCE.getForType(ArmorItem.Type.CHESTPLATE), ArmorStrategyResult::new)
                .build()::sense
        );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<? extends ItemTarget>>> BEST_CHESTPLATE = Sensors.lazyCompose(
        StateKey.sensed("best_chestplate"),
        BestItemSensor.<ArmorStrategy, ArmorStrategyResult<? extends ItemTarget>>builder()
            .withSensor(BEST_CHESTPLATE_EQUIPPED)
            .withSensor(BEST_CHESTPLATE_IN_INVENTORY)
            .withSensor(BEST_CHESTPLATE_IN_WORLD)
            .build()::sense
    );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<ItemTarget.Equipped>>> BEST_LEGGINGS_EQUIPPED = Sensors.lazyCompose(
        StateKey.sensed("best_leggings_equipped"),
        EquippedItemSensor.builder(() -> ArmorStrategySet.INSTANCE.getForType(ArmorItem.Type.LEGGINGS), ArmorStrategyResult::new)
            .withEquipmentSlots(new EquipmentSlot[] { EquipmentSlot.LEGS, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND })
            .build()::sense
    );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<ItemTarget.Inventory>>> BEST_LEGGINGS_IN_INVENTORY = Sensors
        .lazyCompose(
            StateKey.sensed("best_leggings_in_inventory"),
            ItemInInventorySensor.builder(() -> ArmorStrategySet.INSTANCE.getForType(ArmorItem.Type.LEGGINGS), ArmorStrategyResult::new)
                .build()::sense
        );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<ItemTarget.World>>> BEST_LEGGINGS_IN_WORLD = Sensors
        .lazyCompose(
            StateKey.sensed("best_leggings_in_world"),
            ItemInWorldSensor.builder(() -> ArmorStrategySet.INSTANCE.getForType(ArmorItem.Type.LEGGINGS), ArmorStrategyResult::new)
                .build()::sense
        );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<? extends ItemTarget>>> BEST_LEGGINGS = Sensors.lazyCompose(
        StateKey.sensed("best_leggings"),
        BestItemSensor.<ArmorStrategy, ArmorStrategyResult<? extends ItemTarget>>builder()
            .withSensor(BEST_LEGGINGS_EQUIPPED)
            .withSensor(BEST_LEGGINGS_IN_INVENTORY)
            .withSensor(BEST_LEGGINGS_IN_WORLD)
            .build()::sense
    );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<ItemTarget.Equipped>>> BEST_BOOTS_EQUIPPED = Sensors.lazyCompose(
        StateKey.sensed("best_boots_equipped"),
        EquippedItemSensor.builder(() -> ArmorStrategySet.INSTANCE.getForType(ArmorItem.Type.BOOTS), ArmorStrategyResult::new)
            .withEquipmentSlots(new EquipmentSlot[] { EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND })
            .build()::sense
    );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<ItemTarget.Inventory>>> BEST_BOOTS_IN_INVENTORY = Sensors
        .lazyCompose(
            StateKey.sensed("best_boots_in_inventory"),
            ItemInInventorySensor.builder(() -> ArmorStrategySet.INSTANCE.getForType(ArmorItem.Type.BOOTS), ArmorStrategyResult::new)
                .build()::sense
        );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<ItemTarget.World>>> BEST_BOOTS_IN_WORLD = Sensors.lazyCompose(
        StateKey.sensed("best_boots_in_world"),
        ItemInWorldSensor.builder(() -> ArmorStrategySet.INSTANCE.getForType(ArmorItem.Type.BOOTS), ArmorStrategyResult::new)
            .build()::sense
    );

    public static final Sensor.Mono<Marine, Option<ArmorStrategyResult<? extends ItemTarget>>> BEST_BOOTS = Sensors.lazyCompose(
        StateKey.sensed("best_boots"),
        BestItemSensor.<ArmorStrategy, ArmorStrategyResult<? extends ItemTarget>>builder()
            .withSensor(BEST_BOOTS_EQUIPPED)
            .withSensor(BEST_BOOTS_IN_INVENTORY)
            .withSensor(BEST_BOOTS_IN_WORLD)
            .build()::sense
    );

    public static final Sensor.Mono<Marine, ArmorSetTarget> MK50_ARMOR_SET_TARGET = Sensors.lazyCompose(
        StateKey.sensed("mk50_armor_set"),
        (livingEntity, worldState) -> ItemSenseUtil.findFullArmorSetInWorldState(livingEntity, worldState, MK50_ARMOR_SET)
    );

    public static final Sensor.Mono<Marine, ArmorSetTarget> NETHER_CHITIN_ARMOR_SET_TARGET = Sensors.lazyCompose(
        StateKey.sensed("nether_chitin_armor_set"),
        (livingEntity, worldState) -> NETHER_CHITIN_ARMOR_SET_OPTION.map(
            armorSet -> ItemSenseUtil.findFullArmorSetInWorldState(livingEntity, worldState, armorSet)
        ).unwrapOr(ArmorSetTarget.EMPTY)
    );

    public static final Sensor.Mono<Marine, ArmorSetTarget> PLATED_NETHER_CHITIN_ARMOR_SET_TARGET = Sensors.lazyCompose(
        StateKey.sensed("plated_nether_chitin_armor_set"),
        (livingEntity, worldState) -> PLATED_NETHER_CHITIN_ARMOR_SET_OPTION.map(
            armorSet -> ItemSenseUtil.findFullArmorSetInWorldState(livingEntity, worldState, armorSet)
        ).unwrapOr(ArmorSetTarget.EMPTY)
    );

    public static final Sensor.Mono<Marine, ArmorSetTarget> PRESSURE_SUIT_ARMOR_SET_TARGET = Sensors.lazyCompose(
        StateKey.sensed("pressure_suit_armor_set"),
        (livingEntity, worldState) -> ItemSenseUtil.findFullArmorSetInWorldState(livingEntity, worldState, PRESSURE_SUIT_ARMOR_SET)
    );

    public static final Sensor.Mono<LivingEntity, ArmorSetTarget> BEST_ARMOR_SET_TARGET = Sensors.lazyCompose(
        StateKey.sensed("best_armor_set"),
        BestArmorSetTargetSensor::sense
    );

    public static final Sensor.Mono<LivingEntity, Boolean> IS_ANY_BEST_ARMOR_SET_PIECE_IN_WORLD = Sensors.compose(
        BEST_ARMOR_SET_TARGET.key(),
        StateKey.sensed("is_any_best_armor_set_piece_in_world"),
        (livingEntity, bestArmorSetTarget) -> bestArmorSetTarget.anyMatch(ItemTarget.Location.WORLD)
    );

    public static final Sensor.Mono<LivingEntity, Boolean> IS_ANY_BEST_ARMOR_SET_PIECE_IN_INVENTORY = Sensors.compose(
        BEST_ARMOR_SET_TARGET.key(),
        StateKey.sensed("is_any_best_armor_set_piece_in_inventory"),
        (livingEntity, bestArmorSetTarget) -> bestArmorSetTarget.anyMatch(ItemTarget.Location.INVENTORY)
    );

    public static final Sensor.Mono<LivingEntity, Boolean> ARE_ALL_BEST_ARMOR_SET_PIECES_EQUIPPED = Sensors.compose(
        BEST_ARMOR_SET_TARGET.key(),
        StateKey.sensed("are_all_best_armor_set_pieces_equipped"),
        (livingEntity, bestArmorSetTarget) -> bestArmorSetTarget.allNoneOrMatch(ItemTarget.Location.EQUIPPED)
    );

    private EquipArmorSensors() {
        throw new UnsupportedOperationException();
    }
}
