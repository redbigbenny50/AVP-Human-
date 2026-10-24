package com.human.client;

import com.human.client.render.block.QuarryTerminalRenderer;
import com.human.client.render.item.QuarryTerminalItemRenderer;
import com.human.common.gameplay.quarry.QuarryEdition;
import com.human.common.registry.init.HumanQuarry;
import net.minecraft.client.renderer.RenderType;

import static com.human.client.HumanClient.MOD;

/**
 * Client registrations for the quarry: the terminal renderer, its three item renderers and the frame's cutout layer.
 */
public final class HumanQuarryClient {

    private HumanQuarryClient() {}

    public static void initialize() {
        MOD.registries().registerBlockEntityRenderer(HumanQuarry.QUARRY_TERMINAL_BLOCK_ENTITY, context -> new QuarryTerminalRenderer());

        MOD.registries()
            .registerItemRenderer(HumanQuarry.QUARRY_TERMINAL_ITEM, name -> () -> new QuarryTerminalItemRenderer(QuarryEdition.BARE));
        MOD.registries()
            .registerItemRenderer(HumanQuarry.WY_QUARRY_TERMINAL_ITEM, name -> () -> new QuarryTerminalItemRenderer(QuarryEdition.WY));
        MOD.registries()
            .registerItemRenderer(HumanQuarry.USCM_QUARRY_TERMINAL_ITEM, name -> () -> new QuarryTerminalItemRenderer(QuarryEdition.USCM));

        MOD.registries().registerMenuScreen(HumanQuarry.QUARRY_TERMINAL_MENU, com.human.client.screen.QuarryTerminalScreen::new);

        // The frame's gaps are see-through. NeoForge also reads "render_type" from the model; Fabric needs this.
        MOD.registries().registerBlockRenderLayer(HumanQuarry.QUARRY_FRAME, RenderType.cutout());
        // The containment field is see-through glowing energy.
        MOD.registries().registerBlockRenderLayer(HumanQuarry.QUARRY_CONTAINMENT_FIELD, RenderType.translucent());
    }
}
