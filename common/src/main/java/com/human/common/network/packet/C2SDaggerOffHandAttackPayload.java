package com.human.common.network.packet;

import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * A player striking with the dagger in their off hand.
 * <p>
 * Carries only the target's entity id. Everything else - what the player is holding, whether that hand is off cooldown,
 * whether the target is in reach - is re-derived on the server, because a client that can name an entity id must not be
 * able to name its own damage with it.
 */
public record C2SDaggerOffHandAttackPayload(
    int targetEntityId
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("dagger_off_hand_attack");

    public static final Type<C2SDaggerOffHandAttackPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<C2SDaggerOffHandAttackPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.INT,
        C2SDaggerOffHandAttackPayload::targetEntityId,
        C2SDaggerOffHandAttackPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
