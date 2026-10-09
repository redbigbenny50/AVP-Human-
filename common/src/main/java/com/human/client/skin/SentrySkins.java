package com.human.client.skin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The skins, read from resource packs. [stated] Oct 2: each skin is a JSON beside its texture -
 * {@code assets/<ns>/avp_human/skins/<name>.json}:
 *
 * <pre>
 * { "name": "Gold", "targets": ["avp_human:sentry_turret"], "texture": "avp_human:textures/block/sentry_turret_gold.png",
 *   "colour": "#D4AA28" }
 * </pre>
 *
 * A skin whose texture is not in any loaded pack is dropped, so the turret shows the USCM default rather than a missing
 * texture. {@code targets} is there so guns can use the same files later.
 */
public final class SentrySkins extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "avp_human/skins";

    public static final SentrySkins INSTANCE = new SentrySkins();

    public record Skin(
        ResourceLocation texture,
        int colour
    ) {}

    private static Map<ResourceLocation, Skin> skins = Map.of();

    private SentrySkins() {
        super(new com.google.gson.Gson(), DIRECTORY);
    }

    @Override
    protected void apply(
        @NotNull Map<ResourceLocation, JsonElement> files,
        @NotNull ResourceManager manager,
        @NotNull ProfilerFiller profiler
    ) {
        var loaded = new HashMap<ResourceLocation, Skin>();

        for (var file : files.entrySet()) {
            try {
                var json = (JsonObject) file.getValue();
                var texture = ResourceLocation.parse(json.get("texture").getAsString());

                // A skin without its texture falls back to the default rather than drawing the missing-texture checker.
                if (manager.getResource(texture).isEmpty()) {
                    continue;
                }

                var colour = json.has("colour") ? Integer.parseInt(json.get("colour").getAsString().replace("#", ""), 16) : 0xFFFFFF;
                loaded.put(file.getKey(), new Skin(texture, colour));
            } catch (RuntimeException exception) {
                com.human.Human.LOGGER.warn("Skipping sentry skin {}: {}", file.getKey(), exception.getMessage());
            }
        }

        skins = Map.copyOf(loaded);
    }

    /** The texture for {@code skin}, or {@code fallback} when it has none (no skin, or one that did not load). */
    public static ResourceLocation texture(@Nullable ResourceLocation skin, ResourceLocation fallback) {
        var found = skin == null ? null : skins.get(skin);
        return found == null ? fallback : found.texture();
    }

    /** The colour a Skin Kit is tinted, or white. */
    public static int colour(@Nullable ResourceLocation skin) {
        var found = skin == null ? null : skins.get(skin);
        return found == null ? 0xFFFFFFFF : 0xFF000000 | found.colour();
    }
}
