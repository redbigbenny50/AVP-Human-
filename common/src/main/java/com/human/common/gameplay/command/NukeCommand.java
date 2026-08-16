package com.human.common.gameplay.command;

import com.blib.api.common.server.v1.ServerScheduler;
import com.human.util.NuclearExplosionUtil;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.time.Duration;

/**
 * {@code /nuke} — detonates a warhead at the caller's feet.
 * <p>
 * This used to build its own explosion, a near-copy of {@link NuclearExplosionUtil#createNuclearExplosion} that had
 * drifted: it carved the same crater and spread the same fallout, but it never applied the fallout dose, never
 * converted caught xenomorphs to the irradiated strain, never killed aberrants outright, never spawned the mushroom
 * cloud, and its crater did not survive a world unload. Anything tested with the command was therefore not the thing
 * players actually meet.
 * <p>
 * It now delegates, so the command and the placed warhead are the same detonation by construction and cannot drift
 * apart again.
 */
public class NukeCommand {

    private static final String COMMAND_NAME = "nuke";

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(COMMAND_NAME)
            .executes(context -> {
                var level = context.getSource().getLevel();
                var center = context.getSource().getPosition();

                // The one-second delay is kept: it is what gives the caller a moment to get clear, and it matches the
                // primed warhead's own fuse hand-off.
                ServerScheduler.schedule(
                    () -> NuclearExplosionUtil.createNuclearExplosion(
                        level,
                        center,
                        NuclearExplosionUtil.RADIUS,
                        NuclearExplosionUtil.MAX_KNOCKBACK
                    ).explode(),
                    Duration.ofSeconds(1)
                );

                return 1;
            });
    }
}
