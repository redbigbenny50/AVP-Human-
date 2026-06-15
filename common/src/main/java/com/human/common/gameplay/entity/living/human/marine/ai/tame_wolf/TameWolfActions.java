package com.human.common.gameplay.entity.living.human.marine.ai.tame_wolf;

import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.human.common.gameplay.entity.living.human.ai.generic.action.MoveToTargetAction;
import com.human.common.gameplay.entity.living.human.ai.generic.action.PickUpNearbyItemAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.tame_wolf.action.EquipBoneAction;
import com.human.common.gameplay.entity.living.human.marine.ai.tame_wolf.action.UseBoneOnWolfAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.function.Function;

public class TameWolfActions {

    public static final Action<PathfinderMob> MOVE_TO_BONE = BLibAction.<PathfinderMob>builder("MoveToBoneAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(TameWolfSensors.HAS_BONE_IN_WORLD.key(), Expressions.Boolean.isTrue())
        .addPrecondition(TameWolfSensors.IS_NEAREST_BONE_IN_RANGE.key(), Expressions.Boolean.isFalse())
        .addEffect(TameWolfSensors.IS_NEAREST_BONE_IN_RANGE.key().asDerived(), true)
        .withPerformCallback(ctx -> MoveToTargetAction.perform(ctx, TameWolfSensors.NEAREST_BONE_IN_WORLD.key(), ItemEntity::position))
        .withFinishCallback(MoveToTargetAction::onFinish)
        .build();

    public static <T extends LivingEntity & BLibInventoryHolder> Action<T> pickUpBoneFactory() {
        return BLibAction.<T>builder("PickUpBoneAction")
            .addPrecondition(TameWolfSensors.HAS_BONE_IN_WORLD.key(), Expressions.Boolean.isTrue())
            .addPrecondition(TameWolfSensors.IS_NEAREST_BONE_IN_RANGE.key(), Expressions.Boolean.isTrue())
            .addEffect(TameWolfSensors.HAS_BONE_IN_INVENTORY.key().asDerived(), true)
            .withPerformCallback(
                ctx -> PickUpNearbyItemAction.perform(ctx, TameWolfSensors.NEAREST_BONE_IN_WORLD.key(), Function.identity())
            )
            .build();
    }

    public static <T extends LivingEntity & BLibInventoryHolder> Action<T> equipBoneFactory() {
        return BLibAction.<T>builder("EquipBoneAction")
            .addMasks(ActionMasks.USE_MAIN_HAND)
            .addPrecondition(TameWolfSensors.HAS_BONE_IN_INVENTORY.key(), Expressions.Boolean.isTrue())
            .addPrecondition(TameWolfSensors.HAS_BONE_EQUIPPED.key(), Expressions.Boolean.isFalse())
            .addEffect(TameWolfSensors.HAS_BONE_EQUIPPED.key().asDerived(), true)
            .withPerformCallback(EquipBoneAction::perform)
            .build();
    }

    public static final Action<PathfinderMob> MOVE_TO_WOLF = BLibAction.<PathfinderMob>builder("MoveToWolfAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(TameWolfSensors.HAS_UNTAMED_WOLF_NEARBY.key(), Expressions.Boolean.isTrue())
        .addPrecondition(TameWolfSensors.HAS_BONE_EQUIPPED.key(), Expressions.Boolean.isTrue())
        .addPrecondition(TameWolfSensors.IS_WOLF_IN_RANGE.key(), Expressions.Boolean.isFalse())
        .addEffect(TameWolfSensors.IS_WOLF_IN_RANGE.key().asDerived(), true)
        .withPerformCallback(ctx -> MoveToTargetAction.perform(ctx, TameWolfSensors.NEAREST_UNTAMED_WOLF.key(), Wolf::position))
        .withFinishCallback(MoveToTargetAction::onFinish)
        .build();

    public static final Action<Marine> USE_BONE_ON_WOLF = BLibAction.<Marine>builder("UseBoneOnWolfAction")
        .addMasks(ActionMasks.USE_MAIN_HAND, ActionMasks.LOOK)
        .addPrecondition(TameWolfSensors.HAS_UNTAMED_WOLF_NEARBY.key(), Expressions.Boolean.isTrue())
        .addPrecondition(TameWolfSensors.HAS_BONE_EQUIPPED.key(), Expressions.Boolean.isTrue())
        .addPrecondition(TameWolfSensors.IS_WOLF_IN_RANGE.key(), Expressions.Boolean.isTrue())
        .addEffect(TameWolfSensors.HAS_UNTAMED_WOLF_NEARBY.key().asDerived(), false)
        .withPerformCallback(UseBoneOnWolfAction::perform)
        .withFinishCallback(UseBoneOnWolfAction::onFinish)
        .build();

    private TameWolfActions() {
        throw new UnsupportedOperationException();
    }
}
