package com.human.common.gameplay.block.entity.power.impl;

import com.human.common.gameplay.block.power.BatteryBlock;
import com.human.common.gameplay.power.StoreMode;
import com.human.common.gameplay.power.menu.BatteryMenu;
import com.human.common.gameplay.power.upgrade.BatteryUpgrade;
import com.human.common.gameplay.power.upgrade.BatteryUpgradeItem;
import com.human.common.gameplay.power.upgrade.RedstoneControl;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * The battery: the full-size store.
 * <p>
 * ⭐ [stated] Sep 29: the new battery art is the {@code battery} at 1.5M - the old ESU's numbers - while the old ESU's
 * block became the charging hub. Power ports on five faces, none on the front, which carries the charge gauge; the
 * wiring tool opens and closes each port as a breaker. Batteries touching each other connect with no cable.
 * </p>
 * <h2>THE GAUGE</h2> Up to four lit bars, kept in the block state ({@link BatteryBlock#CHARGE}), re-measured every
 * {@value #GAUGE_INTERVAL_TICKS} ticks and written only when the bar count changes.
 * <h2>UPGRADES</h2> [stated] Sep 29 - four upgrade slots, opened with a bare-hand right-click:
 * <ul>
 * <li>CAPACITY (stacks) - each adds the base capacity again: 1.5M, 3M ... 7.5M with four.</li>
 * <li>TRANSFER (stacks) - each doubles the rate: 5,000 -> 10,000 -> ... 80,000 FE/t with four.</li>
 * <li>EMERGENCY - the last {@link #RESERVE_PERCENT}% of charge is kept for Critical machines only (see
 * {@code PowerGrid.drawFromStores}). It does NOT shrink the battery: it still charges to 100%.</li>
 * <li>REDSTONE - {@link RedstoneControl}: ignore, backup (discharge only when powered), hold, or off.</li>
 * <li>ALARM - below {@link #ALARM_PERCENT}%, a bell at the battery and a message to its owner.</li>
 * <li>ARMORED - [stated] Sep 29: obsidian hardness and blast resistance (see {@code BatteryBlock} and
 * {@code MixinExplosionDamageCalculator_ArmoredBattery}).</li>
 * </ul>
 * The upgrades ride along when the battery is broken, like its charge.
 */
public class BatteryBlockEntity extends PowerStorageBlockEntity implements MenuProvider {

    public static final long CAPACITY = 1_500_000L;

    public static final long MAX_TRANSFER_RATE = 5_000L;

    public static final int UPGRADE_SLOTS = 4;

    /** The emergency reserve line and the alarm threshold, both chosen on the battery's screen. */
    public static final int[] RESERVE_PERCENT = { 10, 25, 50 };

    public static final int[] ALARM_PERCENT = { 10, 25, 50 };

    private static final int GAUGE_INTERVAL_TICKS = 10;

    private static final int REDSTONE_INTERVAL_TICKS = 5;

    private static final int ALARM_INTERVAL_TICKS = 20;

    /** While still below the line, the alarm repeats this often (one minute). */
    private static final int ALARM_REPEAT_TICKS = 1200;

    /** The alarm re-arms only once the charge is this many points ABOVE the line, so it cannot chatter at the edge. */
    private static final int ALARM_REARM_MARGIN = 5;

    /** The owner hears the alarm within this many blocks; with no owner recorded, everyone within half of it does. */
    private static final double ALARM_RANGE = 64.0D;

    private static final String NBT_UPGRADES = "Upgrades";

    private static final String NBT_RESERVE = "reserveIndex";

    private static final String NBT_REDSTONE = "redstoneControl";

    private static final String NBT_ALARM = "alarmIndex";

    private static final String NBT_OWNER = "owner";

    // Menu data: 16-bit values only (vanilla sends menu data as shorts), so every long is four chunks.
    public static final int DATA_STORED = 0;

    public static final int DATA_CAPACITY = 4;

    public static final int DATA_RATE = 8;

    public static final int DATA_MODE = 12;

    public static final int DATA_RESERVE = 13;

    public static final int DATA_REDSTONE = 14;

    public static final int DATA_ALARM = 15;

    public static final int DATA_POWERED = 16;

    public static final int DATA_COUNT = 17;

    private final NonNullList<ItemStack> upgrades = NonNullList.withSize(UPGRADE_SLOTS, ItemStack.EMPTY);

    private final Map<BatteryUpgrade, Integer> installed = new EnumMap<>(BatteryUpgrade.class);

    private final Container upgradeContainer = new UpgradeContainer();

    private final ContainerData data = new Data();

    private int reserveIndex;

    private RedstoneControl redstoneControl = RedstoneControl.IGNORED;

    private int alarmIndex;

    @Nullable
    private UUID owner;

    private boolean powered;

    private boolean alarmArmed = true;

    private int alarmRepeat;

    private int gaugeTimer;

    private int redstoneTimer;

    private int alarmTimer;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.BATTERY.get(), pos, state, CAPACITY, MAX_TRANSFER_RATE);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Upgrades
    // ------------------------------------------------------------------------------------------------------------

    public int count(BatteryUpgrade upgrade) {
        return installed.getOrDefault(upgrade, 0);
    }

    public boolean has(BatteryUpgrade upgrade) {
        return count(upgrade) > 0;
    }

    public boolean isArmored() {
        return has(BatteryUpgrade.ARMORED);
    }

    public Container getUpgradeContainer() {
        return upgradeContainer;
    }

    public ContainerData getData() {
        return data;
    }

    private void recountUpgrades() {
        installed.clear();

        for (var stack : upgrades) {
            if (stack.getItem() instanceof BatteryUpgradeItem item) {
                installed.merge(item.getUpgrade(), 1, Integer::sum);
            }
        }
    }

    private void onUpgradesChanged() {
        recountUpgrades();
        setChanged();
    }

    @Override
    public long getCapacity() {
        return CAPACITY * (1L + count(BatteryUpgrade.CAPACITY));
    }

    @Override
    public long getTransferRate() {
        return MAX_TRANSFER_RATE << Math.min(count(BatteryUpgrade.TRANSFER), UPGRADE_SLOTS);
    }

    /** The capacity this battery would have with one capacity upgrade fewer - the slot refuses removal above it. */
    public long capacityWithoutOneUpgrade() {
        return CAPACITY * Math.max(1L, count(BatteryUpgrade.CAPACITY));
    }

    @Override
    public long getReservedPower() {
        if (!has(BatteryUpgrade.EMERGENCY)) {
            return 0L;
        }

        return Math.min(getStoredPower(), getCapacity() * reservePercent() / 100L);
    }

    @Override
    protected boolean canCharge() {
        return !has(BatteryUpgrade.REDSTONE) || redstoneControl.canCharge(powered);
    }

    @Override
    protected boolean canDischarge() {
        return !has(BatteryUpgrade.REDSTONE) || redstoneControl.canDischarge(powered);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Settings (cycled from the screen)
    // ------------------------------------------------------------------------------------------------------------

    public int reservePercent() {
        return RESERVE_PERCENT[Math.floorMod(reserveIndex, RESERVE_PERCENT.length)];
    }

    public int alarmPercent() {
        return ALARM_PERCENT[Math.floorMod(alarmIndex, ALARM_PERCENT.length)];
    }

    public void cycleMode() {
        setMode(getMode().next());
    }

    /** [stated] Oct 2: the side-button pop-ups pick a value directly. Out-of-range values are ignored. */
    public void setReserveIndex(int index) {
        if (index >= 0 && index < RESERVE_PERCENT.length && index != reserveIndex) {
            reserveIndex = index;
            setChanged();
        }
    }

    public void setRedstoneControl(RedstoneControl control) {
        if (control != redstoneControl) {
            redstoneControl = control;
            setChanged();
        }
    }

    public void setAlarmIndex(int index) {
        if (index >= 0 && index < ALARM_PERCENT.length && index != alarmIndex) {
            alarmIndex = index;
            alarmArmed = true;
            setChanged();
        }
    }

    public void cycleReserve() {
        reserveIndex = (reserveIndex + 1) % RESERVE_PERCENT.length;
        setChanged();
    }

    public void cycleRedstone() {
        redstoneControl = redstoneControl.next();
        setChanged();
    }

    public void cycleAlarm() {
        alarmIndex = (alarmIndex + 1) % ALARM_PERCENT.length;
        alarmArmed = true;
        setChanged();
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean canConnectOn(Direction direction) {
        var state = getBlockState();

        return !state.hasProperty(BatteryBlock.FACING) || direction != state.getValue(BatteryBlock.FACING);
    }

    @Override
    public void serverTick(Level level, BlockPos blockPos, BlockState blockState) {
        super.serverTick(level, blockPos, blockState);

        // Redstone is polled, not listened for: five ticks is fast enough for a lever, and a battery WITHOUT the
        // upgrade never looks at all.
        if (has(BatteryUpgrade.REDSTONE) && ++redstoneTimer >= REDSTONE_INTERVAL_TICKS) {
            redstoneTimer = 0;
            powered = level.hasNeighborSignal(blockPos);
        }

        if (has(BatteryUpgrade.ALARM) && ++alarmTimer >= ALARM_INTERVAL_TICKS) {
            alarmTimer = 0;
            checkAlarm(level, blockPos);
        }

        if (++gaugeTimer < GAUGE_INTERVAL_TICKS) {
            return;
        }

        gaugeTimer = 0;

        if (!blockState.hasProperty(BatteryBlock.CHARGE)) {
            return;
        }

        var bars = barsFor(getStoredPower(), getCapacity());

        if (blockState.getValue(BatteryBlock.CHARGE) != bars) {
            // ⚠ UPDATE_CLIENTS only: the look changes, nothing a neighbour cares about does.
            level.setBlock(blockPos, blockState.setValue(BatteryBlock.CHARGE, bars), Block.UPDATE_CLIENTS);
        }
    }

    private void checkAlarm(Level level, BlockPos pos) {
        var capacity = getCapacity();
        var percent = capacity <= 0L ? 100L : getStoredPower() * 100L / capacity;
        var line = alarmPercent();

        if (percent >= line + ALARM_REARM_MARGIN) {
            alarmArmed = true;
            alarmRepeat = 0;
            return;
        }

        if (percent >= line) {
            return;
        }

        if (!alarmArmed && (alarmRepeat += ALARM_INTERVAL_TICKS) < ALARM_REPEAT_TICKS) {
            return;
        }

        alarmArmed = false;
        alarmRepeat = 0;
        soundAlarm(level, pos, line);
    }

    private void soundAlarm(Level level, BlockPos pos, int line) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0F, 0.6F);

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        var name = getDeviceName().isEmpty() ? Component.translatable("block.avp_human.battery") : Component.literal(getDeviceName());
        var message = Component.translatable("message.avp_human.battery.alarm", name, line);
        var centre = pos.getCenter();

        if (owner != null) {
            var player = serverLevel.getServer().getPlayerList().getPlayer(owner);

            if (player != null && player.level() == level && player.distanceToSqr(centre) <= ALARM_RANGE * ALARM_RANGE) {
                player.displayClientMessage(message, true);
            }

            return;
        }

        var range = ALARM_RANGE / 2.0D;

        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(centre) <= range * range) {
                player.displayClientMessage(message, true);
            }
        }
    }

    public static int barsFor(long stored, long capacity) {
        if (stored <= 0L || capacity <= 0L) {
            return 0;
        }

        return (int) Math.min(4L, (stored * 4L + capacity - 1L) / capacity);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public @NotNull Component getDisplayName() {
        return getDeviceName().isEmpty() ? Component.translatable("block.avp_human.battery") : Component.literal(getDeviceName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new BatteryMenu(containerId, inventory, this);
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getCenter()) <= 64.0D;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Save / load / carry in the item
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void loadBeforeStorage(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        for (var index = 0; index < UPGRADE_SLOTS; index++) {
            upgrades.set(index, ItemStack.EMPTY);
        }

        if (tag.contains(NBT_UPGRADES)) {
            ContainerHelper.loadAllItems(tag.getCompound(NBT_UPGRADES), upgrades, registries);
        }

        recountUpgrades();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put(NBT_UPGRADES, ContainerHelper.saveAllItems(new CompoundTag(), upgrades, registries));
        tag.putInt(NBT_RESERVE, reserveIndex);
        tag.putInt(NBT_REDSTONE, redstoneControl.ordinal());
        tag.putInt(NBT_ALARM, alarmIndex);

        if (owner != null) {
            tag.putUUID(NBT_OWNER, owner);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        reserveIndex = Math.floorMod(tag.getInt(NBT_RESERVE), RESERVE_PERCENT.length);
        redstoneControl = RedstoneControl.byOrdinal(tag.getInt(NBT_REDSTONE));
        alarmIndex = Math.floorMod(tag.getInt(NBT_ALARM), ALARM_PERCENT.length);
        owner = tag.hasUUID(NBT_OWNER) ? tag.getUUID(NBT_OWNER) : null;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NotNull Builder components) {
        super.collectImplicitComponents(components);

        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(upgrades));
    }

    /**
     * ⚠ Upgrades FIRST, then super - which restores the stored power and clamps it to the capacity. Reversed, a full
     * 7.5M battery would come back from the item clamped to 1.5M.
     */
    @Override
    protected void applyImplicitComponents(BlockEntity.@NotNull DataComponentInput componentInput) {
        var contents = componentInput.get(DataComponents.CONTAINER);

        if (contents != null) {
            contents.copyInto(upgrades);
            recountUpgrades();
        }

        super.applyImplicitComponents(componentInput);
    }

    @Override
    public void removeComponentsFromTag(@NotNull CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove(NBT_UPGRADES);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The upgrade slots, as a Container for the menu. NOT exposed to hoppers: the battery itself is not a Container.
    // ------------------------------------------------------------------------------------------------------------

    private final class UpgradeContainer implements Container {

        @Override
        public int getContainerSize() {
            return UPGRADE_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return upgrades.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            return upgrades.get(slot);
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            var removed = ContainerHelper.removeItem(upgrades, slot, amount);

            if (!removed.isEmpty()) {
                onUpgradesChanged();
            }

            return removed;
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            var removed = ContainerHelper.takeItem(upgrades, slot);

            onUpgradesChanged();
            return removed;
        }

        @Override
        public void setItem(int slot, @NotNull ItemStack stack) {
            upgrades.set(slot, stack);
            onUpgradesChanged();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            onUpgradesChanged();
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return BatteryBlockEntity.this.stillValid(player);
        }

        @Override
        public void clearContent() {
            for (var index = 0; index < UPGRADE_SLOTS; index++) {
                upgrades.set(index, ItemStack.EMPTY);
            }

            onUpgradesChanged();
        }
    }

    private final class Data implements ContainerData {

        @Override
        public int get(int index) {
            if (index < DATA_CAPACITY) {
                return chunk(getStoredPower(), index - DATA_STORED);
            }

            if (index < DATA_RATE) {
                return chunk(getCapacity(), index - DATA_CAPACITY);
            }

            if (index < DATA_MODE) {
                return chunk(getTransferRate(), index - DATA_RATE);
            }

            return switch (index) {
                case DATA_MODE -> getMode().ordinal();
                case DATA_RESERVE -> reserveIndex;
                case DATA_REDSTONE -> redstoneControl.ordinal();
                case DATA_ALARM -> alarmIndex;
                case DATA_POWERED -> powered ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    }

    /** One 16-bit piece of a long - chunk 0 is the lowest. */
    public static int chunk(long value, int chunk) {
        return (int) ((value >>> (chunk * 16)) & 0xFFFFL);
    }

    /** Rebuilds a long from four 16-bit pieces starting at {@code first}. */
    public static long unchunk(ContainerData data, int first) {
        var value = 0L;

        for (var chunk = 0; chunk < 4; chunk++) {
            value |= (data.get(first + chunk) & 0xFFFFL) << (chunk * 16);
        }

        return value;
    }

    /** Store mode as the menu sees it. */
    public static StoreMode modeOf(ContainerData data) {
        var values = StoreMode.values();
        var ordinal = data.get(DATA_MODE);

        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : StoreMode.PRIMARY;
    }
}
