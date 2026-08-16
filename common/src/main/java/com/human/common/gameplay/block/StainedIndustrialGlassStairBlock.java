package com.human.common.gameplay.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Stairs of coloured industrial glass that tint a beacon beam. See {@link StainedIndustrialGlassSlabBlock} for why the
 * interface has to be added by hand.
 */
public class StainedIndustrialGlassStairBlock extends StairBlock implements BeaconBeamBlock {

    private final DyeColor dyeColor;

    public StainedIndustrialGlassStairBlock(DyeColor dyeColor, BlockState baseState, Properties properties) {
        super(baseState, properties);

        this.dyeColor = dyeColor;
    }

    @Override
    public @NotNull DyeColor getColor() {
        return dyeColor;
    }
}
