package com.human.client.input;

import com.human.Human;
import com.human.common.gameplay.item.GunItem;
import com.human.common.network.packet.C2SGunFirePayload;
import net.minecraft.client.Minecraft;

public class GunInputHandler {

    private static int firingTicks;

    private static boolean wasFiring;

    /**
     * Drives gun fire from vanilla's own resolved attack signal.
     * <p>
     * ⚠⚠ TAKE THE FLAG, DO NOT READ THE KEY. {@code attackDown} is the exact boolean {@code Minecraft.handleKeybinds}
     * passes to {@code continueAttack}, and it is the ONLY place a controller mod's attack input exists. Controllable
     * delivers its attack button with an {@code @ModifyExpressionValue} on the {@code KeyMapping.isDown()} call INSIDE
     * that argument expression - it never touches the KeyMapping itself. So reading {@code options.keyAttack.isDown()}
     * here, as this used to, returns the RAW keyboard state and a controller trigger can swing a sword but never fires
     * a gun.
     * <p>
     * Taking the flag also inherits vanilla's own screen and mouse-grabbed checks for free, since they are part of the
     * same expression - and it works for ANY input mod that patches that call, not just Controllable.
     */
    public static void tick(Minecraft minecraft, boolean attackDown) {
        if (!attackDown || !isTryingToFireGun(minecraft)) {
            reset();
            return;
        }

        var player = minecraft.player;
        var itemStack = player.getMainHandItem();
        var tickProgress = wasFiring ? firingTicks : 0;

        wasFiring = true;
        firingTicks++;

        if (itemStack.getItem() instanceof GunItem gunItem) {
            // The server owns shots, hit detection, ammunition and confirmed recoil.
            Human.MOD.networking().sendToServer(new C2SGunFirePayload(tickProgress, player.getYRot(), player.getXRot()));
        }
    }

    public static boolean isHoldingGun(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof GunItem;
    }

    /**
     * Everything about firing EXCEPT the attack signal itself, which arrives as the flag above. The screen and
     * mouse-grabbed tests are kept as a belt-and-braces guard - vanilla folds them into the same expression, but this
     * method is cheap and a future caller might not.
     */
    private static boolean isTryingToFireGun(Minecraft minecraft) {
        return minecraft.screen == null
            && minecraft.player != null
            && minecraft.mouseHandler.isMouseGrabbed()
            && isHoldingGun(minecraft);
    }

    private static void reset() {
        firingTicks = 0;
        wasFiring = false;
    }

    private GunInputHandler() {
        throw new UnsupportedOperationException();
    }
}
