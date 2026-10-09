package com.human.common.gameplay.block.cryo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Forms cryotanks. [stated] Oct 1-2: a tank is a SOLID box of cryotank blocks like the fluid tanks - a 4x4x4 needs 64
 * blocks - up to 15x15x15, and one block on its own is a 1x1x1 tank.
 * <h2>When it runs</h2> On every place and break, and never from a tick. [stated] Oct 1: placing or breaking re-forms
 * the structure and loads the few chunks it needs - and it must never skip, leaving blocks pointing at a master that is
 * gone (the fluid tank's fluid-loss bug). So the walk reads the blocks it needs even where that loads their chunk; a
 * 15x15x15 box spans at most 4 chunks.
 * <h2>Shapes that are not a square-based box</h2> A cluster that is not a complete box on a SQUARE base - an L, a box
 * with a hole, a 1x3 or 2x4 footprint - is not one tank: every block in it becomes its own 1x1x1 tank until the player
 * finishes the shape.
 * <h2>Specimens survive a re-form</h2> Every specimen in the cluster is lifted out first and put back into whichever
 * new tank now covers its old master - if that tank is free and still big enough. Only a specimen with nowhere to go is
 * released, and then at its old tank, awake.
 */
public final class CryotankFormation {

    public static final int MAX_SIZE = 15;

    /** One more than the largest legal tank, so an over-size cluster is noticed instead of walked forever. */
    private static final int MAX_WALK = MAX_SIZE * MAX_SIZE * MAX_SIZE + 1;

    private CryotankFormation() {}

    /** Re-forms the cluster containing {@code start}. Does nothing if {@code start} is not a cryotank. */
    public static void reform(ServerLevel level, BlockPos start) {
        if (!(level.getBlockState(start).getBlock() instanceof CryotankBlock)) {
            return;
        }

        var cluster = walk(level, start);

        record Held(
            BlockPos oldMaster,
            CompoundTag tag,
            float scale,
            float width,
            float height,
            int thaw,
            @org.jetbrains.annotations.Nullable CompoundTag card,
            int priority,
            boolean deactivated
        ) {}

        var held = new ArrayList<Held>();

        for (var pos : cluster) {
            if (level.getBlockEntity(pos) instanceof CryotankBlockEntity tank && tank.isMaster() && tank.hasSpecimen()) {
                held.add(
                    new Held(
                        pos,
                        tank.getSpecimen(),
                        tank.getScale(),
                        tank.specimenWidth(),
                        tank.specimenHeight(),
                        tank.getThawTicks(),
                        tank.getCard(),
                        tank.getPriority(),
                        tank.isDeactivated()
                    )
                );
                // Cleared without a client update: setShape below sends one for every block anyway.
                tank.setSpecimen(null, 1.0F, 0.0F, 0.0F);
            }
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (var pos : cluster) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        int sx = maxX - minX + 1, sy = maxY - minY + 1, sz = maxZ - minZ + 1;
        // [stated] Oct 2: the base must be SQUARE - 1x1, 2x2, 3x3 ... up to 15x15 - at any height up to 15. A 1x3 or
        // 2x4 footprint does not form.
        var isBox = cluster.size() < MAX_WALK && sx == sz && sx <= MAX_SIZE && sy <= MAX_SIZE
            && (long) sx * sy * sz == cluster.size();

        var boxMaster = new BlockPos(minX, minY, minZ);

        for (var pos : cluster) {
            var state = level.getBlockState(pos);

            if (state.getBlock() instanceof CryotankBlock block) {
                // ⚠ The SAME block in a new state: onRemove/onPlace see their own block and do nothing, the block
                // entity survives, and UPDATE_CLIENTS without neighbour updates keeps this from cascading.
                var look = isBox
                    ? block.shapeState(true, pos.getX() - minX, pos.getY() - minY, pos.getZ() - minZ, sx, sy, sz)
                    : block.shapeState(false, 0, 0, 0, 1, 1, 1);

                if (look != state) {
                    level.setBlock(pos, look, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
            }

            if (level.getBlockEntity(pos) instanceof CryotankBlockEntity tank) {
                var changed = isBox ? tank.setShape(boxMaster, sx, sy, sz) : tank.setShape(pos, 1, 1, 1);

                if (changed) {
                    tank.markUpdated();
                }
            }
        }

        for (var specimen : held) {
            var targetPos = isBox ? boxMaster : specimen.oldMaster();
            var placed = false;

            if (level.getBlockEntity(targetPos) instanceof CryotankBlockEntity target && !target.hasSpecimen()) {
                var scale = CryoSpecimens.fitScale(target, specimen.width(), specimen.height());

                if (scale > 0.0F) {
                    target.setSpecimen(specimen.tag(), scale, specimen.width(), specimen.height());
                    target.setThawTicks(specimen.thaw());
                    // The sheet, the tube's priority and a deactivation all move with the specimen.
                    target.setCard(specimen.card());
                    target.setPriority(specimen.priority());
                    target.setDeactivated(specimen.deactivated());
                    placed = true;
                }
            }

            if (
                !placed && !CryoSpecimens.releaseNear(
                    level,
                    specimen.tag(),
                    specimen.oldMaster(),
                    1,
                    1,
                    1,
                    specimen.width(),
                    specimen.height(),
                    true
                )
            ) {
                // ⚠ A release can fail (a UUID clash, a damaged save). The specimen must never be dropped on the floor
                // of
                // the code: it goes into any free tank in the cluster, fit or not, and only if there is none is it
                // lost.
                stash(level, cluster, specimen.tag(), specimen.width(), specimen.height());
            }
        }
    }

    /**
     * Last resort for a specimen that could neither be placed nor released: any empty tank master among {@code where}.
     *
     * @return false only if there was nowhere at all - logged, because it means a specimen was lost
     */
    static boolean stash(ServerLevel level, List<BlockPos> where, CompoundTag tag, float width, float height) {
        for (var pos : where) {
            if (level.getBlockEntity(pos) instanceof CryotankBlockEntity tank && tank.isMaster() && !tank.hasSpecimen()) {
                tank.setSpecimen(tag, 1.0F, width, height);
                return true;
            }
        }

        com.human.Human.LOGGER.error("[Cryo] A specimen could not be placed, released or stashed and was lost: {}", tag.getString("id"));
        return false;
    }

    /** Re-forms every distinct cluster touching {@code pos} - used after a block is broken out of a tank. */
    public static void reformAround(ServerLevel level, BlockPos pos) {
        Set<BlockPos> done = new HashSet<>();

        for (var direction : Direction.values()) {
            var neighbour = pos.relative(direction);

            if (done.contains(neighbour) || !(level.getBlockState(neighbour).getBlock() instanceof CryotankBlock)) {
                continue;
            }

            done.addAll(walk(level, neighbour));
            reform(level, neighbour);
        }
    }

    private static List<BlockPos> walk(ServerLevel level, BlockPos start) {
        var seen = new HashSet<BlockPos>();
        var order = new ArrayList<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();

        seen.add(start.immutable());
        queue.add(start.immutable());

        while (!queue.isEmpty() && order.size() < MAX_WALK) {
            var pos = queue.poll();
            order.add(pos);

            for (var direction : Direction.values()) {
                var next = pos.relative(direction);

                if (seen.add(next) && level.getBlockState(next).getBlock() instanceof CryotankBlock) {
                    queue.add(next);
                }
            }
        }

        return order;
    }
}
