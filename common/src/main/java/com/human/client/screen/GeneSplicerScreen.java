package com.human.client.screen;

import com.human.Human;
import com.human.common.gameplay.gene.AdvancedGenes;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.gene.Genes;
import com.human.common.gameplay.gene.discovery.ClientGeneDiscoveries;
import com.human.common.gameplay.gene.machine.GeneSplicing;
import com.human.common.gameplay.gene.machine.splicer.ClientSplicerLabel;
import com.human.common.gameplay.gene.machine.splicer.GeneSplicerBlockEntity;
import com.human.common.gameplay.gene.machine.splicer.GeneSplicerMenu;
import com.human.common.gameplay.gene.machine.storage.ClientNetworkTubes;
import com.human.common.gameplay.gene.tube.GeneTubeItem;
import com.human.common.network.packet.C2SNetworkTubePullPayload;
import com.human.common.network.packet.C2SSplicerLabelPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * The Gene Splicer screen - built to the approved mockup (gene_splicer_ingame_scale3.png, 384x334, Oct 28) at its exact
 * positions: header and chips; SYRINGE BUILD with the five numbered holders, IN and OUT, the gene list (amber
 * mutations, red defects), PIPETTE and EMPTIES BACK; NETWORK STORAGE with search and IN SYRINGE marking; the coloured
 * status strip; inventory; INJECT COST with the LABEL field and START / STOP.
 * <p>
 * Every fixed string is measured to fit; game names wider than their frame scroll inside it ({@link TerminalText}).
 * </p>
 */
public class GeneSplicerScreen extends AbstractContainerScreen<GeneSplicerMenu> {

    private static final int G = 0xFF6EF58C;

    private static final int M = 0xFF33974E;

    private static final int D = 0xFF051008;

    private static final int A = 0xFFF5B841;

    private static final int R = 0xFFF56E6E;

    private static final int P = 0xFF09190F;

    private static final int W = 0xFF0E2816;

    private static final int SW = GeneSplicerMenu.SCREEN_WIDTH;

    private static final int SH = GeneSplicerMenu.SCREEN_HEIGHT;

    private static final int NX = 190;

    private static final int NW = SW - 6 - NX;

    private static final int BY = 220;

    private static final int RX = 180;

    private static final int RWID = SW - 6 - RX;

    private static final int ROWS_TOP = 72;

    private static final int VISIBLE_ROWS = 9;

    private static final String[] ICON = {
        "....gg....",
        "....gg....",
        "...mggm...",
        "...m..m...",
        "...m..m...",
        "...mggm...",
        "....mm....",
        "....mm....",
        ".mmmmmmmm."
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

    private EditBox search;

    private EditBox labelBox;

    private int labelVersion = -1;

    private int scroll;

    public GeneSplicerScreen(GeneSplicerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = SW;
        this.imageHeight = SH;
    }

    @Override
    protected void init() {
        super.init();
        topPos = Math.max(0, topPos);
        leftPos = Math.max(0, leftPos);
        search = new EditBox(font, leftPos + NX + 7, topPos + 58, NW - 60, 10, Component.literal("SEARCH"));
        search.setBordered(false);
        search.setMaxLength(24);
        search.setTextColor(G);
        search.setResponder(text -> {
            var upper = text.toUpperCase(Locale.ROOT);
            if (!upper.equals(text)) {
                search.setValue(upper);
            }
            scroll = 0;
        });
        addRenderableWidget(search);

        labelBox = new EditBox(font, leftPos + RX + 39, topPos + BY + 47, RWID - 46, 10, Component.literal("LABEL"));
        labelBox.setBordered(false);
        labelBox.setMaxLength(GeneSplicerBlockEntity.MAX_LABEL);
        labelBox.setTextColor(G);
        labelBox.setResponder(text -> {
            var upper = text.toUpperCase(Locale.ROOT);
            if (!upper.equals(text)) {
                labelBox.setValue(upper);
                return;
            }
            if (labelVersion >= 0) {
                Human.MOD.networking().sendToServer(new C2SSplicerLabelPayload(text));
            }
        });
        addRenderableWidget(labelBox);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The machine's label arrives just after the screen opens.
        if (labelVersion != ClientSplicerLabel.version()) {
            labelVersion = -1;
            labelBox.setValue(ClientSplicerLabel.label());
            labelVersion = ClientSplicerLabel.version();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderSlot(@NotNull GuiGraphics graphics, @NotNull Slot slot) {
        if (slot.index >= GeneSplicerBlockEntity.SLOT_COUNT) {
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

    private List<net.minecraft.world.item.ItemStack> tubes() {
        var list = new ArrayList<net.minecraft.world.item.ItemStack>();
        for (int slot = 0; slot < GeneSplicerBlockEntity.TUBE_SLOTS; slot++) {
            list.add(menu.items().getItem(slot));
        }
        return list;
    }

    private boolean running() {
        return menu.data(GeneSplicerMenu.DATA_RUNNING) != 0;
    }

    private int loadedCount() {
        var count = 0;
        for (var tube : tubes()) {
            if (GeneTubeItem.read(tube) != null) {
                count++;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        var plan = GeneSplicing.plan(tubes());
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0);
        graphics.fill(0, 0, SW, SH, D);
        drawHeader(graphics, plan);
        drawBuild(graphics);
        drawNetwork(graphics);
        drawStatus(graphics, plan);
        drawBottom(graphics, plan);
        graphics.pose().popPose();
    }

    private String cost(GeneSplicing.Plan plan) {
        return plan.ok() ? String.format(Locale.ROOT, "%.2f", -Math.abs(plan.integrityCost())) : "-";
    }

    private void drawHeader(GuiGraphics graphics, GeneSplicing.Plan plan) {
        box(graphics, 6, 6, SW - 12, 32, G, P);
        box(graphics, 9, 9, 26, 26, G, W);
        art(graphics, ICON, 9 + (26 - ICON[0].length()) / 2, 9 + (26 - ICON.length) / 2);
        text(graphics, 40, 11, "GENE SPLICER // SYRINGE ASSEMBLY", G);
        var supplied = menu.data(GeneSplicerMenu.DATA_SUPPLIED) != 0;
        textRight(graphics, SW - 10, 11, !supplied ? "NO POWER" : running() ? "POWER OK \u00b7 40 FE/T" : "POWER OK", M);

        String status;
        int colour;
        if (running()) {
            status = "PIPETTING";
            colour = A;
        } else if (loadedCount() == 0) {
            status = "IDLE";
            colour = G;
        } else if (menu.data(GeneSplicerMenu.DATA_BLOCKER) >= 0) {
            status = "WAITING";
            colour = A;
        } else {
            status = "READY";
            colour = G;
        }
        var chips = new String[] { status, loadedCount() + " OF 5 GENES", "COST " + cost(plan) };
        var x = 40;
        for (int i = 0; i < chips.length; i++) {
            var w = font.width(chips[i]) + 5;
            if (i == 0) {
                box(graphics, x, 22, w, 11, colour, colour);
                text(graphics, x + 3, 24, chips[i], D);
            } else {
                box(graphics, x, 22, w, 11, M, 0);
                text(graphics, x + 3, 24, chips[i], G);
            }
            x += w + 3;
        }
    }

    private void drawBuild(GuiGraphics graphics) {
        box(graphics, 6, 42, 180, 146, G, P);
        text(graphics, 10, 46, "SYRINGE BUILD", M);
        var loaded = loadedCount();
        textRight(graphics, 181, 46, loaded + "/5", G);
        for (int k = 0; k < GeneSplicerBlockEntity.TUBE_SLOTS; k++) {
            var x0 = 10 + k * 22;
            var stack = menu.items().getItem(k);
            var full = GeneTubeItem.read(stack) != null;
            box(graphics, x0, 56, 18, 18, full ? G : M, W);
            if (full) {
                tube(graphics, x0 + 6, 60, GeneTubeItem.colour(stack) | 0xFF000000, false);
            }
            var number = String.valueOf(k + 1);
            text(graphics, x0 + 9 - font.width(number) / 2, 76, number, M);
        }
        box(graphics, 120, 56, 18, 18, M, W);
        var in = menu.items().getItem(GeneSplicerBlockEntity.IN_SLOT);
        if (!in.isEmpty()) {
            art(graphics, SYRINGE, 123, 60);
            if (in.getCount() > 1) {
                textRight(graphics, 137, 66, String.valueOf(in.getCount()), G);
            }
        }
        text(graphics, 129 - font.width("IN") / 2, 76, "IN", M);
        text(graphics, 141, 61, ">", M);
        box(graphics, 152, 54, 22, 22, G, W);
        if (!menu.items().getItem(GeneSplicerBlockEntity.OUT_SLOT).isEmpty()) {
            art(graphics, SYRINGE, 157, 60);
        }
        text(graphics, 163 - font.width("OUT") / 2, 78, "OUT", M);
        graphics.fill(10, 88, 182, 89, M);

        var y = 92;
        for (int k = 0; k < GeneSplicerBlockEntity.TUBE_SLOTS; k++) {
            var contents = GeneTubeItem.read(menu.items().getItem(k));
            if (contents == null) {
                text(graphics, 20, y, "- EMPTY -", M);
            } else {
                tube(graphics, 10, y - 1, GeneTubeItem.colour(menu.items().getItem(k)) | 0xFF000000, false);
                var known = ClientGeneDiscoveries.knowsGene(contents.gene());
                var kind = kind(contents.gene());
                var colour = kind == 1 ? A : kind == 2 ? R : G;
                var value = known ? value(contents.gene(), contents.operation(), contents.value(), kind) : "";
                var valueWidth = font.width(value);
                fit(
                    graphics,
                    20,
                    y,
                    162 - valueWidth - (valueWidth > 0 ? 6 : 0),
                    known ? geneName(contents.gene()) : "???",
                    known ? colour : M,
                    false
                );
                if (valueWidth > 0) {
                    textRight(graphics, 181, y, value, kind == 0 ? G : colour);
                }
            }
            y += 10;
        }
        graphics.fill(10, 144, 182, 145, M);

        var runTicks = menu.data(GeneSplicerMenu.DATA_RUN_TICKS);
        var progress = menu.data(GeneSplicerMenu.DATA_PROGRESS);
        var running = running();
        var stage = running && loaded > 0 && runTicks > 0 ? Math.min(loaded, 1 + progress * loaded / runTicks) : 0;
        text(graphics, 10, 148, "PIPETTE", M);
        textRight(graphics, 181, 148, running ? stage + " OF " + loaded : "-", G);
        box(graphics, 10, 158, 172, 7, M, D);
        var fill = running && runTicks > 0 ? 170 * Math.min(progress, runTicks) / runTicks : 0;
        if (fill > 0) {
            graphics.fill(11, 159, 11 + fill, 164, A);
        }
        for (int k = 1; k < 5; k++) {
            graphics.fill(10 + k * 34, 158, 11 + k * 34, 165, M);
        }
        text(graphics, 10, 170, "EMPTIES BACK", M);
        box(graphics, 164, 166, 18, 18, M, W);
        var empties = menu.items().getItem(GeneSplicerBlockEntity.EMPTIES_SLOT);
        if (!empties.isEmpty()) {
            tube(graphics, 170, 170, 0, true);
            textRight(graphics, 180, 177, String.valueOf(empties.getCount()), G);
        }
    }

    private List<ClientNetworkTubes.Entry> filtered() {
        var query = search.getValue().trim();
        var list = new ArrayList<ClientNetworkTubes.Entry>();
        for (var entry : ClientNetworkTubes.entries()) {
            var name = ClientGeneDiscoveries.knowsGene(entry.gene()) ? geneName(entry.gene()) : "???";
            if (query.isEmpty() || name.contains(query)) {
                list.add(entry);
            }
        }
        return list;
    }

    private void drawNetwork(GuiGraphics graphics) {
        box(graphics, NX, 42, NW, 146, G, P);
        text(graphics, NX + 4, 46, "NETWORK STORAGE", M);
        var list = filtered();
        var tubes = list.stream().mapToInt(ClientNetworkTubes.Entry::count).sum();
        textRight(graphics, NX + NW - 5, 46, tubes + (tubes == 1 ? " TUBE" : " TUBES"), G);
        box(graphics, NX + 4, 56, NW - 8, 12, G, D);
        if (search.getValue().isEmpty() && !search.isFocused()) {
            text(graphics, NX + 7, 58, "_", M);
            textRight(graphics, NX + NW - 8, 58, "SEARCH", M);
        }

        var inSyringe = new HashSet<ResourceLocation>();
        for (var tube : tubes()) {
            var contents = GeneTubeItem.read(tube);
            if (contents != null) {
                inSyringe.add(contents.gene());
            }
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - VISIBLE_ROWS)));
        var y = ROWS_TOP;
        if (list.isEmpty()) {
            text(graphics, NX + 6, y + 1, ClientNetworkTubes.entries().isEmpty() ? "NO TUBES ON THIS GRID" : "NONE MATCH", M);
        }
        for (int i = scroll; i < list.size() && i < scroll + VISIBLE_ROWS; i++) {
            var entry = list.get(i);
            var known = ClientGeneDiscoveries.knowsGene(entry.gene());
            var dim = inSyringe.contains(entry.gene());
            var kind = kind(entry.gene());
            var right = dim
                ? "IN SYRINGE"
                : known ? value(entry.gene(), GeneOperationType.valueOf(entry.operation()), entry.value(), kind) : "";
            // identical tubes stack in one rack slot: show how many this entry holds
            if (entry.count() > 1) {
                right = "x" + entry.count() + (right.isEmpty() ? "" : "  " + right);
            }
            var rightWidth = font.width(right);
            fit(
                graphics,
                NX + 6,
                y + 1,
                NW - 13 - rightWidth - (rightWidth > 0 ? 6 : 0),
                known ? geneName(entry.gene()) : "???",
                dim || !known ? M : G,
                false
            );
            if (rightWidth > 0) {
                textRight(graphics, NX + NW - 7, y + 1, right, M);
            }
            y += 10;
        }
        graphics.fill(NX + 4, 170, NX + NW - 4, 171, M);
        text(graphics, NX + 4, 174, "CLICK A TUBE TO ADD IT", M);
    }

    private void drawStatus(GuiGraphics graphics, GeneSplicing.Plan plan) {
        String line1;
        var line2 = "";
        var colour1 = G;
        var colour2 = M;
        var border = M;
        var blocker = menu.data(GeneSplicerMenu.DATA_BLOCKER);
        var genes = loadedCount();
        var seconds = GeneSplicing.runTicks(genes) / 20;
        var running = running();
        if (!plan.ok() && genes > 0) {
            line1 = plan.refusal().message;
            line2 = "TAKE THE TUBE IN HOLDER " + (plan.refusedIndex() + 1) + " OUT.";
            colour1 = A;
            border = A;
        } else if (!plan.mutationWarnings().isEmpty()) {
            var gene = plan.mutationWarnings().get(0);
            line1 = nameByPath(gene) + " WILL REPLACE THE TARGET'S " + category(gene) + " MUTATION";
            colour1 = A;
            border = A;
        } else if (!plan.defectWarnings().isEmpty()) {
            line1 = nameByPath(plan.defectWarnings().get(0)) + " IS A DEFECT \u00b7 IT WILL BE INJECTED TOO";
            colour1 = R;
            border = R;
        } else if (genes == 0) {
            line1 = "ADD UP TO 5 FILLED TUBES, ONE PER GENE";
            line2 = "CLICK A NETWORK TUBE, OR RIGHT-CLICK TUBES ONTO THE MACHINE.";
        } else {
            line1 = "READY \u00b7 " + genes + (genes == 1 ? " GENE" : " GENES") + " INTO ONE SYRINGE";
        }
        if (line2.isEmpty()) {
            if (running) {
                var stage = Math.min(
                    genes,
                    1 + menu.data(GeneSplicerMenu.DATA_PROGRESS) * Math.max(1, genes) / Math.max(
                        1,
                        menu.data(GeneSplicerMenu.DATA_RUN_TICKS)
                    )
                );
                line2 = "PIPETTING " + stage + " OF " + genes + " \u00b7 " + seconds + " SECONDS \u00b7 USED TUBES COME BACK EMPTY";
            } else {
                line2 = switch (blocker) {
                    case 5 -> "PUT AN EMPTY SYRINGE IN.";
                    case 6 -> "TAKE THE FINISHED SYRINGE OUT.";
                    case 7 -> "TAKE THE EMPTY TUBES OUT.";
                    case 8 -> "NO POWER. CONNECT POWER UNDERNEATH.";
                    case -1 -> "PRESS START TO SPLICE.";
                    default -> "";
                };
                if (blocker >= 5) {
                    colour2 = A;
                }
            }
        }
        box(graphics, 6, 192, SW - 12, 24, border, P);
        fit(graphics, 10, 196, SW - 20, line1, colour1, false);
        fit(graphics, 10, 206, SW - 20, line2, colour2, false);
    }

    private void drawBottom(GuiGraphics graphics, GeneSplicing.Plan plan) {
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
        var genes = plan.ok() ? plan.entries().size() : 0;
        text(graphics, RX + 4, BY + 4, "INJECT COST", M);
        textRight(graphics, RX + RWID - 5, BY + 4, genes == 0 ? "-" : GeneSplicing.runTicks(genes) / 20 + " S", M);
        // [stated] -0.10 per injection plus -0.05 per extra gene.
        kv(graphics, BY + 14, "BASE", genes == 0 ? "-" : "-0.10");
        kv(
            graphics,
            BY + 24,
            "EXTRA GENES X" + Math.max(0, genes - 1),
            genes <= 1 ? "-" : String.format(Locale.ROOT, "%.2f", -0.05D * (genes - 1))
        );
        kv(graphics, BY + 34, "TOTAL", genes == 0 ? "-" : cost(plan));
        text(graphics, RX + 4, BY + 46, "LABEL", M);
        box(graphics, RX + 36, BY + 44, RWID - 40, 13, G, D);
        if (labelBox.getValue().isEmpty() && !labelBox.isFocused()) {
            text(graphics, RX + 39, BY + 47, "SPLICED", M);
        }
        box(graphics, RX + 4, BY + 66, RWID - 8, 14, G, D);
        var label = running() ? "STOP" : "START";
        text(graphics, RX + 4 + (RWID - 8 - font.width(label)) / 2, BY + 69, label, G);

        var footY = BY + 88;
        text(graphics, 6, footY, "ONE TUBE PER GENE \u00b7 MAX 5", M);
        textRight(graphics, SW - 7, footY, "ITEM PORT TAKES EMPTY SYRINGES", M);
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
                focus(search);
                return true;
            }
            if (inside(x, y, RX + 36, BY + 44, RWID - 40, 13)) {
                focus(labelBox);
                return true;
            }
            search.setFocused(false);
            labelBox.setFocused(false);
            if (inside(x, y, NX + 4, ROWS_TOP, NW - 8, VISIBLE_ROWS * 10)) {
                var row = scroll + (y - ROWS_TOP) / 10;
                var list = filtered();
                if (row < list.size()) {
                    Human.MOD.networking().sendToServer(new C2SNetworkTubePullPayload(list.get(row).index()));
                    click();
                }
                return true;
            }
            if (inside(x, y, RX + 4, BY + 66, RWID - 8, 14)) {
                return press(GeneSplicerMenu.BUTTON_START_STOP);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void focus(EditBox box) {
        search.setFocused(box == search);
        labelBox.setFocused(box == labelBox);
        setFocused(box);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var x = (int) (mouseX - leftPos);
        var y = (int) (mouseY - topPos);
        if (inside(x, y, NX, 42, NW, 146)) {
            scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (var box : new EditBox[] { search, labelBox }) {
            if (box.isFocused() && keyCode != 256) {
                return box.keyPressed(keyCode, scanCode, modifiers) || box.canConsumeInput();
            }
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

    /** 0 base / loot / harvest, 1 mutation, 2 defect. */
    private static int kind(ResourceLocation id) {
        var gene = GeneRegistry.getValueOrNull(id);
        if (gene == null) {
            return 0;
        }
        if (AdvancedGenes.definition(gene) != null) {
            return 1;
        }
        return Genes.category(gene) == Genes.Category.DEFECT ? 2 : 0;
    }

    private static String category(String genePath) {
        for (var gene : Genes.specs().keySet()) {
            if (gene.id().getPath().equals(genePath)) {
                var definition = AdvancedGenes.definition(gene);
                if (definition != null) {
                    return definition.category().name();
                }
            }
        }
        return "";
    }

    /** A warning's gene (the plan names it by path): its name if this player knows it, else ???. */
    private static String nameByPath(String genePath) {
        for (var gene : Genes.specs().keySet()) {
            if (gene.id().getPath().equals(genePath)) {
                return ClientGeneDiscoveries.knowsGene(gene.id()) ? geneName(gene.id()) : "???";
            }
        }
        return "???";
    }

    private static String geneName(ResourceLocation id) {
        var gene = GeneRegistry.getValueOrNull(id);
        if (gene == null) {
            return id.getPath().replace('_', ' ').toUpperCase(Locale.ROOT);
        }
        var name = Component.translatable(gene.getTranslationKey()).getString();
        return (name.equals(gene.getTranslationKey()) ? id.getPath().replace('_', ' ') : name).toUpperCase(Locale.ROOT);
    }

    private static String value(ResourceLocation id, GeneOperationType operation, double value, int kind) {
        if (kind == 1) {
            return "MUTATION";
        }
        if (kind == 2) {
            return "DEFECT";
        }
        var gene = GeneRegistry.getValueOrNull(id);
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

    private void kv(GuiGraphics graphics, int y, String key, String value) {
        text(graphics, RX + 4, y, key, M);
        textRight(graphics, RX + RWID - 5, y, value, value.equals("-") ? M : A);
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
