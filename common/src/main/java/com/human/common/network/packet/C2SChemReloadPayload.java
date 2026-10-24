package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * The vapor picked in the chem thrower's ammo menu, by {@code VaporKind} ordinal. ⚠ The server re-checks that the
 * player really carries that tank before loading it.
 */
public record C2SChemReloadPayload(int vapor) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("chem_reload");

    public static final Type<C2SChemReloadPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SChemReloadPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.INT,
        C2SChemReloadPayload::vapor,
        C2SChemReloadPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
