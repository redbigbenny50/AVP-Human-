package com.human.common.gameplay.quarry;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Whether the quarry's owner may break a block - asked of the game itself, so EVERY protection mod answers ([stated]
 * Oct 15: "detect ftb chunk claims and world guard claims too").
 * <p>
 * Each loader fires its own block-break check for a fake player carrying the OWNER's id: NeoForge's
 * {@code BlockEvent.BreakEvent}, Fabric's {@code PlayerBlockBreakEvents.BEFORE}. A claim mod that would stop the owner
 * breaking that block stops the quarry too - FTB Chunks, Open Parties and Claims, vanilla spawn protection, and on
 * hybrid plugin servers possibly WorldGuard. Because the fake player carries the owner's id, the owner's own claims and
 * their teams' are allowed wherever the claim mod allows a member's fake players (FTB Chunks' "fake players by player
 * id" team setting, on by default).
 * </p>
 * <p>
 * ⚠ Never asked about an UNLOADED chunk: a protection mod answering would load it. Callers check first.
 * </p>
 */
public interface QuarryProtection {

    /** Id used when a terminal has no owner (placed by a machine or before ownership was saved). */
    UUID UNOWNED = UUID.nameUUIDFromBytes("avp_human:quarry".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    String UNOWNED_NAME = "[Quarry]";

    /** @return true when the owner may break the block at {@code pos} */
    boolean mayBreak(ServerLevel level, BlockPos pos, @Nullable UUID owner);

    static boolean check(ServerLevel level, BlockPos pos, @Nullable UUID owner) {
        if (!level.isLoaded(pos)) {
            return true;
        }

        return Holder.get().mayBreak(level, pos, owner);
    }

    /** The owner's real name where the server knows it, so claim mods that work by name still match. */
    static GameProfile profileFor(ServerLevel level, @Nullable UUID owner) {
        if (owner == null) {
            return new GameProfile(UNOWNED, UNOWNED_NAME);
        }

        var cache = level.getServer().getProfileCache();
        var known = cache != null ? cache.get(owner) : java.util.Optional.<GameProfile>empty();

        return known.map(profile -> new GameProfile(owner, profile.getName())).orElseGet(() -> new GameProfile(owner, UNOWNED_NAME));
    }

    final class Holder {

        private static QuarryProtection current = (level, pos, owner) -> true;

        private Holder() {}

        public static QuarryProtection get() {
            return current;
        }

        public static void install(@Nullable QuarryProtection protection) {
            current = protection == null ? (level, pos, owner) -> true : protection;
        }
    }
}
