package com.human.common.gameplay.freeze;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Frozen solid (see {@link FrozenSolid}). Each tick: no sideways motion unless a lead, capture cord or capture chain is
 * pulling it — it falls if it is in the air, but it does not slide on its own — and fire thaws it: [stated] "frozen
 * mobs stay frozen 5 minutes unless lit on fire". Returning false from the tick is how a 1.21 effect removes itself.
 */
public class FrozenSolidStatusEffect extends MobEffect {

    private static final int ICE_BLUE = 0x9FE6FF;

    public FrozenSolidStatusEffect() {
        super(MobEffectCategory.HARMFUL, ICE_BLUE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity.isOnFire()) {
            entity.setTicksFrozen(0);
            return false;
        }

        // ⚠ Pinned in place ONLY when nothing is pulling it. A lead, the capture cord and the capture chain all drag by
        // adding to the mob's motion, so pinning it here every tick cancelled every pull and a frozen mob could not be
        // moved at all. [stated] Oct 4: dragging a frozen mob on any of the three is "another way of capturing them and
        // transporting". It still makes no move of its own — its AI is stopped (MixinMob_FrozenSolid).
        if (!FrozenSolid.isTethered(entity)) {
            var motion = entity.getDeltaMovement();
            entity.setDeltaMovement(0.0, Math.min(0.0, motion.y), 0.0);
        }

        // A hostile on a lead only because it is frozen: remembered, so the lead lets go when it thaws.
        if (entity instanceof net.minecraft.world.entity.Mob mob && mob.isLeashed() && !mob.canBeLeashed()) {
            mob.addTag(FrozenSolid.FROZEN_LEASH_TAG);
        }

        FrozenSolid.track(entity);

        return true;
    }
}
