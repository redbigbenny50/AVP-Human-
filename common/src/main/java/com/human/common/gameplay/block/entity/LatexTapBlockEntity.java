package com.human.common.gameplay.block.entity;

import com.human.common.gameplay.block.LatexCauldronBlock;
import com.human.common.registry.init.HumanBlockEntityTypes;
import com.human.common.registry.init.block.CoreBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * The drip. [stated] Sep 24: "lets make it a solid 120 reason is there will be a machine that extracts things faster
 * later" - {@value #TICKS_PER_DRIP} ticks per cauldron level, so a cauldron fills in 6 minutes.
 * <h2>Why a flat counter and not a random tick</h2> ⭐ DELIBERATELY UNLIKE the liquid block and the cauldron cure, which
 * are random-ticked. Vanilla dripstone is random too, and that makes two identical farms finish minutes apart. A tap is
 * infrastructure a player builds and plans throughput around, so it is a counter: exact, predictable, and a fair
 * baseline for the extraction machine to beat.
 * <h2>Where the latex goes</h2> [stated] "you need to place a cauldron under the latex". The tap looks straight DOWN
 * for a cauldron, up to {@value #MAX_DROP} blocks, and stops at the first block that is not air - so a cauldron can
 * hang below with a gap, as with dripstone, but you cannot run latex through a wall. Without a cauldron the timer
 * simply holds: nothing is produced and nothing is wasted, so a tap waiting for its bucket is not quietly dripping onto
 * the floor.
 */
public class LatexTapBlockEntity extends BlockEntity {

    /** [stated] Sep 24: a solid 120 seconds per level. */
    public static final int TICKS_PER_DRIP = 2400;

    public static final int MAX_DROP = 8;

    private int dripTimer;

    /**
     * ⚠ Oct 23 - the cauldron below is looked for once a second, not every tick. A tester's /blib perf report put one
     * latex tap at 221 us a tick, the heaviest avp block entity in the world, for a job that changes only when a player
     * places or breaks a cauldron. The drip timer still counts every tick while a cauldron was there at the last look.
     */
    private static final int CAULDRON_CHECK_INTERVAL_TICKS = 20;

    private int cauldronCheckCooldown;

    private boolean hasCauldron;

    public LatexTapBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.LATEX_TAP.get(), pos, state);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (--cauldronCheckCooldown <= 0) {
            cauldronCheckCooldown = CAULDRON_CHECK_INTERVAL_TICKS;
            hasCauldron = findCauldron(level, pos) != null;
        }

        // No cauldron: hold the timer rather than spend it. The player loses nothing by fitting the pot later.
        if (!hasCauldron) {
            return;
        }

        if (++dripTimer < TICKS_PER_DRIP) {
            return;
        }

        // Looked up again at the moment of filling, never trusted from the last check: in the second since, the
        // cauldron may have been broken or swapped, and fill() must only ever touch a cauldron.
        var cauldron = findCauldron(level, pos);
        if (cauldron == null) {
            hasCauldron = false;
            dripTimer = TICKS_PER_DRIP - 1; // fills the moment a cauldron is back, as the old per-tick check did
            return;
        }

        dripTimer = 0;
        fill(level, cauldron);
        setChanged();
    }

    /** The first non-air block below, if it is a cauldron that can still take latex. */
    private static BlockPos findCauldron(Level level, BlockPos pos) {
        for (var drop = 1; drop <= MAX_DROP; drop++) {
            var below = pos.below(drop);
            var state = level.getBlockState(below);

            if (state.isAir()) {
                continue;
            }

            return canAccept(state) ? below : null;
        }

        return null;
    }

    private static boolean canAccept(BlockState state) {
        if (state.is(Blocks.CAULDRON)) {
            return true;
        }

        // A latex cauldron takes more only while it is still liquid and not yet full.
        return state.getBlock() instanceof LatexCauldronBlock
            && !state.getValue(LatexCauldronBlock.CURED)
            && state.getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MAX_FILL_LEVEL;
    }

    private static void fill(Level level, BlockPos cauldron) {
        var state = level.getBlockState(cauldron);

        if (state.is(Blocks.CAULDRON)) {
            level.setBlockAndUpdate(
                cauldron,
                CoreBlocks.LATEX_CAULDRON.get()
                    .defaultBlockState()
                    .setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MIN_FILL_LEVEL)
                    .setValue(LatexCauldronBlock.CURED, false)
            );
            return;
        }

        level.setBlockAndUpdate(
            cauldron,
            state.setValue(LayeredCauldronBlock.LEVEL, state.getValue(LayeredCauldronBlock.LEVEL) + 1)
        );
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        dripTimer = tag.getInt("DripTimer");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("DripTimer", dripTimer);
    }
}
