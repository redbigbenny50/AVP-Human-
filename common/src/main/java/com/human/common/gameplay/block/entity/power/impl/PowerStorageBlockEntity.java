package com.human.common.gameplay.block.entity.power.impl;

import com.human.common.gameplay.block.entity.power.PowerNodeBlockEntity;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.gameplay.power.StoreMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Everything a power store does, shared by the {@code battery} and the {@code charging_hub}.
 * <p>
 * ⭐ [stated] Sep 29: the block that shipped in the MDK as the battery (the ESU with the charging plate) became the
 * charging hub at 500k, and a NEW battery block at 1.5M took the name. This class is the old ESU with the charging
 * plate taken out, so both keep identical grid behaviour: store modes, the transfer cap, the device name, and charge
 * that survives being broken.
 * </p>
 * <h2>⚠ THE TRANSFER CAP IS PER STORE, NOT PER WIRE</h2> [stated] Sep 22: wires carry no rate limit. The cap here is
 * what stops one store from swallowing a whole grid's surplus or dumping its entire charge into a single tick.
 */
public abstract class PowerStorageBlockEntity extends PowerNodeBlockEntity implements PowerNode.PowerStore {

    protected static final String NBT_STORED_POWER = "storedPower";

    private static final String NBT_MODE = "storeMode";

    private static final String NBT_NAME = "deviceName";

    private static final int MAX_NAME_LENGTH = 32;

    private final long capacity;

    private final long transferRate;

    private long storedPower;

    private StoreMode mode = StoreMode.PRIMARY;

    private String deviceName = "";

    protected PowerStorageBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState state,
        long capacity,
        long transferRate
    ) {
        super(type, pos, state);
        this.capacity = capacity;
        this.transferRate = transferRate;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    public long getTransferRate() {
        return transferRate;
    }

    /**
     * Whether the grid may charge this store right now. Always true here; a battery with the redstone upgrade can say
     * no.
     */
    protected boolean canCharge() {
        return true;
    }

    /** Whether the grid may draw from this store right now. Always true here; see {@link #canCharge()}. */
    protected boolean canDischarge() {
        return true;
    }

    @Override
    public long getRequestedPower() {
        // ⚠ getCapacity()/getTransferRate(), never the fields: a subclass (the upgraded battery) computes them.
        return canCharge() ? Math.max(0L, Math.min(getTransferRate(), getCapacity() - storedPower)) : 0L;
    }

    @Override
    public long receivePower(long maxAmount) {
        var accepted = Math.max(0L, Math.min(maxAmount, getRequestedPower()));

        if (accepted > 0L) {
            storedPower += accepted;
            onStoredChanged();
        }

        return accepted;
    }

    @Override
    public long getAvailablePower() {
        return canDischarge() ? Math.max(0L, Math.min(getTransferRate(), storedPower)) : 0L;
    }

    @Override
    public long extractPower(long maxAmount) {
        var extracted = Math.max(0L, Math.min(maxAmount, getAvailablePower()));

        if (extracted > 0L) {
            storedPower -= extracted;
            onStoredChanged();
        }

        return extracted;
    }

    @Override
    public long getStoredPower() {
        return storedPower;
    }

    /**
     * Spends charge on something other than the grid - the charging hub's plate. Returns what was actually taken.
     */
    protected long drainStored(long amount) {
        var taken = Math.max(0L, Math.min(amount, storedPower));

        if (taken > 0L) {
            storedPower -= taken;
            onStoredChanged();
        }

        return taken;
    }

    /** Called whenever the stored amount changes. Subclasses that show their charge hook in here. */
    protected void onStoredChanged() {
        setChanged();
    }

    @Override
    public int getStorePriority() {
        return mode.priority();
    }

    public StoreMode getMode() {
        return mode;
    }

    public void setMode(StoreMode mode) {
        this.mode = mode == null ? StoreMode.PRIMARY : mode;
        setChanged();
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String name) {
        this.deviceName = sanitiseName(name);
        setChanged();
    }

    private static String sanitiseName(@Nullable String name) {
        if (name == null) {
            return "";
        }

        var stripped = net.minecraft.ChatFormatting.stripFormatting(name);

        if (stripped == null) {
            return "";
        }

        return stripped.length() > MAX_NAME_LENGTH ? stripped.substring(0, MAX_NAME_LENGTH) : stripped;
    }

    @Override
    protected long getStoredEnergyForDrop() {
        return storedPower;
    }

    @Override
    protected void setStoredEnergyFromDrop(long stored) {
        storedPower = Math.max(0L, Math.min(getCapacity(), stored));
        onStoredChanged();
    }

    @Override
    public void removeComponentsFromTag(@NotNull CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove(NBT_STORED_POWER);
    }

    /** Loads anything capacity or rate depend on, before the stored power is read and clamped. Nothing here. */
    protected void loadBeforeStorage(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {}

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong(NBT_STORED_POWER, storedPower);
        tag.putString(NBT_MODE, mode.id());

        if (!deviceName.isEmpty()) {
            tag.putString(NBT_NAME, deviceName);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        // ⚠ BEFORE the clamp below: an upgraded battery's capacity depends on its upgrade slots, so they must be loaded
        // first or a full upgraded battery would be clamped down to its base capacity on every load.
        loadBeforeStorage(tag, registries);
        // ⚠ Clamped on load: a pasted or edited tag cannot put more in a store than it can hold.
        storedPower = Math.max(0L, Math.min(getCapacity(), tag.getLong(NBT_STORED_POWER)));
        mode = StoreMode.byName(tag.getString(NBT_MODE));
        deviceName = sanitiseName(tag.getString(NBT_NAME));
    }
}
