package com.human.common.gameplay.gene;

import com.human.common.gameplay.component.SyringeContents;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The one place gene colours come from, so a gene looks the same everywhere it shows: the tube item's fluid, the
 * syringe's barrel, the Gene Storage carousel and the Analyzer's sample tube.
 * <p>
 * A single gene has a fixed colour (gold for mutations, dark red for defects, otherwise a stable hue from its id).
 * [stated] Oct 29 - a syringe shows a BLEND of the colours of the genes it holds, weighted by how strong each gene is,
 * so one dominant gene tints it most.
 */
public final class GeneColours {

    /** Shown when there is nothing to colour (an empty syringe's barrel is drawn clear instead). */
    public static final int NONE = 0x00FFFFFF;

    public static final int MUTATION = 0xFFE6B428;

    public static final int DEFECT = 0xFF8C1E1E;

    /** An unknown or unreadable gene. */
    public static final int UNKNOWN = 0xFFB0B0B0;

    private GeneColours() {}

    /** The colour of one gene (opaque ARGB). Stable per gene id. */
    public static int of(ResourceLocation geneId) {
        if (geneId == null) {
            return UNKNOWN;
        }
        var gene = GeneRegistry.getValueOrNull(geneId);
        if (gene != null && AdvancedGenes.definition(gene) != null) {
            return MUTATION;
        }
        if (gene != null && GeneDefects.all().contains(gene)) {
            return DEFECT;
        }
        var hue = (geneId.toString().hashCode() & 0xFFFF) / 65535.0F;
        return 0xFF000000 | Mth.hsvToRgb(hue, 0.6F, 0.9F);
    }

    /**
     * The blended colour of everything in a syringe, or {@link #NONE} (fully clear) when it holds nothing. Genetic
     * integrity is bookkeeping, not a gene you can see, so it never colours the blend.
     */
    public static int syringe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return NONE;
        }
        var contents = stack.get(HumanDataComponents.SYRINGE_CONTENTS.get());
        return blend(contents);
    }

    public static int blend(SyringeContents contents) {
        if (contents == null || contents.geneBonusDataEntries().isEmpty()) {
            return NONE;
        }
        double r = 0, g = 0, b = 0, total = 0;
        for (var entry : contents.geneBonusDataEntries()) {
            if (entry.id().equals(Genes.GENETIC_INTEGRITY.id()) || entry.value() == 0.0) {
                continue;
            }
            // Weighted by strength, with a floor so a weak gene still shows a little.
            var weight = Math.max(0.05, Math.abs(entry.value()));
            var colour = of(entry.id());
            r += ((colour >> 16) & 0xFF) * weight;
            g += ((colour >> 8) & 0xFF) * weight;
            b += (colour & 0xFF) * weight;
            total += weight;
        }
        if (total <= 0.0) {
            return NONE;
        }
        return 0xFF000000 | (Mth.clamp((int) Math.round(r / total), 0, 255) << 16) | (Mth.clamp(
            (int) Math.round(g / total),
            0,
            255
        ) << 8) | Mth.clamp((int) Math.round(b / total), 0, 255);
    }
}
