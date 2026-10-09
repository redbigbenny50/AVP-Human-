package com.human.common.gameplay.command;

import com.human.common.gameplay.power.diag.PowerDiagnostics;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /avp_human debug powerdiag on|off|dump} - an opt-in log of what the grid actually moved.
 * <p>
 * ⚠ A COMMAND RATHER THAN A GAMERULE, and deliberately not persistent. [stated] Sep 26: "a command is fine dump is fine
 * too doesnt need to survive a restart." A gamerule would sync to every client and survive restarts for no benefit;
 * this is a testing switch that should be off by default every time the server comes up.
 * </p>
 */
public class PowerDiagCommand {

    private static final String COMMAND_NAME = "powerdiag";

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(COMMAND_NAME)
            .then(Commands.literal("on").executes(context -> set(context.getSource(), true)))
            .then(Commands.literal("off").executes(context -> set(context.getSource(), false)))
            .then(Commands.literal("dump").executes(context -> dump(context.getSource())))
            .executes(context -> status(context.getSource()));
    }

    private static int set(CommandSourceStack source, boolean value) {
        try {
            var file = PowerDiagnostics.setEnabled(value, logsRoot(source));

            if (value) {
                source.sendSuccess(
                    () -> Component.literal("Power diagnostics ON - logging to " + file),
                    true
                );
            } else {
                source.sendSuccess(() -> Component.literal("Power diagnostics OFF"), true);
            }

            return 1;
        } catch (RuntimeException e) {
            source.sendFailure(Component.literal("Could not open the power diagnostics log: " + e.getMessage()));

            return 0;
        }
    }

    /**
     * Writes one immediate snapshot, whether or not logging is running.
     * <p>
     * ⚠ Turns logging on for the note if it was off, then puts it back - so a dump never silently leaves the log
     * running and never requires the player to have enabled it first.
     * </p>
     */
    private static int dump(CommandSourceStack source) {
        var wasEnabled = PowerDiagnostics.isEnabled();

        if (!wasEnabled) {
            PowerDiagnostics.setEnabled(true, logsRoot(source));
        }

        PowerDiagnostics.logNote(
            "manual dump requested by " + source.getTextName() + " - next tick of every loaded"
                + " grid follows"
        );

        var file = PowerDiagnostics.currentFile();

        source.sendSuccess(() -> Component.literal("Snapshot marked in " + file), true);

        return 1;
    }

    private static int status(CommandSourceStack source) {
        var on = PowerDiagnostics.isEnabled();

        source.sendSuccess(
            () -> Component.literal(
                on ? "Power diagnostics ON - " + PowerDiagnostics.currentFile() : "Power diagnostics OFF"
            ),
            false
        );

        return 1;
    }

    /** The server's own run directory, so the folder sits beside logs/ where a player can find it. */
    private static java.nio.file.Path logsRoot(CommandSourceStack source) {
        return source.getServer().getServerDirectory().resolve("logs");
    }
}
