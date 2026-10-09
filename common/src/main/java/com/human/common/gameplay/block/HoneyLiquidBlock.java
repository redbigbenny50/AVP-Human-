package com.human.common.gameplay.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.NotNull;

/**
 * Liquid honey in the world. [stated] Sep 29: "honey like latex its source block dries out and hardens into a block
 * clearing the flow blocks and its sticky but it only slows you down slow II".
 * <ul>
 * <li><b>Drying</b> - only the SOURCE hardens, into a vanilla honey block, at latex's rate: a 50% chance on each random
 * tick. Nothing clears the flowing honey explicitly - with its source gone it has nothing feeding it and recedes on its
 * own, exactly as flowing latex does.</li>
 * <li><b>Sticky</b> - Slowness II while inside, the same for every mob. Latex is Slowness VI with a milder level for
 * xenomorphs and predators; honey is mild enough that no one needs the exception.</li>
 * <li><b>Falls</b> - halved, handled at the moment of landing by {@code FluidFallCushion}. Deliberately NOT a
 * {@code fallOn} override here: vanilla never calls that on a liquid (see that class).</li>
 * </ul>
 */
public class HoneyLiquidBlock extends LiquidBlock {

    /** Slowness II - effect amplifiers count from zero. */
    public static final int SLOWNESS_AMPLIFIER = 1;

    /** The same rate as latex. */
    public static final float HARDEN_CHANCE_PER_RANDOM_TICK = 0.5F;

    private static final int SLOWNESS_DURATION_TICKS = 20;

    public HoneyLiquidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected boolean isRandomlyTicking(@NotNull BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(
        @NotNull BlockState state,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        // Level 0 is the source; flowing honey never hardens by itself.
        if (state.getValue(LEVEL) != 0 || random.nextFloat() >= HARDEN_CHANCE_PER_RANDOM_TICK) {
            return;
        }

        level.setBlockAndUpdate(pos, Blocks.HONEY_BLOCK.defaultBlockState());
    }

    @Override
    protected void entityInside(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Entity entity
    ) {
        super.entityInside(state, level, pos, entity);

        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }

        living.addEffect(
            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION_TICKS, SLOWNESS_AMPLIFIER, true, false)
        );
    }
}
