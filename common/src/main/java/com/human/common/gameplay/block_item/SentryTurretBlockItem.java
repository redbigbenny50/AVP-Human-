package com.human.common.gameplay.block_item;

import com.blib.api.common.tooltip.v1.TooltipCategoryType;
import com.blib.api.common.tooltip.v1.TooltipHintBuilder;
import com.human.common.data.HumanTooltipTranslationKeys;
import com.human.common.registry.init.HumanBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SentryTurretBlockItem extends BlockItem {

    /**
     * ⚠ The two control lines are not decoration. Picking the turret up USED to be a plain right-click and is now sneak
     * + right-click, because plain right-click reports what the turret is waiting on - and a player who learned the old
     * gesture has no other way to discover the new one.
     */
    private static final List<Component> TOOLTIP_COMPONENTS = new TooltipHintBuilder()
        .addCategory(TooltipCategoryType.REQUIREMENTS)
        .addNegativeEffect(HumanTooltipTranslationKeys.REQUIRES_REDSTONE_POWER)
        .addNegativeEffect(HumanTooltipTranslationKeys.REQUIRES_NEARBY_AMMO_CHEST_WITH_AMMO)
        .addCategory(TooltipCategoryType.WHEN_PLACED_IN_WORLD)
        .addPositiveEffect(HumanTooltipTranslationKeys.USE_CHECK_TURRET_STATUS)
        .addPositiveEffect(HumanTooltipTranslationKeys.USE_PICK_UP_TURRET)
        .build();

    public SentryTurretBlockItem() {
        super(HumanBlocks.SENTRY_TURRET.get(), new Item.Properties());
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltipComponents,
        @NotNull TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.addAll(TOOLTIP_COMPONENTS);
    }
}
