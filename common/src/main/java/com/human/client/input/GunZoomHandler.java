package com.human.client.input;

import com.human.common.gameplay.item.GunItem;
import com.human.common.registry.init.item.HumanGunItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class GunZoomHandler {

    private static final double AIM_ZOOM_MULTIPLIER = 0.9;

    private static final double DEFAULT_SCOPED_ZOOM_MULTIPLIER = 0.75;

    private static final double MIN_SCOPED_ZOOM_MULTIPLIER = 0.2;

    private static final double MAX_SCOPED_ZOOM_MULTIPLIER = 0.75;

    private static final double ZOOM_STEP = 0.1;

    private static double scopedZoomMultiplier = DEFAULT_SCOPED_ZOOM_MULTIPLIER;

    public static boolean shouldHandleScroll(Minecraft minecraft) {
        return isAimingGun(minecraft);
    }

    public static boolean shouldZoomOnScroll(Minecraft minecraft) {
        return shouldHandleScroll(minecraft)
            && isZoomableGun(minecraft.player.getUseItem());
    }

    public static void adjust(double scrollY) {
        if (scrollY > 0.0) {
            scopedZoomMultiplier = Math.max(MIN_SCOPED_ZOOM_MULTIPLIER, scopedZoomMultiplier - ZOOM_STEP);
        } else if (scrollY < 0.0) {
            scopedZoomMultiplier = Math.min(MAX_SCOPED_ZOOM_MULTIPLIER, scopedZoomMultiplier + ZOOM_STEP);
        }
    }

    public static double applyZoom(double fov, Minecraft minecraft) {
        if (!isAimingGun(minecraft)) {
            scopedZoomMultiplier = DEFAULT_SCOPED_ZOOM_MULTIPLIER;
            return fov;
        }

        if (isZoomableGun(minecraft.player.getUseItem())) {
            return fov * scopedZoomMultiplier;
        }

        scopedZoomMultiplier = DEFAULT_SCOPED_ZOOM_MULTIPLIER;
        return fov * AIM_ZOOM_MULTIPLIER;
    }

    public static boolean isAimingSniper(Minecraft minecraft) {
        return isAimingGun(minecraft) && isSniper(minecraft.player.getUseItem());
    }

    public static boolean isAimingRocketLauncher(Minecraft minecraft) {
        return isAimingGun(minecraft)
            && minecraft.player.getUseItem().getItem() == HumanGunItems.M6B_ROCKET_LAUNCHER.get();
    }

    private static boolean isAimingGun(Minecraft minecraft) {
        return minecraft.screen == null
            && minecraft.getOverlay() == null
            && minecraft.player != null
            && minecraft.player.isUsingItem()
            // ⚠⚠ DO NOT REINSTATE `&& minecraft.options.keyUse.isDown()` HERE. It broke the sniper scope AND the FOV
            // zoom on Steam Deck and on every controller setup: KeyMapping.isDown() reflects only Minecraft's OWN
            // keybinding state, and Steam Input / Controllable / MidnightControls drive the USE action without ever
            // leaving that mapping flagged down. So isUsingItem() was TRUE while keyUse.isDown() was FALSE, and every
            // caller of this method (SniperScopeOverlay.render via isAimingSniper, and MixinGameRenderer_GunZoom)
            // silently did nothing.
            // <p>
            // It was redundant anyway: isUsingItem() already means the use action is being held, and the instanceof
            // below already identifies WHAT is being held. The keybind added no information on keyboard-and-mouse and
            // excluded every other input device.
            // </p>
            && minecraft.player.getUseItem().getItem() instanceof GunItem;
    }

    private static boolean isZoomableGun(ItemStack itemStack) {
        var item = itemStack.getItem();

        return item instanceof GunItem
            && (isSniper(itemStack)
                || item == HumanGunItems.M4RA_BATTLE_RIFLE.get()
                || item == HumanGunItems.M6B_ROCKET_LAUNCHER.get());
    }

    private static boolean isSniper(ItemStack itemStack) {
        return itemStack.getItem() == HumanGunItems.M42A3_SNIPER_RIFLE.get();
    }

    private GunZoomHandler() {
        throw new UnsupportedOperationException();
    }
}
