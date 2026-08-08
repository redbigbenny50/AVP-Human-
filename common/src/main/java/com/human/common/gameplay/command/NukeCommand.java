package com.human.common.gameplay.command;

import com.blib.api.common.explosion.v1.Explosion;
import com.blib.api.common.explosion.v1.ExplosionProgressTracker;
import com.blib.api.common.explosion.v1.ExplosionUtil;
import com.blib.api.common.server.v1.ServerScheduler;
import com.human.Human;
import com.human.common.gameplay.explosion.nuke.NuclearExplosionEffects;
import com.human.common.gameplay.explosion.nuke.NuclearFalloutSpreader;
import com.human.util.NuclearExplosionUtil;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.time.Duration;

public class NukeCommand {

    private static final String COMMAND_NAME = "nuke";

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(COMMAND_NAME)
            .executes(context -> {
                ServerScheduler.schedule(() -> {
                    var explosion = createNuclearExplosion(context);
                    explosion.explode();
                }, Duration.ofSeconds(1));

                return 1;
            });
    }

    private static Explosion createNuclearExplosion(CommandContext<CommandSourceStack> context) {
        var level = context.getSource().getLevel();
        var center = context.getSource().getPosition();
        var progressTracker = new ExplosionProgressTracker();
        var nuclearExplosionEffects = new NuclearExplosionEffects();
        var radius = NuclearExplosionUtil.RADIUS;
        var maxKnockback = NuclearExplosionUtil.MAX_KNOCKBACK;

        return Explosion.builder(level, center)
            .withRadius(Direction.Plane.HORIZONTAL, radius)
            .withRadius(Direction.UP, radius / 2)
            .withRadius(Direction.DOWN, NuclearExplosionUtil.RADIUS_DOWN)
            .onExplosionStart(() -> {
                NuclearFalloutSpreader.spread(level, BlockPos.containing(center), NuclearExplosionUtil.FALLOUT_RADIUS_IN_CHUNKS);
                progressTracker.startTimer();

                var entities = ExplosionUtil.getEntitiesInRadius(level, center, radius);

                for (var entity : entities) {
                    var distance = entity.distanceToSqr(center);
                    var damage = ExplosionUtil.computeDamage(radius, 5, 1000, distance);

                    entity.igniteForSeconds(15);
                    entity.hurt(level.damageSources().explosion(null), (float) damage);
                    ExplosionUtil.applyKnockback(center, radius, entity, maxKnockback, distance);
                }
            })
            .onBlockSample(($, pos) -> {
                nuclearExplosionEffects.apply($, pos);
                progressTracker.incrementBlockDestroyCounter();
            })
            .onCycleStart(() -> nuclearExplosionEffects.beginCycle(level))
            .onCycleFinish(sampledPositions -> nuclearExplosionEffects.flushParticles(level))
            .onExplosionFinish(() -> {
                progressTracker.stopTimer();

                var timeTakenInMillis = progressTracker.timeTaken();
                var timeTakenInTicks = timeTakenInMillis / 50;

                Human.LOGGER.info(
                    "Explosion @ {} completed in {}ms ({} ticks), destroying {} blocks!",
                    center,
                    timeTakenInMillis,
                    timeTakenInTicks,
                    progressTracker.blocksDestroyed()
                );
            })
            .build();
    }
}
