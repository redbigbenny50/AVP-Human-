package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** The gene reader's LABEL field, for the syringe in its slot. ⚠ The server trims it to 20 characters. */
public record C2SSyringeLabelPayload(String label) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("syringe_label");

    public static final Type<C2SSyringeLabelPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SSyringeLabelPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.STRING_UTF8,
        C2SSyringeLabelPayload::label,
        C2SSyringeLabelPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
