package com.human.common.gameplay.item.canister;

import com.human.common.data.HumanAdvancements;
import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The universal canister: one item that holds up to {@value #CAPACITY_BUCKETS} buckets of ANY fluid - vanilla, ours, or
 * any other mod's.
 * <h2>HOW IT KNOWS WHAT IT HOLDS</h2> The fluid and amount live in the {@code fluid_content} component
 * ({@link FluidContent}). Nothing here names a fluid: clicking a source stores whatever fluid is actually there. This
 * is what the old one-item-per-fluid canisters could not do, and why no mod's fluid could go in one.
 * <h2>HOW IT LOOKS</h2> A filled canister carries custom model data 1, which vanilla's built-in
 * {@code custom_model_data} property turns into the filled model - no model property of our own, so nothing
 * loader-specific. That model's second layer is a greyscale window tinted by the fluid's colour ({@code FluidColors} on
 * the client).
 * <h2>STACKING</h2> Empty canisters stack to 16 as they always have. A filled one is capped at 1 by the
 * {@code max_stack_size} component, set when it fills and cleared when it empties.
 * <h2>WHAT IT DOES NOT HOLD</h2> Powder snow is a block, not a fluid, and milk is not a fluid in vanilla - clicking
 * powder snow still gives the powder snow canister, and cows still fill the milk canister, exactly as before.
 */
public class UniversalCanisterItem extends Item {

    public static final int CAPACITY_BUCKETS = 8;

    public static final int CAPACITY = CAPACITY_BUCKETS * FluidContent.BUCKET;

    /** The model switch: vanilla's generic {@code custom_model_data} property reads this. */
    public static final int FILLED_MODEL_DATA = 1;

    public UniversalCanisterItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Content
    // ------------------------------------------------------------------------------------------------------------

    public static FluidContent getContent(ItemStack stack) {
        return stack.getOrDefault(HumanDataComponents.FLUID_CONTENT.get(), FluidContent.EMPTY);
    }

    /**
     * Writes the content onto the stack, and keeps the two companions in step: a filled canister stacks to one and
     * shows the filled model; an empty one loses all three, so it stacks with every other empty canister again.
     */
    public static void setContent(ItemStack stack, FluidContent content) {
        if (content.isEmpty()) {
            stack.remove(HumanDataComponents.FLUID_CONTENT.get());
            stack.remove(DataComponents.MAX_STACK_SIZE);
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
            return;
        }

        var clamped = content.withAmount(Math.min(content.amount(), CAPACITY));

        stack.set(HumanDataComponents.FLUID_CONTENT.get(), clamped);
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(FILLED_MODEL_DATA));
    }

    /** A single full canister of this fluid - for the creative tab and for conversions. */
    public static ItemStack filled(Fluid fluid, int amount) {
        var stack = new ItemStack(HumanItems.CANISTER.get());

        setContent(stack, FluidContent.of(fluid, amount));

        return stack;
    }

    /**
     * Every fluid that can go in a canister: a SOURCE fluid with a bucket. The bucket is the test other mods agree on
     * for "a real, placeable fluid", and it filters out the technical fluids some mods register for internal use.
     */
    public static List<Fluid> canisterFluids() {
        var fluids = new ArrayList<Fluid>();

        for (var fluid : BuiltInRegistries.FLUID) {
            if (fluid == Fluids.EMPTY || !fluid.isSource(fluid.defaultFluidState()) || fluid.getBucket() == Items.AIR) {
                continue;
            }

            fluids.add(fluid);
        }

        return fluids;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Use
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var content = getContent(stack);
        var placing = player.isShiftKeyDown() && !content.isEmpty();
        var hit = getPlayerPOVHitResult(level, player, placing ? ClipContext.Fluid.NONE : ClipContext.Fluid.SOURCE_ONLY);

        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        var hitPos = hit.getBlockPos();
        var relative = hitPos.relative(hit.getDirection());

        if (!level.mayInteract(player, hitPos) || !player.mayUseItemAt(relative, hit.getDirection(), stack)) {
            return InteractionResultHolder.fail(stack);
        }

        return placing ? place(level, player, stack, content, hit) : pickUp(level, player, stack, content, hitPos);
    }

    private InteractionResultHolder<ItemStack> pickUp(
        Level level,
        Player player,
        ItemStack stack,
        FluidContent content,
        BlockPos pos
    ) {
        var state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof BucketPickup pickup)) {
            return InteractionResultHolder.pass(stack);
        }

        // Powder snow is a block, not a fluid: it keeps its own canister, as before.
        if (state.is(Blocks.POWDER_SNOW)) {
            if (!content.isEmpty() || pickup.pickupBlock(player, level, pos, state).isEmpty()) {
                return InteractionResultHolder.fail(stack);
            }

            return finishPickup(level, player, stack, pickup, pos, new ItemStack(HumanItems.POWDER_SNOW_CANISTER.get()));
        }

        var fluidState = level.getFluidState(pos);
        var fluid = FluidContent.sourceOf(fluidState.getType());

        // ⚠ Everything is decided BEFORE the world is touched: a fluid this canister cannot take is left exactly
        // where it is. (The old canister removed the source first and worked out what to return afterwards.)
        if (fluid == Fluids.EMPTY || !fluidState.isSource() || !content.accepts(fluid)) {
            return InteractionResultHolder.fail(stack);
        }

        if (content.amount() + FluidContent.BUCKET > CAPACITY) {
            return InteractionResultHolder.fail(stack);
        }

        if (pickup.pickupBlock(player, level, pos, state).isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }

        var result = stack.copyWithCount(1);

        setContent(result, FluidContent.of(fluid, content.amount() + FluidContent.BUCKET));

        if (getContent(result).amount() >= CAPACITY && player instanceof ServerPlayer serverPlayer) {
            HumanAdvancements.FILL_CANISTER.grant(serverPlayer);
        }

        return finishPickup(level, player, stack, pickup, pos, result);
    }

    private InteractionResultHolder<ItemStack> finishPickup(
        Level level,
        Player player,
        ItemStack stack,
        BucketPickup pickup,
        BlockPos pos,
        ItemStack result
    ) {
        player.awardStat(Stats.ITEM_USED.get(this));
        pickup.getPickupSound().ifPresent(sound -> player.playSound(sound, 1.0F, 1.0F));
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);

        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer, result);
        }

        // A stack of empty canisters: one leaves the stack and the filled one goes to the inventory; a single canister
        // is
        // simply replaced. createFilledResult does both - the same call vanilla's bucket makes.
        return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(stack, player, result), level.isClientSide());
    }

    private InteractionResultHolder<ItemStack> place(
        Level level,
        Player player,
        ItemStack stack,
        FluidContent content,
        BlockHitResult hit
    ) {
        var hitPos = hit.getBlockPos();
        var hitState = level.getBlockState(hitPos);
        var target = hitState.getBlock() instanceof LiquidBlockContainer && content.fluid().is(FluidTags.WATER)
            ? hitPos
            : hitPos.relative(hit.getDirection());

        if (!placeFluid(player, level, target, hit, content.fluid())) {
            return InteractionResultHolder.fail(stack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, target, stack);
        }

        player.awardStat(Stats.ITEM_USED.get(this));

        if (!player.hasInfiniteMaterials()) {
            setContent(stack, content.withAmount(content.amount() - FluidContent.BUCKET));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Places one bucket's worth of the fluid as a source - vanilla BucketItem.emptyContents, for any fluid. */
    public static boolean placeFluid(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult hit, Fluid fluid) {
        if (!(fluid instanceof FlowingFluid flowing)) {
            return false;
        }

        var state = level.getBlockState(pos);
        var block = state.getBlock();
        var replaceable = state.canBeReplaced(fluid);
        var canPlace = state.isAir()
            || replaceable
            || block instanceof LiquidBlockContainer container && container.canPlaceLiquid(player, level, pos, state, fluid);

        if (!canPlace) {
            return hit != null && placeFluid(player, level, hit.getBlockPos().relative(hit.getDirection()), null, fluid);
        }

        if (level.dimensionType().ultraWarm() && fluid.is(FluidTags.WATER)) {
            level.playSound(
                player,
                pos,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.5F,
                2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F
            );

            for (var i = 0; i < 8; i++) {
                level.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    pos.getX() + Math.random(),
                    pos.getY() + Math.random(),
                    pos.getZ() + Math.random(),
                    0.0D,
                    0.0D,
                    0.0D
                );
            }

            return true;
        }

        if (block instanceof LiquidBlockContainer container && fluid.is(FluidTags.WATER)) {
            container.placeLiquid(level, pos, state, flowing.getSource(false));
            playEmptySound(player, level, pos, fluid);
            return true;
        }

        if (!level.isClientSide && replaceable && !state.liquid()) {
            level.destroyBlock(pos, true);
        }

        if (!level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), 11) && !state.getFluidState().isSource()) {
            return false;
        }

        playEmptySound(player, level, pos, fluid);

        return true;
    }

    private static void playEmptySound(@Nullable Player player, Level level, BlockPos pos, Fluid fluid) {
        var sound = fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;

        level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Name and tooltip
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        var content = getContent(stack);

        return content.isEmpty()
            ? super.getName(stack)
            : Component.translatable("item.avp_human.canister.filled", fluidName(content.fluid()));
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        var content = getContent(stack);

        if (content.isEmpty()) {
            return;
        }

        tooltip.add(
            Component.translatable(
                "tooltip.avp_human.canister.amount",
                content.amount() / FluidContent.BUCKET,
                CAPACITY_BUCKETS
            ).withStyle(ChatFormatting.GRAY)
        );
    }

    /**
     * A fluid's display name. Vanilla fluids have no name of their own, so this goes through the fluid's placed block -
     * "Water", "Lava" - and falls back to its bucket's name for a mod fluid whose block has no translation.
     */
    public static Component fluidName(Fluid fluid) {
        var block = fluid.defaultFluidState().createLegacyBlock().getBlock();
        var key = block.getDescriptionId();

        if (Language.getInstance().has(key)) {
            return Component.translatable(key);
        }

        return fluid.getBucket().getDescription();
    }
}
