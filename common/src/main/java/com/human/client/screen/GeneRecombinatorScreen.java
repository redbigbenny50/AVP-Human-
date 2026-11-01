package com.human.client.screen;

import com.human.Human;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.gene.Genes;
import com.human.common.gameplay.gene.discovery.ClientGeneDiscoveries;
import com.human.common.gameplay.gene.machine.GeneRecombination;
import com.human.common.gameplay.gene.machine.recombinator.GeneRecombinatorBlockEntity;
import com.human.common.gameplay.gene.machine.recombinator.GeneRecombinatorMenu;
import com.human.common.gameplay.gene.machine.storage.ClientNetworkTubes;
import com.human.common.gameplay.gene.tube.GeneTubeItem;
import com.human.common.network.packet.C2SNetworkTubePullPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Gene Recombinator screen - built to the approved mockup (gene_recombinator_ingame_scale3.png, 384x330, Oct 28) at
 * its exact positions: header and chips; LOAD with the LEFT arm, SAMPLE cradle and RIGHT arm, SAMPLE / RESULT, the
 * 8-segment DOSES bar, MIX and EMPTIES BACK; NETWORK STORAGE with its filter, list and PULL FROM NETWORK; the status
 * strip; inventory; RUN with START / STOP.
 * <p>
 * Doses are counted in the stronger vial's tier ([stated] Oct 27: Prime 8, Strong 12, Average 16, Weak 32 draws to the
 * cap); the 8 segments show the sample's share of the cap. Undiscovered genes show as ???. Every fixed string is
 * measured to fit; game names wider than their frame scroll inside it ({@link TerminalText}).
 * </p>
 */
public class GeneRecombinatorScreen extends AbstractContainerScreen<GeneRecombinatorMenu> {

    private static final int G = 0xFF6EF58C;

    private static final int M = 0xFF33974E;

    private static final int D = 0xFF051008;

    private static final int A = 0xFFF5B841;

    private static final int P = 0xFF09190F;

    private static final int W = 0xFF0E2816;

    private static final int SW = GeneRecombinatorMenu.SCREEN_WIDTH;

    private static final int SH = GeneRecombinatorMenu.SCREEN_HEIGHT;

    private static final int NX = 190;

    private static final int NW = SW - 6 - NX;

    private static final int BY = 214;

    private static final int RX = 180;

    private static final int RWID = SW - 6 - RX;

    /** Network rows: y 72 down to the hint line, 10 px each. */
    private static final int ROWS_TOP = 72;

    private static final int VISIBLE_ROWS = 7;

    private static final String[] ICON = {
        "..m........m..",
        ".mm..gggg..mm.",
        "m.m..g..g..m.m",
        "..m..gggg..m..",
        "..mmmmmmmmmm..",
        "......gg......",
        "....gggggg...."
    };

    private EditBox filter;

    private boolean filterTouched;

    private int scroll;

    private int selected = -1;

    public GeneRecombinatorScreen(GeneRecombinatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = SW;
        this.imageHeight = SH;
    }

    @Override
    protected void init() {
        super.init();
        topPos = Math.max(0, topPos);
        leftPos = Math.max(0, leftPos);
        filter = new EditBox(font, leftPos + NX + 7, topPos + 58, NW - 14, 10, Component.literal("FILTER"));
        filter.setBordered(false);
        filter.setMaxLength(24);
        filter.setTextColor(G);
        filter.setResponder(text -> {
            var upper = text.toUpperCase(Locale.ROOT);
            if (!upper.equals(text)) {
                filter.setValue(upper);
            }
            scroll = 0;
            selected = -1;
        });
        addRenderableWidget(filter);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Until the player types, the filter follows the sample's gene.
        if (!filterTouched && !filter.isFocused()) {
            var sample = GeneTubeItem.read(menu.items().getItem(GeneRecombinatorBlockEntity.SAMPLE_SLOT));
            var wanted = sample == null || !ClientGeneDiscoveries.knowsGene(sample.gene()) ? "" : geneName(sample.gene());
            if (!wanted.equals(filter.getValue())) {
                filter.setValue(wanted);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderSlot(@NotNull GuiGraphics graphics, @NotNull Slot slot) {
        if (slot.index >= GeneRecombinatorBlockEntity.SLOT_COUNT) {
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

    private ItemStack slotItem(int slot) {
        return menu.items().getItem(slot);
    }

    private boolean running() {
        return menu.data(GeneRecombinatorMenu.DATA_RUNNING) != 0;
    }

    private int blocker() {
        return menu.data(GeneRecombinatorMenu.DATA_BLOCKER);
    }

    /** The stronger loaded vial's dose, else the sample's own. */
    private double dose(@Nullable GeneTubeItem.Contents sample) {
        var a = GeneTubeItem.read(slotItem(GeneRecombinatorBlockEntity.LEFT_SLOT));
        var b = GeneTubeItem.read(slotItem(GeneRecombinatorBlockEntity.RIGHT_SLOT));
        var best = 0.0D;
        for (var vial : new GeneTubeItem.Contents[] { a, b }) {
            if (vial != null && sample != null && vial.gene().equals(sample.gene()) && Math.abs(vial.perDose()) > Math.abs(best)) {
                best = vial.perDose();
            }
        }
        return best != 0.0D ? best : sample == null ? 0.0D : sample.perDose();
    }

    private static String tierName(double cap, double dose) {
        if (dose == 0.0D) {
            return "";
        }
        if (dose < 0) {
            return "PENALTY";
        }
        var doses = cap / dose;
        return doses <= 9.0D ? "PRIME" : doses <= 13.0D ? "STRONG" : doses <= 20.0D ? "AVERAGE" : "WEAK";
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
        drawLoad(graphics);
        drawNetwork(graphics);
        drawStatus(graphics);
        drawBottom(graphics);
        graphics.pose().popPose();
    }

    private void drawHeader(GuiGraphics graphics) {
        box(graphics, 6, 6, SW - 12, 32, G, P);
        box(graphics, 9, 9, 26, 26, G, W);
        art(graphics, ICON, 9 + (26 - ICON[0].length()) / 2, 9 + (26 - ICON.length) / 2);
        text(graphics, 40, 11, "GENE RECOMBINATOR // CONCENTRATOR", G);
        var supplied = menu.data(GeneRecombinatorMenu.DATA_SUPPLIED) != 0;
        textRight(graphics, SW - 10, 11, !supplied ? "NO POWER" : running() ? "POWER OK \u00b7 40 FE/T" : "POWER OK", M);

        var sample = GeneTubeItem.read(slotItem(GeneRecombinatorBlockEntity.SAMPLE_SLOT));
        String status;
        int colour;
        if (running()) {
            status = "MIXING";
            colour = A;
        } else if (sample == null) {
            status = "IDLE";
            colour = G;
        } else if (blocker() >= 0) {
            status = "WAITING";
            colour = A;
        } else {
            status = "READY";
            colour = G;
        }
        var gene = sample == null ? "NO SAMPLE" : ClientGeneDiscoveries.knowsGene(sample.gene()) ? geneName(sample.gene()) : "???";
        var pull = menu.data(GeneRecombinatorMenu.DATA_PULL) != 0 ? "PULL ON" : "PULL OFF";
        var x = 40;
        var statusWidth = font.width(status) + 5;
        box(graphics, x, 22, statusWidth, 11, colour, colour);
        text(graphics, x + 3, 24, status, D);
        x += statusWidth + 3;
        // The gene chip takes what is left beside the PULL chip; a long name scrolls inside it.
        var pullWidth = font.width(pull) + 5;
        var geneWidth = Math.min(font.width(gene) + 5, SW - 10 - x - 3 - pullWidth);
        box(graphics, x, 22, geneWidth, 11, M, 0);
        fit(graphics, x + 3, 24, geneWidth - 5, gene, G, false);
        x += geneWidth + 3;
        box(graphics, x, 22, pullWidth, 11, M, 0);
        text(graphics, x + 3, 24, pull, G);
    }

    private void drawLoad(GuiGraphics graphics) {
        box(graphics, 6, 42, 180, 140, G, P);
        text(graphics, 10, 46, "LOAD", M);
        holder(graphics, 14, 60, 18, GeneRecombinatorBlockEntity.LEFT_SLOT, "LEFT", false);
        holder(graphics, 82, 58, 22, GeneRecombinatorBlockEntity.SAMPLE_SLOT, "SAMPLE", true);
        holder(graphics, 154, 60, 18, GeneRecombinatorBlockEntity.RIGHT_SLOT, "RIGHT", false);
        text(graphics, 38, 64, ">", M);
        text(graphics, 140, 64, "<", M);
        graphics.fill(10, 96, 182, 97, M);

        var sample = GeneTubeItem.read(slotItem(GeneRecombinatorBlockEntity.SAMPLE_SLOT));
        var known = sample != null && ClientGeneDiscoveries.knowsGene(sample.gene());
        var gene = sample == null ? null : GeneRegistry.getValueOrNull(sample.gene());
        var cap = gene == null ? 0.0D : Genes.cap(gene);
        var dose = dose(sample);
        var tier = tierName(cap, dose);
        var have = sample == null || dose == 0.0D ? 0 : (int) Math.round(Math.abs(sample.value() / dose));
        var max = dose == 0.0D ? 8 : (int) Math.round(cap / Math.abs(dose));

        text(graphics, 10, 100, "SAMPLE", M);
        textRight(
            graphics,
            181,
            100,
            sample == null ? "-" : !known ? "???" : amount(sample, sample.value()) + " \u00b7 " + have + " " + tier,
            G
        );

        var outcome = GeneRecombination.combine(
            slotItem(GeneRecombinatorBlockEntity.SAMPLE_SLOT),
            slotItem(GeneRecombinatorBlockEntity.LEFT_SLOT),
            slotItem(GeneRecombinatorBlockEntity.RIGHT_SLOT)
        );
        var result = GeneTubeItem.read(outcome.result());
        String resultText;
        var resultWarn = false;
        if (outcome.ok() && result != null) {
            resultText = !known ? "???" : amount(result, result.value()) + " \u00b7 " + Math.min(max, have + 1) + " " + tier;
        } else if (outcome.refusal() == GeneRecombination.Refusal.AT_CAP) {
            resultText = "AT CAP";
            resultWarn = true;
        } else {
            resultText = "-";
        }
        text(graphics, 10, 110, "RESULT", M);
        textRight(graphics, 181, 110, resultText, resultWarn ? A : G);

        var after = outcome.ok() ? Math.min(max, have + 1) : have;
        text(graphics, 10, 121, "DOSES", M);
        textRight(
            graphics,
            181,
            121,
            sample == null ? "-" : outcome.ok() ? have + " > " + after + " OF " + max : Math.min(have, max) + " OF " + max,
            G
        );
        var haveSegments = max == 0 ? 0 : Math.min(8, (int) Math.round(8.0D * have / max));
        var afterSegments = max == 0 ? 0 : Math.min(8, (int) Math.round(8.0D * after / max));
        for (int k = 0; k < 8; k++) {
            var x0 = 10 + k * 22;
            box(graphics, x0, 131, 20, 6, M, D);
            if (k < haveSegments) {
                graphics.fill(x0 + 1, 132, x0 + 19, 136, G);
            } else if (k < afterSegments) {
                graphics.fill(x0 + 1, 132, x0 + 19, 136, A);
            }
        }

        var progress = menu.data(GeneRecombinatorMenu.DATA_PROGRESS);
        var pct = running() ? Math.min(100, progress * 100 / GeneRecombinatorBlockEntity.RUN_TICKS) : 0;
        text(graphics, 10, 143, "MIX", M);
        textRight(graphics, 181, 143, running() ? pct + "%" : "-", G);
        box(graphics, 10, 153, 172, 7, M, D);
        var fill = 170 * pct / 100;
        if (fill > 0) {
            graphics.fill(11, 154, 11 + fill, 159, A);
        }

        text(graphics, 10, 166, "EMPTIES BACK", M);
        box(graphics, 164, 163, 18, 18, M, W);
        var empties = slotItem(GeneRecombinatorBlockEntity.EMPTIES_SLOT);
        if (!empties.isEmpty()) {
            tube(graphics, 170, 167, 0, true);
            textRight(graphics, 180, 174, String.valueOf(empties.getCount()), G);
        }
    }

    private void holder(GuiGraphics graphics, int x, int y, int size, int slot, String label, boolean big) {
        box(graphics, x, y, size, size, big ? G : M, W);
        var stack = slotItem(slot);
        if (!stack.isEmpty()) {
            tube(graphics, x + size / 2 - 3, y + (size - 10) / 2, GeneTubeItem.colour(stack) | 0xFF000000, false);
        }
        text(graphics, x + size / 2 - font.width(label) / 2, y + size + 3, label, M);
    }

    private List<ClientNetworkTubes.Entry> filtered() {
        var query = filter.getValue().trim();
        var list = new ArrayList<ClientNetworkTubes.Entry>();
        for (var entry : ClientNetworkTubes.entries()) {
            // Only genes this player knows can be matched by name; unknown ones show as ??? with no filter.
            var name = ClientGeneDiscoveries.knowsGene(entry.gene()) ? geneName(entry.gene()) : "???";
            if (query.isEmpty() || name.contains(query)) {
                list.add(entry);
            }
        }
        return list;
    }

    private void drawNetwork(GuiGraphics graphics) {
        box(graphics, NX, 42, NW, 140, G, P);
        text(graphics, NX + 4, 46, "NETWORK STORAGE", M);
        var list = filtered();
        var query = filter.getValue().trim();
        var space = query.indexOf(' ');
        var hits = list.size() + " " + (query.isEmpty()
            ? (list.size() == 1 ? "TUBE" : "TUBES")
            : space < 0 ? query : query.substring(0, space));
        fit(graphics, NX + NW - 5 - Math.min(font.width(hits), 70) + 1, 46, Math.min(font.width(hits), 70), hits, G, true);
        box(graphics, NX + 4, 56, NW - 8, 12, G, D);
        if (query.isEmpty() && !filter.isFocused()) {
            text(graphics, NX + 7, 58, "_", M);
        }

        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - VISIBLE_ROWS)));
        var y = ROWS_TOP;
        if (list.isEmpty()) {
            text(graphics, NX + 6, y + 1, ClientNetworkTubes.entries().isEmpty() ? "NO TUBES ON THIS GRID" : "NONE MATCH", M);
        }
        for (int i = scroll; i < list.size() && i < scroll + VISIBLE_ROWS; i++) {
            var entry = list.get(i);
            var known = ClientGeneDiscoveries.knowsGene(entry.gene());
            var name = known ? geneName(entry.gene()) + " " + amount(entry) : "???";
            var right = "STORE " + entry.storage();
            var rightWidth = font.width(right);
            var isSelected = i == selected;
            if (isSelected) {
                graphics.fill(NX + 4, y, NX + NW - 4, y + 10, G);
            }
            fit(graphics, NX + 6, y + 1, NW - 13 - rightWidth - 6, name, isSelected ? D : known ? G : M, false);
            textRight(graphics, NX + NW - 7, y + 1, right, isSelected ? D : M);
            y += 10;
        }
        var hintY = ROWS_TOP + VISIBLE_ROWS * 10;
        graphics.fill(NX + 4, hintY + 2, NX + NW - 4, hintY + 3, M);
        text(graphics, NX + 4, hintY + 6, "CLICK A TUBE TO LOAD AN ARM", M);

        var pull = menu.data(GeneRecombinatorMenu.DATA_PULL) != 0;
        box(graphics, NX + 4, 164, NW - 8, 14, pull ? G : M, D);
        text(graphics, NX + 8, 167, "PULL FROM NETWORK", G);
        textRight(graphics, NX + NW - 9, 167, pull ? "ON" : "OFF", pull ? G : M);
    }

    private void drawStatus(GuiGraphics graphics) {
        String line1;
        String line2;
        var warn = false;
        var auto = menu.data(GeneRecombinatorMenu.DATA_AUTO) != 0;
        if (running()) {
            var sample = GeneTubeItem.read(slotItem(GeneRecombinatorBlockEntity.SAMPLE_SLOT));
            var gene = sample == null ? null : GeneRegistry.getValueOrNull(sample.gene());
            var tier = gene == null ? "" : tierName(Genes.cap(gene), dose(sample));
            line1 = "POURING BOTH VIALS INTO THE SAMPLE \u00b7 10 SECONDS";
            line2 = "THE SAMPLE GAINS ONE " + (tier.isEmpty() ? "" : tier + " ") + "DOSE. BOTH VIALS COME BACK EMPTY.";
        } else {
            switch (blocker()) {
                case 0 -> {
                    line1 = "LOAD A SAMPLE AND TWO VIALS";
                    line2 = "SAME GENE IN ALL THREE. RIGHT-CLICK TUBES ONTO THE MACHINE.";
                }
                case 1 -> {
                    line1 = "VIALS MUST MATCH THE SAMPLE'S GENE";
                    line2 = "TAKE THE OTHER GENE'S VIAL OUT.";
                    warn = true;
                }
                case 2 -> {
                    line1 = "MUTATIONS AND DEFECTS DON'T STACK";
                    line2 = "ONLY BASE AND LOOT/HARVEST GENES RECOMBINE.";
                    warn = true;
                }
                case 3 -> {
                    line1 = "SAMPLE IS AT CAP \u00b7 NOTHING TO ADD";
                    line2 = "TAKE IT OUT OR LOAD ANOTHER GENE.";
                    warn = true;
                }
                case 4 -> {
                    line1 = "EMPTIES BACK IS FULL";
                    line2 = "TAKE THE EMPTY TUBES OUT.";
                    warn = true;
                }
                case 5 -> {
                    line1 = "NO POWER";
                    line2 = "CONNECT POWER UNDER THE MACHINE.";
                    warn = true;
                }
                default -> {
                    line1 = "READY \u00b7 ONE DOSE INTO THE SAMPLE";
                    line2 = auto ? "STARTING..." : "PRESS START TO RECOMBINE.";
                }
            }
        }
        box(graphics, 6, 186, SW - 12, 24, warn ? A : M, P);
        text(graphics, 10, 190, line1, warn ? A : G);
        text(graphics, 10, 200, line2, M);
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
        var running = running();
        var left = running ? (GeneRecombinatorBlockEntity.RUN_TICKS - menu.data(GeneRecombinatorMenu.DATA_PROGRESS) + 19) / 20 : 0;
        var auto = menu.data(GeneRecombinatorMenu.DATA_AUTO) != 0;
        kv(graphics, BY + 14, "TIME", GeneRecombinatorBlockEntity.RUN_TICKS / 20 + " S", G);
        kv(graphics, BY + 24, "POWER", GeneRecombinatorBlockEntity.RUN_POWER + " FE/T", G);
        kv(graphics, BY + 34, "TIME LEFT", running ? left + " S" : "-", running ? A : M);
        kv(graphics, BY + 44, "AUTO START", auto ? "ON" : "OFF", auto ? G : M);
        box(graphics, RX + 4, BY + 66, RWID - 8, 14, G, D);
        var label = running ? "STOP" : "START";
        text(graphics, RX + 4 + (RWID - 8 - font.width(label)) / 2, BY + 69, label, G);

        var footY = BY + 88;
        text(graphics, 6, footY, "RIGHT-CLICK A TUBE ONTO AN ARM", M);
        textRight(graphics, SW - 7, footY, "SAME GENE ONLY", M);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var x = (int) Math.floor(mouseX - leftPos);
        var y = (int) Math.floor(mouseY - topPos);
        if (button == 0) {
            if (inside(x, y, NX + 4, 56, NW - 8, 12)) {
                filter.setFocused(true);
                setFocused(filter);
                filterTouched = true;
                return true;
            }
            filter.setFocused(false);
            if (inside(x, y, NX + 4, ROWS_TOP, NW - 8, VISIBLE_ROWS * 10)) {
                var row = scroll + (y - ROWS_TOP) / 10;
                var list = filtered();
                if (row < list.size()) {
                    selected = row;
                    Human.MOD.networking().sendToServer(new C2SNetworkTubePullPayload(list.get(row).index()));
                    click();
                }
                return true;
            }
            if (inside(x, y, NX + 4, 164, NW - 8, 14)) {
                return press(GeneRecombinatorMenu.BUTTON_PULL);
            }
            if (inside(x, y, RX + 4, BY + 44, RWID - 8, 10)) {
                return press(GeneRecombinatorMenu.BUTTON_AUTO);
            }
            if (inside(x, y, RX + 4, BY + 66, RWID - 8, 14)) {
                return press(GeneRecombinatorMenu.BUTTON_START_STOP);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var x = (int) (mouseX - leftPos);
        var y = (int) (mouseY - topPos);
        if (inside(x, y, NX, 42, NW, 140)) {
            scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (filter.isFocused() && keyCode != 256) {
            return filter.keyPressed(keyCode, scanCode, modifiers) || filter.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
        click();
        return true;
    }

    private void click() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.25F, 1.0F);
        }
    }

    private static boolean inside(int x, int y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Names and values
    // ------------------------------------------------------------------------------------------------------------

    private static String geneName(ResourceLocation id) {
        var gene = GeneRegistry.getValueOrNull(id);
        if (gene == null) {
            return id.getPath().replace('_', ' ').toUpperCase(Locale.ROOT);
        }
        var name = Component.translatable(gene.getTranslationKey()).getString();
        return (name.equals(gene.getTranslationKey()) ? id.getPath().replace('_', ' ') : name).toUpperCase(Locale.ROOT);
    }

    private static String amount(GeneTubeItem.Contents contents, double value) {
        return format(contents.gene(), contents.operation().name(), value);
    }

    private static String amount(ClientNetworkTubes.Entry entry) {
        return format(entry.gene(), entry.operation(), entry.value());
    }

    private static String format(ResourceLocation id, String operation, double value) {
        var gene = GeneRegistry.getValueOrNull(id);
        if (gene != null && Genes.category(gene) == Genes.Category.LOOT) {
            return "+" + Math.round(value * 100) + "% DROP";
        }
        if (GeneOperationType.MULTIPLICATIVE.name().equals(operation)) {
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

    private void tube(GuiGraphics graphics, int x, int y, int colour, boolean empty) {
        graphics.fill(x + 2, y, x + 4, y + 2, M);
        graphics.renderOutline(x + 1, y + 2, 4, 8, G);
        if (!empty) {
            graphics.fill(x + 2, y + 5, x + 4, y + 9, colour);
        }
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
