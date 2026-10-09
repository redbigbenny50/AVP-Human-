package com.human.common.gameplay.entity.projectile;

import com.blib.api.common.entity.v1.projectile.BouncingItemProjectile;
import com.human.common.gameplay.effect.RadiationStatusEffect;
import com.human.common.registry.init.HumanEntityTypes;
import com.human.common.registry.init.HumanMobEffects;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ThrownGrenade extends BouncingItemProjectile {

    private static final String IS_INCENDIARY_KEY = "IsIncendiary";

    private static final String IS_IRRADIATED_KEY = "IsIrradiated";

    private boolean isIncendiary;

    private boolean isIrradiated;

    public ThrownGrenade(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
        this.shouldBounce = true;
        this.maxLife = 5 * 20;
    }

    public ThrownGrenade(Level level, LivingEntity livingEntity) {
        super(HumanEntityTypes.GRENADE_THROWN.get(), livingEntity, level);
        this.shouldBounce = true;
        this.maxLife = 5 * 20;
    }

    @Override
    public void tick() {
        super.tick();

        var level = level();
        var pos = position();
        var posX = pos.x;
        var posY = pos.y;
        var posZ = pos.z;

        if (this.firstTick) {
            playSound(SoundEvents.TNT_PRIMED, 1.0F, 1.0F);
        }

        if (level.isClientSide) {
            level.addParticle(ParticleTypes.SMOKE, posX, posY + 0.5D, posZ, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        if (isIrradiated) {
            return HumanItems.GRENADE_IRRADIATED.get();
        }

        if (isIncendiary) {
            return HumanItems.GRENADE_INCENDIARY.get();
        }

        return HumanItems.GRENADE.get();
    }

    @Override
    protected void onDeath() {
        var level = level();

        if (level.isClientSide) {
            return;
        }

        if (getOwner() instanceof com.human.common.gameplay.entity.living.human.marine.Marine marine) {
            // ⚠ Oct 5 squad tactics - A MARINE'S GRENADE SPARES ITS OWN SIDE AND ITS CAMP, like its rockets: no harm to
            // marine allies, and blocks only break if the server allows gunfire to break them.
            var interaction = com.human.common.property.HumanPropertyAccess.INSTANCE.get(
                com.human.common.property.HumanProperties.Weapons.BULLETS_DAMAGE_BLOCKS_ENABLED
            )
                ? Level.ExplosionInteraction.BLOCK
                : Level.ExplosionInteraction.NONE;

            level.explode(
                this,
                null,
                new MarineSafeExplosionDamageCalculator(marine),
                getX(),
                getY(),
                getZ(),
                isIrradiated ? 9F : 3F,
                isIncendiary,
                interaction
            );
        } else {
            level.explode(
                this,
                getX(),
                getY(),
                getZ(),
                isIrradiated ? 9F : 3F,
                isIncendiary,
                Level.ExplosionInteraction.BLOCK
            );
        }

        if (isIrradiated) {
            createIrradiatedCloudEffect(level);
        }
    }

    /** A marine's grenade does not hurt or knock back the marine's allies. */
    private static final class MarineSafeExplosionDamageCalculator extends net.minecraft.world.level.ExplosionDamageCalculator {

        private final net.minecraft.world.entity.Entity thrower;

        private MarineSafeExplosionDamageCalculator(net.minecraft.world.entity.Entity thrower) {
            this.thrower = thrower;
        }

        @Override
        public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, net.minecraft.world.entity.Entity entity) {
            return !com.human.common.gameplay.entity.living.human.marine.MarineAllyUtil.isMarineAlly(thrower, entity)
                && super.shouldDamageEntity(explosion, entity);
        }

        @Override
        public float getKnockbackMultiplier(net.minecraft.world.entity.Entity entity) {
            return com.human.common.gameplay.entity.living.human.marine.MarineAllyUtil.isMarineAlly(thrower, entity)
                ? 0.0F
                : super.getKnockbackMultiplier(entity);
        }
    }

    private void createIrradiatedCloudEffect(Level level) {
        var blockPos = blockPosition();
        var areaEffectCloudEntity = new AreaEffectCloud(level, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        areaEffectCloudEntity.setRadius(10.0F);
        areaEffectCloudEntity.setDuration(100);
        areaEffectCloudEntity.setRadiusPerTick(
            -areaEffectCloudEntity.getRadius() / areaEffectCloudEntity.getDuration()
        );
        areaEffectCloudEntity.setParticle(ParticleTypes.ASH);
        // The cloud hands out the radiation effect directly; the exposure manager absorbs whatever level it
        // implies. Amplifier 1 = CONTAMINATION, so catching a gas cloud plants you two rungs up the ladder and you
        // decay back down from there rather than taking a fixed lethal dose.
        areaEffectCloudEntity.addEffect(
            new MobEffectInstance(HumanMobEffects.getRadiationHolder(), RadiationStatusEffect.MEDIUM_EFFECT_DURATION_IN_TICKS, 1)
        );

        level.addFreshEntity(areaEffectCloudEntity);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        isIncendiary = compoundTag.getBoolean(IS_INCENDIARY_KEY);
        isIrradiated = compoundTag.getBoolean(IS_IRRADIATED_KEY);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putBoolean(IS_INCENDIARY_KEY, isIncendiary);
        compoundTag.putBoolean(IS_IRRADIATED_KEY, isIrradiated);
    }

    public void setIncendiary(boolean incendiary) {
        this.isIncendiary = incendiary;
    }

    public void setIrradiated(boolean isIrradiated) {
        this.isIrradiated = isIrradiated;
    }
}
