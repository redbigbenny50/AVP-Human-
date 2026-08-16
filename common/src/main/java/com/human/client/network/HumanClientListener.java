package com.human.client.network;

import com.human.client.effect.NukeClientEffects;
import com.human.client.effect.VoxelGunEffects;
import com.human.common.gameplay.entity.living.human.marine.sentry.SentryTargetFilter;
import com.human.common.network.packet.S2CBulletHitBlockPayload;
import com.human.common.network.packet.S2CGunKillEffectPayload;
import com.human.common.network.packet.S2CGunRecoilPayload;
import com.human.common.network.packet.S2CGunVoxelEffectPayload;
import com.human.common.network.packet.S2CMarineSentryFilterPayload;
import com.human.common.network.packet.S2CNukeEffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class HumanClientListener {

    /**
     * The filter the server sent for the marine whose screen is opening.
     * <p>
     * Parked in a static rather than handed straight to the screen because the payload can arrive either side of the
     * screen being constructed - the container-open packet and this one are not ordered relative to each other. The
     * screen takes it when it initialises, and clears it so a stale filter cannot leak into the next marine's page.
     */
    private static @Nullable SentryTargetFilter pendingSentryFilter;

    public static void handleMarineSentryFilter(S2CMarineSentryFilterPayload payload) {
        pendingSentryFilter = payload.filter();
    }

    public static @Nullable SentryTargetFilter takePendingSentryFilter() {
        var filter = pendingSentryFilter;

        pendingSentryFilter = null;

        return filter;
    }

    public static void handleBulletHitBlockPayload(S2CBulletHitBlockPayload bulletHitBlockPayload) {
        var blockPos = bulletHitBlockPayload.blockPos();
        var direction = bulletHitBlockPayload.direction();

        for (int i = 0; i < 16; i++) {
            Minecraft.getInstance().particleEngine.crack(blockPos, direction);
        }
    }

    public static void handleGunRecoil(S2CGunRecoilPayload gunRecoilPayload, Player player) {
        player.turn(gunRecoilPayload.horizontalKick() * 2.0F, -gunRecoilPayload.verticalKick() * 2.0F);
    }

    public static void handleGunVoxelEffect(S2CGunVoxelEffectPayload payload) {
        VoxelGunEffects.trigger(payload);
    }

    public static void handleGunKillEffect(S2CGunKillEffectPayload payload) {
        VoxelGunEffects.triggerKillEffect(payload);
    }

    public static void handleNukeEffect(S2CNukeEffectPayload payload) {
        NukeClientEffects.trigger(payload);
    }

    private HumanClientListener() {
        throw new UnsupportedOperationException();
    }
}
