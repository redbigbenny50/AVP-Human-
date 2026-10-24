package com.human.client.screen;

import com.human.HumanResources;
import com.human.common.gameplay.menu.workstation.ElectronicsWorkstationMenu;
import com.human.common.gameplay.workstation.ElectronicsCrafting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The electronics workstation's screen, laid out as he described it on Sep 30:
 * <ul>
 * <li>TOP LEFT - the ITEMS TO CRAFT, as slots, 3 x 3, with a scrollbar. Click one to pick it.</li>
 * <li>Under it - the RESULT: take the finished item from here.</li>
 * <li>TOP RIGHT - the PARTS window: one row per part the picked item takes - icon, amount, name - with a scrollbar. Red
 * rows are parts storage does not have enough of. The status line sits under it.</li>
 * <li>STORAGE - two rows of eight; its scrollbar takes the ninth column, inside the window.</li>
 * </ul>
 * All three scrollbars also follow the mouse wheel over their area. The parts list is planned here on the client from
 * the synced storage, by the same code the server crafts with ({@link ElectronicsCrafting}), so it shows exactly what a
 * craft will use.
 * <p>
 * [stated] Oct 16: coloured like the blueprint table - "both are work stations". The vanilla frame and the player's
 * inventory stay grey; everything above them is the MUTHUR green screen, drawn here over the texture in the blueprint
 * table's own colours and slot-well bevel (sampled from its texture). Parts rows show icon and amount only (the full
 * name was cut off in the 72 px window; hovering still names it), short rows turn amber, and the status line sits under
 * the parts window, beside the result. Every text at full size, no shadow. Slot positions and crafting are unchanged.
 * </p>
 */
public class ElectronicsWorkstationScreen extends AbstractContainerScreen<ElectronicsWorkstationMenu> {

    private static final ResourceLocation TEXTURE = HumanResources.location("textures/gui/container/electronics_workstation.png");

    /** A scroll area: its bar's x and y, the bar's height, and the rows it shows. */
    private record Bar(
        int x,
        int y,
        int height
    ) {}

    // Items to craft: 3 x 3 slots.
    private static final int CRAFT_X = 8;

    private static final int CRAFT_Y = 18;

    private static final int CRAFT_COLUMNS = 3;

    private static final int CRAFT_ROWS = 3;

    private static final Bar CRAFT_BAR = new Bar(63, 18, 52);

    // The parts window: 4 rows of 18.
    private static final int PARTS_X = 81;

    private static final int PARTS_Y = 18;

    private static final int PARTS_W = 72;

    private static final int PARTS_ROWS = 4;

    // One pixel left of where it was, so it sits inside the green screen's edge.
    private static final Bar PARTS_BAR = new Bar(156, 18, 70);

    // Under the parts window, beside the result slot: room for the longest message at full size.
    private static final int STATUS_X = 34;

    private static final int STATUS_Y = 94;

    // Storage: its bar in the ninth column.
    private static final Bar STORAGE_BAR = new Bar(153, 114, 34);

    // The blueprint table's screen colours.
    private static final int SCREEN = 0xFF09180F;

    private static final int WELL = 0xFF0F2618;

    private static final int EDGE = 0xFF1A4429;

    private static final int MINT = 0xFF6EF68D;

    private static final int MID = 0xFF33974F;

    private static final int DIM = 0xFF23573A;

    private static final int WARN = 0xFFF0BE5A;

    private static final int WARN_ROW = 0xFF463414;

    /** Where the green screen ends: above the inventory label. */
    private static final int SCREEN_BOTTOM = 151;

    private int craftRow;

    private int partsRow;

    private @Nullable Bar dragging;

    /** The parts list, planned again only when the pick or the storage changes. */
    private @Nullable ElectronicsCrafting.Plan plan;

    private int planKey = Integer.MIN_VALUE;

    private final List<Part> parts = new ArrayList<>();

    private record Part(
        Item item,
        int count,
        int missing
    ) {}

    public ElectronicsWorkstationScreen(ElectronicsWorkstationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 246;
        this.inventoryLabelY = 154;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderOwnTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // The title on the green screen; the inventory label stays vanilla grey on the grey frame.
        graphics.drawString(font, title.getString().toUpperCase(java.util.Locale.ROOT), 9, 8, MINT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        graphics.drawString(
            font,
            Component.translatable(
                "gui.avp_human.workstation.storage.rows",
                menu.getScrollRow() + 1,
                menu.getScrollRow() + ElectronicsWorkstationMenu.VISIBLE_ROWS,
                ElectronicsWorkstationMenu.ROWS
            ),
            9,
            104,
            MINT,
            false
        );

        var status = status();

        graphics.drawString(font, status.text(), STATUS_X, STATUS_Y, status.colour(), false);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        // The green screen over the top of the texture, mint-edged like the blueprint table's.
        graphics.fill(leftPos + 7, topPos + 5, leftPos + imageWidth - 7, topPos + SCREEN_BOTTOM, MINT);
        graphics.fill(leftPos + 8, topPos + 6, leftPos + imageWidth - 8, topPos + SCREEN_BOTTOM - 1, SCREEN);

        // Wells: the nine make cells, the result, the parts window and the storage rows on screen.
        for (var cell = 0; cell < CRAFT_COLUMNS * CRAFT_ROWS; cell++) {
            well(graphics, CRAFT_X + cell % CRAFT_COLUMNS * 18, CRAFT_Y + cell / CRAFT_COLUMNS * 18, 16, 16);
        }

        well(graphics, ElectronicsWorkstationMenu.RESULT_X, ElectronicsWorkstationMenu.RESULT_Y, 16, 16);
        well(graphics, PARTS_X, PARTS_Y, PARTS_W + 1, PARTS_ROWS * 18 - 1);

        for (var row = 0; row < ElectronicsWorkstationMenu.VISIBLE_ROWS; row++) {
            for (var column = 0; column < ElectronicsWorkstationMenu.COLUMNS; column++) {
                well(graphics, ElectronicsWorkstationMenu.STORAGE_X + column * 18, ElectronicsWorkstationMenu.STORAGE_Y + row * 18, 16, 16);
            }
        }

        refreshPlan();
        renderCraftables(graphics, mouseX, mouseY);
        renderParts(graphics);
        renderBar(graphics, CRAFT_BAR, craftRow, maxCraftRow());
        renderBar(graphics, PARTS_BAR, partsRow, maxPartsRow());
        renderBar(graphics, STORAGE_BAR, menu.getScrollRow(), ElectronicsWorkstationMenu.maxScrollRow());
    }

    /** A green scroll bar in a well, the handle mint (dim when there is nothing to scroll), same travel as before. */
    private void renderBar(GuiGraphics graphics, Bar bar, int row, int maxRow) {
        var knob = maxRow > 0 ? (int) ((bar.height() - 15) * (row / (float) maxRow)) : 0;
        var x = leftPos + bar.x();
        var y = topPos + bar.y();

        graphics.fill(x, y, x + 12, y + bar.height(), EDGE);
        graphics.fill(x + 1, y + 1, x + 12, y + bar.height(), MINT);
        graphics.fill(x + 1, y + 1, x + 11, y + bar.height() - 1, WELL);
        graphics.fill(x + 2, y + 1 + knob, x + 10, y + knob + 14, maxRow > 0 ? MINT : DIM);
    }

    /** The blueprint table's slot well: dark edge top and left, mint bottom and right, dark green inside. */
    private void well(GuiGraphics graphics, int x, int y, int width, int height) {
        var left = leftPos + x;
        var top = topPos + y;

        graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, EDGE);
        graphics.fill(left, top, left + width + 1, top + height + 1, MINT);
        graphics.fill(left, top, left + width, top + height, WELL);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Items to craft
    // ------------------------------------------------------------------------------------------------------------

    private void renderCraftables(GuiGraphics graphics, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        var craftables = menu.getCraftables();

        for (var cell = 0; cell < CRAFT_COLUMNS * CRAFT_ROWS; cell++) {
            var index = craftRow * CRAFT_COLUMNS + cell;

            if (index >= craftables.size()) {
                break;
            }

            var x = leftPos + CRAFT_X + cell % CRAFT_COLUMNS * 18;
            var y = topPos + CRAFT_Y + cell / CRAFT_COLUMNS * 18;

            if (index == menu.getSelected()) {
                // The pick: a mint frame round the well.
                graphics.fill(x - 1, y - 1, x + 17, y + 17, MINT);
                graphics.fill(x, y, x + 16, y + 16, DIM);
            }

            graphics.renderItem(craftables.get(index).result(minecraft.level), x, y);

            if (mouseX >= x && mouseY >= y && mouseX < x + 16 && mouseY < y + 16) {
                graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
            }
        }
    }

    private int craftIndexAt(double mouseX, double mouseY) {
        for (var cell = 0; cell < CRAFT_COLUMNS * CRAFT_ROWS; cell++) {
            var index = craftRow * CRAFT_COLUMNS + cell;
            var x = leftPos + CRAFT_X + cell % CRAFT_COLUMNS * 18;
            var y = topPos + CRAFT_Y + cell / CRAFT_COLUMNS * 18;

            if (index < menu.getCraftables().size() && mouseX >= x && mouseY >= y && mouseX < x + 16 && mouseY < y + 16) {
                return index;
            }
        }

        return -1;
    }

    private int maxCraftRow() {
        var rows = (menu.getCraftables().size() + CRAFT_COLUMNS - 1) / CRAFT_COLUMNS;

        return Math.max(0, rows - CRAFT_ROWS);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The parts window
    // ------------------------------------------------------------------------------------------------------------

    private void refreshPlan() {
        var key = 31 * menu.getSelected() + storageHash();

        if (key == planKey) {
            return;
        }

        planKey = key;
        plan = menu.plan();
        parts.clear();

        if (plan != null) {
            // Each part once: how many the craft takes in all, and how many of those storage cannot give.
            for (Map.Entry<Item, Integer> entry : plan.used().entrySet()) {
                var missing = plan.missing().getOrDefault(entry.getKey(), 0);

                parts.add(new Part(entry.getKey(), entry.getValue() + missing, missing));
            }

            for (Map.Entry<Item, Integer> entry : plan.missing().entrySet()) {
                if (!plan.used().containsKey(entry.getKey())) {
                    parts.add(new Part(entry.getKey(), entry.getValue(), entry.getValue()));
                }
            }
        }

        partsRow = Mth.clamp(partsRow, 0, maxPartsRow());
    }

    private int storageHash() {
        var hash = 1;

        for (var stack : menu.storageContents()) {
            hash = 31 * hash + (stack.isEmpty() ? 0 : ItemStack.hashItemAndComponents(stack) * 31 + stack.getCount());
        }

        return hash;
    }

    private void renderParts(GuiGraphics graphics) {
        for (var row = 0; row < PARTS_ROWS; row++) {
            var index = partsRow + row;

            if (index >= parts.size()) {
                break;
            }

            var part = parts.get(index);
            var x = leftPos + PARTS_X;
            var y = topPos + PARTS_Y + row * 18;
            var stack = new ItemStack(part.item());
            var missing = part.missing() > 0;

            if (missing) {
                graphics.fill(x, y, x + PARTS_W, y + 17, WARN_ROW);
            }

            graphics.renderItem(stack, x + 2, y);

            // Icon and amount only - the name is on hover (it never fitted the window).
            graphics.drawString(font, "x" + part.count(), x + 21, y + 4, missing ? WARN : MINT, false);
        }
    }

    private int maxPartsRow() {
        return Math.max(0, parts.size() - PARTS_ROWS);
    }

    private record Status(
        Component text,
        int colour
    ) {}

    private Status status() {
        if (menu.getSelectedCraftable() == null) {
            return new Status(Component.translatable("gui.avp_human.workstation.pick"), MID);
        }

        if (plan == null || !plan.missing().isEmpty()) {
            return new Status(Component.translatable("gui.avp_human.workstation.missing"), WARN);
        }

        if (!plan.fits()) {
            return new Status(Component.translatable("gui.avp_human.workstation.full"), WARN);
        }

        return new Status(Component.translatable("gui.avp_human.workstation.ready"), MINT);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        dragging = null;

        var index = craftIndexAt(mouseX, mouseY);

        if (
            index >= 0 && minecraft != null && minecraft.player != null && minecraft.gameMode != null
                && menu.clickMenuButton(minecraft.player, index)
        ) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
            return true;
        }

        for (var bar : List.of(CRAFT_BAR, PARTS_BAR, STORAGE_BAR)) {
            if (overBar(bar, mouseX, mouseY)) {
                dragging = bar;
                dragTo(bar, mouseY);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging != null) {
            dragTo(dragging, mouseY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean overBar(Bar bar, double mouseX, double mouseY) {
        var x = leftPos + bar.x();
        var y = topPos + bar.y();

        return mouseX >= x && mouseX < x + 12 && mouseY >= y && mouseY < y + bar.height();
    }

    private void dragTo(Bar bar, double mouseY) {
        var fraction = ((float) mouseY - topPos - bar.y() - 7.5F) / (bar.height() - 15.0F);

        if (bar == CRAFT_BAR) {
            craftRow = Mth.clamp(Math.round(fraction * maxCraftRow()), 0, maxCraftRow());
        } else if (bar == PARTS_BAR) {
            partsRow = Mth.clamp(Math.round(fraction * maxPartsRow()), 0, maxPartsRow());
        } else {
            menu.setScrollRow(Math.round(fraction * ElectronicsWorkstationMenu.maxScrollRow()));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var step = scrollY > 0 ? -1 : scrollY < 0 ? 1 : 0;

        if (inArea(mouseX, mouseY, CRAFT_X - 1, CRAFT_Y - 1, CRAFT_BAR.x() + 13, CRAFT_Y + CRAFT_ROWS * 18)) {
            craftRow = Mth.clamp(craftRow + step, 0, maxCraftRow());
            return true;
        }

        if (inArea(mouseX, mouseY, PARTS_X - 2, PARTS_Y - 1, PARTS_BAR.x() + 13, PARTS_Y + PARTS_ROWS * 18)) {
            partsRow = Mth.clamp(partsRow + step, 0, maxPartsRow());
            return true;
        }

        if (inArea(mouseX, mouseY, 7, STORAGE_BAR.y() - 1, STORAGE_BAR.x() + 13, STORAGE_BAR.y() + STORAGE_BAR.height() + 2)) {
            menu.setScrollRow(menu.getScrollRow() + step);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean inArea(double mouseX, double mouseY, int x0, int y0, int x1, int y1) {
        return mouseX >= leftPos + x0 && mouseX < leftPos + x1 && mouseY >= topPos + y0 && mouseY < topPos + y1;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Tooltips
    // ------------------------------------------------------------------------------------------------------------

    private void renderOwnTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        var index = craftIndexAt(mouseX, mouseY);

        if (index >= 0) {
            graphics.renderTooltip(font, menu.getCraftables().get(index).result(minecraft.level), mouseX, mouseY);
            return;
        }

        for (var row = 0; row < PARTS_ROWS; row++) {
            var part = partsRow + row;
            var y = topPos + PARTS_Y + row * 18;

            if (part < parts.size() && inArea(mouseX, mouseY, PARTS_X - 1, y - topPos - 1, PARTS_X + PARTS_W, y - topPos + 17)) {
                var entry = parts.get(part);
                var lines = new ArrayList<Component>();

                lines.add(new ItemStack(entry.item()).getHoverName());
                lines.add(Component.translatable("gui.avp_human.workstation.material.uses", entry.count()).withStyle(ChatFormatting.GRAY));

                if (entry.missing() > 0) {
                    lines.add(
                        Component.translatable("gui.avp_human.workstation.material.missing", entry.missing()).withStyle(ChatFormatting.RED)
                    );
                }

                com.human.client.screen.widget.GuiText.tooltip(graphics, font, lines, mouseX, mouseY);
                return;
            }
        }
    }
}
