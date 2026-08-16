package com.human.common.network.packet;

import com.human.HumanResources;
import com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * The marine's current target filter, sent as its screen opens so the page shows what is actually set rather than a
 * default the player would then have to notice was wrong.
 */
public record S2CMarineSentryFilterPayload(
    SentryTargetFilter filter
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("marine_sentry_filter_sync");

    public static final Type<S2CMarineSentryFilterPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CMarineSentryFilterPayload> CODEC = RecordStreamCodec.of(
        SentryTargetFilter.STREAM_CODEC,
        S2CMarineSentryFilterPayload::filter,
        S2CMarineSentryFilterPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
