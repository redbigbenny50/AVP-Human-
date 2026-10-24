package com.human.client.machine;

import com.human.common.gameplay.block.power.machine.ElectricMachineBlock;
import com.human.common.gameplay.distributor.DistributorBlockEntity;
import com.human.common.gameplay.distributor.DistributorFog;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;

/**
 * The distributor's fog on the client - [stated] Oct 17: "For now: simple fog particles in the vapor's colour." The
 * real fog visuals are a later job; they will read the same strength map.
 * <h2>WHAT IT DOES</h2> While the machine is WORKING (the block state the server already syncs) and the client knows
 * its vapor, this client scans the room itself ({@link com.human.common.gameplay.distributor.DistributorRoom} - the
 * same class the server uses, so no packet carries the room) and scatters dust in the vapor's colour over the blocks
 * the fog reaches. Each puff is kept with a chance equal to the fog's strength there, so the cloud is thick in the
 * middle, thins over the outer 6 blocks and rounds off at the corners - and stops dead at walls.
 * <h2>COST</h2>
 * <ul>
 * <li>Nothing at all beyond {@value #VIEW_DISTANCE} blocks, or while the machine is idle.</li>
 * <li>The scan is a budget of blocks per tick, repeated every {@value #RESCAN_TICKS} ticks so opened doors and new
 * walls show up.</li>
 * <li>At most {@value #MAX_PUFFS_PER_TICK} particles a tick per distributor, scaled to the room's size, and one shared
 * particle option per vapor - nothing allocated per particle beyond what vanilla does.</li>
 * </ul>
 */
public final class DistributorClientHooks {

    private static final int VIEW_DISTANCE = 64;

    private static final int RESCAN_TICKS = 100;

    private static final int SCAN_BUDGET = 2048;

    private static final int MAX_PUFFS_PER_TICK = 24;

    /** One puff per this many fog blocks, so a closet and a hangar look about as thick. */
    private static final int BLOCKS_PER_PUFF = 300;

    private DistributorClientHooks() {}

    public static void tick(DistributorBlockEntity distributor, boolean working) {
        var level = distributor.getLevel();
        var minecraft = Minecraft.getInstance();
        var kind = distributor.clientVapor;

        if (level == null || minecraft.player == null || !working || kind == null) {
            return;
        }

        var pos = distributor.getBlockPos();

        if (minecraft.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > VIEW_DISTANCE * VIEW_DISTANCE) {
            return;
        }

        var room = distributor.clientRoom;
        var now = level.getGameTime();
        var state = distributor.getBlockState();
        var front = state.hasProperty(ElectricMachineBlock.FACING) ? state.getValue(ElectricMachineBlock.FACING) : Direction.NORTH;

        if (!room.isRunning() && (!room.hasResult() || !room.origin().equals(pos) || now - distributor.clientScanStarted >= RESCAN_TICKS)) {
            distributor.clientScanStarted = now;
            room.start(level, pos, front);
        }

        room.step(level, SCAN_BUDGET, now);

        var size = room.size();

        if (!room.hasResult() || size == 0) {
            return;
        }

        // [stated] Oct 18 - the fog is the vapor's own cloud style: big, slow puffs.
        var style = com.human.common.gameplay.vapor.VaporLook.of(kind).cloud();
        var random = level.random;
        var puffs = Math.max(2, Math.min(MAX_PUFFS_PER_TICK, size / BLOCKS_PER_PUFF));
        var sizeAxis = DistributorFog.SIZE;
        var radius = DistributorFog.RADIUS;

        for (var i = 0; i < puffs; i++) {
            var index = room.cell(random.nextInt(size));

            // Kept with a chance equal to the strength: the fade and the rounded corners come from this.
            if (random.nextFloat() >= DistributorFog.strength(index)) {
                continue;
            }

            var dz = index % sizeAxis - radius;
            var dy = index / sizeAxis % sizeAxis - radius;
            var dx = index / (sizeAxis * sizeAxis) - radius;

            level.addParticle(
                com.human.common.gameplay.vapor.VaporParticleOptions.of(kind, style, 1.2F, 2.4F, 36 + random.nextInt(20)),
                pos.getX() + dx + random.nextDouble(),
                pos.getY() + dy + random.nextDouble(),
                pos.getZ() + dz + random.nextDouble(),
                0.0D,
                0.0D,
                0.0D
            );
        }

        // A steady puff out of the fan itself, so the fog visibly comes from the machine.
        level.addParticle(
            com.human.common.gameplay.vapor.VaporParticleOptions.of(kind, style, 0.5F, 1.4F, 30),
            pos.getX() + 0.5D + front.getStepX() * 0.7D,
            pos.getY() + 0.5D,
            pos.getZ() + 0.5D + front.getStepZ() * 0.7D,
            0.0D,
            0.0D,
            0.0D
        );
    }
}
