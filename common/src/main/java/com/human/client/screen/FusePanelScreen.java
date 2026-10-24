package com.human.client.screen;

import com.human.HumanResources;
import com.human.common.gameplay.power.fuse.FusePanelBlockEntity;
import com.human.common.gameplay.power.fuse.FusePanelMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The fuse panel's screen: its bound devices - icon, name and position, red when out of range - each with an unbind
 * button; the four capacity-card slots; the device count; and the priority button. Bind devices with the wiring tool.
 */
public class FusePanelScreen extends AbstractContainerScreen<FusePanelMenu> {

    private static final ResourceLocation TEXTURE = HumanResources.location("textures/gui/container/fuse_panel.png");

    private static final ResourceLocation SCROLLER = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller");

    private static final ResourceLocation SCROLLER_DISABLED = ResourceLocation.withDefaultNamespace(
        "container/stonecutter/scroller_disabled"
    );

    private static final int LIST_X = 8;

    private static final int LIST_Y = 18;

    private static final int LIST_W = 144;

    private static final int ROWS = 5;

    private static final int BAR_X = 156;

    private static final int BAR_Y = 18;

    private static final int BAR_H = 90;

    private static final int UNBIND_W = 10;

    private static final int PRIORITY_X = 152;

    private static final int PRIORITY_Y = 115;

    private int row;

    private boolean dragging;

    public FusePanelScreen(FusePanelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 130;
    }

    /** One row: where it is, and whether it is a turret (drawn with the turret's icon). */
    private record Row(
        BlockPos pos,
        boolean sentry
    ) {}

    /** Block devices first, then turrets - the same order the menu's unbind buttons count in. */
    private List<Row> devices() {
        var panel = minecraft == null || minecraft.level == null ? null : menu.clientPanel(minecraft.level);

        if (panel == null) {
            return List.of();
        }

        var rows = new java.util.ArrayList<Row>(panel.getBoundCount());

        for (var pos : panel.getDevices()) {
            rows.add(new Row(pos, false));
        }

        for (var sentry : panel.getSentries()) {
            rows.add(new Row(sentry.pos(), true));
        }

        return rows;
    }

    private int maxRow() {
        return Math.max(0, devices().size() - ROWS);
    }

    /** [stated] Oct 2: the standard Priority side button, beside the panel's own priority bars. */
    private com.human.client.screen.widget.OptionSideControls controls;

    @Override
    protected void init() {
        super.init();

        controls = new com.human.client.screen.widget.OptionSideControls(
            menu.containerId,
            font,
            List.of(
                new com.human.client.screen.widget.OptionSideControls.Field(
                    Component.translatable("gui.avp_human.machine.side.priority"),
                    () -> true,
                    (graphics, x, y) -> drawBars(graphics, x, y, menu.getPriority()),
                    this::priorityOptions
                )
            )
        );
    }

    private List<com.human.client.screen.widget.OptionSideControls.Option> priorityOptions() {
        var options = new java.util.ArrayList<com.human.client.screen.widget.OptionSideControls.Option>();

        for (var priority : com.human.common.gameplay.machine.MachinePriority.values()) {
            options.add(
                new com.human.client.screen.widget.OptionSideControls.Option(
                    Component.translatable("gui.avp_human.machine.priority." + priority.id()),
                    menu.getPriority() == priority,
                    FusePanelMenu.BUTTON_SET_PRIORITY + priority.ordinal()
                )
            );
        }

        return options;
    }

    /** One to three bars, coloured by priority - the same as the panel's own indicator and the machines'. */
    private static void drawBars(GuiGraphics graphics, int x, int y, com.human.common.gameplay.machine.MachinePriority priority) {
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

        for (var bar = 0; bar < 3; bar++) {
            var height = 4 + bar * 3;
            var barX = x + 3 + bar * 4;
            var bottom = y + 14;

            graphics.fill(barX, bottom - height, barX + 3, bottom, bar < bars ? colour : 0xFF6A6A6A);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        controls.layout(leftPos, topPos);
        super.render(graphics, mouseX, mouseY, partialTick);
        controls.render(graphics, mouseX, mouseY);

        if (controls.isOpen() || controls.isMouseOver(mouseX, mouseY)) {
            controls.renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        renderTooltip(graphics, mouseX, mouseY);
        renderOwnTooltips(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return controls.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Clicks on the side controls are not clicks outside the window - they must not drop the held item. */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        return !controls.isMouseOver(mouseX, mouseY) && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        var count = devices().size();
        var full = count >= menu.getCapacity();

        graphics.drawString(
            font,
            Component.translatable("gui.avp_human.fuse_panel.devices", count, menu.getCapacity()),
            82,
            121,
            full ? 0xB02020 : 0x404040,
            false
        );
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        var devices = devices();
        row = Mth.clamp(row, 0, maxRow());

        if (devices.isEmpty()) {
            graphics.drawCenteredString(
                font,
                Component.translatable("gui.avp_human.fuse_panel.empty"),
                leftPos + LIST_X + LIST_W / 2,
                topPos + LIST_Y + 38,
                0xE0E0E0
            );
        }

        var panelPos = menu.getPanelPos();

        for (var visible = 0; visible < ROWS; visible++) {
            var index = row + visible;

            if (index >= devices.size()) {
                break;
            }

            var entry = devices.get(index);
            var pos = entry.pos();
            var x = leftPos + LIST_X;
            var y = topPos + LIST_Y + visible * 18;
            var inRange = pos.distSqr(panelPos) <= (double) FusePanelBlockEntity.RANGE * FusePanelBlockEntity.RANGE;
            var state = minecraft != null && minecraft.level != null ? minecraft.level.getBlockState(pos) : null;
            var icon = entry.sentry()
                ? new ItemStack(com.human.common.registry.init.HumanBlocks.SENTRY_TURRET.get())
                : state == null ? ItemStack.EMPTY : new ItemStack(state.getBlock().asItem());

            if (!inRange) {
                graphics.fill(x, y, x + LIST_W, y + 18, 0x60D03030);
            }

            graphics.renderItem(icon, x + 1, y + 1);

            var name = icon.isEmpty() ? Component.translatable("gui.avp_human.fuse_panel.unknown") : icon.getHoverName();

            graphics.drawString(font, font.plainSubstrByWidth(name.getString(), LIST_W - 36), x + 20, y + 1, 0xFFFFFF, true);
            // ⚠ Oct 23 - cut at the same width as the name above it: far-out coordinates ran past the row.
            graphics.drawString(
                font,
                com.human.client.screen.widget.GuiText.fit(font, pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), LIST_W - 36),
                x + 20,
                y + 10,
                0xC0C0C0,
                false
            );

            // Unbind: a small red cross at the row's end.
            var bx = x + LIST_W - UNBIND_W - 2;
            var by = y + 4;
            var hovered = mouseX >= bx && mouseX < bx + UNBIND_W && mouseY >= by && mouseY < by + UNBIND_W;

            graphics.fill(bx, by, bx + UNBIND_W, by + UNBIND_W, hovered ? 0xFF803030 : 0xFF503030);

            for (var step = 1; step < UNBIND_W - 1; step++) {
                graphics.fill(bx + step, by + step, bx + step + 1, by + step + 1, 0xFFFF6060);
                graphics.fill(bx + UNBIND_W - 1 - step, by + step, bx + UNBIND_W - step, by + step + 1, 0xFFFF6060);
            }
        }

        var max = maxRow();
        var knob = max > 0 ? (int) ((BAR_H - 15) * (row / (float) max)) : 0;

        graphics.blitSprite(max > 0 ? SCROLLER : SCROLLER_DISABLED, leftPos + BAR_X, topPos + BAR_Y + knob, 12, 15);

        // Priority: one to three bars, as on the machines.
        var bars = switch (menu.getPriority()) {
            case CRITICAL -> 3;
            case DEFAULT -> 2;
            case DEFERRABLE -> 1;
        };
        var colour = switch (menu.getPriority()) {
            case CRITICAL -> 0xFFD04040;
            case DEFAULT -> 0xFFE0C040;
            case DEFERRABLE -> 0xFF60B060;
        };

        for (var bar = 0; bar < 3; bar++) {
            var height = 4 + bar * 3;
            var barX = leftPos + PRIORITY_X + 3 + bar * 4;
            var bottom = topPos + PRIORITY_Y + 14;

            graphics.fill(barX, bottom - height, barX + 3, bottom, bar < bars ? colour : 0xFF6A6A6A);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        dragging = false;

        if (controls.mouseClicked(mouseX, mouseY)) {
            return true;
        }

        if (minecraft != null && minecraft.player != null && minecraft.gameMode != null) {
            var devices = devices();

            for (var visible = 0; visible < ROWS && row + visible < devices.size(); visible++) {
                var bx = leftPos + LIST_X + LIST_W - UNBIND_W - 2;
                var by = topPos + LIST_Y + visible * 18 + 4;

                if (mouseX >= bx && mouseX < bx + UNBIND_W && mouseY >= by && mouseY < by + UNBIND_W) {
                    click(row + visible);
                    return true;
                }
            }

            if (inArea(mouseX, mouseY, PRIORITY_X, PRIORITY_Y, 16, 16)) {
                click(FusePanelMenu.BUTTON_CYCLE_PRIORITY);
                return true;
            }

            if (inArea(mouseX, mouseY, BAR_X, BAR_Y, 12, BAR_H) && maxRow() > 0) {
                dragging = true;
                dragTo(mouseY);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void click(int id) {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }

        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            dragTo(mouseY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void dragTo(double mouseY) {
        var fraction = ((float) mouseY - topPos - BAR_Y - 7.5F) / (BAR_H - 15.0F);

        row = Mth.clamp(Math.round(fraction * maxRow()), 0, maxRow());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inArea(mouseX, mouseY, LIST_X - 1, LIST_Y - 1, BAR_X + 13 - LIST_X, BAR_H + 2)) {
            row = Mth.clamp(row + (scrollY > 0 ? -1 : 1), 0, maxRow());
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean inArea(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    private void renderOwnTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (inArea(mouseX, mouseY, PRIORITY_X, PRIORITY_Y, 16, 16)) {
            var priority = menu.getPriority();

            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                List.of(
                    Component.translatable("gui.avp_human.machine.priority." + priority.id()),
                    Component.translatable("gui.avp_human.machine.priority." + priority.id() + ".desc").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.avp_human.machine.priority.cycle").withStyle(ChatFormatting.DARK_GRAY)
                ),
                mouseX,
                mouseY
            );
            return;
        }

        var devices = devices();

        for (var visible = 0; visible < ROWS && row + visible < devices.size(); visible++) {
            var bx = leftPos + LIST_X + LIST_W - UNBIND_W - 2;
            var by = topPos + LIST_Y + visible * 18 + 4;

            if (mouseX >= bx && mouseX < bx + UNBIND_W && mouseY >= by && mouseY < by + UNBIND_W) {
                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    Component.translatable("gui.avp_human.fuse_panel.unbind"),
                    mouseX,
                    mouseY
                );
                return;
            }

            var y = topPos + LIST_Y + visible * 18;
            var pos = devices.get(row + visible).pos();

            if (
                mouseX >= leftPos + LIST_X && mouseX < leftPos + LIST_X + LIST_W && mouseY >= y && mouseY < y + 18
                    && pos.distSqr(menu.getPanelPos()) > (double) FusePanelBlockEntity.RANGE * FusePanelBlockEntity.RANGE
            ) {
                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    Component.translatable("gui.avp_human.fuse_panel.out_of_range"),
                    mouseX,
                    mouseY
                );
                return;
            }
        }
    }
}
