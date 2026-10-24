package com.human.common.gameplay.item;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.block.entity.power.PowerNodeBlockEntity;
import com.human.common.gameplay.block.power.CableBlock;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.gameplay.power.grid.PowerGrid;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * The wiring tool. [stated] Sep 29 readme, the parts that have something to act on today:
 * <ul>
 * <li><b>Right-click</b> a machine, cable or any power block: whether it has power, and the grid it is on - what that
 * grid generates, what its machines want, and what its stores hold.</li>
 * <li><b>Shift + right-click a machine</b>: switch it off or on. It shows its OFF front and asks for nothing until
 * switched back on. Machines start on.</li>
 * <li><b>Shift + right-click a face of a battery, charging hub or generator</b>: open or close that face's breaker.
 * Faces that can never carry power (a battery's front, a machine's sides) have no breaker and say so.</li>
 * </ul>
 * <h2>FUSE PANELS</h2> [stated] Sep 30:
 * <ul>
 * <li><b>Right-click a fuse panel</b>: select it, then right-click a device to connect it (again to disconnect).</li>
 * <li><b>Or device first</b> ([stated] Oct 14): right-click a device - its power readout shows and it is selected -
 * then right-click the fuse panel. Works for sentry turrets too.</li>
 * <li>A device connects if it is in the {@code avp_human:fuse_devices} tag, or is a machine with a hub chip; it must be
 * within the panel's range, and the panel must have room. Every outcome is said on screen ([stated] Oct 14): success,
 * success-but-the-panel-has-no-power, or the reason it failed.</li>
 * <li><b>Shift + right-click the air</b>: clear the selection.</li>
 * </ul>
 * Anything that cannot bind still gets the readout. Wall switches and hacking wait for those blocks.
 * <h2>⚠ READOUT COST</h2> The readout polls every node on the grid once, on the click, and never again - see
 * {@link PowerGrid#snapshot()}.
 */
public class WiringToolItem extends Item {

    public WiringToolItem(Properties properties) {
        super(properties);
    }

    /** The three things the tool can hold selected - only one at a time. */
    private static final com.blib.api.common.registry.v1.BLibHolder<net.minecraft.core.component.DataComponentType<net.minecraft.core.GlobalPos>> SELECTED_PANEL =
        com.human.common.registry.init.HumanDataComponents.SELECTED_FUSE_PANEL;

    private static final com.blib.api.common.registry.v1.BLibHolder<net.minecraft.core.component.DataComponentType<net.minecraft.core.GlobalPos>> SELECTED_DEVICE =
        com.human.common.registry.init.HumanDataComponents.SELECTED_FUSE_DEVICE;

    private static final com.blib.api.common.registry.v1.BLibHolder<net.minecraft.core.component.DataComponentType<java.util.UUID>> SELECTED_SENTRY =
        com.human.common.registry.init.HumanDataComponents.SELECTED_FUSE_SENTRY;

    // ------------------------------------------------------------------------------------------------------------
    // [stated] Oct 14 - on-screen feedback so nobody files a false bug report: "If you click on a fuse panel have it
    // say 'panel selected please right click the device you wish to connect' have the reverse come up if selecting
    // the device first. If its successful have it say connection successful if it fails say why ... if its
    // connection was successful but the panel has no power." Device first shows the power readout AND selects it.
    // Status lines go to the action bar (above the hotbar); the readout stays in chat, so neither hides the other.
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var blockEntity = level.getBlockEntity(pos);
        var state = level.getBlockState(pos);

        // [stated] Oct 14 - a pipe: the network readout (what is connected, what each end does, why nothing moves).
        if (state.getBlock() instanceof com.human.common.gameplay.block.pipe.PipeBlock pipe) {
            if (!level.isClientSide && player != null) {
                pipe.printWiringReadout((ServerLevel) level, pos, state, player);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // [stated] Oct 20 - shift-right-click a resonator to SEE its field: the 32-block sphere, drawn through blocks
        // for a few seconds. (A plain right-click still binds it to a fuse panel; its only cable face is underneath,
        // so it has no breaker for a shift-click to toggle anyway.)
        if (
            player != null && player.isShiftKeyDown()
                && blockEntity instanceof com.human.common.gameplay.block.entity.power.impl.ResonatorBlockEntity
        ) {
            if (level.isClientSide) {
                // Through the hook, never the client renderer itself: this class loads on dedicated servers too.
                com.human.common.gameplay.block.resonator.ResonatorSphereHook.show.accept(
                    pos,
                    com.human.common.gameplay.block.entity.power.impl.ResonatorBlockEntity.RADIUS
                );
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        var isCable = state.getBlock() instanceof CableBlock;

        // [stated] Oct 14: "when holding a wiring tool it should not open the block you are clicking on. it should be
        // trying to wire it or give you one of the messages why it cant be wired." Any block that is not part of the
        // power system says so - the click is used either way (the block itself never sees it - see
        // MixinBlockStateBase_WiringToolFirst).
        if (!(blockEntity instanceof PowerNodeBlockEntity) && !isCable) {
            if (!level.isClientSide && player != null) {
                status(player, "message.avp_human.fuse.cannot_bind", ChatFormatting.RED);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }

        var serverLevel = (ServerLevel) level;
        var tool = context.getItemInHand();

        if (!player.isShiftKeyDown()) {
            if (blockEntity instanceof com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel) {
                // A device picked first is connected to the panel clicked now; otherwise the panel is selected.
                if (tool.has(SELECTED_DEVICE.get()) || tool.has(SELECTED_SENTRY.get())) {
                    connectSelectedDevice(serverLevel, player, tool, panel);
                } else {
                    selectPanel(serverLevel, player, tool, panel);
                }

                return InteractionResult.CONSUME;
            }

            if (blockEntity instanceof com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity device) {
                var selected = tool.get(SELECTED_PANEL.get());

                if (selected != null) {
                    var panel = resolvePanel(serverLevel, player, tool, selected);

                    if (panel != null) {
                        connectBlock(serverLevel, player, panel, device);
                    }

                    return InteractionResult.CONSUME;
                }

                // Device first: the readout as always, and the device is selected if it can be wired.
                readout(serverLevel, player, pos, blockEntity);
                selectDevice(serverLevel, player, tool, device);

                return InteractionResult.CONSUME;
            }
        }

        if (player.isShiftKeyDown()) {
            if (blockEntity instanceof ElectricMachineBlockEntity machine) {
                toggleMachine(serverLevel, player, machine);
            } else if (blockEntity instanceof PowerNodeBlockEntity node) {
                toggleBreaker(serverLevel, player, node, context);
            }

            return InteractionResult.CONSUME;
        }

        readout(serverLevel, player, pos, blockEntity);

        return InteractionResult.CONSUME;
    }

    /**
     * [stated] Sep 30 / Oct 14: right-click a SENTRY TURRET with a panel selected to connect it (or disconnect it), or
     * with nothing selected to select it, then right-click the panel. Turrets are entities, so this is the tool's
     * entity interaction - and the turret passes wiring-tool clicks on to it (see SentryTurret.mobInteract).
     */
    @Override
    public @NotNull InteractionResult interactLivingEntity(
        @NotNull net.minecraft.world.item.ItemStack stack,
        @NotNull Player player,
        @NotNull net.minecraft.world.entity.LivingEntity entity,
        @NotNull net.minecraft.world.InteractionHand hand
    ) {
        if (!(entity instanceof com.human.common.gameplay.entity.machine.SentryTurret turret)) {
            return InteractionResult.PASS;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        // The stack in hand, not the copy vanilla may pass: the selection must stay on the tool.
        var tool = player.getItemInHand(hand);
        var selected = tool.get(SELECTED_PANEL.get());

        if (selected == null) {
            tool.remove(SELECTED_DEVICE.get());
            tool.set(SELECTED_SENTRY.get(), turret.getUUID());
            level.playSound(null, turret.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4F, 1.4F);
            status(player, "message.avp_human.fuse.sentry_selected", ChatFormatting.AQUA);

            return InteractionResult.CONSUME;
        }

        var panel = resolvePanel(level, player, tool, selected);

        if (panel != null) {
            connectSentry(level, player, panel, turret);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(
        @NotNull net.minecraft.world.item.ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull java.util.List<Component> lines,
        @NotNull net.minecraft.world.item.TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, lines, flag);

        var panel = stack.get(SELECTED_PANEL.get());
        var device = stack.get(SELECTED_DEVICE.get());

        if (panel != null) {
            var pos = panel.pos();
            lines.add(
                Component.translatable("tooltip.avp_human.wiring_tool.selected", pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.AQUA)
            );
        }

        if (device != null) {
            var pos = device.pos();
            lines.add(
                Component.translatable("tooltip.avp_human.wiring_tool.selected_device", pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.AQUA)
            );
        }

        if (stack.has(SELECTED_SENTRY.get())) {
            lines.add(Component.translatable("tooltip.avp_human.wiring_tool.selected_sentry").withStyle(ChatFormatting.AQUA));
        }

        for (var key : TOOLTIP_LINES) {
            lines.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }

    private static final String[] TOOLTIP_LINES = {
        "tooltip.avp_human.wiring_tool.readout",
        "tooltip.avp_human.wiring_tool.select_panel",
        "tooltip.avp_human.wiring_tool.bind",
        "tooltip.avp_human.wiring_tool.toggle",
        "tooltip.avp_human.wiring_tool.clear"
    };

    /** Shift + right-click the air clears whatever is selected - panel, device or turret. */
    @Override
    public @NotNull net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(
        @NotNull net.minecraft.world.level.Level level,
        @NotNull Player player,
        @NotNull net.minecraft.world.InteractionHand hand
    ) {
        var tool = player.getItemInHand(hand);
        var anything = tool.has(SELECTED_PANEL.get()) || tool.has(SELECTED_DEVICE.get()) || tool.has(SELECTED_SENTRY.get());

        if (player.isShiftKeyDown() && anything) {
            if (!level.isClientSide) {
                clearSelection(tool);
                status(player, "message.avp_human.fuse.cleared", ChatFormatting.GRAY);
            }

            return net.minecraft.world.InteractionResultHolder.sidedSuccess(tool, level.isClientSide);
        }

        return super.use(level, player, hand);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Selecting
    // ------------------------------------------------------------------------------------------------------------

    private static void selectPanel(
        ServerLevel level,
        Player player,
        net.minecraft.world.item.ItemStack tool,
        com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel
    ) {
        clearSelection(tool);
        tool.set(SELECTED_PANEL.get(), net.minecraft.core.GlobalPos.of(level.dimension(), panel.getBlockPos()));
        level.playSound(null, panel.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4F, 1.4F);
        status(player, "message.avp_human.fuse.selected", ChatFormatting.AQUA, panel.getBoundCount(), panel.getDeviceCapacity());
    }

    /**
     * Device first. A device that can be wired is selected; one that cannot says why, so a machine without its hub chip
     * is not mistaken for a broken tool. Other consumers just show their readout.
     */
    private static void selectDevice(
        ServerLevel level,
        Player player,
        net.minecraft.world.item.ItemStack tool,
        com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity device
    ) {
        if (!device.canBindToFusePanel()) {
            if (device instanceof ElectricMachineBlockEntity) {
                status(player, "message.avp_human.fuse.needs_hub_chip", ChatFormatting.RED);
            }

            return;
        }

        clearSelection(tool);
        tool.set(SELECTED_DEVICE.get(), net.minecraft.core.GlobalPos.of(level.dimension(), device.getBlockPos()));
        level.playSound(null, device.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4F, 1.4F);
        status(player, "message.avp_human.fuse.device_selected", ChatFormatting.AQUA);
    }

    /** Device first, then the panel: connect whichever device the tool holds. The selection is used up either way. */
    private static void connectSelectedDevice(
        ServerLevel level,
        Player player,
        net.minecraft.world.item.ItemStack tool,
        com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel
    ) {
        var device = tool.get(SELECTED_DEVICE.get());
        var sentry = tool.get(SELECTED_SENTRY.get());

        tool.remove(SELECTED_DEVICE.get());
        tool.remove(SELECTED_SENTRY.get());

        if (device != null) {
            if (!device.dimension().equals(level.dimension())) {
                status(player, "message.avp_human.fuse.device_other_dimension", ChatFormatting.RED);
                return;
            }

            if (
                !level.isLoaded(device.pos())
                    || !(level.getBlockEntity(
                        device.pos()
                    ) instanceof com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity consumer)
            ) {
                status(player, "message.avp_human.fuse.device_gone", ChatFormatting.RED);
                return;
            }

            connectBlock(level, player, panel, consumer);
            return;
        }

        // A turret: found by its id in THIS dimension only (it may have been picked up, killed or moved elsewhere).
        if (!(level.getEntity(sentry) instanceof com.human.common.gameplay.entity.machine.SentryTurret turret) || !turret.isAlive()) {
            status(player, "message.avp_human.fuse.device_gone", ChatFormatting.RED);
            return;
        }

        connectSentry(level, player, panel, turret);
    }

    private static void clearSelection(net.minecraft.world.item.ItemStack tool) {
        tool.remove(SELECTED_PANEL.get());
        tool.remove(SELECTED_DEVICE.get());
        tool.remove(SELECTED_SENTRY.get());
    }

    /** The selected panel, or null - after saying why (another dimension, or gone, which also clears it). */
    private static com.human.common.gameplay.power.fuse.@org.jetbrains.annotations.Nullable FusePanelBlockEntity resolvePanel(
        ServerLevel level,
        Player player,
        net.minecraft.world.item.ItemStack tool,
        net.minecraft.core.GlobalPos selected
    ) {
        if (!selected.dimension().equals(level.dimension())) {
            status(player, "message.avp_human.fuse.other_dimension", ChatFormatting.RED);
            return null;
        }

        if (
            !level.isLoaded(selected.pos())
                || !(level.getBlockEntity(selected.pos()) instanceof com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel)
        ) {
            tool.remove(SELECTED_PANEL.get());
            status(player, "message.avp_human.fuse.gone", ChatFormatting.RED);
            return null;
        }

        return panel;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Connecting - one path per kind of device, used by both orders
    // ------------------------------------------------------------------------------------------------------------

    private static void connectBlock(
        ServerLevel level,
        Player player,
        com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel,
        com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity device
    ) {
        var panelPos = panel.getBlockPos();
        var devicePos = device.getBlockPos();

        // Already on this panel: disconnect.
        if (panelPos.equals(device.getFusePanel())) {
            panel.unbind(devicePos);
            device.setFusePanel(null);
            level.playSound(null, devicePos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 0.6F);
            status(player, "message.avp_human.fuse.unbound", ChatFormatting.GRAY, panel.getBoundCount(), panel.getDeviceCapacity());
            return;
        }

        if (!device.canBindToFusePanel()) {
            status(
                player,
                device instanceof ElectricMachineBlockEntity
                    ? "message.avp_human.fuse.needs_hub_chip"
                    : "message.avp_human.fuse.cannot_bind",
                ChatFormatting.RED
            );
            return;
        }

        if (!checkRangeAndRoom(player, panel, devicePos)) {
            return;
        }

        // One panel per device: connecting here moves it off its old panel.
        var previous = device.getFusePanel();

        if (
            previous != null && level.isLoaded(previous)
                && level.getBlockEntity(previous) instanceof com.human.common.gameplay.power.fuse.FusePanelBlockEntity oldPanel
        ) {
            oldPanel.unbind(devicePos);
        }

        panel.bind(devicePos);
        device.setFusePanel(panelPos);
        level.playSound(null, devicePos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 1.2F);
        connected(level, player, panel);
    }

    private static void connectSentry(
        ServerLevel level,
        Player player,
        com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel,
        com.human.common.gameplay.entity.machine.SentryTurret turret
    ) {
        var panelPos = panel.getBlockPos();
        var id = turret.getUUID();

        if (panelPos.equals(turret.getFusePanel())) {
            panel.unbindSentry(id);
            turret.setFusePanel(null);
            level.playSound(null, turret.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 0.6F);
            status(player, "message.avp_human.fuse.unbound", ChatFormatting.GRAY, panel.getBoundCount(), panel.getDeviceCapacity());
            return;
        }

        if (!checkRangeAndRoom(player, panel, turret.blockPosition())) {
            return;
        }

        // One panel per turret: connecting here moves it off its old panel.
        var previous = turret.getFusePanel();

        if (
            previous != null && level.isLoaded(previous)
                && level.getBlockEntity(previous) instanceof com.human.common.gameplay.power.fuse.FusePanelBlockEntity oldPanel
        ) {
            oldPanel.unbindSentry(id);
        }

        panel.bindSentry(id, turret.blockPosition());
        turret.setFusePanel(panelPos);
        level.playSound(null, turret.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 1.2F);
        connected(level, player, panel);
    }

    /** Range, then room - each failure says which, with the numbers. */
    private static boolean checkRangeAndRoom(
        Player player,
        com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel,
        net.minecraft.core.BlockPos devicePos
    ) {
        if (!panel.isInRange(devicePos)) {
            // The same straight-line distance the panel's own range check measures, rounded up.
            var distance = (int) Math.ceil(Math.sqrt(devicePos.distSqr(panel.getBlockPos())));

            status(
                player,
                "message.avp_human.fuse.out_of_range",
                ChatFormatting.RED,
                distance,
                com.human.common.gameplay.power.fuse.FusePanelBlockEntity.RANGE
            );
            return false;
        }

        if (panel.getBoundCount() >= panel.getDeviceCapacity()) {
            status(player, "message.avp_human.fuse.full", ChatFormatting.RED, panel.getDeviceCapacity());
            return false;
        }

        return true;
    }

    /**
     * Connected. Green - or yellow if the panel has no power to pass on: not on a grid at all (no cable into its back),
     * or on one that is generating nothing and has nothing stored. One grid snapshot, on this click only.
     */
    private static void connected(ServerLevel level, Player player, com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel) {
        var grid = PowerSystem.get(level).gridAt(panel.getBlockPos());
        var powered = false;

        if (grid != null) {
            var snapshot = grid.snapshot();
            powered = snapshot.generation() > 0 || snapshot.stored() > 0;
        }

        if (powered) {
            status(player, "message.avp_human.fuse.bound", ChatFormatting.GREEN, panel.getBoundCount(), panel.getDeviceCapacity());
        } else {
            status(
                player,
                "message.avp_human.fuse.bound_no_power",
                ChatFormatting.YELLOW,
                panel.getBoundCount(),
                panel.getDeviceCapacity()
            );
        }
    }

    /** One status line above the hotbar, coloured by what it means. */
    private static void status(Player player, String key, ChatFormatting colour, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(colour), true);
    }

    private static void toggleMachine(ServerLevel level, Player player, ElectricMachineBlockEntity machine) {
        var enabled = !machine.isEnabled();

        machine.setEnabled(enabled);
        level.playSound(null, machine.getBlockPos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, enabled ? 1.2F : 0.6F);
        player.displayClientMessage(
            Component.translatable(enabled ? "message.avp_human.machine_activated" : "message.avp_human.machine_deactivated"),
            true
        );
    }

    private static void toggleBreaker(ServerLevel level, Player player, PowerNodeBlockEntity node, UseOnContext context) {
        var face = context.getClickedFace();

        if (!node.canConnectOn(face)) {
            player.displayClientMessage(Component.translatable("message.avp_human.no_port_here"), true);
            return;
        }

        var closed = !node.getPorts().isClosed(face);

        if (!node.setPortClosed(face, closed)) {
            return;
        }

        level.playSound(null, node.getBlockPos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, closed ? 0.6F : 1.2F);
        player.displayClientMessage(
            Component.translatable(
                closed ? "message.avp_human.port_closed" : "message.avp_human.port_opened",
                Component.translatable("message.avp_human.direction." + face.getSerializedName())
            ),
            true
        );
    }

    private static void readout(ServerLevel level, Player player, net.minecraft.core.BlockPos pos, Object blockEntity) {
        var grid = PowerSystem.get(level).gridAt(pos);

        if (blockEntity instanceof ElectricMachineBlockEntity machine) {
            var key = switch (machine.getStatus()) {
                case ElectricMachineBlockEntity.STATUS_DISABLED -> "message.avp_human.wiring.machine_disabled";
                case ElectricMachineBlockEntity.STATUS_WORKING -> "message.avp_human.wiring.machine_working";
                case ElectricMachineBlockEntity.STATUS_IDLE -> "message.avp_human.wiring.machine_idle";
                default -> "message.avp_human.wiring.machine_no_power";
            };

            player.sendSystemMessage(
                Component.translatable(key, format(machine.getDrawPerTick())).withStyle(ChatFormatting.GOLD)
            );
        }

        if (grid == null) {
            player.sendSystemMessage(Component.translatable("message.avp_human.wiring.no_grid").withStyle(ChatFormatting.RED));
            return;
        }

        var snapshot = grid.snapshot();

        player.sendSystemMessage(
            Component.translatable(
                "message.avp_human.wiring.grid",
                snapshot.producers(),
                snapshot.consumers(),
                snapshot.stores()
            ).withStyle(ChatFormatting.AQUA)
        );
        player.sendSystemMessage(
            Component.translatable(
                "message.avp_human.wiring.flow",
                format(snapshot.generation()),
                format(snapshot.demand())
            ).withStyle(ChatFormatting.GRAY)
        );

        if (snapshot.stores() > 0) {
            player.sendSystemMessage(
                Component.translatable(
                    "message.avp_human.wiring.stored",
                    format(snapshot.stored()),
                    format(snapshot.capacity())
                ).withStyle(ChatFormatting.GRAY)
            );
        }
    }

    private static String format(long value) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(value);
    }
}
