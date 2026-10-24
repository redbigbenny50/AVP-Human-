package com.human.fabric.client;

import com.human.Human;
import com.human.client.HumanClient;
import com.human.common.registry.init.HumanFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.resources.ResourceLocation;

public class HumanFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HumanClient.initialize();
        FabricFluidClientInfo.install();

        // Fabric's half of liquid latex. NeoForge gets its textures from the FluidType's client extensions instead;
        // without this the fluid is registered and flows but draws as missing-texture purple.
        FluidRenderHandlerRegistry.INSTANCE.register(
            HumanFluids.LATEX.get(),
            HumanFluids.FLOWING_LATEX.get(),
            new SimpleFluidRenderHandler(
                ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "block/latex_still"),
                ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "block/latex_flow"),
                0xFFFFFFFF
            )
        );

        // Liquid honey, the same way.
        FluidRenderHandlerRegistry.INSTANCE.register(
            HumanFluids.HONEY.get(),
            HumanFluids.FLOWING_HONEY.get(),
            new SimpleFluidRenderHandler(
                ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "block/honey_still"),
                ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "block/honey_flow"),
                0xFFFFFFFF
            )
        );

        // [stated] Oct 17 - the three xeno acids, the same way. Their textures are already their own colour.
        for (var kind : com.human.common.gameplay.acid.AcidKind.values()) {
            FluidRenderHandlerRegistry.INSTANCE.register(
                com.human.common.registry.init.HumanChemicals.acid(kind),
                com.human.common.registry.init.HumanChemicals.flowingAcid(kind),
                new SimpleFluidRenderHandler(
                    ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "block/" + kind.id() + "_still"),
                    ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "block/" + kind.id() + "_flow"),
                    0xFFFFFFFF
                )
            );
        }

        // Cryofluid, the same way.
        FluidRenderHandlerRegistry.INSTANCE.register(
            HumanFluids.CRYOFLUID.get(),
            HumanFluids.FLOWING_CRYOFLUID.get(),
            new SimpleFluidRenderHandler(
                ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "block/cryofluid_still"),
                ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "block/cryofluid_flow"),
                0xFFFFFFFF
            )
        );
    }
}
