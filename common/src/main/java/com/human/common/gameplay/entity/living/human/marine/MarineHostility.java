package com.human.common.gameplay.entity.living.human.marine;

import com.human.common.gameplay.entity.living.dog.MarineDog;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Decides when two marines stop being allies.
 * <p>
 * Marines never shoot each other by default - a marine, a hired marine and someone else's marine all read as the same
 * outfit. What changes that is ENGAGEMENT: once a leader opens fire on the other side, or the other side opens fire on
 * them, the alliance between those two sides is off and everyone involved treats the other as a target.
 * <p>
 * A SIDE is a leader and everyone working for them, or a single unhired marine standing for itself. That is what makes
 * a fight spread the way it should: shoot one unhired marine and your own hired marines join in against it, and it
 * fights back against them rather than only against you. It also keeps two players' squads out of each other's way
 * until one of them actually starts something.
 * <p>
 * ⚠ Nothing here is remembered. Every signal it reads is a vanilla combat field that clears itself after 100 ticks
 * without further violence, so a fight that stops IS over and the two sides go back to ignoring each other. That is the
 * "until retreat" behaviour, and it costs no state and no timer.
 */
public class MarineHostility {

    /**
     * Whether these two have fallen out.
     * <p>
     * Only meaningful between members of the marine outfit; anything else is not its business.
     */
    public static boolean hasBrokenAlliance(Entity source, Entity target) {
        if (!(source instanceof LivingEntity livingSource) || !(target instanceof LivingEntity livingTarget)) {
            return false;
        }

        var sourceSide = sideOf(livingSource);
        var targetSide = sideOf(livingTarget);

        if (sourceSide == null || targetSide == null || Objects.equals(sourceSide, targetSide)) {
            // Same outfit, or something that has no side at all. Never hostile.
            return false;
        }

        return hasEngaged(livingSource, targetSide)
            || hasEngaged(livingTarget, sourceSide)
            || leaderHasEngaged(livingSource, targetSide)
            || leaderHasEngaged(livingTarget, sourceSide);
    }

    /**
     * Whether this one has thrown a punch at, or taken one from, the given side.
     * <p>
     * Aiming counts as well as hitting: a marine lining up on your leader has declared itself, and waiting for it to
     * land the first shot would mean your squad watches it shoot at your leader for free.
     */
    private static boolean hasEngaged(LivingEntity entity, UUID otherSide) {
        if (isOnSide(entity.getLastHurtByMob(), otherSide) || isOnSide(entity.getLastHurtMob(), otherSide)) {
            return true;
        }

        return entity instanceof Mob mob && isOnSide(mob.getTarget(), otherSide);
    }

    /**
     * The same question asked of whoever this one works for. A leader picking a fight commits everyone they have hired,
     * which is the point: the squad follows the leader into it rather than waiting to be shot at individually.
     */
    private static boolean leaderHasEngaged(LivingEntity entity, UUID otherSide) {
        if (!(entity instanceof Marine marine)) {
            return false;
        }

        var leaderOption = marine.getLeader();

        if (leaderOption.isNone() || !(leaderOption.unwrap() instanceof LivingEntity leader)) {
            return false;
        }

        return isOnSide(leader.getLastHurtByMob(), otherSide) || isOnSide(leader.getLastHurtMob(), otherSide);
    }

    /**
     * The side an entity belongs to: its leader if it has one, otherwise itself.
     * <p>
     * Returns null for anything outside the outfit, which is what keeps this from having an opinion about xenomorphs.
     */
    private static @Nullable UUID sideOf(Entity entity) {
        if (entity instanceof Marine marine) {
            return marine.getLeaderUUID().unwrapOr(marine.getUUID());
        }

        if (entity instanceof MarineDog marineDog) {
            // A dog belongs to its marine, and through it to that marine's leader.
            var owner = marineDog.getOwner();

            return owner instanceof Marine ownerMarine
                ? sideOf(ownerMarine)
                : marineDog.getOwnerUUID();
        }

        return null;
    }

    /**
     * Whether a candidate belongs to the given side - either as its leader in person, or as one of its marines.
     */
    private static boolean isOnSide(@Nullable Entity candidate, UUID side) {
        if (candidate == null) {
            return false;
        }

        if (Objects.equals(candidate.getUUID(), side)) {
            return true;
        }

        return Objects.equals(sideOf(candidate), side);
    }

    private MarineHostility() {
        throw new UnsupportedOperationException();
    }
}
