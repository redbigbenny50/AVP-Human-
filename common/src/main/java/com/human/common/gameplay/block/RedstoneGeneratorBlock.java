package com.human.common.gameplay.block;

import com.human.common.gameplay.block.entity.power.impl.RedstoneGeneratorBlockEntity;
import com.human.common.gameplay.block.power.PowerNodeEntityBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The redstone generator - [stated] Oct 3: a small, endless power source, 16 FE a tick (see
 * {@link RedstoneGeneratorBlockEntity}). It used to be a plain block that only gave off particles and produced nothing.
 */
public class RedstoneGeneratorBlock extends PowerNodeEntityBlock<RedstoneGeneratorBlockEntity> {

    public static final MapCodec<RedstoneGeneratorBlock> CODEC = simpleCodec(RedstoneGeneratorBlock::new);

    public RedstoneGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new RedstoneGeneratorBlockEntity(blockPos, blockState);
    }

    /** ⚠ An entity block is INVISIBLE by default (BaseEntityBlock) - draw the block's model, as it did before. */
    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    /** The red dust it always gave off, kept. */
    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        level.addParticle(DustParticleOptions.REDSTONE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 1.0, 0.0, 0.0);
    }
}
