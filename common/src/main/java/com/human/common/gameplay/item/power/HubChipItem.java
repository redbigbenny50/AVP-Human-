package com.human.common.gameplay.item.power;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The HUB CHIP - the teal card. [stated] Sep 30: "a machine with that upgrade can be wired to the hub". In a machine's
 * hub slot it lets the wiring tool bind that machine to a fuse panel, which then powers it with NO WIRE - it only has
 * to be within the panel's range. Take it out and the machine drops off the panel.
 */
public class HubChipItem extends Item {

    public HubChipItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("tooltip.avp_human.hub_upgrade").withStyle(ChatFormatting.GRAY));
    }
}
