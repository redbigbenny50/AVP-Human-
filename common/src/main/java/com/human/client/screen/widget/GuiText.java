package com.human.client.screen.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps GUI text inside its frame.
 * <p>
 * [stated] Oct 18 - "Text should never be larger than its frames borders there should be no overlapping text", and Oct
 * 16 - all GUI text stays the same font and size. [stated] Oct 23 - the MU/TH/UR defence terminal's empty-network line
 * ran straight off the side of its panel, "can you check other things that might have same error".
 * </p>
 * <p>
 * Nothing here shrinks or scales text. A fixed sentence that is too long either wraps onto more lines inside its frame
 * or is reworded shorter in the lang file; text the player controls (a turret's name, a block position) is cut at the
 * frame with "..." because there is no rewording a name.
 * </p>
 * <p>
 * ⚠ TOOLTIPS. Vanilla draws {@code renderTooltip(font, Component, ...)} and {@code renderComponentTooltip} as ONE line
 * per component, however long - several of ours ran 300-580 px, wider than the whole screen at a large GUI scale.
 * {@link #tooltip} wraps them at the same 170 px vanilla's own button tooltips ({@code Tooltip.create}) use.
 * </p>
 */
public final class GuiText {

    /** The wrap width of vanilla's widget tooltips ({@code Tooltip.MAX_WIDTH}), so every tooltip in the mod matches. */
    public static final int TOOLTIP_WIDTH = 170;

    /** One line of the vanilla font: 9 px, the spacing every wrapped block in the mod already uses. */
    public static final int LINE_HEIGHT = 9;

    private static final String ELLIPSIS = "...";

    private GuiText() {}

    /**
     * Draws {@code text} wrapped to {@code maxWidth}, one line every {@link #LINE_HEIGHT} px, at most {@code maxLines}
     * lines. Returns how many lines were drawn.
     */
    public static int drawWrapped(
        GuiGraphics graphics,
        Font font,
        Component text,
        int x,
        int y,
        int maxWidth,
        int maxLines,
        int colour,
        boolean shadow
    ) {
        var lines = font.split(text, Math.max(1, maxWidth));
        var count = Math.min(lines.size(), Math.max(1, maxLines));

        for (int line = 0; line < count; line++) {
            graphics.drawString(font, lines.get(line), x, y + line * LINE_HEIGHT, colour, shadow);
        }

        return count;
    }

    /** As {@link #drawWrapped}, each line centred on {@code centreX}. */
    public static int drawCenteredWrapped(
        GuiGraphics graphics,
        Font font,
        Component text,
        int centreX,
        int y,
        int maxWidth,
        int maxLines,
        int colour
    ) {
        var lines = font.split(text, Math.max(1, maxWidth));
        var count = Math.min(lines.size(), Math.max(1, maxLines));

        for (int line = 0; line < count; line++) {
            var sequence = lines.get(line);
            graphics.drawString(font, sequence, centreX - font.width(sequence) / 2, y + line * LINE_HEIGHT, colour, true);
        }

        return count;
    }

    /** {@code text}, cut with "..." if it is wider than {@code maxWidth}. For player-controlled text only. */
    public static FormattedCharSequence fit(Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text.getVisualOrderText();
        }

        var room = Math.max(0, maxWidth - font.width(ELLIPSIS));
        FormattedText cut = font.substrByWidth(text, room);
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of(ELLIPSIS)));
    }

    /** {@code text}, cut with "..." if it is wider than {@code maxWidth}. For player-controlled text only. */
    public static String fit(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(ELLIPSIS))) + ELLIPSIS;
    }

    /**
     * A tooltip wrapped at {@link #TOOLTIP_WIDTH}, in place of the one-line
     * {@code renderTooltip(font, Component, ...)}.
     */
    public static void tooltip(GuiGraphics graphics, Font font, Component text, int mouseX, int mouseY) {
        graphics.renderTooltip(font, font.split(text, TOOLTIP_WIDTH), mouseX, mouseY);
    }

    /**
     * A multi-line tooltip, each line wrapped at {@link #TOOLTIP_WIDTH}, in place of {@code renderComponentTooltip}.
     */
    public static void tooltip(GuiGraphics graphics, Font font, List<? extends FormattedText> lines, int mouseX, int mouseY) {
        var wrapped = new ArrayList<FormattedCharSequence>();

        for (var line : lines) {
            var split = font.split(line, TOOLTIP_WIDTH);

            // An empty line is a deliberate spacer; split() returns nothing for it, which would close the gap.
            if (split.isEmpty()) {
                wrapped.add(FormattedCharSequence.EMPTY);
            } else {
                wrapped.addAll(split);
            }
        }

        graphics.renderTooltip(font, wrapped, mouseX, mouseY);
    }
}
