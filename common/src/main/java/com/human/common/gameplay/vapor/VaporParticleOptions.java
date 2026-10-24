package com.human.common.gameplay.vapor;

import com.human.common.registry.init.HumanParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * One vapor puff: which vapor (its colour, drift and glow come from {@link VaporLook}), which puff style, how big it
 * starts and ends (blocks across) and how long it lives (ticks; 0 picks the particle's own default).
 */
public record VaporParticleOptions(
    int vapor,
    int style,
    float startSize,
    float endSize,
    int lifetime
) implements ParticleOptions {

    public static final MapCodec<VaporParticleOptions> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.INT.fieldOf("vapor").forGetter(VaporParticleOptions::vapor),
            Codec.INT.fieldOf("style").forGetter(VaporParticleOptions::style),
            Codec.FLOAT.fieldOf("start_size").forGetter(VaporParticleOptions::startSize),
            Codec.FLOAT.fieldOf("end_size").forGetter(VaporParticleOptions::endSize),
            Codec.INT.fieldOf("lifetime").forGetter(VaporParticleOptions::lifetime)
        ).apply(instance, VaporParticleOptions::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, VaporParticleOptions> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        VaporParticleOptions::vapor,
        ByteBufCodecs.VAR_INT,
        VaporParticleOptions::style,
        ByteBufCodecs.FLOAT,
        VaporParticleOptions::startSize,
        ByteBufCodecs.FLOAT,
        VaporParticleOptions::endSize,
        ByteBufCodecs.VAR_INT,
        VaporParticleOptions::lifetime,
        VaporParticleOptions::new
    );

    public static VaporParticleOptions of(VaporKind kind, VaporStyle style, float startSize, float endSize, int lifetime) {
        return new VaporParticleOptions(kind.ordinal(), style.ordinal(), startSize, endSize, lifetime);
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return HumanParticles.VAPOR.get();
    }
}
