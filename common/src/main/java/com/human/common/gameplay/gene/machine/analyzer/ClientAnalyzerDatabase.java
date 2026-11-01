package com.human.common.gameplay.gene.machine.analyzer;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client copy of the Gene Analyzer database (see {@link AnalyzerDatabase}). Replaced whole each time one arrives - on
 * opening the screen and when a scan finishes while it is open. Plain data, no client classes, so it can live in common
 * code next to the packet handler.
 */
public final class ClientAnalyzerDatabase {

    public record GeneInfo(
        ResourceLocation id,
        String kind,
        double cap,
        String operation,
        String who,
        String category,
        List<ResourceLocation> recipe,
        ResourceLocation replaces
    ) {}

    public record SpeciesInfo(
        ResourceLocation id,
        List<String> biomes,
        int draws,
        Map<ResourceLocation, Double> genes
    ) {}

    private static final StringBuilder PENDING = new StringBuilder();

    private static int expected = -1;

    private static Map<ResourceLocation, GeneInfo> genes = Map.of();

    private static Map<ResourceLocation, SpeciesInfo> species = Map.of();

    private static int version;

    private ClientAnalyzerDatabase() {}

    public static synchronized void accept(int part, int total, String text) {
        if (part == 0) {
            PENDING.setLength(0);
            expected = 0;
        }
        if (expected != part) {
            // A part went missing or arrived out of order: drop it and wait for the next whole send.
            expected = -1;
            return;
        }
        PENDING.append(text);
        expected++;
        if (expected == total) {
            parse(PENDING.toString());
            PENDING.setLength(0);
            expected = -1;
        }
    }

    private static void parse(String text) {
        var geneMap = new LinkedHashMap<ResourceLocation, GeneInfo>();
        var speciesMap = new LinkedHashMap<ResourceLocation, SpeciesInfo>();
        var pendingGenes = new LinkedHashMap<ResourceLocation, Map<ResourceLocation, Double>>();
        for (var line : text.split("\n")) {
            var f = line.split("\t", -1);
            switch (f[0]) {
                case "G" -> {
                    if (f.length < 9) {
                        continue;
                    }
                    var id = ResourceLocation.tryParse(f[1]);
                    if (id == null) {
                        continue;
                    }
                    var recipe = new ArrayList<ResourceLocation>();
                    for (var part : f[7].split(",")) {
                        var r = ResourceLocation.tryParse(part);
                        if (!part.isEmpty() && r != null) {
                            recipe.add(r);
                        }
                    }
                    geneMap.put(
                        id,
                        new GeneInfo(
                            id,
                            f[2],
                            number(f[3]),
                            f[4],
                            f[5],
                            f[6],
                            List.copyOf(recipe),
                            f[8].isEmpty() ? null : ResourceLocation.tryParse(f[8])
                        )
                    );
                }
                case "S" -> {
                    if (f.length < 4) {
                        continue;
                    }
                    var id = ResourceLocation.tryParse(f[1]);
                    if (id == null) {
                        continue;
                    }
                    var carried = pendingGenes.computeIfAbsent(id, k -> new LinkedHashMap<>());
                    speciesMap.put(
                        id,
                        new SpeciesInfo(id, f[2].isEmpty() ? List.of() : List.of(f[2].split(",")), (int) number(f[3]), carried)
                    );
                }
                case "D" -> {
                    if (f.length < 4) {
                        continue;
                    }
                    var species = ResourceLocation.tryParse(f[1]);
                    var gene = ResourceLocation.tryParse(f[2]);
                    if (species != null && gene != null) {
                        pendingGenes.computeIfAbsent(species, k -> new LinkedHashMap<>()).merge(gene, number(f[3]), Double::sum);
                    }
                }
                default -> {}
            }
        }
        genes = Collections.unmodifiableMap(geneMap);
        species = Collections.unmodifiableMap(speciesMap);
        version++;
    }

    private static double number(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0D;
        }
    }

    public static Map<ResourceLocation, GeneInfo> genes() {
        return genes;
    }

    public static Map<ResourceLocation, SpeciesInfo> species() {
        return species;
    }

    /** Bumps on every new copy, so the screen knows to rebuild its lists. */
    public static int version() {
        return version;
    }
}
