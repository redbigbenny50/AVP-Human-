package com.human.common.gameplay.entity.living.human.marine;

import com.just.codec.Codec;

public interface MarineMode {

    Follow FOLLOW = Follow.INSTANCE;

    Hold HOLD = Hold.INSTANCE;

    Sentry SENTRY = Sentry.INSTANCE;

    enum Follow implements MarineMode {

        INSTANCE;

        public static final Codec<MarineMode.Follow> CODEC = EnumCodec.of(MarineMode.Follow.class);
    }

    enum Hold implements MarineMode {

        INSTANCE;

        public static final Codec<MarineMode.Hold> CODEC = EnumCodec.of(MarineMode.Hold.class);
    }

    /**
     * Guarding a fixed point. The marine stays within a leash of where it was posted rather than following anyone, but
     * is free to move inside it - to take cover, or to walk to a supply chest.
     */
    enum Sentry implements MarineMode {

        INSTANCE;

        public static final Codec<MarineMode.Sentry> CODEC = EnumCodec.of(MarineMode.Sentry.class);
    }

    Codec<MarineMode> CODEC = Codec.of("MarineMode", MarineModeCodec::decode, MarineModeCodec::encode);
}
