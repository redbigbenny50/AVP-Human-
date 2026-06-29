package com.human.common.gameplay.command;

import com.human.common.gameplay.level.patrol.PatrolSpawner;
import com.human.common.gameplay.level.patrol.impl.ApePatrolSpawnHandle;
import com.human.common.gameplay.level.patrol.impl.MarinePatrolSpawnHandle;
import com.human.common.gameplay.level.patrol.impl.TacticalMarinePatrolSpawnHandle;
import com.human.common.gameplay.level.patrol.impl.WYCPatrolSpawnHandle;
import com.human.common.gameplay.level.patrol.impl.WYEPatrolSpawnHandle;
import com.human.common.gameplay.level.patrol.impl.WYSOCPatrolSpawnHandle;
import com.human.common.gameplay.level.patrol.impl.WYSOEPatrolSpawnHandle;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class SpawnPatrolCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("patrol")
            .then(
                Commands.literal("spawn")
                    .then(
                        Commands.literal("ape")
                            .executes(context -> spawnPatrol(context, "ape", ApePatrolSpawnHandle.INSTANCE.getSpawner()))
                    )
                    .then(
                        Commands.literal("marine")
                            .executes(context -> spawnPatrol(context, "marine", MarinePatrolSpawnHandle.INSTANCE.getSpawner()))
                    )
                    .then(
                        Commands.literal("tactical")
                            .executes(context -> spawnPatrol(context, "tactical", TacticalMarinePatrolSpawnHandle.INSTANCE.getSpawner()))
                    )
                    .then(
                        Commands.literal("wyc")
                            .executes(context -> spawnPatrol(context, "wyc", WYCPatrolSpawnHandle.INSTANCE.getSpawner()))
                    )
                    .then(
                        Commands.literal("wye")
                            .executes(context -> spawnPatrol(context, "wye", WYEPatrolSpawnHandle.INSTANCE.getSpawner()))
                    )
                    .then(
                        Commands.literal("wysoc")
                            .executes(context -> spawnPatrol(context, "wysoc", WYSOCPatrolSpawnHandle.INSTANCE.getSpawner()))
                    )
                    .then(
                        Commands.literal("wysoe")
                            .executes(context -> spawnPatrol(context, "wysoe", WYSOEPatrolSpawnHandle.INSTANCE.getSpawner()))
                    )
            );
    }

    private static int spawnPatrol(
        CommandContext<CommandSourceStack> context,
        String patrolName,
        PatrolSpawner patrolSpawner
    ) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        patrolSpawner.forceSpawnFor(player);
        context.getSource().sendSuccess(() -> Component.literal("Spawned " + patrolName + " patrol."), true);
        return 1;
    }
}
