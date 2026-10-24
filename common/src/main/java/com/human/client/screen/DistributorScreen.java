package com.human.client.screen;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.distributor.DistributorBlockEntity;
import com.human.common.gameplay.distributor.DistributorMode;
import com.human.common.gameplay.distributor.DistributorRemote;
import com.human.common.gameplay.menu.machine.ElectricMachineMenu;
import com.human.common.gameplay.vapor.VaporKind;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The distributor's screen - [stated] Oct 17, his approved mockup ({@code textures/gui/container/distributor.png}):
 * green-screen like the vaporizer, the liquid tank on the LEFT (8 buckets), the vapor chamber in the CENTRE (1,000),
 * the tank slot on the RIGHT, pipes from both sides into the chamber. Everything the shared machine screen draws - the
 * frame, the upgrade panel, the side buttons (priority, filter, redstone card, ports), the liquid gauge and its
 * tooltip, the slot - comes from {@link ElectricMachineScreen}. This adds:
 * <ul>
 * <li>THE CHAMBER: filled from the bottom in the vapor's colour, his tick marks redrawn over it; its tooltip carries
 * the vapor, the amount, the machine's status and draw, the MUTHUR command and whether the sensor sees something.</li>
 * <li>THE MODE BUTTONS: ALWAYS / REDSTONE / SENSOR stacked at the top right of the screen, above the tank slot - his
 * art has no place drawn for them, and that is the one free spot that fits all three at the GUI's own font size
 * ([stated] Oct 16: same font and size, reworded rather than shrunk). The lit one is the current mode.</li>
 * </ul>
 * The palette is the MUTHUR screen palette the other machine screens use (approved Oct 2).
 */
public class DistributorScreen extends ElectricMachineScreen {

    // Measured from his art.
    private static final int CHAMBER_X = 62;

    private static final int CHAMBER_Y = 20;

    private static final int CHAMBER_W = 45;

    private static final int CHAMBER_H = 53;

    /** His tick marks inside the chamber's right edge, redrawn over the fill. */
    private static final int TICK_X = 102;

    private static final int TICK_W = 4;

    private static final int[] TICK_ROWS = { 26, 34, 42, 50, 58, 66 };

    /** The three mode buttons: one column, sharing their borders. */
    private static final int MODE_X = 116;

    private static final int MODE_Y = 8;

    private static final int MODE_W = 53;

    private static final int MODE_H = 9;

    private static final int SLOT_X = 131;

    private static final int SLOT_Y = 38;

    // The MUTHUR palette (approved Oct 2).
    private static final int PANEL = 0xFF09180F;

    private static final int MINT = 0xFF6EF68D;

    private static final int MID = 0xFF33974F;

    private static final int LINE = 0xFF23573A;

    private static final int CHAMBER_ALPHA = 0xC0;

    public DistributorScreen(ElectricMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /**
     * QUALITY CHECK (Oct 18): the title is the block's name, and the Weyland-Yutani edition's ("Weyland-Yutani
     * Distributor") is ~140 px wide - centred, it ran into the mode buttons at the top right. [stated] Oct 16 GUI rule:
     * same font and size, reword rather than shrink - so every edition's screen says "Distributor".
     */
    private static final Component SCREEN_TITLE = Component.translatable("block.avp_human.electric_distributor");

    private static final int TITLE_MINT = 0xFF6EF68D;

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // Centred in the space left of the mode buttons, on the title row the shared screen uses.
        graphics.drawString(font, SCREEN_TITLE, (MODE_X - font.width(SCREEN_TITLE)) / 2, titleLabelY, TITLE_MINT, false);
    }

    private int chamber() {
        return menu.getExtraData(DistributorBlockEntity.EXTRA_CHAMBER);
    }

    private VaporKind vapor() {
        return VaporKind.byOrdinal(menu.getExtraData(DistributorBlockEntity.EXTRA_VAPOR));
    }

    private DistributorMode mode() {
        return DistributorMode.byOrdinal(menu.getExtraData(DistributorBlockEntity.EXTRA_MODE));
    }

    private DistributorRemote remote() {
        return DistributorRemote.byOrdinal(menu.getExtraData(DistributorBlockEntity.EXTRA_REMOTE));
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);

        var x = leftPos;
        var y = topPos;
        var kind = vapor();
        var amount = chamber();

        if (kind != null && amount > 0) {
            var filled = Math.max(1, Math.min(CHAMBER_H, Math.round(amount / (float) DistributorBlockEntity.CHAMBER_CAPACITY * CHAMBER_H)));
            var colour = CHAMBER_ALPHA << 24 | kind.colour();

            graphics.fill(x + CHAMBER_X, y + CHAMBER_Y + CHAMBER_H - filled, x + CHAMBER_X + CHAMBER_W, y + CHAMBER_Y + CHAMBER_H, colour);

            for (var row : TICK_ROWS) {
                graphics.fill(x + TICK_X, y + row, x + TICK_X + TICK_W, y + row + 1, MID);
            }
        }

        var current = mode();

        for (var mode : DistributorMode.values()) {
            var top = y + modeTop(mode);
            var left = x + MODE_X;
            var lit = mode == current;
            var hover = !lit && inside(mouseX, mouseY, left, top, MODE_W, MODE_H);

            graphics.fill(left, top, left + MODE_W, top + MODE_H, MINT);
            graphics.fill(left + 1, top + 1, left + MODE_W - 1, top + MODE_H - 1, lit ? MINT : hover ? LINE : PANEL);

            var label = Component.translatable(mode.translationKey());
            var textX = left + (MODE_W - font.width(label)) / 2;

            graphics.drawString(font, label, textX, top + 1, lit ? PANEL : MINT, false);
        }
    }

    private static int modeTop(DistributorMode mode) {
        // Shared borders: each box starts on the previous one's bottom line.
        return MODE_Y + mode.ordinal() * (MODE_H - 1);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (controls.isOpen() || controls.isMouseOver(mouseX, mouseY)) {
            return;
        }

        if (inside(mouseX, mouseY, leftPos + CHAMBER_X, topPos + CHAMBER_Y, CHAMBER_W, CHAMBER_H)) {
            com.human.client.screen.widget.GuiText.tooltip(graphics, font, chamberTooltip(), mouseX, mouseY);
            return;
        }

        for (var mode : DistributorMode.values()) {
            if (inside(mouseX, mouseY, leftPos + MODE_X, topPos + modeTop(mode), MODE_W, MODE_H)) {
                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    List.of(
                        Component.translatable(mode.translationKey() + ".title"),
                        Component.translatable(mode.translationKey() + ".desc").withStyle(ChatFormatting.GRAY)
                    ),
                    mouseX,
                    mouseY
                );
                return;
            }
        }

        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.x == SLOT_X && hoveredSlot.y == SLOT_Y) {
            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                List.of(
                    Component.translatable("gui.avp_human.distributor.tank_slot"),
                    Component.translatable("gui.avp_human.distributor.tank_slot.desc").withStyle(ChatFormatting.GRAY)
                ),
                mouseX,
                mouseY
            );
        }
    }

    private List<Component> chamberTooltip() {
        var lines = new ArrayList<Component>();
        var kind = vapor();
        var amount = chamber();

        lines.add(Component.translatable("gui.avp_human.distributor.chamber"));
        lines.add(
            kind == null || amount <= 0
                ? Component.translatable("gui.avp_human.machine.tank.empty").withStyle(ChatFormatting.GRAY)
                : Component.translatable(kind.translationKey()).withStyle(ChatFormatting.GRAY)
        );
        lines.add(
            Component.translatable(
                "gui.avp_human.distributor.chamber.amount",
                String.format(Locale.ROOT, "%,d", Math.max(0, amount)),
                String.format(Locale.ROOT, "%,d", DistributorBlockEntity.CHAMBER_CAPACITY)
            ).withStyle(ChatFormatting.GRAY)
        );

        var status = switch (menu.getStatus()) {
            case ElectricMachineBlockEntity.STATUS_DISABLED -> "gui.avp_human.machine.status.disabled";
            case ElectricMachineBlockEntity.STATUS_WORKING -> "gui.avp_human.distributor.status.pouring";
            case ElectricMachineBlockEntity.STATUS_IDLE -> "gui.avp_human.machine.status.idle";
            default -> "gui.avp_human.machine.status.no_power";
        };

        lines.add(Component.translatable(status));
        lines.add(Component.translatable("gui.avp_human.distributor.power", menu.getDrawPerTick()).withStyle(ChatFormatting.GRAY));

        var remote = remote();

        if (remote != DistributorRemote.NONE) {
            lines.add(Component.translatable("gui.avp_human.distributor.remote." + remote.id()).withStyle(ChatFormatting.YELLOW));
        }

        if (mode() == DistributorMode.SENSOR) {
            lines.add(
                Component.translatable(
                    menu.getExtraData(DistributorBlockEntity.EXTRA_SENSING) != 0
                        ? "gui.avp_human.distributor.sensor.hostile"
                        : "gui.avp_human.distributor.sensor.clear"
                ).withStyle(ChatFormatting.GRAY)
            );
        }

        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // An open side pop-up is modal: let the shared screen close it first.
        if (button == 0 && !controls.isOpen() && minecraft != null && minecraft.gameMode != null) {
            for (var mode : DistributorMode.values()) {
                if (inside(mouseX, mouseY, leftPos + MODE_X, topPos + modeTop(mode), MODE_W, MODE_H)) {
                    if (mode != mode()) {
                        minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId,
                            DistributorBlockEntity.BUTTON_SET_MODE + mode.ordinal()
                        );
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }

                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
