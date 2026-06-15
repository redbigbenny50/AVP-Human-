package com.human.common.gameplay.entity.living.human.marine.ai.place_torch.action;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.place_torch.TorchSensors;
import com.just.ai.goap.action.Action;
import com.just.core.functional.option.Option;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;

public class PlaceTorchAction {

    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var worldState = context.getWorldState();
        var level = marine.level();

        // Ensure marine has a torch equipped.
        var offHandItem = marine.getOffhandItem();

        if (!offHandItem.is(Items.TORCH)) {
            return Action.Signal.ABORT;
        }

        // Try to place on wall, first.
        var wallPosOption = worldState.getOrDefault(TorchSensors.TORCH_WALL_PLACEMENT_POS.key(), Option.<BlockPos>none());

        if (wallPosOption.isSome()) {
            var torchPos = wallPosOption.unwrap();

            // Determine which direction the wall is relative to the torch position.
            var wallDirection = findWallDirection(marine, torchPos);

            if (wallDirection != null) {
                var wallTorchState = Blocks.WALL_TORCH.defaultBlockState()
                    .setValue(WallTorchBlock.FACING, wallDirection.getOpposite());

                level.setBlock(torchPos, wallTorchState, 3);
                playPlaceSound(marine, torchPos);
                offHandItem.shrink(1);

                return Action.Signal.CONTINUE;
            }
        }

        // Try to place at feet if wall is not available.
        var canPlaceAtFeet = worldState.getOrDefault(TorchSensors.CAN_PLACE_TORCH_AT_FEET.key(), false);

        if (canPlaceAtFeet) {
            var feetPos = marine.blockPosition();
            level.setBlock(feetPos, Blocks.TORCH.defaultBlockState(), 3);
            playPlaceSound(marine, feetPos);
            offHandItem.shrink(1);

            return Action.Signal.CONTINUE;
        }

        // Couldn't place torch anywhere.
        return Action.Signal.ABORT;
    }

    private static Direction findWallDirection(Marine marine, BlockPos torchPos) {
        var level = marine.level();

        for (var direction : Direction.Plane.HORIZONTAL) {
            var wallPos = torchPos.relative(direction);
            var wallState = level.getBlockState(wallPos);

            if (wallState.isFaceSturdy(level, wallPos, direction.getOpposite())) {
                return direction;
            }
        }

        return null;
    }

    private static void playPlaceSound(Marine marine, BlockPos pos) {
        var level = marine.level();
        var soundType = Blocks.TORCH.defaultBlockState().getSoundType();

        level.playSound(
            null,
            pos,
            soundType.getPlaceSound(),
            SoundSource.BLOCKS,
            (soundType.getVolume() + 1.0F) / 2.0F,
            soundType.getPitch() * 0.8F
        );
    }

    private PlaceTorchAction() {
        throw new UnsupportedOperationException();
    }
}
