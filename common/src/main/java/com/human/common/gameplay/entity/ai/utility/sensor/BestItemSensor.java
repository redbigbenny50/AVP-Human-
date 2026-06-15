package com.human.common.gameplay.entity.ai.utility.sensor;

import com.human.common.gameplay.entity.ai.utility.item.ItemStrategy;
import com.human.common.gameplay.entity.ai.utility.item.ItemStrategyResult;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.sensor.Sensor;
import com.just.ai.goap.state.ReadableWorldState;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BestItemSensor<S extends ItemStrategy, R extends ItemStrategyResult<? extends ItemTarget, S>> {

    public static <S extends ItemStrategy, R extends ItemStrategyResult<?, S>> BestItemSensor.Builder<S, R> builder() {
        return new BestItemSensor.Builder<S, R>();
    }

    private final List<Sensor.Mono<?, ? extends Option<? extends R>>> sensors;

    private BestItemSensor(
        List<Sensor.Mono<?, ? extends Option<? extends R>>> sensors
    ) {
        this.sensors = sensors;
    }

    public Option<R> sense(LivingEntity ignored, ReadableWorldState worldState) {
        Option<R> bestOption = Option.none();

        for (var sensors : sensors) {
            @SuppressWarnings("unchecked")
            var key = (StateKey<Option<? extends R>>) sensors.key();
            var sensorResultOption = (Option<? extends R>) worldState.getOrDefault(key, Option.none());

            bestOption = test(sensorResultOption, bestOption);
        }

        return bestOption;
    }

    private Option<R> test(Option<? extends R> itemTargetOption, Option<R> bestOption) {
        if (itemTargetOption.isSome()) {
            var candidate = itemTargetOption.unwrap();

            if (bestOption.isNone() || candidate.score() > bestOption.unwrap().score()) {
                bestOption = Option.some(candidate);
            }
        }

        return bestOption;
    }

    public static class Builder<S extends ItemStrategy, R extends ItemStrategyResult<? extends ItemTarget, S>> {

        private final List<Sensor.Mono<?, ? extends Option<? extends R>>> sensors;

        private Builder() {
            this.sensors = new ArrayList<>();
        }

        public Builder<S, R> withSensor(Sensor.Mono<?, ? extends Option<? extends R>> sensor) {
            sensors.add(sensor);
            return this;
        }

        public BestItemSensor<S, R> build() {
            return new BestItemSensor<>(Collections.unmodifiableList(sensors));
        }
    }
}
