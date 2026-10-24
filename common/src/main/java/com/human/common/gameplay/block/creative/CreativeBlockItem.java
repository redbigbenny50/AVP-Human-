package com.human.common.gameplay.block.creative;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * [stated] Oct 20 - the creative test blocks (Creative Generator, Creative Battery, Creative Fluid Tank) say so in
 * their tooltip, so nobody mistakes one for a real machine: "Creative only - cannot be crafted", in grey.
 */
public class CreativeBlockItem extends BlockItem {

    public static final String TOOLTIP_KEY = "tooltip.avp_human.creative_only";

    public CreativeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(TOOLTIP_KEY).withStyle(ChatFormatting.GRAY));
    }
}
