package com.human.common.gameplay.entity.living.human.marine.ai.collect_ammo;

import com.blib.api.common.goap.v1.action.ActionMasks;
import com.blib.api.common.goap.v1.action.BLibAction;
import com.human.common.gameplay.entity.living.human.ai.generic.action.MoveToTargetAction;
import com.human.common.gameplay.entity.living.human.ai.generic.action.PickUpNearbyItemAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.condition.expression.Expressions;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.function.Function;

/** Oct 5 - walk to dropped ammunition and pick it up. A mirror of the totem and torch collection. */
public final class AmmoActions {

    /** Walk to the nearest usable ammunition. */
    public static final Action<Marine> MOVE_TO_AMMO = BLibAction.<Marine>builder("MoveToAmmoAction")
        .addMasks(ActionMasks.MOVE)
        .addPrecondition(AmmoSensors.HAS_USABLE_AMMO_IN_WORLD.key(), Expressions.Boolean.isTrue())
        .addPrecondition(AmmoSensors.IS_NEAREST_AMMO_IN_RANGE.key(), Expressions.Boolean.isFalse())
        .addEffect(AmmoSensors.IS_NEAREST_AMMO_IN_RANGE.key().asDerived(), true)
        .withPerformCallback(ctx -> MoveToTargetAction.perform(ctx, AmmoSensors.NEAREST_USABLE_AMMO_IN_WORLD.key(), ItemEntity::position))
        .withFinishCallback(MoveToTargetAction::onFinish)
        .build();

    /** Pick it up. An empty gun then loads from it on its own (MarineResupply). */
    public static final Action<Marine> PICK_UP_AMMO = BLibAction.<Marine>builder("PickUpAmmoAction")
        .addPrecondition(AmmoSensors.HAS_USABLE_AMMO_IN_WORLD.key(), Expressions.Boolean.isTrue())
        .addPrecondition(AmmoSensors.IS_NEAREST_AMMO_IN_RANGE.key(), Expressions.Boolean.isTrue())
        .addEffect(AmmoSensors.HAS_USABLE_AMMO_IN_WORLD.key().asDerived(), false)
        .withPerformCallback(
            ctx -> PickUpNearbyItemAction.perform(ctx, AmmoSensors.NEAREST_USABLE_AMMO_IN_WORLD.key(), Function.identity())
        )
        .build();

    private AmmoActions() {
        throw new UnsupportedOperationException();
    }
}
