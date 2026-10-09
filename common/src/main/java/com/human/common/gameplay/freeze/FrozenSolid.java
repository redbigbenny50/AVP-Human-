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
        if (target.level().isClientSide || !target.isAlive()) {
            return false;
        }

        if (isFrozen(target)) {
            return true;
        }

        if (!canFreezeSolid(target)) {
            return false;
        }

        target.addEffect(new MobEffectInstance(HumanMobEffects.getFrozenSolidHolder(), FROZEN_TICKS, 0, false, false, true));
        track(target);

        return true;
    }

    /** {@return whether the rule lets this mob be frozen solid right now} */
    public static boolean canFreezeSolid(LivingEntity target) {
        if (target instanceof Player || isImmune(target)) {
            return false;
        }

        return !isHostile(target) || target.getHealth() <= target.getMaxHealth() * HEALTH_FRACTION;
    }

    /**
     * {@return whether this mob follows the 10% rule rather than freezing at once} — every vanilla hostile, and the
     * marines: [stated] "marines are hostile and should follow the 10%". A marine is a PathfinderMob, not an Enemy, so
     * it has to be named here; without it any freeze would turn a full-health marine into a statue.
     */
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
