package com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.human.common.gameplay.entity.living.human.ai.generic.action.MoveToTargetAction;
import com.human.common.gameplay.entity.living.human.ai.generic.action.PickUpNearbyItemAction;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.action.EquipWBIAction;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.action.UseWBIAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class WBIActions {

    public static final Action<PathfinderMob> MOVE_TO_BEST_WBI = BLibAction.<PathfinderMob>builder("MoveToBestWBIAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(WBISensors.BEST_WBI_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.WORLD))
        .addPrecondition(WBISensors.IS_BEST_WORLD_WBI_IN_RANGE.key(), Expressions.Boolean.isFalse())
        .addEffect(WBISensors.IS_BEST_WORLD_WBI_IN_RANGE.key().asDerived(), true)
        .withPerformCallback(
            ctx -> MoveToTargetAction.perform(ctx, WBISensors.BEST_WBI_IN_WORLD.key(), r -> r.itemTarget().itemEntity().position())
        )
        .withFinishCallback(MoveToTargetAction::onFinish)
        .build();

    public static <T extends LivingEntity & BLibInventoryHolder> Action<T> pickUpBestWBIFactory() {
        return BLibAction.<T>builder("PickUpBestWBIAction")
            .addPrecondition(WBISensors.BEST_WBI_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.WORLD))
            .addPrecondition(WBISensors.IS_BEST_WORLD_WBI_IN_RANGE.key(), Expressions.Boolean.isTrue())
            .addEffect(WBISensors.BEST_WBI_LOCATION.key().asDerived(), ItemTarget.Location.INVENTORY)
            .withPerformCallback(
                ctx -> PickUpNearbyItemAction.perform(ctx, WBISensors.BEST_WBI_IN_WORLD.key(), r -> r.itemTarget().itemEntity())
            )
            .build();
    }

    public static <T extends LivingEntity & BLibInventoryHolder> Action<T> equipBestWBIFactory() {
        return BLibAction.<T>builder("EquipBestWBIAction")
            .addMasks(ActionMasks.USE_MAIN_HAND)
            .addPrecondition(WBISensors.BEST_WBI_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.INVENTORY))
            .addEffect(WBISensors.BEST_WBI_LOCATION.key().asDerived(), ItemTarget.Location.EQUIPPED)
            .withPerformCallback(EquipWBIAction::perform)
            .build();
    }

    public static final Action<LivingEntity> USE_BEST_WBI = BLibAction.<LivingEntity>builder("UseBestWBIAction")
        .addMasks(ActionMasks.USE_MAIN_HAND)
        .addPrecondition(WBISensors.BEST_WBI_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.EQUIPPED))
        .addEffect(GOAPSensors.HAS_WATER_BREATHING.key().asDerived(), true)
        .withPerformCallback(UseWBIAction::perform)
        .build();

    private WBIActions() {
        throw new UnsupportedOperationException();
    }
}
