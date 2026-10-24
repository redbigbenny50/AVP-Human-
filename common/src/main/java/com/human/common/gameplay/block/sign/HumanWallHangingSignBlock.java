package com.human.common.gameplay.block.sign;

import com.human.common.registry.init.HumanSigns;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.jetbrains.annotations.Nullable;

/**
 * A hanging sign of ours, on a wall bracket. Vanilla's block, making OUR hanging sign block entity instead of vanilla's
 * (whose type cannot hold our blocks) and ticking it under our type.
 */
public class HumanWallHangingSignBlock extends WallHangingSignBlock {

    public HumanWallHangingSignBlock(WoodType type, BlockBehaviour.Properties properties) {
        super(type, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HumanHangingSignBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        return createTickerHelper(blockEntityType, HumanSigns.HANGING_SIGN_BLOCK_ENTITY.get(), SignBlockEntity::tick);
    }
}
