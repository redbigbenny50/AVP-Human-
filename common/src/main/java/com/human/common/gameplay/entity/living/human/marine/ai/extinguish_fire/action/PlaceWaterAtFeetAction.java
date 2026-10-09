package com.human.common.gameplay.entity.living.human.marine.ai.extinguish_fire.action;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import com.just.core.functional.option.Option;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class PlaceWaterAtFeetAction {

    private static final InteractionHand HAND_TO_USE = InteractionHand.MAIN_HAND;

    private static final int INTERACT_RANGE_IN_BLOCKS = 3;

    private static final StateKey.Derived<Option<BlockPos>> WATER_POS_OPTION = StateKey.derived("water_pos_option");

    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        var mainhandItemStack = marine.getMainHandItem();
        var isWaterBucketEquipped = mainhandItemStack.is(Items.WATER_BUCKET);

        if (!isWaterBucketEquipped) {
            return Action.Signal.ABORT;
        }

        var currentWaterPosOption = blackboard.getOrDefault(WATER_POS_OPTION, Option.none());

        if (currentWaterPosOption.isNone()) {
            var blockPos = getBestPosForWaterPlacement(marine);

            // ⚠ Oct 5 audit - POUR IT THE WAY A PLAYER DOES. This used to set a water block outright, which ignored
            // every rule a real bucket obeys: the Nether's evaporation, and any mod dimension that refuses water. The
            // vanilla bucket's own placement respects them - and when it refuses, the marine remembers that water does
            // not work here (MarineBucketMemory) instead of trying again on every flame.
            // ⚠ Review fix - a refusal only teaches anything when the spot was clear: pouring onto lava or into a block
            // that cannot take water fails in every dimension, and must not mark this one waterless.
            var spotWasClear = marine.level().getBlockState(blockPos).canBeReplaced()
                && marine.level().getFluidState(blockPos).isEmpty();
            var placed = ((net.minecraft.world.item.BucketItem) Items.WATER_BUCKET)
                .emptyContents(null, marine.level(), blockPos, null);

            if (!placed || !marine.level().getBlockState(blockPos).is(Blocks.WATER)) {
                if (spotWasClear) {
                    com.human.common.gameplay.entity.living.human.marine.MarineBucketMemory.markWaterless(marine.level());
                }

                return Action.Signal.ABORT;
            }

            currentWaterPosOption = Option.some(blockPos);
            blackboard.set(WATER_POS_OPTION, currentWaterPosOption);
            marine.setItemInHand(HAND_TO_USE, new ItemStack(Items.BUCKET));
            // And if the water went down but the marine is still burning in two seconds, that is remembered too.
            marine.noteBucketAttempt();
        }

        // [stated] Oct 5: keep the marine told about the water each tick, so if this action is dropped without
        // its finish callback the marine takes the water back itself (Marine.tick).
        blackboard.getOrDefault(WATER_POS_OPTION, Option.none()).ifSome(marine::trackPlacedWater);

        return Action.Signal.CONTINUE;
    }

    private static @NotNull BlockPos getBestPosForWaterPlacement(Marine marine) {
        return BlockPos.betweenClosedStream(marine.getBoundingBox())
            .filter(blockPos -> marine.level().getBlockState(blockPos).is(BlockTags.FIRE))
            .findFirst()
            .orElse(marine.blockPosition());
    }

    public static void onFinish(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        var waterPosOption = blackboard.getOrDefault(WATER_POS_OPTION, Option.none());

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
            marine.clearPlacedWater();
        });
    }

    private PlaceWaterAtFeetAction() {
        throw new UnsupportedOperationException();
    }
}
