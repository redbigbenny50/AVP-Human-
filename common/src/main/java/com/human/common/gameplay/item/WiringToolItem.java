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
 * <li><b>Right-click a fuse panel</b>: select it. The tool remembers it.</li>
 * <li><b>With a panel selected, right-click a device</b>: bind it to that panel, or unbind it if it already is. A
 * device binds if it is in the {@code avp_human:fuse_devices} tag, or is a machine with a hub chip; it must be within
 * the panel's range, and the panel must have room.</li>
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

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var blockEntity = level.getBlockEntity(pos);
        var isCable = level.getBlockState(pos).getBlock() instanceof CableBlock;

        if (!(blockEntity instanceof PowerNodeBlockEntity) && !isCable) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }

        var serverLevel = (ServerLevel) level;
        var tool = context.getItemInHand();

        if (!player.isShiftKeyDown()) {
            if (blockEntity instanceof com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel) {
                selectPanel(serverLevel, player, tool, panel);
                return InteractionResult.CONSUME;
            }

            var selected = tool.get(com.human.common.registry.init.HumanDataComponents.SELECTED_FUSE_PANEL.get());

            if (selected != null && blockEntity instanceof com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity device) {
                toggleBinding(serverLevel, player, tool, selected, device);
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
     * Oct 7 - [stated] "the wiring tool has no tool tip to explain how it works". One line per thing it does, plus the
     * panel it currently has selected.
     */
    @Override
    public void appendHoverText(
        @NotNull net.minecraft.world.item.ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull java.util.List<Component> lines,
        @NotNull net.minecraft.world.item.TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, lines, flag);

        var selected = stack.get(com.human.common.registry.init.HumanDataComponents.SELECTED_FUSE_PANEL.get());

        if (selected != null) {
            var pos = selected.pos();
            lines.add(
                Component.translatable("tooltip.avp_human.wiring_tool.selected", pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(net.minecraft.ChatFormatting.AQUA)
            );
        }

        for (var key : TOOLTIP_LINES) {
            lines.add(Component.translatable(key).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    private static final String[] TOOLTIP_LINES = {
        "tooltip.avp_human.wiring_tool.readout",
        "tooltip.avp_human.wiring_tool.select_panel",
        "tooltip.avp_human.wiring_tool.bind",
        "tooltip.avp_human.wiring_tool.toggle",
        "tooltip.avp_human.wiring_tool.clear"
    };

    /** Shift + right-click the air clears the selected fuse panel. */
    @Override
    public @NotNull net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(
        @NotNull net.minecraft.world.level.Level level,
        @NotNull Player player,
        @NotNull net.minecraft.world.InteractionHand hand
    ) {
        var tool = player.getItemInHand(hand);
        var key = com.human.common.registry.init.HumanDataComponents.SELECTED_FUSE_PANEL.get();

        if (player.isShiftKeyDown() && tool.has(key)) {
            if (!level.isClientSide) {
                tool.remove(key);
                player.displayClientMessage(Component.translatable("message.avp_human.fuse.cleared"), true);
            }

            return net.minecraft.world.InteractionResultHolder.sidedSuccess(tool, level.isClientSide);
        }

        return super.use(level, player, hand);
    }

    private static void selectPanel(
        ServerLevel level,
        Player player,
        net.minecraft.world.item.ItemStack tool,
        com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel
    ) {
        tool.set(
            com.human.common.registry.init.HumanDataComponents.SELECTED_FUSE_PANEL.get(),
            net.minecraft.core.GlobalPos.of(level.dimension(), panel.getBlockPos())
        );
        level.playSound(null, panel.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4F, 1.4F);
        player.displayClientMessage(
            Component.translatable("message.avp_human.fuse.selected", panel.getBoundCount(), panel.getDeviceCapacity()),
            true
        );
    }

    private static void toggleBinding(
        ServerLevel level,
        Player player,
        net.minecraft.world.item.ItemStack tool,
        net.minecraft.core.GlobalPos selected,
        com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity device
    ) {
        var panelPos = selected.pos();
        var devicePos = device.getBlockPos();

        if (!selected.dimension().equals(level.dimension())) {
            player.displayClientMessage(Component.translatable("message.avp_human.fuse.other_dimension"), true);
            return;
        }

        if (
            !level.isLoaded(panelPos) || !(level.getBlockEntity(
                panelPos
            ) instanceof com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel)
        ) {
            tool.remove(com.human.common.registry.init.HumanDataComponents.SELECTED_FUSE_PANEL.get());
            player.displayClientMessage(Component.translatable("message.avp_human.fuse.gone"), true);
            return;
        }

        // Already on this panel: unbind.
        if (panelPos.equals(device.getFusePanel())) {
            panel.unbind(devicePos);
            device.setFusePanel(null);
            level.playSound(null, devicePos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 0.6F);
            player.displayClientMessage(
                Component.translatable("message.avp_human.fuse.unbound", panel.getBoundCount(), panel.getDeviceCapacity()),
                true
            );
            return;
        }

        if (!device.canBindToFusePanel()) {
            player.displayClientMessage(
                Component.translatable(
                    device instanceof ElectricMachineBlockEntity
                        ? "message.avp_human.fuse.needs_hub_chip"
                        : "message.avp_human.fuse.cannot_bind"
                ),
                true
            );
            return;
        }

        if (!panel.isInRange(devicePos)) {
            player.displayClientMessage(
                Component.translatable(
                    "message.avp_human.fuse.out_of_range",
                    com.human.common.gameplay.power.fuse.FusePanelBlockEntity.RANGE
                ),
                true
            );
            return;
        }

        if (panel.getBoundCount() >= panel.getDeviceCapacity()) {
            player.displayClientMessage(Component.translatable("message.avp_human.fuse.full", panel.getDeviceCapacity()), true);
            return;
        }

        // One panel per device: binding here moves it off its old panel.
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
        player.displayClientMessage(
            Component.translatable("message.avp_human.fuse.bound", panel.getBoundCount(), panel.getDeviceCapacity()),
            true
        );
    }

    /**
     * [stated] Sep 30: with a fuse panel selected, right-click a SENTRY TURRET to bind it (or unbind it). Turrets are
     * entities, so this is the tool's entity interaction rather than its block one.
     */
    @Override
    public @NotNull InteractionResult interactLivingEntity(
        @NotNull net.minecraft.world.item.ItemStack stack,
        @NotNull Player player,
        @NotNull net.minecraft.world.entity.LivingEntity entity,
        @NotNull net.minecraft.world.InteractionHand hand
    ) {
        var key = com.human.common.registry.init.HumanDataComponents.SELECTED_FUSE_PANEL.get();
        var selected = stack.get(key);

        if (!(entity instanceof com.human.common.gameplay.entity.machine.SentryTurret turret) || selected == null) {
            return InteractionResult.PASS;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        if (!selected.dimension().equals(level.dimension())) {
            player.displayClientMessage(Component.translatable("message.avp_human.fuse.other_dimension"), true);
            return InteractionResult.CONSUME;
        }

        var panelPos = selected.pos();

        if (
            !level.isLoaded(panelPos) || !(level.getBlockEntity(
                panelPos
            ) instanceof com.human.common.gameplay.power.fuse.FusePanelBlockEntity panel)
        ) {
            stack.remove(key);
            player.displayClientMessage(Component.translatable("message.avp_human.fuse.gone"), true);
            return InteractionResult.CONSUME;
        }

        var id = turret.getUUID();

        if (panelPos.equals(turret.getFusePanel())) {
            panel.unbindSentry(id);
            turret.setFusePanel(null);
            level.playSound(null, turret.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 0.6F);
            player.displayClientMessage(
                Component.translatable("message.avp_human.fuse.unbound", panel.getBoundCount(), panel.getDeviceCapacity()),
                true
            );
            return InteractionResult.CONSUME;
        }

        if (!panel.isInRange(turret.blockPosition())) {
            player.displayClientMessage(
                Component.translatable(
                    "message.avp_human.fuse.out_of_range",
                    com.human.common.gameplay.power.fuse.FusePanelBlockEntity.RANGE
                ),
                true
            );
            return InteractionResult.CONSUME;
        }

        if (panel.getBoundCount() >= panel.getDeviceCapacity()) {
            player.displayClientMessage(Component.translatable("message.avp_human.fuse.full", panel.getDeviceCapacity()), true);
            return InteractionResult.CONSUME;
        }

        // One panel per turret: binding here moves it off its old panel.
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
        player.displayClientMessage(
            Component.translatable("message.avp_human.fuse.bound", panel.getBoundCount(), panel.getDeviceCapacity()),
            true
        );

        return InteractionResult.CONSUME;
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
