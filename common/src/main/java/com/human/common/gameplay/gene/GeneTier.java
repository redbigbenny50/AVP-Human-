package com.human.common.gameplay.gene;

/**
 * Donor quality for one gene. The tier is how many syringe draws it takes to reach the gene's cap - Prime 8, Strong 12,
 * Average 16, Weak 32. [stated] Oct 27 - a syringe draw extracts 100% of the donor's species value, so one draw is one
 * dose and a species value is {@code cap / doses}. A burster takes 50% of its host's value (75% from a royal hugger).
 * WEAKNESS is a carrier of the gene as a penalty, at the Average size, negative.
 */
public enum GeneTier {

    PRIME(8),
    STRONG(12),
    AVERAGE(16),
    WEAK(32),
    WEAKNESS(-16);

    private final int dosesToCap;

    GeneTier(int dosesToCap) {
        this.dosesToCap = dosesToCap;
    }

    public int dosesToCap() {
        return dosesToCap;
    }

    /** The species value of {@code gene} for a donor of this tier. */
    public double speciesValue(Gene gene) {
        return Genes.cap(gene) / dosesToCap;
    }
}
