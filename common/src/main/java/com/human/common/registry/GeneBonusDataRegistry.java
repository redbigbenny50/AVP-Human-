package com.human.common.registry;

import com.human.common.gameplay.gene.GeneBonusData;
import com.human.common.gameplay.gene.GeneModifierKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneBonusDataRegistry {

    private static final List<GeneBonusData> GENE_BONUS_MAPS = new ArrayList<>();

    private static final Map<EntityType<?>, Map<GeneModifierKey, Double>> ENTITY_TYPE_TO_GENE_BONUS_MAP = new HashMap<>();

    public static boolean has(EntityType<?> entityType) {
        return ENTITY_TYPE_TO_GENE_BONUS_MAP.containsKey(entityType);
    }

    public static Map<GeneModifierKey, Double> getOrDefault(EntityType<?> entityType) {
        return ENTITY_TYPE_TO_GENE_BONUS_MAP.getOrDefault(entityType, Map.of());
    }

    /** Every species with gene data and its genes - read-only, for the Gene Analyzer's database. */
    public static Map<EntityType<?>, Map<GeneModifierKey, Double>> all() {
        return java.util.Collections.unmodifiableMap(ENTITY_TYPE_TO_GENE_BONUS_MAP);
    }

    public static void clear() {
        GENE_BONUS_MAPS.clear();
    }

    public static void register(GeneBonusData geneBonusData) {
        GENE_BONUS_MAPS.add(geneBonusData);
    }

    public static void rebuildLookupMappings() {
        ENTITY_TYPE_TO_GENE_BONUS_MAP.clear();
        GENE_BONUS_MAPS.forEach(GeneBonusDataRegistry::compute);
    }

    public static void compute(GeneBonusData geneBonusData) {
        BuiltInRegistries.ENTITY_TYPE.stream()
            .filter(geneBonusData.entityTypePredicate()::test)
            .forEach(
                entityType -> geneBonusData.geneBonusDataEntries()
                    .forEach(
                        geneBonusDataEntry -> ENTITY_TYPE_TO_GENE_BONUS_MAP.computeIfAbsent(entityType, $ -> new HashMap<>())
                            .merge(geneBonusDataEntry.toKey(), geneBonusDataEntry.value(), Double::sum)
                    )
            );
    }
}
