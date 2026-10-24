package com.human.common.gameplay.fabrication;

import com.human.common.registry.init.HumanFabrication;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * [stated] Oct 13: what the blueprint table makes from an item plus a piece of raw silicon. It records which item can
 * be printed and nothing else - the materials are worked out from the current recipes when a print starts, so a chip
 * traded between servers still prints correctly with whatever recipes that server has.
 */
public class BlueprintChipItem extends Item {

    public BlueprintChipItem(Properties properties) {
        super(properties);
    }

    /** The item this chip prints, or null for a blank chip - or one naming an item that no longer exists. */
    public static @Nullable Item printed(ItemStack stack) {
        var id = stack.get(HumanFabrication.BLUEPRINT.get());

        return id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        var id = stack.get(HumanFabrication.BLUEPRINT.get());
        var item = printed(stack);

        if (id == null) {
            tooltip.add(Component.translatable("tooltip.avp_human.blueprint_chip.blank").withStyle(ChatFormatting.GRAY));
            return;
        }

        if (item == null) {
            tooltip.add(Component.translatable("tooltip.avp_human.blueprint_chip.missing", id.toString()).withStyle(ChatFormatting.RED));
            return;
        }

        tooltip.add(
            Component.translatable("tooltip.avp_human.blueprint_chip.prints", item.getDescription()).withStyle(ChatFormatting.GRAY)
        );
    }
}
