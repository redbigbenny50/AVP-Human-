package com.human.common.registry.init;

import com.blib.api.common.registry.v1.impl.BLibCommandRegistry;
import com.human.Human;
import com.human.common.gameplay.command.BulletTrajectoryDebugCommand;
import com.human.common.gameplay.command.NukeCommand;
import com.human.common.gameplay.command.SpawnPatrolCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class HumanCommands {

    private static final BLibCommandRegistry REGISTRY = Human.MOD.registries().createCommandRegistry();

    public static void initialize() {
        REGISTRY.register(SpawnPatrolCommand.create());
        REGISTRY.register(
            LiteralArgumentBuilder.<CommandSourceStack>literal(Human.MOD.id())
                .then(SpawnPatrolCommand.create())
                .then(
                    LiteralArgumentBuilder.<CommandSourceStack>literal("test")
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(NukeCommand.create())
                )
                .then(
                    LiteralArgumentBuilder.<CommandSourceStack>literal("debug")
                        .requires(commandSourceStack -> commandSourceStack.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(BulletTrajectoryDebugCommand.create())
                )
        );
    }
}
