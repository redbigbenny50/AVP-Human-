package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * "This mob carries more than one bucket substance" - opens the harvest selection wheel on the player's client.
 * {@code substances} is a bit per {@code HarvestGenes.Substance} ordinal; {@code hand} is the InteractionHand ordinal.
 */
public record S2COpenHarvestWheelPayload(
    int entityId,
    int substances,
    int hand
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("open_harvest_wheel");

    public static final Type<S2COpenHarvestWheelPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2COpenHarvestWheelPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.INT,
        S2COpenHarvestWheelPayload::entityId,
        StreamCodecs.INT,
        S2COpenHarvestWheelPayload::substances,
        StreamCodecs.INT,
        S2COpenHarvestWheelPayload::hand,
        S2COpenHarvestWheelPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
