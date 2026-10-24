package com.human.client.screen;

import com.human.HumanResources;
import com.human.client.fluid.FluidClientInfo;
import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.item.canister.UniversalCanisterItem;
import com.human.common.gameplay.menu.machine.ElectricMachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The screen for every tier-1 cooking machine.
 * <h2>LAYOUT</h2> The main panel is his art, 176x166, one texture per machine. The lightning bolt fills from the bottom
 * as an item cooks, using his {@code indicator} sprite laid exactly over the bolt's grey interior at (83, 35) -
 * measured from the art, the same spot in all four.
 * <h2>THE LEFT PANEL</h2> [stated] Sep 29: "all machines should have a filter option on the left of the gui ... an item
 * filter ... and a priority". His GUI art has no panel for it, so it is drawn here in the same vanilla grey, joined to
 * the left edge:
 * <ul>
 * <li>the PRIORITY button on top - one, two or three bars for deferrable, default and critical, click to cycle</li>
 * <li>four FILTER slots below - click with an item to add it, click empty-handed to clear. Empty filter =
 * anything.</li>
 * <li>[stated] readme - the press only: its two KEEP switches below the filter, item output and fluid output. A red
 * cross means that output is being thrown away.</li>
 * </ul>
 * <h2>THE TANK</h2> The press, hydrator and freezer draw their tank where his art puts it
 * ({@link com.human.common.gameplay.machine.ElectricMachineKind.Tank}), filling from the bottom. A fluid he drew an
 * overlay for uses it ({@code textures/gui/container/fluid/<machine>_<fluid>_overlay.png} - water, lava, honey, latex);
 * any other fluid, from any mod, is drawn from its own texture and tint.
 */
public class ElectricMachineScreen extends AbstractContainerScreen<ElectricMachineMenu> {

    private static final ResourceLocation INDICATOR = HumanResources.location("textures/gui/container/machine_indicator.png");

    /**
     * [stated] Oct 2: empty side-panel slots show what they are for, in the same line-art style as the sentry GUI's
     * slot symbols - drawn only while the slot is empty, like vanilla's empty armour slots.
     */
    private static final ResourceLocation HUB_SYMBOL = HumanResources.location("textures/gui/container/slot/hub_chip.png");

    /** Where the bolt is comes from the machine's layout - it moves between machines on his GUI art. */
    private int indicatorX() {
        return menu.getKind().layout().indicatorX();
    }

    private int indicatorY() {
        return menu.getKind().layout().indicatorY();
    }

    private static final int INDICATOR_W = 11;

    private static final int INDICATOR_H = 15;

    private static final int PANEL_X = -26;

    private static final int PANEL_W = 28;

    private static final int PANEL_Y = 4;

    /** Which hand-drawn overlays exist - looked up once each. */
    private static final Map<ResourceLocation, Boolean> OVERLAY_EXISTS = new HashMap<>();

    private static final int BG = 0xFFC6C6C6;

    private static final int LIGHT = 0xFFFFFFFF;

    private static final int SHADOW = 0xFF555555;

    private static final int OUTLINE = 0xFF000000;

    private static final int SLOT_DARK = 0xFF373737;

    private static final int SLOT_FILL = 0xFF8B8B8B;

    /**
     * [stated] Oct 2: the machine's own area is a MUTHUR "screen" the player looks at (the Cryolink palette, carried by
     * the textures); the title sits on it, so it is drawn in the screen's mint. Everything else - the frame, the side
     * panel with its slots, and the player inventory - stays vanilla.
     */
    private static final int SCREEN_MINT = 0xFF6EF68D;

    private final ResourceLocation texture;

    /** [stated] Oct 2: priority, filter, redstone and the press outputs - side buttons with pop-up sub-menus. */
    /** Protected so a machine's own screen (the distributor) can respect an open pop-up before handling its clicks. */
    protected com.human.client.screen.widget.MachineSideControls controls;

    public ElectricMachineScreen(ElectricMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.texture = HumanResources.location("textures/gui/container/" + menu.getKind().guiTexture() + ".png");
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        controls = new com.human.client.screen.widget.MachineSideControls(menu, font);
    }

    /**
     * The title sits on the machine's screen, so it is drawn in the screen's mint. [stated] Oct 2 the inventory label
     * is left out: the screen runs down to the inventory, and the label already overlapped the press's and hydrator's
     * pipes.
     */
    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, SCREEN_MINT, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        controls.layout(leftPos, topPos, PANEL_X, PANEL_Y);
        super.render(graphics, mouseX, mouseY, partialTick);
        controls.render(graphics, mouseX, mouseY);

        // Over the side controls (or anywhere while a pop-up is open) only their tooltip shows, never a slot's.
        if (controls.isOpen() || controls.isMouseOver(mouseX, mouseY)) {
            controls.renderTooltip(graphics, mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
            renderPanelTooltips(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        var x = leftPos;
        var y = topPos;

        drawPanel(graphics, x, y);
        graphics.blit(texture, x, y, 0, 0, imageWidth, imageHeight);
        drawSlotSymbols(graphics, x, y);
        drawTank(graphics, x, y);

        var progress = menu.getProgress();

        if (progress > 0.0F) {
            // Fills from the bottom of the bolt upward.
            var filled = Math.max(1, Math.round(progress * INDICATOR_H));
            var skipped = INDICATOR_H - filled;

            graphics.blit(
                INDICATOR,
                x + indicatorX(),
                y + indicatorY() + skipped,
                0.0F,
                skipped,
                INDICATOR_W,
                filled,
                INDICATOR_W,
                INDICATOR_H
            );
        }
    }

    private void drawPanel(GuiGraphics graphics, int x, int y) {
        var left = x + PANEL_X;
        var top = y + PANEL_Y;
        var right = left + PANEL_W;
        var bottom = top + panelHeight();

        // A vanilla-style raised panel tucked behind the main art's left edge.
        graphics.fill(left, top, right, bottom, OUTLINE);
        graphics.fill(left + 1, top + 1, right, bottom - 1, BG);
        graphics.fill(left + 1, top + 1, right, top + 2, LIGHT);
        graphics.fill(left + 1, top + 1, left + 2, bottom - 1, LIGHT);
        graphics.fill(left + 2, bottom - 2, right, bottom - 1, SHADOW);

        // [stated] Oct 2: the side panel holds the four upgrade slots, one card each.
        for (var index = 0; index < ElectricMachineBlockEntity.UPGRADE_SLOTS; index++) {
            drawWell(graphics, x + ElectricMachineMenu.FILTER_X - 1, y + ElectricMachineMenu.upgradeSlotY(menu.getKind(), index) - 1, 18);
        }
    }

    /** A filter funnel in each empty filter slot, the hub chip outline in an empty hub slot. */
    private void drawSlotSymbols(GuiGraphics graphics, int x, int y) {
        for (var slot : menu.slots) {
            if (slot.hasItem()) {
                continue;
            }

            ResourceLocation symbol = null;

            if (menu.isUpgradeSlot(slot)) {
                symbol = HUB_SYMBOL;
            }

            if (symbol != null) {
                graphics.blit(symbol, x + slot.x, y + slot.y, 0.0F, 0.0F, 16, 16, 16, 16);
            }
        }
    }

    /** Tall enough for the four upgrade slots. */
    private int panelHeight() {
        return ElectricMachineMenu.upgradeSlotY(menu.getKind(), ElectricMachineBlockEntity.UPGRADE_SLOTS - 1) + 17 + 4 - PANEL_Y;
    }

    /** The tank, filled from the bottom - his overlay for the fluid if he drew one, else the fluid's own texture. */
    private void drawTank(GuiGraphics graphics, int x, int y) {
        var tank = menu.getKind().tank();
        var content = menu.getFluid();

        if (tank == null || content.isEmpty()) {
            return;
        }

        var filled = Math.max(1, Math.min(tank.gaugeH(), Math.round(content.amount() / (float) tank.capacity() * tank.gaugeH())));
        var skipped = tank.gaugeH() - filled;
        var gx = x + tank.gaugeX();
        var gy = y + tank.gaugeY();
        var overlay = overlayFor(tank.overlay(), content);

        if (overlay != null) {
            graphics.blit(overlay, gx, gy + skipped, 0.0F, skipped, tank.gaugeW(), filled, tank.gaugeW(), tank.gaugeH());
            return;
        }

        var still = FluidClientInfo.Holder.get().stillTexture(content.fluid());

        if (still == null || minecraft == null) {
            return;
        }

        var sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        var tint = FluidClientInfo.Holder.get().tint(content.fluid());
        var red = (tint >> 16 & 0xFF) / 255.0F;
        var green = (tint >> 8 & 0xFF) / 255.0F;
        var blue = (tint & 0xFF) / 255.0F;

        // Whole 16px tiles from the bottom up; the top tile is squeezed into what is left.
        var bottom = gy + tank.gaugeH();
        var top = gy + skipped;

        for (var tileBottom = bottom; tileBottom > top; tileBottom -= 16) {
            var tileTop = Math.max(top, tileBottom - 16);

            graphics.blit(gx, tileTop, 0, tank.gaugeW(), tileBottom - tileTop, sprite, red, green, blue, 1.0F);
        }
    }

    private static @org.jetbrains.annotations.Nullable ResourceLocation overlayFor(String machine, FluidContent content) {
        var fluidName = BuiltInRegistries.FLUID.getKey(content.fluid()).getPath();
        var location = HumanResources.location("textures/gui/container/fluid/" + machine + "_" + fluidName + "_overlay.png");

        var exists = OVERLAY_EXISTS.computeIfAbsent(
            location,
            key -> net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(key).isPresent()
        );

        return exists ? location : null;
    }

    private static void drawWell(GuiGraphics graphics, int x, int y, int size) {
        graphics.fill(x, y, x + size, y + size, LIGHT);
        graphics.fill(x, y, x + size - 1, y + size - 1, SLOT_DARK);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, SLOT_FILL);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (controls.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Esc closes an open pop-up rather than the whole screen. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return controls.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Clicks on the left panel or the side controls are not clicks outside the window - without this, clicking them
     * would drop the held item.
     */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        var inPanel = mouseX >= guiLeft + PANEL_X && mouseX < guiLeft && mouseY >= guiTop + PANEL_Y && mouseY < guiTop + PANEL_Y
            + panelHeight();

        return !inPanel && !controls.isMouseOver(mouseX, mouseY) && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
    }

    private void renderPanelTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        var tank = menu.getKind().tank();

        if (tank != null && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            var tx = leftPos + tank.gaugeX();
            var ty = topPos + tank.gaugeY();

            if (mouseX >= tx && mouseX < tx + tank.gaugeW() && mouseY >= ty && mouseY < ty + tank.gaugeH()) {
                var content = menu.getFluid();
                var capacity = String.format(java.util.Locale.ROOT, "%,d", tank.capacity());

                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    content.isEmpty()
                        ? List.of(
                            Component.translatable("gui.avp_human.machine.tank.empty"),
                            Component.translatable("gui.avp_human.machine.tank.amount", 0, capacity).withStyle(ChatFormatting.GRAY)
                        )
                        : List.of(
                            UniversalCanisterItem.fluidName(content.fluid()),
                            Component.translatable(
                                "gui.avp_human.machine.tank.amount",
                                String.format(java.util.Locale.ROOT, "%,d", content.amount()),
                                capacity
                            ).withStyle(ChatFormatting.GRAY)
                        ),
                    mouseX,
                    mouseY
                );
                return;
            }
        }

        if (hoveredSlot != null && menu.isFillSlot(hoveredSlot) && !hoveredSlot.hasItem()) {
            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                List.of(
                    Component.translatable("gui.avp_human.machine.fill"),
                    Component.translatable("gui.avp_human.machine.fill.desc").withStyle(ChatFormatting.GRAY)
                ),
                mouseX,
                mouseY
            );
            return;
        }

        if (hoveredSlot != null && menu.isUpgradeSlot(hoveredSlot) && !hoveredSlot.hasItem()) {
            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                List.of(
                    Component.translatable("gui.avp_human.machine.upgrade"),
                    Component.translatable("gui.avp_human.machine.upgrade.desc").withStyle(ChatFormatting.GRAY)
                ),
                mouseX,
                mouseY
            );
            return;
        }

        var ix = leftPos + indicatorX();
        var iy = topPos + indicatorY();

        if (mouseX >= ix && mouseX < ix + INDICATOR_W && mouseY >= iy && mouseY < iy + INDICATOR_H) {
            var status = switch (menu.getStatus()) {
                case ElectricMachineBlockEntity.STATUS_DISABLED -> "gui.avp_human.machine.status.disabled";
                case ElectricMachineBlockEntity.STATUS_WORKING -> "gui.avp_human.machine.status.working";
                case ElectricMachineBlockEntity.STATUS_IDLE -> "gui.avp_human.machine.status.idle";
                default -> "gui.avp_human.machine.status.no_power";
            };

            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                List.of(
                    Component.translatable(status),
                    // After its cards - speed raises the draw, efficiency lowers it.
                    Component.translatable("gui.avp_human.machine.power", menu.getDrawPerTick())
                        .withStyle(ChatFormatting.GRAY)
                ),
                mouseX,
                mouseY
            );
        }
    }
}
