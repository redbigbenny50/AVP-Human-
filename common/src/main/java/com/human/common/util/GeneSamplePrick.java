package com.human.common.util;

import com.human.mixin.MixinLivingEntity_HurtSoundInvoker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * The "ouch" of a syringe or gene reader sample, with no damage behind it.
 * <p>
 * [stated] Oct 25 - using a syringe or the gene reader shows the red hurt flash and plays the hurt sound, but deals no
 * damage and does not count as a hurt.
 * </p>
 * <p>
 * ⚠ The old code called {@code hurt(generic, 0.01F)}. A real hurt call is a real hurt: it fires every damage hook, sets
 * the last attacker, angers neutral mobs - and a DOWNED QUEEN treats any damage with no attacker entity as the
 * finishing blow, so sampling her killed her. This sends only what the client needs to draw the flash
 * ({@code ClientboundDamageEventPacket}, the same packet vanilla sends after a real hit) and plays the hurt sound from
 * the server so everyone nearby hears it - vanilla's client-side damage event does not play it for other players.
 * </p>
 */
public final class GeneSamplePrick {

    private GeneSamplePrick() {
        throw new UnsupportedOperationException();
    }

    public static void prick(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel) || !target.isAlive()) {
            return;
        }

        var source = target.damageSources().generic();
        serverLevel.broadcastDamageEvent(target, source);
        ((MixinLivingEntity_HurtSoundInvoker) target).avp_human$playHurtSound(source);
    }
}
