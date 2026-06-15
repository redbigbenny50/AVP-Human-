package com.human.common.gameplay.entity.living.human.marine.ai;

import com.blib.api.common.entity.v1.BLibEntityPredicates;
import com.blib.api.common.goap.v1.GOAPSensors;
import com.human.common.gameplay.entity.ai.goap.HumanGOAPSensors;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.FRIActions;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.FRIGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.FRISensors;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.BreakFallActions;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.BreakFallGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.BreakFallSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatActions;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.EquipArmorActions;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.EquipArmorGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.EquipArmorSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_totem.TotemActions;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_totem.TotemGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_totem.TotemSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire.ExtinguishFireActions;
import com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire.ExtinguishFireGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire.ExtinguishFireSensors;
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
import com.human.common.gameplay.entity.living.human.marine.ai.tame_wolf.TameWolfActions;
import com.human.common.gameplay.entity.living.human.marine.ai.tame_wolf.TameWolfGoals;
import com.human.common.gameplay.entity.living.human.marine.ai.tame_wolf.TameWolfSensors;
import com.human.common.registry.tag.HumanEntityTypeTags;
import com.just.ai.goap.Agent;
import com.just.ai.goap.graph.Graph;
import com.just.ai.goap.plan.ReplanPolicies;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Objects;

public class MarineGOAP {

    public static final Graph<Marine> GRAPH = Graph.<Marine>builder()
        .apply(MarineGOAP::addSensorsPackage)
        .apply(MarineGOAP::addAcquireFireResistancePackage)
        .apply(MarineGOAP::addCombatPackage)
        .apply(MarineGOAP::addEquipBestArmorPackage)
        .apply(MarineGOAP::addExtinguishSelfPackage)
        .apply(MarineGOAP::addSatisfyBoredomPackage)
        .apply(MarineGOAP::addStayCloseToLeaderPackage)
        .apply(MarineGOAP::addTameWolfPackage)
        .apply(MarineGOAP::addPlaceTorchPackage)
        .apply(MarineGOAP::addEquipTotemPackage)
        .apply(MarineGOAP::addBreakFallPackage)
        .apply(MarineGOAP::addHealSelfPackage)
        .build();

    public static Agent.Builder<Marine> applyAgentProperties(Agent.Builder<Marine> agentBuilder) {
        return agentBuilder.withReplanPolicy(
            ReplanPolicies.anyOf(
                ReplanPolicies.ifNoActivePlans(),
                // Replan every 20 ticks (every 1 second).
                ReplanPolicies.custom(context -> context.agent().getActor().tickCount % 20 == 0),
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
                    return currentHealthRatio < previousHealthRatio;
                })
            )
        );
    }

    private static Graph.Builder<Marine> addSensorsPackage(Graph.Builder<Marine> graphBuilder) {
        // Environment.
        graphBuilder.addSensor(GOAPSensors.NEARBY_BLOCK_POSITIONS);
        graphBuilder.addSensor(MarineGOAPSensors.IS_CURRENT_BLOCK_POS_REPLACEABLE);
        graphBuilder.addSensor(HumanGOAPSensors.IS_NEAR_RADIOACTIVE_BIOME);
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
        graphBuilder.addSensor(
            GOAPSensors.nearbyAttackableTargetsFactory(
                (marine, livingEntity) -> isAThreat(marine, livingEntity)
                    && marine.getSensing().hasLineOfSight(livingEntity)
            )
        );
        // Used for sensing attackable targets in a sorted order based on distance.
        graphBuilder.addSensor(GOAPSensors.NEAREST_ATTACKABLE_TARGETS);
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

    private static boolean isAThreat(Marine marine, LivingEntity livingEntity) {
        if (
            BLibEntityPredicates.isInvulnerable(livingEntity)
                || Objects.equals(marine.getUUID(), livingEntity.getUUID())
        ) {
            return false;
        }

        return livingEntity.getType().is(HumanEntityTypeTags.HATED_BY_MARINES)
            || shouldRetaliateAgainstLastAttacker(marine, livingEntity)
            || shouldProtectSelfOrAllies(marine, livingEntity)
            || shouldAttackLeaderTarget(marine, livingEntity);
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

        var leaderUUIDOption = marine.getLeaderUUID();

        // Is the mob targeting me?
        return Objects.equals(mobTarget.getUUID(), marine.getUUID())
            // OR is the mob targeting my leader?
            || leaderUUIDOption.isSomeAnd(mobTarget.getUUID()::equals)
            // OR is the mob targeting an ally?
            // (an ally is defined as another marine with the same leader status (no leader or same leader).
            || (mobTarget instanceof Marine otherMarine
                && Objects.equals(otherMarine.getLeaderUUID(), leaderUUIDOption));
    }

    private static boolean shouldRetaliateAgainstLastAttacker(Marine marine, LivingEntity livingEntity) {
        var lastAttacker = marine.getLastHurtByMob();

        return lastAttacker != null
            // AND the last attacker is not our leader...
            && !Objects.equals(lastAttacker.getUUID(), marine.getLeaderUUID().unwrapOr(null))
            // AND the current entity we are checking is our last attacker...
            && Objects.equals(lastAttacker.getUUID(), livingEntity.getUUID())
            // AND the current entity is not a fellow marine...
            // TODO: We'll want to do faction checking here in the future.
            && !(livingEntity instanceof Marine);
    }

    public static void initialize() {}

    private MarineGOAP() {
        throw new UnsupportedOperationException();
    }
}
