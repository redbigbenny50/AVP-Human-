package com.human.common.network.packet;

import com.blib.api.common.codec.v1.BLibCodecs;
import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record S2CNukeEffectPayload(
    BlockPos center,
    int radius,
    float flashIntensity,
    float shakeIntensity,
    int durationTicks
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("nuke_effect");

    public static final Type<S2CNukeEffectPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CNukeEffectPayload> CODEC = RecordStreamCodec.of(
        BLibCodecs.Stream.BLOCK_POS,
        S2CNukeEffectPayload::center,
        StreamCodecs.INT,
        S2CNukeEffectPayload::radius,
        StreamCodecs.FLOAT,
        S2CNukeEffectPayload::flashIntensity,
        StreamCodecs.FLOAT,
        S2CNukeEffectPayload::shakeIntensity,
        StreamCodecs.INT,
        S2CNukeEffectPayload::durationTicks,
        S2CNukeEffectPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
