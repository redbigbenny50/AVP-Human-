package com.human.common.gameplay.block.metal;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * [stated] Oct 21 - a metal ladder (steel, titanium, ferroaluminum, aluminum, composite): "like a wooden ladder but
 * they dont need a block behind them to continue down. think of them like vines".
 * <ul>
 * <li><b>Hangs like a vine.</b> The top ladder of a run needs a solid face behind it, as any ladder does. Every ladder
 * under it needs only the ladder directly above (a metal ladder facing the same way), so a run can hang off a ledge or
 * down an open shaft.</li>
 * <li><b>Breaks like a vine.</b> Take a ladder out and everything hanging from it below comes down too, each dropping
 * itself. The ones above stay.</li>
 * <li><b>Builds down like scaffolding in reverse.</b> [stated] "once you place the ladder at the top if you right click
 * with a similar ladder it then adds another ladder to the bottom until it cant go down further." Right-clicking any
 * ladder in a run with the SAME ladder adds one at the bottom of the run. Sneak to place normally instead.</li>
 * <li><b>Climbable</b> from the {@code minecraft:climbable} tag, like every ladder.</li>
 * </ul>
 * Never grows. ⚡ The walk to the bottom of a run is bounded by the world's height and only runs on a right-click.
 */
public class MetalLadderBlock extends LadderBlock {

    public static final MapCodec<MetalLadderBlock> CODEC = simpleCodec(MetalLadderBlock::new);

    /**
     * How long after a ladder above it goes before this one falls - one tick, so a whole run drops in a quick cascade.
     */
    private static final int FALL_DELAY_TICKS = 1;

    public MetalLadderBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public @NotNull MapCodec<LadderBlock> codec() {
        return (MapCodec) CODEC;
    }

    /** Held up by a solid face behind it, OR by the same-facing metal ladder directly above. */
    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        if (super.canSurvive(state, level, pos)) {
            return true;
        }

        var above = level.getBlockState(pos.above());

        return above.getBlock() instanceof MetalLadderBlock && above.getValue(FACING) == state.getValue(FACING);
    }

    /**
     * The wall behind it or the ladder above it changed: if it can no longer hang, it falls on the next tick - and
     * dropping, so a broken run gives its ladders back (vanilla's own ladder just vanishes into air here).
     */
    @Override
    protected @NotNull BlockState updateShape(
        @NotNull BlockState state,
        @NotNull Direction direction,
        @NotNull BlockState neighbourState,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockPos neighbourPos
    ) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        if ((direction == Direction.UP || direction == state.getValue(FACING).getOpposite()) && !state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, FALL_DELAY_TICKS);
            return state;
        }

        return state;
    }

    @Override
    protected void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    /** Right-click a ladder of the run with the same ladder: one more at the bottom. */
    @Override
    protected @NotNull ItemInteractionResult useItemOn(
        @NotNull ItemStack stack,
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hitResult
    ) {
        if (!stack.is(asItem())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        var facing = state.getValue(FACING);
        var below = pos.below();

        // Down to the first spot that is not this ladder, facing the same way.
        while (below.getY() >= level.getMinBuildHeight()) {
            var at = level.getBlockState(below);

            if (!at.is(this) || at.getValue(FACING) != facing) {
                break;
            }

            below = below.below();
        }

        if (below.getY() < level.getMinBuildHeight()) {
            return ItemInteractionResult.FAIL;
        }

        var target = level.getBlockState(below);
        var fluid = level.getFluidState(below);

        // Air, water, grass and the like - anything a block can be placed into.
        if (!target.canBeReplaced()) {
            return ItemInteractionResult.FAIL;
        }

        var placed = defaultBlockState().setValue(FACING, facing).setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);

        if (!placed.canSurvive(level, below)) {
            return ItemInteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            level.setBlock(below, placed, 3);
            level.gameEvent(player, GameEvent.BLOCK_PLACE, below);

            var sound = placed.getSoundType();
            level.playSound(
                null,
                below,
                sound.getPlaceSound(),
                SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F
            );

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
