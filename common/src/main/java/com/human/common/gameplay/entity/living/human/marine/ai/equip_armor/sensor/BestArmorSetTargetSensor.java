package com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.sensor;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.human.common.gameplay.entity.ai.goap.HumanGOAPSensors;
import com.human.common.gameplay.entity.living.human.ai.model.ArmorSetTarget;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.EquipArmorSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.strategy.ArmorStrategyResult;
import com.just.ai.goap.state.ReadableWorldState;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;

public class BestArmorSetTargetSensor {

    public static ArmorSetTarget sense(LivingEntity livingEntity, ReadableWorldState worldState) {
        if (
            worldState.getOrDefault(GOAPSensors.IS_IN_LAVA.key(), false)
                && !worldState.getOrDefault(GOAPSensors.HAS_FIRE_RESISTANCE.key(), false)
        ) {
            return getFullSetFireResistantArmor(worldState);
        }

        if (worldState.getOrDefault(HumanGOAPSensors.IS_NEAR_RADIOACTIVE_BIOME.key(), false)) {
            return worldState.getOrDefault(EquipArmorSensors.MK50_ARMOR_SET_TARGET.key(), ArmorSetTarget.EMPTY);
        }

        if (
            worldState.getOrDefault(GOAPSensors.IS_ON_FIRE.key(), false)
                && !worldState.getOrDefault(GOAPSensors.HAS_FIRE_RESISTANCE.key(), false)
        ) {
            return getFullSetFireResistantArmor(worldState);
        }

        if (
            worldState.getOrDefault(GOAPSensors.IS_UNDERWATER.key(), false)
                && !worldState.getOrDefault(GOAPSensors.HAS_WATER_BREATHING.key(), false)
        ) {
            return worldState.getOrDefault(EquipArmorSensors.PRESSURE_SUIT_ARMOR_SET_TARGET.key(), ArmorSetTarget.EMPTY);
        }

        // TODO: Select "best" armor pieces (depending on defense, enchantments, etc.)
        // TODO: Equip facehugger-resistant helmets if facehuggers are nearby.
        // TODO: Equip slow-falling boots if entity is falling.
        // TODO: Equip sneak speed boots if sneaking around.
        // TODO: Equip leather boots if walking near powdered snow.
        // TODO: Favor fire protection if entity is on fire and has no fire resistance.
        // TODO: Favor blast protection if a creeper, boiler, TNT or grenade is nearby and about to explode.
        var helmetWorldOption = worldState.getOrDefault(EquipArmorSensors.BEST_HELMET.key(), Option.none());
        var chestplateOption = worldState.getOrDefault(EquipArmorSensors.BEST_CHESTPLATE.key(), Option.none());
        var leggingsOption = worldState.getOrDefault(EquipArmorSensors.BEST_LEGGINGS.key(), Option.none());
        var bootsOption = worldState.getOrDefault(EquipArmorSensors.BEST_BOOTS.key(), Option.none());

        return new ArmorSetTarget(
            helmetWorldOption.<ItemTarget>map(ArmorStrategyResult::itemTarget)
                .unwrapOr(ItemTarget.None.INSTANCE),
            chestplateOption.<ItemTarget>map(ArmorStrategyResult::itemTarget)
                .unwrapOr(ItemTarget.None.INSTANCE),
            leggingsOption.<ItemTarget>map(ArmorStrategyResult::itemTarget)
                .unwrapOr(ItemTarget.None.INSTANCE),
            bootsOption.<ItemTarget>map(ArmorStrategyResult::itemTarget)
                .unwrapOr(ItemTarget.None.INSTANCE)
        );
    }

    private static ArmorSetTarget getFullSetFireResistantArmor(ReadableWorldState worldState) {
        var platedChitinArmorSetTarget = worldState.getOrNull(EquipArmorSensors.PLATED_NETHER_CHITIN_ARMOR_SET_TARGET.key());

        if (platedChitinArmorSetTarget != null && !platedChitinArmorSetTarget.isEmpty()) {
            return platedChitinArmorSetTarget;
        }

        return worldState.getOrDefault(EquipArmorSensors.NETHER_CHITIN_ARMOR_SET_TARGET.key(), ArmorSetTarget.EMPTY);
    }
}
