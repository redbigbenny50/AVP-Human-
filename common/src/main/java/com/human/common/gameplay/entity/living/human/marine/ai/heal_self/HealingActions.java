package com.human.common.gameplay.entity.living.human.marine.ai.heal_self;

import com.blib.api.common.goap.v1.GOAPSensors;
import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.human.common.gameplay.entity.living.human.ai.generic.action.MoveToTargetAction;
import com.human.common.gameplay.entity.living.human.ai.generic.action.PickUpNearbyItemAction;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.action.EquipHealingItemAction;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.action.UseHealingItemAction;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public class HealingActions {

    public static final Action<PathfinderMob> MOVE_TO_BEST_HEALING_ITEM = BLibAction.<PathfinderMob>builder("MoveToBestHealingItemAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(HealingSensors.BEST_HEALING_ITEM_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.WORLD))
        .addPrecondition(HealingSensors.IS_BEST_WORLD_HEALING_ITEM_IN_RANGE.key(), Expressions.Boolean.isFalse())
        .addEffect(HealingSensors.IS_BEST_WORLD_HEALING_ITEM_IN_RANGE.key().asDerived(), true)
        .withPerformCallback(
            ctx -> MoveToTargetAction.perform(
                ctx,
                HealingSensors.BEST_HEALING_ITEM_IN_WORLD.key(),
                r -> r.itemTarget().itemEntity().position()
            )
        )
        .withFinishCallback(MoveToTargetAction::onFinish)
        .build();

    public static <T extends LivingEntity & BLibInventoryHolder> Action<T> pickUpBestHealingItemFactory() {
        return BLibAction.<T>builder("PickUpBestHealingItemAction")
            .addPrecondition(HealingSensors.BEST_HEALING_ITEM_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.WORLD))
            .addPrecondition(HealingSensors.IS_BEST_WORLD_HEALING_ITEM_IN_RANGE.key(), Expressions.Boolean.isTrue())
            .addEffect(HealingSensors.BEST_HEALING_ITEM_LOCATION.key().asDerived(), ItemTarget.Location.INVENTORY)
            .withPerformCallback(
                ctx -> PickUpNearbyItemAction.perform(
                    ctx,
                    HealingSensors.BEST_HEALING_ITEM_IN_WORLD.key(),
                    r -> r.itemTarget().itemEntity()
                )
            )
            .build();
    }

    public static <T extends LivingEntity & BLibInventoryHolder> Action<T> equipBestHealingItemFactory() {
        return BLibAction.<T>builder("EquipBestHealingItemAction")
            .addMasks(ActionMasks.USE_MAIN_HAND)
            .addPrecondition(HealingSensors.BEST_HEALING_ITEM_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.INVENTORY))
            .addEffect(HealingSensors.BEST_HEALING_ITEM_LOCATION.key().asDerived(), ItemTarget.Location.EQUIPPED)
            .withPerformCallback(EquipHealingItemAction::perform)
            .build();
    }

    public static final Action<LivingEntity> USE_BEST_HEALING_ITEM = BLibAction.<LivingEntity>builder("UseBestHealingItemAction")
        .addMasks(ActionMasks.USE_MAIN_HAND)
        .addPrecondition(HealingSensors.BEST_HEALING_ITEM_LOCATION.key(), Expressions.Compare.equalTo(ItemTarget.Location.EQUIPPED))
        .addEffect(GOAPSensors.HEALTH_RATIO.key().asDerived(), 1.0F)
        .withPerformCallback(UseHealingItemAction::perform)
        .build();
}
