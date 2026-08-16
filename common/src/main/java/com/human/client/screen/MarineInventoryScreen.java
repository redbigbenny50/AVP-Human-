package com.human.client.screen;

import com.human.Human;
import com.human.HumanResources;
import com.human.client.network.HumanClientListener;
import com.human.client.screen.sentry.SentryEntityList;
import com.human.client.screen.sentry.SentryFavorites;
import com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter;
import com.human.common.gameplay.menu.marine.MarineInventoryMenu;
import com.human.common.gameplay.menu.marine.MarineInventorySlot;
import com.human.common.network.packet.C2SMarineSentryFilterPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MarineInventoryScreen extends AbstractContainerScreen<MarineInventoryMenu> {

    private static final ResourceLocation TEXTURE = HumanResources.location("textures/gui/container/marine_inventory.png");

    private static final ResourceLocation SENTRY_TEXTURE = HumanResources.location("textures/gui/container/marine_sentry.png");

    private static final Component FOLLOW_LABEL = Component.translatable("gui.avp.marine.mode.follow");

    private static final Component HOLD_LABEL = Component.translatable("gui.avp.marine.mode.hold");

    private static final Component FOLLOW_TOOLTIP = Component.translatable("gui.avp.marine.mode.follow.tooltip");

    private static final Component HOLD_TOOLTIP = Component.translatable("gui.avp.marine.mode.hold.tooltip");

    private static final Component SENTRY_ON_LABEL = Component.translatable("gui.avp.marine.sentry.on");

    private static final Component SENTRY_OFF_LABEL = Component.translatable("gui.avp.marine.sentry.off");

    private static final Component SENTRY_ON_TOOLTIP = Component.translatable("gui.avp.marine.sentry.on.tooltip");

    private static final Component SENTRY_OFF_TOOLTIP = Component.translatable("gui.avp.marine.sentry.off.tooltip");

    private static final Component CONFIGURE_LABEL = Component.translatable("gui.avp.marine.sentry.configure");

    private static final Component CONFIGURE_TOOLTIP = Component.translatable("gui.avp.marine.sentry.configure.tooltip");

    private static final Component WHITELIST_LABEL = Component.translatable("gui.avp.marine.sentry.whitelist");

    private static final Component BLACKLIST_LABEL = Component.translatable("gui.avp.marine.sentry.blacklist");

    private static final Component WHITELIST_TOOLTIP = Component.translatable("gui.avp.marine.sentry.whitelist.tooltip");

    private static final Component BLACKLIST_TOOLTIP = Component.translatable("gui.avp.marine.sentry.blacklist.tooltip");

    private static final Component APPLY_LABEL = Component.translatable("gui.avp.marine.sentry.apply");

    private static final Component CANCEL_LABEL = Component.translatable("gui.avp.marine.sentry.cancel");

    private static final Component SENTRY_PAGE_TITLE = Component.translatable("gui.avp.marine.sentry.title");

    private static final Component SEARCH_HINT = Component.translatable("gui.avp.marine.sentry.search");

    private static final Component NAMED_MOBS_LABEL = Component.translatable("gui.avp.marine.sentry.named");

    private static final Component FAVORITES_LABEL = Component.translatable("gui.avp.marine.sentry.favorites");

    private static final Component FAVORITE_EMPTY_TOOLTIP = Component.translatable("gui.avp.marine.sentry.favorite.empty");

    private static final Component FAVORITE_OVERWRITE_HINT = Component.translatable("gui.avp.marine.sentry.favorite.overwrite");

    private static final Component END_CONTRACT_LABEL = Component.translatable("gui.avp.marine.end_contract");

    private static final Component END_CONTRACT_TOOLTIP = Component.translatable("gui.avp.marine.end_contract.tooltip");

    private static final Component END_CONTRACT_BLOCKED_TOOLTIP = Component.translatable(
        "gui.avp.marine.end_contract.blocked.tooltip"
    );

    private static final int PANEL_WIDTH = 194;

    private static final int PANEL_HEIGHT = 228;

    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace(
        "container/creative_inventory/scroller"
    );

    private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace(
        "container/creative_inventory/scroller_disabled"
    );

    private static final int SCROLLER_WIDTH = 12;

    private static final int SCROLLER_HEIGHT = 15;

    private static final int SCROLLBAR_X = 174;

    private static final int SCROLLBAR_Y = MarineInventoryMenu.MARINE_SLOTS_Y;

    private static final int SCROLLBAR_HEIGHT = MarineInventoryMenu.VISIBLE_MARINE_ROW_COUNT
        * MarineInventoryMenu.SLOT_SIZE;

    /**
     * Sized so three fit the strip with even gaps: 8 + 3 x 56 + 2 x 4 leaves a matching margin on the right, with room
     * for the sentry button to come.
     */
    private static final int COMMAND_BUTTON_WIDTH = 56;

    private static final int COMMAND_BUTTON_GAP = 4;

    /**
     * Narrower than the three command buttons - it is the fourth on a strip sized for three, and takes what is left.
     */
    private static final int CONFIGURE_BUTTON_WIDTH = 14;

    private static final int SENTRY_MARGIN_X = 8;

    /**
     * The sentry page is its own size. Wider than the pack page because the named-mob list sits beside the categories
     * rather than under them, and 310 is comfortably inside the 320 of guaranteed width.
     */
    private static final int SENTRY_PANEL_WIDTH = 310;

    private static final int SENTRY_PANEL_HEIGHT = 228;

    private static final int SENTRY_LIST_X = 158;

    private static final int SENTRY_LIST_Y = 38;

    private static final int SENTRY_LIST_WIDTH = 144;

    private static final int SENTRY_LIST_HEIGHT = 148;

    private static final int SENTRY_FAVORITE_SIZE = 14;

    private static final int SENTRY_FAVORITE_GAP = 2;

    private static final int SENTRY_FAVORITES_Y = 192;

    private static final int SENTRY_FIRST_ROW_Y = 20;

    private static final int SENTRY_ROW_GAP = 2;

    private static final int SENTRY_CHECKBOX_HEIGHT = 16;

    private static final int SENTRY_MODE_BUTTON_WIDTH = 96;

    private static final int SENTRY_FOOTER_BUTTON_WIDTH = 70;

    /**
     * Apply and Back live under the mob list rather than under the categories - the favourites strip has the bottom of
     * the left column, and stacking the two would have put them on top of each other.
     */
    private static final int SENTRY_FOOTER_Y = 192;

    private static final int COMMAND_BUTTON_HEIGHT = 16;

    private static final int COMMAND_BUTTON_X = 8;

    private static final int COMMAND_BUTTON_Y = 17;

    /**
     * Wash drawn over locked slots. Dark and translucent, so the issued item stays readable underneath while looking
     * plainly unavailable.
     */
    private static final int LOCKED_SLOT_TINT = 0x80101010;

    private @Nullable Button modeButton;

    private @Nullable Button endContractButton;

    private @Nullable Button sentryButton;

    /**
     * Scroll position as a fraction, 0 at the top and 1 at the bottom. Kept as a fraction rather than a row so that
     * dragging the knob tracks the mouse smoothly; the row it resolves to is what actually moves the slots.
     */
    private float scrollFraction;

    private boolean draggingScroller;

    /**
     * Which page the panel is showing. A page of the same screen rather than a separate one, so the container is never
     * closed and reopened and the marine stays held still throughout.
     */
    private boolean showingSentryPage;

    /**
     * The filter being edited. A copy, so abandoning the page leaves the marine's own filter untouched.
     */
    private SentryTargetFilter editingFilter = new SentryTargetFilter();

    private @Nullable SentryEntityList entityList;

    private @Nullable EditBox searchBox;

    public MarineInventoryScreen(MarineInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        this.inventoryLabelY = PANEL_HEIGHT - 95;
    }

    @Override
    protected void init() {
        // Set before super.init(), which is what computes leftPos and topPos from them.
        imageWidth = showingSentryPage ? SENTRY_PANEL_WIDTH : PANEL_WIDTH;
        imageHeight = showingSentryPage ? SENTRY_PANEL_HEIGHT : PANEL_HEIGHT;

        super.init();

        modeButton = addRenderableWidget(
            Button.builder(
                labelForMode(menu.getModeIndex()),
                // Vanilla's container-button packet, the same route the stonecutter and loom use for their controls.
                button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MarineInventoryMenu.BUTTON_TOGGLE_MODE);
                    }
                }
            )
                .bounds(leftPos + COMMAND_BUTTON_X, topPos + COMMAND_BUTTON_Y, COMMAND_BUTTON_WIDTH, COMMAND_BUTTON_HEIGHT)
                .tooltip(Tooltip.create(tooltipForMode(menu.getModeIndex())))
                .build()
        );

        var pushedFilter = HumanClientListener.takePendingSentryFilter();

        if (pushedFilter != null) {
            editingFilter = pushedFilter;
        }

        if (showingSentryPage) {
            initSentryPage();

            return;
        }

        sentryButton = addRenderableWidget(
            Button.builder(
                labelForSentry(menu.getModeIndex()),
                button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MarineInventoryMenu.BUTTON_TOGGLE_SENTRY);
                    }
                }
            )
                .bounds(
                    leftPos + COMMAND_BUTTON_X + COMMAND_BUTTON_WIDTH + COMMAND_BUTTON_GAP,
                    topPos + COMMAND_BUTTON_Y,
                    COMMAND_BUTTON_WIDTH,
                    COMMAND_BUTTON_HEIGHT
                )
                .tooltip(Tooltip.create(tooltipForSentry(menu.getModeIndex())))
                .build()
        );

        endContractButton = addRenderableWidget(
            Button.builder(
                END_CONTRACT_LABEL,
                button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MarineInventoryMenu.BUTTON_END_CONTRACT);
                        // Closed from here rather than server-side: Player.closeContainer is protected in vanilla and
                        // only reachable on NeoForge, so calling it in common code compiles on one loader and not the
                        // other. The button packet is already away by this point.
                        onClose();
                    }
                }
            )
                .bounds(
                    leftPos + COMMAND_BUTTON_X + (COMMAND_BUTTON_WIDTH + COMMAND_BUTTON_GAP) * 2,
                    topPos + COMMAND_BUTTON_Y,
                    COMMAND_BUTTON_WIDTH,
                    COMMAND_BUTTON_HEIGHT
                )
                .build()
        );

        addRenderableWidget(
            Button.builder(
                CONFIGURE_LABEL,
                button -> setSentryPage(true)
            )
                .bounds(
                    leftPos + COMMAND_BUTTON_X + (COMMAND_BUTTON_WIDTH + COMMAND_BUTTON_GAP) * 3,
                    topPos + COMMAND_BUTTON_Y,
                    CONFIGURE_BUTTON_WIDTH,
                    COMMAND_BUTTON_HEIGHT
                )
                .tooltip(Tooltip.create(CONFIGURE_TOOLTIP))
                .build()
        );

        updateEndContractButton();
        updatePackSlotPositions();
    }

    /**
     * The sentry page: what this marine will and will not shoot while it is holding a position.
     */
    private void initSentryPage() {
        var y = topPos + SENTRY_FIRST_ROW_Y;

        addRenderableWidget(
            Button.builder(
                editingFilter.isWhitelist() ? WHITELIST_LABEL : BLACKLIST_LABEL,
                button -> {
                    editingFilter.setWhitelist(!editingFilter.isWhitelist());
                    // Rebuilt rather than relabelled: the explanation line underneath changes with it, and one path
                    // that always redraws the page is harder to get subtly wrong than two.
                    rebuildWidgets();
                }
            )
                .bounds(leftPos + SENTRY_MARGIN_X, y, SENTRY_MODE_BUTTON_WIDTH, COMMAND_BUTTON_HEIGHT)
                .tooltip(Tooltip.create(editingFilter.isWhitelist() ? WHITELIST_TOOLTIP : BLACKLIST_TOOLTIP))
                .build()
        );

        var checkboxY = y + COMMAND_BUTTON_HEIGHT + SENTRY_ROW_GAP * 2;

        for (var category : SentryTargetFilter.selectableCategories()) {
            addRenderableWidget(
                Checkbox.builder(Component.translatable(category.getTranslationKey()), font)
                    .pos(leftPos + SENTRY_MARGIN_X, checkboxY)
                    .selected(editingFilter.hasCategory(category))
                    .onValueChange((checkbox, selected) -> editingFilter.setCategory(category, selected))
                    .build()
            );

            checkboxY += SENTRY_CHECKBOX_HEIGHT + SENTRY_ROW_GAP;
        }

        searchBox = new EditBox(
            font,
            leftPos + SENTRY_LIST_X,
            topPos + SENTRY_FIRST_ROW_Y,
            SENTRY_LIST_WIDTH,
            COMMAND_BUTTON_HEIGHT,
            SEARCH_HINT
        );

        searchBox.setHint(SEARCH_HINT);
        searchBox.setResponder(text -> {
            if (entityList != null) {
                entityList.applySearch(text);
            }
        });

        addRenderableWidget(searchBox);

        entityList = new SentryEntityList(
            leftPos + SENTRY_LIST_X,
            topPos + SENTRY_LIST_Y,
            SENTRY_LIST_WIDTH,
            SENTRY_LIST_HEIGHT
        );

        addFavoriteButtons();

        addRenderableWidget(
            Button.builder(APPLY_LABEL, button -> applyAndReturn())
                .bounds(
                    leftPos + SENTRY_LIST_X,
                    topPos + SENTRY_FOOTER_Y,
                    SENTRY_FOOTER_BUTTON_WIDTH,
                    COMMAND_BUTTON_HEIGHT
                )
                .build()
        );

        addRenderableWidget(
            Button.builder(CANCEL_LABEL, button -> setSentryPage(false))
                .bounds(
                    leftPos + SENTRY_LIST_X + SENTRY_FOOTER_BUTTON_WIDTH + COMMAND_BUTTON_GAP,
                    topPos + SENTRY_FOOTER_Y,
                    SENTRY_FOOTER_BUTTON_WIDTH,
                    COMMAND_BUTTON_HEIGHT
                )
                .build()
        );
    }

    /**
     * The favourites strip: click to load a saved configuration, shift-click to overwrite one with what is on screen.
     * <p>
     * Shift to save rather than a separate mode, because a save button plus a slot picker is two clicks and a decision
     * for something a player does rarely, while loading is what they do constantly.
     */
    private void addFavoriteButtons() {
        for (var slot : SentryFavorites.slots()) {
            var saved = SentryFavorites.get(slot);
            var label = Component.literal(String.valueOf(slot + 1));

            var button = Button.builder(
                label,
                pressed -> {
                    if (hasShiftDown()) {
                        SentryFavorites.set(slot, editingFilter);
                    } else {
                        var favorite = SentryFavorites.get(slot);

                        if (favorite == null) {
                            return;
                        }

                        editingFilter = favorite.copy();
                    }

                    // Rebuilt either way: loading changes every checkbox, and saving changes this button's tooltip.
                    rebuildWidgets();
                }
            )
                .bounds(
                    leftPos + SENTRY_MARGIN_X + slot * (SENTRY_FAVORITE_SIZE + SENTRY_FAVORITE_GAP),
                    topPos + SENTRY_FAVORITES_Y,
                    SENTRY_FAVORITE_SIZE,
                    SENTRY_FAVORITE_SIZE
                )
                .tooltip(
                    Tooltip.create(
                        saved == null
                            ? FAVORITE_EMPTY_TOOLTIP
                            : Component.literal(SentryFavorites.describe(saved)).append(FAVORITE_OVERWRITE_HINT)
                    )
                )
                .build();

            addRenderableWidget(button);
        }
    }

    private void setSentryPage(boolean showing) {
        showingSentryPage = showing;

        if (!showing) {
            // Edits are dropped on the way out, so backing out of the page cannot half-apply a filter.
            editingFilter = editingFilter.copy();
            entityList = null;
            searchBox = null;
        }

        rebuildWidgets();
    }

    private void applyAndReturn() {
        Human.MOD.networking().sendToServer(new C2SMarineSentryFilterPayload(editingFilter.copy()));

        setSentryPage(false);
    }

    /**
     * The list is drawn and clicked by hand rather than being a widget, so the page has to route input to it.
     */
    private boolean isSentryListInteraction(double mouseX, double mouseY) {
        return showingSentryPage && entityList != null && entityList.isOver(mouseX, mouseY);
    }

    /**
     * Greys out the dismiss button while the marine holds items the player put there, so a pack full of your own
     * supplies cannot be lost to a misclick.
     */
    private void updateEndContractButton() {
        if (endContractButton == null) {
            return;
        }

        var canEnd = menu.canEndContract();

        endContractButton.active = canEnd;
        endContractButton.setTooltip(Tooltip.create(canEnd ? END_CONTRACT_TOOLTIP : END_CONTRACT_BLOCKED_TOOLTIP));
    }

    /**
     * Moves every pack slot to where the current scroll position says it belongs, and hides the rows that fall outside
     * the window.
     * <p>
     * Nothing here touches which inventory index a slot reads. Scrolling is presentation only, which is what keeps a
     * click unambiguous no matter how far the server is behind the player's scroll wheel.
     */
    private void updatePackSlotPositions() {
        var firstVisibleRow = firstVisibleRow();

        for (var slot : menu.slots) {
            if (!(slot instanceof MarineInventorySlot marineSlot)) {
                continue;
            }

            var windowRow = marineSlot.getPackRow() - firstVisibleRow;
            var visible = !showingSentryPage
                && windowRow >= 0
                && windowRow < MarineInventoryMenu.VISIBLE_MARINE_ROW_COUNT;

            marineSlot.placeAt(
                MarineInventoryMenu.SLOTS_X + slotColumn(marineSlot) * MarineInventoryMenu.SLOT_SIZE,
                MarineInventoryMenu.MARINE_SLOTS_Y + Math.max(windowRow, 0) * MarineInventoryMenu.SLOT_SIZE,
                visible
            );
        }
    }

    private int slotColumn(MarineInventorySlot slot) {
        return slot.getContainerSlot() % MarineInventoryMenu.COLUMN_COUNT;
    }

    private int scrollableRowCount() {
        return MarineInventoryMenu.MARINE_ROW_COUNT - MarineInventoryMenu.VISIBLE_MARINE_ROW_COUNT;
    }

    private int firstVisibleRow() {
        return Math.round(scrollFraction * scrollableRowCount());
    }

    private boolean canScroll() {
        return scrollableRowCount() > 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isSentryListInteraction(mouseX, mouseY)) {
            entityList.scroll(scrollY);

            return true;
        }

        if (showingSentryPage || !canScroll()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        scrollFraction = Mth.clamp(scrollFraction - (float) scrollY / scrollableRowCount(), 0.0F, 1.0F);

        updatePackSlotPositions();

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isSentryListInteraction(mouseX, mouseY) && entityList.mouseClicked(mouseX, mouseY, editingFilter)) {
            return true;
        }

        if (!showingSentryPage && canScroll() && isOverScrollbar(mouseX, mouseY)) {
            draggingScroller = true;

            setScrollFromMouse(mouseY);

            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScroller) {
            setScrollFromMouse(mouseY);

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroller = false;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        var x = leftPos + SCROLLBAR_X;
        var y = topPos + SCROLLBAR_Y;

        return mouseX >= x && mouseX < x + SCROLLER_WIDTH && mouseY >= y && mouseY < y + SCROLLBAR_HEIGHT;
    }

    private void setScrollFromMouse(double mouseY) {
        // Measured from the middle of the knob so it sits under the cursor rather than jumping its own height away.
        var travel = (float) (SCROLLBAR_HEIGHT - SCROLLER_HEIGHT);
        var relative = (float) (mouseY - topPos - SCROLLBAR_Y - SCROLLER_HEIGHT / 2.0);

        scrollFraction = Mth.clamp(relative / travel, 0.0F, 1.0F);

        updatePackSlotPositions();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (showingSentryPage) {
            if (entityList != null) {
                entityList.render(guiGraphics, font, mouseX, mouseY, editingFilter);
            }

            renderTooltip(guiGraphics, mouseX, mouseY);

            return;
        }

        // Drawn after the slots so the wash sits OVER the issued item rather than behind it - the point is that the
        // item reads as present but untouchable.
        renderLockedSlotOverlays(guiGraphics);

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!showingSentryPage) {
            super.renderLabels(guiGraphics, mouseX, mouseY);

            return;
        }

        // The pack label and the player's inventory label mean nothing here.
        guiGraphics.drawString(font, SENTRY_PAGE_TITLE, titleLabelX, titleLabelY, 0x404040, false);
        guiGraphics.drawString(font, NAMED_MOBS_LABEL, SENTRY_LIST_X, SENTRY_FIRST_ROW_Y - 12, 0x404040, false);
        guiGraphics.drawString(font, FAVORITES_LABEL, SENTRY_MARGIN_X, SENTRY_FAVORITES_Y - 11, 0x404040, false);
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (showingSentryPage || modeButton == null) {
            return;
        }

        var modeIndex = menu.getModeIndex();

        modeButton.setMessage(labelForMode(modeIndex));
        modeButton.setTooltip(Tooltip.create(tooltipForMode(modeIndex)));

        if (sentryButton != null) {
            sentryButton.setMessage(labelForSentry(modeIndex));
            sentryButton.setTooltip(Tooltip.create(tooltipForSentry(modeIndex)));
        }

        // Re-evaluated every tick: the pack's contents change as the player drags items in and out.
        updateEndContractButton();
    }

    private Component labelForSentry(int modeIndex) {
        return modeIndex == MarineInventoryMenu.MODE_SENTRY
            ? SENTRY_ON_LABEL
            : SENTRY_OFF_LABEL;
    }

    private Component tooltipForSentry(int modeIndex) {
        return modeIndex == MarineInventoryMenu.MODE_SENTRY
            ? SENTRY_ON_TOOLTIP
            : SENTRY_OFF_TOOLTIP;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (showingSentryPage) {
            // Its own texture: the page is wider than the pack panel, and 310 will not fit a 256-wide file.
            guiGraphics.blit(SENTRY_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 512, 256);

            return;
        }

        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        var knobY = topPos + SCROLLBAR_Y + (int) (scrollFraction * (SCROLLBAR_HEIGHT - SCROLLER_HEIGHT));

        guiGraphics.blitSprite(
            canScroll() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE,
            leftPos + SCROLLBAR_X,
            knobY,
            SCROLLER_WIDTH,
            SCROLLER_HEIGHT
        );
    }

    private void renderLockedSlotOverlays(GuiGraphics guiGraphics) {
        for (var slot : menu.slots) {
            if (slot instanceof MarineInventorySlot marineSlot && marineSlot.isLocked()) {
                guiGraphics.fill(
                    leftPos + slot.x,
                    topPos + slot.y,
                    leftPos + slot.x + 16,
                    topPos + slot.y + 16,
                    LOCKED_SLOT_TINT
                );
            }
        }
    }

    private Component labelForMode(int modeIndex) {
        return modeIndex == MarineInventoryMenu.MODE_HOLD
            ? HOLD_LABEL
            : FOLLOW_LABEL;
    }

    private Component tooltipForMode(int modeIndex) {
        return modeIndex == MarineInventoryMenu.MODE_HOLD
            ? HOLD_TOOLTIP
            : FOLLOW_TOOLTIP;
    }
}
