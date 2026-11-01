package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** A player's whole discovery list (genes, species), newline-separated ids. Sent on join and on every new discovery. */
public record S2CGeneDiscoveriesPayload(
    String genes,
    String species
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("gene_discoveries");

    public static final Type<S2CGeneDiscoveriesPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CGeneDiscoveriesPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.STRING_UTF8,
        S2CGeneDiscoveriesPayload::genes,
        StreamCodecs.STRING_UTF8,
        S2CGeneDiscoveriesPayload::species,
        S2CGeneDiscoveriesPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
