package com.human.client;

import com.alien.client.render.entity.head.EntityHeadDataCache;
import com.alien.client.render.entity.parasite.attachment.ParasiteHeadAttachmentOffsetDataCache;
import com.blib.api.client.mod.v1.BLibClientMod;
import com.blib.api.common.registry.v1.BLibHolder;
import com.human.Human;
import com.human.client.input.keybind.HumanKeybindingRegistry;
import com.human.client.machine.MachineClientHooks;
import com.human.client.render.armor.ApeArmorRenderer;
import com.human.client.render.armor.MK50ArmorRenderer;
import com.human.client.render.armor.PressureArmorRenderer;
import com.human.client.render.armor.TacticalArmorRenderer;
import com.human.client.render.armor.TacticalCamoArmorRenderer;
import com.human.client.render.armor.WYCommandoArmorRenderer;
import com.human.client.render.armor.WYEliteArmorRenderer;
import com.human.client.render.block.AerokineticGeneratorRenderer;
import com.human.client.render.block.ChargingHubRenderer;
import com.human.client.render.block.DeskTerminalRenderer;
import com.human.client.render.block.ElectricMachineRenderer;
import com.human.client.render.block.HydroelectricGeneratorRenderer;
import com.human.client.render.block.ResonatorRenderer;
import com.human.client.render.block.SolarPanelRenderer;
import com.human.client.render.block.ThermoelectricGeneratorRenderer;
import com.human.client.render.entity.FlamethrowRenderer;
import com.human.client.render.entity.MarineDogRenderer;
import com.human.client.render.entity.MarineRenderer;
import com.human.client.render.entity.MushroomCloudRenderer;
import com.human.client.render.entity.NukeRenderer;
import com.human.client.render.entity.RocketRenderer;
import com.human.client.render.entity.SentryTurretRenderer;
import com.human.client.render.item.AerokineticGeneratorItemRenderer;
import com.human.client.render.item.ChargingHubItemRenderer;
import com.human.client.render.item.DeskTerminalItemRenderer;
import com.human.client.render.item.HydroelectricGeneratorItemRenderer;
import com.human.client.render.item.ResonatorItemRenderer;
import com.human.client.render.item.SentryTurretItemRenderer;
import com.human.client.render.item.SimpleItemRenderer;
import com.human.client.render.item.SolarPanelItemRenderer;
import com.human.client.render.item.ThermoelectricGeneratorItemRenderer;
import com.human.client.render.item.gun.SevastopolFlamethrowerItemRenderer;
import com.human.client.render.item.gun.muzzled.impl.F903WEItemRenderer;
import com.human.client.render.item.gun.muzzled.impl.M3712ShotgunItemRenderer;
import com.human.client.render.item.gun.muzzled.impl.M41APulseRifleItemRenderer;
import com.human.client.render.item.gun.muzzled.impl.M42A3SniperRifleItemRenderer;
import com.human.client.render.item.gun.muzzled.impl.M4RABattleRifleItemRenderer;
import com.human.client.render.item.gun.muzzled.impl.M56SmartgunItemRenderer;
import com.human.client.render.item.gun.muzzled.impl.M6BRocketLauncherItemRenderer;
import com.human.client.render.item.gun.muzzled.impl.M88Mod4CombatPistolItemRenderer;
import com.human.client.render.item.gun.muzzled.impl.OldPainlessItemRenderer;
import com.human.client.render.item.gun.muzzled.impl.ZX76ShotgunItemRenderer;
import com.human.client.screen.ArmorCaseScreen;
import com.human.client.screen.ElectricMachineScreen;
import com.human.client.screen.IndustrialFurnaceScreen;
import com.human.client.screen.MarineInventoryScreen;
import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.registry.init.HumanBlockEntityTypes;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.HumanEntityTypes;
import com.human.common.registry.init.HumanMenuTypes;
import com.human.common.registry.init.block.CoreBlocks;
import com.human.common.registry.init.block.HumanCompositeBlocks;
import com.human.common.registry.init.block.HumanFerroaluminumBlocks;
import com.human.common.registry.init.block.HumanIndustrialGlassBlocks;
import com.human.common.registry.init.block.HumanPlasticBlocks;
import com.human.common.registry.init.block.HumanSteelBlocks;
import com.human.common.registry.init.block.HumanTitaniumBlocks;
import com.human.common.registry.init.item.HumanArmorItems;
import com.human.common.registry.init.item.HumanBlockItems;
import com.human.common.registry.init.item.HumanGunItems;
import com.human.common.registry.init.item.HumanItems;
import com.human.compatibility.avp_alien.AVPAlien;
import com.human.compatibility.avp_alien.HumanEntityHeadData;
import com.human.compatibility.avp_alien.HumanParasiteAttachmentOffsetData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.List;

public class HumanClient {

    public static final BLibClientMod MOD = BLibClientMod.createFor(Human.MOD);

    public static void initialize() {
        // The wiring tool shows a resonator's sphere through this hook (see ResonatorSphereHook).
        com.human.common.gameplay.block.resonator.ResonatorSphereHook.show =
            com.human.client.render.ResonatorSphereDisplay::show;

        MOD.initialize(HumanClient::runInitialization);
    }

    private static void runInitialization() {
        registerArmorRenderers();
        registerBlockEntityRenderers();
        registerBlockRenderLayers();
        // [stated] Oct 18 - the vapor puff (Gasworks-style gas), for the chem thrower, its cloud and the distributor.
        MOD.registries()
            .registerParticleProviderFactory(
                com.human.common.registry.init.HumanParticles.VAPOR,
                com.human.client.particle.VaporParticle.Provider::new
            );
        registerEntityRenderers();
        registerItemColors();
        registerItemRenderers();
        registerMenuScreens();
        HumanCratesClient.initialize();
        HumanQuarryClient.initialize();
        MOD.registries()
            .registerMenuScreen(
                com.human.common.registry.init.HumanFabrication.BLUEPRINT_TABLE,
                com.human.client.screen.BlueprintTableScreen::new
            );
        MOD.registries()
            .registerMenuScreen(
                com.human.common.registry.init.HumanFabrication.DRIVE_BAY_MENU,
                com.human.client.screen.DriveBayScreen::new
            );
        // [stated] Oct 23 - the metal signs, drawn by vanilla's own sign renderers under our two block entity types.
        MOD.registries()
            .registerBlockEntityRenderer(
                com.human.common.registry.init.HumanSigns.SIGN_BLOCK_ENTITY,
                net.minecraft.client.renderer.blockentity.SignRenderer::new
            );
        MOD.registries()
            .registerBlockEntityRenderer(
                com.human.common.registry.init.HumanSigns.HANGING_SIGN_BLOCK_ENTITY,
                net.minecraft.client.renderer.blockentity.HangingSignRenderer::new
            );
        // ⚠ The sign texture sheet's lookup tables are filled once, from WoodType.values(), the first time vanilla's
        // Sheets class loads. Our sign types register during mod setup, which is normally earlier - but if anything
        // touched Sheets first, ours would have no texture and the renderer would fail on them. Filled in here if
        // missing; a no-op when they are already there.
        for (var set : com.human.common.registry.init.HumanSigns.all()) {
            var woodType = set.woodType();
            net.minecraft.client.renderer.Sheets.SIGN_MATERIALS.putIfAbsent(
                woodType,
                new net.minecraft.client.resources.model.Material(
                    net.minecraft.client.renderer.Sheets.SIGN_SHEET,
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("entity/signs/" + woodType.name())
                )
            );
            net.minecraft.client.renderer.Sheets.HANGING_SIGN_MATERIALS.putIfAbsent(
                woodType,
                new net.minecraft.client.resources.model.Material(
                    net.minecraft.client.renderer.Sheets.SIGN_SHEET,
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("entity/signs/hanging/" + woodType.name())
                )
            );
        }
        MOD.registries()
            .registerBlockEntityRenderer(
                com.human.common.registry.init.HumanFabrication.DRIVE_BAY_BLOCK_ENTITY,
                com.human.client.render.block.DriveBayRenderer::new
            );
        // [stated] Oct 20 - the Creative Fluid Tank's window shows the fluid it is set to.
        MOD.registries()
            .registerBlockEntityRenderer(
                com.human.common.registry.init.HumanCreativeBlocks.CREATIVE_FLUID_TANK_BLOCK_ENTITY,
                com.human.client.render.block.CreativeFluidTankRenderer::new
            );
        MOD.registries()
            .registerMenuScreen(
                com.human.common.registry.init.HumanFabrication.FABRICATOR_MENU,
                com.human.client.screen.FabricatorScreen::new
            );
        MOD.registries()
            .registerBlockEntityRenderer(
                com.human.common.registry.init.HumanFabrication.FABRICATOR_BLOCK_ENTITY,
                context -> new com.human.client.render.block.FabricatorRenderer()
            );
        MOD.registries()
            .registerItemRenderer(
                com.human.common.registry.init.HumanFabrication.FABRICATOR_ITEM,
                name -> () -> new com.human.client.render.item.FabricatorItemRenderer("fabricator")
            );
        MOD.registries()
            .registerItemRenderer(
                com.human.common.registry.init.HumanFabrication.WY_FABRICATOR_ITEM,
                name -> () -> new com.human.client.render.item.FabricatorItemRenderer("wy_fabricator")
            );
        MOD.registries()
            .registerItemRenderer(
                com.human.common.registry.init.HumanFabrication.USCM_FABRICATOR_ITEM,
                name -> () -> new com.human.client.render.item.FabricatorItemRenderer("uscm_fabricator")
            );

        // [stated] Oct 17 - the vaporizer: its geo model (with the real liquid behind the windows), the shared machine
        // screen on its own art, and the model in the hand for all three editions.
        MOD.registries()
            .registerBlockEntityRenderer(
                com.human.common.registry.init.HumanVaporizer.VAPORIZER_BLOCK_ENTITY,
                context -> new com.human.client.render.block.VaporizerRenderer()
            );
        MOD.registries()
            .registerMenuScreen(
                com.human.common.registry.init.HumanVaporizer.VAPORIZER_MENU,
                com.human.client.screen.ElectricMachineScreen::new
            );
        MOD.registries()
            .registerItemRenderer(
                com.human.common.registry.init.HumanVaporizer.VAPORIZER_ITEM,
                name -> () -> new com.human.client.render.item.VaporizerItemRenderer("vaporizer")
            );
        MOD.registries()
            .registerItemRenderer(
                com.human.common.registry.init.HumanVaporizer.WY_VAPORIZER_ITEM,
                name -> () -> new com.human.client.render.item.VaporizerItemRenderer("wy_vaporizer")
            );
        MOD.registries()
            .registerItemRenderer(
                com.human.common.registry.init.HumanVaporizer.USCM_VAPORIZER_ITEM,
                name -> () -> new com.human.client.render.item.VaporizerItemRenderer("uscm_vaporizer")
            );

        // [stated] Oct 17 - the distributor: the shared machine renderer (its fan strip and LED) and its own screen.
        MOD.registries()
            .registerBlockEntityRenderer(
                com.human.common.registry.init.HumanDistributor.DISTRIBUTOR_BLOCK_ENTITY,
                ElectricMachineRenderer::new
            );
        MOD.registries()
            .registerMenuScreen(
                com.human.common.registry.init.HumanDistributor.DISTRIBUTOR_MENU,
                com.human.client.screen.DistributorScreen::new
            );

        // The machines' client tick - animation clock and working sound. Handed in from here so common code never
        // names a client class.
        ElectricMachineBlockEntity.clientTicker = MachineClientHooks::tick;

        HumanKeybindingRegistry.initialize();

        if (AVPAlien.MOD.isLoaded()) {
            MOD.events().onClientSetup().register(() -> {
                registerEntityHeadData();
                registerParasiteHeadAttachmentOffsetData();
            });
        }
    }

    private static void registerArmorRenderers() {
        MOD.registries()
            .registerArmorRenderer(
                ApeArmorRenderer::new,
                List.of(
                    HumanArmorItems.WY_APE_HELMET,
                    HumanArmorItems.WY_APE_CHESTPLATE,
                    HumanArmorItems.WY_APE_LEGGINGS,
                    HumanArmorItems.WY_APE_BOOTS
                )
            );
        MOD.registries()
            .registerArmorRenderer(
                com.human.client.render.armor.ElectricianArmorRenderer::new,
                List.of(HumanArmorItems.ELECTRICIAN_HARD_HAT, HumanArmorItems.ELECTRICIAN_JACKET)
            );
        MOD.registries()
            .registerArmorRenderer(
                MK50ArmorRenderer::new,
                List.of(
                    HumanArmorItems.MK50_HELMET,
                    HumanArmorItems.MK50_CHESTPLATE,
                    HumanArmorItems.MK50_LEGGINGS,
                    HumanArmorItems.MK50_BOOTS
                )
            );
        MOD.registries()
            .registerArmorRenderer(
                PressureArmorRenderer::new,
                List.of(
                    HumanArmorItems.PRESSURE_HELMET,
                    HumanArmorItems.PRESSURE_CHESTPLATE,
                    HumanArmorItems.PRESSURE_LEGGINGS,
                    HumanArmorItems.PRESSURE_BOOTS
                )
            );
        MOD.registries()
            .registerArmorRenderer(
                TacticalArmorRenderer::new,
                List.of(
                    HumanArmorItems.TACTICAL_HELMET,
                    HumanArmorItems.TACTICAL_CHESTPLATE,
                    HumanArmorItems.TACTICAL_LEGGINGS,
                    HumanArmorItems.TACTICAL_BOOTS
                )
            );
        MOD.registries()
            .registerArmorRenderer(
                TacticalCamoArmorRenderer::new,
                List.of(
                    HumanArmorItems.TACTICAL_CAMO_HELMET,
                    HumanArmorItems.TACTICAL_CAMO_CHESTPLATE,
                    HumanArmorItems.TACTICAL_CAMO_LEGGINGS,
                    HumanArmorItems.TACTICAL_CAMO_BOOTS
                )
            );
        MOD.registries()
            .registerArmorRenderer(
                WYCommandoArmorRenderer::new,
                List.of(
                    HumanArmorItems.WY_COMMANDO_HELMET,
                    HumanArmorItems.WY_COMMANDO_CHESTPLATE,
                    HumanArmorItems.WY_COMMANDO_LEGGINGS,
                    HumanArmorItems.WY_COMMANDO_BOOTS
                )
            );
        MOD.registries()
            .registerArmorRenderer(
                WYEliteArmorRenderer::new,
                List.of(
                    HumanArmorItems.WY_ELITE_HELMET,
                    HumanArmorItems.WY_ELITE_CHESTPLATE,
                    HumanArmorItems.WY_ELITE_LEGGINGS,
                    HumanArmorItems.WY_ELITE_BOOTS
                )
            );
    }

    private static void registerBlockEntityRenderers() {
        MOD.registries().registerBlockEntityRenderer(HumanBlockEntityTypes.AMMO_CHEST, ChestRenderer::new);
        MOD.registries()
            .registerBlockEntityRenderer(
                HumanBlockEntityTypes.CRYOTANK,
                com.human.client.render.block.CryotankRenderer::new
            );
        MOD.registries()
            .registerBlockEntityRenderer(
                HumanBlockEntityTypes.DESK_TERMINAL,
                (BlockEntityRendererProvider.Context rendererDispatcherIn) -> new DeskTerminalRenderer()
            );
        MOD.registries().registerBlockEntityRenderer(HumanBlockEntityTypes.LEAD_CHEST, ChestRenderer::new);
        MOD.registries()
            .registerBlockEntityRenderer(
                HumanBlockEntityTypes.RESONATOR,
                (BlockEntityRendererProvider.Context rendererDispatcherIn) -> new ResonatorRenderer()
            );
        MOD.registries()
            .registerBlockEntityRenderer(
                HumanBlockEntityTypes.SOLAR_PANEL,
                (BlockEntityRendererProvider.Context rendererDispatcherIn) -> new SolarPanelRenderer()
            );
        MOD.registries()
            .registerBlockEntityRenderer(
                HumanBlockEntityTypes.HYDROELECTRIC_GENERATOR,
                (BlockEntityRendererProvider.Context rendererDispatcherIn) -> new HydroelectricGeneratorRenderer()
            );
        MOD.registries()
            .registerBlockEntityRenderer(
                HumanBlockEntityTypes.CHARGING_HUB,
                (BlockEntityRendererProvider.Context rendererDispatcherIn) -> new ChargingHubRenderer()
            );
        MOD.registries()
            .registerBlockEntityRenderer(
                HumanBlockEntityTypes.CABLE,
                com.human.client.render.block.CableFlangeRenderer::new
            );
        MOD.registries()
            .registerBlockEntityRenderer(
                HumanBlockEntityTypes.FLUID_PIPE,
                context -> new com.human.client.render.block.PipeFlangeRenderer<>("block/fluid_pipe_flange")
            );
        MOD.registries()
            .registerBlockEntityRenderer(
                HumanBlockEntityTypes.ITEM_PIPE,
                context -> new com.human.client.render.block.PipeFlangeRenderer<>("block/item_pipe")
            );
        MOD.registries()
            .registerBlockEntityRenderer(
                HumanBlockEntityTypes.FLUID_TANK,
                (BlockEntityRendererProvider.Context context) -> new com.human.client.render.block.FluidTankRenderer()
            );
        MOD.registries()
            .registerBlockEntityRenderer(
                HumanBlockEntityTypes.CANISTER,
                (BlockEntityRendererProvider.Context context) -> new com.human.client.render.block.CanisterBlockRenderer()
            );
        // The new battery is a plain baked block and has no renderer. The machines draw their working front and LED.
        MOD.registries().registerBlockEntityRenderer(HumanBlockEntityTypes.ELECTRIC_FURNACE, ElectricMachineRenderer::new);
        MOD.registries().registerBlockEntityRenderer(HumanBlockEntityTypes.ELECTRIC_SMOKER, ElectricMachineRenderer::new);
        MOD.registries().registerBlockEntityRenderer(HumanBlockEntityTypes.ELECTRIC_BLAST_FURNACE, ElectricMachineRenderer::new);
        MOD.registries()
            .registerBlockEntityRenderer(HumanBlockEntityTypes.ELECTRIC_INDUSTRIAL_FURNACE, ElectricMachineRenderer::new);
        MOD.registries()
            .registerBlockEntityRenderer(HumanBlockEntityTypes.ELECTRIC_GRINDER, ElectricMachineRenderer::new);
        MOD.registries()
            .registerBlockEntityRenderer(HumanBlockEntityTypes.ELECTRIC_PRESS, ElectricMachineRenderer::new);
        MOD.registries()
            .registerBlockEntityRenderer(HumanBlockEntityTypes.ELECTRIC_HYDRATOR, ElectricMachineRenderer::new);
        MOD.registries()
            .registerBlockEntityRenderer(HumanBlockEntityTypes.ELECTRIC_FREEZER, ElectricMachineRenderer::new);
        MOD.registries()
            .registerBlockEntityRenderer(
                HumanBlockEntityTypes.AEROKINETIC_GENERATOR,
                (BlockEntityRendererProvider.Context rendererDispatcherIn) -> new AerokineticGeneratorRenderer()
            );
        MOD.registries()
            .registerBlockEntityRenderer(
                HumanBlockEntityTypes.THERMOELECTRIC_GENERATOR,
                (BlockEntityRendererProvider.Context rendererDispatcherIn) -> new ThermoelectricGeneratorRenderer()
            );
    }

    private static void registerItemColors() {
        registerCanisterColor();

        // [stated] Oct 2: a Skin Kit's label takes its skin's colour.
        MOD.registries()
            .registerItemColor(
                (itemStack, layer) -> layer == 1
                    ? com.human.client.skin.SentrySkins.colour(com.human.common.gameplay.item.SkinKitItem.skinOf(itemStack))
                    : -1,
                List.of(com.human.common.registry.init.item.HumanItems.SKIN_KIT)
            );

        registerItemColor(HumanArmorItems.MK50_HELMET);
        registerItemColor(HumanArmorItems.MK50_CHESTPLATE);
        registerItemColor(HumanArmorItems.MK50_LEGGINGS);
        registerItemColor(HumanArmorItems.MK50_BOOTS);

        registerItemColor(HumanArmorItems.PRESSURE_HELMET);
        registerItemColor(HumanArmorItems.PRESSURE_CHESTPLATE);
        registerItemColor(HumanArmorItems.PRESSURE_LEGGINGS);
        registerItemColor(HumanArmorItems.PRESSURE_BOOTS);

        registerItemColor(HumanArmorItems.WY_COMMANDO_HELMET);
        registerItemColor(HumanArmorItems.WY_COMMANDO_CHESTPLATE);
        registerItemColor(HumanArmorItems.WY_COMMANDO_LEGGINGS);
        registerItemColor(HumanArmorItems.WY_COMMANDO_BOOTS);

        registerItemColor(HumanArmorItems.WY_ELITE_HELMET);
        registerItemColor(HumanArmorItems.WY_ELITE_CHESTPLATE);
        registerItemColor(HumanArmorItems.WY_ELITE_LEGGINGS);
        registerItemColor(HumanArmorItems.WY_ELITE_BOOTS);
    }

    /**
     * The universal canister's window: layer 1 is a greyscale window tinted by whatever fluid is inside, so every fluid
     * from every mod shows its own colour with no art of its own. Layer 0, the canister body, is left untinted.
     */
    private static void registerCanisterColor() {
        MOD.registries()
            .registerItemColor(
                (itemStack, layer) -> {
                    if (layer != 1) {
                        return -1;
                    }

                    var content = com.human.common.gameplay.item.canister.UniversalCanisterItem.getContent(itemStack);

                    return content.isEmpty() ? -1 : com.human.client.fluid.FluidColors.colorOf(content.fluid());
                },
                List.of(com.human.common.registry.init.item.HumanItems.CANISTER)
            );
    }

    private static void registerItemColor(BLibHolder<ArmorItem> holder) {
        MOD.registries()
            .registerItemColor(
                (itemStack, i) -> i > 0 ? -1 : DyedItemColor.getOrDefault(itemStack, -1),
                List.of(holder)
            );
    }

    private static void registerBlockRenderLayers() {
        // USCM set: the cage mesh has see-through holes; the window glass is tinted, so it draws translucent.
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.block.HumanUscmBlocks.CAGE_MESH, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.block.HumanUscmBlocks.WINDOW, RenderType.translucent());
        // W-Y set: its window glass is tinted too.
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.block.HumanWyBlocks.WINDOW, RenderType.translucent());
        // Fluid tanks: the window glass is clear with a translucent glint, so the whole tank draws translucent.
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.HumanBlocks.STEEL_FLUID_TANK, RenderType.translucent());
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.HumanBlocks.FLUID_PIPE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.HumanBlocks.ITEM_PIPE, RenderType.cutout());
        MOD.registries()
            .registerBlockRenderLayer(com.human.common.registry.init.HumanBlocks.FERROALUMINUM_FLUID_TANK, RenderType.translucent());
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.HumanBlocks.ALUMINUM_FLUID_TANK, RenderType.translucent());
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.HumanBlocks.TITANIUM_FLUID_TANK, RenderType.translucent());
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.HumanBlocks.WY_FLUID_TANK, RenderType.translucent());
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.HumanBlocks.USCM_FLUID_TANK, RenderType.translucent());
        // Cryotanks: frame and glass, so the frozen specimen shows through.
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.HumanBlocks.CRYOTANK, RenderType.translucent());

        MOD.registries().registerBlockRenderLayer(HumanFerroaluminumBlocks.FERROALUMINUM_CHAIN_FENCE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanFerroaluminumBlocks.FERROALUMINUM_GRATE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanSteelBlocks.STEEL_BARS, RenderType.cutout());
        // Oct 21 - metal bars, chains and ladders: see-through, like their vanilla counterparts.
        com.human.common.registry.init.HumanMetalFixtures.bars()
            .forEach(block -> MOD.registries().registerBlockRenderLayer(block, RenderType.cutout()));
        com.human.common.registry.init.HumanMetalFixtures.chains()
            .forEach(block -> MOD.registries().registerBlockRenderLayer(block, RenderType.cutout()));
        com.human.common.registry.init.HumanMetalFixtures.ladders()
            .forEach(block -> MOD.registries().registerBlockRenderLayer(block, RenderType.cutout()));
        MOD.registries().registerBlockRenderLayer(HumanSteelBlocks.STEEL_CHAIN_FENCE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanSteelBlocks.STEEL_GRATE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanTitaniumBlocks.TITANIUM_CHAIN_FENCE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanTitaniumBlocks.TITANIUM_GRATE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanBlocks.RAZOR_WIRE, RenderType.cutout());
        // ⚠ CUTOUT, NOT SOLID: the battery gauge and the machines' LED are overlay faces with transparent pixels. On
        // the
        // default solid layer every transparent pixel draws BLACK and hides the face underneath.
        MOD.registries().registerBlockRenderLayer(HumanBlocks.BATTERY, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanBlocks.ELECTRIC_FURNACE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanBlocks.ELECTRIC_SMOKER, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanBlocks.ELECTRIC_BLAST_FURNACE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanBlocks.ELECTRIC_INDUSTRIAL_FURNACE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanBlocks.ELECTRIC_GRINDER, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanBlocks.ELECTRIC_PRESS, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanBlocks.ELECTRIC_HYDRATOR, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanBlocks.ELECTRIC_FREEZER, RenderType.cutout());
        // ⚠ Oct 19 (his report: "the front of the distributor when powered turns black"). The distributor's three
        // editions have the same LED overlay face as every other machine but were never put on the cutout layer, so
        // the overlay's transparent pixels drew BLACK over the whole front the moment it was powered.
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.HumanDistributor.DISTRIBUTOR, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.HumanDistributor.WY_DISTRIBUTOR, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(com.human.common.registry.init.HumanDistributor.USCM_DISTRIBUTOR, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_DOOR, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_TRAP_DOOR, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanFerroaluminumBlocks.FERROALUMINUM_DOOR, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanSteelBlocks.STEEL_DOOR, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanTitaniumBlocks.TITANIUM_DOOR, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanFerroaluminumBlocks.FERROALUMINUM_TRAP_DOOR, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(CoreBlocks.TRINITITE_BLOCK, RenderType.translucent());
        MOD.registries().registerBlockRenderLayer(HumanFerroaluminumBlocks.FERROALUMINUM_GRATE_SLAB, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanFerroaluminumBlocks.FERROALUMINUM_GRATE_STAIRS, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanSteelBlocks.STEEL_GRATE_SLAB, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanSteelBlocks.STEEL_GRATE_STAIRS, RenderType.cutout());

        MOD.registries().registerBlockRenderLayer(HumanCompositeBlocks.COMPOSITE_CHAIN_FENCE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanCompositeBlocks.COMPOSITE_GRATE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanCompositeBlocks.COMPOSITE_DOOR, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanCompositeBlocks.COMPOSITE_GRATE_SLAB, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanCompositeBlocks.COMPOSITE_GRATE_STAIRS, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanTitaniumBlocks.TITANIUM_GRATE_SLAB, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanTitaniumBlocks.TITANIUM_GRATE_STAIRS, RenderType.cutout());

        MOD.registries().registerBlockRenderLayer(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_SLAB, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_STAIRS, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_WALL, RenderType.cutout());
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS.values()
            .forEach(blockSupplier -> MOD.registries().registerBlockRenderLayer(blockSupplier, RenderType.translucent()));
        // Coloured glass is translucent where the uncoloured is cutout, so every coloured derivative has to be
        // registered too - missing one renders it fully opaque with nothing logged.
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_STAIRS.values()
            .forEach(blockSupplier -> MOD.registries().registerBlockRenderLayer(blockSupplier, RenderType.translucent()));
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_SLAB.values()
            .forEach(blockSupplier -> MOD.registries().registerBlockRenderLayer(blockSupplier, RenderType.translucent()));
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_DOOR.values()
            .forEach(blockSupplier -> MOD.registries().registerBlockRenderLayer(blockSupplier, RenderType.translucent()));
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_TRAP_DOOR.values()
            .forEach(blockSupplier -> MOD.registries().registerBlockRenderLayer(blockSupplier, RenderType.translucent()));
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_WALL.values()
            .forEach(blockSupplier -> MOD.registries().registerBlockRenderLayer(blockSupplier, RenderType.translucent()));
        MOD.registries().registerBlockRenderLayer(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_PANE, RenderType.cutout());
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS_PANE.values()
            .forEach(blockSupplier -> MOD.registries().registerBlockRenderLayer(blockSupplier, RenderType.translucent()));
        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE.values()
            .forEach(blockSupplier -> MOD.registries().registerBlockRenderLayer(blockSupplier, RenderType.cutout()));
        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_SLAB.values()
            .forEach(blockSupplier -> MOD.registries().registerBlockRenderLayer(blockSupplier, RenderType.cutout()));
        HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC_GRATE_STAIRS.values()
            .forEach(blockSupplier -> MOD.registries().registerBlockRenderLayer(blockSupplier, RenderType.cutout()));
    }

    private static void registerEntityHeadData() {
        EntityHeadDataCache.put(HumanEntityTypes.MARINE, HumanEntityHeadData.MARINE);
        // [stated] Oct 13 - the soldier types share the marine's proportions.
        EntityHeadDataCache.put(HumanEntityTypes.TACTICAL_MARINE, HumanEntityHeadData.MARINE);
        EntityHeadDataCache.put(HumanEntityTypes.WY_COMMANDO, HumanEntityHeadData.MARINE);
        EntityHeadDataCache.put(HumanEntityTypes.WY_ELITE, HumanEntityHeadData.MARINE);
        EntityHeadDataCache.put(HumanEntityTypes.WY_SPEC_OPS_COMMANDO, HumanEntityHeadData.MARINE);
        EntityHeadDataCache.put(HumanEntityTypes.WY_SPEC_OPS_ELITE, HumanEntityHeadData.MARINE);
        EntityHeadDataCache.put(HumanEntityTypes.WY_APE_TROOPER, HumanEntityHeadData.MARINE);
    }

    private static void registerEntityRenderers() {
        MOD.registries().registerEntityRenderer(HumanEntityTypes.FLAMETHROW, FlamethrowRenderer::new);
        // The chem thrower's lingering cloud draws nothing itself - its particles come from its own client tick.
        MOD.registries().registerEntityRenderer(HumanEntityTypes.CHEM_CLOUD, net.minecraft.client.renderer.entity.NoopRenderer::new);
        MOD.registries().registerEntityRenderer(HumanEntityTypes.GRENADE_THROWN, ThrownItemRenderer::new);
        MOD.registries().registerEntityRenderer(HumanEntityTypes.MARINE, MarineRenderer::new);
        // [stated] Oct 13 - the tactical marine keeps the marine body; the Weyland-Yutani units wear the employee body.
        MOD.registries().registerEntityRenderer(HumanEntityTypes.TACTICAL_MARINE, MarineRenderer::new);
        MOD.registries()
            .registerEntityRenderer(HumanEntityTypes.WY_COMMANDO, com.human.client.render.entity.WeylandYutaniSoldierRenderer::new);
        MOD.registries()
            .registerEntityRenderer(HumanEntityTypes.WY_ELITE, com.human.client.render.entity.WeylandYutaniSoldierRenderer::new);
        MOD.registries()
            .registerEntityRenderer(
                HumanEntityTypes.WY_SPEC_OPS_COMMANDO,
                com.human.client.render.entity.WeylandYutaniSoldierRenderer::new
            );
        MOD.registries()
            .registerEntityRenderer(HumanEntityTypes.WY_SPEC_OPS_ELITE, com.human.client.render.entity.WeylandYutaniSoldierRenderer::new);
        MOD.registries()
            .registerEntityRenderer(HumanEntityTypes.WY_APE_TROOPER, com.human.client.render.entity.WeylandYutaniSoldierRenderer::new);
        com.human.common.gameplay.entity.living.human.soldier.WeylandYutaniEmployeeFeatureManager.installOutfitCatalog(
            com.human.client.render.entity.WeylandYutaniEmployeeOutfits::forSex
        );
        MOD.registries().registerEntityRenderer(HumanEntityTypes.COLONIST, com.human.client.render.entity.ColonistRenderer::new);
        // [stated] Sep 30: colonists pick a random jumpsuit from every outfit texture present - listed here, on the
        // client.
        com.human.common.gameplay.entity.living.human.colonist.ColonistFeatureManager.installOutfitCatalog(
            com.human.client.render.entity.ColonistOutfits::forSex
        );
        MOD.registries().registerEntityRenderer(HumanEntityTypes.MARINE_DOG, MarineDogRenderer::new);
        MOD.registries().registerEntityRenderer(HumanEntityTypes.MUSHROOM_CLOUD, MushroomCloudRenderer::new);
        MOD.registries().registerEntityRenderer(HumanEntityTypes.NUKE, NukeRenderer::new);
        MOD.registries().registerEntityRenderer(HumanEntityTypes.ROCKET, RocketRenderer::new);
        MOD.registries().registerEntityRenderer(HumanEntityTypes.SENTRY_TURRET, SentryTurretRenderer::new);
    }

    private static void registerItemRenderers() {
        MOD.registries().registerItemRenderer(HumanItems.ARMOR_CASE, name -> () -> new SimpleItemRenderer(name));
        MOD.registries().registerItemRenderer(HumanBlockItems.DESK_TERMINAL_BLOCK, name -> DeskTerminalItemRenderer::new);
        MOD.registries().registerItemRenderer(HumanBlockItems.CHARGING_HUB, name -> ChargingHubItemRenderer::new);
        MOD.registries().registerItemRenderer(HumanBlockItems.SOLAR_PANEL, name -> SolarPanelItemRenderer::new);
        MOD.registries()
            .registerItemRenderer(HumanBlockItems.HYDROELECTRIC_GENERATOR, name -> HydroelectricGeneratorItemRenderer::new);
        MOD.registries()
            .registerItemRenderer(HumanBlockItems.AEROKINETIC_GENERATOR, name -> AerokineticGeneratorItemRenderer::new);
        MOD.registries()
            .registerItemRenderer(
                HumanBlockItems.THERMOELECTRIC_GENERATOR,
                name -> ThermoelectricGeneratorItemRenderer::new
            );
        MOD.registries().registerItemRenderer(HumanGunItems.F903WE_RIFLE, name -> () -> new F903WEItemRenderer(name));
        MOD.registries()
            .registerItemRenderer(
                HumanGunItems.FLAMETHROWER_SEVASTOPOL,
                name -> () -> new SevastopolFlamethrowerItemRenderer(name)
            );
        MOD.registries()
            .registerItemRenderer(
                HumanGunItems.CHEM_THROWER,
                name -> () -> new com.human.client.render.item.gun.ChemThrowerItemRenderer(name)
            );
        MOD.registries().registerItemRenderer(HumanGunItems.M37_12_SHOTGUN, name -> () -> new M3712ShotgunItemRenderer(name));
        MOD.registries()
            .registerItemRenderer(
                HumanGunItems.M41A_PULSE_RIFLE,
                name -> () -> new M41APulseRifleItemRenderer(name)
            );
        MOD.registries()
            .registerItemRenderer(
                HumanGunItems.M42A3_SNIPER_RIFLE,
                name -> () -> new M42A3SniperRifleItemRenderer(name)
            );
        MOD.registries()
            .registerItemRenderer(
                HumanGunItems.M4RA_BATTLE_RIFLE,
                name -> () -> new M4RABattleRifleItemRenderer(name)
            );
        MOD.registries().registerItemRenderer(HumanGunItems.M56_SMARTGUN, name -> () -> new M56SmartgunItemRenderer(name));
        MOD.registries()
            .registerItemRenderer(
                HumanGunItems.M6B_ROCKET_LAUNCHER,
                name -> () -> new M6BRocketLauncherItemRenderer(name)
            );
        MOD.registries()
            .registerItemRenderer(
                HumanGunItems.M88MOD4_COMBAT_PISTOL,
                name -> () -> new M88Mod4CombatPistolItemRenderer(name)
            );
        MOD.registries().registerItemRenderer(HumanGunItems.OLD_PAINLESS, name -> () -> new OldPainlessItemRenderer(name));
        MOD.registries().registerItemRenderer(HumanBlockItems.RESONATOR_BLOCK, name -> ResonatorItemRenderer::new);
        MOD.registries().registerItemRenderer(HumanBlockItems.SENTRY_TURRET, name -> SentryTurretItemRenderer::new);
        MOD.registries().registerItemRenderer(HumanGunItems.ZX_76_SHOTGUN, name -> () -> new ZX76ShotgunItemRenderer(name));
    }

    private static void registerMenuScreens() {
        MOD.registries().registerMenuScreen(HumanMenuTypes.ARMOR_CASE, ArmorCaseScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.BATTERY, com.human.client.screen.BatteryScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.INDUSTRIAL_FURNACE_MENU, IndustrialFurnaceScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.MARINE_INVENTORY, MarineInventoryScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.ELECTRIC_FURNACE, ElectricMachineScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.ELECTRIC_SMOKER, ElectricMachineScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.ELECTRIC_BLAST_FURNACE, ElectricMachineScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.ELECTRIC_INDUSTRIAL_FURNACE, ElectricMachineScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.ELECTRIC_GRINDER, ElectricMachineScreen::new);
        MOD.registries()
            .registerMenuScreen(HumanMenuTypes.ELECTRONICS_WORKSTATION, com.human.client.screen.ElectronicsWorkstationScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.FUSE_PANEL, com.human.client.screen.FusePanelScreen::new);
        MOD.registries()
            .registerMenuScreen(
                HumanMenuTypes.CRYOTUBE_CONTROLLER,
                com.human.client.screen.CryotubeControllerScreen::new
            );
        MOD.registries().registerMenuScreen(HumanMenuTypes.SENTRY_TURRET, com.human.client.screen.SentryTurretScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.DESK_TERMINAL, com.human.client.screen.DeskTerminalScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.CABLE_PORTS, com.human.client.screen.CablePortScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.PIPE_END, com.human.client.screen.PipeEndScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.ELECTRIC_PRESS, ElectricMachineScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.ELECTRIC_HYDRATOR, ElectricMachineScreen::new);
        MOD.registries().registerMenuScreen(HumanMenuTypes.ELECTRIC_FREEZER, ElectricMachineScreen::new);
    }

    private static void registerParasiteHeadAttachmentOffsetData() {
        ParasiteHeadAttachmentOffsetDataCache.put(HumanEntityTypes.MARINE, HumanParasiteAttachmentOffsetData.MARINE);
        ParasiteHeadAttachmentOffsetDataCache.put(HumanEntityTypes.TACTICAL_MARINE, HumanParasiteAttachmentOffsetData.MARINE);
        ParasiteHeadAttachmentOffsetDataCache.put(HumanEntityTypes.WY_COMMANDO, HumanParasiteAttachmentOffsetData.MARINE);
        ParasiteHeadAttachmentOffsetDataCache.put(HumanEntityTypes.WY_ELITE, HumanParasiteAttachmentOffsetData.MARINE);
        ParasiteHeadAttachmentOffsetDataCache.put(HumanEntityTypes.WY_SPEC_OPS_COMMANDO, HumanParasiteAttachmentOffsetData.MARINE);
        ParasiteHeadAttachmentOffsetDataCache.put(HumanEntityTypes.WY_SPEC_OPS_ELITE, HumanParasiteAttachmentOffsetData.MARINE);
        ParasiteHeadAttachmentOffsetDataCache.put(HumanEntityTypes.WY_APE_TROOPER, HumanParasiteAttachmentOffsetData.MARINE);
    }
}
