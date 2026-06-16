package com.human.common.gameplay.block.entity.power.impl;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;

public class ResonatorAnimationDispatcher {

    private static final AzCommand<ResonatorBlockEntity> POWER_UP_COMMAND = AzCommand.<ResonatorBlockEntity>replay()
        .play(AzTarget.track("base_controller"), "animation.activate", AzPlayBehaviors.PLAY_ONCE)
        .build();

    private static final AzCommand<ResonatorBlockEntity> UNPOWERED_COMMAND = AzCommand.<ResonatorBlockEntity>idempotent()
        .play(AzTarget.track("base_controller"), "animation.deactivate", AzPlayBehaviors.HOLD_ON_LAST_FRAME)
        .build();

    private static final AzCommand<ResonatorBlockEntity> SPINNING_COMMAND = AzCommand.<ResonatorBlockEntity>idempotent()
        .play(AzTarget.track("base_controller"), "animation.spinning", AzPlayBehaviors.LOOP)
        .build();

    public ResonatorAnimationDispatcher() {}

    public void unpowered(ResonatorBlockEntity entity) {
        if (entity.getLevel() == null || !entity.getLevel().isClientSide()) {
            return;
        }

        UNPOWERED_COMMAND.dispatchForBlockEntity(entity);
    }

    public void powered(ResonatorBlockEntity entity) {
        if (entity.getLevel() == null || !entity.getLevel().isClientSide()) {
            return;
        }

        SPINNING_COMMAND.dispatchForBlockEntity(entity);
    }

    public void powerUp(ResonatorBlockEntity entity) {
        if (entity.getLevel() == null || !entity.getLevel().isClientSide()) {
            return;
        }

        POWER_UP_COMMAND.dispatchForBlockEntity(entity);
    }
}
