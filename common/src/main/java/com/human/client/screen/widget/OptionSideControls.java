package com.human.client.screen.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Side buttons whose pop-ups are a list of choices - the battery and the fuse panel. [stated] Oct 2: one button per
 * field, clicking it opens a sub-menu to pick the setting; a field that needs a card only appears with that card
 * fitted. Same look and behaviour as the machines' {@link MachineSideControls}: the pop-up is modal, Esc closes it, and
 * every choice is a menu button press the server range-checks.
 */
public final class OptionSideControls {

    /** One choice in a pop-up. */
    public record Option(
        Component label,
        boolean selected,
        int buttonId
    ) {}

    /** Draws a button's 16x16 icon. */
    @FunctionalInterface
    public interface Icon {

        void draw(GuiGraphics graphics, int x, int y);
    }

    /** One side button: a list of choices, or (with {@code action}) something done straight away on click. */
    public record Field(
        Component name,
        BooleanSupplier visible,
        Icon icon,
        Supplier<List<Option>> options,
        @org.jetbrains.annotations.Nullable Runnable action
    ) {

        public Field(Component name, BooleanSupplier visible, Icon icon, Supplier<List<Option>> options) {
            this(name, visible, icon, options, null);
        }

        /** A button that acts on click instead of opening a list - the sentry's Targeting page. */
        public static Field action(Component name, BooleanSupplier visible, Icon icon, Runnable action) {
            return new Field(name, visible, icon, List::of, action);
        }
    }

    private static final int BUTTON = 18;

    private static final int GAP = 2;

    private static final int ROW = 14;

    private static final int PAD = 4;

    private static final int TEXT = 0xFF404040;

    private final int containerId;

    private final Font font;

    private final List<Field> fields;

    private int columnX;

    private int columnY;

    private Field open;

    public OptionSideControls(int containerId, Font font, List<Field> fields) {
        this.containerId = containerId;
        this.font = font;
        this.fields = fields;
    }

    /** The column sits just left of the window. */
    public void layout(int leftPos, int topPos) {
        columnX = leftPos - BUTTON - GAP;
        columnY = topPos + PAD;
    }

    private List<Field> visible() {
        var list = new ArrayList<Field>();

        for (var field : fields) {
            if (field.visible().getAsBoolean()) {
                list.add(field);
            }
        }

        // A card taken out while its pop-up is open closes the pop-up.
        if (open != null && !list.contains(open)) {
            open = null;
        }

        return list;
    }

    private int buttonY(int index) {
        return columnY + index * (BUTTON + GAP);
    }

    private int[] popupBox(Field field, int buttonY) {
        var width = 0;
        var options = field.options().get();

        for (var option : options) {
            width = Math.max(width, font.width(option.label()));
        }

        return new int[] { columnX + BUTTON + GAP, buttonY, PAD * 2 + 10 + width, PAD * 2 + options.size() * ROW };
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        var list = visible();

        for (int index = 0; index < list.size(); index++) {
            var field = list.get(index);
            var y = buttonY(index);

            raised(graphics, columnX, y, BUTTON, BUTTON, field == open || inside(mouseX, mouseY, columnX, y, BUTTON, BUTTON));
            field.icon().draw(graphics, columnX + 1, y + 1);
        }

        if (open != null) {
            var box = popupBox(open, buttonY(list.indexOf(open)));
            var options = open.options().get();

            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 400.0F);
            raised(graphics, box[0], box[1], box[2], box[3], false);

            for (int index = 0; index < options.size(); index++) {
                var option = options.get(index);
                var rx = box[0] + PAD;
                var ry = box[1] + PAD + index * ROW;

                if (option.selected()) {
                    graphics.fill(rx - 1, ry, box[0] + box[2] - PAD + 1, ry + ROW - 1, 0xFF9DB3C8);
                } else if (inside(mouseX, mouseY, rx, ry, box[2] - PAD * 2, ROW)) {
                    graphics.fill(rx - 1, ry, box[0] + box[2] - PAD + 1, ry + ROW - 1, 0xFFB8B8B8);
                }

                graphics.drawString(font, option.selected() ? "▶" : " ", rx, ry + 3, TEXT, false);
                graphics.drawString(font, option.label(), rx + 10, ry + 3, TEXT, false);
            }

            graphics.pose().popPose();
        }
    }

    /** @return true when the click was the controls' - the screen must then not handle it. */
    public boolean mouseClicked(double mouseX, double mouseY) {
        var list = visible();

        if (open != null) {
            var box = popupBox(open, buttonY(list.indexOf(open)));

            if (inside(mouseX, mouseY, box[0], box[1], box[2], box[3])) {
                var options = open.options().get();
                var index = (int) ((mouseY - box[1] - PAD) / ROW);

                if (index >= 0 && index < options.size()) {
                    send(options.get(index).buttonId());
                    open = null;
                }

                return true;
            }

            // Modal: elsewhere only closes it - or switches to another side button.
            var clicked = buttonAt(list, mouseX, mouseY);
            open = clicked != null && clicked != open ? clicked : null;
            return true;
        }

        var clicked = buttonAt(list, mouseX, mouseY);

        if (clicked != null) {
            click();

            if (clicked.action() != null) {
                clicked.action().run();
            } else {
                open = clicked;
            }

            return true;
        }

        return false;
    }

    private Field buttonAt(List<Field> list, double mouseX, double mouseY) {
        for (int index = 0; index < list.size(); index++) {
            if (inside(mouseX, mouseY, columnX, buttonY(index), BUTTON, BUTTON)) {
                return list.get(index);
            }
        }

        return null;
    }

    public boolean keyPressed(int keyCode) {
        if (open != null && keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            open = null;
            return true;
        }

        return false;
    }

    public boolean isOpen() {
        return open != null;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        var list = visible();

        if (open != null) {
            var box = popupBox(open, buttonY(list.indexOf(open)));

            if (inside(mouseX, mouseY, box[0], box[1], box[2], box[3])) {
                return true;
            }
        }

        return inside(mouseX, mouseY, columnX, columnY, BUTTON, list.size() * (BUTTON + GAP));
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (open != null) {
            return;
        }

        var field = buttonAt(visible(), mouseX, mouseY);

        if (field != null) {
            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                List.of(field.name(), Component.translatable("gui.avp_human.machine.side.click").withStyle(ChatFormatting.DARK_GRAY)),
                mouseX,
                mouseY
            );
        }
    }

    private void send(int buttonId) {
        var minecraft = Minecraft.getInstance();

        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(containerId, buttonId);
        }

        click();
    }

    private static void click() {
        Minecraft.getInstance()
            .getSoundManager()
            .play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F)
            );
    }

    private static void raised(GuiGraphics graphics, int x, int y, int width, int height, boolean hot) {
        graphics.fill(x, y, x + width, y + height, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, hot ? 0xFFDADADA : 0xFFC6C6C6);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, 0xFFFFFFFF);
        graphics.fill(x + 2, y + height - 2, x + width - 1, y + height - 1, 0xFF555555);
        graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 1, 0xFF555555);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
