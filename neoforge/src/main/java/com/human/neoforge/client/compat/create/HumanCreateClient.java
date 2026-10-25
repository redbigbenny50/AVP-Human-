package com.human.neoforge.client.compat.create;

import com.human.neoforge.compat.create.HumanCreate;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client side of the Create converters - only ever called when Create is installed (see HumanNeoForgeClient). */
public final class HumanCreateClient {

    private HumanCreateClient() {}

    public static void register(IEventBus modBus) {
        // The partial models must exist before models bake.
        ConverterGearRenderer.init();
        modBus.addListener(HumanCreateClient::registerRenderers);
        modBus.addListener(HumanCreateClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(HumanCreate.ALTERNATOR_MENU.get(), AlternatorScreen::new);
        event.register(HumanCreate.GEARBOX_MENU.get(), ElectricGearboxScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HumanCreate.ALTERNATOR_ENTITY.get(), context -> new ConverterGearRenderer<>());
        event.registerBlockEntityRenderer(HumanCreate.GEARBOX_ENTITY.get(), context -> new ConverterGearRenderer<>());
    }
}
