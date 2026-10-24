package com.human.common.gameplay.freeze;

import com.human.Human;
import com.human.common.gameplay.fluid.CryoFreezing;
import com.human.common.network.packet.S2CFrozenSolidPayload;
import com.human.common.registry.init.HumanMobEffects;
import com.human.common.registry.tag.HumanEntityTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Freezing SOLID — the shared freeze rule for avp_human, avp_alien and avp_predator, owned here. [stated] "anything
 * that drops to 10% health, or any passive mob, becomes frozen with the cryochamber frost effect layer; frozen mobs
 * stay frozen 5 minutes unless lit on fire."
 * <h2>The rule</h2>
 * <ul>
 * <li>A freeze source (cryofluid, the yautja freeze grenade, any later freeze weapon) calls {@link #tryFreeze}.</li>
 * <li>It takes a mob at 10% health or less, or any mob that is not hostile. Marines count as hostile.</li>
 * <li>Never a player — [stated] players only get vanilla's freeze.</li>
 * <li>Never anything cryo-immune: the {@code cryofluid_immune} tag (vanilla's freeze-immune mobs included), a full
 * cryo-protective suit, anything that refuses cryofluid damage (how the alien strain rules reach this — irradiated
 * xenos), or the {@code freeze_solid_immune} tag (the Ender Dragon and the Wither — [stated] "dragon and wither no
 * warden and elder guardian yes"). Queens and the empress CAN be frozen: [stated] "we plan to make them all cryotube
 * storable".</li>
 * <li>Frozen: {@link HumanMobEffects#getFrozenSolidHolder() the Frozen Solid effect} for {@link #FROZEN_TICKS} (5 min).
 * Its AI is switched off (MixinMob_FrozenSolid), it does not move, and it can still be hurt. Fire thaws it at once
 * (FrozenSolidStatusEffect). A statue does not shatter: it is meant to be storable.</li>
 * </ul>
 * <h2>For the other mods</h2> avp_predator and avp_alien have no compile dependency on avp_human. They call
 * {@code com.human.common.gameplay.freeze.FrozenSolid.tryFreeze(LivingEntity)} by name, the same arrangement as
 * radiation, and use a stand-in of their own when avp_human is not installed.
 * <h2>The frost overlay on clients</h2> Mob effects are not sent to players watching a mob, only to the mob's own
 * player. So every frozen mob is announced to everyone tracking it once a second ({@link #tickLevel}); a client forgets
 * a mob it has not heard about for two seconds. That covers thawing, mobs coming into view, and joins, with no hooks.
 * The overlay itself is the cryotank's frost (FrostOverlayLayer for vanilla models, AzFrostOverlay for BLib models),
 * already attached to every renderer.
 */
public final class FrozenSolid {

    /** [stated] five minutes. */
    public static final int FROZEN_TICKS = 6000;

    /** [stated] "anything that drops to 10% health". */
    public static final float HEALTH_FRACTION = 0.10F;

    /** How often watchers are told which mobs are frozen. */
    private static final int SYNC_INTERVAL = 20;

    /** Frozen mobs on the server, for the sync. Weak: an unloaded or dead mob simply drops out. */
    private static final Set<LivingEntity> FROZEN = Collections.newSetFromMap(new WeakHashMap<>());

    private FrozenSolid() {}

    /**
     * Freezes this mob solid if the rule allows it. The entry point every freeze source calls — including the other
     * mods, by name. {@return whether it is frozen solid now}
     */
    public static boolean tryFreeze(LivingEntity target) {
        return freeze(target, false);
    }

    /**
     * [stated] Oct 19 - the yautja freeze grenade "instantly freeze[s] anything regardless of hp amount" ("the grenades
     * from predator badlands instantly froze things"). As {@link #tryFreeze} with the health rule dropped: any mob that
     * can be frozen solid at all is, at once. Still never a player and never anything cold-immune, and the strain rules
     * still apply (a nether egg shatters; a nether adult freezes or bursts, 50/50). avp_predator calls this by name.
     * {@return whether it is frozen solid now}
     */
    public static boolean forceFreeze(LivingEntity target) {
        return freeze(target, true);
    }

    private static boolean freeze(LivingEntity target, boolean anyHealth) {
        if (target.level().isClientSide || !target.isAlive()) {
            return false;
        }

        if (isFrozen(target)) {
            return true;
        }

        // [stated] Oct 19 - "nether ovomorphs flat out die when blasted with cryo". Not for a strain immune to cold.
        if (target.getType().is(HumanEntityTypeTags.SHATTERS_FROM_COLD)) {
            if (!isImmune(target)) {
                target.kill();
            }

            return false;
        }

        var allowed = anyHealth ? !(target instanceof Player) && !isImmune(target) : canFreezeSolid(target);

        if (!allowed) {
            return false;
        }

        // [stated] Oct 19 - nether adults at 10 %: "either freeze or explode its a 50 50 chance".
        if (target.getType().is(HumanEntityTypeTags.FREEZE_OR_EXPLODE) && target.getRandom().nextBoolean()) {
            coldBurst(target);
            return false;
        }

        target.addEffect(new MobEffectInstance(HumanMobEffects.getFrozenSolidHolder(), FROZEN_TICKS, 0, false, false, true));
        track(target);

        // [stated] Oct 19 - "play this when enemies freeze solid". Once, at the moment it freezes: an already-frozen
        // mob returned above, so a cryo stream held on it does not replay it. Not for a specimen pulled out of a
        // cryotank (freezeOutOfCryo) - that one was frozen already. A slight pitch spread so a group frozen at once
        // does not sound like one sample stacked.
        target.level()
            .playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                com.human.common.registry.init.HumanSoundEvents.FREEZE_SOLID.get(),
                target.getSoundSource(),
                1.0F,
                0.9F + target.getRandom().nextFloat() * 0.2F
            );

        return true;
    }

    /** {@return whether the rule lets this mob be frozen solid right now} */
    /**
     * [stated] Oct 19 - a specimen pulled out of a cryotank on a lead, cord or chain comes out FROZEN SOLID with a
     * fresh thaw timer, whatever its health: it has just left cryo. Not the Ender Dragon or the Wither (they never go
     * in a tank anyway). Server side.
     */
    public static void freezeOutOfCryo(LivingEntity target) {
        if (target.level().isClientSide || !target.isAlive() || target instanceof Player) {
            return;
        }

        if (target.getType().is(HumanEntityTypeTags.FREEZE_SOLID_IMMUNE)) {
            return;
        }

        target.addEffect(new MobEffectInstance(HumanMobEffects.getFrozenSolidHolder(), FROZEN_TICKS, 0, false, false, true));
        track(target);
    }

    /**
     * A real attack on a frozen mob breaks it free of the ice; small damage does not. [stated] Oct 19: "when an enemy
     * that is frozen is hit or takes damage not just minor but an actual attack or heavy fall they can unfreeze".
     */
    public static final float BREAK_ATTACK_DAMAGE = 2.0F;

    /** A fall dealing this much (a drop of about seven blocks) breaks the ice too - always. */
    public static final float BREAK_FALL_DAMAGE = 4.0F;

    /** [stated] Oct 19 - an attack on a frozen mob "has a good chance to break free lets say 70% chance". */
    public static final float BREAK_ATTACK_CHANCE = 0.70F;

    /**
     * Called after a frozen mob has actually taken damage (MixinLivingEntity_FrozenAttacker). If the hit counts, the
     * mob thaws on the spot - the same as the effect running out, so a lead it was only on because it was frozen still
     * lets go - and the ice-break sound plays. Server side.
     * <ul>
     * <li>Counts: a hit from a mob or player (melee, or anything it fired or threw) or an explosion, of at least
     * {@link #BREAK_ATTACK_DAMAGE} - a {@link #BREAK_ATTACK_CHANCE} chance each time; a fall of at least
     * {@link #BREAK_FALL_DAMAGE} - always.</li>
     * <li>Never: cold of any kind (the cryofluid and cryo vapor that froze it, vanilla freezing) - otherwise the cryo
     * stream that keeps it frozen would break it out; the acid and infernium vapor clouds (gas, though credited to the
     * shooter); damage over time with no attacker (poison, wither, radiation, acid, drowning, cactus...); a mob the hit
     * killed.</li>
     * </ul>
     */
    public static void onHurt(LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (target.level().isClientSide || !target.isAlive() || !isFrozen(target) || !breaksIce(source, amount)) {
            return;
        }

        // A heavy fall always shatters it; an attack only usually does.
        if (!source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) && target.getRandom().nextFloat() >= BREAK_ATTACK_CHANCE) {
            return;
        }

        target.removeEffect(HumanMobEffects.getFrozenSolidHolder());
        target.setTicksFrozen(0);
        target.level()
            .playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                com.human.common.registry.init.HumanSoundEvents.FROZEN_BREAK.get(),
                target.getSoundSource(),
                1.0F,
                0.9F + target.getRandom().nextFloat() * 0.2F
            );
    }

    /** A held mob's thaw timer is topped back up only once it has run down by this much (5 s). */
    private static final int KEEP_FROZEN_MARGIN_TICKS = 100;

    /**
     * Keeps an already-frozen mob frozen while cold keeps reaching it (the distributor fog, a chem thrower stream,
     * standing in cryofluid): its thaw timer is put back to full - but only once it has run down by
     * {@value #KEEP_FROZEN_MARGIN_TICKS} ticks. ⚡ Re-adding the effect every call would send every watching client an
     * effect-update packet for every frozen mob, every tick it stands in cryofluid; this sends one every five seconds.
     */
    public static void keepFrozen(LivingEntity target) {
        var current = target.getEffect(HumanMobEffects.getFrozenSolidHolder());

        if (current != null && current.getDuration() > FROZEN_TICKS - KEEP_FROZEN_MARGIN_TICKS) {
            return;
        }

        target.addEffect(new MobEffectInstance(HumanMobEffects.getFrozenSolidHolder(), FROZEN_TICKS, 0, false, false, true));
        track(target);
    }

    /** Cold of any kind: cryofluid, cryo vapor, and anything in vanilla's is_freezing (powder snow, freeze blasts). */
    public static boolean isColdDamage(net.minecraft.world.damagesource.DamageSource source) {
        return source.is(com.human.common.registry.key.HumanDamageTypeKeys.CRYOFLUID)
            || source.is(com.human.common.registry.key.HumanDamageTypeKeys.CRYO_VAPOR)
            || source.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING);
    }

    private static boolean breaksIce(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (
            isColdDamage(source)
                // Gas, not a blow: a vapor cloud is credited to whoever fired it, but standing in it is not being hit.
                || source.is(com.human.common.registry.key.HumanDamageTypeKeys.ACID_VAPOR)
                || source.is(com.human.common.registry.key.HumanDamageTypeKeys.INFERNIUM_VAPOR)
        ) {
            return false;
        }

        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            return amount >= BREAK_FALL_DAMAGE;
        }

        var isAttack = source.getEntity() != null
            || source.getDirectEntity() != null
            || source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);

        return isAttack && amount >= BREAK_ATTACK_DAMAGE;
    }

    public static boolean canFreezeSolid(LivingEntity target) {
        if (target instanceof Player || isImmune(target)) {
            return false;
        }

        // [stated] Oct 19 - eggs freeze at any health: "ovomorphs are instantly frozen they dont open".
        if (target.getType().is(HumanEntityTypeTags.FREEZES_INSTANTLY)) {
            return true;
        }

        return !isHostile(target) || target.getHealth() <= target.getMaxHealth() * HEALTH_FRACTION;
    }

    /**
     * {@return whether this mob follows the 10% rule rather than freezing at once} — every vanilla hostile, and the
     * marines: [stated] "marines are hostile and should follow the 10%". A marine is a PathfinderMob, not an Enemy, so
     * it has to be named here; without it any freeze would turn a full-health marine into a statue.
     */
    /**
     * Whether cryo touching this does its whole job at once (freeze an egg solid, or kill a nether egg) instead of
     * damaging it first. Callers check this BEFORE dealing cryo damage: avp_alien eggs open when they are hurt, so a
     * damaged egg would hatch before it froze.
     */
    public static boolean reactsOnContact(LivingEntity target) {
        var type = target.getType();

        return (type.is(HumanEntityTypeTags.FREEZES_INSTANTLY) || type.is(HumanEntityTypeTags.SHATTERS_FROM_COLD))
            && !isImmune(target);
    }

    /** A creeper's blast power, at drone size (avp_alien's drone is 0.8 x 1.98). */
    private static final float BURST_BASE_POWER = 3.0F;

    private static final float DRONE_SIZE = 0.8F * 1.98F;

    /** Even an empress's blast stops here. */
    private static final float BURST_MAX_POWER = 8.0F;

    private static final double LIMB_HORIZONTAL_VELOCITY = 0.18;

    private static final double LIMB_VERTICAL_VELOCITY = 0.14;

    /**
     * [stated] Oct 19 - the nether "explode" half of the 50/50: "explosion wont break blocks but would cause damage and
     * it grows with the size of the mob with a drone being basic creeper then scales up from there. and if possible
     * have its limbs blow off". Power is a creeper's at drone size and grows with the square root of the mob's side-on
     * area (so a mob twice as big each way blasts twice as hard), capped at {@value #BURST_MAX_POWER}. Breaks no blocks
     * ({@code ExplosionInteraction.NONE}). Its limbs are blown off first - the same BLib call and launch speeds
     * avp_alien's boiler uses - then it dies, dropping what it would.
     */
    private static void coldBurst(LivingEntity target) {
        var level = target.level();
        var random = target.getRandom();
        var size = target.getBbWidth() * target.getBbHeight();
        var power = Math.min(BURST_MAX_POWER, BURST_BASE_POWER * (float) Math.max(1.0, Math.sqrt(size / DRONE_SIZE)));
        var centre = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);

        for (var definition : com.blib.api.common.dismemberment.v1.LimbDismemberer.getRemainingDefinitions(target)) {
            var angle = random.nextDouble() * Math.TAU;
            var horizontal = LIMB_HORIZONTAL_VELOCITY * (0.65 + random.nextDouble() * 0.7);
            var vertical = LIMB_VERTICAL_VELOCITY * (0.65 + random.nextDouble() * 0.7);
            var velocity = new net.minecraft.world.phys.Vec3(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);

            com.blib.api.common.dismemberment.v1.LimbDismemberer.detach(target, definition.id(), limb -> {
                limb.moveTo(centre, target.getYRot(), target.getXRot());
                limb.launch(velocity);
            });
        }

        level.explode(target, centre.x, centre.y, centre.z, power, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        target.kill();
    }

    public static boolean isHostile(LivingEntity target) {
        return target instanceof Enemy || target instanceof com.human.common.gameplay.entity.living.human.marine.Marine;
    }

    public static boolean isImmune(LivingEntity target) {
        return target.getType().is(HumanEntityTypeTags.FREEZE_SOLID_IMMUNE)
            || CryoFreezing.isImmune(target)
            || target.isInvulnerableTo(CryoFreezing.damageSource(target));
    }

    public static boolean isFrozen(LivingEntity target) {
        return target.hasEffect(HumanMobEffects.getFrozenSolidHolder());
    }

    /**
     * {@return whether something is pulling this mob} — a lead, the titanium capture cord, or avp_alien's capture chain
     * (held, or chained to another mob). [stated] Oct 4: any of the three can be put on a frozen mob to drag it —
     * "another way of capturing them and transporting". A tethered frozen mob is not pinned in place, so the pull moves
     * it; it still cannot move itself.
     */
    public static boolean isTethered(LivingEntity target) {
        return target instanceof net.minecraft.world.entity.Mob mob
            && (mob.isLeashed()
                || com.human.common.gameplay.item.CaptureCordTether.isHeld(mob)
                || com.human.compatibility.avp_alien.AlienCaptureChain.isTethered(mob));
    }

    /**
     * Saved on a mob that is on a lead while frozen but could not normally take one (any hostile). When it thaws, the
     * lead lets go — see MixinMob_FrozenSolid.
     */
    public static final String FROZEN_LEASH_TAG = "avp_human.frozen_leash";

    /** Called by the effect each tick, so a frozen mob loaded from a save is synced too. */
    public static void track(LivingEntity target) {
        if (!target.level().isClientSide) {
            FROZEN.add(target);
        }
    }

    /** Registered on post-level-tick: tells everyone watching which mobs are frozen. */
    public static void tickLevel(Level level) {
        if (!(level instanceof ServerLevel serverLevel) || FROZEN.isEmpty() || serverLevel.getGameTime() % SYNC_INTERVAL != 0) {
            return;
        }

        FROZEN.removeIf(entity -> entity.isRemoved() || !entity.isAlive() || !isFrozen(entity));

        for (var entity : FROZEN) {
            if (entity.level() == serverLevel) {
                Human.MOD.networking().sendToAllClientsTrackingEntity(entity, new S2CFrozenSolidPayload(entity.getId()));
            }
        }
    }
}
