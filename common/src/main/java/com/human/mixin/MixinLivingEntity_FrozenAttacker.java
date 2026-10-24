package com.human.mixin;

import com.human.common.gameplay.freeze.FrozenSolid;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [stated] Oct 19 (tester): mobs frozen solid could still attack. A statue deals no damage.
 * <p>
 * Freezing solid switches a mob's AI step off ({@code MixinMob_FrozenSolid}), which stops vanilla goals, targeting,
 * pathfinding and BLib's GOAP. But some attacks never go through the AI step - avp_alien's xenomorph attack executors
 * run from the creature's own tick, for one - so the swing still landed. Rather than chase every attack system in every
 * mod, the damage itself is refused here: any hit whose ATTACKER (the source's causing entity) is frozen solid does
 * nothing. That covers melee, an acid spit or arrow already in the air when it froze, and an explosion it set off, in
 * every mod at once, with no dependency on any of them.
 * </p>
 * ⚡ One effect lookup on the attacker, only for hits that have one.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity_FrozenAttacker {

    /**
     * [stated] Oct 19 - and the other way round: a frozen mob that takes a real hit or a heavy fall breaks out of the
     * ice ({@link FrozenSolid#onHurt}). Only when the damage actually landed (hurt returned true), so a blocked hit or
     * one during its invulnerability frames does not count.
     */
    @Inject(method = "hurt", at = @At("RETURN"))
    private void avp_human$hitBreaksIce(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            FrozenSolid.onHurt((LivingEntity) (Object) this, source, amount);
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void avp_human$frozenAttackerDealsNothing(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getEntity() instanceof LivingEntity attacker && attacker != (Object) this && FrozenSolid.isFrozen(attacker)) {
            cir.setReturnValue(false);
            return;
        }

        // [stated] Oct 19 - "Distributors cryomist kills frozen xenos possibly other mobs". A mob frozen solid takes no
        // more cold damage of any kind: it is already as frozen as it gets. Cryofluid, cryo vapor (the distributor
        // fog), vanilla's freeze damage (which kept ticking because the fog holds its frost meter full) and the freeze
        // grenade's blast all stopped here - so a frozen specimen can sit in the fog indefinitely and be collected
        // alive. Everything else still hurts it, and a real hit can still break it out.
        if (FrozenSolid.isColdDamage(source) && FrozenSolid.isFrozen((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
