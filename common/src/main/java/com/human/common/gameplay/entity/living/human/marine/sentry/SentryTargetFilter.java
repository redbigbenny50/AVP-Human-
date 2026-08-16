package com.human.common.gameplay.entity.living.human.marine.sentry;

import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import com.just.codec.stream.schema.StreamCodecSchema;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What a sentry will and will not shoot.
 * <p>
 * One selection - some categories, plus any number of individually named entity types - read either as a WHITELIST
 * ("only these") or a BLACKLIST ("anything but these"). Naming a type individually is how you carve an exception out of
 * a category: whitelist ANIMALS and add nothing, and a sentry guards a farm against nothing at all; blacklist ANIMALS
 * and name creepers, and it leaves the cows alone but still shoots creepers.
 * <p>
 * ⚠ The default is a whitelist of MONSTERS rather than an empty one. An empty whitelist is a sentry that shoots
 * nothing, which is a quiet way for someone to conclude the feature is broken.
 */
public class SentryTargetFilter {

    private static final String NBT_WHITELIST = "whitelist";

    private static final String NBT_CATEGORIES = "categories";

    private static final String NBT_CUSTOM = "custom";

    /**
     * ⚠ An anonymous class rather than {@code StreamCodec.of(lambda, lambda)}. Both halves of the interface declare
     * their own {@code <T>} for the buffer type, and a lambda cannot implement a generic method - the compiler cannot
     * infer what it never gets to name.
     */
    public static final StreamCodec<SentryTargetFilter> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public <T> SentryTargetFilter decode(@NotNull StreamCodecSchema<T> schema, @NotNull T input) {
            var filter = new SentryTargetFilter();

            filter.whitelist = StreamCodecs.BOOLEAN.decode(schema, input);
            filter.categories.clear();

            for (var categoryId : StreamCodecs.STRING_UTF8.asList().decode(schema, input)) {
                var category = SentryTargetCategory.byId(categoryId);

                if (category != null) {
                    filter.categories.add(category);
                }
            }

            for (var customId : StreamCodecs.STRING_UTF8.asList().decode(schema, input)) {
                // Unparseable or unknown ids are dropped rather than rejected: a filter naming a mob from a mod that
                // has since been removed should lose that line, not fail to load.
                var resourceLocation = ResourceLocation.tryParse(customId);

                if (resourceLocation != null) {
                    filter.customTypes.add(resourceLocation);
                }
            }

            return filter;
        }

        @Override
        public <T> void encode(@NotNull StreamCodecSchema<T> schema, @NotNull T input, @NotNull SentryTargetFilter filter) {
            StreamCodecs.BOOLEAN.encode(schema, input, filter.whitelist);
            StreamCodecs.STRING_UTF8.asList()
                .encode(
                    schema,
                    input,
                    filter.categories.stream().map(SentryTargetCategory::getId).toList()
                );
            StreamCodecs.STRING_UTF8.asList()
                .encode(
                    schema,
                    input,
                    filter.customTypes.stream().map(ResourceLocation::toString).toList()
                );
        }
    };

    private boolean whitelist = true;

    private final Set<SentryTargetCategory> categories = EnumSet.of(SentryTargetCategory.MONSTERS);

    /**
     * Individually named entity types. A {@link LinkedHashSet} so the list a player built stays in the order they built
     * it rather than reshuffling every time the screen opens.
     */
    private final Set<ResourceLocation> customTypes = new LinkedHashSet<>();

    /**
     * Whether a sentry should engage this entity.
     * <p>
     * ⚠ Answers only the "do I care about this kind of thing" question. Everything about allegiance - the leader being
     * exempt, marines not shooting each other until their leaders fall out - is decided separately and earlier, so a
     * player cannot whitelist their way into a squad that shoots its own employer.
     */
    public boolean shouldEngage(Entity entity) {
        return isSelected(entity) == whitelist;
    }

    private boolean isSelected(Entity entity) {
        if (customTypes.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) {
            return true;
        }

        for (var category : categories) {
            if (category.matches(entity)) {
                return true;
            }
        }

        return false;
    }

    public boolean isWhitelist() {
        return whitelist;
    }

    public void setWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
    }

    public boolean hasCategory(SentryTargetCategory category) {
        return categories.contains(category);
    }

    public void setCategory(SentryTargetCategory category, boolean selected) {
        if (selected) {
            categories.add(category);
        } else {
            categories.remove(category);
        }
    }

    public Set<ResourceLocation> getCustomTypes() {
        return customTypes;
    }

    public void setCustomTypes(Collection<ResourceLocation> newCustomTypes) {
        customTypes.clear();
        customTypes.addAll(newCustomTypes);
    }

    /**
     * A copy that shares nothing, so the screen can edit freely and only what is applied reaches the marine.
     */
    public SentryTargetFilter copy() {
        var copy = new SentryTargetFilter();

        copy.whitelist = whitelist;
        copy.categories.clear();
        copy.categories.addAll(categories);
        copy.customTypes.addAll(customTypes);

        return copy;
    }

    public CompoundTag save() {
        var compoundTag = new CompoundTag();
        var categoryList = new ListTag();
        var customList = new ListTag();

        for (var category : categories) {
            categoryList.add(StringTag.valueOf(category.getId()));
        }

        for (var customType : customTypes) {
            customList.add(StringTag.valueOf(customType.toString()));
        }

        compoundTag.putBoolean(NBT_WHITELIST, whitelist);
        compoundTag.put(NBT_CATEGORIES, categoryList);
        compoundTag.put(NBT_CUSTOM, customList);

        return compoundTag;
    }

    public static SentryTargetFilter load(CompoundTag compoundTag) {
        var filter = new SentryTargetFilter();

        filter.whitelist = compoundTag.getBoolean(NBT_WHITELIST);
        filter.categories.clear();

        var categoryList = compoundTag.getList(NBT_CATEGORIES, Tag.TAG_STRING);

        for (var i = 0; i < categoryList.size(); i++) {
            var category = SentryTargetCategory.byId(categoryList.getString(i));

            if (category != null) {
                filter.categories.add(category);
            }
        }

        var customList = compoundTag.getList(NBT_CUSTOM, Tag.TAG_STRING);

        for (var i = 0; i < customList.size(); i++) {
            var resourceLocation = ResourceLocation.tryParse(customList.getString(i));

            if (resourceLocation != null) {
                filter.customTypes.add(resourceLocation);
            }
        }

        return filter;
    }

    public static List<SentryTargetCategory> selectableCategories() {
        return List.of(SentryTargetCategory.values());
    }
}
