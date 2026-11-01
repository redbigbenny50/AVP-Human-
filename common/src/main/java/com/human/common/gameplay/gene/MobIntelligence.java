package com.human.common.gameplay.gene;

import com.human.common.model.GeneCarrier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * [stated] Nov 1: "in relation to the intelligence gene at the moment have it increase the amount of exp a mob drops."
 * <p>
 * The mob side of the Intelligence gene, on the same scale as the player side ({@link PlayerIntelligence}): a mob drops
 * up to +{@value #DROP_AT_CAP_PERCENT}% experience at the gene's cap (100), in proportion to how much of the cap it
 * carries. A fraction of a point becomes a whole one by chance, so small drops are raised on average instead of never.
 * A negative (weakness) value does nothing, as on a player. Players are left out - a player's own death drop is
 * vanilla's - and it stacks with a smart player's +50% from orbs, which is applied when the orbs are picked up.
 * </p>
 */
public final class MobIntelligence {

    public static final int DROP_AT_CAP_PERCENT = 50;

    private static final double CAP = 100.0D;

    private MobIntelligence() {}

    /** The experience this mob drops, raised by its Intelligence. Server only (genes are server data). */
    public static int scaleDrop(LivingEntity mob, int amount) {
        if (amount <= 0 || mob instanceof Player || mob.level().isClientSide || !(mob instanceof GeneCarrier carrier)) {
            return amount;
        }
        var value = carrier.getOrCreateGeneManager().getGeneContainer().getActiveGeneMap().getValue(Genes.INTELLIGENCE);
        var fraction = Math.clamp(value / CAP, 0.0D, 1.0D);
        if (fraction <= 0.0D) {
            return amount;
        }
        var raised = amount * (1.0D + fraction * DROP_AT_CAP_PERCENT / 100.0D);
        var whole = (int) Math.floor(raised);
        return mob.getRandom().nextDouble() < raised - whole ? whole + 1 : whole;
    }
}
