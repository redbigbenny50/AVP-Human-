package com.human.client.screen;

import com.human.Human;
import com.human.HumanResources;
import com.human.client.network.HumanClientListener;
import com.human.client.screen.sentry.SentryEntityList;
import com.human.client.screen.sentry.SentryFavorites;
import com.human.client.screen.widget.OptionSideControls;
import com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter;
import com.human.common.gameplay.entity.machine.SentryLoadout;
import com.human.common.gameplay.entity.machine.SentryTurret;
import com.human.common.gameplay.machine.MachineUpgrades;
import com.human.common.gameplay.menu.sentry.SentryTurretMenu;
import com.human.common.network.packet.C2SMarineSentryFilterPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The sentry turret screen. [stated] Oct 2: cards down the left; skin, network card, sensor lens and battery down the
 * right; the turret's model and power in the middle on a MUTHUR display; nine ammo slots along the bottom. Side
 * buttons: Targeting (the marine sentry's targeting page) and Redstone.
 * <p>
 * The Targeting page is a page of THIS screen, as it is on the marine: switching to a separate screen would close the
 * container, and the server applies the page's filter to the container the player has open.
 * </p>
 */
public class SentryTurretScreen extends AbstractContainerScreen<SentryTurretMenu> {

    // ------------------------------------------------------------------------------------------------------------
    // Main view
    // ------------------------------------------------------------------------------------------------------------

    private static final int BG = 0xFFC6C6C6;

    private static final int LIGHT = 0xFFFFFFFF;

    private static final int SHADOW = 0xFF555555;

    private static final int OUTLINE = 0xFF000000;

    private static final int WELL_DARK = 0xFF373737;

    private static final int WELL_FILL = 0xFF8B8B8B;

    /** The MUTHUR display in the middle - the Cryolink palette. */
    private static final int SCREEN_BG = 0xFF0B1A12;

    private static final int SCREEN_MINT = 0xFF6EF68D;

    private static final int SCREEN_DIM = 0xFF2E6B3E;

    private static final int DISPLAY_X = 28;

    private static final int DISPLAY_Y = 16;

    private static final int DISPLAY_W = 120;

    private static final int DISPLAY_H = 72;

    private static final int POWER_X = DISPLAY_X + DISPLAY_W - 20;

    private static final int HEAT_X = DISPLAY_X + DISPLAY_W - 10;

    private static final int BAR_Y = DISPLAY_Y + 4;

    private static final int BAR_H = DISPLAY_H - 8;

    private static final int BAR_W = 6;

    private static final int MAIN_WIDTH = 176;

    // ------------------------------------------------------------------------------------------------------------
    // Targeting page - the marine's, unchanged
    // ------------------------------------------------------------------------------------------------------------

    private static final ResourceLocation SENTRY_TEXTURE = HumanResources.location("textures/gui/container/marine_sentry.png");

    private static final int PAGE_WIDTH = 310;

    private static final int PAGE_HEIGHT = 228;

    private static final int PAGE_MARGIN_X = 8;

    private static final int LIST_X = 158;

    private static final int LIST_Y = 38;

    private static final int LIST_WIDTH = 144;

    private static final int LIST_HEIGHT = 148;

    private static final int FAVORITE_SIZE = 14;

    private static final int FAVORITE_GAP = 2;

    private static final int FAVORITES_Y = 192;

    private static final int FIRST_ROW_Y = 20;

    private static final int ROW_GAP = 2;

    private static final int CHECKBOX_HEIGHT = 16;

    private static final int MODE_BUTTON_WIDTH = 96;

    private static final int FOOTER_BUTTON_WIDTH = 70;

    private static final int FOOTER_Y = 192;

    private static final int BUTTON_HEIGHT = 16;

    private static final int BUTTON_GAP = 4;

    private static final Component WHITELIST_LABEL = Component.translatable("gui.avp.marine.sentry.whitelist");

    private static final Component BLACKLIST_LABEL = Component.translatable("gui.avp.marine.sentry.blacklist");

    private static final Component WHITELIST_TOOLTIP = Component.translatable("gui.avp.marine.sentry.whitelist.tooltip");

    private static final Component BLACKLIST_TOOLTIP = Component.translatable("gui.avp.marine.sentry.blacklist.tooltip");

    private static final Component APPLY_LABEL = Component.translatable("gui.avp.marine.sentry.apply");

    private static final Component CANCEL_LABEL = Component.translatable("gui.avp.marine.sentry.cancel");

    private static final Component PAGE_TITLE = Component.translatable("gui.avp.marine.sentry.title");

    private static final Component SEARCH_HINT = Component.translatable("gui.avp.marine.sentry.search");

    private static final Component NAMED_MOBS_LABEL = Component.translatable("gui.avp.marine.sentry.named");

    private static final Component FAVORITES_LABEL = Component.translatable("gui.avp.marine.sentry.favorites");

    private static final Component FAVORITE_EMPTY_TOOLTIP = Component.translatable("gui.avp.marine.sentry.favorite.empty");

    private static final Component FAVORITE_OVERWRITE_HINT = Component.translatable("gui.avp.marine.sentry.favorite.overwrite");

    private boolean showingPage;

    /** What the turret has - as sent when the screen opened, then as last applied. The page always opens on this. */
    private SentryTargetFilter appliedFilter = new SentryTargetFilter();

    /** The filter being edited - a copy, so leaving the page with Cancel changes nothing. */
    private SentryTargetFilter editingFilter = new SentryTargetFilter();

    private @Nullable SentryEntityList entityList;

    private @Nullable OptionSideControls controls;

    public SentryTurretScreen(SentryTurretMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = MAIN_WIDTH;
        this.imageHeight = SentryTurretMenu.IMAGE_HEIGHT;
        this.inventoryLabelY = SentryTurretMenu.PLAYER_Y - 10;
    }

    @Override
    protected void init() {
        // Set before super.init(), which works out leftPos and topPos from them.
        imageWidth = showingPage ? PAGE_WIDTH : MAIN_WIDTH;
        imageHeight = showingPage ? PAGE_HEIGHT : SentryTurretMenu.IMAGE_HEIGHT;
        menu.setSlotsHidden(showingPage);

        super.init();

        // The turret's filter, sent as the screen opened.
        var pushed = HumanClientListener.takePendingSentryFilter();

        if (pushed != null) {
            appliedFilter = pushed;
            editingFilter = pushed.copy();
        }

        if (showingPage) {
            controls = null;
            initPage();
            return;
        }

        controls = new OptionSideControls(
            menu.containerId,
            font,
            List.of(
                OptionSideControls.Field.action(
                    Component.translatable("gui.avp_human.sentry.side.targeting"),
                    () -> true,
                    (graphics, x, y) -> graphics.renderItem(new ItemStack(Items.TARGET), x, y),
                    () -> setPage(true)
                ),
                // [stated] Oct 2: look through the camera - with the camera lens and network card fitted.
                OptionSideControls.Field.action(
                    Component.translatable("gui.avp_human.sentry.side.view"),
                    this::canView,
                    (graphics, x, y) -> graphics.renderItem(
                        new ItemStack(com.human.common.registry.init.item.HumanItems.CAMERA_SENSOR.get()),
                        x,
                        y
                    ),
                    this::view
                ),
                new OptionSideControls.Field(
                    Component.translatable("gui.avp_human.machine.side.redstone"),
                    () -> true,
                    (graphics, x, y) -> graphics.renderItem(redstoneIcon(menu.getRedstoneMode()), x, y),
                    this::redstoneOptions
                )
            )
        );
    }

    private boolean canView() {
        return menu.slots.get(SentryLoadout.SENSOR).getItem().is(com.human.common.registry.init.item.HumanItems.CAMERA_SENSOR.get())
            && !menu.slots.get(SentryLoadout.NETWORK).getItem().isEmpty();
    }

    private void view() {
        if (minecraft != null && minecraft.level != null && minecraft.level.getEntity(menu.getTurretId()) instanceof SentryTurret turret) {
            Human.MOD.networking().sendToServer(new com.human.common.network.packet.C2SSentryViewPayload(turret.getUUID()));
        }
    }

    private static ItemStack redstoneIcon(MachineUpgrades.RedstoneMode mode) {
        return new ItemStack(
            switch (mode) {
                case IGNORE -> Items.GUNPOWDER;
                case HIGH -> Items.REDSTONE_TORCH;
                case LOW -> Items.LEVER;
            }
        );
    }

    private List<OptionSideControls.Option> redstoneOptions() {
        var options = new ArrayList<OptionSideControls.Option>();

        for (var mode : MachineUpgrades.RedstoneMode.values()) {
            options.add(
                new OptionSideControls.Option(
                    Component.translatable("gui.avp_human.machine.redstone." + mode.name().toLowerCase(Locale.ROOT)),
                    menu.getRedstoneMode() == mode,
                    SentryTurretMenu.BUTTON_SET_REDSTONE + mode.ordinal()
                )
            );
        }

        return options;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Targeting page
    // ------------------------------------------------------------------------------------------------------------

    private void initPage() {
        var y = topPos + FIRST_ROW_Y;

        addRenderableWidget(
            Button.builder(
                editingFilter.isWhitelist() ? WHITELIST_LABEL : BLACKLIST_LABEL,
                button -> {
                    editingFilter.setWhitelist(!editingFilter.isWhitelist());
                    rebuildWidgets();
                }
            )
                .bounds(leftPos + PAGE_MARGIN_X, y, MODE_BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(editingFilter.isWhitelist() ? WHITELIST_TOOLTIP : BLACKLIST_TOOLTIP))
                .build()
        );

        var checkboxY = y + BUTTON_HEIGHT + ROW_GAP * 2;

        for (var category : SentryTargetFilter.selectableCategories()) {
            addRenderableWidget(
                Checkbox.builder(Component.translatable(category.getTranslationKey()), font)
                    .pos(leftPos + PAGE_MARGIN_X, checkboxY)
                    .selected(editingFilter.hasCategory(category))
                    .onValueChange((checkbox, selected) -> editingFilter.setCategory(category, selected))
                    .build()
            );

            checkboxY += CHECKBOX_HEIGHT + ROW_GAP;
        }

        var searchBox = new EditBox(font, leftPos + LIST_X, topPos + FIRST_ROW_Y, LIST_WIDTH, BUTTON_HEIGHT, SEARCH_HINT);
        searchBox.setHint(SEARCH_HINT);
        searchBox.setResponder(text -> {
            if (entityList != null) {
                entityList.applySearch(text);
            }
        });
        addRenderableWidget(searchBox);

        entityList = new SentryEntityList(leftPos + LIST_X, topPos + LIST_Y, LIST_WIDTH, LIST_HEIGHT);

        for (var slot : SentryFavorites.slots()) {
            var saved = SentryFavorites.get(slot);

            addRenderableWidget(
                Button.builder(
                    Component.literal(String.valueOf(slot + 1)),
                    pressed -> {
                        // Click loads a favourite; shift-click saves what is on screen into it.
                        if (hasShiftDown()) {
                            SentryFavorites.set(slot, editingFilter);
                        } else {
                            var favorite = SentryFavorites.get(slot);

                            if (favorite == null) {
                                return;
                            }

                            editingFilter = favorite.copy();
                        }

                        rebuildWidgets();
                    }
                )
                    .bounds(
                        leftPos + PAGE_MARGIN_X + slot * (FAVORITE_SIZE + FAVORITE_GAP),
                        topPos + FAVORITES_Y,
                        FAVORITE_SIZE,
                        FAVORITE_SIZE
                    )
                    .tooltip(
                        Tooltip.create(
                            saved == null
                                ? FAVORITE_EMPTY_TOOLTIP
                                : Component.literal(SentryFavorites.describe(saved)).append(FAVORITE_OVERWRITE_HINT)
                        )
                    )
                    .build()
            );
        }

        addRenderableWidget(
            Button.builder(APPLY_LABEL, button -> {
                Human.MOD.networking().sendToServer(new C2SMarineSentryFilterPayload(editingFilter.copy()));
                appliedFilter = editingFilter.copy();
                setPage(false);
            })
                .bounds(leftPos + LIST_X, topPos + FOOTER_Y, FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
        );

        addRenderableWidget(
            Button.builder(CANCEL_LABEL, button -> setPage(false))
                .bounds(leftPos + LIST_X + FOOTER_BUTTON_WIDTH + BUTTON_GAP, topPos + FOOTER_Y, FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
        );
    }

    private void setPage(boolean showing) {
        showingPage = showing;

        if (showing) {
            editingFilter = appliedFilter.copy();
        } else {
            entityList = null;
        }

        rebuildWidgets();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (controls != null && controls.mouseClicked(mouseX, mouseY)) {
            return true;
        }

        // The mob list is drawn and clicked by hand, so the page routes clicks to it.
        if (
            showingPage && entityList != null && entityList.isOver(mouseX, mouseY) && entityList.mouseClicked(mouseX, mouseY, editingFilter)
        ) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (showingPage && entityList != null && entityList.isOver(mouseX, mouseY)) {
            entityList.scroll(scrollY);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (controls != null && controls.keyPressed(keyCode)) {
            return true;
        }

        // Esc on the page goes back to the turret rather than closing it - Cancel, in effect.
        if (showingPage && keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            setPage(false);
            return true;
        }

        // Typing in the search box must not trigger the inventory key and close the screen.
        if (showingPage && getFocused() instanceof EditBox box && box.isFocused()) {
            return box.keyPressed(keyCode, scanCode, modifiers) || box.canConsumeInput() || super.keyPressed(keyCode, scanCode, modifiers);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        return (controls == null || !controls.isMouseOver(mouseX, mouseY)) && super.hasClickedOutside(
            mouseX,
            mouseY,
            guiLeft,
            guiTop,
            button
        );
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (controls != null) {
            controls.layout(leftPos, topPos);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        if (showingPage) {
            if (entityList != null) {
                entityList.render(graphics, font, mouseX, mouseY, editingFilter);
            }

            renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        if (controls != null) {
            controls.render(graphics, mouseX, mouseY);

            if (controls.isOpen() || controls.isMouseOver(mouseX, mouseY)) {
                controls.renderTooltip(graphics, mouseX, mouseY);
                return;
            }
        }

        renderTooltip(graphics, mouseX, mouseY);

        if (hovering(mouseX, mouseY, POWER_X - 1, BAR_Y - 1, BAR_W + 2, BAR_H + 2)) {
            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                List.of(
                    Component.translatable("gui.avp_human.sentry.charge"),
                    Component.translatable("gui.avp_human.sentry.charge.amount", format(menu.getEnergy()), format(SentryTurret.CAPACITY))
                        .withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.avp_human.sentry.charge.cost", SentryTurret.SHOT_COST).withStyle(ChatFormatting.DARK_GRAY)
                ),
                mouseX,
                mouseY
            );
        } else if (hovering(mouseX, mouseY, HEAT_X - 1, BAR_Y - 1, BAR_W + 2, BAR_H + 2)) {
            com.human.client.screen.widget.GuiText.tooltip(
                graphics,
                font,
                Component.translatable("gui.avp_human.sentry.heat"),
                mouseX,
                mouseY
            );
        } else if (hoveredSlot != null && !hoveredSlot.hasItem()) {
            var key = slotKey(SentryTurretMenu.loadoutIndex(hoveredSlot), hoveredSlot.index);

            if (key != null) {
                com.human.client.screen.widget.GuiText.tooltip(graphics, font, Component.translatable(key), mouseX, mouseY);
            }
        }
    }

    /** What an empty slot takes, as a translation key. */
    private static @Nullable String slotKey(int loadoutIndex, int menuIndex) {
        if (menuIndex == SentryTurretMenu.BATTERY_SLOT) {
            return "gui.avp_human.sentry.slot.battery";
        }

        if (loadoutIndex >= 0 && SentryLoadout.isAmmoSlot(loadoutIndex)) {
            return "gui.avp_human.sentry.slot.ammo";
        }

        if (SentryLoadout.isCardSlot(loadoutIndex)) {
            return "gui.avp_human.sentry.slot.card";
        }

        return switch (loadoutIndex) {
            case SentryLoadout.SKIN -> "gui.avp_human.sentry.slot.skin";
            case SentryLoadout.NETWORK -> "gui.avp_human.sentry.slot.network";
            case SentryLoadout.SENSOR -> "gui.avp_human.sentry.slot.sensor";
            default -> null;
        };
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (showingPage) {
            graphics.blit(SENTRY_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 512, 256);
            return;
        }

        var x = leftPos;
        var y = topPos;

        // A vanilla-style panel.
        graphics.fill(x, y, x + imageWidth, y + imageHeight, OUTLINE);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, BG);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 3, LIGHT);
        graphics.fill(x + 1, y + 1, x + 3, y + imageHeight - 1, LIGHT);
        graphics.fill(x + 3, y + imageHeight - 3, x + imageWidth - 1, y + imageHeight - 1, SHADOW);
        graphics.fill(x + imageWidth - 3, y + 3, x + imageWidth - 1, y + imageHeight - 1, SHADOW);

        for (var slot : menu.slots) {
            if (slot.isActive()) {
                well(graphics, x + slot.x - 1, y + slot.y - 1);
            }
        }

        // The MUTHUR display in the middle.
        var dx = x + DISPLAY_X;
        var dy = y + DISPLAY_Y;
        graphics.fill(dx - 1, dy - 1, dx + DISPLAY_W + 1, dy + DISPLAY_H + 1, SCREEN_DIM);
        graphics.fill(dx, dy, dx + DISPLAY_W, dy + DISPLAY_H, SCREEN_BG);

        // The turret itself, in its current skin, turning to follow the mouse.
        if (minecraft != null && minecraft.level != null && minecraft.level.getEntity(menu.getTurretId()) instanceof SentryTurret turret) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                dx + 2,
                dy + 2,
                POWER_X + x - 4,
                dy + DISPLAY_H - 12,
                24,
                0.0625F,
                mouseX,
                mouseY,
                turret
            );
        }

        bar(graphics, x + POWER_X, y + BAR_Y, menu.getEnergy() / (float) SentryTurret.CAPACITY, SCREEN_MINT);
        bar(graphics, x + HEAT_X, y + BAR_Y, menu.getHeat(), menu.isOverheated() ? 0xFFD03030 : 0xFFE08A2B);
    }

    private static void bar(GuiGraphics graphics, int x, int y, float fraction, int colour) {
        graphics.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, SCREEN_DIM);
        graphics.fill(x, y, x + BAR_W, y + BAR_H, 0xFF050C08);

        var height = Math.round(BAR_H * Math.max(0.0F, Math.min(1.0F, fraction)));

        if (height > 0) {
            graphics.fill(x, y + BAR_H - height, x + BAR_W, y + BAR_H, colour);
        }
    }

    private static void well(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, LIGHT);
        graphics.fill(x, y, x + 17, y + 17, WELL_DARK);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, WELL_FILL);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        if (showingPage) {
            graphics.drawString(font, PAGE_TITLE, titleLabelX, titleLabelY, 0x404040, false);
            graphics.drawString(font, NAMED_MOBS_LABEL, LIST_X, FIRST_ROW_Y - 12, 0x404040, false);
            graphics.drawString(font, FAVORITES_LABEL, PAGE_MARGIN_X, FAVORITES_Y - 11, 0x404040, false);
            return;
        }

        super.renderLabels(graphics, mouseX, mouseY);

        // Status along the bottom of the display, in the display's mint.
        var status = menu.getStatus();
        Component line;
        var colour = SCREEN_MINT;

        if (status == 0) {
            line = Component.translatable("gui.avp_human.sentry.ready");
        } else if ((status & SentryTurret.STATUS_NO_POWER) != 0) {
            line = Component.translatable("gui.avp_human.sentry.no_power");
            colour = 0xFFE05050;
        } else if ((status & SentryTurret.STATUS_NO_BULLETS) != 0) {
            line = Component.translatable("gui.avp_human.sentry.no_bullets");
            colour = 0xFFE05050;
        } else {
            // ⚠ Oct 23 - its own shorter line: the item tooltip's "Overheated - cooling down" is 135 px in this 114 px
            // display.
            line = Component.translatable("gui.avp_human.sentry.overheated");
            colour = 0xFFE08A2B;
        }

        graphics.drawString(font, line, DISPLAY_X + 3, DISPLAY_Y + DISPLAY_H - 10, colour, false);

        if (menu.isBound()) {
            graphics.drawString(
                font,
                Component.translatable("gui.avp_human.sentry.wired"),
                DISPLAY_X + 3,
                DISPLAY_Y + 3,
                SCREEN_DIM,
                false
            );
        }
    }

    private boolean hovering(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
