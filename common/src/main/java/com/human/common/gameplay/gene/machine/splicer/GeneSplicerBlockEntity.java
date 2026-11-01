package com.human.common.gameplay.gene.machine.splicer;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehavior;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.gene.GeneColours;
import com.human.common.gameplay.gene.machine.GeneSplicing;
import com.human.common.gameplay.gene.machine.MachinePort;
import com.human.common.gameplay.gene.machine.storage.GeneStorageNetwork;
import com.human.common.gameplay.gene.tube.GeneTubeItem;
import com.human.common.gameplay.item.SyringeItem;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.HumanGeneSplicer;
import com.human.common.registry.init.HumanGeneTubes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
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

import java.util.ArrayList;
import java.util.List;

/**
 * The Gene Splicer's core: a 5-tube rack, an IN stack of empty syringes (fed by the item port), the OUT cradle, and
 * EMPTIES BACK. The rules are {@link GeneSplicing}'s ([stated] Oct 28): up to 5 genes, a gene already in is refused,
 * one mutation per category (amber warning), defects allowed (red warning), the finished syringe is SPLICED and the
 * used tubes come back empty; 6 s plus 2 s per gene.
 * <h2>RUN</h2> Starts on START only - the player picks the genes first, so it never runs by itself on a half-built
 * syringe (the approved screen has no AUTO START). It needs the plan to check out, an empty syringe in IN, OUT empty
 * and room in EMPTIES BACK. 40 FE/t while running, only on paid ticks; STOP, or changing the tubes mid-run, ends it
 * with nothing used up. The finished syringe takes the screen's LABEL as its name.
 * <h2>CLIPS</h2> One track: idle; a run plays home, then pipette_slotN for each loaded holder (spread across the run),
 * then park. Tube and syringe fluid are drawn in gene colours.
 */
public class GeneSplicerBlockEntity extends PowerConsumerBlockEntity {

    public static final int TUBE_SLOTS = GeneSplicing.MAX_GENES;

    public static final int IN_SLOT = 5;

    public static final int OUT_SLOT = 6;

    public static final int EMPTIES_SLOT = 7;

    public static final int SLOT_COUNT = 8;

    public static final long RUN_POWER = 40L;

    public static final int MAX_LABEL = 24;

    private static final int SECOND = 20;

    private static final String NBT_ITEMS = "Items";

    private static final String NBT_RUNNING = "Running";

    private static final String NBT_PROGRESS = "Progress";

    private static final String NBT_RUN_TICKS = "RunTicks";

    private static final String NBT_AUTO = "AutoStart";

    private static final String NBT_LABEL = "Label";

    private static final String NBT_SUPPLIED = "Supplied";

    private final SimpleContainer items = new SimpleContainer(SLOT_COUNT);

    private boolean running;

    private int progress;

    private int runTicks;

    private boolean autoStart = false;

    private String label = "";

    private boolean supplied;

    private int secondTimer;

    public GeneSplicerBlockEntity(BlockPos pos, BlockState state) {
        super(HumanGeneSplicer.BLOCK_ENTITY.get(), pos, state);
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

    public int runTicks() {
        return runTicks;
    }

    public boolean isSupplied() {
        return supplied;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
        setChanged();
    }

    public String label() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label.length() > MAX_LABEL ? label.substring(0, MAX_LABEL) : label;
        setChanged();
    }

    public static boolean isEmptySyringe(ItemStack stack) {
        if (!(stack.getItem() instanceof SyringeItem)) {
            return false;
        }
        var contents = stack.get(HumanDataComponents.SYRINGE_CONTENTS.get());
        return contents == null || contents.geneBonusDataEntries().isEmpty();
    }

    public List<ItemStack> tubes() {
        var list = new ArrayList<ItemStack>();
        for (int slot = 0; slot < TUBE_SLOTS; slot++) {
            list.add(items.getItem(slot));
        }
        return list;
    }

    public GeneSplicing.Plan plan() {
        return GeneSplicing.plan(tubes());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------------------------------------------------

    /** Right-click with a tube: the first empty holder. False when the rack is full or that gene is already in. */
    public boolean loadByHand(ItemStack stack) {
        var contents = GeneTubeItem.read(stack);
        if (contents == null || running || holds(contents.gene())) {
            return false;
        }
        for (int slot = 0; slot < TUBE_SLOTS; slot++) {
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

    private boolean holds(net.minecraft.resources.ResourceLocation gene) {
        for (int slot = 0; slot < TUBE_SLOTS; slot++) {
            var contents = GeneTubeItem.read(items.getItem(slot));
            if (contents != null && contents.gene().equals(gene)) {
                return true;
            }
        }
        return false;
    }

    /** The screen's NETWORK STORAGE list: that tube into the first empty holder (refused if its gene is already in). */
    public boolean pullFrom(GeneStorageNetwork.Member member, int slot) {
        var contents = GeneTubeItem.read(member.rack().getItem(slot));
        if (contents == null || running || holds(contents.gene())) {
            return false;
        }
        for (int target = 0; target < TUBE_SLOTS; target++) {
            if (items.getItem(target).isEmpty()) {
                items.setItem(target, member.rack().removeItem(slot, 1));
                member.rackChanged();
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Running
    // ------------------------------------------------------------------------------------------------------------

    /** Why a run can't start: a plan refusal's name, NO_SYRINGE, OUT_FULL, EMPTIES_FULL, NO_POWER - or null. */
    public @Nullable String blocker() {
        var plan = plan();
        if (!plan.ok()) {
            return plan.refusal().name();
        }
        if (!isEmptySyringe(items.getItem(IN_SLOT))) {
            return "NO_SYRINGE";
        }
        if (!items.getItem(OUT_SLOT).isEmpty()) {
            return "OUT_FULL";
        }
        var empties = items.getItem(EMPTIES_SLOT);
        var genes = plan.entries().size();
        if (
            !empties.isEmpty() && (!empties.is(HumanGeneTubes.EMPTY_GENE_TUBE.get()) || empties.getCount() + genes > empties
                .getMaxStackSize())
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
        runTicks = GeneSplicing.runTicks(plan().entries().size());
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.4F, 1.8F);
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
            // Tubes or the syringe changed mid-run: stop with nothing used up.
            if (!plan().ok() || !isEmptySyringe(items.getItem(IN_SLOT)) || !items.getItem(OUT_SLOT).isEmpty()) {
                stop();
                return;
            }
            if (++progress >= runTicks) {
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
    }

    private void finish(Level level) {
        var plan = plan();
        running = false;
        progress = 0;
        if (plan.ok()) {
            var syringe = items.getItem(IN_SLOT).split(1);
            GeneSplicing.fill(syringe, plan);
            if (!label.isBlank()) {
                syringe.set(DataComponents.CUSTOM_NAME, Component.literal(label.trim()));
            }
            items.setItem(OUT_SLOT, syringe);
            for (int slot = 0; slot < TUBE_SLOTS; slot++) {
                items.setItem(slot, ItemStack.EMPTY);
            }
            var genes = plan.entries().size();
            var empties = items.getItem(EMPTIES_SLOT);
            if (empties.isEmpty()) {
                items.setItem(EMPTIES_SLOT, new ItemStack(HumanGeneTubes.EMPTY_GENE_TUBE.get(), genes));
            } else {
                empties.grow(genes);
                items.setChanged();
            }
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.5F);
        }
        setChanged();
        sync();
    }

    public void use(Player player) {
        player.openMenu(
            new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, opener) -> new GeneSplicerMenu(id, inventory, this),
                Component.translatable(getBlockState().getBlock().getDescriptionId())
            )
        );
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.human.Human.MOD.networking().sendToClient(serverPlayer, new com.human.common.network.packet.S2CSplicerLabelPayload(label));
        }
    }

    /** The item port: empty syringes in, through {@code portFace} only, into IN. */
    public WorldlyContainer port(@Nullable Direction portFace) {
        return new MachinePort(items, portFace, new int[] { IN_SLOT }, GeneSplicerBlockEntity::isEmptySyringe, () -> !isRemoved());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Client
    // ------------------------------------------------------------------------------------------------------------

    /** Fluid colour of rack holder {@code slot} (0-4), 0 for an empty holder. */
    public int tubeColour(int slot) {
        var stack = items.getItem(slot);
        return GeneTubeItem.read(stack) == null ? 0 : GeneTubeItem.colour(stack) | 0xFF000000;
    }

    /** The cradle syringe's colour: the finished syringe in OUT, else the blend of the planned genes while running. */
    public int syringeColour() {
        var out = items.getItem(OUT_SLOT);
        if (!out.isEmpty()) {
            var colour = GeneColours.syringe(out);
            return (colour >>> 24) == 0 ? 0 : colour;
        }
        if (running) {
            var plan = plan();
            if (plan.ok()) {
                var colour = GeneColours.blend(new com.human.common.gameplay.component.SyringeContents(plan.entries()));
                return (colour >>> 24) == 0 ? 0 : colour;
            }
        }
        return 0;
    }

    private static final String TRACK = "base_controller";

    private static AzCommand<GeneSplicerBlockEntity> clip(String name, AzPlayBehavior behavior) {
        return AzCommand.<GeneSplicerBlockEntity>idempotent().play(AzTarget.track(TRACK), name, behavior).build();
    }

    private static final AzCommand<GeneSplicerBlockEntity> IDLE = clip("idle", AzPlayBehaviors.LOOP);

    private static final AzCommand<GeneSplicerBlockEntity> HOME = clip("home", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand<GeneSplicerBlockEntity> PARK = clip("park", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    @SuppressWarnings("unchecked")
    private static final AzCommand<GeneSplicerBlockEntity>[] PIPETTE = new AzCommand[] {
        clip("pipette_slot1", AzPlayBehaviors.HOLD_ON_LAST_FRAME),
        clip("pipette_slot2", AzPlayBehaviors.HOLD_ON_LAST_FRAME),
        clip("pipette_slot3", AzPlayBehaviors.HOLD_ON_LAST_FRAME),
        clip("pipette_slot4", AzPlayBehaviors.HOLD_ON_LAST_FRAME),
        clip("pipette_slot5", AzPlayBehaviors.HOLD_ON_LAST_FRAME)
    };

    /** home 0.5 s and park 0.5 s; a pipette clip is 2.4 s, and each loaded holder gets an equal share of the rest. */
    private static final int HOME_TICKS = 10;

    private int shownStage = -2;

    private int clientRunTick = -1;

    public void clientTick() {
        if (level == null || com.blib.internal.client.animation.AzAnimatorAccessor.getOrNull(this) == null) {
            return;
        }
        if (!running || runTicks <= 0) {
            clientRunTick = -1;
            if (shownStage != -1) {
                IDLE.dispatchForBlockEntity(this);
                shownStage = -1;
            }
            return;
        }
        if (clientRunTick < 0) {
            clientRunTick = 0;
        } else if (supplied && clientRunTick < runTicks) {
            clientRunTick++;
        }
        var loaded = new ArrayList<Integer>();
        for (int slot = 0; slot < TUBE_SLOTS; slot++) {
            if (GeneTubeItem.read(items.getItem(slot)) != null) {
                loaded.add(slot);
            }
        }
        int stage;
        if (clientRunTick < HOME_TICKS || loaded.isEmpty()) {
            stage = 0;
        } else if (clientRunTick >= runTicks - HOME_TICKS) {
            stage = 100;
        } else {
            var share = (runTicks - 2 * HOME_TICKS) / loaded.size();
            stage = 1 + Math.min(loaded.size() - 1, (clientRunTick - HOME_TICKS) / Math.max(1, share));
        }
        if (stage != shownStage) {
            if (stage == 0) {
                HOME.dispatchForBlockEntity(this);
            } else if (stage == 100) {
                PARK.dispatchForBlockEntity(this);
            } else {
                PIPETTE[loaded.get(stage - 1)].dispatchForBlockEntity(this);
            }
            shownStage = stage;
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
        tag.putBoolean(NBT_AUTO, autoStart);
        tag.putString(NBT_LABEL, label);
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
        runTicks = tag.getInt(NBT_RUN_TICKS);
        autoStart = !tag.contains(NBT_AUTO) || tag.getBoolean(NBT_AUTO);
        label = tag.getString(NBT_LABEL);
        supplied = tag.getBoolean(NBT_SUPPLIED);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.put(NBT_ITEMS, ContainerHelper.saveAllItems(new CompoundTag(), items.getItems(), true, registries));
        tag.putBoolean(NBT_RUNNING, running);
        tag.putInt(NBT_PROGRESS, progress);
        tag.putInt(NBT_RUN_TICKS, runTicks);
        tag.putBoolean(NBT_SUPPLIED, supplied);
        return tag;
    }
}
