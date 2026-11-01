package com.human.client.screen;

import com.human.Human;
import com.human.common.gameplay.harvest.HarvestGenes;
import com.human.common.network.packet.C2SHarvestPickPayload;
import com.human.common.network.packet.S2COpenHarvestWheelPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The harvest selection wheel: a bucket used on a mob carrying more than one bucket substance. [stated] Oct 25 - "like
 * the chem thrower's fuel wheel", so it is that wheel's layout, palette and controls: one segment per substance the mob
 * carries (with what the bucket comes out as, and its name), the empty bucket in the middle, pick by pointing and
 * clicking; movement keys pass through; Esc, right-click or switching items closes it.
 */
public class HarvestWheelScreen extends Screen {

    private static final int OUTER = 92;

    private static final int INNER = 46;

    private static final float GAP = 1.5F;

    private static final int ARC_STEPS_PER_90 = 24;

    private static final int PANEL = 0xC009180F;

    private static final int PANEL_HOVER = 0xE023573A;

    private static final int CENTRE = 0xE009180F;

    private static final int MINT = 0xFF6EF68D;

    private static final int MID = 0xFF33974F;

    private static final int RIM = 3;

    /** How far the player may walk from the mob before the wheel closes. */
    private static final double RANGE_SQR = 8.0 * 8.0;

    private final int entityId;

    private final InteractionHand hand;

    private final List<HarvestGenes.Substance> substances = new ArrayList<>();

    private @Nullable HarvestGenes.Substance hovered;

    private HarvestWheelScreen(S2COpenHarvestWheelPayload payload) {
        super(Component.translatable("gui.avp_human.harvest_wheel"));
        this.entityId = payload.entityId();
        this.hand = payload.hand() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        for (var substance : HarvestGenes.Substance.values()) {
            if ((payload.substances() & (1 << substance.ordinal())) != 0) {
                substances.add(substance);
            }
        }
    }

    public static void open(S2COpenHarvestWheelPayload payload) {
        var minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.setScreen(new HarvestWheelScreen(payload)));
    }

    @Override
    protected void init() {
        super.init();
        // Opening a screen releases every key; re-read what is physically held so movement carries on.
        KeyMapping.setAll();
    }

    @Override
    public void tick() {
        var player = minecraft == null ? null : minecraft.player;

        if (
            player == null || substances.isEmpty() || !player.getItemInHand(hand).is(Items.BUCKET)
                || player.level().getEntity(entityId) == null
                || player.distanceToSqr(player.level().getEntity(entityId)) > RANGE_SQR
        ) {
            onClose();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        var cx = width / 2;
        var cy = height / 2;
        var count = substances.size();

        hovered = hoveredAt(mouseX, mouseY);

        if (count == 0) {
            return;
        }

        var span = 360.0F / count;
        var pose = graphics.pose().last().pose();
        var buffer = graphics.bufferSource().getBuffer(RenderType.gui());

        ring(buffer, pose, cx, cy, 0, INNER - 2, 0.0F, 360.0F, CENTRE);
        ring(buffer, pose, cx, cy, INNER - 2, INNER, 0.0F, 360.0F, MID);

        for (var index = 0; index < count; index++) {
            var start = -90.0F + index * span + GAP;
            var end = -90.0F + (index + 1) * span - GAP;
            var lit = substances.get(index) == hovered;

            ring(buffer, pose, cx, cy, INNER + 2, OUTER - RIM, start, end, lit ? PANEL_HOVER : PANEL);
            ring(buffer, pose, cx, cy, OUTER - RIM, OUTER, start, end, lit ? MINT : MID);
        }

        graphics.flush();

        for (var index = 0; index < count; index++) {
            var substance = substances.get(index);
            var middle = Math.toRadians(-90.0F + (index + 0.5F) * span);
            var radius = (INNER + OUTER) / 2.0;
            var x = cx + (int) Math.round(Math.cos(middle) * radius);
            var y = cy + (int) Math.round(Math.sin(middle) * radius);
            var result = substance.result();

            graphics.pose().pushPose();
            graphics.pose().translate(x - 12.0F, y - 18.0F, 0.0F);
            graphics.pose().scale(1.5F, 1.5F, 1.0F);
            graphics.renderItem(result, 0, 0);
            graphics.pose().popPose();

            var name = Component.translatable(substance.gene.getTranslationKey());
            graphics.drawString(font, name, x - font.width(name) / 2, y + 8, substance == hovered ? MINT : MID, false);
        }

        // The bucket in the middle: empty, or showing what the hovered segment would fill it with.
        graphics.pose().pushPose();
        graphics.pose().translate(cx - 24.0F, cy - 24.0F, 100.0F);
        graphics.pose().scale(3.0F, 3.0F, 1.0F);
        graphics.renderItem(hovered == null ? new net.minecraft.world.item.ItemStack(Items.BUCKET) : hovered.result(), 0, 0);
        graphics.pose().popPose();

        graphics.drawString(font, title, cx - font.width(title) / 2, cy - INNER + 8, MID, false);
    }

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

    private @Nullable HarvestGenes.Substance hoveredAt(double mouseX, double mouseY) {
        var count = substances.size();
        var dx = mouseX - width / 2.0;
        var dy = mouseY - height / 2.0;

        if (count == 0 || dx * dx + dy * dy < INNER * INNER) {
            return null;
        }

        var angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        angle = (angle % 360.0 + 360.0) % 360.0;

        return substances.get(Math.min(count - 1, (int) (angle / (360.0 / count))));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            onClose();
            return true;
        }

        var substance = hoveredAt(mouseX, mouseY);

        if (button == 0 && substance != null) {
            Human.MOD.networking().sendToServer(new C2SHarvestPickPayload(entityId, substance.ordinal(), hand.ordinal()));

            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }

            onClose();
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
