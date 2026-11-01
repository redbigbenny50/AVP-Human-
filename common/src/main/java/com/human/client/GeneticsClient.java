package com.human.client;

/**
 * Client-side registration for the genetics screens. Called from both loaders' client entry points right after
 * HumanClient.initialize(), so it runs in the same registration window without touching HumanClient.
 */
public final class GeneticsClient {

    private GeneticsClient() {}

    public static void initialize() {
        HumanClient.MOD.registries()
            .registerMenuScreen(
                com.human.common.registry.init.HumanGeneReader.MENU,
                com.human.client.screen.GeneReaderScreen::new
            );

        // [stated] Oct 29: the Geneticist Station's cleanser screen.
        HumanClient.MOD.registries()
            .registerMenuScreen(
                com.human.common.registry.init.HumanGeneticist.STATION_MENU,
                com.human.client.screen.GeneticistStationScreen::new
            );

        // [stated] Oct 29: the Gene Storage - model, lid clips and the 32 carousel vials; centred model for the items.
        HumanClient.MOD.registries()
            .registerBlockEntityRenderer(
                com.human.common.registry.init.HumanGeneStorage.BLOCK_ENTITY,
                context -> new com.human.client.render.block.GeneStorageRenderer()
            );
        for (var edition : com.human.common.gameplay.gene.machine.storage.GeneStorageBlock.Edition.values()) {
            var item = switch (edition) {
                case BARE -> com.human.common.registry.init.HumanGeneStorage.GENE_STORAGE_ITEM;
                case WY -> com.human.common.registry.init.HumanGeneStorage.WY_GENE_STORAGE_ITEM;
                case USCM -> com.human.common.registry.init.HumanGeneStorage.USCM_GENE_STORAGE_ITEM;
            };
            HumanClient.MOD.registries()
                .registerItemRenderer(item, name -> () -> new com.human.client.render.item.GeneStorageItemRenderer(edition.id));
        }

        HumanClient.MOD.registries()
            .registerMenuScreen(
                com.human.common.registry.init.HumanGeneAnalyzer.MENU,
                com.human.client.screen.GeneAnalyzerScreen::new
            );
        // [stated] Oct 29: the Gene Analyzer - model, two-track clips, the tray sample in the syringe's colour.
        HumanClient.MOD.registries()
            .registerBlockEntityRenderer(
                com.human.common.registry.init.HumanGeneAnalyzer.BLOCK_ENTITY,
                context -> new com.human.client.render.block.GeneAnalyzerRenderer()
            );
        for (var edition : com.human.common.gameplay.gene.machine.analyzer.GeneAnalyzerBlock.Edition.values()) {
            var item = switch (edition) {
                case BARE -> com.human.common.registry.init.HumanGeneAnalyzer.GENE_ANALYZER_ITEM;
                case WY -> com.human.common.registry.init.HumanGeneAnalyzer.WY_GENE_ANALYZER_ITEM;
                case USCM -> com.human.common.registry.init.HumanGeneAnalyzer.USCM_GENE_ANALYZER_ITEM;
            };
            HumanClient.MOD.registries()
                .registerItemRenderer(item, name -> () -> new com.human.client.render.item.GeneAnalyzerItemRenderer(edition.id));
        }

        // [stated] Oct 29: the Gene Separator - model, three clip tracks, screen.
        HumanClient.MOD.registries()
            .registerBlockEntityRenderer(
                com.human.common.registry.init.HumanGeneSeparator.BLOCK_ENTITY,
                context -> new com.human.client.render.block.GeneSeparatorRenderer()
            );
        for (var edition : com.human.common.gameplay.gene.machine.separator.GeneSeparatorBlock.Edition.values()) {
            var item = switch (edition) {
                case BARE -> com.human.common.registry.init.HumanGeneSeparator.GENE_SEPARATOR_ITEM;
                case WY -> com.human.common.registry.init.HumanGeneSeparator.WY_GENE_SEPARATOR_ITEM;
                case USCM -> com.human.common.registry.init.HumanGeneSeparator.USCM_GENE_SEPARATOR_ITEM;
            };
            HumanClient.MOD.registries()
                .registerItemRenderer(item, name -> () -> new com.human.client.render.item.GeneSeparatorItemRenderer(edition.id));
        }
        HumanClient.MOD.registries()
            .registerMenuScreen(
                com.human.common.registry.init.HumanGeneSeparator.MENU,
                com.human.client.screen.GeneSeparatorScreen::new
            );
        com.blib.api.client.viewer.v1.BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_human", "gene_separator"),
                () -> java.util.List.of(
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneSeparator.GENE_SEPARATOR_ITEM.get()),
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneSeparator.WY_GENE_SEPARATOR_ITEM.get()),
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneSeparator.USCM_GENE_SEPARATOR_ITEM.get())
                ),
                java.util.List.of(
                    net.minecraft.network.chat.Component.literal(
                        "A 2x2, 2-tall centrifuge. Splits a filled syringe into one filled gene tube per gene; the syringe comes back empty and loses no integrity."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "Needs one empty tube per gene in its tube bay (9 stacks; its item port fills it from pipes) and room in its 27-slot output."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "A run takes 8 seconds plus 1 per gene at 40 FE/t. Power at the back; no draw while idle."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "OUTPUT TO NETWORK sends finished tubes to Gene Storage on the same power grid, nearest first."
                    )
                )
            )
        );

        // [stated] Oct 29: the Gene Recombinator - model, run clips, gene-coloured fluid, screen.
        HumanClient.MOD.registries()
            .registerBlockEntityRenderer(
                com.human.common.registry.init.HumanGeneRecombinator.BLOCK_ENTITY,
                context -> new com.human.client.render.block.GeneRecombinatorRenderer()
            );
        for (var edition : com.human.common.gameplay.gene.machine.recombinator.GeneRecombinatorBlock.Edition.values()) {
            var item = switch (edition) {
                case BARE -> com.human.common.registry.init.HumanGeneRecombinator.GENE_RECOMBINATOR_ITEM;
                case WY -> com.human.common.registry.init.HumanGeneRecombinator.WY_GENE_RECOMBINATOR_ITEM;
                case USCM -> com.human.common.registry.init.HumanGeneRecombinator.USCM_GENE_RECOMBINATOR_ITEM;
            };
            HumanClient.MOD.registries()
                .registerItemRenderer(item, name -> () -> new com.human.client.render.item.GeneRecombinatorItemRenderer(edition.id));
        }
        HumanClient.MOD.registries()
            .registerMenuScreen(
                com.human.common.registry.init.HumanGeneRecombinator.MENU,
                com.human.client.screen.GeneRecombinatorScreen::new
            );
        com.blib.api.client.viewer.v1.BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_human", "gene_recombinator"),
                () -> java.util.List.of(
                    new net.minecraft.world.item.ItemStack(
                        com.human.common.registry.init.HumanGeneRecombinator.GENE_RECOMBINATOR_ITEM.get()
                    ),
                    new net.minecraft.world.item.ItemStack(
                        com.human.common.registry.init.HumanGeneRecombinator.WY_GENE_RECOMBINATOR_ITEM.get()
                    ),
                    new net.minecraft.world.item.ItemStack(
                        com.human.common.registry.init.HumanGeneRecombinator.USCM_GENE_RECOMBINATOR_ITEM.get()
                    )
                ),
                java.util.List.of(
                    net.minecraft.network.chat.Component.literal(
                        "A 2-wide, 2-tall concentrator. A sample tube in the cradle and a vial of the same gene in each arm: the sample gains one dose of the stronger vial, never past the gene's cap."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "Both vials come back as empty tubes. Only base and loot/harvest genes recombine; mutations and defects don't stack."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "10 seconds a run at 40 FE/t. Power at the back; no draw while idle. Right-click tubes onto the machine to load it."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "Its screen lists the tubes in Gene Storage on the same power grid; PULL FROM NETWORK fills empty arms by itself."
                    )
                )
            )
        );

        // [stated] Oct 29: the Gene Splicer - model, pipette clips, gene-coloured fluid, screen.
        HumanClient.MOD.registries()
            .registerBlockEntityRenderer(
                com.human.common.registry.init.HumanGeneSplicer.BLOCK_ENTITY,
                context -> new com.human.client.render.block.GeneSplicerRenderer()
            );
        for (var edition : com.human.common.gameplay.gene.machine.splicer.GeneSplicerBlock.Edition.values()) {
            var item = switch (edition) {
                case BARE -> com.human.common.registry.init.HumanGeneSplicer.GENE_SPLICER_ITEM;
                case WY -> com.human.common.registry.init.HumanGeneSplicer.WY_GENE_SPLICER_ITEM;
                case USCM -> com.human.common.registry.init.HumanGeneSplicer.USCM_GENE_SPLICER_ITEM;
            };
            HumanClient.MOD.registries()
                .registerItemRenderer(item, name -> () -> new com.human.client.render.item.GeneSplicerItemRenderer(edition.id));
        }
        HumanClient.MOD.registries()
            .registerMenuScreen(
                com.human.common.registry.init.HumanGeneSplicer.MENU,
                com.human.client.screen.GeneSplicerScreen::new
            );
        com.blib.api.client.viewer.v1.BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_human", "gene_splicer"),
                () -> java.util.List.of(
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneSplicer.GENE_SPLICER_ITEM.get()),
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneSplicer.WY_GENE_SPLICER_ITEM.get()),
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneSplicer.USCM_GENE_SPLICER_ITEM.get())
                ),
                java.util.List.of(
                    net.minecraft.network.chat.Component.literal(
                        "A 2-wide, 2-tall syringe assembler. Up to 5 filled gene tubes, one per gene, go into one empty syringe, labelled SPLICED or with your own label."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "A gene already in is refused (stack doses in the Recombinator). One mutation per category, with a warning; defects allowed, with a red warning."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "6 seconds plus 2 per gene at 40 FE/t, on START. Used tubes come back empty. Its item port takes empty syringes."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "Its screen lists the tubes in Gene Storage on the same power grid; click one to add it."
                    )
                )
            )
        );

        // The Geneticist's outfit.
        HumanClient.MOD.registries()
            .registerArmorRenderer(
                com.human.client.render.armor.GeneticistArmorRenderer::new,
                java.util.List.of(
                    com.human.common.registry.init.HumanGeneticist.GENETICIST_GOGGLES,
                    com.human.common.registry.init.HumanGeneticist.GENETICIST_LAB_COAT,
                    com.human.common.registry.init.HumanGeneticist.GENETICIST_COAT_TAILS
                )
            );
        // Its model has see-through gaps (glass, the rack) - cutout so they don't draw solid.
        HumanClient.MOD.registries()
            .registerBlockRenderLayer(
                com.human.common.registry.init.HumanGeneticist.GENETICIST_STATION,
                net.minecraft.client.renderer.RenderType.cutout()
            );

        // A filled gene tube's fluid (layer 1) takes its gene's colour.
        HumanClient.MOD.registries()
            .registerItemColor(
                (stack, layer) -> layer == 1 ? com.human.common.gameplay.gene.tube.GeneTubeItem.colour(stack) : -1,
                java.util.List.of(com.human.common.registry.init.HumanGeneTubes.GENE_TUBE)
            );
        // [stated] Oct 29 - a syringe's barrel (layer 1) shows the blend of its genes' colours; empty draws it clear.
        HumanClient.MOD.registries()
            .registerItemColor(
                (stack, layer) -> layer == 1 ? com.human.common.gameplay.gene.GeneColours.syringe(stack) : -1,
                java.util.List.of(com.human.common.registry.init.item.HumanItems.SYRINGE)
            );

        // [stated] Oct 30 - the syringe's page, now that players can inject themselves.
        com.blib.api.client.viewer.v1.BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_human", "syringe"),
                () -> java.util.List.of(
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.item.HumanItems.SYRINGE.get())
                ),
                java.util.List.of(
                    net.minecraft.network.chat.Component.literal(
                        "Sneak-use to switch mode. Draw a sample from a creature, then inject it into another creature."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "Use it in the air in inject mode to inject yourself: every gene in the syringe, as Gene Infusion, for 3 Minecraft days."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "One infusion at a time - a new one replaces the old. It stays through death and milk; a cleansed empty syringe used on yourself ends it."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "Infused with the Milkable gene, use an empty bucket in the air to fill it with milk."
                    )
                )
            )
        );

        // [stated] Oct 24 - recipe viewers are part of every build: info pages for the tubes (the empty tube's crafting
        // recipe shows on its own).
        com.blib.api.client.viewer.v1.BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_human", "empty_gene_tube"),
                () -> java.util.List.of(
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneTubes.EMPTY_GENE_TUBE.get())
                ),
                java.util.List.of(
                    net.minecraft.network.chat.Component.literal(
                        "Holds one gene. The Gene Separator splits a syringe sample into one filled tube per gene."
                    ),
                    net.minecraft.network.chat.Component.literal("Crafted 8 at a time from a copper ingot over glass.")
                )
            )
        );
        com.blib.api.client.viewer.v1.BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_human", "genetic_research"),
                () -> java.util.List.of(
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneticist.GENETIC_RESEARCH.get())
                ),
                java.util.List.of(
                    net.minecraft.network.chat.Component.literal(
                        "Printed by the Gene Analyzer from your discoveries: one paper and one black dye."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "Using it teaches its genes and species to whoever reads it. It is used up."
                    )
                )
            )
        );
        com.blib.api.client.viewer.v1.BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_human", "geneticist_station"),
                () -> java.util.List.of(
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneticist.GENETICIST_STATION.get())
                ),
                java.util.List.of(
                    net.minecraft.network.chat.Component.literal(
                        "The Geneticist colonist's job site. A colonist within 8 blocks of a free station takes the job."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "Use it to cleanse a syringe: a syringe and a cleanser in, a cleansed syringe out after 5 seconds."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "A cleansed syringe adds +35% integrity when injected (never past full). An empty one becomes a pure cleansing shot."
                    ),
                    net.minecraft.network.chat.Component.literal("One cleanse per syringe. No power needed.")
                )
            )
        );
        com.blib.api.client.viewer.v1.BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_human", "gene_tube"),
                () -> java.util.List.of(
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneTubes.GENE_TUBE.get())
                ),
                java.util.List.of(
                    net.minecraft.network.chat.Component.literal(
                        "One gene with all of its doses, from one donor. Made by the Gene Separator."
                    ),
                    net.minecraft.network.chat.Component.literal("Tubes with the same gene, value and donor stack to 16."),
                    net.minecraft.network.chat.Component.literal(
                        "Concentrate doses in the Recombinator; splice up to 5 genes into a syringe with the Splicer."
                    ),
                    net.minecraft.network.chat.Component.literal("Genes you haven't discovered show as ???.")
                )
            )
        );
        com.blib.api.client.viewer.v1.BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_human", "cleanser"),
                () -> java.util.List.of(
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneticist.CLEANSER.get())
                ),
                java.util.List.of(
                    net.minecraft.network.chat.Component.literal("Crafted from an empty gene tube and a diamond. Geneticists sell it too."),
                    net.minecraft.network.chat.Component.literal(
                        "Put it with a syringe in a Geneticist Station: 5 seconds later the syringe comes out cleansed."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "A cleansed syringe adds +35% integrity when injected - enough to cover even a 5-gene splice."
                    ),
                    net.minecraft.network.chat.Component.literal("An empty syringe becomes a pure cleansing shot. One cleanse per syringe.")
                )
            )
        );

        // [stated] Oct 29: the Gene Storage - screen and its viewer page (all three editions on one page).
        HumanClient.MOD.registries()
            .registerMenuScreen(
                com.human.common.registry.init.HumanGeneStorage.MENU,
                com.human.client.screen.GeneStorageScreen::new
            );
        com.blib.api.client.viewer.v1.BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_human", "gene_storage"),
                () -> java.util.List.of(
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneStorage.GENE_STORAGE_ITEM.get()),
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneStorage.WY_GENE_STORAGE_ITEM.get()),
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneStorage.USCM_GENE_STORAGE_ITEM.get())
                ),
                java.util.List.of(
                    net.minecraft.network.chat.Component.literal(
                        "A 2x2, 2-tall cryo rack holding 90 filled gene tubes. Needs power at the back; no power, no opening."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "Right-click with a tube to store it, or open it to browse, search, sort and take tubes."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "Machines on the same power grid store into it nearest first, and can draw from it."
                    ),
                    net.minecraft.network.chat.Component.literal("ISOLATED hides it from other machines. Breaking it drops what is inside.")
                )
            )
        );
        com.blib.api.client.viewer.v1.BLibViewers.registerInfo(
            new com.blib.api.client.viewer.v1.ViewerInfo(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_human", "gene_analyzer"),
                () -> java.util.List.of(
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneAnalyzer.GENE_ANALYZER_ITEM.get()),
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneAnalyzer.WY_GENE_ANALYZER_ITEM.get()),
                    new net.minecraft.world.item.ItemStack(com.human.common.registry.init.HumanGeneAnalyzer.USCM_GENE_ANALYZER_ITEM.get())
                ),
                java.util.List.of(
                    net.minecraft.network.chat.Component.literal(
                        "A 3-wide, 2-tall scanning bench. Needs power at the back; it only draws power while scanning."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "Open it and put a filled syringe in the tray. The scan takes 12 seconds and adds the sample's genes and donor to your own database."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "Browse genes, species and mutations you know: caps, donors by tier, where they spawn, what each gene helps unlock."
                    ),
                    net.minecraft.network.chat.Component.literal(
                        "PRINT shares discoveries: tick entries, add paper and black dye, and print a Genetic Research for someone else to read."
                    )
                )
            )
        );
    }
}
