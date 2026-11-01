package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** One part of the Gene Analyzer's database text (see AnalyzerDatabase); the client joins the parts in order. */
public record S2CAnalyzerDatabasePayload(
    int part,
    int total,
    String text
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("analyzer_database");

    public static final Type<S2CAnalyzerDatabasePayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CAnalyzerDatabasePayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.INT,
        S2CAnalyzerDatabasePayload::part,
        StreamCodecs.INT,
        S2CAnalyzerDatabasePayload::total,
        StreamCodecs.STRING_UTF8,
        S2CAnalyzerDatabasePayload::text,
        S2CAnalyzerDatabasePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
