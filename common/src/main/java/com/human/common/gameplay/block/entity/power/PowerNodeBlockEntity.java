package com.human.common.gameplay.block.entity.power;

import com.human.common.gameplay.power.PowerNode;
import com.human.common.gameplay.power.PowerPorts;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PowerNodeBlockEntity extends BlockEntity implements com.human.common.gameplay.ship.ShipMoveAware {

    private boolean hasInitialized = false;

    private final PowerNode powerNode;

    private final PowerPorts ports = new PowerPorts();

    public PowerNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);

        // TODO: Remove this, this is bad practice.
        if (!(this instanceof PowerNode node)) {
            throw new IllegalStateException("Child of PowerNodeBlockEntity must be instance of PowerNode!");
        }

        this.powerNode = node;
    }

    public void serverTick(Level level, BlockPos blockPos, BlockState blockState) {
        if (!hasInitialized() && level != null && !level.isClientSide) {
            setHasInitialized(true);

            var serverLevel = (ServerLevel) level;

            // ⭐ ONE CALL. This used to register, then BFS the whole network, then union once per discovered
            // position - per block entity. linkOnLoad discovers a network only once, for the first node that wakes
            // on it; everyone after that is a map lookup. The per-node timing log went with it: it fired for every
            // power block on every chunk load, which is its own cost on a busy server.
            PowerSystem.get(serverLevel).linkOnLoad(serverLevel, blockPos, powerNode);
        }
    }

    /** The breaker state of this node's six faces. Never null. */
    public PowerPorts getPorts() {
        return ports;
    }

    /**
     * Whether this face can EVER carry power, regardless of its breaker.
     * <p>
     * ⭐ [stated] Sep 29: "all machines get power from the back". A machine answers true for its back face only, and a
     * battery for every face but its front. The default is every face, which is what cables, generators and the
     * charging hub have always done.
     * </p>
     * <p>
     * ⚠ This is a FIXED property of the block, not a setting - it is not saved and has no breaker. A face that can
     * never connect has nothing to open or close, so {@link #setPortClosed} refuses it.
     * </p>
     */
    public boolean canConnectOn(Direction direction) {
        return true;
    }

    /**
     * Whether power can cross this face right now: the face can carry power at all AND its breaker is open. This is the
     * one question every connection path asks - the grid walk, placement and the cable's own arms.
     */
    /**
     * [stated] Oct 22 - "panels should connect because you shouldnt need to make a grid of power cables for a whole
     * array." Whether this block joins the power block {@code neighbour} touching it on {@code direction} DIRECTLY,
     * block to block, even through a face that takes no cable. Both blocks must agree. Off by default; the solar panel
     * turns it on for other solar panels beside it, so one cable under any panel of an array collects the whole array.
     */
    public boolean joinsNeighbour(Direction direction, PowerNodeBlockEntity neighbour) {
        return false;
    }

    public boolean isFaceConnectable(Direction direction) {
        return direction != null && canConnectOn(direction) && ports.isOpen(direction);
    }

    /**
     * Opens or closes a face and re-forms the networks around it.
     *
     * @return true when the breaker actually changed
     *         <p>
     *         ⚠ A toggle is a TOPOLOGY change, so it goes through the same queued split the block-removal path uses -
     *         one rebuild pass per tick however many faces are toggled at once, and the frontier re-link picks the
     *         halves back up when a face reopens.
     *         </p>
     */
    public boolean setPortClosed(Direction direction, boolean closed) {
        // ⚠ A face that can never carry power has no breaker to change.
        if (direction == null || !canConnectOn(direction) || !ports.setClosed(direction, closed)) {
            return false;
        }

        setChanged();

        if (level instanceof ServerLevel serverLevel) {
            PowerSystem.get(serverLevel).queueSplit(worldPosition);
            // Push the new port state to everyone watching, not just to disk.
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            refreshCableOn(serverLevel, direction);
        }

        return true;
    }

    /**
     * ⚠ A breaker changing does NOT reshape the cable touching it on its own: {@code sendBlockUpdated} only resyncs
     * this block to clients, and a cable only re-evaluates its arms from {@code updateShape}, which fires when a
     * NEIGHBOUR'S BLOCK changes. Without this the cable would keep drawing an arm into a closed port - or none into a
     * reopened one.
     */
    private void refreshCableOn(ServerLevel serverLevel, Direction direction) {
        var neighbourPos = worldPosition.relative(direction);
        var neighbourState = serverLevel.getBlockState(neighbourPos);

        if (!(neighbourState.getBlock() instanceof com.human.common.gameplay.block.power.CableBlock)) {
            return;
        }

        var reshaped = neighbourState.updateShape(
            direction.getOpposite(),
            getBlockState(),
            serverLevel,
            neighbourPos,
            worldPosition
        );

        if (reshaped != neighbourState) {
            serverLevel.setBlock(neighbourPos, reshaped, net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        ports.save(tag);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        ports.load(tag);
    }

    /**
     * ⚠⚠ WITHOUT THESE TWO THE CLIENT NEVER LEARNS A PORT CHANGED. A block entity's saved data is NOT sent to clients
     * by default - {@code setChanged} only marks the chunk for SAVING. The ESU screen, the wiring tool's readout and
     * any future overlay all need the port and mode state client-side, so the node publishes its whole tag on load and
     * on every update instead of each of them inventing a packet.
     */
    @Override
    public @Nullable net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithoutMetadata(registries);
    }

    /**
     * ⭐⭐ BREAKING A POWER BLOCK MUST NOT DESTROY WHAT IS IN IT. [stated] Sep 25, after the battery: "this rule would
     * also apply to consumers the user breaks too."
     * <p>
     * It lives HERE rather than on the battery so every power machine inherits it - a buffered consumer, the
     * fabricator, anything later. A machine with an internal charge overrides {@link #getStoredEnergyForDrop()} and
     * {@link #setStoredEnergyFromDrop(long)}; one without does nothing and pays nothing.
     * </p>
     * <p>
     * ⚠ THE LOOT TABLE STILL HAS TO COPY IT. These hooks put the values on the dropped stack, but a block whose loot
     * table is {@code dropSelf} or {@code createNameableBlockEntityTable} discards them - vanilla's nameable table
     * copies CUSTOM_NAME and nothing else. Use the power-storage table in the loot provider for any machine that holds
     * something.
     * </p>
     */
    protected long getStoredEnergyForDrop() {
        return 0L;
    }

    protected void setStoredEnergyFromDrop(long stored) {
        // Machines with no internal buffer have nothing to restore.
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NotNull Builder components) {
        super.collectImplicitComponents(components);

        var stored = getStoredEnergyForDrop();

        // ⚠ Zero is written as ABSENT, so an empty machine still stacks and carries no tooltip clutter.
        if (stored > 0L) {
            components.set(HumanDataComponents.STORED_POWER.get(), stored);
        }

        // The breaker layout is part of how a player set the machine up - losing it on every break would make
        // rebuilding a wired room tedious for no reason.
        components.set(HumanDataComponents.POWER_PORTS.get(), ports.toNames());
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.@NotNull DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);

        var stored = componentInput.get(HumanDataComponents.STORED_POWER.get());

        if (stored != null) {
            setStoredEnergyFromDrop(Math.max(0L, stored));
        }

        var portNames = componentInput.get(HumanDataComponents.POWER_PORTS.get());

        if (portNames != null) {
            ports.fromNames(portNames);
        }
    }

    @Override
    public void setRemoved() {
        if (!isRemoved()) {
            var level = getLevel();

            if (level != null && !level.isClientSide) {
                PowerSystem.get((ServerLevel) level).unregisterNode(worldPosition, powerNode);
            }
        }

        super.setRemoved();
    }

    /**
     * [stated] Oct 21 - built into or landed from a ship: the breakers turn with a rotated landing. The grid itself
     * needs nothing - this block entity is new at its new position and links on its first tick, as on any load.
     * Subclasses that store OTHER positions put them right too (and call this).
     */
    @Override
    public void onShipMove(com.human.common.gameplay.ship.ShipMove move) {
        if (move.rotated()) {
            ports.rotate(move);
            setChanged();
        }
    }

    public void setHasInitialized(boolean hasInitialized) {
        this.hasInitialized = hasInitialized;
    }

    public boolean hasInitialized() {
        return hasInitialized;
    }
}
