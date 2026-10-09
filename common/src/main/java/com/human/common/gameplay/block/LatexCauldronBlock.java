package com.human.common.gameplay.block;

import com.human.common.registry.init.HumanCauldronInteractions;
import com.human.common.registry.init.block.CoreBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * A cauldron holding latex. [stated] Sep 24: "you need to place a cauldron under the latex and it fills up like rain or
 * lava think dripstone", "if you leave it in the cauldron it will start hardening into a latex block", "you extract the
 * latex block just by right clicking the cauldron", and "if you attach a liquid pipe to it or bucket the liquid before
 * it hardens into a block you get liquid latex".
 * <h2>The two lives of a full cauldron</h2> A cauldron fills to {@value LayeredCauldronBlock#MAX_FILL_LEVEL} like any
 * other. Once full it starts curing, and {@link #CURED} is the fork in the road:
 * <ul>
 * <li>NOT cured - still liquid. A bucket or canister takes liquid latex out, which is the whole reason to come back for
 * it promptly.</li>
 * <li>CURED - it has set. Right-click and you get a latex BLOCK and an empty cauldron. The liquid is gone; you can no
 * longer bucket it.</li>
 * </ul>
 * That is the trade the design is built around: latex left alone becomes a solid, and liquid latex is something you
 * have to go and collect.
 * <p>
 * ⚠ Precipitation is {@link Biome.Precipitation#NONE}: rain must NOT top up a latex cauldron. The superclass fills from
 * weather otherwise, which would water down the tap entirely.
 * </p>
 * <p>
 * ⚠ Curing is a RANDOM TICK, like the liquid block, so nothing is stored per cauldron. At the default rate that
 * averages a bit over two minutes once full.
 * </p>
 */
public class LatexCauldronBlock extends LayeredCauldronBlock {

    /** False while it is still liquid and bucketable; true once it has set into a block. */
    public static final BooleanProperty CURED = BooleanProperty.create("cured");

    public static final float CURE_CHANCE_PER_RANDOM_TICK = 0.5F;

    public LatexCauldronBlock(Properties properties) {
        super(Biome.Precipitation.NONE, HumanCauldronInteractions.LATEX, properties);
        registerDefaultState(defaultBlockState().setValue(LEVEL, MIN_FILL_LEVEL).setValue(CURED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CURED);
    }

    // ⚠ NO codec() OVERRIDE, AND THAT IS NOT AN OMISSION. LayeredCauldronBlock.codec() is declared to return exactly
    // MapCodec<LayeredCauldronBlock>, not a wildcard, so a subclass CANNOT narrow it - the compiler rejects any
    // override. Inheriting the parent's codec is the only legal option; it is used for datapack block definitions,
    // which this block is never created through.

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
        // Only a FULL cauldron sets. A part-filled one is still being filled and should wait for the tap.
        if (state.getValue(CURED) || state.getValue(LEVEL) < MAX_FILL_LEVEL) {
            return;
        }

        if (random.nextFloat() >= CURE_CHANCE_PER_RANDOM_TICK) {
            return;
        }

        level.setBlockAndUpdate(pos, state.setValue(CURED, true));
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hitResult
    ) {
        if (!state.getValue(CURED)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            popResource(level, pos, new ItemStack(CoreBlocks.LATEX_BLOCK.get()));
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
