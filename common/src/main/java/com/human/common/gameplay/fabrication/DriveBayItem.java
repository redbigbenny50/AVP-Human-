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

/**
 * A drive bay as an item. A broken bay keeps its SSDs and blueprints on this item, and shows how many in the tooltip.
 */
public class DriveBayItem extends BlockItem {

    public DriveBayItem(Block block, Properties properties) {
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

        var ssds = 0;
        var blueprints = 0;
        var index = 0;

        for (var item : contents.stream().toList()) {
            if (!item.isEmpty()) {
                if (index < DriveBayBlockEntity.SSD_SLOTS) {
                    ssds++;
                } else {
                    blueprints++;
                }
            }

            index++;
        }

        tooltip.add(Component.translatable("tooltip.avp_human.drive_bay.contents", ssds, blueprints).withStyle(ChatFormatting.GRAY));
    }
}
