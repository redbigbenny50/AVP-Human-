package com.human.common.gameplay.gene.geneticist;

import com.human.HumanResources;
import com.human.common.gameplay.entity.living.human.colonist.ColonistEntity;
import com.human.common.gameplay.entity.living.human.colonist.ColonistProfession;
import com.human.common.registry.init.HumanGeneTubes;
import com.human.common.registry.init.HumanGeneticist;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * The Geneticist, worked at the Geneticist Station. [stated] Oct 28: the outfit is a knee-length lab coat with goggles,
 * syringes on the belt and gene tubes in the chest pocket, and he holds a gene reader.
 * <p>
 * Trades, [stated] Oct 28: his first trades are syringes, empty gene tubes and the gene reader. Still to come as they
 * are built: the five machine blueprint chips (5 diamonds each, [stated]) and the cleanser.
 * </p>
 */
public final class GeneticistProfession implements ColonistProfession {

    public static final GeneticistProfession INSTANCE = new GeneticistProfession();

    private static final ResourceLocation ID = HumanResources.location("geneticist");

    private static final int MAX_USES = 16;

    private GeneticistProfession() {}

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public boolean isWorkstation(BlockState state) {
        return state.is(HumanGeneticist.GENETICIST_STATION.get());
    }

    @Override
    public Component title() {
        return Component.translatable("entity.avp_human.colonist.geneticist");
    }

    @Override
    public Map<EquipmentSlot, ItemStack> outfit() {
        return Map.of(
            EquipmentSlot.HEAD,
            new ItemStack(HumanGeneticist.GENETICIST_GOGGLES.get()),
            EquipmentSlot.CHEST,
            new ItemStack(HumanGeneticist.GENETICIST_LAB_COAT.get()),
            EquipmentSlot.LEGS,
            new ItemStack(HumanGeneticist.GENETICIST_COAT_TAILS.get()),
            EquipmentSlot.MAINHAND,
            new ItemStack(HumanItems.GENE_READER.get())
        );
    }

    @Override
    public MerchantOffers createOffers(ColonistEntity colonist) {
        var offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3), new ItemStack(HumanItems.SYRINGE.get()), MAX_USES, 2, 0.05F));
        offers.add(
            new MerchantOffer(new ItemCost(Items.EMERALD, 2), new ItemStack(HumanGeneTubes.EMPTY_GENE_TUBE.get(), 8), MAX_USES, 2, 0.05F)
        );
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 8), new ItemStack(HumanItems.GENE_READER.get()), MAX_USES, 5, 0.05F));
        // [stated] Oct 29: the cleanser is pricey but not crazy - a diamond to craft; 12 emeralds from him.
        offers.add(
            new MerchantOffer(
                new ItemCost(Items.EMERALD, 12),
                new ItemStack(com.human.common.registry.init.HumanGeneticist.CLEANSER.get()),
                MAX_USES,
                5,
                0.05F
            )
        );
        return offers;
    }
}
