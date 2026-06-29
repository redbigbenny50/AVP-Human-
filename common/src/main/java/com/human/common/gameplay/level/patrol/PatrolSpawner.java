package com.human.common.gameplay.level.patrol;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PatrolSpawner {

    public static Builder builder() {
        return new Builder();
    }

    private final List<Condition> conditions;

    private final PositionSelector positionSelector;

    private final Spawner spawner;

    private PatrolSpawner(
        List<Condition> conditions,
        PositionSelector positionSelector,
        Spawner spawner
    ) {
        this.conditions = Collections.unmodifiableList(conditions);
        this.positionSelector = positionSelector;
        this.spawner = spawner;
    }

    public boolean canSpawn(Level level) {
        for (var condition : conditions) {
            if (!condition.test(level)) {
                return false;
            }
        }

        return true;
    }

    public boolean spawnFor(Player player) {
        var spawnPosition = positionSelector.select(player);

        if (spawnPosition == null) {
            return false;
        }

        spawner.spawn(player.level(), player, spawnPosition.mutable());

        return true;
    }

    public void forceSpawnFor(Player player) {
        var spawnPosition = PositionSelector.NEAR_PLAYER.select(player);

        if (spawnPosition == null) {
            spawnPosition = player.blockPosition();
        }

        spawner.spawn(player.level(), player, spawnPosition.mutable());
    }

    public static class Builder {

        private final List<Condition> conditions;

        private PositionSelector positionSelector;

        private Builder() {
            this.conditions = new ArrayList<>();
            this.positionSelector = PositionSelector.NEAR_PLAYER;
        }

        public Builder withCondition(Condition condition) {
            conditions.add(condition);
            return this;
        }

        public Builder withSpawnPositionSelector(PositionSelector positionSelector) {
            this.positionSelector = positionSelector;
            return this;
        }

        public PatrolSpawner build(Spawner spawner) {
            return new PatrolSpawner(Collections.unmodifiableList(conditions), positionSelector, spawner);
        }
    }

    public interface Condition {

        boolean test(Level level);
    }

    public interface PositionSelector {

        PositionSelector NEAR_PLAYER = player -> {
            var spawnPosition = getRandomNearbyPosition(player, player.level().random);

            return isValidSpawnLocation(spawnPosition, player.level())
                ? spawnPosition
                : null;
        };

        @Nullable
        BlockPos select(Player player);

        private static BlockPos.MutableBlockPos getRandomNearbyPosition(Player player, RandomSource randomSource) {
            var xOffset = (24 + randomSource.nextInt(24)) * (randomSource.nextBoolean() ? -1 : 1);
            var zOffset = (24 + randomSource.nextInt(24)) * (randomSource.nextBoolean() ? -1 : 1);

            return player.blockPosition().mutable().move(xOffset, 0, zOffset);
        }

        @SuppressWarnings("deprecation")
        private static boolean isValidSpawnLocation(BlockPos pos, Level level) {
            return level.hasChunksAt(pos.getX() - 10, pos.getZ() - 10, pos.getX() + 10, pos.getZ() + 10);
        }
    }

    public interface Spawner {

        void spawn(Level level, Player player, BlockPos.MutableBlockPos mutableBlockPos);
    }
}
