package com.human.client.screen;

import com.human.client.input.AdsSightCalibration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/** Pauses a singleplayer world while the player drags the held sight into the center marker. */
public class AdsSightCalibrationScreen extends Screen {

    private static final float OFFSET_PER_PIXEL = 0.004F;

    private final Item item;

    private final float originalAdjustment;

    private boolean dragging;

    private double dragStartX;

    private float dragStartAdjustment;

    private boolean applied;

    public AdsSightCalibrationScreen(Item item) {
        super(Component.literal("ADS Sight Calibration"));
        this.item = item;
        this.originalAdjustment = AdsSightCalibration.adjustment(item);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No background: the paused first-person view remains visible behind this overlay.
        var centerX = this.width / 2;
        var centerY = this.height / 2;
        graphics.fill(centerX - 1, centerY - 18, centerX + 1, centerY + 19, 0xFFFFD600);
        graphics.fill(centerX - 18, centerY - 1, centerX + 19, centerY + 1, 0xFFFFD600);
        graphics.drawCenteredString(this.font, "DRAG LEFT / RIGHT UNTIL THE SIGHT SITS ON THE YELLOW MARKER", centerX, 16, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, "ENTER: save    R: reset    ESC: cancel", centerX, 28, 0xFFB0B0B0);
        graphics.drawCenteredString(
            this.font,
            String.format(java.util.Locale.ROOT, "Offset: %+.3f", AdsSightCalibration.horizontalOffset(item)),
            centerX,
            40,
            0xFFFFFFFF
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = true;
            dragStartX = mouseX;
            dragStartAdjustment = AdsSightCalibration.adjustment(item);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            AdsSightCalibration.setAdjustment(item, dragStartAdjustment + (float) ((mouseX - dragStartX) * OFFSET_PER_PIXEL));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            applied = true;
            AdsSightCalibration.save();
            onClose();
            return true;
        }
        if (keyCode == 82) {
            AdsSightCalibration.setAdjustment(item, 0.0F);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (!applied) {
            AdsSightCalibration.setAdjustment(item, originalAdjustment);
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
