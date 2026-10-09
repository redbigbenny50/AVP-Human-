package com.human.common.registry.init;

import com.blib.api.common.data_sync.v1.model.DataSyncKey;
import com.blib.api.common.registry.v1.BLibBuiltInRegistries;
import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.HumanResources;
import com.human.common.gameplay.entity.living.human.AbstractHuman;
import com.just.codec.stream.impl.StreamCodecs;
import com.mojang.serialization.Codec;

import java.util.function.Function;

public class HumanDataSyncKeys {

    private static final BLibRegistry<DataSyncKey<?>> REGISTRY = Human.MOD.registries().create(BLibBuiltInRegistries.DATA_SYNC_KEYS);

    public static final BLibHolder<DataSyncKey<Boolean>> ENTITY_HAS_WARP_EFFECT = create(
        "entity_has_warp_effect",
        builder -> builder.networkSynchronized(StreamCodecs.BOOLEAN)
            .build(false)
    );

    /**
     * Whether a mob is held by a titanium capture cord rather than a lead - sent to clients so the cord can be drawn as
     * a braided cable. The SERVER's truth is the mob's {@code avp_human.capture_cord} tag, which vanilla saves; this
     * flag only mirrors it, refreshed every tick by {@code MixinEntity_CaptureCordTick}.
     */
    public static final BLibHolder<DataSyncKey<Boolean>> ENTITY_ON_CAPTURE_CORD = create(
        "entity_on_capture_cord",
        builder -> builder.networkSynchronized(StreamCodecs.BOOLEAN)
            .build(false)
    );

    /**
     * [stated] Oct 3: who holds a mob on a titanium capture cord - their entity id, or -1. Synced so every client can
     * draw the cord; the server's truth is the tether itself. Not saved: a restart drops the cord rather than keep a
     * holder id that no longer means anything.
     */
    public static final BLibHolder<DataSyncKey<Integer>> ENTITY_CAPTURE_CORD_HOLDER = create(
        "entity_capture_cord_holder",
        builder -> builder.networkSynchronized(StreamCodecs.INT)
            .build(-1)
    );

    /**
     * Oct 5 - the side of the pit a human is scrambling up, as a horizontal {@code Direction} 2D value, or -1 when it
     * is not scrambling. Synced so every client can stand the body upright with its chest to that wall instead of
     * drawing the flat swimming tilt (see HumanWallScramble and HumanEntityModelRenderer). Not saved: a scramble does
     * not survive a reload.
     */
    public static final BLibHolder<DataSyncKey<Integer>> HUMAN_SCRAMBLE_DIRECTION = create(
        "human_scramble_direction",
        builder -> builder.networkSynchronized(StreamCodecs.INT)
            .build(-1)
    );

    public static final BLibHolder<DataSyncKey<Integer>> MARINE_SKIN_COLOR = create(
        "marine_skin_color",
        builder -> builder.networkSynchronized(StreamCodecs.INT)
            .persistent("skinColor", Codec.INT)
            .build(0xEED0B6)
    );

    public static final BLibHolder<DataSyncKey<Integer>> MARINE_BEARD_VARIANT = create(
        "marine_beard_variant",
        builder -> builder.networkSynchronized(StreamCodecs.INT)
            .persistent("beardVariant", Codec.INT)
            .build(0)
    );

    public static final BLibHolder<DataSyncKey<Integer>> MARINE_EYE_COLOR = create(
        "marine_eye_color",
        builder -> builder.networkSynchronized(StreamCodecs.INT)
            .persistent("eyeColor", Codec.INT)
            .build(0xA1CAF1)
    );

    public static final BLibHolder<DataSyncKey<Integer>> MARINE_HAIR_COLOR = create(
        "marine_hair_color",
        builder -> builder.networkSynchronized(StreamCodecs.INT)
            .persistent("hairColor", Codec.INT)
            .build(0x86462C)
    );

    public static final BLibHolder<DataSyncKey<Integer>> MARINE_HAIR_VARIANT = create(
        "marine_hair_variant",
        builder -> builder.networkSynchronized(StreamCodecs.INT)
            .persistent("hairVariant", Codec.INT)
            .build(0)
    );

    public static final BLibHolder<DataSyncKey<Boolean>> MARINE_IS_MALE = create(
        "marine_is_male",
        builder -> builder.networkSynchronized(StreamCodecs.BOOLEAN)
            .persistent("isMale", Codec.BOOL)
            .build(true)
    );

    public static final BLibHolder<DataSyncKey<Integer>> MARINE_TICKS_UNTIL_BORED = create(
        "marine_ticks_until_bored",
        builder -> builder.persistent("marineTicksUntilBored", Codec.INT)
            .build(AbstractHuman.MIN_IDLE_TIME_IN_TICKS)
    );

    private static <T> BLibHolder<DataSyncKey<T>> create(String path, Function<DataSyncKey.Builder<T>, DataSyncKey<T>> factory) {
        var resourceLocation = HumanResources.location(path);
        return REGISTRY.createHolder(path, () -> factory.apply(new DataSyncKey.Builder<>(resourceLocation)));
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
