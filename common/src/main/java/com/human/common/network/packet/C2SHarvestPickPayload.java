package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** The substance picked on the harvest wheel. ⚠ The server re-checks everything before harvesting. */
public record C2SHarvestPickPayload(
    int entityId,
    int substance,
    int hand
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("harvest_pick");

    public static final Type<C2SHarvestPickPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SHarvestPickPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.INT,
        C2SHarvestPickPayload::entityId,
        StreamCodecs.INT,
        C2SHarvestPickPayload::substance,
        StreamCodecs.INT,
        C2SHarvestPickPayload::hand,
        C2SHarvestPickPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
