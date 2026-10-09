package com.human.common.gameplay.entity.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Every turret with a network card. [stated] Oct 2: the network MUTHUR will read - for now the desk terminal lists the
 * turrets on it and lets their owner look through a camera lens. Saved with the world (in the overworld's data), so the
 * list survives a restart even before the turrets' chunks load.
 * <p>
 * A turret keeps its own entry current - it re-registers every couple of seconds while it has the card, and removes
 * itself when the card comes out or the turret goes. ⚡ The file is only marked dirty when an entry actually changes.
 * </p>
 */
public final class SentryNetwork extends SavedData {

    private static final String NAME = "avp_human_sentry_network";

    /** One networked turret. {@code owner} may be null for a turret placed without one. */
    public record Entry(
        UUID turret,
        @Nullable UUID owner,
        String dimension,
        BlockPos pos,
        String name,
        boolean camera
    ) {

        CompoundTag save() {
            var tag = new CompoundTag();
            tag.putUUID("turret", turret);

            if (owner != null) {
                tag.putUUID("owner", owner);
            }

            tag.putString("dimension", dimension);
            tag.putLong("pos", pos.asLong());
            tag.putString("name", name);
            tag.putBoolean("camera", camera);
            return tag;
        }

        static Entry load(CompoundTag tag) {
            return new Entry(
                tag.getUUID("turret"),
                tag.hasUUID("owner") ? tag.getUUID("owner") : null,
                tag.getString("dimension"),
                BlockPos.of(tag.getLong("pos")),
                tag.getString("name"),
                tag.getBoolean("camera")
            );
        }
    }

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    private static final Factory<SentryNetwork> FACTORY = new Factory<>(SentryNetwork::new, SentryNetwork::load, null);

    public static SentryNetwork get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    private static SentryNetwork load(CompoundTag tag, HolderLookup.Provider registries) {
        var network = new SentryNetwork();

        for (var element : tag.getList("entries", Tag.TAG_COMPOUND)) {
            var entry = Entry.load((CompoundTag) element);
            network.entries.put(entry.turret(), entry);
        }

        return network;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        var list = new ListTag();

        for (var entry : entries.values()) {
            list.add(entry.save());
        }

        tag.put("entries", list);
        return tag;
    }

    /** Adds or refreshes a turret's entry. Dirty only when something changed. */
    public void put(Entry entry) {
        if (!Objects.equals(entries.put(entry.turret(), entry), entry)) {
            setDirty();
        }
    }

    public void remove(UUID turret) {
        if (entries.remove(turret) != null) {
            setDirty();
        }
    }

    public @Nullable Entry get(UUID turret) {
        return entries.get(turret);
    }

    /** The turrets a player may see on a terminal: their own, and any placed without an owner. */
    public List<Entry> visibleTo(UUID player) {
        var list = new ArrayList<Entry>();

        for (var entry : entries.values()) {
            if (entry.owner() == null || entry.owner().equals(player)) {
                list.add(entry);
            }
        }

        return list;
    }
}
