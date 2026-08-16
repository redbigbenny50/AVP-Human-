package com.human.client.input;

import com.human.common.registry.init.item.HumanGunItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

/** Per-client ADS adjustments made with the in-game sight calibration screen. */
public final class AdsSightCalibration {

    // Baked-in values from the in-game sight calibration tool.
    private static final float F903WE_BASE_OFFSET = 0.017F;

    private static final float M3712_BASE_OFFSET = 0.060F;

    private static final String F903WE_KEY = "f903we";

    private static final String M3712_KEY = "m3712";

    private static float f903weAdjustment;

    private static float m3712Adjustment;

    private static boolean loaded;

    public static boolean supports(Item item) {
        return item == HumanGunItems.F903WE_RIFLE.get() || item == HumanGunItems.M37_12_SHOTGUN.get();
    }

    public static float horizontalOffset(Item item) {
        load();
        if (item == HumanGunItems.F903WE_RIFLE.get()) {
            return F903WE_BASE_OFFSET + f903weAdjustment;
        }
        if (item == HumanGunItems.M37_12_SHOTGUN.get()) {
            return M3712_BASE_OFFSET + m3712Adjustment;
        }
        return 0.0F;
    }

    public static float adjustment(Item item) {
        load();
        return item == HumanGunItems.F903WE_RIFLE.get() ? f903weAdjustment : m3712Adjustment;
    }

    public static void setAdjustment(Item item, float adjustment) {
        if (item == HumanGunItems.F903WE_RIFLE.get()) {
            f903weAdjustment = adjustment;
        } else if (item == HumanGunItems.M37_12_SHOTGUN.get()) {
            m3712Adjustment = adjustment;
        }
    }

    public static void save() {
        load();
        var properties = new Properties();
        properties.setProperty(F903WE_KEY, Float.toString(f903weAdjustment));
        properties.setProperty(M3712_KEY, Float.toString(m3712Adjustment));
        try (var output = Files.newOutputStream(configFile())) {
            properties.store(output, "AVP-Human ADS sight calibration");
        } catch (IOException ignored) {
            // The calibration still works for this session if the local config cannot be written.
        }
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        var properties = new Properties();
        try (var input = Files.newInputStream(configFile())) {
            properties.load(input);
            f903weAdjustment = Float.parseFloat(properties.getProperty(F903WE_KEY, "0"));
            m3712Adjustment = Float.parseFloat(properties.getProperty(M3712_KEY, "0"));
        } catch (IOException | NumberFormatException ignored) {
            // Defaults intentionally keep the original sight positions.
        }
    }

    private static java.nio.file.Path configFile() throws IOException {
        var directory = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        Files.createDirectories(directory);
        return directory.resolve("avp_human_ads_calibration.properties");
    }

    private AdsSightCalibration() {}
}
