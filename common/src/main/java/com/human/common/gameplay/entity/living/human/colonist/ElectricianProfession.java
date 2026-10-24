package com.human.common.gameplay.entity.living.human.colonist;

import com.human.HumanResources;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.item.HumanArmorItems;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.function.Supplier;

/**
 * avp_human's own colonist job, worked at the ELECTRONICS WORKSTATION.
 * <p>
 * [stated] Sep 30: "sells bulk electronics, buys further tree crafted electronics or raw materials like redstone, gold,
 * silicon". Normal clothes, plus an electrician's jacket and hard hat.
 * </p>
 * <h2>PRICES</h2> Emeralds, as vanilla's villagers use. Every item he both sells and buys back is bought for LESS than
 * he sells it, so no loop between the two ever makes emeralds.
 */
public final class ElectricianProfession implements ColonistProfession {

    public static final ElectricianProfession INSTANCE = new ElectricianProfession();

    private static final ResourceLocation ID = HumanResources.location("electrician");

    /** Trades before a restock - a vanilla villager's usual. */
    private static final int MAX_USES = 16;

    private ElectricianProfession() {}

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public boolean isWorkstation(BlockState state) {
        return state.is(HumanBlocks.ELECTRONICS_WORKSTATION.get());
    }

    @Override
    public Component title() {
        return Component.translatable("entity.avp_human.colonist.electrician");
    }

    @Override
    public Map<EquipmentSlot, ItemStack> outfit() {
        return Map.of(
            EquipmentSlot.HEAD,
            new ItemStack(HumanArmorItems.ELECTRICIAN_HARD_HAT.get()),
            EquipmentSlot.CHEST,
            new ItemStack(HumanArmorItems.ELECTRICIAN_JACKET.get())
        );
    }

    @Override
    public MerchantOffers createOffers(ColonistEntity colonist) {
        var offers = new MerchantOffers();

        // SELLS - components in bulk.
        sell(offers, 2, HumanItems.RESISTOR, 16);
        sell(offers, 2, HumanItems.DIODE, 16);
        sell(offers, 3, HumanItems.TRANSISTOR, 8);
        sell(offers, 4, HumanItems.LED, 8);
        sell(offers, 4, HumanItems.CAPACITOR, 4);
        sell(offers, 4, HumanItems.REGULATOR, 4);
        sell(offers, 8, HumanItems.INTEGRATED_CIRCUIT, 2);
        sell(offers, 10, HumanItems.SERVO, 4);
        sell(offers, 12, HumanItems.LED_DISPLAY, 1);
        sell(offers, 16, HumanItems.CPU, 1);
        // [stated] Oct 2: the computer parts - priced by what goes into them.
        sell(offers, 14, HumanItems.RAM, 1);
        sell(offers, 20, HumanItems.MOTHERBOARD, 1);
        sell(offers, 32, HumanItems.SSD, 1);
        // [stated] Oct 13: the W-Y faction license - every W-Y fabricator print - for 32 diamonds.
        offers.add(
            new MerchantOffer(
                new ItemCost(Items.DIAMOND, 32),
                new ItemStack(com.human.common.registry.init.HumanFabrication.WY_FACTION_LICENSE.get()),
                MAX_USES,
                30,
                0.0F
            )
        );

        // BUYS - raw materials...
        buy(offers, () -> Items.REDSTONE, 24, 1);
        buy(offers, () -> Items.GOLD_INGOT, 4, 1);
        buy(offers, () -> Items.COPPER_INGOT, 12, 1);
        buy(offers, HumanItems.SILICON::get, 16, 1);

        // ...and components further up the tree, for less than he sells them.
        buy(offers, HumanItems.INTEGRATED_CIRCUIT::get, 1, 2);
        buy(offers, HumanItems.LED_DISPLAY::get, 1, 4);
        buy(offers, HumanItems.CPU::get, 1, 6);
        buy(offers, HumanItems.RAM::get, 1, 5);
        buy(offers, HumanItems.MOTHERBOARD::get, 1, 7);
        buy(offers, HumanItems.SSD::get, 1, 11);
        buy(offers, HumanItems.SMART_RECEIVER::get, 1, 12);

        return offers;
    }

    private static void sell(MerchantOffers offers, int emeralds, Supplier<? extends Item> item, int count) {
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), new ItemStack(item.get(), count), MAX_USES, 2, 0.05F));
    }

    private static void buy(MerchantOffers offers, Supplier<? extends ItemLike> item, int count, int emeralds) {
        offers.add(new MerchantOffer(new ItemCost(item.get(), count), new ItemStack(Items.EMERALD, emeralds), MAX_USES, 2, 0.05F));
    }
}
