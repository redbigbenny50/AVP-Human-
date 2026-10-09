package com.human.common.gameplay.menu.terminal;

import com.human.common.registry.init.HumanMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The desk terminal's screen. [stated] Oct 2: a simple Cryolink-style list of the turrets on the network; clicking one
 * with a camera lens looks through it. MUTHUR grows out of this later.
 * <p>
 * No slots - the list arrives as its own packet. The open menu is what authorises a camera request: the server honours
 * one only from a player who has a terminal open and is still standing at it.
 * </p>
 */
public class DeskTerminalMenu extends AbstractContainerMenu {

    /** Server only: the terminal this screen is for. */
    private final @Nullable BlockPos terminal;

    public DeskTerminalMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public DeskTerminalMenu(int containerId, Inventory inventory, @Nullable BlockPos terminal) {
        super(HumanMenuTypes.DESK_TERMINAL.get(), containerId);
        this.terminal = terminal;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** Server only: whether this screen belongs to the terminal at {@code pos}. */
    public boolean isFor(BlockPos pos) {
        return pos.equals(terminal);
    }

    /**
     * Still at the terminal, the terminal is still there, and (Oct 7) it still has power - a terminal that loses its
     * panel's power closes on everyone using it.
     */
    @Override
    public boolean stillValid(@NotNull Player player) {
        return terminal == null
            || player.level().getBlockState(terminal).is(com.human.common.registry.init.HumanBlocks.DESK_TERMINAL_BLOCK.get())
                && player.distanceToSqr(terminal.getCenter()) <= 64.0D
                && player.level()
                    .getBlockEntity(
                        terminal
                    ) instanceof com.human.common.gameplay.block.entity.power.impl.DeskTerminalBlockEntity desk
                && desk.isOnline();
    }
}
