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

    private final Entity entity;

    public SentryTurretAnimDispatcher(Entity entity) {
        this.entity = entity;
    }

    public void idle() {
        IDLE_COMMAND.dispatchForEntity(entity);
    }

    public void unpowered() {
        UNPOWERED_COMMAND.dispatchForEntity(entity);
    }

    public void firing() {
        FIRING_COMMAND.dispatchForEntity(entity);
    }

}
