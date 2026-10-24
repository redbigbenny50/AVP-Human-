package com.human.client.screen;

import com.human.Human;
import com.human.HumanResources;
import com.human.client.fluid.FluidClientInfo;
import com.human.common.gameplay.block.pipe.PipeBlockEntity;
import com.human.common.gameplay.block.pipe.PipeEndMenu;
import com.human.common.gameplay.block.pipe.PipeFilter;
import com.human.common.gameplay.item.canister.UniversalCanisterItem;
import com.human.common.network.packet.C2SPipeFilterPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * One pipe end's screen, in two pages of the same screen (so the container never closes between them).
 * <h2>MAIN PAGE</h2> The mode (Auto, Pull, Push, Both, Off), a preview of the filter's first lines, Allow / Block,
 * Priority, the Filter button, and Copy / Paste / Clear.
 * <h2>FILTER PAGE</h2> [stated] Oct 7: "a separate full-size filter screen opened by a Filter button", with a search
 * bar and hot buttons, TAGS, and as many lines as needed ([stated] Oct 8: unlimited). The search takes plain text for
 * items (or fluids), {@code #} for tags and {@code @} for whole mods; with nothing typed it lists the player's own
 * items first. Each line has a "Not enchanted" toggle (items) and a remove button.
 * <p>
 * Every edit is sent whole as a {@link C2SPipeFilterPayload}; the server applies it to this screen's end and the pipe
 * sends itself back, so every open screen agrees.
 */
public class PipeEndScreen extends AbstractContainerScreen<PipeEndMenu> {

    private static final ResourceLocation TEXTURE = HumanResources.location("textures/gui/container/pipe_end.png");

    private static final int MODE_Y = 18;

    private static final int MODE_W = 31;

    private static final int FILTER_Y = 42;

    private static final int CONTROL_Y = 64;

    private static final int LOWER_Y = 84;

    private static final int PREVIEW_COUNT = 9;

    private static final int GUI_GREY = 0xFFC6C6C6;

    private static final String[] MODES = { "auto", "pull", "push", "both", "off" };

    private static final String[] PRIORITIES = { "auto", "low", "normal", "high" };

    /** [stated] Oct 7 hot buttons, plus FOOD. Each adds one tag line. */
    private static final String[][] HOT_BUTTONS = {
        { "ores", "c:ores" },
        { "ingots", "c:ingots" },
        { "gems", "c:gems" },
        { "logs", "minecraft:logs" },
        { "stone", "c:stones" },
        { "dirt", "minecraft:dirt" },
        { "crops", "c:crops" },
        { "seeds", "c:seeds" },
        { "food", "c:foods" }
    };

    /** Copy / paste, one clipboard per pipe kind so an item filter never lands on a fluid pipe. */
    private static @Nullable PipeFilter itemClipboard;

    private static @Nullable PipeFilter fluidClipboard;

    private enum Page {
        MAIN,
        FILTER
    }

    private record Candidate(
        int registryId,
        ResourceLocation key,
        Component name,
        String haystack,
        @Nullable Fluid fluid,
        ItemStack icon
    ) {}

    /** One search result: an exact item / fluid, a tag, or a mod. */
    private record Result(
        PipeFilter.Kind kind,
        String id,
        @Nullable Candidate candidate
    ) {}

    private Page page = Page.MAIN;

    private @Nullable EditBox search;

    private boolean fluidPipe;

    private final List<Candidate> all = new ArrayList<>();

    private final Map<ResourceLocation, Candidate> byKey = new HashMap<>();

    private final List<Result> results = new ArrayList<>();

    private boolean textResults;

    private int resultScroll;

    private int entryScroll;

    /** The filter being edited on the filter page; refreshed from the pipe when the page opens. */
    private PipeFilter editing = new PipeFilter();

    public PipeEndScreen(PipeEndMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 128;
    }

    @Override
    protected void init() {
        super.init();
        fluidPipe = minecraft != null && minecraft.level != null && PipeEndMenu.isFluidPipe(minecraft.level, menu.getPipePos());
        buildCandidates();

        search = new EditBox(font, 0, 0, 150, 14, Component.translatable("gui.avp_human.pipe_end.search"));
        search.setHint(Component.translatable("gui.avp_human.pipe_end.search.hint").withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(text -> refilter());
        search.setMaxLength(100);
        addRenderableWidget(search);
        layoutSearch();
        refilter();
    }

    /**
     * Oct 14 FIX - the cryofluid report: "still unable to get the cryofluid into his cryochamber". Whether this is a
     * FLUID pipe is read from the block at the pipe's position, and that position reaches the client in the menu's data
     * slots - which arrive in the packets AFTER the screen opens (vanilla handles the open-screen packet, running init,
     * first). So init always saw (0, 0, 0), decided "item pipe", and listed ITEMS: on a fluid pipe the only "cryofluid"
     * to pick was the bucket ITEM, a line no fluid ever matches - and an Allow filter of it blocked all flow.
     * Re-checked every tick until the real position is in; the list is rebuilt the moment the answer changes.
     */
    @Override
    protected void containerTick() {
        super.containerTick();

        if (minecraft == null || minecraft.level == null) {
            return;
        }

        var nowFluid = PipeEndMenu.isFluidPipe(minecraft.level, menu.getPipePos());

        if (nowFluid != fluidPipe) {
            fluidPipe = nowFluid;
            buildCandidates();
            layoutSearch();
            refilter();
        }
    }

    private void layoutSearch() {
        if (search == null) {
            return;
        }

        search.visible = page == Page.FILTER;
        search.setX(panelX() + 8);
        search.setY(panelY() + 20);
    }

    private void buildCandidates() {
        all.clear();
        byKey.clear();

        if (fluidPipe) {
            for (var fluid : BuiltInRegistries.FLUID) {
                // Source fluids only - a pipe moves the fluid, not its flowing form.
                if (fluid == Fluids.EMPTY || !fluid.isSource(fluid.defaultFluidState())) {
                    continue;
                }

                var key = BuiltInRegistries.FLUID.getKey(fluid);
                var name = UniversalCanisterItem.fluidName(fluid);
                var bucket = fluid.getBucket();
                // [stated] Oct 14 - "it should treat the bucket and or the fluid the same": the bucket's own name and
                // id
                // find the fluid too, so searching "cryofluid bucket" lands on cryofluid.
                var bucketWords = bucket == Items.AIR
                    ? ""
                    : " " + new ItemStack(bucket).getHoverName().getString() + " " + BuiltInRegistries.ITEM.getKey(bucket);
                add(
                    new Candidate(
                        BuiltInRegistries.FLUID.getId(fluid),
                        key,
                        name,
                        (name.getString() + " " + key + bucketWords).toLowerCase(Locale.ROOT),
                        fluid,
                        bucket == Items.AIR ? ItemStack.EMPTY : new ItemStack(bucket)
                    )
                );
            }
        } else {
            for (var item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) {
                    continue;
                }

                var key = BuiltInRegistries.ITEM.getKey(item);
                var stack = new ItemStack(item);
                var name = stack.getHoverName();
                add(
                    new Candidate(
                        BuiltInRegistries.ITEM.getId(item),
                        key,
                        name,
                        (name.getString() + " " + key).toLowerCase(Locale.ROOT),
                        null,
                        stack
                    )
                );
            }
        }
    }

    private void add(Candidate candidate) {
        all.add(candidate);
        byKey.put(candidate.key(), candidate);
    }

    /** Rebuilds the search results: plain text, {@code #tag}, {@code @mod}, or (empty) the player's items first. */
    private void refilter() {
        results.clear();
        resultScroll = 0;
        var query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);

        if (query.startsWith("#")) {
            textResults = true;
            var text = query.substring(1);
            var names = new TreeSet<String>();
            var tags = fluidPipe
                ? BuiltInRegistries.FLUID.getTagNames().map(TagKey::location)
                : BuiltInRegistries.ITEM.getTagNames().map(TagKey::location);
            tags.forEach(id -> {
                var name = id.toString();

                if (name.contains(text)) {
                    names.add(name);
                }
            });
            names.forEach(name -> results.add(new Result(PipeFilter.Kind.TAG, name, null)));
            return;
        }

        if (query.startsWith("@")) {
            textResults = true;
            var text = query.substring(1);
            var mods = new TreeSet<String>();

            for (var candidate : all) {
                var namespace = candidate.key().getNamespace();

                if (namespace.contains(text)) {
                    mods.add(namespace);
                }
            }

            mods.forEach(mod -> results.add(new Result(PipeFilter.Kind.MOD, mod, null)));
            return;
        }

        textResults = false;

        if (query.isEmpty() && fluidPipe && minecraft != null && minecraft.player != null) {
            // [stated] Oct 14 - nothing typed on a FLUID pipe: the fluids of the buckets the player carries first, the
            // way an item pipe lists the player's own items first. Hold a cryofluid bucket and cryofluid is first.
            var own = new LinkedHashSet<ResourceLocation>();

            for (var stack : minecraft.player.getInventory().items) {
                if (stack.isEmpty() || stack.is(Items.BUCKET)) {
                    continue;
                }

                for (var candidate : all) {
                    if (candidate.fluid() != null && candidate.fluid().getBucket() == stack.getItem()) {
                        own.add(candidate.key());
                    }
                }
            }

            for (var key : own) {
                var candidate = byKey.get(key);

                if (candidate != null) {
                    results.add(new Result(PipeFilter.Kind.EXACT, key.toString(), candidate));
                }
            }

            for (var candidate : all) {
                if (!own.contains(candidate.key())) {
                    results.add(new Result(PipeFilter.Kind.EXACT, candidate.key().toString(), candidate));
                }
            }

            return;
        }

        if (query.isEmpty() && !fluidPipe && minecraft != null && minecraft.player != null) {
            // Nothing typed: the player's own items first - the usual reason to open a filter.
            var own = new LinkedHashSet<ResourceLocation>();

            for (var stack : minecraft.player.getInventory().items) {
                if (!stack.isEmpty()) {
                    own.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                }
            }

            for (var key : own) {
                var candidate = byKey.get(key);

                if (candidate != null) {
                    results.add(new Result(PipeFilter.Kind.EXACT, key.toString(), candidate));
                }
            }

            for (var candidate : all) {
                if (!own.contains(candidate.key())) {
                    results.add(new Result(PipeFilter.Kind.EXACT, candidate.key().toString(), candidate));
                }
            }

            return;
        }

        for (var candidate : all) {
            if (query.isEmpty() || candidate.haystack().contains(query)) {
                results.add(new Result(PipeFilter.Kind.EXACT, candidate.key().toString(), candidate));
            }
        }
    }

    private @Nullable PipeBlockEntity pipe() {
        return minecraft == null || minecraft.level == null ? null : menu.pipe(minecraft.level);
    }

    /** @return the pipe's current filter for this end, as the client last heard it */
    private PipeFilter liveFilter() {
        var pipe = pipe();
        return pipe == null ? new PipeFilter() : pipe.getFilter(menu.getFace());
    }

    /** Sends the edited filter; the server applies it and the pipe sends itself back. */
    private void send(PipeFilter filter) {
        Human.MOD.networking().sendToServer(new C2SPipeFilterPayload(filter.copy()));
        playClick();
    }

    private void openFilterPage() {
        editing = liveFilter().copy();
        page = Page.FILTER;
        entryScroll = 0;

        if (search != null) {
            search.setValue("");
            layoutSearch();
            setFocused(search);
            search.setFocused(true);
        }

        refilter();
    }

    private void closeFilterPage() {
        page = Page.MAIN;

        if (search != null) {
            search.setFocused(false);
            layoutSearch();
        }

        setFocused(null);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Filter page geometry
    // ------------------------------------------------------------------------------------------------------------

    private int panelWidth() {
        return Math.min(332, width - 8);
    }

    private int panelHeight() {
        return Math.min(232, height - 8);
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    private int rightX() {
        return panelX() + 168;
    }

    private int rightWidth() {
        return panelWidth() - 176;
    }

    private int resultsTop() {
        return panelY() + (fluidPipe ? 40 : 90);
    }

    private int resultsBottom() {
        return panelY() + panelHeight() - 8;
    }

    private int resultColumns() {
        return 8;
    }

    private int resultRows() {
        return Math.max(1, (resultsBottom() - resultsTop()) / 18);
    }

    private int textRows() {
        return Math.max(1, (resultsBottom() - resultsTop()) / 11);
    }

    private int entriesTop() {
        return panelY() + 56;
    }

    private int entriesBottom() {
        return panelY() + panelHeight() - 28;
    }

    private int entryRows() {
        return Math.max(1, (entriesBottom() - entriesTop()) / 18);
    }

    private int hotX(int index) {
        return panelX() + 8 + index % 3 * 51;
    }

    private int hotY(int index) {
        return panelY() + 38 + index / 3 * 16;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (page == Page.FILTER) {
            // Oct 14 FIX - [stated] "the text from the previous screen overlaps it". AbstractContainerScreen's
            // renderBackground is "dim the world, THEN renderBg" (read in the 1.21.1 bytecode), and renderBg draws the
            // whole main page - mode buttons, filter preview, Copy/Paste/Clear, "No lines" - under the filter page.
            // Only the dimming is wanted here.
            renderTransparentBackground(graphics);
            renderFilterPage(graphics, mouseX, mouseY, partialTick);
            return;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderMainTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, endTitle("gui.avp_human.pipe_end.title"), titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private Component endTitle(String key) {
        return Component.translatable(
            key,
            Component.translatable("message.avp_human.direction." + menu.getFace().getSerializedName())
        );
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        var pipe = pipe();
        var face = menu.getFace();
        var setting = pipe == null ? PipeBlockEntity.AUTO : pipe.getOverride(face);

        // Mode buttons: the chosen one lit.
        for (var mode = 0; mode < MODES.length; mode++) {
            var x = leftPos + 8 + mode * (MODE_W + 2);
            var y = topPos + MODE_Y;
            var chosen = mode == setting;
            var hovered = inArea(mouseX, mouseY, x, y, MODE_W, 14);
            graphics.fill(x, y, x + MODE_W, y + 14, chosen ? 0xFF2F6F2F : hovered ? 0xFF6A6A6A : 0xFF4A4A4A);
            // ⚠ Oct 23 - [stated] the mode is called "Output" again ("Extract" was 38 px on this 31 px button).
            // "Output"
            // is 31 px of ink - exactly the button - so it is centred on its INK, not on its advance: vanilla's
            // centring
            // counts the blank column after the last letter and would push the "O" one pixel past the left edge.
            var label = Component.translatable("message.avp_human.pipe_end." + MODES[mode]);
            var ink = Math.max(0, font.width(label) - 1);
            graphics.drawString(font, label, x + (MODE_W - ink) / 2, y + 3, 0xFFFFFF, true);
        }

        // Preview of the first lines (the full list is on the filter page).
        var filter = liveFilter();
        var entries = filter.entries();

        for (var index = 0; index < Math.min(PREVIEW_COUNT, entries.size()); index++) {
            drawEntryIcon(graphics, entries.get(index), leftPos + 8 + index * 18, topPos + FILTER_Y);
        }

        // Allow / Block, Priority, Filter.
        var y = topPos + CONTROL_Y;
        drawButton(graphics, leftPos + 8, y, 50, filter.isBlockList() ? 0xFF7A2A2A : 0xFF2A6A2A, allowLabel(filter), mouseX, mouseY);
        drawButton(graphics, leftPos + 60, y, 52, 0xFF4A4A4A, priorityLabel(filter), mouseX, mouseY);
        drawButton(
            graphics,
            leftPos + 114,
            y,
            54,
            0xFF2F4F6F,
            Component.translatable("gui.avp_human.pipe_end.filter", entries.size()),
            mouseX,
            mouseY
        );

        // The old search area becomes Copy / Paste / Clear and a summary.
        graphics.fill(leftPos + 7, topPos + LOWER_Y - 1, leftPos + 169, topPos + LOWER_Y + 37, GUI_GREY);
        drawButton(
            graphics,
            leftPos + 8,
            topPos + LOWER_Y,
            50,
            0xFF4A4A4A,
            Component.translatable("gui.avp_human.pipe_end.copy"),
            mouseX,
            mouseY
        );
        drawButton(
            graphics,
            leftPos + 60,
            topPos + LOWER_Y,
            52,
            0xFF4A4A4A,
            Component.translatable("gui.avp_human.pipe_end.paste"),
            mouseX,
            mouseY
        );
        drawButton(
            graphics,
            leftPos + 114,
            topPos + LOWER_Y,
            54,
            0xFF4A4A4A,
            Component.translatable("gui.avp_human.pipe_end.clear_all"),
            mouseX,
            mouseY
        );
        graphics.drawString(
            font,
            entries.isEmpty()
                ? Component.translatable("gui.avp_human.pipe_end.empty_filter")
                : Component.translatable("gui.avp_human.pipe_end.lines", entries.size()),
            leftPos + 8,
            topPos + LOWER_Y + 22,
            0x404040,
            false
        );
    }

    private Component allowLabel(PipeFilter filter) {
        return Component.translatable(filter.isBlockList() ? "gui.avp_human.pipe_end.block" : "gui.avp_human.pipe_end.allow");
    }

    private Component priorityLabel(PipeFilter filter) {
        return Component.translatable("gui.avp_human.pipe_end.priority." + PRIORITIES[filter.priority()]);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int w, int color, Component label, int mouseX, int mouseY) {
        var hovered = inArea(mouseX, mouseY, x, y, w, 15);
        graphics.fill(x, y, x + w, y + 15, hovered ? brighten(color) : color);
        graphics.drawCenteredString(font, label, x + w / 2, y + 4, 0xFFFFFF);
    }

    private static int brighten(int color) {
        var r = Math.min(255, (color >> 16 & 0xFF) + 32);
        var g = Math.min(255, (color >> 8 & 0xFF) + 32);
        var b = Math.min(255, (color & 0xFF) + 32);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private void renderFilterPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var px = panelX();
        var py = panelY();
        var pw = panelWidth();
        var ph = panelHeight();

        graphics.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF1A1A1A);
        graphics.fill(px, py, px + pw, py + ph, GUI_GREY);
        graphics.drawString(font, endTitle("gui.avp_human.pipe_end.filter_title"), px + 8, py + 6, 0x404040, false);

        if (search != null) {
            search.render(graphics, mouseX, mouseY, partialTick);
        }

        // Hot buttons (items only - fluids have no such families).
        if (!fluidPipe) {
            for (var index = 0; index < HOT_BUTTONS.length; index++) {
                var x = hotX(index);
                var y = hotY(index);
                var hovered = inArea(mouseX, mouseY, x, y, 48, 13);
                graphics.fill(x, y, x + 48, y + 13, hovered ? 0xFF6A6A6A : 0xFF4A4A4A);
                graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.avp_human.pipe_end.hot." + HOT_BUTTONS[index][0]),
                    x + 24,
                    y + 3,
                    0xFFFFFF
                );
            }
        }

        renderResults(graphics, mouseX, mouseY);
        renderEntries(graphics, mouseX, mouseY);

        // Right column controls.
        var rx = rightX();
        drawButton(graphics, rx, py + 20, 50, editing.isBlockList() ? 0xFF7A2A2A : 0xFF2A6A2A, allowLabel(editing), mouseX, mouseY);
        drawButton(graphics, rx + 54, py + 20, Math.max(60, rightWidth() - 54), 0xFF4A4A4A, priorityLabel(editing), mouseX, mouseY);
        drawButton(graphics, rx, py + 38, 40, 0xFF4A4A4A, Component.translatable("gui.avp_human.pipe_end.copy"), mouseX, mouseY);
        drawButton(graphics, rx + 44, py + 38, 40, 0xFF4A4A4A, Component.translatable("gui.avp_human.pipe_end.paste"), mouseX, mouseY);
        drawButton(graphics, rx + 88, py + 38, 40, 0xFF4A4A4A, Component.translatable("gui.avp_human.pipe_end.clear_all"), mouseX, mouseY);
        drawButton(
            graphics,
            px + pw - 58,
            py + ph - 22,
            50,
            0xFF2F6F2F,
            Component.translatable("gui.avp_human.pipe_end.done"),
            mouseX,
            mouseY
        );
        graphics.drawString(
            font,
            editing.isEmpty()
                ? Component.translatable("gui.avp_human.pipe_end.empty_filter")
                : Component.translatable("gui.avp_human.pipe_end.lines", editing.entries().size()),
            rx,
            py + ph - 18,
            0x404040,
            false
        );

        renderFilterTooltips(graphics, mouseX, mouseY);
    }

    private void renderResults(GuiGraphics graphics, int mouseX, int mouseY) {
        var x0 = panelX() + 8;
        var top = resultsTop();
        graphics.fill(x0 - 1, top - 1, x0 + 151, resultsBottom() + 1, 0xFF8B8B8B);

        if (textResults) {
            var rows = textRows();
            resultScroll = Mth.clamp(resultScroll, 0, Math.max(0, results.size() - rows));

            for (var row = 0; row < rows; row++) {
                var index = resultScroll + row;

                if (index >= results.size()) {
                    break;
                }

                var y = top + row * 11;
                var result = results.get(index);
                var hovered = inArea(mouseX, mouseY, x0, y, 150, 11);

                if (hovered) {
                    graphics.fill(x0, y, x0 + 150, y + 11, 0x40FFFFFF);
                }

                var label = (result.kind() == PipeFilter.Kind.TAG ? "#" : "@") + result.id();
                graphics.drawString(font, font.plainSubstrByWidth(label, 148), x0 + 1, y + 2, 0xFFFFFF, false);
            }

            return;
        }

        var columns = resultColumns();
        var rows = resultRows();
        var maxRow = Math.max(0, (results.size() + columns - 1) / columns - rows);
        resultScroll = Mth.clamp(resultScroll, 0, maxRow);

        for (var cell = 0; cell < columns * rows; cell++) {
            var index = resultScroll * columns + cell;

            if (index >= results.size()) {
                break;
            }

            var x = x0 + cell % columns * 18 + 3;
            var y = top + cell / columns * 18 + 1;
            var candidate = results.get(index).candidate();

            if (candidate != null) {
                drawCandidate(graphics, candidate, x, y);
            }

            if (inArea(mouseX, mouseY, x, y, 16, 16)) {
                graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
            }
        }
    }

    private void renderEntries(GuiGraphics graphics, int mouseX, int mouseY) {
        var rx = rightX();
        var rw = rightWidth();
        var top = entriesTop();
        var rows = entryRows();
        var entries = editing.entries();
        graphics.fill(rx - 1, top - 1, rx + rw + 1, top + rows * 18 + 1, 0xFF8B8B8B);
        entryScroll = Mth.clamp(entryScroll, 0, Math.max(0, entries.size() - rows));

        for (var row = 0; row < rows; row++) {
            var index = entryScroll + row;

            if (index >= entries.size()) {
                break;
            }

            var entry = entries.get(index);
            var y = top + row * 18;

            if (inArea(mouseX, mouseY, rx, y, rw, 18)) {
                graphics.fill(rx, y, rx + rw, y + 18, 0x30FFFFFF);
            }

            drawEntryIcon(graphics, entry, rx + 1, y + 1);
            var textWidth = rw - (fluidPipe ? 36 : 60);
            graphics.drawString(font, font.plainSubstrByWidth(entryLabel(entry).getString(), textWidth), rx + 20, y + 5, 0xFFFFFF, false);

            // "Not enchanted" toggle (items only) and remove.
            if (!fluidPipe) {
                var tx = rx + rw - 40;
                graphics.fill(tx, y + 2, tx + 24, y + 16, entry.notEnchanted() ? 0xFF6A4A8A : 0xFF4A4A4A);
                graphics.drawCenteredString(
                    font,
                    Component.translatable(
                        entry.notEnchanted() ? "gui.avp_human.pipe_end.not_enchanted.short" : "gui.avp_human.pipe_end.any"
                    ),
                    tx + 12,
                    y + 5,
                    0xFFFFFF
                );
            }

            var xx = rx + rw - 14;
            graphics.fill(xx, y + 3, xx + 12, y + 15, inArea(mouseX, mouseY, xx, y + 3, 12, 12) ? 0xFFAA3A3A : 0xFF7A2A2A);
            graphics.drawCenteredString(font, "x", xx + 6, y + 5, 0xFFFFFF);
        }
    }

    private Component entryLabel(PipeFilter.Entry entry) {
        return switch (entry.kind()) {
            case TAG -> Component.literal("#" + entry.id());
            case MOD -> Component.literal("@" + entry.id());
            case EXACT -> {
                var id = ResourceLocation.tryParse(entry.id());
                var candidate = id == null ? null : byKey.get(id);
                yield candidate == null ? Component.literal(entry.id()) : candidate.name();
            }
        };
    }

    private void drawEntryIcon(GuiGraphics graphics, PipeFilter.Entry entry, int x, int y) {
        switch (entry.kind()) {
            case EXACT -> {
                var id = ResourceLocation.tryParse(entry.id());
                var candidate = id == null ? null : byKey.get(id);

                if (candidate != null) {
                    drawCandidate(graphics, candidate, x, y);
                }
            }
            case TAG -> {
                var icon = tagIcon(entry.id());

                if (!icon.isEmpty()) {
                    graphics.renderItem(icon, x, y);
                }

                graphics.drawString(font, "#", x, y, 0xFFFFFF55, true);
            }
            case MOD -> graphics.drawString(font, "@", x + 5, y + 4, 0xFF55FFFF, true);
        }
    }

    /** @return the first item of a tag, as its icon, or empty */
    private ItemStack tagIcon(String tagId) {
        var id = ResourceLocation.tryParse(tagId);

        if (id == null) {
            return ItemStack.EMPTY;
        }

        if (fluidPipe) {
            return BuiltInRegistries.FLUID.getTag(TagKey.create(Registries.FLUID, id))
                .flatMap(set -> set.stream().findFirst())
                .map(holder -> holder.value().getBucket())
                .filter(bucket -> bucket != Items.AIR)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
        }

        return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id))
            .flatMap(set -> set.stream().findFirst())
            .map(holder -> new ItemStack(holder.value()))
            .orElse(ItemStack.EMPTY);
    }

    private void drawCandidate(GuiGraphics graphics, Candidate candidate, int x, int y) {
        if (!candidate.icon().isEmpty()) {
            graphics.renderItem(candidate.icon(), x, y);
            return;
        }

        if (candidate.fluid() == null || minecraft == null) {
            return;
        }

        var still = FluidClientInfo.Holder.get().stillTexture(candidate.fluid());

        if (still == null) {
            return;
        }

        var sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        var tint = FluidClientInfo.Holder.get().tint(candidate.fluid());
        graphics.blit(x, y, 0, 16, 16, sprite, (tint >> 16 & 0xFF) / 255F, (tint >> 8 & 0xFF) / 255F, (tint & 0xFF) / 255F, 1.0F);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (page == Page.FILTER) {
            filterPageClick(mouseX, mouseY, button);
            return true;
        }

        for (var mode = 0; mode < MODES.length; mode++) {
            if (inArea(mouseX, mouseY, leftPos + 8 + mode * (MODE_W + 2), topPos + MODE_Y, MODE_W, 14)) {
                click(PipeEndMenu.BUTTON_MODE + mode);
                return true;
            }
        }

        var y = topPos + CONTROL_Y;

        if (inArea(mouseX, mouseY, leftPos + 8, y, 50, 15)) {
            var filter = liveFilter().copy();
            filter.setBlockList(!filter.isBlockList());
            send(filter);
            return true;
        }

        if (inArea(mouseX, mouseY, leftPos + 60, y, 52, 15)) {
            send(cyclePriority(liveFilter().copy(), button));
            return true;
        }

        if (
            inArea(mouseX, mouseY, leftPos + 114, y, 54, 15) || inArea(
                mouseX,
                mouseY,
                leftPos + 8,
                topPos + FILTER_Y,
                PREVIEW_COUNT * 18,
                16
            )
        ) {
            playClick();
            openFilterPage();
            return true;
        }

        if (handleClipboard(mouseX, mouseY, leftPos + 8, leftPos + 60, leftPos + 114, topPos + LOWER_Y, 50, 52, 54, true)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static PipeFilter cyclePriority(PipeFilter filter, int button) {
        var step = button == 1 ? PRIORITIES.length - 1 : 1;
        filter.setPriority((byte) ((filter.priority() + step) % PRIORITIES.length));
        return filter;
    }

    /** Copy / Paste / Clear buttons, on either page. */
    private boolean handleClipboard(
        double mouseX,
        double mouseY,
        int copyX,
        int pasteX,
        int clearX,
        int y,
        int copyW,
        int pasteW,
        int clearW,
        boolean live
    ) {
        var source = live ? liveFilter() : editing;

        if (inArea(mouseX, mouseY, copyX, y, copyW, 15)) {
            if (fluidPipe) {
                fluidClipboard = source.copy();
            } else {
                itemClipboard = source.copy();
            }

            playClick();
            return true;
        }

        if (inArea(mouseX, mouseY, pasteX, y, pasteW, 15)) {
            var clipboard = fluidPipe ? fluidClipboard : itemClipboard;

            if (clipboard != null) {
                if (!live) {
                    editing = clipboard.copy();
                }

                send(clipboard);
            }

            return true;
        }

        if (inArea(mouseX, mouseY, clearX, y, clearW, 15)) {
            var cleared = source.copy();
            cleared.clear();

            if (!live) {
                editing = cleared;
            }

            send(cleared);
            return true;
        }

        return false;
    }

    private void filterPageClick(double mouseX, double mouseY, int button) {
        var px = panelX();
        var py = panelY();
        var pw = panelWidth();
        var ph = panelHeight();
        var rx = rightX();
        var rw = rightWidth();

        if (search != null && search.isMouseOver(mouseX, mouseY)) {
            search.mouseClicked(mouseX, mouseY, button);
            setFocused(search);
            search.setFocused(true);

            // Right-click clears the search, as in creative.
            if (button == 1) {
                search.setValue("");
            }

            return;
        }

        if (search != null) {
            search.setFocused(false);
        }

        // Done.
        if (inArea(mouseX, mouseY, px + pw - 58, py + ph - 22, 50, 15)) {
            playClick();
            closeFilterPage();
            return;
        }

        // Allow / Block, Priority.
        if (inArea(mouseX, mouseY, rx, py + 20, 50, 15)) {
            editing.setBlockList(!editing.isBlockList());
            send(editing);
            return;
        }

        if (inArea(mouseX, mouseY, rx + 54, py + 20, Math.max(60, rw - 54), 15)) {
            send(cyclePriority(editing, button));
            return;
        }

        if (handleClipboard(mouseX, mouseY, rx, rx + 44, rx + 88, py + 38, 40, 40, 40, false)) {
            return;
        }

        // Hot buttons.
        if (!fluidPipe) {
            for (var index = 0; index < HOT_BUTTONS.length; index++) {
                if (inArea(mouseX, mouseY, hotX(index), hotY(index), 48, 13)) {
                    addLine(new PipeFilter.Entry(PipeFilter.Kind.TAG, HOT_BUTTONS[index][1], false));
                    return;
                }
            }
        }

        // Search results.
        var x0 = px + 8;
        var top = resultsTop();

        if (textResults) {
            for (var row = 0; row < textRows(); row++) {
                var index = resultScroll + row;

                if (index < results.size() && inArea(mouseX, mouseY, x0, top + row * 11, 150, 11)) {
                    var result = results.get(index);
                    addLine(new PipeFilter.Entry(result.kind(), result.id(), false));
                    return;
                }
            }
        } else {
            var columns = resultColumns();

            for (var cell = 0; cell < columns * resultRows(); cell++) {
                var index = resultScroll * columns + cell;
                var x = x0 + cell % columns * 18 + 3;
                var y = top + cell / columns * 18 + 1;

                if (index < results.size() && inArea(mouseX, mouseY, x, y, 16, 16)) {
                    var result = results.get(index);
                    addLine(new PipeFilter.Entry(result.kind(), result.id(), false));
                    return;
                }
            }
        }

        // Lines: enchant toggle, remove.
        var entries = editing.entries();

        for (var row = 0; row < entryRows(); row++) {
            var index = entryScroll + row;

            if (index >= entries.size()) {
                break;
            }

            var y = entriesTop() + row * 18;

            if (inArea(mouseX, mouseY, rx + rw - 14, y + 3, 12, 12)) {
                editing.remove(index);
                send(editing);
                return;
            }

            if (!fluidPipe && inArea(mouseX, mouseY, rx + rw - 40, y + 2, 24, 14)) {
                editing.toggleEnchanted(index);
                send(editing);
                return;
            }
        }
    }

    private void addLine(PipeFilter.Entry entry) {
        if (editing.add(entry)) {
            send(editing);
            // Show the new line at the bottom of the list.
            entryScroll = Math.max(0, editing.entries().size() - entryRows());
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return page == Page.FILTER || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return page == Page.FILTER || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (page != Page.FILTER) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        var step = scrollY > 0 ? -1 : 1;

        if (inArea(mouseX, mouseY, rightX(), entriesTop(), rightWidth(), entryRows() * 18)) {
            entryScroll = Math.max(0, entryScroll + step);
        } else if (inArea(mouseX, mouseY, panelX() + 8, resultsTop(), 150, resultsBottom() - resultsTop())) {
            resultScroll = Math.max(0, resultScroll + step);
        }

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (page == Page.FILTER) {
            if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
                closeFilterPage();
                return true;
            }

            if (search != null && search.isFocused()) {
                search.keyPressed(keyCode, scanCode, modifiers);
            }

            // Everything else is swallowed, so the inventory key never closes the screen from the filter page.
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (page == Page.FILTER && search != null && search.isFocused()) {
            return search.charTyped(codePoint, modifiers);
        }

        return super.charTyped(codePoint, modifiers);
    }

    private void click(int id) {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }

        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        playClick();
    }

    private void playClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private boolean inArea(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Tooltips
    // ------------------------------------------------------------------------------------------------------------

    private void renderMainTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (var mode = 0; mode < MODES.length; mode++) {
            if (inArea(mouseX, mouseY, leftPos + 8 + mode * (MODE_W + 2), topPos + MODE_Y, MODE_W, 14)) {
                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    List.of(
                        Component.translatable("message.avp_human.pipe_end." + MODES[mode]),
                        Component.translatable("gui.avp_human.pipe_end.mode." + MODES[mode] + ".desc").withStyle(ChatFormatting.GRAY)
                    ),
                    mouseX,
                    mouseY
                );
                return;
            }
        }

        var y = topPos + CONTROL_Y;

        if (inArea(mouseX, mouseY, leftPos + 8, y, 50, 15)) {
            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                Component.translatable("gui.avp_human.pipe_end.toggle.desc"),
                mouseX,
                mouseY
            );
        } else if (inArea(mouseX, mouseY, leftPos + 60, y, 52, 15)) {
            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                Component.translatable("gui.avp_human.pipe_end.priority.desc"),
                mouseX,
                mouseY
            );
        } else if (inArea(mouseX, mouseY, leftPos + 114, y, 54, 15)) {
            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                Component.translatable("gui.avp_human.pipe_end.filter.desc"),
                mouseX,
                mouseY
            );
        } else {
            var entries = liveFilter().entries();

            for (var index = 0; index < Math.min(PREVIEW_COUNT, entries.size()); index++) {
                if (inArea(mouseX, mouseY, leftPos + 8 + index * 18, topPos + FILTER_Y, 16, 16)) {
                    com.human.client.screen.widget.GuiText.tooltip(graphics, font, entryLabel(entries.get(index)), mouseX, mouseY);
                    return;
                }
            }
        }
    }

    private void renderFilterTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        var py = panelY();
        var rx = rightX();
        var rw = rightWidth();

        if (inArea(mouseX, mouseY, rx, py + 20, 50, 15)) {
            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                Component.translatable("gui.avp_human.pipe_end.toggle.desc"),
                mouseX,
                mouseY
            );
            return;
        }

        if (inArea(mouseX, mouseY, rx + 54, py + 20, Math.max(60, rw - 54), 15)) {
            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                Component.translatable("gui.avp_human.pipe_end.priority.desc"),
                mouseX,
                mouseY
            );
            return;
        }

        if (!fluidPipe) {
            for (var index = 0; index < HOT_BUTTONS.length; index++) {
                if (inArea(mouseX, mouseY, hotX(index), hotY(index), 48, 13)) {
                    com.human.client.screen.widget.GuiText.tooltip(
                        graphics,
                        font,
                        Component.literal("#" + HOT_BUTTONS[index][1]).withStyle(ChatFormatting.GRAY),
                        mouseX,
                        mouseY
                    );
                    return;
                }
            }
        }

        if (!textResults) {
            var columns = resultColumns();

            for (var cell = 0; cell < columns * resultRows(); cell++) {
                var index = resultScroll * columns + cell;
                var x = panelX() + 8 + cell % columns * 18 + 3;
                var y = resultsTop() + cell / columns * 18 + 1;

                if (index < results.size() && inArea(mouseX, mouseY, x, y, 16, 16)) {
                    var candidate = results.get(index).candidate();

                    if (candidate != null) {
                        com.human.client.screen.widget.GuiText.tooltip(
                            graphics,
                            font,
                            List.of(candidate.name(), Component.literal(candidate.key().toString()).withStyle(ChatFormatting.DARK_GRAY)),
                            mouseX,
                            mouseY
                        );
                    }

                    return;
                }
            }
        }

        var entries = editing.entries();

        for (var row = 0; row < entryRows(); row++) {
            var index = entryScroll + row;

            if (index >= entries.size()) {
                break;
            }

            var y = entriesTop() + row * 18;

            if (inArea(mouseX, mouseY, rx + rw - 14, y + 3, 12, 12)) {
                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    Component.translatable("gui.avp_human.pipe_end.remove"),
                    mouseX,
                    mouseY
                );
                return;
            }

            if (!fluidPipe && inArea(mouseX, mouseY, rx + rw - 40, y + 2, 24, 14)) {
                com.human.client.screen.widget.GuiText.tooltip(
                    graphics,
                    font,
                    Component.translatable("gui.avp_human.pipe_end.not_enchanted.desc"),
                    mouseX,
                    mouseY
                );
                return;
            }

            if (inArea(mouseX, mouseY, rx, y, rw, 18)) {
                com.human.client.screen.widget.GuiText.tooltip(graphics, font, entryLabel(entries.get(index)), mouseX, mouseY);
                return;
            }
        }
    }
}
