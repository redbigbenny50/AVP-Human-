package com.human.common.network;

import com.human.common.gameplay.combat.DaggerCombat;
import com.human.common.gameplay.combat.OffHandAttacker;
import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.GunReloading;
import com.human.common.gameplay.menu.marine.MarineInventoryMenu;
import com.human.common.model.Crawler;
import com.human.common.network.packet.C2SDaggerOffHandAttackPayload;
import com.human.common.network.packet.C2SGunFirePayload;
import com.human.common.network.packet.C2SGunReloadPayload;
import com.human.common.network.packet.C2SMarineSentryFilterPayload;
import com.human.common.network.packet.C2SPlayerToggleCrawlPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class HumanServerListener {

    public static void handleGunFirePayload(C2SGunFirePayload gunFirePayload, Player serverPlayer) {
        // AIM SANITY GATE. The server now owns the ray (HitScanGunAttackAction), so it must also be satisfied the
        // shot came from where the client claims it did. A packet whose yaw or pitch disagrees with the player's
        // server-side rotation by more than 35 degrees is discarded rather than traced.
        if (
            Math.abs(net.minecraft.util.Mth.wrapDegrees(gunFirePayload.yaw() - serverPlayer.getYRot())) > 35.0F
                || Math.abs(gunFirePayload.pitch() - serverPlayer.getXRot()) > 35.0F
        ) {
            return;
        }

        var itemStack = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);

        if (itemStack.getItem() instanceof GunItem gunItem) {
            // ⭐ Old Painless spins its barrels from the FIRE path, not from onUseTick - a player's trigger is the
            // attack button and never makes the item "in use". Server-side so every tracking client sees the spin.
            if (gunItem instanceof com.human.common.gameplay.item.old_painless.OldPainlessItem oldPainless) {
                oldPainless.onFired(itemStack, serverPlayer, gunFirePayload.tickProgress());
            }

            gunItem.fire(serverPlayer.level(), serverPlayer, itemStack, gunFirePayload.tickProgress());
        }
    }

    public static void handleGunReloadPayload(C2SGunReloadPayload gunReloadPayload, Player serverPlayer) {
        GunReloading.reload(serverPlayer);
    }

    /**
     * Resolves a strike from the off-hand dagger.
     * <p>
     * ⚠ EVERY PRECONDITION IS RE-CHECKED HERE. The packet names a target and nothing else, so a client cannot use it to
     * hit something out of reach, hit with a hand that is empty, or hit faster than the cooldown allows - the server
     * decides all three from its own copy of the player.
     */
    public static void handleDaggerOffHandAttackPayload(C2SDaggerOffHandAttackPayload payload, Player player) {
        var offHandItemStack = player.getOffhandItem();

        if (!DaggerCombat.isDagger(offHandItemStack)) {
            return;
        }

        if (!(player.level().getEntity(payload.targetEntityId()) instanceof LivingEntity target)) {
            return;
        }

        if (target == player || !target.isAlive() || !DaggerCombat.isWithinReach(player, target)) {
            return;
        }

        var offHandAttacker = (OffHandAttacker) player;

        if (!offHandAttacker.avp_human$isOffHandAttackReady()) {
            return;
        }

        offHandAttacker.avp_human$resetOffHandAttackStrength();

        var damage = DaggerCombat.computeOffHandAttackDamage(player);
        var damageSource = player.damageSources().playerAttack(player);

        if (target.hurt(damageSource, damage)) {
            // Vanilla applies the held item's on-hit effects and durability loss from the main hand; the off-hand
            // stack has to be told about its own swing explicitly.
            offHandItemStack.hurtEnemy(target, player);
            player.setLastHurtMob(target);
        }

        player.swing(InteractionHand.OFF_HAND, true);
        player.causeFoodExhaustion(0.1F);
    }

    public static void handlePlayerToggleCrawlPayload(C2SPlayerToggleCrawlPayload playerToggleCrawlPayload, Player serverPlayer) {
        var crawler = (Crawler) serverPlayer;
        crawler.setCrawling(playerToggleCrawlPayload.shouldCrawl());
    }

    /**
     * Applies a target filter to whichever marine's screen this player has open.
     * <p>
     * The open container IS the authorisation: a player who is not looking at that marine has no container to name it
     * with. Also re-checks leadership, so someone else's open screen cannot be used to retask their marines.
     */
    public static void handleMarineSentryFilterPayload(C2SMarineSentryFilterPayload payload, Player player) {
        if (!(player.containerMenu instanceof MarineInventoryMenu marineInventoryMenu)) {
            return;
        }

        var marine = marineInventoryMenu.getMarine();

        if (marine == null || !marine.isLeader(player)) {
            return;
        }

        marine.setSentryFilter(payload.filter());
    }

    private HumanServerListener() {
        throw new UnsupportedOperationException();
    }
}
