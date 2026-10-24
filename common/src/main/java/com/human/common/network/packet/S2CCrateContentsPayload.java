package com.human.common.network.packet;

import com.blib.api.common.codec.v1.BLibCodecs;
import com.human.HumanResources;
import com.human.common.gameplay.storage.CrateInventory;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** The condensed contents of the crate a player has open: each distinct item once, with its total. */
public record S2CCrateContentsPayload(
    int containerId,
    int usedSlots,
    int totalSlots,
    List<CrateInventory.Entry> entries
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("crate_contents");

    public static final Type<S2CCrateContentsPayload> TYPE = new Type<>(PAYLOAD_ID);

    private static final StreamCodec<CrateInventory.Entry> ENTRY_CODEC = RecordStreamCodec.of(
        BLibCodecs.Stream.ITEM_STACK,
        CrateInventory.Entry::stack,
        StreamCodecs.LONG,
        CrateInventory.Entry::count,
        CrateInventory.Entry::new
    );

    public static final StreamCodec<S2CCrateContentsPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.VAR_INT,
        S2CCrateContentsPayload::containerId,
        StreamCodecs.VAR_INT,
        S2CCrateContentsPayload::usedSlots,
        StreamCodecs.VAR_INT,
        S2CCrateContentsPayload::totalSlots,
        ENTRY_CODEC.asList(),
        S2CCrateContentsPayload::entries,
        S2CCrateContentsPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
