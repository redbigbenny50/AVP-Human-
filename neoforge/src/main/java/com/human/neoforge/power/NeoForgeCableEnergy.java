package com.human.neoforge.power;

import com.human.common.gameplay.block.entity.power.CableBlockEntity;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.gameplay.power.external.ExposedCableFace;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Our cable's faces as NeoForge energy, for the other mods' blocks that only connect to a block offering energy - flux
 * plugs and points, energy pipes. See {@link ExposedCableFace} for which faces offer what.
 * <p>
 * The handler looks its face up on every call rather than holding one, so a face that changes mode, or stops offering
 * because a block that offers energy itself was placed there, is followed at once. A face that offers nothing answers
 * "cannot receive, cannot extract". ⚠ The capability speaks {@code int}; amounts are clamped.
 * </p>
 */
public final class NeoForgeCableEnergy implements IEnergyStorage {

    private final CableBlockEntity cable;

    private final Direction side;

    private NeoForgeCableEnergy(CableBlockEntity cable, Direction side) {
        this.cable = cable;
        this.side = side;
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            HumanBlockEntityTypes.CABLE.get(),
            (cable, side) -> side == null ? null : new NeoForgeCableEnergy(cable, side)
        );
    }

    private @Nullable ExposedCableFace face() {
        if (cable.isRemoved() || !(cable.getLevel() instanceof ServerLevel level)) {
            return null;
        }
        return PowerSystem.get(level).exposedFace(cable.getBlockPos(), side);
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        var face = face();
        return face == null ? 0 : (int) face.receive(toReceive, simulate);
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        var face = face();
        return face == null ? 0 : (int) face.extract(toExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        var face = face();
        return face == null ? 0 : (int) Math.min(Integer.MAX_VALUE, face.stored());
    }

    @Override
    public int getMaxEnergyStored() {
        var face = face();
        return face == null ? 0 : (int) Math.min(Integer.MAX_VALUE, face.capacity());
    }

    @Override
    public boolean canExtract() {
        var face = face();
        return face != null && face.canExtract();
    }

    @Override
    public boolean canReceive() {
        var face = face();
        return face != null && face.canReceive();
    }
}
