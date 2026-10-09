package com.human.common.gameplay.machine;

/**
 * A working-front animation played on the MACHINE'S OWN CLOCK.
 * <h2>⚠⚠ WHY THIS IS NOT AN ANIMATED BLOCK TEXTURE</h2> [stated] Sep 29: the machines must not all animate in lockstep.
 * A texture in {@code textures/block} with an {@code .mcmeta} is animated by the block atlas, which keeps ONE frame
 * counter per sprite for the whole client - every copy of that block shows the same frame at the same moment, whenever
 * it started. That is fine for lava; it is wrong for a press that has a stroke, a working front with a progress bar
 * drawn into it, and a sound timed to the picture.
 * <p>
 * So the working strips live in {@code textures/machine/} - OUTSIDE the atlas, where no mcmeta is read - and the
 * machine renderer picks the frame from the tick that machine started working. These frame timings are what the mcmeta
 * would have said.
 * </p>
 *
 * @param frameTicks  how long each frame shows, in ticks, top of the strip first
 * @param interpolate blend each frame into the next, as vanilla's {@code "interpolate": true} does
 */
public record MachineAnimation(
    int[] frameTicks,
    boolean interpolate
) {

    public MachineAnimation {
        if (frameTicks.length == 0) {
            throw new IllegalArgumentException("A machine animation needs at least one frame");
        }

        for (var ticks : frameTicks) {
            if (ticks <= 0) {
                throw new IllegalArgumentException("Frame times must be positive");
            }
        }
    }

    public int frameCount() {
        return frameTicks.length;
    }

    public int cycleTicks() {
        var total = 0;

        for (var ticks : frameTicks) {
            total += ticks;
        }

        return total;
    }
}
