package com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.human.common.gameplay.entity.living.human.ai.generic.action.EquipWaterBucketAction;
import com.human.common.gameplay.entity.living.human.ai.generic.action.MoveToTargetAction;
import com.human.common.gameplay.entity.living.human.ai.generic.action.PickUpNearbyItemAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.MarineGOAPSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire.action.PlaceWaterAtFeetAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.function.Function;

public class ExtinguishFireActions {

    public static final Action<PathfinderMob> MOVE_TO_WATER_BUCKET = BLibAction.<PathfinderMob>builder("MoveToWaterBucketAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(ExtinguishFireSensors.HAS_WATER_BUCKET_IN_WORLD.key(), Expressions.Boolean.isTrue())
        .addPrecondition(ExtinguishFireSensors.IS_NEAREST_WATER_BUCKET_IN_RANGE.key(), Expressions.Boolean.isFalse())
        .addEffect(ExtinguishFireSensors.IS_NEAREST_WATER_BUCKET_IN_RANGE.key().asDerived(), true)
        .withPerformCallback(
            ctx -> MoveToTargetAction.perform(ctx, ExtinguishFireSensors.NEAREST_WATER_BUCKET_IN_WORLD.key(), ItemEntity::position)
        )
        .withFinishCallback(MoveToTargetAction::onFinish)
        .build();

    public static <T extends LivingEntity & BLibInventoryHolder> Action<T> pickUpWaterBucketFactory() {
        return BLibAction.<T>builder("PickUpWaterBucketAction")
            .addPrecondition(ExtinguishFireSensors.HAS_WATER_BUCKET_IN_WORLD.key(), Expressions.Boolean.isTrue())
            .addPrecondition(ExtinguishFireSensors.IS_NEAREST_WATER_BUCKET_IN_RANGE.key(), Expressions.Boolean.isTrue())
            .addEffect(ExtinguishFireSensors.HAS_WATER_BUCKET_IN_INVENTORY.key().asDerived(), true)
            .withPerformCallback(
                ctx -> PickUpNearbyItemAction.perform(ctx, ExtinguishFireSensors.NEAREST_WATER_BUCKET_IN_WORLD.key(), Function.identity())
            )
            .build();
    }

    public static final Action<Marine> EQUIP_WATER_BUCKET_ACTION = BLibAction.<Marine>builder("EquipWaterBucketAction")
        .addMasks(ActionMasks.USE_MAIN_HAND)
        .addPrecondition(ExtinguishFireSensors.HAS_WATER_BUCKET_IN_INVENTORY.key(), Expressions.Boolean.isTrue())
        .addEffect(ExtinguishFireSensors.HAS_WATER_BUCKET_EQUIPPED.key().asDerived(), true)
        .withPerformCallback(EquipWaterBucketAction::perform)
        .build();

    public static final Action<Marine> PLACE_WATER_AT_FEET_ACTION = BLibAction.<Marine>builder("PlaceWaterAtFeetAction")
        .addMasks(ActionMasks.USE_MAIN_HAND)
        .addPrecondition(GOAPSensors.IS_ON_GROUND.key(), Expressions.Boolean.isTrue())
        .addPrecondition(MarineGOAPSensors.IS_IN_ULTRA_WARM_DIMENSION.key(), Expressions.Boolean.isFalse())
        .addPrecondition(ExtinguishFireSensors.HAS_WATER_BUCKET_EQUIPPED.key(), Expressions.Boolean.isTrue())
        .addPrecondition(MarineGOAPSensors.IS_CURRENT_BLOCK_POS_REPLACEABLE.key(), Expressions.Boolean.isTrue())
        .addEffect(GOAPSensors.IS_ON_FIRE.key().asDerived(), false)
        .withPerformCallback(PlaceWaterAtFeetAction::perform)
        .withFinishCallback(PlaceWaterAtFeetAction::onFinish)
        .build();

    private ExtinguishFireActions() {
        throw new UnsupportedOperationException();
    }
}
