package com.human.common.gameplay.item.gun;

import com.human.common.registry.init.item.HumanItems;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Classifies a gun's ammunition into a CALIBER, for armour rules that care what hit them.
 * <p>
 * Derived from the ammunition item rather than stored on {@link GunConfig}, so no gun definition has to be edited and a
 * gun added later is classified automatically by what it eats.
 * </p>
 * <p>
 * ⚠ The names are the wire format shared with avp_alien - see {@code BulletResistance}. Changing one means changing
 * both, so they are constants rather than an enum whose ordinal or name could drift.
 * </p>
 */
public final class GunCalibers {

    public static final String SMALL = "small";

    public static final String MEDIUM = "medium";

    public static final String HEAVY = "heavy";

    public static final String CASELESS = "caseless";

    public static final String SHELL = "shell";

    /** Rockets and fuel - not "bullets" for an armour rule. The drums classify by the round they feed. */
    public static final String OTHER = "other";

    private GunCalibers() {}

    public static String of(@Nullable GunConfig gunConfig) {
        if (gunConfig == null || gunConfig.ammunitionItemSupplier() == null) {
            return OTHER;
        }

        return of(gunConfig.ammunitionItemSupplier().get().asItem());
    }

    public static String of(Item ammunitionItem) {
        if (ammunitionItem == HumanItems.SMALL_BULLET.get()) {
            return SMALL;
        }
        if (ammunitionItem == HumanItems.MEDIUM_BULLET.get()) {
            return MEDIUM;
        }
        if (ammunitionItem == HumanItems.HEAVY_BULLET.get()) {
            return HEAVY;
        }
        if (ammunitionItem == HumanItems.CASELESS_BULLET.get()) {
            return CASELESS;
        }
        if (ammunitionItem == HumanItems.SHOTGUN_SHELL.get()) {
            return SHELL;
        }

        // ⭐⭐ THE DRUMS CLASSIFY BY WHAT THEY FEED, NOT BY BEING A DRUM. [stated] "smartgun counts as caseless and
        // oldpainless would be medium actually." A drum is a magazine, not a round - the smartgun still fires
        // caseless and Old Painless still fires a medium round, so armour must see them that way. Deriving the
        // caliber purely from the item would have filed both under OTHER and exempted the two highest-volume
        // weapons in the game from every armour rule, which is the opposite of what a heavy is for.
        if (ammunitionItem == HumanItems.DRUM_CARTRIDGE.get()) {
            return CASELESS;
        }
        if (ammunitionItem == HumanItems.DRUM_CANNISTER.get()) {
            return MEDIUM;
        }

        return OTHER;
    }
}
