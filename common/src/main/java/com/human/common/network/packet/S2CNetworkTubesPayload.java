package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * A genetic machine screen's NETWORK STORAGE list (Recombinator, Splicer): one tube per line (index, gene, value,
 * operation, per dose, storage).
 */
public record S2CNetworkTubesPayload(String text) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("network_tubes");

    public static final Type<S2CNetworkTubesPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CNetworkTubesPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.STRING_UTF8,
        S2CNetworkTubesPayload::text,
        S2CNetworkTubesPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
