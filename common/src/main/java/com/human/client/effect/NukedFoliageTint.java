package com.human.client.effect;

import com.human.common.registry.key.HumanBiomeKeys;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;

/**
 * Makes leaves that ignore the biome foliage colour take the fallout tint anyway.
 * <p>
 * THE PROBLEM. The nuked biome sets a foliage colour override, but that only reaches leaves whose colour handler asks
 * the biome for it. Vanilla registers SPRUCE and BIRCH with CONSTANT tints and gives CHERRY and both AZALEAS no handler
 * at all, so those stayed summer-green in the middle of a red irradiated forest. The same is true of any third-party
 * leaf that hardcodes its colour.
 * </p>
 * <p>
 * THE APPROACH. Rather than naming blocks, the colour is post-processed for EVERY leaf at lookup time (see
 * {@code MixinBlockColors_NukedFoliage}). Whatever colour a block would normally have - vanilla constant, mod-supplied,
 * or none at all - is blended toward the biome foliage colour by how much fallout is at that position. A leaf that
 * already tints correctly blends from its own colour to the same colour, so it is left visually untouched, which is
 * what makes it safe to apply this blindly to modded trees.
 * </p>
 * <p>
 * WHY A COLOR RESOLVER AND NOT A BIOME LOOKUP. The colour handler is handed a {@link BlockAndTintGetter} - a render
 * chunk snapshot with deliberately NO biome access, because chunk meshing runs on worker threads. Reaching for the
 * client level from there would be touching the chunk map off-thread. A {@link ColorResolver} is given the biome
 * directly and resolves through vanilla's {@code BlockTintCache}: the same cached, thread-safe path oak leaves already
 * use, and one that is invalidated automatically when chunks reload or biomes are resent - which matters here, because
 * a nuke rewrites biomes at runtime.
 * </p>
 * <p>
 * The resolver returns a SCALAR weight rather than a colour, so vanilla's own 3x3 biome blending averages it into a
 * smooth gradient. Leaves therefore fade in at the edge of the fallout zone exactly like the oak beside them, instead
 * of snapping at the boundary.
 * </p>
 */
public final class NukedFoliageTint {

    /** Full strength inside the fallout biome, nothing outside it; vanilla blends the values between. */
    private static final int FULL_WEIGHT = 255;

    /**
     * Must be a SINGLE shared instance: {@code ClientLevel.getBlockTint} looks the resolver up by identity in its cache
     * map, so a fresh lambda per call would miss the cache and then throw.
     */
    public static final ColorResolver FALLOUT_WEIGHT_RESOLVER = NukedFoliageTint::weightFor;

    private NukedFoliageTint() {
        throw new UnsupportedOperationException();
    }

    private static int weightFor(Biome biome, double x, double z) {
        return biome.getSpecialEffects()
            .getFoliageColorOverride()
            .map(NukedFoliageTint::isFalloutOverride)
            .orElse(Boolean.FALSE)
                ? FULL_WEIGHT
                : 0;
    }

    /**
     * Identifies the fallout biome by its foliage override rather than by biome key, because a {@link ColorResolver}
     * receives the {@link Biome} itself and not a holder it could match a key against.
     */
    private static boolean isFalloutOverride(int override) {
        return override == HumanBiomeKeys.NUKED_FOLIAGE_COLOR;
    }

    /**
     * The colour a leaf should actually render, given the colour it would otherwise have had.
     *
     * @param originalColor what the normal handler returned; {@code -1} (white) when there was no handler
     */
    public static int applyTo(int originalColor, BlockAndTintGetter level, BlockPos pos) {
        var weight = level.getBlockTint(pos, FALLOUT_WEIGHT_RESOLVER);

        if (weight <= 0) {
            return originalColor;
        }

        return blend(originalColor, BiomeColors.getAverageFoliageColor(level, pos), Math.min(weight, FULL_WEIGHT));
    }

    private static int blend(int from, int to, int weight) {
        var t = weight / (float) FULL_WEIGHT;

        return channel(from, to, t, 16) << 16 | channel(from, to, t, 8) << 8 | channel(from, to, t, 0);
    }

    private static int channel(int from, int to, float t, int shift) {
        return Mth.clamp(Math.round(Mth.lerp(t, (from >> shift) & 0xFF, (to >> shift) & 0xFF)), 0, 0xFF);
    }
}
