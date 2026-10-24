package com.human.client.screen;

import com.human.common.gameplay.power.external.CablePortMenu;
import com.human.common.gameplay.power.external.PortMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A cable's port screen: one row per face - what it touches, and its mode as a coloured button (auto grey, out red, in
 * green, both yellow, off dark). Faces that touch nothing foreign are greyed out.
 */
public class CablePortScreen extends AbstractContainerScreen<CablePortMenu> {

    private static final Direction[] FACES = {
        Direction.NORTH,
        Direction.SOUTH,
        Direction.EAST,
        Direction.WEST,
        Direction.UP,
        Direction.DOWN };

    private static final int ROW_Y = 20;

    private static final int ROW_H = 19;

    private static final int BUTTON_X = 116;

    private static final int BUTTON_W = 52;

    public CablePortScreen(CablePortMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = ROW_Y + FACES.length * ROW_H + 8;
        this.inventoryLabelY = 10_000;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        for (var row = 0; row < FACES.length; row++) {
            var face = FACES[row];

            if (menu.hasExternal(face) && overButton(row, mouseX, mouseY)) {
                var mode = menu.getMode(face);

                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    List.of(
                        Component.translatable("gui.avp_human.cable_ports.mode." + mode.id()),
                        Component.translatable("gui.avp_human.cable_ports.mode." + mode.id() + ".desc")
                            .withStyle(net.minecraft.ChatFormatting.GRAY),
                        Component.translatable("gui.avp_human.cable_ports.click").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                    ),
                    mouseX,
                    mouseY
                );
            }
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // A plain vanilla-grey panel.
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF000000);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 2, topPos + 3, 0xFFFFFFFF);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + 3, topPos + imageHeight - 2, 0xFFFFFFFF);
        graphics.fill(leftPos + 3, topPos + imageHeight - 3, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF555555);
        graphics.fill(leftPos + imageWidth - 3, topPos + 3, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF555555);

        var cable = menu.getCablePos();

        for (var row = 0; row < FACES.length; row++) {
            var face = FACES[row];
            var y = topPos + ROW_Y + row * ROW_H;
            var external = menu.hasExternal(face);

            // What the face touches.
            if (minecraft != null && minecraft.level != null) {
                var block = minecraft.level.getBlockState(cable.relative(face)).getBlock();
                var icon = new ItemStack(block.asItem());

                if (external && !icon.isEmpty()) {
                    graphics.renderItem(icon, leftPos + 8, y);
                }
            }

            graphics.drawString(
                font,
                Component.translatable("message.avp_human.direction." + face.getSerializedName()),
                leftPos + 28,
                y + 4,
                external ? 0x404040 : 0x909090,
                false
            );

            if (!external) {
                graphics.drawString(
                    font,
                    Component.translatable("gui.avp_human.cable_ports.none"),
                    leftPos + BUTTON_X,
                    y + 4,
                    0x909090,
                    false
                );
                continue;
            }

            var mode = menu.getMode(face);
            var bx = leftPos + BUTTON_X;
            var hovered = overButton(row, mouseX, mouseY);

            graphics.fill(bx - 1, y - 1, bx + BUTTON_W + 1, y + 17, hovered ? 0xFFFFFFFF : 0xFF373737);
            graphics.fill(bx, y, bx + BUTTON_W, y + 16, mode.colour());
            graphics.drawCenteredString(
                font,
                Component.translatable("gui.avp_human.cable_ports.mode." + mode.id()),
                bx + BUTTON_W / 2,
                y + 4,
                mode == PortMode.OFF ? 0xA0A0A0 : 0xFFFFFF
            );
        }
    }

    private boolean overButton(int row, double mouseX, double mouseY) {
        var bx = leftPos + BUTTON_X;
        var y = topPos + ROW_Y + row * ROW_H;

        return mouseX >= bx && mouseX < bx + BUTTON_W && mouseY >= y && mouseY < y + 16;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (var row = 0; row < FACES.length; row++) {
            var face = FACES[row];

            if (menu.hasExternal(face) && overButton(row, mouseX, mouseY) && minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, face.get3DDataValue());
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
