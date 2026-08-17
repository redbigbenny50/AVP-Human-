package com.human.mixin.client;

import com.human.common.gameplay.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ⭐⭐ THE GUN COOLDOWN OVERLAY IS SHOWN ONLY ON THE SLOT ACTUALLY HOLDING THE GUN BEING FIRED.
 * <p>
 * [stated] "the overlay is gone from all icons even the one shooting it. so we want to return it but only to the icon
 * being used not the the others that arent."
 * </p>
 * <p>
 * ⚠⚠ THE TARGET IS {@code GuiGraphics.renderItemDecorations}, NOT {@code Gui.renderSlot}. I aimed at Gui first and
 * mixin rejected it with "Scanned 0 target(s)" — {@code Gui} never calls {@code getCooldownPercent} at all in 1.21.1.
 * The only call site in the client is here, inside {@code renderItemDecorations(Font, ItemStack, int, int, String)},
 * verified by disassembling the vanilla jar.
 * </p>
 * <p>
 * ⚠ WHY NOT IN {@code ItemCooldowns}: {@code getCooldownPercent(Item, float)} receives only the ITEM, and cooldowns are
 * keyed PER ITEM rather than per stack — so from inside that method every slot holding the gun looks identical and the
 * only possible outcome is suppressing ALL of them. This method is handed the specific ItemStack it is decorating,
 * which is the one place the slot identity exists.
 * </p>
 * <p>
 * ⭐ THE TEST IS REFERENCE IDENTITY, DELIBERATELY. The selected hotbar slot decorates the very same ItemStack OBJECT
 * that {@code getMainHandItem()} returns, while every other slot holds a different object — even another Old Painless
 * with identical components. An {@code equals}-style comparison would match those too and bring the strobe straight
 * back on every duplicate.
 * </p>
 * <p>
 * ⚠ ONLY GUNS ARE AFFECTED — every other item keeps vanilla behaviour, including ender pearls and shields, which
 * legitimately show the overlay on every stack of that item.
 * </p>
 */
@Mixin(GuiGraphics.class)
public class MixinGuiGraphics_GunCooldownOverlayOnHeldSlotOnly {

    @Redirect(
        method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemCooldowns;getCooldownPercent(Lnet/minecraft/world/item/Item;F)F"
        )
    )
    private float avp_human$onlyOnHeldSlot(
        ItemCooldowns cooldowns,
        Item item,
        float partialTick,
        // ⚠ THE TARGET METHOD'S OWN PARAMETERS, APPENDED. A @Redirect handler may declare the enclosing method's
        // arguments after the redirected call's arguments, which is the only way to reach the ItemStack being
        // decorated — and that stack is the whole point of this mixin.
        Font font,
        ItemStack itemStack,
        int x,
        int y,
        String text
    ) {
        if (item instanceof GunItem) {
            var player = Minecraft.getInstance().player;

            if (player != null && itemStack != player.getMainHandItem()) {
                return 0.0F;
            }
        }

        return cooldowns.getCooldownPercent(item, partialTick);
    }
}
