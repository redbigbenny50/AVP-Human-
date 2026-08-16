package com.human.common.gameplay.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Fallout ash, layered like snow.
 * <p>
 * The only behavioural difference from {@link SnowLayerBlock} is what happens when the ground under a layer goes away:
 * ash CRUMBLES AWAY SILENTLY instead of popping into an item.
 * </p>
 * <p>
 * WHY THIS OVERRIDE EXISTS. Vanilla's {@code updateShape} answers "I cannot survive here" by returning air, and
 * {@code Block.updateOrDestroy} turns an air result into {@code destroyBlock(pos, dropBlock)} - which drops the block
 * unless the update that triggered it happened to carry {@code UPDATE_SUPPRESS_DROPS}. That is fine for snow, which is
 * rarely undermined en masse. It is badly wrong for ash, because undermining everything is precisely what a nuke does:
 * ash lands on the crater rim and on treetops, then fire eats the supports and the carve removes the ground, and every
 * orphaned layer becomes a loose item. The result was drifts of pick-up-able ash across the fallout zone, plus the
 * server cost of the item entities that come with them.
 * </p>
 * <p>
 * The fix defers the decision rather than suppressing it. An unsupported layer keeps its state for one tick and
 * schedules itself; the scheduled tick re-checks and calls {@code removeBlock}, which clears the block WITHOUT the drop
 * that {@code destroyBlock} would have produced. Ash still cannot float - it goes on the very next tick - it just stops
 * littering.
 * </p>
 * <p>
 * Mining ash by hand is untouched and still yields the block: that path runs through the loot table, not through here.
 * </p>
 */
public class AshBlock extends SnowLayerBlock {

    private static final int CRUMBLE_DELAY_IN_TICKS = 1;

    public AshBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull BlockState updateShape(
        @NotNull BlockState state,
        @NotNull Direction direction,
        @NotNull BlockState neighborState,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockPos neighborPos
    ) {
        if (!state.canSurvive(level, pos)) {
            // Deliberately NOT returning air here: that is the branch that drops the item. Hold the state and let the
            // scheduled tick below clear it quietly instead.
            level.scheduleTick(pos, this, CRUMBLE_DELAY_IN_TICKS);
            return state;
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void tick(
        @NotNull BlockState state,
        @NotNull ServerLevel level,
        @NotNull BlockPos pos,
        @NotNull RandomSource random
    ) {
        // Re-checked rather than assumed: support may have been restored in the tick since this was scheduled.
        if (!state.canSurvive(level, pos)) {
            level.removeBlock(pos, false);
            return;
        }

        super.tick(state, level, pos, random);
    }
}
