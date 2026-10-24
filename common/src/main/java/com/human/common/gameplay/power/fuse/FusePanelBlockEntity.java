package com.human.common.gameplay.power.fuse;

import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.block.entity.power.PowerNodeBlockEntity;
import com.human.common.gameplay.machine.MachinePriority;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.gameplay.power.upgrade.BatteryUpgrade;
import com.human.common.gameplay.power.upgrade.BatteryUpgradeItem;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The fuse panel: the WIRELESS HUB. [stated] readme + Sep 29/30 -
 * <ul>
 * <li>The main cable feeds its BACK. It stores nothing and has no idle draw: to the grid it is ONE consumer asking,
 * each tick, for exactly what its bound devices ask for.</li>
 * <li>Devices are bound with the wiring tool: up to {@value #BASE_DEVICES}, plus {@value #DEVICES_PER_CARD} per
 * CAPACITY card in its upgrade slots ([stated] "the capacity upgrade could allow more connected devices"), each within
 * {@value #RANGE} blocks.</li>
 * <li>A device belongs to ONE panel. Binding it to another moves it; panels may overlap without double-powering.</li>
 * <li>What it is paid it passes to its devices in their own priority order, and it bills the grid only for what they
 * actually took - the same all-or-nothing consumers the cables feed.</li>
 * </ul>
 * <h2>SENTRY TURRETS</h2> [stated] Sep 30: a turret is an ENTITY, not a block, so the panel keeps it separately - by
 * its id, with where it stands for the screen - and counts it against the same device limit. Turrets are paid FIRST:
 * they are the base's defence, and a turret only asks while it has room.
 * <h2>COST</h2> At most {@value #MAX_DEVICES_EVER} block-entity lookups a tick, only while the panel is loaded; an
 * unloaded device is skipped and simply unpowered.
 */
public class FusePanelBlockEntity extends PowerNodeBlockEntity implements PowerNode.PowerConsumer, MenuProvider {

    public static final int BASE_DEVICES = 16;

    public static final int DEVICES_PER_CARD = 8;

    public static final int UPGRADE_SLOTS = 4;

    public static final int MAX_DEVICES_EVER = BASE_DEVICES + DEVICES_PER_CARD * UPGRADE_SLOTS;

    /** [stated] readme: a 32-block radius. */
    public static final int RANGE = 32;

    private static final int PRUNE_INTERVAL = 40;

    private static final String NBT_DEVICES = "devices";

    private static final String NBT_UPGRADES = "upgrades";

    private static final String NBT_PRIORITY = "priority";

    private static final String NBT_SENTRIES = "sentries";

    /** A bound turret: its id, and where it stands (turrets do not move - kept for the screen and the range check). */
    public record BoundSentry(
        java.util.UUID id,
        BlockPos pos
    ) {}

    private final List<BoundSentry> sentries = new ArrayList<>();

    private final List<BlockPos> devices = new ArrayList<>();

    private final SimpleContainer upgrades = new SimpleContainer(UPGRADE_SLOTS) {

        @Override
        public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
            return isCapacityCard(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            FusePanelBlockEntity.this.setChanged();
        }
    };

    private MachinePriority priority = MachinePriority.DEFAULT;

    public FusePanelBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.FUSE_PANEL.get(), pos, state);
    }

    public static boolean isCapacityCard(ItemStack stack) {
        return stack.getItem() instanceof BatteryUpgradeItem card && card.getUpgrade() == BatteryUpgrade.CAPACITY;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Binding
    // ------------------------------------------------------------------------------------------------------------

    public List<BlockPos> getDevices() {
        return devices;
    }

    public boolean isBound(BlockPos pos) {
        return devices.contains(pos);
    }

    public List<BoundSentry> getSentries() {
        return sentries;
    }

    /** Block devices and turrets together - what the device limit counts. */
    public int getBoundCount() {
        return devices.size() + sentries.size();
    }

    /**
     * [stated] Oct 21 - built into or landed from a ship. Its wireless devices are kept by position: each one that
     * moved with it is followed to its new spot; one left behind is unbound (a ship and the ground cannot share a
     * panel). Sentries are bound by their id, so they stay bound; their remembered spot follows when it was part of the
     * move.
     */
    @Override
    public void onShipMove(com.human.common.gameplay.ship.ShipMove move) {
        super.onShipMove(move);

        var kept = new ArrayList<BlockPos>();

        for (var device : devices) {
            var now = move.remap(device);

            if (now != null) {
                kept.add(now);
            }
        }

        devices.clear();
        devices.addAll(kept);

        for (int index = 0; index < sentries.size(); index++) {
            var sentry = sentries.get(index);
            var now = move.remap(sentry.pos());

            if (now != null) {
                sentries.set(index, new BoundSentry(sentry.id(), now));
            }
        }

        setChanged();
    }

    /**
     * Quality pass: a sentry turret is an ENTITY - Sable carries it separately from the blocks - and it remembers its
     * panel by position too, so after the panel moved the turret would still point at the panel's old spot and lose
     * power. Each bound turret that is loaded and still names this panel's OLD position is pointed at the new one. (A
     * turret not loaded right now keeps the old position; the panel's own unbind check then lets it go.)
     */
    @Override
    public void afterShipMove(com.human.common.gameplay.ship.ShipMove move) {
        var old = move.original(worldPosition);

        if (old == null) {
            return;
        }

        for (var sentry : sentries) {
            if (
                move.level().getEntity(sentry.id()) instanceof com.human.common.gameplay.entity.machine.SentryTurret turret
                    && old.equals(turret.getFusePanel())
            ) {
                turret.setFusePanel(worldPosition);
            }
        }
    }

    public boolean isBoundSentry(java.util.UUID id) {
        for (var sentry : sentries) {
            if (sentry.id().equals(id)) {
                return true;
            }
        }

        return false;
    }

    public void bindSentry(java.util.UUID id, BlockPos pos) {
        if (!isBoundSentry(id)) {
            sentries.add(new BoundSentry(id, pos.immutable()));
            changed();
        }
    }

    public void unbindSentry(java.util.UUID id) {
        if (sentries.removeIf(sentry -> sentry.id().equals(id))) {
            changed();
        }
    }

    /** A bound turret that is loaded, still points at this panel and is in range - or null. */
    private com.human.common.gameplay.entity.machine.@Nullable SentryTurret sentry(Level level, BoundSentry bound) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return null;
        }

        return serverLevel.getEntity(bound.id()) instanceof com.human.common.gameplay.entity.machine.SentryTurret turret
            && turret.isAlive()
            && worldPosition.equals(turret.getFusePanel())
            && isInRange(turret.blockPosition()) ? turret : null;
    }

    /** 16, plus 8 for each capacity card. */
    public int getDeviceCapacity() {
        var cards = 0;

        for (var slot = 0; slot < upgrades.getContainerSize(); slot++) {
            if (isCapacityCard(upgrades.getItem(slot))) {
                cards++;
            }
        }

        return BASE_DEVICES + DEVICES_PER_CARD * cards;
    }

    public boolean isInRange(BlockPos pos) {
        return pos.distSqr(worldPosition) <= (double) RANGE * RANGE;
    }

    /** Adds a device - the caller has checked range, capacity and that it may bind. */
    public void bind(BlockPos pos) {
        if (!devices.contains(pos)) {
            devices.add(pos.immutable());
            changed();
        }
    }

    public void unbind(BlockPos pos) {
        if (devices.remove(pos)) {
            changed();
        }
    }

    /** Releases every loaded device - the panel is being broken. */
    public void releaseAll() {
        if (level == null) {
            return;
        }

        for (var pos : devices) {
            if (
                level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PowerConsumerBlockEntity device
                    && worldPosition.equals(device.getFusePanel())
            ) {
                device.setFusePanel(null);
            }
        }

        for (var bound : sentries) {
            var turret = sentry(level, bound);

            if (turret != null) {
                turret.setFusePanel(null);
            }
        }

        devices.clear();
        sentries.clear();
    }

    private void changed() {
        setChanged();

        // Power nodes publish their whole tag to clients on update - that is how the screen's device list arrives.
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** A bound device that is loaded, still points at this panel, may still bind, and is in range - or null. */
    private @Nullable PowerConsumerBlockEntity device(Level level, BlockPos pos) {
        if (!level.isLoaded(pos) || !isInRange(pos)) {
            return null;
        }

        return level.getBlockEntity(pos) instanceof PowerConsumerBlockEntity device
            && worldPosition.equals(device.getFusePanel())
            && device.canBindToFusePanel() ? device : null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Power
    // ------------------------------------------------------------------------------------------------------------

    /**
     * [stated] readme: "main power cable connects to fuse panel" - at the back. [stated] Oct 3: and on the TOP and the
     * BOTTOM - the model has a port pad on top, and a cable run into either has to connect. Cables, the grid walk and
     * the wiring tool all ask this one method, so these faces work everywhere at once.
     */
    @Override
    public boolean canConnectOn(Direction direction) {
        var state = getBlockState();
        var facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
            ? state.getValue(HorizontalDirectionalBlock.FACING)
            : Direction.NORTH;

        return direction == facing.getOpposite() || direction == Direction.UP || direction == Direction.DOWN;
    }

    @Override
    public long getRequestedPower() {
        if (level == null) {
            return 0L;
        }

        var total = 0L;
        var slots = getDeviceCapacity();

        for (var bound : sentries) {
            if (slots-- <= 0) {
                return total;
            }

            var turret = sentry(level, bound);

            if (turret != null) {
                total += turret.getPanelRequest();
            }
        }

        for (var pos : devices) {
            if (slots-- <= 0) {
                return total;
            }

            var device = device(level, pos);

            if (device != null) {
                total += Math.max(0L, device.getRequestedPower());
            }
        }

        return total;
    }

    /** Hands the payment to the devices, most urgent first, and returns only what they took. */
    @Override
    public long receivePower(long maxAmount) {
        if (level == null || maxAmount <= 0L) {
            return 0L;
        }

        var remaining = maxAmount;

        // Only the devices within the panel's capacity are powered: taking out a capacity card leaves the extras listed
        // (the screen's count turns red) but unpowered - turrets first, then block devices in the order they were
        // bound.
        var slots = getDeviceCapacity();

        // Turrets first - the base's defence.
        for (var entry : sentries) {
            if (slots-- <= 0) {
                return maxAmount - remaining;
            }

            var turret = sentry(level, entry);

            if (turret != null && remaining > 0L) {
                remaining -= turret.receivePanelPower(Math.min(remaining, turret.getPanelRequest()));
            }
        }

        var bound = new ArrayList<PowerConsumerBlockEntity>(devices.size());

        for (var pos : devices) {
            if (slots-- <= 0) {
                break;
            }

            var device = device(level, pos);

            if (device != null && device.getRequestedPower() > 0L) {
                bound.add(device);
            }
        }

        bound.sort(Comparator.comparingInt(PowerConsumerBlockEntity::getPowerPriority));

        for (var device : bound) {
            var wanted = device.getRequestedPower();

            if (wanted <= remaining) {
                remaining -= Math.max(0L, device.receivePower(wanted));
            }
        }

        return maxAmount - remaining;
    }

    @Override
    public int getPowerPriority() {
        return priority.band();
    }

    public MachinePriority getPriority() {
        return priority;
    }

    /** [stated] Oct 2: the Priority side button picks a value directly. */
    public void setPriority(MachinePriority priority) {
        if (this.priority != priority) {
            this.priority = priority;
            setChanged();
        }
    }

    public void cyclePriority() {
        priority = priority.next();
        setChanged();
    }

    @Override
    public void serverTick(Level level, BlockPos blockPos, BlockState blockState) {
        super.serverTick(level, blockPos, blockState);

        // Drop devices that moved to another panel or were broken - only once they are loaded, so an unloaded device
        // keeps its place.
        if (level.getGameTime() % PRUNE_INTERVAL == 0) {
            var removed = devices.removeIf(
                pos -> level.isLoaded(pos)
                    && !(level.getBlockEntity(pos) instanceof PowerConsumerBlockEntity device && blockPos.equals(device.getFusePanel()))
            );

            // A turret that is LOADED but no longer points back was unbound elsewhere - or, after a crash, saved
            // without
            // the binding. Dropped so it cannot hold a slot for ever. A turret that is merely unloaded keeps its place.
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                removed |= sentries.removeIf(
                    bound -> serverLevel.getEntity(bound.id()) instanceof com.human.common.gameplay.entity.machine.SentryTurret turret
                        && !blockPos.equals(turret.getFusePanel())
                );
            }

            if (removed) {
                changed();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Menu and save
    // ------------------------------------------------------------------------------------------------------------

    public SimpleContainer getUpgrades() {
        return upgrades;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new FusePanelMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        var list = new net.minecraft.nbt.ListTag();

        for (var pos : devices) {
            list.add(LongTag.valueOf(pos.asLong()));
        }

        tag.put(NBT_DEVICES, list);

        var sentryList = new net.minecraft.nbt.ListTag();

        for (var sentry : sentries) {
            var entry = new CompoundTag();

            entry.putUUID("id", sentry.id());
            entry.putLong("pos", sentry.pos().asLong());
            sentryList.add(entry);
        }

        tag.put(NBT_SENTRIES, sentryList);
        tag.put(NBT_UPGRADES, upgrades.createTag(registries));
        tag.putString(NBT_PRIORITY, priority.id());
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        devices.clear();

        for (var entry : tag.getList(NBT_DEVICES, Tag.TAG_LONG)) {
            devices.add(BlockPos.of(((LongTag) entry).getAsLong()));
        }

        sentries.clear();

        for (var entry : tag.getList(NBT_SENTRIES, Tag.TAG_COMPOUND)) {
            var compound = (CompoundTag) entry;

            if (compound.hasUUID("id")) {
                sentries.add(new BoundSentry(compound.getUUID("id"), BlockPos.of(compound.getLong("pos"))));
            }
        }

        upgrades.fromTag(tag.getList(NBT_UPGRADES, Tag.TAG_COMPOUND), registries);
        priority = MachinePriority.byId(tag.getString(NBT_PRIORITY));
    }
}
