package com.human.common.gameplay.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.SlabBlock;
import org.jetbrains.annotations.NotNull;

/**
 * A slab of coloured industrial glass that tints a beacon beam.
 * <p>
 * Vanilla ships no stained slab, so the colour has to be attached here. {@link BeaconBeamBlock} is the whole of what
 * {@code StainedGlassBlock} adds over a plain block - a beacon walks the column above itself and asks each block it
 * finds for a colour - so implementing it is what makes a stack of these behave like the full blocks they were cut
 * from. Without it a beacon would shine white through a roof made of red glass slabs.
 */
public class StainedIndustrialGlassSlabBlock extends SlabBlock implements BeaconBeamBlock {

    private final DyeColor dyeColor;

    public StainedIndustrialGlassSlabBlock(DyeColor dyeColor, Properties properties) {
        super(properties);

        this.dyeColor = dyeColor;
    }

    @Override
    public @NotNull DyeColor getColor() {
        return dyeColor;
    }
}
