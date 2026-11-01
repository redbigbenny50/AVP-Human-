package com.human.common.gameplay.gene.machine.separator;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehavior;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.gene.machine.storage.GeneStorageNetwork;
import com.human.common.gameplay.item.SyringeItem;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.HumanGeneSeparator;
import com.human.common.registry.init.HumanGeneTubes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Gene Separator's core: a centrifuge that splits a filled syringe into one filled tube per gene ([stated] Oct 27).
 * <h2>SLOTS</h2> The sample (a filled syringe), the TUBE BAY (9 stacks of empty tubes, 576 - fed by the item port too)
 * and its own 27-slot OUTPUT storage ([stated] ~27 slots, with a toggle to send finished tubes to Gene Storage).
 * <h2>RUN</h2> Needs one empty tube per gene and room in its own output for every tube, else it waits and says why
 * ([stated] refuses to run with a warning if there aren't enough tubes). A run takes {@code 8 + genes} seconds at
 * {@link #RUN_POWER} FE/t, only advancing on ticks that were paid for. At the end each gene goes into its own tube with
 * the syringe's donor, the syringe comes back EMPTY in the sample slot, and separating costs no integrity. AUTO START
 * (on by default) starts a run by itself as soon as everything is ready; START / STOP run it by hand.
 * <h2>NETWORK</h2> With OUTPUT TO NETWORK on, finished tubes go to Gene Storage on the same power grid, nearest first
 * ([stated] Oct 29); whatever doesn't fit stays in the output and is pushed again once a second, and the screen says
 * the network is full.
 * <h2>POWER</h2> No idle draw; grid supply read once a second like the other genetic machines.
 */
public class GeneSeparatorBlockEntity extends PowerConsumerBlockEntity {

    public static final int SAMPLE_SLOT = 0;

    public static final int BAY_START = 1;

    public static final int BAY_SLOTS = 9;

    public static final int OUTPUT_START = BAY_START + BAY_SLOTS;

    public static final int OUTPUT_SLOTS = 27;

    public static final int SLOT_COUNT = OUTPUT_START + OUTPUT_SLOTS;

    public static final long RUN_POWER = 40L;

    public static final int BASE_SECONDS = 8;

    /** The rotor's spin-up and spin-down clips are 2 s each. */
    public static final int SPIN_TICKS = 40;

    private static final int SECOND = 20;

    private static final String NBT_ITEMS = "Items";

    private static final String NBT_RUNNING = "Running";

    private static final String NBT_PROGRESS = "Progress";

    private static final String NBT_RUN_TICKS = "RunTicks";

    private static final String NBT_TO_NETWORK = "ToNetwork";

    private static final String NBT_AUTO = "AutoStart";

    private static final String NBT_SUPPLIED = "Supplied";

    private static final String NBT_NETWORK_FULL = "NetworkFull";

    private static final String NBT_LID = "Lid";

    private final SimpleContainer items = new SimpleContainer(SLOT_COUNT);

    private boolean running;

    private int progress;

    private int runTicks;

    private boolean toNetwork = true;

    private boolean autoStart = true;

    private boolean supplied;

    private boolean networkFull;

    private int secondTimer;

    private int viewers;

    public GeneSeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(HumanGeneSeparator.BLOCK_ENTITY.get(), pos, state);
        items.addListener(container -> setChanged());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Contents
    // ------------------------------------------------------------------------------------------------------------

    public SimpleContainer items() {
        return items;
    }

    public static boolean acceptsSample(ItemStack stack) {
        if (!(stack.getItem() instanceof SyringeItem)) {
            return false;
        }
        var contents = stack.get(HumanDataComponents.SYRINGE_CONTENTS.get());
        return contents != null && !contents.geneBonusDataEntries().isEmpty();
    }

    public static boolean isEmptyTube(ItemStack stack) {
        return stack.is(HumanGeneTubes.EMPTY_GENE_TUBE.get());
    }

    /** Genes in the sample - the tubes a run needs. */
    public int genesInSample() {
        var sample = items.getItem(SAMPLE_SLOT);
        // GeneSeparation's count: integrity is not a gene to tube, and a gene listed twice makes one tube.
        return acceptsSample(sample) ? com.human.common.gameplay.gene.machine.GeneSeparation.tubesNeeded(sample) : 0;
    }

    public int emptyTubes() {
        var count = 0;
        for (int slot = BAY_START; slot < BAY_START + BAY_SLOTS; slot++) {
            if (isEmptyTube(items.getItem(slot))) {
                count += items.getItem(slot).getCount();
            }
        }
        return count;
    }

    public int freeOutputSlots() {
        var free = 0;
        for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUT_SLOTS; slot++) {
            if (items.getItem(slot).isEmpty()) {
                free++;
            }
        }
        return free;
    }

    public int storedTubes() {
        return OUTPUT_SLOTS - freeOutputSlots();
    }

    public static int runTicksFor(int genes) {
        return com.human.common.gameplay.gene.machine.GeneSeparation.runTicks(genes);
    }

    // ------------------------------------------------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------------------------------------------------

    public boolean isRunning() {
        return running;
    }

    public int progress() {
        return progress;
    }

    public int runTicks() {
        return runTicks;
    }

    public boolean isSupplied() {
        return supplied;
    }

    public boolean isToNetwork() {
        return toNetwork;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public boolean isNetworkFull() {
        return networkFull;
    }

    public void setToNetwork(boolean toNetwork) {
        this.toNetwork = toNetwork;
        networkFull = false;
        setChanged();
        sync();
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
        setChanged();
    }

    /** Why a run can't start right now, or null when it can. */
    public @Nullable Blocker blocker() {
        var genes = genesInSample();
        if (genes == 0) {
            return Blocker.NO_SAMPLE;
        }
        if (emptyTubes() < genes) {
            return Blocker.TUBES;
        }
        if (freeOutputSlots() < genes) {
            return Blocker.OUTPUT_FULL;
        }
        if (!supplied) {
            return Blocker.NO_POWER;
        }
        return null;
    }

    public enum Blocker {
        NO_SAMPLE,
        TUBES,
        OUTPUT_FULL,
        NO_POWER
    }

    public boolean start() {
        if (running || blocker() != null) {
            return false;
        }
        running = true;
        progress = 0;
        runTicks = runTicksFor(genesInSample());
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.4F, 1.4F);
        }
        setChanged();
        sync();
        return true;
    }

    /** STOP: the run ends with nothing used up - the syringe and tubes stay as they were. */
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        progress = 0;
        setChanged();
        sync();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------------------------

    /** [stated] Oct 31: power from underneath only. */
    @Override
    public boolean canConnectOn(Direction direction) {
        var state = getBlockState();
        // [stated] Oct 31: genetic machines take power from the BOTTOM ONLY - a back port would sit over the models'
        // counters. Every bottom block of the machine takes a cable for this core (see the block's coreThrough).
        return direction == Direction.DOWN;
    }

    @Override
    public long getRequestedPower() {
        return running ? RUN_POWER : 0L;
    }

    @Override
    public void poweredTick(Level level, BlockPos pos, BlockState state) {
        everySecond(level);
        if (running) {
            // A run whose sample or tubes were pulled out mid-spin stops cleanly instead of finishing on nothing.
            if (genesInSample() == 0 || emptyTubes() < genesInSample() || freeOutputSlots() < genesInSample()) {
                stop();
                return;
            }
            progress++;
            if (progress >= runTicks) {
                finish(level);
            } else if (progress == runTicks - SPIN_TICKS) {
                sync();
            }
        }
    }

    @Override
    public void unpoweredTick(Level level, BlockPos pos, BlockState state) {
        everySecond(level);
    }

    private void everySecond(Level level) {
        if (--secondTimer > 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        secondTimer = SECOND;

        var grid = PowerSystem.get(serverLevel).gridAt(worldPosition);
        var nowSupplied = grid != null && grid.hasSupply();
        var changed = nowSupplied != supplied;
        supplied = nowSupplied;

        changed |= pushToNetwork(serverLevel);

        if (!running && autoStart && blocker() == null) {
            start();
        }
        if (changed) {
            sync();
        }
    }

    /** Sends held output to Gene Storage on this grid. Returns true when the NETWORK FULL state changed. */
    private boolean pushToNetwork(ServerLevel level) {
        var wasFull = networkFull;
        if (!toNetwork || !supplied) {
            networkFull = false;
            return wasFull;
        }
        var held = false;
        for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUT_SLOTS; slot++) {
            var stack = items.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            var left = GeneStorageNetwork.store(level, worldPosition, worldPosition, stack);
            if (left.getCount() != stack.getCount()) {
                items.setItem(slot, left);
            }
            held |= !left.isEmpty();
        }
        networkFull = held;
        return wasFull != networkFull;
    }

    private void finish(Level level) {
        var sample = items.getItem(SAMPLE_SLOT);
        // The shared rules (GeneSeparation): one tube per gene, one dose, the syringe's donor.
        for (var tube : com.human.common.gameplay.gene.machine.GeneSeparation.tubesFor(sample)) {
            takeEmptyTube();
            place(tube);
        }
        // [stated] the syringe comes back empty; separating costs no integrity.
        com.human.common.gameplay.gene.machine.GeneSeparation.emptySyringe(sample);
        items.setItem(SAMPLE_SLOT, sample);

        running = false;
        progress = 0;
        level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6F, 1.4F);
        if (level instanceof ServerLevel serverLevel) {
            pushToNetwork(serverLevel);
        }
        setChanged();
        sync();
    }

    private void takeEmptyTube() {
        for (int slot = BAY_START + BAY_SLOTS - 1; slot >= BAY_START; slot--) {
            if (isEmptyTube(items.getItem(slot))) {
                items.removeItem(slot, 1);
                return;
            }
        }
    }

    private void place(ItemStack tube) {
        for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUT_SLOTS; slot++) {
            if (items.getItem(slot).isEmpty()) {
                items.setItem(slot, tube);
                return;
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Use, lid, item port
    // ------------------------------------------------------------------------------------------------------------

    /** Right-click: the centrifuge screen, and the lid opens while anyone has it open (unless it is spinning). */
    public void use(Player player) {
        player.openMenu(
            new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, opener) -> new GeneSeparatorMenu(id, inventory, this),
                Component.translatable(getBlockState().getBlock().getDescriptionId())
            )
        );
    }

    void startViewing() {
        if (viewers++ == 0) {
            sync();
        }
    }

    void stopViewing() {
        if (viewers > 0 && --viewers == 0) {
            sync();
        }
    }

    public boolean isLidOpen() {
        return viewers > 0 && !running;
    }

    /** No port at all - for the blocks of the machine that aren't its item port. */
    public static final WorldlyContainer NO_PORT = com.human.common.gameplay.gene.machine.MachinePort.CLOSED;

    private static final int[] BAY = java.util.stream.IntStream.range(BAY_START, BAY_START + BAY_SLOTS).toArray();

    /** The item port: empty tubes in, through {@code portFace} only, into the tube bay. Nothing comes out. */
    public WorldlyContainer port(@Nullable Direction portFace) {
        return new com.human.common.gameplay.gene.machine.MachinePort(
            items,
            portFace,
            BAY,
            GeneSeparatorBlockEntity::isEmptyTube,
            () -> !isRemoved()
        );
    }

    // ------------------------------------------------------------------------------------------------------------
    // Client clips: lid, rotor and screen run on their own tracks (their bones never overlap)
    // ------------------------------------------------------------------------------------------------------------

    public static final String LID_TRACK = "lid";

    public static final String ROTOR_TRACK = "rotor";

    public static final String SCREEN_TRACK = "screen";

    private static AzCommand<GeneSeparatorBlockEntity> clip(String track, String name, AzPlayBehavior behavior) {
        return AzCommand.<GeneSeparatorBlockEntity>idempotent().play(AzTarget.track(track), name, behavior).build();
    }

    private static final AzCommand<GeneSeparatorBlockEntity> LID_OPEN = clip(LID_TRACK, "lid_open", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand<GeneSeparatorBlockEntity> LID_CLOSE = clip(LID_TRACK, "lid_close", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand<GeneSeparatorBlockEntity> SPIN_UP = clip(ROTOR_TRACK, "spin_up", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand<GeneSeparatorBlockEntity> SPINNING = clip(ROTOR_TRACK, "spinning", AzPlayBehaviors.LOOP);

    private static final AzCommand<GeneSeparatorBlockEntity> SPIN_DOWN = clip(ROTOR_TRACK, "spin_down", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand<GeneSeparatorBlockEntity> SCREEN_OFF = clip(SCREEN_TRACK, "screen_off", AzPlayBehaviors.LOOP);

    private static final AzCommand<GeneSeparatorBlockEntity> SCREEN_ON = clip(SCREEN_TRACK, "screen_on", AzPlayBehaviors.LOOP);

    private static final AzCommand<GeneSeparatorBlockEntity> SCREEN_WORKING = clip(SCREEN_TRACK, "screen_working", AzPlayBehaviors.LOOP);

    private int shownLid = -1;

    private int shownRotor = -1;

    private int shownScreen = -1;

    private int spinChain = -1;

    private boolean clientLid;

    public void clientTick() {
        if (level == null || com.blib.internal.client.animation.AzAnimatorAccessor.getOrNull(this) == null) {
            return;
        }
        var lid = clientLid ? 1 : 0;
        if (lid != shownLid) {
            // Coming into view closed needs nothing: the model's rest pose is the closed lid.
            if (lid == 1) {
                LID_OPEN.dispatchForBlockEntity(this);
            } else if (shownLid == 1) {
                LID_CLOSE.dispatchForBlockEntity(this);
            }
            shownLid = lid;
        }

        // Rotor: 0 still, 1 spinning, 2 slowing for the last two seconds of a run.
        var rotor = !running ? 0 : progress >= runTicks - SPIN_TICKS ? 2 : 1;
        if (rotor != shownRotor) {
            switch (rotor) {
                case 1 -> {
                    SPIN_UP.dispatchForBlockEntity(this);
                    spinChain = SPIN_TICKS;
                }
                case 2 -> {
                    spinChain = -1;
                    SPIN_DOWN.dispatchForBlockEntity(this);
                }
                default -> {
                    spinChain = -1;
                    if (shownRotor == 1) {
                        SPIN_DOWN.dispatchForBlockEntity(this);
                    }
                }
            }
            shownRotor = rotor;
        }
        if (spinChain >= 0 && --spinChain == 0) {
            SPINNING.dispatchForBlockEntity(this);
            spinChain = -1;
        }

        var screen = !supplied ? 0 : running ? 2 : 1;
        if (screen != shownScreen) {
            (screen == 0 ? SCREEN_OFF : screen == 2 ? SCREEN_WORKING : SCREEN_ON).dispatchForBlockEntity(this);
            shownScreen = screen;
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Save / sync
    // ------------------------------------------------------------------------------------------------------------

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(NBT_ITEMS, ContainerHelper.saveAllItems(new CompoundTag(), items.getItems(), true, registries));
        tag.putBoolean(NBT_RUNNING, running);
        tag.putInt(NBT_PROGRESS, progress);
        tag.putInt(NBT_RUN_TICKS, runTicks);
        tag.putBoolean(NBT_TO_NETWORK, toNetwork);
        tag.putBoolean(NBT_AUTO, autoStart);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NBT_ITEMS)) {
            items.clearContent();
            ContainerHelper.loadAllItems(tag.getCompound(NBT_ITEMS), items.getItems(), registries);
        }
        running = tag.getBoolean(NBT_RUNNING);
        progress = tag.getInt(NBT_PROGRESS);
        runTicks = tag.getInt(NBT_RUN_TICKS);
        toNetwork = !tag.contains(NBT_TO_NETWORK) || tag.getBoolean(NBT_TO_NETWORK);
        autoStart = !tag.contains(NBT_AUTO) || tag.getBoolean(NBT_AUTO);
        supplied = tag.getBoolean(NBT_SUPPLIED);
        networkFull = tag.getBoolean(NBT_NETWORK_FULL);
        clientLid = tag.getBoolean(NBT_LID);
    }

    /** The client needs the state for the clips; the slots reach the screen through its menu. */
    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.putBoolean(NBT_RUNNING, running);
        tag.putInt(NBT_PROGRESS, progress);
        tag.putInt(NBT_RUN_TICKS, runTicks);
        tag.putBoolean(NBT_TO_NETWORK, toNetwork);
        tag.putBoolean(NBT_AUTO, autoStart);
        tag.putBoolean(NBT_SUPPLIED, supplied);
        tag.putBoolean(NBT_NETWORK_FULL, networkFull);
        tag.putBoolean(NBT_LID, isLidOpen());
        return tag;
    }
}
