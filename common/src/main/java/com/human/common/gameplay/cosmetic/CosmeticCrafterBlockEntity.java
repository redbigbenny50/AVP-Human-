package com.human.common.gameplay.cosmetic;

import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.fabrication.FactionLicenseItem;
import com.human.common.gameplay.item.pipe.PipeItemAccess;
import com.human.common.registry.init.HumanCosmetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Cosmetic Crafter's core. [stated] Oct 31: works like the fabricator - raw materials from its INPUT box and from
 * the NETWORK (storage touching it or on a pipe touching it), no pattern; a faction license, held in the crafter's
 * license slot, in the player's inventory or in network storage, unlocks that faction's styles.
 * <h2>WHEN THINGS ARE USED</h2> Nothing is taken when a craft starts - the license and one craft's materials are only
 * CHECKED. Each item is paid for when it finishes: input first, then the network, all or nothing; anything pulled from
 * the network goes back if a later pull comes up short. Never deleted - what cannot go back stays in the input, and
 * failing that drops at its feet.
 * <h2>LICENSE</h2> Checked when CRAFT is pressed. A queued batch then finishes even if the player walks away with it.
 * <h2>POWER</h2> [stated] Nov 1: from the BACK only, any of the three bottom-row back blocks (this block is the back-middle
 * one). Power is drawn only while crafting with room for the result.
 * <h2>COST</h2> Idle, nothing. Storage reads happen when a craft starts or finishes and twice a second while a screen
 * is open - never per tick.
 */
public class CosmeticCrafterBlockEntity extends PowerConsumerBlockEntity {

    public static final int INPUT_SLOTS = 9;

    public static final int OUTPUT_SLOTS = 4;

    /** One license slot per licensed style (W-Y, USCM). */
    public static final int LICENSE_SLOTS = CosmeticStyle.values().length - 1;

    public static final int FIRST_OUTPUT = INPUT_SLOTS;

    public static final int FIRST_LICENSE = FIRST_OUTPUT + OUTPUT_SLOTS;

    public static final int ALL_SLOTS = FIRST_LICENSE + LICENSE_SLOTS;

    /** Power drawn per tick while crafting. */
    public static final long CRAFT_POWER = 80L;

    /** Ticks per item. */
    public static final int CRAFT_TICKS = 60;

    public static final int MAX_QUEUE = 64;

    private static final int MAX_PULL_ATTEMPTS = 256;

    private static final int RUNNING_GRACE_TICKS = 10;

    public enum Status {
        IDLE,
        CRAFTING,
        NO_POWER,
        OUTPUT_FULL,
        MISSING,
        LOCKED
    }

    private final NonNullList<ItemStack> items = NonNullList.withSize(ALL_SLOTS, ItemStack.EMPTY);

    private int target = -1;

    private int queued;

    private int progress;

    private Status status = Status.IDLE;

    private boolean running;

    private long lastPoweredTick = Long.MIN_VALUE / 2;

    /** Counts finished items - the client plays the drawer's "complete" clip each time it goes up. */
    private int completions;

    /**
     * [stated] Oct 31: a job started by a CREATIVE-mode player costs nothing - no materials are checked or taken, and
     * it draws no power - but it still takes the normal crafting time. The faction license is not a cost, so it is
     * still required. Saved, so a free batch stays free across a relog; cleared when the job ends.
     */
    private boolean free;

    public CosmeticCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(HumanCosmetics.CRAFTER_BLOCK_ENTITY.get(), pos, state);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Slots: 0-8 input, 9-12 output, 13.. licenses (one per licensed style, in CosmeticStyle order)
    // ------------------------------------------------------------------------------------------------------------

    public ItemStack getSlot(int slot) {
        return items.get(slot);
    }

    public void setSlot(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    /** The license slot for {@code style}, or -1 when it needs none. */
    public static int licenseSlot(CosmeticStyle style) {
        return style.faction() == null ? -1 : FIRST_LICENSE + style.ordinal() - 1;
    }

    /** Whether {@code stack} may sit in license slot {@code slot}: only that style's own license. */
    public static boolean mayPlaceLicense(int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof FactionLicenseItem license)) {
            return false;
        }

        var style = CosmeticStyle.of(license.faction());

        return style != null && licenseSlot(style) == slot;
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Licenses
    // ------------------------------------------------------------------------------------------------------------

    /** Whether {@code style} is unlocked here: its license in the crafter's slot, the player's inventory or the network. */
    public boolean licensed(ServerLevel level, @Nullable Player player, CosmeticStyle style) {
        return licensed(CosmeticCrafterSources.connected(level, this), player, style);
    }

    /** As above, reading the given storage - so one scan can serve every check in a refresh. */
    public boolean licensed(List<PipeItemAccess.Port> ports, @Nullable Player player, CosmeticStyle style) {
        var faction = style.faction();

        if (faction == null) {
            return true;
        }

        if (isLicense(items.get(licenseSlot(style)), style)) {
            return true;
        }

        if (player != null) {
            for (var stack : player.getInventory().items) {
                if (isLicense(stack, style)) {
                    return true;
                }
            }
        }

        var found = new boolean[1];

        for (var port : ports) {
            port.listContents((stack, count) -> {
                if (!found[0] && isLicense(stack, style)) {
                    found[0] = true;
                }
            });

            if (found[0]) {
                return true;
            }
        }

        return false;
    }

    private static boolean isLicense(ItemStack stack, CosmeticStyle style) {
        return stack.getItem() instanceof FactionLicenseItem license && license.faction() == style.faction();
    }

    /** Bit {@code style.ordinal()} set for every unlocked style - for the screen's drawer. */
    public int licenseBits(List<PipeItemAccess.Port> ports, @Nullable Player player) {
        var bits = 0;

        for (var style : CosmeticStyle.values()) {
            if (licensed(ports, player, style)) {
                bits |= 1 << style.ordinal();
            }
        }

        return bits;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Materials
    // ------------------------------------------------------------------------------------------------------------

    /** One item in one connected inventory, with its count. */
    public record Stored(
        PipeItemAccess.Port port,
        ItemStack stack
    ) {}

    public List<Stored> connectedContents(ServerLevel level) {
        return connectedContents(CosmeticCrafterSources.connected(level, this));
    }

    public List<Stored> connectedContents(List<PipeItemAccess.Port> ports) {
        var entries = new ArrayList<Stored>();

        for (var port : ports) {
            port.listContents((stack, count) -> {
                if (count > 0 && !stack.isEmpty()) {
                    entries.add(new Stored(port, stack.copyWithCount(count)));
                }
            });
        }

        return entries;
    }

    /** How many of each part are here (input + network), in the entry's part order. */
    public int[] have(CosmeticCatalog.Entry entry, List<Stored> connected) {
        var have = new int[entry.parts().size()];

        for (int part = 0; part < have.length; part++) {
            var ingredient = entry.parts().get(part).ingredient();
            long total = 0;

            for (int slot = 0; slot < INPUT_SLOTS; slot++) {
                if (ingredient.test(items.get(slot))) {
                    total += items.get(slot).getCount();
                }
            }

            for (var stored : connected) {
                if (ingredient.test(stored.stack())) {
                    total += stored.stack().getCount();
                }
            }

            have[part] = (int) Math.min(Integer.MAX_VALUE, total);
        }

        return have;
    }

    /**
     * Whether one craft's materials are here. Parts are checked one at a time; two parts that could both use the same
     * stack are counted against the same stock, which is what the paying step does too.
     */
    private boolean affordable(CosmeticCatalog.Entry entry, List<Stored> connected) {
        return plan(entry, connected) != null;
    }

    /** A paid craft: how much leaves each input slot and each connected entry. Null when something is short. */
    private record Plan(
        int[] fromInput,
        int[] fromNetwork
    ) {}

    private @Nullable Plan plan(CosmeticCatalog.Entry entry, List<Stored> connected) {
        var input = new int[INPUT_SLOTS];
        var network = new int[connected.size()];

        for (var part : entry.parts()) {
            var needed = part.count();

            for (int slot = 0; slot < INPUT_SLOTS && needed > 0; slot++) {
                var stack = items.get(slot);

                if (part.ingredient().test(stack)) {
                    var take = Math.min(needed, stack.getCount() - input[slot]);

                    if (take > 0) {
                        input[slot] += take;
                        needed -= take;
                    }
                }
            }

            for (int index = 0; index < connected.size() && needed > 0; index++) {
                var stack = connected.get(index).stack();

                if (part.ingredient().test(stack)) {
                    var take = Math.min(needed, stack.getCount() - network[index]);

                    if (take > 0) {
                        network[index] += take;
                        needed -= take;
                    }
                }
            }

            if (needed > 0) {
                return null;
            }
        }

        return new Plan(input, network);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Crafting
    // ------------------------------------------------------------------------------------------------------------

    public int target() {
        return target;
    }

    public int queued() {
        return queued;
    }

    public Status status() {
        return status;
    }

    public boolean isCrafting() {
        return target >= 0 && queued > 0;
    }

    /** Crafting AND power reaching it - every visual follows this, never {@link #isCrafting} (the fabricator lesson). */
    public boolean isRunning() {
        return running;
    }

    public int completions() {
        return completions;
    }

    public int progressPercent() {
        return isCrafting() ? Math.min(100, progress * 100 / CRAFT_TICKS) : 0;
    }

    public boolean start(ServerLevel level, Player player, int index, int count) {
        var entry = CosmeticCatalog.get(index);

        if (entry == null || isCrafting()) {
            return false;
        }

        if (!licensed(level, player, entry.style())) {
            status = Status.LOCKED;
            return false;
        }

        var creative = player.getAbilities().instabuild;

        if (!creative && !affordable(entry, connectedContents(level))) {
            status = Status.MISSING;
            return false;
        }

        free = creative;
        target = index;
        queued = Math.max(1, Math.min(MAX_QUEUE, count));
        progress = 0;
        status = Status.CRAFTING;
        setChanged();
        sync();

        return true;
    }

    public void stop() {
        stopWith(Status.IDLE);
    }

    private void stopWith(Status reason) {
        target = -1;
        queued = 0;
        free = false;
        progress = 0;
        status = reason;
        running = false;
        setChanged();
        sync();
    }

    private boolean outputHasRoom() {
        var entry = CosmeticCatalog.get(target);

        if (entry == null) {
            return false;
        }

        for (int slot = FIRST_OUTPUT; slot < FIRST_LICENSE; slot++) {
            var held = items.get(slot);

            if (held.isEmpty() || (held.is(entry.result()) && held.getCount() < held.getMaxStackSize())) {
                return true;
            }
        }

        return false;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Item port - [stated] Nov 1: an item pipe connects on the pillar side (CosmeticCrafterBlock.ITEM_PORT_CELL), which
    // is how the crafter reaches a pipe network's storage. Through it a pipe may take FINISHED pieces out; nothing goes
    // in - materials are read from the network's storage when a craft runs, so routing never fills the input with
    // whatever passes by.
    // ------------------------------------------------------------------------------------------------------------

    private final com.human.common.gameplay.item.pipe.PipeItemAccess.Port pipePort = new com.human.common.gameplay.item.pipe.PipeItemAccess.Port() {

        @Override
        public boolean present() {
            return !isRemoved();
        }

        @Override
        public boolean canInsert() {
            return false;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean isMachine() {
            return true;
        }

        /** Only the OUTPUT comes out - the first finished stack the pipe will take. */
        @Override
        public com.human.common.gameplay.item.pipe.PipeItemAccess.@Nullable Offer peek(int max, java.util.function.Predicate<ItemStack> allowed) {
            for (int slot = FIRST_OUTPUT; slot < FIRST_OUTPUT + OUTPUT_SLOTS; slot++) {
                var held = items.get(slot);

                if (held.isEmpty() || !allowed.test(held)) {
                    continue;
                }

                var source = slot;
                var offered = held.copyWithCount(Math.min(max, held.getCount()));

                return new com.human.common.gameplay.item.pipe.PipeItemAccess.Offer() {

                    @Override
                    public ItemStack stack() {
                        return offered;
                    }

                    @Override
                    public int take(int count) {
                        var stack = items.get(source);
                        var took = Math.min(count, stack.getCount());

                        stack.shrink(took);

                        if (stack.isEmpty()) {
                            items.set(source, ItemStack.EMPTY);
                        }

                        setChanged();

                        return took;
                    }
                };
            }

            return null;
        }

        @Override
        public int insert(ItemStack stack) {
            return 0;
        }

        @Override
        public Object storage() {
            return CosmeticCrafterBlockEntity.this;
        }
    };

    /** The crafter's item port, as an item pipe sees it. */
    public com.human.common.gameplay.item.pipe.PipeItemAccess.Port pipePort() {
        return pipePort;
    }

    @Override
    public long getRequestedPower() {
        // A free (creative) job asks for nothing, so the grid never powers it and unpoweredTick runs it instead.
        return !free && isCrafting() && outputHasRoom() ? CRAFT_POWER : 0L;
    }

    /**
     * [stated] Nov 1: power from the BACK only - the gap under the machine means nothing can reach its underside. The core
     * is the back-middle bottom block, so its own back face is one of the three ports; the other two reach it through
     * the machine's parts (see {@link CosmeticCrafterBlock#coreThrough}).
     */
    @Override
    public boolean canConnectOn(Direction direction) {
        var state = getBlockState();

        return state.hasProperty(CosmeticMultiblock.FACING) && direction == state.getValue(CosmeticMultiblock.FACING).getOpposite();
    }

    @Override
    public void unpoweredTick(Level level, BlockPos pos, BlockState state) {
        if (free && isCrafting()) {
            if (outputHasRoom()) {
                advance(level);
                return;
            }

            status = Status.OUTPUT_FULL;
            updateRunning(level);
            return;
        }

        if (isCrafting()) {
            status = outputHasRoom() ? Status.NO_POWER : Status.OUTPUT_FULL;
        }

        updateRunning(level);
    }

    @Override
    public void poweredTick(Level level, BlockPos pos, BlockState state) {
        advance(level);
    }

    /** One tick of crafting: on power, or for a free (creative) job without it. */
    private void advance(Level level) {
        if (!isCrafting() || !(level instanceof ServerLevel serverLevel)) {
            updateRunning(level);
            return;
        }

        status = Status.CRAFTING;
        lastPoweredTick = level.getGameTime();
        progress++;

        if (progress >= CRAFT_TICKS) {
            finish(serverLevel);
        }

        updateRunning(level);
        setChanged();
    }

    private void updateRunning(Level level) {
        var now = isCrafting() && level.getGameTime() - lastPoweredTick <= RUNNING_GRACE_TICKS;

        if (now != running) {
            running = now;
            setChanged();
            sync();
        }
    }

    private void finish(ServerLevel level) {
        var entry = CosmeticCatalog.get(target);

        if (entry == null) {
            stopWith(Status.IDLE);
            return;
        }

        if (!outputHasRoom()) {
            stopWith(Status.OUTPUT_FULL);
            return;
        }

        if (!free) {
            var connected = connectedContents(level);
            var plan = plan(entry, connected);

            if (plan == null) {
                stopWith(Status.MISSING);
                return;
            }

            // The network first - all of it, or none of it.
            if (!pullAll(level, connected, plan.fromNetwork())) {
                stopWith(Status.MISSING);
                return;
            }

            for (int slot = 0; slot < INPUT_SLOTS; slot++) {
                if (plan.fromInput()[slot] > 0) {
                    items.get(slot).shrink(plan.fromInput()[slot]);

                    if (items.get(slot).isEmpty()) {
                        items.set(slot, ItemStack.EMPTY);
                    }
                }
            }
        }

        insertOutput(level, entry.resultStack());

        completions++;
        queued--;
        progress = 0;

        if (queued <= 0) {
            target = -1;
            status = Status.IDLE;
            free = false;
        }

        setChanged();
        sync();
    }

    private void insertOutput(ServerLevel level, ItemStack result) {
        for (int slot = FIRST_OUTPUT; slot < FIRST_LICENSE && !result.isEmpty(); slot++) {
            var held = items.get(slot);

            if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, result) && held.getCount() < held.getMaxStackSize()) {
                var moved = Math.min(result.getCount(), held.getMaxStackSize() - held.getCount());

                held.grow(moved);
                result.shrink(moved);
            }
        }

        for (int slot = FIRST_OUTPUT; slot < FIRST_LICENSE && !result.isEmpty(); slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, result.split(result.getMaxStackSize()));
            }
        }

        // outputHasRoom was checked first, so this never runs - but a made item is never deleted.
        if (!result.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D, result);
        }
    }

    private boolean pullAll(ServerLevel level, List<Stored> connected, int[] taken) {
        var pulled = new ArrayList<Stored>();

        for (int entry = 0; entry < connected.size(); entry++) {
            var amount = taken[entry];

            if (amount <= 0) {
                continue;
            }

            var wanted = connected.get(entry);
            var got = 0;
            var attempts = 0;

            while (got < amount && attempts++ < MAX_PULL_ATTEMPTS) {
                var offer = wanted.port().peek(amount - got, stack -> ItemStack.isSameItemSameComponents(stack, wanted.stack()));

                if (offer == null || offer.stack().isEmpty()) {
                    break;
                }

                // Count what REALLY came out, never what was asked for (the fabricator's Oct 13 hardening).
                var took = offer.take(Math.min(amount - got, offer.stack().getCount()));

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

    private void putBack(ServerLevel level, List<Stored> pulled) {
        for (var entry : pulled) {
            var stack = entry.stack().copy();

            stack.shrink(entry.port().insert(stack.copy()));

            for (int slot = 0; slot < INPUT_SLOTS && !stack.isEmpty(); slot++) {
                var held = items.get(slot);

                if (held.isEmpty()) {
                    items.set(slot, stack.split(stack.getMaxStackSize()));
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

    /** Shift-click into the input box. Returns what did not fit. */
    public ItemStack insertIntoInput(ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int slot = 0; slot < INPUT_SLOTS && !stack.isEmpty(); slot++) {
                var held = items.get(slot);

                if (pass == 0 && !held.isEmpty() && ItemStack.isSameItemSameComponents(held, stack) && held.getCount() < held.getMaxStackSize()) {
                    var moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());

                    held.grow(moved);
                    stack.shrink(moved);
                } else if (pass == 1 && held.isEmpty()) {
                    items.set(slot, stack.split(stack.getMaxStackSize()));
                }
            }
        }

        setChanged();

        return stack;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Saving and syncing
    // ------------------------------------------------------------------------------------------------------------

    private int syncedTarget = -2;

    private int syncedQueued = -1;

    private boolean syncedRunning;

    private int syncedCompletions = -1;

    private void sync() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (syncedTarget != target || syncedQueued != queued || syncedRunning != running || syncedCompletions != completions) {
            syncedTarget = target;
            syncedQueued = queued;
            syncedRunning = running;
            syncedCompletions = completions;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", ContainerHelper.saveAllItems(new CompoundTag(), items, true, registries));
        tag.putInt("Target", target);
        tag.putInt("Queued", queued);
        tag.putInt("Progress", progress);
        tag.putBoolean("Running", running);
        tag.putInt("Completions", completions);
        tag.putBoolean("Free", free);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        for (int slot = 0; slot < ALL_SLOTS; slot++) {
            items.set(slot, ItemStack.EMPTY);
        }

        ContainerHelper.loadAllItems(tag.getCompound("Items"), items, registries);
        target = tag.contains("Target") ? tag.getInt("Target") : -1;
        queued = tag.getInt("Queued");
        progress = tag.getInt("Progress");
        running = tag.getBoolean("Running");
        completions = tag.getInt("Completions");
        free = tag.getBoolean("Free");
        status = isCrafting() ? Status.CRAFTING : Status.IDLE;
    }

    /** Clients get the craft state - never the items. */
    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        var tag = super.getUpdateTag(registries);

        tag.remove("Items");

        return tag;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NotNull Builder components) {
        super.collectImplicitComponents(components);

        if (!isEmpty()) {
            components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.@NotNull DataComponentInput input) {
        super.applyImplicitComponents(input);

        var contents = input.get(DataComponents.CONTAINER);

        if (contents != null) {
            contents.copyInto(items);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(@NotNull CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Items");
    }

    // ------------------------------------------------------------------------------------------------------------
    // Client
    // ------------------------------------------------------------------------------------------------------------

    private final CosmeticCrafterAnimationDispatcher animations = new CosmeticCrafterAnimationDispatcher();

    private int seenCompletions = -1;

    /** Ticks left of the drawer's "complete" clip, during which the loops wait. */
    private int completeTicks;

    private @Nullable Boolean dispatchedRunning;

    public void clientTick() {
        if (seenCompletions < 0) {
            seenCompletions = completions;
        }

        if (completions != seenCompletions && animations.complete(this)) {
            seenCompletions = completions;
            completeTicks = CosmeticCrafterAnimationDispatcher.COMPLETE_TICKS;
            dispatchedRunning = null;
            return;
        }

        if (completeTicks > 0) {
            completeTicks--;
            return;
        }

        var now = isRunning();

        // Only counts as sent once it actually reached an animator (made on first render).
        if ((dispatchedRunning == null || dispatchedRunning != now) && animations.loop(this, now)) {
            dispatchedRunning = now;
        }
    }
}
