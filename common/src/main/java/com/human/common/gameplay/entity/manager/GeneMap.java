package com.human.common.gameplay.entity.manager;

import com.blib.api.common.registry.v1.BLibHolder;
import com.human.common.gameplay.gene.Gene;
import com.human.common.gameplay.gene.GeneModifierKey;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.gene.GeneRegistry;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GeneMap {

    private final Set<GeneModifierKey> dirtyKeys;

    private final Map<GeneModifierKey, Double> geneMap;

    public GeneMap() {
        this.dirtyKeys = new HashSet<>();
        this.geneMap = new Object2DoubleArrayMap<>();
    }

    public boolean hasGene(BLibHolder<Gene> geneHolder) {
        return hasGene(geneHolder.get());
    }

    public boolean hasGene(Gene gene) {
        var geneResourceLocation = gene.id();
        return geneMap.containsKey(new GeneModifierKey(geneResourceLocation, GeneOperationType.ADDITIVE))
            || geneMap.containsKey(new GeneModifierKey(geneResourceLocation, GeneOperationType.MULTIPLICATIVE));
    }

    public boolean hasGeneModifier(GeneModifierKey geneModifierKey) {
        return geneMap.containsKey(geneModifierKey);
    }

    public double getValue(Gene gene, GeneOperationType operation) {
        return getValueFromMap(gene, operation, geneMap);
    }

    public double getValue(Gene gene) {
        var multiplicative = getValue(gene, GeneOperationType.MULTIPLICATIVE);
        var additive = getValue(gene, GeneOperationType.ADDITIVE);
        return multiplicative + additive;
    }

    /** True when no gene is held at all - the cheap check hot paths use before looking anything up. */
    public boolean isEmpty() {
        return geneMap.isEmpty();
    }

    public Map<GeneModifierKey, Double> getBackingMap() {
        return Collections.unmodifiableMap(geneMap);
    }

    public void putAll(Map<GeneModifierKey, Double> geneMap) {
        geneMap.forEach(this::add);
    }

    public void add(Gene gene, GeneOperationType operation, double value) {
        add(gene.id(), operation, value);
    }

    public void add(ResourceLocation resourceLocation, GeneOperationType operation, double value) {
        add(new GeneModifierKey(resourceLocation, operation), value);
    }

    public void add(GeneModifierKey geneModifierKey, double value) {
        addOrInsert(geneModifierKey, value, geneMap);
    }

    private void addOrInsert(
        GeneModifierKey geneModifierKey,
        double additiveValue,
        Map<GeneModifierKey, Double> geneMap
    ) {
        var gene = GeneRegistry.getValueOrNull(geneModifierKey.resourceLocation());

        if (gene == null) {
            return;
        }

        var oldValue = geneMap.get(geneModifierKey);
        var oldValueNotNull = oldValue == null ? 0 : oldValue;
        var newValue = gene.transformer().apply(oldValueNotNull + additiveValue, geneModifierKey.operation());

        geneMap.put(geneModifierKey, newValue);

        var valueDidChange = !Objects.equals(oldValue, newValue);

        if (valueDidChange) {
            dirtyKeys.add(geneModifierKey);
        }
    }

    private double getValueFromMap(BLibHolder<Gene> geneHolder, GeneOperationType operation, Map<GeneModifierKey, Double> geneMap) {
        return getValueFromMap(geneHolder.get(), operation, geneMap);
    }

    private double getValueFromMap(Gene gene, GeneOperationType operation, Map<GeneModifierKey, Double> geneMap) {
        return geneMap.getOrDefault(new GeneModifierKey(gene.id(), operation), 0.0);
    }

    public void clear() {
        setDirty();
        geneMap.clear();
    }

    /** Takes a gene modifier out entirely (marked dirty, so its effect is taken off on the next tick). */
    public void remove(GeneModifierKey geneModifierKey) {
        if (geneMap.remove(geneModifierKey) != null) {
            dirtyKeys.add(geneModifierKey);
        }
    }

    public Set<GeneModifierKey> getDirtyKeys() {
        return Collections.unmodifiableSet(dirtyKeys);
    }

    public void clearDirtyKeys() {
        dirtyKeys.clear();
    }

    public boolean isDirty() {
        return !dirtyKeys.isEmpty();
    }

    public void setDirty() {
        dirtyKeys.addAll(geneMap.keySet());
    }
}
