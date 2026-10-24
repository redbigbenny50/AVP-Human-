package com.human.common.gameplay.entity.living.human.marine;

import com.blib.api.common.codec.v1.BLibCodecs;
import com.blib.api.common.entity.v1.BiomeSenseCache;
import com.blib.api.common.entity.v1.EntitySenseCache;
import com.blib.api.common.entity.v1.EntitySenseCacheUser;
import com.blib.api.common.entity.v1.PlayerStatConstants;
import com.blib.api.common.goap.v1.GOAPUser;
import com.blib.api.common.inventory.v1.BLibInventory;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.blib.api.common.item.v1.ItemUtil;
import com.human.Human;
import com.human.common.data.HumanAdvancements;
import com.human.common.gameplay.entity.living.human.AbstractHuman;
import com.human.common.gameplay.entity.living.human.marine.ai.MarineGOAP;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_fire_resistance.strategy.FRIStrategySet;
import com.human.common.gameplay.entity.living.human.marine.ai.acquire_water_breathing.strategy.WBIStrategySet;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.WeaponStrategySet;
import com.human.common.gameplay.entity.living.human.marine.ai.equip_armor.strategy.ArmorStrategySet;
import com.human.common.gameplay.entity.living.human.marine.ai.heal_self.strategy.HealingStrategySet;
import com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter;
import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.ItemCooldownUser;
import com.human.common.gameplay.menu.marine.MarineInventoryContainer;
import com.human.common.gameplay.menu.marine.MarineInventoryMenu;
import com.human.common.network.packet.S2CMarineSentryFilterPayload;
import com.human.common.registry.init.HumanDataComponents;
import com.human.compatibility.avp_predator.HumanPredatorNet;
import com.just.ai.goap.Agent;
import com.just.ai.goap.graph.Graph;
import com.just.codec.impl.Codecs;
import com.just.core.functional.option.Option;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class Marine extends AbstractHuman implements BLibInventoryHolder, EntitySenseCacheUser, GOAPUser<Marine>, ItemCooldownUser {

    private static final String NBT_INVENTORY = "inventory";

    private static final String NBT_LEADER_UUID = "leaderUUID";

    private static final String NBT_SQUAD_LEADER_UUID = "squadLeaderUUID";

    private static final String NBT_RESUPPLY_TICK = "resupplyTick";

    private static final String NBT_MODE = "mode";

    private static final String NBT_SENTRY_POS = "sentryPos";

    private static final String NBT_SENTRY_FILTER = "sentryFilter";

    public static AttributeSupplier.Builder createMarineAttributes() {
        var builder = Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE);

        builder.add(Attributes.ARMOR, 2.0F);
        builder.add(Attributes.ARMOR_TOUGHNESS, 0f);
        builder.add(Attributes.ATTACK_DAMAGE, PlayerStatConstants.BASE_HEALTH * 0.1F);
        builder.add(Attributes.FOLLOW_RANGE, 20F);
        builder.add(Attributes.KNOCKBACK_RESISTANCE, 0f);
        builder.add(Attributes.MAX_HEALTH, PlayerStatConstants.BASE_HEALTH);
        builder.add(Attributes.MOVEMENT_SPEED, PlayerStatConstants.BASE_SPRINT_JUMP_SPEED);

        return builder;
    }

    private final MarineAnimationDispatcher animationDispatcher;

    private final BiomeSenseCache biomeSenseCache;

    private final EntitySenseCache entitySenseCache;

    private final BLibInventory inventory;

    private final ItemCooldowns itemCooldowns;

    /** Oct 5 - this marine's target scan state (MarineTargetScan). Server side only; not saved. */
    private final com.human.common.gameplay.entity.living.human.marine.ai.MarineTargetScan targetScan =
        new com.human.common.gameplay.entity.living.human.marine.ai.MarineTargetScan();

    private Option<UUID> leaderUUIDOption;

    /** The MARINE this one walks behind on patrol. Separate from the employer above - see getSquadLeaderUUID. */
    private Option<UUID> squadLeaderUUIDOption;

    /** One-shot flag for the legacy migration in tick(); see migrateLegacySquadLeader. */
    private boolean squadLeaderMigrationChecked;

    private MarineMode mode;

    /**
     * How many players currently have this marine's inventory screen open. Not persisted: a marine that was being
     * rummaged through when the world unloaded is nobody's business by the time it loads again.
     */
    private int containerOpenCount;

    private final MarineResupply resupply = new MarineResupply();

    /**
     * Oct 5 audit - targets this marine has given up on, by UUID, with the tick the give-up expires. Server only, not
     * saved; a handful of entries at most, pruned as they expire.
     */
    private final java.util.Map<java.util.UUID, Integer> unreachableTargets = new java.util.HashMap<>();

    /** Oct 5 audit - the target the chase-progress fields below belong to. */
    private @org.jetbrains.annotations.Nullable java.util.UUID chaseTargetId;

    /** Oct 5 audit - closest (squared) this marine has got to {@link #chaseTargetId}. */
    private double chaseBestDistanceSqr;

    /** Oct 5 audit - the tick it last got meaningfully closer. */
    private int chaseLastProgressTick;

    /** Review fix 2 - the last tick the chase was recorded at all. */
    private int chaseLastRecordTick;

    /** Oct 5 audit - the last tick this marine had an attack target (tactical reload waits for quiet). */
    private int lastTargetTick = Integer.MIN_VALUE / 2;

    /** Oct 5 squad tactics - this marine's squad, refreshed by MarineSquads every few ticks. Null when on its own. */
    private com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads.@org.jetbrains.annotations.Nullable Squad squad;

    /** Oct 5 - the hurt timestamp last called out to the squad. */
    private int lastCalledOutHurtStamp = Integer.MIN_VALUE;

    /** Oct 5 - pat-down: the burning friend this marine is going to, and how long it has been patting. */
    private @org.jetbrains.annotations.Nullable java.util.UUID patTargetId;

    private int patTicks;

    /** Oct 5 - next tick this marine may throw a grenade. */
    private int nextGrenadeTick;

    /** Oct 5 - the tick this marine last poured water on itself, to check it worked; 0 when not checking. */
    private int bucketCheckTick;

    /**
     * Where a sentry was posted. Only meaningful while the mode is SENTRY, and kept rather than cleared when the mode
     * changes so that toggling sentry off and on again returns the marine to the same post.
     */
    private Option<BlockPos> sentryPosOption = Option.none();

    private SentryTargetFilter sentryFilter = new SentryTargetFilter();

    public Marine(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.animationDispatcher = new MarineAnimationDispatcher(this);
        // Replaces the vanilla control so a fighting withdrawal can strafe at a chosen speed instead of the
        // hardcoded quarter speed. Behaves identically for every other kind of movement.
        this.moveControl = new MarineMoveControl(this);
        this.biomeSenseCache = BiomeSenseCache.builder(this)
            .withScanRadius(4)
            .withRefreshPolicy(cache -> cache.getEntity().tickCount - cache.getLastSenseTick() > 40)
            .build();
        this.entitySenseCache = EntitySenseCache.builder(this)
            .withScanRadius(40)
            .withRefreshPolicy(cache -> cache.getEntity().tickCount - cache.getLastSenseTick() > 40)
            .build();
        // Sized from the menu constant so the container and the screen can never disagree about the pack size.
        this.inventory = new BLibInventory(MarineInventoryMenu.MARINE_SLOT_COUNT);
        this.itemCooldowns = new ItemCooldowns();
        this.leaderUUIDOption = Option.none();
        this.squadLeaderUUIDOption = Option.none();
        this.mode = MarineMode.FOLLOW;
    }

    @Override
    public Agent.Builder<Marine> blib$applyGOAPAgentProperties(Agent.Builder<Marine> agentBuilder) {
        return MarineGOAP.applyAgentProperties(agentBuilder);
    }

    @Override
    public @Nullable Graph<Marine> blib$getGOAPGraphOrNull() {
        // Oct 5 - Stage 2: AI level of detail. A null graph makes BLib skip the agent this tick and keep its plans.
        // Only inside the entity tick: the GOAP debug overlay asks for the graph too, and must always get it.
        // ⚠ Review pass 2 - a field-craft move in progress (pat-down, fall-back, stepping off fire, backing off a
        // creeper)
        // pauses the agent: every GOAP move action re-issues its own path every few ticks, so without this an idle
        // marine's wander or a chase overwrote the field-craft path before the marine got anywhere.
        if (inEntityTick && tickCount < fieldcraftPauseUntil) {
            return null;
        }

        if (inEntityTick && !com.human.common.gameplay.entity.living.human.marine.ai.MarineAiLod.shouldRunAi(this)) {
            return null;
        }

        return MarineGOAP.GRAPH;
    }

    /** Oct 5 - alien truce: the game time until which this marine's own contact keeps it (MarineAlienTruce). */
    private long alienTruceUntil;

    /** {@return the game time until which this marine's own alien contact keeps the truce} */
    public long alienTruceUntil() {
        return alienTruceUntil;
    }

    /**
     * Sets how long this marine's own alien contact keeps the truce.
     *
     * @param until the game time
     */
    public void setAlienTruceUntil(long until) {
        alienTruceUntil = Math.max(alienTruceUntil, until);
    }

    /** True while super.tick() runs, which is where BLib runs the agent. */
    private boolean inEntityTick;

    /** No AI, in a net, or carried off by a xeno: measured at the start of each server tick. */
    private boolean captive;

    /** The agent is paused until this tick while a field-craft move runs (see blib$getGOAPGraphOrNull). */
    private int fieldcraftPauseUntil;

    /** Pauses the agent for a field-craft move; plans are kept and resume afterwards. */
    private void pauseAgentForFieldcraft(int ticks) {
        fieldcraftPauseUntil = Math.max(fieldcraftPauseUntil, tickCount + ticks);
    }

    /** Stage 2 AI level of detail: how often this marine's agent runs, in ticks (0 = not measured yet). */
    private int aiLodInterval;

    /** {@return how often this marine's agent runs, in ticks; 0 before the first measurement} */
    public int aiLodInterval() {
        return aiLodInterval;
    }

    /**
     * Sets how often this marine's agent runs.
     *
     * @param interval the interval, in ticks
     */
    public void setAiLodInterval(int interval) {
        aiLodInterval = interval;
    }

    /**
     * The tick the weapon action last ran. [stated] Oct 4: marines stood around with their guns raised at nothing - the
     * aim pose follows the head, so an idle marine glancing up at a player pointed its gun at them. The aim pose is the
     * vanilla "aggressive" flag, switched on by UseWeaponAction and off only in its finish callback - and the GOAP
     * library does NOT run finish callbacks when a plan is replaced or abandoned (it just drops it from the list:
     * checked in its bytecode). Any interrupted firefight left the flag on.
     */
    private int lastWeaponUseTick = Integer.MIN_VALUE / 2;

    /** How long after the weapon action last ran the aim pose drops, in ticks. */
    private static final int AIM_POSE_GRACE_TICKS = 10;

    /** The tick the follow-leader move last ran (it frees water while following; see markFollowingLeader). */
    private int lastFollowTick = Integer.MIN_VALUE / 2;

    /** Called each tick the follow-leader move runs. */
    public void markFollowingLeader() {
        lastFollowTick = tickCount;
    }

    /**
     * [stated] Oct 5: camp marines go back to their camp - "if its an option yes otherwise wait on shore". A marine
     * placed by one of our structures (camp, outpost, lab) remembers where it stood as HOME, worked out once on its
     * first tick from the structure around it. Kept through hiring: [stated] a dismissed marine warps back to its camp
     * "as long as its still there", once no player is looking. Null: no home - it stays where it is, as before.
     */
    private @org.jetbrains.annotations.Nullable net.minecraft.core.BlockPos campHome;

    /** The dimension home is in - a marine dismissed in another dimension never warps to these coordinates there. */
    private @org.jetbrains.annotations.Nullable net.minecraft.resources.ResourceLocation campHomeDimension;

    /** Whether the one-time home lookup has run (saved, so it never runs twice). */
    private boolean campHomeChecked;

    /**
     * Whether home is somewhere it can stand: dry, solid footing. [stated] Oct 5: a flooded home would loop - walk
     * home, end up in water, go to the shore, walk home again - so a flooded or blocked home is unusable and the marine
     * waits on the shore instead. Re-checked every {@value #HOME_RECHECK_TICKS} ticks, in case the water drains.
     */
    private boolean campHomeUsable = true;

    private int campHomeCheckedTick = Integer.MIN_VALUE / 2;

    /** Failed paths home in a row; after {@value #HOME_PATH_FAILURES} it rests until {@link #returnHomeRestUntil}. */
    private int homePathFailures;

    private int returnHomeRestUntil;

    /** Hired last tick - to notice the dismissal. */
    private boolean wasHired;

    /**
     * Whether {@link #wasHired} has been read from the marine yet. Not saved: on the first tick after spawning or
     * loading it takes the marine's actual hire state, so a load is never mistaken for a dismissal.
     */
    private boolean hireStateKnown;

    /** Dismissed with a home: warp there once no player is looking. */
    private boolean warpHomePending;

    private static final int HOME_RECHECK_TICKS = 1200;

    private static final int HOME_PATH_FAILURES = 3;

    private static final int HOME_REST_TICKS = 1200;

    /** A home still has its camp if one of these stands within this many blocks of it. */
    private static final int CAMP_PRESENCE_RADIUS = 12;

    private static final String CAMP_HOME_KEY = "CampHome";

    private static final String CAMP_HOME_DIMENSION_KEY = "CampHomeDimension";

    private static final String CAMP_HOME_CHECKED_KEY = "CampHomeChecked";

    private static final String WARP_HOME_PENDING_KEY = "WarpHomePending";

    /** Home, or null. */
    public @org.jetbrains.annotations.Nullable net.minecraft.core.BlockPos getCampHome() {
        return campHome;
    }

    /**
     * Whether an idle marine should walk home now: it has a home in THIS dimension, the home is usable, it is not
     * resting after failed paths, and it is further than {@code radius} away.
     */
    public boolean shouldReturnHome(double radius) {
        if (campHome == null || !isHomeDimension() || !campHomeUsable || tickCount < returnHomeRestUntil) {
            return false;
        }

        return distanceToSqr(campHome.getCenter()) > radius * radius;
    }

    /**
     * Oct 5 - whether this marine is at the camp or structure it spawned in: it has a home, is in that dimension, and
     * is within {@code radius} of it. Used for the faster camp resupply (MarineResupply).
     *
     * @param radius blocks from home
     * @return true when at home
     */
    public boolean isNearCampHome(double radius) {
        return campHome != null && isHomeDimension() && distanceToSqr(campHome.getCenter()) <= radius * radius;
    }

    /**
     * Oct 5 - whether this marine has nothing to fight with: every gun empty with nothing to reload, and no knife or
     * other melee weapon. It falls back to its camp instead of standing among its attackers (IdleSensors).
     *
     * @return true when disarmed
     */
    public boolean isDisarmed() {
        return resupply.isDisarmed();
    }

    /** How long a chase may go without getting closer before the target is given up on: five seconds. */
    public static final int CHASE_GIVE_UP_TICKS = 100;

    /** How long a given-up target is left alone: ten seconds. */
    public static final int UNREACHABLE_TARGET_TICKS = 200;

    /** Getting this much closer (squared blocks) counts as progress - about a block from a few blocks out. */
    private static final double CHASE_PROGRESS_SQR = 2.0;

    /**
     * Oct 5 audit - a chase that is going nowhere. [stated] "these are supposed to be trained soldiers": the move-into-
     * range action used to retry an unreachable target forever, so a marine with a knife stood frozen under a mob on a
     * ledge, and a gunner out of range across a ravine did the same. Called every tick of that action; returns true -
     * and marks the target unreachable for {@link #UNREACHABLE_TARGET_TICKS} - once {@link #CHASE_GIVE_UP_TICKS} pass
     * with no real progress toward it.
     *
     * @param target the target being chased
     * @return true when the chase should be abandoned
     */
    public boolean recordChaseAndCheckGiveUp(net.minecraft.world.entity.LivingEntity target) {
        var distanceSqr = distanceToSqr(target);
        // ⚠ Review fix 2 - A CHASE THAT WAS INTERRUPTED STARTS AFRESH. The marine closes in, shoots for a while (this
        // is not called while it is in range), the target backs off - and the chase resumed against the old best
        // distance and the old progress tick, so it "gave up" on the spot and ignored a perfectly reachable target.
        var interrupted = tickCount - chaseLastRecordTick > 10;
        chaseLastRecordTick = tickCount;

        if (interrupted || !target.getUUID().equals(chaseTargetId)) {
            chaseTargetId = target.getUUID();
            chaseBestDistanceSqr = distanceSqr;
            chaseLastProgressTick = tickCount;
            return false;
        }

        if (distanceSqr < chaseBestDistanceSqr - CHASE_PROGRESS_SQR) {
            chaseBestDistanceSqr = distanceSqr;
            chaseLastProgressTick = tickCount;
            return false;
        }

        if (tickCount - chaseLastProgressTick < CHASE_GIVE_UP_TICKS) {
            return false;
        }

        unreachableTargets.put(target.getUUID(), tickCount + UNREACHABLE_TARGET_TICKS);
        chaseTargetId = null;

        if (getTarget() == target) {
            setTarget(null);
        }

        return true;
    }

    /**
     * {@return whether this marine has given up on {@code target} for now (see #recordChaseAndCheckGiveUp)}
     *
     * @param target the candidate
     */
    public boolean isGivenUpOn(net.minecraft.world.entity.LivingEntity target) {
        if (unreachableTargets.isEmpty()) {
            return false;
        }

        unreachableTargets.values().removeIf(until -> until <= tickCount);

        return unreachableTargets.containsKey(target.getUUID());
    }

    /** {@return ticks since this marine last had an attack target} */
    public int ticksSinceLastTarget() {
        return tickCount - lastTargetTick;
    }

    /**
     * Oct 5 audit - [stated] go-ahead to "knife-only marines never close on creepers, they back off". A marine with no
     * usable gun ignores creepers as targets (MarineGOAP.isAThreat) and, when one is within
     * {@value #CREEPER_EVADE_RADIUS} blocks, walks away from it. Checked twice a second.
     */
    private void evadeCreeperWhenUnarmed() {
        if (tickCount % 10 != 0 || !isOutOfAmmunition()) {
            return;
        }

        var nearby = level().getEntitiesOfClass(
            net.minecraft.world.entity.monster.Creeper.class,
            getBoundingBox().inflate(CREEPER_EVADE_RADIUS),
            net.minecraft.world.entity.LivingEntity::isAlive
        );

        if (nearby.isEmpty()) {
            return;
        }

        var creeper = nearby.get(0);
        var away = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(this, 10, 4, creeper.position());

        if (away != null) {
            // Review pass 3 - pause only when there is a path; a failed move must not leave the marine frozen.
            if (getNavigation().moveTo(away.x, away.y, away.z, 1.2D)) {
                pauseAgentForFieldcraft(10);
            }
        }
    }

    /** {@return this marine's squad, or null} */
    public com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads.@org.jetbrains.annotations.Nullable Squad getSquad() {
        return squad;
    }

    /**
     * Set by MarineSquads.
     *
     * @param squad the squad, or null
     */
    public void setSquad(
        com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads.@org.jetbrains.annotations.Nullable Squad squad
    ) {
        this.squad = squad;
    }

    /** Pat-down reach and search radius, in blocks. */
    private static final double PAT_REACH = 2.0;

    private static final double PAT_SEARCH_RADIUS = 8.0;

    /** Pat-down takes this many checks (10 ticks each): one second at the friend's side. */
    private static final int PAT_CHECKS_TO_EXTINGUISH = 2;

    /** Grenades: cluster size, throw distance and the pause between throws. */
    private static final int GRENADE_CLUSTER = 3;

    private static final double GRENADE_MIN_DISTANCE = 8.0;

    private static final double GRENADE_MAX_DISTANCE = 20.0;

    private static final int GRENADE_COOLDOWN_TICKS = 200;

    /** Burning friends being helped, so two marines do not both run to the same one: friend UUID to helper id. */
    private static final java.util.Map<java.util.UUID, java.lang.ref.WeakReference<Marine>> PAT_CLAIMS = new java.util.HashMap<>();

    /**
     * {@return the marine helping the friend with this UUID, or null} ⚠ Final review (memory) - claims hold the helper
     * weakly: this map is static, and a strong reference kept a closed world's marines - and the world - alive until
     * some marine somewhere happened to tick its pat-down again.
     */
    private static @org.jetbrains.annotations.Nullable Marine patClaimant(java.util.UUID friendId) {
        var ref = PAT_CLAIMS.get(friendId);
        return ref == null ? null : ref.get();
    }

    /**
     * Oct 5 - the squad and field-craft behaviours that are not GOAP plans: callouts, getting off a burning block, the
     * pat-down, flanking moves and grenades, plus checking that a bucket of water actually put the fire out. Each runs
     * on its own slow cadence.
     */
    private void tickSquadAndFieldcraft() {
        // Callout: a fresh hit is told to the squad at once.
        var hurtStamp = getLastHurtByMobTimestamp();

        if (hurtStamp != lastCalledOutHurtStamp && getLastHurtByMob() != null) {
            lastCalledOutHurtStamp = hurtStamp;
            com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads.callout(this, getLastHurtByMob());
        }

        // A bucket that did not put the fire out is remembered for this dimension.
        if (bucketCheckTick != 0 && tickCount >= bucketCheckTick) {
            bucketCheckTick = 0;

            // ⚠ Review fix - only conclude "water does not work here" from a marine that is STANDING IN its water and
            // still burning. One that walked out, or was re-lit by lava or a fire block, proves nothing about the
            // dimension, and one false mark would stop every marine using buckets there until restart.
            var standingInFire = level().getBlockState(blockPosition()).is(net.minecraft.tags.BlockTags.FIRE) || isInLava();

            if (isOnFire() && isInWater() && !standingInFire) {
                MarineBucketMemory.markWaterless(level());
            }
        }

        if ((tickCount + getId()) % 10 != 0) {
            return;
        }

        if (isOnFire()) {
            stepOffBurningGround();
        } else {
            tickPatDown();
        }

        if ((tickCount + getId()) % 20 == 0) {
            // Oct 5 - alien contact for the marine/yautja truce, once a second.
            MarineAlienTruce.tick(this);

            // Oct 5 - stranded at sea: unwatched, it is moved ashore (see tickStrandedRescue).
            tickStrandedRescue();

            // A squad falling back moves as a unit; flanking and spacing wait until it has regrouped.
            if (!tickFightingRetreat()) {
                tickFlank();
                tickSpacing();
            }

            tickGrenade();
        }
    }

    /**
     * Called by the water-at-feet action: check in two seconds whether the fire actually went out.
     */
    public void noteBucketAttempt() {
        bucketCheckTick = tickCount + 40;
    }

    /** {@return whether a squadmate has claimed this marine for a pat-down} */
    public boolean isBeingPattedDown() {
        var helper = patClaimant(getUUID());

        // Review fix 2 - a helper that cannot reach this marine (stuck, or far away) does not count: otherwise the
        // claim
        // alone kept it from using its own bucket while it burned.
        return helper != null && !helper.isRemoved() && helper.isAlive() && helper.level() == level()
            && helper.distanceToSqr(this) <= 144.0;
    }

    /** Standing in fire or lava, a burning marine moves off it first - otherwise nothing can put it out. */
    private void stepOffBurningGround() {
        var standingInFire = level().getBlockState(blockPosition()).is(net.minecraft.tags.BlockTags.FIRE) || isInLava();

        if (!standingInFire) {
            return;
        }

        var away = net.minecraft.world.entity.ai.util.LandRandomPos.getPos(this, 4, 2);

        if (away != null) {
            // Review pass 3 - pause only when there is a path; a failed move must not leave the marine frozen.
            if (getNavigation().moveTo(away.x, away.y, away.z, 1.2D)) {
                pauseAgentForFieldcraft(10);
            }
        }
    }

    /**
     * [stated] Oct 5: "for soldiers on fire with friends nearby have them pat down the one ignited and it extinguishes
     * the flame" - and "yes patdown for others and players". The nearest marine not fighting at arm's length walks over
     * and pats the friend out: other humans and players too. One helper per friend.
     */
    private void tickPatDown() {
        // Final review (TPS) - with no player near (see MarineAiLod) the burning-friend search runs every two seconds
        // instead of every half second, unless this marine is already helping someone.
        if (aiLodInterval > 1 && patTargetId == null && (tickCount + getId()) % 40 != 0) {
            return;
        }

        var target = getTarget();

        if (target != null && distanceToSqr(target) < 16.0) {
            releasePatClaim();
            return;
        }

        // Review fix - claims hold the helper itself, so a helper in another dimension is not mistaken for a dead one.
        PAT_CLAIMS.values().removeIf(ref -> {
            var helper = ref.get();
            return helper == null || helper.isRemoved() || !helper.isAlive();
        });

        var friend = findBurningFriend();

        if (friend == null) {
            releasePatClaim();
            return;
        }

        if (!friend.getUUID().equals(patTargetId)) {
            releasePatClaim();
            patTargetId = friend.getUUID();
            patTicks = 0;
            PAT_CLAIMS.put(friend.getUUID(), new java.lang.ref.WeakReference<>(this));
        }

        if (distanceToSqr(friend) > PAT_REACH * PAT_REACH) {
            if (getNavigation().moveTo(friend, 1.2D)) {
                pauseAgentForFieldcraft(10);
            }
            return;
        }

        getNavigation().stop();
        pauseAgentForFieldcraft(10);
        getLookControl().setLookAt(friend);
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);

        if (++patTicks >= PAT_CHECKS_TO_EXTINGUISH) {
            friend.clearFire();
            releasePatClaim();
        }
    }

    private @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity findBurningFriend() {
        var candidates = level().getEntitiesOfClass(
            net.minecraft.world.entity.LivingEntity.class,
            getBoundingBox().inflate(PAT_SEARCH_RADIUS),
            other -> other != this && other.isAlive() && other.isOnFire() && !other.fireImmune() && !other.isInLava()
                && !level().getBlockState(other.blockPosition()).is(net.minecraft.tags.BlockTags.FIRE)
                && isPatDownFriend(other)
                // Review pass 2 - a sentry or a marine told to hold only helps a friend right beside it.
                && ((!isSentry() && getMode() != MarineMode.HOLD) || distanceToSqr(other) <= 36.0)
        );

        net.minecraft.world.entity.LivingEntity best = null;
        var bestDistance = Double.MAX_VALUE;

        for (var candidate : candidates) {
            var claimedBy = patClaimant(candidate.getUUID());

            if (claimedBy != null && claimedBy != this) {
                continue;
            }

            var distance = distanceToSqr(candidate);

            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        return best;
    }

    private boolean isPatDownFriend(net.minecraft.world.entity.LivingEntity other) {
        // ⚠ Review pass 2 - never an enemy: the marine's own target, an attacker the squad called out, or anyone the
        // marines are at odds with. A burning player the squad was shooting used to get run over to and patted out.
        if (
            other == getTarget()
                || com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads.isCalledOut(this, other)
                || MarineHostility.hasBrokenAlliance(this, other)
        ) {
            return false;
        }

        if (other instanceof net.minecraft.world.entity.player.Player player) {
            return !player.isCreative() && !player.isSpectator();
        }

        return other instanceof com.human.common.gameplay.entity.living.human.AbstractHuman
            && !MarineHostility.hasBrokenAlliance(this, other);
    }

    private void releasePatClaim() {
        if (patTargetId != null) {
            if (patClaimant(patTargetId) == this) {
                PAT_CLAIMS.remove(patTargetId);
            }
            patTargetId = null;
            patTicks = 0;
        }
    }

    /**
     * Oct 5 - tactic 8, the fighting retreat. While the squad is falling back (MarineSquads.retreatPoint), half of it
     * moves toward the camp or leader while the other half holds and keeps shooting, swapping every two seconds - so
     * the squad never turns its back all at once.
     *
     * @return true while this marine's squad is falling back
     */
    private boolean tickFightingRetreat() {
        var point = com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads.retreatPoint(this);

        // Bug pass 1 - a sentry holds its post and a marine told to HOLD holds its ground; neither joins a retreat.
        if (point == null || isSentry() || getMode() == MarineMode.HOLD) {
            return false;
        }

        if (distanceToSqr(point) <= 6.0 * 6.0) {
            return true;
        }

        var movingHalf = ((tickCount / 40) + getId()) % 2 == 0;

        if (movingHalf) {
            if (getNavigation().moveTo(point.x, point.y, point.z, 1.2D)) {
                pauseAgentForFieldcraft(20);
            }
        } else {
            getNavigation().stop();
        }

        return true;
    }

    /**
     * Oct 5 - tactic 6, spacing. A marine in a fight standing within {@value #SPACING_RADIUS} blocks of another steps a
     * couple of blocks apart, so one creeper, acid spray or grenade cannot take out the group. Only when it is not
     * already moving, so it never fights a path it is on.
     */
    private void tickSpacing() {
        if (getTarget() == null || !getNavigation().isDone()) {
            return;
        }

        var crowding = level().getEntitiesOfClass(
            Marine.class,
            getBoundingBox().inflate(SPACING_RADIUS),
            other -> other != this && other.isAlive()
        );

        if (crowding.isEmpty()) {
            return;
        }

        var away = position().subtract(crowding.get(0).position());
        var flat = new net.minecraft.world.phys.Vec3(away.x, 0.0, away.z);

        if (flat.lengthSqr() < 1.0E-4) {
            var angle = getRandom().nextDouble() * Math.PI * 2.0;
            flat = new net.minecraft.world.phys.Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        }

        var step = flat.normalize().scale(2.5);
        var dest = position().add(step);
        var feet = net.minecraft.core.BlockPos.containing(dest);

        if (
            level().getBlockState(feet.below()).isFaceSturdy(level(), feet.below(), net.minecraft.core.Direction.UP)
                && level().getBlockState(feet).getCollisionShape(level(), feet).isEmpty()
                && level().getFluidState(feet).isEmpty()
        ) {
            getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0D);
        }
    }

    /** Tactic 6 - how close two marines may stand in a fight, in blocks. */
    private static final double SPACING_RADIUS = 1.6;

    /**
     * Oct 5 squad tactics - anchor and flank. A flanker already within its gun's range of the big threat walks to the
     * flank spot the squad gave it, firing as it goes. Out of range it closes in the ordinary way first.
     */
    private void tickFlank() {
        var spot = com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads.flankPosition(this);
        var target = getTarget();

        // Review fix - a sentry keeps to its post; the leash rules, not the flank.
        if (
            spot == null || target == null || isSentry()
                || !(getMainHandItem().getItem() instanceof com.human.common.gameplay.item.GunItem gunItem)
        ) {
            return;
        }

        var range = gunItem.getGunConfig().getDefaultFireMode().range();

        if (distanceToSqr(target) > range * range || distanceToSqr(spot) < 2.5 * 2.5) {
            return;
        }

        getNavigation().moveTo(spot.x, spot.y, spot.z, 1.1D);
    }

    /**
     * Oct 5 squad tactics - a grenade into a cluster of {@value #GRENADE_CLUSTER} or more enemies, never danger-close:
     * the cluster is {@value #GRENADE_MIN_DISTANCE} to {@value #GRENADE_MAX_DISTANCE} blocks out and nothing friendly
     * is within
     * {@value com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.impl.GunStrategy#DANGER_CLOSE_IN_BLOCKS}
     * blocks of it. Marines are issued grenades and never threw them before.
     */
    private void tickGrenade() {
        var target = getTarget();

        if (target == null || tickCount < nextGrenadeTick || !getSensing().hasLineOfSight(target)) {
            return;
        }

        var grenade = com.human.common.registry.init.item.HumanItems.GRENADE.get();

        if (!getInventory().hasItem(grenade)) {
            return;
        }

        var distance = Math.sqrt(distanceToSqr(target));

        if (distance < GRENADE_MIN_DISTANCE || distance > GRENADE_MAX_DISTANCE) {
            return;
        }

        var dangerClose = com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.impl.GunStrategy.DANGER_CLOSE_IN_BLOCKS;
        var area = target.getBoundingBox().inflate(dangerClose);
        var nearby = level().getEntitiesOfClass(
            net.minecraft.world.entity.LivingEntity.class,
            area,
            net.minecraft.world.entity.LivingEntity::isAlive
        );
        var enemies = 0;

        for (var other : nearby) {
            if (other == this) {
                return;
            }

            if (com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.impl.GunStrategy.isFriendly(this, other)) {
                return;
            }

            // Final review (enemies) - counted by the marines' own hunting rule, not the bare Enemy marker: a netted or
            // carried-off captive xeno, a tamed or owned mob, or a neutral one that is not angry is not part of the
            // pack.
            if (
                other.distanceToSqr(target) <= 16.0
                    && (other == target
                        || MarineHuntRules.isHuntedOnSight(other)
                        || com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads.isCalledOut(this, other))
            ) {
                enemies++;
            }
        }

        if (enemies < GRENADE_CLUSTER) {
            return;
        }

        var thrown = new com.human.common.gameplay.entity.projectile.ThrownGrenade(level(), this);
        thrown.setItem(new net.minecraft.world.item.ItemStack(grenade));
        var dx = target.getX() - getX();
        var dz = target.getZ() - getZ();
        var dy = target.getY() - getEyeY();
        thrown.shoot(dx, dy + distance * 0.2, dz, (float) (0.6 + distance * 0.035), 2.0F);
        level().addFreshEntity(thrown);
        getInventory().removeItem(grenade, 1);
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        level().playSound(
            null,
            getX(),
            getY(),
            getZ(),
            net.minecraft.sounds.SoundEvents.EGG_THROW,
            net.minecraft.sounds.SoundSource.HOSTILE,
            0.5F,
            0.6F
        );
        nextGrenadeTick = tickCount + GRENADE_COOLDOWN_TICKS;
    }

    /** How close a creeper may get to a gunless marine before it walks away, in blocks. */
    private static final double CREEPER_EVADE_RADIUS = 6.0;

    /** {@return whether every gun is empty with nothing to reload from, as of the last check (about once a second)} */
    public boolean isOutOfAmmunition() {
        return resupply.isOutOfAmmunitionCached();
    }

    /** {@return the ammunition items this marine's guns take, as of the last check} */
    public java.util.Set<net.minecraft.world.item.Item> getUsableAmmunition() {
        return resupply.getUsableAmmunition();
    }

    /** A path home failed; after a few in a row, rest a while instead of retrying. */
    public void homePathFailed() {
        if (++homePathFailures >= HOME_PATH_FAILURES) {
            homePathFailures = 0;
            returnHomeRestUntil = tickCount + HOME_REST_TICKS;
        }
    }

    /** A path home was found. */
    public void homePathFound() {
        homePathFailures = 0;
    }

    private boolean isHomeDimension() {
        return campHomeDimension == null || campHomeDimension.equals(level().dimension().location());
    }

    /** Once, on the first tick: home is here if one of our structures has a piece here. */
    private void findCampHomeOnce() {
        campHomeChecked = true;

        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        var pos = blockPosition();
        var structures = serverLevel.structureManager();
        var registry = serverLevel.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);

        for (var structure : structures.getAllStructuresAt(pos).keySet()) {
            var key = registry.getKey(structure);

            if (
                key != null
                    && com.human.Human.MOD_ID.equals(key.getNamespace())
                    && structures.getStructureWithPieceAt(pos, structure).isValid()
            ) {
                campHome = pos.immutable();
                campHomeDimension = serverLevel.dimension().location();
                return;
            }
        }
    }

    /** Dry, solid footing with room to stand, at home. Only asked when home is loaded. */
    private boolean isHomeStandable() {
        var level = level();
        var below = campHome.below();
        var head = campHome.above();

        return level.getFluidState(campHome).isEmpty()
            && level.getFluidState(head).isEmpty()
            && level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)
            && level.getBlockState(campHome).getCollisionShape(level, campHome).isEmpty()
            && level.getBlockState(head).getCollisionShape(level, head).isEmpty();
    }

    /** Whether the camp is still there: a campfire, a bed or tent padding near home. Scanned once per dismissal. */
    private boolean isCampStillThere() {
        var level = level();
        var cursor = new net.minecraft.core.BlockPos.MutableBlockPos();

        for (int dx = -CAMP_PRESENCE_RADIUS; dx <= CAMP_PRESENCE_RADIUS; dx++) {
            for (int dz = -CAMP_PRESENCE_RADIUS; dz <= CAMP_PRESENCE_RADIUS; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    cursor.set(campHome.getX() + dx, campHome.getY() + dy, campHome.getZ() + dz);
                    var state = level.getBlockState(cursor);

                    var campBlock = state.is(net.minecraft.tags.BlockTags.CAMPFIRES)
                        || state.is(net.minecraft.tags.BlockTags.BEDS);

                    if (campBlock) {
                        return true;
                    }

                    var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());

                    if (com.human.Human.MOD_ID.equals(id.getNamespace()) && id.getPath().contains("padding")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** Oct 5 - stranded rescue: how long a marine must have been in water before it is moved ashore, in ticks. */
    private static final int STRANDED_RESCUE_TICKS = 60 * 20;

    /** How far the rescue looks for dry land, in blocks; every {@value #STRANDED_SHORE_STEP}th column. */
    private static final int STRANDED_SHORE_RADIUS = 160;

    private static final int STRANDED_SHORE_STEP = 4;

    /** No player may be this close when the marine is moved, watching or not. */
    private static final double STRANDED_NO_PLAYER_RADIUS = 48.0;

    /**
     * Oct 6 - [stated] tester: marines "teleport alot whilst swimming". A marine merely swimming across open water for
     * a minute counted as stranded. Stranded now also means it has not got anywhere: less than this far (horizontally)
     * from where its minute in the water began.
     */
    private static final double STRANDED_MAX_DRIFT = 6.0;

    /** Game time it last entered water; -1 while dry. */
    private long inWaterSince = -1L;

    /** Where it was when the current in-water minute began (for STRANDED_MAX_DRIFT). */
    private double inWaterSinceX;

    private double inWaterSinceZ;

    /** Oct 5 - damage multipliers for everything a marine deals (MixinLivingEntity_MarineDamageBonus). */
    public static final float DAMAGE_VS_YAUTJA = 2.0F;

    public static final float DAMAGE_VS_XENOS = 1.5F;

    /**
     * Oct 5 - [stated] "like the alien a marine stranded in the ocean warps to land when not observed. this guy has
     * been here for many mc days." A marine that has been in water for {@value #STRANDED_RESCUE_TICKS} ticks straight,
     * with no fight on, no player leading it, not posted or told to hold, and nobody watching or within
     * {@value #STRANDED_NO_PLAYER_RADIUS} blocks, is moved to the nearest dry ground within
     * {@value #STRANDED_SHORE_RADIUS} blocks - or home to its camp when the sea is wider than that. Same idea as the
     * xenomorph's unobserved crossing; never in front of anyone, never loads a chunk to look.
     */
    private void tickStrandedRescue() {
        var now = level().getGameTime();

        if (!isInWater()) {
            inWaterSince = -1L;
            return;
        }

        if (inWaterSince < 0L) {
            inWaterSince = now;
            inWaterSinceX = getX();
            inWaterSinceZ = getZ();
            return;
        }

        if (now - inWaterSince < STRANDED_RESCUE_TICKS) {
            return;
        }

        var driftX = getX() - inWaterSinceX;
        var driftZ = getZ() - inWaterSinceZ;

        if (driftX * driftX + driftZ * driftZ > STRANDED_MAX_DRIFT * STRANDED_MAX_DRIFT) {
            // Swimming somewhere, not stranded: start a fresh minute from here.
            inWaterSince = now;
            inWaterSinceX = getX();
            inWaterSinceZ = getZ();
            return;
        }

        if (
            getTarget() != null
                || isPassenger()
                || isVehicle()
                || isLeashed()
                || isSentry()
                || getMode() == MarineMode.HOLD
                || getLeader().isSomeAnd(leader -> leader instanceof net.minecraft.world.entity.player.Player)
                || level().getNearestPlayer(this, STRANDED_NO_PLAYER_RADIUS) != null
                || isWatchedByAPlayer()
        ) {
            return;
        }

        var shore = findDryGround();

        if (shore == null && getCampHome() != null && isHomeDimension()) {
            shore = getCampHome();
        }

        // Try again a full interval later either way, so a marine far out at sea costs one search a minute.
        inWaterSince = now;
        inWaterSinceX = getX();
        inWaterSinceZ = getZ();

        if (shore == null) {
            return;
        }

        getNavigation().stop();
        teleportTo(shore.getX() + 0.5, shore.getY(), shore.getZ() + 0.5);
        setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        resetFallDistance();
    }

    /** The nearest dry, standable surface within the rescue radius, ring by ring; unloaded columns are skipped. */
    private @org.jetbrains.annotations.Nullable net.minecraft.core.BlockPos findDryGround() {
        var level = level();
        var origin = blockPosition();

        for (var ring = STRANDED_SHORE_STEP; ring <= STRANDED_SHORE_RADIUS; ring += STRANDED_SHORE_STEP) {
            net.minecraft.core.BlockPos best = null;
            var bestDistSqr = Double.MAX_VALUE;

            for (var dx = -ring; dx <= ring; dx += STRANDED_SHORE_STEP) {
                for (var dz = -ring; dz <= ring; dz += STRANDED_SHORE_STEP) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }

                    var x = origin.getX() + dx;
                    var z = origin.getZ() + dz;

                    if (!level.hasChunk(x >> 4, z >> 4)) {
                        continue;
                    }

                    // MOTION_BLOCKING counts fluids, so over water this is the water's surface - rejected below.
                    var y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    var feet = new net.minecraft.core.BlockPos(x, y, z);
                    var floor = feet.below();

                    if (
                        level.getFluidState(floor).isEmpty()
                            && level.getFluidState(feet).isEmpty()
                            && level.getBlockState(floor).isFaceSturdy(level, floor, net.minecraft.core.Direction.UP)
                            && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                            && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                    ) {
                        var distSqr = feet.distSqr(origin);

                        if (distSqr < bestDistSqr) {
                            best = feet;
                            bestDistSqr = distSqr;
                        }
                    }
                }
            }

            if (best != null) {
                return best;
            }
        }

        return null;
    }

    /** Whether any player is watching this marine: near, facing it within about 60 degrees, and with clear sight. */
    private boolean isWatchedByAPlayer() {
        for (var player : level().players()) {
            if (player.isSpectator() || player.distanceToSqr(this) > 64.0 * 64.0) {
                continue;
            }

            var toMarine = getEyePosition().subtract(player.getEyePosition()).normalize();

            if (player.getViewVector(1.0F).dot(toMarine) > 0.5 && player.hasLineOfSight(this)) {
                return true;
            }
        }

        return false;
    }

    /** The home bookkeeping each server tick: lookup, usability, dismissal, and the unseen warp home. */
    private void tickCampHome() {
        if (!campHomeChecked) {
            findCampHomeOnce();
        }

        var hired = hasLeader();

        if (!hireStateKnown) {
            hireStateKnown = true;
            wasHired = hired;
        }

        if (wasHired && !hired && campHome != null) {
            warpHomePending = true;
        }

        wasHired = hired;

        if (hired) {
            warpHomePending = false;
        }

        if (campHome == null || !isHomeDimension()) {
            return;
        }

        if (tickCount - campHomeCheckedTick >= HOME_RECHECK_TICKS && level().isLoaded(campHome)) {
            campHomeCheckedTick = tickCount;
            campHomeUsable = isHomeStandable();
        }

        // Watch-checked once a second - the warp waits for nobody to be looking.
        if (warpHomePending && tickCount % 20 == 0 && !isWatchedByAPlayer()) {
            warpHomePending = false;

            if (!isCampStillThere()) {
                // The camp is gone: this marine has no home left to go back to.
                campHome = null;
                campHomeDimension = null;
                return;
            }

            campHomeUsable = isHomeStandable();

            if (campHomeUsable && distanceToSqr(campHome.getCenter()) > 24.0 * 24.0) {
                getNavigation().stop();
                teleportTo(campHome.getX() + 0.5, campHome.getY(), campHome.getZ() + 0.5);
            }
        }
    }

    /** The tick the standoff strafe last ran (it leaves sideways input set; see markStandoff). */
    private int lastStandoffTick = Integer.MIN_VALUE / 2;

    /** Called each tick the standoff action strafes. */
    public void markStandoff() {
        lastStandoffTick = tickCount;
    }

    /**
     * A water block a marine placed (putting out a fire, breaking a fall) and has not taken back yet, and the tick its
     * action last ran. [stated] Oct 5 review: the actions take the water back in their finish callback, which the GOAP
     * library skips when a plan is replaced - so the water stayed in the world and the bucket was lost.
     */
    private @org.jetbrains.annotations.Nullable net.minecraft.core.BlockPos placedWaterPos;

    private int placedWaterTick;

    /** The water was placed (call each tick the action runs, so the fallback below waits for it). */
    public void trackPlacedWater(net.minecraft.core.BlockPos pos) {
        placedWaterPos = pos;
        placedWaterTick = tickCount;
    }

    /** The action took its water back itself. */
    public void clearPlacedWater() {
        placedWaterPos = null;
    }

    /** Mirrors the water actions' own clean-up: lift the water back into a bucket if it is still there and in reach. */
    private void reclaimPlacedWater(net.minecraft.core.BlockPos pos) {
        var level = level();

        var stillWater = level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.WATER);

        if (stillWater && distanceToSqr(pos.getCenter()) <= 9.0) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            getInventory().addItem(net.minecraft.world.item.Items.WATER_BUCKET);
        } else {
            getInventory().addItem(net.minecraft.world.item.Items.BUCKET);
        }

        // The action left the emptied bucket in hand; it is accounted for above, so do not keep both.
        if (getMainHandItem().is(net.minecraft.world.item.Items.BUCKET)) {
            setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }

    /** How long after its action stops before an orphaned water block is taken back, in ticks. */
    private static final int PLACED_WATER_GRACE_TICKS = 40;

    /** How long after the last shot a remembered target is let go, in ticks. */
    private static final int TARGET_FORGET_TICKS = 100;

    /** Called each tick the weapon action runs. */
    public void markWeaponUse() {
        lastWeaponUseTick = tickCount;
    }

    @Override
    public void tick() {
        // Oct 5 - profiler v3 laps (/blib perf "inside the tick"); free while no session runs. Never across super.tick.
        var perfLap = com.blib.api.common.perf.v1.BLibPerf.start();

        if (!level().isClientSide) {
            migrateLegacySquadLeader();

            tickCampHome();
            perfLap = com.blib.api.common.perf.v1.BLibPerf.lap(this, "marine.campHome", perfLap);

            // The aim pose ends when the fighting does, however the fighting ended (see lastWeaponUseTick).
            if (isAggressive() && tickCount - lastWeaponUseTick > AIM_POSE_GRACE_TICKS) {
                setAggressive(false);
            }

            // The standoff strafe leaves sideways input that vanilla never resets (it only fades), so once the standoff
            // stops - however it stopped - zero it, once.
            if (tickCount - lastStandoffTick == AIM_POSE_GRACE_TICKS) {
                setXxa(0.0F);
                setZza(0.0F);
            }

            // Water a marine placed and never took back (its action was replaced mid-way): take it back now.
            if (placedWaterPos != null && tickCount - placedWaterTick > PLACED_WATER_GRACE_TICKS) {
                reclaimPlacedWater(placedWaterPos);
                placedWaterPos = null;
            }

            // [stated] Oct 5: forget a target that is gone, or that it has not shot at for a while. The sensor that
            // sets the target never clears it, so marines kept aiming at an old enemy long after the fight moved on.
            var rememberedTarget = getTarget();

            if (
                rememberedTarget != null
                    && (!rememberedTarget.isAlive() || tickCount - lastWeaponUseTick > TARGET_FORGET_TICKS)
            ) {
                setTarget(null);
            }

            // Following frees water for pathing; once the following stops, however it stopped, water costs again.
            if (
                tickCount - lastFollowTick > AIM_POSE_GRACE_TICKS
                    && getPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER) != DEFAULT_WATER_MALUS
            ) {
                setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, DEFAULT_WATER_MALUS);
            }

            // Re-asserted every tick from the open count rather than toggled on open and close. The count is then
            // the only state that matters, and no dropped connection, crash or missed close callback can strand a
            // marine with its brain switched off.
            //
            // ⚠⚠ AND-ED WITH THE NET. avp_predator's net holds a mob by switching this same agent off; re-asserting
            // from the open count alone switched it straight back on, so a netted marine kept fighting through the
            // mesh. The net's own release restores the agent, and the re-assertion here now agrees with it.
            // ⚠⚠ Oct 5 - AND-ED WITH BEING CARRIED OFF BY A XENO, for the same reason as the net: avp_alien switches a
            // captured host's agent off and this re-assertion switched it back on, so a marine on a drone's back kept
            // sensing - and once marines hunted xenos on sight, it shot the xeno carrying it. See AlienCaptives.
            var agent = blib$getGOAPAgentOrNull();
            var netted = HumanPredatorNet.isNetted(this);
            var carriedOff = com.human.compatibility.avp_alien.AlienCaptives.isCarriedOff(this);

            if (agent != null) {
                agent.setEnabled(containerOpenCount <= 0 && !netted && !carriedOff);
            }

            if (containerOpenCount > 0 || netted || carriedOff) {
                getNavigation().stop();
            }

            captive = isNoAi() || netted || carriedOff;
            com.blib.api.common.perf.v1.BLibPerf.lap(this, "marine.preTickChecks", perfLap);
        }

        // Stage 2 AI level of detail gates the agent only inside the entity tick (see blib$getGOAPGraphOrNull).
        inEntityTick = true;

        try {
            super.tick();
        } finally {
            inEntityTick = false;
        }

        if (!level().isClientSide) {
            perfLap = com.blib.api.common.perf.v1.BLibPerf.start();
            reassertAimAfterMovement();

            itemCooldowns.tick();
            resupply.tick(this);
            perfLap = com.blib.api.common.perf.v1.BLibPerf.lap(this, "marine.aim+cooldowns+resupply", perfLap);

            if (getTarget() != null) {
                lastTargetTick = tickCount;
            }

            // ⚠ Final review (ticking) - a marine that cannot act takes no part in squad or field-craft behaviour: no
            // grenades from a marine in a net or carried off by a xeno, none from a NoAI display marine, no pat-downs,
            // no radio, no place in the squad's numbers. One with a player in its inventory screen keeps its squad
            // place but does nothing until the screen closes.
            if (captive) {
                setSquad(null);
                releasePatClaim();
            } else {
                com.human.common.gameplay.entity.living.human.marine.squad.MarineSquads.tick(this);
                perfLap = com.blib.api.common.perf.v1.BLibPerf.lap(this, "marine.squads", perfLap);

                if (containerOpenCount <= 0) {
                    evadeCreeperWhenUnarmed();
                    tickSquadAndFieldcraft();
                }
            }

            perfLap = com.blib.api.common.perf.v1.BLibPerf.lap(this, "marine.fieldcraft", perfLap);

            // Tick items in hand. This prevents gun muzzle flashes from getting stuck when held by marines.
            var mainHandItemStack = getMainHandItem();
            mainHandItemStack.inventoryTick(level(), this, EquipmentSlot.MAINHAND.getIndex(), true);
            var offHandItemStack = getOffhandItem();
            offHandItemStack.inventoryTick(level(), this, EquipmentSlot.OFFHAND.getIndex(), true);
            com.blib.api.common.perf.v1.BLibPerf.lap(this, "marine.heldItemTicks", perfLap);
        }
    }

    /**
     * Puts the marine back on its target after the movement code has finished turning it away.
     * <p>
     * ⚠⚠ THE AIM IS SET EARLY IN THE TICK AND OVERWRITTEN LATE. The combat strategy calls {@code lookAt} while the GOAP
     * agent runs, at the very top of the tick - but {@code MoveControl}'s MOVE_TO branch, which runs afterwards inside
     * {@code serverAiStep}, does {@code setYRot(rotlerp(getYRot(), waypointYaw, 90))} every single tick. It turns the
     * marine to face wherever it is WALKING, by up to ninety degrees at a time, and it always gets the last word.
     * <p>
     * A marine that shoots while walking therefore ends up facing its path, not its enemy - and if the path leads away
     * from what it is shooting at, it faces the exact opposite direction. Both the held weapon and the shot follow
     * {@code yRot}, so the gun points backwards AND the bullets go backwards with it.
     * <p>
     * Running here is the whole fix: {@code super.tick()} has already returned, so the movement code has had its turn
     * and this is the last word instead. Only while actually engaging, so walking, idling and patrolling still turn the
     * marine to face where it is going.
     */
    private void reassertAimAfterMovement() {
        if (!isAggressive()) {
            return;
        }

        var target = getTarget();

        if (target == null || !target.isAlive()) {
            return;
        }

        lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

        // The arms are posed from the head-to-body difference, so squaring the body is what actually puts the rifle on
        // the target rather than out to one side.
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    /**
     * Keeps a hired marine in the world.
     * <p>
     * Without this vanilla despawning DELETES them - beyond 128 blocks, and also through the idle-too-long roll at
     * closer range - and a fresh unhired marine spawns later, which reads as the contract having been forgotten. The
     * contract itself was always saved correctly; the marine holding it simply stopped existing.
     * <p>
     * Read from the leader rather than latched with {@code setPersistenceRequired()}, which is deliberately one-way in
     * vanilla and has no matching clear. Answering the question from current state means ending a contract releases the
     * marine to ordinary despawning again with nothing to reset - so a player who hires and dismisses dozens over a
     * playthrough is not quietly filling their world with permanent ones.
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !hasLeader();
    }

    @Override
    public int getMaxFallDistance() {
        return inventory.hasItem(Items.WATER_BUCKET)
            ? 1024
            : super.getMaxFallDistance();
    }

    @Override
    public void runAttackAnimations() {
        animationDispatcher.rightShoot();
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        Arrays.stream(inventory.getSerializedItemStacks())
            .filter(itemStack -> !itemStack.has(HumanDataComponents.MARINE_OWNED.get()))
            .map(itemStack -> ItemUtil.drop(this, itemStack, true, false))
            .flatMap(Option::toStream)
            .forEach(itemEntity -> level().addFreshEntity(itemEntity));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
        @NotNull ServerLevelAccessor level,
        @NotNull DifficultyInstance difficulty,
        @NotNull MobSpawnType spawnType,
        @Nullable SpawnGroupData spawnGroupData
    ) {
        switch (spawnType) {
            case BUCKET,
                COMMAND,
                DISPENSER,
                MOB_SUMMONED,
                NATURAL,
                SPAWN_EGG,
                SPAWNER,
                TRIGGERED,
                // [stated] Oct 13 - each soldier type spawns in its own kit (SoldierProfiles); a marine's is unchanged.
                TRIAL_SPAWNER -> com.human.common.gameplay.entity.living.human.soldier.SoldierProfiles.of(getType())
                    .kit()
                    .decorate(level(), this);
            case BREEDING,
                CHUNK_GENERATION,
                CONVERSION,
                EVENT,
                JOCKEY,
                PATROL,
                REINFORCEMENT,
                STRUCTURE -> { /* NO-OP */ }
        }

        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand interactionHand) {
        var itemStack = player.getItemInHand(interactionHand);

        // ⭐ Oct 5 - ANY PLAYER CAN HAND OVER AMMUNITION, hired marine or not. [stated] "make it so you can right click
        // a
        // marine with the right ammo and he will take it to reload". The whole stack in hand is taken, as much as the
        // pack holds; only ammunition one of its guns fires is accepted. It stays the player's (not marine-owned), so
        // it drops if the marine dies. An empty gun loads from it on its own within a second or two (MarineResupply).
        // The client only knows it is ammunition, not whether this marine can use it, so the server decides.
        if (MarineResupply.isGunAmmunition(itemStack)) {
            if (level().isClientSide) {
                return InteractionResult.SUCCESS;
            }

            if (!MarineResupply.usesAmmunition(this, itemStack.getItem())) {
                return InteractionResult.PASS;
            }

            var offered = itemStack.copy();
            var addResult = inventory.addItemStack(offered);
            var taken = switch (addResult) {
                case BLibInventory.AddResult.Success success -> offered.getCount();
                case BLibInventory.AddResult.Partial partial -> partial.count();
                default -> 0;
            };

            if (taken <= 0) {
                return InteractionResult.PASS;
            }

            if (!player.getAbilities().instabuild) {
                itemStack.shrink(taken);
            }

            return InteractionResult.CONSUME;
        }

        if (itemStack.getItem() == Items.DIAMOND && !hasLeader()) {
            // [stated] Oct 13 - Weyland-Yutani soldiers only work under a company contract (not built yet), so they
            // turn the diamond down and the player keeps it.
            if (!com.human.common.gameplay.entity.living.human.soldier.SoldierHiring.canHire(this, player)) {
                if (!level().isClientSide) {
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                            com.human.common.gameplay.entity.living.human.soldier.SoldierHiring.CONTRACT_ONLY
                        ),
                        true
                    );
                }

                return InteractionResult.sidedSuccess(level().isClientSide);
            }

            itemStack.consume(1, player);
            setLeader(player);
            // Taking the contract means leaving the patrol - otherwise the sergeant keeps pulling him back.
            removeSquadLeader();

            if (!level().isClientSide) {
                HumanAdvancements.HIRE_MARINE.grant((ServerPlayer) player);
            }

            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Only leaders can give marines items.
        if (getLeaderUUID().isSomeAnd(uuid -> Objects.equals(uuid, player.getUUID()))) {
            if (
                itemStack.getItem() == Items.BONE
                    || itemStack.getItem() == Items.TORCH
                    || itemStack.getItem() == Items.TOTEM_OF_UNDYING
                    || itemStack.getItem() == Items.WATER_BUCKET
                    || ArmorStrategySet.INSTANCE.isAnyValidFor(itemStack)
                    || FRIStrategySet.INSTANCE.isAnyValidFor(itemStack)
                    || WBIStrategySet.INSTANCE.isAnyValidFor(itemStack)
                    || HealingStrategySet.INSTANCE.isAnyValidFor(itemStack)
                    || WeaponStrategySet.INSTANCE.isAnyValidFor(itemStack)
            ) {
                if (!level().isClientSide) {
                    var item = new ItemStack(itemStack.getItem(), 1);
                    item.applyComponents(itemStack.getComponents());
                    itemStack.consume(1, player);
                    inventory.addItemStack(item);
                }

                return InteractionResult.sidedSuccess(level().isClientSide);
            }

            if (itemStack.isEmpty()) {
                // Used to toggle follow/hold. That command now lives in the screen alongside room for more, which
                // frees the empty hand for the gesture players already expect to open a container.
                if (player instanceof ServerPlayer serverPlayer) {
                    openInventoryMenu(serverPlayer);
                }

                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }

        if (itemStack.getItem() instanceof GunItem) {
            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, interactionHand);
    }

    @Override
    public BLibInventory getInventory() {
        return inventory;
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.campHome = compoundTag.contains(CAMP_HOME_KEY)
            ? net.minecraft.core.BlockPos.of(compoundTag.getLong(CAMP_HOME_KEY))
            : null;
        this.campHomeDimension = compoundTag.contains(CAMP_HOME_DIMENSION_KEY)
            ? net.minecraft.resources.ResourceLocation.tryParse(compoundTag.getString(CAMP_HOME_DIMENSION_KEY))
            : null;
        this.campHomeChecked = compoundTag.getBoolean(CAMP_HOME_CHECKED_KEY);
        this.warpHomePending = compoundTag.getBoolean(WARP_HOME_PENDING_KEY);

        if (compoundTag.contains(NBT_INVENTORY)) {
            BLibInventory.CODEC.decode(BLibCodecs.Schema.NBT, compoundTag.get(NBT_INVENTORY))
                .inspectErr(tag -> Human.LOGGER.error("Failed to load tag '{}'. Tag: {}", NBT_INVENTORY, tag))
                .ifOk(loadedInventory -> Arrays.stream(loadedInventory.getSerializedItemStacks()).forEach(inventory::addItemStack));
        }

        // ⚠ The mode was never persisted before sentry existed, so a marine on HOLD quietly went back to FOLLOW every
        // time the world reloaded. MarineMode.CODEC had been written for this and never wired up.
        if (compoundTag.contains(NBT_MODE)) {
            MarineMode.CODEC.decode(BLibCodecs.Schema.NBT, compoundTag.get(NBT_MODE))
                .inspectErr(tag -> Human.LOGGER.error("Failed to load tag '{}'. Tag: {}", NBT_MODE, tag))
                .ifOk(loadedMode -> this.mode = loadedMode);
        }

        if (compoundTag.contains(NBT_SENTRY_FILTER)) {
            this.sentryFilter = SentryTargetFilter.load(compoundTag.getCompound(NBT_SENTRY_FILTER));
        }

        if (compoundTag.contains(NBT_SENTRY_POS)) {
            this.sentryPosOption = Option.some(NbtUtils.readBlockPos(compoundTag, NBT_SENTRY_POS).orElse(blockPosition()));
        }

        if (compoundTag.contains(NBT_RESUPPLY_TICK)) {
            resupply.setResupplyTick(tickCount + compoundTag.getInt(NBT_RESUPPLY_TICK));
        }

        if (compoundTag.contains(NBT_SQUAD_LEADER_UUID)) {
            Codecs.UUID.decode(BLibCodecs.Schema.NBT, compoundTag.get(NBT_SQUAD_LEADER_UUID))
                .ifOk(uuid -> this.squadLeaderUUIDOption = Option.ofNullable(uuid));
        }

        if (compoundTag.contains(NBT_LEADER_UUID)) {
            Codecs.UUID.decode(BLibCodecs.Schema.NBT, compoundTag.get(NBT_LEADER_UUID))
                .ifOk(uuid -> this.leaderUUIDOption = Option.ofNullable(uuid));
        }

        // Absent on every soldier saved before the split: resolved on first use instead (see faction()).
        this.faction = null;

        if (compoundTag.contains(NBT_FACTION)) {
            var id = compoundTag.getString(NBT_FACTION);

            for (var candidate : com.human.common.gameplay.fabrication.Faction.values()) {
                if (candidate.id().equals(id)) {
                    this.faction = candidate;
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);

        if (campHome != null) {
            compoundTag.putLong(CAMP_HOME_KEY, campHome.asLong());
        }

        if (campHomeDimension != null) {
            compoundTag.putString(CAMP_HOME_DIMENSION_KEY, campHomeDimension.toString());
        }

        compoundTag.putBoolean(CAMP_HOME_CHECKED_KEY, campHomeChecked);
        compoundTag.putBoolean(WARP_HOME_PENDING_KEY, warpHomePending);
        compoundTag.put(NBT_INVENTORY, BLibInventory.CODEC.encode(BLibCodecs.Schema.NBT, inventory));
        compoundTag.put(NBT_MODE, MarineMode.CODEC.encode(BLibCodecs.Schema.NBT, mode));
        sentryPosOption.ifSome(sentryPos -> compoundTag.put(NBT_SENTRY_POS, NbtUtils.writeBlockPos(sentryPos)));
        compoundTag.put(NBT_SENTRY_FILTER, sentryFilter.save());

        // Stored as ticks REMAINING rather than the absolute tick it lands on, because tickCount restarts at zero
        // when the entity is reloaded - an absolute deadline would already be in the past and resupply instantly.
        if (resupply.getResupplyTick() != MarineResupply.NO_RESUPPLY_PENDING) {
            compoundTag.putInt(NBT_RESUPPLY_TICK, Math.max(0, resupply.getResupplyTick() - tickCount));
        }
        leaderUUIDOption.ifSome(leaderUUID -> compoundTag.put(NBT_LEADER_UUID, Codecs.UUID.encode(BLibCodecs.Schema.NBT, leaderUUID)));
        compoundTag.putString(NBT_FACTION, faction().id());
        squadLeaderUUIDOption.ifSome(
            squadLeaderUUID -> compoundTag.put(
                NBT_SQUAD_LEADER_UUID,
                Codecs.UUID.encode(BLibCodecs.Schema.NBT, squadLeaderUUID)
            )
        );
    }

    // ------------------------------------------------------------------------------------------------------------
    // [stated] Oct 13 - the soldier split: faction and body
    // ------------------------------------------------------------------------------------------------------------

    private static final String NBT_FACTION = "SoldierFaction";

    /** Null until first asked; saved from then on. */
    private com.human.common.gameplay.fabrication.@Nullable Faction faction;

    private com.human.common.gameplay.entity.living.human.@Nullable HumanFeatureManager employeeFeatures;

    /**
     * This soldier's faction. Normally its type's ({@code SoldierProfiles}). A {@code marine} saved before the split
     * that is wearing Weyland-Yutani armour and has NO leader is a W-Y soldier from an old patrol and is recognised as
     * one; a hired marine always stays USCM, whatever it wears, so no player's squad can ever change sides.
     * <p>
     * Resolved on first use, never during loading (armour and leader must both be in), and kept only on the server: a
     * client asking before the equipment has arrived must not lock in a guess.
     * </p>
     */
    public com.human.common.gameplay.fabrication.Faction faction() {
        var known = faction;

        if (known != null) {
            return known;
        }

        var resolved = com.human.common.gameplay.entity.living.human.soldier.SoldierProfiles.of(getType()).faction();

        if (getType() == com.human.common.registry.init.HumanEntityTypes.MARINE.get() && !hasLeader() && wearsWeylandYutaniArmor()) {
            resolved = com.human.common.gameplay.fabrication.Faction.WY;
        }

        if (!level().isClientSide) {
            faction = resolved;
        }

        return resolved;
    }

    private boolean wearsWeylandYutaniArmor() {
        var chest = getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);

        return chest.is(com.human.common.registry.init.item.HumanArmorItems.WY_COMMANDO_CHESTPLATE.get())
            || chest.is(com.human.common.registry.init.item.HumanArmorItems.WY_ELITE_CHESTPLATE.get())
            || chest.is(com.human.common.registry.init.item.HumanArmorItems.WY_APE_CHESTPLATE.get());
    }

    /**
     * The Weyland-Yutani units wear the {@code weyland_yutani_employee} body (skin, hair, beard, eyes, jumpsuit) - the
     * colonist's look, [stated] "the colonist is a better match than the marine". Decided by type, which the client
     * knows without syncing anything.
     */
    @Override
    public com.human.common.gameplay.entity.living.human.HumanFeatureManager getHumanFeatureManager() {
        if (!com.human.common.gameplay.entity.living.human.soldier.SoldierProfiles.of(getType()).employeeBody()) {
            return super.getHumanFeatureManager();
        }

        var features = employeeFeatures;

        if (features == null) {
            features = new com.human.common.gameplay.entity.living.human.soldier.WeylandYutaniEmployeeFeatureManager(this);
            employeeFeatures = features;
        }

        return features;
    }

    public BiomeSenseCache getBiomeSenseCache() {
        return biomeSenseCache;
    }

    /** {@return this marine's target scan state} */
    public com.human.common.gameplay.entity.living.human.marine.ai.MarineTargetScan getTargetScan() {
        return targetScan;
    }

    @Override
    public EntitySenseCache getEntitySenseCache() {
        return entitySenseCache;
    }

    @Override
    public ItemCooldowns getItemCooldowns() {
        return itemCooldowns;
    }

    public Option<UUID> getLeaderUUID() {
        return leaderUUIDOption;
    }

    public void openInventoryMenu(ServerPlayer serverPlayer) {
        setContainerOpen(true);
        // Pushed as the screen opens so the sentry page shows what is actually set. The filter is too big for a
        // DataSlot - those go over the wire as shorts - so it travels as its own payload.
        Human.MOD.networking().sendToClient(serverPlayer, new S2CMarineSentryFilterPayload(sentryFilter.copy()));
        serverPlayer.openMenu(
            new SimpleMenuProvider(
                (containerId, playerInventory, $1) -> new MarineInventoryMenu(
                    containerId,
                    playerInventory,
                    new MarineInventoryContainer(this),
                    this
                ),
                getDisplayName()
            )
        );
    }

    public void setContainerOpen(boolean open) {
        containerOpenCount = open
            ? containerOpenCount + 1
            : Math.max(0, containerOpenCount - 1);
    }

    public boolean isContainerOpen() {
        return containerOpenCount > 0;
    }

    public boolean isLeader(Player player) {
        return getLeaderUUID().isSomeAnd(uuid -> Objects.equals(uuid, player.getUUID()));
    }

    public void setMode(MarineMode mode) {
        this.mode = mode;
    }

    public SentryTargetFilter getSentryFilter() {
        return sentryFilter;
    }

    public void setSentryFilter(SentryTargetFilter sentryFilter) {
        this.sentryFilter = sentryFilter;
    }

    public boolean isSentry() {
        return mode == MarineMode.SENTRY && sentryPosOption.isSome();
    }

    public Option<BlockPos> getSentryPos() {
        return sentryPosOption;
    }

    /**
     * Posts the marine where it currently stands. Called when sentry is switched on, so the player positions a sentry
     * by walking it there rather than by picking a block.
     */
    public void beginSentry() {
        this.sentryPosOption = Option.some(blockPosition());
        setMode(MarineMode.SENTRY);
    }

    /**
     * Stands the marine down. The post is deliberately KEPT: switching sentry back on without moving should return it
     * to the same spot, and re-posting only happens where it is standing at that moment anyway.
     */
    public void endSentry() {
        setMode(MarineMode.FOLLOW);
    }

    public MarineMode getMode() {
        return mode;
    }

    /**
     * ⭐⭐ THE SQUAD LEADER IS A DIFFERENT THING FROM THE EMPLOYER, AND CONFLATING THEM BROKE HIRING ENTIRELY.
     * <p>
     * {@code MarineSquadLeadershipDecorator} points every patrol member at another MARINE. While that shared
     * {@code leaderUUID} with the player contract, a naturally spawned squad member was permanently stuck: the
     * inventory gate compares the leader to the CLICKING PLAYER so it never matched, and the hire gate is
     * {@code !hasLeader()} which was already true - so the diamond was refused too. The tester's log showed exactly
     * that: 232 interactions, every one {@code leaderMatches=false}, against a leader UUID belonging to no player.
     * </p>
     * <p>
     * ⚠ TWO FIELDS, TWO MEANINGS: {@link #leaderUUIDOption} is the PLAYER who hired this marine (the contract, and what
     * the GUI keys off). {@code squadLeaderUUIDOption} is the marine it walks behind on patrol. Hiring CLEARS the squad
     * leader - taking the job means leaving the squad.
     * </p>
     */
    public Option<UUID> getSquadLeaderUUID() {
        return squadLeaderUUIDOption;
    }

    public void setSquadLeader(Entity entity) {
        this.squadLeaderUUIDOption = Option.some(entity.getUUID());
    }

    public void removeSquadLeader() {
        this.squadLeaderUUIDOption = Option.none();
    }

    /**
     * Whoever this marine currently answers to - the employer if hired, otherwise the squad leader.
     * <p>
     * ⚠ THIS IS WHAT THE COMBAT RULES WANT, NOT {@code getLeaderUUID()}. "Never shoot your leader", "defend whoever is
     * attacking your leader" and "do not retaliate against your leader" all have to hold for a squad marine that has
     * never been hired, or a patrol turns on its own sergeant. Same for {@code MarineHostility.sideOf}, where this
     * value IS the squad's identity.
     * </p>
     */
    public Option<UUID> getAllegianceUUID() {
        return leaderUUIDOption.isSome() ? leaderUUIDOption : squadLeaderUUIDOption;
    }

    /**
     * ⚠⚠ EXISTING WORLDS HAVE A MARINE UUID SITTING IN {@code leaderUUID}, AND WITHOUT THIS THEY STAY UNHIREABLE.
     * <p>
     * Every patrol marine spawned before the split wrote its SQUAD leader into the employer field. Nothing at load time
     * can tell a player UUID from an entity one - offline-mode players are v3 and online-mode players are v4, the same
     * version entities get - so the only reliable test is to RESOLVE it and see what comes back.
     * </p>
     * <p>
     * ⚠ Deliberately lazy and one-shot. It runs on the first server tick where the leader is resolvable; squads spawn
     * and travel together so the sergeant is almost always loaded alongside. If he never resolves (dead, or a genuinely
     * absent player) nothing moves and the marine keeps what it had - a wrong guess here would hand a stranger's marine
     * to the next person who walks past.
     * </p>
     */
    private void migrateLegacySquadLeader() {
        if (squadLeaderMigrationChecked || squadLeaderUUIDOption.isSome() || leaderUUIDOption.isNone()) {
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var leaderUUID = leaderUUIDOption.unwrap();
        var leaderEntity = serverLevel.getEntity(leaderUUID);

        if (leaderEntity == null) {
            return; // not loaded yet - try again next tick rather than guessing
        }

        squadLeaderMigrationChecked = true;

        if (leaderEntity instanceof Marine) {
            this.squadLeaderUUIDOption = Option.some(leaderUUID);
            this.leaderUUIDOption = Option.none();
        }
    }

    public Option<Entity> getLeader() {
        // TODO: Switch this from 'andThen' to 'map' once Just fixes null not being a valid return choice.
        return getAllegianceUUID().andThen(
            uuid -> Option.ofNullable(level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(uuid) : null)
        );
    }

    public boolean hasLeader() {
        return leaderUUIDOption.isSome();
    }

    /**
     * In the water a marine mirrors its allegiance - the hiring player, or its squad leader - so a hired marine swims
     * when the player swims and walks the bottom when the player does. See {@code HumanWaterMovement}.
     */
    @Override
    public @Nullable LivingEntity getWaterMovementLeaderOrNull() {
        return getLeader().unwrapOr(null) instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    public void setLeader(Entity entity) {
        setLeaderUUID(entity.getUUID());
    }

    public void setLeaderUUID(UUID leaderUUID) {
        this.leaderUUIDOption = Option.some(leaderUUID);
    }

    public void removeLeader() {
        this.leaderUUIDOption = Option.none();
    }

    /**
     * Ends the contract: the marine is no longer hired and goes back to being an ordinary patrol spawn.
     * <p>
     * Persistence is released along with the leader, deliberately. A dismissed marine that stayed persistent would
     * linger forever, and a player who hired and dismissed a few dozen over a playthrough would be quietly filling
     * their world with them.
     */
    public void endContract() {
        removeLeader();
        setMode(MarineMode.FOLLOW);
        // Forgotten here, unlike endSentry: a dismissed marine has no reason to remember a post it was set by someone
        // who is no longer its leader.
        this.sentryPosOption = Option.none();
    }

    /**
     * Whether the pack holds anything the PLAYER put there. Issued kit is flagged {@code MARINE_OWNED} and walks off
     * with the marine; anything else is the player's and would be lost with it.
     */
    public boolean carriesPlayerItems() {
        for (var slotIndex = 0; slotIndex < inventory.getSize(); slotIndex++) {
            var itemStack = inventory.getItemStack(slotIndex);

            if (!itemStack.isEmpty() && !itemStack.getOrDefault(HumanDataComponents.MARINE_OWNED.get(), false)) {
                return true;
            }
        }

        return false;
    }
}
