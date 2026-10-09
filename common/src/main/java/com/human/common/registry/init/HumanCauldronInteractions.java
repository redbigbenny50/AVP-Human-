package com.human.common.registry.init;

import com.human.common.gameplay.block.LatexCauldronBlock;
import com.human.common.registry.init.block.CoreBlocks;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;

/**
 * Buckets in and out of a latex cauldron. [stated] Sep 24: "if you attach a liquid pipe to it or bucket the liquid
 * before it hardens into a block you get liquid latex."
 * <p>
 * ⚠⚠ CALL {@link #initialize()} FROM MOD INIT. Vanilla's interaction maps are a plain static map populated at class
 * load; ours is built here and registered into the EMPTY cauldron's map so a latex bucket can be poured into a plain
 * cauldron. Miss this call and the block still exists and still cures - it just cannot be filled or emptied by hand,
 * which reads as "the bucket does nothing" rather than as a missing registration.
 * </p>
 * <p>
 * ⚠ Emptying is refused once the latex has CURED. By then it is a solid block, and the right-click on the cauldron
 * hands it over instead.
 * </p>
 */
public final class HumanCauldronInteractions {

    /** The interactions a latex-filled cauldron itself offers. */
    public static final CauldronInteraction.InteractionMap LATEX = CauldronInteraction.newInteractionMap("latex");

    private HumanCauldronInteractions() {}

    public static void initialize() {
        // Pouring a latex bucket into an EMPTY vanilla cauldron turns it into a full latex cauldron.
        CauldronInteraction.EMPTY.map().put(HumanItems.LATEX_BUCKET.get(), (state, level, pos, player, hand, stack) -> {
            if (!level.isClientSide) {
                var item = stack.getItem();

                player.setItemInHand(
                    hand,
                    ItemStack.isSameItem(stack, new ItemStack(item)) && !player.isCreative()
                        ? new ItemStack(Items.BUCKET)
                        : stack
                );
                player.awardStat(net.minecraft.stats.Stats.FILL_CAULDRON);
                level.setBlockAndUpdate(
                    pos,
                    CoreBlocks.LATEX_CAULDRON.get()
                        .defaultBlockState()
                        .setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL)
                        .setValue(LatexCauldronBlock.CURED, false)
                );
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });

        // Taking liquid latex back out with an empty bucket - only while it is still liquid, and only when full.
        LATEX.map().put(Items.BUCKET, (state, level, pos, player, hand, stack) -> {
            if (
                state.getValue(LatexCauldronBlock.CURED)
                    || state.getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MAX_FILL_LEVEL
            ) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }

            if (!level.isClientSide) {
                if (!player.isCreative()) {
                    player.setItemInHand(hand, new ItemStack(HumanItems.LATEX_BUCKET.get()));
                }

                player.awardStat(net.minecraft.stats.Stats.USE_CAULDRON);
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });
    }
}
