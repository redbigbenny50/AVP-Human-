package com.human.client;

import com.alien.client.render.entity.head.EntityHeadDataCache;
import com.alien.client.render.entity.parasite.attachment.ParasiteHeadAttachmentOffsetDataCache;
import com.blib.api.client.mod.v1.BLibClientMod;
import com.blib.api.common.registry.v1.BLibHolder;
import com.human.Human;
import com.human.client.input.keybind.HumanKeybindingRegistry;
import com.human.client.render.armor.ApeArmorRenderer;
import com.human.client.render.armor.MK50ArmorRenderer;
import com.human.client.render.armor.PressureArmorRenderer;
import com.human.client.render.armor.TacticalArmorRenderer;
import com.human.client.render.armor.TacticalCamoArmorRenderer;
import com.human.client.render.armor.WYCommandoArmorRenderer;
import com.human.client.render.armor.WYEliteArmorRenderer;
import com.human.client.render.block.DeskTerminalRenderer;
import com.human.client.render.block.ResonatorRenderer;
import com.human.client.render.block.SolarPanelRenderer;
import com.human.client.render.entity.FlamethrowRenderer;
import com.human.client.render.entity.MarineDogRenderer;
import com.human.client.render.entity.MarineRenderer;
import com.human.client.render.entity.MushroomCloudRenderer;
import com.human.client.render.entity.NukeRenderer;
import com.human.client.render.entity.RocketRenderer;
import com.human.client.render.entity.SentryTurretRenderer;
import com.human.client.render.item.DeskTerminalItemRenderer;
import com.human.client.render.item.ResonatorItemRenderer;
import com.human.client.render.item.SentryTurretItemRenderer;
import com.human.client.render.item.SimpleItemRenderer;
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
import com.human.client.screen.IndustrialFurnaceScreen;
import com.human.common.registry.init.HumanBlockEntityTypes;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.HumanEntityTypes;
import com.human.common.registry.init.HumanMenuTypes;
import com.human.common.registry.init.block.CoreBlocks;
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
        MOD.initialize(HumanClient::runInitialization);
    }

    private static void runInitialization() {
        registerArmorRenderers();
        registerBlockEntityRenderers();
        registerBlockRenderLayers();
        registerEntityRenderers();
        registerItemColors();
        registerItemRenderers();
        registerMenuScreens();

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
    }

    private static void registerItemColors() {
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

    private static void registerItemColor(BLibHolder<ArmorItem> holder) {
        MOD.registries()
            .registerItemColor(
                (itemStack, i) -> i > 0 ? -1 : DyedItemColor.getOrDefault(itemStack, -1),
                List.of(holder)
            );
    }

    private static void registerBlockRenderLayers() {
        MOD.registries().registerBlockRenderLayer(HumanFerroaluminumBlocks.FERROALUMINUM_CHAIN_FENCE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanFerroaluminumBlocks.FERROALUMINUM_GRATE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanSteelBlocks.STEEL_BARS, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanSteelBlocks.STEEL_CHAIN_FENCE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanSteelBlocks.STEEL_GRATE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanTitaniumBlocks.TITANIUM_CHAIN_FENCE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanTitaniumBlocks.TITANIUM_GRATE, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanBlocks.RAZOR_WIRE, RenderType.cutout());
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
        MOD.registries().registerBlockRenderLayer(HumanTitaniumBlocks.TITANIUM_GRATE_SLAB, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanTitaniumBlocks.TITANIUM_GRATE_STAIRS, RenderType.cutout());

        MOD.registries().registerBlockRenderLayer(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_SLAB, RenderType.cutout());
        MOD.registries().registerBlockRenderLayer(HumanIndustrialGlassBlocks.INDUSTRIAL_GLASS_STAIRS, RenderType.cutout());
        HumanIndustrialGlassBlocks.DYE_COLOR_TO_INDUSTRIAL_GLASS.values()
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
    }

    private static void registerEntityRenderers() {
        MOD.registries().registerEntityRenderer(HumanEntityTypes.FLAMETHROW, FlamethrowRenderer::new);
        MOD.registries().registerEntityRenderer(HumanEntityTypes.GRENADE_THROWN, ThrownItemRenderer::new);
        MOD.registries().registerEntityRenderer(HumanEntityTypes.MARINE, MarineRenderer::new);
        MOD.registries().registerEntityRenderer(HumanEntityTypes.MARINE_DOG, MarineDogRenderer::new);
        MOD.registries().registerEntityRenderer(HumanEntityTypes.MUSHROOM_CLOUD, MushroomCloudRenderer::new);
        MOD.registries().registerEntityRenderer(HumanEntityTypes.NUKE, NukeRenderer::new);
        MOD.registries().registerEntityRenderer(HumanEntityTypes.ROCKET, RocketRenderer::new);
        MOD.registries().registerEntityRenderer(HumanEntityTypes.SENTRY_TURRET, SentryTurretRenderer::new);
    }

    private static void registerItemRenderers() {
        MOD.registries().registerItemRenderer(HumanItems.ARMOR_CASE, name -> () -> new SimpleItemRenderer(name));
        MOD.registries().registerItemRenderer(HumanBlockItems.DESK_TERMINAL_BLOCK, name -> DeskTerminalItemRenderer::new);
        MOD.registries().registerItemRenderer(HumanGunItems.F903WE_RIFLE, name -> () -> new F903WEItemRenderer(name));
        MOD.registries()
            .registerItemRenderer(
                HumanGunItems.FLAMETHROWER_SEVASTOPOL,
                name -> () -> new SevastopolFlamethrowerItemRenderer(name)
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
        MOD.registries().registerMenuScreen(HumanMenuTypes.INDUSTRIAL_FURNACE_MENU, IndustrialFurnaceScreen::new);
    }

    private static void registerParasiteHeadAttachmentOffsetData() {
        ParasiteHeadAttachmentOffsetDataCache.put(HumanEntityTypes.MARINE, HumanParasiteAttachmentOffsetData.MARINE);
    }
}
