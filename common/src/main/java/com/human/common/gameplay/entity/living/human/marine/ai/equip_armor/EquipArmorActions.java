package com.human.common.gameplay.entity.living.human.marine.ai.equip_armor;

import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.human.common.gameplay.entity.living.human.ai.HumanGOAPExpressions;
import com.human.common.gameplay.entity.living.human.ai.model.ArmorSetTarget;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.action.EquipBestArmorSetFromInventoryAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;
import com.just.ai.goap.state.Blackboard;

public class EquipArmorActions {

    public static final Action<Marine> PICK_UP_BEST_ARMOR_PIECES_ACTION = Action.<Marine>builder("PickUpBestArmorPiecesAction")
        .addPrecondition(EquipArmorSensors.IS_ANY_BEST_ARMOR_SET_PIECE_IN_WORLD.key(), Expressions.Boolean.isTrue())
        .addEffect(EquipArmorSensors.IS_ANY_BEST_ARMOR_SET_PIECE_IN_INVENTORY.key().asDerived(), true)
        .withPerformCallback(context -> {
            var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
            var bestArmorTargetOption = blackboard.getOrDefault(EquipArmorSensors.BEST_ARMOR_SET_TARGET.key(), ArmorSetTarget.EMPTY);

            if (bestArmorTargetOption.isEmpty()) {
                return Action.Signal.ABORT;
            }

            return Action.Signal.CONTINUE;
        })
        .build();

    public static final Action<Marine> EQUIP_BEST_ARMOR_PIECES_FROM_INVENTORY_ACTION = BLibAction.<Marine>builder(
        "EquipBestArmorPiecesFromInventoryAction"
    )
        .addMasks(ActionMasks.USE_MAIN_HAND)
        .addPrecondition(EquipArmorSensors.IS_ANY_BEST_ARMOR_SET_PIECE_IN_INVENTORY.key(), Expressions.Boolean.isTrue())
        .addPrecondition(EquipArmorSensors.BEST_ARMOR_SET_TARGET.key(), HumanGOAPExpressions.ArmorSetTarget.isNotEmpty())
        .addEffect(EquipArmorSensors.ARE_ALL_BEST_ARMOR_SET_PIECES_EQUIPPED.key().asDerived(), true)
        .withPerformCallback(EquipBestArmorSetFromInventoryAction::perform)
        .build();

    private EquipArmorActions() {
        throw new UnsupportedOperationException();
    }
}
