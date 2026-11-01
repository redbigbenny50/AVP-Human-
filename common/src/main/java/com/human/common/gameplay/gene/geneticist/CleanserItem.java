package com.human.common.gameplay.gene.geneticist;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * [stated] Oct 29: the cleanser - an empty gene tube and a diamond. Used up cleansing one syringe at the Geneticist
 * Station.
 */
public class CleanserItem extends Item {

    public CleanserItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> lines,
        @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(
            Component.translatable("item.avp_human.cleanser.tooltip.bonus", Math.round(GeneCleanser.BONUS * 100))
                .withStyle(ChatFormatting.GREEN)
        );
        lines.add(Component.translatable("item.avp_human.cleanser.tooltip.use").withStyle(ChatFormatting.GRAY));
    }
}
