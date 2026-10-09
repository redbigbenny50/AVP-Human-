package com.human.client.machine;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.block.power.machine.ElectricMachineBlock;
import com.human.common.gameplay.machine.MachineState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * One machine's working loop.
 * <p>
 * ⭐ ONE LOOPING SOUND, NOT ONE SOUND PER CYCLE. Re-triggering a clip every cycle lands up to a tick late each time, and
 * the gaps are audible; a looping instance repeats sample-accurately. It starts on the same client tick as the
 * machine's animation clock ({@link MachineClientHooks}), so for a machine whose sound is timed to its picture - the
 * press - the two begin together.
 * </p>
 * <p>
 * It stops itself the moment the machine stops working, is broken, or the player walks out of range, so a factory the
 * player has left behind costs nothing.
 * </p>
 */
public class MachineWorkingSound extends AbstractTickableSoundInstance {

    /** Beyond this the loop is stopped outright rather than left playing at zero volume. */
    static final double RANGE = 20.0D;

    private final ElectricMachineBlockEntity machine;

    public MachineWorkingSound(ElectricMachineBlockEntity machine) {
        super(machine.getKind().workingSound(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.machine = machine;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.6F;
        this.pitch = 1.0F;

        var pos = machine.getBlockPos();

        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
    }

    @Override
    public void tick() {
        if (machine.isRemoved() || !isWorking(machine) || !inRange(machine)) {
            stop();
        }
    }

    static boolean isWorking(ElectricMachineBlockEntity machine) {
        var state = machine.getBlockState();

        return state.hasProperty(ElectricMachineBlock.STATE) && state.getValue(ElectricMachineBlock.STATE) == MachineState.WORKING;
    }

    static boolean inRange(ElectricMachineBlockEntity machine) {
        var player = Minecraft.getInstance().player;

        return player != null && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(machine.getBlockPos())) <= RANGE * RANGE;
    }
}
