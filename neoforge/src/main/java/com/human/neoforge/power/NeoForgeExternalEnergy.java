package com.human.neoforge.power;

import com.human.common.gameplay.power.external.ExternalEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Other mods' energy blocks on NeoForge: the energy capability, through a {@link BlockCapabilityCache} per port so a
 * lookup each tick costs almost nothing and follows the block being replaced. The capability speaks {@code int}; every
 * amount is clamped.
 */
public final class NeoForgeExternalEnergy implements ExternalEnergy {

    private NeoForgeExternalEnergy() {}

    public static void install() {
        ExternalEnergy.Holder.install(new NeoForgeExternalEnergy());
    }

    @Override
    public Port open(ServerLevel level, BlockPos pos, Direction side) {
        var cache = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, pos, side);

        return new Port() {

            /** Never reaches into an unloaded chunk - the port lives as long as its grid does. */
            private @Nullable IEnergyStorage handler() {
                return level.isLoaded(pos) ? cache.getCapability() : null;
            }

            @Override
            public boolean present() {
                return handler() != null;
            }

            @Override
            public boolean canReceive() {
                var handler = handler();

                return handler != null && handler.canReceive();
            }

            @Override
            public boolean canExtract() {
                var handler = handler();

                return handler != null && handler.canExtract();
            }

            @Override
            public long receive(long amount, boolean simulate) {
                var handler = handler();

                return handler == null ? 0L : handler.receiveEnergy((int) Math.min(amount, Integer.MAX_VALUE), simulate);
            }

            @Override
            public long extract(long amount, boolean simulate) {
                var handler = handler();

                return handler == null ? 0L : handler.extractEnergy((int) Math.min(amount, Integer.MAX_VALUE), simulate);
            }

            @Override
            public long stored() {
                var handler = handler();

                return handler == null ? 0L : handler.getEnergyStored();
            }

            @Override
            public long capacity() {
                var handler = handler();

                return handler == null ? 0L : handler.getMaxEnergyStored();
            }
        };
    }
}
