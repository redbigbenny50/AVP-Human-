package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/** The turrets on the network, for a desk terminal's list. Sent as the terminal opens. */
public record S2CSentryNetworkPayload(
    List<Turret> turrets
) implements CustomPacketPayload {

    /** One row: which turret, what to call it, where it is, and whether it has a camera lens. */
    public record Turret(
        UUID id,
        String name,
        String dimension,
        long pos,
        boolean camera
    ) {

        public static final StreamCodec<Turret> CODEC = RecordStreamCodec.of(
            StreamCodecs.UUID,
            Turret::id,
            StreamCodecs.STRING_UTF8,
            Turret::name,
            StreamCodecs.STRING_UTF8,
            Turret::dimension,
            StreamCodecs.LONG,
            Turret::pos,
            StreamCodecs.BOOLEAN,
            Turret::camera,
            Turret::new
        );
    }

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("sentry_network");

    public static final Type<S2CSentryNetworkPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CSentryNetworkPayload> CODEC = RecordStreamCodec.of(
        Turret.CODEC.asList(),
        S2CSentryNetworkPayload::turrets,
        S2CSentryNetworkPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
