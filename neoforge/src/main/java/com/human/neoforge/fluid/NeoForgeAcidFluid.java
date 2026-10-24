package com.human.neoforge.fluid;

import com.human.Human;
import com.human.common.gameplay.acid.AcidFluid;
import com.human.common.gameplay.acid.AcidFluidFactory;
import com.human.common.gameplay.acid.AcidKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The NeoForge half of the three xeno acids: the {@code FluidType} NeoForge demands for each, its client textures, and
 * the subclasses carrying {@code getFluidType()} - exactly the pattern of {@link NeoForgeLatexFluid}.
 * <h2>The properties</h2> [stated] Oct 17: they flow like WATER, so they take water's own density and viscosity, and
 * you swim in them like water. They do not put fires out - nether acid starts them.
 * <p>
 * ⚠⚠ {@link #installFactory()} MUST be called before {@code Human.initialize()} - see {@link AcidFluidFactory}.
 * </p>
 */
public final class NeoForgeAcidFluid {

    private static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(
        NeoForgeRegistries.Keys.FLUID_TYPES,
        Human.MOD_ID
    );

    private static final Map<AcidKind, Supplier<FluidType>> TYPES = new EnumMap<>(AcidKind.class);

    static {
        for (var kind : AcidKind.values()) {
            TYPES.put(kind, FLUID_TYPES.register(kind.id(), () -> createType(kind)));
        }
    }

    private NeoForgeAcidFluid() {}

    private static FluidType createType(AcidKind kind) {
        var still = ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "block/" + kind.id() + "_still");
        var flowing = ResourceLocation.fromNamespaceAndPath(Human.MOD_ID, "block/" + kind.id() + "_flow");

        return new FluidType(
            FluidType.Properties.create()
                .density(1000)
                .viscosity(1000)
                .canDrown(true)
                .canSwim(true)
                .canExtinguish(false)
                .supportsBoating(false)
                // Same reason as latex: falls must land the same as on Fabric, which has no such modifier.
                .fallDistanceModifier(1.0F)
        ) {

            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {

                    @Override
                    public ResourceLocation getStillTexture() {
                        return still;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return flowing;
                    }

                    @Override
                    public int getTintColor() {
                        // The textures are already the acid's colour.
                        return 0xFFFFFFFF;
                    }
                });
            }
        };
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
    }

    public static void installFactory() {
        AcidFluidFactory.setFactory(new AcidFluidFactory.Factory() {

            @Override
            public Fluid source(AcidKind kind) {
                return new Source(kind);
            }

            @Override
            public Fluid flowing(AcidKind kind) {
                return new Flowing(kind);
            }
        });
    }

    public static final class Source extends AcidFluid.Source {

        public Source(AcidKind kind) {
            super(kind);
        }

        @Override
        public FluidType getFluidType() {
            return TYPES.get(kind).get();
        }
    }

    public static final class Flowing extends AcidFluid.Flowing {

        public Flowing(AcidKind kind) {
            super(kind);
        }

        @Override
        public FluidType getFluidType() {
            return TYPES.get(kind).get();
        }
    }
}
