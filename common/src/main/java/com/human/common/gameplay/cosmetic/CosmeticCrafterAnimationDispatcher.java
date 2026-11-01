package com.human.common.gameplay.cosmetic;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;

/**
 * The crafter's three clips from cosmetic_crafter.animation.json: {@code crafting.loop} while power is reaching a
 * craft, {@code idle.loop} otherwise, and {@code complete} (the output drawer sliding out and back) once per finished
 * item. Client only; each crafter runs its own clock.
 */
public class CosmeticCrafterAnimationDispatcher {

    private static final String TRACK = "base_controller";

    /** How long the loops wait for {@code complete} to play out (its 1.5 s, in ticks). */
    public static final int COMPLETE_TICKS = 30;

    private static final AzCommand<CosmeticCrafterBlockEntity> CRAFTING = AzCommand.<CosmeticCrafterBlockEntity>idempotent()
        .play(AzTarget.track(TRACK), "crafting.loop", AzPlayBehaviors.LOOP)
        .build();

    private static final AzCommand<CosmeticCrafterBlockEntity> IDLE = AzCommand.<CosmeticCrafterBlockEntity>idempotent()
        .play(AzTarget.track(TRACK), "idle.loop", AzPlayBehaviors.LOOP)
        .build();

    private static final AzCommand<CosmeticCrafterBlockEntity> COMPLETE = AzCommand.<CosmeticCrafterBlockEntity>idempotent()
        .play(AzTarget.track(TRACK), "complete", AzPlayBehaviors.PLAY_ONCE)
        .build();

    private static boolean ready(CosmeticCrafterBlockEntity crafter) {
        // BLib drops a command silently when the block entity has no animator yet (made on first render) - the
        // fabricator's Oct 17 fix: the caller retries until this says yes.
        return crafter.getLevel() != null && crafter.getLevel().isClientSide() && com.blib.internal.client.animation.AzAnimatorAccessor
            .getOrNull(crafter) != null;
    }

    public boolean loop(CosmeticCrafterBlockEntity crafter, boolean crafting) {
        if (!ready(crafter)) {
            return false;
        }

        (crafting ? CRAFTING : IDLE).dispatchForBlockEntity(crafter);
        return true;
    }

    public boolean complete(CosmeticCrafterBlockEntity crafter) {
        if (!ready(crafter)) {
            return false;
        }

        COMPLETE.dispatchForBlockEntity(crafter);
        return true;
    }
}
