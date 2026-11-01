package com.human.common.gameplay.gene.machine;

import com.human.common.gameplay.component.SyringeContents;
import com.human.common.gameplay.gene.AdvancedGenes;
import com.human.common.gameplay.gene.GeneBonusDataEntry;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.gene.Genes;
import com.human.common.gameplay.gene.tube.GeneTubeItem;
import com.human.common.gameplay.item.SyringeItem;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * The Gene Splicer's work. [stated] Oct 28 rulings:
 * <ul>
 * <li>up to 5 genes per syringe;</li>
 * <li>a gene already in the syringe is REFUSED - stacking doses is the Recombinator's job;</li>
 * <li>at most one mutation per category (combat / movement / body), with an amber warning that it replaces the target's
 * mutation in that category;</li>
 * <li>defect tubes are allowed, with a red warning;</li>
 * <li>the finished syringe is labelled SPLICED, and the used tubes come back empty;</li>
 * <li>6 seconds plus 2 seconds per gene (it pipettes one tube at a time).</li>
 * </ul>
 */
public final class GeneSplicing {

    public static final int MAX_GENES = 5;

    public static final int BASE_TICKS = 6 * 20;

    public static final int TICKS_PER_GENE = 2 * 20;

    /** The donor id a spliced syringe carries; the reader and tooltips show it as SPLICED. */
    public static final String SPLICED = "avp_human:spliced";

    public enum Refusal {

        EMPTY("ADD AT LEAST ONE TUBE"),
        TOO_MANY("A SYRINGE HOLDS 5 GENES"),
        DUPLICATE("ALREADY IN · USE THE RECOMBINATOR TO STACK DOSES"),
        SECOND_MUTATION("ONE MUTATION PER CATEGORY"),
        NOT_A_TUBE("ONLY FILLED GENE TUBES");

        public final String message;

        Refusal(String message) {
            this.message = message;
        }
    }

    /** The checked syringe: what goes in, what it costs, and anything the player should be warned about. */
    public record Plan(
        List<GeneBonusDataEntry> entries,
        double integrityCost,
        List<String> mutationWarnings,
        List<String> defectWarnings,
        @Nullable Refusal refusal,
        int refusedIndex
    ) {

        public boolean ok() {
            return refusal == null;
        }
    }

    private GeneSplicing() {}

    /** Checks a set of tubes, in pipetting order. The first problem found refuses the whole plan. */
    public static Plan plan(List<ItemStack> tubes) {
        var entries = new ArrayList<GeneBonusDataEntry>();
        var mutationWarnings = new ArrayList<String>();
        var defectWarnings = new ArrayList<String>();
        var genes = new HashSet<String>();
        var mutationCategories = new HashSet<AdvancedGenes.AdvancedCategory>();

        var index = 0;
        for (var tube : tubes) {
            if (tube.isEmpty()) {
                index++;
                continue;
            }
            var contents = GeneTubeItem.read(tube);
            if (contents == null) {
                return refused(Refusal.NOT_A_TUBE, index);
            }
            if (!genes.add(contents.gene().toString())) {
                return refused(Refusal.DUPLICATE, index);
            }
            if (genes.size() > MAX_GENES) {
                return refused(Refusal.TOO_MANY, index);
            }

            var gene = GeneRegistry.getValueOrNull(contents.gene());
            if (gene != null) {
                var definition = AdvancedGenes.definition(gene);
                if (definition != null) {
                    if (!mutationCategories.add(definition.category())) {
                        return refused(Refusal.SECOND_MUTATION, index);
                    }
                    mutationWarnings.add(gene.id().getPath());
                } else if (Genes.category(gene) == Genes.Category.DEFECT) {
                    defectWarnings.add(gene.id().getPath());
                }
            }

            entries.add(new GeneBonusDataEntry(contents.gene(), contents.operation(), contents.value()));
            index++;
        }

        if (entries.isEmpty()) {
            return refused(Refusal.EMPTY, -1);
        }

        var cost = SyringeItem.injectionIntegrityCost(new SyringeContents(entries));
        return new Plan(entries, cost, mutationWarnings, defectWarnings, null, -1);
    }

    private static Plan refused(Refusal refusal, int index) {
        return new Plan(List.of(), 0.0, List.of(), List.of(), refusal, index);
    }

    public static int runTicks(int genes) {
        return BASE_TICKS + TICKS_PER_GENE * Math.max(0, genes);
    }

    /** Fills an empty syringe with a checked plan: contents, INJECT mode, SPLICED as its donor. */
    public static void fill(ItemStack syringe, Plan plan) {
        syringe.set(HumanDataComponents.SYRINGE_CONTENTS.get(), new SyringeContents(List.copyOf(plan.entries())));
        syringe.set(HumanDataComponents.SYRINGE_MODE.get(), com.human.common.gameplay.component.SyringeMode.INJECT);
        CustomData.update(DataComponents.CUSTOM_DATA, syringe, tag -> tag.putString(SyringeItem.DONOR_KEY, SPLICED));
    }
}
