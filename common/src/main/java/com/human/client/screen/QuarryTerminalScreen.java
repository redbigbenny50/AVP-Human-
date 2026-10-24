package com.human.client.screen;

import com.human.common.gameplay.quarry.QuarrySettings;
import com.human.common.gameplay.quarry.QuarryTerminalBlockEntity;
import com.human.common.gameplay.quarry.QuarryTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Quarry-Com, the quarry terminal's screen ([stated] Oct 15), drawn in the CRYOLINK style: black panels, green frames,
 * amber for warnings. Laid out from the approved mockup at one third of its size.
 * <ul>
 * <li>HEADER on every tab: the title, START / STOP for mining, the edition's brand.</li>
 * <li>WORK AREA: the chunk map and depth - arrives with the frame build (phase 2b).</li>
 * <li>SETTINGS: a scrolling list, each option with its choices and a line saying what the chosen one does; SAVE / LOAD
 * keep a copy on this computer ([stated] Oct 15: like the sentry favourites, so it works on every world); the REFILL
 * HOLE panel (greyed while refill is off) and CONNECTIONS.</li>
 * <li>PROGRESS: status and readouts, the four upgrade slots and the inventory.</li>
 * <li>STORAGE: the 128 slots (8 wide, scrolling), the frame slots lined up with the inventory, and the tank.</li>
 * </ul>
 */
public class QuarryTerminalScreen extends AbstractContainerScreen<QuarryTerminalMenu> {

    private static final int BG = 0xFF051008;

    private static final int PANEL = 0xFF09190F;

    private static final int WELL = 0xFF0E2816;

    private static final int GREEN = 0xFF6EF58C;

    private static final int MUTED = 0xFF33974E;

    private static final int DIM = 0xFF16351F;

    private static final int WARN = 0xFFF0BE5A;

    private static final int TEXT_DARK = 0xFF051008;

    private static final String[] TAB_KEYS = { "work_area", "settings", "progress", "storage" };

    private static final int SETTINGS_TOP = 60;

    private static final int SETTINGS_HEIGHT = 146;

    private static final int SETTING_ROW = 36;

    /**
     * Header START / STOP: START is 30 px of text, STOP 24; STOP ends at x 196, two pixels clear of "USCM ENGINEERING"
     * (x 198).
     */
    private static final int START_X = 132;

    private static final int START_W = 34;

    private static final int STOP_X = 168;

    private static final int STOP_W = 28;

    /** The BOTTOM Y stepper's buttons, clear of its label. */
    private static final int BOTTOM_MINUS_X = 219;

    private static final int BOTTOM_PLUS_X = 268;

    private static final String SAVE_FILE = "avp_human_quarry_settings.properties";

    private int settingsScroll;

    private Component saveNote = Component.empty();

    public QuarryTerminalScreen(QuarryTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = QuarryTerminalMenu.GUI_WIDTH;
        imageHeight = QuarryTerminalMenu.GUI_HEIGHT;
        menu.setTab(QuarryTerminalMenu.TAB_PROGRESS);
    }

    private static Component t(String key, Object... args) {
        return Component.translatable("gui.avp_human.quarry." + key, args);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // What the empty enchantment slot takes.
        if (
            menu.tab() == QuarryTerminalMenu.TAB_PROGRESS && !menu.getSlot(QuarryTerminalMenu.FIRST_ENCHANT).hasItem()
                && inside(mouseX, mouseY, leftPos + QuarryTerminalMenu.ENCHANTS_X - 1, topPos + QuarryTerminalMenu.ENCHANTS_Y - 1, 18, 18)
        ) {
            com.human.client.screen.widget.GuiText.tooltip(graphics, font, t("enchant.takes"), mouseX, mouseY);
        }

        // What the speed cards do, over the empty upgrade wells (the note used to sit where the enchantment slot is).
        if (
            menu.tab() == QuarryTerminalMenu.TAB_PROGRESS && menu.getCarried().isEmpty()
                && inside(mouseX, mouseY, leftPos + QuarryTerminalMenu.UPGRADES_X - 1, topPos + QuarryTerminalMenu.UPGRADES_Y - 1, 36, 36)
                && (hoveredSlot == null || !hoveredSlot.hasItem())
        ) {
            com.human.client.screen.widget.GuiText.tooltip(graphics, font, t("upgrades.note"), mouseX, mouseY);
        }

        // Why a map cell is what it is.
        if (menu.tab() == QuarryTerminalMenu.TAB_WORK_AREA) {
            var size = QuarryTerminalBlockEntity.MAP_SIZE;
            var column = Math.floorDiv(mouseX - leftPos - MAP_X, CELL);
            var row = Math.floorDiv(mouseY - topPos - MAP_Y, CELL);

            if (row >= 0 && row < size && column >= 0 && column < size) {
                var code = menu.data(QuarryTerminalMenu.DATA_MAP + row * size + column);

                graphics.renderTooltip(
                    font,
                    font.split(t("cell." + CELL_KEYS[Math.clamp(code, 0, CELL_KEYS.length - 1)]), 160),
                    mouseX,
                    mouseY
                );
            }
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        var x = leftPos;
        var y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, BG);
        header(graphics, x, y, mouseX, mouseY);

        switch (menu.tab()) {
            case QuarryTerminalMenu.TAB_WORK_AREA -> workArea(graphics, x, y, mouseX, mouseY);
            case QuarryTerminalMenu.TAB_SETTINGS -> settings(graphics, x, y, mouseX, mouseY);
            case QuarryTerminalMenu.TAB_STORAGE -> storage(graphics, x, y);
            default -> progress(graphics, x, y);
        }
    }

    private void header(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(x + 8, y + 4, 0);
        // 1.5x, not 2x: measured with the real font, "/// Quarry-Com" at 2x is 164 px and ran under START (x 134).
        graphics.pose().scale(1.5F, 1.5F, 1.0F);
        graphics.drawString(font, t("title"), 0, 0, GREEN, false);
        graphics.pose().popPose();
        graphics.drawString(font, t("subtitle"), x + 8, y + 22, MUTED, false);

        var running = menu.data(QuarryTerminalMenu.DATA_RUNNING) == 1;

        // Sized to their labels at FULL size ([stated] Oct 16: same font and size everywhere, no shrunk text).
        button(graphics, x + START_X, y + 10, START_W, 11, t("start"), running, mouseX, mouseY);
        button(graphics, x + STOP_X, y + 10, STOP_W, 11, t("stop"), !running, mouseX, mouseY);

        var brand = t("brand." + menu.edition().textureKey());
        var name = t("terminal");

        graphics.drawString(font, brand, x + imageWidth - 8 - font.width(brand), y + 5, GREEN, false);
        graphics.drawString(font, name, x + imageWidth - 8 - font.width(name), y + 15, MUTED, false);

        for (int tab = 0; tab < TAB_KEYS.length; tab++) {
            button(graphics, x + 8 + tab * 70, y + 32, 66, 12, t("tab." + TAB_KEYS[tab]), menu.tab() == tab, mouseX, mouseY);
        }
    }

    // WORK AREA layout: the 7 x 7 map (the terminal on the bottom row, the chunk it faces above it).
    private static final int MAP_X = 20;

    private static final int MAP_Y = 63;

    private static final int CELL = 18;

    private static final String[] CELL_KEYS = {
        "available",
        "selected",
        "out_of_range",
        "claimed",
        "xeno",
        "near_quarry",
        "other_quarry",
        "terminal",
        "behind" };

    private void workArea(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        var size = QuarryTerminalBlockEntity.MAP_SIZE;

        panel(graphics, x + 8, y + 48, 152, 180, t("site_map"));

        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                var code = menu.data(QuarryTerminalMenu.DATA_MAP + row * size + column);
                var cx = x + MAP_X + column * CELL;
                var cy = y + MAP_Y + row * CELL;

                cell(graphics, cx, cy, code, inside(mouseX, mouseY, cx, cy, CELL, CELL));
            }
        }

        // Legend, two columns.
        legend(graphics, x + 14, y + 192, "#", GREEN, true, t("legend.selected"));
        legend(graphics, x + 84, y + 192, " ", MUTED, false, t("legend.available"));
        legend(graphics, x + 14, y + 201, "X", WARN, false, t("legend.claimed"));
        legend(graphics, x + 84, y + 201, "!", WARN, false, t("legend.xeno"));
        legend(graphics, x + 14, y + 210, "Q", WARN, false, t("legend.quarry"));
        legend(graphics, x + 84, y + 210, ".", DIM, false, t("legend.out_of_range"));
        legend(graphics, x + 14, y + 219, "T", GREEN, false, t("legend.terminal"));

        // AREA
        var mask = menu.data(QuarryTerminalMenu.DATA_SELECTION);
        var shape = com.human.common.gameplay.quarry.QuarryArea.shape(mask);
        var needed = menu.data(QuarryTerminalMenu.DATA_NEEDED);
        var held = menu.data(QuarryTerminalMenu.DATA_FRAMES);
        var confirmed = menu.data(QuarryTerminalMenu.DATA_CONFIRMED) == 1;
        var frameState = QuarryTerminalBlockEntity.FrameState.values()[Math.clamp(menu.data(QuarryTerminalMenu.DATA_FRAME_STATE), 0, 3)];

        panel(graphics, x + 164, y + 48, 124, 100, t("tab.work_area"));
        row(
            graphics,
            x + 169,
            y + 63,
            114,
            t("size"),
            shape.width() == 0 ? Component.literal("-") : t("size.value", shape.width(), shape.depth()),
            GREEN
        );
        row(
            graphics,
            x + 169,
            y + 74,
            114,
            t("footprint"),
            shape.width() == 0 ? Component.literal("-") : Component.literal(shape.width() * 16 + " x " + shape.depth() * 16),
            GREEN
        );
        row(graphics, x + 169, y + 85, 114, t("frames_needed"), Component.literal(String.valueOf(needed)), GREEN);
        row(
            graphics,
            x + 169,
            y + 96,
            114,
            t("frames_in_place"),
            Component.literal(String.valueOf(menu.data(QuarryTerminalMenu.DATA_IN_PLACE))),
            MUTED
        );
        row(graphics, x + 169, y + 107, 114, t("frames_held_short"), Component.literal(String.valueOf(held)), held < needed ? WARN : GREEN);

        Component line;
        var lineColour = GREEN;

        if (!confirmed) {
            var problem = QuarryTerminalBlockEntity.PROBLEMS[Math.clamp(
                menu.data(QuarryTerminalMenu.DATA_PROBLEM),
                0,
                QuarryTerminalBlockEntity.PROBLEMS.length - 1
            )];

            line = t("problem." + problem);
            lineColour = problem.equals("ready") ? GREEN : WARN;
        } else if (menu.data(QuarryTerminalMenu.DATA_BLOCKED) > 0 && frameState == QuarryTerminalBlockEntity.FrameState.BUILDING) {
            line = t("frame.blocked", menu.data(QuarryTerminalMenu.DATA_BLOCKED));
            lineColour = WARN;
        } else {
            line = t("frame." + frameState.name().toLowerCase(java.util.Locale.ROOT));
        }

        graphics.drawWordWrap(font, line, x + 169, y + 119, 114, lineColour);

        // DEPTH
        panel(graphics, x + 164, y + 152, 124, 76, t("depth"));

        var start = menu.data(QuarryTerminalMenu.DATA_START_Y);
        var bottom = menu.data(QuarryTerminalMenu.DATA_BOTTOM_Y);

        row(graphics, x + 169, y + 166, 114, t("starting_y"), Component.literal(String.valueOf(start)), GREEN);
        graphics.drawString(font, t("bottom_y"), x + 169, y + 178, GREEN, false);
        // Oct 16 - moved right: "BOTTOM Y" is 46 px in Minecraft's font and ran under the minus button at x 210.
        button(graphics, x + BOTTOM_MINUS_X, y + 177, 13, 10, Component.literal("-"), false, mouseX, mouseY);
        centered(graphics, String.valueOf(bottom), x + BOTTOM_MINUS_X + 28, y + 178, GREEN);
        button(graphics, x + BOTTOM_PLUS_X, y + 177, 13, 10, Component.literal("+"), false, mouseX, mouseY);
        row(graphics, x + 169, y + 190, 114, t("layers"), Component.literal(String.valueOf(Math.max(0, start - bottom + 1))), MUTED);

        var confirmLabel = !confirmed ? t("confirm") : t("edit");
        var canEdit = !confirmed || frameState == QuarryTerminalBlockEntity.FrameState.NONE;

        button(graphics, x + 169, y + 202, 56, 11, confirmLabel, !confirmed && shape.valid(), canEdit ? mouseX : -1, mouseY);
        button(
            graphics,
            x + 228,
            y + 202,
            56,
            11,
            t("retract"),
            false,
            frameState != QuarryTerminalBlockEntity.FrameState.NONE ? mouseX : -1,
            mouseY
        );
        graphics.drawString(font, t("bottom_y.shift"), x + 169, y + 216, DIM, false);
    }

    private void cell(GuiGraphics graphics, int cx, int cy, int code, boolean hover) {
        var border = switch (code) {
            case QuarryTerminalBlockEntity.CELL_SELECTED, QuarryTerminalBlockEntity.CELL_TERMINAL -> GREEN;
            case QuarryTerminalBlockEntity.CELL_CLAIMED, QuarryTerminalBlockEntity.CELL_XENO, QuarryTerminalBlockEntity.CELL_NEAR_QUARRY,
                QuarryTerminalBlockEntity.CELL_OTHER_QUARRY -> WARN;
            case QuarryTerminalBlockEntity.CELL_AVAILABLE -> MUTED;
            default -> DIM;
        };
        var fill = switch (code) {
            case QuarryTerminalBlockEntity.CELL_SELECTED -> GREEN;
            case QuarryTerminalBlockEntity.CELL_TERMINAL -> WELL;
            case QuarryTerminalBlockEntity.CELL_CLAIMED, QuarryTerminalBlockEntity.CELL_XENO, QuarryTerminalBlockEntity.CELL_NEAR_QUARRY,
                QuarryTerminalBlockEntity.CELL_OTHER_QUARRY -> 0xFF1E1806;
            case QuarryTerminalBlockEntity.CELL_AVAILABLE -> hover ? WELL : PANEL;
            default -> BG;
        };
        var mark = switch (code) {
            case QuarryTerminalBlockEntity.CELL_SELECTED -> "#";
            case QuarryTerminalBlockEntity.CELL_TERMINAL -> "T";
            case QuarryTerminalBlockEntity.CELL_CLAIMED -> "X";
            case QuarryTerminalBlockEntity.CELL_XENO -> "!";
            case QuarryTerminalBlockEntity.CELL_NEAR_QUARRY, QuarryTerminalBlockEntity.CELL_OTHER_QUARRY -> "Q";
            case QuarryTerminalBlockEntity.CELL_AVAILABLE -> "";
            default -> ".";
        };

        graphics.fill(cx, cy, cx + CELL, cy + CELL, border);
        graphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, fill);
        centered(graphics, mark, cx + CELL / 2, cy + 5, code == QuarryTerminalBlockEntity.CELL_SELECTED ? TEXT_DARK : border);
    }

    private void legend(GuiGraphics graphics, int x, int y, String mark, int colour, boolean filled, Component label) {
        graphics.fill(x, y, x + 8, y + 8, colour);
        graphics.fill(x + 1, y + 1, x + 7, y + 7, filled ? colour : BG);
        centered(graphics, mark, x + 4, y, filled ? TEXT_DARK : colour);
        graphics.drawString(font, label, x + 11, y, colour == DIM ? MUTED : colour, false);
    }

    private void settings(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        panel(graphics, x + 8, y + 48, 160, 180, t("operation"));

        // Row heights follow the chosen descriptions, so the list can get shorter when a choice changes.
        settingsScroll = Math.clamp(settingsScroll, 0, maxSettingsScroll());

        graphics.enableScissor(x + 9, y + SETTINGS_TOP, x + 167, y + SETTINGS_TOP + SETTINGS_HEIGHT);

        var options = QuarrySettings.Option.values();

        var rowTop = 0;

        for (int index = 0; index < options.length; index++) {
            var option = options[index];
            var rowY = y + SETTINGS_TOP + 2 + rowTop - settingsScroll;
            var chosen = menu.setting(option);

            rowTop += settingRowHeight(option);

            graphics.drawString(font, t("setting." + option.key()), x + 12, rowY, GREEN, false);

            var width = (146 - (option.choices() - 1) * 3) / option.choices();

            for (int choice = 0; choice < option.choices(); choice++) {
                button(
                    graphics,
                    x + 12 + choice * (width + 3),
                    rowY + 10,
                    width,
                    10,
                    t("setting." + option.key() + "." + choice),
                    chosen == choice,
                    mouseX,
                    mouseY
                );
            }

            graphics.drawWordWrap(font, t("setting." + option.key() + "." + chosen + ".desc"), x + 12, rowY + 22, 146, MUTED);
        }

        graphics.disableScissor();

        // Scroll bar.
        var track = SETTINGS_HEIGHT - 2;
        var thumb = Math.max(12, track * SETTINGS_HEIGHT / settingsHeight());
        var thumbY = maxSettingsScroll() == 0 ? 0 : (track - thumb) * settingsScroll / maxSettingsScroll();

        graphics.fill(x + 160, y + SETTINGS_TOP, x + 166, y + SETTINGS_TOP + SETTINGS_HEIGHT, WELL);
        graphics.fill(x + 161, y + SETTINGS_TOP + 1 + thumbY, x + 165, y + SETTINGS_TOP + 1 + thumbY + thumb, GREEN);

        button(graphics, x + 12, y + 212, 72, 11, t("save"), false, mouseX, mouseY);
        button(graphics, x + 88, y + 212, 72, 11, t("load"), false, mouseX, mouseY);

        if (!saveNote.getString().isEmpty()) {
            graphics.drawString(font, saveNote, x + 12, y + 50 + 180 - 30, MUTED, false);
        }

        refillPanel(graphics, x, y, mouseX, mouseY);

        panel(graphics, x + 172, y + 172, 116, 56, t("connections"));
        row(graphics, x + 178, y + 186, 104, t("port.item"), t("port.item.where"), GREEN);
        row(graphics, x + 178, y + 198, 104, t("port.liquid"), t("port.liquid.where"), GREEN);
        row(graphics, x + 178, y + 210, 104, t("port.power"), t("port.power.where"), GREEN);
    }

    // REFILL panel layout (the approved mockup's pixels / 3).
    /** The REFILL title-bar START (34 wide) and STOP (28 wide), full-size labels, after the title "REFILL". */
    private static final int REFILL_BUTTONS_X = 220;

    private static final int REFILL_START_W = 34;

    private static final int REFILL_STOP_X = 256;

    private static final int REFILL_STOP_W = 28;

    private static final int REFILL_MINUS_X = 230;

    private static final int REFILL_PLUS_X = 269;

    private static final int REFILL_MIN_Y = 64;

    private static final int REFILL_MAX_Y = 80;

    /**
     * REFILL HOLE, as the mockup: its own START / STOP in the title bar, MIN Y and MAX Y with - and + (shift = 10),
     * FILL WITH and the 3 x 3 grid numbered 1-9 in the order it is used, and the note beside it. The whole panel is
     * drawn dim while REFILL HOLE is OFF in the settings list ([stated] Oct 15: "its panel greyed out when off").
     */
    private void refillPanel(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        var on = menu.data(QuarryTerminalMenu.DATA_REFILL_ON) == 1;
        var filling = menu.data(QuarryTerminalMenu.DATA_FILL_RUNNING) == 1;
        var text = on ? GREEN : DIM;

        panel(graphics, x + 172, y + 48, 116, 120, t("refill"));

        // START / STOP in the title bar, full size.
        smallButton(graphics, x + REFILL_BUTTONS_X, y + 49, REFILL_START_W, t("start"), on && filling, on, mouseX, mouseY);
        smallButton(graphics, x + REFILL_STOP_X, y + 49, REFILL_STOP_W, t("stop"), on && !filling, on, mouseX, mouseY);

        stepper(graphics, x, y + REFILL_MIN_Y, t("refill.min_y"), menu.data(QuarryTerminalMenu.DATA_REFILL_MIN), on, mouseX, mouseY);
        stepper(graphics, x, y + REFILL_MAX_Y, t("refill.max_y"), menu.data(QuarryTerminalMenu.DATA_REFILL_MAX), on, mouseX, mouseY);

        graphics.drawString(font, t("refill.fill_with"), x + 177, y + 97, text, false);
        wells(graphics, x + QuarryTerminalMenu.FILL_X, y + QuarryTerminalMenu.FILL_Y, 3, 3);

        // Slot numbers in each well's corner, full size: the order they are used in. A block in the slot covers it.
        for (int slot = 0; slot < QuarryTerminalBlockEntity.FILL_SLOTS; slot++) {
            graphics.drawString(
                font,
                String.valueOf(slot + 1),
                x + QuarryTerminalMenu.FILL_X + (slot % 3) * 18 + 1,
                y + QuarryTerminalMenu.FILL_Y + (slot / 3) * 18 + 1,
                on ? MUTED : DIM,
                false
            );
        }

        // The note beside the grid, full size: five short lines fit the 50 px beside it.
        graphics.drawWordWrap(font, t("refill.note"), x + 235, y + 107, 50, on ? MUTED : DIM);

        if (!on) {
            // Dim the slot wells too: the grid still takes blocks, but nothing will use them until it is switched on.
            graphics.fill(
                x + QuarryTerminalMenu.FILL_X - 1,
                y + QuarryTerminalMenu.FILL_Y - 1,
                x + QuarryTerminalMenu.FILL_X + 53,
                y + QuarryTerminalMenu.FILL_Y + 53,
                0x99051008
            );
        }
    }

    /** A title-bar button: full-size label; drawn dim and ignored while its panel is off. */
    private void smallButton(
        GuiGraphics graphics,
        int x,
        int y,
        int width,
        Component label,
        boolean lit,
        boolean enabled,
        int mouseX,
        int mouseY
    ) {
        var hover = enabled && inside(mouseX, mouseY, x, y, width, 10);
        var edge = enabled ? GREEN : DIM;

        graphics.fill(x, y, x + width, y + 10, edge);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 9, lit ? GREEN : hover ? WELL : PANEL);
        graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + 1, lit ? TEXT_DARK : edge, false);
    }

    /** "MIN Y [-] -64 [+]" - the value centred between the buttons. */
    private void stepper(GuiGraphics graphics, int x, int rowY, Component label, int value, boolean enabled, int mouseX, int mouseY) {
        var colour = enabled ? GREEN : DIM;
        var shown = Component.literal(String.valueOf(value));

        graphics.drawString(font, label, x + 177, rowY + 2, colour, false);
        button(graphics, x + REFILL_MINUS_X, rowY, 13, 11, Component.literal("-"), false, enabled ? mouseX : -1, mouseY);
        graphics.drawString(font, shown, x + 256 - font.width(shown) / 2, rowY + 2, colour, false);
        button(graphics, x + REFILL_PLUS_X, rowY, 13, 11, Component.literal("+"), false, enabled ? mouseX : -1, mouseY);
    }

    /**
     * PROGRESS - laid out as the approved Quarry-Com mockup: a two-column panel (status, chunk, layer, blocks mined,
     * blocks left, time left | area done, energy, internal storage with their bars), values in [brackets], then the
     * UPGRADES panel with the player's inventory. Coordinates are the mockup's pixels / 3. The enchantment slot (added
     * after the mockup) sits under the 2 x 2 upgrades, with the mockup's note below it.
     */
    private void progress(GuiGraphics graphics, int x, int y) {
        if (menu.data(QuarryTerminalMenu.DATA_FILL_RUNNING) == 1) {
            progressRefill(graphics, x, y);
            upgradesPanel(graphics, x, y);
            return;
        }

        panel(graphics, x + 8, y + 48, 280, 82, t("progress.mining"));

        var status = menu.status();
        var statusColour = switch (status) {
            case NO_POWER, STORAGE_FULL, NO_AREA, NEED_FRAMES, FRAME_BLOCKED, FRAME_SLOTS_FULL, WAITING, REDSTONE, TANK_FULL, NO_FILL ->
                WARN;
            default -> GREEN;
        };
        var label = t("status." + status.name().toLowerCase(java.util.Locale.ROOT));

        var chunks = menu.data(QuarryTerminalMenu.DATA_CHUNKS);
        var left = menu.data(QuarryTerminalMenu.DATA_LAYERS_LEFT);
        var total = Math.max(1, menu.data(QuarryTerminalMenu.DATA_LAYERS_TOTAL));
        var digging = chunks > 0 && left > 0;
        var none = t("time_left.none");

        // Left column: x 13..142.
        var lx = x + 13;
        var lw = 129;

        graphics.drawString(font, t("status"), lx, y + 62, GREEN, false);
        graphics.fill(lx + lw - font.width(label) - 6, y + 60, lx + lw, y + 71, statusColour);
        graphics.drawString(font, label, lx + lw - font.width(label) - 3, y + 62, TEXT_DARK, false);

        row(
            graphics,
            lx,
            y + 72,
            lw,
            t("chunk"),
            digging
                ? bracket(menu.data(QuarryTerminalMenu.DATA_CHUNK) + " OF " + chunks)
                : none,
            digging ? GREEN : MUTED
        );
        row(graphics, lx, y + 82, lw, t("layer"), digging ? layerValue(lw - font.width(t("layer")) - 4) : none, digging ? GREEN : MUTED);
        row(
            graphics,
            lx,
            y + 92,
            lw,
            t("blocks_mined"),
            bracket(grouped(menu.wide(QuarryTerminalMenu.DATA_MINED_LO, QuarryTerminalMenu.DATA_MINED_HI))),
            GREEN
        );
        row(graphics, lx, y + 102, lw, t("blocks_left"), bracket(grouped(left * 256)), digging ? GREEN : MUTED);
        // TIME LEFT is layers left x seconds a layer - the speed cards visibly change it ([stated] Oct 15).
        row(
            graphics,
            lx,
            y + 112,
            lw,
            t("time_left"),
            digging
                ? bracket("~" + duration(left * menu.data(QuarryTerminalMenu.DATA_SECONDS)))
                : none,
            digging ? GREEN : MUTED
        );

        // Right column: x 154..283.
        var rx = x + 154;
        var rw = 129;
        var done = chunks == 0 ? 0.0F : 1.0F - left / (float) total;
        var energy = menu.data(QuarryTerminalMenu.DATA_ENERGY);
        var used = menu.data(QuarryTerminalMenu.DATA_USED);

        row(graphics, rx, y + 62, rw, t("area_done"), bracket(Math.round(done * 100.0F) + "%"), GREEN);
        bar(graphics, rx, y + 72, rw, done);
        row(graphics, rx, y + 81, rw, t("energy"), bracket(energy + "%"), energy < 100 ? WARN : GREEN);
        bar(graphics, rx, y + 91, rw, energy / 100.0F);
        row(graphics, rx, y + 100, rw, t("internal"), bracket(used + " / " + QuarryTerminalBlockEntity.STORAGE_SLOTS), GREEN);
        bar(graphics, rx, y + 110, rw, used / (float) QuarryTerminalBlockEntity.STORAGE_SLOTS);

        upgradesPanel(graphics, x, y);
    }

    /**
     * PROGRESS - REFILL, the mockup's refill layout: STATUS, CHUNK, LAYER, BLOCKS PLACED, BLOCKS LEFT, TIME LEFT | AREA
     * FILLED, ENERGY, FILLING WITH (the slot and its block, on its own line - the pair is too wide for one in
     * Minecraft's font) and ON HAND.
     */
    private void progressRefill(GuiGraphics graphics, int x, int y) {
        panel(graphics, x + 8, y + 48, 280, 82, t("progress.refill"));

        var status = menu.status();
        var statusColour = switch (status) {
            case NO_POWER, NO_FILL, NO_AREA, NEED_FRAMES, FRAME_BLOCKED, WAITING, REDSTONE -> WARN;
            default -> GREEN;
        };
        var label = t("status." + status.name().toLowerCase(java.util.Locale.ROOT));

        var chunks = menu.data(QuarryTerminalMenu.DATA_CHUNKS);
        var left = menu.data(QuarryTerminalMenu.DATA_FILL_LEFT);
        var total = Math.max(1, menu.data(QuarryTerminalMenu.DATA_FILL_TOTAL));
        var filling = chunks > 0 && left > 0;
        var none = t("time_left.none");

        var lx = x + 13;
        var lw = 129;

        graphics.drawString(font, t("status"), lx, y + 62, GREEN, false);
        graphics.fill(lx + lw - font.width(label) - 6, y + 60, lx + lw, y + 71, statusColour);
        graphics.drawString(font, label, lx + lw - font.width(label) - 3, y + 62, TEXT_DARK, false);

        row(
            graphics,
            lx,
            y + 72,
            lw,
            t("chunk"),
            filling ? bracket(menu.data(QuarryTerminalMenu.DATA_FILL_CHUNK) + " OF " + chunks) : none,
            filling ? GREEN : MUTED
        );

        var layer = Component.literal(
            "[" + menu.data(QuarryTerminalMenu.DATA_FILL_LAYER_OF) + " OF " + menu.data(QuarryTerminalMenu.DATA_FILL_LAYERS) + "] Y ["
                + menu.data(QuarryTerminalMenu.DATA_FILL_Y) + "]"
        );

        if (font.width(layer) > lw - font.width(t("layer")) - 4) {
            layer = Component.literal(
                "[" + menu.data(QuarryTerminalMenu.DATA_FILL_LAYER_OF) + "/" + menu.data(QuarryTerminalMenu.DATA_FILL_LAYERS) + "] Y ["
                    + menu.data(QuarryTerminalMenu.DATA_FILL_Y) + "]"
            );
        }

        row(graphics, lx, y + 82, lw, t("layer"), filling ? layer : none, filling ? GREEN : MUTED);
        row(
            graphics,
            lx,
            y + 92,
            lw,
            t("blocks_placed"),
            bracket(grouped(menu.wide(QuarryTerminalMenu.DATA_PLACED_LO, QuarryTerminalMenu.DATA_PLACED_HI))),
            GREEN
        );
        row(graphics, lx, y + 102, lw, t("blocks_left"), bracket(grouped(left * 256)), filling ? GREEN : MUTED);
        row(
            graphics,
            lx,
            y + 112,
            lw,
            t("time_left"),
            filling
                ? bracket("~" + duration(left * menu.data(QuarryTerminalMenu.DATA_SECONDS)))
                : none,
            filling ? GREEN : MUTED
        );

        var rx = x + 154;
        var rw = 129;
        var done = chunks == 0 ? 0.0F : 1.0F - left / (float) total;
        var energy = menu.data(QuarryTerminalMenu.DATA_ENERGY);

        row(graphics, rx, y + 62, rw, t("area_filled"), bracket(Math.round(done * 100.0F) + "%"), GREEN);
        bar(graphics, rx, y + 72, rw, done);
        row(graphics, rx, y + 81, rw, t("energy"), bracket(energy + "%"), energy < 100 ? WARN : GREEN);
        bar(graphics, rx, y + 91, rw, energy / 100.0F);

        // FILLING WITH: "SLOT n [BLOCK]" under its label, cut to the column if the name is long.
        var slot = menu.data(QuarryTerminalMenu.DATA_FILL_SLOT);
        var with = slot <= 0
            ? "-"
            : "SLOT " + slot + " [" + menu.getSlot(QuarryTerminalMenu.FIRST_FILL + slot - 1)
                .getItem()
                .getHoverName()
                .getString()
                .toUpperCase(java.util.Locale.ROOT) + "]";

        if (slot > 0 && menu.getSlot(QuarryTerminalMenu.FIRST_FILL + slot - 1).getItem().isEmpty()) {
            with = "SLOT " + slot;
        }

        graphics.drawString(font, t("filling_with"), rx, y + 100, GREEN, false);
        var shown = font.plainSubstrByWidth(with, rw);
        graphics.drawString(font, shown, rx + rw - font.width(shown), y + 110, GREEN, false);
        row(
            graphics,
            rx,
            y + 120,
            rw,
            t("on_hand"),
            bracket(grouped(menu.data(QuarryTerminalMenu.DATA_ON_HAND))),
            menu.data(QuarryTerminalMenu.DATA_ON_HAND) > 0 ? GREEN : WARN
        );
    }

    /** UPGRADES: the 2 x 2 cards, the enchantment slot and the player's inventory - the same on both progress views. */
    private void upgradesPanel(GuiGraphics graphics, int x, int y) {
        panel(graphics, x + 8, y + 132, 280, 96, t("upgrades"));
        wells(graphics, x + QuarryTerminalMenu.UPGRADES_X, y + QuarryTerminalMenu.UPGRADES_Y, 2, 2);
        enchantSlots(graphics, x, y);
        // The mockup's note, beside the 2 x 2 (one line under it would run into the hotbar at x 117).
        graphics.drawWordWrap(font, t("upgrades.place"), x + 57, y + 156, 58, MUTED);
        inventory(graphics, x + QuarryTerminalMenu.PROGRESS_INVENTORY_X, y);
    }

    /**
     * "[41 OF 127] Y [21]" as in the mockup. Measured with the real font, a deep dig ("[127 OF 384] Y [-64]", 104 px)
     * no longer fits beside LAYER, so it falls back to "[127/384] Y [-64]" only when it would overlap.
     */
    private Component layerValue(int room) {
        var layer = menu.data(QuarryTerminalMenu.DATA_LAYER_OF_CHUNK);
        var of = menu.data(QuarryTerminalMenu.DATA_LAYERS_PER_CHUNK);
        var tail = "] Y [" + menu.data(QuarryTerminalMenu.DATA_LAYER_Y) + "]";
        var full = Component.literal("[" + layer + " OF " + of + tail);

        return font.width(full) <= room ? full : Component.literal("[" + layer + "/" + of + tail);
    }

    private static Component bracket(String value) {
        return Component.literal("[" + value + "]");
    }

    /** 43008 -> 43,008. */
    private static String grouped(int value) {
        return String.format(java.util.Locale.ROOT, "%,d", value);
    }

    /** Book outline shown in the empty enchantment slot ([stated] Oct 15: "a book outline in the slot"). */
    private static final net.minecraft.resources.ResourceLocation BOOK_OUTLINE =
        com.human.HumanResources.location("textures/gui/quarry/book_outline.png");

    /** The ENCHANTMENT slot beside the upgrades (a FORTUNE or SILK TOUCH book), with a book outline while empty. */
    private void enchantSlots(GuiGraphics graphics, int x, int y) {
        var slotX = x + QuarryTerminalMenu.ENCHANTS_X;

        wells(graphics, slotX, y + QuarryTerminalMenu.ENCHANTS_Y, 1, QuarryTerminalBlockEntity.ENCHANT_SLOTS);

        for (int slot = 0; slot < QuarryTerminalBlockEntity.ENCHANT_SLOTS; slot++) {
            var slotY = y + QuarryTerminalMenu.ENCHANTS_Y + slot * 18;

            if (!menu.getSlot(QuarryTerminalMenu.FIRST_ENCHANT + slot).hasItem()) {
                graphics.blit(BOOK_OUTLINE, slotX, slotY, 0.0F, 0.0F, 16, 16, 16, 16);
            }

            // Full-size name beside it (66 px, clear of the inventory at x 117); what it takes is in the tooltip.
            graphics.drawString(font, t("enchant"), slotX + 20, slotY + 4, MUTED, false);
        }
    }

    /** 1H 05M, 4M 20S, 40S. */
    private static String duration(int seconds) {
        var hours = seconds / 3600;
        var minutes = seconds / 60 % 60;

        if (hours > 0) {
            return hours + "H " + minutes + "M";
        }

        return minutes > 0 ? minutes + "M " + seconds % 60 + "S" : seconds + "S";
    }

    private void storage(GuiGraphics graphics, int x, int y) {
        panel(graphics, x + 8, y + 48, 280, 180, t("internal_storage"));
        wells(
            graphics,
            x + QuarryTerminalMenu.STORAGE_X,
            y + QuarryTerminalMenu.STORAGE_Y,
            QuarryTerminalMenu.VIEW_COLUMNS,
            QuarryTerminalMenu.VIEW_ROWS
        );

        // Scroll column: up, track, down - exactly the height of the four rows.
        var sx = x + 162;
        var sy = y + 68;

        button(graphics, sx, sy, 13, 13, Component.literal("^"), false, -1, -1);
        graphics.fill(sx, sy + 14, sx + 13, sy + 58, WELL);

        var thumbY = QuarryTerminalMenu.MAX_SCROLL == 0 ? 0 : 34 * menu.scrollRow() / QuarryTerminalMenu.MAX_SCROLL;

        graphics.fill(sx + 2, sy + 15 + thumbY, sx + 11, sy + 24 + thumbY, GREEN);
        button(graphics, sx, sy + 59, 13, 13, Component.literal("v"), false, -1, -1);

        var used = menu.data(QuarryTerminalMenu.DATA_USED);

        row(graphics, x + 184, y + 70, 96, t("slots"), Component.literal(used + "/" + QuarryTerminalBlockEntity.STORAGE_SLOTS), GREEN);
        graphics.drawWordWrap(font, t("storage.note"), x + 184, y + 84, 66, MUTED);

        graphics.drawString(font, t("frames"), x + 184, y + 134, GREEN, false);
        graphics.drawString(font, String.valueOf(menu.data(QuarryTerminalMenu.DATA_FRAMES)), x + 238, y + 134, GREEN, false);
        wells(graphics, x + QuarryTerminalMenu.FRAMES_X, y + QuarryTerminalMenu.FRAMES_Y, 3, 3);
        graphics.drawString(font, t("frames.only"), x + 184, y + 203, MUTED, false);

        // Tank (phase 4b): filled from the bottom in the liquid's own map colour, its name and buckets under it.
        var tenths = menu.data(QuarryTerminalMenu.DATA_TANK);
        var fluidId = menu.data(QuarryTerminalMenu.DATA_TANK_FLUID);
        var fluid = fluidId < 0 || tenths <= 0 ? null : net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(fluidId);

        // Oct 16: the whole gauge sits 14 px lower, so the SLOTS line above (which runs to x 280) clears its label.
        graphics.drawString(font, t("tank"), x + 259, y + 82, GREEN, false);
        graphics.fill(x + 257, y + 93, x + 282, y + 178, GREEN);
        graphics.fill(x + 258, y + 94, x + 281, y + 177, WELL);

        if (fluid != null) {
            var block = fluid.defaultFluidState().createLegacyBlock();
            var colour = 0xFF000000 | block.getBlock().defaultMapColor().col;
            var height = Math.round(83 * Math.min(1.0F, tenths / (float) (QuarryTerminalBlockEntity.TANK_CAPACITY / 100)));

            graphics.fill(x + 258, y + 177 - height, x + 281, y + 177, colour);

            // Names wider than the gauge are cut rather than run into the inventory panel's edge.
            var name = font.plainSubstrByWidth(block.getBlock().getName().getString().toUpperCase(java.util.Locale.ROOT), 30);

            graphics.drawString(font, name, x + 257, y + 182, GREEN, false);
        } else {
            graphics.drawString(font, t("tank.empty"), x + 257, y + 182, MUTED, false);
        }

        graphics.drawString(font, tenths / 10 + "/128", x + 257, y + 192, fluid != null ? GREEN : MUTED, false);

        inventory(graphics, x + QuarryTerminalMenu.STORAGE_INVENTORY_X, y);
    }

    private void inventory(GuiGraphics graphics, int slotX, int y) {
        wells(graphics, slotX, y + QuarryTerminalMenu.INVENTORY_Y, 9, 3);
        wells(graphics, slotX, y + QuarryTerminalMenu.HOTBAR_Y, 9, 1);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Pieces
    // ------------------------------------------------------------------------------------------------------------

    private void panel(GuiGraphics graphics, int x, int y, int width, int height, Component title) {
        graphics.fill(x, y, x + width, y + height, GREEN);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL);
        graphics.drawString(font, title, x + 4, y + 2, GREEN, false);
        graphics.fill(x + 1, y + 11, x + width - 1, y + 12, GREEN);
    }

    private void button(GuiGraphics graphics, int x, int y, int width, int height, Component label, boolean on, int mouseX, int mouseY) {
        var hover = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

        graphics.fill(x, y, x + width, y + height, GREEN);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, on ? GREEN : hover ? WELL : PANEL);

        // Oct 16 - text overlap fix: a label wider than its button (Minecraft's font is wider than the mockup's) is
        // drawn smaller to fit, rather than spilling over the button's edges and its neighbours.
        var labelWidth = font.width(label);
        var room = width - 4;

        if (labelWidth <= room) {
            centered(graphics, label, x + width / 2, y + (height - 8) / 2 + 1, on ? TEXT_DARK : GREEN);
            return;
        }

        var scale = Math.max(0.55F, room / (float) labelWidth);

        graphics.pose().pushPose();
        graphics.pose().translate(x + width / 2.0F, y + height / 2.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, label, -labelWidth / 2, -4, on ? TEXT_DARK : GREEN, false);
        graphics.pose().popPose();
    }

    /**
     * Centred text WITHOUT a drop shadow. ⚠ Oct 16 fix: vanilla's drawCenteredString always adds a shadow, and on the
     * lit (bright green) buttons the dark text's shadow sat one pixel off and made every label look smeared and bold.
     * The rest of the screen already draws its text shadowless.
     */
    private void centered(GuiGraphics graphics, Component text, int centreX, int y, int colour) {
        graphics.drawString(font, text, centreX - font.width(text) / 2, y, colour, false);
    }

    private void centered(GuiGraphics graphics, String text, int centreX, int y, int colour) {
        graphics.drawString(font, text, centreX - font.width(text) / 2, y, colour, false);
    }

    private void row(GuiGraphics graphics, int x, int y, int width, Component label, Component value, int valueColour) {
        graphics.drawString(font, label, x, y, GREEN, false);
        graphics.drawString(font, value, x + width - font.width(value), y, valueColour, false);
    }

    private void bar(GuiGraphics graphics, int x, int y, int width, float fill) {
        graphics.fill(x, y, x + width, y + 6, GREEN);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 5, WELL);
        graphics.fill(x + 1, y + 1, x + 1 + Math.round((width - 2) * Math.clamp(fill, 0.0F, 1.0F)), y + 5, GREEN);
    }

    /** Slot wells around vanilla slot positions (the item's top-left is one pixel inside the well). */
    private void wells(GuiGraphics graphics, int slotX, int slotY, int columns, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                var wx = slotX - 1 + column * 18;
                var wy = slotY - 1 + row * 18;

                graphics.fill(wx, wy, wx + 18, wy + 18, GREEN);
                graphics.fill(wx + 1, wy + 1, wx + 17, wy + 17, WELL);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var x = leftPos;
        var y = topPos;

        if (button == 0) {
            if (inside(mouseX, mouseY, x + START_X, y + 10, START_W, 11)) {
                press(QuarryTerminalMenu.BUTTON_START);
                return true;
            }

            if (inside(mouseX, mouseY, x + STOP_X, y + 10, STOP_W, 11)) {
                press(QuarryTerminalMenu.BUTTON_STOP);
                return true;
            }

            for (int tab = 0; tab < TAB_KEYS.length; tab++) {
                if (inside(mouseX, mouseY, x + 8 + tab * 70, y + 32, 66, 12)) {
                    menu.setTab(tab);
                    return true;
                }
            }

            if (menu.tab() == QuarryTerminalMenu.TAB_WORK_AREA && workAreaClicked(mouseX, mouseY, x, y)) {
                return true;
            }

            if (
                menu.tab() == QuarryTerminalMenu.TAB_SETTINGS && (refillClicked(mouseX, mouseY, x, y) || settingsClicked(
                    mouseX,
                    mouseY,
                    x,
                    y
                ))
            ) {
                return true;
            }

            if (menu.tab() == QuarryTerminalMenu.TAB_STORAGE) {
                if (inside(mouseX, mouseY, x + 162, y + 68, 13, 13)) {
                    scrollStorage(-1);
                    return true;
                }

                if (inside(mouseX, mouseY, x + 162, y + 127, 13, 13)) {
                    scrollStorage(1);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** The REFILL panel: START / STOP and the MIN Y / MAX Y steppers (shift = 10). Ignored while REFILL HOLE is OFF. */
    private boolean refillClicked(double mouseX, double mouseY, int x, int y) {
        if (menu.data(QuarryTerminalMenu.DATA_REFILL_ON) != 1) {
            return false;
        }

        if (inside(mouseX, mouseY, x + REFILL_BUTTONS_X, y + 49, REFILL_START_W, 10)) {
            press(QuarryTerminalMenu.BUTTON_REFILL_START);
            return true;
        }

        if (inside(mouseX, mouseY, x + REFILL_STOP_X, y + 49, REFILL_STOP_W, 10)) {
            press(QuarryTerminalMenu.BUTTON_REFILL_STOP);
            return true;
        }

        var shift = hasShiftDown();

        for (
            var stepper : new int[][] {
                { REFILL_MIN_Y, QuarryTerminalMenu.BUTTON_REFILL_MIN },
                { REFILL_MAX_Y, QuarryTerminalMenu.BUTTON_REFILL_MAX } }
        ) {
            if (inside(mouseX, mouseY, x + REFILL_MINUS_X, y + stepper[0], 13, 11)) {
                press(stepper[1] + (shift ? 0 : 1));
                return true;
            }

            if (inside(mouseX, mouseY, x + REFILL_PLUS_X, y + stepper[0], 13, 11)) {
                press(stepper[1] + (shift ? 3 : 2));
                return true;
            }
        }

        return false;
    }

    private boolean workAreaClicked(double mouseX, double mouseY, int x, int y) {
        var size = QuarryTerminalBlockEntity.MAP_SIZE;

        for (int row = 0; row < size - 1; row++) {
            for (int column = 0; column < size; column++) {
                if (inside(mouseX, mouseY, x + MAP_X + column * CELL, y + MAP_Y + row * CELL, CELL, CELL)) {
                    press(QuarryTerminalMenu.BUTTON_CELL + row * size + column);
                    return true;
                }
            }
        }

        var shift = hasShiftDown();

        if (inside(mouseX, mouseY, x + BOTTOM_MINUS_X, y + 177, 13, 10)) {
            press(QuarryTerminalMenu.BUTTON_BOTTOM + (shift ? 0 : 1));
            return true;
        }

        if (inside(mouseX, mouseY, x + BOTTOM_PLUS_X, y + 177, 13, 10)) {
            press(QuarryTerminalMenu.BUTTON_BOTTOM + (shift ? 3 : 2));
            return true;
        }

        if (inside(mouseX, mouseY, x + 169, y + 202, 56, 11)) {
            press(menu.data(QuarryTerminalMenu.DATA_CONFIRMED) == 1 ? QuarryTerminalMenu.BUTTON_EDIT : QuarryTerminalMenu.BUTTON_CONFIRM);
            return true;
        }

        if (inside(mouseX, mouseY, x + 228, y + 202, 56, 11)) {
            press(QuarryTerminalMenu.BUTTON_RETRACT);
            return true;
        }

        return false;
    }

    private boolean settingsClicked(double mouseX, double mouseY, int x, int y) {
        if (inside(mouseX, mouseY, x + 12, y + 212, 72, 11)) {
            saveSettings();
            return true;
        }

        if (inside(mouseX, mouseY, x + 88, y + 212, 72, 11)) {
            loadSettings();
            return true;
        }

        if (!inside(mouseX, mouseY, x + 9, y + SETTINGS_TOP, 150, SETTINGS_HEIGHT)) {
            return false;
        }

        var options = QuarrySettings.Option.values();
        var rowTop = 0;

        for (int index = 0; index < options.length; index++) {
            var option = options[index];
            var rowY = y + SETTINGS_TOP + 2 + rowTop - settingsScroll;
            var width = (146 - (option.choices() - 1) * 3) / option.choices();

            rowTop += settingRowHeight(option);

            for (int choice = 0; choice < option.choices(); choice++) {
                if (inside(mouseX, mouseY, x + 12 + choice * (width + 3), rowY + 10, width, 10)) {
                    press(QuarryTerminalMenu.BUTTON_SETTING + index * 10 + choice);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.tab() == QuarryTerminalMenu.TAB_SETTINGS) {
            settingsScroll = Math.clamp(settingsScroll - (int) Math.signum(scrollY) * 12, 0, maxSettingsScroll());
            return true;
        }

        if (menu.tab() == QuarryTerminalMenu.TAB_STORAGE) {
            scrollStorage(scrollY > 0 ? -1 : 1);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void scrollStorage(int rows) {
        var row = Math.clamp(menu.scrollRow() + rows, 0, QuarryTerminalMenu.MAX_SCROLL);

        if (row != menu.scrollRow()) {
            menu.scrollTo(row);
            press(QuarryTerminalMenu.BUTTON_SCROLL + row);
        }
    }

    /**
     * Oct 16 - text overlap fix: a settings row is as tall as its CHOSEN description wraps to (one to four lines in
     * Minecraft's font), not a fixed 36 px - the fixed row let longer descriptions run into the next setting.
     */
    private int settingRowHeight(QuarrySettings.Option option) {
        var lines = font.split(t("setting." + option.key() + "." + menu.setting(option) + ".desc"), DESCRIPTION_WIDTH).size();

        return Math.max(SETTING_ROW, 22 + lines * 9 + 5);
    }

    private static final int DESCRIPTION_WIDTH = 146;

    private int settingsHeight() {
        var total = 4;

        for (var option : QuarrySettings.Option.values()) {
            total += settingRowHeight(option);
        }

        return total;
    }

    private int maxSettingsScroll() {
        return Math.max(0, settingsHeight() - SETTINGS_HEIGHT);
    }

    // ------------------------------------------------------------------------------------------------------------
    // SAVE / LOAD - a copy of the settings on this computer, usable on any world ([stated] Oct 15)
    // ------------------------------------------------------------------------------------------------------------

    private java.nio.file.Path saveFile() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(SAVE_FILE);
    }

    private void saveSettings() {
        var properties = new Properties();

        for (var option : QuarrySettings.Option.values()) {
            properties.setProperty(option.key(), String.valueOf(menu.setting(option)));
        }

        try {
            Files.createDirectories(saveFile().getParent());
        } catch (java.io.IOException failed) {
            saveNote = t("save_failed");
            return;
        }

        try (var out = Files.newBufferedWriter(saveFile(), StandardCharsets.UTF_8)) {
            properties.store(out, "Quarry-Com saved settings");
            saveNote = t("saved");
        } catch (java.io.IOException failed) {
            saveNote = t("save_failed");
        }
    }

    private void loadSettings() {
        if (!Files.exists(saveFile())) {
            saveNote = t("nothing_saved");
            return;
        }

        var properties = new Properties();

        try (var in = Files.newBufferedReader(saveFile(), StandardCharsets.UTF_8)) {
            properties.load(in);
        } catch (java.io.IOException failed) {
            saveNote = t("save_failed");
            return;
        }

        var options = QuarrySettings.Option.values();

        for (int index = 0; index < options.length; index++) {
            try {
                var choice = Integer.parseInt(properties.getProperty(options[index].key(), "-1"));

                if (choice >= 0 && choice < options[index].choices() && choice != menu.setting(options[index])) {
                    press(QuarryTerminalMenu.BUTTON_SETTING + index * 10 + choice);
                }
            } catch (NumberFormatException ignored) {
                // A hand-edited file with a bad line skips that line.
            }
        }

        saveNote = t("loaded");
    }
}
