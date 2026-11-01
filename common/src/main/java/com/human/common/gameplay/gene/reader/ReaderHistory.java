package com.human.common.gameplay.gene.reader;

import com.human.common.gameplay.component.SyringeContents;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.item.SyringeItem;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * [stated] Nov 1 - the gene reader's HISTORY: the last {@value #MAX} syringes it has read, newest first, so a sample's
 * report can be looked at again without the syringe. Kept on the READER ITEM (its custom data), so it goes wherever the
 * reader goes and reaches the client with the item - no packet of its own.
 * <p>
 * An entry is a snapshot taken when the syringe was read: its name, donor, whether it was analyzed then, and its genes.
 * Reading the same syringe again (same sample name) updates its entry and moves it to the top instead of adding
 * another; that is how a sample read before and after analysis shows as analyzed. The 21st sample pushes out the
 * oldest.
 * </p>
 */
public final class ReaderHistory {

    public static final int MAX = 20;

    private static final String KEY = "avp_human_reader_history";

    private ReaderHistory() {}

    public record Gene(
        ResourceLocation id,
        GeneOperationType operation,
        double value
    ) {}

    /** One sample read: {@code id} is its reader name (SAMPLE-COW-3), {@code name} what the syringe is called now. */
    public record Entry(
        String id,
        String name,
        @Nullable String donor,
        boolean analyzed,
        List<Gene> genes
    ) {}

    /** The history on this reader, newest first (empty for anything else). */
    public static List<Entry> read(ItemStack reader) {
        var data = reader.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return List.of();
        }
        var root = data.copyTag();
        if (!root.contains(KEY)) {
            return List.of();
        }
        var list = new ArrayList<Entry>();
        for (var element : root.getList(KEY, Tag.TAG_COMPOUND)) {
            var tag = (CompoundTag) element;
            var genes = new ArrayList<Gene>();
            for (var geneElement : tag.getList("genes", Tag.TAG_COMPOUND)) {
                var geneTag = (CompoundTag) geneElement;
                var id = ResourceLocation.tryParse(geneTag.getString("id"));
                if (id == null) {
                    continue;
                }
                GeneOperationType operation;
                try {
                    operation = GeneOperationType.valueOf(geneTag.getString("op"));
                } catch (IllegalArgumentException exception) {
                    operation = GeneOperationType.ADDITIVE;
                }
                genes.add(new Gene(id, operation, geneTag.getDouble("value")));
            }
            list.add(
                new Entry(
                    tag.getString("id"),
                    tag.getString("name"),
                    tag.contains("donor") ? tag.getString("donor") : null,
                    tag.getBoolean("analyzed"),
                    List.copyOf(genes)
                )
            );
        }
        return List.copyOf(list);
    }

    /**
     * Records the syringe's sample at the top of the reader's history. Does nothing for a syringe with no sample or no
     * reader name (the menu names it first).
     */
    public static void record(ItemStack reader, ItemStack syringe) {
        var id = SyringeItem.sampleIdOf(syringe);
        var contents = syringe.get(HumanDataComponents.SYRINGE_CONTENTS.get());
        if (id == null || contents == null || contents.equals(SyringeContents.EMPTY) || contents.geneBonusDataEntries().isEmpty()) {
            return;
        }
        var entry = new CompoundTag();
        entry.putString("id", id);
        var name = syringe.get(DataComponents.CUSTOM_NAME);
        entry.putString("name", name == null ? id : name.getString());
        var donor = SyringeItem.donorOf(syringe);
        if (donor != null) {
            entry.putString("donor", donor);
        }
        entry.putBoolean("analyzed", SyringeItem.isAnalyzed(syringe));
        var genes = new ListTag();
        for (var gene : contents.geneBonusDataEntries()) {
            var geneTag = new CompoundTag();
            geneTag.putString("id", gene.id().toString());
            geneTag.putString("op", gene.operation().name());
            geneTag.putDouble("value", gene.value());
            genes.add(geneTag);
        }
        entry.put("genes", genes);

        CustomData.update(DataComponents.CUSTOM_DATA, reader, root -> {
            var old = root.getList(KEY, Tag.TAG_COMPOUND);
            var updated = new ListTag();
            updated.add(entry);
            for (var element : old) {
                if (updated.size() >= MAX) {
                    break;
                }
                // The same sample read again replaces its old entry (it moved to the top).
                if (!id.equals(((CompoundTag) element).getString("id"))) {
                    updated.add(element);
                }
            }
            root.put(KEY, updated);
        });
    }
}
