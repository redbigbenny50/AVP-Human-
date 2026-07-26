package com.human.common.gameplay.command;

import com.human.util.NuclearExplosionUtil;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class NukeCommand {

    private static final String COMMAND_NAME = "nuke";

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(COMMAND_NAME)
            .executes(context -> {
                NuclearExplosionUtil.detonateNuke(
                    context.getSource().getLevel(),
                    context.getSource().getPosition(),
                    context.getSource().getEntity()
                );

                return 1;
            });
    }
}
