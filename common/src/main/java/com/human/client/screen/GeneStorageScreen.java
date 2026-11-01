package com.human.client.screen;

import com.human.common.gameplay.gene.AdvancedGenes;
import com.human.common.gameplay.gene.GeneDefects;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.gene.Genes;
import com.human.common.gameplay.gene.discovery.ClientGeneDiscoveries;
import com.human.common.gameplay.gene.machine.GeneSplicing;
import com.human.common.gameplay.gene.machine.storage.GeneStorageInventory;
import com.human.common.gameplay.gene.machine.storage.GeneStorageMenu;
import com.human.common.gameplay.gene.tube.GeneTubeItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * The Gene Storage screen - built to the approved mockup (gene_storage_ingame_scale3.png, Oct 29) at its exact
 * positions: header with chips, the 10x9 rack drawn as tube glyphs, search, selected-tube and network readouts,
 * ISOLATED, quick actions, sort chips, AUTO SORT, inventory and footer.
 * <p>
 * Click a rack cell to select it; shift-click takes it. Search lights matching tubes and dims the rest.
 * </p>
 */
public class GeneStorageScreen extends AbstractContainerScreen<GeneStorageMenu> {

    private static final int G = 0xFF6EF58C;

    private static final int M = 0xFF33974E;

    private static final int D = 0xFF051008;

    private static final int A = 0xFFF5B841;

    private static final int P = 0xFF09190F;

    private static final int W = 0xFF0E2816;

    private static final int SW = GeneStorageMenu.SCREEN_WIDTH;

    private static final int SH = GeneStorageMenu.SCREEN_HEIGHT;

    /** Right panel and quick-actions panel. */
    private static final int PX = 180;

    private static final int PW = SW - 6 - PX;

    private static final int BY = 242;

    private static final int TOGGLE_Y = 160;

    private static final String[] ICON = {
        "...mmmmmmmm...",
        "..m........m..",
        ".m.g.g.g.g.g.m",
        ".m.g.g.g.g.g.m",
        ".m.g.g.g.g.g.m",
        ".mmmmmmmmmmmmm",
        ".m...ggggg...m",
        ".mmmmmmmmmmmmm"
    };

    private static final String[] SORTS = { "A-Z", "Z-A", "DOSE" };

    private static final int[] SORT_BUTTONS = {
        GeneStorageMenu.BUTTON_SORT_AZ,
        GeneStorageMenu.BUTTON_SORT_ZA,
        GeneStorageMenu.BUTTON_SORT_DOSE };

    private int selected = -1;

    private EditBox search;

    public GeneStorageScreen(GeneStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = SW;
        this.imageHeight = SH;
    }

    @Override
    protected void init() {
        super.init();
        topPos = Math.max(0, topPos);
        leftPos = Math.max(0, leftPos);

        search = new EditBox(font, leftPos + PX + 7, topPos + 48, PW - 70, 10, Component.literal("SEARCH"));
        search.setBordered(false);
        search.setMaxLength(24);
        search.setTextColor(G);
        search.setResponder(text -> {
            // The terminal shows capitals only.
            var upper = text.toUpperCase(Locale.ROOT);
            if (!upper.equals(text)) {
                search.setValue(upper);
            }
        });
        addRenderableWidget(search);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        var cell = cellAt(mouseX - leftPos, mouseY - topPos);
        if (cell >= 0 && !menu.rack().getItem(cell).isEmpty() && menu.getCarried().isEmpty()) {
            graphics.renderTooltip(font, menu.rack().getItem(cell), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (selected >= 0 && menu.rack().getItem(selected).isEmpty()) {
            selected = -1;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0);
        graphics.fill(0, 0, SW, SH, D);
        drawHeader(graphics);
        drawRack(graphics);
        drawRightPanel(graphics);
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
        text(graphics, 40, 11, "GENE STORAGE // CRYO RACK", G);
        textRight(graphics, SW - 10, 11, "POWER OK", M);

        var tubes = tubeCount();
        var chips = new java.util.ArrayList<String>();
        chips.add(tubes + " OF " + GeneStorageInventory.SIZE);
        if (isolated()) {
            chips.add("ISOLATED");
            chips.add("NOT ON NETWORK");
        } else if (menu.data(GeneStorageMenu.DATA_ON_GRID) == 0) {
            chips.add("NOT ON NETWORK");
        } else {
            chips.add("ON NETWORK");
            chips.add("FILL ORDER #" + menu.data(GeneStorageMenu.DATA_FILL_RANK));
        }
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

    private void drawRack(GuiGraphics graphics) {
        box(graphics, 6, 42, 170, 196, G, P);
        text(graphics, 10, 46, "RACK", M);
        // tubes held (stacks counted) and rack slots used - identical tubes stack up to 16 in one slot
        textRight(graphics, 171, 46, tubeTotal() + " TUBES  " + tubeCount() + "/" + GeneStorageInventory.SIZE, G);
        var query = search.getValue().trim();
        for (int row = 0; row < GeneStorageInventory.ROWS; row++) {
            for (int column = 0; column < GeneStorageInventory.COLUMNS; column++) {
                var index = row * GeneStorageInventory.COLUMNS + column;
                var x0 = 10 + column * 18;
                var y0 = 56 + row * 18;
                var isSelected = index == selected;
                box(graphics, x0, y0, 18, 18, isSelected ? G : M, isSelected ? P : W);
                var stack = menu.rack().getItem(index);
                if (!stack.isEmpty()) {
                    var dim = !query.isEmpty() && !matches(stack, query);
                    if (stack.getCount() > 1) {
                        // a stack: the glyph moves to the cell's left and its size sits at the right, clear of it
                        tube(graphics, x0, y0 + 4, GeneTubeItem.colour(stack) | 0xFF000000, dim);
                        textRight(graphics, x0 + 17, y0 + 5, String.valueOf(stack.getCount()), dim ? M : G);
                    } else {
                        tube(graphics, x0 + 6, y0 + 4, GeneTubeItem.colour(stack) | 0xFF000000, dim);
                    }
                }
            }
        }
    }

    /**
     * The mockup's tube glyph: cap, glass outline, fluid in the gene's colour. Dimmed: outline only, in the dark green.
     */
    private void tube(GuiGraphics graphics, int x, int y, int colour, boolean dim) {
        graphics.fill(x + 2, y, x + 4, y + 2, M);
        graphics.renderOutline(x + 1, y + 2, 4, 8, dim ? M : G);
        if (!dim) {
            graphics.fill(x + 2, y + 5, x + 4, y + 9, colour);
        }
    }

    private void drawRightPanel(GuiGraphics graphics) {
        box(graphics, PX, 42, PW, 196, G, P);
        box(graphics, PX + 4, 46, PW - 8, 12, G, D);
        var query = search.getValue().trim();
        if (query.isEmpty()) {
            if (!search.isFocused()) {
                text(graphics, PX + 7, 48, "_", M);
            }
            textRight(graphics, PX + PW - 8, 48, "SEARCH", M);
        } else {
            textRight(graphics, PX + PW - 8, 48, hits(query) + " FOUND", M);
        }

        var stack = selected >= 0 ? menu.rack().getItem(selected) : ItemStack.EMPTY;
        var contents = stack.isEmpty() ? null : GeneTubeItem.read(stack);
        var y = 62;
        text(graphics, PX + 4, y, "SELECTED TUBE", M);
        y += 10;
        if (contents == null) {
            kv(graphics, y, "GENE", "-", M);
            kv(graphics, y + 10, "VALUE", "-", M);
            kv(graphics, y + 20, "DONOR", "-", M);
            kv(graphics, y + 30, "TYPE", "-", M);
        } else {
            kv(graphics, y, "GENE", geneName(contents.gene()), G);
            kv(graphics, y + 10, "VALUE", value(contents) + " \u00b7 " + doses(contents.doses()), G);
            kv(graphics, y + 20, "DONOR", donor(contents.donor()), G);
            kv(graphics, y + 30, "TYPE", type(contents.gene()), G);
        }
        y += 40;
        line(graphics, y);
        y += 4;
        text(graphics, PX + 4, y, "NETWORK", M);
        y += 10;
        kv(graphics, y, "STORAGES", String.valueOf(menu.data(GeneStorageMenu.DATA_STORAGES)), G);
        kv(
            graphics,
            y + 10,
            "TUBES",
            menu.data(GeneStorageMenu.DATA_NETWORK_TUBES) + " OF " + menu.data(GeneStorageMenu.DATA_NETWORK_ROOM),
            G
        );
        var rank = menu.data(GeneStorageMenu.DATA_FILL_RANK);
        kv(graphics, y + 20, "THIS ONE FILLS", rank == 0 ? "NEVER" : ordinal(rank), G);
        y += 30;
        line(graphics, y);
        y += 4;

        var on = isolated();
        box(graphics, PX + 4, y, PW - 8, 14, on ? G : M, D);
        text(graphics, PX + 8, y + 3, "ISOLATED", G);
        textRight(graphics, PX + PW - 9, y + 3, on ? "ON" : "OFF", on ? G : M);
        y += 17;
        if (on) {
            text(graphics, PX + 4, y, "HIDDEN FROM OTHER MACHINES", A);
        } else {
            text(graphics, PX + 4, y, "SEPARATOR AND SPLICER SEE IT", M);
        }
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

        box(graphics, PX, BY, PW, 84, G, P);
        text(graphics, PX + 4, BY + 4, "QUICK ACTIONS", M);
        button(graphics, BY + 14, "STORE ALL TUBES");
        button(graphics, BY + 31, "TAKE SELECTED");
        text(graphics, PX + 4, BY + 52, "SORT", M);
        var x = PX + 30;
        var mode = menu.data(GeneStorageMenu.DATA_SORT_MODE);
        for (int i = 0; i < SORTS.length; i++) {
            var w = font.width(SORTS[i]) + 9;
            var lit = i == mode;
            box(graphics, x, BY + 50, w, 12, lit ? G : M, lit ? P : W);
            text(graphics, x + 5, BY + 52, SORTS[i], lit ? G : M);
            x += w + 3;
        }
        var auto = menu.data(GeneStorageMenu.DATA_AUTO_SORT) != 0;
        box(graphics, PX + 4, BY + 66, PW - 8, 14, auto ? G : M, D);
        text(graphics, PX + 8, BY + 69, "AUTO SORT", G);
        textRight(graphics, PX + PW - 9, BY + 69, auto ? "ON" : "OFF", auto ? G : M);

        var footY = BY + 88;
        text(graphics, 6, footY, "SHIFT-CLICK TUBES TO STORE", M);
        textRight(graphics, SW - 7, footY, "FILLED TUBES ONLY", M);
    }

    private void button(GuiGraphics graphics, int y, String label) {
        box(graphics, PX + 4, y, PW - 8, 14, G, D);
        text(graphics, PX + 4 + (PW - 8 - font.width(label)) / 2, y + 3, label, G);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var x = (int) Math.floor(mouseX - leftPos);
        var y = (int) Math.floor(mouseY - topPos);
        if (button == 0) {
            var cell = cellAt(x, y);
            if (cell >= 0) {
                search.setFocused(false);
                if (menu.rack().getItem(cell).isEmpty()) {
                    selected = -1;
                } else if (Screen.hasShiftDown()) {
                    press(GeneStorageMenu.BUTTON_TAKE + cell);
                } else {
                    selected = cell;
                }
                return true;
            }
            if (inside(x, y, PX + 4, TOGGLE_Y, PW - 8, 14)) {
                return press(GeneStorageMenu.BUTTON_ISOLATED);
            }
            if (inside(x, y, PX + 4, BY + 14, PW - 8, 14)) {
                return press(GeneStorageMenu.BUTTON_STORE_ALL);
            }
            if (inside(x, y, PX + 4, BY + 31, PW - 8, 14)) {
                return selected >= 0 && press(GeneStorageMenu.BUTTON_TAKE + selected);
            }
            var chipX = PX + 30;
            for (int i = 0; i < SORTS.length; i++) {
                var w = font.width(SORTS[i]) + 9;
                if (inside(x, y, chipX, BY + 50, w, 12)) {
                    return press(SORT_BUTTONS[i]);
                }
                chipX += w + 3;
            }
            if (inside(x, y, PX + 4, BY + 66, PW - 8, 14)) {
                return press(GeneStorageMenu.BUTTON_AUTO_SORT);
            }
            if (inside(x, y, PX + 4, 46, PW - 8, 12)) {
                search.setFocused(true);
                setFocused(search);
                return true;
            }
            search.setFocused(false);
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Typing a search: every key goes to the field (so E doesn't close the screen), except Escape.
        if (search.isFocused() && keyCode != 256) {
            return search.keyPressed(keyCode, scanCode, modifiers) || search.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static int cellAt(int x, int y) {
        if (x < 10 || y < 56) {
            return -1;
        }
        var column = (x - 10) / 18;
        var row = (y - 56) / 18;
        if (column >= GeneStorageInventory.COLUMNS || row >= GeneStorageInventory.ROWS) {
            return -1;
        }
        return row * GeneStorageInventory.COLUMNS + column;
    }

    private static boolean inside(int x, int y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Readouts
    // ------------------------------------------------------------------------------------------------------------

    private boolean isolated() {
        return menu.data(GeneStorageMenu.DATA_ISOLATED) != 0;
    }

    /** Every tube on the rack, stacks counted. */
    private int tubeTotal() {
        var total = 0;
        for (int i = 0; i < GeneStorageInventory.SIZE; i++) {
            total += menu.rack().getItem(i).getCount();
        }
        return total;
    }

    /** Rack slots in use. */
    private int tubeCount() {
        var count = 0;
        for (int i = 0; i < GeneStorageInventory.SIZE; i++) {
            if (!menu.rack().getItem(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private int hits(String query) {
        var hits = 0;
        for (int i = 0; i < GeneStorageInventory.SIZE; i++) {
            var stack = menu.rack().getItem(i);
            if (!stack.isEmpty() && matches(stack, query)) {
                hits++;
            }
        }
        return hits;
    }

    /**
     * What the search matches: the gene's name, the donor and the type - only as far as this player has discovered
     * them.
     */
    private boolean matches(ItemStack stack, String query) {
        var contents = GeneTubeItem.read(stack);
        if (contents == null) {
            return false;
        }
        return geneName(contents.gene()).contains(query) || donor(contents.donor()).contains(query) || type(contents.gene()).contains(
            query
        );
    }

    private static String geneName(ResourceLocation id) {
        var gene = GeneRegistry.getValueOrNull(id);
        if (gene == null || !ClientGeneDiscoveries.knowsGene(id)) {
            return "???";
        }
        return Component.translatable(gene.getTranslationKey()).getString().toUpperCase(Locale.ROOT);
    }

    private static String value(GeneTubeItem.Contents contents) {
        var gene = GeneRegistry.getValueOrNull(contents.gene());
        if (gene == null || !ClientGeneDiscoveries.knowsGene(contents.gene())) {
            return "???";
        }
        if (AdvancedGenes.definition(gene) != null) {
            return "MUTATION";
        }
        if (GeneDefects.all().contains(gene)) {
            return "DEFECT";
        }
        var value = contents.value();
        if (Genes.category(gene) == Genes.Category.LOOT) {
            return "+" + Math.round(value * 100) + "% DROP";
        }
        if (contents.operation() == GeneOperationType.MULTIPLICATIVE) {
            var percent = value * 100;
            return Math.abs(percent - Math.rint(percent)) < 0.05
                ? String.format(Locale.ROOT, "%+d%%", Math.round(percent))
                : String.format(Locale.ROOT, "%+.1f%%", percent);
        }
        return String.format(Locale.ROOT, "%+.1f", value);
    }

    private static String doses(int doses) {
        return doses == 1 ? "1 DOSE" : doses + " DOSES";
    }

    private static String donor(String typeId) {
        if (typeId == null) {
            return "-";
        }
        if (GeneSplicing.SPLICED.equals(typeId)) {
            return "SPLICED";
        }
        var id = ResourceLocation.tryParse(typeId);
        if (id == null || !ClientGeneDiscoveries.knowsSpecies(id)) {
            return "???";
        }
        return EntityType.byString(typeId).map(type -> type.getDescription().getString()).orElse(typeId).toUpperCase(Locale.ROOT);
    }

    private static String type(ResourceLocation id) {
        var gene = GeneRegistry.getValueOrNull(id);
        if (gene == null || !ClientGeneDiscoveries.knowsGene(id)) {
            return "???";
        }
        if (AdvancedGenes.definition(gene) != null) {
            return "MUTATION \u00b7 ADVANCED";
        }
        if (GeneDefects.all().contains(gene)) {
            return "DEFECT";
        }
        return switch (Genes.category(gene)) {
            case LOOT -> "BASE \u00b7 LOOT";
            case HARVEST -> "BASE \u00b7 HARVEST";
            default -> "BASE \u00b7 STATS";
        };
    }

    private static String ordinal(int n) {
        var mod100 = n % 100;
        var suffix = mod100 >= 11 && mod100 <= 13 ? "TH" : switch (n % 10) {
            case 1 -> "ST";
            case 2 -> "ND";
            case 3 -> "RD";
            default -> "TH";
        };
        return n + suffix;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------------

    /** Key on the left in the dark green, value right-aligned - clipped so it never runs into the key. */
    private void kv(GuiGraphics graphics, int y, String key, String value, int colour) {
        text(graphics, PX + 4, y, key, M);
        var room = PW - 9 - font.width(key) - 6;
        TerminalText.draw(graphics, font, leftPos, topPos, PX + PW - 4 - room, y, room, value, colour, true);
    }

    private void line(GuiGraphics graphics, int y) {
        graphics.fill(PX + 4, y, PX + PW - 4, y + 1, M);
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
