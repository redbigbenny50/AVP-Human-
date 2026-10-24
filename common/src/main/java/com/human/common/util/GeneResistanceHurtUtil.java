package com.human.common.util;

import com.alien.common.registry.key.AlienDamageTypeKeys;
import com.human.common.gameplay.entity.manager.GeneContainer;
import com.human.common.gameplay.gene.Gene;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.gene.Genes;
import com.human.common.model.GeneCarrier;
import com.human.compatibility.avp_alien.AVPAlien;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

public class GeneResistanceHurtUtil {

    public static float applyResistancesToDamage(
        GeneCarrier geneCarrier,
        DamageSource damageSource,
        float damage
    ) {
        var geneContainer = geneCarrier.getOrCreateGeneManager().getGeneContainer();

        if (AVPAlien.MOD.isLoaded()) {
            if (damageSource.is(AlienDamageTypeKeys.ACID)) {
                return applyGeneResistanceToDamage(geneContainer, Genes.ACID_RESISTANCE, damage);
            }
        }

        // [stated] Oct 19 - the vapors are acid too: acidium and nether acidium vapor.
        if (damageSource.is(com.human.common.registry.key.HumanDamageTypeKeys.ACID_VAPOR)) {
            return applyGeneResistanceToDamage(geneContainer, Genes.ACID_RESISTANCE, damage);
        }

        if (damageSource.is(DamageTypeTags.IS_FIRE)) {
            return applyGeneResistanceToDamage(geneContainer, Genes.FIRE_RESISTANCE, damage);
        }

        // [stated] Oct 19 - "the fire resistance and ice resistance genes reduce the effects right": cold resistance
        // only ever matched vanilla's own FREEZE damage, so cryofluid and cryo vapor (and every other mod's freezing,
        // which is_freezing covers) went straight through it. Fire was already fine: infernium vapor is in is_fire.
        if (
            damageSource.is(DamageTypes.FREEZE)
                || damageSource.is(DamageTypeTags.IS_FREEZING)
                || damageSource.is(com.human.common.registry.key.HumanDamageTypeKeys.CRYOFLUID)
                || damageSource.is(com.human.common.registry.key.HumanDamageTypeKeys.CRYO_VAPOR)
        ) {
            return applyGeneResistanceToDamage(geneContainer, Genes.COLD_RESISTANCE, damage);
        }

        return damage;
    }

    private static float applyGeneResistanceToDamage(
        GeneContainer geneContainer,
        Gene gene,
        float damage
    ) {
        // Percent reduction value.
        var percentageResistance = geneContainer.getActiveGeneMap()
            .getValue(gene, GeneOperationType.MULTIPLICATIVE);
        // Damage reduction value.
        var damageResistance = geneContainer.getActiveGeneMap()
            .getValue(gene, GeneOperationType.ADDITIVE);
        var modifiedDamage = Math.max(damage - (percentageResistance * damage) - damageResistance, 0);

        return (float) modifiedDamage;
    }
}
