package com.human.common.gameplay.entity.nuke;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;

public class MushroomCloudAnimDispatcher {

    private static final AzCommand<MushroomCloudEntity> EXPLODE_COMMAND = AzCommand.<MushroomCloudEntity>replay()
        .play(AzTarget.track("base_controller"), "animation.explode", AzPlayBehaviors.HOLD_ON_LAST_FRAME)
        .build();

    private MushroomCloudEntity mushroomCloudEntity;

    public MushroomCloudAnimDispatcher(MushroomCloudEntity mushroomCloudEntity) {
        this.mushroomCloudEntity = mushroomCloudEntity;
    }

    public void explode() {
        if (!mushroomCloudEntity.level().isClientSide()) {
            return;
        }

        EXPLODE_COMMAND.dispatchForEntity(mushroomCloudEntity);
    }
}
