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

        // Two reasons to dress for a dive: the marine is itself under the water without water breathing, or the leader
        // it mirrors has committed to staying under (see HumanWaterMovement). His ruling: "if the player stays
        // underwater too long or has an effect letting them breathe underwater the marines will put on the pressure
        // suit or mk50 if they have them."
        var isUnderwaterUnaided = worldState.getOrDefault(GOAPSensors.IS_UNDERWATER.key(), false)
            && !worldState.getOrDefault(GOAPSensors.HAS_WATER_BREATHING.key(), false);
        var isLeaderStayingUnderwater = worldState.getOrDefault(HumanGOAPSensors.IS_LEADER_STAYING_UNDERWATER.key(), false);

        // Space, per the Sep 21 rulings: "pressure suit is bare bones gives air, mk50 is environmental protection and
        // air but its slower." Extreme heat or cold is MK50 or nothing (the pressure suit does not help, so it is
        // not offered). Airless alone is the same choice as a dive - the lighter suit first, an MK50 already worn kept.
        var isInExtremeTemperature = worldState.getOrDefault(HumanGOAPSensors.IS_IN_EXTREME_HEAT.key(), false)
            || worldState.getOrDefault(HumanGOAPSensors.IS_IN_EXTREME_COLD.key(), false);

        if (isInExtremeTemperature) {
            var mk50 = worldState.getOrDefault(EquipArmorSensors.MK50_ARMOR_SET_TARGET.key(), ArmorSetTarget.EMPTY);

            if (!mk50.isEmpty()) {
                return mk50;
            }
        }

        var isInAirlessEnvironment = worldState.getOrDefault(HumanGOAPSensors.IS_IN_AIRLESS_ENVIRONMENT.key(), false);

        if (isUnderwaterUnaided || isLeaderStayingUnderwater || isInAirlessEnvironment) {
            var diveSuit = getDiveSuit(worldState);

            // No suit at all: fall through to the normal choice rather than returning an empty target, which would
            // otherwise stop the marine wearing its ordinary armor for as long as it is in the water or the vacuum.
            if (!diveSuit.isEmpty()) {
                return diveSuit;
            }
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

    /**
     * The pressure suit is the bare-bones air supply and the MK50 is full environmental protection at the cost of
     * speed, so the pressure suit is the default dive dress. An MK50 that is ALREADY WORN is kept - it breathes just as
     * well, and swapping a full set underwater for no gain is exactly the kind of fidgeting a diver does not do.
     */
    private static ArmorSetTarget getDiveSuit(ReadableWorldState worldState) {
        var mk50 = worldState.getOrDefault(EquipArmorSensors.MK50_ARMOR_SET_TARGET.key(), ArmorSetTarget.EMPTY);

        if (!mk50.isEmpty() && mk50.allMatch(ItemTarget.Location.EQUIPPED)) {
            return mk50;
        }

        var pressureSuit = worldState.getOrDefault(EquipArmorSensors.PRESSURE_SUIT_ARMOR_SET_TARGET.key(), ArmorSetTarget.EMPTY);

        return pressureSuit.isEmpty() ? mk50 : pressureSuit;
    }

    private static ArmorSetTarget getFullSetFireResistantArmor(ReadableWorldState worldState) {
        var platedChitinArmorSetTarget = worldState.getOrNull(EquipArmorSensors.PLATED_NETHER_CHITIN_ARMOR_SET_TARGET.key());

        if (platedChitinArmorSetTarget != null && !platedChitinArmorSetTarget.isEmpty()) {
            return platedChitinArmorSetTarget;
        }

        return worldState.getOrDefault(EquipArmorSensors.NETHER_CHITIN_ARMOR_SET_TARGET.key(), ArmorSetTarget.EMPTY);
    }
}
