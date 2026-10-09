package com.human.common.gameplay.power.upgrade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A machine or sentry upgrade card. The tooltip says what it does, what it fits, and whether more copies add up - the
 * same three lines the battery cards show.
 */
public class UpgradeCardItem extends Item {

    private final UpgradeCard card;

    public UpgradeCardItem(UpgradeCard card, Properties properties) {
        super(properties);
        this.card = card;
    }

    public UpgradeCard getCard() {
        return card;
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("tooltip.avp_human.upgrade_card." + card.id()).withStyle(ChatFormatting.GRAY));
        tooltip.add(
            Component.translatable(
                card.fits(UpgradeCard.Target.SENTRY)
                    ? "tooltip.avp_human.upgrade_card.fits_sentry"
                    : "tooltip.avp_human.upgrade_card.fits_machine"
            ).withStyle(ChatFormatting.DARK_GRAY)
        );
        tooltip.add(
            Component.translatable(
                card.group() != null
                    ? "tooltip.avp_human.upgrade_card.one_ammo"
                    : card.stackable() ? "tooltip.avp_human.upgrade_card.stackable" : "tooltip.avp_human.upgrade_card.single"
            ).withStyle(ChatFormatting.DARK_GRAY)
        );
    }
}
