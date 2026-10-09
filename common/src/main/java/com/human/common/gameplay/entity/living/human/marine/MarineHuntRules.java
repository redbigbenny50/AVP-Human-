package com.human.common.gameplay.entity.living.human.marine;

import com.human.common.gameplay.entity.living.human.AbstractHuman;
import com.human.common.registry.tag.HumanEntityTypeTags;
import com.human.compatibility.avp_alien.AlienCaptives;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Which creatures a marine treats as hostile WITHOUT anyone having listed them - so a third-party mod's monsters and
 * bosses are hunted on sight with no tag work.
 * <p>
 * [stated] Oct 5: "is there a way we can make them attack any monster or boss even when its a 3rd party mod we dont
 * have preproggrammed in" - then "yes and yes" to bosses and to the smart-gun HUD using the same rule.
 * </p>
 * <p>
 * ⭐ THREE SIGNALS EVERY MOB ALREADY CARRIES. {@code Enemy} is vanilla's hostile marker; every subclass of vanilla's
 * {@code Monster} has it automatically, which is nearly every modded hostile, plus slimes, ghasts, phantoms, hoglins,
 * shulkers and the dragon. The MONSTER spawn category is what every hostile mod registers its mob under.
 * {@code c:bosses} is the shared NeoForge/Fabric convention tag for bosses. Any one of the three is enough.
 * </p>
 * <p>
 * ⚠ AND THE EXCLUSIONS, which matter as much - in order:
 * </p>
 * <ul>
 * <li><b>Other humans</b> (marines, colonists). Squads that fall out are handled by the alliance rules; nobody is
 * hunted on sight for being a person.</li>
 * <li><b>avp_human:ignored_by_marines</b> - the datapack switch for anything a pack wants left alone. It ships with the
 * yautja, because their ruling stands: a marine fights one only when it targets them uncloaked or strikes first. That
 * holds even though yautja ARE monsters, which is why this tag is needed at all.</li>
 * <li><b>Tamed or owned mobs.</b></li>
 * <li><b>Neutral mobs that are not angry</b> - endermen, zombified piglins, wolves, bees, iron golems, polar bears.
 * Shooting one for looking at it, or setting off a zombified piglin horde, is not marine behaviour. Once angry they are
 * fair game.</li>
 * <li><b>Captives</b> being carried off by a xenomorph.</li>
 * </ul>
 */
public final class MarineHuntRules {

    /** The NeoForge/Fabric convention boss tag, named by string so nothing loader-specific is needed. */
    private static final TagKey<EntityType<?>> CONVENTION_BOSSES = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("c", "bosses")
    );

    private MarineHuntRules() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@return whether a marine may hunt {@code entity} on sight} - the hated-by-marines tag OR the generic hostile
     * rule, minus every exclusion. Line of sight and the cloak are the caller's business.
     */
    public static boolean isHuntedOnSight(LivingEntity entity) {
        if (isExcluded(entity, true)) {
            return false;
        }

        return entity.getType().is(HumanEntityTypeTags.HATED_BY_MARINES) || isGenericallyHostile(entity);
    }

    /**
     * The smart-gun HUD's version: the same generic rule and exclusions, EXCEPT the marine ignore list. That list is
     * about what a marine picks a fight with; the HUD is a player's targeting aid, and an uncloaked yautja is exactly
     * what it should box.
     */
    public static boolean isHostileForTargeting(LivingEntity entity) {
        return !isExcluded(entity, false) && isGenericallyHostile(entity);
    }

    private static boolean isGenericallyHostile(LivingEntity entity) {
        return entity instanceof Enemy
            || entity.getType().getCategory() == MobCategory.MONSTER
            || entity.getType().is(CONVENTION_BOSSES);
    }

    private static boolean isExcluded(LivingEntity entity, boolean applyMarineIgnoreList) {
        if (entity instanceof Player || entity instanceof AbstractHuman) {
            return true;
        }

        if (applyMarineIgnoreList && entity.getType().is(HumanEntityTypeTags.IGNORED_BY_MARINES)) {
            return true;
        }

        if (entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return true;
        }

        if (entity instanceof NeutralMob neutral && !neutral.isAngry()) {
            return true;
        }

        return AlienCaptives.isCarriedOff(entity);
    }
}
