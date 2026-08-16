package com.human.common.gameplay.entity.living.human.marine;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * A move control that allows strafing at a chosen speed.
 * <p>
 * Vanilla's {@link MoveControl#strafe(float, float)} hardcodes a speed modifier of 0.25, which suits a skeleton
 * circling its target and is far too slow for a marine trying to open up ground against something charging it. This
 * subclass exists purely to let the caller pick that modifier.
 * <p>
 * Strafing is the only movement mode that leaves {@code yRot} alone. MOVE_TO turns the body toward its waypoint every
 * tick and derives movement from the resulting yaw, so a mob using it cannot move in one direction while facing another
 * - which is exactly what a fighting withdrawal is.
 */
public class MarineMoveControl extends MoveControl {

    public MarineMoveControl(Mob mob) {
        super(mob);
    }

    public void strafe(float forward, float strafe, double speedModifier) {
        super.strafe(forward, strafe);

        this.speedModifier = speedModifier;
    }
}
