package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * One tick of a chem thrower blast, for the clients to draw: where the nozzle is, the aim, how far it reached before a
 * wall, how wide its end is (wider with FLAME), and the vapor. Sent to everyone tracking the shooter and to the
 * shooter.
 */
public record S2CChemBlastPayload(
    float x,
    float y,
    float z,
    float dx,
    float dy,
    float dz,
    float reach,
    float farHalf,
    int vapor,
    // [stated] Oct 19 - the shooter's entity id, so each client can run that shooter's start / loop / stop sound.
    int shooter
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("chem_blast");

    public static final Type<S2CChemBlastPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CChemBlastPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.FLOAT,
        S2CChemBlastPayload::x,
        StreamCodecs.FLOAT,
        S2CChemBlastPayload::y,
        StreamCodecs.FLOAT,
        S2CChemBlastPayload::z,
        StreamCodecs.FLOAT,
        S2CChemBlastPayload::dx,
        StreamCodecs.FLOAT,
        S2CChemBlastPayload::dy,
        StreamCodecs.FLOAT,
        S2CChemBlastPayload::dz,
        StreamCodecs.FLOAT,
        S2CChemBlastPayload::reach,
        StreamCodecs.FLOAT,
        S2CChemBlastPayload::farHalf,
        StreamCodecs.INT,
        S2CChemBlastPayload::vapor,
        StreamCodecs.VAR_INT,
        S2CChemBlastPayload::shooter,
        S2CChemBlastPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
