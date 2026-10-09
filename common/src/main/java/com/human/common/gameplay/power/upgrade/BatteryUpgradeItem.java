package com.human.common.gameplay.power.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** An upgrade card for a battery's upgrade slots. Does nothing on its own. */
public class BatteryUpgradeItem extends Item {

    private final BatteryUpgrade upgrade;

    public BatteryUpgradeItem(BatteryUpgrade upgrade, Properties properties) {
        super(properties);
        this.upgrade = upgrade;
    }

    public BatteryUpgrade getUpgrade() {
        return upgrade;
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("tooltip.avp_human.battery_upgrade." + upgrade.id()).withStyle(ChatFormatting.GRAY));
        tooltip.add(
            Component.translatable(
                upgrade.stackable() ? "tooltip.avp_human.battery_upgrade.stackable" : "tooltip.avp_human.battery_upgrade.single"
            ).withStyle(ChatFormatting.DARK_GRAY)
        );
    }
}
