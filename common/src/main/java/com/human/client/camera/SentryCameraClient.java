package com.human.client.camera;

import com.human.Human;
import com.human.common.gameplay.entity.machine.SentryTurret;
import com.human.common.network.packet.C2SSentryControlPayload;
import com.human.common.network.packet.S2CSentryViewPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Looking through a turret's camera lens. [stated] Oct 2: the view moves into the turret; the mouse aims it within its
 * field of fire, attack fires it, sneak leaves. The player's body stays where it stood - its movement keys are released
 * every tick - and can be attacked meanwhile.
 * <p>
 * The server decides everything that matters: it started the session, clamps the aim, fires, and ends the session the
 * moment anything stops being true. This side only moves the camera and passes the controls on.
 * </p>
 */
public final class SentryCameraClient {

    private static final int MINT = 0xFF6EF68D;

    private static final int DIM = 0xFF2E6B3E;

    /** The turret being looked through, or -1. */
    private static int entityId = S2CSentryViewPayload.END;

    private SentryCameraClient() {}

    public static boolean isActive() {
        return entityId != S2CSentryViewPayload.END;
    }

    /** The server starting or ending a session. */
    public static void handleView(S2CSentryViewPayload payload) {
        var minecraft = Minecraft.getInstance();

        if (payload.entityId() == S2CSentryViewPayload.END) {
            reset(minecraft);
            return;
        }

        entityId = payload.entityId();

        if (minecraft.level != null && minecraft.player != null && minecraft.level.getEntity(entityId) instanceof SentryTurret turret) {
            // Start looking where the turret is looking, so the view does not jump.
            minecraft.player.setYRot(turret.getYRot());
            minecraft.player.setXRot(turret.getXRot());
            minecraft.setCameraEntity(turret);
        }
    }

    private static void reset(Minecraft minecraft) {
        entityId = S2CSentryViewPayload.END;

        if (minecraft.player != null) {
            minecraft.setCameraEntity(minecraft.player);
        }
    }

    /** Called at the start of every client tick, before vanilla reads the key bindings. */
    public static void tick() {
        if (!isActive()) {
            return;
        }

        var minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            entityId = S2CSentryViewPayload.END;
            return;
        }

        if (!(minecraft.level.getEntity(entityId) instanceof SentryTurret turret) || turret.isRemoved()) {
            // The turret is gone from this client - stop, and tell the server.
            Human.MOD.networking().sendToServer(new C2SSentryControlPayload(0.0F, 0.0F, false, true));
            reset(minecraft);
            return;
        }

        if (minecraft.getCameraEntity() != turret) {
            minecraft.setCameraEntity(turret);
        }

        var options = minecraft.options;

        // The body stays where it stood.
        for (
            KeyMapping key : new KeyMapping[] {
                options.keyUp,
                options.keyDown,
                options.keyLeft,
                options.keyRight,
                options.keyJump,
                options.keySprint }
        ) {
            key.setDown(false);
        }

        var exit = options.keyShift.isDown();
        options.keyShift.setDown(false);

        var trigger = minecraft.screen == null && options.keyAttack.isDown();
        var yaw = minecraft.player.getYRot();
        var pitch = minecraft.player.getXRot();

        // Turn the view straight away; the server clamps it to the turret's cone and its answer follows.
        turret.setYRot(yaw);
        turret.setYHeadRot(yaw);
        turret.setXRot(pitch);

        Human.MOD.networking().sendToServer(new C2SSentryControlPayload(yaw, pitch, trigger, exit));

        if (exit) {
            reset(minecraft);
        }
    }

    /** The camera HUD: the smartgun's mint frame, a crosshair and the controls. */
    public static void renderHud(GuiGraphics graphics) {
        if (!isActive()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var width = graphics.guiWidth();
        var height = graphics.guiHeight();
        var inset = 12;
        var arm = 18;

        // Corner brackets.
        corner(graphics, inset, inset, 1, 1, arm);
        corner(graphics, width - inset - 1, inset, -1, 1, arm);
        corner(graphics, inset, height - inset - 1, 1, -1, arm);
        corner(graphics, width - inset - 1, height - inset - 1, -1, -1, arm);

        // A crosshair with a gap in the middle.
        var cx = width / 2;
        var cy = height / 2;
        graphics.fill(cx - 9, cy, cx - 3, cy + 1, MINT);
        graphics.fill(cx + 3, cy, cx + 9, cy + 1, MINT);
        graphics.fill(cx, cy - 9, cx + 1, cy - 3, MINT);
        graphics.fill(cx, cy + 3, cx + 1, cy + 9, MINT);

        var font = minecraft.font;
        graphics.drawString(font, Component.translatable("gui.avp_human.sentry.camera.title"), inset + 6, inset + 6, MINT, false);
        graphics.drawString(
            font,
            Component.translatable("gui.avp_human.sentry.camera.controls"),
            inset + 6,
            height - inset - 14,
            DIM,
            false
        );
    }

    private static void corner(GuiGraphics graphics, int x, int y, int dx, int dy, int arm) {
        var x2 = x + dx * arm;
        var y2 = y + dy * arm;
        graphics.fill(Math.min(x, x2), y, Math.max(x, x2) + 1, y + 1, MINT);
        graphics.fill(x, Math.min(y, y2), x + 1, Math.max(y, y2) + 1, MINT);
    }

    /**
     * [stated] Oct 5: remote control was jittery. The aim was applied only in {@link #tick()} - 20 times a second while
     * the mouse moves every FRAME - and between ticks the client interpolated the turret back toward the server's
     * slightly older rotation. Called once per frame just before the world is drawn (after the tick has interpolated),
     * it puts the live aim on the turret and on its previous-frame rotation too, so nothing is blended and the view
     * follows the mouse exactly. The server still clamps where the turret actually fires.
     */
    public static void applyViewThisFrame() {
        if (!isActive()) {
            return;
        }

        var minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (!(minecraft.level.getEntity(entityId) instanceof SentryTurret turret) || turret.isRemoved()) {
            return;
        }

        var yaw = minecraft.player.getYRot();
        var pitch = minecraft.player.getXRot();
        turret.setYRot(yaw);
        turret.yRotO = yaw;
        turret.setYHeadRot(yaw);
        turret.yHeadRotO = yaw;
        turret.setXRot(pitch);
        turret.xRotO = pitch;
    }
}
