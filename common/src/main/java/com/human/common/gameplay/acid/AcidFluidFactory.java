package com.human.common.gameplay.acid;

import net.minecraft.world.level.material.Fluid;

/**
 * Loader hook for the acid fluids - the same pattern, and for the same reason, as
 * {@link com.human.common.gameplay.fluid.HumanFluidFactory}: NeoForge needs subclasses that add {@code getFluidType()},
 * which common cannot declare. The defaults are the plain common classes, which is what Fabric uses.
 * <p>
 * ⚠⚠ NeoForge must install its factory BEFORE {@code Human.initialize()} - the fluid holders only call their suppliers
 * during registration, which runs inside it.
 * </p>
 */
public final class AcidFluidFactory {

    private static Factory factory = new Factory() {};

    private AcidFluidFactory() {}

    public static void setFactory(Factory newFactory) {
        factory = newFactory == null ? new Factory() {} : newFactory;
    }

    public static Fluid source(AcidKind kind) {
        return factory.source(kind);
    }

    public static Fluid flowing(AcidKind kind) {
        return factory.flowing(kind);
    }

    public interface Factory {

        default Fluid source(AcidKind kind) {
            return new AcidFluid.Source(kind);
        }

        default Fluid flowing(AcidKind kind) {
            return new AcidFluid.Flowing(kind);
        }
    }
}
