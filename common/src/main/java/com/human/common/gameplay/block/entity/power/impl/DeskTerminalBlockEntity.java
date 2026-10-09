package com.human.common.gameplay.block.entity.power.impl;

import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.menu.terminal.DeskTerminalMenu;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The desk terminal.
 * <h2>⭐ Oct 7 - POWERED ONLY THROUGH A FUSE PANEL</h2> [stated] "the terminal block accepts cables but it doesnt look
 * good so lets make the terminal need to wire to a fusepanel to work" / "the terminal works without power people are
 * able to login and use it with no power."
 * <ul>
 * <li><b>No cables:</b> no face connects to a cable ({@link #canConnectOn}). It is powered by a fuse panel it is bound
 * to with the wiring tool (the panel's hidden in-wall wiring), or by a redstone generator directly beneath it.</li>
 * <li><b>Power while in use only</b> ([stated] Sep 22, a terminal "only draws power while in use"): it asks for
 * {@value #DRAW_PER_TICK} FE/t while it is starting up for someone or while anyone has its screen open, and nothing
 * otherwise.</li>
 * <li><b>Start-up:</b> right-clicking an idle terminal asks the panel for power; the screen opens the moment the first
 * delivery arrives (one tick on a working panel). With no power after {@value #BOOT_TICKS} ticks the player is told
 * why: not wired to a panel, or the panel has no power.</li>
 * <li><b>Losing power</b> closes the screen for everyone using it (the menu stops being valid, see
 * {@link DeskTerminalMenu#stillValid}).</li>
 * </ul>
 * Start-up state is not saved: a terminal reloads idle, which it would be anyway once nobody is at it.
 */
public class DeskTerminalBlockEntity extends PowerConsumerBlockEntity {

    /**
     * FE a tick while starting up or in use. [stated] Oct 7: "it should use the same power output as a redstone
     * generator" - tied to that generator's output (16 FE/t), so one redstone generator runs one terminal exactly.
     */
    public static final long DRAW_PER_TICK = RedstoneGeneratorBlockEntity.OUTPUT;

    /** How long a start-up waits for power before giving up. */
    public static final int BOOT_TICKS = 40;

    /** A screen stays valid this many ticks past the last delivery, so one late grid tick never closes it. */
    public static final int ONLINE_GRACE_TICKS = 10;

    private long lastPoweredTick = Long.MIN_VALUE / 2;

    private @Nullable UUID pendingOpener;

    private long bootUntil;

    public DeskTerminalBlockEntity(BlockPos pos, BlockState blockState) {
        super(HumanBlockEntityTypes.DESK_TERMINAL.get(), pos, blockState);
    }

    /**
     * No cables: power comes from a fuse panel - OR, [stated] Oct 7, "if a terminal is placed above a redstone
     * generator it will work no wire needed it just needs to touch the generator but only from the bottom". So the
     * bottom face connects only when a redstone generator sits directly beneath; every other face, and the bottom over
     * anything else (a cable included), never connects. The grid joins touching power blocks without a cable, the same
     * way stacked batteries join, so nothing else is needed.
     */
    @Override
    public boolean canConnectOn(Direction direction) {
        return direction == Direction.DOWN
            && level != null
            && level.isLoaded(getBlockPos().below())
            && level.getBlockState(getBlockPos().below()).is(com.human.common.registry.init.HumanBlocks.REDSTONE_GENERATOR.get());
    }

    /** Whether the terminal had power within the last {@value #ONLINE_GRACE_TICKS} ticks. */
    public boolean isOnline() {
        return level != null && level.getGameTime() - lastPoweredTick <= ONLINE_GRACE_TICKS;
    }

    /** A player right-clicked it: open now if it is running, otherwise start it up. */
    public void requestOpen(ServerPlayer player) {
        if (isOnline()) {
            openFor(player);
            return;
        }

        pendingOpener = player.getUUID();
        bootUntil = player.serverLevel().getGameTime() + BOOT_TICKS;
        player.displayClientMessage(Component.translatable("message.avp_human.terminal.booting"), true);
    }

    private void openFor(ServerPlayer player) {
        var pos = getBlockPos();

        player.openMenu(
            new SimpleMenuProvider(
                (id, inventory, opener) -> new DeskTerminalMenu(id, inventory, pos),
                Component.translatable("gui.avp_human.terminal.title")
            )
        );
        com.human.common.network.HumanServerListener.sendSentryNetwork(player);
    }

    @Override
    public void poweredTick(Level level, BlockPos blockPos, BlockState blockState) {
        lastPoweredTick = level.getGameTime();

        if (pendingOpener == null) {
            return;
        }

        var opener = pendingOpener;
        pendingOpener = null;

        if (
            level instanceof ServerLevel serverLevel
                && serverLevel.getPlayerByUUID(opener) instanceof ServerPlayer player
                && player.distanceToSqr(blockPos.getCenter()) <= 64.0D
        ) {
            openFor(player);
        }
    }

    @Override
    public void unpoweredTick(Level level, BlockPos blockPos, BlockState blockState) {
        if (pendingOpener == null || level.getGameTime() < bootUntil) {
            return;
        }

        var opener = pendingOpener;
        pendingOpener = null;

        if (level instanceof ServerLevel serverLevel && serverLevel.getPlayerByUUID(opener) instanceof ServerPlayer player) {
            // On its own generator it cannot be short of power, so "not wired" is only said when it truly has neither.
            player.displayClientMessage(
                Component.translatable(
                    getFusePanel() == null && !canConnectOn(Direction.DOWN)
                        ? "message.avp_human.terminal.not_wired"
                        : "message.avp_human.terminal.no_power"
                ),
                true
            );
        }
    }

    /** Power while starting up for someone, or while anyone has the screen open - nothing otherwise. */
    @Override
    public long getRequestedPower() {
        return pendingOpener != null || hasViewer() ? DRAW_PER_TICK : 0L;
    }

    private boolean hasViewer() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        for (var player : serverLevel.players()) {
            if (player.containerMenu instanceof DeskTerminalMenu menu && menu.isFor(getBlockPos())) {
                return true;
            }
        }

        return false;
    }
}
