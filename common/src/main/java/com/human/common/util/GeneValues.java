package com.human.common.util;

import com.human.common.gameplay.gene.Gene;
import com.human.common.model.GeneCarrier;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Reads a gene's live value off any entity. Every gene is dosed in ONE operation (see Genes.operation), but old saves
 * can still hold the other one, so the value read is the sum of both - the same total the reader shows.
 */
public final class GeneValues {

    private GeneValues() {}

    /** The gene's active value on {@code entity}, or 0 if it carries no genes. */
    public static double get(@Nullable Entity entity, Gene gene) {
        if (!(entity instanceof GeneCarrier carrier)) {
            return 0.0;
        }

        // ⚠ Hot path: this runs from step sounds, vibrations and every visibility check. Almost every mob carries no
        // genes, so bail out before building any lookup keys.
        var map = carrier.getOrCreateGeneManager().getGeneContainer().getActiveGeneMap();
        return map.isEmpty() ? 0.0 : map.getValue(gene);
    }

    /** True when this advanced gene is ACTIVE on the entity (a mutation or engineered). */
    public static boolean hasAdvanced(@Nullable Entity entity, Gene gene) {
        return entity instanceof GeneCarrier carrier && carrier.getOrCreateGeneManager().hasAdvanced(gene);
    }

    /** True when the entity carries this defect. */
    public static boolean carriesDefect(@Nullable Entity entity, Gene defect) {
        if (!(entity instanceof GeneCarrier carrier)) {
            return false;
        }

        var container = carrier.getOrCreateGeneManager().getGeneContainer();
        // Hot path (every heal and every effect applied): skip the lookup on the many mobs with no genes.
        return !container.getActiveGeneMap().isEmpty() && com.human.common.gameplay.gene.GeneDefects.carries(container, defect);
    }

    /** True when the entity carries no active genes at all - lets per-tick effects bail out cheaply. */
    public static boolean hasNoGenes(Entity entity) {
        return !(entity instanceof GeneCarrier carrier)
            || carrier.getOrCreateGeneManager().getGeneContainer().getActiveGeneMap().isEmpty();
    }
}
