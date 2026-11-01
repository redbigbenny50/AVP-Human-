package com.human.client.screen;

import com.human.HumanResources;
import com.human.common.gameplay.gene.geneticist.GeneCleanser;
import com.human.common.gameplay.gene.geneticist.GeneticistStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * The Geneticist Station's cleanser screen, built to the approved mockup ([stated] Oct 29:
 * geneticist_station_ingame_scale3.png). A worktable screen like the blueprint table - MUTHUR green panel, vanilla
 * frame and inventory - with a title row above the panel.
 * <p>
 * Panel (all y below are +{@link GeneticistStationMenu#TOP} for the title row): syringe slot (26,25) and cleanser slot
 * (26,51) joined by a bracket and arrow to the output (134,38); the info box (50-126 x 11-38) says what the shot will
 * do; the segmented progress bar (50-125 x 51-58) fills amber while cleansing and turns green when done; the status
 * line sits under it at (50,63). Every line was measured against its frame in the mockup.
 * </p>
 */
public class GeneticistStationScreen extends AbstractContainerScreen<GeneticistStationMenu> {

    private static final ResourceLocation TEXTURE = HumanResources.location("textures/gui/container/geneticist_station.png");

    private static final int TOP = GeneticistStationMenu.TOP;

    private static final int BRIGHT = 0xFF6EF68D;

    private static final int AMBER = 0xFFF0BE3C;

    private static final int DIM = 0xFF23573A;

    private static final int TITLE = 0xFF404040;

    public GeneticistStationScreen(GeneticistStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166 + TOP;
        titleLabelX = 8;
        titleLabelY = 6;
        // The approved mockup has no "Inventory" label - park it off screen.
        inventoryLabelY = 99999;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Faint outlines of what goes in an EMPTY slot.
        if (menu.slots.get(GeneticistStationMenu.SYRINGE_SLOT).getItem().isEmpty()) {
            graphics.blit(TEXTURE, leftPos + 26, topPos + 25 + TOP, 176, 0, 16, 16);
        }
        if (menu.slots.get(GeneticistStationMenu.CLEANSER_SLOT).getItem().isEmpty()) {
            graphics.blit(TEXTURE, leftPos + 26, topPos + 51 + TOP, 192, 0, 16, 16);
        }
        if (menu.slots.get(GeneticistStationMenu.OUTPUT_SLOT).getItem().isEmpty()) {
            graphics.blit(TEXTURE, leftPos + 134, topPos + 38 + TOP, 176, 0, 16, 16);
        }

        var done = isDone();
        var fraction = done ? 1.0F : menu.progress() / (float) GeneCleanser.TICKS;

        // The arrow lights up when the cleanse is done.
        if (done) {
            graphics.fill(leftPos + 46, topPos + 46 + TOP, leftPos + 126, topPos + 47 + TOP, BRIGHT);
        }

        // Segmented fill inside the bar frame: a 1 px gap every 5 px, amber while running, green when done.
        var inner = 125 - 50 - 3;
        var filled = (int) (inner * fraction);
        var colour = done ? BRIGHT : AMBER;
        for (int i = 0; i < filled; i++) {
            if ((i + 1) % 5 == 0) {
                continue;
            }
            var x = leftPos + 52 + i;
            graphics.fill(x, topPos + 53 + TOP, x + 1, topPos + 57 + TOP, colour);
        }
    }

    /** DONE shows while the finished syringe is still waiting in the output. */
    private boolean isDone() {
        return menu.justFinished() && !menu.result().isEmpty();
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TITLE, false);

        var status = menu.status();
        var lines = new java.util.ArrayList<Line>();
        Line under;

        if (isDone()) {
            var shot = menu.result();
            addShotLines(lines, shot);
            under = new Line(Component.translatable("gui.avp_human.geneticist_station.done"), BRIGHT);
        } else {
            switch (status) {
                case ALREADY_CLEANSED -> {
                    lines.add(new Line(Component.translatable("gui.avp_human.geneticist_station.already"), AMBER));
                    lines.add(new Line(Component.translatable("gui.avp_human.geneticist_station.cleansed"), AMBER));
                    lines.add(new Line(Component.translatable("gui.avp_human.geneticist_station.one_only"), DIM));
                    under = new Line(Component.translatable("gui.avp_human.geneticist_station.used_once"), AMBER);
                }
                case READY -> {
                    addShotLines(lines, menu.syringe());
                    var left = Math.max(1, (GeneCleanser.TICKS - menu.progress() + 19) / 20);
                    under = new Line(Component.translatable("gui.avp_human.geneticist_station.left", left), AMBER);
                }
                case NEEDS_CLEANSER -> {
                    addShotLines(lines, menu.syringe());
                    under = new Line(Component.translatable("gui.avp_human.geneticist_station.add_cleanser"), AMBER);
                }
                case NEEDS_SYRINGE -> under = new Line(Component.translatable("gui.avp_human.geneticist_station.add_syringe"), AMBER);
                case OUTPUT_FULL -> under = new Line(Component.translatable("gui.avp_human.geneticist_station.output_full"), AMBER);
                default -> under = new Line(Component.translatable("gui.avp_human.geneticist_station.empty"), DIM);
            }
        }

        // The info box: up to three lines, 8 px apart, inside its frame (x 50-126).
        var y = 11 + TOP + 3;
        for (int i = 0; i < lines.size() && i < 3; i++) {
            graphics.drawString(font, lines.get(i).text(), 53, y, lines.get(i).colour(), false);
            y += 8;
        }
        graphics.drawString(font, under.text(), 50, 63 + TOP, under.colour(), false);
    }

    /** CLEANSE +35% / INJECT -20% / NET +15% - or PURE SHOT first for an empty syringe. */
    private static void addShotLines(java.util.List<Line> lines, net.minecraft.world.item.ItemStack syringe) {
        var cost = GeneCleanser.injectionCost(syringe);
        var bonus = GeneCleanser.isCleansed(syringe) ? GeneCleanser.bonusOf(syringe) : GeneCleanser.BONUS;
        var net = cost + bonus;

        if (GeneCleanser.isPureShot(syringe)) {
            lines.add(new Line(Component.translatable("gui.avp_human.geneticist_station.pure_shot"), BRIGHT));
        } else {
            lines.add(new Line(Component.translatable("gui.avp_human.geneticist_station.cleanse", percent(bonus)), BRIGHT));
        }
        lines.add(new Line(Component.translatable("gui.avp_human.geneticist_station.inject", percent(cost)), AMBER));
        lines.add(new Line(Component.translatable("gui.avp_human.geneticist_station.net", percent(net)), BRIGHT));
    }

    /** "+35%" / "-20%". */
    private static String percent(double value) {
        var rounded = Math.round(value * 100);
        return (rounded >= 0 ? "+" : "") + rounded + "%";
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // Hovering an EMPTY input says what goes in it.
        if (hoveredSlot != null && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            if (hoveredSlot.index == GeneticistStationMenu.SYRINGE_SLOT) {
                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    Component.translatable("gui.avp_human.geneticist_station.hint.syringe"),
                    mouseX,
                    mouseY
                );
            } else if (hoveredSlot.index == GeneticistStationMenu.CLEANSER_SLOT) {
                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    Component.translatable("gui.avp_human.geneticist_station.hint.cleanser"),
                    mouseX,
                    mouseY
                );
            }
        }
    }

    private record Line(
        Component text,
        int colour
    ) {}
}
