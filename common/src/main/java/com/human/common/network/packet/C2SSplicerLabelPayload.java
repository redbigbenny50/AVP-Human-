package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** The Gene Splicer's LABEL field (from the screen as the player types). */
public record C2SSplicerLabelPayload(String label) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("splicer_label_up");

    public static final Type<C2SSplicerLabelPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SSplicerLabelPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.STRING_UTF8,
        C2SSplicerLabelPayload::label,
        C2SSplicerLabelPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
