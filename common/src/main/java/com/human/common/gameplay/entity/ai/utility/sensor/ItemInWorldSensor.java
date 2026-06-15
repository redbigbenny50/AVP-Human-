package com.human.common.gameplay.entity.ai.utility.sensor;

import com.human.common.gameplay.entity.ai.utility.item.ItemStrategy;
import com.human.common.gameplay.entity.ai.utility.item.ItemStrategyResult;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.state.ReadableWorldState;
import com.just.core.functional.function.Function3;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class ItemInWorldSensor<S extends ItemStrategy, R extends ItemStrategyResult<ItemTarget.World, S>> {

    public static <S extends ItemStrategy, R extends ItemStrategyResult<ItemTarget.World, S>> ItemInWorldSensor.Builder<S, R> builder(
        Supplier<List<S>> strategySupplier,
        Function3<ItemTarget.World, S, Double, R> resultFactory
    ) {
        return new ItemInWorldSensor.Builder<>(strategySupplier, resultFactory);
    }

    private final Supplier<List<S>> strategySupplier;

    private final Function3<ItemTarget.World, S, Double, R> resultFactory;

    private ItemInWorldSensor(
        Supplier<List<S>> strategySupplier,
        Function3<ItemTarget.World, S, Double, R> resultFactory
    ) {
        this.strategySupplier = strategySupplier;
        this.resultFactory = resultFactory;
    }

    public @NotNull Option<R> sense(
        Marine marine,
        ReadableWorldState worldState
    ) {
        var bestScore = -Double.MIN_VALUE;
        ItemEntity bestItemEntity = null;
        S bestStrategy = null;

        for (var strategy : strategySupplier.get()) {
            if (!strategy.isValidWorldState(marine, worldState)) {
                continue;
            }

            var itemEntities = marine.getEntitySenseCache().getByType(EntityType.ITEM);

            // TODO: Access by item -> item entities map first, then filter item entities as current impl does.
            for (var entry : itemEntities) {
                var itemStack = entry.getItem();

                if (!strategy.isValidItemStack(itemStack)) {
                    continue;
                }

                var newScore = strategy.score(marine, worldState, itemStack);

                if (newScore > bestScore) {
                    bestScore = newScore;
                    bestItemEntity = entry;
                    bestStrategy = strategy;
                }
            }
        }

        return bestItemEntity == null
            ? Option.none()
            : Option.some(resultFactory.apply(new ItemTarget.World(bestItemEntity), bestStrategy, bestScore));
    }

    public static class Builder<S extends ItemStrategy, R extends ItemStrategyResult<ItemTarget.World, S>> {

        private final Supplier<List<S>> strategySupplier;

        private final Function3<ItemTarget.World, S, Double, R> resultFactory;

        private Builder(Supplier<List<S>> strategySupplier, Function3<ItemTarget.World, S, Double, R> resultFactory) {
            this.strategySupplier = strategySupplier;
            this.resultFactory = resultFactory;
        }

        public ItemInWorldSensor<S, R> build() {
            return new ItemInWorldSensor<>(strategySupplier, resultFactory);
        }
    }
}
