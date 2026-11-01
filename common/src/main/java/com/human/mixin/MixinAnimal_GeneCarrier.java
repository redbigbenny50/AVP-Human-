package com.human.mixin;

import com.human.common.gameplay.gene.GeneInheritance;
import com.human.common.model.GeneCarrier;
import com.human.common.registry.init.HumanGameRules;
import com.human.common.util.EmbryoUtil;
import com.human.common.util.GeneIntegrityUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class MixinAnimal_GeneCarrier extends AgeableMob implements GeneCarrier {

    public MixinAnimal_GeneCarrier(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract void finalizeSpawnChildFromBreeding(ServerLevel level, Animal animal, @Nullable AgeableMob baby);

    /**
     * [stated] Oct 25 - the bonus-embryo extra babies stop when 60 or more of the same animal already live nearby, so a
     * gene-boosted pen cannot snowball. Game rule {@code humanBonusEmbryoCrowdCap} turns the cap off. Only the BONUS
     * babies are capped; the normal vanilla baby is always born.
     */
    @Unique
    private static final int avp_human$BONUS_EMBRYO_CROWD_CAP = 60;

    @Unique
    private static final double avp_human$BONUS_EMBRYO_CROWD_RADIUS = 32.0;

    @Inject(at = @At("HEAD"), method = "spawnChildFromBreeding")
    public void avp_human$spawnChildFromBreeding(ServerLevel level, Animal mate, CallbackInfo ci) {
        if (level.getGameRules().getBoolean(HumanGameRules.BONUS_EMBRYO_CROWD_CAP)) {
            var crowd = level.getEntities(
                getType(),
                getBoundingBox().inflate(avp_human$BONUS_EMBRYO_CROWD_RADIUS),
                entity -> entity.isAlive()
            ).size();

            if (crowd >= avp_human$BONUS_EMBRYO_CROWD_CAP) {
                return;
            }
        }

        EmbryoUtil.birthEmbryos(
            this,
            getOrCreateGeneManager().getGeneContainer(),
            parent -> avp_human$birthChild(level, mate),
            0
        );
    }

    @Inject(at = @At("HEAD"), method = "finalizeSpawnChildFromBreeding")
    public void avp_human$finalizeSpawnChildFromBreeding(ServerLevel level, Animal mate, @Nullable AgeableMob baby, CallbackInfo ci) {
        if (baby == null) {
            return;
        }

        var geneCarrier = (GeneCarrier) baby;

        var babyGeneContainer = geneCarrier.getOrCreateGeneManager().getGeneContainer();

        var parentGeneContainer = getOrCreateGeneManager().getGeneContainer();
        // ⚠ This used to read THIS animal's container a second time, so the mate was ignored and the first parent's
        // genes were added twice. The mate is the other parent.
        var otherParentGeneContainer = ((GeneCarrier) mate).getOrCreateGeneManager().getGeneContainer();

        // [stated] Oct 25 - Option A blend (see GeneInheritance). Dormant genes of both parents wake in the baby.
        // With both parents' ids the baby gets a family record, and a modified line gets inbreeding strain.
        GeneInheritance.blendInto(
            parentGeneContainer,
            ((net.minecraft.world.entity.Entity) (Object) this).getUUID(),
            otherParentGeneContainer,
            mate.getUUID(),
            babyGeneContainer,
            baby.getRandom()
        );

        var geneDecayLevel = GeneIntegrityUtil.getGeneDecayLevel(geneCarrier);

        switch (geneDecayLevel) {
            case FATAL -> baby.kill();
            case VOLATILE -> {
                // Ex. -1.75 -> 1.75
                var totalGeneIntegrity = Math.abs(GeneIntegrityUtil.getTotalGeneticIntegrity(geneCarrier));
                // Ex. 1.75 - 1 = 0.75
                var chance = totalGeneIntegrity - Math.floor(totalGeneIntegrity);
                // Ex. 0.75 means 75% chance to die.
                if (baby.getRandom().nextDouble() < chance) {
                    baby.kill();
                }
            }
            case STABLE, UNSTABLE -> { /* NO-OP */ }
        }
    }

    @Unique
    public @Nullable AgeableMob avp_human$birthChild(ServerLevel level, Animal mate) {
        var ageablemob = getBreedOffspring(level, mate);

        if (ageablemob != null) {
            ageablemob.setBaby(true);
            ageablemob.moveTo(getX(), getY(), getZ(), 0.0F, 0.0F);
            finalizeSpawnChildFromBreeding(level, mate, ageablemob);
            level.addFreshEntityWithPassengers(ageablemob);
        }

        return ageablemob;
    }
}
