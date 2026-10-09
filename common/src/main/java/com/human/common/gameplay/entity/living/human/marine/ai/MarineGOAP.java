package com.human.common.gameplay.entity.living.human.marine.ai;

import com.blib.api.common.entity.v1.BLibEntityPredicates;
import com.blib.api.common.goap.v1.GOAPSensors;
import com.human.common.gameplay.entity.ai.goap.HumanGOAPSensors;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.MarineAllyUtil;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.FRIActions;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.FRIGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.FRISensors;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.WBIActions;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.WBIGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.WBISensors;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.BreakFallActions;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.BreakFallGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.BreakFallSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatActions;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.cut_free.CutFreeActions;
import com.human.common.gameplay.entity.living.human.marine.ai.cut_free.CutFreeGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.cut_free.CutFreeSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.EquipArmorActions;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.EquipArmorGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.EquipArmorSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_totem.TotemActions;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_totem.TotemGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_totem.TotemSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire.ExtinguishFireActions;
import com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire.ExtinguishFireGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire.ExtinguishFireSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.fall_back.FallBackActions;
import com.human.common.gameplay.entity.living.human.marine.ai.fall_back.FallBackGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.fall_back.FallBackSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.follow_leader.FollowLeaderActions;
import com.human.common.gameplay.entity.living.human.marine.ai.follow_leader.FollowLeaderGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.follow_leader.FollowLeaderSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.HealingActions;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.HealingGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.HealingSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.idle.IdleActions;
import com.human.common.gameplay.entity.living.human.marine.ai.idle.IdleGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.idle.IdleSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.place_torch.TorchActions;
import com.human.common.gameplay.entity.living.human.marine.ai.place_torch.TorchGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.place_torch.TorchSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.sentry.SentryActions;
import com.human.common.gameplay.entity.living.human.marine.ai.sentry.SentryGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.sentry.SentrySensors;
import com.human.common.gameplay.entity.living.human.marine.ai.standoff.StandoffActions;
import com.human.common.gameplay.entity.living.human.marine.ai.standoff.StandoffGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.standoff.StandoffSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.tame_wolf.TameWolfActions;
import com.human.common.gameplay.entity.living.human.marine.ai.tame_wolf.TameWolfGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.tame_wolf.TameWolfSensors;
import com.just.ai.goap.Agent;
import com.just.ai.goap.graph.Graph;
import com.just.ai.goap.plan.ReplanPolicies;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.Objects;

public class MarineGOAP {

    public static final Graph<Marine> GRAPH = Graph.<Marine>builder()
        .apply(MarineGOAP::addSensorsPackage)
        .apply(MarineGOAP::addAcquireFireResistancePackage)
        .apply(MarineGOAP::addCombatPackage)
        .apply(MarineGOAP::addFallBackPackage)
        .apply(MarineGOAP::addStandoffPackage)
        .apply(MarineGOAP::addSentryPackage)
        .apply(MarineGOAP::addEquipBestArmorPackage)
        .apply(MarineGOAP::addExtinguishSelfPackage)
        .apply(MarineGOAP::addSatisfyBoredomPackage)
        .apply(MarineGOAP::addStayCloseToLeaderPackage)
        .apply(MarineGOAP::addTameWolfPackage)
        .apply(MarineGOAP::addCutFreePackage)
        .apply(MarineGOAP::addAcquireWaterBreathingPackage)
        .apply(MarineGOAP::addPlaceTorchPackage)
        .apply(MarineGOAP::addEquipTotemPackage)
        .apply(MarineGOAP::addCollectAmmoPackage)
        .apply(MarineGOAP::addBreakFallPackage)
        .apply(MarineGOAP::addHealSelfPackage)
        .build();

    public static Agent.Builder<Marine> applyAgentProperties(Agent.Builder<Marine> agentBuilder) {
        return agentBuilder.withReplanPolicy(
            ReplanPolicies.anyOf(
                // [stated] Oct 5 performance review. No active plan: replan every 5 ticks, not every tick - a marine
                // with no plan executes nothing, so this only delays the next idea by a quarter second and never
                // slows movement (avp_alien's throttle slowed walking until it was gated on exactly this).
                ReplanPolicies.custom(context -> !context.agent().hasPlan() && staggeredTick(context, 5)),
                // Replan every 20 ticks (every 1 second) - offset per marine, so a camp does not all think at once.
                ReplanPolicies.custom(context -> staggeredTick(context, 20)),
                ReplanPolicies.custom(context -> {
                    var isOnFire = context.worldState().getOrDefault(GOAPSensors.IS_ON_FIRE.key(), false);
                    var wasOnFire = context.previousWorldState().getOrDefault(GOAPSensors.IS_ON_FIRE.key(), false);
                    // If the marine was previously not on fire, and is now on fire, then replan.
                    return !wasOnFire && isOnFire;
                }),
                ReplanPolicies.custom(context -> {
                    // if the marine is falling, we want him to replan so he can save his own life.
                    return context.worldState().getOrDefault(BreakFallSensors.IS_FALLING.key(), false);
                }),
                ReplanPolicies.custom(context -> {
                    var currentHealthRatio = context.worldState().getOrDefault(GOAPSensors.HEALTH_RATIO.key(), 0F);
                    var previousHealthRatio = context.previousWorldState().getOrDefault(GOAPSensors.HEALTH_RATIO.key(), 0F);
                    // If the marine's health decreased, then replan.
                    return currentHealthRatio < previousHealthRatio && mayReplanForDamage(
                        context,
                        currentHealthRatio,
                        previousHealthRatio
                    );
                })
            )
        );
    }

    private static Graph.Builder<Marine> addSensorsPackage(Graph.Builder<Marine> graphBuilder) {
        // Environment.
        graphBuilder.addSensor(GOAPSensors.NEARBY_BLOCK_POSITIONS);
        graphBuilder.addSensor(MarineGOAPSensors.IS_CURRENT_BLOCK_POS_REPLACEABLE);
        graphBuilder.addSensor(HumanGOAPSensors.IS_NEAR_RADIOACTIVE_BIOME);
        graphBuilder.addSensor(HumanGOAPSensors.IS_LEADER_STAYING_UNDERWATER);
        graphBuilder.addSensor(HumanGOAPSensors.IS_IN_AIRLESS_ENVIRONMENT);
        graphBuilder.addSensor(HumanGOAPSensors.IS_IN_EXTREME_HEAT);
        graphBuilder.addSensor(HumanGOAPSensors.IS_IN_EXTREME_COLD);
        graphBuilder.addSensor(GOAPSensors.IS_IN_LAVA);
        graphBuilder.addSensor(GOAPSensors.IS_ON_GROUND);
        graphBuilder.addSensor(GOAPSensors.IS_UNDERWATER);
        graphBuilder.addSensor(GOAPSensors.HAS_WATER_BREATHING);
        // Self state.
        graphBuilder.addSensor(GOAPSensors.FIRE_RESISTANCE_REMAINING_TICKS);
        graphBuilder.addSensor(GOAPSensors.HAS_FIRE_RESISTANCE);
        graphBuilder.addSensor(GOAPSensors.HEALTH_RATIO);
        graphBuilder.addSensor(GOAPSensors.IS_ON_FIRE);

        return graphBuilder;
    }

    /**
     * Drinking (or splashing) Water Breathing when air runs short and no suit is at hand. The mirror of the
     * fire-resistance package; [stated] Sep 21 "if they have waterbreathing potions they will use them."
     */
    private static Graph.Builder<Marine> addAcquireWaterBreathingPackage(Graph.Builder<Marine> graphBuilder) {
        graphBuilder.addGoal(WBIGoals.ACQUIRE_WATER_BREATHING_GOAL);

        graphBuilder.addAction(WBIActions.MOVE_TO_BEST_WBI);
        graphBuilder.addAction(WBIActions.pickUpBestWBIFactory());
        graphBuilder.addAction(WBIActions.equipBestWBIFactory());
        graphBuilder.addAction(WBIActions.USE_BEST_WBI);

        graphBuilder.addSensor(WBISensors.WANTS_WATER_BREATHING);
        graphBuilder.addSensor(HumanGOAPSensors.AIR_RATIO);
        graphBuilder.addSensor(HumanGOAPSensors.WATER_BREATHING_REMAINING_TICKS);
        graphBuilder.addSensor(WBISensors.BEST_WBI);
        graphBuilder.addSensor(WBISensors.BEST_WBI_LOCATION);
        graphBuilder.addSensor(WBISensors.BEST_WBI_IN_HANDS);
        graphBuilder.addSensor(WBISensors.BEST_WBI_IN_INVENTORY);
        graphBuilder.addSensor(WBISensors.BEST_WBI_IN_WORLD);
        graphBuilder.addSensor(WBISensors.IS_BEST_WORLD_WBI_IN_RANGE);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addAcquireFireResistancePackage(Graph.Builder<Marine> graphBuilder) {
        // The goal we want to complete.
        graphBuilder.addGoal(FRIGoals.ACQUIRE_FIRE_RESISTANCE_GOAL);

        // Actions that can complete the goal.
        graphBuilder.addAction(FRIActions.MOVE_TO_BEST_FRI);
        graphBuilder.addAction(FRIActions.pickUpBestFRIFactory());
        graphBuilder.addAction(FRIActions.equipBestFRIFactory());
        graphBuilder.addAction(FRIActions.USE_BEST_FRI);

        // Used for locating best FRI.
        graphBuilder.addSensor(FRISensors.BEST_FRI);
        graphBuilder.addSensor(FRISensors.BEST_FRI_LOCATION);
        // Used for locating best FRI on self.
        graphBuilder.addSensor(FRISensors.BEST_FRI_IN_HANDS);
        graphBuilder.addSensor(FRISensors.BEST_FRI_IN_INVENTORY);
        // Used for locating best FRI in world.
        graphBuilder.addSensor(FRISensors.BEST_FRI_IN_WORLD);
        graphBuilder.addSensor(FRISensors.IS_BEST_WORLD_FRI_IN_RANGE);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addCombatPackage(Graph.Builder<Marine> graphBuilder) {
        // The goal we want to complete.
        graphBuilder.addGoal(CombatGoals.HAS_WEAPON_GOAL);
        graphBuilder.addGoal(CombatGoals.NO_ATTACK_TARGET_GOAL);

        // Actions that can complete the goal.
        graphBuilder.addAction(CombatActions.MOVE_TO_BEST_WEAPON);
        graphBuilder.addAction(CombatActions.pickUpBestWeaponFactory());
        graphBuilder.addAction(CombatActions.equipBestWeaponFactory());
        graphBuilder.addAction(CombatActions.MOVE_UNTIL_ATTACK_TARGET_IN_RANGE_FOR_EQUIPPED_BEST_WEAPON_ACTION);
        graphBuilder.addAction(CombatActions.USE_BEST_WEAPON);

        // Used for sensing attackable targets.
        // ⚠ Oct 5 - stage 2: nearest-first, capped and briefly remembered (MarineTargetScan). The game rule off is the
        // old scan, exactly: every candidate, every tick, sight-checked.
        graphBuilder.addSensor(
            com.just.ai.goap.sensor.Sensors.<Marine, List<LivingEntity>>map(
                GOAPSensors.NEARBY_ATTACKABLE_TARGETS_KEY,
                marine -> marine.level()
                    .getGameRules()
                    .getBoolean(com.human.common.registry.init.HumanGameRules.MARINE_TARGET_SCAN_CAP)
                        ? marine.getTargetScan().sense(marine, (scanner, candidate) -> isAThreat(scanner, candidate, false))
                        : scanEverythingEveryTick(marine)
            )
        );
        // Used for sensing attackable targets in a sorted order based on distance.
        // Oct 5 squad tactics - the same key, ordered the squad's way (MarineSquads.prioritise): cover requests, the
        // big
        // threat, then distance with crowded small targets pushed back. NEAREST_ATTACKABLE_TARGET takes the first.
        graphBuilder.addSensor(
            com.just.ai.goap.sensor.Sensors.<Marine, List<LivingEntity>, List<LivingEntity>>compose(
                GOAPSensors.NEARBY_ATTACKABLE_TARGETS_KEY,
                GOAPSensors.NEAREST_ATTACKABLE_TARGETS.key(),
                (marine, targets) -> com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads.prioritise(marine, targets)
            )
        );
        // Used for picking out the closest attackable target.
        graphBuilder.addSensor(GOAPSensors.NEAREST_ATTACKABLE_TARGET);
        // Used for locating best weapon.
        graphBuilder.addSensor(CombatSensors.BEST_WEAPON);
        graphBuilder.addSensor(CombatSensors.BEST_WEAPON_LOCATION);
        // Used for locating best weapon on self.
        graphBuilder.addSensor(CombatSensors.BEST_WEAPON_IN_HANDS);
        graphBuilder.addSensor(CombatSensors.BEST_WEAPON_IN_INVENTORY);
        // Used for locating best weapon in world.
        graphBuilder.addSensor(CombatSensors.BEST_WEAPON_IN_WORLD);
        graphBuilder.addSensor(CombatSensors.IS_BEST_WORLD_WEAPON_IN_RANGE);
        // Used for checking if entity has an attack target.
        graphBuilder.addSensor(GOAPSensors.HAS_ATTACK_TARGET);
        // Used for checking if the entity has a weapon (either in their inventory or in their hands).
        graphBuilder.addSensor(CombatSensors.HAS_WEAPON);
        // Used for checking if the attack target is in range of the agent's currently equipped best weapon.
        graphBuilder.addSensor(CombatSensors.IS_ATTACK_TARGET_IN_RANGE_OF_EQUIPPED_BEST_WEAPON);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addFallBackPackage(Graph.Builder<Marine> graphBuilder) {
        // The goal we want to complete.
        graphBuilder.addGoal(FallBackGoals.FALL_BACK_GOAL);

        // Actions that can complete the goal.
        graphBuilder.addAction(FallBackActions.FALL_BACK_FROM_ATTACK_TARGET);

        // Read out of the world state by the composite below. Sensing is demand-driven and memoised, so the order
        // here is documentation rather than a dependency.
        graphBuilder.addSensor(FallBackSensors.IS_LOW_ON_AMMUNITION);
        graphBuilder.addSensor(FallBackSensors.IS_ATTACK_TARGET_CHARGING);
        graphBuilder.addSensor(FallBackSensors.IS_AT_SAFE_DISTANCE);
        graphBuilder.addSensor(FallBackSensors.IS_ATTACK_TARGET_TOUGH);
        graphBuilder.addSensor(FallBackSensors.SHOULD_WITHDRAW);
        graphBuilder.addSensor(FallBackSensors.IS_FALLING_BACK);
        // Used for deciding whether giving ground is worth planning for at all.
        graphBuilder.addSensor(FallBackSensors.SHOULD_FALL_BACK);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addStandoffPackage(Graph.Builder<Marine> graphBuilder) {
        // The goal we want to complete.
        graphBuilder.addGoal(StandoffGoals.MAINTAIN_STANDOFF_GOAL);

        // Actions that can complete the goal.
        graphBuilder.addAction(StandoffActions.MAINTAIN_STANDOFF);

        // Read out of the world state by the composite below. Sensing is demand-driven and memoised, so the order
        // here is documentation rather than a dependency.
        graphBuilder.addSensor(StandoffSensors.IS_TARGET_INSIDE_STANDOFF);
        graphBuilder.addSensor(StandoffSensors.IS_AT_STANDOFF_DISTANCE);
        // Used for deciding whether holding distance is worth planning for at all.
        graphBuilder.addSensor(StandoffSensors.SHOULD_MAINTAIN_STANDOFF);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addSentryPackage(Graph.Builder<Marine> graphBuilder) {
        // The goal we want to complete.
        graphBuilder.addGoal(SentryGoals.RETURN_TO_POST_GOAL);

        // Actions that can complete the goal.
        graphBuilder.addAction(SentryActions.RETURN_TO_POST);

        // Used for checking whether the marine is guarding a point, and whether it has strayed off it.
        graphBuilder.addSensor(SentrySensors.IS_SENTRY);
        graphBuilder.addSensor(SentrySensors.IS_AWAY_FROM_POST);
        graphBuilder.addSensor(SentrySensors.IS_AT_POST);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addEquipBestArmorPackage(Graph.Builder<Marine> graphBuilder) {
        graphBuilder.addGoal(EquipArmorGoals.EQUIP_BEST_ARMOR_GOAL);

        graphBuilder.addAction(EquipArmorActions.EQUIP_BEST_ARMOR_PIECES_FROM_INVENTORY_ACTION);

        // Best helmet sensors.
        graphBuilder.addSensor(EquipArmorSensors.BEST_HELMET);
        graphBuilder.addSensor(EquipArmorSensors.BEST_HELMET_IN_WORLD);
        graphBuilder.addSensor(EquipArmorSensors.BEST_HELMET_IN_INVENTORY);
        graphBuilder.addSensor(EquipArmorSensors.BEST_HELMET_EQUIPPED);
        // Best chestplate sensors.
        graphBuilder.addSensor(EquipArmorSensors.BEST_CHESTPLATE);
        graphBuilder.addSensor(EquipArmorSensors.BEST_CHESTPLATE_IN_WORLD);
        graphBuilder.addSensor(EquipArmorSensors.BEST_CHESTPLATE_IN_INVENTORY);
        graphBuilder.addSensor(EquipArmorSensors.BEST_CHESTPLATE_EQUIPPED);
        // Best leggings sensors.
        graphBuilder.addSensor(EquipArmorSensors.BEST_LEGGINGS);
        graphBuilder.addSensor(EquipArmorSensors.BEST_LEGGINGS_IN_WORLD);
        graphBuilder.addSensor(EquipArmorSensors.BEST_LEGGINGS_IN_INVENTORY);
        graphBuilder.addSensor(EquipArmorSensors.BEST_LEGGINGS_EQUIPPED);
        // Best boots sensors.
        graphBuilder.addSensor(EquipArmorSensors.BEST_BOOTS);
        graphBuilder.addSensor(EquipArmorSensors.BEST_BOOTS_IN_WORLD);
        graphBuilder.addSensor(EquipArmorSensors.BEST_BOOTS_IN_INVENTORY);
        graphBuilder.addSensor(EquipArmorSensors.BEST_BOOTS_EQUIPPED);
        // Best armor set sensors.
        graphBuilder.addSensor(EquipArmorSensors.BEST_ARMOR_SET_TARGET);
        graphBuilder.addSensor(EquipArmorSensors.IS_ANY_BEST_ARMOR_SET_PIECE_IN_WORLD);
        graphBuilder.addSensor(EquipArmorSensors.IS_ANY_BEST_ARMOR_SET_PIECE_IN_INVENTORY);
        graphBuilder.addSensor(EquipArmorSensors.ARE_ALL_BEST_ARMOR_SET_PIECES_EQUIPPED);
        // Full set armor sensors.
        graphBuilder.addSensor(EquipArmorSensors.MK50_ARMOR_SET_TARGET);
        graphBuilder.addSensor(EquipArmorSensors.NETHER_CHITIN_ARMOR_SET_TARGET);
        graphBuilder.addSensor(EquipArmorSensors.PLATED_NETHER_CHITIN_ARMOR_SET_TARGET);
        graphBuilder.addSensor(EquipArmorSensors.PRESSURE_SUIT_ARMOR_SET_TARGET);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addExtinguishSelfPackage(Graph.Builder<Marine> graphBuilder) {
        graphBuilder.addGoal(ExtinguishFireGoals.EXTINGUISH_SELF_GOAL);

        graphBuilder.addAction(ExtinguishFireActions.MOVE_TO_WATER_BUCKET);
        graphBuilder.addAction(ExtinguishFireActions.pickUpWaterBucketFactory());
        graphBuilder.addAction(ExtinguishFireActions.EQUIP_WATER_BUCKET_ACTION);
        graphBuilder.addAction(ExtinguishFireActions.PLACE_WATER_AT_FEET_ACTION);

        graphBuilder.addSensor(ExtinguishFireSensors.HAS_WATER_BUCKET_EQUIPPED);
        graphBuilder.addSensor(MarineGOAPSensors.IS_IN_ULTRA_WARM_DIMENSION);
        graphBuilder.addSensor(ExtinguishFireSensors.SHOULD_USE_BUCKET_ON_FIRE);
        graphBuilder.addSensor(ExtinguishFireSensors.WATER_BUCKET_IN_INVENTORY);
        graphBuilder.addSensor(ExtinguishFireSensors.HAS_WATER_BUCKET_IN_INVENTORY);
        graphBuilder.addSensor(ExtinguishFireSensors.NEAREST_WATER_BUCKET_IN_WORLD);
        graphBuilder.addSensor(ExtinguishFireSensors.HAS_WATER_BUCKET_IN_WORLD);
        graphBuilder.addSensor(ExtinguishFireSensors.IS_NEAREST_WATER_BUCKET_IN_RANGE);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addSatisfyBoredomPackage(Graph.Builder<Marine> graphBuilder) {
        // The goal we want to complete.
        graphBuilder.addGoal(IdleGoals.SATISFY_BOREDOM_GOAL);

        // Actions that can complete the goal.
        graphBuilder.addAction(IdleActions.WANDER_ACTION);

        // [stated] Oct 4: an idle marine in water heads for shore and waits there.
        graphBuilder.addGoal(IdleGoals.GET_OUT_OF_WATER_GOAL);
        graphBuilder.addAction(IdleActions.GET_OUT_OF_WATER_ACTION);
        graphBuilder.addSensor(IdleSensors.IS_IN_WATER);
        graphBuilder.addSensor(IdleSensors.IS_CLEAR_TO_LEAVE_WATER);

        // [stated] Oct 5: an idle camp marine that has strayed walks back to its camp.
        graphBuilder.addGoal(IdleGoals.RETURN_HOME_GOAL);
        graphBuilder.addAction(IdleActions.RETURN_HOME_ACTION);
        graphBuilder.addSensor(IdleSensors.IS_AWAY_FROM_HOME);
        graphBuilder.addSensor(IdleSensors.IS_CLEAR_TO_GO_HOME);

        // Used for preventing wandering if marine has a leader.
        graphBuilder.addSensor(FollowLeaderSensors.HAS_LEADER);
        // Used for determining when the marine should wander around.
        graphBuilder.addSensor(IdleSensors.IS_BORED);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addStayCloseToLeaderPackage(Graph.Builder<Marine> graphBuilder) {
        // The goal we want to complete.
        graphBuilder.addGoal(FollowLeaderGoals.STAY_CLOSE_TO_LEADER_GOAL);

        // Actions that can complete the goal.
        graphBuilder.addAction(FollowLeaderActions.MOVE_CLOSER_TO_LEADER_ACTION);

        // Used for determining if the marine is able to follow the leader.
        graphBuilder.addSensor(FollowLeaderSensors.CAN_FOLLOW_LEADER);
        // Used for determining if the marine has a leader to follow.
        graphBuilder.addSensor(FollowLeaderSensors.HAS_LEADER);
        graphBuilder.addSensor(FollowLeaderSensors.HAS_PLAYER_LEADER);
        // Used for determining if the marine is too far away from the leader.
        graphBuilder.addSensor(FollowLeaderSensors.IS_CLOSE_TO_LEADER);

        return graphBuilder;
    }

    /**
     * Freeing a squadmate from avp_predator's net. [stated] "if a marine is netted and there are other marines around
     * they will move to the netted marine and cut them free with the combat knife." Every sensor answers false without
     * avp_predator installed, so the goal is simply never live there.
     */
    private static Graph.Builder<Marine> addCutFreePackage(Graph.Builder<Marine> graphBuilder) {
        graphBuilder.addGoal(CutFreeGoals.CUT_FREE_NETTED_MARINE_GOAL);

        graphBuilder.addAction(CutFreeActions.EQUIP_BLADE);
        graphBuilder.addAction(CutFreeActions.MOVE_TO_NETTED_MARINE);
        graphBuilder.addAction(CutFreeActions.CUT_FREE);

        graphBuilder.addSensor(CutFreeSensors.NEAREST_NETTED_MARINE);
        graphBuilder.addSensor(CutFreeSensors.HAS_NETTED_MARINE_NEARBY);
        graphBuilder.addSensor(CutFreeSensors.IS_NETTED_MARINE_IN_RANGE);
        graphBuilder.addSensor(CutFreeSensors.HAS_BLADE_IN_INVENTORY);
        graphBuilder.addSensor(CutFreeSensors.HAS_BLADE_EQUIPPED);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addTameWolfPackage(Graph.Builder<Marine> graphBuilder) {
        // Goals for taming wolves and collecting bones.
        graphBuilder.addGoal(TameWolfGoals.TAME_WOLF_GOAL);
        graphBuilder.addGoal(TameWolfGoals.COLLECT_BONES_GOAL);

        // Actions for taming wolves.
        graphBuilder.addAction(TameWolfActions.MOVE_TO_BONE);
        graphBuilder.addAction(TameWolfActions.pickUpBoneFactory());
        graphBuilder.addAction(TameWolfActions.equipBoneFactory());
        graphBuilder.addAction(TameWolfActions.MOVE_TO_WOLF);
        graphBuilder.addAction(TameWolfActions.USE_BONE_ON_WOLF);

        // Wolf detection sensors.
        graphBuilder.addSensor(TameWolfSensors.NEAREST_UNTAMED_WOLF);
        graphBuilder.addSensor(TameWolfSensors.HAS_UNTAMED_WOLF_NEARBY);
        graphBuilder.addSensor(TameWolfSensors.IS_WOLF_IN_RANGE);
        // Bone detection sensors.
        graphBuilder.addSensor(TameWolfSensors.NEAREST_BONE_IN_WORLD);
        graphBuilder.addSensor(TameWolfSensors.HAS_BONE_IN_WORLD);
        graphBuilder.addSensor(TameWolfSensors.IS_NEAREST_BONE_IN_RANGE);
        graphBuilder.addSensor(TameWolfSensors.BONE_COUNT_IN_INVENTORY);
        graphBuilder.addSensor(TameWolfSensors.HAS_BONE_IN_INVENTORY);
        graphBuilder.addSensor(TameWolfSensors.HAS_BONE_EQUIPPED);
        // Bone collection sensor.
        graphBuilder.addSensor(TameWolfSensors.SHOULD_COLLECT_MORE_BONES);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addPlaceTorchPackage(Graph.Builder<Marine> graphBuilder) {
        // Goals for lighting dark areas and collecting torches.
        graphBuilder.addGoal(TorchGoals.LIGHT_DARK_AREA_GOAL);
        graphBuilder.addGoal(TorchGoals.COLLECT_TORCHES_GOAL);

        // Actions for handling torches.
        graphBuilder.addAction(TorchActions.MOVE_TO_TORCH);
        graphBuilder.addAction(TorchActions.pickUpTorchFactory());
        graphBuilder.addAction(TorchActions.equipTorchFactory());
        graphBuilder.addAction(TorchActions.PLACE_TORCH);

        // Torch detection sensors.
        graphBuilder.addSensor(TorchSensors.NEAREST_TORCH_IN_WORLD);
        graphBuilder.addSensor(TorchSensors.HAS_TORCH_IN_WORLD);
        graphBuilder.addSensor(TorchSensors.IS_NEAREST_TORCH_IN_RANGE);
        graphBuilder.addSensor(TorchSensors.TORCH_COUNT_IN_INVENTORY);
        graphBuilder.addSensor(TorchSensors.HAS_TORCH_IN_INVENTORY);
        graphBuilder.addSensor(TorchSensors.HAS_TORCH_EQUIPPED);
        graphBuilder.addSensor(TorchSensors.SHOULD_COLLECT_MORE_TORCHES);
        // Torch placement sensors.
        graphBuilder.addSensor(TorchSensors.IS_IN_DARK_AREA);
        graphBuilder.addSensor(TorchSensors.CAN_PLACE_TORCH_AT_FEET);
        graphBuilder.addSensor(TorchSensors.TORCH_WALL_PLACEMENT_POS);
        graphBuilder.addSensor(TorchSensors.CAN_PLACE_TORCH_ON_WALL);
        graphBuilder.addSensor(TorchSensors.SHOULD_PLACE_TORCH);

        return graphBuilder;
    }

    /** Oct 5 - dropped ammunition the marine's guns take (collect_ammo). */
    private static Graph.Builder<Marine> addCollectAmmoPackage(Graph.Builder<Marine> graphBuilder) {
        graphBuilder.addGoal(com.human.common.gameplay.entity.living.human.marine.ai.collect_ammo.AmmoGoals.COLLECT_AMMO_GOAL);

        graphBuilder.addAction(com.human.common.gameplay.entity.living.human.marine.ai.collect_ammo.AmmoActions.MOVE_TO_AMMO);
        graphBuilder.addAction(com.human.common.gameplay.entity.living.human.marine.ai.collect_ammo.AmmoActions.PICK_UP_AMMO);

        graphBuilder.addSensor(
            com.human.common.gameplay.entity.living.human.marine.ai.collect_ammo.AmmoSensors.NEAREST_USABLE_AMMO_IN_WORLD
        );
        graphBuilder.addSensor(com.human.common.gameplay.entity.living.human.marine.ai.collect_ammo.AmmoSensors.HAS_USABLE_AMMO_IN_WORLD);
        graphBuilder.addSensor(com.human.common.gameplay.entity.living.human.marine.ai.collect_ammo.AmmoSensors.IS_NEAREST_AMMO_IN_RANGE);
        graphBuilder.addSensor(com.human.common.gameplay.entity.living.human.marine.ai.collect_ammo.AmmoSensors.SHOULD_COLLECT_AMMO);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addEquipTotemPackage(Graph.Builder<Marine> graphBuilder) {
        // Goal for equipping totem when near death.
        graphBuilder.addGoal(TotemGoals.EQUIP_TOTEM_WHEN_NEAR_DEATH_GOAL);
        // Goal for always keeping at least one totem in inventory.
        graphBuilder.addGoal(TotemGoals.COLLECT_TOTEM_GOAL);

        // Actions for handling totems.
        graphBuilder.addAction(TotemActions.MOVE_TO_TOTEM);
        graphBuilder.addAction(TotemActions.pickUpTotemFactory());
        graphBuilder.addAction(TotemActions.equipTotemFactory());

        // Totem detection sensors.
        graphBuilder.addSensor(TotemSensors.NEAREST_TOTEM_IN_WORLD);
        graphBuilder.addSensor(TotemSensors.HAS_TOTEM_IN_WORLD);
        graphBuilder.addSensor(TotemSensors.IS_NEAREST_TOTEM_IN_RANGE);
        graphBuilder.addSensor(TotemSensors.TOTEM_COUNT_IN_INVENTORY);
        graphBuilder.addSensor(TotemSensors.HAS_TOTEM_IN_INVENTORY);
        graphBuilder.addSensor(TotemSensors.HAS_TOTEM_EQUIPPED);
        graphBuilder.addSensor(TotemSensors.SHOULD_COLLECT_TOTEM);
        // Health-based sensors.
        graphBuilder.addSensor(TotemSensors.IS_NEAR_DEATH);
        graphBuilder.addSensor(TotemSensors.SHOULD_EQUIP_TOTEM);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addBreakFallPackage(Graph.Builder<Marine> graphBuilder) {
        // Goals for breaking falls and cleaning up water.
        graphBuilder.addGoal(BreakFallGoals.BREAK_FALL_GOAL);

        // Actions for breaking falls.
        graphBuilder.addAction(ExtinguishFireActions.EQUIP_WATER_BUCKET_ACTION);
        graphBuilder.addAction(BreakFallActions.PLACE_WATER_TO_BREAK_FALL);

        // Fall detection sensors.
        graphBuilder.addSensor(BreakFallSensors.IS_FALLING);
        graphBuilder.addSensor(BreakFallSensors.FALL_DISTANCE);
        graphBuilder.addSensor(BreakFallSensors.WILL_TAKE_FALL_DAMAGE);
        graphBuilder.addSensor(BreakFallSensors.LANDING_BLOCK_POS);
        graphBuilder.addSensor(BreakFallSensors.WILL_LAND_IN_FLUID);
        graphBuilder.addSensor(BreakFallSensors.CAN_PLACE_WATER_AT_LANDING);
        graphBuilder.addSensor(BreakFallSensors.IS_CLOSE_TO_LANDING);
        // Water bucket sensors.
        graphBuilder.addSensor(ExtinguishFireSensors.WATER_BUCKET_IN_INVENTORY);
        graphBuilder.addSensor(ExtinguishFireSensors.HAS_WATER_BUCKET_IN_INVENTORY);
        graphBuilder.addSensor(ExtinguishFireSensors.HAS_WATER_BUCKET_EQUIPPED);
        // Combined sensors.
        graphBuilder.addSensor(BreakFallSensors.SHOULD_BREAK_FALL);

        return graphBuilder;
    }

    private static Graph.Builder<Marine> addHealSelfPackage(Graph.Builder<Marine> graphBuilder) {
        // Goal for healing when health is low.
        graphBuilder.addGoal(HealingGoals.HEAL_SELF_GOAL);

        // Actions for healing.
        graphBuilder.addAction(HealingActions.MOVE_TO_BEST_HEALING_ITEM);
        graphBuilder.addAction(HealingActions.pickUpBestHealingItemFactory());
        graphBuilder.addAction(HealingActions.equipBestHealingItemFactory());
        graphBuilder.addAction(HealingActions.USE_BEST_HEALING_ITEM);

        // Best healing item sensors.
        graphBuilder.addSensor(HealingSensors.BEST_HEALING_ITEM);
        graphBuilder.addSensor(HealingSensors.BEST_HEALING_ITEM_LOCATION);
        // Best healing item on self.
        graphBuilder.addSensor(HealingSensors.BEST_HEALING_ITEM_IN_HANDS);
        graphBuilder.addSensor(HealingSensors.BEST_HEALING_ITEM_IN_INVENTORY);
        // Best healing item in world.
        graphBuilder.addSensor(HealingSensors.BEST_HEALING_ITEM_IN_WORLD);
        graphBuilder.addSensor(HealingSensors.IS_BEST_WORLD_HEALING_ITEM_IN_RANGE);

        return graphBuilder;
    }

    /** True once every {@code period} ticks, offset by the marine's id so marines spread across ticks. */
    /** Oct 7 - the last tick each marine replanned because it was hurt (server thread; dies with the marine). */
    private static final java.util.Map<Marine, Integer> LAST_DAMAGE_REPLAN = new java.util.WeakHashMap<>();

    /** Oct 7 - ordinary hits replan at most this often. */
    private static final int DAMAGE_REPLAN_INTERVAL_TICKS = 10;

    /**
     * Oct 7 - /blib perf: a marine in a fight cost 702 us per tick, 175 us of it planning and 212 us sensors - under
     * steady fire EVERY hit forced a full replan, and each replan re-reads every goal's sensors. Hits now replan at
     * most every {@link #DAMAGE_REPLAN_INTERVAL_TICKS} ticks; a heavy hit (15% of max health or more in one tick) or
     * health under 30% always replans at once, so a marine in real trouble still reacts immediately. The 1-second
     * periodic replan still runs regardless.
     */
    private static boolean mayReplanForDamage(
        com.just.ai.goap.plan.ReplanPolicy.Context<Marine> context,
        float currentHealthRatio,
        float previousHealthRatio
    ) {
        var marine = context.agent().getActor();

        if (marine.level().isClientSide()) {
            return true;
        }

        var urgent = previousHealthRatio - currentHealthRatio >= 0.15F || currentHealthRatio < 0.3F;
        var last = LAST_DAMAGE_REPLAN.get(marine);

        if (!urgent && last != null && marine.tickCount - last < DAMAGE_REPLAN_INTERVAL_TICKS) {
            return false;
        }

        LAST_DAMAGE_REPLAN.put(marine, marine.tickCount);
        return true;
    }

    private static boolean staggeredTick(com.just.ai.goap.plan.ReplanPolicy.Context<Marine> context, int period) {
        var marine = context.agent().getActor();
        return Math.floorMod(marine.tickCount + marine.getId(), period) == 0;
    }

    /**
     * [stated] Oct 5 performance review: whether a marine may START an engagement with this entity. It must be able to
     * SEE it - the scan box is 80 blocks, and without this marines locked onto mobs in caves below them and behind
     * walls, then pathed at them every tick and aimed into the ground. Its current target stays eligible, so a target
     * that steps behind a wall is chased rather than forgotten. Self defence never needs sight (see
     * {@link #isAThreat}). Sensing caches line of sight for the tick, as vanilla's own goals rely on.
     */
    private static boolean canEngage(Marine marine, LivingEntity livingEntity, boolean checkSight) {
        return !checkSight || livingEntity == marine.getTarget() || marine.getSensing().hasLineOfSight(livingEntity);
    }

    /** The pre-stage-2 sensor, kept verbatim for {@code /gamerule humanMarineTargetScanCap false}. */
    private static List<LivingEntity> scanEverythingEveryTick(Marine marine) {
        return marine.getEntitySenseCache()
            .getByClass(LivingEntity.class)
            .stream()
            .filter(
                livingEntity -> BLibEntityPredicates.isAlive(livingEntity)
                    && isAThreat(marine, livingEntity, true)
                    && marine.getSensing().hasLineOfSight(livingEntity)
            )
            .toList();
    }

    /** Something the marine goes after on its own: a hated mob, or whatever its leader is fighting. */
    private static boolean isHunted(Marine marine, LivingEntity livingEntity) {
        // Oct 5 - the hated tag OR any monster or boss, from any mod, minus the exclusions in MarineHuntRules.
        return com.human.common.gameplay.entity.living.human.marine.MarineHuntRules.isHuntedOnSight(livingEntity)
            || shouldAttackLeaderTarget(marine, livingEntity);
    }

    /**
     * @param checkSight {@code false} when the caller does the sight check itself (MarineTargetScan does it
     *                   nearest-first and capped); every other rule is the same either way
     */
    private static boolean isAThreat(Marine marine, LivingEntity livingEntity, boolean checkSight) {
        if (
            BLibEntityPredicates.isInvulnerable(livingEntity)
                || Objects.equals(marine.getUUID(), livingEntity.getUUID())
                || MarineAllyUtil.isMarineAlly(marine, livingEntity)
                // The leader is never a target, whatever the filter says. That has to sit above the filter rather than
                // inside it, or a blacklist with nothing ticked would turn a squad on its own employer.
                // ⚠ ALLEGIANCE, not the employer: a patrol marine must not shoot its own squad leader either.
                || marine.getAllegianceUUID().isSomeAnd(livingEntity.getUUID()::equals)
                // Oct 5 - the alien truce: no yautja is a threat while it holds, not even one whose stray blow just
                // landed. Above the retaliation and callout checks on purpose.
                || com.human.common.gameplay.entity.living.human.marine.MarineAlienTruce.spares(marine, livingEntity)
        ) {
            return false;
        }

        // Oct 5 audit - a target this marine chased for five seconds without getting closer is left alone for ten,
        // unless it is the one hitting it. Otherwise the same unreachable mob was picked again the next tick.
        if (marine.isGivenUpOn(livingEntity) && !shouldRetaliateAgainstLastAttacker(marine, livingEntity)) {
            return false;
        }

        // Oct 5 squad tactics - whatever hit a squadmate nearby is a threat to this marine too (a callout). Like being
        // hit
        // itself, that also reveals a cloaked attacker.
        // Oct 5 audit - a marine with no usable gun never closes on a creeper; it walks away from it instead
        // (Marine.evadeCreeperWhenUnarmed). Review pass 1 - checked BEFORE the callout, so a knife-only marine does not
        // charge a creeper a squadmate called out.
        if (livingEntity instanceof net.minecraft.world.entity.monster.Creeper && marine.isOutOfAmmunition()) {
            return false;
        }

        if (com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads.isCalledOut(marine, livingEntity)) {
            return true;
        }

        // ⚠⚠ Oct 5 - A CLOAKED YAUTJA IS INVISIBLE TO MARINES UNTIL IT STRIKES. [stated] "marines should hunt yautja
        // but
        // only if it attacks them ... its when it targets them if its not cloaked or on first strike" and "clocking
        // hides
        // the yautja fron sentry turrets (unless they have the em lens) and marines". This sensor never went through
        // vanilla's TargetingConditions, which is the only place the cloak hides anything, so a marine saw straight
        // through it. Concealed now means: no hunting, no "it is targeting me / my squad" - only retaliation against
        // the thing that just hit this marine (vanilla forgets the attacker 5 seconds after the last hit).
        if (isConcealedFrom(marine, livingEntity)) {
            return shouldRetaliateAgainstLastAttacker(marine, livingEntity);
        }

        if (marine.isSentry()) {
            // ⚠ The filter chooses what a sentry HUNTS. It does not choose whether it defends itself.
            //
            // Self defence and protecting the leader are kept, because the alternative is a marine that stands still
            // while something eats it - which no player reads as "configured correctly", they read it as broken. What
            // IS dropped for a sentry is the HATED_BY_MARINES tag, now the filter's business, and following the
            // leader's fire: punching a cow next to a sentry told to leave animals alone must not get the cow shot,
            // and that case is the whole reason the filter exists.
            return (marine.getSentryFilter().shouldEngage(livingEntity) && canEngage(marine, livingEntity, checkSight))
                || shouldRetaliateAgainstLastAttacker(marine, livingEntity)
                || shouldProtectSelfOrAllies(marine, livingEntity);
        }

        // Self defence first and without needing sight; hunting (hated mobs, the leader's target) needs sight.
        return shouldRetaliateAgainstLastAttacker(marine, livingEntity)
            || shouldProtectSelfOrAllies(marine, livingEntity)
            || (isHunted(marine, livingEntity) && canEngage(marine, livingEntity, checkSight));
    }

    private static boolean shouldAttackLeaderTarget(Marine marine, LivingEntity livingEntity) {
        var leaderOption = marine.getLeader();

        if (!leaderOption.isSome()) {
            return false;
        }

        var leader = leaderOption.unwrap();

        if (!(leader instanceof LivingEntity livingLeader)) {
            return false;
        }

        var leaderLastTarget = livingLeader.getLastHurtMob();

        // Was the mob hurt by my leader?
        return leaderLastTarget != null
            && Objects.equals(leaderLastTarget.getUUID(), livingEntity.getUUID());
    }

    private static boolean shouldProtectSelfOrAllies(Marine marine, LivingEntity livingEntity) {
        if (!(livingEntity instanceof Mob mob)) {
            return false;
        }

        var mobTarget = mob.getTarget();

        if (mobTarget == null) {
            return false;
        }

        var leaderUUIDOption = marine.getAllegianceUUID();

        // Is the mob targeting me?
        return Objects.equals(mobTarget.getUUID(), marine.getUUID())
            // OR is the mob targeting my leader?
            || leaderUUIDOption.isSomeAnd(mobTarget.getUUID()::equals)
            // OR is the mob targeting an ally?
            || MarineAllyUtil.isMarineAlly(marine, mobTarget)
            // Oct 5 - tactic 9: OR is it going after a civilian - a villager, a colonist or other friendly human, or a
            // player who is not playing creative.
            || (isCivilian(marine, mobTarget) && !isLawfulDefender(mob));
    }

    private static boolean isCivilian(Marine marine, LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.npc.AbstractVillager
            || (entity instanceof com.human.common.gameplay.entity.living.human.AbstractHuman human
                && !(human instanceof Marine)
                && !com.human.common.gameplay.entity.living.human.marine.MarineHostility.hasBrokenAlliance(marine, human))
            || (entity instanceof net.minecraft.world.entity.player.Player player
                && !player.isCreative() && !player.isSpectator());
    }

    /**
     * Final review (enemies) - attackers the marines never turn on to protect a civilian: an iron golem (it only goes
     * after a player who attacked the village) and anyone's tamed or owned animal. Marines already hunt every hostile
     * mob on sight, so tactic 9 matters for what they would not otherwise shoot - and these two would have turned it
     * into marines killing a village's golem because a player punched a villager.
     */
    private static boolean isLawfulDefender(Mob mob) {
        return mob instanceof net.minecraft.world.entity.animal.IronGolem
            || (mob instanceof net.minecraft.world.entity.OwnableEntity ownable && ownable.getOwnerUUID() != null);
    }

    /**
     * Whether {@code livingEntity} is hidden from {@code marine} by a cloak.
     * <p>
     * ⭐ DEPENDENCY-FREE. avp_predator's cloak makes {@code getVisibilityPercent} return exactly 0 for a concealed
     * wearer (its MixinLivingEntity_CloakVisibility), and nothing in vanilla ever reaches 0 - an invisibility potion
     * bottoms out at 0.07 - so "zero" means "cloaked" without avp_human naming a single avp_predator class. Without
     * avp_predator installed this is always false.
     * </p>
     */
    public static boolean isConcealedFrom(LivingEntity viewer, LivingEntity livingEntity) {
        return livingEntity.getVisibilityPercent(viewer) <= 0.0;
    }

    private static boolean shouldRetaliateAgainstLastAttacker(Marine marine, LivingEntity livingEntity) {
        var lastAttacker = marine.getLastHurtByMob();

        return lastAttacker != null
            // AND the last attacker is not our leader...
            && !Objects.equals(lastAttacker.getUUID(), marine.getAllegianceUUID().unwrapOr(null))
            // AND the current entity we are checking is our last attacker...
            && Objects.equals(lastAttacker.getUUID(), livingEntity.getUUID());
    }

    public static void initialize() {
        // Oct 5 - registers the marine game rules during mod initialisation, before any world exists.
        com.human.common.registry.init.HumanGameRules.initialize();
    }

    private MarineGOAP() {
        throw new UnsupportedOperationException();
    }
}
