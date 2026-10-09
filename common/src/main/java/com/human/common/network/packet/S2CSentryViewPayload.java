package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Start looking through a turret (its entity id), or stop ({@link #END}). */
public record S2CSentryViewPayload(
    int entityId
) implements CustomPacketPayload {

    public static final int END = -1;

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("sentry_view_state");

    public static final Type<S2CSentryViewPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CSentryViewPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.INT,
        S2CSentryViewPayload::entityId,
        S2CSentryViewPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
