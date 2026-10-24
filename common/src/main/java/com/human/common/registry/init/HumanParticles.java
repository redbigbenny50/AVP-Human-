package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.vapor.VaporParticleOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/** avp_human's particle types: the vapor puff the chem thrower, its lingering cloud and the distributor draw. */
public final class HumanParticles {

    private static final BLibRegistry<ParticleType<?>> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.PARTICLE_TYPE);

    public static final BLibHolder<ParticleType<VaporParticleOptions>> VAPOR = REGISTRY.createHolder(
        "vapor",
        () -> new ParticleType<VaporParticleOptions>(false) {

            @Override
            public @NotNull MapCodec<VaporParticleOptions> codec() {
                return VaporParticleOptions.CODEC;
            }

            @Override
            public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, VaporParticleOptions> streamCodec() {
                return VaporParticleOptions.STREAM_CODEC;
            }
        }
    );

    private HumanParticles() {}

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
