package com.human.neoforge.client.compat.create;

import com.human.neoforge.compat.create.ElectricGearboxMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * The electric gearbox's reading screen: the speed it is set to, power drawn, stress supplied, the buffer and a status
 * line. No slots and no inventory - its speed is set on the block's front with Create's value box.
 * <p>
 * Labels end before x 70; values run 70-164; nothing can overlap or cross the frame.
 * </p>
 */
public class ElectricGearboxScreen extends AbstractContainerScreen<ElectricGearboxMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "avp_human",
        "textures/gui/container/electric_gearbox.png"
    );

    private static final int LABEL_X = 12;

    private static final int VALUE_X = 70;

    private static final int VALUE_RIGHT = 164;

    public ElectricGearboxScreen(ElectricGearboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 96;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        var stored = menu.get(ElectricGearboxMenu.STORED);
        var capacity = Math.max(1L, menu.get(ElectricGearboxMenu.CAPACITY));
        var fill = (int) Math.round(93.0D * Math.min(1.0D, (double) stored / capacity));

        if (fill > 0) {
            graphics.fill(leftPos + 71, topPos + 55, leftPos + 71 + fill, topPos + 60, 0xFF000000 | ConverterScreenText.HI);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        var width = VALUE_RIGHT - VALUE_X;
        var rpm = menu.get(ElectricGearboxMenu.RPM);

        graphics.drawString(
            font,
            Component.translatable("gui.avp_human.converter.electric_gearbox"),
            LABEL_X,
            9,
            ConverterScreenText.HI,
            false
        );
        line(
            graphics,
            22,
            "gui.avp_human.converter.set_to",
            (rpm < 0 ? "-" : "") + ConverterScreenText.fit(font, Math.abs(rpm), "RPM", width - 6)
        );
        line(
            graphics,
            32,
            "gui.avp_human.converter.draw",
            ConverterScreenText.fit(font, menu.get(ElectricGearboxMenu.DRAW), "FE/T", width)
        );
        line(
            graphics,
            42,
            "gui.avp_human.converter.supplies",
            ConverterScreenText.fit(font, menu.get(ElectricGearboxMenu.SUPPLY), "SU", width)
        );
        graphics.drawString(font, Component.translatable("gui.avp_human.converter.buffer"), LABEL_X, 53, ConverterScreenText.DIM, false);

        var status = (int) menu.get(ElectricGearboxMenu.STATUS);
        graphics.drawString(
            font,
            Component.translatable(switch (status) {
                case ElectricGearboxMenu.STATUS_RUNNING -> "gui.avp_human.converter.running";
                case ElectricGearboxMenu.STATUS_NO_POWER -> "gui.avp_human.converter.no_power";
                case ElectricGearboxMenu.STATUS_OVERSTRESSED -> "gui.avp_human.converter.overstressed";
                default -> "gui.avp_human.converter.off";
            }),
            LABEL_X,
            64,
            switch (status) {
                case ElectricGearboxMenu.STATUS_RUNNING -> ConverterScreenText.HI;
                case ElectricGearboxMenu.STATUS_OFF -> ConverterScreenText.AMBER;
                default -> ConverterScreenText.RED;
            },
            false
        );
        graphics.drawString(
            font,
            Component.translatable("gui.avp_human.converter.set_on_front"),
            LABEL_X,
            75,
            ConverterScreenText.DIM,
            false
        );
    }

    private void line(GuiGraphics graphics, int y, String label, String value) {
        graphics.drawString(font, Component.translatable(label), LABEL_X, y, ConverterScreenText.DIM, false);
        graphics.drawString(font, value, VALUE_X, y, ConverterScreenText.HI, false);
    }
}
