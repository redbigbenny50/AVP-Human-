package com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy;

import com.human.common.gameplay.entity.ai.utility.item.ItemStrategy;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.ReadableWorldState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface WeaponStrategy extends ItemStrategy {

    double getRangeForWeapon(LivingEntity livingEntity, ItemStack itemStack);

    Action.Signal execute(Action.Context<? extends LivingEntity> context);

    ScoreResult computeScore(LivingEntity livingEntity, ReadableWorldState worldState, ItemStack itemStack);

    @Override
    default double score(LivingEntity livingEntity, ReadableWorldState worldState, ItemStack itemStack) {
        return computeScore(livingEntity, worldState, itemStack).weightedAverage();
    }

    record Weights(
        Positive positive,
        Negative negative
    ) {

        public static final Weights DEFAULT = new Weights(
            new Positive(30, 30),
            new Negative(40)
        );

        record Positive(
            int effectiveness,
            int range
        ) {}

        record Negative(
            int risk
        ) {}
    }

    class ScoreResult {

        private static final ThreadLocal<ScoreResult> THREAD_LOCAL = ThreadLocal.withInitial(ScoreResult::new);

        private Weights weights;

        private double effectivenessScore;

        private double rangeScore;

        private double riskScore;

        public static ScoreResult zero() {
            return of(Weights.DEFAULT, 0, 0, 0);
        }

        public static ScoreResult of(
            Weights weights,
            double effectivenessScore,
            double rangeScore,
            double riskScore
        ) {
            var scoreResult = THREAD_LOCAL.get();

            scoreResult.weights = weights;
            scoreResult.effectivenessScore = effectivenessScore;
            scoreResult.rangeScore = rangeScore;
            scoreResult.riskScore = riskScore;

            return scoreResult;
        }

        private ScoreResult() {
            this.weights = Weights.DEFAULT;
        }

        public double weightedAverage() {
            var positiveWeight = weights.positive().effectiveness()
                + weights.positive().range();
            var negativeWeight = weights.negative().risk();

            if (positiveWeight <= 0.0 || negativeWeight <= 0.0) {
                return 0.0;
            }

            var positiveScore = clampWeight(effectivenessScore, weights.positive().effectiveness())
                + clampWeight(rangeScore, weights.positive().range());

            positiveScore /= positiveWeight;

            var negativeScore = clampWeight(riskScore, weights.negative().risk());

            negativeScore /= negativeWeight;

            return Math.clamp(positiveScore - negativeScore, 0.0, 1.0);
        }

        private double clampWeight(double score, double weight) {
            return Math.clamp(score, 0, 1) * weight;
        }
    }
}
