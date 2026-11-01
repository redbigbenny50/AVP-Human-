package com.human.common.gameplay.entity.manager;

import com.blib.api.common.nbt.v1.model.NBTSerializable;
import com.human.common.util.GeneSerializationUtil;
import net.minecraft.nbt.CompoundTag;

public class GeneContainer implements NBTSerializable {

    private static final String NBT_GENE_MODIFIERS = "geneModifiers";

    private static final String NBT_DORMANT_GENE_MODIFIERS = "dormantGeneModifiers";

    private static final String NBT_MODIFIED = "geneModified";

    private static final String NBT_GENERATION = "geneGeneration";

    private static final String NBT_ENGINEERED = "engineeredGenes";

    private static final String NBT_STRAIN = "geneStrain";

    private static final String NBT_LINEAGE = "geneLineage";

    private final GeneMap geneMap;

    private final GeneMap dormantGeneMap;

    /**
     * [stated] Oct 26 naming - a mob whose line has been injected is MODIFIED. The first injection sets it; it passes
     * to offspring (either parent modified ⇒ baby modified) and to anything hatched or burst from it. Degradation
     * tracking (strain, generation) only applies to modified lines.
     */
    private boolean modified;

    /** Generations since the line was first modified (0 = the injected mob itself). */
    private int generation;

    /**
     * Inbreeding strain, 0..1 (see GeneInheritance). Builds when close relatives in a MODIFIED line are bred, halves
     * with unrelated blood. Only bred animals have it.
     */
    private double strain;

    /**
     * The family record kinship is read from: the two parents' UUIDs then the four grandparents' (any may be null).
     * Only animals born from breeding carry one, so wild and spawned mobs cost nothing. It never looks anything up in
     * the world - kinship is worked out from these ids alone, so it works with dead or unloaded relatives.
     */
    private java.util.UUID[] lineage = new java.util.UUID[0];

    /**
     * Advanced genes locked in by the genetics machines (ENGINEERED). Kept per mob; the machines that create them are
     * the next build, this is the storage they will write to. See AdvancedGenes.
     */
    private final java.util.Set<com.human.common.gameplay.gene.Gene> engineeredGenes = new java.util.LinkedHashSet<>();

    public GeneContainer() {
        this.geneMap = new GeneMap();
        this.dormantGeneMap = new GeneMap();
    }

    public GeneMap getActiveGeneMap() {
        return geneMap;
    }

    public GeneMap getDormantGeneMap() {
        return dormantGeneMap;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public int getGeneration() {
        return generation;
    }

    public void setGeneration(int generation) {
        this.generation = Math.max(0, generation);
    }

    public double getStrain() {
        return strain;
    }

    public void setStrain(double strain) {
        this.strain = Math.clamp(strain, 0.0, 1.0);
    }

    /** Parents then grandparents (6 slots, nulls allowed), or an empty array for a mob that wasn't bred. */
    public java.util.UUID[] getLineage() {
        return lineage.clone();
    }

    public void setLineage(java.util.UUID[] lineage) {
        this.lineage = lineage == null ? new java.util.UUID[0] : lineage.clone();
    }

    public java.util.Set<com.human.common.gameplay.gene.Gene> getEngineeredGenes() {
        return java.util.Collections.unmodifiableSet(engineeredGenes);
    }

    /** Takes an engineered advanced gene off again (a player's temporary infusion ending). */
    public void unlockEngineered(com.human.common.gameplay.gene.Gene gene) {
        if (engineeredGenes.remove(gene)) {
            geneMap.setDirty();
        }
    }

    /** Locks in an engineered advanced gene, replacing any other engineered gene in the same category. */
    public void lockEngineered(com.human.common.gameplay.gene.Gene gene) {
        var definition = com.human.common.gameplay.gene.AdvancedGenes.definition(gene);

        if (definition == null) {
            return;
        }

        engineeredGenes.removeIf(other -> {
            var otherDefinition = com.human.common.gameplay.gene.AdvancedGenes.definition(other);
            return otherDefinition != null && otherDefinition.category() == definition.category();
        });
        engineeredGenes.add(gene);
        // Make the manager re-evaluate advanced genes on its next tick.
        geneMap.setDirty();
    }

    public void transfer(GeneContainer other, boolean activateDormantGenes) {
        // A modified line stays modified through every egg, hugger and burster it passes into.
        if (modified) {
            other.modified = true;
            other.generation = Math.max(other.generation, generation);
        }

        other.geneMap.putAll(geneMap.getBackingMap());

        if (activateDormantGenes) {
            dormantGeneMap.getBackingMap().forEach(other.getActiveGeneMap()::add);
        } else {
            other.dormantGeneMap.putAll(dormantGeneMap.getBackingMap());
        }
    }

    public void clear() {
        geneMap.clear();
        dormantGeneMap.clear();
        modified = false;
        generation = 0;
        strain = 0;
        lineage = new java.util.UUID[0];
        engineeredGenes.clear();
    }

    @Override
    public void load(CompoundTag compoundTag) {
        geneMap.clear();
        dormantGeneMap.clear();

        GeneSerializationUtil.loadGeneModifiers(NBT_GENE_MODIFIERS, compoundTag, geneMap);
        GeneSerializationUtil.loadGeneModifiers(NBT_DORMANT_GENE_MODIFIERS, compoundTag, dormantGeneMap);

        modified = compoundTag.getBoolean(NBT_MODIFIED);
        generation = compoundTag.getInt(NBT_GENERATION);
        strain = compoundTag.getDouble(NBT_STRAIN);

        var lineageList = compoundTag.getList(NBT_LINEAGE, net.minecraft.nbt.Tag.TAG_INT_ARRAY);
        lineage = new java.util.UUID[lineageList.size()];
        for (var i = 0; i < lineageList.size(); i++) {
            var ints = lineageList.getIntArray(i);
            lineage[i] = ints.length == 4 && (ints[0] | ints[1] | ints[2] | ints[3]) != 0
                ? net.minecraft.core.UUIDUtil.uuidFromIntArray(ints)
                : null;
        }

        engineeredGenes.clear();
        for (var element : compoundTag.getList(NBT_ENGINEERED, net.minecraft.nbt.Tag.TAG_STRING)) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(element.getAsString());
            var gene = id == null ? null : com.human.common.gameplay.gene.GeneRegistry.getValueOrNull(id);
            if (gene != null) {
                engineeredGenes.add(gene);
            }
        }

        // Mark all keys as dirty post-load in case the data changed.
        geneMap.setDirty();
    }

    @Override
    public void save(CompoundTag compoundTag) {
        GeneSerializationUtil.saveGeneModifiers(NBT_GENE_MODIFIERS, compoundTag, geneMap);
        GeneSerializationUtil.saveGeneModifiers(NBT_DORMANT_GENE_MODIFIERS, compoundTag, dormantGeneMap);
        if (modified) {
            compoundTag.putBoolean(NBT_MODIFIED, true);
            compoundTag.putInt(NBT_GENERATION, generation);
        }

        if (strain > 0) {
            compoundTag.putDouble(NBT_STRAIN, strain);
        }

        if (lineage.length > 0) {
            var lineageList = new net.minecraft.nbt.ListTag();
            for (var id : lineage) {
                lineageList.add(
                    new net.minecraft.nbt.IntArrayTag(id == null ? new int[4] : net.minecraft.core.UUIDUtil.uuidToIntArray(id))
                );
            }
            compoundTag.put(NBT_LINEAGE, lineageList);
        }

        if (!engineeredGenes.isEmpty()) {
            var list = new net.minecraft.nbt.ListTag();
            for (var gene : engineeredGenes) {
                list.add(net.minecraft.nbt.StringTag.valueOf(gene.id().toString()));
            }
            compoundTag.put(NBT_ENGINEERED, list);
        }
    }
}
