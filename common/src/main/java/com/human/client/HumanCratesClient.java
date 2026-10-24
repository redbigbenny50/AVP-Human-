package com.human.client;

import com.human.client.render.block.CrateRenderer;
import com.human.client.render.item.CrateItemRenderer;
import com.human.client.screen.CrateScreen;
import com.human.common.registry.init.HumanCrates;

import static com.human.client.HumanClient.MOD;

/** Client registrations for the storage crates: the shared block renderer, an item renderer per crate, the screen. */
public final class HumanCratesClient {

    private HumanCratesClient() {}

    public static void initialize() {
        MOD.registries().registerBlockEntityRenderer(HumanCrates.CRATE_BLOCK_ENTITY, context -> new CrateRenderer());

        HumanCrates.ITEM_BY_TYPE.forEach(
            (type, item) -> MOD.registries().registerItemRenderer(item, name -> () -> new CrateItemRenderer(type))
        );

        MOD.registries().registerMenuScreen(HumanCrates.CRATE_MENU, CrateScreen::new);
    }
}
