package com.human.common.gameplay.block.creative;

import com.human.common.gameplay.fluid.FluidContainerInteractions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * [stated] Oct 20 - the Creative Fluid Tank. A filled bucket SETS its fluid (the bucket is kept); an empty bucket draws
 * a full one of that fluid; sneak + empty hand clears it. Anything else that holds fluid (canisters) uses the shared
 * container rules against its endless supply. See CreativeFluidTankBlockEntity.
 */
public class CreativeFluidTankBlock extends BaseEntityBlock {

    public static final MapCodec<CreativeFluidTankBlock> CODEC = simpleCodec(CreativeFluidTankBlock::new);

    public CreativeFluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(
        @NotNull ItemStack stack,
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof CreativeFluidTankBlockEntity tank)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.is(Items.BUCKET)) {
            if (tank.fluid() == Fluids.EMPTY || tank.fluid().getBucket() == Items.AIR) {
                return ItemInteractionResult.CONSUME;
            }

            if (!level.isClientSide) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(tank.fluid().getBucket())));
                FluidContainerInteractions.playSound(level, pos, tank.fluid(), true);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.getItem() instanceof BucketItem) {
            var fluid = fluidInBucket(stack.getItem());

            if (fluid == null) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!level.isClientSide) {
                tank.setFluid(fluid);
                FluidContainerInteractions.playSound(level, pos, fluid, false);
                player.displayClientMessage(
                    Component.translatable(
                        "message.avp_human.creative_fluid_tank.set",
                        fluid.defaultFluidState().createLegacyBlock().getBlock().getName()
                    ),
                    true
                );
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        // Canisters and the like: the shared rules, against the endless supply and the void.
        return FluidContainerInteractions.useItem(stack, level, pos, player, hand, tank);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof CreativeFluidTankBlockEntity tank) {
            if (!level.isClientSide) {
                tank.setFluid(Fluids.EMPTY);
                player.displayClientMessage(Component.translatable("message.avp_human.creative_fluid_tank.cleared"), true);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    /** Which fluid a bucket holds: the fluid whose own bucket it is (vanilla keeps BucketItem's fluid private). */
    private static @Nullable Fluid fluidInBucket(Item bucket) {
        for (var fluid : BuiltInRegistries.FLUID) {
            if (fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState()) && fluid.getBucket() == bucket) {
                return fluid;
            }
        }

        return null;
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CreativeFluidTankBlockEntity(pos, state);
    }
}
