package com.human.common.gameplay.quarry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Every confirmed quarry work area in a level, so quarries keep their distance ([stated] Oct 15: "quarrys cant go
 * directly next to eachother they need at least 1 chunk apart"). Saved with the level; a terminal's entry goes when the
 * terminal is broken or its area is cleared.
 */
public final class QuarryRegistry extends SavedData {

    private static final String NAME = "avp_human_quarries";

    public enum Nearby {
        NONE,
        NEAR,
        INSIDE
    }

    private final Map<Long, long[]> areas = new HashMap<>();

    public static QuarryRegistry get(ServerLevel level) {
        var registry = level.getDataStorage()
            .computeIfAbsent(new SavedData.Factory<>(QuarryRegistry::new, QuarryRegistry::load, null), NAME);

        registry.prune(level);

        return registry;
    }

    /** Ticks between stale-entry sweeps. */
    private static final long PRUNE_TICKS = 200L;

    private long lastPrune = Long.MIN_VALUE;

    /**
     * ⚠ Final pass - drops areas whose terminal is GONE. A terminal normally forgets its area when broken, but one
     * removed without its block being broken normally (WorldEdit, a /setblock, a mod deleting it) would have reserved
     * its chunks - and blocked every quarry beside them - forever. Only LOADED positions are judged, so an unloaded
     * quarry is never mistaken for a missing one. At most once every 10 s, and only when a quarry asks.
     */
    private void prune(ServerLevel level) {
        var now = level.getGameTime();

        if (now - lastPrune < PRUNE_TICKS) {
            return;
        }

        lastPrune = now;

        var gone = new java.util.ArrayList<Long>();

        for (var key : areas.keySet()) {
            var pos = net.minecraft.core.BlockPos.of(key);

            if (level.isLoaded(pos) && !(level.getBlockEntity(pos) instanceof QuarryTerminalBlockEntity)) {
                gone.add(key);
            }
        }

        if (!gone.isEmpty()) {
            gone.forEach(areas::remove);
            setDirty();
        }
    }

    private static QuarryRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        var registry = new QuarryRegistry();
        var list = tag.getList("Areas", Tag.TAG_COMPOUND);

        for (int index = 0; index < list.size(); index++) {
            var entry = list.getCompound(index);

            registry.areas.put(entry.getLong("Terminal"), entry.getLongArray("Chunks"));
        }

        return registry;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        var list = new ListTag();

        areas.forEach((terminal, chunks) -> {
            var entry = new CompoundTag();

            entry.putLong("Terminal", terminal);
            entry.put("Chunks", new LongArrayTag(chunks));
            list.add(entry);
        });

        tag.put("Areas", list);

        return tag;
    }

    public void put(BlockPos terminal, java.util.List<ChunkPos> chunks) {
        areas.put(terminal.asLong(), chunks.stream().mapToLong(ChunkPos::toLong).toArray());
        setDirty();
    }

    public void remove(BlockPos terminal) {
        if (areas.remove(terminal.asLong()) != null) {
            setDirty();
        }
    }

    /** How close {@code chunk} is to another quarry's area: inside it, beside it (no chunk between) or clear. */
    public Nearby nearby(BlockPos self, ChunkPos chunk) {
        var result = Nearby.NONE;

        for (var entry : areas.entrySet()) {
            if (entry.getKey() == self.asLong()) {
                continue;
            }

            for (var packed : entry.getValue()) {
                var other = new ChunkPos(packed);
                var distance = Math.max(Math.abs(other.x - chunk.x), Math.abs(other.z - chunk.z));

                if (distance == 0) {
                    return Nearby.INSIDE;
                }

                if (distance == 1) {
                    result = Nearby.NEAR;
                }
            }
        }

        return result;
    }
}
