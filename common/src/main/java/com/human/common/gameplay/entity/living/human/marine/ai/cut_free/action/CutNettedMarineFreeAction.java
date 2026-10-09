package com.human.common.gameplay.entity.living.human.marine.ai.cut_free.action;

import com.blib.api.common.goap.v1.action.impl.UnequipItemAction;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.entity.living.human.marine.ai.cut_free.CutFreeSensors;
import com.human.compatibility.avp_predator.HumanPredatorNet;
import com.just.ai.goap.StateKey;
import com.just.ai.goap.action.Action;
import com.just.ai.goap.state.Blackboard;
import com.just.core.functional.option.Option;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Sawing a squadmate out of the mesh. A few visible knife strokes over a couple of seconds, then the same release the
 * player's sword makes - one code path, so whatever the net does on release it does here too.
 */
public class CutNettedMarineFreeAction {

    /**
     * How long the cut takes. Long enough to read as work, short enough that a hunter standing over them still wins.
     */
    public static final int CUT_DURATION_TICKS = 40;

    /** One stroke of the knife every half second. */
    public static final int STROKE_INTERVAL_TICKS = 10;

    private static final EquipmentSlot HAND_TO_USE = EquipmentSlot.MAINHAND;

    /** ⚠ ACTION scope: it must reset every time the action starts, and it does not need to survive a replan. */
    private static final StateKey<Integer> CUT_TICKS = StateKey.sensed("cut_free_ticks");

    public static Action.Signal perform(Action.Context<? extends Marine> context) {
        var marine = context.getActor();
        var worldState = context.getWorldState();
        var nettedOption = worldState.getOrDefault(CutFreeSensors.NEAREST_NETTED_MARINE.key(), Option.none());

        if (nettedOption.isNone()) {
            return Action.Signal.ABORT;
        }

        var netted = nettedOption.unwrap();

        if (!netted.isAlive() || !HumanPredatorNet.isNetted(netted)) {
            // Freed by someone else, or dead. Nothing left to cut.
            return Action.Signal.ABORT;
        }

        if (marine.distanceToSqr(netted) >= CutFreeSensors.CUT_RANGE_SQR) {
            // They were dragged off, or we were pushed. Replan and walk back over.
            return Action.Signal.ABORT;
        }

        var blade = marine.getItemBySlot(HAND_TO_USE);

        if (!CutFreeSensors.isBlade(blade)) {
            return Action.Signal.ABORT;
        }

        marine.getLookControl().setLookAt(netted, 30.0F, 30.0F);

        var blackboard = context.getBlackboard(Blackboard.Scope.ACTION);
        var ticks = blackboard.getOrDefault(CUT_TICKS, 0);

        if (ticks % STROKE_INTERVAL_TICKS == 0) {
            marine.swing(InteractionHand.MAIN_HAND, true);
        }

        ticks++;
        blackboard.set(CUT_TICKS, ticks);

        if (ticks < CUT_DURATION_TICKS) {
            return Action.Signal.CONTINUE;
        }

        // Same three things the player's cut does: release, the shear sound, one point of durability.
        HumanPredatorNet.release(netted);
        marine.level().playSound(null, netted.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.1F);
        blade.hurtAndBreak(1, marine, HAND_TO_USE);

        return Action.Signal.CONTINUE;
    }

    public static Action.Signal onFinish(Action.Context<? extends Marine> context) {
        return UnequipItemAction.perform(context.getActor(), HAND_TO_USE);
    }

    private CutNettedMarineFreeAction() {
        throw new UnsupportedOperationException();
    }
}
