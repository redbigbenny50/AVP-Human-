package com.human.client.screen;

import com.human.Human;
import com.human.common.gameplay.menu.terminal.DeskTerminalMenu;
import com.human.common.network.packet.C2SSentryViewPayload;
import com.human.common.network.packet.S2CSentryNetworkPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The desk terminal. [stated] Oct 2: a simple screen styled like the cryotube controller's Cryolink - the turrets on
 * the network, one per row; clicking one with a camera lens looks through it. MUTHUR's defence menu grows out of this.
 */
public class DeskTerminalScreen extends AbstractContainerScreen<DeskTerminalMenu> {

    private static final int BG = 0xFF0B1A12;

    private static final int FRAME = 0xFF2E6B3E;

    private static final int MINT = 0xFF6EF68D;

    private static final int DIM = 0xFF3F8F55;

    private static final int ROW_HOVER = 0xFF143322;

    private static final int LIST_X = 8;

    private static final int LIST_Y = 22;

    private static final int ROW_H = 14;

    private static final int ROWS = 9;

    /** Where the coordinates column starts; the name column is cut to fit in front of it. */
    private static final int COORDS_X = LIST_X + 92;

    /** The space left of each column, less a 4 px gap - [stated] Oct 18 "no overlapping text". */
    private static final int NAME_WIDTH = COORDS_X - LIST_X - 4;

    /** Room for the "CAM" tag at the right of a row. */
    private static final int CAM_WIDTH = 22;

    /** The list as last sent. It can arrive either side of the screen opening, so it is kept here. */
    private static List<S2CSentryNetworkPayload.Turret> turrets = List.of();

    private int scroll;

    public DeskTerminalScreen(DeskTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 236;
        this.imageHeight = LIST_Y + ROWS * ROW_H + 22;
    }

    public static void handleNetwork(S2CSentryNetworkPayload payload) {
        turrets = payload.turrets();
    }

    private int maxScroll() {
        return Math.max(0, turrets.size() - ROWS);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        var x = leftPos;
        var y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, FRAME);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, BG);
        graphics.fill(x + 6, y + 18, x + imageWidth - 6, y + 19, FRAME);

        for (int row = 0; row < ROWS && scroll + row < turrets.size(); row++) {
            var turret = turrets.get(scroll + row);
            var ry = y + LIST_Y + row * ROW_H;

            if (turret.camera() && mouseX >= x + LIST_X && mouseX < x + imageWidth - LIST_X && mouseY >= ry && mouseY < ry + ROW_H) {
                graphics.fill(x + LIST_X - 2, ry, x + imageWidth - LIST_X + 2, ry + ROW_H, ROW_HOVER);
            }
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // ⚠ Oct 23 - [stated] the empty-network line "goes off screen": it is 302 px in a 220 px panel. Every line here
        // is now held inside the frame - the sentence WRAPS (same font, same size), the title and the player-named
        // turret names are cut at the frame.
        var inner = imageWidth - 2 * LIST_X;
        graphics.drawString(font, com.human.client.screen.widget.GuiText.fit(font, title, inner), 8, 6, MINT, false);

        if (turrets.isEmpty()) {
            com.human.client.screen.widget.GuiText.drawWrapped(
                graphics,
                font,
                Component.translatable("gui.avp_human.terminal.empty"),
                LIST_X,
                LIST_Y + 2,
                inner,
                ROWS,
                DIM,
                false
            );
        }

        for (int row = 0; row < ROWS && scroll + row < turrets.size(); row++) {
            var turret = turrets.get(scroll + row);
            var ry = LIST_Y + row * ROW_H + 3;
            var pos = BlockPos.of(turret.pos());

            graphics.drawString(
                font,
                com.human.client.screen.widget.GuiText.fit(font, turret.name(), NAME_WIDTH),
                LIST_X,
                ry,
                turret.camera() ? MINT : DIM,
                false
            );
            graphics.drawString(
                font,
                com.human.client.screen.widget.GuiText.fit(
                    font,
                    pos.getX() + " " + pos.getY() + " " + pos.getZ(),
                    imageWidth - LIST_X - CAM_WIDTH - 4 - COORDS_X
                ),
                COORDS_X,
                ry,
                DIM,
                false
            );

            if (turret.camera()) {
                graphics.drawString(
                    font,
                    Component.translatable("gui.avp_human.terminal.camera"),
                    imageWidth - LIST_X - CAM_WIDTH,
                    ry,
                    MINT,
                    false
                );
            }
        }

        graphics.drawString(
            font,
            com.human.client.screen.widget.GuiText.fit(
                font,
                Component.translatable("gui.avp_human.terminal.hint"),
                imageWidth - 2 * LIST_X
            ),
            LIST_X,
            imageHeight - 14,
            DIM,
            false
        );
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int row = 0; row < ROWS && scroll + row < turrets.size(); row++) {
            var turret = turrets.get(scroll + row);
            var ry = topPos + LIST_Y + row * ROW_H;

            if (
                turret.camera() && mouseX >= leftPos + LIST_X && mouseX < leftPos + imageWidth - LIST_X && mouseY >= ry && mouseY < ry
                    + ROW_H
            ) {
                // The server checks everything - ownership, lens, card, power, range - and says why if it refuses.
                Human.MOD.networking().sendToServer(new C2SSentryViewPayload(turret.id()));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll + (scrollY > 0 ? -1 : 1)));
        return true;
    }
}
