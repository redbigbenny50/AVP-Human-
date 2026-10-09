package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.effect.RadiationStatusEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

import java.util.function.Supplier;

public class HumanMobEffects {

    private static final BLibRegistry<MobEffect> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.MOB_EFFECT);

    private static final BLibHolder<MobEffect> RADIATION = create("radiation", RadiationStatusEffect::new);

    public static Holder<MobEffect> getRadiationHolder() {
        return RADIATION.getBackingHolder();
    }

    /** Frozen solid — the shared freeze rule (see FrozenSolid). */
    private static final BLibHolder<MobEffect> FROZEN_SOLID = create(
        "frozen_solid",
        com.human.common.gameplay.freeze.FrozenSolidStatusEffect::new
    );

    public static Holder<MobEffect> getFrozenSolidHolder() {
        return FROZEN_SOLID.getBackingHolder();
    }

    private static BLibHolder<MobEffect> create(String path, Supplier<MobEffect> mobEffectSupplier) {
        return REGISTRY.createHolder(path, mobEffectSupplier);
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
