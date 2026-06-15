package com.human.common.gameplay.entity.living.human.marine.ai.break_fall.action;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.break_fall.BreakFallSensors;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import com.just.core.functional.option.Option;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class PlaceWaterToBreakFallAction {

    private static final InteractionHand HAND_TO_USE = InteractionHand.MAIN_HAND;

    private static final int INTERACT_RANGE_IN_BLOCKS = 3;

    private static final StateKey.Derived<Option<BlockPos>> KEY_WATER_POS_OPTION = StateKey.derived("water_pos_option");

    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var worldState = context.getWorldState();

        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        var currentWaterPosOption = blackboard.getOrDefault(KEY_WATER_POS_OPTION, Option.none());

        if (currentWaterPosOption.isNone()) {

            // Get the landing position.
            var landingPosOption = worldState.getOrDefault(
                BreakFallSensors.LANDING_BLOCK_POS.key(),
                Option.none()
            );

            if (landingPosOption.isNone()) {
                return Action.Signal.ABORT;
            }

            var landingPos = landingPosOption.unwrap();
            var level = marine.level();

            // Verify the block can still have water placed on it.
            var landingState = level.getBlockState(landingPos);

            if (!landingState.canBeReplaced()) {
                return Action.Signal.ABORT;
            }

            // Place the water.
            level.setBlock(landingPos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
            currentWaterPosOption = Option.some(landingPos);
            blackboard.set(KEY_WATER_POS_OPTION, currentWaterPosOption);
            // Replace the water bucket with an empty bucket.
            marine.setItemInHand(HAND_TO_USE, new ItemStack(Items.BUCKET));

        }

        return Action.Signal.CONTINUE;
    }

    public static void onFinish(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        var waterPosOption = blackboard.getOrDefault(KEY_WATER_POS_OPTION, Option.none());

        waterPosOption.ifSome(blockPos -> {
            var blockState = marine.level().getBlockState(blockPos);

            if (
                blockState.is(Blocks.WATER)
                    && marine.distanceToSqr(blockPos.getCenter()) <= INTERACT_RANGE_IN_BLOCKS * INTERACT_RANGE_IN_BLOCKS
            ) {
                // Block state is water AND marine is close enough to pick up block state.
                // Set the block state to air.
                marine.level().setBlock(blockPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                // Add the water bucket back to the marine's inventory.
                marine.getInventory().addItem(Items.WATER_BUCKET);
            } else {
                // Otherwise block state is not water OR marine is too far away from it to pick it up, so return an
                // empty bucket to the marine's inventory.
                marine.getInventory().addItem(Items.BUCKET);
            }

            marine.setItemInHand(HAND_TO_USE, ItemStack.EMPTY);
        });
    }

    private PlaceWaterToBreakFallAction() {
        throw new UnsupportedOperationException();
    }
}
