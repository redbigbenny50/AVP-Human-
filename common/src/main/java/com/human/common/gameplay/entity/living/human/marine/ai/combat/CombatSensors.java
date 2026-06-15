package com.human.common.gameplay.entity.living.human.marine.ai.combat;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.human.common.gameplay.entity.ai.utility.sensor.BestItemSensor;
import com.human.common.gameplay.entity.ai.utility.sensor.EquippedItemSensor;
import com.human.common.gameplay.entity.ai.utility.sensor.ItemInInventorySensor;
import com.human.common.gameplay.entity.ai.utility.sensor.ItemInWorldSensor;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.WeaponStrategy;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.WeaponStrategyResult;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.WeaponStrategySet;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Compose;
import com.just.ai.goap.sensor.Compose2;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.sensor.Sensors;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class CombatSensors {

    public static final Sensor.Mono<LivingEntity, Option<WeaponStrategyResult<ItemTarget.Equipped>>> BEST_WEAPON_IN_HANDS = Sensors
        .lazyCompose(
            StateKey.sensed("best_weapon_in_hands"),
            EquippedItemSensor.builder(WeaponStrategySet.INSTANCE::getAll, WeaponStrategyResult::new)
                .withEquipmentSlots(new EquipmentSlot[] { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND })
                .build()::sense
        );

    public static final Sensor.Mono<Marine, Option<WeaponStrategyResult<ItemTarget.Inventory>>> BEST_WEAPON_IN_INVENTORY =
        Sensors.lazyCompose(
            StateKey.sensed("best_weapon_in_inventory"),
            ItemInInventorySensor.builder(WeaponStrategySet.INSTANCE::getAll, WeaponStrategyResult::new)
                .build()::sense
        );

    public static final Sensor.Mono<Marine, Option<WeaponStrategyResult<ItemTarget.World>>> BEST_WEAPON_IN_WORLD = Sensors
        .lazyCompose(
            StateKey.sensed("best_weapon_in_world"),
            ItemInWorldSensor.builder(WeaponStrategySet.INSTANCE::getAll, WeaponStrategyResult::new)
                .build()::sense
        );

    public static final Sensor.Mono<LivingEntity, Option<WeaponStrategyResult<? extends ItemTarget>>> BEST_WEAPON = Sensors.lazyCompose(
        StateKey.sensed("best_weapon"),
        BestItemSensor.<WeaponStrategy, WeaponStrategyResult<? extends ItemTarget>>builder()
            .withSensor(BEST_WEAPON_IN_HANDS)
            .withSensor(BEST_WEAPON_IN_INVENTORY)
            .withSensor(BEST_WEAPON_IN_WORLD)
            .build()::sense
    );

    public static final Compose2<Marine, Option<WeaponStrategyResult<ItemTarget.Equipped>>, Option<WeaponStrategyResult<ItemTarget.Inventory>>, Boolean> HAS_WEAPON =
        Sensors.compose(
            BEST_WEAPON_IN_HANDS.key(),
            BEST_WEAPON_IN_INVENTORY.key(),
            StateKey.sensed("has_weapon"),
            ($1, equippedWeapon, inventoryWeapon) -> equippedWeapon.isSome()
                || inventoryWeapon.isSome()
        );

    // TODO: We don't need the marine here, GOAP should support this case.
    public static final Compose<Object, Option<WeaponStrategyResult<? extends ItemTarget>>, ItemTarget.Location> BEST_WEAPON_LOCATION =
        Sensors.compose(
            BEST_WEAPON.key(),
            StateKey.sensed("best_weapon_location"),
            ($1, bwOption) -> bwOption.map(result -> result.itemTarget().location()).unwrapOr(ItemTarget.Location.NONE)
        );

    public static final Compose2<Mob, Option<LivingEntity>, Option<WeaponStrategyResult<ItemTarget.Equipped>>, Boolean> IS_ATTACK_TARGET_IN_RANGE_OF_EQUIPPED_BEST_WEAPON =
        Sensors.compose(
            GOAPSensors.NEAREST_ATTACKABLE_TARGET.key(),
            BEST_WEAPON_IN_HANDS.key(),
            StateKey.sensed("is_attack_target_in_range_of_equipped_best_weapon"),
            (mob, attackTargetOption, bwOption) -> bwOption.isSomeAnd(weaponStrategyResult -> {
                var itemStackOrNull = mob.getItemBySlot(weaponStrategyResult.itemTarget().equipmentSlot());

                if (attackTargetOption.isNone()) {
                    return false;
                }

                var attackTarget = attackTargetOption.unwrap();
                var rangeInBlocks = weaponStrategyResult.strategy().getRangeForWeapon(mob, itemStackOrNull);
                var rangeInBlocksSqr = rangeInBlocks * rangeInBlocks;

                return mob.distanceToSqr(attackTarget) <= rangeInBlocksSqr;
            })
        );

    public static final Compose<LivingEntity, Option<WeaponStrategyResult<ItemTarget.World>>, Boolean> IS_BEST_WORLD_WEAPON_IN_RANGE =
        Sensors
            .compose(
                BEST_WEAPON_IN_WORLD.key(),
                StateKey.sensed("is_best_world_weapon_in_range"),
                (livingEntity, bwOption) -> bwOption.isSomeAnd(
                    result -> livingEntity.distanceToSqr(result.itemTarget().itemEntity()) < 4
                )
            );

    private CombatSensors() {
        throw new UnsupportedOperationException();
    }
}
