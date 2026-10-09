package com.human.common.gameplay.block.cryo;

import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.fluid.FluidEndpoint;
import com.human.common.gameplay.item.power.HubChipItem;
import com.human.common.gameplay.menu.cryo.CryotubeControllerMenu;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.registry.init.HumanBlockEntityTypes;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.HumanFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The cryotube controller's brain. It is the ONLY thing in the cryo system that ticks.
 * <h2>What it costs</h2> [stated] Sep 30 - Oct 1: a tank draws ONLY while it holds a specimen, at 1 FE/t and 0.1 mB/s
 * of cryofluid per block of tank. The cryofluid comes from the controller's own 16-bucket buffer, filled only through
 * the TOP port and only with cryofluid. One controller serves at most 3,375 tank blocks - a full 15x15x15; tanks past
 * that on the same conduit are left dark and cannot take a specimen.
 * <h2>Pause or thaw</h2> [stated] Oct 1, and the grid half agreed in the power chat: with no power, the controller asks
 * the grid whether it is PARTLY UNLOADED. If it is - a battery, cable or fuse panel in an unloaded chunk - the
 * specimens simply wait. Only a real outage on a fully loaded grid, or an empty buffer, starts the thaw. When a tank's
 * countdown runs out the specimen breaks out, awake.
 * <h2>Fuse panel</h2> With a hub chip in its slot the controller binds to a fuse panel like any machine. Bound, it asks
 * about the PANEL's grid, and an unloaded panel is itself a pause.
 */
public class CryotubeControllerBlockEntity extends PowerConsumerBlockEntity implements net.minecraft.world.MenuProvider, FluidEndpoint {

    /** [stated] Sep 30: a 16-bucket buffer. */
    public static final int CAPACITY = 16 * FluidContent.BUCKET;

    /** [stated] Sep 30: 3,375 tank blocks per controller - one full 15x15x15. */
    public static final int MAX_SERVED_BLOCKS = 15 * 15 * 15;

    /**
     * [stated] Oct 2: and never more than 8 TANKS, whichever limit comes first - otherwise 3,375 separate 1x1x1 tubes
     * would all fit under the block cap. 8 is tier 1; later tiers raise it.
     */
    public static final int MAX_SERVED_TANKS = 8;

    /** 0.1 mB per block per second is one two-hundredth of a millibucket per block per tick. */
    private static final double FUEL_PER_BLOCK_PER_TICK = 0.1 / 20.0;

    private static final int RESCAN_INTERVAL = 40;

    public static final int STATUS_IDLE = 0;

    public static final int STATUS_RUNNING = 1;

    public static final int STATUS_PAUSED = 2;

    public static final int STATUS_THAWING = 3;

    /** A real outage, carried by the backup batteries. */
    public static final int STATUS_ON_BATTERY = 4;

    /** [stated] Oct 2: backup batteries live in the controller - 3 slots for tier 1 (every current edition). */
    public static final int BATTERY_SLOTS = 3;

    private static final String NBT_BUFFER = "buffer";

    private static final String NBT_DEBT = "fuelDebt";

    private FluidContent buffer = FluidContent.EMPTY;

    private double fuelDebt;

    private final SimpleContainer hub = new SimpleContainer(1) {

        @Override
        public void setChanged() {
            super.setChanged();
            CryotubeControllerBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof HubChipItem;
        }
    };

    /**
     * Backup power. [stated] Oct 2: used ONLY in a real outage - never while the grid is merely partly unloaded, which
     * is a pause and costs nothing - so a battery is not drained by a chunk border.
     */
    private final SimpleContainer batteries = new SimpleContainer(BATTERY_SLOTS) {

        @Override
        public void setChanged() {
            super.setChanged();
            CryotubeControllerBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
            return com.human.common.gameplay.power.ItemEnergyBridge.canDischarge(stack);
        }
    };

    /** Tanks this controller serves, from the last scan. Not saved: rebuilt within two seconds of loading. */
    private final List<BlockPos> served = new ArrayList<>();

    private int servedBlocks;

    private int occupiedBlocks;

    private int status = STATUS_IDLE;

    private int rescanTimer;

    /** What a short grid offer covered this tick (see {@link #receivePower}). */
    private long partialSupply;

    // ---- [stated] Oct 2: the screen's RECENT EVENTS log - the last 20, saved with the controller. ----

    public static final int MAX_EVENTS = 20;

    private static final String NBT_EVENTS = "events";

    /** Each event: "t" (day time), "k" (what happened), "a" (its details as plain strings). */
    private final java.util.ArrayDeque<CompoundTag> events = new java.util.ArrayDeque<>();

    private int snapshotTimer;

    /** Client side only: the last screen snapshot received. */
    private @Nullable CompoundTag clientSnapshot;

    public CryotubeControllerBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.CRYOTUBE_CONTROLLER.get(), pos, state);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Power
    // ------------------------------------------------------------------------------------------------------------

    private Direction facing() {
        return getBlockState().getValue(CryotubeControllerBlock.FACING);
    }

    /** [stated] Sep 30: power in at the BACK only. */
    @Override
    public boolean canConnectOn(Direction direction) {
        return direction == facing().getOpposite();
    }

    /** Losing a specimen cannot be undone, so it is served before any convenience machine. */
    @Override
    public int getPowerPriority() {
        return PowerNode.PRIORITY_CRITICAL;
    }

    /** [stated] Oct 2: only ACTIVE tubes draw - a deactivated tube is thawing and costs nothing. */
    @Override
    public long getRequestedPower() {
        return hasFuel() ? occupiedWantingBlocks(wantingByPriority()) : 0L;
    }

    @Override
    public boolean canBindToFusePanel() {
        return hub.getItem(0).getItem() instanceof HubChipItem;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void serverTick(Level level, BlockPos blockPos, BlockState blockState) {
        if (--rescanTimer <= 0) {
            rescanTimer = RESCAN_INTERVAL;
            rescan(level);
        }

        if (--snapshotTimer <= 0 && level instanceof ServerLevel serverLevel) {
            snapshotTimer = 20;
            sendSnapshots(serverLevel);
        }

        super.serverTick(level, blockPos, blockState);

        // [stated] Oct 3: the glass frosts over while a tank runs and thaws clear at the thaw rate.
        for (var masterPos : served) {
            if (level.isLoaded(masterPos) && level.getBlockEntity(masterPos) instanceof CryotankBlockEntity tank) {
                tank.tickFrost();
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Event log
    // ------------------------------------------------------------------------------------------------------------

    /** Adds to the log. {@code key} becomes gui.avp_human.cryo.event.KEY; {@code args} fill its %s slots. */
    public void logEvent(String key, String... args) {
        if (level == null || level.isClientSide) {
            return;
        }

        var event = new CompoundTag();
        event.putLong("t", level.getDayTime());
        event.putString("k", key);
        var list = new net.minecraft.nbt.ListTag();

        for (var arg : args) {
            list.add(net.minecraft.nbt.StringTag.valueOf(arg == null ? "?" : arg));
        }

        event.put("a", list);
        events.addFirst(event);

        while (events.size() > MAX_EVENTS) {
            events.removeLast();
        }

        setChanged();
        sendSnapshotSoon();
    }

    /** "TUBE 03" - the tube's place in this controller's list, or "TUBE ?" for one it no longer serves. */
    public String tubeLabel(BlockPos masterPos) {
        var index = served.indexOf(masterPos);
        return index < 0 ? "TUBE ?" : String.format("TUBE %02d", index + 1);
    }

    private static String specimenName(CryotankBlockEntity tank) {
        var id = tank.specimenTypeId();

        if (id == null) {
            return "UNKNOWN";
        }

        return EntityType.byString(id)
            .map(type -> type.getDescription().getString())
            .orElse(id);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Screen snapshot
    // ------------------------------------------------------------------------------------------------------------

    /** Sends on the next tick rather than waiting for the regular refresh - after an action, so the screen reacts. */
    public void sendSnapshotSoon() {
        snapshotTimer = 0;
    }

    /**
     * Everything the CRYOLINK screen shows, as one tag. Sent ONLY to players who have this controller's screen open -
     * once a second, and straight after anything changes - so a closed controller costs nothing.
     */
    private CompoundTag snapshot() {
        var tag = new CompoundTag();
        tag.putInt("status", status);
        tag.putInt("fuel", buffer.amount());
        tag.putInt("tubeCap", MAX_SERVED_TANKS);
        tag.putLong("draw", getRequestedPower());
        tag.putDouble("usage", getRequestedPower() * FUEL_PER_BLOCK_PER_TICK * 20.0);
        tag.putBoolean("gridSupplied", hasPowerSupply());
        tag.putBoolean("gridPartly", level != null && isPartlyUnloaded(level));

        var charge = 0.0F;
        var cells = 0;

        for (int slot = 0; slot < BATTERY_SLOTS; slot++) {
            var fraction = com.human.common.gameplay.power.ItemEnergyBridge.chargeFraction(batteries.getItem(slot));

            if (fraction >= 0.0F) {
                charge += fraction;
                cells++;
            }
        }

        tag.putInt("cells", cells);
        tag.putFloat("charge", cells == 0 ? 0.0F : charge / cells);

        var tubes = new net.minecraft.nbt.ListTag();

        for (int index = 0; index < served.size(); index++) {
            var tube = new CompoundTag();
            tube.putInt("n", index + 1);
            var masterPos = served.get(index);

            if (level != null && level.isLoaded(masterPos) && level.getBlockEntity(masterPos) instanceof CryotankBlockEntity tank) {
                tube.putString("size", tank.sizeX() + "x" + tank.sizeZ() + "x" + tank.sizeY());
                tube.putInt("blocks", tank.blockCount());
                tube.putInt("priority", tank.getPriority());
                tube.putInt("state", tubeState(tank));
                tube.putInt("thaw", tank.hasSpecimen() ? tank.getThawTicks() * 100 / Math.max(1, tank.thawTicksRequired()) : 0);

                if (tank.hasSpecimen()) {
                    tube.putString("specimen", tank.specimenTypeId() == null ? "" : tank.specimenTypeId());

                    if (tank.getCard() == null && level instanceof ServerLevel) {
                        // A specimen frozen before cards existed gets its sheet now, once.
                        var rebuilt = SpecimenCard.rebuild(level, tank.getSpecimen());

                        if (rebuilt != null) {
                            tank.setCard(rebuilt);
                        }
                    }

                    if (tank.getCard() != null) {
                        tube.put("card", tank.getCard().copy());
                    }
                }
            } else {
                tube.putInt("state", TUBE_UNLOADED);
            }

            tubes.add(tube);
        }

        tag.put("tubes", tubes);

        var log = new net.minecraft.nbt.ListTag();
        log.addAll(events);
        tag.put("events", log);
        return tag;
    }

    public static final int TUBE_STANDBY = 0;

    public static final int TUBE_ACTIVE = 1;

    public static final int TUBE_THAWING = 2;

    public static final int TUBE_DEACTIVATED = 3;

    public static final int TUBE_PAUSED = 4;

    public static final int TUBE_UNLOADED = 5;

    /** [stated] Oct 2: empty and connected = STANDBY (ready to take a specimen); occupied = ACTIVE. */
    private int tubeState(CryotankBlockEntity tank) {
        if (!tank.hasSpecimen()) {
            return TUBE_STANDBY;
        }

        if (tank.isDeactivated()) {
            return TUBE_DEACTIVATED;
        }

        if (status == STATUS_PAUSED && tank.getThawTicks() == 0) {
            return TUBE_PAUSED;
        }

        return tank.getThawTicks() > 0 ? TUBE_THAWING : TUBE_ACTIVE;
    }

    private void sendSnapshots(ServerLevel level) {
        net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> packet = null;

        for (var player : level.players()) {
            if (
                player.containerMenu instanceof com.human.common.gameplay.menu.cryo.CryotubeControllerMenu menu
                    && menu.isFor(this)
            ) {
                if (packet == null) {
                    var wrapper = new CompoundTag();
                    wrapper.put("cryolink", snapshot());
                    packet = net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this, (be, registries) -> wrapper);
                }

                player.connection.send(packet);
            }
        }
    }

    public @Nullable CompoundTag getClientSnapshot() {
        return clientSnapshot;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Actions from the screen
    // ------------------------------------------------------------------------------------------------------------

    /**
     * [stated] Oct 2: deactivating an occupied tube starts its thaw; deactivating again before it finishes reactivates
     * it and the thaw is cancelled.
     */
    public void toggleDeactivated(int index) {
        var tank = servedTank(index);

        if (tank == null || !tank.hasSpecimen()) {
            return;
        }

        var deactivate = !tank.isDeactivated();
        tank.setDeactivated(deactivate);

        if (!deactivate) {
            tank.setThawTicks(0);
            logEvent("reactivated", tubeLabel(tank.getBlockPos()), specimenName(tank));
        }

        sendSnapshotSoon();
    }

    public void changePriority(int index, int delta) {
        var tank = servedTank(index);

        if (tank != null) {
            tank.setPriority(tank.getPriority() + delta);
            sendSnapshotSoon();
        }
    }

    /** The next tick re-walks the conduit - after a capture, so the new specimen is billed at once. */
    public void requestRescan() {
        rescanTimer = 0;
    }

    private void rescan(Level level) {
        served.clear();
        servedBlocks = 0;
        occupiedBlocks = 0;

        for (var masterPos : CryoNetwork.tanksFrom(level, worldPosition)) {
            // ⚠ The conduit only reaches LOADED tube blocks, but a big tube's master can sit in the next chunk over.
            // Reading it would load that chunk from a tick. A tube left out of a rescan is not billed, paused or
            // thawed - it simply waits, untouched, until its chunk is back.
            if (!level.isLoaded(masterPos) || !(level.getBlockEntity(masterPos) instanceof CryotankBlockEntity tank)) {
                continue;
            }

            // Tanks past either cap stay dark: not billed, not thawed, cannot take a specimen.
            if (served.size() >= MAX_SERVED_TANKS) {
                break;
            }

            if (servedBlocks + tank.blockCount() > MAX_SERVED_BLOCKS) {
                continue;
            }

            if (!claim(level, tank)) {
                continue;
            }

            served.add(masterPos);
            servedBlocks += tank.blockCount();

            if (tank.hasSpecimen()) {
                occupiedBlocks += tank.blockCount();
            }
        }
    }

    /**
     * One tank, one controller. A tank another controller already serves is left to it - unless that controller is
     * GONE. ⚠ An owner in an unloaded chunk is NOT gone: taking its tanks would hand them back and forth every time a
     * chunk loads, so they are left alone until it can be checked.
     */
    private boolean claim(Level level, CryotankBlockEntity tank) {
        var owner = tank.getOwner();

        if (owner == null || owner.equals(worldPosition)) {
            tank.setOwner(worldPosition);
            return true;
        }

        if (level.isLoaded(owner) && !(level.getBlockEntity(owner) instanceof CryotubeControllerBlockEntity)) {
            tank.setOwner(worldPosition);
            return true;
        }

        return false;
    }

    @Override
    public void poweredTick(Level level, BlockPos blockPos, BlockState blockState) {
        process(level, Long.MAX_VALUE);
    }

    @Override
    public void unpoweredTick(Level level, BlockPos blockPos, BlockState blockState) {
        var supply = partialSupply;
        partialSupply = 0L;
        process(level, supply);
    }

    /**
     * ⚠ The base consumer takes power all-or-nothing. [stated] Oct 2 tube PRIORITY needs the other case: when the grid
     * can only cover part of the draw, it covers whole tubes in priority order (P1 first) and the rest fall to the
     * batteries, then thaw. So a short offer is taken in part here, and the next tick shares it out.
     */
    @Override
    public long receivePower(long maxAmount) {
        var wanted = getRequestedPower();

        if (wanted <= 0L || maxAmount >= wanted) {
            return super.receivePower(maxAmount);
        }

        long covered = 0L;

        for (var tank : wantingByPriority()) {
            if (covered + tank.blockCount() <= maxAmount) {
                covered += tank.blockCount();
            }
        }

        partialSupply = covered;
        return covered;
    }

    /** Occupied, NOT deactivated, loaded tanks - the ones that want keeping - highest priority (P1) first. */
    private List<CryotankBlockEntity> wantingByPriority() {
        var wanting = new ArrayList<CryotankBlockEntity>();

        if (level == null) {
            return wanting;
        }

        for (var masterPos : served) {
            if (
                level.isLoaded(masterPos)
                    && level.getBlockEntity(masterPos) instanceof CryotankBlockEntity tank
                    && tank.hasSpecimen()
                    && !tank.isDeactivated()
            ) {
                wanting.add(tank);
            }
        }

        wanting.sort(java.util.Comparator.comparingInt(CryotankBlockEntity::getPriority));
        return wanting;
    }

    /**
     * One tick of keeping the tubes. {@code gridSupply} is how much of the draw the grid covered - all of it, part of
     * it, or nothing.
     */
    private void process(Level level, long gridSupply) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        var wanting = wantingByPriority();
        var fuelled = hasFuel();
        var remaining = gridSupply;
        var onBattery = false;
        var anyPaused = false;
        var anyThawing = false;
        long kept = 0L;

        // Paused when the grid falls SHORT while part of it is unloaded - then the tubes it could not cover neither
        // thaw nor drain a battery. [stated] Oct 1 + the power chat's grid rule. ⚠ Short, not only zero: with one
        // generator loaded and another in an unloaded chunk the grid still sends SOME power, and the tubes it cannot
        // cover must wait for the rest, not start thawing. Asked only when short, so a fed controller costs nothing.
        var paused = fuelled
            && gridSupply < occupiedWantingBlocks(wanting)
            && isPartlyUnloaded(level);

        for (var tank : wanting) {
            var covered = false;

            if (fuelled && tank.blockCount() <= remaining) {
                remaining -= tank.blockCount();
                covered = true;
            } else if (fuelled && !paused && drawFromBatteries(tank.blockCount())) {
                covered = true;
                onBattery = true;
            }

            if (covered) {
                kept += tank.blockCount();
                tank.setThawTicks(0);
            } else if (paused) {
                anyPaused = true;
            } else {
                anyThawing = true;
                thawStep(serverLevel, tank, false);
            }
        }

        // [stated] Oct 2: a DEACTIVATED tube thaws whatever the power is doing, and lets its specimen out when done.
        for (var masterPos : served) {
            if (
                level.isLoaded(masterPos)
                    && level.getBlockEntity(masterPos) instanceof CryotankBlockEntity tank
                    && tank.hasSpecimen()
                    && tank.isDeactivated()
            ) {
                thawStep(serverLevel, tank, true);
            }
        }

        burnFuel(kept);

        var newStatus = wanting.isEmpty()
            ? STATUS_IDLE
            : anyThawing
                ? STATUS_THAWING
                : anyPaused
                    ? STATUS_PAUSED
                    : onBattery
                        ? STATUS_ON_BATTERY
                        : STATUS_RUNNING;

        noteTransitions(newStatus);
        setStatus(newStatus);
    }

    private static long occupiedWantingBlocks(List<CryotankBlockEntity> wanting) {
        long total = 0L;

        for (var tank : wanting) {
            total += tank.blockCount();
        }

        return total;
    }

    private void burnFuel(long blocks) {
        if (blocks <= 0L) {
            return;
        }

        fuelDebt += blocks * FUEL_PER_BLOCK_PER_TICK;
        var whole = (int) fuelDebt;

        if (whole > 0) {
            fuelDebt -= whole;
            buffer = buffer.withAmount(Math.max(0, buffer.amount() - whole));
            setChanged();
        }
    }

    /**
     * One tick of thaw. At the end the specimen comes out: a deliberate DEACTIVATION lets it out calm, a power or
     * cryofluid failure breaks it out awake.
     */
    private void thawStep(ServerLevel level, CryotankBlockEntity tank, boolean deliberate) {
        var ticks = tank.getThawTicks() + 1;

        if (ticks == 1) {
            logEvent(deliberate ? "deactivated" : "thawing", tubeLabel(tank.getBlockPos()), specimenName(tank));
        }

        if (ticks < tank.thawTicksRequired()) {
            tank.setThawTicks(ticks);
            return;
        }

        var name = specimenName(tank);

        if (CryoSpecimens.releaseFromTank(level, tank, null, !deliberate)) {
            logEvent(deliberate ? "released" : "broke_out", tubeLabel(tank.getBlockPos()), name);
            tank.setSpecimen(null, 1.0F, 0.0F, 0.0F);
            requestRescan();
            sendSnapshotSoon();
        } else {
            // ⚠ A failed release (a UUID clash, a damaged save) must not be retried every tick - each try builds a
            // whole entity. Hold the specimen and try again in five seconds.
            tank.setThawTicks(Math.max(0, tank.thawTicksRequired() - 100));
        }
    }

    private boolean wasOnBattery;

    private boolean lowFuelLatched;

    /** Events for the log that come from the controller's own state changing. */
    private void noteTransitions(int newStatus) {
        var onBattery = newStatus == STATUS_ON_BATTERY;

        if (onBattery && !wasOnBattery) {
            logEvent("on_battery");
        } else if (!onBattery && wasOnBattery && newStatus == STATUS_RUNNING) {
            logEvent("power_restored");
        }

        wasOnBattery = onBattery;

        var low = buffer.amount() < CAPACITY / 10;

        if (low && !lowFuelLatched && occupiedBlocks > 0) {
            logEvent(buffer.amount() <= 0 ? "fuel_empty" : "fuel_low");
        }

        lowFuelLatched = low;
    }

    /**
     * The power chat's rule: cabled, ask about this controller's own grid; bound to a fuse panel, an unloaded panel is
     * a pause and otherwise ask about the PANEL's grid.
     */
    /** True only if the batteries together supplied the full {@code amount} this tick. */
    private boolean drawFromBatteries(long amount) {
        var remaining = amount;

        for (int slot = 0; slot < BATTERY_SLOTS && remaining > 0L; slot++) {
            if (batteries.getItem(slot).isEmpty()) {
                continue;
            }

            final int index = slot;
            var stackSlot = new com.human.common.gameplay.power.StackSlot(
                () -> batteries.getItem(index),
                updated -> batteries.setItem(index, updated)
            );

            remaining -= com.human.common.gameplay.power.ItemEnergyBridge.discharge(stackSlot, remaining);
        }

        return remaining <= 0L;
    }

    private boolean isPartlyUnloaded(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        var panel = getFusePanel();

        if (panel != null) {
            if (!level.isLoaded(panel)) {
                return true;
            }

            return PowerSystem.get(serverLevel).isGridPartlyUnloaded(level, panel);
        }

        return PowerSystem.get(serverLevel).isGridPartlyUnloaded(level, worldPosition);
    }

    private void setStatus(int status) {
        if (status == this.status) {
            return;
        }

        var wasActive = this.status == STATUS_RUNNING || this.status == STATUS_ON_BATTERY;
        var nowActive = status == STATUS_RUNNING || status == STATUS_ON_BATTERY;
        this.status = status;

        // [stated] Oct 2: the terminal's power-up / power-down, when the controller starts or stops keeping tanks.
        if (wasActive != nowActive && level != null && !level.isClientSide) {
            var sound = nowActive
                ? com.human.common.registry.init.HumanSoundEvents.CRYO_CONTROLLER_ACTIVATE
                : com.human.common.registry.init.HumanSoundEvents.CRYO_CONTROLLER_DEACTIVATE;
            level.playSound(null, worldPosition, sound.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.0F);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the tanks ask
    // ------------------------------------------------------------------------------------------------------------

    public boolean hasFuel() {
        return !buffer.isEmpty() && buffer.amount() > 0;
    }

    /** Whether the grid feeding this controller has anything to give right now. Used to refuse a capture. */
    public boolean hasPowerSupply() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        var panel = getFusePanel();
        var feedPos = panel != null ? panel : worldPosition;

        if (!level.isLoaded(feedPos)) {
            return false;
        }

        var grid = PowerSystem.get(serverLevel).gridAt(feedPos);
        return grid != null && grid.hasSupply();
    }

    /** Whether this tank is within the 8-tank and 3,375-block caps - true for any tank the last scan already served. */
    public boolean canServe(CryotankBlockEntity tank) {
        return served.contains(tank.getBlockPos())
            || served.size() < MAX_SERVED_TANKS && servedBlocks + tank.blockCount() <= MAX_SERVED_BLOCKS;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Screen
    // ------------------------------------------------------------------------------------------------------------

    public SimpleContainer getHub() {
        return hub;
    }

    public SimpleContainer getBatteries() {
        return batteries;
    }

    public int getStatus() {
        return status;
    }

    public int getFuelAmount() {
        return buffer.amount();
    }

    public int getServedCount() {
        return served.size();
    }

    public int getOccupiedBlocks() {
        return occupiedBlocks;
    }

    public @Nullable CryotankBlockEntity servedTank(int index) {
        if (level == null || index < 0 || index >= served.size()) {
            return null;
        }

        var pos = served.get(index);
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof CryotankBlockEntity tank ? tank : null;
    }

    /**
     * Raw entity-type id + 1 of a tank's specimen, 0 when empty, or -1 for a specimen whose mob no longer exists (its
     * mod was removed) - still shown, and still releasable, rather than passed off as an empty tank.
     */
    public static int specimenNetworkId(@Nullable CryotankBlockEntity tank) {
        if (tank == null || !tank.hasSpecimen()) {
            return 0;
        }

        var id = tank.specimenTypeId();

        return id == null ? -1 : EntityType.byString(id).map(type -> BuiltInRegistries.ENTITY_TYPE.getId(type) + 1).orElse(-1);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new CryotubeControllerMenu(containerId, inventory, this);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Fluid - receive only, cryofluid only, top only
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public FluidContent getContent() {
        return buffer;
    }

    @Override
    public int getCapacity() {
        return CAPACITY;
    }

    private static boolean isCryofluid(Fluid fluid) {
        return fluid != null && fluid.isSame(HumanFluids.CRYOFLUID.get());
    }

    @Override
    public int insert(Fluid fluid, int maxAmount, boolean simulate) {
        if (!isCryofluid(fluid) || maxAmount <= 0) {
            return 0;
        }

        var accepted = Math.min(maxAmount, CAPACITY - buffer.amount());

        if (accepted > 0 && !simulate) {
            buffer = FluidContent.of(HumanFluids.CRYOFLUID.get(), buffer.amount() + accepted);
            notifyContentChanged();
        }

        return Math.max(0, accepted);
    }

    /** [stated] Oct 1: the top port is RECEIVE-ONLY. Nothing ever comes back out. */
    @Override
    public int extract(@Nullable Fluid fluid, int maxAmount, boolean simulate) {
        return 0;
    }

    @Override
    public void setContentSilently(FluidContent content) {
        buffer = content != null && isCryofluid(content.fluid()) ? content : FluidContent.EMPTY;
    }

    @Override
    public void notifyContentChanged() {
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** A null side is a bucket or canister in the player's hand - allowed from any face. */
    @Override
    public boolean canFill(@Nullable Direction side) {
        return side == null || side == Direction.UP;
    }

    @Override
    public boolean canDrain(@Nullable Direction side) {
        return false;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Persistence - the buffer and the hub chip survive a break, like every power machine's contents
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        // Only cryofluid is ever held, so the amount alone is the whole buffer.
        tag.putInt(NBT_BUFFER, buffer.isEmpty() ? 0 : buffer.amount());

        tag.putDouble(NBT_DEBT, fuelDebt);
        tag.put("hub", hub.createTag(registries));
        tag.put("batteries", batteries.createTag(registries));

        var log = new net.minecraft.nbt.ListTag();
        log.addAll(events);
        tag.put(NBT_EVENTS, log);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        // A screen snapshot is ONLY that - it must not be read as the controller's saved state, or the client copy's
        // fields would be wiped by the keys it does not carry.
        if (tag.contains("cryolink")) {
            clientSnapshot = tag.getCompound("cryolink");
            return;
        }

        super.loadAdditional(tag, registries);

        events.clear();

        for (var event : tag.getList(NBT_EVENTS, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            if (event instanceof CompoundTag compound && events.size() < MAX_EVENTS) {
                events.addLast(compound);
            }
        }

        var amount = Math.max(0, Math.min(CAPACITY, tag.getInt(NBT_BUFFER)));
        buffer = amount > 0 ? FluidContent.of(HumanFluids.CRYOFLUID.get(), amount) : FluidContent.EMPTY;
        fuelDebt = tag.getDouble(NBT_DEBT);

        if (tag.contains("hub")) {
            hub.fromTag(tag.getList("hub", net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
        }

        if (tag.contains("batteries")) {
            batteries.fromTag(tag.getList("batteries", net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NotNull Builder components) {
        super.collectImplicitComponents(components);

        if (!buffer.isEmpty()) {
            components.set(HumanDataComponents.FLUID_CONTENT.get(), buffer);
        }

        // Hub chip first, then the batteries - all of it rides on the dropped controller.
        var held = new ArrayList<ItemStack>();
        held.add(hub.getItem(0).copy());

        for (int slot = 0; slot < BATTERY_SLOTS; slot++) {
            held.add(batteries.getItem(slot).copy());
        }

        if (held.stream().anyMatch(stack -> !stack.isEmpty())) {
            components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(held));
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.@NotNull DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);

        var content = componentInput.get(HumanDataComponents.FLUID_CONTENT.get());

        if (content != null) {
            setContentSilently(content);
        }

        var contents = componentInput.get(DataComponents.CONTAINER);

        if (contents != null) {
            var items = NonNullList.withSize(1 + BATTERY_SLOTS, ItemStack.EMPTY);
            contents.copyInto(items);
            hub.setItem(0, items.get(0));

            for (int slot = 0; slot < BATTERY_SLOTS; slot++) {
                batteries.setItem(slot, items.get(1 + slot));
            }
        }
    }
}
