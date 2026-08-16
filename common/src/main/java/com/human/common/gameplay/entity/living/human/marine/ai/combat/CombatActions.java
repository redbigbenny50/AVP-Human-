package com.human.common.gameplay.entity.living.human.marine.ai.combat;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.human.common.gameplay.entity.living.human.ai.generic.action.MoveToTargetAction;
import com.human.common.gameplay.entity.living.human.ai.generic.action.PickUpNearbyItemAction;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.action.EquipWeaponAction;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.action.MoveUntilAttackTargetInRangeForEquippedBestWeaponAction;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.action.UseWeaponAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class CombatActions {

    public static final Action<PathfinderMob> MOVE_TO_BEST_WEAPON = BLibAction.<PathfinderMob>builder("MoveToBestWeaponAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(CombatSensors.BEST_WEAPON_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.WORLD))
        .addPrecondition(CombatSensors.IS_BEST_WORLD_WEAPON_IN_RANGE.key(), Expressions.Boolean.isFalse())
        .addEffect(CombatSensors.IS_BEST_WORLD_WEAPON_IN_RANGE.key().asDerived(), true)
        .withPerformCallback(
            ctx -> MoveToTargetAction.perform(ctx, CombatSensors.BEST_WEAPON_IN_WORLD.key(), r -> r.itemTarget().itemEntity().position())
        )
        .withFinishCallback(MoveToTargetAction::onFinish)
        .build();

    public static <T extends LivingEntity & BLibInventoryHolder> Action<T> pickUpBestWeaponFactory() {
        return BLibAction.<T>builder("PickUpBestWeaponAction")
            .addPrecondition(CombatSensors.BEST_WEAPON_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.WORLD))
            .addPrecondition(CombatSensors.IS_BEST_WORLD_WEAPON_IN_RANGE.key(), Expressions.Boolean.isTrue())
            .addEffect(CombatSensors.BEST_WEAPON_LOCATION.key().asDerived(), ItemTarget.Location.INVENTORY)
            .addEffect(CombatSensors.HAS_WEAPON.key().asDerived(), true)
            .withPerformCallback(
                ctx -> PickUpNearbyItemAction.perform(ctx, CombatSensors.BEST_WEAPON_IN_WORLD.key(), r -> r.itemTarget().itemEntity())
            )
            .build();
    }

    public static <T extends LivingEntity & BLibInventoryHolder> Action<T> equipBestWeaponFactory() {
        return BLibAction.<T>builder("EquipBestWeaponAction")
            .addMasks(ActionMasks.USE_MAIN_HAND)
            .addPrecondition(CombatSensors.BEST_WEAPON_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.INVENTORY))
            .addEffect(CombatSensors.BEST_WEAPON_LOCATION.key().asDerived(), ItemTarget.Location.EQUIPPED)
            .withPerformCallback(EquipWeaponAction::perform)
            .build();
    }

    public static final Action<PathfinderMob> MOVE_UNTIL_ATTACK_TARGET_IN_RANGE_FOR_EQUIPPED_BEST_WEAPON_ACTION = BLibAction
        .<PathfinderMob>builder("MoveUntilAttackTargetInRangeForEquippedBestWeaponAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(CombatSensors.BEST_WEAPON_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.EQUIPPED))
        .addPrecondition(CombatSensors.IS_ATTACK_TARGET_IN_RANGE_OF_EQUIPPED_BEST_WEAPON.key(), Expressions.Boolean.isFalse())
        .addEffect(CombatSensors.IS_ATTACK_TARGET_IN_RANGE_OF_EQUIPPED_BEST_WEAPON.key().asDerived(), true)
        .withPerformCallback(MoveUntilAttackTargetInRangeForEquippedBestWeaponAction::perform)
        .withFinishCallback(MoveUntilAttackTargetInRangeForEquippedBestWeaponAction::onFinish)
        .build();

    public static final Action<LivingEntity> USE_BEST_WEAPON = BLibAction.<LivingEntity>builder("UseBestWeaponAction")
        .addMasks(ActionMasks.LOOK, ActionMasks.USE_MAIN_HAND)
        .addPrecondition(CombatSensors.BEST_WEAPON_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.EQUIPPED))
        .addPrecondition(CombatSensors.IS_ATTACK_TARGET_IN_RANGE_OF_EQUIPPED_BEST_WEAPON.key(), Expressions.Boolean.isTrue())
        .addEffect(GOAPSensors.HAS_ATTACK_TARGET.key().asDerived(), false)
        .withPerformCallback(UseWeaponAction::perform)
        .withFinishCallback(UseWeaponAction::onFinish)
        .build();

    private CombatActions() {
        throw new UnsupportedOperationException();
    }
}
