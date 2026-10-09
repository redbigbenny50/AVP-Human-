package com.human.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client-only display preferences, read once from {@code config/avp_human-client.json}.
 * <p>
 * ⚠ CLIENT-ONLY BY DESIGN. Nothing here may ever affect damage, spawning or anything a server arbitrates - these are
 * preferences about what a player looks at, so a server has no business in them and they need no syncing.
 * </p>
 * <p>
 * ⚠ MISSING FILE IS NOT AN ERROR. First run has no file; every option falls back to its default and the file is written
 * so the player has something to edit. A malformed file is also non-fatal - defaults win and the game keeps running
 * rather than dying over a stray comma in a cosmetic setting.
 * </p>
 */
public final class HumanClientConfig {

    private static final String FILE_NAME = "avp_human-client.json";

    private static final String KEY_SMART_HUD_BOOT_ANIMATION = "smart_hud_boot_animation";

    private static final String KEY_SMART_HUD_OVERHEAT_FLICKER = "smart_hud_overheat_flicker";

    private static boolean smartHudBootAnimation = true;

    private static boolean smartHudOverheatFlicker = true;

    private static boolean loaded;

    /**
     * ⭐ BOTH TOGGLES EXIST BECAUSE THESE TWO EFFECTS ARE THE MOST LIKELY TO GRATE. A half-second startup sweep is
     * characterful the first fifty times and tiresome for someone who swaps weapons constantly, and a flicker during
     * the overheat lockout is exactly the sort of thing that bothers a minority of players a great deal.
     */
    public static boolean smartHudBootAnimation() {
        load();
        return smartHudBootAnimation;
    }

    public static boolean smartHudOverheatFlicker() {
        load();
        return smartHudOverheatFlicker;
    }

    private static synchronized void load() {
        if (loaded) {
            return;
        }

        loaded = true;

        var path = configPath();

        if (path == null) {
            return;
        }

        try {
            if (Files.exists(path)) {
                var root = JsonParser.parseString(Files.readString(path));

                if (root.isJsonObject()) {
                    var object = root.getAsJsonObject();
                    smartHudBootAnimation = readBoolean(object, KEY_SMART_HUD_BOOT_ANIMATION, smartHudBootAnimation);
                    smartHudOverheatFlicker = readBoolean(object, KEY_SMART_HUD_OVERHEAT_FLICKER, smartHudOverheatFlicker);
                }
            }

            write(path);
        } catch (Exception exception) {
            // Cosmetic settings are never worth a crash. Defaults are already in the fields.
        }
    }

    private static boolean readBoolean(JsonObject object, String key, boolean fallback) {
        if (!object.has(key)) {
            return fallback;
        }

        var element = object.get(key);

        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean() ? element.getAsBoolean() : fallback;
    }

    private static void write(Path path) throws IOException {
        var object = new JsonObject();
        object.addProperty(KEY_SMART_HUD_BOOT_ANIMATION, smartHudBootAnimation);
        object.addProperty(KEY_SMART_HUD_OVERHEAT_FLICKER, smartHudOverheatFlicker);

        Files.createDirectories(path.getParent());
        Files.writeString(path, object.toString());
    }

    private static Path configPath() {
        var minecraft = Minecraft.getInstance();

        return minecraft == null ? null : minecraft.gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }

    private HumanClientConfig() {
        throw new UnsupportedOperationException();
    }
}
