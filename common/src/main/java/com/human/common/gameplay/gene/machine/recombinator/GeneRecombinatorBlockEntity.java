package com.human.common.gameplay.gene.machine.recombinator;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehavior;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.gene.machine.GeneRecombination;
import com.human.common.gameplay.gene.machine.storage.GeneStorageNetwork;
import com.human.common.gameplay.gene.tube.GeneTubeItem;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.registry.init.HumanGeneRecombinator;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Gene Recombinator's core: a sample tube in the cradle, a vial in each arm's clamp, and an EMPTIES BACK slot. The
 * rules are {@link GeneRecombination}'s ([stated] Oct 28): the sample gains one dose of the stronger vial, never past
 * the cap; only base and loot/harvest genes; vials must match the sample's gene; both vials come back empty; 10 seconds
 * a run.
 * <h2>RUN</h2> Starts by itself (AUTO START, on by default) when the three tubes recombine and the empties slot has
 * room for two, or by START. 40 FE/t while running, only advancing on ticks that were paid for; STOP or pulling a tube
 * out ends the run with nothing used up.
 * <h2>PULL FROM NETWORK</h2> [stated] off by default. On, once a second it fills an empty arm from Gene Storage on the
 * same power grid with the strongest tube of the sample's gene. The screen can also load a chosen tube by hand.
 * <h2>CLIPS</h2> One track: idle (arms empty) or load (both vials clamped); a run plays load, pour, return, release,
 * mix and press in 10 seconds. The fluid in the vials, chamber and output is drawn in the gene's colour.
 */
public class GeneRecombinatorBlockEntity extends PowerConsumerBlockEntity {

    public static final int SAMPLE_SLOT = 0;

    public static final int LEFT_SLOT = 1;

    public static final int RIGHT_SLOT = 2;

    public static final int EMPTIES_SLOT = 3;

    public static final int SLOT_COUNT = 4;

    public static final long RUN_POWER = 40L;

    public static final int RUN_TICKS = GeneRecombination.RUN_TICKS;

    private static final int SECOND = 20;

    private static final String NBT_ITEMS = "Items";

    private static final String NBT_RUNNING = "Running";

    private static final String NBT_PROGRESS = "Progress";

    private static final String NBT_AUTO = "AutoStart";

    private static final String NBT_PULL = "Pull";

    private static final String NBT_SUPPLIED = "Supplied";

    private final SimpleContainer items = new SimpleContainer(SLOT_COUNT);

    private boolean running;

    private int progress;

    private boolean autoStart = true;

    private boolean pull;

    private boolean supplied;

    private int secondTimer;

    public GeneRecombinatorBlockEntity(BlockPos pos, BlockState state) {
        super(HumanGeneRecombinator.BLOCK_ENTITY.get(), pos, state);
        items.addListener(container -> {
            setChanged();
            sync();
        });
    }

    public SimpleContainer items() {
        return items;
    }

    public boolean isRunning() {
        return running;
    }

    public int progress() {
        return progress;
    }

    public boolean isSupplied() {
        return supplied;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public boolean isPull() {
        return pull;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
        setChanged();
    }

    public void setPull(boolean pull) {
        this.pull = pull;
        setChanged();
    }

    public static boolean isFilledTube(ItemStack stack) {
        return GeneTubeItem.read(stack) != null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------------------------------------------------

    /** Right-click with a tube: the cradle if empty, else an empty arm. False when there is nowhere for it. */
    public boolean loadByHand(ItemStack stack) {
        for (var slot : new int[] { SAMPLE_SLOT, LEFT_SLOT, RIGHT_SLOT }) {
            if (items.getItem(slot).isEmpty()) {
                items.setItem(slot, stack.split(1));
                if (level != null) {
                    level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 1.5F);
                }
                return true;
            }
        }
        return false;
    }

    /** Why a run can't start, or null when it can. */
    public @Nullable String blocker() {
        var outcome = GeneRecombination.combine(items.getItem(SAMPLE_SLOT), items.getItem(LEFT_SLOT), items.getItem(RIGHT_SLOT));
        if (!outcome.ok()) {
            return outcome.refusal().name();
        }
        var empties = items.getItem(EMPTIES_SLOT);
        if (
            !empties.isEmpty() && (!empties.is(HumanGeneTubes.EMPTY_GENE_TUBE.get()) || empties.getCount()
                + GeneRecombination.EMPTIES_BACK > empties.getMaxStackSize())
        ) {
            return "EMPTIES_FULL";
        }
        if (!supplied) {
            return "NO_POWER";
        }
        return null;
    }

    public boolean start() {
        if (running || blocker() != null) {
            return false;
        }
        running = true;
        progress = 0;
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.4F, 1.6F);
        }
        setChanged();
        sync();
        return true;
    }

    public void stop() {
        if (running) {
            running = false;
            progress = 0;
            setChanged();
            sync();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------------------------

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
            if (!GeneRecombination.combine(items.getItem(SAMPLE_SLOT), items.getItem(LEFT_SLOT), items.getItem(RIGHT_SLOT)).ok()) {
                stop();
                return;
            }
            if (++progress >= RUN_TICKS) {
                finish(level);
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
        if (nowSupplied != supplied) {
            supplied = nowSupplied;
            sync();
        }
        if (pull && supplied && !running) {
            autoPull(serverLevel);
        }
        if (!running && autoStart && blocker() == null) {
            start();
        }
    }

    /** PULL FROM NETWORK: an empty arm takes the strongest tube of the sample's gene from Gene Storage on this grid. */
    private void autoPull(ServerLevel level) {
        var sample = GeneTubeItem.read(items.getItem(SAMPLE_SLOT));
        if (sample == null) {
            return;
        }
        for (var arm : new int[] { LEFT_SLOT, RIGHT_SLOT }) {
            if (!items.getItem(arm).isEmpty()) {
                continue;
            }
            GeneStorageNetwork.Member best = null;
            var bestSlot = -1;
            var bestDose = 0.0D;
            for (var member : GeneStorageNetwork.storagesFor(level, worldPosition, worldPosition)) {
                var rack = member.rack();
                for (int slot = 0; slot < rack.getContainerSize(); slot++) {
                    var contents = GeneTubeItem.read(rack.getItem(slot));
                    if (contents != null && contents.gene().equals(sample.gene()) && Math.abs(contents.perDose()) > bestDose) {
                        best = member;
                        bestSlot = slot;
                        bestDose = Math.abs(contents.perDose());
                    }
                }
            }
            if (best == null) {
                return;
            }
            items.setItem(arm, best.rack().removeItem(bestSlot, 1));
            best.rackChanged();
        }
    }

    /** The screen's network list: load that storage slot's tube into an empty arm (or the cradle if it is empty). */
    public boolean pullFrom(GeneStorageNetwork.Member member, int slot) {
        var tube = GeneTubeItem.read(member.rack().getItem(slot));
        if (tube == null || running) {
            return false;
        }
        var sample = GeneTubeItem.read(items.getItem(SAMPLE_SLOT));
        int target;
        if (sample == null) {
            target = SAMPLE_SLOT;
        } else if (!sample.gene().equals(tube.gene())) {
            return false;
        } else if (items.getItem(LEFT_SLOT).isEmpty()) {
            target = LEFT_SLOT;
        } else if (items.getItem(RIGHT_SLOT).isEmpty()) {
            target = RIGHT_SLOT;
        } else {
            return false;
        }
        items.setItem(target, member.rack().removeItem(slot, 1));
        member.rackChanged();
        return true;
    }

    private void finish(Level level) {
        var outcome = GeneRecombination.combine(items.getItem(SAMPLE_SLOT), items.getItem(LEFT_SLOT), items.getItem(RIGHT_SLOT));
        running = false;
        progress = 0;
        if (outcome.ok()) {
            items.setItem(SAMPLE_SLOT, outcome.result());
            items.setItem(LEFT_SLOT, ItemStack.EMPTY);
            items.setItem(RIGHT_SLOT, ItemStack.EMPTY);
            var empties = items.getItem(EMPTIES_SLOT);
            if (empties.isEmpty()) {
                items.setItem(EMPTIES_SLOT, new ItemStack(HumanGeneTubes.EMPTY_GENE_TUBE.get(), GeneRecombination.EMPTIES_BACK));
            } else {
                empties.grow(GeneRecombination.EMPTIES_BACK);
                items.setChanged();
            }
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        setChanged();
        sync();
    }

    public void use(Player player) {
        player.openMenu(
            new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, opener) -> new GeneRecombinatorMenu(id, inventory, this),
                Component.translatable(getBlockState().getBlock().getDescriptionId())
            )
        );
    }

    // ------------------------------------------------------------------------------------------------------------
    // Client
    // ------------------------------------------------------------------------------------------------------------

    /** Colour of the gene being worked: the sample's, else either vial's; 0 for none. */
    public int fluidColour() {
        for (var slot : new int[] { SAMPLE_SLOT, LEFT_SLOT, RIGHT_SLOT }) {
            var stack = items.getItem(slot);
            if (isFilledTube(stack)) {
                return GeneTubeItem.colour(stack) | 0xFF000000;
            }
        }
        return 0;
    }

    private static final String TRACK = "base_controller";

    private static AzCommand<GeneRecombinatorBlockEntity> clip(String name, AzPlayBehavior behavior) {
        return AzCommand.<GeneRecombinatorBlockEntity>idempotent().play(AzTarget.track(TRACK), name, behavior).build();
    }

    private static final AzCommand<GeneRecombinatorBlockEntity> IDLE = clip("idle", AzPlayBehaviors.LOOP);

    private static final AzCommand<GeneRecombinatorBlockEntity> LOAD = clip("load", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand<GeneRecombinatorBlockEntity> POUR = clip("pour", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand<GeneRecombinatorBlockEntity> RETURN = clip("return", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand<GeneRecombinatorBlockEntity> RELEASE = clip("release", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand<GeneRecombinatorBlockEntity> MIX = clip("mix", AzPlayBehaviors.LOOP);

    private static final AzCommand<GeneRecombinatorBlockEntity> PRESS = clip("press", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    /** The run's clips and the tick each starts on: load 0.6 s, pour 3.4, return 1.6, release 0.6, mix, press 1.5. */
    private static final int[] RUN_STARTS = { 0, 12, 80, 112, 124, 170 };

    @SuppressWarnings("unchecked")
    private static final AzCommand<GeneRecombinatorBlockEntity>[] RUN_CLIPS = new AzCommand[] { LOAD, POUR, RETURN, RELEASE, MIX, PRESS };

    private int shownPose = -2;

    private int clientRunTick = -1;

    public void clientTick() {
        if (level == null || com.blib.internal.client.animation.AzAnimatorAccessor.getOrNull(this) == null) {
            return;
        }
        if (running) {
            // The client runs its own clock from the start sync; a paused (unpowered) run holds its clip.
            if (clientRunTick < 0) {
                clientRunTick = 0;
            } else if (supplied && clientRunTick < RUN_TICKS) {
                clientRunTick++;
            }
            var stage = 0;
            for (int i = RUN_STARTS.length - 1; i >= 0; i--) {
                if (clientRunTick >= RUN_STARTS[i]) {
                    stage = i;
                    break;
                }
            }
            if (shownPose != 10 + stage) {
                RUN_CLIPS[stage].dispatchForBlockEntity(this);
                shownPose = 10 + stage;
            }
            return;
        }
        clientRunTick = -1;
        // Still: both vials clamped shows the load pose, otherwise the arms rest empty.
        var pose = isFilledTube(items.getItem(LEFT_SLOT)) && isFilledTube(items.getItem(RIGHT_SLOT)) ? 1 : 0;
        if (pose != shownPose) {
            (pose == 1 ? LOAD : IDLE).dispatchForBlockEntity(this);
            shownPose = pose;
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
        tag.putBoolean(NBT_AUTO, autoStart);
        tag.putBoolean(NBT_PULL, pull);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NBT_ITEMS)) {
            items.getItems().clear();
            ContainerHelper.loadAllItems(tag.getCompound(NBT_ITEMS), items.getItems(), registries);
        }
        running = tag.getBoolean(NBT_RUNNING);
        progress = tag.getInt(NBT_PROGRESS);
        autoStart = !tag.contains(NBT_AUTO) || tag.getBoolean(NBT_AUTO);
        pull = tag.getBoolean(NBT_PULL);
        supplied = tag.getBoolean(NBT_SUPPLIED);
    }

    /** The client needs the tubes (fluid colour, vials shown) and the run state for the clips. */
    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.put(NBT_ITEMS, ContainerHelper.saveAllItems(new CompoundTag(), items.getItems(), true, registries));
        tag.putBoolean(NBT_RUNNING, running);
        tag.putInt(NBT_PROGRESS, progress);
        tag.putBoolean(NBT_SUPPLIED, supplied);
        return tag;
    }
}
