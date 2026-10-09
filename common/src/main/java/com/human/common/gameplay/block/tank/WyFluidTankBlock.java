package com.human.common.gameplay.block.tank;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The Weyland-Yutani tank. [stated] design review: W-Y hulls are large white panels, 1 block wide x 2 tall, staggered
 * by one block between neighbouring columns - so each block also knows which half of a panel it is on each axis.
 */
public class WyFluidTankBlock extends FluidTankBlock {

    public static final EnumProperty<TankPanel> PANEL_X = EnumProperty.create("panel_x", TankPanel.class);

    public static final EnumProperty<TankPanel> PANEL_Z = EnumProperty.create("panel_z", TankPanel.class);

    public WyFluidTankBlock(Properties properties) {
        super(FluidTankEdition.WY, properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PANEL_X, PANEL_Z);
    }
}
