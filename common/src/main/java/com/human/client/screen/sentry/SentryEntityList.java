package com.human.client.screen.sentry;

import com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The scrollable, searchable list of individual entity types.
 * <p>
 * Categories cover the common cases; this is for the exceptions - the one mob you want spared, or the one you want shot
 * regardless. Ticking an entry adds it to the filter's named set, which reads as an addition under a whitelist and as
 * an exemption under a blacklist.
 * <p>
 * ⚠ A search box is not optional here. Vanilla alone is around 150 entity types and a large modpack is several hundred;
 * scrolling that unaided to find one mob is not a feature anyone would use twice.
 */
public class SentryEntityList {

    public static final int ROW_HEIGHT = 14;

    private final int x;

    private final int y;

    private final int width;

    private final int height;

    /**
     * Every selectable type, built once. MISC is excluded - it is boats, item frames, arrows and the player entity, so
     * offering it would be a long list of things a marine cannot meaningfully guard against.
     */
    private final List<EntityType<?>> allTypes = new ArrayList<>();

    private final List<EntityType<?>> visibleTypes = new ArrayList<>();

    private String search = "";

    private double scrollRows;

    public SentryEntityList(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        for (var entityType : BuiltInRegistries.ENTITY_TYPE) {
            if (entityType.getCategory() != MobCategory.MISC) {
                allTypes.add(entityType);
            }
        }

        allTypes.sort((left, right) -> displayName(left).compareToIgnoreCase(displayName(right)));

        applySearch("");
    }

    public void applySearch(String search) {
        this.search = search.toLowerCase(Locale.ROOT).trim();

        visibleTypes.clear();

        for (var entityType : allTypes) {
            // Matched against both the display name and the registry id, because a player may know either - "creeper"
            // and "minecraft:creeper" should both find it, and modded mobs are often only known by their id.
            if (
                this.search.isEmpty()
                    || displayName(entityType).toLowerCase(Locale.ROOT).contains(this.search)
                    || idOf(entityType).toString().contains(this.search)
            ) {
                visibleTypes.add(entityType);
            }
        }

        scrollRows = 0.0;
    }

    public int visibleRowCount() {
        return height / ROW_HEIGHT;
    }

    private int maxScrollRows() {
        return Math.max(0, visibleTypes.size() - visibleRowCount());
    }

    public void scroll(double rows) {
        scrollRows = Mth.clamp(scrollRows - rows, 0.0, maxScrollRows());
    }

    public boolean isOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /**
     * @return whether the click landed on a row, having toggled it.
     */
    public boolean mouseClicked(double mouseX, double mouseY, SentryTargetFilter filter) {
        if (!isOver(mouseX, mouseY)) {
            return false;
        }

        var index = (int) scrollRows + (int) ((mouseY - y) / ROW_HEIGHT);

        if (index < 0 || index >= visibleTypes.size()) {
            return false;
        }

        var id = idOf(visibleTypes.get(index));
        var customTypes = new ArrayList<>(filter.getCustomTypes());

        if (!customTypes.remove(id)) {
            customTypes.add(id);
        }

        filter.setCustomTypes(customTypes);

        return true;
    }

    public void render(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY, SentryTargetFilter filter) {
        // Clipped so a row half-scrolled past the top or bottom is cut off by the well rather than spilling onto the
        // panel around it.
        guiGraphics.enableScissor(x, y, x + width, y + height);

        var first = (int) scrollRows;

        for (var row = 0; row < visibleRowCount() + 1; row++) {
            var index = first + row;

            if (index >= visibleTypes.size()) {
                break;
            }

            var entityType = visibleTypes.get(index);
            var rowY = y + row * ROW_HEIGHT;
            var selected = filter.getCustomTypes().contains(idOf(entityType));
            var hovered = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            if (selected) {
                guiGraphics.fill(x, rowY, x + width, rowY + ROW_HEIGHT, 0x804CAF50);
            } else if (hovered) {
                guiGraphics.fill(x, rowY, x + width, rowY + ROW_HEIGHT, 0x40FFFFFF);
            }

            // Trimmed by hand rather than with substrByWidth, whose FormattedText overload drawString will not take.
            guiGraphics.drawString(
                font,
                font.plainSubstrByWidth(displayName(entityType), width - 8),
                x + 3,
                rowY + 3,
                selected ? 0xFFFFFF : 0xE0E0E0,
                false
            );
        }

        guiGraphics.disableScissor();

        renderScrollbar(guiGraphics);
    }

    private void renderScrollbar(GuiGraphics guiGraphics) {
        if (maxScrollRows() <= 0) {
            return;
        }

        var trackX = x + width - 3;
        var knobHeight = Math.max(12, height * visibleRowCount() / Math.max(1, visibleTypes.size()));
        var knobY = y + (int) (scrollRows / maxScrollRows() * (height - knobHeight));

        guiGraphics.fill(trackX, y, trackX + 3, y + height, 0x60000000);
        guiGraphics.fill(trackX, knobY, trackX + 3, knobY + knobHeight, 0xFFC6C6C6);
    }

    public int getSelectedCount(SentryTargetFilter filter) {
        return filter.getCustomTypes().size();
    }

    private static String displayName(EntityType<?> entityType) {
        return entityType.getDescription().getString();
    }

    private static ResourceLocation idOf(EntityType<?> entityType) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
    }
}
