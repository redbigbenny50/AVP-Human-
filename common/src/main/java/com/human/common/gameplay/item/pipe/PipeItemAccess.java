package com.human.common.gameplay.item.pipe;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.Nullable;

/**
 * One face of a block that holds items, as an item pipe sees it.
 * <h2>WHICH WAY A FACE WORKS</h2> [stated] Sep 30:
 * <ul>
 * <li>A block WITH DEDICATED PORTS keeps them - our machines: in at the top and left, out at the bottom and right, as
 * each machine's kind says.</li>
 * <li>Any other storage - chests, barrels, furnaces, other mods' inventories: the TOP takes items in, the BOTTOM gives
 * them out, and the SIDES take items in, as hoppers treat them.</li>
 * </ul>
 * The player can change any end with the flange click.
 * <h2>HOW ITEMS MOVE</h2> Vanilla-style containers - ours, vanilla's, and most mods' - go through vanilla's own hopper
 * insert, which respects each block's per-face slot rules and merges double chests. Other mods' inventories go through
 * the loader's item API. An item is only taken from its source AFTER its destination has taken it, so nothing is ever
 * duplicated or lost.
 */
public interface PipeItemAccess {

    /** What a source offers: a copy of the stack, and how to take part of it for real. */
    interface Offer {

        ItemStack stack();

        /** Removes {@code count} of the offered stack from the source. */
        /**
         * Takes {@code count} of the offered stack out for real.
         *
         * @return how many really came out - Oct 13 hardening: a caller that already put items somewhere must check it,
         *         or a misbehaving inventory that gives less than its own dry run promised would duplicate the
         *         difference
         */
        int take(int count);
    }

    interface Port {

        boolean present();

        /**
         * Oct 13 hardening - removes up to {@code count} items matching {@code like} (same item and components) again,
         * to undo an insert whose source then gave less than promised. Ignores face rules: it only ever takes back what
         * the pipe itself just put in.
         *
         * @return how many were removed
         */
        default int retract(ItemStack like, int count) {
            return 0;
        }

        /** The pipe may put items in on this face. */
        boolean canInsert();

        /** The pipe may take items out on this face. */
        boolean canExtract();

        /** The first stack that could come out and that {@code allowed} accepts, up to {@code max} - or null. */
        @Nullable
        Offer peek(int max, java.util.function.Predicate<ItemStack> allowed);

        /** Puts the stack in for real. @return how many went in */
        int insert(ItemStack stack);

        /** Which inventory this is - both halves of a double chest are one. */
        Object storage();

        /**
         * Oct 8 - whether this is a MACHINE rather than STORAGE, for routing ([stated] Oct 7: machines default to
         * Normal priority and storage to Low, and machines win ties). Our machines and vanilla furnaces, brewing stands
         * and the like are machines; chests, barrels, shulker boxes, hoppers and other mods' inventories are storage
         * unless the player sets the end's priority.
         *
         * @return whether this port belongs to a machine
         */
        default boolean isMachine() {
            return false;
        }

        /**
         * Oct 13 - everything this inventory holds, each item with its count, for the fabricator's network storage view
         * and its material planning. Read only; nothing moves. An inventory that cannot be listed lists nothing.
         */
        default void listContents(java.util.function.ObjIntConsumer<ItemStack> sink) {}
    }

    /** Another mod's inventory that is not a vanilla-style container, or null. */
    @Nullable
    Port openForeign(Level level, BlockPos pos, Direction side);

    /** [stated] Sep 30 - the default for storage without dedicated ports: top and sides in, bottom out. */
    static boolean defaultIn(Direction side) {
        return side != Direction.DOWN;
    }

    static boolean defaultOut(Direction side) {
        return side == Direction.DOWN;
    }

    static Port open(Level level, BlockPos pos, Direction side) {
        var state = level.getBlockState(pos);

        // Most pipe faces are air - no lookup at all for those.
        if (state.isAir()) {
            return NONE;
        }

        // Storage crates answer for EVERY one of their blocks with one shared inventory and one identity, so a pipe
        // network sees a 3x3 or a container as a single storage. Checked before vanilla containers on purpose: a
        // crate holds 64 of anything per slot, which the vanilla container path would cap at one unstackable.
        // Oct 13 - the fabricator: pipes push materials into its storage and pull finished prints from its output.
        if (
            state.getBlock() instanceof com.human.common.gameplay.fabrication.FabricatorBlock
                && level.getBlockEntity(
                    com.human.common.gameplay.fabrication.FabricatorBlock.corePos(pos, state)
                ) instanceof com.human.common.gameplay.fabrication.FabricatorBlockEntity fabricator
        ) {
            return fabricator.pipePort();
        }

        // Oct 15 - the quarry terminal: its ITEM port is the TOP of its back-row clockwise block ([stated] Oct 13: item
        // port top left, facing the screen). It gives mined items from the internal slots and takes frame blocks in.
        // Every other face of the terminal is no item port at all.
        if (state.getBlock() instanceof com.human.common.gameplay.quarry.QuarryTerminalBlock) {
            if (
                side == Direction.UP && state.getValue(
                    com.human.common.gameplay.quarry.QuarryTerminalBlock.PART
                ) == com.human.common.gameplay.quarry.QuarryTerminalPart.BACK_CW
                    && level.getBlockEntity(
                        com.human.common.gameplay.quarry.QuarryTerminalBlock.brainPos(pos, state)
                    ) instanceof com.human.common.gameplay.quarry.QuarryTerminalBlockEntity terminal
            ) {
                return terminal.pipePort();
            }

            return NONE;
        }

        if (level.getBlockEntity(pos) instanceof com.human.common.gameplay.storage.CrateBlockEntity crate) {
            var port = crate.pipePort(side);

            return port != null ? port : NONE;
        }

        var container = containerAt(level, pos);

        if (container != null) {
            return new ContainerPort(container, side, storageOf(level, pos), level.getBlockEntity(pos));
        }

        // Other mods' inventories live in block entities.
        var foreign = state.hasBlockEntity() ? Holder.get().openForeign(level, pos, side) : null;

        return foreign != null ? foreign : NONE;
    }

    /** The block's container, a double chest as one - never an entity's. */
    private static @Nullable Container containerAt(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);

        if (state.getBlock() instanceof WorldlyContainerHolder holder) {
            return holder.getContainer(state, level, pos);
        }

        if (state.getBlock() instanceof ChestBlock chest) {
            return ChestBlock.getContainer(chest, state, level, pos, true);
        }

        return level.getBlockEntity(pos) instanceof Container container ? container : null;
    }

    /** A double chest's halves share one identity - the lower of the two positions. */
    private static Object storageOf(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);

        if (
            level.getBlockEntity(pos) instanceof ChestBlockEntity && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
        ) {
            var other = pos.relative(ChestBlock.getConnectedDirection(state));

            return other.asLong() < pos.asLong() ? other.immutable() : pos.immutable();
        }

        return pos.immutable();
    }

    Port NONE = new Port() {

        @Override
        public boolean present() {
            return false;
        }

        @Override
        public boolean canInsert() {
            return false;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public @Nullable Offer peek(int max, java.util.function.Predicate<ItemStack> allowed) {
            return null;
        }

        @Override
        public int insert(ItemStack stack) {
            return 0;
        }

        @Override
        public Object storage() {
            return this;
        }
    };

    /** Vanilla-style containers, through vanilla's hopper rules. */
    final class ContainerPort implements Port {

        private final Container container;

        private final Direction side;

        private final Object storage;

        private final boolean canInsert;

        private final boolean canExtract;

        private final boolean machine;

        /** Oct 17 - a vaporizer face with no item port at all: not a port, so a pipe does not even connect to it. */
        private final boolean noPort;

        ContainerPort(Container container, Direction side, Object storage, @Nullable Object blockEntity) {
            this.container = container;
            this.side = side;
            this.storage = storage;

            // Oct 17 - the vaporizer's two blocks answer through VaporizerPortContainer, which only gives the TOP
            // block's ports ([stated] items in at the top, out of the left and right of the top block). It is still
            // our machine, with dedicated ports, whichever of its blocks the pipe touches.
            var vaporizer = container instanceof com.human.common.gameplay.vaporizer.VaporizerPortContainer port ? port : null;

            if (vaporizer != null) {
                blockEntity = vaporizer.machine();
            }

            // Oct 8: storage is anything built on the vanilla storage base (chests, barrels, shulker boxes, hoppers,
            // dispensers - and most modded chests, which extend it); every other inventory is a machine.
            this.machine = blockEntity != null
                && !(blockEntity instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity);

            // Our machines have DEDICATED ports: the face gives its input slot or its output slots, or nothing.
            if (blockEntity instanceof ElectricMachineBlockEntity machine) {
                // Oct 8 - FIX: a face set to BOTH lists the input slot first and the outputs after it, and this used
                // to judge the face by its FIRST slot only - so a Both face could take in but never give out. Judge by
                // all of the face's slots.
                var slots = vaporizer != null ? vaporizer.getSlotsForFace(side) : machine.getSlotsForFace(side);
                var input = false;
                var output = false;

                for (var slot : slots) {
                    if (machine.isOutputSlot(slot)) {
                        output = true;
                    } else {
                        input = true;
                    }
                }

                this.canInsert = input;
                this.canExtract = output;
                this.noPort = vaporizer != null && slots.length == 0;
            } else {
                this.canInsert = defaultIn(side);
                this.canExtract = defaultOut(side);
                this.noPort = false;
            }
        }

        @Override
        public boolean present() {
            return !noPort && container.getContainerSize() > 0;
        }

        @Override
        public boolean canInsert() {
            return canInsert;
        }

        @Override
        public boolean isMachine() {
            return machine;
        }

        @Override
        public boolean canExtract() {
            return canExtract;
        }

        @Override
        public @Nullable Offer peek(int max, java.util.function.Predicate<ItemStack> allowed) {
            var worldly = container instanceof WorldlyContainer sided ? sided : null;
            var slots = worldly != null ? worldly.getSlotsForFace(side) : null;
            var count = slots != null ? slots.length : container.getContainerSize();

            var found = -1;

            for (var index = 0; index < count; index++) {
                var slot = slots != null ? slots[index] : index;
                var stack = container.getItem(slot);

                if (stack.isEmpty() || !allowed.test(stack) || worldly != null && !worldly.canTakeItemThroughFace(slot, stack, side)) {
                    continue;
                }

                // [stated] Oct 7 MOST WORN FIRST: a damageable item gives the most damaged copy of that item.
                if (found < 0) {
                    found = slot;

                    if (!stack.isDamageableItem()) {
                        break;
                    }
                } else {
                    var best = container.getItem(found);

                    if (stack.is(best.getItem()) && stack.getDamageValue() > best.getDamageValue()) {
                        found = slot;
                    }
                }
            }

            if (found >= 0) {
                var slot = found;
                var stack = container.getItem(slot);
                var offered = stack.copyWithCount(Math.min(max, stack.getCount()));

                return new Offer() {

                    @Override
                    public ItemStack stack() {
                        return offered;
                    }

                    @Override
                    public int take(int amount) {
                        var removed = container.removeItem(slot, amount);
                        container.setChanged();
                        return removed.getCount();
                    }
                };
            }

            return null;
        }

        @Override
        public int retract(ItemStack like, int count) {
            var removed = 0;

            for (var slot = 0; slot < container.getContainerSize() && removed < count; slot++) {
                if (ItemStack.isSameItemSameComponents(container.getItem(slot), like)) {
                    removed += container.removeItem(slot, count - removed).getCount();
                }
            }

            if (removed > 0) {
                container.setChanged();
            }

            return removed;
        }

        @Override
        public int insert(ItemStack stack) {
            var before = stack.getCount();
            var remainder = HopperBlockEntity.addItem(null, container, stack.copy(), side);

            return before - remainder.getCount();
        }

        /** Oct 13 - every slot, for the fabricator's view. Reading moves nothing. */
        @Override
        public void listContents(java.util.function.ObjIntConsumer<ItemStack> sink) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                var stack = container.getItem(slot);

                if (!stack.isEmpty()) {
                    sink.accept(stack, stack.getCount());
                }
            }
        }

        @Override
        public Object storage() {
            return storage;
        }
    }

    final class Holder {

        private static PipeItemAccess current = (level, pos, side) -> null;

        private Holder() {}

        public static PipeItemAccess get() {
            return current;
        }

        public static void install(@Nullable PipeItemAccess access) {
            current = access == null ? (level, pos, side) -> null : access;
        }
    }
}
