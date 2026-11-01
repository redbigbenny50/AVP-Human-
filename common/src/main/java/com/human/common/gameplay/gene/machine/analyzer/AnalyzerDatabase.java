package com.human.common.gameplay.gene.machine.analyzer;

import com.human.Human;
import com.human.common.gameplay.gene.AdvancedGenes;
import com.human.common.gameplay.gene.GeneDefects;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.gene.Genes;
import com.human.common.gameplay.gene.LootGenes;
import com.human.common.gameplay.gene.discovery.GeneDiscoveries;
import com.human.common.network.packet.S2CAnalyzerDatabasePayload;
import com.human.common.registry.GeneBonusDataRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Gene Analyzer's database, sent to a player when they open one ([stated] Oct 27: totals are dynamic - whatever
 * genes and species are loaded, datapack and mod-compat included).
 * <p>
 * It carries the catalogue (every gene with its kind, cap, who it applies to, and a mutation's recipe), every species
 * with gene data (its genes and values, the biomes it spawns in) and this player's draw counts. It does NOT carry what
 * the player has discovered - their client already has that ({@code ClientGeneDiscoveries}) - so the screen decides
 * what to name and what to show as ???. Undiscovered names never reach the screen as text; the client only ever turns
 * ids it knows into names.
 * </p>
 * <p>
 * Plain text lines, tab-separated, split into payloads of {@link #CHUNK} characters:
 * {@code G id kind cap op who category recipe replaces}, {@code S id biomes draws}, {@code D species gene value}. Built
 * once per opening; nothing per tick.
 * </p>
 */
public final class AnalyzerDatabase {

    public static final int CHUNK = 30000;

    /** Biomes listed per species. */
    private static final int MAX_BIOMES = 3;

    private AnalyzerDatabase() {}

    public static void send(ServerPlayer player) {
        var text = build(player);
        var parts = Math.max(1, (text.length() + CHUNK - 1) / CHUNK);
        for (int part = 0; part < parts; part++) {
            var chunk = text.substring(part * CHUNK, Math.min(text.length(), (part + 1) * CHUNK));
            Human.MOD.networking().sendToClient(player, new S2CAnalyzerDatabasePayload(part, parts, chunk));
        }
    }

    static String build(ServerPlayer player) {
        var out = new StringBuilder();
        var lootGenes = new LinkedHashSet<ResourceLocation>();

        // Species and their genes.
        var all = GeneBonusDataRegistry.all();
        var biomes = biomesBySpecies(player);
        var draws = player.getServer() == null
            ? Map.<String, Integer>of()
            : GeneDiscoveries.get(player.getServer()).drawsOf(player.getUUID());
        for (var entry : all.entrySet()) {
            var typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entry.getKey()).toString();
            var spawns = biomes.getOrDefault(entry.getKey(), List.of());
            out.append("S\t")
                .append(typeId)
                .append('\t')
                .append(String.join(",", spawns))
                .append('\t')
                .append(draws.getOrDefault(typeId, 0))
                .append('\n');
            for (var gene : entry.getValue().entrySet()) {
                var geneId = gene.getKey().resourceLocation();
                out.append("D\t").append(typeId).append('\t').append(geneId).append('\t').append(gene.getValue()).append('\n');
                var resolved = GeneRegistry.getValueOrNull(geneId);
                if (resolved != null && LootGenes.isLootGene(resolved)) {
                    lootGenes.add(geneId);
                }
            }
        }

        // The catalogue: every registered gene but integrity, plus the loot genes species carry.
        for (var spec : Genes.specs().entrySet()) {
            var gene = spec.getKey();
            if (gene == Genes.GENETIC_INTEGRITY) {
                continue;
            }
            var definition = AdvancedGenes.definition(gene);
            String kind;
            if (definition != null) {
                kind = "MUTATION";
            } else if (GeneDefects.all().contains(gene)) {
                kind = "DEFECT";
            } else if (spec.getValue().category() == Genes.Category.HARVEST) {
                kind = "HARVEST";
            } else if (spec.getValue().category() == Genes.Category.LOOT) {
                kind = "LOOT";
            } else {
                kind = "BASE";
            }
            var recipe = new ArrayList<String>();
            if (definition != null) {
                definition.recipe().forEach(part -> recipe.add(part.id().toString()));
            }
            out.append("G\t")
                .append(gene.id())
                .append('\t')
                .append(kind)
                .append('\t')
                .append(spec.getValue().cap())
                .append('\t')
                .append(spec.getValue().operation().name())
                .append('\t')
                .append(spec.getValue().who().name())
                .append('\t')
                .append(definition == null ? "" : definition.category().name())
                .append('\t')
                .append(String.join(",", recipe))
                .append('\t')
                .append(definition == null || definition.replaces() == null ? "" : definition.replaces().id().toString())
                .append('\n');
        }
        for (var id : lootGenes) {
            var gene = GeneRegistry.getValueOrNull(id);
            if (gene == null || Genes.specs().containsKey(gene)) {
                continue;
            }
            out.append("G\t").append(id).append("\tLOOT\t").append(Genes.cap(gene)).append("\tADDITIVE\tALL\t\t\t\n");
        }
        return out.toString();
    }

    /** Where each species spawns naturally: a few biome translation keys, from the biome registry's spawn lists. */
    private static Map<EntityType<?>, List<String>> biomesBySpecies(ServerPlayer player) {
        var result = new HashMap<EntityType<?>, List<String>>();
        var registry = player.serverLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (var entry : registry.entrySet()) {
            var key = "biome." + entry.getKey().location().getNamespace() + "." + entry.getKey().location().getPath();
            var settings = entry.getValue().getMobSettings();
            for (var category : MobCategory.values()) {
                for (var spawn : settings.getMobs(category).unwrap()) {
                    var list = result.computeIfAbsent(spawn.type, t -> new ArrayList<>());
                    if (list.size() < MAX_BIOMES && !list.contains(key)) {
                        list.add(key);
                    }
                }
            }
        }
        return result;
    }

    static String upper(String text) {
        return text.toUpperCase(Locale.ROOT);
    }
}
