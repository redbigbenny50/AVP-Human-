package com.human.common.gameplay.gene;

import com.human.common.gameplay.entity.manager.GeneContainer;
import com.human.common.gameplay.entity.manager.GeneMap;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

/**
 * How a bred baby's genes come from its two parents.
 * <p>
 * [stated] Oct 25, "Option A": genes blend - not climbing too fast, not stagnating. For every gene the baby takes a
 * blend that leans 70% toward the stronger parent, then wobbles between -5% and +18% of that blend. Near a gene's cap
 * the upward wobble shrinks ({@code (1 - value / cap)^0.5}) so selective breeding approaches the cap rather than
 * slamming into it. Genetic integrity does NOT lean - it is the plain average of both parents, or two degraded parents
 * would breed their way back to health.
 * </p>
 * <p>
 * A parent's DORMANT genes count on top of its active ones: injected genes sleep in the injected animal and wake in its
 * offspring. The baby's dormant map starts empty.
 * </p>
 * <p>
 * <b>Inbreeding strain</b> (Oct 25/26 design): only in a MODIFIED line - [stated] "the gene process begins for a pair
 * once the player injects it", and one injected parent is enough. Kinship comes from the family record each bred animal
 * carries (parents + grandparents):
 * </p>
 * <ul>
 * <li>siblings, or parent with child: the parents' average strain <b>+{@value #STRAIN_CLOSE}</b>;</li>
 * <li>cousins (a shared grandparent, or aunt/uncle): <b>+{@value #STRAIN_COUSIN}</b>;</li>
 * <li>unrelated: <b>halved</b> - fresh blood clears it.</li>
 * </ul>
 * <p>
 * Strain (0..1) shrinks the upward wobble by up to 60%, deepens the downward wobble from -5% to -15%, and costs the
 * baby up to {@value #STRAIN_INTEGRITY_COST} integrity at birth, scaled by strain. Unmodified lines never strain.
 * </p>
 */
public final class GeneInheritance {

    public static final double LEAN_TO_STRONGER = 0.70;

    public static final double WOBBLE_MIN = -0.05;

    public static final double WOBBLE_MAX = 0.18;

    public static final double STRAIN_CLOSE = 0.15;

    public static final double STRAIN_COUSIN = 0.05;

    /** Strain shrinks the upward wobble by up to this share. */
    public static final double STRAIN_UP_PENALTY = 0.6;

    /** Strain deepens the downward wobble by up to this much (-5% becomes -15% at full strain). */
    public static final double STRAIN_DOWN_PENALTY = 0.10;

    /** Integrity a baby loses at birth at full strain. */
    public static final double STRAIN_INTEGRITY_COST = 0.10;

    public enum Kinship {
        CLOSE,
        COUSIN,
        UNRELATED
    }

    /** A transformer result at or above this is read as "no cap" (the gene is not clamped). */
    private static final double UNCAPPED = 1.0e8;

    private GeneInheritance() {
        throw new UnsupportedOperationException();
    }

    public static void blendInto(GeneContainer parentA, GeneContainer parentB, GeneContainer child, RandomSource random) {
        blendInto(parentA, null, parentB, null, child, random);
    }

    /**
     * Breeds {@code child} from two parents. With the parents' UUIDs the baby gets a family record and, in a modified
     * line, inbreeding strain.
     */
    public static void blendInto(
        GeneContainer parentA,
        @org.jetbrains.annotations.Nullable java.util.UUID idA,
        GeneContainer parentB,
        @org.jetbrains.annotations.Nullable java.util.UUID idB,
        GeneContainer child,
        RandomSource random
    ) {
        var modifiedLine = parentA.isModified() || parentB.isModified();
        var kinship = kinship(idA, parentA.getLineage(), idB, parentB.getLineage());
        var childStrain = modifiedLine ? childStrain(parentA.getStrain(), parentB.getStrain(), kinship) : 0.0;
        var wobbleMax = WOBBLE_MAX * (1.0 - STRAIN_UP_PENALTY * childStrain);
        var wobbleMin = WOBBLE_MIN - STRAIN_DOWN_PENALTY * childStrain;

        var a = effective(parentA);
        var b = effective(parentB);

        var keys = new java.util.LinkedHashSet<GeneModifierKey>();
        keys.addAll(a.keySet());
        keys.addAll(b.keySet());

        child.clear();

        for (var key : keys) {
            var gene = GeneRegistry.getValueOrNull(key.resourceLocation());

            if (gene == null) {
                continue;
            }

            var va = a.getOrDefault(key, 0.0);
            var vb = b.getOrDefault(key, 0.0);
            double value;

            if (gene == Genes.GENETIC_INTEGRITY) {
                value = (va + vb) / 2.0;
            } else {
                var high = Math.max(va, vb);
                var low = Math.min(va, vb);
                var blend = LEAN_TO_STRONGER * high + (1.0 - LEAN_TO_STRONGER) * low;
                var wobble = wobbleMin + random.nextDouble() * (wobbleMax - wobbleMin);

                if (wobble > 0) {
                    var cap = gene.transformer().apply(UNCAPPED * 10, key.operation());

                    if (cap < UNCAPPED && cap > 0 && blend > 0) {
                        wobble *= Math.sqrt(Math.max(0.0, 1.0 - blend / cap));
                    }
                }

                // Wobble is a share of the blend's size, so a negative (weakness) gene wobbles the same way a positive
                // one does: an upward wobble always moves it toward "better".
                value = blend + Math.abs(blend) * wobble;
            }

            if (value != 0.0) {
                child.getActiveGeneMap().add(key, value);
            }
        }

        // A baby of a modified line is modified, one generation further on.
        if (modifiedLine) {
            child.setModified(true);
            child.setGeneration(Math.max(parentA.getGeneration(), parentB.getGeneration()) + 1);
        }

        // Inbreeding strain and its integrity cost (modified lines only), and the baby's family record.
        child.setStrain(childStrain);
        if (childStrain > 0) {
            child.getActiveGeneMap()
                .add(Genes.GENETIC_INTEGRITY.id(), GeneOperationType.ADDITIVE, -STRAIN_INTEGRITY_COST * childStrain);
        }
        if (idA != null && idB != null) {
            var lineageA = parentA.getLineage();
            var lineageB = parentB.getLineage();
            child.setLineage(
                new java.util.UUID[] { idA, idB, slot(lineageA, 0), slot(lineageA, 1), slot(lineageB, 0), slot(lineageB, 1) }
            );
        }

        // Born UNSTABLE or worse: a chance of a defect.
        GeneDefects.rollAtBirth(child, random);
    }

    /** Active plus dormant, summed per modifier. */
    public static double childStrain(double strainA, double strainB, Kinship kinship) {
        var average = (strainA + strainB) / 2.0;
        return switch (kinship) {
            case CLOSE -> Math.min(1.0, average + STRAIN_CLOSE);
            case COUSIN -> Math.min(1.0, average + STRAIN_COUSIN);
            case UNRELATED -> average * 0.5;
        };
    }

    /**
     * How closely two animals are related, from their own ids and family records (parents at 0-1, grandparents at 2-5).
     * Missing ids mean unknown, which counts as unrelated.
     */
    public static Kinship kinship(
        @org.jetbrains.annotations.Nullable java.util.UUID idA,
        java.util.UUID[] lineageA,
        @org.jetbrains.annotations.Nullable java.util.UUID idB,
        java.util.UUID[] lineageB
    ) {
        var parentsA = ids(lineageA, 0, 2);
        var parentsB = ids(lineageB, 0, 2);
        var grandparentsA = ids(lineageA, 2, 6);
        var grandparentsB = ids(lineageB, 2, 6);

        // Parent with child, or siblings (a shared parent).
        if ((idA != null && parentsB.contains(idA)) || (idB != null && parentsA.contains(idB)) || intersects(parentsA, parentsB)) {
            return Kinship.CLOSE;
        }

        // Grandparent with grandchild, aunt/uncle with niece/nephew, or cousins (a shared grandparent).
        if (
            (idA != null && grandparentsB.contains(idA)) || (idB != null && grandparentsA.contains(idB))
                || intersects(parentsA, grandparentsB) || intersects(parentsB, grandparentsA)
                || intersects(grandparentsA, grandparentsB)
        ) {
            return Kinship.COUSIN;
        }

        return Kinship.UNRELATED;
    }

    private static java.util.Set<java.util.UUID> ids(java.util.UUID[] lineage, int from, int to) {
        var set = new java.util.HashSet<java.util.UUID>();
        for (var i = from; i < Math.min(to, lineage.length); i++) {
            if (lineage[i] != null) {
                set.add(lineage[i]);
            }
        }
        return set;
    }

    private static boolean intersects(java.util.Set<java.util.UUID> a, java.util.Set<java.util.UUID> b) {
        for (var id : a) {
            if (b.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private static @org.jetbrains.annotations.Nullable java.util.UUID slot(java.util.UUID[] lineage, int index) {
        return index < lineage.length ? lineage[index] : null;
    }

    private static Map<GeneModifierKey, Double> effective(GeneContainer container) {
        var out = new HashMap<GeneModifierKey, Double>(container.getActiveGeneMap().getBackingMap());
        merge(out, container.getDormantGeneMap());
        return out;
    }

    private static void merge(Map<GeneModifierKey, Double> into, GeneMap from) {
        from.getBackingMap().forEach((k, v) -> into.merge(k, v, Double::sum));
    }
}
