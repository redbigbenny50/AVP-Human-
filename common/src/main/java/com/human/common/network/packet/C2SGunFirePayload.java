package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record C2SGunFirePayload(
    int tickProgress
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("gun_fire");

    public static final Type<C2SGunFirePayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SGunFirePayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.INT,
        C2SGunFirePayload::tickProgress,
        C2SGunFirePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
