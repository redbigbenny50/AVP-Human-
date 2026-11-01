package com.human.common.gameplay.item;

import com.human.common.gameplay.gene.AdvancedGenes;
import com.human.common.gameplay.gene.GeneDefects;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.gene.Genes;
import com.human.common.gameplay.gene.discovery.ClientGeneDiscoveries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.Locale;

/**
 * Tooltip lines shared by the syringe and gene tubes: a gene's name and value when the viewing player has discovered
 * it, ??? when they haven't. Mutations read amber, defects red. Reads the client's own discovery list.
 */
public final class GeneTooltips {

    private GeneTooltips() {}

    public static Component line(ResourceLocation id, GeneOperationType operation, double value) {
        var gene = GeneRegistry.getValueOrNull(id);

        if (gene == null || !ClientGeneDiscoveries.knowsGene(id)) {
            return Component.literal("  ???").withStyle(ChatFormatting.DARK_GRAY);
        }

        var name = Component.translatable(gene.getTranslationKey());

        if (AdvancedGenes.definition(gene) != null) {
            return Component.literal("  ").append(name).append(" - mutation").withStyle(ChatFormatting.GOLD);
        }
        if (GeneDefects.all().contains(gene)) {
            return Component.literal("  ").append(name).append(" - defect").withStyle(ChatFormatting.RED);
        }

        var category = Genes.category(gene);
        String amount;
        if (category == Genes.Category.LOOT) {
            amount = "+" + Math.round(value * 100) + "% drop";
        } else if (operation == GeneOperationType.MULTIPLICATIVE) {
            amount = String.format(Locale.ROOT, "%+.1f%%", value * 100);
        } else {
            amount = String.format(Locale.ROOT, "%+.2f", value);
        }

        return Component.literal("  ").append(name).append(" " + amount).withStyle(value < 0 ? ChatFormatting.RED : ChatFormatting.GRAY);
    }

    public static Component donor(String typeId) {
        if (com.human.common.gameplay.gene.machine.GeneSplicing.SPLICED.equals(typeId)) {
            return Component.literal("Spliced").withStyle(ChatFormatting.DARK_GRAY);
        }
        var id = ResourceLocation.tryParse(typeId);
        if (id == null || !ClientGeneDiscoveries.knowsSpecies(id)) {
            return Component.literal("From: ???").withStyle(ChatFormatting.DARK_GRAY);
        }
        var name = EntityType.byString(typeId).map(type -> type.getDescription()).orElse(Component.literal(typeId));
        return Component.literal("From: ").append(name).withStyle(ChatFormatting.DARK_GRAY);
    }
}
