package com.human.client.screen.widget;

import com.human.HumanResources;
import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.machine.MachinePriority;
import com.human.common.gameplay.machine.MachineUpgrades;
import com.human.common.gameplay.menu.machine.ElectricMachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The machine screen's side buttons. [stated] Oct 2: one button per FIELD, and clicking it opens a small sub-menu to
 * pick the setting - priority, filter, redstone (only with a redstone card fitted) and, on the press, its outputs.
 * <p>
 * A pop-up is modal: while one is open, a click outside it only closes it, so nothing behind it is clicked by accident.
 * Esc closes it too. Every choice is sent to the server as a menu button press; the server range-checks the id and the
 * screen redraws from the synced values, so the client never decides anything.
 * </p>
 */
public final class MachineSideControls {

    private static final ResourceLocation FILTER_SYMBOL = HumanResources.location("textures/gui/container/slot/filter.png");

    private static final int BUTTON = 18;

    private static final int GAP = 2;

    private static final int ROW = 18;

    private static final int PAD = 4;

    private static final int BG = 0xFFC6C6C6;

    private static final int LIGHT = 0xFFFFFFFF;

    private static final int SHADOW = 0xFF555555;

    private static final int OUTLINE = 0xFF000000;

    private static final int WELL_DARK = 0xFF373737;

    private static final int WELL_FILL = 0xFF8B8B8B;

    private static final int TEXT = 0xFF404040;

    private enum Field {
        PRIORITY,
        FILTER,
        REDSTONE,
        OUTPUTS,
        PORTS
    }

    /** [stated] Oct 2 port colours: blue in, orange out, green both, grey off. */
    private static int portColour(com.human.common.gameplay.machine.MachinePorts.Mode mode) {
        return switch (mode) {
            case IN -> 0xFF3B7BD8;
            case OUT -> 0xFFE08A2B;
            case BOTH -> 0xFF4DAE4D;
            case OFF -> 0xFF6A6A6A;
        };
    }

    /** The face map, as an unfolded cube: {column, row} per face. */
    private static int[] cell(com.human.common.gameplay.machine.MachinePorts.Face face) {
        return switch (face) {
            case TOP -> new int[] { 1, 0 };
            case LEFT -> new int[] { 0, 1 };
            case FRONT -> new int[] { 1, 1 };
            case RIGHT -> new int[] { 2, 1 };
            case BACK -> new int[] { 3, 1 };
            case BOTTOM -> new int[] { 1, 2 };
        };
    }

    /** The face being edited in the Ports pop-up. */
    private com.human.common.gameplay.machine.MachinePorts.Face selectedFace = com.human.common.gameplay.machine.MachinePorts.Face.TOP;

    private final ElectricMachineMenu menu;

    private final Font font;

    /** The column's top-left, relative to the screen; set every frame from the GUI's position. */
    private int columnX;

    private int columnY;

    private Field open;

    public MachineSideControls(ElectricMachineMenu menu, Font font) {
        this.menu = menu;
        this.font = font;
    }

    /** The column sits just left of the side panel. */
    public void layout(int leftPos, int topPos, int panelX, int panelY) {
        columnX = leftPos + panelX - BUTTON - GAP;
        columnY = topPos + panelY + PAD;
    }

    private List<Field> fields() {
        var fields = new ArrayList<Field>();
        fields.add(Field.PRIORITY);
        fields.add(Field.FILTER);

        // [stated] Oct 2: the redstone option only appears with a redstone card fitted.
        if (menu.hasRedstoneCard()) {
            fields.add(Field.REDSTONE);
        }

        if (menu.getKind().hasDiscardSwitches()) {
            fields.add(Field.OUTPUTS);
        }

        fields.add(Field.PORTS);

        // A card taken out while its pop-up is open closes the pop-up.
        if (open != null && !fields.contains(open)) {
            open = null;
        }

        return fields;
    }

    private int buttonY(int index) {
        return columnY + index * (BUTTON + GAP);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------------------------

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        var fields = fields();

        for (int index = 0; index < fields.size(); index++) {
            var x = columnX;
            var y = buttonY(index);
            var field = fields.get(index);
            var hot = field == open || inside(mouseX, mouseY, x, y, BUTTON, BUTTON);

            raised(graphics, x, y, BUTTON, BUTTON, hot);
            drawIcon(graphics, field, x + 1, y + 1);
        }

        if (open != null) {
            var index = fields.indexOf(open);
            var box = popupBox(open, buttonY(index));

            graphics.pose().pushPose();
            // Above the slots and their items.
            graphics.pose().translate(0.0F, 0.0F, 400.0F);
            raised(graphics, box[0], box[1], box[2], box[3], false);
            drawPopup(graphics, open, box, mouseX, mouseY);
            graphics.pose().popPose();
        }
    }

    private void drawIcon(GuiGraphics graphics, Field field, int x, int y) {
        switch (field) {
            case PRIORITY -> drawBars(graphics, x, y, menu.getPriority());
            case FILTER -> graphics.blit(FILTER_SYMBOL, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
            case REDSTONE -> graphics.renderItem(redstoneIcon(menu.getRedstoneMode()), x, y);
            case OUTPUTS -> graphics.renderItem(new ItemStack(Items.HOPPER), x, y);
            case PORTS -> graphics.renderItem(new ItemStack(com.human.common.registry.init.item.HumanBlockItems.ITEM_PIPE.get()), x, y);
        }
    }

    /** The priority icon: one to three bars, coloured by priority - the panel button it replaces, unchanged. */
    private static void drawBars(GuiGraphics graphics, int x, int y, MachinePriority priority) {
        var bars = switch (priority) {
            case CRITICAL -> 3;
            case DEFAULT -> 2;
            case DEFERRABLE -> 1;
        };
        var colour = switch (priority) {
            case CRITICAL -> 0xFFD04040;
            case DEFAULT -> 0xFFE0C040;
            case DEFERRABLE -> 0xFF60B060;
        };

        for (int bar = 0; bar < 3; bar++) {
            var height = 4 + bar * 3;
            var barX = x + 3 + bar * 4;
            var bottom = y + 14;

            graphics.fill(barX, bottom - height, barX + 3, bottom, bar < bars ? colour : 0xFF6A6A6A);
        }
    }

    private static ItemStack redstoneIcon(MachineUpgrades.RedstoneMode mode) {
        return new ItemStack(
            switch (mode) {
                case IGNORE -> Items.GUNPOWDER;
                case HIGH -> Items.REDSTONE_TORCH;
                case LOW -> Items.LEVER;
            }
        );
    }

    /** The filter hint never runs to more lines than this; the box is sized for exactly what it draws. */
    private static final int FILTER_HINT_MAX_LINES = 3;

    private int filterHintLines() {
        var lines = font.split(Component.translatable("gui.avp_human.machine.filter.hint"), ElectricMachineBlockEntity.FILTER_SIZE * ROW)
            .size();
        return Math.max(1, Math.min(FILTER_HINT_MAX_LINES, lines));
    }

    /** {x, y, width, height} of a field's pop-up, opening to the right of its button. */
    private int[] popupBox(Field field, int buttonY) {
        var x = columnX + BUTTON + GAP;

        return switch (field) {
            // ⚠ Oct 23 - the hint under the slots WRAPS inside the box now ("Left: set Right: clear" is 108 px over a
            // 72 px row of slots), so the box grows by however many lines it takes. One line: the old height exactly.
            case FILTER -> new int[] {
                x,
                buttonY,
                PAD * 2 + ElectricMachineBlockEntity.FILTER_SIZE * ROW,
                PAD * 2 + ROW + 3 + filterHintLines() * GuiText.LINE_HEIGHT };
            case PORTS -> new int[] {
                x,
                buttonY,
                PAD * 2 + Math.max(4 * ROW, modeRowWidth()),
                PAD * 2 + 3 * ROW + 4 + (hasTank() ? 2 : 1) * 14 };
            default -> {
                var rows = rows(field);
                var width = 0;

                for (var row : rows) {
                    width = Math.max(width, font.width(row.label()));
                }

                yield new int[] { x, buttonY, PAD * 2 + 20 + width + 4, PAD * 2 + rows.size() * ROW };
            }
        };
    }

    private record Row(
        Component label,
        boolean selected,
        int buttonId,
        ItemStack icon,
        MachinePriority bars
    ) {}

    private List<Row> rows(Field field) {
        var rows = new ArrayList<Row>();

        switch (field) {
            case PRIORITY -> {
                for (var priority : MachinePriority.values()) {
                    rows.add(
                        new Row(
                            Component.translatable("gui.avp_human.machine.priority." + priority.id()),
                            menu.getPriority() == priority,
                            ElectricMachineMenu.BUTTON_SET_PRIORITY + priority.ordinal(),
                            ItemStack.EMPTY,
                            priority
                        )
                    );
                }
            }
            case REDSTONE -> {
                for (var mode : MachineUpgrades.RedstoneMode.values()) {
                    rows.add(
                        new Row(
                            Component.translatable("gui.avp_human.machine.redstone." + mode.name().toLowerCase(java.util.Locale.ROOT)),
                            menu.getRedstoneMode() == mode,
                            ElectricMachineMenu.BUTTON_SET_REDSTONE + mode.ordinal(),
                            redstoneIcon(mode),
                            null
                        )
                    );
                }
            }
            case OUTPUTS -> {
                rows.add(
                    new Row(
                        Component.translatable("gui.avp_human.machine.keep_items"),
                        menu.keepsItemOutput(),
                        ElectricMachineMenu.BUTTON_TOGGLE_KEEP_ITEMS,
                        new ItemStack(Items.COBBLESTONE),
                        null
                    )
                );
                rows.add(
                    new Row(
                        Component.translatable("gui.avp_human.machine.keep_fluid"),
                        menu.keepsFluidOutput(),
                        ElectricMachineMenu.BUTTON_TOGGLE_KEEP_FLUID,
                        new ItemStack(Items.WATER_BUCKET),
                        null
                    )
                );
            }
            default -> {}
        }

        return rows;
    }

    private boolean hasTank() {
        return menu.getKind().tank() != null;
    }

    private static final com.human.common.gameplay.machine.MachinePorts.Mode[] ITEM_MODES =
        com.human.common.gameplay.machine.MachinePorts.Mode.values();

    /** A tank fills or drains in one direction only, so its sides offer that direction or off. */
    private com.human.common.gameplay.machine.MachinePorts.Mode[] fluidModes() {
        var tank = menu.getKind().tank();
        return new com.human.common.gameplay.machine.MachinePorts.Mode[] {
            tank != null && tank.output()
                ? com.human.common.gameplay.machine.MachinePorts.Mode.OUT
                : com.human.common.gameplay.machine.MachinePorts.Mode.IN,
            com.human.common.gameplay.machine.MachinePorts.Mode.OFF };
    }

    private int modeRowWidth() {
        var width = font.width(Component.translatable("gui.avp_human.machine.port.fluids")) + 4;

        for (var mode : ITEM_MODES) {
            width += font.width(modeLabel(mode)) + 6 + 2;
        }

        return width;
    }

    private static Component modeLabel(com.human.common.gameplay.machine.MachinePorts.Mode mode) {
        return Component.translatable("gui.avp_human.machine.port.mode." + mode.name().toLowerCase(java.util.Locale.ROOT));
    }

    /** The mode buttons of one row: {x, width} per mode, after the row's label. */
    private int[][] modeButtons(int rowX, com.human.common.gameplay.machine.MachinePorts.Mode[] modes) {
        var x = rowX + font.width(Component.translatable("gui.avp_human.machine.port.fluids")) + 4;
        var out = new int[modes.length][];

        for (int index = 0; index < modes.length; index++) {
            var width = font.width(modeLabel(modes[index])) + 6;
            out[index] = new int[] { x, width };
            x += width + 2;
        }

        return out;
    }

    private void drawPorts(GuiGraphics graphics, int[] box, int mouseX, int mouseY) {
        var mapX = box[0] + PAD;
        var mapY = box[1] + PAD;

        for (var face : com.human.common.gameplay.machine.MachinePorts.Face.values()) {
            var c = cell(face);
            var cx = mapX + c[0] * ROW;
            var cy = mapY + c[1] * ROW;

            well(graphics, cx, cy);
            graphics.fill(cx + 1, cy + 1, cx + 17, cy + 17, portColour(menu.getItemPort(face)));

            // A machine with a tank: the fluid setting as a corner marker.
            if (hasTank()) {
                graphics.fill(cx + 11, cy + 11, cx + 17, cy + 17, 0xFF000000);
                graphics.fill(cx + 12, cy + 12, cx + 17, cy + 17, portColour(menu.getFluidPort(face)));
            }

            // The face's initial, so the map reads without a tooltip.
            var name = Component.translatable("gui.avp_human.machine.port.face." + face.name().toLowerCase(java.util.Locale.ROOT))
                .getString();
            graphics.drawString(font, name.isEmpty() ? "?" : name.substring(0, 1), cx + 3, cy + 3, 0xFFFFFFFF, true);

            if (face == selectedFace) {
                graphics.renderOutline(cx, cy, ROW, ROW, 0xFFFFFFFF);
            } else if (inside(mouseX, mouseY, cx, cy, ROW, ROW)) {
                graphics.renderOutline(cx, cy, ROW, ROW, 0xFFDDDDDD);
            }
        }

        var rowY = mapY + 3 * ROW + 4;
        drawModeRow(
            graphics,
            box[0] + PAD,
            rowY,
            Component.translatable("gui.avp_human.machine.port.items"),
            ITEM_MODES,
            menu.getItemPort(selectedFace),
            mouseX,
            mouseY
        );

        if (hasTank()) {
            drawModeRow(
                graphics,
                box[0] + PAD,
                rowY + 14,
                Component.translatable("gui.avp_human.machine.port.fluids"),
                fluidModes(),
                menu.getFluidPort(selectedFace),
                mouseX,
                mouseY
            );
        }
    }

    private void drawModeRow(
        GuiGraphics graphics,
        int x,
        int y,
        Component label,
        com.human.common.gameplay.machine.MachinePorts.Mode[] modes,
        com.human.common.gameplay.machine.MachinePorts.Mode current,
        int mouseX,
        int mouseY
    ) {
        graphics.drawString(font, label, x, y + 2, TEXT, false);
        var buttons = modeButtons(x, modes);

        for (int index = 0; index < modes.length; index++) {
            var bx = buttons[index][0];
            var bw = buttons[index][1];
            var hot = inside(mouseX, mouseY, bx, y, bw, 12);

            graphics.fill(bx, y, bx + bw, y + 12, modes[index] == current ? portColour(modes[index]) : hot ? 0xFFB8B8B8 : 0xFF9A9A9A);
            graphics.renderOutline(bx, y, bw, 12, modes[index] == current ? 0xFFFFFFFF : 0xFF555555);
            graphics.drawString(font, modeLabel(modes[index]), bx + 3, y + 2, 0xFFFFFFFF, true);
        }
    }

    private void drawPopup(GuiGraphics graphics, Field field, int[] box, int mouseX, int mouseY) {
        if (field == Field.PORTS) {
            drawPorts(graphics, box, mouseX, mouseY);
            return;
        }

        if (field == Field.FILTER) {
            for (int entry = 0; entry < ElectricMachineBlockEntity.FILTER_SIZE; entry++) {
                var cx = box[0] + PAD + entry * ROW;
                var cy = box[1] + PAD;

                well(graphics, cx, cy);
                var item = menu.getFilterEntry(entry);

                if (item.isEmpty()) {
                    graphics.blit(FILTER_SYMBOL, cx + 1, cy + 1, 0.0F, 0.0F, 16, 16, 16, 16);
                } else {
                    graphics.renderItem(item, cx + 1, cy + 1);
                }

                if (inside(mouseX, mouseY, cx, cy, ROW, ROW)) {
                    graphics.fill(cx + 1, cy + 1, cx + 17, cy + 17, 0x80FFFFFF);
                }
            }

            GuiText.drawWrapped(
                graphics,
                font,
                Component.translatable("gui.avp_human.machine.filter.hint"),
                box[0] + PAD,
                box[1] + PAD + ROW + 2,
                ElectricMachineBlockEntity.FILTER_SIZE * ROW,
                FILTER_HINT_MAX_LINES,
                TEXT,
                false
            );
            return;
        }

        var rows = rows(field);

        for (int index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            var rx = box[0] + PAD;
            var ry = box[1] + PAD + index * ROW;

            if (row.selected()) {
                graphics.fill(rx - 1, ry, box[0] + box[2] - PAD + 1, ry + ROW - 1, 0xFF9DB3C8);
            } else if (inside(mouseX, mouseY, rx, ry, box[2] - PAD * 2, ROW)) {
                graphics.fill(rx - 1, ry, box[0] + box[2] - PAD + 1, ry + ROW - 1, 0xFFB8B8B8);
            }

            if (row.bars() != null) {
                drawBars(graphics, rx, ry + 1, row.bars());
            } else if (!row.icon().isEmpty()) {
                graphics.renderItem(row.icon(), rx, ry + 1);
            }

            // A switch row (the press outputs) shows whether it is on; a choice row shows which one is chosen.
            var label = field == Field.OUTPUTS
                ? row.label()
                    .copy()
                    .append(
                        Component.literal(row.selected() ? "  ✔" : "  ✘")
                            .withStyle(row.selected() ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED)
                    )
                : row.label();

            graphics.drawString(font, label, rx + 20, ry + 5, TEXT, false);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    /** @return true when the click was the controls' - the screen must then not handle it. */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var fields = fields();

        if (open != null) {
            var box = popupBox(open, buttonY(fields.indexOf(open)));

            if (inside(mouseX, mouseY, box[0], box[1], box[2], box[3])) {
                clickPopup(open, box, mouseX, mouseY, button);
                return true;
            }

            // Modal: a click anywhere else only closes the pop-up - unless it is another side button, which switches.
            var clicked = buttonAt(fields, mouseX, mouseY);
            open = clicked != null && clicked != open ? clicked : null;
            return true;
        }

        var clicked = buttonAt(fields, mouseX, mouseY);

        if (clicked != null) {
            open = clicked;
            click();
            return true;
        }

        return false;
    }

    private Field buttonAt(List<Field> fields, double mouseX, double mouseY) {
        for (int index = 0; index < fields.size(); index++) {
            if (inside(mouseX, mouseY, columnX, buttonY(index), BUTTON, BUTTON)) {
                return fields.get(index);
            }
        }

        return null;
    }

    private void clickPopup(Field field, int[] box, double mouseX, double mouseY, int button) {
        if (field == Field.PORTS) {
            var mapX = box[0] + PAD;
            var mapY = box[1] + PAD;

            for (var face : com.human.common.gameplay.machine.MachinePorts.Face.values()) {
                var c = cell(face);

                if (inside(mouseX, mouseY, mapX + c[0] * ROW, mapY + c[1] * ROW, ROW, ROW)) {
                    selectedFace = face;
                    click();
                    return;
                }
            }

            var rowY = mapY + 3 * ROW + 4;
            clickModeRow(mouseX, mouseY, box[0] + PAD, rowY, ITEM_MODES, false);

            if (hasTank()) {
                clickModeRow(mouseX, mouseY, box[0] + PAD, rowY + 14, fluidModes(), true);
            }

            return;
        }

        if (field == Field.FILTER) {
            for (int entry = 0; entry < ElectricMachineBlockEntity.FILTER_SIZE; entry++) {
                if (inside(mouseX, mouseY, box[0] + PAD + entry * ROW, box[1] + PAD, ROW, ROW)) {
                    // Left click: what is held (or nothing, which clears). Right click: clear.
                    send(button == 1 ? ElectricMachineMenu.BUTTON_CLEAR_FILTER + entry : ElectricMachineMenu.BUTTON_SET_FILTER + entry);
                    return;
                }
            }

            return;
        }

        var rows = rows(field);
        var index = (int) ((mouseY - box[1] - PAD) / ROW);

        if (index >= 0 && index < rows.size()) {
            send(rows.get(index).buttonId());

            // A choice closes the pop-up; a switch (the press outputs) stays open so both can be set.
            if (field != Field.OUTPUTS) {
                open = null;
            }
        }
    }

    private void clickModeRow(
        double mouseX,
        double mouseY,
        int x,
        int y,
        com.human.common.gameplay.machine.MachinePorts.Mode[] modes,
        boolean fluid
    ) {
        var buttons = modeButtons(x, modes);

        for (int index = 0; index < modes.length; index++) {
            if (inside(mouseX, mouseY, buttons[index][0], y, buttons[index][1], 12)) {
                send(ElectricMachineMenu.portButton(selectedFace, fluid, modes[index]));
                return;
            }
        }
    }

    /** Esc closes an open pop-up instead of the whole screen. */
    public boolean keyPressed(int keyCode) {
        if (open != null && keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            open = null;
            return true;
        }

        return false;
    }

    /** True over a button or the open pop-up - the screen hides slot tooltips there and keeps items from dropping. */
    public boolean isMouseOver(double mouseX, double mouseY) {
        var fields = fields();

        if (open != null) {
            var box = popupBox(open, buttonY(fields.indexOf(open)));

            if (inside(mouseX, mouseY, box[0], box[1], box[2], box[3])) {
                return true;
            }
        }

        return inside(mouseX, mouseY, columnX, columnY, BUTTON, fields.size() * (BUTTON + GAP));
    }

    /** True while a pop-up is open - the screen then treats every click as the controls'. */
    public boolean isOpen() {
        return open != null;
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        var fields = fields();

        if (open == Field.FILTER) {
            var box = popupBox(open, buttonY(fields.indexOf(open)));

            if (inside(mouseX, mouseY, box[0], box[1], box[2], box[3])) {
                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    List.of(
                        Component.translatable("gui.avp_human.machine.filter"),
                        Component.translatable("gui.avp_human.machine.filter.desc").withStyle(ChatFormatting.GRAY)
                    ),
                    mouseX,
                    mouseY
                );
                return;
            }
        }

        if (open == Field.PORTS) {
            var box = popupBox(open, buttonY(fields.indexOf(open)));

            for (var face : com.human.common.gameplay.machine.MachinePorts.Face.values()) {
                var c = cell(face);

                if (inside(mouseX, mouseY, box[0] + PAD + c[0] * ROW, box[1] + PAD + c[1] * ROW, ROW, ROW)) {
                    var lines = new ArrayList<Component>();
                    lines.add(Component.translatable("gui.avp_human.machine.port.face." + face.name().toLowerCase(java.util.Locale.ROOT)));
                    lines.add(
                        Component.translatable("gui.avp_human.machine.port.items")
                            .append(": ")
                            .append(modeLabel(menu.getItemPort(face)))
                            .withStyle(ChatFormatting.GRAY)
                    );

                    if (hasTank()) {
                        lines.add(
                            Component.translatable("gui.avp_human.machine.port.fluids")
                                .append(": ")
                                .append(modeLabel(menu.getFluidPort(face)))
                                .withStyle(ChatFormatting.GRAY)
                        );
                    }

                    com.human.client.screen.widget.GuiText.tooltip(graphics, font, lines, mouseX, mouseY);
                    return;
                }
            }
        }

        var field = buttonAt(fields, mouseX, mouseY);

        if (field != null && open == null) {
            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                List.of(
                    Component.translatable("gui.avp_human.machine.side." + field.name().toLowerCase(java.util.Locale.ROOT)),
                    Component.translatable("gui.avp_human.machine.side.click").withStyle(ChatFormatting.DARK_GRAY)
                ),
                mouseX,
                mouseY
            );
        }
    }

    private void send(int buttonId) {
        var minecraft = Minecraft.getInstance();

        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
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

    // ------------------------------------------------------------------------------------------------------------
    // Vanilla-style shapes
    // ------------------------------------------------------------------------------------------------------------

    private static void raised(GuiGraphics graphics, int x, int y, int width, int height, boolean hot) {
        graphics.fill(x, y, x + width, y + height, OUTLINE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, hot ? 0xFFDADADA : BG);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, LIGHT);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, LIGHT);
        graphics.fill(x + 2, y + height - 2, x + width - 1, y + height - 1, SHADOW);
        graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 1, SHADOW);
    }

    private static void well(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + ROW, y + ROW, LIGHT);
        graphics.fill(x, y, x + ROW - 1, y + ROW - 1, WELL_DARK);
        graphics.fill(x + 1, y + 1, x + ROW - 1, y + ROW - 1, WELL_FILL);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
