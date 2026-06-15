package com.human.common.gameplay.entity.living.human.marine.ai.tame_wolf.action;

import com.blib.api.common.goap.v1.action.impl.UnequipItemAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.tame_wolf.TameWolfSensors;
import com.human.mixin.MixinWolf_Accessor;
import com.just.ai.goap.action.Action;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;

public class UseBoneOnWolfAction {

    private static final EquipmentSlot HAND_TO_USE = EquipmentSlot.MAINHAND;

    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var worldState = context.getWorldState();
        var wolfOption = worldState.getOrDefault(TameWolfSensors.NEAREST_UNTAMED_WOLF.key(), Option.none());

        if (wolfOption.isNone()) {
            return Action.Signal.ABORT;
        }

        var wolf = wolfOption.unwrap();

        // Ensure marine has a bone in main hand.
        var itemStackInHand = marine.getItemBySlot(HAND_TO_USE);

        if (!itemStackInHand.is(Items.BONE)) {
            return Action.Signal.ABORT;
        }

        // Make the marine look at the wolf.
        marine.getLookControl().setLookAt(wolf, 30.0F, 30.0F);

        // Consume the bone.
        itemStackInHand.shrink(1);

        // Attempt to tame the wolf (1/3 chance, same as vanilla).
        if (marine.getRandom().nextInt(3) == 0) {
            // If the marine does not have a leader, then the wolf belongs to marine.
            // If the marine has a leader but the leader is not loaded, then the wolf belongs to the marine.
            if (!marine.hasLeader() || !marine.getLeader().isSome()) {
                wolf.setOwnerUUID(marine.getUUID());
                wolf.getEntityData().set(MixinWolf_Accessor.getDataCollarColor(), DyeColor.GREEN.getId());
            } else {
                var leader = marine.getLeader().unwrap();

                // If the marine has a leader and the leader is NOT a player, then the wolf belongs to the marine.
                if (!(leader instanceof Player)) {
                    wolf.setOwnerUUID(marine.getUUID());
                    wolf.getEntityData().set(MixinWolf_Accessor.getDataCollarColor(), DyeColor.GREEN.getId());
                } else {
                    wolf.setOwnerUUID(leader.getUUID());
                }
            }

            wolf.setTame(true, true);
            wolf.getNavigation().stop();
            wolf.setOrderedToSit(false);
            wolf.setInSittingPose(false);
            // Hearts particle effect.
            wolf.level().broadcastEntityEvent(wolf, EntityEvent.TAMING_SUCCEEDED);

            return Action.Signal.CONTINUE;
        } else {
            // Taming failed, show smoke particles.
            wolf.level().broadcastEntityEvent(wolf, EntityEvent.TAMING_FAILED);
        }

        // If we still have bones equipped, continue trying.
        if (itemStackInHand.is(Items.BONE) && itemStackInHand.getCount() > 0) {
            return Action.Signal.CONTINUE;
        }

        // Check if we have more bones in inventory.
        var hasBoneInInventory = worldState.getOrDefault(TameWolfSensors.HAS_BONE_IN_INVENTORY.key(), false);

        if (hasBoneInInventory) {
            // Need to equip another bone, so abort this action to re-plan.
            return Action.Signal.ABORT;
        }

        // No more bones, abort.
        return Action.Signal.ABORT;
    }

    public static Action.Signal onFinish(Action.Context<? extends Marine> context) {
        return UnequipItemAction.perform(context.getActor(), HAND_TO_USE);
    }

    private UseBoneOnWolfAction() {
        throw new UnsupportedOperationException();
    }
}
