package com.human.common.gameplay.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The contents of every full crate that is currently an ITEM (picked up, dropped, or in a chest). A placed crate keeps
 * its items in its own block entity; only crates in item form live here. The item carries just the id, so thousands of
 * stacks never ride along in item data or network packets.
 * <p>
 * Placing a crate TAKES its entry out of this file. A duplicated item (creative copy) therefore finds nothing once the
 * first copy has been placed, and gets an empty crate rather than sharing contents.
 */
public final class CrateStorageData extends SavedData {

    private static final String NAME = "avp_human_crates";

    private static final Factory<CrateStorageData> FACTORY = new Factory<>(CrateStorageData::new, CrateStorageData::load, null);

    private final Map<UUID, ListTag> contents = new HashMap<>();

    public static CrateStorageData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public void put(UUID id, ListTag items) {
        contents.put(id, items);
        setDirty();
    }

    /** Removes and returns a crate's contents, or null if they were already placed elsewhere. */
    public @Nullable ListTag take(UUID id) {
        var items = contents.remove(id);

        if (items != null) {
            setDirty();
        }

        return items;
    }

    private static CrateStorageData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new CrateStorageData();
        var crates = tag.getCompound("Crates");

        for (var key : crates.getAllKeys()) {
            try {
                data.contents.put(UUID.fromString(key), crates.getList(key, Tag.TAG_COMPOUND));
            } catch (IllegalArgumentException ignored) {
                // A malformed key cannot belong to any crate item; skip it rather than fail the whole file.
            }
        }

        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        var crates = new CompoundTag();

        contents.forEach((id, items) -> crates.put(id.toString(), items));
        tag.put("Crates", crates);

        return tag;
    }
}
