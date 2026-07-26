package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/** A client-only burst sent after a confirmed lethal conventional-gun hit. */
public record S2CGunKillEffectPayload(
    Vec3 position,
    Vec3 direction,
    int fluidType,
    int burstCount,
    int seed
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("gun_kill_effect");

    public static final Type<S2CGunKillEffectPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CGunKillEffectPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.DOUBLE,
        payload -> payload.position().x,
        StreamCodecs.DOUBLE,
        payload -> payload.position().y,
        StreamCodecs.DOUBLE,
        payload -> payload.position().z,
        StreamCodecs.DOUBLE,
        payload -> payload.direction().x,
        StreamCodecs.DOUBLE,
        payload -> payload.direction().y,
        StreamCodecs.DOUBLE,
        payload -> payload.direction().z,
        StreamCodecs.INT,
        S2CGunKillEffectPayload::fluidType,
        StreamCodecs.INT,
        S2CGunKillEffectPayload::burstCount,
        StreamCodecs.INT,
        S2CGunKillEffectPayload::seed,
        (x, y, z, directionX, directionY, directionZ, fluidType, burstCount, seed) -> new S2CGunKillEffectPayload(
            new Vec3(x, y, z),
            new Vec3(directionX, directionY, directionZ),
            fluidType,
            burstCount,
            seed
        )
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
