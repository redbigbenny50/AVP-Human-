package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** The Gene Splicer's LABEL field (to the screen when it opens). */
public record S2CSplicerLabelPayload(String label) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("splicer_label_down");

    public static final Type<S2CSplicerLabelPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CSplicerLabelPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.STRING_UTF8,
        S2CSplicerLabelPayload::label,
        S2CSplicerLabelPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
