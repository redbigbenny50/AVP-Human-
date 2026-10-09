package com.human.common.gameplay.entity.living.human.marine.ai.cut_free;

import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.human.common.gameplay.entity.living.human.ai.generic.action.MoveToTargetAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.cut_free.action.CutNettedMarineFreeAction;
import com.human.common.gameplay.entity.living.human.marine.ai.cut_free.action.EquipBladeAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;

public class CutFreeActions {

    public static final Action<Marine> EQUIP_BLADE = BLibAction.<Marine>builder("EquipBladeForCutFreeAction")
        .addMasks(ActionMasks.USE_MAIN_HAND)
        .addPrecondition(CutFreeSensors.HAS_NETTED_MARINE_NEARBY.key(), Expressions.Boolean.isTrue())
        .addPrecondition(CutFreeSensors.HAS_BLADE_IN_INVENTORY.key(), Expressions.Boolean.isTrue())
        .addPrecondition(CutFreeSensors.HAS_BLADE_EQUIPPED.key(), Expressions.Boolean.isFalse())
        .addEffect(CutFreeSensors.HAS_BLADE_EQUIPPED.key().asDerived(), true)
        .withPerformCallback(EquipBladeAction::perform)
        .build();

    public static final Action<Marine> MOVE_TO_NETTED_MARINE = BLibAction.<Marine>builder("MoveToNettedMarineAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(CutFreeSensors.HAS_NETTED_MARINE_NEARBY.key(), Expressions.Boolean.isTrue())
        .addPrecondition(CutFreeSensors.HAS_BLADE_EQUIPPED.key(), Expressions.Boolean.isTrue())
        .addPrecondition(CutFreeSensors.IS_NETTED_MARINE_IN_RANGE.key(), Expressions.Boolean.isFalse())
        .addEffect(CutFreeSensors.IS_NETTED_MARINE_IN_RANGE.key().asDerived(), true)
        .withPerformCallback(ctx -> MoveToTargetAction.perform(ctx, CutFreeSensors.NEAREST_NETTED_MARINE.key(), Marine::position))
        .withFinishCallback(MoveToTargetAction::onFinish)
        .build();

    public static final Action<Marine> CUT_FREE = BLibAction.<Marine>builder("CutNettedMarineFreeAction")
        .addMasks(ActionMasks.USE_MAIN_HAND, ActionMasks.LOOK)
        .addPrecondition(CutFreeSensors.HAS_NETTED_MARINE_NEARBY.key(), Expressions.Boolean.isTrue())
        .addPrecondition(CutFreeSensors.HAS_BLADE_EQUIPPED.key(), Expressions.Boolean.isTrue())
        .addPrecondition(CutFreeSensors.IS_NETTED_MARINE_IN_RANGE.key(), Expressions.Boolean.isTrue())
        .addEffect(CutFreeSensors.HAS_NETTED_MARINE_NEARBY.key().asDerived(), false)
        .withPerformCallback(CutNettedMarineFreeAction::perform)
        .withFinishCallback(CutNettedMarineFreeAction::onFinish)
        .build();

    private CutFreeActions() {
        throw new UnsupportedOperationException();
    }
}
