package com.human.client.screen;

import com.human.Human;
import com.human.common.gameplay.gene.AdvancedGenes;
import com.human.common.gameplay.gene.Gene;
import com.human.common.gameplay.gene.GeneDefects;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.gene.Genes;
import com.human.common.gameplay.gene.discovery.ClientGeneDiscoveries;
import com.human.common.gameplay.gene.reader.GeneReaderMenu;
import com.human.common.gameplay.gene.reader.GeneReaderSnapshot;
import com.human.common.gameplay.gene.reader.ReaderHistory;
import com.human.common.gameplay.harvest.HarvestGenes;
import com.human.common.gameplay.item.SyringeItem;
import com.human.common.network.packet.C2SSyringeLabelPayload;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The gene reader screen. ⚠ Built to the approved true-scale mockup, gene_reader_ingame_scale3.png ([stated] Oct 27:
 * "make it look like your promised design"): every box, row and text position below is that render's, in GUI units, on
 * a 384 x 314 panel. Same font and size throughout; text that could run past its frame is clipped to the frame.
 * <p>
 * Tabs: ACTIVE and DORMANT show the sampled mob (grouped STATS / LOOT / HARVEST / ADVANCED / DEFECTS); SYRINGE appears
 * while a syringe sits in the slot and shows what it holds and what injecting it costs. The right panel shows the mob's
 * integrity, generation, donor rest and harvest timers (queens: aberrant-egg chance); the detail bar explains the
 * selected gene and how many doses it is from its cap.
 * </p>
 */
public class GeneReaderScreen extends AbstractContainerScreen<GeneReaderMenu> {

    private static final int G = 0xFF6EF58C;

    private static final int M = 0xFF33974E;

    private static final int D = 0xFF051008;

    private static final int A = 0xFFF5B841;

    private static final int P = 0xFF09190F;

    private static final int W = 0xFF0E2816;

    private static final int SW = 384;

    private static final int SH = 314;

    // Layout constants straight from the mockup.
    private static final int LIST_Y = 59;

    private static final int LIST_H = 122;

    private static final int RIGHT_W = 124;

    private static final int LIST_W = SW - 12 - RIGHT_W - 4;

    private static final int ROW_X = 9;

    private static final int ROW_W = LIST_W - 15;

    private static final int DETAIL_Y = LIST_Y + LIST_H + 4;

    private static final int BOTTOM_Y = DETAIL_Y + 28;

    private static final int SYRINGE_X = 180;

    private static final int SYRINGE_W = SW - 6 - SYRINGE_X;

    private enum Tab {

        ACTIVE("ACTIVE"),
        DORMANT("DORMANT"),
        SYRINGE("SYRINGE"),
        HISTORY("HISTORY");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /** One list line: a section header, or a gene row. */
    private record Row(
        boolean header,
        String name,
        String value,
        int valueColour,
        String detail1,
        String detail2
    ) {}

    private Tab tab = Tab.ACTIVE;

    private int scroll;

    private int selected = -1;

    private List<Row> rows = List.of();

    private EditBox label;

    private ItemStack lastSyringe = ItemStack.EMPTY;

    /** A HISTORY sample opened with OPEN REPORT (its id), shown on the SYRINGE tab until the slot's syringe changes. */
    private @Nullable String viewing;

    private @Nullable LivingEntity icon;

    private String iconType = "";

    public GeneReaderScreen(GeneReaderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = SW;
        this.imageHeight = SH;
    }

    @Override
    protected void init() {
        super.init();
        topPos = Math.max(0, topPos);
        leftPos = Math.max(0, leftPos);

        label = new EditBox(font, leftPos + SYRINGE_X + 7, topPos + BOTTOM_Y + 44, SYRINGE_W - 14, 10, Component.literal("LABEL"));
        label.setBordered(false);
        label.setMaxLength(GeneReaderMenu.MAX_LABEL);
        label.setTextColor(G);
        label.setTextColorUneditable(M);
        label.setResponder(text -> {
            if (!menu.syringe().isEmpty()) {
                Human.MOD.networking().sendToServer(new C2SSyringeLabelPayload(text));
            }
        });
        addRenderableWidget(label);
        refreshSyringe();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------------------------------------------------

    private @Nullable GeneReaderSnapshot snapshot() {
        var player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return null;
        }
        var stack = player.getMainHandItem().is(HumanItems.GENE_READER.get()) ? player.getMainHandItem() : player.getOffhandItem();
        return GeneReaderSnapshot.readFrom(stack);
    }

    /** The gene reader this screen is for (main hand first, as {@link #snapshot()} reads it). */
    private ItemStack reader() {
        var player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        return player.getMainHandItem().is(HumanItems.GENE_READER.get()) ? player.getMainHandItem() : player.getOffhandItem();
    }

    private List<ReaderHistory.Entry> history() {
        return ReaderHistory.read(reader());
    }

    /** A sample the SYRINGE tab can show: the slot's syringe, or a HISTORY entry. */
    private record Sample(
        String name,
        @Nullable String donor,
        boolean analyzed,
        List<ReaderHistory.Gene> genes
    ) {}

    /**
     * [stated] Nov 1 - what the SYRINGE tab shows: a report opened from HISTORY, else the filled syringe in the slot,
     * else the last sample this reader read (the tab keeps its last reading), else nothing.
     */
    private @Nullable Sample shownSample() {
        var entries = history();
        if (viewing != null) {
            for (var entry : entries) {
                if (entry.id().equals(viewing)) {
                    return fromEntry(entry);
                }
            }
        }
        var syringe = menu.syringe();
        var contents = syringe.get(HumanDataComponents.SYRINGE_CONTENTS.get());
        if (contents != null && !contents.geneBonusDataEntries().isEmpty()) {
            var genes = contents.geneBonusDataEntries()
                .stream()
                .map(e -> new ReaderHistory.Gene(e.id(), e.operation(), e.value()))
                .toList();
            var name = syringe.get(DataComponents.CUSTOM_NAME);
            var id = SyringeItem.sampleIdOf(syringe);
            return new Sample(
                name != null ? name.getString() : id != null ? id : "SYRINGE",
                SyringeItem.donorOf(syringe),
                SyringeItem.isAnalyzed(syringe),
                genes
            );
        }
        return entries.isEmpty() ? null : fromEntry(entries.get(0));
    }

    private static Sample fromEntry(ReaderHistory.Entry entry) {
        return new Sample(entry.name(), entry.donor(), entry.analyzed(), entry.genes());
    }

    private static String donorName(@Nullable String donor) {
        if (donor == null) {
            return "UNKNOWN";
        }
        if (com.human.common.gameplay.gene.machine.GeneSplicing.SPLICED.equals(donor)) {
            return "SPLICED";
        }
        return EntityType.byString(donor).map(type -> type.getDescription().getString()).orElse(donor).toUpperCase(Locale.ROOT);
    }

    /** Whether this player has learned every gene in {@code sample} (the Gene Analyzer teaches them). */
    private static boolean learned(Sample sample) {
        return sample.genes().stream().allMatch(gene -> ClientGeneDiscoveries.knowsGene(gene.id()));
    }

    private long now() {
        return minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0L;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshSyringe();
    }

    private void refreshSyringe() {
        var syringe = menu.syringe();

        if (!ItemStack.matches(syringe, lastSyringe)) {
            var changedItem = syringe.isEmpty() != lastSyringe.isEmpty();
            lastSyringe = syringe.copy();

            if (changedItem || !label.isFocused()) {
                var name = syringe.get(DataComponents.CUSTOM_NAME);
                label.setValue(syringe.isEmpty() || name == null ? "" : name.getString());
            }

            label.setEditable(!syringe.isEmpty());

            // [stated] Nov 1: the SYRINGE tab keeps its last reading when the syringe comes out; a syringe put in (or
            // taken out) ends an opened HISTORY report, so the tab follows the slot again.
            viewing = null;
        }
    }

    private List<Tab> tabs() {
        return List.of(Tab.ACTIVE, Tab.DORMANT, Tab.SYRINGE, Tab.HISTORY);
    }

    private List<Row> buildRows(@Nullable GeneReaderSnapshot snapshot) {
        var list = new ArrayList<Row>();

        if (tab == Tab.HISTORY) {
            var entries = history();
            if (entries.isEmpty()) {
                list.add(new Row(true, "NO SAMPLES READ YET", "", M, "", ""));
                list.add(new Row(false, "PUT A FILLED SYRINGE IN THE SLOT", "", M, "", ""));
                return list;
            }
            list.add(new Row(true, "SAMPLES READ - " + entries.size() + " OF " + ReaderHistory.MAX, "", M, "", ""));
            for (var entry : entries) {
                var count = entry.genes().size();
                var value = entry.analyzed() ? count + (count == 1 ? " GENE" : " GENES") : "UNANALYZED";
                // What the Gene Analyzer still has to do for this player to learn the genes (tooltips, machine lists).
                var hint = learned(fromEntry(entry))
                    ? ""
                    : !ClientGeneDiscoveries.hasProfile(entry.donor())
                        ? "PROFILE THE DONOR WITH THE READER, THEN ANALYZE IT TO LEARN ITS GENES."
                        // Profiled: an analysis now teaches them - also for a sample analyzed before the profile.
                        : "ANALYZE IT IN A GENE ANALYZER TO LEARN ITS GENES.";
                list.add(
                    new Row(
                        false,
                        entry.name(),
                        value,
                        entry.analyzed() ? G : A,
                        entry.name() + " FROM " + donorName(entry.donor()) + ". OPEN REPORT TO SEE ITS GENES.",
                        hint
                    )
                );
            }
            return list;
        }

        if (tab == Tab.SYRINGE) {
            var sample = shownSample();
            if (sample == null) {
                list.add(new Row(true, "NO SAMPLE READ YET", "", M, "", ""));
                list.add(new Row(false, "PUT A FILLED SYRINGE IN THE SLOT", "", M, "", ""));
                return list;
            }
            list.add(
                new Row(
                    true,
                    sample.name() + " - FROM " + donorName(sample.donor()) + " - " + (sample.analyzed() ? "ANALYZED" : "UNANALYZED"),
                    "",
                    M,
                    "",
                    ""
                )
            );
            // [stated] Nov 1 (corrected): the reader shows a syringe's contents in full, named - reading samples is
            // what
            // it is for. Only the item tooltip keeps ??? until the Gene Analyzer teaches the genes.
            var lines = sample.genes()
                .stream()
                .map(gene -> new GeneReaderSnapshot.Line(gene.id().toString(), gene.operation().ordinal(), gene.value()))
                .toList();
            addGeneGroups(list, lines, null, snapshot, true);
            return list;
        }

        if (snapshot == null) {
            list.add(new Row(true, "NO SAMPLE", "", M, "", ""));
            list.add(new Row(false, "RIGHT-CLICK A MOB TO SAMPLE IT", "", M, "", ""));
            return list;
        }

        addGeneGroups(list, tab == Tab.ACTIVE ? snapshot.active() : snapshot.dormant(), snapshot, snapshot, false);

        if (list.isEmpty()) {
            list.add(new Row(true, tab == Tab.ACTIVE ? "NO ACTIVE GENES" : "NO DORMANT GENES", "", M, "", ""));
        }
        return list;
    }

    private void addGeneGroups(
        List<Row> list,
        List<GeneReaderSnapshot.Line> lines,
        @Nullable GeneReaderSnapshot timers,
        @Nullable GeneReaderSnapshot target,
        boolean syringe
    ) {
        var stats = new ArrayList<Row>();
        var loot = new ArrayList<Row>();
        var harvest = new ArrayList<Row>();
        var defects = new ArrayList<Row>();
        var detail1Syringe = "INJECTS AS DORMANT. WAKES IN THE NEXT GEN.";

        // Totals per gene (an old save can hold both operations).
        var totals = new java.util.LinkedHashMap<Gene, Double>();
        for (var line : lines) {
            var gene = GeneRegistry.getValueOrNull(net.minecraft.resources.ResourceLocation.parse(line.id()));
            if (gene != null && gene != Genes.GENETIC_INTEGRITY) {
                totals.merge(gene, line.value(), Double::sum);
            }
        }

        var bucketSubstances = new ArrayList<String>();
        for (var substance : HarvestGenes.Substance.values()) {
            if (totals.getOrDefault(substance.gene, 0.0) > 0) {
                bucketSubstances.add(name(substance.gene));
            }
        }

        for (var entry : totals.entrySet()) {
            var gene = entry.getKey();
            var value = entry.getValue();
            var category = Genes.category(gene);
            var desc = description(gene);

            switch (category) {
                case LOOT -> loot.add(
                    new Row(
                        false,
                        name(gene),
                        "+" + Math.round(value * 100) + "% DROP",
                        G,
                        syringe ? detail1Syringe : desc,
                        syringe ? desc : doses(value, 0.1, 30)
                    )
                );
                case HARVEST -> {
                    var tool = toolFor(gene);
                    if (tool == null) {
                        // A substance: shown on its action's row when the mob has one, otherwise on its own.
                        var hasAction = totals.getOrDefault(Genes.MILKABLE, 0.0) > 0 || (gene == Genes.STEW && totals.getOrDefault(
                            Genes.BOWL_FEEDING,
                            0.0
                        ) > 0);
                        if (syringe || timers == null || !hasAction) {
                            harvest.add(
                                new Row(
                                    false,
                                    name(gene),
                                    percent(value),
                                    G,
                                    syringe ? detail1Syringe : desc,
                                    syringe ? desc : doses(value, 1.0 / 8, 8)
                                )
                            );
                        }
                        continue;
                    }
                    var rowName = name(gene);
                    if (tool == HarvestGenes.Tool.BUCKET && !bucketSubstances.isEmpty()) {
                        rowName += " + " + String.join(" + ", bucketSubstances);
                    } else if (tool == HarvestGenes.Tool.BOWL) {
                        rowName += " + " + name(Genes.STEW);
                    }
                    var readyText = timers == null ? percent(value) : timeLeft(timers.harvestReadyAt()[tool.ordinal()]);
                    var readyColour = readyText.startsWith("WAIT") ? A : G;
                    harvest.add(
                        new Row(
                            false,
                            rowName,
                            readyText,
                            readyColour,
                            syringe ? detail1Syringe : desc,
                            syringe ? desc : doses(value, 1.0 / 8, 8)
                        )
                    );
                }
                case DEFECT -> {
                    if (value >= GeneDefects.CARRIED) {
                        defects.add(
                            new Row(
                                false,
                                name(gene),
                                "CARRIED",
                                A,
                                syringe ? detail1Syringe : desc,
                                "FROM LOW INTEGRITY. PASSES TO OFFSPRING."
                            )
                        );
                    }
                }
                case ADVANCED -> { /* engineered/advanced are listed from the snapshot's states below */ }
                default -> stats.add(
                    new Row(
                        false,
                        name(gene),
                        statValue(gene, value),
                        value < 0 ? A : G,
                        syringe ? detail1Syringe : desc,
                        syringe ? desc : doses(Math.abs(value), Genes.cap(gene) / 8.0, 8)
                    )
                );
            }
        }

        section(list, syringe ? null : "STATS", stats);
        section(list, "LOOT", loot);
        section(list, "HARVEST", harvest);

        if (!syringe && target != null && tab == Tab.ACTIVE) {
            var advanced = new ArrayList<Row>();
            for (var state : target.advanced()) {
                var gene = GeneRegistry.getValueOrNull(net.minecraft.resources.ResourceLocation.parse(state.id()));
                if (gene == null) {
                    continue;
                }
                var definition = AdvancedGenes.definition(gene);
                var needs = definition == null
                    ? ""
                    : "NEEDS " + String.join(" + ", definition.recipe().stream().map(GeneReaderScreen::name).toList()) + " AT " + Math
                        .round(com.human.common.gameplay.gene.AdvancedGenes.THRESHOLD * 100) + "% CAP";
                advanced.add(
                    new Row(
                        false,
                        name(gene) + " \u00b7 " + (state.engineered() ? "ENGINEERED" : "MUTATION"),
                        state.status().name(),
                        state.status() == AdvancedGenes.Status.ACTIVE ? G : M,
                        description(gene),
                        needs
                    )
                );
            }
            section(list, "ADVANCED", advanced);
        }

        section(list, "DEFECTS", defects);
    }

    private static void section(List<Row> list, @Nullable String title, List<Row> rows) {
        if (rows.isEmpty()) {
            return;
        }
        if (title != null) {
            list.add(new Row(true, title, "", M, "", ""));
        }
        list.addAll(rows);
    }

    private static @Nullable HarvestGenes.Tool toolFor(Gene gene) {
        for (var tool : HarvestGenes.Tool.values()) {
            if (tool.gene == gene) {
                return tool;
            }
        }
        return null;
    }

    private static String name(Gene gene) {
        return Component.translatable(gene.getTranslationKey()).getString().toUpperCase(Locale.ROOT);
    }

    private static String description(Gene gene) {
        return Component.translatable(gene.getDescriptionKey()).getString().toUpperCase(Locale.ROOT);
    }

    private static String statValue(Gene gene, double value) {
        var sign = value >= 0 ? "+" : "-";
        var magnitude = Math.abs(value);
        return Genes.operation(gene) == GeneOperationType.MULTIPLICATIVE
            ? sign + String.format(Locale.ROOT, "%.1f", magnitude * 100) + "%"
            : sign + String.format(Locale.ROOT, "%.1f", magnitude);
    }

    private static String percent(double value) {
        return "+" + Math.round(value * 100) + "%";
    }

    private static String doses(double value, double perDose, int toCap) {
        var count = (int) Math.min(toCap, Math.round(value / perDose));
        return count + " OF " + toCap + " DOSES TO CAP";
    }

    private String timeLeft(long readyAt) {
        var remaining = readyAt - now();
        if (remaining <= 0) {
            return "READY";
        }
        var seconds = (remaining + 19) / 20;
        return String.format(Locale.ROOT, "WAIT %d:%02d", seconds / 60, seconds % 60);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // Every label is drawn in renderBg at the mockup's positions.
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        var snapshot = snapshot();
        rows = buildRows(snapshot);
        if (selected >= rows.size() || (selected >= 0 && rows.get(selected).header())) {
            selected = -1;
        }

        var x0 = leftPos;
        var y0 = topPos;
        graphics.pose().pushPose();
        graphics.pose().translate(x0, y0, 0);

        graphics.fill(0, 0, SW, SH, D);

        drawHeader(graphics, snapshot);
        drawTabs(graphics);
        drawList(graphics);
        drawRightPanel(graphics, snapshot);
        drawDetail(graphics);
        drawInventoryPanel(graphics);
        drawSyringePanel(graphics);
        drawFooter(graphics);

        graphics.pose().popPose();

        drawIcon(graphics, snapshot, mouseX, mouseY);
    }

    private void box(GuiGraphics graphics, int x, int y, int w, int h, int border, int fill) {
        if (fill != 0) {
            graphics.fill(x, y, x + w, y + h, fill);
        }
        graphics.renderOutline(x, y, w, h, border);
    }

    private void line(GuiGraphics graphics, int x1, int x2, int y, int colour) {
        graphics.fill(x1, y, x2 + 1, y + 1, colour);
    }

    private void text(GuiGraphics graphics, int x, int y, String text, int colour) {
        graphics.drawString(font, text, x, y, colour, false);
    }

    private void textRight(GuiGraphics graphics, int rightX, int y, String text, int colour) {
        graphics.drawString(font, text, rightX - font.width(text) + 1, y, colour, false);
    }

    private void drawHeader(GuiGraphics graphics, @Nullable GeneReaderSnapshot snapshot) {
        box(graphics, 6, 6, SW - 12, 32, G, P);
        box(graphics, 9, 9, 26, 26, G, W);

        var title = "GENE READER // SAMPLE";
        text(graphics, 40, 11, title, G);

        var target = snapshot == null ? "NO SAMPLE" : (snapshot.name() + " \u00b7 " + snapshot.type()).toUpperCase(Locale.ROOT);
        var room = (SW - 10) - (40 + font.width(title) + 8);
        TerminalText.draw(graphics, font, leftPos, topPos, SW - 10 - room + 1, 11, room, target, M, true);

        if (snapshot == null) {
            return;
        }

        var chips = List.of(
            snapshot.modified() ? "MODIFIED" : "NATURAL",
            "HEALTH " + Math.round(snapshot.health()) + "/" + Math.round(snapshot.maxHealth())
        );
        var x = 40;
        for (var i = 0; i < chips.size(); i++) {
            var chip = chips.get(i);
            var w = font.width(chip) + 5;
            if (i == 0) {
                box(graphics, x, 22, w, 11, G, G);
                text(graphics, x + 3, 24, chip, D);
            } else {
                box(graphics, x, 22, w, 11, M, 0);
                text(graphics, x + 3, 24, chip, G);
            }
            x += w + 3;
        }
    }

    private void drawIcon(GuiGraphics graphics, @Nullable GeneReaderSnapshot snapshot, int mouseX, int mouseY) {
        if (snapshot == null || minecraft == null || minecraft.level == null) {
            return;
        }

        if (!snapshot.type().equals(iconType)) {
            iconType = snapshot.type();
            icon = EntityType.byString(iconType)
                .map(type -> type.create(minecraft.level))
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .orElse(null);
        }

        if (icon == null) {
            return;
        }

        try {
            var size = Math.max(icon.getBbHeight(), icon.getBbWidth());
            var scale = Math.max(1, (int) (20 / Math.max(0.5F, size)));
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                leftPos + 10,
                topPos + 10,
                leftPos + 34,
                topPos + 34,
                scale,
                0.0625F,
                mouseX,
                mouseY,
                icon
            );
        } catch (RuntimeException exception) {
            // A modded mob whose renderer won't draw out of world: leave the box empty rather than break the screen.
            icon = null;
        }
    }

    private void drawTabs(GuiGraphics graphics) {
        var x = 6;
        for (var each : tabs()) {
            var w = font.width(each.label) + 9;
            var on = each == tab;
            box(graphics, x, 42, w, 13, G, on ? G : P);
            text(graphics, x + 5, 45, each.label, on ? D : G);
            x += w + 3;
        }
    }

    private int visibleRowsFrom(int first) {
        var y = LIST_Y + 3;
        var count = 0;
        for (var i = first; i < rows.size(); i++) {
            if (y > LIST_Y + LIST_H - 12) {
                break;
            }
            y += rows.get(i).header() ? 11 : 10;
            count++;
        }
        return count;
    }

    private void drawList(GuiGraphics graphics) {
        box(graphics, 6, LIST_Y, LIST_W, LIST_H, G, W);

        scroll = Math.clamp(scroll, 0, Math.max(0, maxScroll()));

        var sbx = 6 + LIST_W - 8;
        box(graphics, sbx, LIST_Y + 3, 5, LIST_H - 6, M, D);
        var track = LIST_H - 8;
        var visible = visibleRowsFrom(scroll);
        var total = Math.max(1, rows.size());
        var thumb = Math.max(4, track * Math.min(total, visible) / total);
        var thumbTop = maxScroll() == 0 ? 0 : (track - thumb) * scroll / maxScroll();
        graphics.fill(sbx + 1, LIST_Y + 4 + thumbTop, sbx + 4, LIST_Y + 4 + thumbTop + thumb, M);

        var y = LIST_Y + 3;
        for (var i = scroll; i < rows.size(); i++) {
            if (y > LIST_Y + LIST_H - 12) {
                break;
            }
            var row = rows.get(i);
            if (row.header()) {
                TerminalText.draw(graphics, font, leftPos, topPos, ROW_X + 2, y + 1, ROW_W - 4, row.name(), M, false);
                line(graphics, ROW_X, ROW_X + ROW_W - 1, y + 9, M);
                y += 11;
                continue;
            }

            var valueWidth = font.width(row.value());
            var nameRoom = ROW_W - 4 - valueWidth - 6;
            if (i == selected) {
                graphics.fill(ROW_X, y, ROW_X + ROW_W, y + 10, G);
                TerminalText.draw(graphics, font, leftPos, topPos, ROW_X + 2, y + 1, nameRoom, row.name(), D, false);
                textRight(graphics, ROW_X + ROW_W - 3, y + 1, row.value(), D);
            } else {
                TerminalText.draw(graphics, font, leftPos, topPos, ROW_X + 2, y + 1, nameRoom, row.name(), G, false);
                textRight(graphics, ROW_X + ROW_W - 3, y + 1, row.value(), row.valueColour());
            }
            y += 10;
        }
    }

    private int maxScroll() {
        // The furthest first row that still fills the list.
        for (var first = 0; first < rows.size(); first++) {
            if (first + visibleRowsFrom(first) >= rows.size()) {
                return first;
            }
        }
        return 0;
    }

    private void drawRightPanel(GuiGraphics graphics, @Nullable GeneReaderSnapshot snapshot) {
        var px = 6 + LIST_W + 4;
        var integrity = snapshot == null ? 0.0 : snapshot.integrity();
        var warn = tab != Tab.SYRINGE && snapshot != null && integrity < -1;
        box(graphics, px, LIST_Y, RIGHT_W, LIST_H, warn ? A : G, P);

        var x = px + 4;
        var w = RIGHT_W - 8;
        var y = LIST_Y + 4;

        if (tab == Tab.HISTORY) {
            drawHistoryPanel(graphics, x, y, w);
            return;
        }
        if (tab == Tab.SYRINGE) {
            // The cost of injecting the sample the tab is showing (slot, opened report or last read).
            var sample = shownSample();
            var genes = sample == null ? 0 : (int) sample.genes().stream().map(ReaderHistory.Gene::id).distinct().count();
            var cost = genes == 0
                ? 0.0
                : SyringeItem.INTEGRITY_COST_PER_INJECTION + SyringeItem.INTEGRITY_COST_PER_EXTRA_GENE * Math.max(0, genes - 1);

            text(graphics, x, y, "INJECT COST", M);
            y += 10;
            kv(graphics, x, y, w, "BASE", money(SyringeItem.INTEGRITY_COST_PER_INJECTION), A);
            y += 10;
            if (genes > 1) {
                kv(graphics, x, y, w, "EXTRA GENE X" + (genes - 1), money(SyringeItem.INTEGRITY_COST_PER_EXTRA_GENE * (genes - 1)), A);
                y += 10;
            }
            line(graphics, x, x + w - 1, y, M);
            y += 4;
            kv(graphics, x, y, w, "TOTAL", money(cost), A);
            y += 10;
            if (snapshot != null) {
                var after = snapshot.integrity() + cost;
                kv(graphics, x, y, w, "AFTER", money(after), after < -1 ? A : G);
            }
            return;
        }

        if (snapshot == null) {
            text(graphics, x, y, "INTEGRITY", M);
            y += 10;
            text(graphics, x, y, "NO SAMPLE", M);
            return;
        }

        text(graphics, x, y, "INTEGRITY", M);
        y += 10;

        var colour = warn ? A : G;
        var status = integrity < -3 ? "FATAL" : integrity < -2 ? "VOLATILE" : integrity < -1 ? "UNSTABLE" : "STABLE";
        text(graphics, x, y, money(integrity), colour);
        var chipWidth = font.width(status) + 5;
        box(graphics, x + w - chipWidth, y - 1, chipWidth, 10, colour, colour);
        text(graphics, x + w - chipWidth + 3, y, status, D);
        y += 11;

        box(graphics, x, y, w, 7, M, D);
        var pct = Math.min(1.0, Math.abs(integrity) / 3.0);
        var barEnd = x + 1 + (int) ((w - 2) * pct);
        if (barEnd > x + 1) {
            graphics.fill(x + 1, y + 1, barEnd, y + 6, colour);
        }
        y += 9;

        var ticks = new String[] { "0", "-1", "-2", "-3" };
        for (var j = 0; j < ticks.length; j++) {
            var tickX = x + (w - 1) * j / 3;
            if (j == 0) {
                text(graphics, tickX, y, ticks[j], M);
            } else if (j == 3) {
                textRight(graphics, x + w - 1, y, ticks[j], M);
            } else {
                text(graphics, tickX - font.width(ticks[j]) / 2, y, ticks[j], M);
            }
        }
        y += 10;
        line(graphics, x, x + w - 1, y, M);
        y += 4;

        if (snapshot.aberrantChance() >= 0) {
            kv(graphics, x, y, w, "ABERRANT EGGS", Math.round(snapshot.aberrantChance() * 100) + "%", A);
            y += 10;
        }
        kv(graphics, x, y, w, "GENERATION", snapshot.modified() ? String.valueOf(snapshot.generation()) : "-", G);
        y += 10;
        // Strain only exists in a modified line; it reads amber from 0.5 up.
        kv(
            graphics,
            x,
            y,
            w,
            "STRAIN",
            snapshot.modified() ? String.format(Locale.ROOT, "%.2f", snapshot.strain()) : "N/A",
            snapshot.strain() >= 0.5 ? A : G
        );
        y += 10;

        var donor = timeLeft(snapshot.donorReadyAt());
        kv(graphics, x, y, w, "DONOR", donor, donor.startsWith("WAIT") ? A : G);
        y += 10;

        var soonest = Long.MAX_VALUE;
        for (var readyAt : snapshot.harvestReadyAt()) {
            if (readyAt >= 0) {
                soonest = Math.min(soonest, readyAt);
            }
        }
        if (soonest != Long.MAX_VALUE) {
            var harvest = timeLeft(soonest);
            kv(graphics, x, y, w, "HARVEST", harvest, harvest.startsWith("WAIT") ? A : G);
        }
    }

    /** The HISTORY entry the selected row is (row 0 is the header), or null. */
    private ReaderHistory.@Nullable Entry selectedEntry() {
        if (tab != Tab.HISTORY || selected < 1) {
            return null;
        }
        var entries = history();
        return selected - 1 < entries.size() ? entries.get(selected - 1) : null;
    }

    /** OPEN REPORT, at the foot of the HISTORY panel (approved mockup Nov 1). */
    private static final int REPORT_BUTTON_Y = LIST_Y + LIST_H - 19;

    /** [stated] Nov 1 mockup - the HISTORY tab's panel: the selected sample, and OPEN REPORT. */
    private void drawHistoryPanel(GuiGraphics graphics, int x, int y, int w) {
        var entry = selectedEntry();
        if (entry == null) {
            text(graphics, x, y, "SAMPLE", M);
            text(graphics, x, y + 10, "SELECT A SAMPLE", M);
            return;
        }
        TerminalText.draw(graphics, font, leftPos, topPos, x, y, w, entry.name(), M, false);
        y += 10;
        // A long (modded) species name scrolls inside its space rather than being cut off.
        var donorRoom = w - font.width("DONOR") - 6;
        text(graphics, x, y, "DONOR", M);
        TerminalText.draw(graphics, font, leftPos, topPos, x + w - donorRoom, y, donorRoom, donorName(entry.donor()), G, true);
        y += 10;
        kv(graphics, x, y, w, "STATUS", entry.analyzed() ? "ANALYZED" : "UNANALYZED", entry.analyzed() ? G : A);
        y += 10;
        kv(graphics, x, y, w, "GENES", String.valueOf(entry.genes().size()), G);
        y += 10;
        var profiled = ClientGeneDiscoveries.hasProfile(entry.donor());
        kv(graphics, x, y, w, "DONOR PROFILED", profiled ? "YES" : "NO", profiled ? G : A);
        y += 10;
        line(graphics, x, x + w - 1, y, M);
        box(graphics, x, REPORT_BUTTON_Y, w, 14, G, D);
        var open = "OPEN REPORT";
        text(graphics, x + (w - font.width(open)) / 2, REPORT_BUTTON_Y + 3, open, G);
    }

    private void kv(GuiGraphics graphics, int x, int y, int w, String key, String value, int valueColour) {
        var valueWidth = font.width(value);
        TerminalText.draw(graphics, font, leftPos, topPos, x, y, w - valueWidth - 4, key, M, false);
        textRight(graphics, x + w - 1, y, value, valueColour);
    }

    private static String money(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void drawDetail(GuiGraphics graphics) {
        box(graphics, 6, DETAIL_Y, SW - 12, 24, M, P);
        var row = selected >= 0 && selected < rows.size() ? rows.get(selected) : null;
        var line1 = row == null
            ? tab == Tab.HISTORY ? "SELECT A SAMPLE TO SEE ITS DETAILS." : "SELECT A GENE TO SEE WHAT IT DOES."
            : row.detail1();
        var line2 = row == null ? "" : row.detail2();
        TerminalText.draw(graphics, font, leftPos, topPos, 10, DETAIL_Y + 4, SW - 20, line1, G, false);
        TerminalText.draw(graphics, font, leftPos, topPos, 10, DETAIL_Y + 14, SW - 20, line2, M, false);
    }

    private void drawInventoryPanel(GuiGraphics graphics) {
        box(graphics, 6, BOTTOM_Y, 170, 84, G, P);
        for (var r = 0; r < 3; r++) {
            for (var c = 0; c < 9; c++) {
                box(graphics, 10 + c * 18, BOTTOM_Y + 4 + r * 18, 18, 18, M, W);
            }
        }
        for (var c = 0; c < 9; c++) {
            box(graphics, 10 + c * 18, BOTTOM_Y + 62, 18, 18, M, c == menu.lockedHotbarSlot() ? D : W);
        }
    }

    private void drawSyringePanel(GuiGraphics graphics) {
        var has = !menu.syringe().isEmpty();
        box(graphics, SYRINGE_X, BOTTOM_Y, SYRINGE_W, 84, G, P);
        box(graphics, SYRINGE_X + 4, BOTTOM_Y + 4, 22, 22, G, W);
        text(graphics, SYRINGE_X + 30, BOTTOM_Y + 6, "SYRINGE", G);
        text(graphics, SYRINGE_X + 30, BOTTOM_Y + 16, has ? "CLICK SLOT TO TAKE OUT" : "DRAG A SYRINGE HERE", M);
        text(graphics, SYRINGE_X + 4, BOTTOM_Y + 31, "LABEL", M);
        box(graphics, SYRINGE_X + 4, BOTTOM_Y + 41, SYRINGE_W - 8, 14, has ? G : M, D);
        box(graphics, SYRINGE_X + 4, BOTTOM_Y + 66, SYRINGE_W - 8, 14, has ? G : M, D);
        var clear = "CLEAR SAMPLE";
        text(graphics, SYRINGE_X + 4 + (SYRINGE_W - 8 - font.width(clear)) / 2, BOTTOM_Y + 69, clear, has ? G : M);
    }

    private void drawFooter(GuiGraphics graphics) {
        var y = BOTTOM_Y + 88;
        var a = "RIGHT-CLICK MOB: SAMPLE";
        var b = "SHIFT-CLICK: SELF";
        var c = "RIGHT-CLICK AIR: OPEN";
        text(graphics, 6, y, a, M);
        text(graphics, (SW - font.width(b)) / 2, y, b, M);
        textRight(graphics, SW - 7, y, c, M);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var x = mouseX - leftPos;
        var y = mouseY - topPos;

        if (button == 0) {
            // Tabs.
            var tabX = 6;
            for (var each : tabs()) {
                var w = font.width(each.label) + 9;
                if (x >= tabX && x < tabX + w && y >= 42 && y < 55) {
                    if (tab != each) {
                        tab = each;
                        scroll = 0;
                        selected = -1;
                    }
                    return true;
                }
                tabX += w + 3;
            }

            // List rows.
            if (x >= ROW_X && x < ROW_X + ROW_W && y >= LIST_Y + 3 && y < LIST_Y + LIST_H - 3) {
                var rowY = LIST_Y + 3;
                for (var i = scroll; i < rows.size(); i++) {
                    var height = rows.get(i).header() ? 11 : 10;
                    if (rowY > LIST_Y + LIST_H - 12) {
                        break;
                    }
                    if (y >= rowY && y < rowY + height) {
                        if (!rows.get(i).header()) {
                            selected = i;
                        }
                        return true;
                    }
                    rowY += height;
                }
            }

            // Scrollbar track: jump.
            var sbx = 6 + LIST_W - 8;
            if (x >= sbx && x < sbx + 5 && y >= LIST_Y + 3 && y < LIST_Y + LIST_H - 3) {
                var fraction = (y - (LIST_Y + 4)) / (double) (LIST_H - 8);
                scroll = (int) Math.round(fraction * maxScroll());
                return true;
            }

            // OPEN REPORT: the selected HISTORY sample, shown on the SYRINGE tab.
            var reportX = 6 + LIST_W + 4 + 4;
            var reportW = RIGHT_W - 8;
            var entry = selectedEntry();
            if (
                entry != null && x >= reportX && x < reportX + reportW && y >= REPORT_BUTTON_Y && y < REPORT_BUTTON_Y + 14
            ) {
                viewing = entry.id();
                tab = Tab.SYRINGE;
                scroll = 0;
                selected = -1;
                return true;
            }

            // CLEAR SAMPLE.
            if (
                !menu.syringe().isEmpty() && x >= SYRINGE_X + 4 && x < SYRINGE_X + SYRINGE_W - 4 && y >= BOTTOM_Y + 66
                    && y < BOTTOM_Y + 80 && minecraft != null && minecraft.gameMode != null
            ) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, GeneReaderMenu.BUTTON_CLEAR_SAMPLE);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var x = mouseX - leftPos;
        var y = mouseY - topPos;

        if (x >= 6 && x < 6 + LIST_W && y >= LIST_Y && y < LIST_Y + LIST_H) {
            scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, maxScroll()));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Typing a label: every key goes to the field (so E or a number key doesn't close or swap), except Escape.
        if (label.isFocused() && keyCode != 256 /* GLFW escape */) {
            return label.keyPressed(keyCode, scanCode, modifiers) || label.canConsumeInput();
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
