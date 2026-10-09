package com.human.common.gameplay.block.pipe;

import com.human.common.gameplay.block.fluid.FluidPipeBlock;
import com.human.common.registry.init.HumanMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One pipe end's screen. [stated] Sep 30: sneak + bare-hand right-click a flange.
 * <p>
 * The MODE (Auto, Pull, Push, Both, Off) is a menu button ({@value #BUTTON_MODE}..+4): vanilla's button packet is
 * enough for it. Oct 8: the FILTER - any number of item / tag / mod lines, allow or block, priority - is edited on the
 * screen's filter page and sent whole as a {@link com.human.common.network.packet.C2SPipeFilterPayload}, which the
 * server applies through {@link #applyFilter} to this menu's end only. The filter lives on the pipe's block entity,
 * which sends itself to clients on every change; the menu syncs only where the pipe is and which end.
 */
public class PipeEndMenu extends AbstractContainerMenu {

    public static final int BUTTON_MODE = 0;

    public static final int PLAYER_Y = 140;

    public static final int HOTBAR_Y = 198;

    private static final int DATA_FACE = 6;

    private static final int DATA_COUNT = 7;

    private final ContainerData data;

    private final @Nullable ServerLevel level;

    private final BlockPos pos;

    private final Direction face;

    public PipeEndMenu(int containerId, Inventory inventory) {
        super(HumanMenuTypes.PIPE_END.get(), containerId);
        this.data = new SimpleContainerData(DATA_COUNT);
        this.level = null;
        this.pos = BlockPos.ZERO;
        this.face = Direction.NORTH;
        addPlayer(inventory);
        addDataSlots(data);
    }

    public PipeEndMenu(int containerId, Inventory inventory, ServerLevel level, BlockPos pos, Direction face) {
        super(HumanMenuTypes.PIPE_END.get(), containerId);
        this.level = level;
        this.pos = pos.immutable();
        this.face = face;
        this.data = new ContainerData() {

            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> pos.getX() & 0xFFFF;
                    case 1 -> pos.getX() >>> 16;
                    case 2 -> pos.getY() & 0xFFFF;
                    case 3 -> pos.getY() >>> 16;
                    case 4 -> pos.getZ() & 0xFFFF;
                    case 5 -> pos.getZ() >>> 16;
                    case DATA_FACE -> face.get3DDataValue();
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
        addPlayer(inventory);
        addDataSlots(data);
    }

    private void addPlayer(Inventory inventory) {
        for (var row = 0; row < 3; row++) {
            for (var column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, PLAYER_Y + row * 18));
            }
        }

        for (var column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }
    }

    public BlockPos getPipePos() {
        return new BlockPos(join(0), join(2), join(4));
    }

    private int join(int index) {
        return (data.get(index) & 0xFFFF) | (data.get(index + 1) & 0xFFFF) << 16;
    }

    public Direction getFace() {
        return Direction.from3DDataValue(data.get(DATA_FACE));
    }

    /** The pipe as this side sees it. */
    public @Nullable PipeBlockEntity pipe(Level world) {
        return world.getBlockEntity(getPipePos()) instanceof PipeBlockEntity pipe ? pipe : null;
    }

    public static boolean isFluidPipe(Level world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof FluidPipeBlock;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (level == null) {
            return true;
        }

        if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity pipe)) {
            return false;
        }

        if (id >= BUTTON_MODE && id <= BUTTON_MODE + PipeBlockEntity.OFF) {
            pipe.setOverride(face, (byte) (id - BUTTON_MODE));
            return true;
        }

        return false;
    }

    /**
     * Oct 8 - server side: replaces this menu's end's filter with an edited one from the screen.
     *
     * @param filter the edited filter (already capped by its codec)
     */
    public void applyFilter(PipeFilter filter) {
        if (level != null && level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) {
            pipe.setFilter(face, filter);
        }
    }

    /** Only the player's own inventory is here - shift-click moves nothing. */
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return level == null || level.getBlockEntity(pos) instanceof PipeBlockEntity && player.distanceToSqr(pos.getCenter()) <= 64.0D;
    }
}
