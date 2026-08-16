package com.human.client.input;

import com.human.Human;
import com.human.common.gameplay.combat.DaggerCombat;
import com.human.common.gameplay.combat.OffHandAttacker;
import com.human.common.network.packet.C2SDaggerOffHandAttackPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Client-side dagger controls: the off-hand stab, and the alternation that makes duel wield mode worth using.
 */
public class DaggerInputHandler {

    /**
     * Which hand duel wield mode should use for the next left click. Client-only: the server never trusts it, it only
     * decides which of the two attack routes the click takes.
     */
    private static boolean nextSwingIsOffHand;

    /**
     * Takes the left click when the off hand's turn has come round.
     *
     * @return whether this click was consumed and vanilla's attack should be cancelled.
     */
    public static boolean tryTakeAttackWithOffHand(Minecraft minecraft) {
        var player = minecraft.player;

        if (player == null || !DaggerCombat.isDualWielding(player)) {
            // Not duel wielding. Leave the click alone entirely and reset, so picking the daggers back up always starts
            // on the main hand rather than mid-rhythm.
            nextSwingIsOffHand = false;

            return false;
        }

        if (!nextSwingIsOffHand) {
            // The main hand's turn. Vanilla handles it, and the NEXT click belongs to the off hand.
            nextSwingIsOffHand = true;

            return false;
        }

        nextSwingIsOffHand = false;

        var target = pickTargetOrNull(minecraft);

        if (target == null) {
            // Nothing to hit. Hand the click back rather than swallowing it, so a miss still plays vanilla's swing.
            return false;
        }

        // ⚠ Checked on the client too, purely so a click on a hand that has not recovered falls through to vanilla and
        // swings the main hand instead of silently doing nothing. The server checks it again and is the authority.
        if (!((OffHandAttacker) player).avp_human$isOffHandAttackReady()) {
            return false;
        }

        Human.MOD.networking().sendToServer(new C2SDaggerOffHandAttackPayload(target.getId()));

        return true;
    }

    /**
     * The off-hand stab, on right click. Only when the main hand is holding something else - with a dagger in both
     * hands this stops responding and the left button drives them instead.
     *
     * @return whether the right click was consumed.
     */
    public static boolean tryStabWithOffHand(Minecraft minecraft) {
        var player = minecraft.player;

        if (player == null || !DaggerCombat.canStabWithOffHand(player)) {
            return false;
        }

        var target = pickTargetOrNull(minecraft);

        if (target == null || !((OffHandAttacker) player).avp_human$isOffHandAttackReady()) {
            return false;
        }

        Human.MOD.networking().sendToServer(new C2SDaggerOffHandAttackPayload(target.getId()));

        return true;
    }

    /**
     * Whatever the crosshair is on, if it is something that can be hit.
     */
    private static LivingEntity pickTargetOrNull(Minecraft minecraft) {
        return minecraft.hitResult instanceof EntityHitResult entityHitResult
            && entityHitResult.getEntity() instanceof LivingEntity target
                ? target
                : null;
    }

    private DaggerInputHandler() {
        throw new UnsupportedOperationException();
    }
}
