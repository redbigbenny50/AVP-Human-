package com.human.common.gameplay.block.power;

import com.human.common.gameplay.block.entity.power.impl.SolarPanelBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * No FACING on purpose: [stated] "the panel faces east to west so no matter when you stand when you place it it places
 * the same direction." The only state is the power light.
 */
public class SolarPanelBlock extends PowerNodeEntityBlock<SolarPanelBlockEntity> {

    public static final MapCodec<SolarPanelBlock> CODEC = simpleCodec(SolarPanelBlock::new);

    public SolarPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PowerMode.PROPERTY, PowerMode.OFF));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PowerMode.PROPERTY);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new SolarPanelBlockEntity(blockPos, blockState);
    }
}
