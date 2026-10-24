package com.human.common.gameplay.fluid.pipe;

import com.human.common.gameplay.block.tank.FluidTankBlockEntity;
import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.fluid.FluidEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * One face of a block that holds fluid, as a pipe sees it. OUR blocks ({@link FluidEndpoint} - tanks, canisters,
 * machines) are read directly, with their real per-face rules; any other mod's block goes through the loader's fluid
 * API (NeoForge's fluid capability, Fabric's Transfer API), installed by each loader. Amounts are millibuckets.
 */
public interface PipeFluidAccess {

    interface Port {

        boolean present();

        /** A pipe may put fluid in on this face. */
        boolean canFill();

        /** A pipe may take fluid out on this face. */
        boolean canDrain();

        /** What could come out right now - the fluid, and how much. EMPTY when nothing. */
        FluidContent peek(int max);

        int fill(Fluid fluid, int amount, boolean simulate);

        int drain(Fluid fluid, int amount, boolean simulate);

        /**
         * Which storage this is. Two faces of one multiblock tank are the same storage - so the network never pipes a
         * tank into itself.
         */
        Object storage();

        /**
         * Oct 8 - whether this is a MACHINE rather than STORAGE, for routing ([stated] Oct 7: machines default to
         * Normal priority and storage to Low, and machines win ties). Tanks and canisters are storage; our machines are
         * machines; other mods' fluid blocks count as storage unless the player sets the end's priority.
         *
         * @return whether this port belongs to a machine
         */
        default boolean isMachine() {
            return false;
        }
    }

    /** A port onto another mod's block, or null when there is none. */
    @Nullable
    Port openForeign(ServerLevel level, BlockPos pos, Direction side);

    static Port open(ServerLevel level, BlockPos pos, Direction side) {
        if (level.getBlockEntity(pos) instanceof FluidEndpoint endpoint) {
            return new EndpointPort(endpoint, side, pos);
        }

        // Oct 17 FIX - [stated] "fluid pipe is set to extract but doesnt show" on the quarry's liquid port (the top of
        // the back-right block; output only). That block is a PART of the terminal and has no block entity of its own,
        // so it was only ever reached through the loader's fluid capability, as if it were another mod's block - and
        // that path calls a face "can take out" only while there is liquid in it right now. An empty tank, which is
        // what the quarry has until it digs into liquid, read as a face with nothing to give: the end resolved to off,
        // so no red flange, until the first liquid arrived. It is our own tank, so it is now opened as ours: its own
        // rule decides (out through the top, never in), whatever is in it.
        var quarryPort = com.human.common.gameplay.quarry.QuarryTerminalBlock.liquidPort(level, pos, level.getBlockState(pos), side);

        if (quarryPort != null) {
            return new EndpointPort(quarryPort, side, pos);
        }

        var foreign = Holder.get().openForeign(level, pos, side);

        return foreign != null ? foreign : NONE;
    }

    Port NONE = new Port() {

        @Override
        public boolean present() {
            return false;
        }

        @Override
        public boolean canFill() {
            return false;
        }

        @Override
        public boolean canDrain() {
            return false;
        }

        @Override
        public FluidContent peek(int max) {
            return FluidContent.EMPTY;
        }

        @Override
        public int fill(Fluid fluid, int amount, boolean simulate) {
            return 0;
        }

        @Override
        public int drain(Fluid fluid, int amount, boolean simulate) {
            return 0;
        }

        @Override
        public Object storage() {
            return this;
        }
    };

    /** Our own blocks, with their real face rules. */
    record EndpointPort(
        FluidEndpoint endpoint,
        Direction side,
        BlockPos pos
    ) implements Port {

        @Override
        public boolean present() {
            return endpoint.isPort(side);
        }

        @Override
        public boolean canFill() {
            return endpoint.canFill(side);
        }

        @Override
        public boolean canDrain() {
            return endpoint.canDrain(side);
        }

        @Override
        public FluidContent peek(int max) {
            var content = endpoint.getContent();

            if (content.isEmpty()) {
                return FluidContent.EMPTY;
            }

            var available = endpoint.extract(content.fluid(), max, true);

            return available <= 0 ? FluidContent.EMPTY : FluidContent.of(content.fluid(), available);
        }

        @Override
        public int fill(Fluid fluid, int amount, boolean simulate) {
            return endpoint.insert(fluid, amount, simulate);
        }

        @Override
        public int drain(Fluid fluid, int amount, boolean simulate) {
            return endpoint.extract(fluid, amount, simulate);
        }

        @Override
        public Object storage() {
            return endpoint instanceof FluidTankBlockEntity tank ? tank.getControllerPos() : pos;
        }

        @Override
        public boolean isMachine() {
            return !(endpoint instanceof FluidTankBlockEntity)
                && !(endpoint instanceof com.human.common.gameplay.block.entity.CanisterBlockEntity);
        }
    }

    final class Holder {

        private static PipeFluidAccess current = (level, pos, side) -> null;

        private Holder() {}

        public static PipeFluidAccess get() {
            return current;
        }

        public static void install(@Nullable PipeFluidAccess access) {
            current = access == null ? (level, pos, side) -> null : access;
        }
    }
}
