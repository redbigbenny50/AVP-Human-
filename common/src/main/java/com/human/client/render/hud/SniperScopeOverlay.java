package com.human.client.render.hud;

import com.human.client.input.GunZoomHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class SniperScopeOverlay {

    private static final int MASK_COLOR = 0xD9000000;

    private static final int RETICLE_COLOR = 0xCC000000;

    private static final int RETICLE_HIGHLIGHT_COLOR = 0x99FFFFFF;

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();

        if (!GunZoomHandler.isAimingSniper(minecraft) || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        var width = guiGraphics.guiWidth();
        var height = guiGraphics.guiHeight();
        var centerX = (width - 1) / 2;
        var centerY = (height - 1) / 2;
        var scopeSize = Mth.clamp(Math.min(width, height) - 36, 160, 420);
        var scopeRadius = scopeSize / 2;

        renderMask(guiGraphics, width, height, centerX, centerY, scopeRadius);
        renderReticle(guiGraphics, centerX, centerY, scopeRadius);
    }

    private static void renderMask(GuiGraphics guiGraphics, int width, int height, int centerX, int centerY, int scopeRadius) {
        var left = centerX - scopeRadius;
        var right = centerX + scopeRadius;
        var top = centerY - scopeRadius;
        var bottom = centerY + scopeRadius;

        guiGraphics.fill(0, 0, width, Math.max(0, top), MASK_COLOR);
        guiGraphics.fill(0, Math.min(height, bottom), width, height, MASK_COLOR);
        guiGraphics.fill(0, Math.max(0, top), Math.max(0, left), Math.min(height, bottom), MASK_COLOR);
        guiGraphics.fill(Math.min(width, right), Math.max(0, top), width, Math.min(height, bottom), MASK_COLOR);

        guiGraphics.fill(left, top, right, top + 2, MASK_COLOR);
        guiGraphics.fill(left, bottom - 2, right, bottom, MASK_COLOR);
        guiGraphics.fill(left, top, left + 2, bottom, MASK_COLOR);
        guiGraphics.fill(right - 2, top, right, bottom, MASK_COLOR);
    }

    private static void renderReticle(GuiGraphics guiGraphics, int centerX, int centerY, int scopeRadius) {
        var edgeGap = 16;
        var centerGap = 7;

        guiGraphics.hLine(centerX - scopeRadius + edgeGap, centerX - centerGap, centerY, RETICLE_COLOR);
        guiGraphics.hLine(centerX + centerGap, centerX + scopeRadius - edgeGap, centerY, RETICLE_COLOR);
        guiGraphics.vLine(centerX, centerY - scopeRadius + edgeGap, centerY - centerGap, RETICLE_COLOR);
        guiGraphics.vLine(centerX, centerY + centerGap, centerY + scopeRadius - edgeGap, RETICLE_COLOR);

        guiGraphics.hLine(centerX - 3, centerX + 3, centerY, RETICLE_HIGHLIGHT_COLOR);
        guiGraphics.vLine(centerX, centerY - 3, centerY + 3, RETICLE_HIGHLIGHT_COLOR);

        for (var offset = 28; offset < scopeRadius - 18; offset += 28) {
            var tickLength = offset % 84 == 0 ? 7 : 4;

            guiGraphics.vLine(centerX - offset, centerY - tickLength, centerY + tickLength, RETICLE_COLOR);
            guiGraphics.vLine(centerX + offset, centerY - tickLength, centerY + tickLength, RETICLE_COLOR);
            guiGraphics.hLine(centerX - tickLength, centerX + tickLength, centerY - offset, RETICLE_COLOR);
            guiGraphics.hLine(centerX - tickLength, centerX + tickLength, centerY + offset, RETICLE_COLOR);
        }
    }

    private SniperScopeOverlay() {
        throw new UnsupportedOperationException();
    }
}
