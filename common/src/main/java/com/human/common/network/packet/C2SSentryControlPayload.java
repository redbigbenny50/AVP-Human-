package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * The controls of a turret being looked through, sent every tick while viewing: where to aim, whether the trigger is
 * held, whether to stop. Carries no turret - the server uses the session it started for this player, so nothing else
 * can be steered with it.
 */
public record C2SSentryControlPayload(
    float yaw,
    float pitch,
    boolean trigger,
    boolean exit
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("sentry_control");

    public static final Type<C2SSentryControlPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SSentryControlPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.FLOAT,
        C2SSentryControlPayload::yaw,
        StreamCodecs.FLOAT,
        C2SSentryControlPayload::pitch,
        StreamCodecs.BOOLEAN,
        C2SSentryControlPayload::trigger,
        StreamCodecs.BOOLEAN,
        C2SSentryControlPayload::exit,
        C2SSentryControlPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
