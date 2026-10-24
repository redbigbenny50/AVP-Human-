package com.human.common.gameplay.block.sign;

import com.human.common.registry.init.HumanSigns;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.jetbrains.annotations.Nullable;

/**
 * A standing sign of ours. Vanilla's block, making OUR sign block entity instead of vanilla's (whose type cannot hold
 * our blocks) and ticking it under our type.
 */
public class HumanStandingSignBlock extends StandingSignBlock {

    public HumanStandingSignBlock(WoodType type, BlockBehaviour.Properties properties) {
        super(type, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HumanSignBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        return createTickerHelper(blockEntityType, HumanSigns.SIGN_BLOCK_ENTITY.get(), SignBlockEntity::tick);
    }
}
