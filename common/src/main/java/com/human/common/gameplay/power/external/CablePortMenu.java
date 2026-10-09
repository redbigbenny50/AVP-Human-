package com.human.common.gameplay.power.external;

import com.human.common.gameplay.block.power.CableBlock;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.gameplay.power.grid.PowerGridExploreUtil;
import com.human.common.registry.init.HumanMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A cable's port screen: for each face that touches another mod's energy block, its mode - auto, out (red), in (green),
 * both (yellow) or off. The button ids are the faces' 3D data values; a click cycles that face and re-attaches it to
 * the grid at once. No slots.
 */
public class CablePortMenu extends AbstractContainerMenu {

    private static final int DATA_MASK = 0;

    private static final int DATA_MODES = 1;

    private static final int DATA_POS = DATA_MODES + 6;

    private static final int DATA_COUNT = DATA_POS + 6;

    private final ContainerData data;

    private final @Nullable ServerLevel level;

    private final BlockPos pos;

    public CablePortMenu(int containerId, Inventory inventory) {
        super(HumanMenuTypes.CABLE_PORTS.get(), containerId);
        this.data = new SimpleContainerData(DATA_COUNT);
        this.level = null;
        this.pos = BlockPos.ZERO;
        addDataSlots(data);
    }

    public CablePortMenu(int containerId, Inventory inventory, ServerLevel level, BlockPos pos) {
        super(HumanMenuTypes.CABLE_PORTS.get(), containerId);
        this.level = level;
        this.pos = pos.immutable();
        this.data = new ContainerData() {

            @Override
            public int get(int index) {
                if (index == DATA_MASK) {
                    return externalFaces(level, CablePortMenu.this.pos);
                }

                if (index >= DATA_MODES && index < DATA_POS) {
                    return CablePortModes.get(level).get(CablePortMenu.this.pos, Direction.from3DDataValue(index - DATA_MODES)).ordinal();
                }

                var p = CablePortMenu.this.pos;

                return switch (index - DATA_POS) {
                    case 0 -> p.getX() & 0xFFFF;
                    case 1 -> p.getX() >>> 16;
                    case 2 -> p.getY() & 0xFFFF;
                    case 3 -> p.getY() >>> 16;
                    case 4 -> p.getZ() & 0xFFFF;
                    case 5 -> p.getZ() >>> 16;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        addDataSlots(data);
    }

    /** Which of the cable's faces touch another mod's energy block, as a bitmask of 3D data values. */
    public static int externalFaces(ServerLevel level, BlockPos pos) {
        var mask = 0;
        var energy = ExternalEnergy.Holder.get();

        for (var face : Direction.values()) {
            var target = pos.relative(face);

            if (
                level.isLoaded(target) && !PowerGridExploreUtil.isConnectable(level, target)
                    && ExternalEnergy.mayHoldEnergy(level, target)
                    && energy.open(level, target, face.getOpposite()).present()
            ) {
                mask |= 1 << face.get3DDataValue();
            }
        }

        return mask;
    }

    public boolean hasExternal(Direction face) {
        return (data.get(DATA_MASK) & 1 << face.get3DDataValue()) != 0;
    }

    public PortMode getMode(Direction face) {
        return PortMode.byOrdinal(data.get(DATA_MODES + face.get3DDataValue()));
    }

    public BlockPos getCablePos() {
        return new BlockPos(join(DATA_POS), join(DATA_POS + 2), join(DATA_POS + 4));
    }

    private int join(int index) {
        return (data.get(index) & 0xFFFF) | (data.get(index + 1) & 0xFFFF) << 16;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id < 0 || id >= 6) {
            return false;
        }

        if (level == null) {
            return true;
        }

        var face = Direction.from3DDataValue(id);

        if ((externalFaces(level, pos) & 1 << id) == 0) {
            return false;
        }

        var modes = CablePortModes.get(level);

        modes.set(pos, face, modes.get(pos, face).next());
        PowerSystem.get(level).refreshCable(level, pos);
        // The arm and its flange colour follow the new mode - Off hides the arm.
        CableBlock.refreshVisual(level, pos);

        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return level == null
            || level.getBlockState(pos).getBlock() instanceof CableBlock && player.distanceToSqr(pos.getCenter()) <= 64.0D;
    }
}
