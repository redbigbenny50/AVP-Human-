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
 */
public class ElectronicsWorkstationScreen extends AbstractContainerScreen<ElectronicsWorkstationMenu> {

    private static final ResourceLocation TEXTURE = HumanResources.location("textures/gui/container/electronics_workstation.png");

    private static final ResourceLocation SCROLLER = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller");

    private static final ResourceLocation SCROLLER_DISABLED = ResourceLocation.withDefaultNamespace(
        "container/stonecutter/scroller_disabled"
    );

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

    private static final Bar PARTS_BAR = new Bar(157, 18, 70);

    private static final int STATUS_X = 80;

    private static final int STATUS_Y = 92;

    // Storage: its bar in the ninth column.
    private static final Bar STORAGE_BAR = new Bar(153, 114, 34);

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
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(
            font,
            Component.translatable(
                "gui.avp_human.workstation.storage.rows",
                menu.getScrollRow() + 1,
                menu.getScrollRow() + ElectronicsWorkstationMenu.VISIBLE_ROWS,
                ElectronicsWorkstationMenu.ROWS
            ),
            8,
            104,
            0x404040,
            false
        );

        var status = status();

        graphics.drawString(font, status.text(), STATUS_X, STATUS_Y, status.colour(), false);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        refreshPlan();
        renderCraftables(graphics, mouseX, mouseY);
        renderParts(graphics);
        renderBar(graphics, CRAFT_BAR, craftRow, maxCraftRow());
        renderBar(graphics, PARTS_BAR, partsRow, maxPartsRow());
        renderBar(graphics, STORAGE_BAR, menu.getScrollRow(), ElectronicsWorkstationMenu.maxScrollRow());
    }

    private void renderBar(GuiGraphics graphics, Bar bar, int row, int maxRow) {
        var knob = maxRow > 0 ? (int) ((bar.height() - 15) * (row / (float) maxRow)) : 0;

        graphics.blitSprite(maxRow > 0 ? SCROLLER : SCROLLER_DISABLED, leftPos + bar.x(), topPos + bar.y() + knob, 12, 15);
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
                // The pick: a green frame inside the slot.
                graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF3FAF3F);
                graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
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
                graphics.fill(x - 1, y - 1, x + PARTS_W, y + 17, 0x40D03030);
            }

            graphics.renderItem(stack, x, y);

            var text = part.count() + " " + stack.getHoverName().getString();
            var fitted = font.plainSubstrByWidth(text, PARTS_W - 20);

            if (fitted.length() < text.length()) {
                fitted = font.plainSubstrByWidth(text, PARTS_W - 26) + "..";
            }

            graphics.drawString(font, fitted, x + 18, y + 4, missing ? 0xFFFF5555 : 0xFFFFFFFF, true);
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
            return new Status(Component.translatable("gui.avp_human.workstation.pick"), 0x404040);
        }

        if (plan == null || !plan.missing().isEmpty()) {
            return new Status(Component.translatable("gui.avp_human.workstation.missing"), 0xB02020);
        }

        if (!plan.fits()) {
            return new Status(Component.translatable("gui.avp_human.workstation.full"), 0xB02020);
        }

        return new Status(Component.translatable("gui.avp_human.workstation.ready"), 0x207020);
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

                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
                return;
            }
        }
    }
}
