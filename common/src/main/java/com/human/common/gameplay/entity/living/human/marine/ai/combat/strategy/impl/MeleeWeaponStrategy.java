package com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.impl;

import com.blib.api.common.enchantment.v1.EnchantmentUtil;
import com.human.common.gameplay.entity.living.human.ai.AttributeUtil;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatSensors;
import com.human.common.gameplay.entity.living.human.marine.ai.combat.strategy.WeaponStrategy;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import com.just.ai.goap.state.ReadableWorldState;
import com.just.core.functional.option.Option;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public class MeleeWeaponStrategy implements WeaponStrategy {

    private static final StateKey<Integer> ATTACK_DELAY_IN_TICKS = StateKey.sensed("attack_delay_in_ticks");

    @Override
    public boolean isValidItemStack(ItemStack itemStack) {
        // TODO: Replace with "melee weapons" tag.
        return itemStack.is(ItemTags.AXES)
            || itemStack.is(ItemTags.SWORDS)
            || itemStack.is(Items.MACE);
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
        return AttributeUtil.getAttributeBaseOrDefaultValue(livingEntity, Attributes.ENTITY_INTERACTION_RANGE);
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

        var currentAttackDelayInTicks = blackboard.getOrDefault(ATTACK_DELAY_IN_TICKS, 0);

        if (currentAttackDelayInTicks > 0) {
            blackboard.set(ATTACK_DELAY_IN_TICKS, currentAttackDelayInTicks - 1);
            return Action.Signal.CONTINUE;
        }

        var equippedWeaponOption = worldState.getOrDefault(CombatSensors.BEST_WEAPON_IN_HANDS.key(), Option.none());

        equippedWeaponOption.ifSome(equippedWeapon -> {
            var equipmentSlot = equippedWeapon.itemTarget().equipmentSlot();
            var itemStack = livingEntity.getItemBySlot(equipmentSlot);

            // Always look at the target while attacking.
            mob.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
            mob.getLookControl().setLookAt(target);
            // Hurt the target.
            mob.doHurtTarget(target);

            var modifiedAttackSpeed = AttributeUtil.computeModifiedAttributeValue(mob, Attributes.ATTACK_SPEED, itemStack, equipmentSlot);

            blackboard.set(ATTACK_DELAY_IN_TICKS, Math.abs((int) (modifiedAttackSpeed * 20)));
        });

        return Action.Signal.CONTINUE;
    }

    private double computeEffectivenessScore(Mob mob, LivingEntity target, ReadableWorldState worldState, ItemStack itemStack) {
        var sharpnessLevel = EnchantmentUtil.getLevel(mob.level(), itemStack, Enchantments.SHARPNESS);
        var sharpnessBonus = sharpnessLevel > 0
            ? 1.0 + 0.5 * sharpnessLevel
            : 0.0;
        var attackDamage = AttributeUtil.computeModifiedAttributeValue(mob, Attributes.ATTACK_DAMAGE, itemStack, null)
            + sharpnessBonus;
        var attackSpeed = AttributeUtil.computeModifiedAttributeValue(mob, Attributes.ATTACK_SPEED, itemStack, null);
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
        if (target instanceof Creeper creeper && creeper.getSwellDir() > 0) {
            return 1;
        }

        var targetAttackDamage = AttributeUtil.getAttributeBaseOrDefaultValue(target, Attributes.ATTACK_DAMAGE);

        return Math.clamp(targetAttackDamage / mob.getHealth(), 0, 1);
    }
}
