package com.human.client.screen;

import com.human.HumanResources;
import com.human.common.gameplay.fabrication.DriveBayBlockEntity;
import com.human.common.gameplay.fabrication.DriveBayMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/** The drive bay's screen. MUTHUR colours on the bay's panel; the frame and the player inventory stay vanilla. */
public class DriveBayScreen extends AbstractContainerScreen<DriveBayMenu> {

    private static final ResourceLocation TEXTURE = HumanResources.location("textures/gui/container/drive_bay.png");

    private static final int MINT = 0xFF6EF68D;

    private static final int MID = 0xFF33974F;

    public DriveBayScreen(DriveBayMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 192;
        imageHeight = 250;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Grey out every blueprint slot past the bay's capacity, from the bottom up.
        var capacity = menu.capacity();

        for (int slot = capacity; slot < DriveBayBlockEntity.BLUEPRINT_SLOTS; slot++) {
            var x = leftPos + DriveBayMenu.GRID_X + (slot % DriveBayMenu.GRID_COLUMNS) * 18;
            var y = topPos + DriveBayMenu.TOP_Y + (slot / DriveBayMenu.GRID_COLUMNS) * 18;

            graphics.blit(TEXTURE, x, y, 200, 0, 16, 16);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 7, MINT, false);

        var capacity = Component.translatable(
            "gui.avp_human.drive_bay.capacity",
            menu.capacity(),
            DriveBayBlockEntity.BLUEPRINT_SLOTS
        );

        graphics.drawString(font, capacity, imageWidth - 10 - font.width(capacity), 7, MID, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // A greyed slot explains itself, so nobody wonders why a blueprint will not go in.
        if (hoveredSlot != null && !hoveredSlot.hasItem()) {
            var index = hoveredSlot.index - DriveBayBlockEntity.SSD_SLOTS;

            if (index >= menu.capacity() && index < DriveBayBlockEntity.BLUEPRINT_SLOTS) {
                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    Component.translatable("gui.avp_human.drive_bay.greyed"),
                    mouseX,
                    mouseY
                );
            }
        }
    }
}
