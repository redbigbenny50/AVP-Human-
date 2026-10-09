package com.human.common.gameplay.entity.living.human;

import com.blib.api.common.entity.v1.BLibEntityPredicates;
import com.human.common.registry.tag.HumanItemTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

/**
 * How a human moves in water: float and swim on the surface like a player, or walk the bottom like a diver.
 * <p>
 * ⚠⚠ THIS REPLACES A DROWNED RIG THAT SANK EVERY MARINE. {@code AbstractHuman} used to carry a copy of vanilla
 * {@code Drowned}'s water code: a {@code WaterBoundPathNavigation} + BLib {@code WaterMoveControl} pair swapped in
 * whenever the eyes went under, and a {@code travel} override that applied 0.8 friction with no buoyancy. Nothing ever
 * set {@code jumping} in water - vanilla mobs only float because {@code FloatGoal} does that for them, and a marine's
 * goal selector is empty - so a marine walked in, sank until submerged, hung there with no path target, and drowned.
 * <p>
 * ⚠⚠ THE SAME RIG ALSO SILENTLY BROKE THE FIGHTING WITHDRAWAL. {@code updateSwimming} ran every tick and called
 * {@code switchToGround}, which put back the vanilla {@code MoveControl} captured in the constructor BEFORE
 * {@code Marine} installed its {@code MarineMoveControl}. Both fall-back actions check {@code instanceof
 * MarineMoveControl} and quietly did nothing. With the rig gone the marine's own control is never replaced.
 * <p>
 * THE RULES, as he set them Sep 21:
 * <ul>
 * <li>"Suited" means a full MK50 or a full pressure set - the two sets {@code Human} already refills air for.</li>
 * <li>Unsuited and alone: float, swim across the surface with the player's swim animation.</li>
 * <li>Suited and alone: walk the bottom. Where the floor drops away (a trench, an underwater cliff) kick up and swim
 * across, then settle again once there is bottom under it.</li>
 * <li>A leader in the water is mirrored: leader on the surface, marine on the surface; leader under the water, marine
 * goes down after them.</li>
 * <li>An unsuited marine following a leader down surfaces on its own when its air runs low, breathes, and comes back
 * down. The suit-equip side of that ("put on the pressure suit or MK50 if they have them") is a GOAP decision in
 * {@code BestArmorSetTargetSensor}, fed by {@link #isLeaderStayingUnderwater()}.</li>
 * </ul>
 * ⭐ THE LEADER IS WHOEVER {@code AbstractHuman.getWaterMovementLeaderOrNull} SAYS IT IS. For a marine that is its
 * allegiance - the hiring player or the squad leader - so a squad mirrors its leader and a future "assigned" mission
 * marine gets the behaviour for free by returning the mission's player from the same hook.
 * <p>
 * ⚠ RUN ORDER MATTERS. {@link #tick()} must run BEFORE {@code jumpControl.tick()} at the end of the (final)
 * {@code Mob.serverAiStep}, because that is what turns a queued jump into the {@code jumping} flag that
 * {@code LivingEntity.aiStep} then reads to call {@code jumpInLiquid}. {@code AbstractHuman.customServerAiStep} is the
 * open hook inside that method and sits before the controls, so it calls this from there. Vanilla's own
 * {@code FloatGoal} runs from the goal selector in the same method, a few lines earlier - same window.
 */
public class HumanWaterMovement {

    public enum Mode {
        /** Not in water, or not deep enough to matter. */
        NONE,
        /** Float at the surface; swim across it when moving. */
        SURFACE,
        /** Sink and walk the bottom. */
        BOTTOM
    }

    /**
     * How far below its feet a bottom-walker looks for a floor before deciding it is over a drop and swimming instead.
     * Twelve blocks is deeper than any lake bed it should be walking and shallower than a trench it should be crossing.
     */
    public static final int FLOOR_SEARCH_DEPTH = 12;

    /** Below this fraction of max air an unsuited marine gives up the bottom and surfaces to breathe. */
    public static final float SURFACE_FOR_AIR_RATIO = 1.0F / 3.0F;

    /** ...and only goes back down once it has refilled to this fraction, so it does not bob at the threshold. */
    public static final float RESUME_DIVE_AIR_RATIO = 0.9F;

    /**
     * A leader that has been under the water this long is treated as staying down, which is what tells the marines to
     * put a suit on rather than keep surfacing for air. Five seconds is past a casual dip and well short of a player's
     * own drowning point.
     */
    public static final int LEADER_UNDERWATER_STAY_TICKS = 5 * 20;

    /**
     * Extra downward pull while diving for the bottom. Vanilla water gravity alone is gravity/16 per tick against 0.8
     * friction, which settles at about half a block a second - a marine following a player down would arrive long after
     * them. This roughly triples it without turning the descent into a plummet.
     */
    public static final double SINK_ASSIST_PER_TICK = -0.02;

    /** Same 80% duty cycle as vanilla {@code FloatGoal}; a constant jump input over-lifts the mob out of the water. */
    public static final float FLOAT_JUMP_CHANCE = 0.8F;

    /** Horizontal speed at which a floating marine is swimming rather than treading water. */
    private static final double SWIMMING_SPEED_SQR = 0.02 * 0.02;

    /**
     * Oct 5 - [stated] "they swim too slow. the slowness was only for when climbing but in water they should swim
     * faster". Vanilla's water travel gives a mob a fiftieth of its land speed; this is the 1.21 water movement
     * efficiency attribute (what Depth Strider sets), applied to marines while they are in water and not climbing a
     * bank. 1.0 = their land speed on the bottom, half that while swimming (vanilla halves it off the ground).
     */
    public static final double MARINE_WATER_MOVEMENT_EFFICIENCY = 1.0;

    private static final net.minecraft.resources.ResourceLocation SWIM_SPEED_ID = net.minecraft.resources.ResourceLocation
        .fromNamespaceAndPath("avp_human", "marine_swim_speed");

    private final AbstractHuman human;

    private Mode mode = Mode.NONE;

    private boolean surfacingForAir;

    private int leaderUnderwaterTicks;

    public HumanWaterMovement(AbstractHuman human) {
        this.human = human;
    }

    public Mode getMode() {
        return mode;
    }

    /**
     * Whether the leader this human mirrors has committed to being under the water, either by having water breathing or
     * by simply staying down. This is the trigger for a marine to dress for a dive.
     */
    public boolean isLeaderStayingUnderwater() {
        var leader = human.getWaterMovementLeaderOrNull();

        if (leader == null || !leader.isInWater()) {
            return false;
        }

        return leader.hasEffect(MobEffects.WATER_BREATHING) || leaderUnderwaterTicks >= LEADER_UNDERWATER_STAY_TICKS;
    }

    /**
     * True when this human can stay under the water indefinitely: water breathing, or a full MK50 or pressure set. The
     * two sets are the ones {@code Human} refills air for, so this and that are the same rule.
     */
    public boolean canBreatheIndefinitely() {
        if (human.hasEffect(MobEffects.WATER_BREATHING) || human.canBreatheUnderwater()) {
            return true;
        }

        return BLibEntityPredicates.hasFullArmorSetMatching(human, itemStack -> itemStack.is(HumanItemTags.MK50_ARMOR))
            || BLibEntityPredicates.hasFullArmorSetMatching(human, itemStack -> itemStack.is(HumanItemTags.PRESSURE_ARMOR));
    }

    /** Server only. Decides the mode for this tick and applies it; see the class note on when this must run. */
    public void tick() {
        var leader = human.getWaterMovementLeaderOrNull();

        trackLeader(leader);

        this.mode = decide(leader);

        apply();
    }

    /**
     * Server only, from {@code Entity.baseTick} via {@code updateSwimming}. Keeps the synced swimming flag and pose in
     * step with the mode. Separated from {@link #tick()} because {@code updateSwimming} is the one vanilla hook that
     * runs for the mob every tick regardless of AI, so a marine with its brain switched off (GUI open, {@code NoAI})
     * still surfaces its pose correctly.
     */
    public void updateSwimming() {
        var swimming = mode == Mode.SURFACE
            && !human.onGround()
            && human.getDeltaMovement().horizontalDistanceSqr() > SWIMMING_SPEED_SQR;

        human.setSwimming(swimming);

        // A player is only ever in the swimming pose while actually swimming, and stays in it out of the water only
        // while there is no headroom to stand - the crawl. Same here: prefer standing, fall back to the low pose when
        // the standing box does not fit, and never pick a pose the human cannot occupy.
        // Oct 5 - a scramble up a pit bank holds the swimming pose, so it plays the swim stroke and keeps the low box
        // until it is over the top (see HumanWallScramble).
        if (swimming || HumanWallScramble.isScrambling(human)) {
            setPoseIfFits(Pose.SWIMMING);
        } else if (!setPoseIfFits(Pose.STANDING)) {
            setPoseIfFits(Pose.SWIMMING);
        }
    }

    private void trackLeader(@Nullable LivingEntity leader) {
        if (leader != null && leader.isUnderWater()) {
            leaderUnderwaterTicks = Math.min(leaderUnderwaterTicks + 1, Integer.MAX_VALUE - 1);
        } else {
            leaderUnderwaterTicks = 0;
        }
    }

    private Mode decide(@Nullable LivingEntity leader) {
        if (!human.isInWater()) {
            surfacingForAir = false;
            return Mode.NONE;
        }

        var canBreathe = canBreatheIndefinitely();
        boolean wantsBottom;

        if (leader != null && leader.isInWater()) {
            // Mirror the leader: eyes under means they are diving or on the bottom, either way we go down after them.
            wantsBottom = leader.isUnderWater();
        } else {
            wantsBottom = canBreathe;
        }

        if (!wantsBottom) {
            surfacingForAir = false;
            return Mode.SURFACE;
        }

        if (!canBreathe && updateSurfacingForAir()) {
            return Mode.SURFACE;
        }

        if (!hasFloorBelow(leader)) {
            // Over a drop: kick up and swim across. The trench is crossed on the surface and the marine settles again
            // on the far side the moment a floor is back under it.
            return Mode.SURFACE;
        }

        return Mode.BOTTOM;
    }

    /** Hysteresis on the air supply so an unsuited diver breathes properly instead of bobbing at one third. */
    private boolean updateSurfacingForAir() {
        var maxAir = Math.max(1, human.getMaxAirSupply());
        var ratio = (float) human.getAirSupply() / maxAir;

        if (surfacingForAir) {
            if (ratio >= RESUME_DIVE_AIR_RATIO) {
                surfacingForAir = false;
            }
        } else if (ratio <= SURFACE_FOR_AIR_RATIO) {
            surfacingForAir = true;
        }

        return surfacingForAir;
    }

    /**
     * Whether there is something to stand on within {@link #FLOOR_SEARCH_DEPTH} blocks below. A leader that is below us
     * counts as a floor - we are heading down to them, and whatever they are standing on will do.
     */
    private boolean hasFloorBelow(@Nullable LivingEntity leader) {
        if (human.onGround()) {
            return true;
        }

        if (leader != null && leader.getY() < human.getY()) {
            return true;
        }

        var level = human.level();
        var pos = human.blockPosition().mutable();

        for (var i = 0; i < FLOOR_SEARCH_DEPTH; i++) {
            pos.move(0, -1, 0);

            if (pos.getY() < level.getMinBuildHeight()) {
                return false;
            }

            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private void apply() {
        applySwimSpeed();

        // canFloat is what makes the ground pathfinder plan ACROSS water (the start node is lifted to the surface and
        // water nodes are walkable). Off, it plans along the bottom with the WATER malus, which is exactly a diver.
        human.getNavigation().setCanFloat(mode != Mode.BOTTOM);

        switch (mode) {
            case SURFACE -> {
                var deepEnough = human.getFluidHeight(FluidTags.WATER) > human.getFluidJumpThreshold();

                if (deepEnough && human.getRandom().nextFloat() < FLOAT_JUMP_CHANCE) {
                    human.getJumpControl().jump();
                }
            }
            case BOTTOM -> {
                if (!human.onGround() && human.getFluidHeight(FluidTags.WATER) > 0) {
                    human.setDeltaMovement(human.getDeltaMovement().add(0, SINK_ASSIST_PER_TICK, 0));
                }
            }
            case NONE -> {}
        }
    }

    /** Marines only: the water speed modifier is on while in water and not scrambling up a bank, off otherwise. */
    private void applySwimSpeed() {
        if (!(human instanceof com.human.common.gameplay.entity.living.human.marine.Marine)) {
            return;
        }

        var attribute = human.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.WATER_MOVEMENT_EFFICIENCY);

        if (attribute == null) {
            return;
        }

        var wanted = mode != Mode.NONE && !HumanWallScramble.isScrambling(human);

        if (wanted) {
            if (!attribute.hasModifier(SWIM_SPEED_ID)) {
                attribute.addOrUpdateTransientModifier(
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        SWIM_SPEED_ID,
                        MARINE_WATER_MOVEMENT_EFFICIENCY,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    )
                );
            }
        } else if (attribute.hasModifier(SWIM_SPEED_ID)) {
            attribute.removeModifier(SWIM_SPEED_ID);
        }
    }

    private boolean setPoseIfFits(Pose pose) {
        if (human.getPose() == pose) {
            return true;
        }

        var box = human.getDimensions(pose).makeBoundingBox(human.position()).deflate(1.0E-7);

        if (!human.level().noCollision(human, box)) {
            return false;
        }

        human.setPose(pose);

        return true;
    }
}
