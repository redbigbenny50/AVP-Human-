package com.human.common.registry.init;

import com.blib.internal.mixin.MixinGameRulesAccessor;
import com.blib.internal.mixin.MixinGameRulesBooleanValueAccessor;
import net.minecraft.world.level.GameRules;

/**
 * avp_human's game rules. Registered through BLib's game-rule accessor, the same way BLib registers its own, so no
 * loader-specific code is needed.
 */
public final class HumanGameRules {

    /**
     * Oct 5 - performance programme, stage 2. {@code true}: a marine looks for targets nearest-first, stops after a
     * few, and reuses the answer for a quarter of a second (see MarineTargetScan). {@code false}: the old scan - every
     * living thing within 40 blocks, sight-checked every tick. Takes effect on the next tick.
     */
    public static final GameRules.Key<GameRules.BooleanValue> MARINE_TARGET_SCAN_CAP = MixinGameRulesAccessor
        .blib$register(
            "humanMarineTargetScanCap",
            GameRules.Category.MOBS,
            MixinGameRulesBooleanValueAccessor.blib$create(true)
        );

    /**
     * Oct 5 - Stage 2: marines far from every player think less often (MarineAiLod). Off = every marine thinks every
     * tick, as before.
     */
    public static final GameRules.Key<GameRules.BooleanValue> MARINE_AI_LOD = MixinGameRulesAccessor
        .blib$register(
            "humanMarineAiLod",
            GameRules.Category.MOBS,
            MixinGameRulesBooleanValueAccessor.blib$create(true)
        );

    private HumanGameRules() {
        throw new UnsupportedOperationException();
    }

    /** Loads the class, which registers the rules. Must run during mod initialisation, before any world loads. */
    public static void initialize() {
        // Static registration happens on class load.
    }
}
