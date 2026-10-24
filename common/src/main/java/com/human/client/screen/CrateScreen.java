package com.human.client.screen;

import com.human.Human;
import com.human.common.gameplay.storage.CrateInventory;
import com.human.common.gameplay.storage.CrateMenu;
import com.human.common.network.packet.C2SCrateInsertPayload;
import com.human.common.network.packet.C2SCrateTakePayload;
import com.human.common.network.packet.S2CCrateContentsPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A crate's contents as a condensed list - every distinct item once, with its total - rather than slots. Search by name
 * (or {@code @mod} for a mod's items), sort by name, count or mod. Items that come in several versions (worn or
 * enchanted gear) group under one line that expands on click.
 * <p>
 * Click: take a stack. Right-click: take half. Shift-click: a stack into your inventory. Click the list while holding
 * something: put it all in (right-click: one). Shift-click your own inventory: send that stack in.
 */
public class CrateScreen extends AbstractContainerScreen<CrateMenu> {

    private static final int COLUMNS = 9;

    private static final int ROWS = 5;

    private static final int GRID_X = 17;

    private static final int GRID_Y = 32;

    private static final int CELL = 18;

    private static final int PANEL = 0xFF2B2D30;

    private static final int PANEL_EDGE = 0xFF4A4D52;

    private static final int WELL = 0xFF1C1D20;

    private static final int CELL_BG = 0xFF3A3C40;

    private static final int CELL_HOVER = 0xFF5A5E66;

    private enum Sort {
        NAME,
        COUNT,
        MOD
    }

    private @Nullable EditBox search;

    private Sort sort = Sort.NAME;

    private int scrollRow;

    private int builtRevision = -1;

    private String builtQuery = "";

    private final Set<Item> expanded = new HashSet<>();

    private final List<Cell> cells = new ArrayList<>();

    public CrateScreen(CrateMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 196;
        imageHeight = 222;
        inventoryLabelX = CrateMenu.PLAYER_INVENTORY_X;
        inventoryLabelY = CrateMenu.PLAYER_INVENTORY_Y - 11;
        titleLabelX = 8;
        titleLabelY = 5;
    }

    /** Called on the client when the server sends the crate's list. */
    public static void handleContents(S2CCrateContentsPayload payload) {
        var minecraft = Minecraft.getInstance();

        minecraft.execute(() -> {
            if (
                minecraft.player != null && minecraft.player.containerMenu instanceof CrateMenu menu
                    && menu.containerId == payload.containerId()
            ) {
                menu.setClientContents(payload.usedSlots(), payload.totalSlots(), payload.entries());
            }
        });
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + GRID_X, topPos + 16, 110, 12, Component.translatable("gui.avp_human.crate.search"));
        search.setHint(Component.translatable("gui.avp_human.crate.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(text -> scrollRow = 0);
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(sortLabel(), button -> {
            sort = Sort.values()[(sort.ordinal() + 1) % Sort.values().length];
            button.setMessage(sortLabel());
            builtRevision = -1;
        }).bounds(leftPos + GRID_X + 114, topPos + 15, 48, 14).build());
    }

    private Component sortLabel() {
        return Component.translatable("gui.avp_human.crate.sort." + sort.name().toLowerCase(Locale.ROOT));
    }

    // ---- view ----

    /** One cell of the grid: a single stored item, or the header of a group of versions of one item. */
    private record Cell(
        ItemStack icon,
        long count,
        @Nullable CrateInventory.Entry entry,
        Item item,
        int versions,
        boolean expanded
    ) {

        boolean isGroupHeader() {
            return entry == null;
        }
    }

    private void rebuildIfNeeded() {
        var query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);

        if (builtRevision == menu.clientRevision() && query.equals(builtQuery)) {
            return;
        }

        builtRevision = menu.clientRevision();
        builtQuery = query;
        cells.clear();

        var groups = new LinkedHashMap<Item, List<CrateInventory.Entry>>();

        for (var entry : menu.clientEntries()) {
            if (matches(entry.stack(), query)) {
                groups.computeIfAbsent(entry.stack().getItem(), item -> new ArrayList<>()).add(entry);
            }
        }

        var ordered = new ArrayList<>(groups.entrySet());

        ordered.sort(groupOrder());

        for (var group : ordered) {
            var entries = group.getValue();

            if (entries.size() == 1) {
                var entry = entries.get(0);

                cells.add(new Cell(entry.stack(), entry.count(), entry, group.getKey(), 1, false));
                continue;
            }

            var total = entries.stream().mapToLong(CrateInventory.Entry::count).sum();
            var open = expanded.contains(group.getKey());

            cells.add(new Cell(new ItemStack(group.getKey()), total, null, group.getKey(), entries.size(), open));

            if (open) {
                entries.sort(Comparator.comparingLong(CrateInventory.Entry::count).reversed());

                for (var entry : entries) {
                    cells.add(new Cell(entry.stack(), entry.count(), entry, group.getKey(), 1, false));
                }
            }
        }

        scrollRow = Math.min(scrollRow, maxScroll());
    }

    private boolean matches(ItemStack stack, String query) {
        if (query.isEmpty()) {
            return true;
        }

        if (query.startsWith("@")) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().contains(query.substring(1));
        }

        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private Comparator<java.util.Map.Entry<Item, List<CrateInventory.Entry>>> groupOrder() {
        Comparator<java.util.Map.Entry<Item, List<CrateInventory.Entry>>> byName = Comparator.comparing(
            group -> group.getKey().getDescription().getString().toLowerCase(Locale.ROOT)
        );

        return switch (sort) {
            case NAME -> byName;
            case COUNT -> Comparator.<java.util.Map.Entry<Item, List<CrateInventory.Entry>>>comparingLong(
                group -> group.getValue().stream().mapToLong(CrateInventory.Entry::count).sum()
            ).reversed().thenComparing(byName);
            case MOD -> Comparator.<java.util.Map.Entry<Item, List<CrateInventory.Entry>>, String>comparing(
                group -> BuiltInRegistries.ITEM.getKey(group.getKey()).getNamespace()
            ).thenComparing(byName);
        };
    }

    private int totalRows() {
        return (cells.size() + COLUMNS - 1) / COLUMNS;
    }

    private int maxScroll() {
        return Math.max(0, totalRows() - ROWS);
    }

    private int cellIndexAt(double mouseX, double mouseY) {
        var x = (int) Math.floor(mouseX) - leftPos - GRID_X;
        var y = (int) Math.floor(mouseY) - topPos - GRID_Y;

        if (x < 0 || y < 0 || x >= COLUMNS * CELL || y >= ROWS * CELL) {
            return -2;
        }

        var index = (scrollRow + y / CELL) * COLUMNS + x / CELL;

        return index < cells.size() ? index : -1;
    }

    // ---- drawing ----

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        rebuildIfNeeded();
        super.render(graphics, mouseX, mouseY, partialTick);

        var hovered = cellIndexAt(mouseX, mouseY);

        if (hovered >= 0 && menu.getCarried().isEmpty()) {
            com.human.client.screen.widget.GuiText.tooltip(graphics, font, tooltipFor(cells.get(hovered)), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL_EDGE);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL);
        graphics.fill(x + GRID_X - 1, y + GRID_Y - 1, x + GRID_X + COLUMNS * CELL + 1, y + GRID_Y + ROWS * CELL + 1, WELL);

        var hovered = cellIndexAt(mouseX, mouseY);

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                var index = (scrollRow + row) * COLUMNS + column;
                var cx = x + GRID_X + column * CELL;
                var cy = y + GRID_Y + row * CELL;

                graphics.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, index == hovered ? CELL_HOVER : CELL_BG);

                if (index < cells.size()) {
                    drawCell(graphics, cells.get(index), cx + 1, cy + 1);
                }
            }
        }

        // Scrollbar.
        var barX = x + GRID_X + COLUMNS * CELL + 3;
        var barTop = y + GRID_Y;
        var barHeight = ROWS * CELL;

        graphics.fill(barX, barTop, barX + 5, barTop + barHeight, WELL);

        if (maxScroll() > 0) {
            var thumb = Math.max(8, barHeight * ROWS / totalRows());
            var thumbY = barTop + (barHeight - thumb) * scrollRow / maxScroll();

            graphics.fill(barX, thumbY, barX + 5, thumbY + thumb, PANEL_EDGE);
        }

        // Player inventory wells.
        for (var slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, WELL);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, CELL_BG);
        }
    }

    private void drawCell(GuiGraphics graphics, Cell cell, int x, int y) {
        graphics.renderItem(cell.icon(), x, y);

        var text = compact(cell.count());
        var pose = graphics.pose();

        pose.pushPose();
        pose.translate(0.0F, 0.0F, 200.0F);
        pose.scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(font, text, (x + 16) * 2 - font.width(text), (y + 12) * 2, 0xFFFFFF, true);

        if (cell.isGroupHeader()) {
            graphics.drawString(font, cell.expanded() ? "-" : "+", x * 2, y * 2, 0xFFE0A12E, true);
        }

        pose.popPose();
    }

    private List<Component> tooltipFor(Cell cell) {
        var lines = new ArrayList<Component>();

        if (cell.isGroupHeader()) {
            lines.add(cell.icon().getHoverName());
            lines.add(Component.translatable("gui.avp_human.crate.group", cell.versions(), cell.count()).withStyle(ChatFormatting.GRAY));
            lines.add(
                Component.translatable(cell.expanded() ? "gui.avp_human.crate.collapse" : "gui.avp_human.crate.expand")
                    .withStyle(ChatFormatting.DARK_GRAY)
            );

            return lines;
        }

        lines.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(), cell.icon()));
        lines.add(Component.translatable("gui.avp_human.crate.stored", cell.count()).withStyle(ChatFormatting.GOLD));

        return lines;
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE0E0E0, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFB0B0B0, false);

        var used = Component.translatable("gui.avp_human.crate.slots", menu.clientUsedSlots(), menu.clientTotalSlots());

        graphics.drawString(font, used, imageWidth - 8 - font.width(used), inventoryLabelY, 0xFFB0B0B0, false);
    }

    private static String compact(long count) {
        if (count < 1000) {
            return Long.toString(count);
        }

        if (count < 1_000_000) {
            return (count < 10_000 ? String.format(Locale.ROOT, "%.1f", count / 1000.0) : Long.toString(count / 1000)) + "k";
        }

        return (count < 10_000_000 ? String.format(Locale.ROOT, "%.1f", count / 1_000_000.0) : Long.toString(count / 1_000_000)) + "M";
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var index = cellIndexAt(mouseX, mouseY);

        if (index == -2 || (button != 0 && button != 1)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (!menu.getCarried().isEmpty()) {
            Human.MOD.networking().sendToServer(new C2SCrateInsertPayload(menu.containerId, button == 1));

            return true;
        }

        if (index < 0) {
            return true;
        }

        var cell = cells.get(index);

        if (cell.isGroupHeader()) {
            if (!expanded.remove(cell.item())) {
                expanded.add(cell.item());
            }

            builtRevision = -1;

            return true;
        }

        byte action = hasShiftDown() ? CrateMenu.TAKE_TO_INVENTORY : button == 1 ? CrateMenu.TAKE_HALF : CrateMenu.TAKE_STACK;

        Human.MOD.networking().sendToServer(new C2SCrateTakePayload(menu.containerId, action, cell.entry().stack()));

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (cellIndexAt(mouseX, mouseY) != -2) {
            scrollRow = Math.max(0, Math.min(maxScroll(), scrollRow - (int) Math.signum(scrollY)));

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // While typing a search, letters such as E must not close the screen.
        if (search != null && search.isFocused() && keyCode != 256) {
            return search.keyPressed(keyCode, scanCode, modifiers) || search.canConsumeInput();
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
