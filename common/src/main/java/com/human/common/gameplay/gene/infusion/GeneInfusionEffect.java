package com.human.common.gameplay.gene.infusion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The icon and timer of a player's temporary gene infusion ([stated] Oct 30: "like a 3 day mc potion"). The effect
 * itself does nothing each tick: the genes live in the player's gene manager, and {@link PlayerGeneInfusion} takes them
 * off the moment this effect is gone - time up, milk, or the fresh body after a respawn.
 */
public class GeneInfusionEffect extends MobEffect {

    /** A muted gene-green. */
    private static final int COLOUR = 0x5FB86A;

    public GeneInfusionEffect() {
        super(MobEffectCategory.NEUTRAL, COLOUR);
    }
}
