package com.human.mixin;

import com.blib.api.common.entity.v1.BLibEntityPredicates;
import com.human.common.gameplay.effect.RadiationLevel;
import com.human.common.gameplay.effect.RadiationStatusEffect;
import com.human.common.model.RadiationExposure;
import com.human.common.registry.init.HumanMobEffects;
import com.human.common.registry.tag.HumanBlockTags;
import com.human.common.registry.tag.HumanEntityTypeTags;
import com.human.util.HumanPredicates;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The radiation exposure manager: turns TIME SPENT IN RADIATION into a rising sickness level.
 * <p>
 * Radiation is not a fixed-duration debuff. Sources ({@code MixinItem_GiveRads} for hot items in the inventory,
 * {@code MixinLivingEntity_NukedRadiation} for irradiated ground, blasts and radioactive attacks) mark the entity as
 * exposed each tick they are present. This manager accumulates that exposure, derives a {@link RadiationLevel} from it,
 * mirrors the level onto the visible radiation effect, and applies the level's damage and afflictions.
 * </p>
 * <p>
 * Exposure climbs one level per 60 seconds of contact and falls one level per 90 seconds of being clean, so escaping a
 * source starts the clock running backwards - through every level, in order - rather than curing you outright.
 * Contamination is faster than recovery on purpose.
 * </p>
 * <p>
 * This replaces an earlier design in which each source re-applied a fixed-duration effect. Because a source could not
 * refresh an effect that was already running, that design produced an endless sawtooth: a single hot item in a pocket
 * re-applied a full lethal dose the instant the previous one expired, forever.
 * </p>
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity_RadiationDamage extends Entity implements RadiationExposure {

    @Unique
    private static final String NBT_RADIATION_EXPOSURE = "radiationExposure";

    /**
     * How far a placed radiation source reaches. Kept deliberately short: this is the single biggest lever on the cost
     * of the proximity scan, which grows with the CUBE of this number.
     */
    @Unique
    private static final int BLOCK_SCAN_RADIUS = 3;

    /** Ticks between proximity scans. The result is cached and re-applied every tick in between. */
    @Unique
    private static final int BLOCK_SCAN_INTERVAL_TICKS = 20;

    /** Cached strength of nearby placed sources, refreshed by the scan and re-marked every tick until then. */
    @Unique
    private int avp_human$nearbyBlockRate = 0;

    /** Accumulated exposure, persisted. See {@link RadiationLevel} for the units. */
    @Unique
    private int avp_human$radiationExposure = 0;

    /**
     * The strongest source rate marked this tick, consumed and cleared every tick. Deliberately NOT persisted - sources
     * re-mark themselves while they are present, so a reload simply re-detects them.
     */
    @Unique
    private int avp_human$radiationSourceRate = 0;

    protected MixinLivingEntity_RadiationDamage(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public int avp_human$getRadiationExposure() {
        return avp_human$radiationExposure;
    }

    @Override
    public void avp_human$setRadiationExposure(int exposure) {
        avp_human$radiationExposure = Math.max(0, Math.min(RadiationLevel.MAX_EXPOSURE, exposure));
    }

    @Override
    public void avp_human$addRadiationExposure(int amount) {
        avp_human$setRadiationExposure(avp_human$radiationExposure + amount);
    }

    @Override
    public void avp_human$markRadiationSource(int rate) {
        avp_human$radiationSourceRate = Math.max(avp_human$radiationSourceRate, rate);
    }

    @Inject(at = @At("TAIL"), method = "addAdditionalSaveData")
    private void avp_human$saveRadiationData(CompoundTag tag, CallbackInfo ci) {
        if (avp_human$radiationExposure > 0) {
            tag.putInt(NBT_RADIATION_EXPOSURE, avp_human$radiationExposure);
        }
    }

    @Inject(at = @At("TAIL"), method = "readAdditionalSaveData")
    private void avp_human$loadRadiationData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(NBT_RADIATION_EXPOSURE)) {
            avp_human$radiationExposure = tag.getInt(NBT_RADIATION_EXPOSURE);
        }
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void avp_human$tickRadiation(CallbackInfo ci) {
        var self = LivingEntity.class.cast(this);

        if (self.level().isClientSide()) {
            return;
        }

        // Placed sources: rescan occasionally and re-apply the cached answer in between, so standing beside hot
        // blocks irradiates you without a per-tick block search.
        if (self instanceof Player) {
            if (self.tickCount % BLOCK_SCAN_INTERVAL_TICKS == 0) {
                avp_human$nearbyBlockRate = avp_human$scanNearbyRadioactiveBlocks(self);
            }

            if (avp_human$nearbyBlockRate > 0) {
                avp_human$markRadiationSource(avp_human$nearbyBlockRate);
            }
        }

        // Consume this tick's source marks whatever happens next, so a stale flag can never carry over.
        var sourceRate = avp_human$radiationSourceRate;
        avp_human$radiationSourceRate = 0;

        // Entities that simply do not get irradiated (xenomorphs, undead, the invulnerable) shed any contamination
        // outright rather than decaying it - they were never really carrying it.
        if (self.getType().is(HumanEntityTypeTags.RADIATION_RESISTANT) || BLibEntityPredicates.isInvulnerable(self)) {
            if (avp_human$radiationExposure > 0) {
                avp_human$radiationExposure = 0;
                self.removeEffect(HumanMobEffects.getRadiationHolder());
            }
            return;
        }

        // A full radiation-resistant suit stops exposure CLIMBING; it is protection, not decontamination, so
        // anything already accumulated still has to decay off the wearer.
        if (sourceRate > 0 && HumanPredicates.canBeIrradiated(self)) {
            avp_human$radiationExposure = Math.min(
                RadiationLevel.MAX_EXPOSURE,
                avp_human$radiationExposure + RadiationLevel.GAIN_PER_TICK * sourceRate
            );
        } else if (avp_human$radiationExposure > 0) {
            avp_human$radiationExposure = Math.max(0, avp_human$radiationExposure - RadiationLevel.DECAY_PER_TICK);
        }

        // ABSORB foreign doses. Anything that hands out the radiation effect directly - a grenade's gas cloud, a
        // command, another mod - is treated as contamination to at least that level, so every path feeds the one
        // counter instead of fighting it. Self-applied readouts absorb to no more than they already are.
        var existing = self.getEffect(HumanMobEffects.getRadiationHolder());

        if (existing != null) {
            avp_human$radiationExposure = Math.max(
                avp_human$radiationExposure,
                Math.min(RadiationLevel.MAX_EXPOSURE, existing.getAmplifier() * RadiationLevel.UNITS_PER_LEVEL + 1)
            );
        }

        var radiationLevel = RadiationLevel.byExposure(avp_human$radiationExposure);

        if (radiationLevel == RadiationLevel.NONE) {
            if (self.hasEffect(HumanMobEffects.getRadiationHolder())) {
                self.removeEffect(HumanMobEffects.getRadiationHolder());
            }
            return;
        }

        avp_human$syncRadiationEffect(self, radiationLevel);
        radiationLevel.applySideEffects(self);

        if (radiationLevel.isHarmful() && self.tickCount % radiationLevel.damageIntervalTicks() == 0) {
            self.hurt(RadiationStatusEffect.createRadiationDamageSource(self), radiationLevel.damage());
        }
    }

    /**
     * The strength of placed radiation sources around the entity, or zero. Cost is contained deliberately, because an
     * earlier attempt at proximity radiation cost TPS:
     * <ul>
     * <li><b>Players only.</b> Mobs never scan - radiation sickness in a wandering cow changes nothing a player can
     * observe, and mob counts are what make per-entity scans expensive.</li>
     * <li><b>Once a second, not every tick</b> ({@link #BLOCK_SCAN_INTERVAL_TICKS}), with the answer cached.</li>
     * <li><b>A short radius</b> ({@link #BLOCK_SCAN_RADIUS}) - the volume grows with its cube.</li>
     * <li><b>Early exit</b> the moment the strongest possible source is found, which is the common case inside an
     * irradiated hive or a uranium store.</li>
     * </ul>
     * <p>
     * That is a few hundred cached block-state reads per player per second - far below what vanilla's own random
     * ticking does - and the two constants above are the dials if it ever needs trimming further.
     * </p>
     */
    @Unique
    private int avp_human$scanNearbyRadioactiveBlocks(LivingEntity self) {
        var level = self.level();
        var origin = self.blockPosition();
        var strongest = 0;

        for (
            var pos : BlockPos.betweenClosed(
                origin.offset(-BLOCK_SCAN_RADIUS, -BLOCK_SCAN_RADIUS, -BLOCK_SCAN_RADIUS),
                origin.offset(BLOCK_SCAN_RADIUS, BLOCK_SCAN_RADIUS, BLOCK_SCAN_RADIUS)
            )
        ) {
            var state = level.getBlockState(pos);

            if (state.is(HumanBlockTags.HIGHLY_RADIOACTIVE_BLOCKS)) {
                return 4;
            }

            if (strongest == 0 && state.is(HumanBlockTags.RADIOACTIVE_BLOCKS)) {
                strongest = 2;
            }
        }

        return strongest;
    }

    /**
     * Keeps the visible effect in step with the exposure level. The effect is a READOUT of the counter, not the thing
     * driving it, so it is re-applied whenever the level changes or the display would otherwise lapse.
     */
    @Unique
    private void avp_human$syncRadiationEffect(LivingEntity self, RadiationLevel radiationLevel) {
        var holder = HumanMobEffects.getRadiationHolder();
        var current = self.getEffect(holder);

        if (current != null && current.getAmplifier() == radiationLevel.amplifier() && current.getDuration() > 40) {
            return;
        }

        self.addEffect(new MobEffectInstance(holder, 100, radiationLevel.amplifier(), true, false, true));
    }
}
