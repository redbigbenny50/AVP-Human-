package com.human.common.gameplay.gene.machine;

import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.gene.Genes;
import com.human.common.gameplay.gene.tube.GeneTubeItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Gene Recombinator's work. [stated] Oct 28 rulings:
 * <ul>
 * <li>the sample gains ONE dose of the stronger vial's strength, never past the gene's cap;</li>
 * <li>the two poured vials come back as empty tubes;</li>
 * <li>only base and loot/harvest genes recombine - mutations and defects don't stack;</li>
 * <li>the vials must match the sample's gene (not its donor);</li>
 * <li>10 seconds a run.</li>
 * </ul>
 */
public final class GeneRecombination {

    public static final int RUN_TICKS = 10 * 20;

    public static final int EMPTIES_BACK = 2;

    public enum Refusal {

        MISSING("LOAD A SAMPLE AND TWO VIALS"),
        WRONG_GENE("VIALS MUST MATCH THE SAMPLE'S GENE"),
        DOES_NOT_STACK("MUTATIONS AND DEFECTS DON'T STACK"),
        AT_CAP("SAMPLE IS AT CAP · NOTHING TO ADD");

        public final String message;

        Refusal(String message) {
            this.message = message;
        }
    }

    /** Either a result tube or the reason there is none. */
    public record Outcome(
        ItemStack result,
        @Nullable Refusal refusal
    ) {

        public boolean ok() {
            return refusal == null;
        }
    }

    private GeneRecombination() {}

    public static Outcome combine(ItemStack sample, ItemStack vialA, ItemStack vialB) {
        var s = GeneTubeItem.read(sample);
        var a = GeneTubeItem.read(vialA);
        var b = GeneTubeItem.read(vialB);

        if (s == null || a == null || b == null) {
            return new Outcome(ItemStack.EMPTY, Refusal.MISSING);
        }
        if (!s.gene().equals(a.gene()) || !s.gene().equals(b.gene())) {
            return new Outcome(ItemStack.EMPTY, Refusal.WRONG_GENE);
        }

        var gene = GeneRegistry.getValueOrNull(s.gene());
        if (gene == null) {
            return new Outcome(ItemStack.EMPTY, Refusal.MISSING);
        }
        var category = Genes.category(gene);
        if (category == Genes.Category.ADVANCED || category == Genes.Category.DEFECT) {
            return new Outcome(ItemStack.EMPTY, Refusal.DOES_NOT_STACK);
        }

        var cap = Genes.cap(gene);
        // The stronger vial's dose, by magnitude (a weakness tube's doses are negative and grow more negative).
        var dose = Math.abs(a.perDose()) >= Math.abs(b.perDose()) ? a.perDose() : b.perDose();
        if (Math.abs(s.value()) >= cap - 1.0E-6) {
            return new Outcome(ItemStack.EMPTY, Refusal.AT_CAP);
        }

        var value = s.value() + dose;
        value = Math.copySign(Math.min(Math.abs(value), cap), value);

        return new Outcome(GeneTubeItem.create(s.gene(), s.operation(), value, s.donor(), s.doses() + 1), null);
    }
}
