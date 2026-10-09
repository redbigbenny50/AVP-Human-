package com.human.common.gameplay.power.upgrade;

import java.util.EnumSet;
import java.util.Set;

/**
 * The machine and sentry upgrade cards. [stated] Oct 2: every machine has four visible upgrade slots holding ONE card
 * each; a STACKABLE card adds up across slots (four speed cards are four times the effect), a single-use MODULE counts
 * once and a second copy is refused, and cards in the same exclusive GROUP (the sentry's ammo cards) cannot be fitted
 * together.
 * <p>
 * The battery's own cards (capacity, transfer, emergency, redstone, alarm, armoured) are {@link BatteryUpgrade}; the
 * hub chip is {@code HubChipItem}. They keep working exactly as before.
 * </p>
 */
public enum UpgradeCard {

    /** [stated] Oct 2: 1.5x speed per card - four cards is 6x - for +5% energy per item per card. */
    SPEED("speed", true, null, Target.MACHINE),

    /** [stated] Oct 2: -25% power draw per card, multiplied - four cards is about a third of the energy per item. */
    EFFICIENCY("efficiency", true, null, Target.MACHINE),

    // [stated] Oct 2: the turret takes ONE of each card - none of them stack.

    /** +32 blocks of range: 32 -> 64. */
    RANGE("range", false, null, Target.SENTRY),

    /** +45 degrees each side: a 90-degree field of fire becomes 180. */
    WIDE_CONE("wide_cone", false, null, Target.SENTRY),

    /** Twice as long firing before the turret overheats: 10 s -> 20 s. */
    COOLING("cooling", false, null, Target.SENTRY),

    /** As tough as obsidian: immune to explosions, half damage from everything else. */
    DEFENSE("defense", false, null, Target.SENTRY),

    /** [stated] Oct 2: fires caseless bullets at 1.5x damage. One ammo card at a time. */
    CASELESS_AMMO("caseless_ammo", false, "ammo", Target.SENTRY),

    /** [stated] Oct 2: fires rockets every 50 ticks; immune to its own blasts, which break no blocks. One ammo card. */
    ROCKET_AMMO("rocket_ammo", false, "ammo", Target.SENTRY);

    /** What a card can be fitted to. */
    public enum Target {
        MACHINE,
        SENTRY
    }

    /** Upgrade slots on every machine and on the sentry. */
    public static final int SLOTS = 4;

    // ------------------------------------------------------------------------------------------------------------
    // The numbers [stated] Oct 2. Machine draw and recipe times scale from these; nothing else hard-codes them.
    // ------------------------------------------------------------------------------------------------------------

    /** Speed added by each speed card: 1 card 1.5x, 4 cards 6x. */
    public static final double SPEED_PER_CARD = 1.5;

    /** Extra energy per item for each speed card: the draw rises a little faster than the speed. */
    public static final double SPEED_ENERGY_PER_CARD = 0.05;

    /** Each efficiency card keeps this share of the draw (-25%). */
    public static final double EFFICIENCY_KEEP_PER_CARD = 0.75;

    public static final int RANGE_BONUS = 32;

    public static final int CONE_BONUS_DEGREES = 45;

    /** The cooling card doubles the time before overheating. */
    public static final double COOLING_MULTIPLIER = 2.0;

    /** The defence card halves non-explosion damage; explosions do nothing. */
    public static final double DEFENSE_DAMAGE_MULTIPLIER = 0.5;

    public static final double CASELESS_DAMAGE_MULTIPLIER = 1.5;

    public static final int ROCKET_COOLDOWN_TICKS = 50;

    private final String id;

    private final boolean stackable;

    private final String group;

    private final Set<Target> targets;

    UpgradeCard(String id, boolean stackable, String group, Target first, Target... rest) {
        this.id = id;
        this.stackable = stackable;
        this.group = group;
        this.targets = EnumSet.of(first, rest);
    }

    public String id() {
        return id;
    }

    /** True if more copies add up; false for a module, where a second copy is refused. */
    public boolean stackable() {
        return stackable;
    }

    /** Cards sharing a non-null group cannot be fitted together (the sentry's ammo cards). */
    public String group() {
        return group;
    }

    public boolean fits(Target target) {
        return targets.contains(target);
    }

    /** How much faster a machine works with {@code cards} speed cards: 1, 1.5, 3, 4.5, 6. */
    public static double speedMultiplier(int cards) {
        return cards <= 0 ? 1.0 : SPEED_PER_CARD * cards;
    }

    /**
     * How much a machine's power draw is multiplied by, for its speed and efficiency cards together. The speed term
     * keeps the energy per item at +5% per speed card; the efficiency term takes 25% off per card, multiplied.
     */
    public static double drawMultiplier(int speedCards, int efficiencyCards) {
        var speed = speedMultiplier(speedCards) * (1.0 + SPEED_ENERGY_PER_CARD * Math.max(0, speedCards));
        return speed * Math.pow(EFFICIENCY_KEEP_PER_CARD, Math.max(0, efficiencyCards));
    }
}
