package com.human.fabric.client;

import com.human.client.fluid.FluidClientInfo;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric's answer to "how does this fluid look": the Transfer API's GUI rendering helper, which covers every mod's
 * fluid and gives water its default blue when there is no biome to ask.
 */
public final class FabricFluidClientInfo implements FluidClientInfo {

    private FabricFluidClientInfo() {}

    public static void install() {
        FluidClientInfo.Holder.install(new FabricFluidClientInfo());
    }

    @Override
    public @Nullable ResourceLocation stillTexture(Fluid fluid) {
        var sprite = FluidVariantRendering.getSprite(FluidVariant.of(fluid));

        return sprite == null ? null : sprite.contents().name();
    }

    @Override
    public int tint(Fluid fluid) {
        return 0xFF000000 | FluidVariantRendering.getColor(FluidVariant.of(fluid));
    }
}
