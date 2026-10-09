package com.human.common.gameplay.entity.living.human.marine.squad;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.item.GunItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Oct 5 - marines fighting as a unit. [stated] "have the ai use tactics like flanking or group dynamics like 1 marine
 * or a group get a powerful enemies attention while the others go behind and shoot it", then "follow your choices and
 * suggestions", "make the anchors shots draw the enemies attention", and for xenos and yautja "anchor might work the
 * first time for a small window but it shouldnt keep working they would catch onto it. make the units know when a
 * tactic doesnt work" - preds fall for it more than aliens.
 * <p>
 * ⭐ ONE BRAIN PER SQUAD, THINKING ONCE A SECOND. A squad is: the marines hired by one player, a patrol under one squad
 * leader, or the marines of one camp. Each marine reports itself every few ticks; whichever reports first in a new
 * second updates the brain. Everything a marine reads from it in between is a map lookup. Cost is a handful of
 * comparisons per squad per second.
 * </p>
 * <p>
 * What the brain decides:
 * </p>
 * <ul>
 * <li><b>Spread and focus</b> - how many squadmates are already on each target; small fry are capped at
 * {@value #MAX_PER_SMALL_TARGET} marines each, a BIG threat gets everyone.</li>
 * <li><b>Anchor and flank</b> - against a big threat one marine anchors (holds its attention from the front and pulls
 * it back with its shots), the rest take flank positions around its sides and back.</li>
 * <li><b>Cover me</b> - a marine starting a reload asks the squad to shoot whatever is on it for {@value #COVER_TICKS}
 * ticks.</li>
 * <li><b>Callouts</b> - whatever hits a marine is known to every squadmate within {@value #CALLOUT_RADIUS} blocks.</li>
 * <li><b>Learning</b> - an anchor pull that does not take is noticed; twice and the squad stops trying it on that enemy
 * and falls back to plain focus fire. Smart enemies only fall for it a set number of times.</li>
 * </ul>
 * Server thread only. State is not saved: a squad re-forms within a second of loading.
 */
public final class MarineSquads {

    /** A big threat: at least this much max health (or a boss, or a yautja). */
    public static final double BIG_THREAT_MAX_HEALTH = 40.0;

    /** Small targets are shared out: at most this many marines on one. */
    public static final int MAX_PER_SMALL_TARGET = 2;

    /** How long a "cover me" lasts, in ticks: three seconds. */
    public static final int COVER_TICKS = 60;

    /** Who hears a callout, in blocks from the marine that was hit. */
    public static final double CALLOUT_RADIUS = 24.0;

    /** How long a callout lasts, in ticks. */
    private static final int CALLOUT_TICKS = 100;

    /**
     * Radio: at most one exchange per squad this often, in ticks - a marine taking hit after hit does not key the radio
     * on every one.
     */
    private static final int RADIO_COOLDOWN_TICKS = 120;

    /** Radio: how many squadmates answer a callout. */
    private static final int RADIO_REPLIES = 2;

    /** How often the squad thinks, in ticks. */
    private static final int THINK_INTERVAL = 20;

    /** A member not heard from for this long has left the squad. */
    private static final int MEMBER_TIMEOUT = 60;

    /** An anchor may pull its target back at most this often, in ticks. */
    private static final int PULL_COOLDOWN = 60;

    /** How long after a pull the squad checks whether it worked, in ticks. */
    private static final int PULL_VERIFY_DELAY = 40;

    /** Pulls that did not take before the squad gives the tactic up on that enemy. */
    private static final int PULL_FAILURES_TO_GIVE_UP = 2;

    /** [stated] yautja fall for the bait more often than xenos - but neither keeps falling for it. */
    private static final int YAUTJA_PULL_ALLOWANCE = 2;

    /** Xenomorphs catch on after one pull. */
    private static final int XENO_PULL_ALLOWANCE = 1;

    /** Fighting retreat: the squad falls back below this share of its total health... */
    private static final float RETREAT_HEALTH_RATIO = 0.4F;

    /** ...or once it has lost half the marines it went into the fight with. Re-checked every second. */
    private static final int RETREAT_HOLD_TICKS = 60;

    /** Formation: rows behind the leader, and how far each marine stands to the side per row, in blocks. */
    private static final double FORMATION_ROW_SPACING = 2.0;

    private static final double FORMATION_SIDE_SPACING = 1.6;

    /** Flank distance limits, in blocks. */
    private static final double FLANK_MIN = 6.0;

    private static final double FLANK_MAX = 14.0;

    private static final TagKey<EntityType<?>> CONVENTION_BOSSES = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("c", "bosses")
    );

    /**
     * ⚠ Final review (memory) - squads are held WEAKLY here. Each marine holds its own squad, so a squad lives exactly
     * as long as a marine that uses it; when a world closes, its marines, their squads and the world itself can all be
     * collected even if no marine ever ticks again to trigger the clean-up below.
     */
    private static final Map<Object, java.lang.ref.WeakReference<Squad>> SQUADS = new HashMap<>();

    private static int lastPruneTick;

    /** The server the squads belong to; weak, so it never keeps a closed world in memory. */
    private static java.lang.ref.WeakReference<net.minecraft.server.MinecraftServer> owner = new java.lang.ref.WeakReference<>(null);

    private MarineSquads() {
        throw new UnsupportedOperationException();
    }

    /**
     * Server side, from {@code Marine.tick}. Reports the marine to its squad and lets the squad think once a second.
     *
     * @param marine the marine
     */
    public static void tick(Marine marine) {
        if (!(marine.level() instanceof ServerLevel level) || (marine.tickCount + marine.getId()) % 5 != 0) {
            return;
        }

        var key = squadKey(marine);

        if (key == null) {
            marine.setSquad(null);
            return;
        }

        var now = (int) level.getGameTime();

        // ⚠ Pre-merge review - SQUADS is static and held every squad's Marine objects. In single player, leaving a
        // world
        // and opening another kept the old squads - and through their marines the whole closed world - alive, and the
        // new world's earlier game time meant the sweep below never ran for them. Start fresh for a new server.
        if (owner.get() != level.getServer()) {
            SQUADS.clear();
            owner = new java.lang.ref.WeakReference<>(level.getServer());
            lastPruneTick = now;
        }

        // Squads whose marines all died or unloaded are never ticked again; sweep them out every half minute.
        if (now - lastPruneTick > 600 || now < lastPruneTick) {
            lastPruneTick = now;
            SQUADS.values().removeIf(ref -> {
                var squad = ref.get();
                return squad == null || now - squad.lastThinkTick > 200;
            });
        }

        var ref = SQUADS.get(key);
        var squad = ref == null ? null : ref.get();

        if (squad == null) {
            squad = new Squad();
            SQUADS.put(key, new java.lang.ref.WeakReference<>(squad));
        }

        squad.members.put(marine.getId(), new Member(marine, now));
        marine.setSquad(squad);

        squad.playDueReplies(now);

        if (now - squad.lastThinkTick >= THINK_INTERVAL) {
            squad.lastThinkTick = now;
            squad.think(now);
        }
    }

    /** {@return the key of this marine's squad, or null for a marine on its own} */
    private static @Nullable Object squadKey(Marine marine) {
        var allegiance = marine.getAllegianceUUID();

        // Pre-merge review - per dimension, like camp squads: hired marines left in two dimensions are two squads, not
        // one whose callouts, formation places and retreat point mix coordinates from different worlds.
        if (allegiance.isSome()) {
            return marine.level().dimension().location() + "#" + allegiance.unwrap();
        }

        var home = marine.getCampHome();

        return home == null ? null : marine.level().dimension().location() + "@" + home.asLong();
    }

    /**
     * {@return whether {@code entity} is a big threat}: max health of {@value #BIG_THREAT_MAX_HEALTH} or more, a boss,
     * or a yautja.
     *
     * @param entity the candidate
     */
    public static boolean isBigThreat(LivingEntity entity) {
        return entity.getMaxHealth() >= BIG_THREAT_MAX_HEALTH
            || entity.getType().is(CONVENTION_BOSSES)
            || "avp_predator".equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace());
    }

    /**
     * Oct 5 - records alien contact for the marine's whole squad, so every squadmate honours the truce.
     *
     * @param marine the marine that made contact
     * @param until  the game time the truce lasts until
     */
    public static void noteAlienContact(Marine marine, long until) {
        var squad = marine.getSquad();

        if (squad != null && until > squad.alienContactUntil) {
            squad.alienContactUntil = until;
        }
    }

    /**
     * {@return the game time until which the marine's squad honours the alien truce, or 0}
     *
     * @param marine the marine
     */
    public static long alienContactUntil(Marine marine) {
        var squad = marine.getSquad();

        return squad == null ? 0L : squad.alienContactUntil;
    }

    /**
     * Orders a marine's visible targets the squad's way: cover requests first, then the big threat the squad is
     * focusing, then the rest by distance - with small targets that already have {@value #MAX_PER_SMALL_TARGET}
     * squadmates on them pushed back, so the squad spreads out instead of everyone shooting the nearest zombie.
     *
     * @param marine  the marine
     * @param targets its visible targets
     * @return the list in priority order (a new list)
     */
    public static List<LivingEntity> prioritise(Marine marine, List<LivingEntity> targets) {
        var squad = marine.getSquad();

        if (squad == null || targets.size() < 2) {
            return targets.stream().sorted(Comparator.comparingDouble(marine::distanceToSqr)).toList();
        }

        var now = (int) marine.level().getGameTime();
        var current = marine.getTarget();
        var sorted = new ArrayList<>(targets);
        // ⚠ Review fix 2 - SELF-DEFENCE COMES FIRST. Whatever is hitting this marine at close range outranks the
        // squad's
        // big threat and every cover request - otherwise a marine kept shooting a boss 20 blocks away while a zombie
        // chewed on it.
        var attacker = marine.getLastHurtByMob();
        var underAttack = attacker != null && marine.tickCount - marine.getLastHurtByMobTimestamp() < 100
            && marine.distanceToSqr(attacker) <= 36.0;

        sorted.sort(Comparator.comparingDouble(target -> {
            var score = Math.sqrt(marine.distanceToSqr(target));
            var id = target.getUUID();

            if (underAttack && target == attacker) {
                score -= 5000;
            }

            if (squad.coverUntil.getOrDefault(id, 0) > now) {
                score -= 1000;
            }

            if (id.equals(squad.bigThreat)) {
                score -= 300;
            } else {
                var engaged = squad.engaged.getOrDefault(id, 0) - (target == current ? 1 : 0);

                if (engaged >= MAX_PER_SMALL_TARGET) {
                    score += 200;
                }
            }

            return score;
        }));

        return sorted;
    }

    /**
     * {@return whether a squadmate called {@code entity} out in the last few seconds near this marine} - it is then a
     * threat to this marine too, as if it had been hit by it.
     *
     * @param marine the marine
     * @param entity the candidate
     */
    public static boolean isCalledOut(Marine marine, LivingEntity entity) {
        var squad = marine.getSquad();

        if (squad == null) {
            return false;
        }

        var callout = squad.callouts.get(entity.getUUID());

        return callout != null
            && callout.until > marine.level().getGameTime()
            && marine.distanceToSqr(callout.where) <= CALLOUT_RADIUS * CALLOUT_RADIUS;
    }

    /**
     * A marine was hit: tell the squad, and turn idle squadmates nearby to face it.
     *
     * @param marine   the marine that was hit
     * @param attacker what hit it
     */
    public static void callout(Marine marine, LivingEntity attacker) {
        var squad = marine.getSquad();

        if (squad == null || !attacker.isAlive()) {
            return;
        }

        var now = (int) marine.level().getGameTime();
        squad.callouts.put(attacker.getUUID(), new Callout(marine.position(), now + CALLOUT_TICKS));
        squad.radioCallout(marine, now);

        for (var member : squad.members.values()) {
            var mate = member.marine;

            if (
                mate != marine && mate.isAlive() && mate.getTarget() == null
                    && mate.distanceToSqr(marine) <= CALLOUT_RADIUS * CALLOUT_RADIUS
            ) {
                mate.getLookControl().setLookAt(attacker, 30.0F, 30.0F);
            }
        }
    }

    /**
     * A marine is starting a reload: the squad covers it.
     *
     * @param marine the reloading marine
     */
    public static void requestCover(Marine marine) {
        var squad = marine.getSquad();

        if (squad == null) {
            return;
        }

        var recentlyHit = marine.tickCount - marine.getLastHurtByMobTimestamp() < 100;
        var threat = recentlyHit ? marine.getLastHurtByMob() : marine.getTarget();

        if (threat != null && threat.isAlive()) {
            squad.coverUntil.put(threat.getUUID(), (int) marine.level().getGameTime() + COVER_TICKS);
        }
    }

    /**
     * A marine fired at {@code target}. If it is the squad's anchor and the target is the big threat, the shot pulls
     * the target's attention back onto the anchor - while that still works on this enemy.
     *
     * @param marine the shooter
     * @param target what it shot at
     */
    public static void onShot(Marine marine, LivingEntity target) {
        var squad = marine.getSquad();

        if (
            squad == null || squad.anchorId != marine.getId() || !target.getUUID().equals(squad.bigThreat)
                || !(target instanceof Mob mob) || mob.getTarget() == marine
        ) {
            return;
        }

        var now = (int) marine.level().getGameTime();
        var record = squad.pulls.computeIfAbsent(target.getUUID(), $ -> new PullRecord());

        if (record.failed || now < record.nextAllowed) {
            return;
        }

        var allowance = pullAllowance(target);

        if (allowance >= 0 && record.pulls >= allowance) {
            // ⭐ They caught on. Not a failure the squad has to discover - this kind of enemy simply stops falling for
            // it.
            record.failed = true;
            return;
        }

        mob.setTarget(marine);
        mob.setLastHurtByMob(marine);
        record.pulls++;
        record.nextAllowed = now + PULL_COOLDOWN;
        record.verifyAt = now + PULL_VERIFY_DELAY;
        record.anchorId = marine.getId();
    }

    /** {@return how many pulls this enemy falls for: -1 for no limit} */
    private static int pullAllowance(LivingEntity target) {
        var namespace = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getNamespace();

        return switch (namespace) {
            case "avp_predator" -> YAUTJA_PULL_ALLOWANCE;
            case "avp_alien" -> XENO_PULL_ALLOWANCE;
            default -> -1;
        };
    }

    /**
     * {@return where this marine's squad is falling back to, or null when it is not falling back} Oct 5 - tactic 8, the
     * fighting retreat: its camp, or for a hired squad its leader.
     *
     * @param marine the marine
     */
    public static @Nullable Vec3 retreatPoint(Marine marine) {
        var squad = marine.getSquad();

        if (squad == null || squad.retreatPoint == null || marine.level().getGameTime() >= squad.retreatUntil) {
            return null;
        }

        return squad.retreatPoint;
    }

    /**
     * {@return this marine's place in a loose wedge behind {@code leader}, or null when it has none} Oct 5 - tactic 10,
     * formation follow: two to a row, alternating sides, each row further back and wider, on standable ground.
     *
     * @param marine the marine
     * @param leader whoever it follows
     */
    public static @Nullable Vec3 formationSlot(Marine marine, net.minecraft.world.entity.Entity leader) {
        var squad = marine.getSquad();

        if (squad == null) {
            return null;
        }

        var index = squad.formationIndex.getOrDefault(marine.getId(), -1);

        if (index < 0) {
            return null;
        }

        var row = index / 2 + 1;
        var side = index % 2 == 0 ? -1.0 : 1.0;
        var yaw = Math.toRadians(leader.getYRot());
        // Minecraft's facing: yaw 0 looks along +Z. Forward is (-sin, cos); the right hand is (-cos, -sin).
        var forwardX = -Math.sin(yaw);
        var forwardZ = Math.cos(yaw);
        var rightX = -Math.cos(yaw);
        var rightZ = -Math.sin(yaw);
        var back = FORMATION_ROW_SPACING * row;
        var across = FORMATION_SIDE_SPACING * row * side;
        var x = leader.getX() - forwardX * back + rightX * across;
        var z = leader.getZ() - forwardZ * back + rightZ * across;

        return standableAt(leader.level(), x, leader.getY(), z);
    }

    /**
     * {@return whether this marine is standing in its formation place behind {@code leader}}
     *
     * @param marine the marine
     * @param leader whoever it follows
     */
    public static boolean isAtFormationSlot(Marine marine, net.minecraft.world.entity.Entity leader) {
        var slot = formationSlot(marine, leader);

        return slot != null && marine.distanceToSqr(slot) < 2.5 * 2.5;
    }

    /**
     * {@return where this marine should stand to flank the squad's big threat, or null when it should not flank}
     *
     * @param marine the marine
     */
    public static @Nullable Vec3 flankPosition(Marine marine) {
        var squad = marine.getSquad();

        return squad == null ? null : squad.flankPositions.get(marine.getId());
    }

    /** One squad's shared picture of the fight. */
    public static final class Squad {

        private final Map<Integer, Member> members = new HashMap<>();

        private final Map<UUID, Integer> engaged = new HashMap<>();

        private final Map<UUID, Integer> coverUntil = new HashMap<>();

        private final Map<UUID, Callout> callouts = new HashMap<>();

        private final Map<UUID, PullRecord> pulls = new HashMap<>();

        private final Map<Integer, Vec3> flankPositions = new HashMap<>();

        private int lastThinkTick = Integer.MIN_VALUE / 2;

        /** Tactic 8 - the most marines the squad has had in the current fight. */
        private int peakMembers;

        /** Tactic 8 - marines killed since the current fight began. */
        private int deathsThisFight;

        /** Tactic 8 - the last second any member had a target. */
        private int lastFightTick = Integer.MIN_VALUE / 2;

        private int retreatUntil;

        private @Nullable Vec3 retreatPoint;

        /** Tactic 10 - each member's place in the wedge, by entity id. */
        private final Map<Integer, Integer> formationIndex = new HashMap<>();

        private int nextRadioTick;

        /** Oct 5 - alien truce: the game time until which the whole squad honours it (MarineAlienTruce). */
        private long alienContactUntil;

        private final List<RadioReply> pendingReplies = new ArrayList<>();

        private @Nullable UUID bigThreat;

        private int anchorId = -1;

        private Squad() {}

        /**
         * [stated] Oct 5: "have the call out make one and the other marines reply". The marine that was hit keys its
         * radio (a burst of scrambled chatter) and up to two squadmates within the callout radius answer, one after the
         * other.
         */
        private void radioCallout(Marine caller, int now) {
            if (now < nextRadioTick) {
                return;
            }

            nextRadioTick = now + RADIO_COOLDOWN_TICKS;
            playRadio(caller);

            var random = caller.getRandom();
            var delay = 0;
            var answered = 0;

            for (var member : members.values()) {
                var mate = member.marine;

                if (
                    answered >= RADIO_REPLIES || mate == caller || !mate.isAlive() || mate.isRemoved()
                        || mate.distanceToSqr(caller) > CALLOUT_RADIUS * CALLOUT_RADIUS
                ) {
                    continue;
                }

                // Each answer waits for the transmission before it to finish (clips run about a second and a half).
                delay += 30 + random.nextInt(15);
                pendingReplies.add(new RadioReply(mate, now + delay));
                answered++;
            }
        }

        private void playDueReplies(int now) {
            if (pendingReplies.isEmpty()) {
                return;
            }

            pendingReplies.removeIf(reply -> {
                if (reply.at > now) {
                    return false;
                }

                if (reply.marine.isAlive() && !reply.marine.isRemoved()) {
                    playRadio(reply.marine);
                }

                return true;
            });
        }

        private static void playRadio(Marine marine) {
            marine.level()
                .playSound(
                    null,
                    marine.getX(),
                    marine.getEyeY(),
                    marine.getZ(),
                    com.human.common.registry.init.HumanSoundEvents.MARINE_RADIO_CHATTER.get(),
                    net.minecraft.sounds.SoundSource.NEUTRAL,
                    0.9F,
                    0.95F + marine.getRandom().nextFloat() * 0.1F
                );
        }

        private void think(int now) {
            // ⚠ Bug pass 1 - only a marine that DIED counts as a loss for the fighting retreat. One that walked out of
            // loaded chunks also leaves the squad, and counting it would send the rest running for no reason.
            members.values().removeIf(member -> {
                var gone = now - member.lastSeen > MEMBER_TIMEOUT || member.marine.isRemoved() || !member.marine.isAlive();

                if (
                    gone && (member.marine.isDeadOrDying()
                        || member.marine.getRemovalReason() == net.minecraft.world.entity.Entity.RemovalReason.KILLED)
                ) {
                    deathsThisFight++;
                }

                return gone;
            });
            coverUntil.values().removeIf(until -> until <= now);
            callouts.values().removeIf(callout -> callout.until <= now);
            engaged.clear();
            flankPositions.clear();
            thinkFormationAndRetreat(now);

            LivingEntity threat = null;
            var threatDistance = Double.MAX_VALUE;

            for (var member : members.values()) {
                var target = member.marine.getTarget();

                if (target == null || !target.isAlive()) {
                    continue;
                }

                engaged.merge(target.getUUID(), 1, Integer::sum);

                if (isBigThreat(target)) {
                    var distance = member.marine.distanceToSqr(target);

                    if (distance < threatDistance) {
                        threatDistance = distance;
                        threat = target;
                    }
                }
            }

            // ⚠ Review fix - a tactic that FAILED is remembered ten minutes, not one: forgetting it after a minute let
            // the
            // same xeno fall for the bait again, which is exactly what "they would catch onto it" rules out.
            pulls.values().removeIf(record -> now - record.nextAllowed > (record.failed ? 12000 : 1200));

            if (threat == null) {
                bigThreat = null;
                anchorId = -1;
                return;
            }

            bigThreat = threat.getUUID();
            verifyPull(threat, now);

            var record = pulls.get(bigThreat);
            var tacticFailed = record != null && record.failed;

            anchorId = pickAnchor(threat);

            // ⚠ Once the bait has stopped working on this enemy, flanking is pointless - it would just leave marines
            // out
            // on the flanks with nobody holding its attention. The squad falls back to plain focus fire.
            if (!tacticFailed) {
                assignFlanks(threat);
            }
        }

        /**
         * Tactic 10 places, and tactic 8: [stated] approved "fighting retreat as a unit" - below 40% of the squad's
         * total health, or down to half the marines it started the fight with, the squad falls back together to its
         * camp or its leader, bounding: half of it moves while the other half holds and keeps firing, swapping every
         * two seconds.
         */
        private void thinkFormationAndRetreat(int now) {
            formationIndex.clear();
            // Pre-merge review - only marines actually following take a place; a sentry or a marine told to hold used
            // to take one too and leave a gap in the wedge.
            var ordered = new ArrayList<Integer>();

            for (var member : members.values()) {
                if (member.marine.getMode() == com.human.common.gameplay.entity.living.human.marine.MarineMode.FOLLOW) {
                    ordered.add(member.marine.getId());
                }
            }

            ordered.sort(Integer::compare);

            for (var i = 0; i < ordered.size(); i++) {
                formationIndex.put(ordered.get(i), i);
            }

            var fighting = false;
            var health = 0.0F;
            var maxHealth = 0.0F;
            Vec3 fallBackTo = null;

            for (var member : members.values()) {
                var marine = member.marine;
                fighting |= marine.getTarget() != null;
                health += marine.getHealth();
                maxHealth += marine.getMaxHealth();

                // Bug pass 2 - a hired marine falls back on its LEADER, even if it once belonged to a camp (it keeps
                // the
                // camp as home); only a squad with no leader falls back on its camp.
                if (fallBackTo == null) {
                    var home = marine.getCampHome();

                    if (marine.getLeader().isSome()) {
                        fallBackTo = marine.getLeader().unwrap().position();
                    } else if (home != null) {
                        fallBackTo = Vec3.atBottomCenterOf(home);
                    }
                }
            }

            // Bug pass 2 - a fight is over after ten quiet seconds, not at the first second without a target; otherwise
            // every lull between enemies wiped the count of marines lost.
            if (fighting) {
                lastFightTick = now;
            } else if (now - lastFightTick > 200) {
                peakMembers = members.size();
                deathsThisFight = 0;
                return;
            } else {
                return;
            }

            peakMembers = Math.max(peakMembers, members.size() + deathsThisFight);

            var badlyHurt = maxHealth > 0 && health / maxHealth < RETREAT_HEALTH_RATIO;
            var halfLost = peakMembers >= 2 && deathsThisFight * 2 >= peakMembers;

            if (fallBackTo != null && (badlyHurt || halfLost)) {
                retreatPoint = fallBackTo;
                retreatUntil = now + RETREAT_HOLD_TICKS;
            }
        }

        /** Did the last pull take? If the target is not on the anchor after the delay, it did not. */
        private void verifyPull(LivingEntity threat, int now) {
            var record = pulls.get(threat.getUUID());

            if (record == null || record.failed || record.verifyAt == 0 || now < record.verifyAt) {
                return;
            }

            record.verifyAt = 0;
            var hunting = threat instanceof Mob mob ? mob.getTarget() : null;

            if (hunting == null || hunting.getId() != record.anchorId) {
                record.failures++;

                if (record.failures >= PULL_FAILURES_TO_GIVE_UP) {
                    record.failed = true;
                }
            }
        }

        /** The marine the threat is already on, or else the toughest marine fighting it. */
        private int pickAnchor(LivingEntity threat) {
            if (threat instanceof Mob mob && mob.getTarget() instanceof Marine hunted && members.containsKey(hunted.getId())) {
                return hunted.getId();
            }

            Marine best = null;
            var bestToughness = -1.0;

            for (var member : members.values()) {
                var marine = member.marine;

                if (marine.getTarget() != threat) {
                    continue;
                }

                var toughness = marine.getHealth() + marine.getAttributeValue(Attributes.ARMOR) * 2.0;

                if (toughness > bestToughness) {
                    bestToughness = toughness;
                    best = marine;
                }
            }

            return best == null ? -1 : best.getId();
        }

        /** Spread the other gunners around the threat's sides and back, opposite the anchor. */
        private void assignFlanks(LivingEntity threat) {
            var anchor = members.get(anchorId);

            if (anchor == null) {
                return;
            }

            var toAnchor = anchor.marine.position().subtract(threat.position());
            var baseAngle = Math.atan2(toAnchor.z, toAnchor.x);
            var index = 0;
            double[] offsets = { Math.PI / 2, -Math.PI / 2, Math.PI, Math.PI * 0.75, -Math.PI * 0.75 };

            for (var member : members.values()) {
                var marine = member.marine;

                if (
                    marine.getId() == anchorId || marine.getTarget() != threat
                        || !(marine.getMainHandItem().getItem() instanceof GunItem gunItem)
                ) {
                    continue;
                }

                var range = gunItem.getGunConfig().getDefaultFireMode().range();
                var radius = Mth.clamp(range * 0.6, FLANK_MIN, FLANK_MAX);
                var angle = baseAngle + offsets[index % offsets.length];
                index++;

                var spot = standableNear(
                    threat,
                    threat.getX() + Math.cos(angle) * radius,
                    threat.getZ() + Math.sin(angle) * radius
                );

                if (spot != null) {
                    flankPositions.put(marine.getId(), spot);
                }
            }
        }

        private static @Nullable Vec3 standableNear(LivingEntity threat, double x, double z) {
            return standableAt(threat.level(), x, threat.getY(), z);
        }
    }

    /** {@return a standable spot at (x, z) within three blocks up or down of y, or null} */
    private static @Nullable Vec3 standableAt(net.minecraft.world.level.Level level, double x, double y, double z) {
        var base = BlockPos.containing(x, y, z);

        for (var dy = 0; dy <= 6; dy++) {
            var feet = base.above(dy % 2 == 0 ? dy / 2 : -(dy + 1) / 2);
            var below = feet.below();

            if (
                level.isLoaded(feet)
                    && level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)
                    && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                    && level.getFluidState(feet).isEmpty()
            ) {
                return Vec3.atBottomCenterOf(feet);
            }
        }

        return null;
    }

    private record Member(
        Marine marine,
        int lastSeen
    ) {}

    private record RadioReply(
        Marine marine,
        int at
    ) {}

    private record Callout(
        Vec3 where,
        int until
    ) {}

    private static final class PullRecord {

        private int pulls;

        private int failures;

        private boolean failed;

        private int nextAllowed;

        private int verifyAt;

        private int anchorId = -1;
    }
}
