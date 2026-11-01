package com.human.common.gameplay.gene.machine.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Gene Storage network ([stated] Oct 27/28): machines see every Gene Storage on their network unless it is set
 * ISOLATED; with several storages the NEAREST fills first, and each storage gets a fill order automatically when it is
 * placed (shown on its screen, NEVER when isolated).
 * <p>
 * Storages register themselves here when their block entity loads and leave when it unloads or breaks, so this only
 * ever lists storages in loaded chunks - nothing is scanned.
 * </p>
 * <p>
 * [stated] Oct 29: "on the network" means on the SAME POWER NETWORK - a storage counts for a machine only when both are
 * wired into the same power grid. Grid membership is asked of the power system's union-find (gridAt), which is cheap
 * and never walks the world.
 * </p>
 */
public final class GeneStorageNetwork {

    /** What a machine needs from a storage block entity. */
    public interface Member {

        BlockPos storagePos();

        /** Where this storage connects to the power grid (its power port). */
        BlockPos gridPos();

        boolean isIsolated();

        /** The order this storage was placed in on its network; ties in distance fill the earlier one first. */
        int fillOrder();

        GeneStorageInventory rack();

        /** Called after the network put tubes in or took them out, so the block entity saves and syncs. */
        void rackChanged();
    }

    private static final Map<ResourceKey<Level>, Map<BlockPos, Member>> MEMBERS = new HashMap<>();

    private GeneStorageNetwork() {}

    public static synchronized void join(Level level, Member member) {
        // Server only: in singleplayer the client's copy of a storage has the same dimension key, and must never be
        // mistaken for the real one.
        if (level.isClientSide()) {
            return;
        }
        MEMBERS.computeIfAbsent(level.dimension(), key -> new HashMap<>()).put(member.storagePos().immutable(), member);
    }

    public static synchronized void leave(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        var members = MEMBERS.get(level.dimension());
        if (members != null) {
            members.remove(pos);
        }
    }

    /** A fill order for a newly placed storage: one past the highest already on its network. */
    public static synchronized int nextFillOrder(Level level, BlockPos pos, BlockPos gridPos) {
        var highest = 0;
        for (var member : onGrid(level, pos, gridPos, true)) {
            highest = Math.max(highest, member.fillOrder());
        }
        return Math.max(highest + 1, 1);
    }

    /**
     * The non-isolated storages a machine at {@code pos} can use, nearest first. {@code gridPos} is where the machine
     * connects to the power grid; a machine on no grid sees no storage.
     */
    public static synchronized List<Member> storagesFor(Level level, BlockPos pos, BlockPos gridPos) {
        return onGrid(level, pos, gridPos, false);
    }

    /** Every storage on the same grid as {@code gridPos}, ISOLATED ones included - for the screen's figures. */
    public static synchronized List<Member> allOnGrid(Level level, BlockPos pos, BlockPos gridPos) {
        return onGrid(level, pos, gridPos, true);
    }

    private static List<Member> onGrid(Level level, BlockPos pos, BlockPos gridPos, boolean includeIsolated) {
        var members = MEMBERS.get(level.dimension());
        var list = new ArrayList<Member>();
        if (members == null || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return list;
        }
        var power = com.human.common.gameplay.power.PowerSystem.get(serverLevel);
        var grid = power.gridAt(gridPos);
        if (grid == null) {
            return list;
        }
        // A storage whose block entity is gone (or belongs to an old world in singleplayer) is dropped here, so the
        // static registry can never hold a stale one.
        members.values()
            .removeIf(
                member -> member instanceof net.minecraft.world.level.block.entity.BlockEntity entity
                    && (entity.isRemoved() || entity.getLevel() != level)
            );
        for (var member : members.values()) {
            if (!includeIsolated && member.isIsolated()) {
                continue;
            }
            if (power.gridAt(member.gridPos()) == grid) {
                list.add(member);
            }
        }
        list.sort(
            Comparator.comparingDouble((Member m) -> m.storagePos().distSqr(pos)).thenComparingInt(Member::fillOrder)
        );
        return list;
    }

    /**
     * Stores {@code stack} on the network of a machine at {@code pos}, nearest storage first, and returns what did not
     * fit (empty when it all went in). The argument is not modified.
     */
    public static ItemStack store(Level level, BlockPos pos, BlockPos gridPos, ItemStack stack) {
        var remaining = stack.copy();
        for (var member : storagesFor(level, pos, gridPos)) {
            if (remaining.isEmpty()) {
                break;
            }
            var before = remaining.getCount();
            remaining = member.rack().insert(remaining);
            if (remaining.getCount() != before) {
                member.rackChanged();
            }
        }
        return remaining;
    }

    /** True when the network has room for at least one of this tube (machines warn NETWORK FULL otherwise). */
    public static boolean hasRoomFor(Level level, BlockPos pos, BlockPos gridPos, ItemStack stack) {
        for (var member : storagesFor(level, pos, gridPos)) {
            if (member.rack().hasRoomFor(stack)) {
                return true;
            }
        }
        return false;
    }

    public static int totalTubes(Level level, BlockPos pos, BlockPos gridPos) {
        var total = 0;
        for (var member : storagesFor(level, pos, gridPos)) {
            total += member.rack().tubeCount();
        }
        return total;
    }
}
