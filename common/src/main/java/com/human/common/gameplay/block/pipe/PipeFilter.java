package com.human.common.gameplay.block.pipe;

import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import com.just.codec.stream.schema.StreamCodecSchema;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One pipe end's filter: any number of entries, an allow / block switch and a priority.
 * <p>
 * [stated] Oct 7: "enough slots for multiple items" - not 1, 8 or a crafting grid - with TAGS, a search and hot
 * buttons; an entry for an item with durability matches it at ANY durability; a per-entry "Not enchanted" toggle
 * (default Any). [stated] Oct 8: "if theres no difference in performance make it infinite" - there is none: entries are
 * compiled into hash sets, so an exact-item check costs the same with 5 entries or 5,000. Only tag and mod entries are
 * checked one by one, and those are few. {@link #MAX_ENTRIES} is a server safety limit against a hostile packet, not a
 * gameplay cap.
 * <h2>ENTRY KINDS</h2>
 * <ul>
 * <li>EXACT - one item (or, on a fluid pipe, one fluid) by registry id, any durability and components;</li>
 * <li>TAG - an item or fluid tag, e.g. {@code c:ores};</li>
 * <li>MOD - everything from one mod, by namespace.</li>
 * </ul>
 * <h2>PRIORITY</h2> [stated] Oct 7 plan: compression crates High, machines Normal, chests and plain crates Low. AUTO
 * resolves by what the end touches - see {@link #resolvePriority}.
 */
public final class PipeFilter {

    /** Server safety limit on entries per end - far beyond any real filter; protects against hostile packets. */
    public static final int MAX_ENTRIES = 4096;

    public static final byte PRIORITY_AUTO = 0;

    public static final byte PRIORITY_LOW = 1;

    public static final byte PRIORITY_NORMAL = 2;

    public static final byte PRIORITY_HIGH = 3;

    /** What one entry names. */
    public enum Kind {

        EXACT,
        TAG,
        MOD;

        static Kind byOrdinal(int ordinal) {
            var values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : EXACT;
        }
    }

    /**
     * One filter line.
     *
     * @param kind         what {@code id} names
     * @param id           a registry id (EXACT), a tag id without '#' (TAG) or a namespace (MOD)
     * @param notEnchanted when true, enchanted items do not match this line (items only)
     */
    public record Entry(
        Kind kind,
        String id,
        boolean notEnchanted
    ) {

        /** @return the same line with the enchant toggle flipped */
        public Entry toggleEnchanted() {
            return new Entry(kind, id, !notEnchanted);
        }

        /** @return whether this line names the same thing as another, ignoring the enchant toggle */
        public boolean sameTarget(Entry other) {
            return kind == other.kind && id.equals(other.id);
        }
    }

    public static final StreamCodec<PipeFilter> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public <T> PipeFilter decode(@NotNull StreamCodecSchema<T> schema, @NotNull T input) {
            var filter = new PipeFilter();
            filter.blockList = StreamCodecs.BOOLEAN.decode(schema, input);
            filter.priority = clampPriority(StreamCodecs.BYTE.decode(schema, input));
            var count = StreamCodecs.VAR_INT.decode(schema, input);

            for (var index = 0; index < count; index++) {
                var kind = Kind.byOrdinal(StreamCodecs.BYTE.decode(schema, input));
                var id = StreamCodecs.STRING_UTF8.decode(schema, input);
                var notEnchanted = StreamCodecs.BOOLEAN.decode(schema, input);

                // Read every line so the stream stays aligned, keep only what fits the safety limit.
                if (filter.entries.size() < MAX_ENTRIES && !id.isBlank() && id.length() <= 256) {
                    filter.entries.add(new Entry(kind, id, notEnchanted));
                }
            }

            return filter;
        }

        @Override
        public <T> void encode(@NotNull StreamCodecSchema<T> schema, @NotNull T input, @NotNull PipeFilter filter) {
            StreamCodecs.BOOLEAN.encode(schema, input, filter.blockList);
            StreamCodecs.BYTE.encode(schema, input, filter.priority);
            StreamCodecs.VAR_INT.encode(schema, input, filter.entries.size());

            for (var entry : filter.entries) {
                StreamCodecs.BYTE.encode(schema, input, (byte) entry.kind().ordinal());
                StreamCodecs.STRING_UTF8.encode(schema, input, entry.id());
                StreamCodecs.BOOLEAN.encode(schema, input, entry.notEnchanted());
            }
        }
    };

    private final List<Entry> entries = new ArrayList<>();

    private boolean blockList;

    private byte priority = PRIORITY_AUTO;

    /** Built on first use after a change; never saved. */
    private @Nullable Compiled compiled;

    // ------------------------------------------------------------------------------------------------------------
    // Editing
    // ------------------------------------------------------------------------------------------------------------

    /** @return a deep copy */
    public PipeFilter copy() {
        var copy = new PipeFilter();
        copy.entries.addAll(entries);
        copy.blockList = blockList;
        copy.priority = priority;
        return copy;
    }

    /** @return the lines, read-only */
    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /** @return whether there are no lines (everything passes) */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Adds a line unless one naming the same thing is already there.
     *
     * @param entry the line
     * @return whether it was added
     */
    public boolean add(Entry entry) {
        if (entries.size() >= MAX_ENTRIES) {
            return false;
        }

        for (var existing : entries) {
            if (existing.sameTarget(entry)) {
                return false;
            }
        }

        entries.add(entry);
        compiled = null;
        return true;
    }

    /** @param index the line to remove */
    public void remove(int index) {
        if (index >= 0 && index < entries.size()) {
            entries.remove(index);
            compiled = null;
        }
    }

    /** @param index the line whose "Not enchanted" toggle to flip */
    public void toggleEnchanted(int index) {
        if (index >= 0 && index < entries.size()) {
            entries.set(index, entries.get(index).toggleEnchanted());
            compiled = null;
        }
    }

    /** Removes every line. */
    public void clear() {
        entries.clear();
        compiled = null;
    }

    /** @return whether the lines are a block list rather than an allow list */
    public boolean isBlockList() {
        return blockList;
    }

    /** @param blockList whether the lines are a block list */
    public void setBlockList(boolean blockList) {
        this.blockList = blockList;
    }

    /** @return the end's priority setting, {@link #PRIORITY_AUTO} by default */
    public byte priority() {
        return priority;
    }

    /** @param priority the end's priority setting */
    public void setPriority(byte priority) {
        this.priority = clampPriority(priority);
    }

    private static byte clampPriority(byte value) {
        return value < PRIORITY_AUTO || value > PRIORITY_HIGH ? PRIORITY_AUTO : value;
    }

    /**
     * [stated] Oct 7 defaults: machines Normal, storage Low. (Compression crates will default High when they exist.)
     *
     * @param machine whether the end touches a machine rather than storage
     * @return the priority this end routes at
     */
    public byte resolvePriority(boolean machine) {
        if (priority != PRIORITY_AUTO) {
            return priority;
        }

        return machine ? PRIORITY_NORMAL : PRIORITY_LOW;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Matching
    // ------------------------------------------------------------------------------------------------------------

    /**
     * @param stack an item stack
     * @return whether this end lets it through (an empty filter lets everything through)
     */
    public boolean allowsItem(ItemStack stack) {
        if (entries.isEmpty()) {
            return true;
        }

        return compiled().matchesItem(stack) != blockList;
    }

    /**
     * @param fluid a fluid
     * @return whether this end lets it through (an empty filter lets everything through)
     */
    public boolean allowsFluid(Fluid fluid) {
        if (entries.isEmpty()) {
            return true;
        }

        return compiled().matchesFluid(fluid) != blockList;
    }

    private Compiled compiled() {
        var built = compiled;

        if (built == null) {
            built = new Compiled(entries);
            compiled = built;
        }

        return built;
    }

    /**
     * The lines turned into lookups. Built against the live registries; tags are kept as keys and checked live, so a
     * data-pack reload that changes a tag is seen at once.
     */
    private static final class Compiled {

        private final Set<Item> items = new HashSet<>();

        private final Set<Item> plainItems = new HashSet<>();

        private final List<TagKey<Item>> itemTags = new ArrayList<>();

        private final List<TagKey<Item>> plainItemTags = new ArrayList<>();

        private final Set<String> mods = new HashSet<>();

        private final Set<String> plainMods = new HashSet<>();

        private final Set<Fluid> fluids = new HashSet<>();

        private final List<TagKey<Fluid>> fluidTags = new ArrayList<>();

        Compiled(List<Entry> entries) {
            for (var entry : entries) {
                var plain = entry.notEnchanted();

                switch (entry.kind()) {
                    case EXACT -> {
                        var id = ResourceLocation.tryParse(entry.id());

                        if (id == null) {
                            continue;
                        }

                        // A line naming something no longer registered (a removed mod) is kept but matches nothing.
                        BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> (plain ? plainItems : items).add(item));
                        BuiltInRegistries.FLUID.getOptional(id).ifPresent(fluids::add);

                        // Oct 14 FIX - a BUCKET line also matches the fluid in it. Fluid pipes used to list items by
                        // mistake (PipeEndScreen), so worlds hold lines like avp_human:cryofluid_bucket on fluid pipes
                        // that never matched anything; this makes them work as meant, with no rebuilding by hand.
                        // Through each fluid's own getBucket() - the bucket's fluid field is private in vanilla.
                        BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> {
                            if (item == net.minecraft.world.item.Items.BUCKET || item == net.minecraft.world.item.Items.AIR) {
                                return;
                            }

                            for (var fluid : BuiltInRegistries.FLUID) {
                                if (fluid.getBucket() == item && fluid.isSource(fluid.defaultFluidState())) {
                                    fluids.add(fluid);
                                }
                            }
                        });
                    }
                    case TAG -> {
                        var id = ResourceLocation.tryParse(entry.id());

                        if (id == null) {
                            continue;
                        }

                        (plain ? plainItemTags : itemTags).add(TagKey.create(Registries.ITEM, id));
                        fluidTags.add(TagKey.create(Registries.FLUID, id));
                    }
                    case MOD -> (plain ? plainMods : mods).add(entry.id());
                }
            }
        }

        boolean matchesItem(ItemStack stack) {
            var item = stack.getItem();

            if (items.contains(item)) {
                return true;
            }

            var unenchanted = !stack.isEnchanted();

            if (unenchanted && plainItems.contains(item)) {
                return true;
            }

            if (!mods.isEmpty() || !plainMods.isEmpty()) {
                var namespace = BuiltInRegistries.ITEM.getKey(item).getNamespace();

                if (mods.contains(namespace) || unenchanted && plainMods.contains(namespace)) {
                    return true;
                }
            }

            for (var tag : itemTags) {
                if (stack.is(tag)) {
                    return true;
                }
            }

            if (unenchanted) {
                for (var tag : plainItemTags) {
                    if (stack.is(tag)) {
                        return true;
                    }
                }
            }

            return false;
        }

        boolean matchesFluid(Fluid fluid) {
            if (fluids.contains(fluid)) {
                return true;
            }

            if (!mods.isEmpty() || !plainMods.isEmpty()) {
                var namespace = BuiltInRegistries.FLUID.getKey(fluid).getNamespace();

                if (mods.contains(namespace) || plainMods.contains(namespace)) {
                    return true;
                }
            }

            for (var tag : fluidTags) {
                if (fluid.is(tag)) {
                    return true;
                }
            }

            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------------------------------------------------

    /** @param tag where to write this filter */
    public void save(CompoundTag tag) {
        tag.putBoolean("block", blockList);
        tag.putByte("priority", priority);
        var list = new ListTag();

        for (var entry : entries) {
            var line = new CompoundTag();
            line.putByte("kind", (byte) entry.kind().ordinal());
            line.putString("id", entry.id());

            if (entry.notEnchanted()) {
                line.putBoolean("plain", true);
            }

            list.add(line);
        }

        tag.put("entries", list);
    }

    /**
     * Reads a filter, including the pre-Oct-8 format (nine "ids" strings, empty for a free slot), which becomes EXACT
     * lines so nobody's existing filters are lost.
     *
     * @param tag the saved filter
     * @return the filter
     */
    public static PipeFilter load(CompoundTag tag) {
        var filter = new PipeFilter();
        filter.blockList = tag.getBoolean("block");
        filter.priority = clampPriority(tag.getByte("priority"));

        if (tag.contains("entries", Tag.TAG_LIST)) {
            var list = tag.getList("entries", Tag.TAG_COMPOUND);

            for (var index = 0; index < list.size() && filter.entries.size() < MAX_ENTRIES; index++) {
                var line = list.getCompound(index);
                var id = line.getString("id");

                if (!id.isBlank()) {
                    filter.entries
                        .add(new Entry(Kind.byOrdinal(line.getByte("kind")), id, line.getBoolean("plain")));
                }
            }
        } else {
            var ids = tag.getList("ids", Tag.TAG_STRING);

            for (var index = 0; index < ids.size(); index++) {
                var id = ids.getString(index);

                if (!id.isEmpty()) {
                    filter.add(new Entry(Kind.EXACT, id, false));
                }
            }
        }

        return filter;
    }
}
