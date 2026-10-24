package com.human.common.gameplay.fabrication;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;

/**
 * The fabricator's two clips from fabricator.animation.json. Played on the client only, and only when printing starts
 * or stops - every fabricator runs its own clock, so a row of them never moves in lockstep ([stated] Sep 29).
 */
public class FabricatorAnimationDispatcher {

    private static final String TRACK = "base_controller";

    private static final AzCommand<FabricatorBlockEntity> PRINTING = AzCommand.<FabricatorBlockEntity>idempotent()
        .play(AzTarget.track(TRACK), "fabricating", AzPlayBehaviors.LOOP)
        .build();

    private static final AzCommand<FabricatorBlockEntity> IDLE = AzCommand.<FabricatorBlockEntity>idempotent()
        .play(AzTarget.track(TRACK), "idle.loop", AzPlayBehaviors.LOOP)
        .build();

    /**
     * @return whether the clip reached the fabricator. ⚠ Oct 17 fix - BLib drops a command silently when the block
     *         entity has no animator yet (it is made on first render), so the caller retries until this says yes.
     */
    public boolean play(FabricatorBlockEntity fabricator, boolean printing) {
        if (fabricator.getLevel() == null || !fabricator.getLevel().isClientSide()) {
            return false;
        }

        if (com.blib.internal.client.animation.AzAnimatorAccessor.getOrNull(fabricator) == null) {
            return false;
        }

        (printing ? PRINTING : IDLE).dispatchForBlockEntity(fabricator);
        return true;
    }
}
