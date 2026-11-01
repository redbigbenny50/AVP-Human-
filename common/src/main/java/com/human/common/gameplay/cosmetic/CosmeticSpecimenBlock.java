package com.human.common.gameplay.cosmetic;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A cosmetic piece with a swappable interior - terrariums, specimen tanks, crystal cases. [stated] Oct 31: right-click
 * it with a sapling, mushroom, amethyst shard, prismarine crystals, emerald, redstone, bone, rotten flesh or
 * fermented spider eye to change what is inside.
 * Only the contents its model has are accepted; anything else does nothing. The item is not used up.
 * <p>
 * The choice lives in the {@link #CONTENTS} state of every cell, so it saves with the world and the renderer reads it
 * from the core without any block-entity data.
 * </p>
 */
public class CosmeticSpecimenBlock extends CosmeticBlock {

    public static final EnumProperty<CosmeticContents> CONTENTS = EnumProperty.create("contents", CosmeticContents.class);

    private final List<CosmeticContents> accepted;

    protected CosmeticSpecimenBlock(Properties properties, CosmeticDesign design, CosmeticStyle style) {
        super(properties, design, style);
        this.accepted = accepted(design.blockName(style));
        registerDefaultState(defaultBlockState().setValue(CONTENTS, accepted.isEmpty() ? CosmeticContents.PLANT : accepted.getFirst()));
    }

    private static List<CosmeticContents> accepted(String name) {
        var out = new ArrayList<CosmeticContents>();

        for (var bone : CosmeticModelTables.CONTENTS.getOrDefault(name, List.of())) {
            var contents = CosmeticContents.fromBone(bone);

            if (contents != null) {
                out.add(contents);
            }
        }

        return out;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONTENTS);
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(properties -> CosmeticBlock.create(properties, design(), style()));
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
        var contents = CosmeticContents.fromItem(stack);

        if (contents == null || !accepted.contains(contents)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }

        var core = corePos(pos, state);
        var coreState = level.getBlockState(core);

        if (!coreState.is(this) || coreState.getValue(CONTENTS) == contents) {
            return ItemInteractionResult.CONSUME;
        }

        if (!level.isClientSide) {
            // Every cell carries the value, so the whole piece agrees whichever block is looked at.
            for (var cell : cells()) {
                var cellPos = cellPos(core, coreState.getValue(FACING), cell);
                var cellState = level.getBlockState(cellPos);

                if (cellState.is(this)) {
                    level.setBlock(cellPos, cellState.setValue(CONTENTS, contents), Block.UPDATE_CLIENTS);
                }
            }

            level.playSound(null, pos, com.human.common.registry.init.HumanSoundEvents.BLOCK_COSMETIC_ITEM_PLACE.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** How to change the contents - shown to everyone, whichever recipe viewer they use (or none). */
    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        Item.@NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.avp_human.cosmetic.contents_hint").withStyle(ChatFormatting.GRAY));

        for (var contents : accepted) {
            tooltip.add(Component.literal(" - ").append(Component.translatable(contents.translationKey())).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public List<CosmeticContents> acceptedContents() {
        return accepted;
    }
}
