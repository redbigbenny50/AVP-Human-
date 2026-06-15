package com.human.common.gameplay.entity.living.human.marine;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import com.human.common.gameplay.entity.living.human.AbstractHuman;

public class MarineAnimationDispatcher {

    private static final AzCommand<AbstractHuman> IDLE = AzCommand.<AbstractHuman>idempotent()
        .play(AzTarget.track(MarineAnimationRefs.FULL_BODY_CONTROLLER_NAME), MarineAnimationRefs.IDLE_ANIMATION_NAME, AzPlayBehaviors.LOOP)
        .build();

    private static final AzCommand<AbstractHuman> RIGHT_SHOOT = AzCommand.<AbstractHuman>replay()
        .play(
            AzTarget.track(MarineAnimationRefs.FULL_BODY_CONTROLLER_NAME),
            MarineAnimationRefs.RIGHT_SHOOT_ANIMATION_NAME,
            AzPlayBehaviors.PLAY_ONCE
        )
        .build();

    private static final AzCommand<AbstractHuman> SWIM = AzCommand.<AbstractHuman>idempotent()
        .play(AzTarget.track(MarineAnimationRefs.FULL_BODY_CONTROLLER_NAME), MarineAnimationRefs.SWIM_ANIMATION_NAME, AzPlayBehaviors.LOOP)
        .build();

    private static final AzCommand<AbstractHuman> WALK = AzCommand.<AbstractHuman>idempotent()
        .play(AzTarget.track(MarineAnimationRefs.FULL_BODY_CONTROLLER_NAME), MarineAnimationRefs.WALK_ANIMATION_NAME, AzPlayBehaviors.LOOP)
        .build();

    private final AbstractHuman abstractHuman;

    public MarineAnimationDispatcher(AbstractHuman abstractHuman) {
        this.abstractHuman = abstractHuman;
    }

    public void idle() {
        IDLE.dispatchForEntity(abstractHuman);
    }

    public void swim() {
        SWIM.dispatchForEntity(abstractHuman);
    }

    public void walk() {
        WALK.dispatchForEntity(abstractHuman);
    }

    public void rightShoot() {
        RIGHT_SHOOT.dispatchForEntity(abstractHuman);
    }
}
