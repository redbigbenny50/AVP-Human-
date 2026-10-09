package com.human.neoforge.fluid;

import com.human.Human;
import com.human.common.gameplay.fluid.CryofluidFluid;
import com.human.common.gameplay.fluid.HoneyFluid;
import com.human.common.gameplay.fluid.HumanFluidFactory;
import com.human.common.gameplay.fluid.LatexFluid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The NeoForge half of liquid latex: the {@code FluidType} NeoForge demands, its client textures, and the two
 * subclasses that carry the {@code getFluidType()} override common cannot declare. Modelled directly on the
 * avp_prometheus black-goo fluid, which is proven on both loaders.
 * <h2>The properties, and why latex is not water</h2>
 * <ul>
 * <li>density {@value #DENSITY} - heavier than water's 1000, so you sink rather than float</li>
 * <li>viscosity {@value #VISCOSITY} - thick. Together with the Slowness this is what wading through latex feels
 * like</li>
 * <li>{@code canSwim(false)} - ⚠ you CANNOT swim in it. That plus the density is the real teeth behind [stated]
 * "maximum slowness while in it"; the effect alone would still let you stroke out of a deep pool</li>
 * <li>{@code canDrown(true)} - it is a liquid and it will drown you</li>
 * <li>{@code canExtinguish(false)} - latex is not water and does not put you out</li>
 * <li>{@code supportsBoating(false)}</li>
 * </ul>
 * ⚠⚠ {@link #installFactory()} MUST be called before {@code Human.initialize()}. See {@link HumanFluidFactory}.
 */
public final class NeoForgeLatexFluid {

    public static final int DENSITY = 1400;

    public static final int VISCOSITY = 1800;

    private static final ResourceLocation STILL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Human.MOD_ID,
        "block/latex_still"
    );

    private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Human.MOD_ID,
        "block/latex_flow"
    );

    private static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(
        NeoForgeRegistries.Keys.FLUID_TYPES,
        Human.MOD_ID
    );

    private static final Supplier<FluidType> LATEX_TYPE = FLUID_TYPES.register(
        "latex",
        () -> new FluidType(
            FluidType.Properties.create()
                .density(DENSITY)
                .viscosity(VISCOSITY)
                .canDrown(true)
                .canSwim(false)
                .canExtinguish(false)
                .supportsBoating(false)
                // ⚠ 1.0, NOT THE DEFAULT 0.5. NeoForge multiplies an entity's fall distance by this EVERY TICK it is
                // inside the fluid, and Fabric has no such thing - so the default made falls into latex soft on
                // NeoForge
                // and full-damage on Fabric. Landing rules now live in FluidFallCushion and apply identically on both.
                .fallDistanceModifier(1.0F)
        ) {

            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {

                    @Override
                    public ResourceLocation getStillTexture() {
                        return STILL_TEXTURE;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return FLOWING_TEXTURE;
                    }

                    @Override
                    public int getTintColor() {
                        // The texture is already the colour latex should be; no tint.
                        return 0xFFFFFFFF;
                    }
                });
            }
        }
    );

    /** Honey: a touch heavier than latex, and runnier - thick, but it moves. */
    public static final int HONEY_DENSITY = 1420;

    public static final int HONEY_VISCOSITY = 1500;

    private static final ResourceLocation HONEY_STILL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Human.MOD_ID,
        "block/honey_still"
    );

    private static final ResourceLocation HONEY_FLOWING_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Human.MOD_ID,
        "block/honey_flow"
    );

    private static final Supplier<FluidType> HONEY_TYPE = FLUID_TYPES.register(
        "honey",
        () -> new FluidType(
            FluidType.Properties.create()
                .density(HONEY_DENSITY)
                .viscosity(HONEY_VISCOSITY)
                .canDrown(true)
                .canSwim(false)
                .canExtinguish(false)
                .supportsBoating(false)
                // Same reason as latex: FluidFallCushion owns landings on both loaders.
                .fallDistanceModifier(1.0F)
        ) {

            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {

                    @Override
                    public ResourceLocation getStillTexture() {
                        return HONEY_STILL_TEXTURE;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return HONEY_FLOWING_TEXTURE;
                    }

                    @Override
                    public int getTintColor() {
                        return 0xFFFFFFFF;
                    }
                });
            }
        }
    );

    /**
     * [stated] Oct 1: cryofluid has LAVA'S PROPERTIES - lava's own density and viscosity - but it is freezing, so it is
     * cold, gives no light and puts fires out. You cannot swim up through it, and it will drown you.
     */
    public static final int CRYOFLUID_DENSITY = 3000;

    public static final int CRYOFLUID_VISCOSITY = 6000;

    /** Kelvin. Liquid nitrogen is 77; the slush is a little warmer than that. */
    public static final int CRYOFLUID_TEMPERATURE = 90;

    private static final ResourceLocation CRYOFLUID_STILL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Human.MOD_ID,
        "block/cryofluid_still"
    );

    private static final ResourceLocation CRYOFLUID_FLOWING_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Human.MOD_ID,
        "block/cryofluid_flow"
    );

    private static final Supplier<FluidType> CRYOFLUID_TYPE = FLUID_TYPES.register(
        "cryofluid",
        () -> new FluidType(
            FluidType.Properties.create()
                .density(CRYOFLUID_DENSITY)
                .viscosity(CRYOFLUID_VISCOSITY)
                .temperature(CRYOFLUID_TEMPERATURE)
                .lightLevel(0)
                .canDrown(true)
                .canSwim(false)
                .canExtinguish(true)
                .supportsBoating(false)
                // Same reason as latex: fall rules must match Fabric, which has no such modifier.
                .fallDistanceModifier(1.0F)
        ) {

            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {

                    @Override
                    public ResourceLocation getStillTexture() {
                        return CRYOFLUID_STILL_TEXTURE;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return CRYOFLUID_FLOWING_TEXTURE;
                    }

                    @Override
                    public int getTintColor() {
                        return 0xFFFFFFFF;
                    }
                });
            }
        }
    );

    private NeoForgeLatexFluid() {}

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
    }

    public static void installFactory() {
        HumanFluidFactory.setFactory(new HumanFluidFactory.Factory() {

            @Override
            public Supplier<Fluid> createLatexSource() {
                return Source::new;
            }

            @Override
            public Supplier<Fluid> createLatexFlowing() {
                return Flowing::new;
            }

            @Override
            public Supplier<Fluid> createHoneySource() {
                return HoneySource::new;
            }

            @Override
            public Supplier<Fluid> createHoneyFlowing() {
                return HoneyFlowing::new;
            }

            @Override
            public Supplier<Fluid> createCryofluidSource() {
                return CryofluidSource::new;
            }

            @Override
            public Supplier<Fluid> createCryofluidFlowing() {
                return CryofluidFlowing::new;
            }
        });
    }

    public static final class Source extends LatexFluid.Source {

        @Override
        public FluidType getFluidType() {
            return LATEX_TYPE.get();
        }
    }

    public static final class Flowing extends LatexFluid.Flowing {

        @Override
        public FluidType getFluidType() {
            return LATEX_TYPE.get();
        }
    }

    public static final class HoneySource extends HoneyFluid.Source {

        @Override
        public FluidType getFluidType() {
            return HONEY_TYPE.get();
        }
    }

    public static final class HoneyFlowing extends HoneyFluid.Flowing {

        @Override
        public FluidType getFluidType() {
            return HONEY_TYPE.get();
        }
    }

    public static final class CryofluidSource extends CryofluidFluid.Source {

        @Override
        public FluidType getFluidType() {
            return CRYOFLUID_TYPE.get();
        }
    }

    public static final class CryofluidFlowing extends CryofluidFluid.Flowing {

        @Override
        public FluidType getFluidType() {
            return CRYOFLUID_TYPE.get();
        }
    }
}
