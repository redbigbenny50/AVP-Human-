package com.human.client.screen;

import com.human.Human;
import com.human.common.gameplay.component.SyringeContents;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.gene.discovery.ClientGeneDiscoveries;
import com.human.common.gameplay.gene.machine.GeneSplicing;
import com.human.common.gameplay.gene.machine.analyzer.ClientAnalyzerDatabase;
import com.human.common.gameplay.gene.machine.analyzer.GeneAnalyzerBlockEntity;
import com.human.common.gameplay.gene.machine.analyzer.GeneAnalyzerMenu;
import com.human.common.gameplay.item.SyringeItem;
import com.human.common.network.packet.C2SAnalyzerPrintPayload;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The Gene Analyzer database screen - built to the approved mockup (genetic_analyzer_v2_ingame_scale3.png, 432x336, Oct
 * 28) at its exact positions: header with counts, the five tabs (GENES with BASE / LOOT/HARVEST / MUTATIONS, SPECIES,
 * SAMPLE, SEARCH, PRINT), the list with its scrollbar, the right-hand detail panel, the two-line description strip,
 * inventory, and the SAMPLE TRAY / SCAN / PRINT SUPPLIES panel.
 * <p>
 * Everything shown is this player's own knowledge ([stated] "DISCOVERIES ARE YOURS ONLY"): genes and species they have
 * not discovered are ???, donors they have not met are ???, and best-donor advice only ever uses discovered species.
 * </p>
 */
public class GeneAnalyzerScreen extends AbstractContainerScreen<GeneAnalyzerMenu> {

    private static final int G = 0xFF6EF58C;

    private static final int M = 0xFF33974E;

    private static final int D = 0xFF051008;

    private static final int A = 0xFFF5B841;

    private static final int P = 0xFF09190F;

    private static final int W = 0xFF0E2816;

    private static final int SW = GeneAnalyzerMenu.SCREEN_WIDTH;

    /** The mutation unlock threshold as shown, from the one dial in AdvancedGenes. */
    private static final String THRESHOLD_TEXT = Math.round(com.human.common.gameplay.gene.AdvancedGenes.THRESHOLD * 100) + "%";

    private static final int SH = GeneAnalyzerMenu.SCREEN_HEIGHT;

    private static final int RPW = 170;

    private static final int LW = SW - 12 - RPW - 4;

    private static final int LY = 72;

    private static final int LH = 131;

    private static final int DY = LY + LH + 4;

    private static final int BY = DY + 28;

    private static final int PX = 6 + LW + 4;

    private static final int PTOP = 58;

    private static final int RX = 9;

    private static final int RW = LW - 15;

    private static final int SX = 180;

    private static final int SWID = SW - 6 - SX;

    private static final String[] ICON = {
        "..mmmmmmmmmmmmmmmmmm..",
        ".m..................m.",
        ".m.gggg.gggg.gggg...m.",
        ".m.g..g.g..g.g..g...m.",
        ".m.gggg.gggg.gggg...m.",
        ".m..................m.",
        ".mmmmmmmmmmmmmmmmmmmm.",
        "...m..............m...",
        "...m...mmmmmmmm...m...",
        "...mmmmm......mmmmm..."
    };

    private enum Tab {

        GENES("GENES"),
        SPECIES("SPECIES"),
        SAMPLE("SAMPLE"),
        SEARCH("SEARCH"),
        PRINT("PRINT");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    /** One list line. {@code key} is the gene or species id it stands for; {@code check} marks a PRINT row. */
    private record Row(
        boolean header,
        String name,
        String value,
        int valueColour,
        @Nullable ResourceLocation key,
        boolean species,
        boolean check
    ) {

        static Row header(String name) {
            return new Row(true, name, "", M, null, false, false);
        }
    }

    private Tab tab = Tab.GENES;

    private int geneSub;

    private int printSub;

    private int scroll;

    private int selected = -1;

    private List<Row> rows = List.of();

    private int builtVersion = -1;

    private String builtKey = "";

    private final Set<ResourceLocation> tickedGenes = new LinkedHashSet<>();

    private final Set<ResourceLocation> tickedSpecies = new LinkedHashSet<>();

    private EditBox search;

    public GeneAnalyzerScreen(GeneAnalyzerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = SW;
        this.imageHeight = SH;
    }

    @Override
    protected void init() {
        super.init();
        topPos = Math.max(0, topPos);
        leftPos = Math.max(0, leftPos);
        search = new EditBox(font, leftPos + 9, topPos + 60, LW - 80, 10, Component.literal("SEARCH"));
        search.setBordered(false);
        search.setMaxLength(24);
        search.setTextColor(G);
        search.setResponder(text -> {
            var upper = text.toUpperCase(Locale.ROOT);
            if (!upper.equals(text)) {
                search.setValue(upper);
            }
        });
        search.visible = false;
        addRenderableWidget(search);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Rows
    // ------------------------------------------------------------------------------------------------------------

    private void rebuildIfNeeded() {
        var key = tab + "/" + geneSub + "/" + printSub + "/" + search.getValue() + "/" + ClientGeneDiscoveries.geneCount() + "/"
            + ClientGeneDiscoveries.speciesCount() + "/" + sampleKey() + "/" + (menu.data(
                GeneAnalyzerMenu.DATA_PROGRESS
            ) >= GeneAnalyzerBlockEntity.SCAN_TICKS / 2);
        if (key.equals(builtKey) && builtVersion == ClientAnalyzerDatabase.version()) {
            return;
        }
        var sameView = builtKey.startsWith(tab + "/" + geneSub + "/" + printSub + "/");
        builtKey = key;
        builtVersion = ClientAnalyzerDatabase.version();
        rows = switch (tab) {
            case GENES -> geneRows();
            case SPECIES -> speciesRows(false);
            case SAMPLE -> sampleRows();
            case SEARCH -> searchRows();
            case PRINT -> printSub == 0 ? printGeneRows() : speciesRows(true);
        };
        if (!sameView) {
            scroll = 0;
            selected = -1;
        }
        if (selected >= rows.size() || selected >= 0 && rows.get(selected).header()) {
            selected = -1;
        }
        if (selected < 0 && tab != Tab.PRINT) {
            for (int i = 0; i < rows.size(); i++) {
                if (!rows.get(i).header()) {
                    selected = i;
                    break;
                }
            }
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private String sampleKey() {
        var sample = menu.items().getItem(GeneAnalyzerBlockEntity.SAMPLE_SLOT);
        return sample.isEmpty() ? "" : String.valueOf(sample.hashCode());
    }

    private List<ClientAnalyzerDatabase.GeneInfo> genesOfKind(String... kinds) {
        var list = new ArrayList<ClientAnalyzerDatabase.GeneInfo>();
        for (var gene : ClientAnalyzerDatabase.genes().values()) {
            for (var kind : kinds) {
                if (gene.kind().equals(kind)) {
                    list.add(gene);
                }
            }
        }
        list.sort(Comparator.comparing(gene -> gene.id().getPath()));
        return list;
    }

    private List<Row> geneRows() {
        var out = new ArrayList<Row>();
        switch (geneSub) {
            case 0 -> {
                var all = genesOfKind("BASE");
                section(out, "STATS", all.stream().filter(gene -> !gene.id().getPath().endsWith("_resistance")).toList());
                section(out, "RESISTANCES", all.stream().filter(gene -> gene.id().getPath().endsWith("_resistance")).toList());
            }
            case 1 -> {
                section(out, "LOOT", genesOfKind("LOOT"));
                section(out, "HARVEST", genesOfKind("HARVEST"));
            }
            default -> {
                var mutations = genesOfKind("MUTATION");
                for (var category : List.of("COMBAT", "MOVEMENT", "BODY")) {
                    section(out, category, mutations.stream().filter(gene -> gene.category().equals(category)).toList());
                }
                section(out, "DEFECTS", genesOfKind("DEFECT"));
            }
        }
        return out;
    }

    private void section(List<Row> out, String title, List<ClientAnalyzerDatabase.GeneInfo> genes) {
        if (genes.isEmpty()) {
            return;
        }
        out.add(Row.header(title));
        for (var gene : genes) {
            if (!knowsGene(gene.id())) {
                out.add(new Row(false, "???", "", M, gene.id(), false, false));
                continue;
            }
            String value;
            if (gene.kind().equals("MUTATION")) {
                value = recipeShort(gene);
            } else {
                var best = bestDonor(gene.id());
                value = best == null ? "???" : speciesName(best.getKey());
            }
            out.add(new Row(false, geneName(gene.id()), value, value.equals("???") ? M : G, gene.id(), false, false));
        }
    }

    private List<Row> speciesRows(boolean print) {
        var groups = new java.util.TreeMap<String, List<ClientAnalyzerDatabase.SpeciesInfo>>(
            Comparator.comparing((String name) -> name.equals("VANILLA") ? "" : name)
        );
        for (var info : ClientAnalyzerDatabase.species().values()) {
            if (!ClientGeneDiscoveries.knowsSpecies(info.id())) {
                continue;
            }
            var ns = info.id().getNamespace();
            var group = ns.equals("minecraft") ? "VANILLA" : ns.startsWith("avp") ? "AVP" : ns.toUpperCase(Locale.ROOT);
            groups.computeIfAbsent(group, k -> new ArrayList<>()).add(info);
        }
        var out = new ArrayList<Row>();
        groups.forEach((group, list) -> {
            list.sort(Comparator.comparing(info -> speciesName(info.id())));
            out.add(Row.header(group));
            for (var info : list) {
                var count = info.genes().size();
                out.add(new Row(false, speciesName(info.id()), count == 1 ? "1 GENE" : count + " GENES", M, info.id(), true, print));
            }
        });
        return out;
    }

    private List<Row> sampleRows() {
        var out = new ArrayList<Row>();
        var contents = sampleContents();
        if (contents == null) {
            return out;
        }
        var revealed = menu.data(GeneAnalyzerMenu.DATA_PROGRESS) >= GeneAnalyzerBlockEntity.SCAN_TICKS / 2;
        var groups = new java.util.LinkedHashMap<String, List<Row>>();
        for (var entry : contents.geneBonusDataEntries()) {
            var info = ClientAnalyzerDatabase.genes().get(entry.id());
            var kind = info == null ? "BASE" : info.kind();
            var title = switch (kind) {
                case "LOOT" -> "LOOT";
                case "HARVEST" -> "HARVEST";
                case "MUTATION" -> "MUTATIONS";
                case "DEFECT" -> "DEFECTS";
                default -> "STATS";
            };
            var known = knowsGene(entry.id());
            // [stated] the scan reads the sample: names show from the first DNA screen (50%) on.
            var name = known || revealed ? rawGeneName(entry.id()) : "???";
            var value = value(entry.id(), entry.operation(), entry.value());
            if (!known) {
                value += " \u00b7 NEW";
            }
            groups.computeIfAbsent(title, k -> new ArrayList<>()).add(new Row(false, name, value, known ? G : A, entry.id(), false, false));
        }
        groups.forEach((title, list) -> {
            out.add(Row.header(title));
            out.addAll(list);
        });
        return out;
    }

    private List<Row> searchRows() {
        var out = new ArrayList<Row>();
        var query = search.getValue().trim();
        if (query.isEmpty()) {
            return out;
        }
        var matched = new ArrayList<ClientAnalyzerDatabase.GeneInfo>();
        for (var gene : ClientAnalyzerDatabase.genes().values()) {
            if (knowsGene(gene.id()) && !gene.kind().equals("MUTATION") && rawGeneName(gene.id()).contains(query)) {
                matched.add(gene);
            }
        }
        matched.sort(Comparator.comparing(gene -> rawGeneName(gene.id())));
        if (!matched.isEmpty()) {
            out.add(Row.header("GENES"));
            for (var gene : matched) {
                out.add(new Row(false, rawGeneName(gene.id()), gene.kind(), G, gene.id(), false, false));
            }
        }
        var mutations = new ArrayList<Row>();
        for (var gene : genesOfKind("MUTATION")) {
            var hit = knowsGene(gene.id()) && rawGeneName(gene.id()).contains(query);
            for (var part : gene.recipe()) {
                hit |= matched.stream().anyMatch(match -> match.id().equals(part));
            }
            if (hit) {
                var known = knowsGene(gene.id());
                mutations.add(
                    new Row(
                        false,
                        known ? rawGeneName(gene.id()) : "???",
                        known ? recipeShort(gene) : "NOT FOUND",
                        known ? G : M,
                        gene.id(),
                        false,
                        false
                    )
                );
            }
        }
        if (!mutations.isEmpty()) {
            out.add(Row.header("MUTATIONS"));
            out.addAll(mutations);
        }
        return out;
    }

    private List<Row> printGeneRows() {
        var out = new ArrayList<Row>();
        var groups = new String[][] { { "STATS", "BASE" }, { "LOOT/HARVEST", "LOOT", "HARVEST" }, { "MUTATIONS", "MUTATION", "DEFECT" } };
        for (var group : groups) {
            var kinds = java.util.Arrays.copyOfRange(group, 1, group.length);
            var list = genesOfKind(kinds).stream().filter(gene -> knowsGene(gene.id())).toList();
            if (list.isEmpty()) {
                continue;
            }
            out.add(Row.header(group[0]));
            for (var gene : list) {
                out.add(new Row(false, rawGeneName(gene.id()), gene.kind(), M, gene.id(), false, true));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        search.visible = tab == Tab.SEARCH;
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        rebuildIfNeeded();
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0);
        graphics.fill(0, 0, SW, SH, D);
        drawHeader(graphics);
        drawTabs(graphics);
        drawList(graphics);
        drawRightPanel(graphics);
        drawDetail(graphics);
        drawBottom(graphics);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // Every label is drawn in renderBg at the mockup's positions.
    }

    private void drawHeader(GuiGraphics graphics) {
        box(graphics, 6, 6, SW - 12, 32, G, P);
        box(graphics, 9, 9, 26, 26, G, W);
        var iconX = 9 + (26 - ICON[0].length()) / 2;
        var iconY = 9 + (26 - ICON.length) / 2;
        for (int row = 0; row < ICON.length; row++) {
            for (int column = 0; column < ICON[row].length(); column++) {
                var c = ICON[row].charAt(column);
                if (c != '.') {
                    graphics.fill(iconX + column, iconY + row, iconX + column + 1, iconY + row + 1, c == 'g' ? G : M);
                }
            }
        }
        text(graphics, 40, 11, "GENE ANALYZER // DATABASE", G);
        textRight(graphics, SW - 10, 11, powerText(), M);

        var chips = List.of(
            "GENES " + count("BASE"),
            "SPECIES " + speciesCount(),
            "MUTATIONS " + count("MUTATION", "DEFECT"),
            "LOOT " + count("LOOT", "HARVEST")
        );
        var x = 40;
        for (int i = 0; i < chips.size(); i++) {
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

    private String powerText() {
        var k = menu.data(GeneAnalyzerMenu.DATA_STORED_K);
        if (k < 0) {
            return menu.data(GeneAnalyzerMenu.DATA_SUPPLIED) != 0 ? "POWER OK" : "NO POWER";
        }
        return k > 0 ? "POWER " + k + "K FE" : "POWER " + menu.data(GeneAnalyzerMenu.DATA_STORED_REST) + " FE";
    }

    private void drawTabs(GuiGraphics graphics) {
        var x = 6;
        for (var t : Tab.values()) {
            var w = font.width(t.label) + 9;
            var on = t == tab;
            box(graphics, x, 42, w, 13, G, on ? G : P);
            text(graphics, x + 5, 45, t.label, on ? D : G);
            x += w + 3;
        }
        if (tab == Tab.SEARCH) {
            box(graphics, 6, 58, LW, 12, G, D);
            if (search.getValue().isEmpty() && !search.isFocused()) {
                text(graphics, 9, 60, "_", M);
            }
            var hits = (int) rows.stream().filter(row -> !row.header()).count();
            textRight(
                graphics,
                6 + LW - 4,
                60,
                search.getValue().isEmpty() ? "TYPE A GENE" : hits == 1 ? "1 RESULT" : hits + " RESULTS",
                M
            );
            return;
        }
        var subs = subs();
        x = 6;
        for (int i = 0; i < subs.size(); i++) {
            var w = font.width(subs.get(i)) + 7;
            var on = i == currentSub();
            box(graphics, x, 58, w, 12, on ? G : M, on ? P : W);
            text(graphics, x + 4, 60, subs.get(i), on ? G : M);
            x += w + 2;
        }
        var note = subNote();
        if (!note.isEmpty()) {
            textRight(graphics, 6 + LW - 1, 60, note, M);
        }
    }

    private List<String> subs() {
        return switch (tab) {
            case GENES -> List.of("BASE", "LOOT/HARVEST", "MUTATIONS");
            case SPECIES -> List.of("DISCOVERED");
            case PRINT -> List.of("GENES", "SPECIES");
            default -> List.of();
        };
    }

    private int currentSub() {
        return tab == Tab.GENES ? geneSub : tab == Tab.PRINT ? printSub : 0;
    }

    private String subNote() {
        return switch (tab) {
            case GENES -> switch (geneSub) {
                case 0 -> count("BASE");
                case 1 -> count("LOOT", "HARVEST");
                default -> count("MUTATION", "DEFECT");
            };
            case SPECIES -> speciesCount();
            case PRINT -> (tickedGenes.size() + tickedSpecies.size()) + " TICKED";
            default -> "";
        };
    }

    private void drawList(GuiGraphics graphics) {
        box(graphics, 6, LY, LW, LH, G, W);
        var sbx = 6 + LW - 8;
        box(graphics, sbx, LY + 3, 5, LH - 6, M, D);
        var total = rowsHeight(0, rows.size());
        var track = LH - 8;
        var thumb = total <= LH - 6 ? track : Math.max(6, track * (LH - 6) / total);
        var maxScroll = maxScroll();
        var thumbTop = maxScroll == 0 ? 0 : (track - thumb) * scrollPixels() / Math.max(1, rowsHeight(0, rows.size()) - (LH - 6));
        graphics.fill(sbx + 1, LY + 4 + thumbTop, sbx + 4, LY + 4 + thumbTop + thumb, M);

        if (rows.isEmpty()) {
            text(graphics, RX + 2, LY + 4, emptyText(), M);
            return;
        }
        var y = LY + 3;
        for (int i = scroll; i < rows.size(); i++) {
            var row = rows.get(i);
            var h = row.header() ? 11 : 10;
            if (y > LY + LH - 12) {
                break;
            }
            if (row.header()) {
                text(graphics, RX + 2, y + 1, row.name(), M);
                graphics.fill(RX, y + 9, RX + RW, y + 10, M);
            } else if (row.check()) {
                var on = row.species() ? tickedSpecies.contains(row.key()) : tickedGenes.contains(row.key());
                box(graphics, RX + 2, y + 1, 8, 8, G, on ? G : D);
                fit(graphics, RX + 14, y + 1, RW - 17 - font.width(row.value()) - 6, row.name(), G, false);
                textRight(graphics, RX + RW - 3, y + 1, row.value(), M);
            } else if (i == selected) {
                graphics.fill(RX, y, RX + RW, y + 10, G);
                rowText(graphics, row, y, D, D);
            } else {
                rowText(graphics, row, y, row.name().equals("???") ? M : G, row.valueColour());
            }
            y += h;
        }
    }

    /**
     * A row's name on the left and value on the right. When both fit they sit at the edges; when they don't, they share
     * the row (the value gets up to half) and whichever is still too long scrolls - they never touch.
     */
    private void rowText(GuiGraphics graphics, Row row, int y, int nameColour, int valueColour) {
        var room = RW - 5;
        var gap = 6;
        var valueWidth = font.width(row.value());
        var nameWidth = font.width(row.name());
        if (valueWidth == 0) {
            fit(graphics, RX + 2, y + 1, room, row.name(), nameColour, false);
            return;
        }
        var valueRoom = nameWidth + gap + valueWidth <= room
            ? valueWidth
            : Math.min(valueWidth, Math.max(room / 2, room - gap - nameWidth));
        fit(graphics, RX + 2, y + 1, room - gap - valueRoom, row.name(), nameColour, false);
        fit(graphics, RX + RW - 2 - valueRoom, y + 1, valueRoom, row.value(), valueColour, true);
    }

    private void fit(GuiGraphics graphics, int x, int y, int width, String text, int colour, boolean right) {
        TerminalText.draw(graphics, font, leftPos, topPos, x, y, width, text, colour, right);
    }

    private String emptyText() {
        return switch (tab) {
            case SAMPLE -> "NO SAMPLE IN THE TRAY";
            case SEARCH -> search.getValue().isEmpty() ? "SEARCH YOUR GENES" : "NOTHING FOUND";
            case SPECIES -> "NO SPECIES DISCOVERED YET";
            default -> ClientAnalyzerDatabase.genes().isEmpty() ? "LOADING DATABASE..." : "NOTHING DISCOVERED YET";
        };
    }

    // ---------------------------------------------------------------- right panel

    private int py;

    private void drawRightPanel(GuiGraphics graphics) {
        box(graphics, PX, PTOP, RPW, LY + LH - PTOP, G, P);
        py = PTOP + 4;
        var row = selected >= 0 && selected < rows.size() ? rows.get(selected) : null;
        switch (tab) {
            case GENES, SEARCH -> {
                if (row != null && row.key() != null) {
                    genePanel(graphics, row.key(), tab == Tab.SEARCH);
                }
            }
            case SPECIES -> {
                if (row != null && row.key() != null) {
                    speciesPanel(graphics, row.key());
                }
            }
            case SAMPLE -> samplePanel(graphics);
            case PRINT -> printPanel(graphics);
        }
    }

    private void genePanel(GuiGraphics graphics, ResourceLocation id, boolean search) {
        var info = ClientAnalyzerDatabase.genes().get(id);
        if (info == null || !knowsGene(id)) {
            lbl(graphics, "???");
            txt(graphics, "NOT DISCOVERED YET", M);
            return;
        }
        lbl(graphics, rawGeneName(id));
        kv(graphics, "CAP", amount(info, info.cap()), G);
        if (!search) {
            kv(graphics, "PER DOSE (PRIME)", amount(info, info.cap() / 8.0D), G);
            kv(graphics, "APPLIES TO", switch (info.who()) {
                case "MOBS" -> "MOBS ONLY";
                case "PLAYERS" -> "PLAYERS ONLY";
                default -> "ALL MOBS";
            }, G);
        }
        line(graphics);

        if (info.kind().equals("MUTATION")) {
            lbl(graphics, "RECIPE");
            for (var part : info.recipe()) {
                kv(graphics, knowsGene(part) ? rawGeneName(part) : "???", THRESHOLD_TEXT, knowsGene(part) ? G : M);
            }
            if (info.replaces() != null) {
                kv(graphics, "REPLACES", knowsGene(info.replaces()) ? rawGeneName(info.replaces()) : "???", A);
            }
            kv(graphics, "CATEGORY", info.category(), G);
            return;
        }

        var donors = donors(id);
        if (search) {
            lbl(graphics, "CARRIED BY");
            var shown = 0;
            for (var donor : donors) {
                if (!ClientGeneDiscoveries.knowsSpecies(donor.getKey()) || shown == 3) {
                    continue;
                }
                kv(graphics, speciesName(donor.getKey()), tier(info, donor.getValue()), donor.getValue() < 0 ? A : G);
                var biomes = biomeNames(donor.getKey());
                txt(
                    graphics,
                    "  " + (biomes.isEmpty() ? "NO NATURAL SPAWNS" : TerminalText.joinFit(font, biomes, ", ", RPW - 8 - font.width("  "))),
                    M
                );
                shown++;
            }
            if (shown == 0) {
                txt(graphics, "NO DISCOVERED CARRIER", M);
            }
        } else {
            lbl(graphics, "DONORS FOUND");
            var shown = 0;
            for (var donor : donors) {
                if (shown == 5) {
                    break;
                }
                var known = ClientGeneDiscoveries.knowsSpecies(donor.getKey());
                kv(
                    graphics,
                    tier(info, donor.getValue()),
                    known ? speciesName(donor.getKey()) : "???",
                    !known ? M : donor.getValue() < 0 ? A : G
                );
                shown++;
            }
            if (shown == 0) {
                txt(graphics, "NO SPECIES CARRIES IT", M);
            }
        }
        line(graphics);
        lbl(graphics, search ? "HELPS UNLOCK" : "PART OF");
        var unlocks = new ArrayList<String>();
        for (var mutation : genesOfKind("MUTATION")) {
            if (mutation.recipe().contains(id)) {
                unlocks.add(knowsGene(mutation.id()) ? rawGeneName(mutation.id()) : "???");
            }
        }
        txt(
            graphics,
            unlocks.isEmpty() ? "NO MUTATION" : TerminalText.joinFit(font, unlocks, " \u00b7 ", RPW - 8),
            unlocks.isEmpty() ? M : G
        );
    }

    private void speciesPanel(GuiGraphics graphics, ResourceLocation id) {
        var info = ClientAnalyzerDatabase.species().get(id);
        if (info == null) {
            return;
        }
        lbl(graphics, speciesName(id) + " CARRIES");
        var loot = new ArrayList<String>();
        var shown = 0;
        for (var gene : info.genes().entrySet()) {
            var geneInfo = ClientAnalyzerDatabase.genes().get(gene.getKey());
            if (geneInfo != null && geneInfo.kind().equals("LOOT")) {
                loot.add(knowsGene(gene.getKey()) ? rawGeneName(gene.getKey()) : "???");
                continue;
            }
            if (shown < 7) {
                var known = knowsGene(gene.getKey());
                kv(
                    graphics,
                    known ? rawGeneName(gene.getKey()) : "???",
                    geneInfo == null ? "-" : tier(geneInfo, gene.getValue()),
                    gene.getValue() < 0 ? A : G
                );
                shown++;
            }
        }
        line(graphics);
        kv(graphics, "LOOT", loot.isEmpty() ? "NONE" : TerminalText.joinFit(font, loot, ", ", kvRoom("LOOT")), G);
        var biomes = biomeNames(id);
        kv(graphics, "FOUND IN", biomes.isEmpty() ? "NONE NATURAL" : TerminalText.joinFit(font, biomes, ", ", kvRoom("FOUND IN")), G);
        kv(graphics, "DRAWS TAKEN", String.valueOf(info.draws()), G);
    }

    private void samplePanel(GuiGraphics graphics) {
        var sample = menu.items().getItem(GeneAnalyzerBlockEntity.SAMPLE_SLOT);
        var contents = sampleContents();
        lbl(graphics, "SAMPLE");
        if (contents == null) {
            txt(graphics, "TRAY IS EMPTY", M);
            return;
        }
        var donor = SyringeItem.donorOf(sample);
        var genes = contents.geneBonusDataEntries().size();
        kv(graphics, "TYPE", "SYRINGE", G);
        kv(
            graphics,
            "DONOR",
            donor == null ? "-" : GeneSplicing.SPLICED.equals(donor) ? "SPLICED" : speciesName(ResourceLocation.tryParse(donor)),
            G
        );
        kv(graphics, "GENES", String.valueOf(genes), G);
        kv(graphics, "DOSES", donor == null || GeneSplicing.SPLICED.equals(donor) ? "-" : "1", G);
        line(graphics);
        var fresh = contents.geneBonusDataEntries().stream().filter(entry -> !knowsGene(entry.id())).count();
        kv(graphics, "NEW DISCOVERIES", String.valueOf(fresh), fresh > 0 ? A : G);
        // [stated] -0.10 per injection plus -0.05 per extra gene.
        kv(graphics, "INJECT COST", String.format(Locale.ROOT, "%.2f", -(0.10D + 0.05D * Math.max(0, genes - 1))), A);
        line(graphics);
        var progress = menu.data(GeneAnalyzerMenu.DATA_PROGRESS);
        var done = progress >= GeneAnalyzerBlockEntity.SCAN_TICKS;
        kv(graphics, "SCAN", percent() + "%", done ? G : A);
        txt(
            graphics,
            done ? "SCAN COMPLETE." : menu.data(GeneAnalyzerMenu.DATA_SUPPLIED) == 0 ? "NO POWER. SCAN PAUSED." : "SCANNING. KEEP POWERED.",
            done ? G : M
        );
    }

    private void printPanel(GuiGraphics graphics) {
        lbl(graphics, "DOCUMENT WILL TEACH");
        var genes = 0;
        var mutations = 0;
        for (var id : tickedGenes) {
            var info = ClientAnalyzerDatabase.genes().get(id);
            if (info != null && (info.kind().equals("MUTATION") || info.kind().equals("DEFECT"))) {
                mutations++;
            } else {
                genes++;
            }
        }
        kv(graphics, "GENES", String.valueOf(genes), G);
        kv(graphics, "SPECIES", String.valueOf(tickedSpecies.size()), G);
        kv(graphics, "MUTATIONS", String.valueOf(mutations), G);
        line(graphics);
        kv(
            graphics,
            "PAPER",
            Math.min(1, menu.items().getItem(GeneAnalyzerBlockEntity.PAPER_SLOT).getCount()) + " OF 1",
            hasPaper() ? G : A
        );
        kv(
            graphics,
            "BLACK DYE",
            Math.min(1, menu.items().getItem(GeneAnalyzerBlockEntity.DYE_SLOT).getCount()) + " OF 1",
            hasDye() ? G : A
        );
        line(graphics);
        btn(graphics, "SELECT ALL");
        btn(graphics, "SELECT NONE");
        btn(graphics, "PRINT DOCUMENT");
    }

    /** Where the three PRINT buttons sit (fixed by the panel's layout above). */
    private static final int PRINT_BUTTON_Y = PTOP + 4 + 10 + 30 + 4 + 20 + 4;

    private boolean hasPaper() {
        return menu.items().getItem(GeneAnalyzerBlockEntity.PAPER_SLOT).is(Items.PAPER);
    }

    private boolean hasDye() {
        return menu.items().getItem(GeneAnalyzerBlockEntity.DYE_SLOT).is(Items.BLACK_DYE);
    }

    // ---------------------------------------------------------------- detail strip and bottom

    private void drawDetail(GuiGraphics graphics) {
        box(graphics, 6, DY, SW - 12, 24, M, P);
        var lines = detailLines();
        fit(graphics, 10, DY + 4, SW - 22, lines[0], G, false);
        fit(graphics, 10, DY + 14, SW - 22, lines[1], M, false);
    }

    private String[] detailLines() {
        var row = selected >= 0 && selected < rows.size() ? rows.get(selected) : null;
        switch (tab) {
            case PRINT:
                return new String[] {
                    "TICK WHAT TO SHARE. THE DOCUMENT TEACHES IT TO WHOEVER USES IT.",
                    "TICKS STAY WHEN YOU SWITCH BETWEEN GENES AND SPECIES. USED UP WHEN READ." };
            case SPECIES: {
                if (row == null || row.key() == null) {
                    return new String[] { "", "" };
                }
                var best = new ArrayList<String>();
                var info = ClientAnalyzerDatabase.species().get(row.key());
                if (info != null) {
                    for (var gene : info.genes().entrySet()) {
                        var top = bestDonor(gene.getKey());
                        if (top != null && top.getKey().equals(row.key()) && knowsGene(gene.getKey()) && gene.getValue() > 0) {
                            best.add(rawGeneName(gene.getKey()));
                        }
                    }
                }
                return new String[] {
                    speciesName(row.key()) + " \u00b7 " + row.key().toString().toUpperCase(Locale.ROOT),
                    best.isEmpty()
                        ? "NOT THE BEST DONOR YOU HAVE FOUND FOR ANY GENE."
                        : "BEST DONOR OF: " + TerminalText.joinFit(font, best, ", ", SW - 22 - font.width("BEST DONOR OF: "))
                };
            }
            case SAMPLE: {
                if (row == null || row.key() == null) {
                    return new String[] { "INSERT A FILLED SYRINGE TO SCAN IT.", "" };
                }
                var known = knowsGene(row.key());
                var revealed = known || menu.data(GeneAnalyzerMenu.DATA_PROGRESS) >= GeneAnalyzerBlockEntity.SCAN_TICKS / 2;
                return new String[] {
                    revealed ? description(row.key()) : "READING...",
                    known ? "ALREADY IN YOUR DATABASE." : "NEW: ADDED TO YOUR DATABASE WHEN THE SCAN FINISHES."
                };
            }
            default: {
                if (row == null || row.key() == null || !knowsGene(row.key())) {
                    return new String[] {
                        row == null ? "" : "NOT DISCOVERED YET.",
                        row == null ? "" : "READ OR DRAW FROM A CARRIER, OR SCAN A SAMPLE, TO DISCOVER IT." };
                }
                var info = ClientAnalyzerDatabase.genes().get(row.key());
                return new String[] { description(row.key()), info == null ? "" : bestDonorLine(info) };
            }
        }
    }

    private String bestDonorLine(ClientAnalyzerDatabase.GeneInfo info) {
        if (info.kind().equals("MUTATION")) {
            return "UNLOCKS AT " + THRESHOLD_TEXT + " OF EACH RECIPE GENE'S CAP, OR INJECT IT AS ENGINEERED.";
        }
        var best = bestDonor(info.id());
        if (best == null) {
            return "NO DONOR FOUND YET.";
        }
        var doses = (int) Math.ceil(info.cap() / best.getValue() - 1.0E-6);
        return "BEST DONOR FOUND: " + tier(info, best.getValue()) + ", " + doses + " DOSES TO CAP. A PRIME DONOR WOULD TAKE 8.";
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

        box(graphics, SX, BY, SWID, 84, G, P);
        box(graphics, SX + 4, BY + 4, 22, 22, G, W);
        var progress = menu.data(GeneAnalyzerMenu.DATA_PROGRESS);
        var hasSample = !menu.items().getItem(GeneAnalyzerBlockEntity.SAMPLE_SLOT).isEmpty();
        var done = hasSample && progress >= GeneAnalyzerBlockEntity.SCAN_TICKS;
        var scanning = hasSample && !done;
        text(graphics, SX + 30, BY + 6, "SAMPLE TRAY", G);
        fit(graphics, SX + 30, BY + 16, SWID - 34, trayText(hasSample, done), scanning ? A : M, false);
        text(graphics, SX + 4, BY + 31, "SCAN", M);
        textRight(graphics, SX + SWID - 5, BY + 31, hasSample ? percent() + "%" : "-", G);
        box(graphics, SX + 4, BY + 41, SWID - 8, 7, M, D);
        var fill = hasSample
            ? (SWID - 10) * Math.min(progress, GeneAnalyzerBlockEntity.SCAN_TICKS) / GeneAnalyzerBlockEntity.SCAN_TICKS
            : 0;
        if (fill > 0) {
            graphics.fill(SX + 5, BY + 42, SX + 5 + fill, BY + 47, scanning ? A : G);
        }
        text(graphics, SX + 4, BY + 53, "PRINT SUPPLIES", M);
        var labels = new String[] { "PAPER", "BLACK DYE" };
        for (int i = 0; i < 2; i++) {
            var bx = SX + 4 + i * 68;
            box(graphics, bx, BY + 63, 18, 18, M, W);
            text(graphics, bx + 21, BY + 68, labels[i], M);
        }
        var ox = SX + SWID - 26;
        box(graphics, ox, BY + 61, 22, 22, G, W);
        text(graphics, ox - font.width("DOCUMENT") - 4, BY + 68, "DOCUMENT", M);

        var footY = BY + 88;
        text(graphics, 6, footY, "INSERT A SAMPLE TO SCAN IT", M);
        textRight(graphics, SW - 7, footY, "DISCOVERIES ARE YOURS ONLY", M);
    }

    private String trayText(boolean hasSample, boolean done) {
        if (!hasSample) {
            return "EMPTY";
        }
        if (done) {
            return "SCAN COMPLETE";
        }
        if (menu.data(GeneAnalyzerMenu.DATA_SUPPLIED) == 0) {
            return "PAUSED - NO POWER";
        }
        var donor = SyringeItem.donorOf(menu.items().getItem(GeneAnalyzerBlockEntity.SAMPLE_SLOT));
        if (donor == null || GeneSplicing.SPLICED.equals(donor)) {
            return "SCANNING SYRINGE";
        }
        return "SCANNING " + speciesName(ResourceLocation.tryParse(donor));
    }

    private int percent() {
        return Math.min(100, menu.data(GeneAnalyzerMenu.DATA_PROGRESS) * 100 / GeneAnalyzerBlockEntity.SCAN_TICKS);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var x = (int) Math.floor(mouseX - leftPos);
        var y = (int) Math.floor(mouseY - topPos);
        if (button == 0) {
            // Tabs.
            var tx = 6;
            for (var t : Tab.values()) {
                var w = font.width(t.label) + 9;
                if (inside(x, y, tx, 42, w, 13)) {
                    if (tab != t) {
                        tab = t;
                        search.setFocused(false);
                        if (t == Tab.SEARCH) {
                            search.setFocused(true);
                            setFocused(search);
                        }
                    }
                    click();
                    return true;
                }
                tx += w + 3;
            }
            // Sub tabs.
            if (tab != Tab.SEARCH) {
                var sx = 6;
                var subs = subs();
                for (int i = 0; i < subs.size(); i++) {
                    var w = font.width(subs.get(i)) + 7;
                    if (inside(x, y, sx, 58, w, 12)) {
                        if (tab == Tab.GENES) {
                            geneSub = i;
                        } else if (tab == Tab.PRINT) {
                            printSub = i;
                        }
                        click();
                        return true;
                    }
                    sx += w + 2;
                }
            } else if (inside(x, y, 6, 58, LW, 12)) {
                search.setFocused(true);
                setFocused(search);
                return true;
            }
            // List rows.
            if (inside(x, y, RX, LY + 3, RW, LH - 6)) {
                var row = rowAt(y);
                if (row >= 0 && !rows.get(row).header()) {
                    var picked = rows.get(row);
                    if (picked.check()) {
                        var set = picked.species() ? tickedSpecies : tickedGenes;
                        if (!set.remove(picked.key())) {
                            set.add(picked.key());
                        }
                    } else {
                        selected = row;
                    }
                    click();
                }
                return true;
            }
            // PRINT buttons.
            if (tab == Tab.PRINT) {
                var bx = PX + 4;
                var bw = RPW - 8;
                if (inside(x, y, bx, PRINT_BUTTON_Y, bw, 13)) {
                    for (var row : rows) {
                        if (row.check()) {
                            (row.species() ? tickedSpecies : tickedGenes).add(row.key());
                        }
                    }
                    click();
                    return true;
                }
                if (inside(x, y, bx, PRINT_BUTTON_Y + 15, bw, 13)) {
                    for (var row : rows) {
                        if (row.check()) {
                            (row.species() ? tickedSpecies : tickedGenes).remove(row.key());
                        }
                    }
                    click();
                    return true;
                }
                if (inside(x, y, bx, PRINT_BUTTON_Y + 30, bw, 13)) {
                    if (!tickedGenes.isEmpty() || !tickedSpecies.isEmpty()) {
                        Human.MOD.networking().sendToServer(new C2SAnalyzerPrintPayload(join(tickedGenes), join(tickedSpecies)));
                        click();
                    }
                    return true;
                }
            }
            if (search.isFocused() && !inside(x, y, 6, 58, LW, 12)) {
                search.setFocused(false);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var x = (int) (mouseX - leftPos);
        var y = (int) (mouseY - topPos);
        if (inside(x, y, 6, LY, LW, LH)) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Typing a search: every key goes to the field (so E doesn't close the screen), except Escape.
        if (search.isFocused() && keyCode != 256) {
            return search.keyPressed(keyCode, scanCode, modifiers) || search.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int rowAt(int y) {
        var top = LY + 3;
        for (int i = scroll; i < rows.size(); i++) {
            var h = rows.get(i).header() ? 11 : 10;
            if (top > LY + LH - 12) {
                break;
            }
            if (y >= top && y < top + h) {
                return i;
            }
            top += h;
        }
        return -1;
    }

    private int rowsHeight(int from, int to) {
        var h = 0;
        for (int i = from; i < to && i < rows.size(); i++) {
            h += rows.get(i).header() ? 11 : 10;
        }
        return h;
    }

    private int scrollPixels() {
        return rowsHeight(0, scroll);
    }

    /** The furthest first row that still fills the list. */
    private int maxScroll() {
        var visible = LH - 6;
        for (int first = 0; first < rows.size(); first++) {
            if (rowsHeight(first, rows.size()) <= visible) {
                return first;
            }
        }
        return 0;
    }

    private void click() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.25F, 1.0F);
        }
    }

    private static boolean inside(int x, int y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }

    private static String join(Set<ResourceLocation> ids) {
        var parts = new ArrayList<String>();
        ids.forEach(id -> parts.add(id.toString()));
        return String.join("\n", parts);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Knowledge
    // ------------------------------------------------------------------------------------------------------------

    private static boolean knowsGene(ResourceLocation id) {
        return ClientGeneDiscoveries.knowsGene(id);
    }

    private String count(String... kinds) {
        var total = 0;
        var known = 0;
        for (var gene : genesOfKind(kinds)) {
            total++;
            if (knowsGene(gene.id())) {
                known++;
            }
        }
        return known + "/" + total;
    }

    private String speciesCount() {
        var total = ClientAnalyzerDatabase.species().size();
        var known = 0;
        for (var id : ClientAnalyzerDatabase.species().keySet()) {
            if (ClientGeneDiscoveries.knowsSpecies(id)) {
                known++;
            }
        }
        return known + "/" + total;
    }

    /** Every species carrying {@code gene}, strongest first, penalties last. */
    private static List<Map.Entry<ResourceLocation, Double>> donors(ResourceLocation gene) {
        var list = new ArrayList<Map.Entry<ResourceLocation, Double>>();
        for (var species : ClientAnalyzerDatabase.species().values()) {
            var value = species.genes().get(gene);
            if (value != null && value != 0.0D) {
                list.add(Map.entry(species.id(), value));
            }
        }
        list.sort(Map.Entry.<ResourceLocation, Double>comparingByValue().reversed());
        return list;
    }

    /** The strongest DISCOVERED donor - never one the player has not met. */
    private static @Nullable Map.Entry<ResourceLocation, Double> bestDonor(ResourceLocation gene) {
        for (var donor : donors(gene)) {
            if (donor.getValue() > 0 && ClientGeneDiscoveries.knowsSpecies(donor.getKey())) {
                return donor;
            }
        }
        return null;
    }

    /**
     * Donor tier from how many draws reach the cap: Prime 8, Strong 12, Average 16, Weak 32; a negative carrier is a
     * penalty.
     */
    private static String tier(ClientAnalyzerDatabase.GeneInfo info, double value) {
        if (value < 0) {
            return "PENALTY";
        }
        var doses = info.cap() / value;
        return doses <= 9.0D ? "PRIME" : doses <= 13.0D ? "STRONG" : doses <= 20.0D ? "AVERAGE" : "WEAK";
    }

    private static String recipeShort(ClientAnalyzerDatabase.GeneInfo mutation) {
        var parts = new ArrayList<String>();
        for (var part : mutation.recipe()) {
            parts.add(knowsGene(part) ? firstWord(rawGeneName(part)) : "???");
        }
        return String.join(" + ", parts);
    }

    private static String firstWord(String name) {
        var space = name.indexOf(' ');
        return space < 0 ? name : name.substring(0, space);
    }

    private static String rawGeneName(ResourceLocation id) {
        var gene = GeneRegistry.getValueOrNull(id);
        if (gene == null) {
            return id.getPath().replace('_', ' ').toUpperCase(Locale.ROOT);
        }
        var name = Component.translatable(gene.getTranslationKey()).getString();
        if (name.equals(gene.getTranslationKey())) {
            name = id.getPath().replace('_', ' ');
        }
        return name.toUpperCase(Locale.ROOT);
    }

    private static String geneName(ResourceLocation id) {
        return knowsGene(id) ? rawGeneName(id) : "???";
    }

    private static String description(ResourceLocation id) {
        var gene = GeneRegistry.getValueOrNull(id);
        if (gene == null) {
            return "";
        }
        var text = Component.translatable(gene.getDescriptionKey()).getString();
        return text.equals(gene.getDescriptionKey()) ? "" : text.toUpperCase(Locale.ROOT);
    }

    private static String speciesName(@Nullable ResourceLocation id) {
        if (id == null) {
            return "???";
        }
        return EntityType.byString(id.toString())
            .map(type -> type.getDescription().getString())
            .orElse(id.getPath())
            .toUpperCase(Locale.ROOT);
    }

    private static List<String> biomeNames(ResourceLocation species) {
        var info = ClientAnalyzerDatabase.species().get(species);
        var names = new ArrayList<String>();
        if (info != null) {
            for (var key : info.biomes()) {
                names.add(Component.translatable(key).getString().toUpperCase(Locale.ROOT));
            }
        }
        return names;
    }

    private @Nullable SyringeContents sampleContents() {
        var sample = menu.items().getItem(GeneAnalyzerBlockEntity.SAMPLE_SLOT);
        if (sample.isEmpty()) {
            return null;
        }
        var contents = sample.get(HumanDataComponents.SYRINGE_CONTENTS.get());
        return contents == null || contents.geneBonusDataEntries().isEmpty() ? null : contents;
    }

    private static String amount(ClientAnalyzerDatabase.GeneInfo info, double value) {
        if (info.kind().equals("LOOT")) {
            return "+" + Math.round(value * 100) + "% DROP";
        }
        if (info.operation().equals("MULTIPLICATIVE")) {
            return String.format(Locale.ROOT, "%+.1f%%", value * 100);
        }
        return String.format(Locale.ROOT, "%+.2f", value);
    }

    private static String value(ResourceLocation id, GeneOperationType operation, double value) {
        var info = ClientAnalyzerDatabase.genes().get(id);
        if (info != null && (info.kind().equals("MUTATION") || info.kind().equals("DEFECT"))) {
            return info.kind();
        }
        if (info != null && info.kind().equals("LOOT")) {
            return "+" + Math.round(value * 100) + "% DROP";
        }
        if (operation == GeneOperationType.MULTIPLICATIVE) {
            return String.format(Locale.ROOT, "%+.1f%%", value * 100);
        }
        return String.format(Locale.ROOT, "%+.2f", value);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing helpers (right panel flows top-down from py)
    // ------------------------------------------------------------------------------------------------------------

    private boolean panelFull() {
        return py > LY + LH - 12;
    }

    private void lbl(GuiGraphics graphics, String text) {
        if (!panelFull()) {
            fit(graphics, PX + 4, py, RPW - 8, text, M, false);
        }
        py += 10;
    }

    private void txt(GuiGraphics graphics, String text, int colour) {
        if (!panelFull()) {
            fit(graphics, PX + 4, py, RPW - 8, text, colour, false);
        }
        py += 10;
    }

    /** Room for a key-value row's value next to a fixed key. */
    private int kvRoom(String key) {
        return RPW - 8 - font.width(key) - 6;
    }

    /**
     * Key left, value right. When both fit they sit at the edges; when they don't, they share the row (the value gets
     * up to half) and whichever is still too long scrolls inside its own part - they never touch.
     */
    private void kv(GuiGraphics graphics, String key, String value, int colour) {
        if (!panelFull()) {
            var w = RPW - 8;
            var gap = 6;
            var keyWidth = font.width(key);
            var valueWidth = font.width(value);
            if (value.isEmpty()) {
                fit(graphics, PX + 4, py, w, key, M, false);
            } else if (keyWidth + gap + valueWidth <= w) {
                text(graphics, PX + 4, py, key, M);
                textRight(graphics, PX + 4 + w - 1, py, value, colour);
            } else {
                var valueRoom = Math.min(valueWidth, Math.max(w / 2, w - gap - keyWidth));
                fit(graphics, PX + 4, py, w - gap - valueRoom, key, M, false);
                fit(graphics, PX + 4 + w - valueRoom, py, valueRoom, value, colour, true);
            }
        }
        py += 10;
    }

    private void line(GuiGraphics graphics) {
        if (!panelFull()) {
            graphics.fill(PX + 4, py, PX + RPW - 4, py + 1, M);
        }
        py += 4;
    }

    private void btn(GuiGraphics graphics, String label) {
        var w = RPW - 8;
        box(graphics, PX + 4, py, w, 13, G, D);
        text(graphics, PX + 4 + (w - font.width(label)) / 2, py + 3, label, G);
        py += 15;
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
