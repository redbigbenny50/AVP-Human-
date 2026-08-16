package com.human.common.gameplay.entity.living.human.marine.sentry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The coarse groups a sentry's target filter is written in terms of.
 * <p>
 * Mostly vanilla's own {@link MobCategory}, which is the useful part: every entity in the game declares one, including
 * every modded entity, so a filter written here keeps working in a modpack nobody anticipated.
 * <p>
 * Two of vanilla's eight are deliberately not offered. MISC is boats, armour stands and item frames - a sentry shooting
 * furniture is nobody's intent - and AXOLOTLS exists only as a spawn-cap quirk. {@link #PLAYERS} is ours: players have
 * no meaningful category of their own (they are MISC), and "shoot trespassers" is a thing someone will want.
 */
public enum SentryTargetCategory {

    MONSTERS("monsters", MobCategory.MONSTER),
    ANIMALS("animals", MobCategory.CREATURE),
    AMBIENT("ambient", MobCategory.AMBIENT),
    WATER_ANIMALS("water_animals", MobCategory.WATER_CREATURE),
    WATER_AMBIENT("water_ambient", MobCategory.WATER_AMBIENT),
    UNDERGROUND_WATER("underground_water", MobCategory.UNDERGROUND_WATER_CREATURE),
    PLAYERS("players", null);

    private final String id;

    private final @Nullable MobCategory mobCategory;

    SentryTargetCategory(String id, @Nullable MobCategory mobCategory) {
        this.id = id;
        this.mobCategory = mobCategory;
    }

    public String getId() {
        return id;
    }

    /**
     * The translation key for this category's checkbox label.
     */
    public String getTranslationKey() {
        return "gui.avp.marine.sentry.category." + id;
    }

    public boolean matches(Entity entity) {
        if (mobCategory == null) {
            return entity instanceof Player;
        }

        // A player would otherwise fall through to MobCategory.MISC and never match anything, which is why PLAYERS is
        // a category of its own rather than a mapping onto one of vanilla's.
        return !(entity instanceof Player) && entity.getType().getCategory() == mobCategory;
    }

    public static @Nullable SentryTargetCategory byId(String id) {
        for (var category : values()) {
            if (category.id.equals(id)) {
                return category;
            }
        }

        return null;
    }
}
