package com.human.client.screen;

import com.human.common.gameplay.block.cryo.CryotubeControllerBlockEntity;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.gameplay.menu.cryo.CryotubeControllerMenu;
import com.human.common.registry.init.HumanSoundEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CRYOLINK - the cryotube controller's screen. [stated] Oct 2: the four-tab design (OVERVIEW, CRYOTUBES, SPECIMENS,
 * DIAGNOSTICS) cleaned up in the MU-TH-UR terminal style from his dropship mod - black panels, green frames, white
 * values - with that terminal's sounds.
 * <p>
 * Everything shown comes from the controller's snapshot (see {@code CryotubeControllerBlockEntity#snapshot}), which the
 * server sends only while this screen is open. Item slots exist only on DIAGNOSTICS.
 * </p>
 * ⚠ Drawn with fills and the vanilla font until a GUI texture exists; the layout is the one the team approved.
 */
public class CryotubeControllerScreen extends AbstractContainerScreen<CryotubeControllerMenu> {

    // Palette - [stated] Oct 2: the CRYOLINK design's own colours (sampled from it): a near-black green screen, one
    // bright phosphor green for frames, text and selections, a dimmer green for secondary text. Selected things are
    // filled solid green with dark text. Amber is the one extra colour, for warnings.
    private static final int BG = 0xFF051008;

    private static final int PANEL = 0xFF09190F;

    private static final int HEAD = 0xFF09190F;

    private static final int GREEN = 0xFF6EF58C;

    private static final int BUTTON = 0xFF6EF58C;

    private static final int WELL = 0xFF0E2816;

    private static final int WHITE = 0xFF6EF58C;

    private static final int DARK = 0xFF051008;

    private static final int MUTED = 0xFF33974E;

    private static final int GOOD = 0xFF6EF58C;

    private static final int WARN = 0xFFF0BE5A;

    private static final int FLUID = 0xFF6EF58C;

    private static final String[] TABS = { "overview", "cryotubes", "specimens", "diagnostics" };

    private static final int TUBES_PER_PAGE = 4;

    private static final long CONFIRM_WINDOW_MS = 3000L;

    private int tab;

    private int tubePage;

    private int selectedTube;

    private int specimenPage;

    private int selectedSpecimen;

    private int selectedAlert;

    private long armedAt = -1L;

    private int armedTube = -1;

    private @Nullable String hovered;

    /** Click targets laid out this frame: id -> {x0, y0, x1, y1}. */
    private final Map<String, int[]> targets = new HashMap<>();

    /** One display copy per specimen type, for the previews. */
    private final Map<String, LivingEntity> previews = new HashMap<>();

    public CryotubeControllerScreen(CryotubeControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = CryotubeControllerMenu.GUI_WIDTH;
        imageHeight = CryotubeControllerMenu.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        menu.setSlotsShown(tab == 3);
        ui(HumanSoundEvents.TERMINAL_MENU_OPEN.get());
    }

    @Override
    public void removed() {
        ui(HumanSoundEvents.TERMINAL_MENU_CLOSE.get());
        super.removed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------------------------------------------------

    private CompoundTag snapshot() {
        if (minecraft == null || minecraft.level == null) {
            return new CompoundTag();
        }

        var be = minecraft.level.getBlockEntity(menu.controllerPos());
        var snap = be instanceof CryotubeControllerBlockEntity controller ? controller.getClientSnapshot() : null;
        return snap == null ? new CompoundTag() : snap;
    }

    private static List<CompoundTag> tubes(CompoundTag snap) {
        var out = new ArrayList<CompoundTag>();

        for (var tag : snap.getList("tubes", Tag.TAG_COMPOUND)) {
            out.add((CompoundTag) tag);
        }

        return out;
    }

    private static List<CompoundTag> occupied(CompoundTag snap) {
        var out = new ArrayList<CompoundTag>();

        for (var tube : tubes(snap)) {
            if (tube.contains("specimen")) {
                out.add(tube);
            }
        }

        return out;
    }

    private static Component speciesName(CompoundTag tube) {
        var id = tube.getString("specimen");
        return EntityType.byString(id).map(EntityType::getDescription).orElse(Component.translatable("gui.avp_human.cryo.unknown"));
    }

    private static String tubeLabel(CompoundTag tube) {
        return String.format("TUBE %02d", tube.getInt("n"));
    }

    private @Nullable LivingEntity preview(String typeId) {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }

        return previews.computeIfAbsent(typeId, id -> {
            var made = EntityType.byString(id).map(type -> type.create(minecraft.level)).orElse(null);

            if (made instanceof LivingEntity living) {
                // [stated] Oct 2: an ICON copy - its own model in flat bright green, held on one frame of its idle.
                com.human.client.render.cryo.CryoIcon.mark(living);
                return living;
            }

            return null;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // [stated] Oct 3: clicking the hive nickname copies the full lineage id.
        if (button == 0 && hiveX0 >= 0 && mouseX >= hiveX0 && mouseX < hiveX1 && mouseY >= hiveY0 && mouseY < hiveY1) {
            if (minecraft != null) {
                minecraft.keyboardHandler.setClipboard(hiveFullId);
                minecraft.getSoundManager()
                    .play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK,
                            1.0F
                        )
                    );
            }

            hiveCopiedAt = System.currentTimeMillis();
            return true;
        }

        if (button == 0 && overGeneBar(mouseX, mouseY)) {
            draggingGeneBar = true;
            scrollGeneBarTo(mouseY);
            return true;
        }

        var hit = hitTarget(mouseX, mouseY);

        if (hit != null && button == 0) {
            click(hit);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private @Nullable String hitTarget(double mouseX, double mouseY) {
        for (var entry : targets.entrySet()) {
            var r = entry.getValue();

            if (mouseX >= r[0] && mouseX < r[2] && mouseY >= r[1] && mouseY < r[3]) {
                return entry.getKey();
            }
        }

        return null;
    }

    private void click(String id) {
        var snap = snapshot();

        if (id.startsWith("tab:")) {
            tab = Integer.parseInt(id.substring(4));
            menu.setSlotsShown(tab == 3);
            disarm(false);
            ui(HumanSoundEvents.TERMINAL_SELECT.get());
        } else if (id.startsWith("tube:")) {
            selectedTube = Integer.parseInt(id.substring(5));
            disarm(false);
            ui(HumanSoundEvents.TERMINAL_SELECT.get());
        } else if (id.startsWith("spec:")) {
            selectedSpecimen = Integer.parseInt(id.substring(5));
            ui(HumanSoundEvents.TERMINAL_SELECT.get());
        } else if (id.startsWith("alert:")) {
            selectedAlert = Integer.parseInt(id.substring(6));
            ui(HumanSoundEvents.TERMINAL_SELECT.get());
        } else if (id.equals("tubes:prev") || id.equals("tubes:next")) {
            var pages = Math.max(1, (tubes(snap).size() + TUBES_PER_PAGE - 1) / TUBES_PER_PAGE);
            tubePage = Math.floorMod(tubePage + (id.endsWith("next") ? 1 : -1), pages);
            ui(HumanSoundEvents.TERMINAL_SELECT.get());
        } else if (id.equals("specs:prev") || id.equals("specs:next")) {
            var pages = Math.max(1, (occupied(snap).size() + TUBES_PER_PAGE - 1) / TUBES_PER_PAGE);
            specimenPage = Math.floorMod(specimenPage + (id.endsWith("next") ? 1 : -1), pages);
            ui(HumanSoundEvents.TERMINAL_SELECT.get());
        } else if (id.equals("prio:up")) {
            send(CryotubeControllerMenu.BUTTON_PRIORITY_UP + selectedTube);
            ui(HumanSoundEvents.TERMINAL_SELECT.get());
        } else if (id.equals("prio:down")) {
            send(CryotubeControllerMenu.BUTTON_PRIORITY_DOWN + selectedTube);
            ui(HumanSoundEvents.TERMINAL_SELECT.get());
        } else if (id.equals("toggle")) {
            // Deactivating starts a thaw that ends with the specimen let out, so it takes a second click to confirm.
            // Reactivating is safe and takes one.
            var tubes = tubes(snap);
            var tube = selectedTube < tubes.size() ? tubes.get(selectedTube) : null;

            if (tube == null || !tube.contains("specimen")) {
                return;
            }

            var reactivating = tube.getInt("state") == CryotubeControllerBlockEntity.TUBE_DEACTIVATED;

            if (reactivating || (armedTube == selectedTube && System.currentTimeMillis() - armedAt <= CONFIRM_WINDOW_MS)) {
                armedTube = -1;
                ui(HumanSoundEvents.TERMINAL_CONFIRM.get());
                send(CryotubeControllerMenu.BUTTON_TOGGLE_DEACTIVATE + selectedTube);
            } else {
                armedTube = selectedTube;
                armedAt = System.currentTimeMillis();
                ui(HumanSoundEvents.TERMINAL_SELECT.get());
            }
        }
    }

    private void disarm(boolean withSound) {
        if (armedTube >= 0) {
            armedTube = -1;

            if (withSound) {
                ui(HumanSoundEvents.TERMINAL_CANCEL.get());
            }
        }
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private void ui(SoundEvent sound) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 0.8F));
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing helpers
    // ------------------------------------------------------------------------------------------------------------

    private void target(String id, int x0, int y0, int x1, int y1) {
        targets.put(id, new int[] { leftPos + x0, topPos + y0, leftPos + x1, topPos + y1 });
    }

    private void frame(GuiGraphics g, int x0, int y0, int x1, int y1, int fill, int border) {
        g.fill(leftPos + x0, topPos + y0, leftPos + x1, topPos + y1, border);
        g.fill(leftPos + x0 + 1, topPos + y0 + 1, leftPos + x1 - 1, topPos + y1 - 1, fill);
    }

    private void panel(GuiGraphics g, int x0, int y0, int x1, int y1, String titleKey) {
        frame(g, x0, y0, x1, y1, PANEL, GREEN);

        if (titleKey != null) {
            text(g, Component.translatable(titleKey), x0 + 4, y0 + 2, GREEN);
            g.fill(leftPos + x0, topPos + y0 + 11, leftPos + x1, topPos + y0 + 12, GREEN);
        }
    }

    private void text(GuiGraphics g, Component text, int x, int y, int colour) {
        g.drawString(font, text, leftPos + x, topPos + y, colour, false);
    }

    private void text(GuiGraphics g, String text, int x, int y, int colour) {
        g.drawString(font, text, leftPos + x, topPos + y, colour, false);
    }

    private void textRight(GuiGraphics g, String text, int right, int y, int colour) {
        g.drawString(font, text, leftPos + right - font.width(text), topPos + y, colour, false);
    }

    private void textRight(GuiGraphics g, Component text, int right, int y, int colour) {
        g.drawString(font, text, leftPos + right - font.width(text), topPos + y, colour, false);
    }

    private void row(GuiGraphics g, String labelKey, String value, int x0, int x1, int y, int colour) {
        text(g, Component.translatable(labelKey), x0, y, GREEN);
        textRight(g, value, x1, y, colour);
    }

    private void bar(GuiGraphics g, int x0, int y, int x1, float fraction, int colour) {
        frame(g, x0, y, x1, y + 6, WELL, GREEN);
        var width = (int) ((x1 - x0 - 2) * Math.max(0.0F, Math.min(1.0F, fraction)));
        g.fill(leftPos + x0 + 1, topPos + y + 1, leftPos + x0 + 1 + width, topPos + y + 5, colour);
    }

    /** A clickable button, highlighted on hover; returns nothing - clicks arrive through the target map. */
    private void button(GuiGraphics g, String id, Component label, int x0, int y0, int x1, int y1, boolean on, int mouseX, int mouseY) {
        target(id, x0, y0, x1, y1);
        var hover = id.equals(hovered);
        frame(g, x0, y0, x1, y1, on ? BUTTON : hover ? WELL : PANEL, GREEN);
        var w = font.width(label);
        g.drawString(font, label, leftPos + (x0 + x1 - w) / 2, topPos + (y0 + y1 - 8) / 2, on ? DARK : GREEN, false);
    }

    private static String clock(long dayTime) {
        var ticks = Math.floorMod(dayTime, 24000L);
        var hours = (int) ((ticks / 1000L + 6L) % 24L);
        var minutes = (int) (ticks % 1000L * 60L / 1000L);
        return String.format("%02d:%02d", hours, minutes);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Frame
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (armedTube >= 0 && System.currentTimeMillis() - armedAt > CONFIRM_WINDOW_MS) {
            disarm(true);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        var now = hitTarget(mouseX, mouseY);

        if (now != null && !now.equals(hovered)) {
            ui(HumanSoundEvents.TERMINAL_MOUSEOVER.get());
        }

        hovered = now;
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        targets.clear();
        var snap = snapshot();

        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG);

        // Header.
        g.pose().pushPose();
        g.pose().translate(leftPos + 8, topPos + 5, 0);
        g.pose().scale(2.0F, 2.0F, 1.0F);
        g.drawString(font, "/// CRYOLINK", 0, 0, GREEN, false);
        g.pose().popPose();
        text(g, Component.translatable("gui.avp_human.cryo.subtitle"), 8, 22, MUTED);
        textRight(g, "WEYLAND-YUTANI", 288, 6, GREEN);
        textRight(g, Component.translatable("gui.avp_human.cryo.controller"), 288, 15, MUTED);

        for (int i = 0; i < TABS.length; i++) {
            var x = 8 + i * 70;
            button(g, "tab:" + i, Component.translatable("gui.avp_human.cryo.tab." + TABS[i]), x, 32, x + 66, 44, i == tab, mouseX, mouseY);
        }

        switch (tab) {
            case 0 -> overview(g, snap);
            case 1 -> cryotubes(g, snap, mouseX, mouseY);
            case 2 -> specimens(g, snap, mouseX, mouseY);
            default -> diagnostics(g, snap, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // Everything is drawn in renderBg; no default title or inventory label on this screen.
    }

    // ------------------------------------------------------------------------------------------------------------
    // OVERVIEW
    // ------------------------------------------------------------------------------------------------------------

    private void overview(GuiGraphics g, CompoundTag snap) {
        var tubes = tubes(snap);
        var status = snap.getInt("status");
        int active = 0, standby = 0;

        for (var tube : tubes) {
            if (tube.contains("specimen")) {
                active++;
            } else if (tube.getInt("state") == CryotubeControllerBlockEntity.TUBE_STANDBY) {
                standby++;
            }
        }

        panel(g, 8, 48, 146, 228, "gui.avp_human.cryo.panel.system");
        text(g, Component.translatable("gui.avp_human.cryo.label.status"), 12, 63, GREEN);
        var nominal = status == CryotubeControllerBlockEntity.STATUS_RUNNING || status == CryotubeControllerBlockEntity.STATUS_IDLE;
        var pill = Component.translatable("gui.avp_human.cryo.status." + statusKey(status));
        frame(g, 142 - font.width(pill) - 8, 61, 142, 72, nominal ? GREEN : WARN, nominal ? GREEN : WARN);
        textRight(g, pill, 138, 63, DARK);

        row(
            g,
            "gui.avp_human.cryo.label.connected",
            String.format("%02d / %02d", tubes.size(), snap.getInt("tubeCap")),
            12,
            142,
            78,
            WHITE
        );
        row(g, "gui.avp_human.cryo.label.active", String.format("%02d", active), 12, 142, 88, WHITE);
        row(g, "gui.avp_human.cryo.label.standby", String.format("%02d", standby), 12, 142, 98, WHITE);
        g.fill(leftPos + 12, topPos + 109, leftPos + 142, topPos + 110, GREEN);

        var cells = snap.getInt("cells");
        row(
            g,
            "gui.avp_human.cryo.label.battery",
            cells == 0 ? "--" : Math.round(snap.getFloat("charge") * 100) + "%",
            12,
            142,
            113,
            WHITE
        );
        bar(g, 12, 123, 142, snap.getFloat("charge"), GREEN);
        row(g, "gui.avp_human.cryo.label.draw", snap.getLong("draw") + " FE/t", 12, 142, 132, WHITE);
        var grid = snap.getBoolean("gridSupplied") ? "supplied" : snap.getBoolean("gridPartly") ? "partly" : "none";
        row(
            g,
            "gui.avp_human.cryo.label.grid",
            Component.translatable("gui.avp_human.cryo.grid." + grid).getString(),
            12,
            142,
            142,
            grid.equals("supplied") ? GOOD : WARN
        );
        g.fill(leftPos + 12, topPos + 153, leftPos + 142, topPos + 154, GREEN);

        var fuel = snap.getInt("fuel");
        row(g, "gui.avp_human.cryo.label.cryofluid", String.format("%,d mB", fuel), 12, 142, 157, WHITE);
        bar(g, 12, 167, 142, fuel / (float) CryotubeControllerBlockEntity.CAPACITY, FLUID);
        var usage = snap.getDouble("usage");
        row(g, "gui.avp_human.cryo.label.usage", String.format("%.1f mB/s", usage), 12, 142, 176, WHITE);
        var seconds = usage > 0 ? (long) (fuel / usage) : -1L;
        row(
            g,
            "gui.avp_human.cryo.label.time_left",
            seconds < 0 ? "--" : (seconds / 3600) + "h " + String.format("%02d", seconds / 60 % 60) + "m",
            12,
            142,
            186,
            WHITE
        );

        // Network diagram: the controller and one column per tube, lit while it holds a specimen.
        panel(g, 150, 48, 288, 128, "gui.avp_human.cryo.panel.network");
        frame(g, 209, 62, 229, 70, GREEN, GREEN);
        g.fill(leftPos + 218, topPos + 70, leftPos + 220, topPos + 76, GREEN);
        var count = Math.max(1, Math.min(8, tubes.size()));
        var spacing = 120 / count;
        g.fill(leftPos + 158, topPos + 76, leftPos + 160 + spacing * (count - 1) + 8, topPos + 77, GREEN);

        for (int i = 0; i < Math.min(8, tubes.size()); i++) {
            var x = 158 + i * spacing;
            var state = tubes.get(i).getInt("state");
            var lit = tubes.get(i).contains("specimen");
            g.fill(leftPos + x + 4, topPos + 76, leftPos + x + 5, topPos + 80, GREEN);
            frame(
                g,
                x,
                80,
                x + 9,
                120,
                PANEL,
                state == CryotubeControllerBlockEntity.TUBE_THAWING || state == CryotubeControllerBlockEntity.TUBE_DEACTIVATED
                    ? WARN
                    : GREEN
            );

            if (lit) {
                g.fill(leftPos + x + 2, topPos + 82, leftPos + x + 7, topPos + 118, GREEN);
            }
        }

        panel(g, 150, 132, 288, 228, "gui.avp_human.cryo.panel.events");
        var events = snap.getList("events", Tag.TAG_COMPOUND);

        for (int i = 0; i < Math.min(9, events.size()); i++) {
            var event = events.getCompound(i);
            var y = 147 + i * 9;
            text(g, clock(event.getLong("t")), 154, y, GREEN);
            var message = eventText(event);
            var shown = font.plainSubstrByWidth(message.getString(), 284 - 182);
            text(g, shown, 182, y, eventColour(event.getString("k")));
        }
    }

    private static Component eventText(CompoundTag event) {
        var args = event.getList("a", Tag.TAG_STRING);
        var values = new Object[args.size()];

        for (int i = 0; i < args.size(); i++) {
            values[i] = args.getString(i);
        }

        return Component.translatable("gui.avp_human.cryo.event." + event.getString("k"), values);
    }

    private static int eventColour(String key) {
        return switch (key) {
            case "broke_out", "escaped", "broken", "broken_by", "broken_near", "thawing", "fuel_empty", "on_battery" -> WARN;
            case "power_restored", "frozen", "reactivated" -> GOOD;
            default -> WHITE;
        };
    }

    // ------------------------------------------------------------------------------------------------------------
    // CRYOTUBES
    // ------------------------------------------------------------------------------------------------------------

    private void cryotubes(GuiGraphics g, CompoundTag snap, int mouseX, int mouseY) {
        var tubes = tubes(snap);
        selectedTube = Math.min(selectedTube, Math.max(0, tubes.size() - 1));
        var pages = Math.max(1, (tubes.size() + TUBES_PER_PAGE - 1) / TUBES_PER_PAGE);
        tubePage = Math.min(tubePage, pages - 1);

        panel(g, 8, 48, 110, 228, "gui.avp_human.cryo.panel.tubes");

        for (int i = 0; i < TUBES_PER_PAGE; i++) {
            var index = tubePage * TUBES_PER_PAGE + i;

            if (index >= tubes.size()) {
                break;
            }

            var tube = tubes.get(index);
            var y = 62 + i * 36;
            var on = index == selectedTube;
            target("tube:" + index, 14, y, 104, y + 30);
            frame(g, 14, y, 104, y + 30, on ? BUTTON : ("tube:" + index).equals(hovered) ? WELL : PANEL, GREEN);
            text(g, tubeLabel(tube), 18, y + 5, on ? DARK : GREEN);
            textRight(g, "P" + tube.getInt("priority"), 100, y + 5, on ? DARK : GREEN);
            var who = tube.contains("specimen") ? speciesName(tube) : Component.translatable("gui.avp_human.cryo.empty");
            text(g, font.plainSubstrByWidth(who.getString(), 84), 18, y + 17, on ? DARK : tube.contains("specimen") ? GREEN : MUTED);
        }

        if (tubes.isEmpty()) {
            text(g, Component.translatable("gui.avp_human.cryo.no_tubes"), 16, 64, MUTED);
        }

        pager(g, "tubes", tubePage, pages, 59, 213, mouseX, mouseY);

        panel(g, 114, 48, 288, 228, null);

        if (tubes.isEmpty()) {
            return;
        }

        var tube = tubes.get(selectedTube);
        frame(g, 114, 48, 288, 59, HEAD, GREEN);
        text(g, tubeLabel(tube), 118, 50, GREEN);

        // The tube graphic, filled while it holds a specimen.
        var state = tube.getInt("state");
        frame(g, 124, 66, 150, 166, PANEL, GREEN);
        g.fill(leftPos + 124, topPos + 162, leftPos + 150, topPos + 166, GREEN);

        if (tube.contains("specimen")) {
            g.fill(
                leftPos + 127,
                topPos + 69,
                leftPos + 147,
                topPos + 161,
                state == CryotubeControllerBlockEntity.TUBE_ACTIVE ? MUTED : WARN
            );
        }

        var occupied = tube.contains("specimen");
        var blocks = tube.getInt("blocks");
        var drawing = state == CryotubeControllerBlockEntity.TUBE_ACTIVE;
        row(g, "gui.avp_human.cryo.label.size", tube.getString("size"), 160, 284, 64, WHITE);
        row(
            g,
            "gui.avp_human.cryo.label.status",
            Component.translatable("gui.avp_human.cryo.tube." + tubeStateKey(state)).getString(),
            160,
            284,
            74,
            tubeStateColour(state)
        );
        row(
            g,
            "gui.avp_human.cryo.label.specimen",
            occupied ? speciesName(tube).getString() : Component.translatable("gui.avp_human.cryo.none").getString(),
            160,
            284,
            84,
            occupied ? WHITE : MUTED
        );
        row(g, "gui.avp_human.cryo.label.power", drawing ? blocks + " FE/t" : "0 FE/t", 160, 284, 94, WHITE);
        row(g, "gui.avp_human.cryo.label.fuel", drawing ? String.format("%.1f mB/s", blocks * 0.1) : "0 mB/s", 160, 284, 104, WHITE);
        row(
            g,
            "gui.avp_human.cryo.label.thaw",
            tube.getInt("thaw") > 0 ? tube.getInt("thaw") + "%" : "--",
            160,
            284,
            114,
            tube.getInt("thaw") > 0 ? WARN : MUTED
        );

        text(g, Component.translatable("gui.avp_human.cryo.label.priority"), 160, 132, GREEN);
        button(g, "prio:up", Component.literal("-"), 232, 129, 243, 140, false, mouseX, mouseY);
        textRight(g, String.valueOf(tube.getInt("priority")), 256, 131, WHITE);
        button(g, "prio:down", Component.literal("+"), 262, 129, 273, 140, false, mouseX, mouseY);
        text(g, Component.translatable("gui.avp_human.cryo.priority_hint"), 160, 143, MUTED);

        // [stated] Oct 2: empty + connected = standby, ready to take a specimen; occupied = active, and deactivating
        // starts the thaw.
        if (occupied) {
            var key = state == CryotubeControllerBlockEntity.TUBE_DEACTIVATED
                ? "reactivate"
                : armedTube == selectedTube
                    ? "confirm_deactivate"
                    : "deactivate";
            button(g, "toggle", Component.translatable("gui.avp_human.cryo.action." + key), 160, 192, 282, 206, true, mouseX, mouseY);
            // [stated] Oct 3: the hint ran off the right of the panel. Wrapped to the button's width (160-282), two
            // lines
            // at most, ending inside the panel's bottom border (228).
            var hint = font.split(Component.translatable("gui.avp_human.cryo.deactivate_hint"), 282 - 160);

            for (int line = 0; line < Math.min(2, hint.size()); line++) {
                g.drawString(font, hint.get(line), leftPos + 160, topPos + 209 + line * 9, MUTED, false);
            }
        } else {
            frame(g, 160, 192, 282, 206, PANEL, GREEN);
            var label = Component.translatable("gui.avp_human.cryo.action.standby");
            text(g, label, (160 + 282 - font.width(label)) / 2, 195, GOOD);
        }
    }

    private void pager(GuiGraphics g, String id, int page, int pages, int centre, int y, int mouseX, int mouseY) {
        button(g, id + ":prev", Component.literal("<"), centre - 40, y - 2, centre - 28, y + 9, false, mouseX, mouseY);
        var label = (page + 1) + " / " + pages;
        text(g, label, centre - font.width(label) / 2, y, GREEN);
        button(g, id + ":next", Component.literal(">"), centre + 28, y - 2, centre + 40, y + 9, false, mouseX, mouseY);
    }

    // ------------------------------------------------------------------------------------------------------------
    // SPECIMENS
    // ------------------------------------------------------------------------------------------------------------

    private void specimens(GuiGraphics g, CompoundTag snap, int mouseX, int mouseY) {
        var list = occupied(snap);
        selectedSpecimen = Math.min(selectedSpecimen, Math.max(0, list.size() - 1));
        var pages = Math.max(1, (list.size() + TUBES_PER_PAGE - 1) / TUBES_PER_PAGE);
        specimenPage = Math.min(specimenPage, pages - 1);

        panel(g, 8, 48, 110, 228, "gui.avp_human.cryo.panel.specimens");

        for (int i = 0; i < TUBES_PER_PAGE; i++) {
            var index = specimenPage * TUBES_PER_PAGE + i;

            if (index >= list.size()) {
                break;
            }

            var tube = list.get(index);
            var y = 62 + i * 36;
            var on = index == selectedSpecimen;
            target("spec:" + index, 14, y, 104, y + 30);
            frame(g, 14, y, 104, y + 30, on ? BUTTON : ("spec:" + index).equals(hovered) ? WELL : PANEL, GREEN);
            frame(g, 17, y + 3, 41, y + 27, PANEL, on ? DARK : GREEN);
            var mob = preview(tube.getString("specimen"));

            if (mob != null) {
                entity(g, mob, 17, y + 3, 41, y + 27, mouseX, mouseY);
            }

            text(g, font.plainSubstrByWidth(speciesName(tube).getString(), 58), 45, y + 6, on ? DARK : GREEN);
            text(g, tubeLabel(tube), 45, y + 17, on ? DARK : MUTED);
        }

        if (list.isEmpty()) {
            text(g, Component.translatable("gui.avp_human.cryo.no_specimens"), 16, 64, MUTED);
        }

        pager(g, "specs", specimenPage, pages, 59, 213, mouseX, mouseY);

        panel(g, 114, 48, 288, 128, "gui.avp_human.cryo.panel.specimen_data");
        panel(g, 114, 132, 288, 228, "gui.avp_human.cryo.panel.genetics");

        if (list.isEmpty()) {
            return;
        }

        var tube = list.get(selectedSpecimen);
        var card = tube.getCompound("card");
        frame(g, 118, 63, 178, 124, PANEL, GREEN);
        var mob = preview(tube.getString("specimen"));

        if (mob != null) {
            entity(g, mob, 118, 63, 178, 124, mouseX, mouseY);
        }

        row(g, "gui.avp_human.cryo.label.species", font.plainSubstrByWidth(speciesName(tube).getString(), 60), 184, 284, 63, WHITE);
        row(g, "gui.avp_human.cryo.label.tube", String.format("%02d (%s)", tube.getInt("n"), tube.getString("size")), 184, 284, 73, WHITE);
        row(
            g,
            "gui.avp_human.cryo.label.health",
            Math.round(card.getFloat("health")) + " / " + Math.round(card.getFloat("maxHealth")),
            184,
            284,
            83,
            WHITE
        );
        row(g, "gui.avp_human.cryo.label.armor", String.format("%.1f", card.getFloat("armor")), 184, 284, 93, WHITE);
        row(
            g,
            "gui.avp_human.cryo.label.attack",
            card.getDouble("attack") < 0 ? "--" : String.format("%.1f", card.getDouble("attack")),
            184,
            284,
            103,
            WHITE
        );
        row(
            g,
            "gui.avp_human.cryo.label.speed",
            card.getDouble("speed") < 0 ? "--" : String.format("%.2f", card.getDouble("speed")),
            184,
            284,
            113,
            WHITE
        );

        // Genetic data: a xenomorph's strain / stage / hive, then [stated] Oct 2 every gene it has - its type's
        // default genes and any bonus genes of its own. [stated] Oct 2: it scrolls - a mob with many genes is longer
        // than the panel.
        geneticLines(g, card, mouseX, mouseY);
    }

    // ---- Genetic data, scrollable ----

    /** The genetic panel's text area, in screen-relative coordinates. */
    private static final int GENE_TOP = 146;

    private static final int GENE_BOTTOM = 225;

    private static final int GENE_LINE = 9;

    private static final int GENE_VISIBLE = (GENE_BOTTOM - GENE_TOP) / GENE_LINE;

    private static final int GENE_BAR_X = 281;

    /** First visible line, and the specimen it belongs to (a new specimen starts at the top). */
    private int geneScroll;

    private int geneScrollFor = -1;

    private int geneLineCount;

    /**
     * [stated] Oct 3: the hive's lineage id ran off the panel - far too long to show. The row shows a short NICKNAME
     * (#20B149 - the id's first six characters) and CLICKING it copies the full id. Where the row was drawn this frame
     * (screen coords, x0 = -1 when not shown), the full id, and when it was last copied (for the COPIED flash).
     */
    private int hiveX0 = -1;

    private int hiveY0;

    private int hiveX1;

    private int hiveY1;

    private String hiveFullId = "";

    private long hiveCopiedAt;

    /** The short nickname for a lineage id: its first six characters, capitals, after any "lineage/" prefix. */
    private static String hiveNickname(String id) {
        var bare = id.substring(id.lastIndexOf('/') + 1).replace("-", "");
        return bare.isEmpty() ? "--" : "#" + bare.substring(0, Math.min(6, bare.length())).toUpperCase(java.util.Locale.ROOT);
    }

    private boolean draggingGeneBar;

    private record GeneLine(
        Component label,
        String value,
        int labelColour,
        int valueColour,
        int indent
    ) {}

    private void geneticLines(GuiGraphics g, CompoundTag card, int mouseX, int mouseY) {
        if (geneScrollFor != selectedSpecimen) {
            geneScrollFor = selectedSpecimen;
            geneScroll = 0;
        }

        var lines = new ArrayList<GeneLine>();
        var hiveIndex = -1;
        hiveX0 = -1;

        if (card.contains("strain")) {
            lines.add(new GeneLine(Component.translatable("gui.avp_human.cryo.label.strain"), card.getString("strain"), GREEN, WHITE, 0));
            lines.add(new GeneLine(Component.translatable("gui.avp_human.cryo.label.stage"), card.getString("stage"), GREEN, WHITE, 0));
            hiveFullId = card.getString("hive");
            var copied = System.currentTimeMillis() - hiveCopiedAt < 1500;
            // The hive's own name (NO_1_2) when the card has it; specimens frozen before names were stored fall back to
            // a short nickname of the id. A hiveless specimen ("NONE") shows as such and has nothing to copy.
            var name = card.getString("hiveName");
            var label = !name.isEmpty() ? name : "NONE".equals(hiveFullId) ? "NONE" : hiveNickname(hiveFullId);

            if ("NONE".equals(hiveFullId)) {
                hiveFullId = "";
            }

            var shown = copied ? Component.translatable("gui.avp_human.cryo.copied").getString() : label;
            hiveIndex = lines.size();
            lines.add(new GeneLine(Component.translatable("gui.avp_human.cryo.label.hive"), shown, GREEN, copied ? GOOD : WHITE, 0));
        }

        var defaults = card.getList("defaultGenes", Tag.TAG_COMPOUND);
        var bonus = card.getList("bonusGenes", Tag.TAG_COMPOUND);
        geneLines(lines, defaults, "gui.avp_human.cryo.label.default_genes");
        geneLines(lines, bonus, "gui.avp_human.cryo.label.bonus_genes");

        if (defaults.isEmpty() && bonus.isEmpty()) {
            lines.add(new GeneLine(Component.translatable("gui.avp_human.cryo.no_genes"), "", MUTED, MUTED, 0));
        }

        geneLineCount = lines.size();
        var maxScroll = Math.max(0, geneLineCount - GENE_VISIBLE);
        geneScroll = Math.max(0, Math.min(geneScroll, maxScroll));
        var scrolls = maxScroll > 0;
        var right = scrolls ? GENE_BAR_X - 3 : 284;

        for (int row = 0; row < GENE_VISIBLE && geneScroll + row < lines.size(); row++) {
            var line = lines.get(geneScroll + row);
            var y = GENE_TOP + row * GENE_LINE;

            // The hive row is clickable: remember where it is, and light the nickname under the mouse.
            if (geneScroll + row == hiveIndex && !hiveFullId.isEmpty()) {
                hiveX0 = leftPos + 120;
                hiveY0 = topPos + y - 1;
                hiveX1 = leftPos + right;
                hiveY1 = topPos + y + GENE_LINE - 1;

                if (mouseX >= hiveX0 && mouseX < hiveX1 && mouseY >= hiveY0 && mouseY < hiveY1 && line.valueColour() == WHITE) {
                    line = new GeneLine(line.label(), line.value(), line.labelColour(), GOOD, line.indent());
                }
            }
            var valueWidth = font.width(line.value());
            var room = right - (120 + line.indent()) - valueWidth - 4;
            text(g, font.plainSubstrByWidth(line.label().getString(), Math.max(10, room)), 120 + line.indent(), y, line.labelColour());

            if (!line.value().isEmpty()) {
                textRight(g, line.value(), right, y, line.valueColour());
            }
        }

        if (scrolls) {
            // Track, then a thumb sized to the visible share and placed by the scroll position.
            g.fill(leftPos + GENE_BAR_X, topPos + GENE_TOP - 1, leftPos + GENE_BAR_X + 3, topPos + GENE_BOTTOM, WELL);
            var track = GENE_BOTTOM - GENE_TOP + 1;
            var thumb = Math.max(8, track * GENE_VISIBLE / geneLineCount);
            var thumbY = GENE_TOP - 1 + (track - thumb) * geneScroll / maxScroll;
            var hot = draggingGeneBar || overGeneBar(mouseX, mouseY);
            g.fill(leftPos + GENE_BAR_X, topPos + thumbY, leftPos + GENE_BAR_X + 3, topPos + thumbY + thumb, hot ? WHITE : MUTED);
        }
    }

    private void geneLines(List<GeneLine> lines, ListTag list, String headingKey) {
        if (list.isEmpty()) {
            return;
        }

        lines.add(new GeneLine(Component.translatable(headingKey), "", GREEN, GREEN, 0));

        for (int i = 0; i < list.size(); i++) {
            var gene = list.getCompound(i);
            var id = ResourceLocation.tryParse(gene.getString("id"));
            var known = id == null ? null : GeneRegistry.getValueOrNull(id);
            Component name = known != null ? Component.translatable(known.getTranslationKey()) : Component.literal(gene.getString("id"));
            var multiplicative = "MULTIPLICATIVE".equals(gene.getString("op"));
            var value = gene.getDouble("v");
            var shown = (value > 0 ? "+" : "") + String.format("%.2f", multiplicative ? value * 100 : value) + (multiplicative ? "%" : "");
            lines.add(new GeneLine(name, shown, WHITE, value >= 0 ? GOOD : WARN, 6));
        }
    }

    private boolean overGenePanel(double mouseX, double mouseY) {
        return tab == 2
            && mouseX >= leftPos + 114 && mouseX < leftPos + 288
            && mouseY >= topPos + 132 && mouseY < topPos + 228;
    }

    private boolean overGeneBar(double mouseX, double mouseY) {
        return tab == 2
            && geneLineCount > GENE_VISIBLE
            && mouseX >= leftPos + GENE_BAR_X - 1 && mouseX < leftPos + GENE_BAR_X + 4
            && mouseY >= topPos + GENE_TOP - 1 && mouseY < topPos + GENE_BOTTOM;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overGenePanel(mouseX, mouseY) && geneLineCount > GENE_VISIBLE) {
            setGeneScroll(geneScroll - (int) Math.signum(scrollY));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingGeneBar) {
            scrollGeneBarTo(mouseY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingGeneBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void scrollGeneBarTo(double mouseY) {
        var maxScroll = Math.max(0, geneLineCount - GENE_VISIBLE);
        var fraction = (mouseY - (topPos + GENE_TOP)) / (double) (GENE_BOTTOM - GENE_TOP);
        setGeneScroll((int) Math.round(Math.max(0.0, Math.min(1.0, fraction)) * maxScroll));
    }

    /**
     * Moves the gene list, with a scroll blip when it actually moves. [stated] Oct 2: the same blip his avp_alien
     * tracking PDA plays when its list scrolls - the terminal mouseover tick, quieter and higher (0.3 volume, 1.4
     * pitch) - so a list already at its end makes no sound.
     */
    private void setGeneScroll(int target) {
        var clamped = Math.max(0, Math.min(target, Math.max(0, geneLineCount - GENE_VISIBLE)));

        if (clamped != geneScroll) {
            geneScroll = clamped;

            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(HumanSoundEvents.TERMINAL_MOUSEOVER.get(), 1.4F, 0.3F));
            }
        }
    }

    /** [stated] Oct 2: the icon is the mob's own model, shrunk in code to fit its frame - see {@link IconFit}. */
    private void entity(GuiGraphics g, LivingEntity mob, int x0, int y0, int x1, int y1, int mouseX, int mouseY) {
        com.human.client.render.cryo.IconFit.draw(
            g,
            mob,
            leftPos + x0 + 1,
            topPos + y0 + 1,
            leftPos + x1 - 1,
            topPos + y1 - 1,
            mouseX,
            mouseY
        );
    }

    // ------------------------------------------------------------------------------------------------------------
    // DIAGNOSTICS
    // ------------------------------------------------------------------------------------------------------------

    private record Alert(
        String key,
        int colour,
        String icon,
        Component detail
    ) {}

    private List<Alert> alerts(CompoundTag snap) {
        var out = new ArrayList<Alert>();
        var fuel = snap.getInt("fuel");

        if (fuel <= 0 && !occupied(snap).isEmpty()) {
            out.add(new Alert("fuel_empty", WARN, "!", Component.translatable("gui.avp_human.cryo.alert.fuel_empty.detail")));
        } else if (fuel < CryotubeControllerBlockEntity.CAPACITY / 10) {
            out.add(new Alert("fuel_low", WARN, "!", Component.translatable("gui.avp_human.cryo.alert.fuel_low.detail")));
        }

        if (snap.getInt("status") == CryotubeControllerBlockEntity.STATUS_ON_BATTERY) {
            out.add(new Alert("on_battery", WARN, "!", Component.translatable("gui.avp_human.cryo.alert.on_battery.detail")));
        }

        if (snap.getBoolean("gridPartly")) {
            out.add(new Alert("grid_partly", WARN, "i", Component.translatable("gui.avp_human.cryo.alert.grid_partly.detail")));
        }

        for (var tube : tubes(snap)) {
            var state = tube.getInt("state");

            if (state == CryotubeControllerBlockEntity.TUBE_THAWING || state == CryotubeControllerBlockEntity.TUBE_DEACTIVATED) {
                out.add(
                    new Alert(
                        "tube_thawing",
                        WARN,
                        "!",
                        Component.translatable("gui.avp_human.cryo.alert.tube_thawing.detail", tubeLabel(tube), tube.getInt("thaw"))
                    )
                );
            }
        }

        if (tubes(snap).size() >= snap.getInt("tubeCap") && snap.getInt("tubeCap") > 0) {
            out.add(new Alert("at_capacity", MUTED, "i", Component.translatable("gui.avp_human.cryo.alert.at_capacity.detail")));
        }

        var events = snap.getList("events", Tag.TAG_COMPOUND);

        for (int i = 0; i < events.size(); i++) {
            var event = events.getCompound(i);

            if (event.getString("k").startsWith("broken")) {
                out.add(new Alert("tube_broken", WARN, "!", eventText(event)));
            }
        }

        if (out.isEmpty()) {
            out.add(new Alert("nominal", GOOD, "O", Component.translatable("gui.avp_human.cryo.alert.nominal.detail")));
        }

        return out;
    }

    private void diagnostics(GuiGraphics g, CompoundTag snap, int mouseX, int mouseY) {
        var alerts = alerts(snap);
        selectedAlert = Math.min(selectedAlert, alerts.size() - 1);

        panel(g, 8, 48, 160, 128, "gui.avp_human.cryo.panel.alerts");

        for (int i = 0; i < Math.min(4, alerts.size()); i++) {
            var alert = alerts.get(i);
            var y = 62 + i * 16;
            var on = i == selectedAlert;
            target("alert:" + i, 12, y, 156, y + 13);
            frame(g, 12, y, 156, y + 13, on ? BUTTON : ("alert:" + i).equals(hovered) ? WELL : PANEL, GREEN);
            var ink = on ? DARK : alert.colour();
            frame(g, 14, y + 2, 23, y + 11, on ? BUTTON : PANEL, ink);
            text(g, alert.icon(), 17, y + 3, ink);
            text(
                g,
                font.plainSubstrByWidth(Component.translatable("gui.avp_human.cryo.alert." + alert.key()).getString(), 128),
                27,
                y + 3,
                on ? DARK : GREEN
            );
        }

        panel(g, 164, 48, 288, 128, "gui.avp_human.cryo.panel.details");
        var lines = font.split(alerts.get(Math.max(0, selectedAlert)).detail(), 118);

        for (int i = 0; i < Math.min(5, lines.size()); i++) {
            g.drawString(font, lines.get(i), leftPos + 168, topPos + 62 + i * 9, WHITE, false);
        }

        row(
            g,
            "gui.avp_human.cryo.label.grid",
            Component.translatable(
                "gui.avp_human.cryo.grid." + (snap.getBoolean("gridSupplied")
                    ? "supplied"
                    : snap.getBoolean("gridPartly") ? "partly" : "none")
            ).getString(),
            168,
            284,
            108,
            snap.getBoolean("gridSupplied") ? GOOD : WARN
        );
        row(g, "gui.avp_human.cryo.label.temperature", "-196 C", 168, 284, 117, WHITE);

        panel(g, 8, 132, 288, 228, "gui.avp_human.cryo.panel.inventory");
        text(g, Component.translatable("gui.avp_human.cryo.label.batteries"), 14, 148, GREEN);
        text(g, Component.translatable("gui.avp_human.cryo.label.hub"), 14, 182, GREEN);

        for (int slot = 0; slot < CryotubeControllerBlockEntity.BATTERY_SLOTS; slot++) {
            frame(
                g,
                CryotubeControllerMenu.BATTERY_X + slot * 19 - 1,
                CryotubeControllerMenu.BATTERY_Y - 1,
                CryotubeControllerMenu.BATTERY_X + slot * 19 + 17,
                CryotubeControllerMenu.BATTERY_Y + 17,
                WELL,
                GREEN
            );
        }

        frame(
            g,
            CryotubeControllerMenu.HUB_X - 1,
            CryotubeControllerMenu.HUB_Y - 1,
            CryotubeControllerMenu.HUB_X + 17,
            CryotubeControllerMenu.HUB_Y + 17,
            WELL,
            GREEN
        );

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                var x = CryotubeControllerMenu.INVENTORY_X + column * 18;
                var y = CryotubeControllerMenu.INVENTORY_Y + row * 18;
                frame(g, x - 1, y - 1, x + 17, y + 17, WELL, GREEN);
            }
        }

        for (int column = 0; column < 9; column++) {
            var x = CryotubeControllerMenu.INVENTORY_X + column * 18;
            frame(g, x - 1, CryotubeControllerMenu.HOTBAR_Y - 1, x + 17, CryotubeControllerMenu.HOTBAR_Y + 17, WELL, GREEN);
        }

        text(g, Component.translatable("gui.avp_human.cryo.label.inventory"), 72, 148, MUTED);
        text(g, Component.translatable("gui.avp_human.cryo.label.hotbar"), 72, 208, MUTED);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Names
    // ------------------------------------------------------------------------------------------------------------

    private static String statusKey(int status) {
        return switch (status) {
            case CryotubeControllerBlockEntity.STATUS_RUNNING -> "running";
            case CryotubeControllerBlockEntity.STATUS_PAUSED -> "paused";
            case CryotubeControllerBlockEntity.STATUS_THAWING -> "thawing";
            case CryotubeControllerBlockEntity.STATUS_ON_BATTERY -> "battery";
            default -> "idle";
        };
    }

    private static String tubeStateKey(int state) {
        return switch (state) {
            case CryotubeControllerBlockEntity.TUBE_ACTIVE -> "active";
            case CryotubeControllerBlockEntity.TUBE_THAWING -> "thawing";
            case CryotubeControllerBlockEntity.TUBE_DEACTIVATED -> "deactivated";
            case CryotubeControllerBlockEntity.TUBE_PAUSED -> "paused";
            case CryotubeControllerBlockEntity.TUBE_UNLOADED -> "unloaded";
            default -> "standby";
        };
    }

    private static int tubeStateColour(int state) {
        return switch (state) {
            case CryotubeControllerBlockEntity.TUBE_ACTIVE, CryotubeControllerBlockEntity.TUBE_STANDBY -> GOOD;
            case CryotubeControllerBlockEntity.TUBE_THAWING, CryotubeControllerBlockEntity.TUBE_DEACTIVATED -> WARN;
            default -> MUTED;
        };
    }
}
