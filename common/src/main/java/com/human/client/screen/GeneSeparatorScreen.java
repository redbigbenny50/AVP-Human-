package com.human.client.screen;

import com.human.common.gameplay.gene.AdvancedGenes;
import com.human.common.gameplay.gene.GeneColours;
import com.human.common.gameplay.gene.GeneDefects;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.gene.Genes;
import com.human.common.gameplay.gene.discovery.ClientGeneDiscoveries;
import com.human.common.gameplay.gene.machine.GeneSplicing;
import com.human.common.gameplay.gene.machine.separator.GeneSeparatorBlockEntity;
import com.human.common.gameplay.gene.machine.separator.GeneSeparatorMenu;
import com.human.common.gameplay.gene.tube.GeneTubeItem;
import com.human.common.gameplay.item.SyringeItem;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * The Gene Separator screen - built to the approved mockup (gene_separator_ingame_scale3.png, 384x324, Oct 28) at its
 * exact positions: header and chips, SAMPLE (syringe, EMPTY TUBES bay, SPIN bar, TUBES THIS RUN), OUTPUT STORAGE with
 * OUTPUT TO NETWORK, the status strip, inventory, and the RUN panel with START / STOP.
 * <p>
 * The machine's own slots are drawn as the mockup's glyphs (syringe, empty tubes with counts, filled tubes in their
 * gene colour) instead of item icons; hovering still shows the item tooltip. Every fixed string is measured against the
 * vanilla font; game-supplied names that could be wider than their frame scroll inside it ({@link TerminalText}).
 * </p>
 */
public class GeneSeparatorScreen extends AbstractContainerScreen<GeneSeparatorMenu> {

    private static final int G = 0xFF6EF58C;

    private static final int M = 0xFF33974E;

    private static final int D = 0xFF051008;

    private static final int A = 0xFFF5B841;

    private static final int P = 0xFF09190F;

    private static final int W = 0xFF0E2816;

    private static final int SW = GeneSeparatorMenu.SCREEN_WIDTH;

    private static final int SH = GeneSeparatorMenu.SCREEN_HEIGHT;

    private static final int SX = 190;

    private static final int SWID = SW - 6 - SX;

    private static final int BY = 222;

    private static final int RX = 180;

    private static final int RWID = SW - 6 - RX;

    private static final String[] ICON = {
        "....mmmmmmmm....",
        "..mm........mm..",
        ".m....gggg....m.",
        ".m...g....g...m.",
        ".m...g.gg.g...m.",
        ".m...g....g...m.",
        ".m....gggg....m.",
        "..mm........mm..",
        "....mmmmmmmm...."
    };

    private static final String[] SYRINGE = {
        "..........mm",
        "........mmgm",
        ".......mggm.",
        "......mggm..",
        ".....mggm...",
        "....mggm....",
        "...mggm.....",
        "..mmgm......",
        ".mm.m.......",
        "m..........."
    };

    public GeneSeparatorScreen(GeneSeparatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = SW;
        this.imageHeight = SH;
    }

    @Override
    protected void init() {
        super.init();
        topPos = Math.max(0, topPos);
        leftPos = Math.max(0, leftPos);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    /** The machine's slots are drawn as glyphs in renderBg; only the player's inventory shows item icons. */
    @Override
    protected void renderSlot(@NotNull GuiGraphics graphics, @NotNull Slot slot) {
        if (slot.index >= GeneSeparatorBlockEntity.SLOT_COUNT) {
            super.renderSlot(graphics, slot);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // Every label is drawn in renderBg at the mockup's positions.
    }

    // ------------------------------------------------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------------------------------------------------

    private boolean running() {
        return menu.data(GeneSeparatorMenu.DATA_RUNNING) != 0;
    }

    private boolean supplied() {
        return menu.data(GeneSeparatorMenu.DATA_SUPPLIED) != 0;
    }

    private int blocker() {
        return menu.data(GeneSeparatorMenu.DATA_BLOCKER);
    }

    private com.human.common.gameplay.component.SyringeContents contents() {
        var sample = menu.items().getItem(GeneSeparatorBlockEntity.SAMPLE_SLOT);
        if (!GeneSeparatorBlockEntity.acceptsSample(sample)) {
            return null;
        }
        return sample.get(HumanDataComponents.SYRINGE_CONTENTS.get());
    }

    private int genes() {
        var sample = menu.items().getItem(GeneSeparatorBlockEntity.SAMPLE_SLOT);
        return contents() == null ? 0 : com.human.common.gameplay.gene.machine.GeneSeparation.tubesNeeded(sample);
    }

    private int emptyTubes() {
        var count = 0;
        for (int i = 0; i < GeneSeparatorBlockEntity.BAY_SLOTS; i++) {
            var stack = menu.items().getItem(GeneSeparatorBlockEntity.BAY_START + i);
            if (GeneSeparatorBlockEntity.isEmptyTube(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int stored() {
        var count = 0;
        for (int i = 0; i < GeneSeparatorBlockEntity.OUTPUT_SLOTS; i++) {
            if (!menu.items().getItem(GeneSeparatorBlockEntity.OUTPUT_START + i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private int spinPercent() {
        var total = menu.data(GeneSeparatorMenu.DATA_RUN_TICKS);
        return !running() || total <= 0 ? 0 : Math.min(100, menu.data(GeneSeparatorMenu.DATA_PROGRESS) * 100 / total);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0);
        graphics.fill(0, 0, SW, SH, D);
        drawHeader(graphics);
        drawSample(graphics);
        drawOutput(graphics);
        drawStatus(graphics);
        drawBottom(graphics);
        graphics.pose().popPose();
    }

    private void drawHeader(GuiGraphics graphics) {
        box(graphics, 6, 6, SW - 12, 32, G, P);
        box(graphics, 9, 9, 26, 26, G, W);
        art(graphics, ICON, 9 + (26 - ICON[0].length()) / 2, 9 + (26 - ICON.length) / 2);
        text(graphics, 40, 11, "GENE SEPARATOR // CENTRIFUGE", G);
        var power = !supplied() ? "NO POWER" : running() ? "POWER OK \u00b7 40 FE/T" : "POWER OK";
        textRight(graphics, SW - 10, 11, power, M);

        String status;
        int statusColour;
        if (running()) {
            status = "SPINNING";
            statusColour = A;
        } else if (genes() == 0) {
            status = "IDLE";
            statusColour = G;
        } else if (blocker() >= 0) {
            status = "WAITING";
            statusColour = A;
        } else {
            status = "READY";
            statusColour = G;
        }
        var genes = genes();
        var network = menu.data(GeneSeparatorMenu.DATA_NETWORK) == 0
            ? "NETWORK OFF"
            : menu.data(GeneSeparatorMenu.DATA_NETWORK_FULL) != 0 ? "NETWORK FULL" : "NETWORK ON";
        var chips = new String[] { status, genes == 1 ? "1 GENE" : genes + " GENES", network };
        var x = 40;
        for (int i = 0; i < chips.length; i++) {
            var w = font.width(chips[i]) + 5;
            if (i == 0) {
                box(graphics, x, 22, w, 11, statusColour, statusColour);
                text(graphics, x + 3, 24, chips[i], D);
            } else {
                box(graphics, x, 22, w, 11, M, 0);
                text(graphics, x + 3, 24, chips[i], G);
            }
            x += w + 3;
        }
    }

    private void drawSample(GuiGraphics graphics) {
        box(graphics, 6, 42, 180, 148, G, P);
        text(graphics, 10, 46, "SAMPLE", M);
        box(graphics, 10, 56, 22, 22, G, W);
        var sample = menu.items().getItem(GeneSeparatorBlockEntity.SAMPLE_SLOT);
        var contents = contents();
        var genes = genes();
        if (!sample.isEmpty()) {
            art(graphics, SYRINGE, 15, 62);
        }
        if (contents != null) {
            fit(graphics, 36, 58, 145, "SYRINGE \u00b7 " + donorName(SyringeItem.donorOf(sample)), G, false);
            text(graphics, 36, 68, "1 DOSE \u00b7 " + (genes == 1 ? "1 GENE" : genes + " GENES"), M);
        } else if (!sample.isEmpty()) {
            text(graphics, 36, 58, "EMPTY SYRINGE", G);
            text(graphics, 36, 68, "NOTHING TO SEPARATE", M);
        } else {
            text(graphics, 36, 58, "NO SAMPLE", M);
            text(graphics, 36, 68, "PUT A FILLED SYRINGE IN", M);
        }

        var tubes = emptyTubes();
        text(graphics, 10, 82, "EMPTY TUBES", G);
        var short_ = genes > 0 && tubes < genes;
        textRight(graphics, 181, 82, genes > 0 ? tubes + " \u00b7 NEEDS " + genes : String.valueOf(tubes), short_ ? A : G);
        for (int c = 0; c < GeneSeparatorBlockEntity.BAY_SLOTS; c++) {
            box(graphics, 10 + c * 18, 92, 18, 18, M, W);
            var stack = menu.items().getItem(GeneSeparatorBlockEntity.BAY_START + c);
            if (!stack.isEmpty()) {
                tube(graphics, 16 + c * 18, 96, G, false);
                var count = String.valueOf(stack.getCount());
                textRight(graphics, 26 + c * 18, 103, count, G);
            }
        }

        var spinning = running();
        text(graphics, 10, 114, "SPIN", M);
        textRight(graphics, 181, 114, spinning ? spinPercent() + "%" : "-", G);
        box(graphics, 10, 124, 172, 7, M, D);
        var fill = 170 * spinPercent() / 100;
        if (fill > 0) {
            graphics.fill(11, 125, 11 + fill, 130, spinning ? A : G);
        }

        graphics.fill(10, 134, 182, 135, M);
        text(graphics, 10, 137, "TUBES THIS RUN", M);
        textRight(graphics, 181, 137, String.valueOf(genes), G);
        if (contents != null) {
            // The tubes this run will make, from the same rules the machine uses.
            var entries = com.human.common.gameplay.gene.machine.GeneSeparation.tubesFor(sample)
                .stream()
                .map(GeneTubeItem::read)
                .filter(java.util.Objects::nonNull)
                .toList();
            var show = entries.size() <= 3 ? entries.size() : 2;
            var y = 148;
            for (int i = 0; i < show; i++) {
                var entry = entries.get(i);
                tube(graphics, 10, y - 1, GeneColours.of(entry.gene()) | 0xFF000000, false);
                var known = ClientGeneDiscoveries.knowsGene(entry.gene());
                var name = known ? geneName(entry.gene()) : "???";
                var value = known ? value(entry.gene(), entry.operation(), entry.value()) : "";
                var valueWidth = font.width(value);
                fit(graphics, 20, y, 162 - valueWidth - (valueWidth > 0 ? 6 : 0), name, known ? G : M, false);
                if (valueWidth > 0) {
                    textRight(graphics, 181, y, value, G);
                }
                y += 10;
            }
            if (entries.size() > 3) {
                text(graphics, 20, y, "+ " + (entries.size() - 2) + " MORE", M);
            }
        }
    }

    private void drawOutput(GuiGraphics graphics) {
        box(graphics, SX, 42, SWID, 148, G, P);
        text(graphics, SX + 4, 46, "OUTPUT STORAGE", M);
        textRight(graphics, SX + SWID - 5, 46, stored() + " OF " + GeneSeparatorBlockEntity.OUTPUT_SLOTS, G);
        for (int i = 0; i < GeneSeparatorBlockEntity.OUTPUT_SLOTS; i++) {
            var x = SX + 4 + (i % 9) * 18;
            var y = 56 + (i / 9) * 18;
            box(graphics, x, y, 18, 18, M, W);
            var stack = menu.items().getItem(GeneSeparatorBlockEntity.OUTPUT_START + i);
            if (!stack.isEmpty()) {
                tube(graphics, x + 6, y + 4, GeneTubeItem.colour(stack) | 0xFF000000, false);
            }
        }
        var on = menu.data(GeneSeparatorMenu.DATA_NETWORK) != 0;
        box(graphics, SX + 4, 116, SWID - 8, 14, on ? G : M, D);
        text(graphics, SX + 8, 119, "OUTPUT TO NETWORK", G);
        textRight(graphics, SX + SWID - 9, 119, on ? "ON" : "OFF", on ? G : M);
        if (on && menu.data(GeneSeparatorMenu.DATA_NETWORK_FULL) != 0) {
            text(graphics, SX + 4, 136, "NETWORK FULL \u00b7 HOLDING TUBES", A);
            text(graphics, SX + 4, 148, "TAKE TUBES OUT OR FREE SPACE", M);
        } else if (on) {
            text(graphics, SX + 4, 136, "SENDS FINISHED TUBES TO", M);
            text(graphics, SX + 4, 148, "GENE STORAGE ON THIS GRID", M);
        } else {
            text(graphics, SX + 4, 136, "FINISHED TUBES STAY HERE", M);
            text(graphics, SX + 4, 148, "TAKE THEM OUT BY HAND", M);
        }
        graphics.fill(SX + 4, 160, SX + SWID - 4, 161, M);
        text(graphics, SX + 4, 164, "TUBE BAY HOLDS 9 STACKS (576)", M);
        text(graphics, SX + 4, 174, "ITEM PORT REFILLS IT FROM PIPES", M);
    }

    private void drawStatus(GuiGraphics graphics) {
        var genes = genes();
        String line1;
        String line2;
        var warn = false;
        var auto = menu.data(GeneSeparatorMenu.DATA_AUTO) != 0;
        if (running()) {
            var seconds = menu.data(GeneSeparatorMenu.DATA_RUN_TICKS) / 20;
            line1 = "SEPARATING " + genes + (genes == 1 ? " GENE INTO 1 TUBE" : " GENES INTO " + genes + " TUBES") + " \u00b7 " + seconds
                + " SECONDS";
            line2 = "THE SYRINGE COMES BACK EMPTY. SEPARATING COSTS NO INTEGRITY.";
        } else if (genes == 0) {
            line1 = "INSERT A FILLED SYRINGE";
            line2 = "ONE EMPTY TUBE PER GENE. THE SYRINGE COMES BACK EMPTY.";
        } else {
            var blocker = blocker();
            if (blocker == GeneSeparatorBlockEntity.Blocker.TUBES.ordinal()) {
                var have = emptyTubes();
                line1 = "NEEDS " + genes + " EMPTY TUBES \u00b7 HAS " + have;
                line2 = "ADD " + (genes - have) + " MORE " + (genes - have == 1 ? "TUBE" : "TUBES") + (auto
                    ? ". IT STARTS BY ITSELF WHEN READY."
                    : ", THEN PRESS START.");
                warn = true;
            } else if (blocker == GeneSeparatorBlockEntity.Blocker.OUTPUT_FULL.ordinal()) {
                line1 = "OUTPUT FULL \u00b7 NEEDS " + genes + " FREE SLOTS";
                line2 = "TAKE TUBES OUT OR TURN ON OUTPUT TO NETWORK.";
                warn = true;
            } else if (blocker == GeneSeparatorBlockEntity.Blocker.NO_POWER.ordinal()) {
                line1 = "NO POWER";
                line2 = "CONNECT POWER UNDER THE MACHINE.";
                warn = true;
            } else {
                line1 = "READY \u00b7 " + genes + (genes == 1 ? " GENE INTO 1 TUBE" : " GENES INTO " + genes + " TUBES");
                line2 = auto ? "STARTING..." : "PRESS START TO SEPARATE.";
            }
        }
        box(graphics, 6, 194, SW - 12, 24, warn ? A : M, P);
        text(graphics, 10, 198, line1, warn ? A : G);
        text(graphics, 10, 208, line2, M);
    }

    private void drawBottom(GuiGraphics graphics) {
        box(graphics, 6, BY, 170, 84, G, P);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                box(graphics, 10 + column * 18, BY + 4 + row * 18, 18, 18, M, W);
            }
        }
        for (int column = 0; column < 9; column++) {
            box(graphics, 10 + column * 18, BY + 62, 18, 18, M, W);
        }

        box(graphics, RX, BY, RWID, 84, G, P);
        text(graphics, RX + 4, BY + 4, "RUN", M);
        var genes = genes();
        var running = running();
        var left = running ? (menu.data(GeneSeparatorMenu.DATA_RUN_TICKS) - menu.data(GeneSeparatorMenu.DATA_PROGRESS) + 19) / 20 : 0;
        var auto = menu.data(GeneSeparatorMenu.DATA_AUTO) != 0;
        kv(graphics, BY + 14, "TIME", genes == 0 ? "-" : (GeneSeparatorBlockEntity.BASE_SECONDS + genes) + " S", G);
        kv(graphics, BY + 24, "POWER", GeneSeparatorBlockEntity.RUN_POWER + " FE/T", G);
        kv(graphics, BY + 34, "TIME LEFT", running ? left + " S" : "-", running ? A : M);
        kv(graphics, BY + 44, "AUTO START", auto ? "ON" : "OFF", auto ? G : M);
        box(graphics, RX + 4, BY + 66, RWID - 8, 14, G, D);
        var label = running ? "STOP" : "START";
        text(graphics, RX + 4 + (RWID - 8 - font.width(label)) / 2, BY + 69, label, G);

        var footY = BY + 88;
        text(graphics, 6, footY, "SHIFT-CLICK TUBES TO FILL", M);
        textRight(graphics, SW - 7, footY, "ITEM PORT TAKES EMPTY TUBES", M);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var x = (int) Math.floor(mouseX - leftPos);
        var y = (int) Math.floor(mouseY - topPos);
        if (button == 0) {
            if (inside(x, y, SX + 4, 116, SWID - 8, 14)) {
                return press(GeneSeparatorMenu.BUTTON_NETWORK);
            }
            if (inside(x, y, RX + 4, BY + 44, RWID - 8, 10)) {
                return press(GeneSeparatorMenu.BUTTON_AUTO);
            }
            if (inside(x, y, RX + 4, BY + 66, RWID - 8, 14)) {
                return press(GeneSeparatorMenu.BUTTON_START_STOP);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            if (minecraft.player != null) {
                minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.25F, 1.0F);
            }
        }
        return true;
    }

    private static boolean inside(int x, int y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Names and values (only what this player knows)
    // ------------------------------------------------------------------------------------------------------------

    private static String donorName(String donor) {
        if (donor == null) {
            return "UNKNOWN";
        }
        if (GeneSplicing.SPLICED.equals(donor)) {
            return "SPLICED";
        }
        var id = ResourceLocation.tryParse(donor);
        if (id == null || !ClientGeneDiscoveries.knowsSpecies(id)) {
            return "???";
        }
        return EntityType.byString(donor).map(type -> type.getDescription().getString()).orElse(id.getPath()).toUpperCase(Locale.ROOT);
    }

    private static String geneName(ResourceLocation id) {
        var gene = GeneRegistry.getValueOrNull(id);
        if (gene == null) {
            return id.getPath().replace('_', ' ').toUpperCase(Locale.ROOT);
        }
        var name = Component.translatable(gene.getTranslationKey()).getString();
        return (name.equals(gene.getTranslationKey()) ? id.getPath().replace('_', ' ') : name).toUpperCase(Locale.ROOT);
    }

    private static String value(ResourceLocation id, GeneOperationType operation, double value) {
        var gene = GeneRegistry.getValueOrNull(id);
        if (gene != null && AdvancedGenes.definition(gene) != null) {
            return "MUTATION";
        }
        if (gene != null && GeneDefects.all().contains(gene)) {
            return "DEFECT";
        }
        if (gene != null && Genes.category(gene) == Genes.Category.LOOT) {
            return "+" + Math.round(value * 100) + "% DROP";
        }
        if (operation == GeneOperationType.MULTIPLICATIVE) {
            return String.format(Locale.ROOT, "%+.1f%%", value * 100);
        }
        return String.format(Locale.ROOT, "%+.2f", value);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing helpers
    // ------------------------------------------------------------------------------------------------------------

    private void kv(GuiGraphics graphics, int y, String key, String value, int colour) {
        text(graphics, RX + 4, y, key, M);
        textRight(graphics, RX + RWID - 5, y, value, colour);
    }

    /** The mockup's tube glyph: cap, glass outline, fluid in the given colour. */
    private void tube(GuiGraphics graphics, int x, int y, int colour, boolean dim) {
        graphics.fill(x + 2, y, x + 4, y + 2, M);
        graphics.renderOutline(x + 1, y + 2, 4, 8, dim ? M : G);
        graphics.fill(x + 2, y + 5, x + 4, y + 9, colour);
    }

    private void art(GuiGraphics graphics, String[] art, int x, int y) {
        for (int row = 0; row < art.length; row++) {
            for (int column = 0; column < art[row].length(); column++) {
                var c = art[row].charAt(column);
                if (c != '.') {
                    graphics.fill(x + column, y + row, x + column + 1, y + row + 1, c == 'g' ? G : M);
                }
            }
        }
    }

    private void fit(GuiGraphics graphics, int x, int y, int width, String text, int colour, boolean right) {
        TerminalText.draw(graphics, font, leftPos, topPos, x, y, width, text, colour, right);
    }

    private void box(GuiGraphics graphics, int x, int y, int w, int h, int border, int fill) {
        if (fill != 0) {
            graphics.fill(x, y, x + w, y + h, fill);
        }
        graphics.renderOutline(x, y, w, h, border);
    }

    private void text(GuiGraphics graphics, int x, int y, String text, int colour) {
        graphics.drawString(font, text, x, y, colour, false);
    }

    private void textRight(GuiGraphics graphics, int rightX, int y, String text, int colour) {
        graphics.drawString(font, text, rightX - font.width(text) + 1, y, colour, false);
    }
}
