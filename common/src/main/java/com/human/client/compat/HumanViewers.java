package com.human.client.compat;

import com.blib.api.client.viewer.v1.BLibViewers;
import com.blib.api.client.viewer.v1.ViewerCategory;
import com.blib.api.client.viewer.v1.ViewerPage;
import com.blib.api.client.viewer.v1.ViewerSlot;
import com.human.Human;
import com.human.common.data.CommissaryTradeLevels;
import com.human.common.gameplay.entity.living.human.colonist.ColonistProfessions;
import com.human.common.gameplay.item.GunItem;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.item.HumanGunItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.List;

/**
 * avp_human's pages on BLib's shared viewer tabs (JEI, REI and EMI alike). avp_human's machine tabs stay in its own
 * plugins; these are the rest of the recipe-viewer audit.
 * <ul>
 * <li>Phase 1 - Mob Drops and Block Drops, read from the loot tables (the ores keep their block with Silk Touch).</li>
 * <li>Phase 2 - <b>Trades</b>: the Commissary villager's five levels, and every colonist profession's trades (the
 * Electrician, and the dropship mod's Navigator when it is installed); and <b>Unloading Guns</b>: a loaded gun alone in
 * a crafting grid comes back empty - a special recipe no viewer can show by itself.</li>
 * <li>Phase 3 - <b>World Interactions</b>: what the nuke turns blocks into at its crater rim (read from the nuke
 * conversion table, so a data pack's own rules show too), and the latex tap filling a cauldron from a jungle tree.</li>
 * <li>Phase 4 - <b>Information</b> pages for the items nothing else explains: dog tags (from marines), and the liquid
 * buckets (filled from a source block).</li>
 * </ul>
 */
public final class HumanViewers {

    public static final ResourceLocation TRADES = ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "trades");

    public static final ResourceLocation UNLOADING = ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "gun_unloading");

    private static final String[] LEVELS = { "Novice", "Apprentice", "Journeyman", "Expert", "Master" };

    private HumanViewers() {}

    public static void register() {
        BLibViewers.registerLootDrops(Human.MOD_ID, Human.class);

        BLibViewers.registerCategory(
            new ViewerCategory(TRADES, Component.literal("Trades"), () -> new ItemStack(Items.EMERALD), HumanViewers::tradeWorkstations)
        );
        BLibViewers.registerPages(HumanViewers::trades);

        BLibViewers.registerCategory(
            new ViewerCategory(
                UNLOADING,
                Component.literal("Unloading Guns"),
                () -> new ItemStack(HumanGunItems.M41A_PULSE_RIFLE.get()),
                () -> List.of(new ItemStack(Items.CRAFTING_TABLE))
            )
        );
        BLibViewers.registerPages(HumanViewers::unloading);

        BLibViewers.worldInteractions();
        BLibViewers.registerPages(HumanViewers::worldInteractions);

        BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "dog_tag"),
                () -> items("dog_tag"),
                List.of(Component.literal("Carried by USCM marines and tactical marines on patrol - dropped when they die."))
            )
        );
        BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "liquid_buckets"),
                () -> items(
                    "latex_bucket",
                    "honey_bucket",
                    "cryofluid_bucket",
                    "xeno_acid_bucket",
                    "nether_xeno_acid_bucket",
                    "irradiated_xeno_acid_bucket"
                ),
                List.of(Component.literal("Use an empty bucket on a source block of the liquid to fill it."))
            )
        );
        // Oct 24 - the Create converters (NeoForge with Create only; with no Create these pages find no items and are
        // skipped). [stated] Oct 24 - recipe viewers are part of every build.
        BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "alternator"),
                () -> items("steel_alternator", "wy_alternator", "uscm_alternator"),
                List.of(
                    Component.literal("Turns Create rotation into power: 1.5 FE/t for every RPM - 384 FE/t at 256 RPM."),
                    Component.literal("Shafts on the back, left or right. Power out of the top and bottom."),
                    Component.literal(
                        "Loads Create with 64 SU per RPM. Speed cards: 1.5x output and load per card. Efficiency cards: 25% less load each. A redstone card can switch it."
                    ),
                    Component.literal("Rates can be changed in avp_human.properties.")
                )
            )
        );
        BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "electric_gearbox"),
                () -> items("steel_electric_gearbox", "wy_electric_gearbox", "uscm_electric_gearbox"),
                List.of(
                    Component.literal("Turns power into Create rotation: 2 FE/t for every RPM, never less than 8 - 512 FE/t at 256 RPM."),
                    Component.literal("Shaft out of the back. Power in through the left, right, top and bottom."),
                    Component.literal("Set its speed (up to 256 RPM, either way) with the value box on its front. Supplies 64 SU per RPM."),
                    Component.literal("Rates can be changed in avp_human.properties.")
                )
            )
        );
    }

    /** avp_human items by name, skipping any that are not registered - nothing here may fail on a missing item. */
    private static List<ItemStack> items(String... names) {
        var stacks = new ArrayList<ItemStack>();
        for (var name : names) {
            BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, name))
                .filter(item -> item != Items.AIR)
                .ifPresent(item -> stacks.add(new ItemStack(item)));
        }
        return stacks;
    }

    private static List<ViewerPage> worldInteractions() {
        var pages = new ArrayList<ViewerPage>(
            com.blib.api.client.viewer.v1.ConversionViewerPages.pages(
                Human.class,
                "/data/avp_human/avp_human/nuke_conversions/default.json",
                Component.literal("Nuclear blast: crater rim"),
                Component.literal("Anything not listed becomes basalt").withStyle(ChatFormatting.DARK_GRAY),
                new ItemStack(com.human.common.registry.init.HumanBlocks.NUKE_BLOCK.get())
            )
        );

        // The latex tap: on a real jungle tree, over a cauldron.
        pages.add(
            new ViewerPage(
                BLibViewers.WORLD_INTERACTIONS,
                List.of(
                    ViewerSlot.of(new ItemStack(com.human.common.registry.init.block.CoreBlocks.LATEX_TAP.get()))
                        .withTooltip(Component.literal("On a living jungle tree (logs + leaves)").withStyle(ChatFormatting.GOLD)),
                    ViewerSlot.of(new ItemStack(Items.JUNGLE_LOG)),
                    ViewerSlot.of(new ItemStack(Items.CAULDRON))
                        .withTooltip(Component.literal("Up to 8 blocks below the tap").withStyle(ChatFormatting.GOLD))
                ),
                List.of(ViewerSlot.fluid(com.human.common.registry.init.HumanFluids.LATEX.get(), 1000L)),
                List.of(
                    Component.literal("Latex tap"),
                    Component.literal("A level every 2 min - a full cauldron in 6").withStyle(ChatFormatting.DARK_GRAY)
                ),
                3 * com.human.common.gameplay.block.entity.LatexTapBlockEntity.TICKS_PER_DRIP
            )
        );
        return pages;
    }

    /** The Commissary's job site, and every colonist profession's workstation. */
    private static List<ItemStack> tradeWorkstations() {
        var stations = new ArrayList<ItemStack>();
        stations.add(new ItemStack(HumanBlocks.BLUEPRINT_BLOCK.get()));
        for (var profession : ColonistProfessions.all()) {
            for (var block : BuiltInRegistries.BLOCK) {
                try {
                    if (block.asItem() != Items.AIR && profession.isWorkstation(block.defaultBlockState())) {
                        stations.add(new ItemStack(block));
                    }
                } catch (RuntimeException ignored) {
                    // A profession's test that throws on some other mod's block just does not list that block.
                }
            }
        }
        return stations;
    }

    private static List<ViewerPage> trades() {
        var pages = new ArrayList<ViewerPage>();
        var random = RandomSource.create(0L);

        List<List<VillagerTrades.ItemListing>> levels = List.of(
            CommissaryTradeLevels.LEVEL_1,
            CommissaryTradeLevels.LEVEL_2,
            CommissaryTradeLevels.LEVEL_3,
            CommissaryTradeLevels.LEVEL_4,
            CommissaryTradeLevels.LEVEL_5
        );
        for (int level = 0; level < levels.size(); level++) {
            var heading = Component.literal("Commissary (villager)");
            var detail = Component.literal("Level " + (level + 1) + ": " + LEVELS[level]).withStyle(ChatFormatting.DARK_GRAY);
            for (var listing : levels.get(level)) {
                try {
                    var offer = listing.getOffer(null, random);
                    if (offer != null) {
                        pages.add(page(offer, heading, detail));
                    }
                } catch (RuntimeException ignored) {
                    // A trade that needs a real villager to build (a map, an enchantment) is left out.
                }
            }
        }

        for (var profession : ColonistProfessions.all()) {
            try {
                var offers = profession.createOffers(null);
                var heading = Component.literal("Colonist: ").append(profession.title());
                var detail = Component.literal("At its workstation").withStyle(ChatFormatting.DARK_GRAY);
                for (var offer : offers) {
                    pages.add(page(offer, heading, detail));
                }
            } catch (RuntimeException ignored) {
                // A profession that needs a real colonist to build its trades shows none here.
            }
        }
        return pages;
    }

    private static ViewerPage page(MerchantOffer offer, Component heading, Component detail) {
        var inputs = new ArrayList<ViewerSlot>();
        inputs.add(ViewerSlot.of(offer.getBaseCostA()));
        var costB = offer.getCostB();
        if (!costB.isEmpty()) {
            inputs.add(ViewerSlot.of(costB));
        }
        var uses = Component.literal("Up to " + offer.getMaxUses() + " trades before a restock").withStyle(ChatFormatting.GOLD);
        return new ViewerPage(TRADES, inputs, List.of(ViewerSlot.of(offer.getResult()).withTooltip(uses)), List.of(heading, detail), 0);
    }

    /** Every gun, loaded on the left and empty on the right, cycling in step. */
    private static List<ViewerPage> unloading() {
        var loaded = new ArrayList<ItemStack>();
        var empty = new ArrayList<ItemStack>();
        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof GunItem) {
                var full = new ItemStack(item);
                full.set(HumanDataComponents.AMMUNITION.get(), 1);
                var unloaded = new ItemStack(item);
                unloaded.set(HumanDataComponents.AMMUNITION.get(), 0);
                loaded.add(full);
                empty.add(unloaded);
            }
        }
        if (loaded.isEmpty()) {
            return List.of();
        }
        return List.of(
            new ViewerPage(
                UNLOADING,
                List.of(ViewerSlot.of(loaded).withTooltip(Component.literal("Alone in a crafting grid").withStyle(ChatFormatting.GOLD))),
                List.of(ViewerSlot.of(empty).withTooltip(Component.literal("The rounds are not returned").withStyle(ChatFormatting.RED))),
                List.of(
                    Component.literal("A loaded gun alone in a crafting grid"),
                    Component.literal("comes back empty - the rounds are lost").withStyle(ChatFormatting.DARK_GRAY)
                ),
                0
            )
        );
    }
}
