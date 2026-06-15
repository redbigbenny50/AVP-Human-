package com.human.common.gameplay.entity.ai.utility.sensor;

import com.human.common.gameplay.entity.ai.utility.item.ItemStrategy;
import com.human.common.gameplay.entity.ai.utility.item.ItemStrategyResult;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.just.ai.goap.state.ReadableWorldState;
import com.just.core.functional.function.Function3;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.function.Supplier;

public class EquippedItemSensor<S extends ItemStrategy, R extends ItemStrategyResult<ItemTarget.Equipped, S>> {

    public static <S extends ItemStrategy, R extends ItemStrategyResult<ItemTarget.Equipped, S>> Builder<S, R> builder(
        Supplier<List<S>> strategySupplier,
        Function3<ItemTarget.Equipped, S, Double, R> resultFactory
    ) {
        return new Builder<>(strategySupplier, resultFactory);
    }

    private final Supplier<List<S>> strategySupplier;

    private final Function3<ItemTarget.Equipped, S, Double, R> resultFactory;

    private final EquipmentSlot[] equipmentSlots;

    private EquippedItemSensor(
        Supplier<List<S>> strategySupplier,
        Function3<ItemTarget.Equipped, S, Double, R> resultFactory,
        EquipmentSlot[] equipmentSlots
    ) {
        this.strategySupplier = strategySupplier;
        this.resultFactory = resultFactory;
        this.equipmentSlots = equipmentSlots;
    }

    public Option<R> sense(
        LivingEntity livingEntity,
        ReadableWorldState worldState
    ) {
        var bestScore = -Double.MIN_VALUE;
        EquipmentSlot bestEquipmentSlot = null;
        S bestStrategy = null;

        for (var equipmentSlot : equipmentSlots) {
            var itemStack = livingEntity.getItemBySlot(equipmentSlot);

            for (var strategy : strategySupplier.get()) {
                if (!strategy.isValidItemStack(itemStack) || !strategy.isValidWorldState(livingEntity, worldState)) {
                    continue;
                }

                var newScore = strategy.score(livingEntity, worldState, itemStack);

                if (newScore > bestScore) {
                    bestScore = newScore;
                    bestEquipmentSlot = equipmentSlot;
                    bestStrategy = strategy;
                }
            }
        }

        return bestEquipmentSlot == null
            ? Option.none()
            : Option.some(resultFactory.apply(new ItemTarget.Equipped(bestEquipmentSlot), bestStrategy, bestScore));
    }

    public static class Builder<S extends ItemStrategy, R extends ItemStrategyResult<ItemTarget.Equipped, S>> {

        private final Supplier<List<S>> strategySupplier;

        private final Function3<ItemTarget.Equipped, S, Double, R> resultFactory;

        private EquipmentSlot[] equipmentSlots;

        private Builder(Supplier<List<S>> strategySupplier, Function3<ItemTarget.Equipped, S, Double, R> resultFactory) {
            this.strategySupplier = strategySupplier;
            this.resultFactory = resultFactory;
            this.equipmentSlots = new EquipmentSlot[] {};
        }

        public Builder<S, R> withEquipmentSlots(EquipmentSlot[] equipmentSlots) {
            this.equipmentSlots = equipmentSlots;
            return this;
        }

        public EquippedItemSensor<S, R> build() {
            return new EquippedItemSensor<>(strategySupplier, resultFactory, equipmentSlots);
        }
    }
}
