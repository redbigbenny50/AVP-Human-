package com.human.client.screen;

import com.human.HumanResources;
import com.human.client.screen.widget.OptionSideControls;
import com.human.common.gameplay.block.entity.power.impl.BatteryBlockEntity;
import com.human.common.gameplay.power.StoreMode;
import com.human.common.gameplay.power.menu.BatteryMenu;
import com.human.common.gameplay.power.upgrade.BatteryUpgrade;
import com.human.common.gameplay.power.upgrade.RedstoneControl;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The battery screen: the charge bar with the emergency-reserve and alarm lines marked on it, stored power and rate,
 * and the four upgrade slots.
 * <p>
 * [stated] Oct 2: its settings are side buttons now, each opening a sub-menu - mode always, and reserve, redstone and
 * alarm only with their card fitted (the button appears when the card goes in).
 * </p>
 */
public class BatteryScreen extends AbstractContainerScreen<BatteryMenu> {

    private static final ResourceLocation TEXTURE = HumanResources.location("textures/gui/container/battery.png");

    private static final int BAR_X = 8;

    private static final int BAR_Y = 18;

    private static final int BAR_WIDTH = 106;

    private static final int BAR_HEIGHT = 12;

    private static final int BAR_FILL = 0xFF3FA34D;

    private static final int BAR_FILL_TOP = 0xFF72D17F;

    private static final int RESERVE_MARK = 0xFFA24CD0;

    private static final int ALARM_MARK = 0xFFF0F0F0;

    private static final int TEXT = 0x404040;

    private OptionSideControls controls;

    public BatteryScreen(BatteryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        controls = new OptionSideControls(
            menu.containerId,
            font,
            List.of(
                new OptionSideControls.Field(
                    Component.translatable("gui.avp_human.battery.side.mode"),
                    () -> true,
                    item(new ItemStack(com.human.common.registry.init.item.HumanBlockItems.BATTERY.get())),
                    this::modeOptions
                ),
                new OptionSideControls.Field(
                    Component.translatable("gui.avp_human.battery.side.reserve"),
                    () -> menu.has(BatteryUpgrade.EMERGENCY),
                    item(new ItemStack(HumanItems.EMERGENCY_UPGRADE.get())),
                    this::reserveOptions
                ),
                new OptionSideControls.Field(
                    Component.translatable("gui.avp_human.battery.side.redstone"),
                    () -> menu.has(BatteryUpgrade.REDSTONE),
                    item(new ItemStack(HumanItems.REDSTONE_UPGRADE.get())),
                    this::redstoneOptions
                ),
                new OptionSideControls.Field(
                    Component.translatable("gui.avp_human.battery.side.alarm"),
                    () -> menu.has(BatteryUpgrade.ALARM),
                    item(new ItemStack(HumanItems.ALARM_UPGRADE.get())),
                    this::alarmOptions
                )
            )
        );
    }

    private static OptionSideControls.Icon item(ItemStack stack) {
        return (graphics, x, y) -> graphics.renderItem(stack, x, y);
    }

    private List<OptionSideControls.Option> modeOptions() {
        var options = new ArrayList<OptionSideControls.Option>();

        for (var mode : StoreMode.values()) {
            options.add(
                new OptionSideControls.Option(
                    Component.translatable("gui.avp_human.battery.mode." + mode.id()),
                    menu.mode() == mode,
                    BatteryMenu.BUTTON_SET_MODE + mode.ordinal()
                )
            );
        }

        return options;
    }

    private List<OptionSideControls.Option> reserveOptions() {
        var options = new ArrayList<OptionSideControls.Option>();

        for (int index = 0; index < BatteryBlockEntity.RESERVE_PERCENT.length; index++) {
            var percent = BatteryBlockEntity.RESERVE_PERCENT[index];
            options.add(
                new OptionSideControls.Option(
                    Component.translatable("gui.avp_human.battery.reserve", percent),
                    menu.reservePercent() == percent,
                    BatteryMenu.BUTTON_SET_RESERVE + index
                )
            );
        }

        return options;
    }

    private List<OptionSideControls.Option> redstoneOptions() {
        var options = new ArrayList<OptionSideControls.Option>();

        for (var control : RedstoneControl.values()) {
            options.add(
                new OptionSideControls.Option(
                    Component.translatable("gui.avp_human.battery.redstone." + control.name().toLowerCase(Locale.ROOT)),
                    menu.redstone() == control,
                    BatteryMenu.BUTTON_SET_REDSTONE + control.ordinal()
                )
            );
        }

        return options;
    }

    private List<OptionSideControls.Option> alarmOptions() {
        var options = new ArrayList<OptionSideControls.Option>();

        for (int index = 0; index < BatteryBlockEntity.ALARM_PERCENT.length; index++) {
            var percent = BatteryBlockEntity.ALARM_PERCENT[index];
            options.add(
                new OptionSideControls.Option(
                    Component.translatable("gui.avp_human.battery.alarm", percent),
                    menu.alarmPercent() == percent,
                    BatteryMenu.BUTTON_SET_ALARM + index
                )
            );
        }

        return options;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        controls.layout(leftPos, topPos);
        super.render(graphics, mouseX, mouseY, partialTick);
        controls.render(graphics, mouseX, mouseY);

        if (controls.isOpen() || controls.isMouseOver(mouseX, mouseY)) {
            controls.renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        renderTooltip(graphics, mouseX, mouseY);

        // A capacity upgrade the battery is too full to give back: say why the slot will not let go.
        if (hoveredSlot != null && hoveredSlot.hasItem() && menu.isLockedCapacitySlot(hoveredSlot)) {
            graphics.renderTooltip(font, Component.translatable("gui.avp_human.battery.capacity_locked"), mouseX, mouseY + 20);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return controls.mouseClicked(mouseX, mouseY) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return controls.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Clicks on the side controls are not clicks outside the window - they must not drop the held item. */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        return !controls.isMouseOver(mouseX, mouseY) && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        var x = leftPos;
        var y = topPos;

        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        var capacity = menu.capacity();
        var fraction = capacity <= 0L ? 0.0D : Math.min(1.0D, menu.stored() / (double) capacity);
        var width = (int) Math.round(BAR_WIDTH * fraction);

        if (width > 0) {
            graphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + width, y + BAR_Y + BAR_HEIGHT, BAR_FILL);
            graphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + width, y + BAR_Y + 1, BAR_FILL_TOP);
        }

        if (menu.has(BatteryUpgrade.EMERGENCY)) {
            mark(graphics, x, y, menu.reservePercent(), RESERVE_MARK);
        }

        if (menu.has(BatteryUpgrade.ALARM)) {
            mark(graphics, x, y, menu.alarmPercent(), ALARM_MARK);
        }
    }

    /** A one-pixel line across the bar at a percentage - where the reserve starts, or where the alarm goes off. */
    private static void mark(GuiGraphics graphics, int x, int y, int percent, int colour) {
        var at = x + BAR_X + BAR_WIDTH * percent / 100;

        graphics.fill(at, y + BAR_Y, at + 1, y + BAR_Y + BAR_HEIGHT, colour);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        graphics.drawString(
            font,
            Component.translatable("gui.avp_human.battery.stored", format(menu.stored()), format(menu.capacity())),
            BAR_X,
            BAR_Y + BAR_HEIGHT + 4,
            TEXT,
            false
        );
        graphics.drawString(
            font,
            Component.translatable("gui.avp_human.battery.rate", format(menu.rate())),
            BAR_X,
            BAR_Y + BAR_HEIGHT + 14,
            TEXT,
            false
        );
    }

    private static String format(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /** For callers that want the per-upgrade base capacity - kept here so the screen and slot rule agree. */
    public static long baseCapacity() {
        return BatteryBlockEntity.CAPACITY;
    }
}
