package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** "This mob is frozen solid" — resent every second while it is (see FrozenSolid). The client draws the frost. */
public record S2CFrozenSolidPayload(int entityId) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("frozen_solid");

    public static final Type<S2CFrozenSolidPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CFrozenSolidPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.INT,
        S2CFrozenSolidPayload::entityId,
        S2CFrozenSolidPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
