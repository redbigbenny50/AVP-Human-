package com.human.common.gameplay.acid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * How much life each poured acid SOURCE has left, per dimension. [stated] Oct 17: a poured source lasts 20 seconds, as
 * long as one bleed's blood, then fades - and everything that flowed from it drains away after it.
 * <h2>WHY A SAVED MAP AND NOT A SCHEDULED TICK</h2> A block keeps no data of its own, and Minecraft keeps ONE scheduled
 * tick per block position - the acid's 4-tick bite already uses it, so a second 400-tick "fade" tick would be dropped.
 * Instead each bite subtracts its 4 ticks from the source's entry here (twice that touching water, as the blood ages
 * twice as fast in water). The count only runs while the chunk is loaded - exactly like the blood, which is an entity
 * and freezes with its chunk.
 * <h2>CLEANUP</h2> An entry goes when its source fades, or when anything else takes the source away (a bucket, a block
 * placed into it) - see {@link AcidLiquidBlock#onRemove}. A source this map has never seen (a structure, a command) is
 * given a fresh 20 seconds on its first bite.
 */
public final class AcidSources extends SavedData {

    /** [stated] 20 seconds - one bleed's blood (avp_alien Acid.DEFAULT_MAX_LIFE_IN_TICKS). */
    public static final int LIFETIME_TICKS = 20 * 20;

    private static final String NAME = "avp_human_acid_sources";

    private static final String POSITIONS = "positions";

    private static final String REMAINING = "remaining";

    private final Map<Long, Integer> remaining = new HashMap<>();

    private AcidSources() {}

    public static AcidSources get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(AcidSources::new, AcidSources::load, null), NAME);
    }

    /** A source was poured here: it gets the full lifetime. */
    public void start(BlockPos pos) {
        remaining.put(pos.asLong(), LIFETIME_TICKS);
        setDirty();
    }

    public void forget(BlockPos pos) {
        if (remaining.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    /**
     * Ages the source at {@code pos} by {@code ticks}.
     *
     * @return true when it has run out - the entry is gone and the caller removes the source
     */
    public boolean age(BlockPos pos, int ticks) {
        var key = pos.asLong();
        int left = remaining.getOrDefault(key, LIFETIME_TICKS);

        left -= ticks;
        setDirty();

        if (left <= 0) {
            remaining.remove(key);
            return true;
        }

        remaining.put(key, left);
        return false;
    }

    private static AcidSources load(CompoundTag tag, HolderLookup.Provider registries) {
        var sources = new AcidSources();
        var positions = tag.getLongArray(POSITIONS);
        var left = tag.getIntArray(REMAINING);

        for (int i = 0; i < Math.min(positions.length, left.length); i++) {
            sources.remaining.put(positions[i], left[i]);
        }

        return sources;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        var positions = new long[remaining.size()];
        var left = new int[remaining.size()];
        var index = 0;

        for (var entry : remaining.entrySet()) {
            positions[index] = entry.getKey();
            left[index] = entry.getValue();
            index++;
        }

        tag.put(POSITIONS, new LongArrayTag(positions));
        tag.putIntArray(REMAINING, left);
        return tag;
    }
}
