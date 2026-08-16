package com.human.common.data;

import com.human.Human;
import org.jetbrains.annotations.NotNull;

public class HumanTooltipTranslationKeys {

    public static final String EFFECT_AUTO_EQUIP_ARMOR_SET = create("auto_equip_armor_set");

    public static final String EFFECT_AUTO_EQUIP_ARMOR_STAND_ARMOR_SET = create("auto_equip_armor_stand_armor_set");

    public static final String EFFECT_AUTO_STORE_IRRADIATED_ITEMS = create("auto_store_irradiated_items");

    public static final String EFFECT_GUNS_AUTO_RELOAD_FROM_CHEST = create("guns_auto_reload_from_chest");

    public static final String EFFECT_NEARBY_TURRETS_USE_AMMO_FROM_CHEST = create("nearby_turrets_use_ammo_from_chest");

    public static final String EFFECT_RADIATION_RESISTANCE = create("radiation_resistance");

    public static final String REQUIRES_NEARBY_AMMO_CHEST_WITH_AMMO = create("requires_nearby_ammo_chest_with_ammo");

    public static final String STATUS_NEEDS_AMMO_CHEST = create("status.needs_ammo_chest");

    public static final String STATUS_NEEDS_MEDIUM_BULLETS = create("status.needs_medium_bullets");

    public static final String STATUS_NEEDS_REDSTONE = create("status.needs_redstone");

    public static final String STATUS_READY = create("status.ready");

    public static final String USE_CHECK_TURRET_STATUS = create("use.check_turret_status");

    public static final String USE_PICK_UP_TURRET = create("use.pick_up_turret");

    public static final String REQUIRES_REDSTONE_POWER = create("requires_redstone_power");

    private static @NotNull String create(String name) {
        return "tooltip." + Human.MOD_ID + "." + name;
    }
}
