package com.human.common.gameplay.cosmetic;

import com.human.common.gameplay.block.item.ItemPipeBlock;
import com.human.common.gameplay.item.pipe.ItemPipeSystem;
import com.human.common.gameplay.item.pipe.PipeItemAccess;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The storage a Cosmetic Crafter reads from - [stated] Oct 31: "materials in input and network". Exactly the
 * fabricator's rules ({@code FabricatorSources}), over all twelve of the crafter's blocks:
 * <ul>
 * <li>anything TOUCHING any of its blocks, and anything on an item pipe network a pipe touching it belongs to;</li>
 * <li>each inventory once, however many faces or pipes reach it;</li>
 * <li>machines left out, unloaded blocks skipped and never loaded, at most {@value #MAX_SOURCES} inventories.</li>
 * </ul>
 * Called while the screen is open (twice a second at most) and when a craft starts or finishes - never per tick.
 */
public final class CosmeticCrafterSources {

    public static final int MAX_SOURCES = 64;

    private CosmeticCrafterSources() {}

    public static List<PipeItemAccess.Port> connected(ServerLevel level, CosmeticCrafterBlockEntity crafter) {
        var ports = new ArrayList<PipeItemAccess.Port>();
        var seen = new HashSet<Object>();
        var state = crafter.getBlockState();

        if (!(state.getBlock() instanceof CosmeticCrafterBlock block)) {
            return ports;
        }

        var core = crafter.getBlockPos();
        var facing = state.getValue(CosmeticMultiblock.FACING);
        var pipes = ItemPipeSystem.get(level);

        seen.add(crafter);

        var cells = block.cells();

        for (int index = 0; index < cells.length; index++) {
            var pos = CosmeticMultiblock.cellPos(core, facing, cells[index]);

            for (var direction : Direction.values()) {
                var neighbour = pos.relative(direction);

                if (!level.isLoaded(neighbour) || block.isCellOf(level, neighbour, core)) {
                    continue;
                }

                if (level.getBlockState(neighbour).getBlock() instanceof ItemPipeBlock) {
                    // [stated] Nov 1: a pipe network is reached only through the ITEM PORT - the one face a pipe
                    // visibly connects to. Storage touching the machine directly still counts on any face.
                    if (index != CosmeticCrafterBlock.ITEM_PORT_CELL || direction != facing.getClockWise()) {
                        continue;
                    }

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

    private static void add(List<PipeItemAccess.Port> ports, Set<Object> seen, PipeItemAccess.Port port) {
        if (ports.size() < MAX_SOURCES && port.present() && !port.isMachine() && seen.add(port.storage())) {
            ports.add(port);
        }
    }
}
