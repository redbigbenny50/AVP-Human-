package com.human.common.gameplay.gene;

import com.human.common.model.GeneCarrier;
import net.minecraft.world.entity.player.Player;

/**
 * [stated] Oct 30: "For intelligence on a player make it reduce xp cost and increase xp gain."
 * <p>
 * Scales with how much of the gene's cap (100) the player carries - in practice from a gene infusion:
 * </p>
 * <ul>
 * <li><b>XP gain</b> from experience orbs: up to +{@value #GAIN_AT_CAP_PERCENT}% at the cap.</li>
 * <li><b>XP cost</b> of enchanting and of anvil work: up to -{@value #COST_CUT_AT_CAP_PERCENT}% at the cap. An anvil
 * job still costs at least 1 level (vanilla shows no result at 0), and "Too Expensive!" is decided before the cut, as
 * vanilla decides it.</li>
 * </ul>
 * <p>
 * Whole numbers come out of fractions by chance - a cost of 2 cut to 1.4 is 1 level six times in ten, 2 levels four
 * times - so small costs are cut on average instead of never. The anvil's cost is the exception: it is shown before
 * paying, so it rounds to the nearest level instead. Server only; genes are server data, and the server's numbers are
 * what is paid and synced. A negative (weakness) value does nothing here.
 * </p>
 */
public final class PlayerIntelligence {

    public static final int GAIN_AT_CAP_PERCENT = 50;

    public static final int COST_CUT_AT_CAP_PERCENT = 30;

    private static final double CAP = 100.0D;

    private PlayerIntelligence() {}

    /** 0..1: the share of the cap this player carries. 0 on the client or without the gene. */
    public static double fraction(Player player) {
        if (player.level().isClientSide) {
            return 0.0D;
        }
        var value = ((GeneCarrier) player).getOrCreateGeneManager().getGeneContainer().getActiveGeneMap().getValue(Genes.INTELLIGENCE);
        return Math.clamp(value / CAP, 0.0D, 1.0D);
    }

    /** Experience points from orbs, raised. */
    public static int scaleGain(Player player, int amount) {
        var fraction = fraction(player);
        if (amount <= 0 || fraction <= 0.0D) {
            return amount;
        }
        return roundByChance(player, amount * (1.0D + fraction * GAIN_AT_CAP_PERCENT / 100.0D));
    }

    /** Levels paid at the enchanting table, lowered (can reach 0 on a 1-level enchant, by chance). */
    public static int cutEnchantCost(Player player, int levels) {
        var fraction = fraction(player);
        if (levels <= 0 || fraction <= 0.0D) {
            return levels;
        }
        return roundByChance(player, levels * (1.0D - fraction * COST_CUT_AT_CAP_PERCENT / 100.0D));
    }

    /** An anvil job's shown and paid cost, lowered; never below 1. */
    public static int cutAnvilCost(Player player, int levels) {
        var fraction = fraction(player);
        if (levels <= 1 || fraction <= 0.0D) {
            return levels;
        }
        return Math.max(1, (int) Math.round(levels * (1.0D - fraction * COST_CUT_AT_CAP_PERCENT / 100.0D)));
    }

    private static int roundByChance(Player player, double value) {
        var whole = (int) Math.floor(value);
        return player.getRandom().nextDouble() < value - whole ? whole + 1 : whole;
    }
}
