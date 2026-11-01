package com.human.common.gameplay.gene;

import com.just.core.functional.function.Function2;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Consumer;

public sealed interface Gene {

    ResourceLocation id();

    Function2<Double, GeneOperationType, Double> transformer();

    default String getTranslationKey() {
        return "gene.avp." + id();
    }

    /** Description shown in the gene reader / tooltips: what the gene does (never where to get it). */
    default String getDescriptionKey() {
        return "gene.avp." + id() + ".desc";
    }

    /**
     * A gene that is applied as an attribute modifier.
     * <p>
     * {@code attributeScale} multiplies the gene's value before it is applied, so a gene can be stored as a positive,
     * player-facing number while pushing its attribute the other way (Lightweight is a positive % that LOWERS gravity).
     * </p>
     */
    record Attribute(
        ResourceLocation id,
        Holder<net.minecraft.world.entity.ai.attributes.Attribute> attributeHolder,
        Function2<Double, GeneOperationType, Double> transformer,
        double attributeScale
    ) implements Gene {

        public Attribute(
            ResourceLocation id,
            Holder<net.minecraft.world.entity.ai.attributes.Attribute> attributeHolder,
            Function2<Double, GeneOperationType, Double> transformer
        ) {
            this(id, attributeHolder, transformer, 1.0);
        }
    }

    record Effect(
        ResourceLocation id,
        Consumer<LivingEntity> onChange,
        Function2<Double, GeneOperationType, Double> transformer
    ) implements Gene {}

    record Simple(
        ResourceLocation id,
        Function2<Double, GeneOperationType, Double> transformer
    ) implements Gene {}

    /**
     * A loot gene: the carrier drops extra of one item when it dies. Created on demand from loot tables (see
     * LootGenes), one per item, so it is named after its item.
     */
    record Loot(
        ResourceLocation id,
        ResourceLocation itemId,
        Function2<Double, GeneOperationType, Double> transformer
    ) implements Gene {

        @Override
        public String getTranslationKey() {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId).getDescriptionId();
        }

        @Override
        public String getDescriptionKey() {
            return "gene.avp.loot.desc";
        }
    }
}
