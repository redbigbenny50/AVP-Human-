package com.human.common.gameplay.fabrication;

import com.human.common.gameplay.block.item.ItemPipeBlock;
import com.human.common.gameplay.item.pipe.ItemPipeSystem;
import com.human.common.gameplay.item.pipe.PipeItemAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * [stated] Oct 13: the storage a fabricator can see and print from - anything TOUCHING any of its four blocks, and
 * anything on an item pipe network a pipe touching it belongs to.
 * <h2>RULES</h2>
 * <ul>
 * <li>Each inventory counts once, however many faces or pipes reach it (double chests and crates have one
 * identity).</li>
 * <li>MACHINES are left out - a furnace's input is not a store of materials - and so is the fabricator itself.</li>
 * <li>Unloaded blocks are skipped, never loaded.</li>
 * <li>At most {@value #MAX_SOURCES} inventories, so a huge network can never turn one look into a lag spike.</li>
 * </ul>
 * <h2>COST</h2> 4 blocks x 6 faces, plus a map lookup per touching pipe for its network's end list. Called while the
 * screen is open (twice a second at most) and when a print starts or finishes - never per tick.
 */
public final class FabricatorSources {

    public static final int MAX_SOURCES = 64;

    private FabricatorSources() {}

    public static List<PipeItemAccess.Port> connected(ServerLevel level, FabricatorBlockEntity fabricator) {
        var ports = new ArrayList<PipeItemAccess.Port>();
        var seen = new HashSet<Object>();
        var state = fabricator.getBlockState();

        if (!(state.getBlock() instanceof FabricatorBlock)) {
            return ports;
        }

        var core = fabricator.getBlockPos();
        var facing = state.getValue(FabricatorBlock.FACING);
        var pipes = ItemPipeSystem.get(level);

        seen.add(fabricator);

        for (var part : FabricatorPart.values()) {
            var pos = part.fromCore(core, facing);

            for (var direction : Direction.values()) {
                var neighbour = pos.relative(direction);

                if (!level.isLoaded(neighbour) || isPartOf(level, neighbour, core)) {
                    continue;
                }

                if (level.getBlockState(neighbour).getBlock() instanceof ItemPipeBlock) {
                    for (var attached : pipes.attachedTo(neighbour)) {
                        if (level.isLoaded(attached.target())) {
                            add(ports, seen, PipeItemAccess.open(level, attached.target(), attached.face().getOpposite()));
                        }
                    }
                } else {
                    add(ports, seen, PipeItemAccess.open(level, neighbour, direction.getOpposite()));
                }

                if (ports.size() >= MAX_SOURCES) {
                    return ports;
                }
            }
        }

        return ports;
    }

    private static boolean isPartOf(ServerLevel level, BlockPos pos, BlockPos core) {
        var state = level.getBlockState(pos);

        return state.getBlock() instanceof FabricatorBlock && FabricatorBlock.corePos(pos, state).equals(core);
    }

    private static void add(List<PipeItemAccess.Port> ports, java.util.Set<Object> seen, PipeItemAccess.Port port) {
        if (ports.size() < MAX_SOURCES && port.present() && !port.isMachine() && seen.add(port.storage())) {
            ports.add(port);
        }
    }
}
