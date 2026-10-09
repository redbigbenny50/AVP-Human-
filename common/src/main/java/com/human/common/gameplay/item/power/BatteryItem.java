package com.human.common.gameplay.item.power;

import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * A portable power cell. [stated] Sep 29/30:
 * <ul>
 * <li>BATTERY PACK (lithium) - {@value #PACK_CAPACITY} FE, RECHARGEABLE (the charging hub's plate, or any mod's
 * charger), stays as an empty pack when drained. Starts empty. Tools built with a pack hold its capacity.</li>
 * <li>NUCLEAR BATTERY - ten times the pack, {@value #NUCLEAR_CAPACITY} FE. NOT rechargeable, and CONSUMED - the item is
 * gone - the moment it is drained. It therefore starts FULL: nothing could ever fill it.</li>
 * </ul>
 * The charge is stored on the item ({@link HumanDataComponents#STORED_POWER}, the same component a broken battery
 * block's item carries), so a half-used cell can be moved between machines or traded. Each loader exposes these as a
 * standard energy item, so other mods' chargers and machines see them too.
 */
public class BatteryItem extends Item {

    public static final long PACK_CAPACITY = 100_000L;

    public static final long NUCLEAR_CAPACITY = 1_000_000L;

    /** The most one cell moves in a single transfer, in or out. */
    public static final long MAX_TRANSFER = 5_000L;

    private final long capacity;

    private final boolean rechargeable;

    public BatteryItem(Properties properties, long capacity, boolean rechargeable) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
        this.rechargeable = rechargeable;
    }

    public long getCapacity() {
        return capacity;
    }

    public boolean isRechargeable() {
        return rechargeable;
    }

    /** The cell's charge. With no charge recorded, a rechargeable cell is empty and a nuclear one full. */
    public long getStored(ItemStack stack) {
        var stored = stack.get(HumanDataComponents.STORED_POWER.get());

        if (stored == null) {
            return rechargeable ? 0L : capacity;
        }

        return Mth.clamp(stored, 0L, capacity);
    }

    public void setStored(ItemStack stack, long amount) {
        stack.set(HumanDataComponents.STORED_POWER.get(), Mth.clamp(amount, 0L, capacity));
    }

    /** @return how much the cell took (or would take) - never more than offered, nothing for a nuclear cell */
    public long receive(ItemStack stack, long amount, boolean simulate) {
        if (!rechargeable || amount <= 0L || stack.isEmpty()) {
            return 0L;
        }

        var stored = getStored(stack);
        var moved = Math.min(Math.min(amount, MAX_TRANSFER), capacity - stored);

        if (moved > 0L && !simulate) {
            setStored(stack, stored + moved);
        }

        return Math.max(0L, moved);
    }

    /**
     * Draws power from the cell.
     * <p>
     * ⚠ A NUCLEAR cell drained to zero is CONSUMED: the stack is shrunk here, in place, so whatever holds it (a slot, a
     * machine, another mod's container) sees it gone at once.
     * </p>
     *
     * @return how much came (or would come) out
     */
    public long extract(ItemStack stack, long amount, boolean simulate) {
        if (amount <= 0L || stack.isEmpty()) {
            return 0L;
        }

        var stored = getStored(stack);
        var moved = Math.min(Math.min(amount, MAX_TRANSFER), stored);

        if (moved > 0L && !simulate) {
            setStored(stack, stored - moved);

            if (!rechargeable && stored - moved <= 0L) {
                stack.shrink(1);
            }
        }

        return Math.max(0L, moved);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The charge bar and tooltip
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return Math.round(13.0F * getStored(stack) / capacity);
    }

    /** Green when full through yellow to red when empty, as vanilla's durability bar. */
    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        var fraction = (float) getStored(stack) / capacity;

        return Mth.hsvToRgb(fraction / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        tooltip.add(
            Component.translatable(
                "tooltip.avp_human.battery.charge",
                String.format(Locale.ROOT, "%,d", getStored(stack)),
                String.format(Locale.ROOT, "%,d", capacity)
            ).withStyle(ChatFormatting.GRAY)
        );
        tooltip.add(
            Component.translatable(rechargeable ? "tooltip.avp_human.battery.rechargeable" : "tooltip.avp_human.battery.single_use")
                .withStyle(ChatFormatting.DARK_GRAY)
        );
    }
}
