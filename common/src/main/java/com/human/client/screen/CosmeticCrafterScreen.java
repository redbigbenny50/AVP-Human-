package com.human.client.screen;

import com.human.common.gameplay.cosmetic.CosmeticCatalog;
import com.human.common.gameplay.cosmetic.CosmeticCrafterBlockEntity;
import com.human.common.gameplay.cosmetic.CosmeticCrafterMenu;
import com.human.common.gameplay.cosmetic.CosmeticDesign;
import com.human.common.gameplay.cosmetic.CosmeticSet;
import com.human.common.gameplay.cosmetic.CosmeticStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Cosmetic Crafter's screen, built to the APPROVED mockup ([stated] Oct 31,
 * cosmetic_crafter_gui_style_drawer.png) at its exact GUI-pixel positions - [stated] Oct 27: built GUIs match the
 * approved mockup, never a simplified version.
 * <ul>
 * <li>Vanilla frame and slots; the MUTHUR-green screen holds SETS, CATALOGUE and the selected design.</li>
 * <li>[stated] Oct 31: ONE style button opens a DRAWER listing every style - however many factions come - with a
 * license slot on each faction's row. A style is locked until its license is in that slot, in the player's inventory
 * or in network storage.</li>
 * <li>[stated] Oct 31: NETWORK under the input box shows how many storages are connected.</li>
 * <li>All text is the vanilla font at one size, inside its frame ([stated] Oct 16 / Oct 18).</li>
 * </ul>
 */
public class CosmeticCrafterScreen extends AbstractContainerScreen<CosmeticCrafterMenu> {

    public static final int WIDTH = 320;

    public static final int HEIGHT = 248;

    // MUTHUR screen (the Fab-Com's palette) and the vanilla frame.
    private static final int BG = 0xFF051008;

    private static final int PANEL = 0xFF09190F;

    private static final int GREEN = 0xFF6EF58C;

    private static final int MUTED = 0xFF33974E;

    private static final int DIM = 0xFF16351F;

    private static final int WELL = 0xFF0E2816;

    private static final int DARK = 0xFF051008;

    private static final int WARN = 0xFFF0BE5A;

    private static final int V_BG = 0xFFC6C6C6;

    private static final int V_HI = 0xFFFFFFFF;

    private static final int V_LO = 0xFF555555;

    private static final int V_EDGE = 0xFF000000;

    private static final int V_LABEL = 0xFF404040;

    private static final int V_FIELD = 0xFF8B8B8B;

    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/slot");

    private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");

    private static final ResourceLocation BUTTON_HOVER = ResourceLocation.withDefaultNamespace("widget/button_highlighted");

    private static final ResourceLocation BUTTON_OFF = ResourceLocation.withDefaultNamespace("widget/button_disabled");

    // Layout (from the mockup).
    private static final int GRID_X = 95;

    private static final int GRID_Y = 48;

    private static final int GRID_COLUMNS = 5;

    private static final int GRID_ROWS = 5;

    /** SETS rows on screen at once: ALL + seven, as in the approved mockup. */
    private static final int SET_ROWS = 8;

    private static final int MATERIAL_ROWS = 3;

    private static final int DRAWER_ROWS = 3;

    /** GLFW_KEY_ESCAPE (common has no lwjgl on its compile path). */
    private static final int KEY_ESCAPE = 256;

    private final List<Runnable> clicks = new ArrayList<>();

    private final List<int[]> clickBoxes = new ArrayList<>();

    private @Nullable EditBox search;

    /** -1 = ALL, else a CosmeticSet ordinal. */
    private int setFilter = -1;

    private int gridScroll;

    /** First visible row of the SETS column ([stated] Oct 31: it scrolls like the catalogue). */
    private int setScroll;

    private int materialScroll;

    private int drawerScroll;

    private @Nullable CosmeticDesign design;

    private CosmeticStyle style = CosmeticStyle.INDUSTRIAL;

    public CosmeticCrafterScreen(CosmeticCrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 97, topPos + 34, 96, 9, Component.translatable(t("search")));
        search.setBordered(false);
        search.setMaxLength(32);
        search.setTextColor(GREEN);
        search.setHint(Component.translatable(t("search")).withColor(MUTED));
        search.setResponder(text -> gridScroll = 0);
        addRenderableWidget(search);
    }

    private static String t(String key) {
        return "gui.avp_human.cosmetic_crafter." + key;
    }

    // ------------------------------------------------------------------------------------------------------------
    // What is shown
    // ------------------------------------------------------------------------------------------------------------

    private List<CosmeticDesign> visibleDesigns() {
        var query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        var shown = new ArrayList<CosmeticDesign>();

        for (var candidate : CosmeticDesign.values()) {
            var entry = CosmeticCatalog.find(candidate, CosmeticStyle.INDUSTRIAL);

            if (entry == null || (setFilter >= 0 && candidate.set().ordinal() != setFilter)) {
                continue;
            }

            if (!query.isEmpty() && !entry.resultStack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }

            shown.add(candidate);
        }

        return shown;
    }

    private @Nullable CosmeticCatalog.Entry current() {
        if (design == null) {
            var synced = CosmeticCatalog.get(menu.selected());

            if (synced != null) {
                design = synced.design();
                style = synced.style();
            }
        }

        return design == null ? null : CosmeticCatalog.find(design, style);
    }

    private void select(CosmeticDesign chosen, CosmeticStyle wanted) {
        var entry = CosmeticCatalog.find(chosen, wanted);

        if (entry == null || !menu.unlocked(wanted)) {
            wanted = CosmeticStyle.INDUSTRIAL;
            entry = CosmeticCatalog.find(chosen, wanted);
        }

        if (entry == null || minecraft == null || minecraft.gameMode == null) {
            return;
        }

        design = chosen;
        style = wanted;
        materialScroll = 0;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CosmeticCrafterMenu.SELECT + entry.index());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        clicks.clear();
        clickBoxes.clear();

        // Vanilla outer panel.
        fill(g, 1, 0, WIDTH - 1, HEIGHT, V_EDGE);
        fill(g, 0, 1, WIDTH, HEIGHT - 1, V_EDGE);
        fill(g, 1, 1, WIDTH - 1, HEIGHT - 1, V_BG);
        fill(g, 1, 1, WIDTH - 2, 3, V_HI);
        fill(g, 1, 1, 3, HEIGHT - 2, V_HI);
        fill(g, 3, HEIGHT - 3, WIDTH - 1, HEIGHT - 1, V_LO);
        fill(g, WIDTH - 3, 3, WIDTH - 1, HEIGHT - 1, V_LO);

        text(g, title.getString(), 8, 6, V_LABEL);
        textRight(g, Component.translatable(t("edition." + edition().id())).getString(), WIDTH - 8, 6, V_LABEL);

        // MUTHUR screen.
        frame(g, 7, 17, 313, 151, MUTED, BG);
        sets(g);
        catalogue(g);
        selected(g);

        // Lower half.
        text(g, Component.translatable(t("input")).getString(), 8, 154, V_LABEL);
        text(g, Component.translatable(t("inventory")).getString(), 79, 154, V_LABEL);
        text(g, Component.translatable(t("craft")).getString(), 250, 154, V_LABEL);

        for (var slot : menu.slots) {
            // License slots sit in the drawer and are framed there.
            if (slot.isActive() && slot.y > 150) {
                g.blitSprite(SLOT, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
            }
        }

        frame(g, 7, 220, 61, 242, MUTED, BG);
        boxText(g, Component.translatable(t("network")).getString(), 7, 220, 61, 231, MUTED);
        var linked = menu.linked();
        boxText(g, linked > 0 ? Component.translatable(t("linked"), linked).getString() : Component.translatable(t("none")).getString(), 7, 230, 61, 241, GREEN);

        craftControls(g, mouseX, mouseY);
    }

    /** The machine's own edition, for the brand at the top right (synced by the menu). */
    private CosmeticStyle edition() {
        return menu.edition();
    }

    private void sets(GuiGraphics g) {
        text(g, Component.translatable(t("sets")).getString(), 12, 21, GREEN);

        // ALL plus every set; past eight rows the column scrolls, with the catalogue's own scroll bar beside it.
        var total = CosmeticSet.values().length + 1;
        setScroll = Math.clamp(setScroll, 0, Math.max(0, total - SET_ROWS));

        for (int row = 0; row < SET_ROWS && row + setScroll < total; row++) {
            var filter = row + setScroll - 1;
            var label = filter < 0 ? Component.translatable(t("set.all")).getString() : Component.translatable(CosmeticSet.values()[filter].translationKey())
                .getString();
            var y = 32 + row * 14;

            screenButton(g, label, 11, y, 82, y + 12, setFilter == filter, true, () -> {
                setFilter = filter;
                gridScroll = 0;
            });
        }

        scrollBar(g, 83, 32, 89, 142, setScroll, Math.max(0, total - SET_ROWS), SET_ROWS, Math.max(SET_ROWS, total));
    }

    private void catalogue(GuiGraphics g) {
        text(g, Component.translatable(t("catalogue")).getString(), 95, 21, GREEN);
        frame(g, 94, 32, 196, 44, MUTED, WELL);

        var designs = visibleDesigns();
        var rows = (designs.size() + GRID_COLUMNS - 1) / GRID_COLUMNS;

        gridScroll = Math.clamp(gridScroll, 0, Math.max(0, rows - GRID_ROWS));

        for (int cell = 0; cell < GRID_COLUMNS * GRID_ROWS; cell++) {
            var x = GRID_X + (cell % GRID_COLUMNS) * 18;
            var y = GRID_Y + (cell / GRID_COLUMNS) * 18;
            var index = gridScroll * GRID_COLUMNS + cell;
            var shown = index < designs.size() ? designs.get(index) : null;

            frame(g, x - 1, y - 1, x + 17, y + 17, shown != null && shown == design ? GREEN : MUTED, WELL);

            if (shown == null) {
                continue;
            }

            var entry = CosmeticCatalog.find(shown, CosmeticStyle.INDUSTRIAL);

            if (entry != null) {
                g.renderItem(entry.resultStack(), leftPos + x, topPos + y);
            }

            click(x, y, x + 16, y + 16, () -> select(shown, style));
        }

        scrollBar(g, 186, 47, 192, 137, gridScroll, Math.max(0, rows - GRID_ROWS), GRID_ROWS, Math.max(GRID_ROWS, rows));
        text(g, Component.translatable(t("designs"), designs.size()).getString(), 95, 140, MUTED);
    }

    private void selected(GuiGraphics g) {
        var entry = current();

        if (entry == null) {
            frame(g, 198, 31, 311, 72, MUTED, WELL);
            frame(g, 198, 75, 311, 87, MUTED, WELL);
            text(g, Component.translatable(style.translationKey()).getString(), 201, 77, MUTED);
            text(g, Component.translatable(t("materials")).getString(), 199, 90, MUTED);
            text(g, Component.translatable(t("pick")).getString(), 199, 100, MUTED);
            return;
        }

        var footprint = entry.design().footprint();
        var footprintWidth = font.width(footprint);
        var name = font.plainSubstrByWidth(entry.resultStack().getHoverName().getString().toUpperCase(Locale.ROOT), 310 - footprintWidth - 4 - 199);

        text(g, name, 199, 21, GREEN);
        textRight(g, footprint, 310, 21, MUTED);

        // Preview: the item's own model, as large as the window allows.
        frame(g, 198, 31, 311, 72, MUTED, WELL);
        var scale = 2.3F;
        g.pose().pushPose();
        g.pose().translate(leftPos + 254.5F - 8.0F * scale, topPos + 51.5F - 8.0F * scale, 100.0F);
        g.pose().scale(scale, scale, scale);
        g.renderItem(entry.resultStack(), 0, 0);
        g.pose().popPose();

        // STYLE: one button, its drawer below.
        frame(g, 198, 75, 311, 87, menu.drawerOpen ? GREEN : MUTED, WELL);
        text(g, Component.translatable(style.translationKey()).getString(), 201, 77, GREEN);

        for (int i = 0; i < 3; i++) {
            var y = menu.drawerOpen ? 82 - i : 80 + i;

            fill(g, 303 + i, y, 308 - i, y + 1, GREEN);
        }

        click(198, 75, 311, 87, () -> menu.drawerOpen = !menu.drawerOpen);

        if (menu.drawerOpen) {
            drawer(g, entry.design());
        } else {
            materials(g, entry);
        }
    }

    private void materials(GuiGraphics g, CosmeticCatalog.Entry entry) {
        text(g, Component.translatable(t("materials")).getString(), 199, 90, MUTED);

        var parts = entry.parts();

        materialScroll = Math.clamp(materialScroll, 0, Math.max(0, parts.size() - MATERIAL_ROWS));

        for (int row = 0; row < MATERIAL_ROWS; row++) {
            var index = materialScroll + row;

            if (index >= parts.size()) {
                break;
            }

            var part = parts.get(index);
            var y = 100 + row * 16;
            // NEED is one craft's cost (as on the approved mockup); HAVE turns amber when the whole batch is short.
            var need = part.count();
            var batch = need * Math.max(1, menu.count());
            var have = menu.have(index);

            g.renderItem(cycle(part.ingredient().getItems()), leftPos + 199, topPos + y);
            text(g, Component.translatable(t("need"), need).getString(), 218, y + 4, GREEN);
            // Compact so the two never meet: "HAVE 32K" is 45px, right-aligned at 302; "NEED 8" ends at 251.
            var haveText = have >= Short.MAX_VALUE ? Component.translatable(t("have_max")).getString() : Component.translatable(
                t("have"),
                have < 1000 ? String.valueOf(have) : (have / 1000) + "K"
            ).getString();
            textRight(g, haveText, 302, y + 4, have >= batch ? GREEN : WARN);
        }

        scrollBar(g, 305, 99, 311, 148, materialScroll, Math.max(0, parts.size() - MATERIAL_ROWS), MATERIAL_ROWS, Math.max(
            MATERIAL_ROWS,
            parts.size()
        ));
    }

    /** One row per style; faction rows carry their license slot (a real menu slot, at the row's right). */
    private void drawer(GuiGraphics g, CosmeticDesign chosen) {
        frame(g, 198, 88, 311, 149, GREEN, PANEL);

        var styles = CosmeticStyle.values();

        drawerScroll = Math.clamp(drawerScroll, 0, Math.max(0, styles.length - DRAWER_ROWS));

        for (int row = 0; row < DRAWER_ROWS && drawerScroll + row < styles.length; row++) {
            var option = styles[drawerScroll + row];
            var y = CosmeticCrafterMenu.DRAWER_TOP + 2 + row * CosmeticCrafterMenu.DRAWER_ROW;
            var unlocked = menu.unlocked(option);

            if (option == style) {
                fill(g, 200, y, 304, y + CosmeticCrafterMenu.DRAWER_ROW - 1, DIM);
            }

            fill(g, 199, y + 1, 201, y + CosmeticCrafterMenu.DRAWER_ROW - 2, option.swatch());
            text(g, Component.translatable(option.translationKey()).getString(), 202, y + 5, unlocked ? GREEN : MUTED);

            if (option.faction() == null) {
                textRight(g, Component.translatable(t("free")).getString(), 303, y + 5, MUTED);
            } else {
                frame(g, 286, y, 304, y + 18, MUTED, WELL);
                var slot = slotFor(option);

                if (!unlocked && (slot == null || !slot.hasItem())) {
                    fill(g, 292, y + 8, 298, y + 13, MUTED);
                    fill(g, 293, y + 5, 297, y + 8, MUTED);
                    fill(g, 294, y + 6, 296, y + 8, WELL);
                }
            }

            if (unlocked && CosmeticCatalog.find(chosen, option) != null) {
                click(199, y, 285, y + CosmeticCrafterMenu.DRAWER_ROW - 1, () -> {
                    select(chosen, option);
                    menu.drawerOpen = false;
                });
            }
        }

        scrollBar(g, 305, 90, 311, 148, drawerScroll, Math.max(0, styles.length - DRAWER_ROWS), DRAWER_ROWS, Math.max(
            DRAWER_ROWS,
            styles.length
        ));
    }

    private net.minecraft.world.inventory.@Nullable Slot slotFor(CosmeticStyle option) {
        var index = CosmeticCrafterBlockEntity.licenseSlot(option);

        for (var slot : menu.slots) {
            if (slot.getContainerSlot() == index && slot.index >= CosmeticCrafterMenu.MENU_FIRST_LICENSE && slot.index < CosmeticCrafterMenu.MENU_FIRST_INVENTORY) {
                return slot;
            }
        }

        return null;
    }

    private void craftControls(GuiGraphics g, int mouseX, int mouseY) {
        var crafting = menu.queued() > 0;

        vanillaButton(g, "-", 250, 164, 264, 178, true, mouseX, mouseY, () -> press(Screen.hasShiftDown() ? CosmeticCrafterMenu.COUNT_DOWN_TEN : CosmeticCrafterMenu.COUNT_DOWN));
        frame(g, 265, 164, 295, 178, V_LO, V_FIELD);
        var count = String.valueOf(crafting ? menu.queued() : menu.count());
        g.drawString(font, count, leftPos + 265 + (30 - font.width(count)) / 2 + 1, topPos + 167, 0xFFFFFFFF, false);
        vanillaButton(g, "+", 296, 164, 310, 178, true, mouseX, mouseY, () -> press(Screen.hasShiftDown() ? CosmeticCrafterMenu.COUNT_UP_TEN : CosmeticCrafterMenu.COUNT_UP));

        var entry = current();
        var canCraft = crafting || (entry != null && menu.unlocked(entry.style()));
        var label = Component.translatable(t(crafting ? "stop" : "craft_button")).getString();

        vanillaButton(g, label, 250, 181, 310, 197, canCraft, mouseX, mouseY, () -> {
            if (crafting) {
                press(CosmeticCrafterMenu.STOP);
            } else if (entry != null) {
                // The server's selection follows the screen's before every craft.
                press(CosmeticCrafterMenu.SELECT + entry.index());
                press(CosmeticCrafterMenu.CRAFT);
            }
        });

        frame(g, 250, 199, 310, 203, V_LO, V_FIELD);
        fill(g, 251, 200, 251 + 58 * Math.clamp(menu.progress(), 0, 100) / 100, 202, 0xFF01EB0A);

        // Power gauge: consumers store no power, so it shows whether power is reaching a craft.
        frame(g, 302, 205, 310, 241, V_LO, 0xFF373737);

        if (menu.running()) {
            fill(g, 303, 206, 309, 240, 0xFFE6BE28);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // Everything is drawn in renderBg.
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        tooltips(g, mouseX - leftPos, mouseY - topPos, mouseX, mouseY);
    }

    private void tooltips(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        // Catalogue cells.
        var designs = visibleDesigns();

        for (int cell = 0; cell < GRID_COLUMNS * GRID_ROWS; cell++) {
            var cx = GRID_X + (cell % GRID_COLUMNS) * 18;
            var cy = GRID_Y + (cell / GRID_COLUMNS) * 18;
            var index = gridScroll * GRID_COLUMNS + cell;

            if (index < designs.size() && x >= cx && x < cx + 16 && y >= cy && y < cy + 16) {
                var entry = CosmeticCatalog.find(designs.get(index), CosmeticStyle.INDUSTRIAL);

                if (entry != null) {
                    g.renderTooltip(font, entry.resultStack(), mouseX, mouseY);
                }

                return;
            }
        }

        var entry = current();

        // Material icons.
        if (entry != null && !menu.drawerOpen) {
            for (int row = 0; row < MATERIAL_ROWS; row++) {
                var index = materialScroll + row;
                var top = 100 + row * 16;

                if (index < entry.parts().size() && x >= 199 && x < 215 && y >= top && y < top + 16) {
                    g.renderTooltip(font, cycle(entry.parts().get(index).ingredient().getItems()), mouseX, mouseY);
                    return;
                }
            }
        }

        // Locked styles.
        if (menu.drawerOpen) {
            var styles = CosmeticStyle.values();

            for (int row = 0; row < DRAWER_ROWS && drawerScroll + row < styles.length; row++) {
                var option = styles[drawerScroll + row];
                var top = CosmeticCrafterMenu.DRAWER_TOP + 2 + row * CosmeticCrafterMenu.DRAWER_ROW;

                if (!menu.unlocked(option) && x >= 199 && x < 285 && y >= top && y < top + CosmeticCrafterMenu.DRAWER_ROW) {
                    g.renderTooltip(font, Component.translatable(t("locked"), Component.translatable(option.translationKey())), mouseX, mouseY);
                    return;
                }
            }
        }

        // Power gauge.
        if (x >= 302 && x < 310 && y >= 205 && y < 241) {
            g.renderTooltip(
                font,
                menu.running() ? Component.translatable(t("power.on"), menu.powerDraw()) : Component.translatable(t("power.off")),
                mouseX,
                mouseY
            );
            return;
        }

        // Craft button and progress bar: what the machine is doing.
        if (x >= 250 && x < 310 && y >= 181 && y < 203) {
            g.renderTooltip(font, Component.translatable(t("status." + menu.status().name().toLowerCase(Locale.ROOT))), mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            var x = mouseX - leftPos;
            var y = mouseY - topPos;

            for (int i = clickBoxes.size() - 1; i >= 0; i--) {
                var box = clickBoxes.get(i);

                if (x >= box[0] && x < box[2] && y >= box[1] && y < box[3]) {
                    clicks.get(i).run();
                    return true;
                }
            }

            // A click on the open drawer never falls through to the materials list underneath (its license slots
            // still take clicks - they are real slots, handled below).
            if (menu.drawerOpen && x >= 198 && x < 311 && y >= 88 && y < 149 && !(x >= 286 && x < 305)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var x = mouseX - leftPos;
        var y = mouseY - topPos;
        var step = scrollY > 0 ? -1 : 1;

        if (x >= 94 && x < 193 && y >= 47 && y < 138) {
            gridScroll += step;
            return true;
        }

        if (x >= 11 && x < 90 && y >= 32 && y < 143) {
            setScroll += step;
            return true;
        }

        if (x >= 198 && x < 312 && y >= 88 && y < 149) {
            if (menu.drawerOpen) {
                drawerScroll += step;
            } else {
                materialScroll += step;
            }

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** While searching, typing never closes the screen or triggers inventory keys. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (search != null && search.isFocused() && keyCode != KEY_ESCAPE) {
            return search.keyPressed(keyCode, scanCode, modifiers) || true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing helpers (GUI-pixel coordinates relative to the screen's corner, end-exclusive like the mockup)
    // ------------------------------------------------------------------------------------------------------------

    private void click(int x0, int y0, int x1, int y1, Runnable action) {
        clickBoxes.add(new int[] { x0, y0, x1, y1 });
        clicks.add(action);
    }

    private void fill(GuiGraphics g, int x0, int y0, int x1, int y1, int colour) {
        g.fill(leftPos + x0, topPos + y0, leftPos + x1, topPos + y1, colour);
    }

    private void frame(GuiGraphics g, int x0, int y0, int x1, int y1, int edge, int inner) {
        fill(g, x0, y0, x1, y1, edge);
        fill(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, inner);
    }

    private void text(GuiGraphics g, String text, int x, int y, int colour) {
        g.drawString(font, text, leftPos + x, topPos + y, colour, false);
    }

    private void textRight(GuiGraphics g, String text, int x, int y, int colour) {
        text(g, text, x - font.width(text), y, colour);
    }

    /** Centred inside a frame, never touching its border - shortened rather than shrunk ([stated] Oct 16). */
    private void boxText(GuiGraphics g, String label, int x0, int y0, int x1, int y1, int colour) {
        var fitted = font.plainSubstrByWidth(label, x1 - x0 - 4);

        text(g, fitted, x0 + (x1 - x0 - font.width(fitted)) / 2 + 1, y0 + (y1 - y0 - 8) / 2 + 1, colour);
    }

    private void screenButton(GuiGraphics g, String label, int x0, int y0, int x1, int y1, boolean on, boolean enabled, Runnable action) {
        if (on) {
            frame(g, x0, y0, x1, y1, GREEN, GREEN);
            boxText(g, label, x0, y0, x1, y1, DARK);
        } else {
            frame(g, x0, y0, x1, y1, enabled ? MUTED : DIM, WELL);
            boxText(g, label, x0, y0, x1, y1, enabled ? GREEN : MUTED);
        }

        if (enabled) {
            click(x0, y0, x1, y1, action);
        }
    }

    private void vanillaButton(GuiGraphics g, String label, int x0, int y0, int x1, int y1, boolean enabled, int mouseX, int mouseY, Runnable action) {
        var hovered = mouseX >= leftPos + x0 && mouseX < leftPos + x1 && mouseY >= topPos + y0 && mouseY < topPos + y1;
        var sprite = !enabled ? BUTTON_OFF : hovered ? BUTTON_HOVER : BUTTON;

        g.blitSprite(sprite, leftPos + x0, topPos + y0, x1 - x0, y1 - y0);

        var fitted = font.plainSubstrByWidth(label, x1 - x0 - 4);

        g.drawString(
            font,
            fitted,
            leftPos + x0 + (x1 - x0 - font.width(fitted)) / 2 + 1,
            topPos + y0 + (y1 - y0 - 8) / 2,
            enabled ? 0xFFFFFFFF : 0xFFA0A0A0,
            false
        );

        if (enabled) {
            click(x0, y0, x1, y1, action);
        }
    }

    private void scrollBar(GuiGraphics g, int x0, int y0, int x1, int y1, int scroll, int maxScroll, int shown, int total) {
        frame(g, x0, y0, x1, y1, MUTED, WELL);

        var track = y1 - y0 - 2;
        var thumb = Math.max(4, track * shown / Math.max(1, total));
        var top = maxScroll <= 0 ? 0 : (track - thumb) * scroll / maxScroll;

        fill(g, x0 + 1, y0 + 1 + top, x1 - 1, y0 + 1 + top + thumb, GREEN);
    }

    /** A tag ingredient shows each of its items in turn, a second each. */
    private ItemStack cycle(ItemStack[] options) {
        if (options.length == 0) {
            return ItemStack.EMPTY;
        }

        var time = minecraft == null || minecraft.level == null ? 0 : minecraft.level.getGameTime();

        return options[(int) ((time / 20) % options.length)];
    }
}
