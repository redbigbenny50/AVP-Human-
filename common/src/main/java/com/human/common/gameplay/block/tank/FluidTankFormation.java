package com.human.common.gameplay.block.tank;

import com.human.common.gameplay.fluid.FluidContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Forms, merges and splits multiblock fluid tanks. Runs ONLY when a tank block is placed or broken, or its windows are
 * toggled - never per tick.
 * <h2>THE STEPS</h2>
 * <ol>
 * <li><b>Region.</b> Same-edition tank blocks connected to the change, within reach of it
 * ({@link FluidTankRules#REFORM_REACH_XZ}, {@link FluidTankRules#REFORM_REACH_Y}), plus the WHOLE of every existing
 * tank that reaches into that. Bounded by the tank size limits, not by the size of a tank farm.</li>
 * <li><b>Partition.</b> Greedy, from the lowest corner up (lowest y, then x, then z): take the widest square that fits
 * (up to {@link FluidTankRules#MAX_WIDTH}), then grow it up a layer at a time (up to
 * {@link FluidTankRules#MAX_HEIGHT}). A square or layer is refused if it would put two DIFFERENT fluids in one tank, so
 * tanks holding different fluids never merge.</li>
 * <li><b>Fluid.</b> Each old tank's fluid is shared out by volume - how many of its blocks landed in each new tank. The
 * broken block's share, and anything a new tank cannot hold, is spilled and lost, as in Create. (Design section 6: flag
 * it in the changelog.)</li>
 * <li><b>Stamp.</b> Each block's position, window part and W-Y panel half go into its block state - written only where
 * something actually changed.</li>
 * </ol>
 * <h2>⚠ RE-ENTRY</h2> Stamping calls setBlock on tank blocks, and vanilla calls onPlace/onRemove on EVERY state change,
 * properties included (read in {@code LevelChunk.setBlockState}). {@link FluidTankBlock} ignores changes where the
 * block stays a tank, so stamping never re-triggers forming.
 * <h2>UNLOADED CHUNKS</h2> A re-form LOADS the few chunks it needs (it only reaches
 * {@link FluidTankRules#REFORM_REACH_XZ} blocks) rather than skipping. It used to skip - and when the broken block was
 * the CONTROLLER, the only copy of the tank's fluid, that lost the whole tank's fluid and left its other blocks
 * pointing at a controller that no longer existed. A re-form is a one-off event on a block being placed or broken,
 * never per tick, so loading a chunk for it costs nothing lasting. (Everyday use - pipes, buckets, the renderer - never
 * loads a chunk: see {@link FluidTankBlockEntity#controller()}.)
 */
public final class FluidTankFormation {

    /** What a broken block knew, captured before its block entity was removed. */
    public record Removed(
        BlockPos pos,
        BlockPos controller,
        FluidContent content,
        int blockCount,
        boolean windows
    ) {}

    private record OldTank(
        FluidContent content,
        int blockCount,
        boolean windows
    ) {}

    private record NewTank(
        BlockPos corner,
        int width,
        int height,
        Fluid fluid
    ) {}

    private static final Comparator<BlockPos> BOTTOM_UP = Comparator
        .<BlockPos>comparingInt(pos -> pos.getY())
        .thenComparingInt(pos -> pos.getX())
        .thenComparingInt(pos -> pos.getZ());

    private FluidTankFormation() {}

    // ------------------------------------------------------------------------------------------------------------
    // Entry points
    // ------------------------------------------------------------------------------------------------------------

    /** A tank block was placed at {@code pos}. */
    public static void onPlaced(Level level, BlockPos pos, Block block) {
        reform(level, pos, block, List.of(pos), null);
    }

    /** A tank block was broken at {@code pos}; its neighbours re-form without it. */
    public static void onRemoved(Level level, BlockPos pos, Block block, @Nullable Removed removed) {
        var seeds = new ArrayList<BlockPos>();

        for (var direction : Direction.values()) {
            var neighbour = pos.relative(direction);

            if (level.getBlockState(neighbour).is(block)) {
                seeds.add(neighbour);
            }
        }

        if (!seeds.isEmpty()) {
            reform(level, pos, block, seeds, removed);
        }
    }

    /** Windows on or off for the whole tank the controller runs: re-stamps its blocks' window parts. */
    public static void setWindows(Level level, FluidTankBlockEntity controller, boolean windows) {
        controller.setWindows(windows);
        stampTank(level, controller.getBlockPos(), controller.getWidth(), controller.getHeight(), windows);
    }

    /** Tells comparators beside every block of a tank that its signal changed. */
    public static void updateComparators(Level level, BlockPos corner, int width, int height) {
        var block = level.getBlockState(corner).getBlock();

        for (var dy = 0; dy < height; dy++) {
            for (var dx = 0; dx < width; dx++) {
                for (var dz = 0; dz < width; dz++) {
                    level.updateNeighbourForOutputSignal(corner.offset(dx, dy, dz), block);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The re-form
    // ------------------------------------------------------------------------------------------------------------

    private static void reform(Level level, BlockPos origin, Block block, List<BlockPos> seeds, @Nullable Removed removed) {
        var region = collectRegion(level, origin, block, seeds);

        if (region == null || region.isEmpty()) {
            return;
        }

        // Which old tank each block was in, and what each old tank held.
        var oldControllerOf = new HashMap<BlockPos, BlockPos>();
        var oldTanks = new HashMap<BlockPos, OldTank>();

        for (var pos : region) {
            if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) || !tank.hasTank()) {
                continue;
            }

            var controllerPos = tank.getControllerPos();

            oldControllerOf.put(pos, controllerPos);

            if (
                !oldTanks.containsKey(controllerPos) && level.getBlockEntity(controllerPos) instanceof FluidTankBlockEntity controller
                    && controller.isController()
            ) {
                oldTanks.put(
                    controllerPos,
                    new OldTank(
                        controller.getContent(),
                        controller.getWidth() * controller.getWidth() * controller.getHeight(),
                        controller.hasWindows()
                    )
                );
            }
        }

        // A broken CONTROLLER's block entity is already gone: its tank's fluid comes from what it captured.
        if (removed != null && !oldTanks.containsKey(removed.controller())) {
            oldTanks.put(removed.controller(), new OldTank(removed.content(), removed.blockCount(), removed.windows()));
        }

        var newTanks = partition(region, oldControllerOf, oldTanks);

        for (var tank : newTanks) {
            write(level, tank, oldControllerOf, oldTanks);
        }
    }

    /**
     * The region to re-form, or null when it reaches an unloaded chunk. Flood-fills same-edition blocks from the seeds
     * inside the reach box, then adds the whole of every existing tank those blocks belong to.
     */
    private static @Nullable Set<BlockPos> collectRegion(Level level, BlockPos origin, Block block, List<BlockPos> seeds) {
        var region = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();

        for (var seed : seeds) {
            if (region.add(seed.immutable())) {
                queue.add(seed.immutable());
            }
        }

        while (!queue.isEmpty()) {
            var pos = queue.poll();

            for (var direction : Direction.values()) {
                var next = pos.relative(direction);

                if (region.contains(next) || !withinReach(origin, next)) {
                    continue;
                }

                // On the server this loads the chunk if needed - see UNLOADED CHUNKS above.
                if (level.getBlockState(next).is(block)) {
                    region.add(next);
                    queue.add(next);
                }
            }
        }

        // The whole of every tank already touching the region - a tank is never re-formed in part.
        var extra = new HashSet<BlockPos>();

        for (var pos : region) {
            if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) || !tank.hasTank()) {
                continue;
            }

            var controllerPos = tank.getControllerPos();

            if (!(level.getBlockEntity(controllerPos) instanceof FluidTankBlockEntity controller) || !controller.isController()) {
                continue;
            }

            for (var dy = 0; dy < controller.getHeight(); dy++) {
                for (var dx = 0; dx < controller.getWidth(); dx++) {
                    for (var dz = 0; dz < controller.getWidth(); dz++) {
                        var member = controllerPos.offset(dx, dy, dz);

                        if (region.contains(member) || extra.contains(member)) {
                            continue;
                        }

                        if (level.getBlockState(member).is(block)) {
                            extra.add(member);
                        }
                    }
                }
            }
        }

        region.addAll(extra);
        return region;
    }

    private static boolean withinReach(BlockPos origin, BlockPos pos) {
        return Math.abs(pos.getX() - origin.getX()) <= FluidTankRules.REFORM_REACH_XZ
            && Math.abs(pos.getZ() - origin.getZ()) <= FluidTankRules.REFORM_REACH_XZ
            && Math.abs(pos.getY() - origin.getY()) <= FluidTankRules.REFORM_REACH_Y;
    }

    private static List<NewTank> partition(Set<BlockPos> region, Map<BlockPos, BlockPos> oldControllerOf, Map<BlockPos, OldTank> oldTanks) {
        var sorted = new ArrayList<>(region);
        var assigned = new HashSet<BlockPos>();
        var tanks = new ArrayList<NewTank>();

        sorted.sort(BOTTOM_UP);

        for (var corner : sorted) {
            if (assigned.contains(corner)) {
                continue;
            }

            var width = 1;
            var fluid = fluidOf(corner, oldControllerOf, oldTanks);

            for (var candidate = FluidTankRules.MAX_WIDTH; candidate > 1; candidate--) {
                var layerFluid = layerFluid(corner, candidate, fluid, region, assigned, oldControllerOf, oldTanks);

                if (layerFluid != null) {
                    width = candidate;
                    fluid = layerFluid;
                    break;
                }
            }

            var height = 1;

            while (height < FluidTankRules.MAX_HEIGHT) {
                var layerFluid = layerFluid(corner.above(height), width, fluid, region, assigned, oldControllerOf, oldTanks);

                if (layerFluid == null) {
                    break;
                }

                fluid = layerFluid;
                height++;
            }

            for (var dy = 0; dy < height; dy++) {
                for (var dx = 0; dx < width; dx++) {
                    for (var dz = 0; dz < width; dz++) {
                        assigned.add(corner.offset(dx, dy, dz));
                    }
                }
            }

            tanks.add(new NewTank(corner, width, height, fluid));
        }

        return tanks;
    }

    /**
     * Whether a width x width layer with its low corner at {@code corner} is all free region blocks with at most one
     * fluid between it and {@code fluid}. Returns the (possibly now known) fluid, or null when the layer does not fit.
     */
    private static @Nullable Fluid layerFluid(
        BlockPos corner,
        int width,
        Fluid fluid,
        Set<BlockPos> region,
        Set<BlockPos> assigned,
        Map<BlockPos, BlockPos> oldControllerOf,
        Map<BlockPos, OldTank> oldTanks
    ) {
        var result = fluid;

        for (var dx = 0; dx < width; dx++) {
            for (var dz = 0; dz < width; dz++) {
                var pos = corner.offset(dx, 0, dz);

                if (!region.contains(pos) || assigned.contains(pos)) {
                    return null;
                }

                var here = fluidOf(pos, oldControllerOf, oldTanks);

                if (here == Fluids.EMPTY) {
                    continue;
                }

                if (result == Fluids.EMPTY) {
                    result = here;
                } else if (!result.isSame(here)) {
                    return null;
                }
            }
        }

        return result;
    }

    private static Fluid fluidOf(BlockPos pos, Map<BlockPos, BlockPos> oldControllerOf, Map<BlockPos, OldTank> oldTanks) {
        var controller = oldControllerOf.get(pos);
        var old = controller == null ? null : oldTanks.get(controller);

        return old == null ? Fluids.EMPTY : old.content().fluid();
    }

    private static void write(Level level, NewTank tank, Map<BlockPos, BlockPos> oldControllerOf, Map<BlockPos, OldTank> oldTanks) {
        // Share each old tank's fluid out by how many of its blocks landed here.
        var landed = new HashMap<BlockPos, Integer>();

        for (var dy = 0; dy < tank.height(); dy++) {
            for (var dx = 0; dx < tank.width(); dx++) {
                for (var dz = 0; dz < tank.width(); dz++) {
                    var old = oldControllerOf.get(tank.corner().offset(dx, dy, dz));

                    if (old != null) {
                        landed.merge(old, 1, Integer::sum);
                    }
                }
            }
        }

        var amount = 0L;
        Boolean windows = null;

        for (var entry : landed.entrySet()) {
            var old = oldTanks.get(entry.getKey());

            if (old == null) {
                continue;
            }

            if (windows == null || entry.getKey().equals(oldControllerOf.get(tank.corner()))) {
                windows = old.windows();
            }

            if (!old.content().isEmpty() && old.content().fluid().isSame(tank.fluid()) && old.blockCount() > 0) {
                amount += (long) old.content().amount() * entry.getValue() / old.blockCount();
            }
        }

        var capacity = FluidTankBlockEntity.capacityOf(tank.width(), tank.height());
        var content = tank.fluid() == Fluids.EMPTY ? FluidContent.EMPTY : FluidContent.of(tank.fluid(), (int) Math.min(amount, capacity));
        var hasWindows = windows == null || windows;

        for (var dy = 0; dy < tank.height(); dy++) {
            for (var dx = 0; dx < tank.width(); dx++) {
                for (var dz = 0; dz < tank.width(); dz++) {
                    var pos = tank.corner().offset(dx, dy, dz);

                    if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity member)) {
                        continue;
                    }

                    if (pos.equals(tank.corner())) {
                        member.setController(tank.width(), tank.height(), content, hasWindows);
                    } else {
                        member.setMember(tank.corner());
                    }
                }
            }
        }

        stampTank(level, tank.corner(), tank.width(), tank.height(), hasWindows);

        var controllerState = level.getBlockState(tank.corner());

        level.sendBlockUpdated(tank.corner(), controllerState, controllerState, Block.UPDATE_CLIENTS);
        updateComparators(level, tank.corner(), tank.width(), tank.height());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Block states
    // ------------------------------------------------------------------------------------------------------------

    private static void stampTank(Level level, BlockPos corner, int width, int height, boolean windows) {
        for (var dy = 0; dy < height; dy++) {
            for (var dx = 0; dx < width; dx++) {
                for (var dz = 0; dz < width; dz++) {
                    var pos = corner.offset(dx, dy, dz);
                    var state = level.getBlockState(pos);

                    if (!(state.getBlock() instanceof FluidTankBlock)) {
                        continue;
                    }

                    var stamped = stamp(state, dx, dy, dz, width, height, windows);

                    if (stamped != state) {
                        level.setBlock(pos, stamped, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    static BlockState stamp(BlockState state, int dx, int dy, int dz, int width, int height, boolean windows) {
        var stamped = state
            .setValue(FluidTankBlock.X_POS, axis(dx, width))
            .setValue(FluidTankBlock.Z_POS, axis(dz, width))
            .setValue(FluidTankBlock.Y_POS, height(dy, height))
            .setValue(FluidTankBlock.X_WINDOW, window(dx, width, windows))
            .setValue(FluidTankBlock.Z_WINDOW, window(dz, width, windows));

        if (stamped.hasProperty(WyFluidTankBlock.PANEL_X)) {
            // 1 x 2 panels, staggered one block between neighbouring columns, counted from the tank's corner.
            stamped = stamped
                .setValue(WyFluidTankBlock.PANEL_X, (dx + dy) % 2 == 0 ? TankPanel.LOWER : TankPanel.UPPER)
                .setValue(WyFluidTankBlock.PANEL_Z, (dz + dy) % 2 == 0 ? TankPanel.LOWER : TankPanel.UPPER);
        }

        return stamped;
    }

    private static TankAxisPos axis(int offset, int width) {
        if (width <= 1) {
            return TankAxisPos.SINGLE;
        }

        return offset == 0 ? TankAxisPos.MIN : offset == width - 1 ? TankAxisPos.MAX : TankAxisPos.MID;
    }

    private static TankHeightPos height(int offset, int height) {
        if (height <= 1) {
            return TankHeightPos.SINGLE;
        }

        return offset == 0 ? TankHeightPos.BOTTOM : offset == height - 1 ? TankHeightPos.TOP : TankHeightPos.MID;
    }

    /** One centred window per face: the middle block on odd widths, straddling the middle seam on even widths. */
    private static TankWindow window(int offset, int width, boolean windows) {
        if (!windows) {
            return TankWindow.NONE;
        }

        if (width % 2 == 1) {
            return offset == width / 2 ? TankWindow.FULL : TankWindow.NONE;
        }

        return offset == width / 2 - 1 ? TankWindow.LOW : offset == width / 2 ? TankWindow.HIGH : TankWindow.NONE;
    }
}
