package com.human.common.gameplay.gene.discovery;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * This client's copy of its own player's discoveries (sent by the server - see GeneDiscoveries). Read by tooltips and
 * screens to show a gene's or species' name, or ??? when it isn't discovered yet. Lives in common so tooltip code on
 * either side can ask; on a server it is simply empty.
 */
public final class ClientGeneDiscoveries {

    private static volatile Set<String> genes = Set.of();

    private static volatile Set<String> species = Set.of();

    /** Species profiled with the gene reader - see GeneDiscoveries#profile. */
    private static volatile Set<String> profiles = Set.of();

    private ClientGeneDiscoveries() {}

    public static void accept(String geneList, String speciesList, String profileList) {
        genes = geneList.isEmpty() ? Set.of() : Set.of(geneList.split("\n"));
        species = speciesList.isEmpty() ? Set.of() : Set.of(speciesList.split("\n"));
        profiles = profileList.isEmpty() ? Set.of() : Set.of(profileList.split("\n"));
    }

    /**
     * Whether this player has profiled the species {@code entityTypeId} (a syringe's donor id) with the gene reader.
     */
    public static boolean hasProfile(@org.jetbrains.annotations.Nullable String entityTypeId) {
        return entityTypeId != null && profiles.contains(entityTypeId);
    }

    public static boolean knowsGene(ResourceLocation id) {
        return genes.contains(id.toString());
    }

    public static boolean knowsSpecies(ResourceLocation id) {
        return species.contains(id.toString());
    }

    public static int geneCount() {
        return genes.size();
    }

    public static int speciesCount() {
        return species.size();
    }
}
