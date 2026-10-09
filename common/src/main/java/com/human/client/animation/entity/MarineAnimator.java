package com.human.client.animation.entity;

import com.blib.api.client.animation.v1.BLibEntityAnimationUtils;
import com.blib.api.client.animation.v1.animator.AzEntityAnimator;
import com.blib.api.client.animation.v1.track.AzAnimationTrack;
import com.blib.api.client.animation.v1.track.AzAnimationTrackContainer;
import com.blib.api.client.model.v1.AzBone;
import com.human.HumanResources;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.MarineAnimationRefs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class MarineAnimator extends AzEntityAnimator<Marine> {

    private static final String NAME = "marine";

    private static final ResourceLocation ANIMATION = HumanResources.entityAnimationLocation(NAME);

    /**
     * Vanilla's crossbow hold, from {@code AnimationUtils.animateCrossbowHold}, WITH EVERY CONSTANT NEGATED.
     * <p>
     * ⚠⚠ THE SIGNS ARE FLIPPED ON PURPOSE AND THAT IS THE WHOLE FIX. These numbers were lifted from vanilla
     * {@code HumanoidModel}, which drives {@code ModelPart}. Azure bones turn the OPPOSITE WAY on both axes - BLib's
     * own {@code applyHeadRotations} is the proof, since it negates the head's pitch AND its yaw before writing them to
     * a bone. Copying vanilla's constants across without flipping them pointed the arms the wrong way, and for the
     * pitch that meant {@code -PI/2} swung the rifle up over the shoulder and BACKWARDS rather than forwards - a
     * constant error of almost exactly 180 degrees, no matter where the target was or whether the marine was moving.
     * <p>
     * Converting properly: vanilla asks for {@code armV = -PI/2 + headV}; an Azure bone needs {@code armA = -armV}, and
     * {@code headA = -headV} already, so it works out to {@code armA = +PI/2 + headA}. The constant flips, the head
     * term stays added. Same derivation for every offset below.
     */
    private static final float FIRING_ARM_YAW_OFFSET = 0.3F;

    private static final float SUPPORT_ARM_YAW_OFFSET = -0.6F;

    private static final float FIRING_ARM_PITCH_OFFSET = -0.1F;

    private static final float SUPPORT_ARM_PITCH = 1.5F;

    /**
     * Vanilla's crawl stroke, from {@code HumanoidModel.setupAnim}'s {@code swimAmount > 0} block. One stroke is 26
     * limb-swing units: 0-14 the arm sweeps round over the head, 14-22 it pulls down through the water, 22-26 it
     * recovers. The constants are vanilla's own.
     */
    private static final float SWIM_STROKE_LENGTH = 26.0F;

    private static final float SWIM_STROKE_SWEEP_END = 14.0F;

    private static final float SWIM_STROKE_PULL_END = 22.0F;

    private static final float SWIM_ARM_SWEEP = 1.8707964F;

    private static final float SWIM_ARM_PULL_START_LEFT = 5.012389F;

    private static final float SWIM_ARM_PULL_START_RIGHT = 1.2707963F;

    private static final float SWIM_LEG_KICK = 0.3F;

    private static final float SWIM_LEG_KICK_RATE = 0.33333334F;

    /** Vanilla lifts the head 45 degrees while swimming so it looks ahead along the surface rather than at the bed. */
    private static final float SWIM_HEAD_LIFT = Mth.HALF_PI / 2;

    /** Oct 5 - one full hand-over-hand cycle (both arms), in ticks. */
    private static final float CLIMB_CYCLE_TICKS = 20.0F;

    /** Arm reaching up the wall: about 150 degrees forward-and-up. */
    private static final float CLIMB_ARM_REACH = 2.6F;

    /** Arm pulled down to about shoulder level. */
    private static final float CLIMB_ARM_PULL = 1.4F;

    /** Leg push while climbing, radians each way. */
    private static final float CLIMB_LEG_PUSH = 0.35F;

    @Override
    public void registerTracks(AzAnimationTrackContainer<Marine> animationControllerContainer) {
        animationControllerContainer.add(
            AzAnimationTrack.builder(this, MarineAnimationRefs.FULL_BODY_CONTROLLER_NAME)
                .setTransitionLength(5)
                .build()
        );
    }

    @Override
    public @NotNull ResourceLocation getAnimationLocation(Marine animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(Marine animatable, float partialTicks) {
        super.setCustomAnimations(animatable, partialTicks);

        BLibEntityAnimationUtils.applyHeadRotations(animatable, context(), partialTicks, "gHead", 0F);

        var boneCache = this.context().boneCache();
        var head = boneCache.getBakedModel().getBoneOrNull("gHead");
        var leftArm = boneCache.getBakedModel().getBoneOrNull("gLeftArm");
        var rightArm = boneCache.getBakedModel().getBoneOrNull("gRightArm");
        var leftLeg = boneCache.getBakedModel().getBoneOrNull("gLeftLeg");
        var rightLeg = boneCache.getBakedModel().getBoneOrNull("gRightLeg");

        // ⚠⚠ Oct 5 - ARMS STUCK IN THE AIR. Every branch below writes an arm's X, but only some write Y and Z: the aim
        // pose sets Y, the swim stroke sets Y and Z (Z = PI is an arm straight overhead), the walk swing touches
        // neither.
        // A procedural rotation is not undone by the animation, so when the marine stopped aiming or left the water the
        // last Y/Z stayed - the field reports of a marine "with its arms raised" after climbing out of a pool, and one
        // "aiming in the water with his arms stuck". Each frame starts both arms from their bind pose instead.
        resetArmTwist(leftArm);
        resetArmTwist(rightArm);

        // The weapon hangs off rightHand_Item, which is a child of gRightArm, so the gun points wherever that bone
        // points and nowhere else. Nothing was aiming it: the walk swing below is skipped while aggressive, and the
        // only animation that touches the arm is a fixed -85 degree pose barely one tick long, so the arm sat in its
        // bind pose -- straight down -- for the whole engagement.
        //
        // These marines are posed the way a vanilla player is, so the aim pose is vanilla's two-handed ranged hold
        // rather than anything bespoke. It is driven off the head bone instead of the entity's own pitch and yaw,
        // because applyHeadRotations has already resolved the aim above; reading it back keeps the muzzle exactly on
        // the line the marine is looking along, elevation included, and inherits its sign conventions for free.
        if (head != null && animatable.isAggressive()) {
            if (rightArm != null) {
                rightArm.setRotY(FIRING_ARM_YAW_OFFSET + head.getRotY());
                rightArm.setRotX(Mth.HALF_PI + head.getRotX() + FIRING_ARM_PITCH_OFFSET);
            }

            if (leftArm != null) {
                leftArm.setRotY(SUPPORT_ARM_YAW_OFFSET + head.getRotY());
                leftArm.setRotX(SUPPORT_ARM_PITCH + head.getRotX());
            }
        }

        if (leftArm != null && !animatable.isAggressive()) {
            leftArm.setRotX(
                Mth.cos(
                    animatable.walkAnimation.position(
                        partialTicks
                    ) * 0.6662F
                ) * 1.0F * animatable.walkAnimation.speed() * 0.9F
            );
        }

        if (rightArm != null && !animatable.isAggressive()) {
            rightArm.setRotX(
                Mth.cos(
                    animatable.walkAnimation.position(
                        partialTicks
                    ) * 0.6662F + ((float) Math.PI)
                ) * 1.0F * animatable.walkAnimation.speed() * 0.9F
            );
        }

        if (leftLeg != null) {
            leftLeg.setRotX(
                Mth.cos(
                    animatable.walkAnimation.position(
                        partialTicks
                    ) * 0.6662F + ((float) Math.PI)
                ) * 1.4F * animatable.walkAnimation.speed() * 0.9F
            );
        }

        if (rightLeg != null) {
            rightLeg.setRotX(
                Mth.cos(
                    animatable.walkAnimation.position(
                        partialTicks
                    ) * 0.6662F
                ) * 1.4F * animatable.walkAnimation.speed() * 0.9F
            );
        }

        applySwimStroke(animatable, partialTicks, head, leftArm, rightArm, leftLeg, rightLeg);
    }

    /**
     * The player's swimming animation, ported from {@code HumanoidModel.setupAnim}. Blended in by {@code swimAmount}
     * exactly as vanilla does, so it fades in and out over a few ticks instead of snapping.
     * <p>
     * ⚠⚠ EVERY X AND Y ANGLE IS NEGATED AND EVERY Z ANGLE IS NOT. Same conversion as the aim pose above: an Azure bone
     * turns the opposite way to a {@code ModelPart} on X and Y. That is the {@code scale(-1, -1, 1)} vanilla applies
     * before drawing a model - a half-turn about Z - so conjugating a rotation through it flips X and Y and leaves Z
     * alone. Both systems compose Z, then Y, then X ({@code ModelPart.translateAndRotate} uses {@code rotationZYX};
     * BLib's {@code rotateMatrixAroundBone} multiplies Z, Y, X in that order), so the per-axis flip is the whole
     * conversion.
     * <p>
     * The arm stroke yields to the aim pose (an aggressive marine keeps its gun up, as a player using an item does in
     * vanilla) and to a held-item use for the same reason. The legs and head always take the swim.
     */
    /**
     * Hand over hand up a bank: each arm reaches up the wall and pulls down while the other reaches, the legs push in
     * turn. Timed off {@code tickCount} so it animates with no sideways movement. Azure X is negated from vanilla (see
     * {@link #applySwimStroke}): positive X raises an arm forward, so {@link #CLIMB_ARM_REACH} is up the wall and
     * {@link #CLIMB_ARM_PULL} is about level with the shoulder.
     */
    private void applyClimb(
        Marine animatable,
        float partialTicks,
        AzBone leftArm,
        AzBone rightArm,
        AzBone leftLeg,
        AzBone rightLeg
    ) {
        var time = (animatable.tickCount + partialTicks) * Mth.TWO_PI / CLIMB_CYCLE_TICKS;
        // 0..1 and back, the two arms half a cycle apart.
        var leftReach = 0.5F + 0.5F * Mth.sin(time);
        var rightReach = 0.5F - 0.5F * Mth.sin(time);

        if (leftArm != null) {
            leftArm.setRotX(Mth.lerp(leftReach, CLIMB_ARM_PULL, CLIMB_ARM_REACH));
            leftArm.setRotY(0.0F);
            leftArm.setRotZ(0.0F);
        }

        if (rightArm != null) {
            rightArm.setRotX(Mth.lerp(rightReach, CLIMB_ARM_PULL, CLIMB_ARM_REACH));
            rightArm.setRotY(0.0F);
            rightArm.setRotZ(0.0F);
        }

        if (leftLeg != null) {
            leftLeg.setRotX(CLIMB_LEG_PUSH * Mth.sin(time + Mth.PI));
        }

        if (rightLeg != null) {
            rightLeg.setRotX(CLIMB_LEG_PUSH * Mth.sin(time));
        }
    }

    private void applySwimStroke(
        Marine animatable,
        float partialTicks,
        AzBone head,
        AzBone leftArm,
        AzBone rightArm,
        AzBone leftLeg,
        AzBone rightLeg
    ) {
        // ⚠⚠ Oct 5 - A PIT CLIMB IS NOT A SWIM STROKE. The stroke is timed off walkAnimation, which only advances with
        // SIDEWAYS movement; a climbing marine moves straight up, so the stroke froze mid-sweep - and on a body
        // standing
        // upright against the wall that frozen frame was one arm stuck straight in the air. While the synced climb
        // direction is set, a hand-over-hand climb timed off the clock runs instead.
        if (animatable.scrambleDirection.get() >= 0) {
            applyClimb(animatable, partialTicks, leftArm, rightArm, leftLeg, rightLeg);
            return;
        }

        var swimAmount = animatable.getSwimAmount(partialTicks);

        if (swimAmount <= 0.0F) {
            return;
        }

        var limbSwing = animatable.walkAnimation.position(partialTicks);

        if (head != null && animatable.isVisuallySwimming()) {
            head.setRotX(rotlerpRad(swimAmount, head.getRotX(), SWIM_HEAD_LIFT));
        }

        if (leftArm != null && rightArm != null && !animatable.isAggressive() && !animatable.isUsingItem()) {
            var phase = limbSwing % SWIM_STROKE_LENGTH;

            if (phase < SWIM_STROKE_SWEEP_END) {
                var sweep = SWIM_ARM_SWEEP * quadraticArmUpdate(phase) / quadraticArmUpdate(SWIM_STROKE_SWEEP_END);

                leftArm.setRotX(rotlerpRad(swimAmount, leftArm.getRotX(), 0.0F));
                rightArm.setRotX(rotlerpRad(swimAmount, rightArm.getRotX(), 0.0F));
                leftArm.setRotY(rotlerpRad(swimAmount, leftArm.getRotY(), -Mth.PI));
                rightArm.setRotY(rotlerpRad(swimAmount, rightArm.getRotY(), -Mth.PI));
                leftArm.setRotZ(rotlerpRad(swimAmount, leftArm.getRotZ(), Mth.PI + sweep));
                rightArm.setRotZ(rotlerpRad(swimAmount, rightArm.getRotZ(), Mth.PI - sweep));
            } else if (phase < SWIM_STROKE_PULL_END) {
                var pull = (phase - SWIM_STROKE_SWEEP_END) / (SWIM_STROKE_PULL_END - SWIM_STROKE_SWEEP_END);

                leftArm.setRotX(rotlerpRad(swimAmount, leftArm.getRotX(), -Mth.HALF_PI * pull));
                rightArm.setRotX(rotlerpRad(swimAmount, rightArm.getRotX(), -Mth.HALF_PI * pull));
                leftArm.setRotY(rotlerpRad(swimAmount, leftArm.getRotY(), -Mth.PI));
                rightArm.setRotY(rotlerpRad(swimAmount, rightArm.getRotY(), -Mth.PI));
                leftArm.setRotZ(rotlerpRad(swimAmount, leftArm.getRotZ(), SWIM_ARM_PULL_START_LEFT - SWIM_ARM_SWEEP * pull));
                rightArm.setRotZ(rotlerpRad(swimAmount, rightArm.getRotZ(), SWIM_ARM_PULL_START_RIGHT + SWIM_ARM_SWEEP * pull));
            } else {
                var recover = (phase - SWIM_STROKE_PULL_END) / (SWIM_STROKE_LENGTH - SWIM_STROKE_PULL_END);

                leftArm.setRotX(rotlerpRad(swimAmount, leftArm.getRotX(), -(Mth.HALF_PI - Mth.HALF_PI * recover)));
                rightArm.setRotX(rotlerpRad(swimAmount, rightArm.getRotX(), -(Mth.HALF_PI - Mth.HALF_PI * recover)));
                leftArm.setRotY(rotlerpRad(swimAmount, leftArm.getRotY(), -Mth.PI));
                rightArm.setRotY(rotlerpRad(swimAmount, rightArm.getRotY(), -Mth.PI));
                leftArm.setRotZ(rotlerpRad(swimAmount, leftArm.getRotZ(), Mth.PI));
                rightArm.setRotZ(rotlerpRad(swimAmount, rightArm.getRotZ(), Mth.PI));
            }
        }

        if (leftLeg != null) {
            leftLeg.setRotX(
                Mth.lerp(swimAmount, leftLeg.getRotX(), -SWIM_LEG_KICK * Mth.cos(limbSwing * SWIM_LEG_KICK_RATE + Mth.PI))
            );
        }

        if (rightLeg != null) {
            rightLeg.setRotX(Mth.lerp(swimAmount, rightLeg.getRotX(), -SWIM_LEG_KICK * Mth.cos(limbSwing * SWIM_LEG_KICK_RATE)));
        }
    }

    private static void resetArmTwist(AzBone arm) {
        if (arm == null) {
            return;
        }

        var initial = arm.getInitialAzSnapshot();

        arm.setRotY(initial == null ? 0.0F : initial.getRotY());
        arm.setRotZ(initial == null ? 0.0F : initial.getRotZ());
    }

    /** Vanilla {@code HumanoidModel.rotlerpRad}: lerp an angle the short way round. */
    private static float rotlerpRad(float amount, float from, float to) {
        var delta = (to - from) % Mth.TWO_PI;

        if (delta < -Mth.PI) {
            delta += Mth.TWO_PI;
        }

        if (delta >= Mth.PI) {
            delta -= Mth.TWO_PI;
        }

        return from + amount * delta;
    }

    /** Vanilla {@code HumanoidModel.quadraticArmUpdate}. */
    private static float quadraticArmUpdate(float limbSwing) {
        return -65.0F * limbSwing + limbSwing * limbSwing;
    }
}
