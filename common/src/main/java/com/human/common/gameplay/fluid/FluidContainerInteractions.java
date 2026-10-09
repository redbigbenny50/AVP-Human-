package com.human.common.gameplay.fluid;

import com.human.common.gameplay.item.canister.UniversalCanisterItem;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Buckets and canisters used on any {@link FluidEndpoint} - the placed canister and the fluid tank share this.
 * <ul>
 * <li>Empty bucket: takes one bucket out and gives a bucket of that fluid. [stated] Sep 29.</li>
 * <li>Full bucket (any mod's): pours one bucket in and gives the empty bucket back.</li>
 * <li>Universal canister: pours one bucket in if it holds fluid the endpoint can take, otherwise draws one out.</li>
 * </ul>
 * One bucket per click, both ways - one fill level on a placed canister.
 */
public final class FluidContainerInteractions {

    /**
     * Bucket item -> the fluid it holds. Vanilla keeps BucketItem's fluid private, so it is looked up from the fluids.
     */
    private static Map<Item, Fluid> bucketFluids;

    private FluidContainerInteractions() {}

    /** @return the result, or PASS_TO_DEFAULT_BLOCK_INTERACTION when the item is not a fluid container */
    public static ItemInteractionResult useItem(
        ItemStack stack,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        FluidEndpoint endpoint
    ) {
        if (stack.is(HumanItems.CANISTER.get())) {
            return withCanister(stack, level, pos, player, hand, endpoint);
        }

        if (stack.is(Items.BUCKET)) {
            return drainIntoBucket(stack, level, pos, player, hand, endpoint);
        }

        if (stack.getItem() instanceof BucketItem) {
            return pourFromBucket(stack, level, pos, player, hand, endpoint);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static ItemInteractionResult withCanister(
        ItemStack stack,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        FluidEndpoint endpoint
    ) {
        var held = UniversalCanisterItem.getContent(stack);
        var inside = endpoint.getContent();

        if (!held.isEmpty() && endpoint.insert(held.fluid(), FluidContent.BUCKET, true) == FluidContent.BUCKET) {
            if (!level.isClientSide) {
                endpoint.insert(held.fluid(), FluidContent.BUCKET, false);

                if (!player.hasInfiniteMaterials()) {
                    UniversalCanisterItem.setContent(stack, held.withAmount(held.amount() - FluidContent.BUCKET));
                }

                playSound(level, pos, held.fluid(), false);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (
            !inside.isEmpty()
                && held.accepts(inside.fluid())
                && held.amount() + FluidContent.BUCKET <= UniversalCanisterItem.CAPACITY
                && endpoint.extract(inside.fluid(), FluidContent.BUCKET, true) == FluidContent.BUCKET
        ) {
            if (!level.isClientSide) {
                var fluid = inside.fluid();
                var filled = stack.copyWithCount(1);

                endpoint.extract(fluid, FluidContent.BUCKET, false);
                UniversalCanisterItem.setContent(filled, FluidContent.of(fluid, held.amount() + FluidContent.BUCKET));
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled));
                playSound(level, pos, fluid, true);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.FAIL;
    }

    private static ItemInteractionResult drainIntoBucket(
        ItemStack stack,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        FluidEndpoint endpoint
    ) {
        var inside = endpoint.getContent();

        if (
            inside.isEmpty()
                || inside.fluid().getBucket() == Items.AIR
                || endpoint.extract(inside.fluid(), FluidContent.BUCKET, true) < FluidContent.BUCKET
        ) {
            return ItemInteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            var fluid = inside.fluid();

            endpoint.extract(fluid, FluidContent.BUCKET, false);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(fluid.getBucket())));
            playSound(level, pos, fluid, true);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static ItemInteractionResult pourFromBucket(
        ItemStack stack,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        FluidEndpoint endpoint
    ) {
        var fluid = fluidInBucket(stack.getItem());

        if (fluid == null || endpoint.insert(fluid, FluidContent.BUCKET, true) < FluidContent.BUCKET) {
            return ItemInteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            endpoint.insert(fluid, FluidContent.BUCKET, false);

            if (!player.hasInfiniteMaterials()) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }

            playSound(level, pos, fluid, false);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** "Water: 3 / 8 buckets" - or "Empty". */
    public static Component describe(FluidEndpoint endpoint) {
        var content = endpoint.getContent();

        if (content.isEmpty()) {
            return Component.translatable("message.avp_human.canister.empty");
        }

        return Component.translatable(
            "message.avp_human.canister.contents",
            UniversalCanisterItem.fluidName(content.fluid()),
            String.format(java.util.Locale.ROOT, "%,d", content.amount() / FluidContent.BUCKET),
            String.format(java.util.Locale.ROOT, "%,d", endpoint.getCapacity() / FluidContent.BUCKET)
        ).withStyle(ChatFormatting.GRAY);
    }

    public static void playSound(Level level, BlockPos pos, Fluid fluid, boolean fill) {
        var lava = fluid.is(FluidTags.LAVA);
        var sound = fill
            ? (lava ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL)
            : (lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY);

        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public static synchronized @Nullable Fluid fluidInBucket(Item bucket) {
        if (bucketFluids == null) {
            bucketFluids = new HashMap<>();

            for (var fluid : UniversalCanisterItem.canisterFluids()) {
                bucketFluids.put(fluid.getBucket(), fluid);
            }
        }

        return bucketFluids.get(bucket);
    }
}
