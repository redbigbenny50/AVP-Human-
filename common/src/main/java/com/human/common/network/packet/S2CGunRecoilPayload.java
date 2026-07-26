package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record S2CGunRecoilPayload(
    float recoil
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("gun_recoil");

    public static final Type<S2CGunRecoilPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CGunRecoilPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.FLOAT,
        S2CGunRecoilPayload::recoil,
        S2CGunRecoilPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
