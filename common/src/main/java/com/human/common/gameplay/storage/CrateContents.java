package com.human.common.gameplay.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * What a full crate ITEM carries: the id of its contents in {@link CrateStorageData}, plus a short summary for the
 * tooltip. The items themselves are never stored on the item.
 */
public record CrateContents(
    UUID id,
    long items,
    int usedSlots,
    int totalSlots,
    List<Preview> preview
) {

    public static final int PREVIEW_SIZE = 5;

    public record Preview(
        ItemStack stack,
        long count
    ) {

        public static final Codec<Preview> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                ItemStack.SINGLE_ITEM_CODEC.fieldOf("stack").forGetter(Preview::stack),
                Codec.LONG.fieldOf("count").forGetter(Preview::count)
            ).apply(instance, Preview::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, Preview> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            Preview::stack,
            ByteBufCodecs.VAR_LONG,
            Preview::count,
            Preview::new
        );
    }

    public static final Codec<CrateContents> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(CrateContents::id),
            Codec.LONG.fieldOf("items").forGetter(CrateContents::items),
            Codec.INT.fieldOf("used_slots").forGetter(CrateContents::usedSlots),
            Codec.INT.fieldOf("total_slots").forGetter(CrateContents::totalSlots),
            Preview.CODEC.listOf().fieldOf("preview").forGetter(CrateContents::preview)
        ).apply(instance, CrateContents::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CrateContents> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        CrateContents::id,
        ByteBufCodecs.VAR_LONG,
        CrateContents::items,
        ByteBufCodecs.VAR_INT,
        CrateContents::usedSlots,
        ByteBufCodecs.VAR_INT,
        CrateContents::totalSlots,
        Preview.STREAM_CODEC.apply(ByteBufCodecs.list()),
        CrateContents::preview,
        CrateContents::new
    );

    /** Builds the summary for an inventory about to be packed away under {@code id}. */
    public static CrateContents of(UUID id, CrateInventory inventory) {
        var entries = new java.util.ArrayList<>(inventory.summarize());

        entries.sort((a, b) -> Long.compare(b.count(), a.count()));

        var preview = entries.stream()
            .limit(PREVIEW_SIZE)
            .map(entry -> new Preview(entry.stack(), entry.count()))
            .toList();

        return new CrateContents(id, inventory.totalItems(), inventory.usedSlots(), inventory.size(), preview);
    }
}
