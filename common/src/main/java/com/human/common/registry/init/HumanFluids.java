package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.fluid.HumanFluidFactory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Supplier;

/**
 * ⭐ THE MOD'S FIRST FLUID. Nothing here existed before Sep 24, so if a fluid misbehaves this file and
 * {@link com.human.common.gameplay.fluid.LatexFluid} are where to look, not some older working example.
 * <p>
 * A fluid is registered as a PAIR - a source and a flowing variant - and each must be able to name the other, which is
 * why they are separate holders rather than one object. {@code initialize()} has to run before
 * {@link com.human.common.registry.init.block.CoreBlocks} registers the liquid block, because the block constructor
 * takes the source fluid.
 * </p>
 */
public class HumanFluids {

    public static final BLibRegistry<Fluid> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.FLUID);

    /**
     * ⚠ Typed as plain {@link Fluid}, NOT as {@code LatexFluid.Source}, because on NeoForge the registered object is a
     * SUBCLASS supplied by {@link HumanFluidFactory}. Anything needing the flowing-fluid API casts to
     * {@link net.minecraft.world.level.material.FlowingFluid}, which both loaders' classes satisfy.
     */
    public static final BLibHolder<Fluid> LATEX = create("latex", HumanFluidFactory::createLatexSource);

    public static final BLibHolder<Fluid> FLOWING_LATEX = create(
        "flowing_latex",
        HumanFluidFactory::createLatexFlowing
    );

    /** [stated] Sep 29: honey, built the same way as latex. The press makes it; the freezer and time harden it. */
    public static final BLibHolder<Fluid> HONEY = create("honey", HumanFluidFactory::createHoneySource);

    public static final BLibHolder<Fluid> FLOWING_HONEY = create("flowing_honey", HumanFluidFactory::createHoneyFlowing);

    /** [stated] Oct 1: cryofluid, the cryotube fuel. The press makes it from cryogel. */
    public static final BLibHolder<Fluid> CRYOFLUID = create("cryofluid", HumanFluidFactory::createCryofluidSource);

    public static final BLibHolder<Fluid> FLOWING_CRYOFLUID = create(
        "flowing_cryofluid",
        HumanFluidFactory::createCryofluidFlowing
    );

    private static <T extends Fluid> BLibHolder<T> create(String path, Supplier<T> fluidSupplier) {
        return REGISTRY.createHolder(path, fluidSupplier);
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
