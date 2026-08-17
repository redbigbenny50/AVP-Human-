package com.human.common.registry.init;

import com.blib.api.common.codec.v1.stream.adapter.J2MStreamCodecAdapter;
import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.component.ArmorCaseContainerContents;
import com.human.common.gameplay.component.GeneReaderContents;
import com.human.common.gameplay.component.GeneReaderMode;
import com.human.common.gameplay.component.SyringeContents;
import com.human.common.gameplay.component.SyringeMode;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;

import java.util.function.UnaryOperator;

public class HumanDataComponents {

    private static final BLibRegistry<DataComponentType<?>> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.DATA_COMPONENT_TYPE);

    public static final BLibHolder<DataComponentType<Integer>> AMMUNITION = create(
        "ammunition",
        builder -> builder.persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<ArmorCaseContainerContents>> ARMOR_CASE_CONTAINER = create(
        "armor_case_container",
        builder -> builder.persistent(ArmorCaseContainerContents.CODEC)
            .networkSynchronized(new J2MStreamCodecAdapter<>(ArmorCaseContainerContents.STREAM_CODEC))
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<Integer>> CANISTER_CAPACITY = create(
        "canister_capacity",
        builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<Integer>> GUN_ANIMATION_ID = create(
        "gun_animation_id",
        builder -> builder.persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<Integer>> GUN_ANIMATION_TYPE = create(
        "gun_animation_type",
        builder -> builder.persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .cacheEncoding()
    );

    /**
     * ⭐ Which phase of the barrel spin Old Painless is in: 0 stopped, 1 spinning up, 2 looping.
     * <p>
     * ⚠ ON THE STACK, NOT IN A FIELD ON THE ITEM. Item instances are singletons shared by every player and every stack,
     * so a field would have one wielder's spin state stomping another's. Persistent so a gun saved mid-burst comes back
     * honest, and network-synchronised because the CLIENT animator is what reads it.
     * </p>
     */
    /**
     * The tick the gun last fired, used to spin the barrels DOWN on a timeout.
     * <p>
     * ⚠ THE SERVER NEVER LEARNS THAT A PLAYER RELEASED FIRE. Players shoot by streaming C2SGunFirePayload every tick
     * the attack button is held; letting go simply stops the stream, and no "stopped" packet is ever sent. So the only
     * way to know the trigger was released is that the payloads stopped arriving.
     * </p>
     */
    public static final BLibHolder<DataComponentType<Integer>> OLD_PAINLESS_LAST_FIRE_TICK = create(
        "old_painless_last_fire_tick",
        builder -> builder.persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .cacheEncoding()
    );

    /** Ticks elapsed since the wind-down started, so the animation type can be cleared once it has played out. */
    public static final BLibHolder<DataComponentType<Integer>> OLD_PAINLESS_SPIN_DOWN_TICK = create(
        "old_painless_spin_down_tick",
        builder -> builder.persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<Integer>> OLD_PAINLESS_SPIN_PHASE = create(
        "old_painless_spin_phase",
        builder -> builder.persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<GeneReaderContents>> GENE_READER_CONTENTS = create(
        "gene_reader_contents",
        builder -> builder.persistent(GeneReaderContents.CODEC)
            .networkSynchronized(new J2MStreamCodecAdapter<>(GeneReaderContents.STREAM_CODEC))
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<GeneReaderMode>> GENE_READER_MODE = create(
        "gene_reader_mode",
        builder -> builder.persistent(GeneReaderMode.CODEC)
            .networkSynchronized(new J2MStreamCodecAdapter<>(GeneReaderMode.STREAM_CODEC))
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<Boolean>> IS_FIRING = create(
        "is_firing",
        builder -> builder.persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL)
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<Boolean>> MARINE_OWNED = create(
        "marine_owned",
        builder -> builder.persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL)
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<Integer>> MUZZLE_FLASH_DURATION_IN_TICKS = create(
        "muzzle_flash_duration_in_ticks",
        builder -> builder.persistent(Codec.INT)
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<SyringeContents>> SYRINGE_CONTENTS = create(
        "syringe_contents",
        builder -> builder.persistent(SyringeContents.CODEC)
            .networkSynchronized(new J2MStreamCodecAdapter<>(SyringeContents.STREAM_CODEC))
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<SyringeMode>> SYRINGE_MODE = create(
        "syringe_mode",
        builder -> builder.persistent(SyringeMode.CODEC)
            .networkSynchronized(new J2MStreamCodecAdapter<>(SyringeMode.STREAM_CODEC))
            .cacheEncoding()
    );

    // OLD PAINLESS OVERHEAT. Heat is a persisted, network-synced component so the HUD/heat render layer and the
    // server-side firing lockout agree without a bespoke packet.
    public static final BLibHolder<DataComponentType<Integer>> OLD_PAINLESS_HEAT = create(
        "old_painless_heat",
        builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<Boolean>> OLD_PAINLESS_OVERHEATED = create(
        "old_painless_overheated",
        builder -> builder.persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL)
            .cacheEncoding()
    );

    private static <T> BLibHolder<DataComponentType<T>> create(
        String id,
        UnaryOperator<DataComponentType.Builder<T>> unaryOperator
    ) {
        return REGISTRY.createHolder(id, () -> unaryOperator.apply(DataComponentType.builder()).build());
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
