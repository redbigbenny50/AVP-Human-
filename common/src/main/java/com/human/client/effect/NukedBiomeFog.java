package com.human.client.effect;

import com.human.common.registry.key.HumanBiomeKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

public final class NukedBiomeFog {

    private static final float FADE_IN_STEP = 1.0F / 60.0F;

    private static final float FADE_OUT_STEP = 1.0F / 40.0F;

    private static ClientLevel trackedLevel;

    private static float previousBlend;

    private static float blend;

    private NukedBiomeFog() {
        throw new UnsupportedOperationException();
    }

    public static void tick(Minecraft minecraft) {
        var level = minecraft.level;
        if (level == null) {
            reset();
            return;
        }

        if (trackedLevel != level) {
            trackedLevel = level;
            previousBlend = 0.0F;
            blend = 0.0F;
        } else {
            previousBlend = blend;
        }

        var player = minecraft.player;
        var inNukedBiome =
            player != null && level.getBiome(player.blockPosition()).is(HumanBiomeKeys.NUKED_BIOME);
        blend = inNukedBiome
            ? Math.min(1.0F, blend + FADE_IN_STEP)
            : Math.max(0.0F, blend - FADE_OUT_STEP);
    }

    public static float getBlend(float partialTick) {
        return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), previousBlend, blend);
    }

    private static void reset() {
        trackedLevel = null;
        previousBlend = 0.0F;
        blend = 0.0F;
    }
}
