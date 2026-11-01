package com.human.common.gameplay.cosmetic;

/**
 * One moving part of a cosmetic design, from the generated {@link CosmeticModelTables#MOTIONS}.
 * <ul>
 * <li>A DOOR or LID ({@code spin == false}): turns {@code amount} degrees about {@code axis} at its hinge while the
 * block is OPEN, easing open and shut.</li>
 * <li>A SPINNER ({@code spin == true}): turns {@code amount} degrees per tick about {@code axis} while the block is
 * SPINNING.</li>
 * </ul>
 * The hinge or axis point is the bone's pivot in the model; the sign of {@code amount} is already the right way out
 * (doors swing toward the front, lids lift).
 *
 * @param bone   the model group that moves
 * @param axis   'x', 'y' or 'z'
 * @param amount degrees open (doors, lids) or degrees per tick (spinners)
 * @param spin   true for a spinner
 */
public record CosmeticMotion(String bone, char axis, float amount, boolean spin) {}
