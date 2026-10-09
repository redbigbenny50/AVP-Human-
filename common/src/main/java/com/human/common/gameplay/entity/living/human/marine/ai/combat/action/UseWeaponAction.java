package com.human.common.gameplay.entity.living.human.marine.ai.combat.action;

import com.human.common.gameplay.entity.living.human.marine.ai.combat.CombatSensors;
import com.just.ai.goap.action.Action;
import com.just.core.functional.option.Option;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class UseWeaponAction {

    public static Action.Signal perform(Action.Context<? extends LivingEntity> context) {
        var worldState = context.getWorldState();
        var itemTargetOption = worldState.getOrDefault(CombatSensors.BEST_WEAPON.key(), Option.none());

        if (itemTargetOption.isNone()) {
            setAggressive(context, false);

            return Action.Signal.ABORT;
        }

        setAggressive(context, true);

        // [stated] Oct 4: stamp it - the marine drops the aim pose itself once this stops running, because the GOAP
        // library never calls onFinish for a plan it replaces or abandons.
        if (context.getActor() instanceof com.human.common.gameplay.entity.living.human.marine.Marine marine) {
            marine.markWeaponUse();
        }

        return itemTargetOption.unwrap().strategy().execute(context);
    }

    public static void onFinish(Action.Context<? extends LivingEntity> context) {
        setAggressive(context, false);
    }

    /**
     * Drives the vanilla "aggressive" flag, which is what the renderer reads to decide whether to raise the arms.
     * <p>
     * ⚠ Nothing was setting it. Vanilla raises that flag from inside {@code MeleeAttackGoal} and {@code
     * RangedAttackGoal}, and these marines run on GOAP instead - so it sat false forever, the aim pose in
     * {@code MarineAnimator} was gated behind a condition that could never be true, and the arms stayed at the marine's
     * sides through every firefight. Using the weapon action to set it means the flag is true exactly while a weapon is
     * being used, which is exactly when the arms should be up.
     * <p>
     * The flag lives in synched entity data, so it reaches the client on its own. It is only written when it actually
     * changes, to keep a firing marine from broadcasting a redundant update every tick.
     */
    private static void setAggressive(Action.Context<? extends LivingEntity> context, boolean aggressive) {
        if (context.getActor() instanceof Mob mob && mob.isAggressive() != aggressive) {
            mob.setAggressive(aggressive);
        }
    }

    private UseWeaponAction() {
        throw new UnsupportedOperationException();
    }
}
