package com.human.client.screen;

import com.human.common.gameplay.fabrication.FabricatorBlockEntity;
import com.human.common.gameplay.fabrication.FabricatorMenu;
import com.human.common.gameplay.machine.MachinePriority;
import com.human.common.gameplay.machine.MachineUpgrades;
import com.human.common.network.packet.S2CFabricatorPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The fabricator terminal, Oct 16 layout ([stated] "the fabricator gui seems to be too small"; "can we fit all options
 * on the screen at the top?"). 320 x 236, MUTHUR colours, drawn in code. One row of four buttons at the top:
 * <ul>
 * <li><b>PRINT</b> - catalogue (search, ALL / LOCAL / NET, 8 x 6 grid), materials (NEED / HAVE) and printing (QTY,
 * PRINT / STOP, progress, output) together, so picking, checking and printing never switch view.</li>
 * <li><b>BLUEPRINTS</b> - the 16 local slots.</li>
 * <li><b>STORAGE</b> - 48 internal slots in an 8-wide scrolling grid, or the connected storage in the same grid
 * (INTERNAL / CONNECTED).</li>
 * <li><b>SYSTEM</b> - upgrade cards, priority, redstone.</li>
 * </ul>
 * [stated] Oct 16: every label at the SAME size - anything that did not fit was reworded, never shrunk. Text has no
 * drop shadow (a shadow smears dark text on the lit buttons). The player inventory is green here, as ruled.
 */
public class FabricatorScreen extends AbstractContainerScreen<FabricatorMenu> {

    public static final int WIDTH = 320;

    public static final int HEIGHT = 236;

    private static final int BG = 0xFF051008;

    private static final int PANEL = 0xFF09190F;

    private static final int GREEN = 0xFF6EF58C;

    private static final int MUTED = 0xFF33974E;

    private static final int DIM = 0xFF16351F;

    private static final int WELL = 0xFF0E2816;

    private static final int DARK = 0xFF051008;

    private static final int WARN = 0xFFF0BE5A;

    // Catalogue grid.
    private static final int GRID_X = 13;

    private static final int GRID_Y = 91;

    private static final int GRID_COLUMNS = 8;

    private static final int GRID_ROWS = 6;

    // Materials rows.
    private static final int MATERIAL_ROWS = 3;

    // The STORAGE grid (internal slots or connected entries) and its scroll bar.
    private static final int STORE_BAR_X = FabricatorMenu.SLOT_X + FabricatorMenu.STORAGE_COLUMNS * 18 + 2;

    private static List<S2CFabricatorPayload.Entry> catalogue = List.of();

    private static List<S2CFabricatorPayload.Material> materials = List.of();

    private static List<S2CFabricatorPayload.Entry> storage = List.of();

    private static ItemStack printing = ItemStack.EMPTY;

    private static int selected = -1;

    private static int receivedFor = -1;

    private final List<Target> targets = new ArrayList<>();

    private EditBox catalogueSearch;

    private EditBox storageSearch;

    /** 0 all, 1 local, 2 network. */
    private int filter;

    private int catalogueScroll;

    private int materialScroll;

    private int connectedScroll;

    private record Target(
        int x0,
        int y0,
        int x1,
        int y1,
        Runnable action
    ) {}

    public FabricatorScreen(FabricatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    /** Called from the packet handler on the client thread. */
    public static void handle(S2CFabricatorPayload payload) {
        if (Minecraft.getInstance().screen instanceof FabricatorScreen screen && screen.menu.containerId == payload.containerId()) {
            catalogue = payload.catalogue();
            selected = payload.selected();
            materials = payload.materials();
            printing = payload.printing();
            storage = payload.storage();
            receivedFor = payload.containerId();
        }
    }

    @Override
    protected void init() {
        super.init();

        if (receivedFor != menu.containerId) {
            catalogue = List.of();
            materials = List.of();
            storage = List.of();
            printing = ItemStack.EMPTY;
            selected = -1;
        }

        catalogueSearch = new EditBox(font, leftPos + 15, topPos + 63, 158, 9, Component.empty());
        catalogueSearch.setBordered(false);
        catalogueSearch.setTextColor(GREEN);
        catalogueSearch.setHint(t("search").withStyle(style -> style.withColor(MUTED)));
        catalogueSearch.setResponder(text -> catalogueScroll = 0);
        addRenderableWidget(catalogueSearch);

        storageSearch = new EditBox(font, leftPos + 185, topPos + 78, 122, 9, Component.empty());
        storageSearch.setBordered(false);
        storageSearch.setTextColor(GREEN);
        storageSearch.setHint(t("search").withStyle(style -> style.withColor(MUTED)));
        storageSearch.setResponder(text -> connectedScroll = 0);
        addRenderableWidget(storageSearch);

        updateWidgets();
    }

    private void updateWidgets() {
        catalogueSearch.visible = menu.tab == FabricatorMenu.PRINT_VIEW;
        storageSearch.visible = menu.tab == FabricatorMenu.STORAGE && !menu.showInternal;
    }

    private void showTab(int tab) {
        menu.tab = tab;
        press(tab);
        updateWidgets();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        targets.clear();
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG);

        // Header: the title at 1.5x (2x ran under the machine name), the machine and its status on the right.
        g.pose().pushPose();
        g.pose().translate(leftPos + 8, topPos + 4, 0);
        g.pose().scale(1.5F, 1.5F, 1.0F);
        g.drawString(font, "/// Fab-Com", 0, 0, GREEN, false);
        g.pose().popPose();
        text(g, t("subtitle"), 8, 19, MUTED);
        textRight(g, title, WIDTH - 8, 4, GREEN);

        var status = t("status." + menu.status().name().toLowerCase(Locale.ROOT));
        var statusWidth = font.width(status);

        frame(g, WIDTH - 8 - statusWidth - 6, 13, WIDTH - 8, 24, statusColour(), statusColour());
        text(g, status, WIDTH - 8 - statusWidth - 3, 15, DARK);

        for (int i = 0; i < FabricatorMenu.TABS.length; i++) {
            var tab = i;
            var x = 8 + i * 77;

            button(g, t("tab." + FabricatorMenu.TABS[i]), x, 30, x + 73, 42, i == menu.tab, true, mouseX, mouseY, () -> showTab(tab));
        }

        switch (menu.tab) {
            case FabricatorMenu.BLUEPRINTS -> blueprints(g);
            case FabricatorMenu.STORAGE -> storage(g, mouseX, mouseY);
            case FabricatorMenu.SYSTEM -> system(g, mouseX, mouseY);
            default -> print(g, mouseX, mouseY);
        }

        if (menu.tab != FabricatorMenu.PRINT_VIEW) {
            panel(g, 8, 138, 304, 90, t("inventory"));
        }

        // Wells behind whichever slots this view shows.
        for (var slot : menu.slots) {
            if (slot.isActive()) {
                frame(g, slot.x - 1, slot.y - 1, slot.x + 17, slot.y + 17, MUTED, WELL);
            }
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // Everything is drawn in renderBg.
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        var hovered = hoveredStack(mouseX, mouseY);

        if (!hovered.isEmpty()) {
            g.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // PRINT: catalogue, materials, printing
    // ------------------------------------------------------------------------------------------------------------

    private List<Integer> visibleCatalogue() {
        var query = catalogueSearch == null ? "" : catalogueSearch.getValue().trim().toLowerCase(Locale.ROOT);
        var visible = new ArrayList<Integer>();

        for (int index = 0; index < catalogue.size(); index++) {
            var entry = catalogue.get(index);

            if ((filter == 1 && entry.value() != 1) || (filter == 2 && entry.value() != 2) || !matches(entry.stack(), query)) {
                continue;
            }

            visible.add(index);
        }

        return visible;
    }

    private void print(GuiGraphics g, int mouseX, int mouseY) {
        // --- CATALOGUE ---
        panel(g, 8, 46, 170, 182, t("catalogue"));
        frame(g, 12, 61, 174, 73, GREEN, WELL);

        String[] filters = { "all", "local", "network" };
        int[][] boxes = { { 12, 52 }, { 55, 101 }, { 104, 144 } };

        for (int i = 0; i < filters.length; i++) {
            var choice = i;

            button(g, t("filter." + filters[i]), boxes[i][0], 76, boxes[i][1], 87, filter == i, true, mouseX, mouseY, () -> {
                filter = choice;
                catalogueScroll = 0;
            });
        }

        var visible = visibleCatalogue();
        var first = catalogueScroll * GRID_COLUMNS;

        for (int cell = 0; cell < GRID_COLUMNS * GRID_ROWS; cell++) {
            var x = GRID_X + (cell % GRID_COLUMNS) * 18;
            var y = GRID_Y + (cell / GRID_COLUMNS) * 18;

            frame(g, x - 1, y - 1, x + 17, y + 17, MUTED, WELL);

            if (first + cell >= visible.size()) {
                continue;
            }

            var index = visible.get(first + cell);
            var entry = catalogue.get(index);

            if (index == selected) {
                frame(g, x - 1, y - 1, x + 17, y + 17, GREEN, WELL);
            }

            g.renderItem(entry.stack(), leftPos + x, topPos + y);

            // Oct 21 - how many one print makes, when it is more than one (ammo and the like).
            if (entry.stack().getCount() > 1) {
                g.renderItemDecorations(font, entry.stack(), leftPos + x, topPos + y);
            }

            // A small mark in the corner: this print comes from a drive bay on the cable, not this fabricator.
            if (entry.value() == 2) {
                g.fill(leftPos + x + 13, topPos + y, leftPos + x + 16, topPos + y + 3, MUTED);
            }

            targets.add(new Target(x, y, x + 16, y + 16, () -> press(FabricatorMenu.SELECT + index)));
        }

        var rows = (visible.size() + GRID_COLUMNS - 1) / GRID_COLUMNS;

        scrollBar(g, 162, GRID_Y - 1, GRID_ROWS * 18, catalogueScroll, rows - GRID_ROWS);

        if (visible.isEmpty()) {
            wrap(g, t(catalogue.isEmpty() ? "empty" : "no_match"), 14, 94, 140, MUTED);
        }

        text(g, t("catalogue_count", visible.size()), 12, 212, MUTED);

        if (rows > GRID_ROWS) {
            textRight(g, (catalogueScroll + 1) + "/" + (rows - GRID_ROWS + 1), 174, 212, MUTED);
        }

        // --- MATERIALS ---
        panel(g, 182, 46, 130, 100, t("materials"));

        if (selected < 0 || selected >= catalogue.size()) {
            wrap(g, t("pick"), 186, 61, 122, MUTED);
        } else {
            var name = font.plainSubstrByWidth(catalogue.get(selected).stack().getHoverName().getString().toUpperCase(Locale.ROOT), 122);

            text(g, Component.literal(name), 186, 61, GREEN);
            // Columns end before the scroll bar (x 302-308), which is always shown, like the other lists.
            textRight(g, t("need"), 254, 72, MUTED);
            textRight(g, t("have"), 294, 72, MUTED);

            if (materials.isEmpty()) {
                text(g, t("calculating"), 186, 86, MUTED);
            }

            var shortBy = 0;

            for (var line : materials) {
                shortBy += line.missing();
            }

            for (int row = 0; row < MATERIAL_ROWS && materialScroll + row < materials.size(); row++) {
                var line = materials.get(materialScroll + row);
                var y = 82 + row * 17;
                var need = line.internal() + line.network() + line.missing();
                var have = line.internal() + line.network();

                g.renderItem(line.stack(), leftPos + 186, topPos + y);
                textRight(g, compact(need), 254, y + 4, GREEN);
                textRight(g, compact(have), 294, y + 4, have < need ? WARN : GREEN);
            }

            scrollBar(g, 302, 81, MATERIAL_ROWS * 17, materialScroll, materials.size() - MATERIAL_ROWS);

            if (!materials.isEmpty()) {
                text(g, shortBy > 0 ? t("short", compact(shortBy)) : t("all_here"), 186, 134, shortBy > 0 ? WARN : MUTED);
            }
        }

        // --- PRINT ---
        panel(g, 182, 150, 130, 78, t("print"));
        text(g, t("qty"), 186, 165, GREEN);
        button(g, Component.literal("-"), 216, 163, 229, 174, false, true, mouseX, mouseY, () -> press(FabricatorMenu.COUNT_DOWN));

        var count = String.valueOf(menu.count());

        g.drawString(font, count, leftPos + 242 - font.width(count) / 2, topPos + 165, GREEN, false);
        button(g, Component.literal("+"), 256, 163, 269, 174, false, true, mouseX, mouseY, () -> press(FabricatorMenu.COUNT_UP));
        button(g, Component.literal("+16"), 272, 163, 308, 174, false, true, mouseX, mouseY, () -> press(FabricatorMenu.COUNT_UP_SIXTEEN));

        var missing = materials.stream().anyMatch(line -> line.missing() > 0);
        var busy = menu.queued() > 0;
        var canPrint = selected >= 0 && !missing && !busy;

        button(g, t("print"), 186, 178, 244, 191, false, canPrint, mouseX, mouseY, () -> press(FabricatorMenu.PRINT));
        button(g, t("stop"), 250, 178, 308, 191, false, busy, mouseX, mouseY, () -> press(FabricatorMenu.STOP));

        if (busy) {
            text(g, t("left", menu.queued()), 186, 195, GREEN);
            textRight(g, formatTime(menu.secondsLeft()), 308, 195, GREEN);
        } else {
            text(g, t("idle_line"), 186, 195, MUTED);
        }

        frame(g, 186, 212, 284, 218, GREEN, WELL);
        g.fill(leftPos + 187, topPos + 213, leftPos + 187 + menu.progress() * 96 / 100, topPos + 217, GREEN);
    }

    // ------------------------------------------------------------------------------------------------------------
    // BLUEPRINTS
    // ------------------------------------------------------------------------------------------------------------

    private void blueprints(GuiGraphics g) {
        panel(g, 8, 46, 304, 88, t("blueprints_title"));

        var lines = wrap(g, t("blueprints_help"), 168, 62, 140, MUTED);

        wrap(g, t("blueprints_shared"), 168, 62 + lines * 9 + 6, 140, MUTED);
    }

    // ------------------------------------------------------------------------------------------------------------
    // STORAGE: internal slots or connected entries in one 8-wide scrolling grid
    // ------------------------------------------------------------------------------------------------------------

    private List<S2CFabricatorPayload.Entry> visibleStorage() {
        var query = storageSearch == null ? "" : storageSearch.getValue().trim().toLowerCase(Locale.ROOT);
        var visible = new ArrayList<S2CFabricatorPayload.Entry>();

        for (var entry : storage) {
            if (matches(entry.stack(), query)) {
                visible.add(entry);
            }
        }

        return visible;
    }

    private void storage(GuiGraphics g, int mouseX, int mouseY) {
        panel(g, 8, 46, 304, 88, t("storage"));

        button(g, t("internal"), 182, 59, 242, 71, menu.showInternal, true, mouseX, mouseY, () -> {
            menu.showInternal = true;
            updateWidgets();
        });
        button(g, t("connected"), 246, 59, 308, 71, !menu.showInternal, true, mouseX, mouseY, () -> {
            menu.showInternal = false;
            updateWidgets();
        });

        var top = FabricatorMenu.STORAGE_Y - 1;
        var height = FabricatorMenu.STORAGE_ROWS_SHOWN * 18;

        if (menu.showInternal) {
            // The internal slots draw their own wells (they are real slots); only the bar and the notes here.
            scrollBar(g, STORE_BAR_X, top, height, menu.storageScroll(), FabricatorMenu.MAX_STORAGE_SCROLL);
            text(g, t("slots", FabricatorBlockEntity.STORAGE_SLOTS), 182, 78, GREEN);
            wrap(g, t("internal_hint"), 182, 90, 126, MUTED);
            return;
        }

        frame(g, 182, 76, 308, 88, GREEN, WELL);

        var visible = visibleStorage();
        var first = connectedScroll * FabricatorMenu.STORAGE_COLUMNS;

        for (int cell = 0; cell < FabricatorMenu.STORAGE_SHOWN; cell++) {
            var x = FabricatorMenu.SLOT_X + (cell % FabricatorMenu.STORAGE_COLUMNS) * 18;
            var y = FabricatorMenu.STORAGE_Y + (cell / FabricatorMenu.STORAGE_COLUMNS) * 18;

            frame(g, x - 1, y - 1, x + 17, y + 17, MUTED, WELL);

            if (first + cell < visible.size()) {
                var entry = visible.get(first + cell);

                g.renderItem(entry.stack(), leftPos + x, topPos + y);
                // The total, drawn the way vanilla draws a stack count - full size, shortened when large (12k).
                g.renderItemDecorations(font, entry.stack(), leftPos + x, topPos + y, compact(entry.value()));
            }
        }

        var rows = (visible.size() + FabricatorMenu.STORAGE_COLUMNS - 1) / FabricatorMenu.STORAGE_COLUMNS;

        scrollBar(g, STORE_BAR_X, top, height, connectedScroll, rows - FabricatorMenu.STORAGE_ROWS_SHOWN);
        text(g, t("types", storage.size()), 182, 93, GREEN);
        wrap(g, t(storage.isEmpty() ? "storage_empty" : "connected_hint"), 182, 105, 126, MUTED);
    }

    // ------------------------------------------------------------------------------------------------------------
    // SYSTEM
    // ------------------------------------------------------------------------------------------------------------

    private void system(GuiGraphics g, int mouseX, int mouseY) {
        panel(g, 8, 46, 148, 88, t("upgrades_title"));

        var lines = wrap(g, t("power_note", FabricatorBlockEntity.PRINT_POWER), 14, 86, 138, MUTED);

        wrap(g, t("speed_note"), 14, 86 + lines * 9 + 9, 138, MUTED);

        panel(g, 160, 46, 152, 88, t("control"));
        text(g, t("priority"), 164, 61, GREEN);

        var priorities = MachinePriority.values();
        int[][] priorityBoxes = { { 164, 212 }, { 214, 260 }, { 262, 308 } };

        for (int i = 0; i < priorities.length && i < priorityBoxes.length; i++) {
            var choice = i;

            button(
                g,
                t("priority." + priorities[i].id()),
                priorityBoxes[i][0],
                72,
                priorityBoxes[i][1],
                83,
                menu.priority() == priorities[i],
                true,
                mouseX,
                mouseY,
                () -> press(FabricatorMenu.PRIORITY + choice)
            );
        }

        text(g, t("redstone"), 164, 90, GREEN);

        var modes = MachineUpgrades.RedstoneMode.values();
        var card = menu.hasRedstoneCard();

        for (int i = 0; i < modes.length && i < priorityBoxes.length; i++) {
            var choice = i;

            button(
                g,
                t("redstone." + modes[i].name().toLowerCase(Locale.ROOT)),
                priorityBoxes[i][0],
                101,
                priorityBoxes[i][1],
                112,
                card && menu.redstoneMode() == modes[i],
                card,
                mouseX,
                mouseY,
                () -> press(FabricatorMenu.REDSTONE + choice)
            );
        }

        if (!card) {
            text(g, t("redstone_card"), 164, 118, MUTED);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            var x = (int) mouseX - leftPos;
            var y = (int) mouseY - topPos;

            for (var target : List.copyOf(targets)) {
                if (x >= target.x0() && x < target.x1() && y >= target.y0() && y < target.y1()) {
                    target.action().run();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var step = -(int) Math.signum(scrollY);
        var x = (int) mouseX - leftPos;

        switch (menu.tab) {
            case FabricatorMenu.PRINT_VIEW -> {
                if (x >= 182) {
                    materialScroll = clamp(materialScroll + step, materials.size() - MATERIAL_ROWS);
                } else {
                    var rows = (visibleCatalogue().size() + GRID_COLUMNS - 1) / GRID_COLUMNS;

                    catalogueScroll = clamp(catalogueScroll + step, rows - GRID_ROWS);
                }
            }
            case FabricatorMenu.STORAGE -> {
                if (menu.showInternal) {
                    var row = clamp(menu.storageScroll() + step, FabricatorMenu.MAX_STORAGE_SCROLL);

                    if (row != menu.storageScroll()) {
                        menu.setStorageScroll(row);
                        press(FabricatorMenu.STORAGE_SCROLL + row);
                    }
                } else {
                    var rows = (visibleStorage().size() + FabricatorMenu.STORAGE_COLUMNS - 1) / FabricatorMenu.STORAGE_COLUMNS;

                    connectedScroll = clamp(connectedScroll + step, rows - FabricatorMenu.STORAGE_ROWS_SHOWN);
                }
            }
            default -> {}
        }

        return true;
    }

    /** A focused search box keeps its keys - typing "e" must not close the screen. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (var box : new EditBox[] { catalogueSearch, storageSearch }) {
            if (box != null && box.isFocused() && box.visible && keyCode != 256) {
                return box.keyPressed(keyCode, scanCode, modifiers) || box.canConsumeInput();
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** The item under the mouse in a drawn grid (not a real slot): catalogue, materials, connected storage. */
    private ItemStack hoveredStack(int mouseX, int mouseY) {
        var x = mouseX - leftPos;
        var y = mouseY - topPos;

        if (menu.tab == FabricatorMenu.PRINT_VIEW) {
            if (x >= GRID_X && x < GRID_X + GRID_COLUMNS * 18 && y >= GRID_Y && y < GRID_Y + GRID_ROWS * 18) {
                var visible = visibleCatalogue();
                var index = catalogueScroll * GRID_COLUMNS + (y - GRID_Y) / 18 * GRID_COLUMNS + (x - GRID_X) / 18;

                return index < visible.size() ? catalogue.get(visible.get(index)).stack() : ItemStack.EMPTY;
            }

            if (x >= 186 && x < 202 && y >= 82 && y < 82 + MATERIAL_ROWS * 17 && (y - 82) % 17 < 16) {
                var index = materialScroll + (y - 82) / 17;

                return index < materials.size() ? materials.get(index).stack() : ItemStack.EMPTY;
            }
        }

        if (
            menu.tab == FabricatorMenu.STORAGE && !menu.showInternal && x >= FabricatorMenu.SLOT_X
                && x < FabricatorMenu.SLOT_X + FabricatorMenu.STORAGE_COLUMNS * 18 && y >= FabricatorMenu.STORAGE_Y
                && y < FabricatorMenu.STORAGE_Y + FabricatorMenu.STORAGE_ROWS_SHOWN * 18
        ) {
            var visible = visibleStorage();
            var index = connectedScroll * FabricatorMenu.STORAGE_COLUMNS + (y - FabricatorMenu.STORAGE_Y) / 18
                * FabricatorMenu.STORAGE_COLUMNS
                + (x - FabricatorMenu.SLOT_X) / 18;

            return index < visible.size() ? visible.get(index).stack() : ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------------

    private static net.minecraft.network.chat.MutableComponent t(String key, Object... args) {
        return Component.translatable("gui.avp_human.fabricator." + key, args);
    }

    private static boolean matches(ItemStack stack, String query) {
        if (query.isEmpty()) {
            return true;
        }

        if (query.startsWith("@")) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().contains(query.substring(1));
        }

        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(Math.max(0, max), value));
    }

    private int statusColour() {
        return switch (menu.status()) {
            case PRINTING -> GREEN;
            case IDLE -> MUTED;
            default -> WARN;
        };
    }

    /** Short totals that fit a slot: 860, 4k, 12k, 1M. Hovering shows the exact item. */
    private static String compact(int value) {
        if (value >= 1_000_000) {
            return value / 1_000_000 + "M";
        }

        return value >= 1_000 ? value / 1_000 + "k" : String.valueOf(value);
    }

    private static String formatTime(int seconds) {
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private void text(GuiGraphics g, Component text, int x, int y, int colour) {
        g.drawString(font, text, leftPos + x, topPos + y, colour, false);
    }

    private void textRight(GuiGraphics g, Component text, int x, int y, int colour) {
        g.drawString(font, text, leftPos + x - font.width(text), topPos + y, colour, false);
    }

    private void textRight(GuiGraphics g, String text, int x, int y, int colour) {
        g.drawString(font, text, leftPos + x - font.width(text), topPos + y, colour, false);
    }

    /** Word-wrapped text, 9 px a line. Returns how many lines it took. */
    private int wrap(GuiGraphics g, Component text, int x, int y, int width, int colour) {
        var lines = font.split(text, width);

        for (int line = 0; line < lines.size(); line++) {
            g.drawString(font, lines.get(line), leftPos + x, topPos + y + line * 9, colour, false);
        }

        return lines.size();
    }

    private void frame(GuiGraphics g, int x0, int y0, int x1, int y1, int border, int fill) {
        g.fill(leftPos + x0, topPos + y0, leftPos + x1, topPos + y1, border);
        g.fill(leftPos + x0 + 1, topPos + y0 + 1, leftPos + x1 - 1, topPos + y1 - 1, fill);
    }

    /** A titled panel: green border, title, rule under it. */
    private void panel(GuiGraphics g, int x, int y, int width, int height, Component title) {
        frame(g, x, y, x + width, y + height, GREEN, PANEL);
        text(g, title, x + 4, y + 2, GREEN);
        g.fill(leftPos + x + 1, topPos + y + 11, leftPos + x + width - 1, topPos + y + 12, GREEN);
    }

    /** A vertical scroll bar beside a grid; the thumb shows where in the list the view is. */
    private void scrollBar(GuiGraphics g, int x, int y, int height, int scroll, int maxScroll) {
        frame(g, x, y, x + 6, y + height, MUTED, WELL);

        if (maxScroll <= 0) {
            return;
        }

        var thumb = Math.max(8, height / (maxScroll + 1));
        var top = y + 1 + (height - 2 - thumb) * Math.min(scroll, maxScroll) / maxScroll;

        g.fill(leftPos + x + 1, topPos + top, leftPos + x + 5, topPos + top + thumb, GREEN);
    }

    /** Full-size label, centred, no shadow; lit = filled green with dark text; disabled = dim. */
    private void button(
        GuiGraphics g,
        Component label,
        int x0,
        int y0,
        int x1,
        int y1,
        boolean active,
        boolean enabled,
        int mouseX,
        int mouseY,
        Runnable action
    ) {
        var hovered = enabled && mouseX - leftPos >= x0 && mouseX - leftPos < x1 && mouseY - topPos >= y0 && mouseY - topPos < y1;
        var border = !enabled ? DIM : (active || hovered) ? GREEN : MUTED;

        frame(g, x0, y0, x1, y1, border, active ? GREEN : hovered ? WELL : PANEL);

        var colour = active ? DARK : enabled ? GREEN : DIM;

        g.drawString(font, label, leftPos + (x0 + x1) / 2 - font.width(label) / 2, topPos + y0 + (y1 - y0 - 8) / 2 + 1, colour, false);

        if (enabled) {
            targets.add(new Target(x0, y0, x1, y1, action));
        }
    }
}
