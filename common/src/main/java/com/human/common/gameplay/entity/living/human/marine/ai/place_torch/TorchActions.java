package com.human.common.gameplay.entity.living.human.marine.ai.place_torch;

import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.human.common.gameplay.entity.living.human.ai.generic.action.MoveToTargetAction;
import com.human.common.gameplay.entity.living.human.ai.generic.action.PickUpNearbyItemAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.place_torch.action.EquipTorchAction;
import com.human.common.gameplay.entity.living.human.marine.ai.place_torch.action.PlaceTorchAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.function.Function;

public class TorchActions {

    public static final Action<PathfinderMob> MOVE_TO_TORCH = BLibAction.<PathfinderMob>builder("MoveToTorchAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(TorchSensors.HAS_TORCH_IN_WORLD.key(), Expressions.Boolean.isTrue())
        .addPrecondition(TorchSensors.IS_NEAREST_TORCH_IN_RANGE.key(), Expressions.Boolean.isFalse())
        .addEffect(TorchSensors.IS_NEAREST_TORCH_IN_RANGE.key().asDerived(), true)
        .withPerformCallback(ctx -> MoveToTargetAction.perform(ctx, TorchSensors.NEAREST_TORCH_IN_WORLD.key(), ItemEntity::position))
        .withFinishCallback(MoveToTargetAction::onFinish)
        .build();

    public static <T extends LivingEntity & BLibInventoryHolder> Action<T> pickUpTorchFactory() {
        return BLibAction.<T>builder("PickUpTorchAction")
            .addPrecondition(TorchSensors.HAS_TORCH_IN_WORLD.key(), Expressions.Boolean.isTrue())
            .addPrecondition(TorchSensors.IS_NEAREST_TORCH_IN_RANGE.key(), Expressions.Boolean.isTrue())
            .addEffect(TorchSensors.HAS_TORCH_IN_INVENTORY.key().asDerived(), true)
            .withPerformCallback(ctx -> PickUpNearbyItemAction.perform(ctx, TorchSensors.NEAREST_TORCH_IN_WORLD.key(), Function.identity()))
            .build();
    }

    public static <T extends LivingEntity & BLibInventoryHolder> Action<T> equipTorchFactory() {
        return BLibAction.<T>builder("EquipTorchAction")
            .addMasks(ActionMasks.USE_OFF_HAND)
            .addPrecondition(TorchSensors.HAS_TORCH_IN_INVENTORY.key(), Expressions.Boolean.isTrue())
            .addPrecondition(TorchSensors.HAS_TORCH_EQUIPPED.key(), Expressions.Boolean.isFalse())
            .addEffect(TorchSensors.HAS_TORCH_EQUIPPED.key().asDerived(), true)
            .withPerformCallback(EquipTorchAction::perform)
            .build();
    }

    public static final Action<Marine> PLACE_TORCH = BLibAction.<Marine>builder("PlaceTorchAction")
        .addMasks(ActionMasks.USE_OFF_HAND)
        .addPrecondition(TorchSensors.SHOULD_PLACE_TORCH.key(), Expressions.Boolean.isTrue())
        .addPrecondition(TorchSensors.HAS_TORCH_EQUIPPED.key(), Expressions.Boolean.isTrue())
        .addEffect(TorchSensors.IS_IN_DARK_AREA.key().asDerived(), false)
        .withPerformCallback(PlaceTorchAction::perform)
        .build();

    private TorchActions() {
        throw new UnsupportedOperationException();
    }
}
