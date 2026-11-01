package com.human.common.gameplay.gene.discovery;

import com.human.Human;
import com.human.common.network.packet.S2CGeneDiscoveriesPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Which genes and species each player has discovered ([stated] discoveries are per player, "DISCOVERIES ARE YOURS
 * ONLY"). Saved with the world (overworld data storage), keyed by player UUID, so it survives death, relogging and
 * dimension changes on both loaders without any respawn-copy hook. Mutations and defects are genes too, so they sit in
 * the gene set.
 * <p>
 * Discovered by: reading a mob with the gene reader (its genes and species), drawing a syringe (the genes drawn and the
 * donor), injecting (the genes injected), and - when the machines land - an Analyzer scan and Genetic Research
 * printouts.
 * </p>
 * <p>
 * The owning player's client gets the full list on join and again whenever it grows, so tooltips and screens can show
 * names for what they know and ??? for the rest.
 * </p>
 */
public final class GeneDiscoveries extends SavedData {

    private static final String ID = "avp_human_gene_discoveries";

    private final Map<UUID, Set<String>> genes = new HashMap<>();

    private final Map<UUID, Set<String>> species = new HashMap<>();

    /** Syringe draws each player has taken per species - the Analyzer's DRAWS TAKEN. */
    private final Map<UUID, Map<String, Integer>> draws = new HashMap<>();

    private static final SavedData.Factory<GeneDiscoveries> FACTORY = new SavedData.Factory<>(
        GeneDiscoveries::new,
        GeneDiscoveries::load,
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );

    public static GeneDiscoveries get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    // ---------------------------------------------------------------- queries

    public Set<String> genesOf(UUID player) {
        return genes.getOrDefault(player, Set.of());
    }

    public Set<String> speciesOf(UUID player) {
        return species.getOrDefault(player, Set.of());
    }

    public Map<String, Integer> drawsOf(UUID player) {
        return draws.getOrDefault(player, Map.of());
    }

    /** One syringe draw from {@code type} by this player. */
    public static void countDraw(ServerPlayer player, EntityType<?> type) {
        if (player.getServer() == null) {
            return;
        }
        var data = get(player.getServer());
        data.draws.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
            .merge(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(), 1, Integer::sum);
        data.setDirty();
    }

    // ---------------------------------------------------------------- discovering

    /** Records genes (and optionally a species) as discovered by this player; syncs their client if anything is new. */
    public static void discover(ServerPlayer player, Collection<ResourceLocation> geneIds, @Nullable EntityType<?> speciesType) {
        if (player.getServer() == null) {
            return;
        }

        var data = get(player.getServer());
        var changed = false;
        var id = player.getUUID();

        if (!geneIds.isEmpty()) {
            var set = data.genes.computeIfAbsent(id, k -> new LinkedHashSet<>());
            for (var gene : geneIds) {
                changed |= set.add(gene.toString());
            }
        }

        if (speciesType != null) {
            changed |= data.species.computeIfAbsent(id, k -> new LinkedHashSet<>())
                .add(BuiltInRegistries.ENTITY_TYPE.getKey(speciesType).toString());
        }

        if (changed) {
            data.setDirty();
            sync(player);
        }
    }

    /** Sends this player their whole discovery list. Called on join and after each new discovery. */
    public static void sync(ServerPlayer player) {
        if (player.getServer() == null) {
            return;
        }
        var data = get(player.getServer());
        Human.MOD.networking()
            .sendToClient(
                player,
                new S2CGeneDiscoveriesPayload(
                    String.join("\n", data.genesOf(player.getUUID())),
                    String.join("\n", data.speciesOf(player.getUUID()))
                )
            );
    }

    // ---------------------------------------------------------------- persistence

    private static GeneDiscoveries load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new GeneDiscoveries();
        var players = tag.getCompound("players");
        for (var key : players.getAllKeys()) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            var entry = players.getCompound(key);
            data.genes.put(id, readSet(entry.getList("genes", Tag.TAG_STRING)));
            data.species.put(id, readSet(entry.getList("species", Tag.TAG_STRING)));
            var drawTag = entry.getCompound("draws");
            if (!drawTag.isEmpty()) {
                var map = new HashMap<String, Integer>();
                for (var type : drawTag.getAllKeys()) {
                    map.put(type, drawTag.getInt(type));
                }
                data.draws.put(id, map);
            }
        }
        return data;
    }

    private static Set<String> readSet(ListTag list) {
        var set = new LinkedHashSet<String>();
        for (var element : list) {
            set.add(element.getAsString());
        }
        return set;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        var players = new CompoundTag();
        var ids = new LinkedHashSet<UUID>();
        ids.addAll(genes.keySet());
        ids.addAll(species.keySet());
        ids.addAll(draws.keySet());
        for (var id : ids) {
            var entry = new CompoundTag();
            entry.put("genes", writeSet(genes.getOrDefault(id, Set.of())));
            entry.put("species", writeSet(species.getOrDefault(id, Set.of())));
            var drawTag = new CompoundTag();
            draws.getOrDefault(id, Map.of()).forEach(drawTag::putInt);
            entry.put("draws", drawTag);
            players.put(id.toString(), entry);
        }
        tag.put("players", players);
        return tag;
    }

    private static ListTag writeSet(Set<String> set) {
        var list = new ListTag();
        for (var value : set) {
            list.add(StringTag.valueOf(value));
        }
        return list;
    }
}
