package com.human.common.gameplay.fabrication;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * [stated] Oct 13: a faction's whole blueprint collection on one gold chip - it unlocks that faction's versions of
 * blocks and its own blocks in a fabricator. The commissary sells the USCM license and the electrician the W-Y one, for
 * 32 diamonds each. The blueprint table refuses to scan a license, so one can never be copied.
 */
public class FactionLicenseItem extends Item {

    private final Faction faction;

    public FactionLicenseItem(Faction faction, Properties properties) {
        super(properties);
        this.faction = faction;
    }

    public Faction faction() {
        return faction;
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("tooltip.avp_human.faction_license." + faction.id()).withStyle(ChatFormatting.GOLD));
    }
}
