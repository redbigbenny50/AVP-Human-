package com.human.common.gameplay.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * "Is something that carries fluid attached here?" - asked in each loader's own way.
 * <p>
 * The tank uses this to know where a pipe is attached: [stated] Sep 29, a bare-hand right-click on a side with a pipe
 * attached switches that port, and a valve will be drawn where a pipe attaches. It asks the NEIGHBOUR whether it
 * exposes fluid on the face that touches us - which is how every mod's pipe presents itself (NeoForge's fluid
 * capability, Fabric's Transfer API), and how ours will. Only asked on a click or a neighbour change, never per tick.
 * </p>
 */
public final class FluidConnections {

    private static Bridge bridge = (level, pos, side) -> false;

    private FluidConnections() {}

    public static void setBridge(Bridge newBridge) {
        bridge = newBridge == null ? (level, pos, side) -> false : newBridge;
    }

    /**
     * @param level the level
     * @param pos   the block whose face is being checked (the tank)
     * @param face  the face of {@code pos}
     * @return whether the block on that face carries fluid towards it
     */
    public static boolean isConnected(Level level, BlockPos pos, Direction face) {
        var neighbour = pos.relative(face);

        if (!level.isLoaded(neighbour)) {
            return false;
        }

        // [stated] Sep 30: our own fluid pipe holds nothing, so it exposes no fluid storage - but it IS attached.
        if (level.getBlockState(neighbour).getBlock() instanceof com.human.common.gameplay.block.fluid.FluidPipeBlock) {
            return true;
        }

        return bridge.exposesFluid(level, neighbour, face.getOpposite());
    }

    /** Whether the block at {@code pos} holds fluid on its {@code side} face - our blocks or any mod's. */
    public static boolean exposesFluid(Level level, BlockPos pos, Direction side) {
        return level.isLoaded(pos) && !level.getBlockState(pos).isAir() && bridge.exposesFluid(level, pos, side);
    }

    @FunctionalInterface
    public interface Bridge {

        /** Whether the block at {@code pos} exposes fluid storage on its {@code side} face. */
        boolean exposesFluid(Level level, BlockPos pos, Direction side);
    }
}
