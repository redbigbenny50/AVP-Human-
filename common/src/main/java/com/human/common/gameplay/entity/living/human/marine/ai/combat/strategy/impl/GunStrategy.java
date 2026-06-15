package com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.impl;

import com.human.common.gameplay.entity.living.human.ai.AttributeUtil;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.WeaponStrategy;
import com.human.common.gameplay.item.GunItem;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.tag.HumanItemTags;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import com.just.ai.goap.state.ReadableWorldState;
import com.just.core.functional.option.Option;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class GunStrategy implements WeaponStrategy {

    private static final StateKey<Integer> TICK_COUNTDOWN = StateKey.sensed("tick_countdown");

    @Override
    public boolean isValidItemStack(ItemStack itemStack) {
        if (!itemStack.is(HumanItemTags.GUNS)) {
            return false;
        }

        return itemStack.getOrDefault(HumanDataComponents.AMMUNITION.get(), 0) > 0;
    }

    @Override
    public boolean isValidWorldState(LivingEntity livingEntity, ReadableWorldState worldState) {
        return true;
    }

    @Override
    public ScoreResult computeScore(LivingEntity livingEntity, ReadableWorldState worldState, ItemStack itemStack) {
        if (!(livingEntity instanceof Mob mob)) {
            return ScoreResult.zero();
        }

        var target = mob.getTarget();

        if (target == null) {
            return ScoreResult.zero();
        }

        var effectivenessScore = computeEffectivenessScore(mob, target, worldState, itemStack);
        var rangeScore = computeRangeScore(mob, target, worldState, itemStack);
        var riskScore = computeRiskScore(mob, target, worldState, itemStack);

        return ScoreResult.of(Weights.DEFAULT, effectivenessScore, rangeScore, riskScore);
    }

    @Override
    public double getRangeForWeapon(LivingEntity livingEntity, ItemStack itemStack) {
        if (itemStack.getItem() instanceof GunItem gunItem) {
            return gunItem.getGunConfig().getDefaultFireMode().range();
        }

        var attributes = livingEntity.getAttributes();
        var attribute = Attributes.ENTITY_INTERACTION_RANGE;

        return attributes.hasAttribute(attribute)
            ? attributes.getBaseValue(attribute)
            : attribute.value().getDefaultValue();
    }

    @Override
    public Action.Signal execute(Action.Context<? extends LivingEntity> context) {
        var livingEntity = context.getActor();
        var worldState = context.getWorldState();
        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);

        if (!(livingEntity instanceof Mob mob)) {
            return Action.Signal.ABORT;
        }

        var target = mob.getTarget();

        if (target == null) {
            return Action.Signal.ABORT;
        }

        var currentTickCountdown = blackboard.getOrNull(TICK_COUNTDOWN);

        if (currentTickCountdown == null) {
            currentTickCountdown = Integer.MAX_VALUE;
            blackboard.set(TICK_COUNTDOWN, currentTickCountdown);
        } else {
            currentTickCountdown -= 1;
            blackboard.set(TICK_COUNTDOWN, currentTickCountdown);
        }

        var tickCountdown = currentTickCountdown;
        var equippedWeaponOption = worldState.getOrDefault(CombatSensors.BEST_WEAPON_IN_HANDS.key(), Option.none());

        equippedWeaponOption.ifSome(equippedWeapon -> {
            var equipmentSlot = equippedWeapon.itemTarget().equipmentSlot();
            var itemStack = mob.getItemBySlot(equipmentSlot);

            if (itemStack.getItem() instanceof GunItem gunItem) {
                // Always look at the target while shooting.
                mob.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                mob.getLookControl().setLookAt(target);

                gunItem.onUseTick(mob.level(), mob, itemStack, tickCountdown);
            }
        });

        return Action.Signal.CONTINUE;
    }

    private double computeEffectivenessScore(Mob mob, LivingEntity target, ReadableWorldState worldState, ItemStack itemStack) {
        if (itemStack.getItem() instanceof GunItem gunItem) {
            var fireMode = gunItem.getGunConfig().getDefaultFireMode();
            var attackDamage = fireMode.damage();
            var attackSpeed = 20.0 / Math.max(fireMode.cooldownInTicks(), 0.001);
            var damagePerSecond = attackDamage * attackSpeed;
            var targetHealth = target.getHealth();
            var timeToKillInSeconds = targetHealth / Math.max(damagePerSecond, 0.001);

            // TODO: This is a very rough approximation and doesn't account for the target's held weapons.
            var incomingDamagePerSecond = AttributeUtil.getAttributeBaseOrDefaultValue(target, Attributes.ATTACK_DAMAGE)
                * AttributeUtil.getAttributeBaseOrDefaultValue(target, Attributes.ATTACK_SPEED);
            // How long until the target can kill us?
            var timeToFailureInSeconds = mob.getHealth() / Math.max(incomingDamagePerSecond, 0.001);
            // If less than 1, the target killing us is faster than us killing the target.
            // If greater than 1, the target killing us is slower than us killing the target.
            var ratio = timeToFailureInSeconds / Math.max(timeToKillInSeconds, 0.001);
            // Ex. 99 / 100 = 0.99... Larger ratios approach 1.
            // 0.01 / 1.01 = 0.0099... Smaller ratios approach 0.
            var effectiveness = ratio / (ratio + 1.0);

            return Math.clamp(effectiveness, 0.0, 1.0);
        }

        return 0;
    }

    private double computeRangeScore(Mob mob, LivingEntity target, ReadableWorldState worldState, ItemStack itemStack) {
        var reach = getRangeForWeapon(mob, itemStack);
        var distance = mob.distanceTo(target);

        double rangeScore;

        if (distance <= reach) {
            rangeScore = 1.0;
        } else {
            var excess = distance - reach;
            rangeScore = Math.exp(-excess * 2.5);
        }

        return rangeScore;
    }

    private double computeRiskScore(Mob mob, LivingEntity target, ReadableWorldState worldState, ItemStack itemStack) {
        var targetAttackDamage = AttributeUtil.getAttributeBaseOrDefaultValue(target, Attributes.ATTACK_DAMAGE);
        return Math.clamp(targetAttackDamage / mob.getHealth(), 0, 1);
    }
}
