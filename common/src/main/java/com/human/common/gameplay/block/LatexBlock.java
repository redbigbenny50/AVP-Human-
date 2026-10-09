package com.human.common.gameplay.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Hardened latex. [stated] Sep 24: "landing on latex blocks is like a cushion and absorbs your fall but little bounce"
 * and "less bouncy than slime".
 * <h2>What it does and how it differs from slime</h2> Vanilla's {@code SlimeBlock} takes NO fall damage and returns
 * your ENTIRE downward speed - a living entity bounces back to the height it fell from, forever. Latex keeps the first
 * half and cuts the second: the fall is fully absorbed, but only {@value #BOUNCE_FACTOR} of the impact comes back, so
 * you get a small hop and settle. A bounce that small dies out on its own instead of needing a crouch to stop it.
 * <p>
 * ⚠ {@code isSuppressingBounce()} (a crouching player) is honoured exactly as slime honours it, so shift-landing gives
 * the plain vanilla landing with fall damage. That is deliberate: it is the established way to stand on a bouncy block
 * without being thrown, and players already know it.
 * </p>
 * Slime also slows anything walking across it. Latex does NOT - it is a cushion, not flypaper. The sticky behaviour he
 * asked for is the WALL hold, which is per-entity state and lives outside this class.
 */
public class LatexBlock extends Block {

    public static final MapCodec<LatexBlock> CODEC = simpleCodec(LatexBlock::new);

    /**
     * How much of the landing speed comes back. Slime returns 1.0 for a living entity and 0.8 for everything else; this
     * is a fraction of that, which is what "less bouncy than slime" and "little bounce" come to in practice.
     */
    public static final double BOUNCE_FACTOR = 0.15;

    public LatexBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull MapCodec<LatexBlock> codec() {
        return CODEC;
    }

    @Override
    public void fallOn(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockPos pos,
        @NotNull Entity entity,
        float fallDistance
    ) {
        if (entity.isSuppressingBounce()) {
            super.fallOn(level, state, pos, entity, fallDistance);
            return;
        }

        // The 0.0F multiplier is what actually cancels the damage; the call still fires the fall event so anything
        // listening for a landing (advancements, mob AI) still sees it.
        entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
    }

    @Override
    public void updateEntityAfterFallOn(@NotNull BlockGetter level, @NotNull Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
            return;
        }

        var movement = entity.getDeltaMovement();

        if (movement.y >= 0.0) {
            return;
        }

        // Non-living things keep slime's slightly deader bounce, scaled the same way.
        var damping = entity instanceof LivingEntity ? BOUNCE_FACTOR : BOUNCE_FACTOR * 0.8;

        entity.setDeltaMovement(movement.x, -movement.y * damping, movement.z);
    }
}
