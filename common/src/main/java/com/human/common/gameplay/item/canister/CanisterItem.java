package com.human.common.gameplay.item.canister;

import com.human.common.data.HumanAdvancements;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CanisterItem extends Item implements DispensibleContainerItem {

    public final Fluid content;

    public static final int MAX_CAPACITY = 8;

    public CanisterItem(Fluid content, Properties properties) {
        super(properties);
        this.content = content;
    }

    /**
     * ⭐ LEGACY. The water, lava and latex canisters are one-item-per-fluid, from before the universal canister
     * ({@link UniversalCanisterItem}). They stay registered - so any sitting in a chest still load instead of vanishing
     * - but the moment one is in a player's inventory it turns itself into a universal canister holding the same fluid
     * and the same number of buckets. [stated] Sep 29: yes to universal canisters, with existing ones converted.
     * <p>
     * ⚠ Replaced by IDENTITY in the player's inventory lists, not by {@code slotId}: vanilla ticks the main inventory,
     * the armour and the off-hand with their own separate indexes, so slot 0 could mean the hotbar or the off-hand.
     * </p>
     */
    @Override
    public void inventoryTick(
        @NotNull ItemStack stack,
        @NotNull Level level,
        @NotNull net.minecraft.world.entity.Entity entity,
        int slotId,
        boolean isSelected
    ) {
        if (level.isClientSide || this.content == Fluids.EMPTY || !(entity instanceof Player player)) {
            return;
        }

        var buckets = Math.max(1, stack.getOrDefault(HumanDataComponents.CANISTER_CAPACITY.get(), 1));
        var replacement = UniversalCanisterItem.filled(
            this.content,
            buckets * com.human.common.gameplay.fluid.FluidContent.BUCKET
        );

        replaceByIdentity(player.getInventory().items, stack, replacement);
        replaceByIdentity(player.getInventory().armor, stack, replacement);
        replaceByIdentity(player.getInventory().offhand, stack, replacement);
    }

    private static void replaceByIdentity(java.util.List<ItemStack> list, ItemStack target, ItemStack replacement) {
        for (var index = 0; index < list.size(); index++) {
            if (list.get(index) == target) {
                list.set(index, replacement);
                return;
            }
        }
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltipComponents,
        @NotNull TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        int currentContentAmount = stack.getOrDefault(HumanDataComponents.CANISTER_CAPACITY.get(), 0);

        if (currentContentAmount == 0) {
            return;
        }

        tooltipComponents.add(Component.translatable("tooltip.avp.capacity").append(currentContentAmount + "/" + MAX_CAPACITY));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var canisterStack = player.getItemInHand(usedHand);
        var hitResult = getPlayerPOVHitResult(
            level,
            player,
            isFluidPlacementAction(player) ? ClipContext.Fluid.NONE : ClipContext.Fluid.SOURCE_ONLY
        );

        if (isInvalidHitResult(hitResult)) {
            return InteractionResultHolder.fail(canisterStack);
        }

        var hitPos = hitResult.getBlockPos();
        var hitDir = hitResult.getDirection();
        var relativePos = hitPos.relative(hitDir);

        if (!canPlayerInteract(level, player, hitPos, relativePos, hitDir, canisterStack)) {
            return InteractionResultHolder.fail(canisterStack);
        }

        var hitState = level.getBlockState(hitPos);

        if (isFluidPickupAction(player, hitState)) {
            return handleFluidPickup(player, level, canisterStack, hitPos, hitState);
        } else if (isFluidPlacementAction(player)) {
            return handleFluidPlacement(player, level, canisterStack, hitResult, hitPos, hitState, relativePos);
        }

        return InteractionResultHolder.fail(canisterStack);
    }

    public static boolean isInvalidHitResult(BlockHitResult hitResult) {
        return hitResult.getType() == HitResult.Type.MISS || hitResult.getType() != HitResult.Type.BLOCK;
    }

    public static boolean canPlayerInteract(
        Level level,
        Player player,
        BlockPos hitPos,
        BlockPos relativePos,
        Direction hitDir,
        ItemStack stack
    ) {
        return level.mayInteract(player, hitPos) && player.mayUseItemAt(relativePos, hitDir, stack);
    }

    public static boolean isFluidPickupAction(Player player, BlockState hitState) {
        return !player.isShiftKeyDown() && hitState.getBlock() instanceof BucketPickup;
    }

    private boolean isFluidPlacementAction(Player player) {
        return player.isShiftKeyDown() && this.content != Fluids.EMPTY;
    }

    private InteractionResultHolder<ItemStack> handleFluidPickup(
        Player player,
        Level level,
        ItemStack canisterStack,
        BlockPos hitPos,
        BlockState hitState
    ) {
        var bucketPickup = (BucketPickup) hitState.getBlock();

        if (canisterStack.getOrDefault(HumanDataComponents.CANISTER_CAPACITY.get(), 0) >= MAX_CAPACITY) {
            return InteractionResultHolder.fail(canisterStack);
        }

        ItemStack filledStack = pickupBlock(player, level, canisterStack, bucketPickup, hitPos, hitState);

        if (filledStack.isEmpty()) {
            return InteractionResultHolder.fail(canisterStack);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        bucketPickup.getPickupSound().ifPresent(sound -> player.playSound(sound, 1.0F, 1.0F));
        level.gameEvent(player, GameEvent.FLUID_PICKUP, hitPos);

        ItemStack resultStack;

        if (this.content != Fluids.EMPTY) {
            resultStack = updateCapacity(player, filledStack, 1);
        } else {
            resultStack = ItemUtils.createFilledResult(canisterStack, player, filledStack);
        }

        if (!level.isClientSide) {
            CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer) player, filledStack);
        }

        return InteractionResultHolder.sidedSuccess(resultStack, level.isClientSide());
    }

    private InteractionResultHolder<ItemStack> handleFluidPlacement(
        Player player,
        Level level,
        ItemStack canisterStack,
        BlockHitResult hitResult,
        BlockPos hitPos,
        BlockState hitState,
        BlockPos relativePos
    ) {
        var targetPos = hitState.getBlock() instanceof LiquidBlockContainer && this.content == Fluids.WATER ? hitPos : relativePos;

        if (!emptyContents(player, level, targetPos, hitResult)) {
            return InteractionResultHolder.fail(canisterStack);
        }

        checkExtraContent(player, level, canisterStack, targetPos);

        if (player instanceof ServerPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, targetPos, canisterStack);
        }

        player.awardStat(Stats.ITEM_USED.get(this));

        ItemStack resultStack;

        if (canisterStack.getOrDefault(HumanDataComponents.CANISTER_CAPACITY.get(), 0) > 1 && !player.isCreative()) {
            resultStack = updateCapacity(player, canisterStack, -1);
        } else {
            resultStack = ItemUtils.createFilledResult(canisterStack, player, getEmptySuccessItem(canisterStack, player));
        }

        return InteractionResultHolder.sidedSuccess(resultStack, level.isClientSide());
    }

    public static ItemStack updateCapacity(LivingEntity livingEntity, ItemStack stack, int amount) {
        int currentContentAmount = stack.getOrDefault(HumanDataComponents.CANISTER_CAPACITY.get(), 0);
        int newAmount = Mth.clamp(currentContentAmount + amount, 0, MAX_CAPACITY);

        if (newAmount >= MAX_CAPACITY && livingEntity instanceof ServerPlayer serverPlayer) {
            HumanAdvancements.FILL_CANISTER.grant(serverPlayer);
        }

        stack.applyComponents(
            DataComponentPatch.builder()
                .set(HumanDataComponents.CANISTER_CAPACITY.get(), newAmount)
                .build()
        );

        return stack;
    }

    /**
     * Takes the source into this canister, or returns {@link ItemStack#EMPTY} - and removes NOTHING - when it cannot.
     * <h2>⚠⚠ DECIDE FIRST, THEN TAKE</h2> This used to call {@code pickupBlock} FIRST and only then work out which
     * canister to return. Two bugs came from that order, both now closed:
     * <ul>
     * <li>An EMPTY canister on a fluid it had no canister for - latex, anything modded - removed the source and handed
     * back the same empty canister. The source was simply destroyed.</li>
     * <li>A FILLED canister topped itself up from ANY source: a latex canister clicked on water drank the water and
     * counted it as latex.</li>
     * </ul>
     * Now the result is chosen from what is actually there, and the world is only touched once there is an answer. An
     * empty return makes the caller fail the click, so nothing is lost.
     */
    private ItemStack pickupBlock(
        Player player,
        Level level,
        ItemStack canisterStack,
        BucketPickup bucketPickup,
        BlockPos blockPos,
        BlockState blockState
    ) {
        var fluidHere = level.getFluidState(blockPos).getType();
        ItemStack result;

        if (this.content != Fluids.EMPTY) {
            // Topping up: only from the same fluid.
            if (fluidHere == Fluids.EMPTY || !this.content.isSame(fluidHere)) {
                return ItemStack.EMPTY;
            }

            result = canisterStack;
        } else {
            result = filledCanisterFor(fluidHere, blockState);

            if (result.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        var picked = bucketPickup.pickupBlock(player, level, blockPos, blockState);

        // Something refused to give up its contents after all - keep the canister as it was.
        return picked.isEmpty() ? ItemStack.EMPTY : result;
    }

    /**
     * The filled canister for what is at the clicked spot, or EMPTY when there is no canister for it. Powder snow is a
     * block with no fluid, so it is recognised by the block.
     */
    private static ItemStack filledCanisterFor(Fluid fluid, BlockState blockState) {
        if (blockState.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)) {
            return new ItemStack(HumanItems.POWDER_SNOW_CANISTER.get());
        }

        if (fluid.isSame(Fluids.WATER)) {
            return new ItemStack(HumanItems.WATER_CANISTER.get());
        }

        if (fluid.isSame(Fluids.LAVA)) {
            return new ItemStack(HumanItems.LAVA_CANISTER.get());
        }

        if (fluid.isSame(com.human.common.registry.init.HumanFluids.LATEX.get())) {
            return new ItemStack(HumanItems.LATEX_CANISTER.get());
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getEmptySuccessItem(ItemStack canisterStack, Player player) {
        return !player.hasInfiniteMaterials() ? new ItemStack(HumanItems.CANISTER.get()) : canisterStack;
    }

    @Override
    public boolean emptyContents(@Nullable Player player, @NotNull Level level, @NotNull BlockPos pos, @Nullable BlockHitResult result) {
        if (!(this.content instanceof FlowingFluid flowingFluid)) {
            return false;
        }

        var blockState = level.getBlockState(pos);
        var block = blockState.getBlock();
        var isReplaceable = blockState.canBeReplaced(this.content);

        boolean canEmptyHere;

        if (!blockState.isAir() && !isReplaceable) {
            canEmptyHere = block instanceof LiquidBlockContainer liquidBlockContainer && liquidBlockContainer.canPlaceLiquid(
                player,
                level,
                pos,
                blockState,
                this.content
            );
        } else {
            canEmptyHere = true;
        }

        if (!canEmptyHere) {
            return result != null && this.emptyContents(player, level, result.getBlockPos().relative(result.getDirection()), null);
        }

        if (level.dimensionType().ultraWarm() && this.content.is(FluidTags.WATER)) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            level.playSound(
                player,
                pos,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.5F,
                2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F
            );

            for (int i = 0; i < 8; i++) {
                level.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    x + Math.random(),
                    y + Math.random(),
                    z + Math.random(),
                    0.0,
                    0.0,
                    0.0
                );
            }
            return true;
        }

        if (block instanceof LiquidBlockContainer liquidBlockContainer && this.content == Fluids.WATER) {
            liquidBlockContainer.placeLiquid(level, pos, blockState, flowingFluid.getSource(false));
            this.playEmptySound(player, level, pos);
            return true;
        }

        if (!level.isClientSide && isReplaceable && !blockState.liquid()) {
            level.destroyBlock(pos, true);
        }

        if (!level.setBlock(pos, this.content.defaultFluidState().createLegacyBlock(), 11) && !blockState.getFluidState().isSource()) {
            return false;
        } else {
            this.playEmptySound(player, level, pos);
            return true;
        }
    }

    protected void playEmptySound(@Nullable Player player, LevelAccessor level, BlockPos pos) {
        var soundEvent = this.content.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        level.playSound(player, pos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    }
}
