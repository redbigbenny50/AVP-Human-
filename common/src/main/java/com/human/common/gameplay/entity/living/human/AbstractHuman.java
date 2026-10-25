package com.human.common.gameplay.entity.living.human;

import com.blib.api.common.data_sync.v1.DataAccessor;
import com.blib.api.common.data_sync.v1.model.DataUser;
import com.blib.api.common.entity.v1.MovementAnalyzer;
import com.human.common.registry.init.HumanDataSyncKeys;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractHuman extends PathfinderMob implements DataUser {

    /**
     * The water cost humans path with: cheap, so they cross a pond rather than walk around it (vanilla's default is 8).
     * Following a leader drops it to 0 for a while; Marine.tick puts this back (see Marine.markFollowingLeader).
     */
    public static final float DEFAULT_WATER_MALUS = 0.5F;

    public static final int MAX_IDLE_TIME_IN_TICKS = 12 * 20;

    public static final int MIN_IDLE_TIME_IN_TICKS = 7 * 20;

    public final DataAccessor<Integer> beardVariant;

    public final DataAccessor<Integer> eyeColor;

    public final DataAccessor<Integer> hairColor;

    public final DataAccessor<Integer> hairVariant;

    public final DataAccessor<Boolean> isMale;

    public final DataAccessor<Integer> skinColor;

    /** Oct 5 - which wall a pit scramble faces (Direction 2D value), or -1. Written by HumanWallScramble only. */
    public final DataAccessor<Integer> scrambleDirection;

    public final DataAccessor<Integer> ticksUntilBored;

    protected final MovementAnalyzer movementAnalyzer;

    private final HumanWaterMovement waterMovement;

    private final HumanFeatureManager humanFeatureManager;

    public AbstractHuman(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.beardVariant = new DataAccessor<>(this, HumanDataSyncKeys.MARINE_BEARD_VARIANT.get());
        this.eyeColor = new DataAccessor<>(this, HumanDataSyncKeys.MARINE_EYE_COLOR.get());
        this.hairColor = new DataAccessor<>(this, HumanDataSyncKeys.MARINE_HAIR_COLOR.get());
        this.hairVariant = new DataAccessor<>(this, HumanDataSyncKeys.MARINE_HAIR_VARIANT.get());
        this.isMale = new DataAccessor<>(this, HumanDataSyncKeys.MARINE_IS_MALE.get());
        this.skinColor = new DataAccessor<>(this, HumanDataSyncKeys.MARINE_SKIN_COLOR.get());
        this.scrambleDirection = new DataAccessor<>(this, HumanDataSyncKeys.HUMAN_SCRAMBLE_DIRECTION.get());

        this.movementAnalyzer = new MovementAnalyzer(this);
        this.waterMovement = new HumanWaterMovement(this);

        // The ground navigator's tuning, kept from the old navigation manager: humans open doors, and water is cheap
        // to path through (vanilla's default malus of 8 makes a mob route around a pond it could just cross). Floating
        // starts on and HumanWaterMovement toggles it per tick.
        if (getNavigation() instanceof GroundPathNavigation groundPathNavigation) {
            groundPathNavigation.setCanOpenDoors(true);
        }

        // [stated] Oct 23 - "make sure all humans can open doors." The line above only lets the pathfinder plan a
        // route THROUGH a closed door; nothing ever opened one, so every human - marine, colonist, every W-Y unit -
        // walked up to a closed door and stood there. This is vanilla's villager door goal: it opens the door in the
        // path and closes it behind them (true). Added HERE, in the shared constructor, and not in registerGoals, so
        // no human type can lose it by overriding that. Vanilla's goal selector ticks for every mob with AI, whatever
        // else drives it (the marines' GOAP plans move them through this same navigation).
        //
        // Works on any door a hand can open: every avp_human metal door (steel, titanium, ferroaluminum, composite)
        // uses the copper door type, which can be. Iron-type doors that need redstone stay shut, as for villagers.
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));

        setPathfindingMalus(PathType.WATER, DEFAULT_WATER_MALUS);
        getNavigation().setCanFloat(true);
        this.humanFeatureManager = new HumanFeatureManager(this);

        this.ticksUntilBored = new DataAccessor<>(this, HumanDataSyncKeys.MARINE_TICKS_UNTIL_BORED.get());
    }

    public abstract void runAttackAnimations();

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.EMPTY;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return SoundEvents.GENERIC_HURT;
    }

    @Override
    public void tick() {
        super.tick();
        movementAnalyzer.tick();

        if (!level().isClientSide()) {
            ticksUntilBored.set(Math.max(ticksUntilBored.get() - 1, 0));
        }
    }

    /**
     * Water behaviour lives in {@link HumanWaterMovement}; read its class note before touching anything here. The
     * {@code travel} override and the navigation swap that used to sit in this class are gone on purpose - they were a
     * copied Drowned rig that sank every marine and, as a side effect, replaced the marine's own move control every
     * tick.
     * <p>
     * {@code customServerAiStep} is the hook vanilla leaves open inside the (final) {@code serverAiStep}, and it runs
     * BEFORE {@code jumpControl.tick()} there - which is what turns a queued jump into the {@code jumping} flag. A jump
     * queued any later is a tick late.
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        waterMovement.tick();
        // Oct 5 - after the water movement, so a scramble's velocity is the one the travel step uses this tick.
        HumanWallScramble.tick(this);
    }

    /**
     * Server side only. The swimming flag and the pose are synced entity data, so the client must NOT run vanilla's
     * version - for a mob it evaluates to {@code isSprinting() && ...}, which is always false, and would clear the flag
     * locally every tick after the server set it.
     */
    @Override
    public void updateSwimming() {
        if (!level().isClientSide) {
            waterMovement.updateSwimming();
        }
    }

    /**
     * A human is a player in every way that matters, so it gets the player's swimming box: 0.6 wide and tall, eyes at
     * 0.4. That is what lets it swim through a one-block gap and crawl under a low ceiling the way a player can.
     */
    @Override
    protected @NotNull EntityDimensions getDefaultDimensions(@NotNull Pose pose) {
        if (pose == Pose.SWIMMING) {
            return EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F).scale(getAgeScale());
        }

        return super.getDefaultDimensions(pose);
    }

    /**
     * The entity this human mirrors in the water, or null to act on its own. A marine returns its allegiance - the
     * hiring player or its squad leader - and an assigned mission marine can return the mission's player from the same
     * hook when that lands.
     */
    public @Nullable LivingEntity getWaterMovementLeaderOrNull() {
        return null;
    }

    public HumanWaterMovement getWaterMovement() {
        return waterMovement;
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        resetTicksUntilBored();
    }

    public HumanFeatureManager getHumanFeatureManager() {
        return humanFeatureManager;
    }

    public Integer getBeardVariantOrNull() {
        return isMale.get()
            ? beardVariant.get()
            : null;
    }

    public void setBeardVariant(int variantIndex) {
        if (!isMale.get()) {
            return;
        }

        beardVariant.set(variantIndex);
    }

    public int getTicksUntilBored() {
        return ticksUntilBored.get();
    }

    public void resetTicksUntilBored() {
        this.ticksUntilBored.set(getRandom().nextIntBetweenInclusive(MIN_IDLE_TIME_IN_TICKS, MAX_IDLE_TIME_IN_TICKS));
    }

    /** avp_alien's parasites (facehuggers of every strain), by tag id - no compile dependency on avp_alien. */
    private static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> ALIEN_PARASITES =
        net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ENTITY_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("avp_alien", "parasites")
        );

    /**
     * Whether a facehugger is on this human's face.
     * <p>
     * [stated, relayed] Oct 24 - "marines keep shooting in the air even after being successfully facehugged." avp_alien
     * only switches a hugged mob's AI off ten seconds into the hug (when the embryo takes hold), and a marine's own
     * fieldcraft (grenades, flanking) runs outside that AI entirely - so for those first seconds he went on firing
     * blind, wherever his last aim pointed. A man with a facehugger clamped over his face does not shoot; see
     * Marine.tick and MarineTargetScan.
     * </p>
     */
    public static boolean isFacehugged(net.minecraft.world.entity.Entity human) {
        for (var passenger : human.getPassengers()) {
            if (passenger.getType().is(ALIEN_PARASITES)) {
                return true;
            }
        }
        return false;
    }
}
