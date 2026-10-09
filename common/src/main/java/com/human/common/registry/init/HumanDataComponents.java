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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.function.UnaryOperator;

public class HumanDataComponents {

    private static final BLibRegistry<DataComponentType<?>> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.DATA_COMPONENT_TYPE);

    /**
     * The charge carried on a broken power-storage block's ITEM.
     * <p>
     * ⭐ [stated] Sep 25: tier-2 recipes consume the tier-1 block in the fabricator, so breaking an ESU to upgrade is
     * the intended path - and a full one silently losing 1,500,000 FE on the way would be a nasty surprise. Written by
     * {@code BatteryBlockEntity.collectImplicitComponents}, copied by the loot table, restored on placement.
     * </p>
     * <p>
     * ⚠ {@code networkSynchronized} so the client can show it in a tooltip; a server-only component would leave the
     * held item looking empty.
     * </p>
     */
    /** [stated] Sep 30: the fuse panel a wiring tool has selected - dimension and position - for binding devices. */
    public static final BLibHolder<DataComponentType<net.minecraft.core.GlobalPos>> SELECTED_FUSE_PANEL = create(
        "selected_fuse_panel",
        builder -> builder.persistent(net.minecraft.core.GlobalPos.CODEC).networkSynchronized(net.minecraft.core.GlobalPos.STREAM_CODEC)
    );

    /** [stated] Oct 2: a Skin Kit's skin - the id of a skin defined in a resource pack. */
    public static final BLibHolder<DataComponentType<net.minecraft.resources.ResourceLocation>> SENTRY_SKIN = create(
        "sentry_skin",
        builder -> builder.persistent(net.minecraft.resources.ResourceLocation.CODEC)
            .networkSynchronized(net.minecraft.resources.ResourceLocation.STREAM_CODEC)
    );

    public static final BLibHolder<DataComponentType<Long>> STORED_POWER = create(
        "stored_power",
        builder -> builder.persistent(Codec.LONG)
            .networkSynchronized(ByteBufCodecs.VAR_LONG)
            .cacheEncoding()
    );

    /**
     * The closed breaker faces, carried on a broken power block's item so a wired room does not have to be reconfigured
     * after every rebuild.
     * <p>
     * ⚠ NAMES, NOT A BITMASK. A mask silently means something different if the direction order ever changes; an
     * unrecognised name is simply skipped.
     * </p>
     */
    public static final BLibHolder<DataComponentType<java.util.List<String>>> POWER_PORTS = create(
        "power_ports",
        builder -> builder.persistent(Codec.STRING.listOf())
            .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
            .cacheEncoding()
    );

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

    /**
     * The fluid in a universal canister - any registered fluid, from any mod, by the millibucket. See
     * {@link com.human.common.gameplay.item.canister.UniversalCanisterItem}.
     */
    public static final BLibHolder<DataComponentType<com.human.common.gameplay.fluid.FluidContent>> FLUID_CONTENT = create(
        "fluid_content",
        builder -> builder.persistent(com.human.common.gameplay.fluid.FluidContent.CODEC)
            .networkSynchronized(com.human.common.gameplay.fluid.FluidContent.STREAM_CODEC)
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<Integer>> CANISTER_CAPACITY = create(
        "canister_capacity",
        builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .cacheEncoding()
    );

    /**
     * ⭐ SHARED BY EVERY GUN WITH A HEAT PROFILE, not just Old Painless. Persistent so a gun put away hot comes back
     * hot, and network-synchronised because the CLIENT renders the barrel tint from it.
     * <p>
     * ⚠ THE OLD {@code old_painless_heat} / {@code old_painless_overheated} TYPES ARE STILL REGISTERED BELOW AND ARE
     * NOW UNUSED. They are deliberately not deleted: a saved gun in someone's world may still carry them, and
     * unregistering a component type that exists in saved data is not a safe thing to do to a live world. Nothing reads
     * them any more, and they fall off the stack the first time it is written.
     * </p>
     */
    /**
     * The id of a targeting profile fitted to this stack by a bolt-on smart module.
     * <p>
     * ⚠⚠ NETWORK-SYNCHRONISED, AND THAT IS LOAD-BEARING. The CLIENT draws the smart HUD, so it must know the stack
     * carries a module. An unsynced component here would be the muzzle-flash bug again: written only by the server,
     * seen by the client only when something ELSE about the stack happened to change.
     * </p>
     * <p>
     * ⚠ AN ID, NOT AN INLINED PROFILE. Storing the whole record would bloat every stack and freeze its tuning at the
     * moment of crafting, so a later balance pass would leave old modules on old numbers forever.
     * </p>
     */
    public static final BLibHolder<DataComponentType<ResourceLocation>> SMART_MODULE = create(
        "smart_module",
        builder -> builder.persistent(ResourceLocation.CODEC)
            .networkSynchronized(ResourceLocation.STREAM_CODEC)
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<Integer>> GUN_HEAT = create(
        "gun_heat",
        builder -> builder.persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .cacheEncoding()
    );

    public static final BLibHolder<DataComponentType<Boolean>> GUN_OVERHEATED = create(
        "gun_overheated",
        builder -> builder.persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL)
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

    /**
     * ⚠⚠ THIS WAS NOT NETWORK-SYNCHRONISED, AND THAT IS WHY MUZZLE FLASHES WERE ERRATIC. Only the server ever sets it -
     * the client sends a fire packet and never runs the shoot pipeline itself - so an unsynced value meant the renderer
     * was reading a component the client had no honest copy of. It appeared to work only because ANY change to the
     * stack (ammunition, the animation id) resends the whole stack, so the flash arrived as a side effect of some other
     * component changing, and went missing whenever nothing else happened to change that tick.
     * </p>
     */
    public static final BLibHolder<DataComponentType<Integer>> MUZZLE_FLASH_DURATION_IN_TICKS = create(
        "muzzle_flash_duration_in_ticks",
        builder -> builder.persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
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
