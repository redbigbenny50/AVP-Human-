package com.human.common.gameplay.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Places the wheel by its BLADES rather than by its hub.
 * <h2>Why the wheel needs its own placement</h2> [stated] Sep 25: "it places it too deep in water and on land its in
 * the ground when you place it the bottom is what should be at ground level", and "if you right click water it should
 * put the center block above the waters surface with the bottom being in the water".
 * <p>
 * ⚠ THE BLOCK IS ONE SOLID CUBE; THE BLADES ARE RENDER-ONLY. The geo spans a full block either side in X and Z, but
 * collision is a single cube at the hub, so the blades hang over neighbouring AIR. Placed at the clicked position the
 * hub sits at ground or water level, which buries the lower blades and - the reason this matters beyond looks - lets
 * water pour straight over the TOP of the wheel through the air above the hub. Lifting the hub one block puts the
 * blades in the water and the hub above it, which is both the look he described and the only arrangement where the top
 * of the wheel is out of the flow.
 * </p>
 */
public class HydroelectricGeneratorBlockItem extends BlockItem {

    public HydroelectricGeneratorBlockItem() {
        super(com.human.common.registry.init.HumanBlocks.HYDROELECTRIC_GENERATOR.get(), new Properties());
    }

    /**
     * ⚠ Returning null here REFUSES the placement - so every early-out returns the untouched context instead, and the
     * player simply gets vanilla behaviour rather than a click that does nothing.
     */
    @Override
    public @Nullable BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        var level = context.getLevel();
        var clicked = context.getClickedPos();

        // ⭐⭐ EXTENDING AN EXISTING WHEEL: MATCH ITS HUB, DO NOT LIFT.
        //
        // [stated] Sep 26: "if you are placing against another wheel its center places on that wheels center. right
        // now its doing the placing one block above thing which is incorrect when placing on a wheel but is correct
        // placing on a surface."
        //
        // The lift below exists so a wheel placed on GROUND sits with its blades at ground level. Applied to a click
        // on another section it does the opposite of what is wanted - it steps the new hub a block above its
        // neighbour's, so the two wheels never line up and never join as one run.
        var against = context.getClickedFace().getOpposite();
        var neighbour = clicked.relative(against);

        if (level.getBlockState(neighbour).getBlock() == getBlock()) {
            // Sit at the neighbour's own level: same hub height, so the sections form one wheel.
            return BlockPlaceContext.at(context, neighbour.relative(context.getClickedFace()), context.getClickedFace());
        }

        // Clicking a water surface: the hub goes ABOVE the water, blades below it.
        if (isWater(level.getFluidState(clicked).getType())) {
            return lift(context, clicked.above());
        }

        // Clicking solid ground: one more block up, so the lower blades clear the ground instead of sinking into it.
        return lift(context, clicked.above());
    }

    private static boolean isWater(net.minecraft.world.level.material.Fluid fluid) {
        return fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
    }

    /** Moves the placement to {@code target} only when that space can actually take the block. */
    private static BlockPlaceContext lift(BlockPlaceContext context, BlockPos target) {
        var level = context.getLevel();
        var state = level.getBlockState(target);

        // ⚠ Never lift into something solid - better to place where the player clicked than to refuse or to
        // overwrite a block they did not aim at.
        if (!state.canBeReplaced(context)) {
            return context;
        }

        return BlockPlaceContext.at(context, target, context.getClickedFace());
    }
}
