package com.human.common.gameplay.block.item;

import com.human.common.gameplay.block.pipe.PipeBlock;
import com.human.common.gameplay.item.pipe.ItemPipeSystem;
import com.human.common.gameplay.item.pipe.PipeItemAccess;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * The item pipe - [stated] Sep 30, "similar kind of rules as the fluid pipe". Everything visual and the flange click is
 * {@link PipeBlock}; its ports are anything that holds items on that face (chests, barrels, furnaces, our machines,
 * other mods' inventories), and its network is {@link ItemPipeSystem}.
 */
public class ItemPipeBlock extends PipeBlock {

    public static final MapCodec<ItemPipeBlock> CODEC = simpleCodec(ItemPipeBlock::new);

    public ItemPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean joins(BlockState neighbour) {
        return neighbour.getBlock() instanceof ItemPipeBlock;
    }

    @Override
    protected boolean isPort(Level level, BlockPos pos, Direction side) {
        return PipeItemAccess.open(level, pos, side).present();
    }

    @Override
    protected BlockEntity newPortEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Override
    protected void invalidate(ServerLevel level, BlockPos pos) {
        ItemPipeSystem.get(level).invalidate(pos);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
