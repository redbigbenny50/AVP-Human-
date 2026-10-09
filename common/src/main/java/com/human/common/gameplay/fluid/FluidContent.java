package com.human.common.gameplay.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * A quantity of one fluid: the unit every avp_human container speaks - canisters now, machine tanks, pipes and the
 * multiblock tank next.
 * <h2>ANY FLUID, FROM ANY MOD</h2> The fluid is a registry entry, never a hard-coded list. Every mod registers its
 * fluids in the same global registry, so a container that stores one of these can hold any of them without knowing they
 * exist.
 * <h2>UNITS</h2> Millibuckets, {@value #BUCKET} to a bucket - NeoForge's own unit. Fabric counts 81,000 per bucket; the
 * Fabric bridge converts at the loader edge, so common code never sees droplets.
 * <h2>ALWAYS THE SOURCE</h2> A flowing fluid is stored as its source, so "flowing water" and "water" are the same
 * content and merge.
 */
public record FluidContent(
    Fluid fluid,
    int amount
) {

    public static final int BUCKET = 1000;

    public static final FluidContent EMPTY = new FluidContent(Fluids.EMPTY, 0);

    public static final Codec<FluidContent> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(FluidContent::fluid),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("amount").forGetter(FluidContent::amount)
        ).apply(instance, FluidContent::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidContent> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.registry(Registries.FLUID),
        FluidContent::fluid,
        ByteBufCodecs.VAR_INT,
        FluidContent::amount,
        FluidContent::new
    );

    public FluidContent {
        fluid = sourceOf(fluid);

        // An empty fluid holds nothing, and nothing is always the empty fluid - there is exactly one "empty".
        if (fluid == Fluids.EMPTY || amount <= 0) {
            fluid = Fluids.EMPTY;
            amount = 0;
        }
    }

    public static FluidContent of(Fluid fluid, int amount) {
        return new FluidContent(fluid, amount);
    }

    public boolean isEmpty() {
        return amount <= 0;
    }

    /** Whether this holds nothing, or holds the same fluid as the given one. */
    public boolean accepts(Fluid other) {
        return isEmpty() || fluid.isSame(other);
    }

    public FluidContent withAmount(int newAmount) {
        return new FluidContent(fluid, newAmount);
    }

    public static Fluid sourceOf(Fluid fluid) {
        return fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
    }
}
