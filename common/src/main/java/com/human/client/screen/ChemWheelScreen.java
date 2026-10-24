package com.human.client.screen;

import com.human.Human;
import com.human.common.gameplay.item.gun.chem.ChemThrowerReloading;
import com.human.common.gameplay.vapor.VaporKind;
import com.human.common.network.packet.C2SChemReloadPayload;
import com.human.common.registry.init.HumanDataComponents;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The chem thrower's selection wheel. [stated] Oct 18, from his reference image (a weapon wheel):
 * <ul>
 * <li>The gun in the MIDDLE, empty - no tank.</li>
 * <li>Around it, one segment per kind of vapor tank CARRIED - only those ([stated] "not all the tanks possible ever") -
 * each with its tank and its name under it.</li>
 * <li>HOVER a tank and its skin appears on the gun in the middle. CLICK it and it goes on (the insert clip).</li>
 * <li>[stated] "i can still move and jump etc the menu doesnt stop that": movement, jump, sneak and sprint keep working
 * while it is up - their keys are passed straight through. Only looking around stops, since the mouse is the
 * pointer.</li>
 * <li>[stated] Switching to another hotbar slot (number keys or the scroll wheel, both still work) closes it. The gun
 * stays EMPTY; the reload key brings the wheel back - so it can be carried ready, with no menu in the way.</li>
 * <li>Esc or right-click closes it the same way.</li>
 * </ul>
 * Hover works by DIRECTION: anywhere outside the middle, the segment the pointer points at is the one - a flick toward
 * a tank is enough. The palette is the MUTHUR screen one; same font and size as every other GUI ([stated] Oct 16).
 */
public class ChemWheelScreen extends Screen {

    private static final int OUTER = 92;

    private static final int INNER = 46;

    /** Degrees of gap either side of each segment. */
    private static final float GAP = 1.5F;

    private static final int ARC_STEPS_PER_90 = 24;

    private static final int PANEL = 0xC009180F;

    private static final int PANEL_HOVER = 0xE023573A;

    private static final int CENTRE = 0xE009180F;

    private static final int MINT = 0xFF6EF68D;

    private static final int MID = 0xFF33974F;

    /** Rim width - the lit outer edge of the hovered segment, like the blue arc in his reference. */
    private static final int RIM = 3;

    private List<VaporKind> kinds = new ArrayList<>();

    private @Nullable VaporKind hovered;

    private boolean picked;

    public ChemWheelScreen() {
        super(Component.translatable("gui.avp_human.chem_thrower.ammo"));
    }

    @Override
    protected void init() {
        super.init();
        refreshKinds();
        // ⚠ Opening a screen releases every key; a player already holding W would stop dead. Re-read what is
        // physically held so movement carries straight on.
        KeyMapping.setAll();
    }

    private void refreshKinds() {
        var player = minecraft == null ? null : minecraft.player;
        kinds = player == null ? new ArrayList<>() : new ArrayList<>(ChemThrowerReloading.choiceKinds(player));
    }

    @Override
    public void tick() {
        var player = minecraft == null ? null : minecraft.player;

        // Switched item, the gun got loaded some other way, or the tanks are gone: nothing to choose here.
        if (
            player == null || !ChemThrowerReloading.isChemThrower(player.getMainHandItem())
                || ChemThrowerReloading.onGun(player.getMainHandItem()) != null
        ) {
            onClose();
            return;
        }

        refreshKinds();

        if (kinds.isEmpty()) {
            onClose();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        var cx = width / 2;
        var cy = height / 2;
        var count = kinds.size();

        hovered = hoveredAt(mouseX, mouseY);

        if (count == 0) {
            return;
        }

        var span = 360.0F / count;
        var pose = graphics.pose().last().pose();
        var buffer = graphics.bufferSource().getBuffer(RenderType.gui());

        // The middle: a dark disc with a mint ring.
        ring(buffer, pose, cx, cy, 0, INNER - 2, 0.0F, 360.0F, CENTRE);
        ring(buffer, pose, cx, cy, INNER - 2, INNER, 0.0F, 360.0F, MID);

        for (var index = 0; index < count; index++) {
            var start = -90.0F + index * span + GAP;
            var end = -90.0F + (index + 1) * span - GAP;
            var lit = kinds.get(index) == hovered;

            ring(buffer, pose, cx, cy, INNER + 2, OUTER - RIM, start, end, lit ? PANEL_HOVER : PANEL);
            ring(buffer, pose, cx, cy, OUTER - RIM, OUTER, start, end, lit ? MINT : MID);
        }

        graphics.flush();

        for (var index = 0; index < count; index++) {
            var kind = kinds.get(index);
            var middle = Math.toRadians(-90.0F + (index + 0.5F) * span);
            var radius = (INNER + OUTER) / 2.0;
            var x = cx + (int) Math.round(Math.cos(middle) * radius);
            var y = cy + (int) Math.round(Math.sin(middle) * radius);

            graphics.pose().pushPose();
            graphics.pose().translate(x - 12.0F, y - 18.0F, 0.0F);
            graphics.pose().scale(1.5F, 1.5F, 1.0F);
            graphics.renderItem(new ItemStack(kind.tank()), 0, 0);
            graphics.pose().popPose();

            var name = Component.translatable(kind.translationKey());
            graphics.drawString(font, name, x - font.width(name) / 2, y + 8, kind == hovered ? MINT : MID, false);
        }

        drawGun(graphics, cx, cy);
        graphics.drawString(font, title, cx - font.width(title) / 2, cy - INNER + 8, MID, false);
    }

    /** The gun in the middle, three times icon size: empty, or wearing the hovered tank's skin. */
    private void drawGun(GuiGraphics graphics, int cx, int cy) {
        var player = minecraft == null ? null : minecraft.player;

        if (player == null) {
            return;
        }

        var gun = player.getMainHandItem().copy();

        // A display copy only: the renderer shows a tank when the gun has vapor in it, and which one by its id.
        gun.set(HumanDataComponents.CHEM_PREVIOUS_VAPOR.get(), "");

        if (hovered != null) {
            gun.set(HumanDataComponents.AMMUNITION.get(), 1);
            gun.set(HumanDataComponents.CHEM_VAPOR.get(), hovered.id());
        } else {
            gun.set(HumanDataComponents.AMMUNITION.get(), 0);
        }

        graphics.pose().pushPose();
        graphics.pose().translate(cx - 24.0F, cy - 24.0F, 100.0F);
        graphics.pose().scale(3.0F, 3.0F, 1.0F);
        graphics.renderItem(gun, 0, 0);
        graphics.pose().popPose();
    }

    /** An annulus segment from {@code startDeg} to {@code endDeg} (0 = east, clockwise), as a strip of quads. */
    private static void ring(
        com.mojang.blaze3d.vertex.VertexConsumer buffer,
        org.joml.Matrix4f pose,
        int cx,
        int cy,
        float inner,
        float outer,
        float startDeg,
        float endDeg,
        int colour
    ) {
        var steps = Math.max(2, Math.round((endDeg - startDeg) / 90.0F * ARC_STEPS_PER_90));

        for (var step = 0; step < steps; step++) {
            var a0 = Math.toRadians(startDeg + (endDeg - startDeg) * step / steps);
            var a1 = Math.toRadians(startDeg + (endDeg - startDeg) * (step + 1) / steps);
            var c0 = (float) Math.cos(a0);
            var s0 = (float) Math.sin(a0);
            var c1 = (float) Math.cos(a1);
            var s1 = (float) Math.sin(a1);

            buffer.addVertex(pose, cx + c0 * inner, cy + s0 * inner, 0.0F).setColor(colour);
            buffer.addVertex(pose, cx + c1 * inner, cy + s1 * inner, 0.0F).setColor(colour);
            buffer.addVertex(pose, cx + c1 * outer, cy + s1 * outer, 0.0F).setColor(colour);
            buffer.addVertex(pose, cx + c0 * outer, cy + s0 * outer, 0.0F).setColor(colour);
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No blur or darkening: the world stays in view while the player keeps moving.
    }

    /** The segment the pointer points at - by direction, anywhere outside the middle. */
    private @Nullable VaporKind hoveredAt(double mouseX, double mouseY) {
        var count = kinds.size();
        var dx = mouseX - width / 2.0;
        var dy = mouseY - height / 2.0;

        if (count == 0 || dx * dx + dy * dy < INNER * INNER) {
            return null;
        }

        // Angle from straight up, clockwise, 0-360 - the first segment starts at the top.
        var angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        angle = (angle % 360.0 + 360.0) % 360.0;

        return kinds.get(Math.min(count - 1, (int) (angle / (360.0 / count))));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            onClose();
            return true;
        }

        var kind = hoveredAt(mouseX, mouseY);

        if (button == 0 && kind != null) {
            picked = true;
            Human.MOD.networking().sendToServer(new C2SChemReloadPayload(kind.ordinal()));

            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }

            onClose();
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // The hotbar scroll still works - and moving off the gun closes the wheel (see tick).
        var player = minecraft == null ? null : minecraft.player;

        if (player != null && scrollY != 0.0) {
            var inventory = player.getInventory();
            inventory.selected = Math.floorMod(inventory.selected - (int) Math.signum(scrollY), 9);
        }

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.player != null) {
            var options = minecraft.options;

            for (var slot = 0; slot < options.keyHotbarSlots.length; slot++) {
                if (options.keyHotbarSlots[slot].matches(keyCode, scanCode)) {
                    minecraft.player.getInventory().selected = slot;
                    return true;
                }
            }

            if (isMovementKey(keyCode, scanCode)) {
                KeyMapping.set(InputConstants.getKey(keyCode, scanCode), true);
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (isMovementKey(keyCode, scanCode)) {
            KeyMapping.set(InputConstants.getKey(keyCode, scanCode), false);
            return true;
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    /** Walking, jumping, sneaking and sprinting pass straight through the wheel. */
    private boolean isMovementKey(int keyCode, int scanCode) {
        if (minecraft == null) {
            return false;
        }

        var options = minecraft.options;

        for (
            var key : new KeyMapping[] {
                options.keyUp,
                options.keyDown,
                options.keyLeft,
                options.keyRight,
                options.keyJump,
                options.keyShift,
                options.keySprint
            }
        ) {
            if (key.matches(keyCode, scanCode)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
