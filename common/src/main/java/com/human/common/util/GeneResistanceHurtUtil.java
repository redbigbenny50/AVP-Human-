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

        var isAcid = damageSource.is(com.human.common.registry.key.HumanDamageTypeKeys.ACID_VAPOR)
            || (AVPAlien.MOD.isLoaded() && damageSource.is(AlienDamageTypeKeys.ACID));

        if (isAcid) {
            // Corrosive Damage: the ATTACKER's acid hits harder (+100% at the cap). Applied before the target's own
            // acid resistance, so a resistant target still cuts the boosted hit.
            var corrosive = GeneValues.get(damageSource.getEntity(), Genes.CORROSIVE_DAMAGE);
            var boosted = corrosive > 0 ? (float) (damage * (1.0 + corrosive)) : damage;

            // [stated] Oct 19 - the vapors are acid too: acidium and nether acidium vapor.
            return applyGeneResistanceToDamage(geneContainer, Genes.ACID_RESISTANCE, boosted);
        }

        // Radiation Resistance: avp_human radiation and avp_alien radiation sickness. 100% = immune.
        if (
            damageSource.is(com.human.common.registry.key.HumanDamageTypeKeys.RADIATION)
                || (AVPAlien.MOD.isLoaded() && damageSource.is(AlienDamageTypeKeys.RADIATION_SICKNESS))
        ) {
            return applyGeneResistanceToDamage(geneContainer, Genes.RADIATION_RESISTANCE, damage);
        }

        // Toxin Resistance: wither, and the poison effect's tick (vanilla deals it as plain MAGIC with no attacker, so
        // it
        // is recognised by the victim actually being poisoned). 100% = immune.
        if (damageSource.is(DamageTypes.WITHER) || isPoisonTick(geneCarrier, damageSource)) {
            return applyGeneResistanceToDamage(geneContainer, Genes.TOXIN_RESISTANCE, damage);
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

    private static boolean isPoisonTick(GeneCarrier geneCarrier, DamageSource damageSource) {
        return damageSource.is(DamageTypes.MAGIC)
            && damageSource.getEntity() == null
            && geneCarrier instanceof net.minecraft.world.entity.LivingEntity living
            && living.hasEffect(net.minecraft.world.effect.MobEffects.POISON);
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
