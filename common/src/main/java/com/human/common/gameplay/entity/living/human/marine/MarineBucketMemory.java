package com.human.common.gameplay.entity.living.human.marine;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

/**
 * Oct 5 - dimensions where a bucket of water is no use. [stated] "for marines who attempt to use a bucket and no water
 * comes out or the fire doesnt extinguish have them smart enough to not try it again in that dimension. this should
 * cover dimensions that dont allow water that arent ultra hot."
 * <p>
 * Ultra-warm dimensions (the Nether) are known up front. Anything else is learned: a marine pours water at its feet and
 * either no water appears or it is still burning two seconds later, and from then on no marine reaches for a bucket to
 * fight fire in that dimension. Shared by all marines on the server; not saved, so a restart lets them check again.
 * </p>
 */
public final class MarineBucketMemory {

    private static final Set<ResourceKey<Level>> WATERLESS = new HashSet<>();

    /**
     * Pre-merge review - the server the memory belongs to. In single player, leaving one world and opening another
     * keeps the same game running, so without this a dimension learned waterless in one world stayed waterless in the
     * next. Weak, so it never keeps a closed world in memory.
     */
    private static java.lang.ref.WeakReference<net.minecraft.server.MinecraftServer> owner = new java.lang.ref.WeakReference<>(null);

    private MarineBucketMemory() {
        throw new UnsupportedOperationException();
    }

    /**
     * Whether a marine should not bother with a bucket here.
     *
     * @param level the level
     * @return true when water is known not to work there
     */
    public static boolean isWaterless(Level level) {
        forgetIfNewWorld(level);
        return level.dimensionType().ultraWarm() || WATERLESS.contains(level.dimension());
    }

    /**
     * Remember that water did not work in {@code level}'s dimension.
     *
     * @param level the level
     */
    public static void markWaterless(Level level) {
        forgetIfNewWorld(level);
        WATERLESS.add(level.dimension());
    }

    private static void forgetIfNewWorld(Level level) {
        var server = level.getServer();

        if (server != null && owner.get() != server) {
            WATERLESS.clear();
            owner = new java.lang.ref.WeakReference<>(server);
        }
    }
}
