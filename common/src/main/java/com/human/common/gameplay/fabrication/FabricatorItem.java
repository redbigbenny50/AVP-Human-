package com.human.common.gameplay.fabrication;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** A fabricator as an item. A broken one keeps its storage and blueprints on this item. */
public class FabricatorItem extends BlockItem {

    public FabricatorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        var contents = stack.get(DataComponents.CONTAINER);

        if (contents == null) {
            return;
        }

        var stored = 0;
        var blueprints = 0;
        var index = 0;

        for (var item : contents.stream().toList()) {
            if (!item.isEmpty()) {
                if (
                    index >= FabricatorBlockEntity.FIRST_BLUEPRINT && index < FabricatorBlockEntity.OUTPUT_SLOT
                ) {
                    blueprints++;
                } else {
                    stored += item.getCount();
                }
            }

            index++;
        }

        tooltip.add(Component.translatable("tooltip.avp_human.fabricator.contents", stored, blueprints).withStyle(ChatFormatting.GRAY));
    }
}
