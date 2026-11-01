package com.human.common.gameplay.gene.machine;

import com.human.common.gameplay.component.SyringeContents;
import com.human.common.gameplay.gene.GeneBonusDataEntry;
import com.human.common.gameplay.gene.Genes;
import com.human.common.gameplay.gene.tube.GeneTubeItem;
import com.human.common.gameplay.item.SyringeItem;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The Gene Separator's work, kept apart from the block so it is the same everywhere (screen preview, the run itself, a
 * future automation path). [stated] Oct 28 rulings:
 * <ul>
 * <li>one tube per gene, holding everything the syringe had of that gene (a syringe is one draw, so one dose);</li>
 * <li>a draw's mutations and defects come out as tubes of their own (that is how defects get filtered out);</li>
 * <li>it needs one empty tube per gene and refuses otherwise;</li>
 * <li>8 seconds plus 1 second per gene;</li>
 * <li>separating costs no integrity, and the syringe comes back empty.</li>
 * </ul>
 */
public final class GeneSeparation {

    public static final int BASE_TICKS = 8 * 20;

    public static final int TICKS_PER_GENE = 20;

    private GeneSeparation() {}

    /**
     * The tubes a syringe would make, one per gene (Genetic Integrity is never in a sample, and is skipped if it is).
     */
    public static List<ItemStack> tubesFor(ItemStack syringe) {
        var contents = syringe.get(HumanDataComponents.SYRINGE_CONTENTS.get());
        if (contents == null) {
            return List.of();
        }
        var donor = SyringeItem.donorOf(syringe);
        var tubes = new ArrayList<ItemStack>();
        for (var entry : merged(contents)) {
            tubes.add(GeneTubeItem.create(entry.id(), entry.operation(), entry.value(), donor, 1));
        }
        return tubes;
    }

    /** One entry per gene (an old sample could carry the same gene twice; they add together). */
    private static List<GeneBonusDataEntry> merged(SyringeContents contents) {
        var byGene = new LinkedHashMap<String, GeneBonusDataEntry>();
        for (var entry : contents.geneBonusDataEntries()) {
            if (entry.id().equals(Genes.GENETIC_INTEGRITY.id()) || entry.value() == 0.0) {
                continue;
            }
            var key = entry.id() + "|" + entry.operation();
            var previous = byGene.get(key);
            byGene.put(
                key,
                previous == null ? entry : new GeneBonusDataEntry(entry.id(), entry.operation(), previous.value() + entry.value())
            );
        }
        return new ArrayList<>(byGene.values());
    }

    public static int tubesNeeded(ItemStack syringe) {
        return tubesFor(syringe).size();
    }

    public static int runTicks(int genes) {
        return BASE_TICKS + TICKS_PER_GENE * Math.max(0, genes);
    }

    /** Empties the syringe after a run (same as the reader's CLEAR SAMPLE). */
    public static void emptySyringe(ItemStack syringe) {
        SyringeItem.emptySyringe(syringe);
    }
}
