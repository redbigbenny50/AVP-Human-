package com.human.common.gameplay.gene.machine;

import com.human.common.gameplay.gene.AdvancedGenes;
import com.human.common.gameplay.gene.Gene;
import com.human.common.gameplay.gene.GeneModifierKey;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.gene.GeneTier;
import com.human.common.gameplay.gene.Genes;
import com.human.common.registry.GeneBonusDataRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the Gene Analyzer knows about the world's genes, worked out from the species data (gene_bonus_data) and the
 * advanced-gene recipes. Server side; the Analyzer screen filters it to what its user has discovered.
 * <ul>
 * <li>{@link #donorsOf}: every species carrying a gene, with its donor tier (Prime/Strong/Average/Weak, or a weakness),
 * best first - the GENES tab's DONORS list and SEARCH's CARRIED BY.</li>
 * <li>{@link #genesOf}: a species' genes and tiers - the SPECIES tab.</li>
 * <li>{@link #unlocks}: which mutations a gene helps unlock - SEARCH's HELPS UNLOCK.</li>
 * <li>{@link #foundIn}: the biomes a species spawns in naturally - SEARCH's location and SPECIES' FOUND IN.</li>
 * </ul>
 * ⚡ Nothing is cached across datapack reloads; it is built when a screen asks, which is rare.
 */
public final class GeneDatabase {

    public record Donor(
        EntityType<?> type,
        GeneTier tier,
        double value
    ) {}

    private GeneDatabase() {}

    /** The tier a species value works out to: how many draws reach the cap, rounded to the nearest tier. */
    public static GeneTier tierOf(Gene gene, double value) {
        if (value < 0) {
            return GeneTier.WEAKNESS;
        }
        var doses = Genes.cap(gene) / Math.max(1.0E-9, value);
        GeneTier best = GeneTier.WEAK;
        var bestGap = Double.MAX_VALUE;
        for (var tier : GeneTier.values()) {
            if (tier.dosesToCap() <= 0) {
                continue;
            }
            var gap = Math.abs(Math.log(doses) - Math.log(tier.dosesToCap()));
            if (gap < bestGap) {
                bestGap = gap;
                best = tier;
            }
        }
        return best;
    }

    /** Every species with species data for this gene, best donor first (weaknesses last). */
    public static List<Donor> donorsOf(Gene gene) {
        var donors = new ArrayList<Donor>();
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            if (!GeneBonusDataRegistry.has(type)) {
                continue;
            }
            var total = sum(GeneBonusDataRegistry.getOrDefault(type), gene);
            if (total != 0.0) {
                donors.add(new Donor(type, tierOf(gene, total), total));
            }
        }
        donors.sort(Comparator.comparingDouble((Donor donor) -> -donor.value()));
        return donors;
    }

    /** A species' genes with their tiers, in gene order. */
    public static Map<Gene, GeneTier> genesOf(EntityType<?> type) {
        var out = new LinkedHashMap<Gene, GeneTier>();
        var map = GeneBonusDataRegistry.getOrDefault(type);
        var totals = new LinkedHashMap<ResourceLocation, Double>();
        map.forEach((key, value) -> totals.merge(key.resourceLocation(), value, Double::sum));
        totals.forEach((id, value) -> {
            var gene = GeneRegistry.getValueOrNull(id);
            if (gene != null && value != 0.0 && gene != Genes.GENETIC_INTEGRITY) {
                out.put(gene, tierOf(gene, value));
            }
        });
        return out;
    }

    /** The mutations whose recipe includes this gene. */
    public static List<AdvancedGenes.Definition> unlocks(Gene gene) {
        var out = new ArrayList<AdvancedGenes.Definition>();
        for (var definition : AdvancedGenes.definitions()) {
            if (definition.recipe().contains(gene)) {
                out.add(definition);
            }
        }
        return out;
    }

    /** The biomes this species spawns in naturally (any spawn category), by id. Empty for summoned/bred-only mobs. */
    public static List<ResourceLocation> foundIn(ServerLevel level, EntityType<?> type) {
        var out = new ArrayList<ResourceLocation>();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        for (var entry : biomes.entrySet()) {
            var settings = entry.getValue().getMobSettings();
            var spawns = false;
            for (var category : MobCategory.values()) {
                for (var data : settings.getMobs(category).unwrap()) {
                    if (data.type == type) {
                        spawns = true;
                        break;
                    }
                }
                if (spawns) {
                    break;
                }
            }
            if (spawns) {
                out.add(entry.getKey().location());
            }
        }
        return out;
    }

    private static double sum(Map<GeneModifierKey, Double> map, Gene gene) {
        var total = 0.0;
        for (var entry : map.entrySet()) {
            if (entry.getKey().resourceLocation().equals(gene.id())) {
                total += entry.getValue();
            }
        }
        return total;
    }
}
