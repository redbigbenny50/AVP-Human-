package com.human.common.network.packet;

import com.blib.api.common.codec.v1.stream.impl.EnumStreamCodec;
import com.human.HumanResources;
import com.just.codec.stream.RecordStreamCodec;
import com.just.codec.stream.StreamCodec;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;

public record S2CGunAnimationPayload(
    int entityId,
    InteractionHand hand,
    Animation animation
) implements CustomPacketPayload {

    public static final ResourceLocation PAYLOAD_ID = HumanResources.location("gun_animation");

    public static final Type<S2CGunAnimationPayload> TYPE = new Type<>(PAYLOAD_ID);

    public static final StreamCodec<S2CGunAnimationPayload> CODEC = RecordStreamCodec.of(
        StreamCodecs.INT,
        S2CGunAnimationPayload::entityId,
        EnumStreamCodec.of(InteractionHand.class, InteractionHand.MAIN_HAND),
        S2CGunAnimationPayload::hand,
        EnumStreamCodec.of(Animation.class, Animation.SHOOT),
        S2CGunAnimationPayload::animation,
        S2CGunAnimationPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Animation {
        SHOOT,
        RELOAD,
        OLD_PAINLESS_SPIN_LOOP,
        OLD_PAINLESS_SPIN_DOWN,
        OLD_PAINLESS_SPIN_UP
    }
}
