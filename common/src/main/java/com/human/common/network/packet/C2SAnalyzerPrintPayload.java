package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** PRINT DOCUMENT: the ticked gene and species ids, newline-separated. The server prints only what the player knows. */
public record C2SAnalyzerPrintPayload(
    String genes,
    String species
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("analyzer_print");

    public static final Type<C2SAnalyzerPrintPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SAnalyzerPrintPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.STRING_UTF8,
        C2SAnalyzerPrintPayload::genes,
        StreamCodecs.STRING_UTF8,
        C2SAnalyzerPrintPayload::species,
        C2SAnalyzerPrintPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
