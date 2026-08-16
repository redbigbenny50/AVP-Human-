package com.human.client.screen.sentry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.human.Human;
import com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetCategory;
import com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Saved sentry configurations, so a player setting up a dozen marines does not fill the same form a dozen times.
 * <p>
 * Deliberately CLIENT-SIDE, in a file next to the game's other config. A favourite is a shortcut for filling in the
 * screen - loading one only populates the widgets, and applying it sends exactly the same packet as editing by hand -
 * so the server never needs to know these exist. That keeps them out of world saves, out of the protocol, and usable on
 * any world or server the player joins.
 * <p>
 * ⚠ The trade-off, worth knowing: favourites live on the machine, not the account. They do not follow a player to
 * another computer.
 */
public class SentryFavorites {

    /**
     * Nine because that is what the row can hold at this panel width, and because a tenth would not fit the mental
     * model of a hotbar-like strip.
     */
    public static final int FAVORITE_COUNT = 9;

    private static final String FILE_NAME = "avp_human_sentry_favorites.json";

    private static final String JSON_WHITELIST = "whitelist";

    private static final String JSON_CATEGORIES = "categories";

    private static final String JSON_CUSTOM = "custom";

    private static final @Nullable SentryTargetFilter[] FAVORITES = new SentryTargetFilter[FAVORITE_COUNT];

    private static boolean loaded;

    public static @Nullable SentryTargetFilter get(int slot) {
        load();

        return slot >= 0 && slot < FAVORITE_COUNT ? FAVORITES[slot] : null;
    }

    public static void set(int slot, @Nullable SentryTargetFilter filter) {
        load();

        if (slot < 0 || slot >= FAVORITE_COUNT) {
            return;
        }

        FAVORITES[slot] = filter == null ? null : filter.copy();

        save();
    }

    /**
     * A short human description of what a slot holds, for its tooltip - otherwise nine identical buttons are a memory
     * test.
     */
    public static String describe(SentryTargetFilter filter) {
        var categories = new ArrayList<String>();

        for (var category : SentryTargetCategory.values()) {
            if (filter.hasCategory(category)) {
                categories.add(category.getId());
            }
        }

        var description = (filter.isWhitelist() ? "Only: " : "All but: ")
            + (categories.isEmpty() ? "nothing" : String.join(", ", categories));

        return filter.getCustomTypes().isEmpty()
            ? description
            : description + " (+" + filter.getCustomTypes().size() + " named)";
    }

    private static void load() {
        if (loaded) {
            return;
        }

        // Set before reading, not after: a malformed file should be reported once and then left alone, not re-read and
        // re-logged on every frame the screen is open.
        loaded = true;

        var path = path();

        if (path == null || !Files.exists(path)) {
            return;
        }

        try {
            var root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();

            for (var slot = 0; slot < FAVORITE_COUNT; slot++) {
                var key = String.valueOf(slot);

                if (root.has(key) && root.get(key).isJsonObject()) {
                    FAVORITES[slot] = fromJson(root.getAsJsonObject(key));
                }
            }
        } catch (Exception exception) {
            Human.LOGGER.error("Could not read sentry favourites from '{}'; starting with none.", path, exception);

            Arrays.fill(FAVORITES, null);
        }
    }

    private static void save() {
        var path = path();

        if (path == null) {
            return;
        }

        var root = new JsonObject();

        for (var slot = 0; slot < FAVORITE_COUNT; slot++) {
            var filter = FAVORITES[slot];

            if (filter != null) {
                root.add(String.valueOf(slot), toJson(filter));
            }
        }

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, root.toString(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Human.LOGGER.error("Could not write sentry favourites to '{}'.", path, exception);
        }
    }

    private static JsonObject toJson(SentryTargetFilter filter) {
        var json = new JsonObject();
        var categories = new JsonArray();
        var custom = new JsonArray();

        for (var category : SentryTargetCategory.values()) {
            if (filter.hasCategory(category)) {
                categories.add(category.getId());
            }
        }

        for (var customType : filter.getCustomTypes()) {
            custom.add(customType.toString());
        }

        json.addProperty(JSON_WHITELIST, filter.isWhitelist());
        json.add(JSON_CATEGORIES, categories);
        json.add(JSON_CUSTOM, custom);

        return json;
    }

    private static SentryTargetFilter fromJson(JsonObject json) {
        var filter = new SentryTargetFilter();

        filter.setWhitelist(!json.has(JSON_WHITELIST) || json.get(JSON_WHITELIST).getAsBoolean());

        for (var category : SentryTargetCategory.values()) {
            filter.setCategory(category, false);
        }

        if (json.has(JSON_CATEGORIES)) {
            for (var element : json.getAsJsonArray(JSON_CATEGORIES)) {
                var category = SentryTargetCategory.byId(element.getAsString());

                if (category != null) {
                    filter.setCategory(category, true);
                }
            }
        }

        var customTypes = new ArrayList<ResourceLocation>();

        if (json.has(JSON_CUSTOM)) {
            for (var element : json.getAsJsonArray(JSON_CUSTOM)) {
                var resourceLocation = ResourceLocation.tryParse(element.getAsString());

                if (resourceLocation != null) {
                    customTypes.add(resourceLocation);
                }
            }
        }

        filter.setCustomTypes(customTypes);

        return filter;
    }

    private static @Nullable Path path() {
        var minecraft = Minecraft.getInstance();

        return minecraft == null ? null : minecraft.gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }

    public static List<Integer> slots() {
        var slots = new ArrayList<Integer>(FAVORITE_COUNT);

        for (var slot = 0; slot < FAVORITE_COUNT; slot++) {
            slots.add(slot);
        }

        return slots;
    }

    private SentryFavorites() {
        throw new UnsupportedOperationException();
    }
}
