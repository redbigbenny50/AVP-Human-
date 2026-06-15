package com.human.common.gameplay.entity.ai.utility.sensor;

import com.blib.api.common.inventory.v1.BLibInventory;
import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.human.common.gameplay.entity.ai.utility.item.ItemStrategy;
import com.human.common.gameplay.entity.ai.utility.item.ItemStrategyResult;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.just.ai.goap.state.ReadableWorldState;
import com.just.core.functional.function.Function3;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class ItemInInventorySensor<S extends ItemStrategy, R extends ItemStrategyResult<ItemTarget.Inventory, S>> {

    public static <S extends ItemStrategy, R extends ItemStrategyResult<ItemTarget.Inventory, S>> ItemInInventorySensor.Builder<S, R> builder(
        Supplier<List<S>> strategySupplier,
        Function3<ItemTarget.Inventory, S, Double, R> resultFactory
    ) {
        return new ItemInInventorySensor.Builder<>(strategySupplier, resultFactory);
    }

    private final Supplier<List<S>> strategySupplier;

    private final Function3<ItemTarget.Inventory, S, Double, R> resultFactory;

    private ItemInInventorySensor(
        Supplier<List<S>> strategySupplier,
        Function3<ItemTarget.Inventory, S, Double, R> resultFactory
    ) {
        this.strategySupplier = strategySupplier;
        this.resultFactory = resultFactory;
    }

    public <T extends LivingEntity & BLibInventoryHolder> @NotNull Option<R> sense(
        T livingEntityWithInventory,
        ReadableWorldState worldState
    ) {
        var bestScore = -Double.MIN_VALUE;
        BLibInventory.Entry bestEntry = null;
        S bestStrategy = null;

        for (var strategy : strategySupplier.get()) {
            if (!strategy.isValidWorldState(livingEntityWithInventory, worldState)) {
                continue;
            }

            var entries = strategy.selectEntriesFromInventory(livingEntityWithInventory.getInventory());

            for (var entry : entries) {
                var itemStack = entry.copyItemStack();

                if (!strategy.isValidItemStack(itemStack)) {
                    continue;
                }

                var newScore = strategy.score(livingEntityWithInventory, worldState, itemStack);

                if (newScore > bestScore) {
                    bestScore = newScore;
                    bestEntry = entry;
                    bestStrategy = strategy;
                }
            }
        }

        return bestEntry == null
            ? Option.none()
            : Option.some(resultFactory.apply(new ItemTarget.Inventory(bestEntry), bestStrategy, bestScore));
    }

    public static class Builder<S extends ItemStrategy, R extends ItemStrategyResult<ItemTarget.Inventory, S>> {

        private final Supplier<List<S>> strategySupplier;

        private final Function3<ItemTarget.Inventory, S, Double, R> resultFactory;

        private Builder(Supplier<List<S>> strategySupplier, Function3<ItemTarget.Inventory, S, Double, R> resultFactory) {
            this.strategySupplier = strategySupplier;
            this.resultFactory = resultFactory;
        }

        public ItemInInventorySensor<S, R> build() {
            return new ItemInInventorySensor<>(strategySupplier, resultFactory);
        }
    }
}
