package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * "The purge has finished and there is more than one vapor to choose from" - opens the chem thrower's selection wheel
 * on the holder's client. Sent only while the player is still holding the emptied gun.
 */
public class S2COpenChemWheelPayload implements CustomPacketPayload {

    public static final S2COpenChemWheelPayload INSTANCE = new S2COpenChemWheelPayload();

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("open_chem_wheel");

    public static final Type<S2COpenChemWheelPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2COpenChemWheelPayload> CODEC = StreamCodec.unit(INSTANCE);

    private S2COpenChemWheelPayload() {}

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
