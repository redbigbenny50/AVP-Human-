package com.human.common.gameplay.entity.living.human.marine.ai;

import com.blib.api.common.entity.v1.BLibEntityPredicates;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * One marine's target scan: which enemies it can see right now, nearest first.
 * <p>
 * 🚨 Oct 5 - THE COST IT REPLACES, MEASURED BY {@code /blib perf}. The old sensor took every living thing in the
 * marine's 40-block sense cache, ran the full threat check on each, and sight-checked (a raycast) every one that passed
 * - on every tick, for every marine. At night beside a camp that is dozens of raycasts per marine per tick: one marine
 * measured 384 µs of AI per tick with no path searches at all.
 * </p>
 * <p>
 * ⭐ WHAT IT DOES INSTEAD. All the cheap rules first (allies, leader, cloak, sentry filter, self defence, the hunt
 * rules); then the survivors are sight-checked NEAREST FIRST, stopping at {@link #MAX_VISIBLE_TARGETS} in sight or
 * {@link #MAX_SIGHT_CHECKS} raycasts. The answer is kept for {@link #SCAN_INTERVAL_TICKS} ticks; in between, only
 * targets that died or despawned are dropped. A fresh scan happens at once when the marine is hit, or when the list it
 * held has emptied. Each marine's scan tick is offset by its entity id, so a camp does not all scan on the same tick.
 * </p>
 * <p>
 * [stated] Oct 5: "lets give it a try at youre preference" - and 6 rather than 8 visible targets. Six is still more
 * than the four at which weapon choice stops caring how big the crowd is (GunStrategy's crowd score), and the marine
 * always shoots the nearest anyway.
 * </p>
 * <p>
 * Server thread only, one instance per marine. Switched off by {@code /gamerule humanMarineTargetScanCap false}.
 * </p>
 */
public final class MarineTargetScan {

    /** How long a scan's answer is reused: a quarter of a second. */
    public static final int SCAN_INTERVAL_TICKS = 5;

    /** Stop once this many enemies are known to be in sight. */
    public static final int MAX_VISIBLE_TARGETS = 6;

    /** And never spend more than this many sight raycasts on one scan, however many are hidden. */
    public static final int MAX_SIGHT_CHECKS = 12;

    private List<LivingEntity> targets = List.of();

    private boolean hasScanned;

    private int lastScanTick;

    private int lastHurtByMobTimestamp;

    /** A fresh scan state for one marine; its first read scans at once. */
    public MarineTargetScan() {}

    /**
     * {@return the enemies this marine can see, nearest first, at most {@link #MAX_VISIBLE_TARGETS}}
     *
     * @param marine     the marine this scan belongs to
     * @param isEligible every targeting rule EXCEPT line of sight - this class does the sight checks
     */
    public List<LivingEntity> sense(Marine marine, BiPredicate<Marine, LivingEntity> isEligible) {
        var now = marine.tickCount;

        // Oct 24 - a facehugged marine sees nothing to shoot at (AbstractHuman.isFacehugged).
        if (com.human.common.gameplay.entity.living.human.AbstractHuman.isFacehugged(marine)) {
            targets = List.of();
            hasScanned = false;
            return targets;
        }

        if (hasScanned && lastScanTick == now) {
            return targets;
        }

        if (hasScanned && !isScanDue(marine, now)) {
            if (targets.isEmpty()) {
                return targets;
            }

            var stillThere = withoutTheDead(targets);

            // ⚠ Rescan the moment a held list runs out - a marine that just killed its last target should find the next
            // one now, not up to a quarter second later. An empty list that was ALREADY empty waits for the interval,
            // or a quiet camp would scan every tick.
            if (!stillThere.isEmpty()) {
                targets = stillThere;
                return targets;
            }
        }

        targets = scan(marine, isEligible);
        hasScanned = true;
        lastScanTick = now;
        lastHurtByMobTimestamp = marine.getLastHurtByMobTimestamp();

        return targets;
    }

    private boolean isScanDue(Marine marine, int now) {
        // Hit since the last scan: answer at once.
        if (marine.getLastHurtByMobTimestamp() != lastHurtByMobTimestamp) {
            return true;
        }

        // The staggered slot, with a backstop in case the slot was skipped (the sensor is not read every tick).
        return Math.floorMod(now + marine.getId(), SCAN_INTERVAL_TICKS) == 0 || now - lastScanTick >= SCAN_INTERVAL_TICKS;
    }

    private static List<LivingEntity> scan(Marine marine, BiPredicate<Marine, LivingEntity> isEligible) {
        var eligible = new ArrayList<LivingEntity>();

        for (var candidate : marine.getEntitySenseCache().getByClass(LivingEntity.class)) {
            if (BLibEntityPredicates.isAlive(candidate) && isEligible.test(marine, candidate)) {
                eligible.add(candidate);
            }
        }

        if (eligible.isEmpty()) {
            return List.of();
        }

        eligible.sort(Comparator.comparingDouble(marine::distanceToSqr));

        var visible = new ArrayList<LivingEntity>(Math.min(eligible.size(), MAX_VISIBLE_TARGETS));
        var sightChecks = 0;

        for (var candidate : eligible) {
            if (visible.size() >= MAX_VISIBLE_TARGETS || sightChecks >= MAX_SIGHT_CHECKS) {
                break;
            }

            sightChecks++;

            if (marine.getSensing().hasLineOfSight(candidate)) {
                visible.add(candidate);
            }
        }

        return List.copyOf(visible);
    }

    private static List<LivingEntity> withoutTheDead(List<LivingEntity> held) {
        for (var target : held) {
            if (!BLibEntityPredicates.isAlive(target) || target.isRemoved()) {
                var survivors = new ArrayList<LivingEntity>(held.size());

                for (var other : held) {
                    if (BLibEntityPredicates.isAlive(other) && !other.isRemoved()) {
                        survivors.add(other);
                    }
                }

                return List.copyOf(survivors);
            }
        }

        return held;
    }
}
