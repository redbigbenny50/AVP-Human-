package com.human.common.gameplay.entity.machine;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.Entity;

public class SentryTurretAnimDispatcher {

    private static final AzCommand<Entity> IDLE_COMMAND = AzCommand.<Entity>idempotent()
        .play(AzTarget.track("base_controller"), "animation.idle", AzPlayBehaviors.LOOP)
        .build();

    private static final AzCommand<Entity> UNPOWERED_COMMAND = AzCommand.<Entity>idempotent()
        .play(AzTarget.track("base_controller"), "animation.unpowered", AzPlayBehaviors.HOLD_ON_LAST_FRAME)
        .build();

    private static final AzCommand<Entity> FIRING_COMMAND = AzCommand.<Entity>idempotent()
        .play(AzTarget.track("base_controller"), "animation.firing", AzPlayBehaviors.LOOP)
        .build();

    /**
     * The clip the turret should be playing, as sent to clients: {@link #IDLE}, {@link #UNPOWERED} or {@link #FIRING}.
     */
    public static final byte IDLE = 0;

    public static final byte UNPOWERED = 1;

    public static final byte FIRING = 2;

    private final SentryTurret turret;

    /** Client only: the clip last started, so a clip is started once when it changes, not every tick. */
    private byte applied = -1;

    public SentryTurretAnimDispatcher(SentryTurret turret) {
        this.turret = turret;
    }

    // ⚠ Oct 16 fix - BLib animation commands only play on the CLIENT, and every call below came from the turret's
    // SERVER tick, so none of them ever played: a placed turret sat in its raw model pose, which has the muzzle flash
    // showing ([stated] Oct 16: "when putting the turret down it has a firing flare"). The server now only records
    // which clip it wants in synced entity data (sent only when it changes); each client starts that clip itself.

    public void idle() {
        turret.setAnimState(IDLE);
    }

    public void unpowered() {
        turret.setAnimState(UNPOWERED);
    }

    public void firing() {
        turret.setAnimState(FIRING);
    }

    /** Client tick: start the clip the server asked for, if it changed. Idle (flash hidden) from the first tick. */
    public void applyOnClient() {
        var wanted = turret.getAnimState();

        if (wanted == applied) {
            return;
        }

        // ⚠ Oct 17 fix - BLib drops a command silently when the entity has no animator yet, and the animator is only
        // made the first time the turret is RENDERED. The first client ticks of a freshly placed turret come before
        // that, so the idle clip (the one that hides the muzzle flash) was dropped - and marked as sent, so it was
        // never sent again: the flash stayed lit for good. Now it only counts as sent once there is an animator to
        // receive it, and is retried every tick until then.
        if (com.blib.internal.client.animation.AzAnimatorAccessor.getOrNull(turret) == null) {
            return;
        }

        applied = wanted;

        switch (wanted) {
            case UNPOWERED -> UNPOWERED_COMMAND.dispatchForEntity(turret);
            case FIRING -> FIRING_COMMAND.dispatchForEntity(turret);
            default -> IDLE_COMMAND.dispatchForEntity(turret);
        }
    }
}
