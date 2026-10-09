package com.human.common.gameplay.item;

import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * [stated] Oct 2: the Skin Kit - one item for every skin. It carries the skin's id as a component and is tinted with
 * the skin's colour. Skins are defined in resource packs beside their textures ({@code assets/<ns>/avp_human/skins/}),
 * so a single pack adds a whole skin; a kit whose skin is missing makes the turret fall back to the USCM default.
 */
public class SkinKitItem extends Item {

    public SkinKitItem(Properties properties) {
        super(properties);
    }

    public static @Nullable ResourceLocation skinOf(ItemStack stack) {
        return stack.get(HumanDataComponents.SENTRY_SKIN.get());
    }

    public static ItemStack of(Item item, ResourceLocation skin) {
        var stack = new ItemStack(item);
        stack.set(HumanDataComponents.SENTRY_SKIN.get(), skin);
        return stack;
    }

    /** Named after its skin: "Gold Skin Kit". */
    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        var skin = skinOf(stack);

        return skin == null
            ? super.getName(stack)
            : Component.translatable(
                "item.avp_human.skin_kit.named",
                Component.translatable("skin." + skin.getNamespace() + "." + skin.getPath())
            );
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.avp_human.skin_kit").withStyle(ChatFormatting.GRAY));
    }
}
