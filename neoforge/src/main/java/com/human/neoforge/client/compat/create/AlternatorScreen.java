package com.human.neoforge.client.compat.create;

import com.human.neoforge.compat.create.AlternatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * The alternator's reading screen, in the machines' green screen style: speed in, load on Create, power out, the
 * buffer, a status line, and the four card slots (with the redstone card's mode button under them when one is fitted).
 * <p>
 * Every label and value has a fixed column, measured against the real font: labels end before x 66, values run 66-118
 * (the card column starts at 126), so nothing can overlap or cross the frame.
 * </p>
 */
public class AlternatorScreen extends AbstractContainerScreen<AlternatorMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "avp_human",
        "textures/gui/container/alternator.png"
    );

    private static final int LABEL_X = 12;

    private static final int VALUE_X = 66;

    private static final int VALUE_RIGHT = 118;

    private Button redstoneButton;

    public AlternatorScreen(AlternatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        redstoneButton = addRenderableWidget(
            Button.builder(Component.empty(), button -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AlternatorMenu.BUTTON_REDSTONE);
                }
            }).bounds(leftPos + 126, topPos + 61, 40, 13).build()
        );
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (redstoneButton != null) {
            redstoneButton.visible = menu.get(AlternatorMenu.HAS_REDSTONE) != 0;
            redstoneButton.setMessage(Component.translatable(switch ((int) menu.get(AlternatorMenu.MODE)) {
                case 1 -> "gui.avp_human.fabricator.redstone.high";
                case 2 -> "gui.avp_human.fabricator.redstone.low";
                default -> "gui.avp_human.fabricator.redstone.ignore";
            }));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        var stored = menu.get(AlternatorMenu.STORED);
        var capacity = Math.max(1L, menu.get(AlternatorMenu.CAPACITY));
        var fill = (int) Math.round(51.0D * Math.min(1.0D, (double) stored / capacity));

        if (fill > 0) {
            graphics.fill(leftPos + 67, topPos + 55, leftPos + 67 + fill, topPos + 60, 0xFF000000 | ConverterScreenText.HI);
        }
    }

    /** Our own labels only: vanilla's "Inventory" title would sit on the screen panel. */
    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        var width = VALUE_RIGHT - VALUE_X;

        graphics.drawString(font, Component.translatable("gui.avp_human.converter.alternator"), LABEL_X, 9, ConverterScreenText.HI, false);
        graphics.drawString(font, Component.translatable("gui.avp_human.converter.cards"), 128, 9, ConverterScreenText.HI, false);

        line(graphics, 22, "gui.avp_human.converter.speed", ConverterScreenText.fit(font, menu.get(AlternatorMenu.SPEED), "RPM", width));
        line(graphics, 32, "gui.avp_human.converter.load", ConverterScreenText.fit(font, menu.get(AlternatorMenu.LOAD), "SU", width));
        line(graphics, 42, "gui.avp_human.converter.output", ConverterScreenText.fit(font, menu.get(AlternatorMenu.OUTPUT), "FE/T", width));
        graphics.drawString(font, Component.translatable("gui.avp_human.converter.buffer"), LABEL_X, 53, ConverterScreenText.DIM, false);

        var status = (int) menu.get(AlternatorMenu.STATUS);
        graphics.drawString(
            font,
            Component.translatable(switch (status) {
                case AlternatorMenu.STATUS_GENERATING -> "gui.avp_human.converter.generating";
                case AlternatorMenu.STATUS_OVERSTRESSED -> "gui.avp_human.converter.overstressed";
                case AlternatorMenu.STATUS_REDSTONE -> "gui.avp_human.converter.redstone_off";
                default -> "gui.avp_human.converter.stopped";
            }),
            LABEL_X,
            64,
            switch (status) {
                case AlternatorMenu.STATUS_GENERATING -> ConverterScreenText.HI;
                case AlternatorMenu.STATUS_OVERSTRESSED -> ConverterScreenText.RED;
                default -> ConverterScreenText.AMBER;
            },
            false
        );
    }

    private void line(GuiGraphics graphics, int y, String label, String value) {
        graphics.drawString(font, Component.translatable(label), LABEL_X, y, ConverterScreenText.DIM, false);
        graphics.drawString(font, value, VALUE_X, y, ConverterScreenText.HI, false);
    }
}
