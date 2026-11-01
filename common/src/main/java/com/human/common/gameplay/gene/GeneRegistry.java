package com.human.common.gameplay.gene;

import com.blib.api.common.registry.v1.BLibHolder;
import com.just.core.functional.option.Option;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public class GeneRegistry {

    // Concurrent: loot genes are created on demand, from the server thread and (for tooltips) the client thread.
    private static final Map<ResourceLocation, Gene> GENES_BY_RESOURCE_LOCATION = new java.util.concurrent.ConcurrentHashMap<>();

    private static final Map<Gene, ResourceLocation> RESOURCE_LOCATION_BY_VALUE = new java.util.concurrent.ConcurrentHashMap<>();

    public static @Nullable Gene getValueOrNull(ResourceLocation resourceLocation) {
        var gene = GENES_BY_RESOURCE_LOCATION.get(resourceLocation);

        // A loot gene that has not been seen yet this session (loaded from a save, a syringe, a packet): make it.
        if (gene == null) {
            gene = LootGenes.resolve(resourceLocation);
        }

        return gene;
    }

    /** Only what is already registered - never makes a loot gene. */
    static @Nullable Gene getRegisteredOrNull(ResourceLocation resourceLocation) {
        return GENES_BY_RESOURCE_LOCATION.get(resourceLocation);
    }

    public static @Nullable ResourceLocation getResourceLocationOrNull(BLibHolder<Gene> geneHolder) {
        return RESOURCE_LOCATION_BY_VALUE.get(geneHolder.get());
    }

    public static Option<Gene> getValue(ResourceLocation resourceLocation) {
        return Option.ofNullable(getValueOrNull(resourceLocation));
    }

    public static Option<ResourceLocation> getResourceLocation(BLibHolder<Gene> geneHolder) {
        return Option.ofNullable(getResourceLocationOrNull(geneHolder));
    }

    public static Gene register(Supplier<Gene> geneSupplier) {
        var gene = geneSupplier.get();
        var resourceLocation = gene.id();

        GENES_BY_RESOURCE_LOCATION.put(resourceLocation, gene);
        RESOURCE_LOCATION_BY_VALUE.put(gene, resourceLocation);

        return gene;
    }
}
