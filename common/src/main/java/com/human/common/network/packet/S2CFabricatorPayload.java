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

import java.util.List;

/**
 * What the open fabricator screen shows, sent only when it changes: the catalogue (each entry LOCAL or NETWORK), the
 * selected entry, its materials split by where they come from, the print in progress, and - only while the STORAGE tab
 * is open - the condensed contents of connected storage.
 */
public record S2CFabricatorPayload(
    int containerId,
    List<Entry> catalogue,
    int selected,
    List<Material> materials,
    ItemStack printing,
    List<Entry> storage
) implements CustomPacketPayload {

    /** An item and a number: for the catalogue, 1 = local and 2 = network; for storage, the count. */
    public record Entry(
        ItemStack stack,
        int value
    ) {}

    public record Material(
        ItemStack stack,
        int internal,
        int network,
        int missing
    ) {}

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("fabricator");

    public static final Type<S2CFabricatorPayload> TYPE = new Type<>(PAYLOAD_ID);

    private static final StreamCodec<Entry> ENTRY = RecordStreamCodec.of(
        BLibCodecs.Stream.ITEM_STACK,
        Entry::stack,
        StreamCodecs.VAR_INT,
        Entry::value,
        Entry::new
    );

    private static final StreamCodec<Material> MATERIAL = RecordStreamCodec.of(
        BLibCodecs.Stream.ITEM_STACK,
        Material::stack,
        StreamCodecs.VAR_INT,
        Material::internal,
        StreamCodecs.VAR_INT,
        Material::network,
        StreamCodecs.VAR_INT,
        Material::missing,
        Material::new
    );

    public static final StreamCodec<S2CFabricatorPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.VAR_INT,
        S2CFabricatorPayload::containerId,
        ENTRY.asList(),
        S2CFabricatorPayload::catalogue,
        StreamCodecs.VAR_INT,
        S2CFabricatorPayload::selected,
        MATERIAL.asList(),
        S2CFabricatorPayload::materials,
        BLibCodecs.Stream.ITEM_STACK,
        S2CFabricatorPayload::printing,
        ENTRY.asList(),
        S2CFabricatorPayload::storage,
        S2CFabricatorPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
