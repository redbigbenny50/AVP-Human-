package com.human.common.gameplay.command;

import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.debug.BulletTrajectoryDebug;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

public class BulletTrajectoryDebugCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("bullets")
            .then(Commands.literal("toggle").executes(BulletTrajectoryDebugCommand::toggle))
            .then(Commands.literal("on").executes(context -> setEnabled(context, true)))
            .then(Commands.literal("off").executes(context -> setEnabled(context, false)))
            .then(Commands.literal("show").executes(BulletTrajectoryDebugCommand::showHeldGun))
            .then(Commands.literal("state").executes(BulletTrajectoryDebugCommand::showState))
            .then(Commands.literal("spread").executes(BulletTrajectoryDebugCommand::showSpread))
            .then(Commands.literal("recoil").executes(BulletTrajectoryDebugCommand::showRecoil));
    }

    private static int toggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var enabled = BulletTrajectoryDebug.toggle(player);

        sendState(context, enabled);
        return enabled ? 1 : 0;
    }

    private static int setEnabled(CommandContext<CommandSourceStack> context, boolean enabled) throws CommandSyntaxException {
        BulletTrajectoryDebug.setEnabled(context.getSource().getPlayerOrException(), enabled);
        sendState(context, enabled);
        return enabled ? 1 : 0;
    }

    private static int showHeldGun(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!(itemStack.getItem() instanceof GunItem gunItem)) {
            context.getSource().sendFailure(Component.literal("Hold a gun first."));
            return 0;
        }

        BulletTrajectoryDebug.sendHeldGunInfo(player, gunItem);
        return 1;
    }

    private static int showState(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var gunItem = getHeldGun(context, player);
        if (gunItem == null) {
            return 0;
        }

        BulletTrajectoryDebug.sendAccuracyState(player, gunItem);
        return 1;
    }

    private static int showSpread(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var gunItem = getHeldGun(context, player);
        if (gunItem == null) {
            return 0;
        }

        BulletTrajectoryDebug.sendSpreadInfo(player, gunItem);
        return 1;
    }

    private static int showRecoil(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var gunItem = getHeldGun(context, player);
        if (gunItem == null) {
            return 0;
        }

        BulletTrajectoryDebug.sendRecoilInfo(player, gunItem);
        return 1;
    }

    private static GunItem getHeldGun(CommandContext<CommandSourceStack> context, net.minecraft.server.level.ServerPlayer player) {
        var itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (itemStack.getItem() instanceof GunItem gunItem) {
            return gunItem;
        }

        context.getSource().sendFailure(Component.literal("Hold a gun first."));
        return null;
    }

    private static void sendState(CommandContext<CommandSourceStack> context, boolean enabled) {
        context.getSource()
            .sendSuccess(
                () -> Component.literal("Bullet trajectory debug " + (enabled ? "enabled." : "disabled.")),
                false
            );
    }

    private BulletTrajectoryDebugCommand() {
        throw new UnsupportedOperationException();
    }
}
