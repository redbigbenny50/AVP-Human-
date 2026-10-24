package com.human.client.machine;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import com.human.client.animation.block.VaporizerAnimator;
import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;

/**
 * The vaporizer's model animation, played on the client from the machine's synced working state - the same state its
 * sound follows. [stated] Oct 17: while it runs the gauge needle sits at working pressure, the screen scrolls, the
 * yellow lamp blinks, the liquid boils behind the windows and the relief valve pops; idle, all of that stops.
 * <h2>⚠ THE FIRST-FRAME RULE</h2> BLib silently drops a command sent before the animator exists, and the animator is
 * only made the first time the machine is drawn (the sentry turret's lit-muzzle bug, Oct 17). So the clip is only
 * marked as played once it actually reached an animator; until then it is tried again every tick.
 */
public final class VaporizerClientHooks {

    private static final AzCommand<ElectricMachineBlockEntity> RUNNING = AzCommand.<ElectricMachineBlockEntity>idempotent()
        .play(AzTarget.track(VaporizerAnimator.TRACK), "running", AzPlayBehaviors.LOOP)
        .build();

    private static final AzCommand<ElectricMachineBlockEntity> IDLE = AzCommand.<ElectricMachineBlockEntity>idempotent()
        .play(AzTarget.track(VaporizerAnimator.TRACK), "idle", AzPlayBehaviors.LOOP)
        .build();

    /** Per-machine memory of what was last played - weak, so an unloaded machine is forgotten with it. */
    private static final java.util.Map<ElectricMachineBlockEntity, Boolean> PLAYED = new java.util.WeakHashMap<>();

    private VaporizerClientHooks() {}

    public static void tick(ElectricMachineBlockEntity machine, boolean working) {
        var played = PLAYED.get(machine);

        if (played != null && played == working) {
            return;
        }

        if (com.blib.internal.client.animation.AzAnimatorAccessor.getOrNull(machine) == null) {
            return;
        }

        (working ? RUNNING : IDLE).dispatchForBlockEntity(machine);
        PLAYED.put(machine, working);
    }
}
