package com.human.client.fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * How a fluid looks, asked in each loader's own way - the client half of the fluid bridge.
 * <p>
 * NeoForge answers from the fluid type's client extensions; Fabric from the Transfer API's
 * {@code FluidVariantRendering}. Both know every mod's fluids, which is the whole point: common code asks this and
 * never names a fluid. Installed from each loader's client entry point; until then the vanilla fallback below answers
 * for water and lava only.
 * </p>
 */
public interface FluidClientInfo {

    /** The fluid's still texture, as a block-atlas name such as {@code minecraft:block/water_still}. */
    @Nullable
    ResourceLocation stillTexture(Fluid fluid);

    /** The colour the texture is tinted by, ARGB. White means "the texture is already the right colour". */
    int tint(Fluid fluid);

    FluidClientInfo VANILLA_FALLBACK = new FluidClientInfo() {

        @Override
        public @Nullable ResourceLocation stillTexture(Fluid fluid) {
            if (fluid.isSame(Fluids.WATER)) {
                return ResourceLocation.withDefaultNamespace("block/water_still");
            }

            if (fluid.isSame(Fluids.LAVA)) {
                return ResourceLocation.withDefaultNamespace("block/lava_still");
            }

            return null;
        }

        @Override
        public int tint(Fluid fluid) {
            return fluid.isSame(Fluids.WATER) ? 0xFF3F76E4 : 0xFFFFFFFF;
        }
    };

    final class Holder {

        private static FluidClientInfo current = VANILLA_FALLBACK;

        private Holder() {}

        public static FluidClientInfo get() {
            return current;
        }

        public static void install(FluidClientInfo info) {
            current = info == null ? VANILLA_FALLBACK : info;
            FluidColors.clear();
        }
    }
}
