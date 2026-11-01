package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** A click on a NETWORK STORAGE row: load that listed tube into the machine (Recombinator or Splicer). */
public record C2SNetworkTubePullPayload(int index) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("network_tube_pull");

    public static final Type<C2SNetworkTubePullPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SNetworkTubePullPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.INT,
        C2SNetworkTubePullPayload::index,
        C2SNetworkTubePullPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
