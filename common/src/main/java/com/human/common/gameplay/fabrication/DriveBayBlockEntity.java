package com.human.common.gameplay.fabrication;

import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.registry.init.HumanFabrication;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * [stated] Oct 13: the drive bay. Blueprints always live HERE, never on an SSD: each SSD in the bay only unlocks 16 of
 * its blueprint slots, up to 4 SSDs for 64. Take an SSD out and the slots grey out from the bottom up - their
 * blueprints stay, can still be taken out by hand, but no fabricator can print from them until the capacity is back.
 * <h2>NETWORK</h2> Fabricators find drive bays through the power grid they share ({@link #onGridOf}), so a bay is on a
 * network exactly when a power cable joins it. Nothing scans the world.
 * <h2>COST</h2> One boolean per tick from the power base class. Once a second it reads whether its grid has supply,
 * from numbers the grid already measured, and changes nothing unless that flips. It asks for power only while a
 * fabricator is reading one of its SSDs.
 * <h2>DATA SAFETY</h2> Every change calls {@link #setChanged()}. Unloading never touches the contents. Breaking it
 * drops ONE item that carries everything ({@link #collectImplicitComponents}), protected like a full crate.
 */
public class DriveBayBlockEntity extends PowerConsumerBlockEntity {

    public static final int SSD_SLOTS = 4;

    public static final int SLOTS_PER_SSD = 16;

    public static final int BLUEPRINT_SLOTS = SSD_SLOTS * SLOTS_PER_SSD;

    /** Power drawn while a fabricator reads from this bay, per tick. */
    public static final long READ_POWER = 8L;

    /** How long one read keeps an SSD's light on and the bay drawing power. */
    private static final int READ_TICKS = 40;

    private static final int SUPPLY_CHECK_TICKS = 20;

    private static final String NBT_SSDS = "Ssds";

    private static final String NBT_BLUEPRINTS = "Blueprints";

    private static final String NBT_SUPPLIED = "Supplied";

    private static final String NBT_READING = "Reading";

    private final NonNullList<ItemStack> ssds = NonNullList.withSize(SSD_SLOTS, ItemStack.EMPTY);

    private final NonNullList<ItemStack> blueprints = NonNullList.withSize(BLUEPRINT_SLOTS, ItemStack.EMPTY);

    /** Game time until which each SSD position is being read. Not saved: a read is a moment, not state. */
    private final long[] readUntil = new long[SSD_SLOTS];

    /** Synced to clients for the lights: the grid has supply, and which SSD positions are being read. */
    private boolean supplied;

    private int readingMask;

    private int supplyTimer;

    public DriveBayBlockEntity(BlockPos pos, BlockState state) {
        super(HumanFabrication.DRIVE_BAY_BLOCK_ENTITY.get(), pos, state);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Contents
    // ------------------------------------------------------------------------------------------------------------

    public ItemStack getSsd(int slot) {
        return ssds.get(slot);
    }

    public void setSsd(int slot, ItemStack stack) {
        ssds.set(slot, stack);
        contentsChanged();
    }

    public ItemStack getBlueprint(int slot) {
        return blueprints.get(slot);
    }

    public void setBlueprint(int slot, ItemStack stack) {
        blueprints.set(slot, stack);
        contentsChanged();
    }

    public int installedSsds() {
        var count = 0;

        for (var ssd : ssds) {
            if (!ssd.isEmpty()) {
                count++;
            }
        }

        return count;
    }

    /** How many blueprint slots are usable: 16 for each SSD installed. Slots past this are greyed. */
    public int capacity() {
        return installedSsds() * SLOTS_PER_SSD;
    }

    public static boolean isSsd(ItemStack stack) {
        return stack.is(HumanItems.SSD.get());
    }

    /** What a blueprint slot holds: a written blueprint chip, or a faction license (a whole faction's blueprints). */
    public static boolean isBlueprint(ItemStack stack) {
        return (stack.getItem() instanceof BlueprintChipItem && BlueprintChipItem.printed(stack) != null)
            || stack.getItem() instanceof FactionLicenseItem;
    }

    /** The chips and licenses in usable slots, with their slots - nothing while the grid has no supply. */
    public List<StackEntry> usableStacks() {
        var entries = new ArrayList<StackEntry>();

        if (!supplied) {
            return entries;
        }

        var capacity = capacity();

        for (int slot = 0; slot < capacity; slot++) {
            var stack = blueprints.get(slot);

            if (isBlueprint(stack)) {
                entries.add(new StackEntry(slot, stack));
            }
        }

        return entries;
    }

    public record StackEntry(
        int slot,
        ItemStack stack
    ) {}

    /** Puts an SSD in the first empty position. Returns false when all four are full. */
    public boolean insertSsd(ItemStack stack) {
        if (!isSsd(stack)) {
            return false;
        }

        for (int slot = 0; slot < SSD_SLOTS; slot++) {
            if (ssds.get(slot).isEmpty()) {
                setSsd(slot, stack.split(1));
                return true;
            }
        }

        return false;
    }

    /**
     * The blueprints a fabricator may print from right now: usable slots only, and nothing at all while the bay's grid
     * has no supply. Each entry keeps its slot, so a print can light the right SSD.
     */
    public List<Entry> usableBlueprints() {
        var entries = new ArrayList<Entry>();

        if (!supplied) {
            return entries;
        }

        var capacity = capacity();

        for (int slot = 0; slot < capacity; slot++) {
            var chip = blueprints.get(slot);
            var item = BlueprintChipItem.printed(chip);

            if (item != null) {
                entries.add(new Entry(slot, item));
            }
        }

        return entries;
    }

    public record Entry(
        int slot,
        net.minecraft.world.item.Item item
    ) {}

    // ------------------------------------------------------------------------------------------------------------
    // Network
    // ------------------------------------------------------------------------------------------------------------

    /** Every drive bay on the same power grid as {@code pos}. A lookup in the grid's own member set - no scan. */
    public static List<DriveBayBlockEntity> onGridOf(ServerLevel level, BlockPos pos) {
        var bays = new ArrayList<DriveBayBlockEntity>();
        var grid = PowerSystem.get(level).gridAt(pos);

        if (grid == null) {
            return bays;
        }

        for (var consumer : grid.getConsumers()) {
            if (consumer instanceof DriveBayBlockEntity bay && !bay.isRemoved()) {
                bays.add(bay);
            }
        }

        return bays;
    }

    /** A fabricator is reading the blueprint in {@code blueprintSlot}: light its SSD and draw power for a moment. */
    public void markReading(int blueprintSlot) {
        if (level == null || blueprintSlot < 0 || blueprintSlot >= capacity()) {
            return;
        }

        var position = ssdPositionFor(blueprintSlot);

        if (position >= 0) {
            readUntil[position] = level.getGameTime() + READ_TICKS;
            updateReadingMask();
        }
    }

    /**
     * Which SSD position a usable blueprint slot belongs to. Capacity counts installed SSDs, so the n-th group of 16
     * slots belongs to the n-th INSTALLED SSD, whichever positions they sit in.
     */
    private int ssdPositionFor(int blueprintSlot) {
        var group = blueprintSlot / SLOTS_PER_SSD;

        for (int position = 0; position < SSD_SLOTS; position++) {
            if (!ssds.get(position).isEmpty() && group-- == 0) {
                return position;
            }
        }

        return -1;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Power
    // ------------------------------------------------------------------------------------------------------------

    /** [stated] Sep 29: machines take power at the BACK. */
    @Override
    public boolean canConnectOn(Direction direction) {
        var state = getBlockState();

        return state.hasProperty(DriveBayBlock.FACING) && direction == state.getValue(DriveBayBlock.FACING).getOpposite();
    }

    @Override
    public long getRequestedPower() {
        return readingMask != 0 ? READ_POWER : 0L;
    }

    @Override
    public int getPowerPriority() {
        return PowerNode.PRIORITY_DEFERRABLE;
    }

    @Override
    public void unpoweredTick(Level level, BlockPos pos, BlockState state) {
        tickLights(level);
    }

    @Override
    public void poweredTick(Level level, BlockPos pos, BlockState state) {
        tickLights(level);
    }

    private void tickLights(Level level) {
        if (readingMask != 0) {
            updateReadingMask();
        }

        if (--supplyTimer > 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        supplyTimer = SUPPLY_CHECK_TICKS;

        var grid = PowerSystem.get(serverLevel).gridAt(worldPosition);
        var nowSupplied = grid != null && grid.hasSupply();

        if (nowSupplied != supplied) {
            supplied = nowSupplied;
            sync();
        }
    }

    private void updateReadingMask() {
        if (level == null) {
            return;
        }

        var now = level.getGameTime();
        var mask = 0;

        for (int position = 0; position < SSD_SLOTS; position++) {
            if (readUntil[position] > now && !ssds.get(position).isEmpty()) {
                mask |= 1 << position;
            }
        }

        if (mask != readingMask) {
            readingMask = mask;
            sync();
        }
    }

    public boolean isSupplied() {
        return supplied;
    }

    public int readingMask() {
        return readingMask;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Saving and syncing
    // ------------------------------------------------------------------------------------------------------------

    private void contentsChanged() {
        setChanged();
        updateReadingMask();
        sync();
    }

    /** Pushes the SSDs and lights to clients. Only called when something they draw actually changed. */
    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(NBT_SSDS, ContainerHelper.saveAllItems(new CompoundTag(), ssds, true, registries));
        tag.put(NBT_BLUEPRINTS, ContainerHelper.saveAllItems(new CompoundTag(), blueprints, true, registries));
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        clear(ssds);
        clear(blueprints);
        ContainerHelper.loadAllItems(tag.getCompound(NBT_SSDS), ssds, registries);
        ContainerHelper.loadAllItems(tag.getCompound(NBT_BLUEPRINTS), blueprints, registries);

        // Client copies carry the lights too; a save never contains them.
        supplied = tag.getBoolean(NBT_SUPPLIED);
        readingMask = tag.getInt(NBT_READING);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        var tag = super.getUpdateTag(registries);

        tag.putBoolean(NBT_SUPPLIED, supplied);
        tag.putInt(NBT_READING, readingMask);

        return tag;
    }

    /**
     * The whole bay goes onto its dropped item: SSDs first, then the 64 blueprint slots, in one container component. An
     * empty bay writes nothing, so empty bays still stack.
     */
    @Override
    protected void collectImplicitComponents(DataComponentMap.@NotNull Builder components) {
        super.collectImplicitComponents(components);

        if (isEmpty()) {
            return;
        }

        var all = NonNullList.withSize(SSD_SLOTS + BLUEPRINT_SLOTS, ItemStack.EMPTY);

        for (int slot = 0; slot < SSD_SLOTS; slot++) {
            all.set(slot, ssds.get(slot).copy());
        }

        for (int slot = 0; slot < BLUEPRINT_SLOTS; slot++) {
            all.set(SSD_SLOTS + slot, blueprints.get(slot).copy());
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

        var all = NonNullList.withSize(SSD_SLOTS + BLUEPRINT_SLOTS, ItemStack.EMPTY);

        contents.copyInto(all);

        for (int slot = 0; slot < SSD_SLOTS; slot++) {
            ssds.set(slot, all.get(slot));
        }

        for (int slot = 0; slot < BLUEPRINT_SLOTS; slot++) {
            blueprints.set(slot, all.get(SSD_SLOTS + slot));
        }
    }

    /** The contents now live on the item component; keep them out of its block-entity data so nothing is doubled. */
    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(@NotNull CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove(NBT_SSDS);
        tag.remove(NBT_BLUEPRINTS);
    }

    public boolean isEmpty() {
        for (var ssd : ssds) {
            if (!ssd.isEmpty()) {
                return false;
            }
        }

        for (var chip : blueprints) {
            if (!chip.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static void clear(NonNullList<ItemStack> list) {
        for (int slot = 0; slot < list.size(); slot++) {
            list.set(slot, ItemStack.EMPTY);
        }
    }
}
