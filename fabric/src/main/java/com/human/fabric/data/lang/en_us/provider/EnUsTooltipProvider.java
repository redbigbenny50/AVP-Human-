package com.human.fabric.data.lang.en_us.provider;

import com.human.common.data.HumanTooltipTranslationKeys;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import java.util.function.Consumer;

public class EnUsTooltipProvider {

    public static final Consumer<FabricLanguageProvider.TranslationBuilder> CONSUMER = builder -> {
        builder.add("message.avp.reload_too_early", "Your training tells you it's too early to reload.");

        builder.add("gui.avp.marine.mode.follow", "Following");
        builder.add("gui.avp.marine.mode.follow.tooltip", "This marine is following you. Click to make it hold position.");
        builder.add("gui.avp.marine.mode.hold", "Holding");
        builder.add("gui.avp.marine.mode.hold.tooltip", "This marine is holding position. Click to make it follow you.");

        builder.add("gui.avp.marine.sentry.on", "Sentry: On");
        builder.add("gui.avp.marine.sentry.off", "Sentry: Off");
        builder.add("gui.avp.marine.sentry.on.tooltip", "Guarding this position. Click to stand down and follow you again.");
        builder.add("gui.avp.marine.sentry.off.tooltip", "Post this marine here to guard the area. It will stay within 16 blocks.");
        builder.add("gui.avp.marine.sentry.configure", "\u2699");
        builder.add("gui.avp.marine.sentry.configure.tooltip", "Choose what this marine shoots while on sentry.");
        builder.add("gui.avp.marine.sentry.title", "Sentry Targets");
        builder.add("gui.avp.marine.sentry.whitelist", "Only shoot these");
        builder.add("gui.avp.marine.sentry.blacklist", "Shoot all but these");
        builder.add(
            "gui.avp.marine.sentry.whitelist.tooltip",
            "Ticked groups are the only things this marine will engage. Click to invert."
        );
        builder.add(
            "gui.avp.marine.sentry.blacklist.tooltip",
            "Ticked groups are left alone; everything else is engaged. Click to invert."
        );
        builder.add("gui.avp.marine.sentry.apply", "Apply");
        builder.add("gui.avp.marine.sentry.cancel", "Back");
        builder.add("gui.avp.marine.sentry.category.monsters", "Monsters");
        builder.add("gui.avp.marine.sentry.category.animals", "Animals");
        builder.add("gui.avp.marine.sentry.category.ambient", "Ambient");
        builder.add("gui.avp.marine.sentry.category.water_animals", "Water animals");
        builder.add("gui.avp.marine.sentry.category.water_ambient", "Water ambient");
        builder.add("gui.avp.marine.sentry.category.underground_water", "Underground water");
        builder.add("gui.avp.marine.sentry.category.players", "Players");
        builder.add("gui.avp.marine.sentry.search", "Search mobs...");
        builder.add("gui.avp.marine.sentry.named", "Named mobs");
        builder.add("gui.avp.marine.sentry.favorites", "Favourites");
        builder.add("gui.avp.marine.sentry.favorite.empty", "Empty. Shift-click to save the current setup here.");
        builder.add("gui.avp.marine.sentry.favorite.overwrite", "\nShift-click to overwrite.");
        builder.add("gui.avp.marine.end_contract", "Dismiss");
        builder.add("gui.avp.marine.end_contract.tooltip", "End this marine's contract. It keeps its issued gear and returns to patrol.");
        builder.add("gui.avp.marine.end_contract.blocked.tooltip", "Take your own items out of the pack before dismissing this marine.");
        builder.add("tooltip.avp.accuracy", "Accuracy: ");
        builder.add("tooltip.avp.ammunition", "Ammo: ");
        builder.add("tooltip.avp.ammunition_type", "Fires: ");
        builder.add("tooltip.avp.damage", "Damage: ");
        builder.add("tooltip.avp.fire_mode", "Fire Mode: ");
        builder.add("tooltip.avp.fire_rate", "Fire Rate: ");
        builder.add("tooltip.avp.knockback", "Knockback: ");
        builder.add("tooltip.avp.recoil", "Recoil: ");
        builder.add("tooltip.avp.capacity", "Capacity: ");

        builder.add(HumanTooltipTranslationKeys.EFFECT_AUTO_EQUIP_ARMOR_SET, "Auto-Equips Armor Sets");
        builder.add(HumanTooltipTranslationKeys.EFFECT_AUTO_EQUIP_ARMOR_STAND_ARMOR_SET, "Auto-Equips Armor Stand Armor Sets");
        builder.add(HumanTooltipTranslationKeys.EFFECT_AUTO_STORE_IRRADIATED_ITEMS, "Irradiated Items Auto-Stored in Chest");
        builder.add(HumanTooltipTranslationKeys.EFFECT_GUNS_AUTO_RELOAD_FROM_CHEST, "Guns Auto-Reload Ammo from Chest");
        builder.add(HumanTooltipTranslationKeys.EFFECT_NEARBY_TURRETS_USE_AMMO_FROM_CHEST, "Nearby Turrets use Ammo from Chest");
        builder.add(HumanTooltipTranslationKeys.EFFECT_RADIATION_RESISTANCE, "Radiation Resistance");

        builder.add(HumanTooltipTranslationKeys.REQUIRES_REDSTONE_POWER, "Redstone Power");
        builder.add(HumanTooltipTranslationKeys.REQUIRES_NEARBY_AMMO_CHEST_WITH_AMMO, "Nearby Ammo Chest with Medium Bullets");
        builder.add(HumanTooltipTranslationKeys.STATUS_NEEDS_REDSTONE, "Needs a redstone signal");
        builder.add(HumanTooltipTranslationKeys.STATUS_NEEDS_AMMO_CHEST, "Needs an Ammo Chest within %s blocks");
        builder.add(HumanTooltipTranslationKeys.STATUS_NEEDS_MEDIUM_BULLETS, "Ammo Chest has no Medium Bullets");
        builder.add(HumanTooltipTranslationKeys.STATUS_READY, "Powered and supplied");
        builder.add(HumanTooltipTranslationKeys.USE_CHECK_TURRET_STATUS, "Right-click to check what it needs");
        builder.add(HumanTooltipTranslationKeys.USE_PICK_UP_TURRET, "Sneak + right-click to pick it back up");
    };
}
