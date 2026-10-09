package com.human.common.gameplay.block.item;

import com.human.common.gameplay.block.pipe.PipeBlockEntity;
import com.human.common.gameplay.item.pipe.ItemPipeSystem;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** An item pipe that touches a port: flange colours and end settings, and how the item network finds it on load. */
public class ItemPipeBlockEntity extends PipeBlockEntity {

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.ITEM_PIPE.get(), pos, state);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();

        if (level instanceof ServerLevel serverLevel) {
            ItemPipeSystem.get(serverLevel).registerPortPipe(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            ItemPipeSystem.get(serverLevel).unregisterPortPipe(worldPosition);
        }

        super.setRemoved();
    }
}
