package com.human.client.input;

import com.blib.api.common.dismemberment.v1.hitbox.LimbHitPredictionRegistry;
import com.human.Human;
import com.human.common.gameplay.item.GunItem;
import com.human.common.network.packet.C2SGunFirePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class GunInputHandler {

    private static int firingTicks;

    private static boolean wasFiring;

    public static void tick(Minecraft minecraft) {
        if (!isTryingToFireGun(minecraft)) {
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
            var prediction = LimbHitPredictionRegistry.findNearest(
                player.level(),
                player.getEyePosition(),
                player.getEyePosition().add(player.getViewVector(1.0F).scale(128.0D))
            ).orElse(null);
            if (prediction != null && minecraft.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
                player.displayClientMessage(Component.literal("Visual limb: " + prediction.limbId().getPath()), true);
            }
            Human.MOD.networking()
                .sendToServer(
                    new C2SGunFirePayload(
                        tickProgress,
                        player.getYRot(),
                        player.getXRot(),
                        prediction == null ? -1 : prediction.entityId(),
                        prediction == null ? "" : prediction.limbId().toString()
                    )
                );
        }
    }

    public static boolean isHoldingGun(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof GunItem;
    }

    private static boolean isTryingToFireGun(Minecraft minecraft) {
        return minecraft.screen == null
            && minecraft.player != null
            && minecraft.mouseHandler.isMouseGrabbed()
            && minecraft.options.keyAttack.isDown()
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
