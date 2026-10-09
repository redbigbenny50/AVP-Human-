package com.human.common.gameplay.fluid;

import net.minecraft.world.level.material.Fluid;

import java.util.function.Supplier;

/**
 * Loader hook for fluids. ⭐ THIS INDIRECTION IS NOT OPTIONAL AND IT IS NOT OVER-ENGINEERING.
 * <p>
 * NeoForge requires every modded fluid to override {@code getFluidType()}, returning a {@code FluidType} registered to
 * a NeoForge-only registry. That method DOES NOT EXIST on the vanilla API that common and Fabric compile against, so
 * common physically cannot declare it. The way out is for common to ask this factory for its fluid classes, and for the
 * NeoForge entrypoint to install subclasses that add the override.
 * </p>
 * <p>
 * ⚠⚠ ORDERING. {@code BLibRegistry.createHolder} stores the supplier and only CALLS it during {@code registerAll()},
 * which runs inside {@code Human.initialize()}. So NeoForge must install its factory BEFORE it calls
 * {@code Human.initialize()}. Install it after, and the plain common fluid is registered instead, the {@code FluidType}
 * is never attached, and latex is invisible and physics-less on NeoForge while working perfectly on Fabric - a
 * difference that will not show up on the loader you happen to test.
 * </p>
 * Fabric needs no subclass at all: it takes the defaults here, and its client registers a render handler instead.
 * <p>
 * This is the pattern proven in the avp_prometheus black-goo fluid, which runs on both loaders.
 * </p>
 */
public final class HumanFluidFactory {

    private static Factory factory = new Factory() {};

    private HumanFluidFactory() {}

    public static void setFactory(Factory newFactory) {
        factory = newFactory == null ? new Factory() {} : newFactory;
    }

    public static Fluid createLatexSource() {
        return factory.createLatexSource().get();
    }

    public static Fluid createLatexFlowing() {
        return factory.createLatexFlowing().get();
    }

    public static Fluid createHoneySource() {
        return factory.createHoneySource().get();
    }

    public static Fluid createHoneyFlowing() {
        return factory.createHoneyFlowing().get();
    }

    public static Fluid createCryofluidSource() {
        return factory.createCryofluidSource().get();
    }

    public static Fluid createCryofluidFlowing() {
        return factory.createCryofluidFlowing().get();
    }

    /** The defaults are the plain common classes, which is exactly what Fabric wants. */
    public interface Factory {

        default Supplier<Fluid> createLatexSource() {
            return LatexFluid.Source::new;
        }

        default Supplier<Fluid> createLatexFlowing() {
            return LatexFluid.Flowing::new;
        }

        default Supplier<Fluid> createHoneySource() {
            return HoneyFluid.Source::new;
        }

        default Supplier<Fluid> createHoneyFlowing() {
            return HoneyFluid.Flowing::new;
        }

        default Supplier<Fluid> createCryofluidSource() {
            return CryofluidFluid.Source::new;
        }

        default Supplier<Fluid> createCryofluidFlowing() {
            return CryofluidFluid.Flowing::new;
        }
    }
}
