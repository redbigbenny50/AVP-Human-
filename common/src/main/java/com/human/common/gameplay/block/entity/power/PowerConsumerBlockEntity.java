package com.human.common.gameplay.block.entity.power;

import com.human.common.gameplay.power.PowerNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base for everything that DRAWS power. Consumers store nothing: [stated] Sep 29, "there is no storage of power at the
 * consumers they only use power when processing things".
 * <h2>⚠⚠ THE BUG THIS REPLACES</h2> {@code receivePower} used to note whether enough was offered and then RETURN 0. The
 * grid bills producers and stores only for what a consumer reports taking, so every consumer ran for free: nothing was
 * ever drawn, and the power log showed its whole demand as unmet while it worked. The ESU had the identical bug before
 * the grid rewrite.
 * <h2>ALL OR NOTHING</h2> A consumer takes its full request or none of it. A machine short of power does not run
 * slower, it waits - so accepting a partial offer would pull power out of the grid and do nothing with it. Returning 0
 * on a short offer leaves that power with the grid, which gives it back to the producers and stores it came from.
 * <h2>ONE TICK OF LAG, BY DESIGN</h2> The grid ticks after block entities, so power received on tick N is spent by the
 * block entity on tick N+1. The {@code powered} flag carries it across and is cleared as it is spent, so one delivery
 * buys exactly one tick of work.
 */
public abstract class PowerConsumerBlockEntity extends PowerNodeBlockEntity implements PowerNode.PowerConsumer {

    private static final String NBT_FUSE_PANEL = "fusePanel";

    /** How often a bound device checks its panel still lists it - cheap, and only while bound. */
    private static final int FUSE_CHECK_INTERVAL = 40;

    private boolean powered;

    /**
     * [stated] Sep 30: the fuse panel this device is bound to, or null. While bound it is powered ONLY through the
     * panel: the grid skips it on any cable it touches (see PowerGrid), so it can never be paid twice.
     */
    private @org.jetbrains.annotations.Nullable BlockPos fusePanel;

    public PowerConsumerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public abstract void unpoweredTick(
        Level level,
        BlockPos blockPos,
        BlockState blockState
    );

    public abstract void poweredTick(
        Level level,
        BlockPos blockPos,
        BlockState blockState
    );

    @Override
    public void serverTick(Level level, BlockPos blockPos, BlockState blockState) {
        super.serverTick(level, blockPos, blockState);

        // A panel that was broken, or that dropped this device, releases it. An UNLOADED panel keeps it: the device is
        // simply unpowered until the panel's chunk is back, as the grid's loaded-fragment rule has it.
        if (fusePanel != null && level.getGameTime() % FUSE_CHECK_INTERVAL == 0 && level.isLoaded(fusePanel)) {
            if (
                !(level.getBlockEntity(fusePanel) instanceof com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel)
                    || !panel.isBound(blockPos)
                    || !canBindToFusePanel()
            ) {
                setFusePanel(null);
            }
        }

        var hadPower = powered;

        // Spent now, whichever branch runs: the next tick's work needs the next tick's delivery.
        powered = false;

        if (!hadPower) {
            unpoweredTick(level, blockPos, blockState);
            return;
        }

        poweredTick(level, blockPos, blockState);
    }

    @Override
    public long receivePower(long maxAmount) {
        var wanted = getRequestedPower();

        // ⚠ Nothing asked for means nothing taken. The grid only offers to a consumer that requested power, but a
        // request can drop to zero between the grid polling it and offering to it.
        if (wanted <= 0L || maxAmount < wanted) {
            return 0L;
        }

        powered = true;

        return wanted;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Fuse panel
    // ------------------------------------------------------------------------------------------------------------

    public @org.jetbrains.annotations.Nullable BlockPos getFusePanel() {
        return fusePanel;
    }

    public void setFusePanel(@org.jetbrains.annotations.Nullable BlockPos panel) {
        if (java.util.Objects.equals(fusePanel, panel)) {
            return;
        }

        fusePanel = panel == null ? null : panel.immutable();
        setChanged();
    }

    /**
     * Whether this device may be bound to a fuse panel. [stated] Sep 30: devices in the {@code avp_human:fuse_devices}
     * block tag bind as they are; a MACHINE needs a hub chip, and overrides this.
     */
    public boolean canBindToFusePanel() {
        return getBlockState().is(com.human.common.registry.tag.HumanBlockTags.FUSE_DEVICES);
    }

    @Override
    protected void saveAdditional(
        @org.jetbrains.annotations.NotNull net.minecraft.nbt.CompoundTag tag,
        net.minecraft.core.HolderLookup.@org.jetbrains.annotations.NotNull Provider registries
    ) {
        super.saveAdditional(tag, registries);

        if (fusePanel != null) {
            tag.putLong(NBT_FUSE_PANEL, fusePanel.asLong());
        }
    }

    @Override
    protected void loadAdditional(
        @org.jetbrains.annotations.NotNull net.minecraft.nbt.CompoundTag tag,
        net.minecraft.core.HolderLookup.@org.jetbrains.annotations.NotNull Provider registries
    ) {
        super.loadAdditional(tag, registries);
        fusePanel = tag.contains(NBT_FUSE_PANEL) ? BlockPos.of(tag.getLong(NBT_FUSE_PANEL)) : null;
    }

    /** Whether this block entity was fed on the last grid tick, before its own tick spends it. */
    protected boolean isPowered() {
        return powered;
    }
}
