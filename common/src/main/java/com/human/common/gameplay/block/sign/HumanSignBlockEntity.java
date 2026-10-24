package com.human.common.gameplay.block.sign;

import com.human.common.registry.init.HumanSigns;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A standing or wall sign of ours. Identical to vanilla's, under its own block entity type: vanilla's sign type only
 * lists vanilla's sign blocks, so it cannot hold ours.
 */
public class HumanSignBlockEntity extends SignBlockEntity {

    public HumanSignBlockEntity(BlockPos pos, BlockState state) {
        super(HumanSigns.SIGN_BLOCK_ENTITY.get(), pos, state);
    }
}
