package com.human.common.gameplay.entity.living.human.marine;

import com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Oct 5 - the comics alliance. [stated] "like in the comics the yautja and humans will team up to fight the aliens. if
 * the marines and yautja do encounter aliens allow them both to focus on the aliens and not eachother then have a 90s
 * truce period where the two wont attack eachother."
 * <p>
 * A marine within {@value #CONTACT_RADIUS} blocks of a live, hostile xenomorph - or fighting one - is in ALIEN CONTACT.
 * From then until {@value #TRUCE_SECONDS} seconds after the last contact it and its whole squad (shared through the
 * squad, the same way callouts are) treat yautja as neither a threat nor a target: they drop a yautja they were
 * fighting, never shoot through one, and ignore a stray blow from one. Players are not part of it - only marines and
 * yautja. avp_predator runs the mirror image, so each side honours it on its own and neither mod needs the other.
 * </p>
 * <p>
 * Cost: one bounding-box query per marine per second, skipped whenever the marine is already fighting a xeno.
 * </p>
 */
public final class MarineAlienTruce {

    /** How close a xenomorph has to be to count as contact, in blocks. */
    public static final double CONTACT_RADIUS = 24.0;

    /** How long the truce lasts after the last contact, in seconds. */
    public static final int TRUCE_SECONDS = 90;

    /** The same, in ticks. */
    public static final int TRUCE_TICKS = TRUCE_SECONDS * 20;

    private MarineAlienTruce() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@return whether {@code entity} is a yautja} By registry id, so avp_predator does not have to be installed.
     *
     * @param entity the entity
     */
    public static boolean isYautja(Entity entity) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        return "avp_predator".equals(key.getNamespace()) && key.getPath().contains("yautja");
    }

    /**
     * {@return whether {@code entity} is a xenomorph that counts as contact} A live avp_alien mob the marines would
     * hunt - so not a captive, an owned one or an egg.
     *
     * @param entity the candidate
     */
    public static boolean isHostileXeno(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }

        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        return "avp_alien".equals(key.getNamespace())
            && !key.getPath().contains("ovomorph")
            && MarineHuntRules.isHuntedOnSight(entity);
    }

    /**
     * Review pass 1 - contact is a xenomorph nearby, OR a yautja nearby that is itself fighting one. Each side measures
     * from where it stands, so without the second half a marine twenty blocks from the xenos honoured the truce while a
     * yautja thirty blocks out did not - and the marine, holding fire, would have been the only one keeping it. Seeing
     * the other side fight the aliens counts, on both sides.
     */
    private static boolean isContact(LivingEntity entity) {
        return isHostileXeno(entity)
            || (isYautja(entity)
                && entity instanceof net.minecraft.world.entity.Mob mob
                && mob.getTarget() != null
                && isXeno(mob.getTarget()));
    }

    private static boolean isXeno(Entity entity) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        return "avp_alien".equals(key.getNamespace()) && !key.getPath().contains("ovomorph");
    }

    /**
     * Refreshes this marine's contact, about once a second (called from the marine's field-craft tick).
     *
     * @param marine the marine
     */
    public static void tick(Marine marine) {
        // [stated] Oct 13 - "its actually ok if the yautja truce doesnt apply to the WY units": W-Y soldiers never
        // join it, so they skip the contact search altogether.
        if (!takesPart(marine)) {
            return;
        }

        var now = marine.level().getGameTime();
        var target = marine.getTarget();
        var contact = target != null && isHostileXeno(target);

        if (!contact) {
            contact = !marine.level()
                .getEntitiesOfClass(
                    LivingEntity.class,
                    marine.getBoundingBox().inflate(CONTACT_RADIUS),
                    MarineAlienTruce::isContact
                )
                .isEmpty();
        }

        if (!contact) {
            return;
        }

        marine.setAlienTruceUntil(now + TRUCE_TICKS);
        MarineSquads.noteAlienContact(marine, now + TRUCE_TICKS);

        // A yautja it was fighting is let go - unless that yautja is still going after marines (see spares).
        if (target != null && spares(marine, target)) {
            marine.setTarget(null);
        }
    }

    /**
     * {@return whether this marine is honouring the truce right now} Its own contact or its squad's.
     *
     * @param marine the marine
     */
    public static boolean isActive(Marine marine) {
        if (!takesPart(marine)) {
            return false;
        }

        var now = marine.level().getGameTime();

        return now < marine.alienTruceUntil() || now < MarineSquads.alienContactUntil(marine);
    }

    /**
     * {@return whether the truce spares {@code other} from this marine} True for a yautja while the truce holds -
     * unless that yautja is itself going after a marine.
     * <p>
     * ⚠ Pre-merge review: each side measures contact from where it stands, so a yautja further from the xenos can be
     * outside the truce while this marine is inside it. A yautja honouring the truce never keeps a marine as its target
     * (avp_predator drops it at once), so one that DOES is not honouring it - and without this exception the marine
     * would have held its fire while being cut down.
     * </p>
     *
     * @param marine the marine
     * @param other  the candidate
     */
    public static boolean spares(Marine marine, Entity other) {
        if (!isYautja(other) || !isActive(marine)) {
            return false;
        }

        return !(other instanceof net.minecraft.world.entity.Mob mob
            && mob.getTarget() != null
            && isMarine(mob.getTarget()));
    }

    /**
     * A USCM soldier (the marine or the tactical marine) or a marine dog: a yautja going after one of these is not
     * honouring the truce. Oct 13: this matched ids starting with "marine", which missed the tactical marine; W-Y
     * soldiers are outside the truce by ruling, so a yautja hunting one does not break it.
     */
    private static boolean isMarine(Entity entity) {
        return (entity instanceof Marine soldier && takesPart(soldier))
            || entity instanceof com.human.common.gameplay.entity.living.dog.MarineDog;
    }

    /** USCM soldiers take part in the truce; Weyland-Yutani soldiers do not ([stated] Oct 13). */
    private static boolean takesPart(Marine marine) {
        return marine.faction() == com.human.common.gameplay.fabrication.Faction.USCM;
    }
}
