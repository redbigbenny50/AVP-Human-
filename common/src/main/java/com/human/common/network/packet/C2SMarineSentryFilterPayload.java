package com.human.common.network.packet;

import com.human.HumanResources;
import com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * A player applying a sentry's target filter.
 * <p>
 * Carries no entity id on purpose. The server takes the marine from the container the player currently has open, which
 * is the same validation vanilla's own container-button packet relies on - so a client cannot use this to reconfigure a
 * marine it was never allowed to open, or one on the other side of the world.
 */
public record C2SMarineSentryFilterPayload(
    SentryTargetFilter filter
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("marine_sentry_filter");

    public static final Type<C2SMarineSentryFilterPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SMarineSentryFilterPayload> CODEC = RecordStreamCodec.of(
        SentryTargetFilter.STREAM_CODEC,
        C2SMarineSentryFilterPayload::filter,
        C2SMarineSentryFilterPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
