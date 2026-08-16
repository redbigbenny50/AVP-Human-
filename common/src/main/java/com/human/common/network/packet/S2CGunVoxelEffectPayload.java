package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public record S2CGunVoxelEffectPayload(
    Vec3 origin,
    Vec3 endpoint,
    Vec3 impactNormal,
    int seed,
    boolean entityImpact,
    int fluidType
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("gun_voxel_effect");

    public static final Type<S2CGunVoxelEffectPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CGunVoxelEffectPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.DOUBLE,
        payload -> payload.origin().x,
        StreamCodecs.DOUBLE,
        payload -> payload.origin().y,
        StreamCodecs.DOUBLE,
        payload -> payload.origin().z,
        StreamCodecs.DOUBLE,
        payload -> payload.endpoint().x,
        StreamCodecs.DOUBLE,
        payload -> payload.endpoint().y,
        StreamCodecs.DOUBLE,
        payload -> payload.endpoint().z,
        StreamCodecs.DOUBLE,
        payload -> payload.impactNormal().x,
        StreamCodecs.DOUBLE,
        payload -> payload.impactNormal().y,
        StreamCodecs.DOUBLE,
        payload -> payload.impactNormal().z,
        StreamCodecs.INT,
        S2CGunVoxelEffectPayload::seed,
        StreamCodecs.BOOLEAN,
        S2CGunVoxelEffectPayload::entityImpact,
        StreamCodecs.INT,
        S2CGunVoxelEffectPayload::fluidType,
        (
            originX,
            originY,
            originZ,
            endpointX,
            endpointY,
            endpointZ,
            normalX,
            normalY,
            normalZ,
            seed,
            entityImpact,
            fluidType
        ) -> new S2CGunVoxelEffectPayload(
            new Vec3(originX, originY, originZ),
            new Vec3(endpointX, endpointY, endpointZ),
            new Vec3(normalX, normalY, normalZ),
            seed,
            entityImpact,
            fluidType
        )
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
