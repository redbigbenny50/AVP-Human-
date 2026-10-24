package com.human.common.gameplay.fabrication;

import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.item.pipe.PipeItemAccess;
import com.human.common.gameplay.machine.MachinePriority;
import com.human.common.gameplay.machine.MachineUpgrades;
import com.human.common.gameplay.storage.ContentsRules;
import com.human.common.registry.init.HumanFabrication;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The fabricator's core. [stated] Sep 22 / Oct 13: it prints anything it has a blueprint for - its own 16 blueprint
 * slots (LOCAL) or the drive bays on its power grid (NETWORK) - from its own storage plus every inventory touching it
 * or on a pipe network touching it, using finished parts as shortcuts. Faction items need that faction's license.
 * <h2>WHEN THINGS ARE USED</h2> Nothing is taken when a print starts. When it finishes, the blueprint, license, output
 * space and materials are all checked again; connected storage is pulled first, and if any pull comes up short
 * everything already pulled goes back. Only then is internal storage committed. Taking something away mid-print stops
 * the print and costs nothing.
 * <h2>MACHINE STANDARD</h2> Four upgrade slots (speed, efficiency, redstone - the machines' own rules in
 * {@link MachineUpgrades}), a priority, and a redstone mode used only while a redstone card is fitted.
 * <h2>COST</h2> Power only while printing with room for the result. Idle, nothing but the power base's boolean.
 * Planning and connected-storage reads happen at the start and the end of a print, and twice a second while a screen is
 * open - never per tick.
 * <h2>DATA SAFETY</h2> Every change calls {@link #setChanged()}; unloading touches nothing; breaking keeps everything
 * on the one dropped item; nothing pulled from storage is ever deleted - if it cannot go back it stays in the
 * fabricator, and failing that drops at its feet.
 */
public class FabricatorBlockEntity extends PowerConsumerBlockEntity {

    /** [stated] Oct 16: 48 internal slots (was 18) - 6 rows of 8 on the new STORAGE view, 4 showing at a time. */
    public static final int STORAGE_SLOTS = 48;

    /**
     * ⚠ SLOT NUMBERS ARE KEPT WHERE THEY WERE. The first 18 storage slots stay 0-17 and the blueprints, output and
     * upgrades keep 18-38; the 30 new storage slots go AFTER them, from {@link #FIRST_EXTRA_STORAGE}. A fabricator
     * picked up before this change carries its contents in that numbering (the item's container data), and would
     * otherwise put its blueprint chips and upgrade cards into storage slots when placed again.
     */
    public static final int FIRST_BLUEPRINT = 18;

    public static final int BLUEPRINT_SLOTS = 16;

    public static final int UPGRADE_SLOTS = com.human.common.gameplay.power.upgrade.UpgradeCard.SLOTS;

    public static final int OUTPUT_SLOT = FIRST_BLUEPRINT + BLUEPRINT_SLOTS;

    public static final int FIRST_UPGRADE = OUTPUT_SLOT + 1;

    /** Where storage slots 18-47 sit in the slot numbering (after the upgrades). */
    public static final int FIRST_EXTRA_STORAGE = FIRST_UPGRADE + UPGRADE_SLOTS;

    public static final int ALL_SLOTS = FIRST_EXTRA_STORAGE + STORAGE_SLOTS - FIRST_BLUEPRINT;

    /** The slot number of internal storage slot {@code index} (0-47). */
    public static int storageSlot(int index) {
        return index < FIRST_BLUEPRINT ? index : FIRST_EXTRA_STORAGE + index - FIRST_BLUEPRINT;
    }

    public static final long PRINT_POWER = 200L;

    private static final int BASE_TICKS = 80;

    private static final int TICKS_PER_STEP = 20;

    private static final int READ_REFRESH_TICKS = 20;

    public static final int MAX_QUEUE = 64;

    private static final int MAX_PULL_ATTEMPTS = 256;

    /**
     * Most distinct item lines read from connected storage in one look. A huge modded storage can list tens of
     * thousands; planning and the screen only need what is there in quantity, and this keeps one look from becoming a
     * lag spike.
     */
    public static final int MAX_CONNECTED_LINES = 4096;

    public enum Status {
        IDLE,
        PRINTING,
        NO_POWER,
        OUTPUT_FULL,
        MISSING,
        NO_BLUEPRINT,
        REDSTONE
    }

    private final NonNullList<ItemStack> storage = NonNullList.withSize(STORAGE_SLOTS, ItemStack.EMPTY);

    private final NonNullList<ItemStack> blueprints = NonNullList.withSize(BLUEPRINT_SLOTS, ItemStack.EMPTY);

    private final NonNullList<ItemStack> upgrades = NonNullList.withSize(UPGRADE_SLOTS, ItemStack.EMPTY);

    private final Container upgradeView = new ListContainer(upgrades);

    private ItemStack output = ItemStack.EMPTY;

    private @Nullable Item target;

    private int queued;

    private int progress;

    private int required;

    private Status status = Status.IDLE;

    private MachinePriority priority = MachinePriority.DEFAULT;

    private MachineUpgrades.RedstoneMode redstoneMode = MachineUpgrades.RedstoneMode.IGNORE;

    private double drawMultiplier = 1.0;

    private boolean redstoneCard;

    /** Not saved: which bay slot the running print reads from, refreshed at start and finish. */
    private @Nullable Printable source;

    public FabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(HumanFabrication.FABRICATOR_BLOCK_ENTITY.get(), pos, state);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Slots: 0-17 storage, 18-33 blueprints, 34 output, 35-38 upgrades, 39-68 storage 18-47
    // ------------------------------------------------------------------------------------------------------------

    public ItemStack getSlot(int slot) {
        if (slot < FIRST_BLUEPRINT) {
            return storage.get(slot);
        }

        if (slot < OUTPUT_SLOT) {
            return blueprints.get(slot - FIRST_BLUEPRINT);
        }

        if (slot == OUTPUT_SLOT) {
            return output;
        }

        return slot < FIRST_EXTRA_STORAGE ? upgrades.get(slot - FIRST_UPGRADE) : storage.get(FIRST_BLUEPRINT + slot - FIRST_EXTRA_STORAGE);
    }

    public void setSlot(int slot, ItemStack stack) {
        if (slot < FIRST_BLUEPRINT) {
            storage.set(slot, stack);
        } else if (slot < OUTPUT_SLOT) {
            blueprints.set(slot - FIRST_BLUEPRINT, stack);
        } else if (slot == OUTPUT_SLOT) {
            output = stack;
        } else if (slot < FIRST_EXTRA_STORAGE) {
            upgrades.set(slot - FIRST_UPGRADE, stack);
            recountUpgrades();
        } else {
            storage.set(FIRST_BLUEPRINT + slot - FIRST_EXTRA_STORAGE, stack);
        }

        setChanged();
    }

    public static boolean isBlueprintSlotItem(ItemStack stack) {
        return DriveBayBlockEntity.isBlueprint(stack);
    }

    public boolean mayPlaceUpgrade(int upgradeSlot, ItemStack stack) {
        return MachineUpgrades.mayPlace(upgradeView, upgradeSlot, stack);
    }

    private void recountUpgrades() {
        drawMultiplier = MachineUpgrades.drawMultiplier(upgradeView);
        redstoneCard = MachineUpgrades.hasRedstone(upgradeView);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Settings - the machine standard
    // ------------------------------------------------------------------------------------------------------------

    public MachinePriority priority() {
        return priority;
    }

    public void setPriority(MachinePriority priority) {
        this.priority = priority;
        setChanged();
    }

    public MachineUpgrades.RedstoneMode redstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(MachineUpgrades.RedstoneMode mode) {
        this.redstoneMode = mode;
        setChanged();
    }

    public boolean hasRedstoneCard() {
        return redstoneCard;
    }

    private boolean redstoneAllows() {
        return !redstoneCard || level == null || redstoneMode.allows(level.hasNeighborSignal(worldPosition));
    }

    /** Power drawn per tick while printing, after speed and efficiency cards. */
    public long printPower() {
        return Math.max(1L, Math.round(PRINT_POWER * drawMultiplier));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Blueprints
    // ------------------------------------------------------------------------------------------------------------

    /** One printable item and where its blueprint is: a local slot (bay null) or a drive bay slot. */
    public record Printable(
        Item item,
        boolean network,
        @Nullable DriveBayBlockEntity bay,
        int slot
    ) {}

    /** Everything printable now, LOCAL first, then by item id. Faction items only while their license is available. */
    public Map<Item, Printable> printables(ServerLevel level) {
        var found = new LinkedHashMap<Item, Printable>();
        var licences = new java.util.EnumMap<Faction, Printable>(Faction.class);

        for (int slot = 0; slot < BLUEPRINT_SLOTS; slot++) {
            collect(blueprints.get(slot), false, null, slot, found, licences);
        }

        for (var bay : DriveBayBlockEntity.onGridOf(level, worldPosition)) {
            for (var entry : bay.usableStacks()) {
                collect(entry.stack(), true, bay, entry.slot(), found, licences);
            }
        }

        for (var licence : licences.entrySet()) {
            for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(licence.getKey().items())) {
                var at = licence.getValue();

                found.putIfAbsent(holder.value(), new Printable(holder.value(), at.network(), at.bay(), at.slot()));
            }
        }

        found.entrySet().removeIf(entry -> {
            var faction = Faction.owning(new ItemStack(entry.getKey()));

            return (faction != null && !licences.containsKey(faction)) || !FabricatorPlanner.canMake(level, entry.getKey());
        });

        var sorted = new LinkedHashMap<Item, Printable>();

        found.entrySet()
            .stream()
            .sorted((a, b) -> {
                if (a.getValue().network() != b.getValue().network()) {
                    return a.getValue().network() ? 1 : -1;
                }

                return BuiltInRegistries.ITEM.getKey(a.getKey()).compareTo(BuiltInRegistries.ITEM.getKey(b.getKey()));
            })
            .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));

        return sorted;
    }

    private static void collect(
        ItemStack stack,
        boolean network,
        @Nullable DriveBayBlockEntity bay,
        int slot,
        Map<Item, Printable> found,
        Map<Faction, Printable> licences
    ) {
        if (stack.getItem() instanceof FactionLicenseItem licence) {
            licences.putIfAbsent(licence.faction(), new Printable(stack.getItem(), network, bay, slot));
            return;
        }

        var item = BlueprintChipItem.printed(stack);

        if (item != null) {
            found.putIfAbsent(item, new Printable(item, network, bay, slot));
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Connected storage
    // ------------------------------------------------------------------------------------------------------------

    /** One item in one connected inventory, with its count (which may be more than a stack). */
    public record Stored(
        PipeItemAccess.Port port,
        ItemStack stack
    ) {}

    /** Everything in connected storage, each item once per inventory. */
    public List<Stored> connectedContents(ServerLevel level) {
        var entries = new ArrayList<Stored>();

        for (var port : FabricatorSources.connected(level, this)) {
            // Grouped by item first, so a big inventory only compares components among copies of the same item.
            var byItem = new LinkedHashMap<Item, List<ItemStack>>();

            var lines = new int[] { entries.size() };

            port.listContents((stack, count) -> {
                if (lines[0] >= MAX_CONNECTED_LINES) {
                    return;
                }

                var same = byItem.computeIfAbsent(stack.getItem(), item -> new ArrayList<>());

                for (var held : same) {
                    if (ItemStack.isSameItemSameComponents(held, stack)) {
                        held.setCount((int) Math.min(Integer.MAX_VALUE, (long) held.getCount() + count));
                        return;
                    }
                }

                same.add(stack.copyWithCount(count));
                lines[0]++;
            });

            byItem.values().forEach(list -> list.forEach(stack -> entries.add(new Stored(port, stack))));
        }

        return entries;
    }

    public FabricatorPlanner.Plan plan(ServerLevel level, Item item) {
        return plan(level, item, connectedContents(level));
    }

    public FabricatorPlanner.Plan plan(ServerLevel level, Item item, List<Stored> connected) {
        var network = new ArrayList<ItemStack>(connected.size());

        connected.forEach(entry -> network.add(entry.stack()));

        return FabricatorPlanner.plan(level, item, storage, network);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Printing
    // ------------------------------------------------------------------------------------------------------------

    public @Nullable Item target() {
        return target;
    }

    public int queued() {
        return queued;
    }

    public Status status() {
        return status;
    }

    public int progressPercent() {
        return required <= 0 ? 0 : Math.min(100, progress * 100 / required);
    }

    /** Ticks left on the current print. */
    public int ticksLeft() {
        return Math.max(0, required - progress);
    }

    public boolean isPrinting() {
        return target != null && queued > 0;
    }

    /**
     * Whether the print is actually MOVING - a job queued AND power arriving. {@link #isPrinting} only says a job is
     * queued.
     * <p>
     * 🚨 Oct 23 - [stated] "The fabricator appears to be printing items when you press print even though it has no
     * power ... when it has no power it just plays the loop for printing." The client drew everything from
     * isPrinting(): the printing loop, the head's sparks, and its own tick-by-tick build of the bed - while the server,
     * with no power, never advanced a tick of it. So an unpowered fabricator looked busy and printed nothing. The
     * server now reports whether power is actually reaching the print, and every visual follows that instead. Read on
     * the client from the last update; on the server it is current.
     * </p>
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * A grid that delivers on some ticks and not others (a brownout, a shared supply) would otherwise flip the
     * animation - and send a block update - every tick. The print counts as running until this many ticks pass without
     * power.
     */
    private static final int RUNNING_GRACE_TICKS = 10;

    private boolean running;

    private long lastPoweredTick = Long.MIN_VALUE;

    private void updateRunning(Level level) {
        var now = isPrinting() && level.getGameTime() - lastPoweredTick <= RUNNING_GRACE_TICKS;

        if (now != running) {
            running = now;
            setChanged();
            sync();
        }
    }

    public boolean start(ServerLevel level, Item item, int count) {
        var printable = printables(level).get(item);

        if (printable == null) {
            status = Status.NO_BLUEPRINT;
            return false;
        }

        var plan = plan(level, item);

        if (!plan.printable()) {
            status = Status.MISSING;
            return false;
        }

        target = item;
        queued = Math.max(1, Math.min(MAX_QUEUE, count));
        progress = 0;
        required = MachineUpgrades.effectiveTicks(upgradeView, BASE_TICKS + TICKS_PER_STEP * plan.steps());
        source = printable;
        status = Status.PRINTING;
        setChanged();
        sync();

        return true;
    }

    public void stop() {
        stopWith(Status.IDLE);
    }

    private boolean outputHasRoom() {
        if (target == null) {
            return false;
        }

        // ⚠ Oct 21 - room for a whole print's batch (see FabricatorPlanner.batchSize), not just one more item.
        var batch = level == null ? 1 : FabricatorPlanner.batchSize(level, target);

        return output.isEmpty() || (output.is(target) && output.getCount() + batch <= output.getMaxStackSize() && !ContentsRules
            .holdsContents(
                output
            ));
    }

    @Override
    public long getRequestedPower() {
        return isPrinting() && outputHasRoom() && redstoneAllows() ? printPower() : 0L;
    }

    @Override
    public int getPowerPriority() {
        return priority.band();
    }

    /** [stated] Sep 29: machines take power at the BACK - the core block's back face. */
    @Override
    public boolean canConnectOn(Direction direction) {
        var state = getBlockState();

        return state.hasProperty(FabricatorBlock.FACING) && direction == state.getValue(FabricatorBlock.FACING).getOpposite();
    }

    @Override
    public void unpoweredTick(Level level, BlockPos pos, BlockState state) {
        if (isPrinting()) {
            status = !redstoneAllows() ? Status.REDSTONE : outputHasRoom() ? Status.NO_POWER : Status.OUTPUT_FULL;
        }

        updateRunning(level);
    }

    @Override
    public void poweredTick(Level level, BlockPos pos, BlockState state) {
        if (!isPrinting() || !(level instanceof ServerLevel serverLevel)) {
            updateRunning(level);
            return;
        }

        status = Status.PRINTING;
        lastPoweredTick = level.getGameTime();

        if (source != null && source.bay() != null && !source.bay().isRemoved() && progress % READ_REFRESH_TICKS == 0) {
            source.bay().markReading(source.slot());
        }

        progress++;

        if (progress >= required) {
            finish(serverLevel);
        }

        updateRunning(level);
        setChanged();
    }

    private void finish(ServerLevel level) {
        var item = target;

        if (item == null) {
            return;
        }

        var printable = printables(level).get(item);

        if (printable == null) {
            stopWith(Status.NO_BLUEPRINT);
            return;
        }

        if (!outputHasRoom()) {
            stopWith(Status.OUTPUT_FULL);
            return;
        }

        var connected = connectedContents(level);
        var plan = plan(level, item, connected);

        if (!plan.printable()) {
            stopWith(Status.MISSING);
            return;
        }

        var result = plan.result(item);

        // Only ever stacked onto an IDENTICAL item: merging onto a different one would wipe its contents.
        if (!output.isEmpty() && !ItemStack.isSameItemSameComponents(output, result)) {
            stopWith(Status.OUTPUT_FULL);
            return;
        }

        // Connected storage first - all of it, or none of it.
        if (!pullAll(level, connected, plan.taken())) {
            stopWith(Status.MISSING);
            return;
        }

        for (int slot = 0; slot < STORAGE_SLOTS; slot++) {
            storage.set(slot, plan.internal().get(slot));
        }

        if (output.isEmpty()) {
            output = result;
        } else {
            output.grow(result.getCount());
        }

        queued--;
        progress = 0;

        if (queued <= 0) {
            target = null;
            source = null;
            status = Status.IDLE;
        } else {
            source = printable;
            required = MachineUpgrades.effectiveTicks(upgradeView, BASE_TICKS + TICKS_PER_STEP * Math.max(1, plan.steps()));
        }

        setChanged();
        sync();
    }

    /**
     * Pulls what the plan takes from each connected inventory. If ANY pull comes up short (something changed since the
     * plan), everything already pulled is put back where it came from - or, failing that, into this fabricator's
     * storage, or at its feet. Never deleted.
     */
    private boolean pullAll(ServerLevel level, List<Stored> connected, int[] taken) {
        var pulled = new ArrayList<Stored>();

        for (int entry = 0; entry < connected.size(); entry++) {
            var amount = taken[entry];

            if (amount <= 0) {
                continue;
            }

            var wanted = connected.get(entry);
            var got = 0;
            // A misbehaving inventory that offers but never gives must not spin the server: cap the attempts, then
            // treat it as short - which puts everything back.
            var attempts = 0;

            while (got < amount && attempts++ < MAX_PULL_ATTEMPTS) {
                var offer = wanted.port().peek(amount - got, stack -> ItemStack.isSameItemSameComponents(stack, wanted.stack()));

                if (offer == null || offer.stack().isEmpty()) {
                    break;
                }

                var count = Math.min(amount - got, offer.stack().getCount());
                // Oct 13 hardening: count what REALLY came out. Recording the request instead meant a short take,
                // followed by a put-back, returned more than was taken - duplicating the difference.
                var took = offer.take(count);

                if (took <= 0) {
                    break;
                }

                pulled.add(new Stored(wanted.port(), wanted.stack().copyWithCount(took)));
                got += took;
            }

            if (got < amount) {
                putBack(level, pulled);
                return false;
            }
        }

        return true;
    }

    /**
     * Shift-click into internal storage: into ALL 48 slots, filling matching stacks first, then empty slots. Returns
     * what did not fit.
     */
    public ItemStack insertIntoStorage(ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int slot = 0; slot < STORAGE_SLOTS && !stack.isEmpty(); slot++) {
                var held = storage.get(slot);

                if (
                    pass == 0 && !held.isEmpty() && ItemStack.isSameItemSameComponents(held, stack) && held.getCount() < held
                        .getMaxStackSize()
                ) {
                    var moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());

                    held.grow(moved);
                    stack.shrink(moved);
                } else if (pass == 1 && held.isEmpty()) {
                    storage.set(slot, stack.split(stack.getMaxStackSize()));
                }
            }
        }

        setChanged();

        return stack;
    }

    private void putBack(ServerLevel level, List<Stored> pulled) {
        for (var entry : pulled) {
            var stack = entry.stack().copy();
            var inserted = entry.port().insert(stack.copy());

            stack.shrink(inserted);

            for (int slot = 0; slot < STORAGE_SLOTS && !stack.isEmpty(); slot++) {
                var held = storage.get(slot);

                if (held.isEmpty()) {
                    storage.set(slot, stack.split(stack.getMaxStackSize()));
                } else if (ItemStack.isSameItemSameComponents(held, stack) && held.getCount() < held.getMaxStackSize()) {
                    var moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());

                    held.grow(moved);
                    stack.shrink(moved);
                }
            }

            while (!stack.isEmpty()) {
                Containers.dropItemStack(
                    level,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 1.0D,
                    worldPosition.getZ() + 0.5D,
                    stack.split(stack.getMaxStackSize())
                );
            }
        }

        setChanged();
    }

    private void stopWith(Status reason) {
        target = null;
        queued = 0;
        progress = 0;
        source = null;
        status = reason;
        running = false;
        setChanged();
        sync();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Pipes: materials in, finished prints out
    // ------------------------------------------------------------------------------------------------------------

    private final PipeItemAccess.Port pipePort = new PipeItemAccess.Port() {

        @Override
        public boolean present() {
            return !isRemoved();
        }

        @Override
        public boolean canInsert() {
            return true;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        /** Only the OUTPUT comes out - pipes never take materials or blueprints. */
        @Override
        public @Nullable PipeItemAccess.Offer peek(int max, java.util.function.Predicate<ItemStack> allowed) {
            if (output.isEmpty() || !allowed.test(output)) {
                return null;
            }

            var offered = output.copyWithCount(Math.min(max, output.getCount()));

            return new PipeItemAccess.Offer() {

                @Override
                public ItemStack stack() {
                    return offered;
                }

                @Override
                public int take(int count) {
                    var took = Math.min(count, output.getCount());

                    output.shrink(took);

                    if (output.isEmpty()) {
                        output = ItemStack.EMPTY;
                    }

                    setChanged();

                    return took;
                }
            };
        }

        /**
         * Oct 13 hardening - takes back items a pipe just put into STORAGE (the only place pipe inserts go), when their
         * source then gave less than it promised.
         */
        @Override
        public int retract(ItemStack like, int count) {
            var removed = 0;

            for (int slot = 0; slot < STORAGE_SLOTS && removed < count; slot++) {
                var held = storage.get(slot);

                if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, like)) {
                    var take = Math.min(count - removed, held.getCount());

                    held.shrink(take);
                    removed += take;

                    if (held.isEmpty()) {
                        storage.set(slot, ItemStack.EMPTY);
                    }
                }
            }

            if (removed > 0) {
                setChanged();
            }

            return removed;
        }

        /** Materials go into STORAGE only, merged first. Blueprints, licenses and cards are never piped in. */
        @Override
        public int insert(ItemStack stack) {
            if (stack.isEmpty() || isBlueprintSlotItem(stack)) {
                return 0;
            }

            var remaining = stack.copy();

            for (int slot = 0; slot < STORAGE_SLOTS && !remaining.isEmpty(); slot++) {
                var held = storage.get(slot);

                if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, remaining)) {
                    var moved = Math.min(remaining.getCount(), held.getMaxStackSize() - held.getCount());

                    held.grow(moved);
                    remaining.shrink(moved);
                }
            }

            for (int slot = 0; slot < STORAGE_SLOTS && !remaining.isEmpty(); slot++) {
                if (storage.get(slot).isEmpty()) {
                    storage.set(slot, remaining.split(remaining.getMaxStackSize()));
                }
            }

            var inserted = stack.getCount() - remaining.getCount();

            if (inserted > 0) {
                setChanged();
            }

            return inserted;
        }

        @Override
        public Object storage() {
            return FabricatorBlockEntity.this;
        }

        @Override
        public boolean isMachine() {
            return true;
        }
    };

    public PipeItemAccess.Port pipePort() {
        return pipePort;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Saving and syncing
    // ------------------------------------------------------------------------------------------------------------

    private boolean syncedPrinting;

    private @Nullable Item syncedTarget;

    private int syncedQueued;

    private boolean syncedRunning;

    private void sync() {
        if (level == null || level.isClientSide) {
            return;
        }

        // The queue count too: each finished item restarts the bed, and the client's progress must restart with it.
        // ⚠ Oct 23 - and whether power is reaching it, which the client's printing visuals now follow (see isRunning).
        if (syncedPrinting != isPrinting() || syncedTarget != target || syncedQueued != queued || syncedRunning != running) {
            syncedRunning = running;
            syncedPrinting = isPrinting();
            syncedTarget = target;
            syncedQueued = queued;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Storage", ContainerHelper.saveAllItems(new CompoundTag(), storage, true, registries));
        tag.put("Blueprints", ContainerHelper.saveAllItems(new CompoundTag(), blueprints, true, registries));
        tag.put("Upgrades", ContainerHelper.saveAllItems(new CompoundTag(), upgrades, true, registries));

        if (!output.isEmpty()) {
            tag.put("Output", output.save(registries));
        }

        tag.putString("Priority", priority.name());
        tag.putInt("RedstoneMode", redstoneMode.ordinal());
        writeState(tag);
    }

    private void writeState(CompoundTag tag) {
        if (target != null) {
            tag.putString("Target", BuiltInRegistries.ITEM.getKey(target).toString());
        }

        tag.putInt("Queued", queued);
        tag.putInt("Progress", progress);
        tag.putInt("Required", required);
        tag.putBoolean("Running", running);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        fill(storage);
        fill(blueprints);
        fill(upgrades);
        ContainerHelper.loadAllItems(tag.getCompound("Storage"), storage, registries);
        ContainerHelper.loadAllItems(tag.getCompound("Blueprints"), blueprints, registries);
        ContainerHelper.loadAllItems(tag.getCompound("Upgrades"), upgrades, registries);
        output = tag.contains("Output") ? ItemStack.parseOptional(registries, tag.getCompound("Output")) : ItemStack.EMPTY;

        try {
            priority = tag.contains("Priority") ? MachinePriority.valueOf(tag.getString("Priority")) : MachinePriority.DEFAULT;
        } catch (IllegalArgumentException unknown) {
            priority = MachinePriority.DEFAULT;
        }

        redstoneMode = MachineUpgrades.RedstoneMode.byOrdinal(tag.getInt("RedstoneMode"));

        var id = tag.contains("Target") ? ResourceLocation.tryParse(tag.getString("Target")) : null;

        target = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        queued = tag.getInt("Queued");
        progress = tag.getInt("Progress");
        required = tag.getInt("Required");
        running = tag.getBoolean("Running");
        status = isPrinting() ? Status.PRINTING : Status.IDLE;
        recountUpgrades();
    }

    /** Clients get the ports and the print state - never the storage. Sent only on a print change. */
    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        var tag = super.getUpdateTag(registries);

        tag.remove("Storage");
        tag.remove("Blueprints");
        tag.remove("Upgrades");
        tag.remove("Output");

        return tag;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NotNull Builder components) {
        super.collectImplicitComponents(components);

        if (isEmpty()) {
            return;
        }

        var all = NonNullList.withSize(ALL_SLOTS, ItemStack.EMPTY);

        for (int slot = 0; slot < ALL_SLOTS; slot++) {
            all.set(slot, getSlot(slot).copy());
        }

        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(all));
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.@NotNull DataComponentInput input) {
        super.applyImplicitComponents(input);

        var contents = input.get(DataComponents.CONTAINER);

        if (contents == null) {
            return;
        }

        var all = NonNullList.withSize(ALL_SLOTS, ItemStack.EMPTY);

        contents.copyInto(all);

        // Same numbering as getSlot / setSlot, so a fabricator picked up before the storage grew loads exactly as it
        // was.
        for (int slot = 0; slot < ALL_SLOTS; slot++) {
            var stack = all.get(slot);

            if (slot < FIRST_BLUEPRINT) {
                storage.set(slot, stack);
            } else if (slot < OUTPUT_SLOT) {
                blueprints.set(slot - FIRST_BLUEPRINT, stack);
            } else if (slot == OUTPUT_SLOT) {
                output = stack;
            } else if (slot < FIRST_EXTRA_STORAGE) {
                upgrades.set(slot - FIRST_UPGRADE, stack);
            } else {
                storage.set(FIRST_BLUEPRINT + slot - FIRST_EXTRA_STORAGE, stack);
            }
        }

        recountUpgrades();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(@NotNull CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Storage");
        tag.remove("Blueprints");
        tag.remove("Upgrades");
        tag.remove("Output");
    }

    public boolean isEmpty() {
        for (int slot = 0; slot < ALL_SLOTS; slot++) {
            if (!getSlot(slot).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static void fill(NonNullList<ItemStack> list) {
        for (int slot = 0; slot < list.size(); slot++) {
            list.set(slot, ItemStack.EMPTY);
        }
    }

    /** A list seen as a container, so the machines' card rules in {@link MachineUpgrades} apply unchanged. */
    private static final class ListContainer implements Container {

        private final NonNullList<ItemStack> list;

        private ListContainer(NonNullList<ItemStack> list) {
            this.list = list;
        }

        @Override
        public int getContainerSize() {
            return list.size();
        }

        @Override
        public boolean isEmpty() {
            return list.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            return list.get(slot);
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            return ContainerHelper.removeItem(list, slot, amount);
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(list, slot);
        }

        @Override
        public void setItem(int slot, @NotNull ItemStack stack) {
            list.set(slot, stack);
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(@NotNull Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            fill(list);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Client
    // ------------------------------------------------------------------------------------------------------------

    private final FabricatorAnimationDispatcher animations = new FabricatorAnimationDispatcher();

    private @Nullable Boolean dispatchedPrinting;

    private long lastSparkTick = Long.MIN_VALUE;

    /** Sparks at most once per game tick, however many frames draw this fabricator. */
    public boolean claimSparkTick(long gameTime) {
        if (gameTime == lastSparkTick) {
            return false;
        }

        lastSparkTick = gameTime;

        return true;
    }

    /**
     * How far the print on the bed has built, 0 to 1. The server sends progress when a print starts or an item
     * finishes; between those the client counts ticks itself, which is all the bed needs.
     */
    public float clientProgress(float partialTick) {
        return required <= 0 ? 0.0F : Math.min(1.0F, (progress + partialTick) / required);
    }

    public void clientTick() {
        // ⚠ Oct 23 - RUNNING, not merely queued: an unpowered fabricator holds its bed where it stopped and plays idle.
        var printing = isRunning();

        if (printing && progress < required) {
            progress++;
        }

        // Only counts as sent once it actually reached an animator (see FabricatorAnimationDispatcher.play).
        if ((dispatchedPrinting == null || dispatchedPrinting != printing) && animations.play(this, printing)) {
            dispatchedPrinting = printing;
        }
    }
}
