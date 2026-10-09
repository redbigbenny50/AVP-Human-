package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A player asking to look through a turret's camera lens - from a desk terminal's list, or the turret's own View
 * button. The server only honours it from one of those two open screens, for a turret the player owns.
 */
public record C2SSentryViewPayload(
    UUID turret
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("sentry_view");

    public static final Type<C2SSentryViewPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SSentryViewPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.UUID,
        C2SSentryViewPayload::turret,
        C2SSentryViewPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
