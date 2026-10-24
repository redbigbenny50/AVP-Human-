package com.human.mixin;

import com.human.common.model.RadiationExposure;
import com.human.common.registry.key.HumanBiomeKeys;
import com.human.util.HumanPredicates;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity_NukedRadiation extends Entity {

    public MixinLivingEntity_NukedRadiation(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /** Whether this entity stood in the nuked biome at its last check - refreshed once a second, see {@link #tick}. */
    @Unique
    private boolean avp_human$inNukedBiome;

    /**
     * ⚡ Runs for EVERY living entity, every tick. Oct 13 audit: it used to run on both sides and check all four armour
     * slots and the biome every tick, although almost nothing is ever in the nuked biome and radiation is applied on
     * the server only. Now: server only; the biome once a second per entity (staggered by id); armour only for the few
     * that are actually in fallout. Same result - a mark per tick while standing in the biome.
     */
    @Inject(at = @At("HEAD"), method = "tick")
    public void tick(CallbackInfo callbackInfo) {
        var self = LivingEntity.class.cast(this);

        if (self.level().isClientSide()) {
            return;
        }

        if ((self.tickCount + self.getId()) % 20 == 0) {
            avp_human$inNukedBiome = isIsEntityInAnIrradiatedBiome(self);
        }

        if (!avp_human$inNukedBiome || !HumanPredicates.canBeIrradiated(self)) {
            return;
        }

        // Standing in the fallout is a STRONGER source than carrying something hot: it drives the sickness level
        // up twice as fast, so an irradiated biome is somewhere you visit deliberately and briefly.
        ((RadiationExposure) self).avp_human$markRadiationSource(2);
    }

    @Unique
    private boolean isIsEntityInAnIrradiatedBiome(LivingEntity self) {
        return self.level().getBiome(self.blockPosition()).is(HumanBiomeKeys.NUKED_BIOME);
    }
}
