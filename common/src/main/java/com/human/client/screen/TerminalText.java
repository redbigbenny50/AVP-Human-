package com.human.client.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Text that always fits its frame ([stated] Oct 18/29: no text larger than its frame, no overlapping, nothing cut off).
 * <p>
 * Fixed labels are worded to fit (measured against the vanilla font). Values that come from the game - gene, species
 * and biome names, including other mods' - can't all be known in advance, so a value wider than its frame SCROLLS
 * inside it: it holds at the start, slides to show its end, holds, and slides back. It is clipped to the frame while it
 * moves, so it never runs into anything, and every character is shown in turn - nothing is permanently hidden.
 * </p>
 * <p>
 * Lists (loot, biomes, best-donor-of) use {@link #joinFit}: as many whole entries as fit, then "+N" for the rest.
 * </p>
 * <p>
 * Note: vanilla 1.21.1 {@code GuiGraphics.enableScissor} does NOT apply the pose (checked in the bytecode), so the
 * screen's absolute origin is passed in for the clip rectangle.
 * </p>
 */
public final class TerminalText {

    /** Pixels per second the overflow slides at, and the hold at each end. */
    private static final float SPEED = 20.0F;

    private static final float HOLD_SECONDS = 1.5F;

    private TerminalText() {}

    /**
     * Draws {@code text} in the frame {@code [x, x + width)} at row {@code y} (frame-relative coordinates, the pose
     * already translated by {@code originX, originY}). Right-aligned when it fits and {@code right} is set.
     */
    public static void draw(
        GuiGraphics graphics,
        Font font,
        int originX,
        int originY,
        int x,
        int y,
        int width,
        String text,
        int colour,
        boolean right
    ) {
        var textWidth = font.width(text);
        if (textWidth <= width) {
            var drawX = right ? x + width - textWidth : x;
            graphics.drawString(font, text, drawX, y, colour, false);
            return;
        }
        var overflow = textWidth - width;
        var slide = overflow / SPEED;
        var cycle = 2.0F * (slide + HOLD_SECONDS);
        var t = (Util.getMillis() / 1000.0F) % cycle;
        float offset;
        if (t < HOLD_SECONDS) {
            offset = 0.0F;
        } else if (t < HOLD_SECONDS + slide) {
            offset = (t - HOLD_SECONDS) * SPEED;
        } else if (t < 2.0F * HOLD_SECONDS + slide) {
            offset = overflow;
        } else {
            offset = overflow - (t - 2.0F * HOLD_SECONDS - slide) * SPEED;
        }
        graphics.enableScissor(originX + x, originY + y - 1, originX + x + width, originY + y + 9);
        graphics.drawString(font, text, x - Math.round(offset), y, colour, false);
        graphics.disableScissor();
    }

    /** As many whole entries as fit in {@code width}, then " +N" for the ones left out; at least the first entry. */
    public static String joinFit(Font font, List<String> entries, String separator, int width) {
        if (entries.isEmpty()) {
            return "";
        }
        var all = String.join(separator, entries);
        if (font.width(all) <= width) {
            return all;
        }
        for (int count = entries.size() - 1; count >= 1; count--) {
            var text = String.join(separator, entries.subList(0, count)) + " +" + (entries.size() - count);
            if (font.width(text) <= width) {
                return text;
            }
        }
        return entries.get(0) + " +" + (entries.size() - 1);
    }
}
