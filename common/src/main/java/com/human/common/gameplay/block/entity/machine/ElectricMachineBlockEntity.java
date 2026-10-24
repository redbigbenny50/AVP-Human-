package com.human.common.gameplay.block.entity.machine;

import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.block.power.machine.ElectricMachineBlock;
import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.fluid.FluidEndpoint;
import com.human.common.gameplay.machine.ElectricMachineKind;
import com.human.common.gameplay.machine.MachineFluidItems;
import com.human.common.gameplay.machine.MachinePriority;
import com.human.common.gameplay.machine.MachineState;
import com.human.common.gameplay.menu.machine.ElectricMachineMenu;
import com.human.common.gameplay.power.PowerSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * A tier-1 machine: the electric furnace, smoker, blast furnace and industrial furnace, the grinder, and the press,
 * hydrator and freezer - {@link ElectricMachineKind} says which, and
 * {@link com.human.common.gameplay.machine.MachineLogic} holds what each actually does.
 * <h2>POWER</h2> [stated] Sep 29: consumers store no power and draw only while processing. This asks the grid for
 * {@link ElectricMachineKind#powerPerTick()} on any tick where an item can actually cook - input present, a recipe for
 * it, room in the output, switched on - and for nothing otherwise. A tick that gets its power advances the cook by one;
 * a tick that does not simply waits, keeping its progress.
 * <h2>FACES</h2> Power enters at the BACK only ({@link #canConnectOn}). Items, for hoppers and later pipes, use the
 * faces the kind lists ({@link ElectricMachineKind.Faces}): in through the TOP and/or LEFT, out through the BOTTOM
 * and/or RIGHT - left and right as seen by a player looking at the front. The front takes nothing.
 * <h2>THE LIGHT</h2> {@link MachineState#WORKING} while cooking, and for {@value #WORKING_GRACE_TICKS} ticks after, so
 * a steady feed of items is one state change rather than one per item - every change rebuilds the chunk's mesh on every
 * client watching. {@link MachineState#ON} when switched on and the grid has power to offer; {@link MachineState#OFF}
 * otherwise.
 * <h2>FLUID (PRESS, HYDRATOR, FREEZER)</h2> A machine whose kind has a {@link ElectricMachineKind.Tank} holds one fluid
 * and is a {@link FluidEndpoint}, so pipes - ours and other mods' - reach it through the same loader adapters as the
 * tanks. An OUTPUT tank (press, hydrator) only gives fluid out, through the BOTTOM; an INPUT tank (freezer) only takes
 * it in, through the LEFT, and only fluids it has a recipe for. [stated] readme.
 * <ul>
 * <li>FILL SLOT (press, hydrator): an empty bucket or a canister put there is filled from the tank, no power
 * needed.</li>
 * <li>Freezer: a full bucket or canister in the INPUT slot is poured into the tank, and the empty container moves to
 * the output - so a hopper can feed it buckets and another can take the empties away.</li>
 * </ul>
 * The tank's contents are lost when the machine is broken, as with the multiblock tanks.
 */
public class ElectricMachineBlockEntity extends PowerConsumerBlockEntity implements WorldlyContainer, MenuProvider, FluidEndpoint {

    public static final int SLOT_INPUT = 0;

    /** The first output slot. The furnace types have one output; the grinder nine (slots 1-9). */
    public static final int SLOT_OUTPUT = 1;

    public static final int FIRST_OUTPUT = SLOT_OUTPUT;

    public static final int FILTER_SIZE = 4;

    /** Menu data indices. */
    public static final int DATA_PROGRESS = 0;

    public static final int DATA_TOTAL = 1;

    public static final int DATA_STATUS = 2;

    public static final int DATA_PRIORITY = 3;

    /** The tank's fluid as its registry id (the fluid registry is synced, so the ids agree), or -1 when empty. */
    public static final int DATA_FLUID = 4;

    /** ⚠ Menu data travels as a SHORT - fine for a tank of at most 32,767 mB. {@link ElectricMachineKind.Tank}. */
    public static final int DATA_FLUID_AMOUNT = 5;

    public static final int DATA_FLAGS = 6;

    /** The redstone mode, as {@link com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode#ordinal()}. */
    public static final int DATA_REDSTONE = 7;

    /**
     * [stated] Oct 2: the item port modes, six faces packed two bits each
     * ({@link com.human.common.gameplay.machine.MachinePorts#pack}).
     */
    public static final int DATA_PORTS_ITEMS = 8;

    /** The fluid port modes, packed the same way. */
    public static final int DATA_PORTS_FLUIDS = 9;

    public static final int DATA_COUNT = 10;

    /** [stated] Oct 2: every machine has four visible upgrade slots, one card each. */
    public static final int UPGRADE_SLOTS = com.human.common.gameplay.power.upgrade.UpgradeCard.SLOTS;

    /** {@link #DATA_FLAGS}: the press keeps its item output. */
    public static final int FLAG_KEEP_ITEMS = 1;

    /** {@link #DATA_FLAGS}: the press keeps its fluid output. */
    public static final int FLAG_KEEP_FLUID = 2;

    public static final int STATUS_DISABLED = 0;

    public static final int STATUS_NO_POWER = 1;

    public static final int STATUS_IDLE = 2;

    public static final int STATUS_WORKING = 3;

    private static final int WORKING_GRACE_TICKS = 20;

    private static final int SUPPLY_CHECK_INTERVAL_TICKS = 20;

    private static final int[] SLOTS_IN = { SLOT_INPUT };

    private static final int[] SLOTS_NONE = {};

    private static final String NBT_PROGRESS = "progress";

    private static final String NBT_TOTAL = "total";

    private static final String NBT_ENABLED = "enabled";

    private static final String NBT_PRIORITY = "priority";

    private static final String NBT_XP = "storedXp";

    private static final String NBT_FILTER = "filter";

    /** The old single hub-chip slot - read only, to move a saved chip into the upgrade slots. */
    private static final String NBT_HUB = "hub";

    private static final String NBT_UPGRADES = "upgrades";

    private static final String NBT_REDSTONE = "redstone";

    private static final String NBT_PORTS_ITEMS = "portsItems";

    private static final String NBT_PORTS_FLUIDS = "portsFluids";

    private static final String NBT_FLUID = "fluid";

    private static final String NBT_FLUID_AMOUNT = "fluidAmount";

    private static final String NBT_KEEP_ITEMS = "keepItems";

    private static final String NBT_KEEP_FLUID = "keepFluid";

    /**
     * ⚠ Set by the client entry point. Common code must never name a client class - a client class reachable from
     * common is exactly what kills datagen and dedicated servers - so the client tick is handed in from outside.
     */
    public static Consumer<ElectricMachineBlockEntity> clientTicker = blockEntity -> {};

    private final ElectricMachineKind kind;

    private final NonNullList<ItemStack> items;

    /** What this machine actually does - see {@link com.human.common.gameplay.machine.MachineLogic}. */
    private final com.human.common.gameplay.machine.MachineLogic logic;

    /** The output slot indexes, for automation. */
    private final int[] slotsOut;

    /** The input slot and every output - for a BOTH side. Built once; getSlotsForFace never allocates. */
    private final int[] slotsBoth;

    /** [stated] Oct 2: per relative face. Defaults are the machine's old fixed layout. */
    private final com.human.common.gameplay.machine.MachinePorts.Mode[] itemPorts =
        new com.human.common.gameplay.machine.MachinePorts.Mode[com.human.common.gameplay.machine.MachinePorts.FACES];

    private final com.human.common.gameplay.machine.MachinePorts.Mode[] fluidPorts =
        new com.human.common.gameplay.machine.MachinePorts.Mode[com.human.common.gameplay.machine.MachinePorts.FACES];

    private final SimpleContainer filter = new SimpleContainer(FILTER_SIZE) {

        @Override
        public void setChanged() {
            super.setChanged();
            ElectricMachineBlockEntity.this.setChanged();
        }
    };

    private int progress;

    private int total;

    private boolean enabled = true;

    private MachinePriority priority = MachinePriority.DEFAULT;

    /**
     * [stated] Oct 2: four upgrade slots, one card each - speed and efficiency (stacking), the hub chip (a machine with
     * one binds to a fuse panel) and the redstone card. The rules are
     * {@link com.human.common.gameplay.machine.MachineUpgrades}.
     */
    private final SimpleContainer upgrades = new SimpleContainer(UPGRADE_SLOTS) {

        @Override
        public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
            return com.human.common.gameplay.machine.MachineUpgrades.mayPlace(this, slot, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            recountUpgrades();
            ElectricMachineBlockEntity.this.setChanged();
        }
    };

    private float storedXp;

    /** The machine's tank - always EMPTY for a kind without one. */
    private FluidContent fluid = FluidContent.EMPTY;

    /** [stated] readme: the press's two switches. Both on by default - nothing is thrown away until you say so. */
    private boolean keepItemOutput = true;

    private boolean keepFluidOutput = true;

    /**
     * ⚡ The cards, counted once whenever the upgrade slots change (and on load) - every tick then reads these instead
     * of walking the slots three or four times per machine.
     */
    private double cachedDrawMultiplier = 1.0;

    private double cachedSpeed = 1.0;

    private boolean cachedHub;

    private boolean cachedRedstone;

    private void recountUpgrades() {
        cachedDrawMultiplier = com.human.common.gameplay.machine.MachineUpgrades.drawMultiplier(upgrades);
        cachedSpeed = com.human.common.gameplay.power.upgrade.UpgradeCard.speedMultiplier(
            com.human.common.gameplay.machine.MachineUpgrades.count(upgrades, com.human.common.gameplay.power.upgrade.UpgradeCard.SPEED)
        );
        cachedHub = com.human.common.gameplay.machine.MachineUpgrades.hasHub(upgrades);
        cachedRedstone = com.human.common.gameplay.machine.MachineUpgrades.hasRedstone(upgrades);
    }

    /** A recipe's time after speed cards: never below one tick. */
    private int effectiveTicks(int recipeTicks) {
        return recipeTicks <= 0 ? recipeTicks : Math.max(1, (int) Math.ceil(recipeTicks / cachedSpeed));
    }

    /** Only consulted while a redstone card is fitted. */
    private com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode redstoneMode =
        com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode.IGNORE;

    private boolean wantsPower;

    private long lastWorkTick = Long.MIN_VALUE / 2;

    private boolean gridHasSupply;

    private int supplyTimer;

    private int status = STATUS_NO_POWER;

    /** CLIENT ONLY: the tick this machine was last seen to start working - its animation clock. */
    public long clientWorkStart;

    /** CLIENT ONLY: whether the client has already seen the current working spell begin. */
    public boolean clientWorking;

    /** CLIENT ONLY: the looping working sound, typed as Object so common code never names a client class. */
    public @Nullable Object clientSound;

    private final ContainerData data = new ContainerData() {

        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                // The bolt fills over the time the machine will actually take - shorter with speed cards.
                case DATA_TOTAL -> effectiveTicks(total);
                case DATA_STATUS -> status;
                case DATA_PRIORITY -> priority.ordinal();
                case DATA_FLUID -> fluid.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(fluid.fluid());
                case DATA_FLUID_AMOUNT -> fluid.amount();
                case DATA_FLAGS -> (keepItemOutput ? FLAG_KEEP_ITEMS : 0) | (keepFluidOutput ? FLAG_KEEP_FLUID : 0);
                case DATA_REDSTONE -> redstoneMode.ordinal();
                case DATA_PORTS_ITEMS -> com.human.common.gameplay.machine.MachinePorts.pack(itemPorts);
                case DATA_PORTS_FLUIDS -> com.human.common.gameplay.machine.MachinePorts.pack(fluidPorts);
                // A machine's own values follow the shared ones (the distributor's chamber, mode...).
                default -> index >= DATA_COUNT ? extraData(index - DATA_COUNT) : 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-authoritative: the menu only ever reads these.
        }

        @Override
        public int getCount() {
            return DATA_COUNT + kind.extraDataCount();
        }
    };

    public ElectricMachineBlockEntity(ElectricMachineKind kind, BlockPos pos, BlockState state) {
        super(kind.blockEntityType().get(), pos, state);
        this.kind = kind;
        this.items = NonNullList.withSize(kind.layout().slotCount(), ItemStack.EMPTY);
        this.logic = kind.createLogic();
        this.slotsOut = new int[kind.layout().outputs().length];

        for (var index = 0; index < slotsOut.length; index++) {
            slotsOut[index] = FIRST_OUTPUT + index;
        }

        this.slotsBoth = new int[slotsOut.length + 1];
        this.slotsBoth[0] = SLOT_INPUT;
        System.arraycopy(slotsOut, 0, slotsBoth, 1, slotsOut.length);
        resetPorts();
    }

    public ElectricMachineKind getKind() {
        return kind;
    }

    /**
     * A machine's own menu value number {@code index} (0-based, after the shared ones) - for a kind whose
     * {@link ElectricMachineKind#extraDataCount()} is above 0. Server side only.
     */
    protected int extraData(int index) {
        return 0;
    }

    /**
     * A menu button the shared menu does not know - a machine's own control (the distributor's mode buttons). Server
     * side. @return whether the id was this machine's
     */
    public boolean handleMenuButton(Player player, int id) {
        return false;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Power
    // ------------------------------------------------------------------------------------------------------------

    /** [stated] Sep 29: "all machines get power from the back" - the vaporizer from its underside (Oct 17). */
    @Override
    public boolean canConnectOn(Direction direction) {
        return direction == kind.powerSide(facing());
    }

    @Override
    public long getRequestedPower() {
        return wantsPower ? getDrawPerTick() : 0L;
    }

    /**
     * What this machine draws while working, after its cards: [stated] Oct 2 speed 1.5x per card at +5% energy per
     * item, efficiency -25% per card. The grid and the wiring tool read this.
     */
    public long getDrawPerTick() {
        return Math.max(1L, Math.round(kind.powerPerTick() * cachedDrawMultiplier));
    }

    @Override
    public int getPowerPriority() {
        return priority.band();
    }

    @Override
    public void serverTick(Level level, BlockPos blockPos, BlockState blockState) {
        // Buckets and canisters move fluid whether or not there is power - they are the player's hand, not the machine.
        serviceContainers(level);

        // Decide what to ask for BEFORE spending last tick's delivery, so the grid - which ticks after block entities -
        // sees this tick's answer.
        wantsPower = enabled && redstoneAllows(level, blockPos) && logic.work(this, level) > 0;

        super.serverTick(level, blockPos, blockState);

        updateState(level, blockPos, blockState);

        if (kind.syncsFluidToClients()) {
            syncFluidToClients(level, blockPos, getBlockState());
        }
    }

    @Override
    public void poweredTick(Level level, BlockPos blockPos, BlockState blockState) {
        // ⚠ Re-checked, not assumed: the input may have been pulled out between the grid paying and this tick running.
        var needed = enabled && redstoneAllows(level, blockPos) ? logic.work(this, level) : 0;

        if (needed <= 0) {
            return;
        }

        total = needed;
        progress++;
        lastWorkTick = level.getGameTime();

        // Speed cards shorten the job; taking one out mid-job simply finishes it a little later.
        if (progress >= effectiveTicks(total)) {
            progress = 0;
            logic.finish(this, level);
        }

        setChanged();
    }

    @Override
    public void unpoweredTick(Level level, BlockPos blockPos, BlockState blockState) {
        // Nothing to do any more: drop the half-finished progress, as a vanilla furnace does when emptied.
        if (progress > 0 && logic.work(this, level) <= 0) {
            progress = 0;
            setChanged();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // For the machine logic
    // ------------------------------------------------------------------------------------------------------------

    /** The input stack - live, so the logic can shrink it. */
    public ItemStack getInput() {
        return items.get(SLOT_INPUT);
    }

    /**
     * Whether ALL these stacks fit in the output slots together - each merged into matching stacks first, then empty
     * slots. Simulated on copies; nothing changes.
     */
    public boolean canInsertOutputs(java.util.List<ItemStack> stacks) {
        var simulated = new ItemStack[slotsOut.length];

        for (var index = 0; index < slotsOut.length; index++) {
            simulated[index] = items.get(slotsOut[index]).copy();
        }

        for (var stack : stacks) {
            if (!stack.isEmpty() && !place(simulated, stack.copy())) {
                return false;
            }
        }

        return true;
    }

    /** Puts these stacks into the output slots. Call only after {@link #canInsertOutputs} said they fit. */
    public void insertOutputs(java.util.List<ItemStack> stacks) {
        var live = new ItemStack[slotsOut.length];

        for (var index = 0; index < slotsOut.length; index++) {
            live[index] = items.get(slotsOut[index]);
        }

        for (var stack : stacks) {
            if (!stack.isEmpty()) {
                place(live, stack.copy());
            }
        }

        for (var index = 0; index < slotsOut.length; index++) {
            items.set(slotsOut[index], live[index]);
        }
    }

    /** Merges into matching stacks, then fills empty slots. @return whether it all fit */
    private boolean place(ItemStack[] slots, ItemStack stack) {
        for (var slot : slots) {
            if (stack.isEmpty()) {
                return true;
            }

            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                var room = Math.min(slot.getMaxStackSize(), getMaxStackSize()) - slot.getCount();
                var moved = Math.min(room, stack.getCount());

                if (moved > 0) {
                    slot.grow(moved);
                    stack.shrink(moved);
                }
            }
        }

        for (var index = 0; index < slots.length && !stack.isEmpty(); index++) {
            if (slots[index].isEmpty()) {
                var moved = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), getMaxStackSize()));

                slots[index] = stack.split(moved);
            }
        }

        return stack.isEmpty();
    }

    /** Experience earned by a job, paid out to whoever takes the output (the furnace types). */
    public void addExperience(float experience) {
        storedXp += experience;
    }

    /** Output slots only - the fill slot, which follows them, is not an output. */
    public boolean isOutputSlot(int slot) {
        return slot >= FIRST_OUTPUT && slot < FIRST_OUTPUT + slotsOut.length;
    }

    public boolean isFillSlot(int slot) {
        return kind.layout().hasFillSlot() && slot == kind.layout().fillSlot();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The tank - for the machine logic
    // ------------------------------------------------------------------------------------------------------------

    public FluidContent getFluid() {
        return fluid;
    }

    /** Millibuckets of this fluid that still fit - 0 when the tank holds another fluid, or the machine has no tank. */
    public int fluidRoom(Fluid other) {
        var tank = kind.tank();

        if (tank == null || !fluid.accepts(other)) {
            return 0;
        }

        return Math.max(0, tank.capacity() - fluid.amount());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Telling nearby clients what is in the tank - the vaporizer only (its model shows the liquid)
    // ------------------------------------------------------------------------------------------------------------

    private static final String NBT_CLIENT_SYNC = "clientFluidSync";

    /** How often, at most, a changed tank is sent to the clients watching: twice a second. */
    private static final int CLIENT_FLUID_SYNC_TICKS = 10;

    private boolean clientFluidDirty;

    private void markFluidForClients() {
        if (kind.syncsFluidToClients()) {
            clientFluidDirty = true;
        }
    }

    /** Server side, from the tick: at most twice a second, and only when the tank actually changed. */
    private void syncFluidToClients(Level level, BlockPos blockPos, BlockState blockState) {
        if (clientFluidDirty && level.getGameTime() % CLIENT_FLUID_SYNC_TICKS == 0) {
            clientFluidDirty = false;
            level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * What a client is sent about this machine: nothing for most, the tank alone for the vaporizer - marked, so
     * {@link #loadAdditional} reads only that and never clears the client copy's other state.
     */
    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        var tag = super.getUpdateTag(registries);

        if (kind.syncsFluidToClients()) {
            tag.putBoolean(NBT_CLIENT_SYNC, true);

            if (!fluid.isEmpty()) {
                tag.putString(NBT_FLUID, BuiltInRegistries.FLUID.getKey(fluid.fluid()).toString());
                tag.putInt(NBT_FLUID_AMOUNT, fluid.amount());
            }
        }

        return tag;
    }

    @Override
    public @Nullable net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return kind.syncsFluidToClients() ? net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this) : null;
    }

    /**
     * Puts fluid in, clamped to the tank. The machine's own production and intake - pipes go through {@link #insert}.
     */
    public void addFluid(Fluid added, int amount) {
        var moved = Math.min(amount, fluidRoom(added));

        if (moved > 0) {
            fluid = FluidContent.of(added, fluid.amount() + moved);
            markFluidForClients();
            setChanged();
        }
    }

    /** Takes fluid out - what a recipe uses. */
    public void removeFluid(int amount) {
        if (amount > 0 && !fluid.isEmpty()) {
            fluid = fluid.withAmount(fluid.amount() - amount);
            markFluidForClients();
            setChanged();
        }
    }

    public boolean keepsItemOutput() {
        return keepItemOutput;
    }

    public boolean keepsFluidOutput() {
        return keepFluidOutput;
    }

    public void toggleKeepItemOutput() {
        if (kind.hasDiscardSwitches()) {
            keepItemOutput = !keepItemOutput;
            setChanged();
        }
    }

    public void toggleKeepFluidOutput() {
        if (kind.hasDiscardSwitches()) {
            keepFluidOutput = !keepFluidOutput;
            setChanged();
        }
    }

    /**
     * The fill slot (output tanks) and the freezer's poured-in containers (input tanks). Runs every tick but costs a
     * slot read when there is nothing to do.
     */
    private void serviceContainers(Level level) {
        var tank = kind.tank();

        if (tank == null) {
            return;
        }

        if (tank.output()) {
            if (!kind.layout().hasFillSlot() || fluid.isEmpty()) {
                return;
            }

            var slot = kind.layout().fillSlot();
            var stack = items.get(slot);

            if (stack.getCount() != 1 || !MachineFluidItems.isFillable(stack)) {
                return;
            }

            var result = MachineFluidItems.fill(stack, fluid);

            if (!result.isNone()) {
                items.set(slot, result.container());
                removeFluid(result.moved());
            }

            return;
        }

        var input = getInput();
        var held = MachineFluidItems.contained(input);

        if (held.isEmpty() || input.getCount() != 1 || !logic.acceptsFluid(this, level, held.fluid())) {
            return;
        }

        var result = MachineFluidItems.drain(input, held.fluid(), fluidRoom(held.fluid()));

        if (result.isNone()) {
            return;
        }

        var emptied = MachineFluidItems.isEmptied(result.container());

        // ⚠ An emptied container leaves for the output - and waits, full, until there is room for it there, so an empty
        // bucket is never destroyed and never left blocking the input.
        if (emptied && !canInsertOutputs(List.of(result.container()))) {
            return;
        }

        addFluid(held.fluid(), result.moved());

        if (emptied) {
            items.set(SLOT_INPUT, ItemStack.EMPTY);
            insertOutputs(List.of(result.container()));
        } else {
            items.set(SLOT_INPUT, result.container());
        }

        setChanged();
    }

    // ------------------------------------------------------------------------------------------------------------
    // FluidEndpoint - pipes and containers from outside
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public FluidContent getContent() {
        return fluid;
    }

    @Override
    public int getCapacity() {
        var tank = kind.tank();

        return tank == null ? 0 : tank.capacity();
    }

    /** Only an INPUT tank takes fluid from outside, and only a fluid it has a recipe for. */
    @Override
    public int insert(Fluid inserted, int maxAmount, boolean simulate) {
        var tank = kind.tank();

        if (tank == null || tank.output() || level == null || maxAmount <= 0) {
            return 0;
        }

        var source = FluidContent.sourceOf(inserted);

        if (!logic.acceptsFluid(this, level, source)) {
            return 0;
        }

        var moved = Math.min(maxAmount, fluidRoom(source));

        if (!simulate && moved > 0) {
            addFluid(source, moved);
        }

        return moved;
    }

    /** Only an OUTPUT tank gives fluid out. */
    @Override
    public int extract(@Nullable Fluid wanted, int maxAmount, boolean simulate) {
        var tank = kind.tank();

        if (tank == null || !tank.output() || fluid.isEmpty() || maxAmount <= 0) {
            return 0;
        }

        if (wanted != null && !fluid.fluid().isSame(wanted)) {
            return 0;
        }

        var moved = Math.min(maxAmount, fluid.amount());

        if (!simulate) {
            removeFluid(moved);
        }

        return moved;
    }

    @Override
    public void setContentSilently(FluidContent content) {
        fluid = content;
        markFluidForClients();
    }

    @Override
    public void notifyContentChanged() {
        setChanged();
    }

    /** [stated] readme - freezer: "import liquid on left side". */
    @Override
    public boolean canFill(@Nullable Direction side) {
        var tank = kind.tank();

        return tank != null && !tank.output() && (side == null || fluidPorts[faceOf(side)].allowsIn());
    }

    /** [stated] readme - press and hydrator: "liquid export on bottom". */
    @Override
    public boolean canDrain(@Nullable Direction side) {
        var tank = kind.tank();

        return tank != null && tank.output() && (side == null || fluidPorts[faceOf(side)].allowsOut());
    }

    private void updateState(Level level, BlockPos blockPos, BlockState blockState) {
        MachineState target;

        if (!enabled) {
            target = MachineState.OFF;
            status = STATUS_DISABLED;
        } else if (level.getGameTime() - lastWorkTick <= WORKING_GRACE_TICKS) {
            target = MachineState.WORKING;
            status = STATUS_WORKING;
        } else {
            // ⚠ Once a second, and from numbers the grid already measured - never a fresh poll of every producer.
            if (--supplyTimer <= 0) {
                supplyTimer = SUPPLY_CHECK_INTERVAL_TICKS;

                // A machine on a fuse panel is fed by the panel's grid, not by any cable it may touch.
                var source = getFusePanel() != null ? getFusePanel() : blockPos;
                var grid = level instanceof ServerLevel serverLevel ? PowerSystem.get(serverLevel).gridAt(source) : null;

                gridHasSupply = grid != null && grid.hasSupply();
            }

            target = gridHasSupply ? MachineState.ON : MachineState.OFF;
            status = gridHasSupply ? STATUS_IDLE : STATUS_NO_POWER;
        }

        if (blockState.hasProperty(ElectricMachineBlock.STATE) && blockState.getValue(ElectricMachineBlock.STATE) != target) {
            // UPDATE_CLIENTS only: the look changes, nothing a neighbour depends on does. Same block, so this block
            // entity is kept and the grid is untouched.
            level.setBlock(blockPos, blockState.setValue(ElectricMachineBlock.STATE, target), Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------------------------------------------------

    public boolean isEnabled() {
        return enabled;
    }

    /** The wiring tool's switch. [stated] Sep 29: "machines are active by default when placed". */
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;
        wantsPower = false;
        setChanged();

        if (level != null && !level.isClientSide) {
            updateState(level, worldPosition, getBlockState());
        }
    }

    public MachinePriority getPriority() {
        return priority;
    }

    /** [stated] Oct 2: the priority pop-up picks a value directly. */
    public void setPriority(MachinePriority priority) {
        if (this.priority != priority) {
            this.priority = priority;
            setChanged();
        }
    }

    public void setRedstoneMode(com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode mode) {
        if (redstoneMode != mode) {
            redstoneMode = mode;
            setChanged();
        }
    }

    public void cyclePriority() {
        priority = priority.next();
        setChanged();
    }

    public int getStatus() {
        return status;
    }

    public Container getFilter() {
        return filter;
    }

    public SimpleContainer getUpgrades() {
        return upgrades;
    }

    /** [stated] Sep 30: a machine binds to a fuse panel only with a hub chip installed - now in any upgrade slot. */
    @Override
    public boolean canBindToFusePanel() {
        return cachedHub;
    }

    public com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        setChanged();
    }

    /** Without a redstone card the signal is ignored, whatever the mode says. */
    private boolean redstoneAllows(Level level, BlockPos pos) {
        return !cachedRedstone || redstoneMode.allows(level.hasNeighborSignal(pos));
    }

    public ContainerData getData() {
        return data;
    }

    /** An empty filter lets anything through; otherwise only the listed items. Hand insertion ignores the filter. */
    public boolean filterAllows(ItemStack stack) {
        var any = false;

        for (var index = 0; index < filter.getContainerSize(); index++) {
            var entry = filter.getItem(index);

            if (entry.isEmpty()) {
                continue;
            }

            any = true;

            if (ItemStack.isSameItem(entry, stack)) {
                return true;
            }
        }

        return !any;
    }

    public boolean hasRecipeFor(ItemStack stack) {
        return level != null && logic.accepts(this, level, stack);
    }

    /** Pays out the experience banked by everything cooked since the output was last emptied. */
    public void awardExperience(ServerPlayer player) {
        dropExperience(player.serverLevel(), player.position());
    }

    public void dropExperience(ServerLevel level, Vec3 position) {
        // Vanilla's rounding: the whole part always, the fraction as a chance.
        var whole = (int) Math.floor(storedXp);
        var fraction = storedXp - whole;

        if (fraction > 0.0F && level.random.nextFloat() < fraction) {
            whole++;
        }

        storedXp = 0.0F;

        if (whole > 0) {
            ExperienceOrb.award(level, position, whole);
        }

        setChanged();
    }

    private Direction facing() {
        var state = getBlockState();

        return state.hasProperty(ElectricMachineBlock.FACING) ? state.getValue(ElectricMachineBlock.FACING) : Direction.NORTH;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Client
    // ------------------------------------------------------------------------------------------------------------

    public static void clientTick(ElectricMachineBlockEntity blockEntity) {
        clientTicker.accept(blockEntity);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Container
    // ------------------------------------------------------------------------------------------------------------

    /**
     * [stated] Oct 2: each side's item port - IN exposes the input, OUT the outputs, BOTH both, OFF nothing. Left and
     * right as seen by a player looking at the front.
     * <p>
     * ⚠ No duplication through BOTH: whatever a side allows, only the INPUT slot can be inserted into (and only an item
     * the filter allows and a recipe accepts) and only OUTPUT slots can be extracted from - see the two checks below.
     * The fill slot and the upgrade slots are never exposed at all.
     * </p>
     */
    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        return switch (itemPorts[faceOf(side)]) {
            case IN -> SLOTS_IN;
            case OUT -> slotsOut;
            case BOTH -> slotsBoth;
            case OFF -> SLOTS_NONE;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, @NotNull ItemStack stack, @Nullable Direction direction) {
        return slot == SLOT_INPUT && (direction == null || itemPorts[faceOf(direction)].allowsIn()) && filterAllows(stack) && hasRecipeFor(
            stack
        );
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, @NotNull ItemStack stack, @NotNull Direction direction) {
        return isOutputSlot(slot) && itemPorts[faceOf(direction)].allowsOut();
    }

    private int faceOf(Direction side) {
        return com.human.common.gameplay.machine.MachinePorts.Face.of(side, facing()).ordinal();
    }

    public com.human.common.gameplay.machine.MachinePorts.Mode getItemPort(com.human.common.gameplay.machine.MachinePorts.Face face) {
        return itemPorts[face.ordinal()];
    }

    public com.human.common.gameplay.machine.MachinePorts.Mode getFluidPort(com.human.common.gameplay.machine.MachinePorts.Face face) {
        return fluidPorts[face.ordinal()];
    }

    /**
     * Sets one side. Fluid ports only exist on a machine with a tank, and only in the tank's own direction (or off): an
     * output tank cannot be filled through a side, an input tank cannot be drained.
     */
    public void setPort(
        com.human.common.gameplay.machine.MachinePorts.Face face,
        boolean fluidPort,
        com.human.common.gameplay.machine.MachinePorts.Mode mode
    ) {
        if (fluidPort) {
            var tank = kind.tank();

            if (
                tank == null || mode != com.human.common.gameplay.machine.MachinePorts.Mode.OFF && mode != (tank.output()
                    ? com.human.common.gameplay.machine.MachinePorts.Mode.OUT
                    : com.human.common.gameplay.machine.MachinePorts.Mode.IN)
            ) {
                return;
            }
        }

        var ports = fluidPort ? fluidPorts : itemPorts;

        if (ports[face.ordinal()] == mode) {
            return;
        }

        ports[face.ordinal()] = mode;
        setChanged();

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.human.common.gameplay.machine.MachinePorts.refresh(serverLevel, worldPosition);
        }
    }

    /** The old fixed layout, which is what every machine starts with - and what an old world's machines keep. */
    private void resetPorts() {
        var faces = kind.faces();
        java.util.Arrays.fill(itemPorts, com.human.common.gameplay.machine.MachinePorts.Mode.OFF);
        java.util.Arrays.fill(fluidPorts, com.human.common.gameplay.machine.MachinePorts.Mode.OFF);

        if (faces.inTop) {
            itemPorts[com.human.common.gameplay.machine.MachinePorts.Face.TOP.ordinal()] =
                com.human.common.gameplay.machine.MachinePorts.Mode.IN;
        }

        if (faces.inLeft) {
            itemPorts[com.human.common.gameplay.machine.MachinePorts.Face.LEFT.ordinal()] =
                com.human.common.gameplay.machine.MachinePorts.Mode.IN;
        }

        if (faces.outBottom) {
            itemPorts[com.human.common.gameplay.machine.MachinePorts.Face.BOTTOM.ordinal()] =
                com.human.common.gameplay.machine.MachinePorts.Mode.OUT;
        }

        if (faces.outRight) {
            itemPorts[com.human.common.gameplay.machine.MachinePorts.Face.RIGHT.ordinal()] =
                com.human.common.gameplay.machine.MachinePorts.Mode.OUT;
        }

        if (faces.inRight) {
            itemPorts[com.human.common.gameplay.machine.MachinePorts.Face.RIGHT.ordinal()] =
                com.human.common.gameplay.machine.MachinePorts.Mode.IN;
        }

        if (faces.outLeft) {
            itemPorts[com.human.common.gameplay.machine.MachinePorts.Face.LEFT.ordinal()] =
                com.human.common.gameplay.machine.MachinePorts.Mode.OUT;
        }

        var tank = kind.tank();

        if (tank != null) {
            if (tank.output()) {
                fluidPorts[com.human.common.gameplay.machine.MachinePorts.Face.BOTTOM.ordinal()] =
                    com.human.common.gameplay.machine.MachinePorts.Mode.OUT;
            } else {
                fluidPorts[kind.fluidInFace().ordinal()] = com.human.common.gameplay.machine.MachinePorts.Mode.IN;
            }
        }
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return slot == SLOT_INPUT || isFillSlot(slot) && MachineFluidItems.isFillable(stack);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (var stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        var removed = ContainerHelper.removeItem(items, slot, amount);

        if (!removed.isEmpty()) {
            setChanged();
        }

        return removed;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        var previous = items.get(slot);
        var changedInput = slot == SLOT_INPUT && !ItemStack.isSameItemSameComponents(previous, stack);

        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));

        if (changedInput) {
            progress = 0;
        }

        setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public @NotNull Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new ElectricMachineMenu(kind, containerId, inventory, this);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Save
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt(NBT_PROGRESS, progress);
        tag.putInt(NBT_TOTAL, total);
        tag.putBoolean(NBT_ENABLED, enabled);
        tag.putString(NBT_PRIORITY, priority.id());
        tag.putFloat(NBT_XP, storedXp);
        tag.put(NBT_FILTER, filter.createTag(registries));
        tag.put(NBT_UPGRADES, upgrades.createTag(registries));
        tag.putInt(NBT_REDSTONE, redstoneMode.ordinal());
        tag.putInt(NBT_PORTS_ITEMS, com.human.common.gameplay.machine.MachinePorts.pack(itemPorts));
        tag.putInt(NBT_PORTS_FLUIDS, com.human.common.gameplay.machine.MachinePorts.pack(fluidPorts));

        if (!fluid.isEmpty()) {
            tag.putString(NBT_FLUID, BuiltInRegistries.FLUID.getKey(fluid.fluid()).toString());
            tag.putInt(NBT_FLUID_AMOUNT, fluid.amount());
        }

        tag.putBoolean(NBT_KEEP_ITEMS, keepItemOutput);
        tag.putBoolean(NBT_KEEP_FLUID, keepFluidOutput);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        // A client update carrying only the tank (vaporizer): read the tank, touch nothing else.
        if (tag.getBoolean(NBT_CLIENT_SYNC)) {
            var syncedId = net.minecraft.resources.ResourceLocation.tryParse(tag.getString(NBT_FLUID));
            var synced = syncedId == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(syncedId);

            fluid = FluidContent.of(synced, Math.min(tag.getInt(NBT_FLUID_AMOUNT), getCapacity()));
            return;
        }

        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = Math.max(0, tag.getInt(NBT_PROGRESS));
        total = Math.max(0, tag.getInt(NBT_TOTAL));
        // ⚠ Absent means ON: a machine saved before this field existed, or a pasted tag, comes back working.
        enabled = !tag.contains(NBT_ENABLED) || tag.getBoolean(NBT_ENABLED);
        priority = MachinePriority.byId(tag.getString(NBT_PRIORITY));
        storedXp = Math.max(0.0F, tag.getFloat(NBT_XP));
        filter.fromTag(tag.getList(NBT_FILTER, net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
        if (tag.contains(NBT_UPGRADES)) {
            upgrades.fromTag(tag.getList(NBT_UPGRADES, net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
        } else if (tag.contains(NBT_HUB)) {
            // A machine saved before the upgrade slots: its hub chip moves into the first slot, so nothing is lost.
            var old = new SimpleContainer(1);
            old.fromTag(tag.getList(NBT_HUB, net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
            upgrades.setItem(0, old.getItem(0));
        }

        redstoneMode = com.human.common.gameplay.machine.MachineUpgrades.RedstoneMode.byOrdinal(tag.getInt(NBT_REDSTONE));
        recountUpgrades();

        // A machine saved before ports existed keeps its old fixed layout - the defaults.
        resetPorts();

        if (tag.contains(NBT_PORTS_ITEMS)) {
            var packed = tag.getInt(NBT_PORTS_ITEMS);

            for (var face : com.human.common.gameplay.machine.MachinePorts.Face.values()) {
                itemPorts[face.ordinal()] = com.human.common.gameplay.machine.MachinePorts.unpack(packed, face);
            }
        }

        if (tag.contains(NBT_PORTS_FLUIDS)) {
            var packed = tag.getInt(NBT_PORTS_FLUIDS);

            for (var face : com.human.common.gameplay.machine.MachinePorts.Face.values()) {
                fluidPorts[face.ordinal()] = com.human.common.gameplay.machine.MachinePorts.unpack(packed, face);
            }
        }

        // ⚠ A fluid from a mod since removed reads back as EMPTY rather than failing the load.
        var fluidId = net.minecraft.resources.ResourceLocation.tryParse(tag.getString(NBT_FLUID));
        var loaded = fluidId == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(fluidId);

        fluid = FluidContent.of(loaded, Math.min(tag.getInt(NBT_FLUID_AMOUNT), getCapacity()));
        // Absent means ON, like "enabled": a press saved before these switches existed keeps everything.
        keepItemOutput = !tag.contains(NBT_KEEP_ITEMS) || tag.getBoolean(NBT_KEEP_ITEMS);
        keepFluidOutput = !tag.contains(NBT_KEEP_FLUID) || tag.getBoolean(NBT_KEEP_FLUID);
    }
}
