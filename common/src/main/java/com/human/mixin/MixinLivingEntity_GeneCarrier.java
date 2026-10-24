package com.human.mixin;

import com.blib.api.common.data_sync.v1.DataAccessor;
import com.blib.api.common.data_sync.v1.model.DataUser;
import com.blib.api.common.entity.v1.TeleportUtil;
import com.human.common.gameplay.entity.manager.GeneManager;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.gene.Genes;
import com.human.common.model.GeneCarrier;
import com.human.common.registry.init.HumanDataSyncKeys;
import com.human.common.util.GeneResistanceHurtUtil;
import com.human.mixin_util.MixinLivingEntity_GeneCarrier$Compatibility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity_GeneCarrier extends Entity implements GeneCarrier, DataUser {

    @Unique
    private final DataAccessor<Boolean> avp_human$hasWarpEffect = new DataAccessor<>(this, HumanDataSyncKeys.ENTITY_HAS_WARP_EFFECT.get());

    @Unique
    private GeneManager avp_human$geneManager;

    public MixinLivingEntity_GeneCarrier(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo callbackInfo) {
        var geneManager = getOrCreateGeneManager();

        geneManager.tick();

        if (level().isClientSide && avp_human$hasWarpEffect.get()) {
            for (int i = 0; i < 2; ++i) {
                level().addParticle(
                    ParticleTypes.PORTAL,
                    getRandomX(0.5F),
                    getRandomY() - (double) 0.25F,
                    getRandomZ(0.5F),
                    (random.nextDouble() - (double) 0.5F) * (double) 2.0F,
                    -random.nextDouble(),
                    (random.nextDouble() - (double) 0.5F) * (double) 2.0F
                );
            }
        }

        if (!level().isClientSide) {
            avp_human$hasWarpEffect.set(avp_human$hasWarpGene());
        }
    }

    /**
     * [stated] Oct 19 - a mob whose cold resistance gene is at 100% never freezes: no frost meter from powder snow or
     * anything else that uses vanilla's freezing.
     */
    @Inject(method = "canFreeze", at = @At("HEAD"), cancellable = true)
    private void avp_human$fullColdResistanceNeverFreezes(CallbackInfoReturnable<Boolean> cir) {
        if (com.human.common.util.GeneImmunity.coldImmune((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(
        method = "hurt",
        at = @At(value = "HEAD"),
        index = 2,
        argsOnly = true
    )
    private float avp_human$hurt(float originalDamage, DamageSource damageSource) {
        // Apply resistance modifiers to the damage value.
        return GeneResistanceHurtUtil.applyResistancesToDamage(this, damageSource, originalDamage);
    }

    @Inject(at = @At("HEAD"), method = "hurt", cancellable = true)
    public void avp_human$hurt(DamageSource damageSource, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (damage == 0) {
            cir.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "hurt", cancellable = true)
    public void avp_human$preHurtEffects(DamageSource damageSource, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (!avp_human$hasWarpEffect.get()) {
            return;
        }

        // TODO: Factor in multiplicative in here.
        var warpStrength = getOrCreateGeneManager().getGeneContainer()
            .getActiveGeneMap()
            .getValue(Genes.WARP, GeneOperationType.ADDITIVE);

        if (avp_human$shouldTeleportToDodgeProjectile(damageSource, warpStrength) || avp_human$shouldTeleportRandomly(warpStrength)) {
            var self = LivingEntity.class.cast(this);

            for (var i = 0; i < 64; ++i) {
                if (TeleportUtil.teleport(self)) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "hurt")
    public void avp_human$postHurtEffects(DamageSource damageSource, float damage, CallbackInfoReturnable<Boolean> cir) {
        var isHurt = cir.getReturnValueZ();

        if (isHurt) {
            avp_human$handleAcidBloodGene(damageSource, damage);
            avp_human$handlePoisonousBarbsGene(damageSource, damage);
            avp_human$handleThornsGene(damageSource, damage);
        }
    }

    @Inject(at = @At("RETURN"), method = "isSensitiveToWater", cancellable = true)
    public void avp_human$isSensitiveToWater(CallbackInfoReturnable<Boolean> cir) {
        var isSensitiveToWater = cir.getReturnValueZ();
        cir.setReturnValue(isSensitiveToWater || avp_human$hasWarpGene());
    }

    @Inject(at = @At("HEAD"), method = "readAdditionalSaveData")
    public void readAdditionalSaveData(CompoundTag compoundTag, CallbackInfo callbackInfo) {
        getOrCreateGeneManager().load(compoundTag);
    }

    @Inject(at = @At("HEAD"), method = "addAdditionalSaveData")
    public void addAdditionalSaveData(CompoundTag compoundTag, CallbackInfo callbackInfo) {
        getOrCreateGeneManager().save(compoundTag);
    }

    @Override
    public GeneManager getOrCreateGeneManager() {
        if (avp_human$geneManager == null) {
            var self = LivingEntity.class.cast(this);
            this.avp_human$geneManager = new GeneManager(self);
        }

        return avp_human$geneManager;
    }

    @Override
    public void setGeneManager(GeneManager geneManager) {
        this.avp_human$geneManager = geneManager;
    }

    @Unique
    private boolean avp_human$hasWarpGene() {
        return getOrCreateGeneManager().getGeneContainer()
            .getActiveGeneMap()
            .hasGene(Genes.WARP);
    }

    @Unique
    private boolean avp_human$shouldTeleportRandomly(double warpStrength) {
        // If warp strength is 0.1, then this is 1.
        // If warp strength is 0.5, then this is 0.6.
        // If warp strength is 1, then this is 0.1.
        var randomTeleportPoolSizeMultiplier = Math.clamp(1.1 - warpStrength, 0, 1.0);
        var randomTeleportPoolSize = (int) (100 * randomTeleportPoolSizeMultiplier);

        return random.nextInt(randomTeleportPoolSize) == 0;
    }

    @Unique
    private boolean avp_human$shouldTeleportToDodgeProjectile(DamageSource damageSource, double warpStrength) {
        var projectileTeleportPoolSizeMultiplier = Math.clamp(1.1 - warpStrength, 0, 1.0);
        var projectileTeleportPoolSize = (int) (10 * projectileTeleportPoolSizeMultiplier);

        return damageSource.is(DamageTypeTags.IS_PROJECTILE)
            && random.nextInt(projectileTeleportPoolSize) == 0;
    }

    @Unique
    private void avp_human$handleAcidBloodGene(DamageSource damageSource, float damage) {
        MixinLivingEntity_GeneCarrier$Compatibility.runAcidicBloodGeneEffects(this, damageSource, damage);
    }

    @Unique
    private void avp_human$handlePoisonousBarbsGene(DamageSource damageSource, float damage) {
        if (
            !damageSource.is(DamageTypeTags.AVOIDS_GUARDIAN_THORNS)
                && damageSource.getDirectEntity() instanceof LivingEntity livingEntity
        ) {
            // TODO: Factor in additive in here.
            var poisonChance = getOrCreateGeneManager().getGeneContainer()
                .getActiveGeneMap()
                .getValue(Genes.POISONOUS_BARBS, GeneOperationType.MULTIPLICATIVE);

            if (getRandom().nextDouble() < poisonChance) {
                // TODO: Amplify level with increasing levels.
                livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 6 * 20, 0), this);
            }
        }
    }

    @Unique
    private void avp_human$handleThornsGene(DamageSource damageSource, float damage) {
        if (
            !damageSource.is(DamageTypeTags.AVOIDS_GUARDIAN_THORNS)
                && damageSource.getDirectEntity() instanceof LivingEntity livingEntity
        ) {
            // TODO: Factor in multiplicative in here.
            var thornsDamage = getOrCreateGeneManager().getGeneContainer()
                .getActiveGeneMap()
                .getValue(Genes.THORNS, GeneOperationType.ADDITIVE);

            if (thornsDamage >= 0) {
                livingEntity.hurt(livingEntity.damageSources().thorns(this), (float) thornsDamage);
            }
        }
    }
}
