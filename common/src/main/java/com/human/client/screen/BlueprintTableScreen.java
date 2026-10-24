package com.human.client.screen;

import com.human.HumanResources;
import com.human.common.gameplay.fabrication.BlueprintScanning;
import com.human.common.gameplay.fabrication.BlueprintTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * The blueprint table's screen. MUTHUR colours on the screen area only; frame and inventory stay vanilla.
 * <p>
 * [stated] Oct 16 layout: one CONFIRM button under the line beneath the message window. A valid item in the breakdown
 * slot shows "ITEM WILL BE BROKEN DOWN." and LIGHTS the button; pressing it makes the chip and uses up the item. A
 * written chip shows "CHIP WILL BE COPIED." the same way (1 chip + 2 silicon = 2 chips). Anything else shows why not -
 * "INVALID ITEM. NO RECIPE.", "NOT ENOUGH SILICON." - and the button stays dark. The message IS the warning, so one
 * press is enough (this replaces the Sep 22 two-press arming). Hovering an empty slot says what goes in it.
 * </p>
 */
public class BlueprintTableScreen extends AbstractContainerScreen<BlueprintTableMenu> {

    private static final ResourceLocation TEXTURE = HumanResources.location("textures/gui/container/blueprint_table.png");

    private static final int MINT = 0xFF6EF68D;

    private static final int MID = 0xFF33974F;

    private static final int DIM = 0xFF325D41;

    private static final int INTERIOR = 0xFF0F2618;

    private static final int DARK = 0xFF051008;

    private static final int WARN = 0xFFF0BE5A;

    private static final int BUTTON_X = 58;

    private static final int BUTTON_Y = 52;

    private static final int BUTTON_W = 60;

    private static final int BUTTON_H = 14;

    public BlueprintTableScreen(BlueprintTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
        titleLabelY = 99999;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);

        var status = menu.status();
        var ready = status == BlueprintScanning.Result.OK;
        var key = ready
            ? (menu.isCopy() ? "gui.avp_human.blueprint_table.status.copy" : "gui.avp_human.blueprint_table.status.ok")
            : status.key();

        // An empty slot is not a problem, just a prompt; anything else that stops the button is a warning.
        var colour = ready ? MINT : status == BlueprintScanning.Result.EMPTY ? MID : WARN;

        // The message window: three lines, 8 px apart, inside its frame (x 50-127, y 11-38).
        var lines = font.split(Component.translatable(key), 72);
        var y = 12;

        for (int line = 0; line < lines.size() && line < 3; line++) {
            graphics.drawString(font, lines.get(line), 53, y, colour, false);
            y += 8;
        }

        // CONFIRM, under the line: lit (filled green, dark text) when the item or chip is valid, dark otherwise.
        var hovered = ready && isOverButton(mouseX - leftPos, mouseY - topPos);

        graphics.fill(BUTTON_X, BUTTON_Y, BUTTON_X + BUTTON_W, BUTTON_Y + BUTTON_H, ready ? MINT : DIM);
        graphics.fill(
            BUTTON_X + 1,
            BUTTON_Y + 1,
            BUTTON_X + BUTTON_W - 1,
            BUTTON_Y + BUTTON_H - 1,
            ready ? (hovered ? MID : MINT) : INTERIOR
        );

        var label = Component.translatable("gui.avp_human.blueprint_table.confirm");

        graphics.drawString(font, label, BUTTON_X + (BUTTON_W - font.width(label)) / 2, BUTTON_Y + 3, ready ? DARK : DIM, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverButton((int) mouseX - leftPos, (int) mouseY - topPos) && menu.status() == BlueprintScanning.Result.OK) {
            // The server checks everything again before it touches anything.
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BlueprintTableMenu.SCAN_BUTTON);
            }

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // Hovering an EMPTY input slot says what goes in it.
        if (hoveredSlot != null && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            if (hoveredSlot.index == BlueprintTableMenu.ITEM_SLOT) {
                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    Component.translatable("gui.avp_human.blueprint_table.hint.item"),
                    mouseX,
                    mouseY
                );
            } else if (hoveredSlot.index == BlueprintTableMenu.SILICON_SLOT) {
                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    Component.translatable("gui.avp_human.blueprint_table.hint.silicon"),
                    mouseX,
                    mouseY
                );
            }
        }
    }

    private static boolean isOverButton(int x, int y) {
        return x >= BUTTON_X && x < BUTTON_X + BUTTON_W && y >= BUTTON_Y && y < BUTTON_Y + BUTTON_H;
    }
}
