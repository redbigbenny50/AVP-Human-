package com.human.common.registry.init;

import com.blib.api.common.registry.v1.impl.BLibCompostableRegistry;
import com.human.Human;
import com.human.common.registry.init.item.HumanBlockItems;

/**
 * What avp_human adds to the composter.
 * <p>
 * Registered through BLib's compostable registry, the same way {@link HumanFuels} registers furnace fuels: on Fabric
 * BLib hands it to Fabric's composting registry at startup, and on NeoForge BLib's own datagen writes it into the
 * {@code neoforge:compostables} data map. So the entry survives every datagen run - nothing to put back by hand.
 * </p>
 */
public class HumanCompostables {

    private static final BLibCompostableRegistry REGISTRY = Human.MOD.registries().createCompostableRegistry();

    /** [stated] Oct 1: a kelp block always adds a layer, so 7 make one bone meal. */
    public static final float KELP_BLOCK_CHANCE = 1.0F;

    public static void initialize() {
        REGISTRY.register(HumanBlockItems.KELP_BLOCK, KELP_BLOCK_CHANCE, false, false);
    }
}
