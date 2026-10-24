package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Put the cursor stack (or one item of it) into the open crate. */
public record C2SCrateInsertPayload(
    int containerId,
    boolean single
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("crate_insert");

    public static final Type<C2SCrateInsertPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SCrateInsertPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.VAR_INT,
        C2SCrateInsertPayload::containerId,
        StreamCodecs.BOOLEAN,
        C2SCrateInsertPayload::single,
        C2SCrateInsertPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
