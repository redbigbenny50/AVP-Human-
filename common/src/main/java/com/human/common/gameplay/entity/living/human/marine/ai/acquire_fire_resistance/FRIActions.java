package com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.human.common.gameplay.entity.living.human.ai.generic.action.MoveToTargetAction;
import com.human.common.gameplay.entity.living.human.ai.generic.action.PickUpNearbyItemAction;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.action.EquipFRIAction;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.action.UseFRIAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class FRIActions {

    public static final Action<PathfinderMob> MOVE_TO_BEST_FRI = BLibAction.<PathfinderMob>builder("MoveToBestFRIAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(FRISensors.BEST_FRI_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.WORLD))
        .addPrecondition(FRISensors.IS_BEST_WORLD_FRI_IN_RANGE.key(), Expressions.Boolean.isFalse())
        .addEffect(FRISensors.IS_BEST_WORLD_FRI_IN_RANGE.key().asDerived(), true)
        .withPerformCallback(
            ctx -> MoveToTargetAction.perform(ctx, FRISensors.BEST_FRI_IN_WORLD.key(), r -> r.itemTarget().itemEntity().position())
        )
        .withFinishCallback(MoveToTargetAction::onFinish)
        .build();

    public static <T extends LivingEntity & BLibInventoryHolder> Action<T> pickUpBestFRIFactory() {
        return BLibAction.<T>builder("PickUpBestFRIAction")
            .addPrecondition(FRISensors.BEST_FRI_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.WORLD))
            .addPrecondition(FRISensors.IS_BEST_WORLD_FRI_IN_RANGE.key(), Expressions.Boolean.isTrue())
            .addEffect(FRISensors.BEST_FRI_LOCATION.key().asDerived(), ItemTarget.Location.INVENTORY)
            .withPerformCallback(
                ctx -> PickUpNearbyItemAction.perform(ctx, FRISensors.BEST_FRI_IN_WORLD.key(), r -> r.itemTarget().itemEntity())
            )
            .build();
    }

    public static <T extends LivingEntity & BLibInventoryHolder> Action<T> equipBestFRIFactory() {
        return BLibAction.<T>builder("EquipBestFRIAction")
            .addMasks(ActionMasks.USE_MAIN_HAND)
            .addPrecondition(FRISensors.BEST_FRI_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.INVENTORY))
            .addEffect(FRISensors.BEST_FRI_LOCATION.key().asDerived(), ItemTarget.Location.EQUIPPED)
            .withPerformCallback(EquipFRIAction::perform)
            .build();
    }

    public static final Action<LivingEntity> USE_BEST_FRI = BLibAction.<LivingEntity>builder("UseBestFRIAction")
        .addMasks(ActionMasks.USE_MAIN_HAND)
        .addPrecondition(FRISensors.BEST_FRI_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.EQUIPPED))
        .addEffect(GOAPSensors.HAS_FIRE_RESISTANCE.key().asDerived(), true)
        .withPerformCallback(UseFRIAction::perform)
        .build();
}
