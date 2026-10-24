package com.human.client.machine;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import net.minecraft.client.Minecraft;

/**
 * The client side of a machine: its own animation clock and its working sound.
 * <h2>⭐ THE MACHINE'S OWN CLOCK</h2> [stated] Sep 29: machines must not animate in lockstep. The moment a machine is
 * seen to START working, the current game tick is recorded on it; its renderer counts frames from there and its sound
 * starts on the same tick. So each machine starts at frame 0 when it starts, and two started a minute apart stay a
 * minute apart.
 * <p>
 * This reads only the block state the server already syncs - no packet, no server work.
 * </p>
 */
public final class MachineClientHooks {

    private MachineClientHooks() {}

    public static void tick(ElectricMachineBlockEntity machine) {
        var level = machine.getLevel();

        if (level == null) {
            return;
        }

        var working = MachineWorkingSound.isWorking(machine);

        if (working && !machine.clientWorking) {
            machine.clientWorking = true;
            machine.clientWorkStart = level.getGameTime();
            startSound(machine);
        } else if (!working && machine.clientWorking) {
            machine.clientWorking = false;
            // The sound notices on its own tick and stops itself.
        } else if (working && machine.clientSound == null && MachineWorkingSound.inRange(machine)) {
            // Walked back into range of a machine that kept working: pick the loop back up.
            startSound(machine);
        }

        if (machine.clientSound instanceof MachineWorkingSound sound && sound.isStopped()) {
            machine.clientSound = null;
        }

        // [stated] Oct 17: the vaporizer animates its geo model rather than a texture strip.
        if (machine.getKind() == com.human.common.gameplay.machine.ElectricMachineKind.VAPORIZER) {
            VaporizerClientHooks.tick(machine, working);
        }

        // [stated] Oct 17: the distributor's fog - placeholder particles in the vapor's colour.
        if (machine instanceof com.human.common.gameplay.distributor.DistributorBlockEntity distributor) {
            DistributorClientHooks.tick(distributor, working);
        }
    }

    private static void startSound(ElectricMachineBlockEntity machine) {
        if (!MachineWorkingSound.inRange(machine)) {
            return;
        }

        var sound = new MachineWorkingSound(machine);

        machine.clientSound = sound;
        Minecraft.getInstance().getSoundManager().play(sound);
    }
}
