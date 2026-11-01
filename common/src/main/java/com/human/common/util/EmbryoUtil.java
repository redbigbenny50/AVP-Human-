package com.human.common.util;

import com.human.common.gameplay.entity.manager.GeneContainer;
import com.human.common.gameplay.gene.Genes;
import com.human.common.model.GeneCarrier;
import com.human.common.registry.GeneBonusDataRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class EmbryoUtil {

    public static List<Entity> birthEmbryos(
        LivingEntity parentEntity,
        GeneContainer parentGeneContainer,
        Function<LivingEntity, @Nullable Entity> embryoFactory,
        int baseBirthCount
    ) {
        var embryoList = new ArrayList<Entity>();

        // 1 added here to guarantee 1 birth by default.
        var birthBonus = baseBirthCount + parentGeneContainer.getActiveGeneMap()
            .getValue(Genes.BONUS_EMBRYO_COUNT);
        var baseOffspring = (int) birthBonus;
        var fractionalChance = birthBonus - baseOffspring;

        // Guaranteed births based on whole number values.
        for (int i = 0; i < baseOffspring; i++) {
            var embryo = embryoFactory.apply(parentEntity);

            if (embryo != null) {
                embryoList.add(embryo);
            }
        }

        // Probabilistic birth based on fractional values.
        if (parentEntity.getRandom().nextDouble() < fractionalChance) {
            var embryo = embryoFactory.apply(parentEntity);

            if (embryo != null) {
                embryoList.add(embryo);
            }
        }

        return embryoList;
    }

    /** Normal facehugger: the burster takes half of its host's stats. [stated] Oct 27 */
    public static final double HOST_SHARE_NORMAL = 0.5;

    /** Royal facehugger: the burster takes three quarters of its host's stats. [stated] Oct 27 */
    public static final double HOST_SHARE_ROYAL = 0.75;

    /**
     * [stated] "a host's inbreeding strain should carry over to the burster and add to its genetic integrity loss". At
     * the locked damage share: a normal facehugger takes half of it, a royal one a quarter (royals keep more of the
     * good and less of the bad). Host strain 1.0 costs a normal burster -0.5 integrity.
     */
    public static final double HOST_STRAIN_SHARE_NORMAL = 0.5;

    public static final double HOST_STRAIN_SHARE_ROYAL = 0.25;

    public static void applyGenesToEmbryo(
        EntityType<?> parentType,
        GeneContainer parentGeneContainer,
        GeneCarrier offspring,
        boolean addBonusGenes
    ) {
        applyGenesToEmbryo(parentType, parentGeneContainer, offspring, addBonusGenes ? 1.0 : 0.0, null);
    }

    /**
     * Gives a host-born embryo its genes.
     * <ul>
     * <li>The parasite line ({@code parentGeneContainer}, what the facehugger carried in) passes in full, dormant genes
     * waking.</li>
     * <li>The HOST passes {@code hostShare} of its stats: its species values plus whatever genes the host itself
     * carries (e.g. a host that was injected), so a modified host makes a modified burster.</li>
     * <li>[stated] Oct 27 - the host's own integrity carries over to the burster as it is (not scaled by the share),
     * but facehugging itself adds no integrity cost the way a syringe injection does.</li>
     * </ul>
     */
    public static void applyGenesToEmbryo(
        EntityType<?> hostType,
        GeneContainer parentGeneContainer,
        GeneCarrier offspring,
        double hostShare,
        @Nullable GeneContainer hostGeneContainer
    ) {
        var offspringGeneContainer = offspring.getOrCreateGeneManager().getGeneContainer();

        // Transfer the parasite line's genes.
        parentGeneContainer.transfer(offspringGeneContainer, true);

        if (hostShare > 0.0) {
            addHostShare(hostType, offspringGeneContainer, hostShare, hostGeneContainer);
        }

        // Born UNSTABLE or worse: a chance of a defect.
        var random = offspring instanceof Entity entity ? entity.getRandom() : net.minecraft.util.RandomSource.create();
        com.human.common.gameplay.gene.GeneDefects.rollAtBirth(offspringGeneContainer, random);
    }

    private static void addHostShare(
        EntityType<?> hostType,
        GeneContainer offspringGeneContainer,
        double hostShare,
        @Nullable GeneContainer hostGeneContainer
    ) {
        var integrityId = Genes.GENETIC_INTEGRITY.id();

        GeneBonusDataRegistry.getOrDefault(hostType)
            .forEach((key, value) -> {
                if (!key.resourceLocation().equals(integrityId)) {
                    offspringGeneContainer.getActiveGeneMap().add(key, value * hostShare);
                }
            });

        if (hostGeneContainer != null) {
            hostGeneContainer.getActiveGeneMap()
                .getBackingMap()
                .forEach((key, value) -> {
                    var isIntegrity = key.resourceLocation().equals(integrityId);
                    offspringGeneContainer.getActiveGeneMap().add(key, isIntegrity ? value : value * hostShare);
                });

            var strain = hostGeneContainer.getStrain();
            if (strain > 0) {
                var strainShare = hostShare >= HOST_SHARE_ROYAL ? HOST_STRAIN_SHARE_ROYAL : HOST_STRAIN_SHARE_NORMAL;
                offspringGeneContainer.getActiveGeneMap()
                    .add(integrityId, com.human.common.gameplay.gene.GeneOperationType.ADDITIVE, -strain * strainShare);
            }

            if (hostGeneContainer.isModified()) {
                offspringGeneContainer.setModified(true);
                offspringGeneContainer.setGeneration(
                    Math.max(offspringGeneContainer.getGeneration(), hostGeneContainer.getGeneration() + 1)
                );
            }
        }
    }
}
