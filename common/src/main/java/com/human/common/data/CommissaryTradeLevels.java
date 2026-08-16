package com.human.common.data;

import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.item.HumanArmorItems;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.List;

public class CommissaryTradeLevels {

    public static List<VillagerTrades.ItemListing> LEVEL_1 = List.of(
        // Rations, cheap and plentiful - the commissary's most ordinary stock, and the reliable way to get the
        // cornbread a marine dog is tamed with. Priced at one emerald because it is bread, and stocked deep (12 uses)
        // so taming a dog never comes down to waiting for a restock.
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.EMERALD, 1),
            new ItemStack(HumanItems.CORNBREAD.get(), 6),
            12,
            2,
            0.02f
        ),
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.EMERALD, 1),
            new ItemStack(HumanItems.SMALL_BULLET.get(), 8),
            4,
            7,
            0.04f
        ),
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.EMERALD, 2),
            new ItemStack(HumanItems.MEDIUM_BULLET.get(), 16),
            3,
            12,
            0.09f
        ),
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.GUNPOWDER, 16),
            new ItemStack(Items.EMERALD, 4),
            3,
            12,
            0.09f
        )
    );

    public static List<VillagerTrades.ItemListing> LEVEL_2 = List.of(
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.EMERALD, 5),
            new ItemStack(HumanArmorItems.TACTICAL_CAMO_HELMET.get(), 1),
            4,
            7,
            0.04f
        ),
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.CLAY_BALL, 12),
            new ItemStack(Items.EMERALD, 2),
            3,
            12,
            0.09f
        ),
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.EMERALD, 8),
            new ItemStack(HumanArmorItems.TACTICAL_CAMO_CHESTPLATE.get(), 1),
            3,
            12,
            0.09f
        )
    );

    public static List<VillagerTrades.ItemListing> LEVEL_3 = List.of(
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.EMERALD, 15),
            new ItemStack(HumanItems.BLUEPRINT_M88MOD4_COMBAT_PISTOL.get(), 1),
            4,
            7,
            0.04f
        ),
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.EMERALD, 10),
            new ItemStack(HumanArmorItems.TACTICAL_CAMO_LEGGINGS.get(), 1),
            3,
            12,
            0.09f
        ),
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(HumanItems.STEEL_INGOT.get(), 8),
            new ItemStack(Items.EMERALD, 8),
            3,
            12,
            0.09f
        )
    );

    public static List<VillagerTrades.ItemListing> LEVEL_4 = List.of(
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.EMERALD, 8),
            new ItemStack(HumanArmorItems.TACTICAL_CAMO_BOOTS.get(), 1),
            4,
            7,
            0.04f
        ),
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.EMERALD, 10),
            new ItemStack(HumanItems.BLUEPRINT_M4RA_BATTLE_RIFLE.get(), 1),
            3,
            12,
            0.09f
        ),
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(HumanItems.BRASS_INGOT.get(), 12),
            new ItemStack(Items.EMERALD, 10),
            3,
            12,
            0.09f
        )
    );

    public static List<VillagerTrades.ItemListing> LEVEL_5 = List.of(
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.EMERALD, 25),
            new ItemStack(HumanBlocks.SENTRY_TURRET.get(), 1),
            4,
            7,
            0.04f
        ),
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.EMERALD, 10),
            new ItemStack(HumanItems.GRENADE.get(), 3),
            3,
            12,
            0.09f
        ),
        (entity, randomSource) -> new MerchantOffer(
            new ItemCost(Items.EMERALD, 50),
            new ItemStack(HumanItems.BLUEPRINT_M6B_ROCKET_LAUNCHER.get(), 1),
            3,
            12,
            0.09f
        )
    );
}
