package com.human.compatibility.avp_alien;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * avp_alien's capture chain, for the cryotank. [stated] Oct 1: a specimen can be brought to a tank on the capture chain
 * as well as on a lead or the titanium cord.
 * <p>
 * The chain is not a vanilla leash - avp_alien tracks who holds what in {@code CaptureHoldManager} - so the tank asks
 * it directly. ⚠ Every avp_alien class is touched only inside {@link Impl}, which is never loaded unless avp_alien is
 * installed; without it these simply report nothing held.
 * </p>
 */
public final class AlienCaptureChain {

    private AlienCaptureChain() {}

    /** The mob this player holds on a capture chain, or null. */
    public static @Nullable Mob heldBy(ServerLevel level, Player player) {
        return AVPAlien.MOD.isLoaded() ? Impl.heldBy(level, player) : null;
    }

    /**
     * The client's view of the same thing. avp_alien sends every chain hold to nearby players' games
     * ({@code CaptureHoldClientState}), so the client can tell a chain is in play and pass the click to the server
     * rather than placing a block. ⚠ CLIENT ONLY - the class it reads is never loaded on a dedicated server.
     */
    public static @Nullable Mob heldByOnClient(net.minecraft.world.level.Level level, Player player) {
        return AVPAlien.MOD.isLoaded() ? ClientImpl.heldBy(level, player) : null;
    }

    /**
     * {@return whether this mob is on a capture chain} — held by a player, or chained to another mob. Server side. Used
     * by freezing solid, so a frozen mob on a chain can be dragged.
     */
    public static boolean isTethered(Mob mob) {
        return AVPAlien.MOD.isLoaded() && Impl.isTethered(mob);
    }

    /** Lets go of a chained mob, so it can be frozen. The chain stays with the player. */
    public static void release(Mob mob) {
        if (AVPAlien.MOD.isLoaded()) {
            Impl.release(mob);
        }
    }

    private static final class ClientImpl {

        static @Nullable Mob heldBy(net.minecraft.world.level.Level level, Player player) {
            var box = player.getBoundingBox().inflate(16.0);

            for (var mob : level.getEntitiesOfClass(Mob.class, box)) {
                if (com.alien.client.render.CaptureHoldClientState.holderOf(mob) == player) {
                    return mob;
                }
            }

            return null;
        }
    }

    private static final class Impl {

        static @Nullable Mob heldBy(ServerLevel level, Player player) {
            return com.alien.common.gameplay.capture.CaptureHoldManager.heldBy(level, player);
        }

        static boolean isTethered(Mob mob) {
            return com.alien.common.gameplay.capture.CaptureHoldManager.isHeld(mob)
                || com.alien.common.gameplay.capture.MobChainManager.isLinked(mob);
        }

        static void release(Mob mob) {
            com.alien.common.gameplay.capture.CaptureHoldManager.release(mob);
        }
    }
}
