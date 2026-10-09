package com.human.neoforge.client;

import com.human.client.fluid.FluidClientInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.Nullable;

/** NeoForge's answer to "how does this fluid look": the fluid type's client extensions, which every mod's fluid has. */
public final class NeoForgeFluidClientInfo implements FluidClientInfo {

    private NeoForgeFluidClientInfo() {}

    public static void install() {
        FluidClientInfo.Holder.install(new NeoForgeFluidClientInfo());
    }

    @Override
    public @Nullable ResourceLocation stillTexture(Fluid fluid) {
        return IClientFluidTypeExtensions.of(fluid).getStillTexture();
    }

    @Override
    public int tint(Fluid fluid) {
        return IClientFluidTypeExtensions.of(fluid).getTintColor();
    }
}
