package com.human.common.network.packet;

import com.blib.api.common.codec.v1.BLibCodecs;
import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Take an item from the open crate: a stack, half a stack, or a stack straight into the inventory. */
public record C2SCrateTakePayload(
    int containerId,
    byte action,
    ItemStack prototype
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("crate_take");

    public static final Type<C2SCrateTakePayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SCrateTakePayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.VAR_INT,
        C2SCrateTakePayload::containerId,
        StreamCodecs.BYTE,
        C2SCrateTakePayload::action,
        BLibCodecs.Stream.ITEM_STACK,
        C2SCrateTakePayload::prototype,
        C2SCrateTakePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
