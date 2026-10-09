package com.human.common.gameplay.power.grid;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class PowerGridDSU {

    // Maps each block position to the root of its grid (used for DSU)
    private final Map<BlockPos, BlockPos> parentMap;

    public PowerGridDSU() {
        this.parentMap = new HashMap<>();
    }

    /**
     * True when this position has already been linked into a grid. ⭐ The whole point of the fast path: a node whose
     * network was already discovered by an earlier node does no work at all.
     */
    public boolean isKnown(BlockPos pos) {
        return parentMap.containsKey(pos);
    }

    /**
     * Finds the root BlockPos for the grid that contains the given position.
     * <p>
     * ⚠ ITERATIVE ON PURPOSE. This was recursive, and the recursion depth is the length of the parent chain - a long
     * cable run built one block at a time makes a long chain, and a deep one overflows the stack on the server thread.
     * The loop below cannot.
     * </p>
     */
    public BlockPos find(BlockPos pos) {
        var parent = parentMap.get(pos);

        if (parent == null) {
            parentMap.put(pos, pos);
            return pos;
        }

        var root = pos;

        while (true) {
            var next = parentMap.get(root);

            if (next == null || next.equals(root)) {
                break;
            }

            root = next;
        }

        // Path compression: point everything on the walk straight at the root.
        var current = pos;

        while (!current.equals(root)) {
            var next = parentMap.get(current);

            parentMap.put(current, root);

            if (next == null) {
                break;
            }

            current = next;
        }

        return root;
    }

    public void remove(BlockPos pos) {
        parentMap.remove(pos);
    }

    public void setParent(BlockPos rootA, BlockPos rootB) {
        parentMap.put(rootA, rootB);
    }
}
