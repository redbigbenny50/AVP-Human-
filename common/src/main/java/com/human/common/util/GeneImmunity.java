package com.human.common.util;

import com.human.common.gameplay.gene.Gene;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.gene.Genes;
import com.human.common.model.GeneCarrier;
import net.minecraft.world.entity.Entity;

/**
 * [stated] Oct 19 - "If a mob is cold immune it wont freeze so if their cold resist gene was at 100% they wouldn't
 * freeze same thing with fire and acid needs 100% to be immune". Below 100% a resistance gene only cuts the damage
 * (GeneResistanceHurtUtil); AT 100% the mob is treated exactly like one that is naturally immune:
 * <ul>
 * <li><b>Cold</b> - never freezes: no frost from powder snow or cryo, never frozen solid, no cold damage
 * (CryoFreezing.isImmune, which FrozenSolid and the vapors read, and LivingEntity.canFreeze).</li>
 * <li><b>Fire</b> - fire-immune like a blaze: never set alight, no fire or lava damage, infernium vapor passes it by
 * (Entity.fireImmune).</li>
 * <li><b>Acid</b> - untouched by acid liquid and acid vapor, as an acid-immune mob is (AcidEffects, VaporEffects).</li>
 * </ul>
 * "100%" is the gene's percentage reduction - the same value GeneResistanceHurtUtil uses - reaching 1.
 */
public final class GeneImmunity {

    /** A hair under 1, so a value that adds up to 100% through rounding still counts. */
    private static final double FULL = 1.0D - 1.0E-6D;

    private GeneImmunity() {}

    public static boolean coldImmune(Entity entity) {
        return full(entity, Genes.COLD_RESISTANCE);
    }

    public static boolean fireImmune(Entity entity) {
        return full(entity, Genes.FIRE_RESISTANCE);
    }

    public static boolean acidImmune(Entity entity) {
        return full(entity, Genes.ACID_RESISTANCE);
    }

    private static boolean full(Entity entity, Gene gene) {
        if (!(entity instanceof GeneCarrier carrier)) {
            return false;
        }

        return carrier.getOrCreateGeneManager()
            .getGeneContainer()
            .getActiveGeneMap()
            .getValue(gene, GeneOperationType.MULTIPLICATIVE) >= FULL;
    }
}
