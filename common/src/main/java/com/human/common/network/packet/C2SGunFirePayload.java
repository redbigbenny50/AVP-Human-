package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SGunFirePayload(
    int tickProgress,
    float yaw,
    float pitch,
    int predictedEntityId,
    String predictedLimbId
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("gun_fire");

    public static final Type<C2SGunFirePayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SGunFirePayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.INT,
        C2SGunFirePayload::tickProgress,
        StreamCodecs.FLOAT,
        C2SGunFirePayload::yaw,
        StreamCodecs.FLOAT,
        C2SGunFirePayload::pitch,
        StreamCodecs.INT,
        C2SGunFirePayload::predictedEntityId,
        StreamCodecs.STRING_UTF8,
        C2SGunFirePayload::predictedLimbId,
        C2SGunFirePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
