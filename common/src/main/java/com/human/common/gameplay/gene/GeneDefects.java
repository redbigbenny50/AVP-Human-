package com.human.common.gameplay.gene;

import com.human.common.gameplay.entity.manager.GeneContainer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Defect genes (team-reviewed list, Oct 25/26). They can appear when a line's integrity is low: a baby or burster born
 * UNSTABLE or worse rolls once for a defect, with a higher chance the worse its integrity is. Defects are stored like
 * any other gene (1.0 = carried) so they pass down through breeding and eggs; anything at 0.5 or more counts as
 * carried.
 * <ul>
 * <li><b>Photophobia</b> - weakened and slowed in bright light.</li>
 * <li><b>Brittle Armor</b> - a quarter less armour.</li>
 * <li><b>Haemophilia</b> - harmful effects (bleeding, poison, damage over time) last half again as long.</li>
 * <li><b>Sterile</b> - cannot breed; a queen or empress lays more slowly.</li>
 * <li><b>Short-Lived</b> - every source of healing heals half as much.</li>
 * </ul>
 */
public final class GeneDefects {

    /** A defect at or above this value is carried. */
    public static final double CARRIED = 0.5;

    /** Chance a newborn rolls a defect, by integrity level. */
    public static final double CHANCE_UNSTABLE = 0.15;

    public static final double CHANCE_VOLATILE = 0.35;

    public static final double CHANCE_FATAL = 0.60;

    private static final List<Gene> DEFECTS = List.of(
        Genes.PHOTOPHOBIA,
        Genes.BRITTLE_ARMOR,
        Genes.HAEMOPHILIA,
        Genes.STERILE,
        Genes.SHORT_LIVED
    );

    private GeneDefects() {}

    public static List<Gene> all() {
        return DEFECTS;
    }

    public static boolean carries(GeneContainer container, Gene defect) {
        return container.getActiveGeneMap().getValue(defect) >= CARRIED;
    }

    /**
     * The line's integrity as the decay levels read it (0 healthy; below -1 / -2 / -3 is Unstable / Volatile / Fatal).
     */
    public static double integrity(GeneContainer container) {
        return container.getActiveGeneMap().getValue(Genes.GENETIC_INTEGRITY);
    }

    /**
     * Rolls a newborn for a defect. Call once, after its genes are final. Only rolls for UNSTABLE or worse, and only
     * picks a defect the newborn does not already carry.
     */
    public static void rollAtBirth(GeneContainer newborn, RandomSource random) {
        var integrity = integrity(newborn);
        double chance;

        if (integrity < -3) {
            chance = CHANCE_FATAL;
        } else if (integrity < -2) {
            chance = CHANCE_VOLATILE;
        } else if (integrity < -1) {
            chance = CHANCE_UNSTABLE;
        } else {
            return;
        }

        if (random.nextDouble() >= chance) {
            return;
        }

        var candidates = new ArrayList<Gene>();
        for (var defect : DEFECTS) {
            if (!carries(newborn, defect)) {
                candidates.add(defect);
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        var defect = candidates.get(random.nextInt(candidates.size()));
        var current = newborn.getActiveGeneMap().getValue(defect, GeneOperationType.ADDITIVE);
        newborn.getActiveGeneMap().add(defect.id(), GeneOperationType.ADDITIVE, 1.0 - current);
    }
}
