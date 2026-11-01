package com.human.mixin;

import com.human.common.gameplay.gene.Genes;
import com.human.common.util.GeneImmunity;
import com.human.common.util.GeneValues;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gene effects that hook vanilla living-entity behaviour (genetics phase 2, Oct 27):
 * <ul>
 * <li><b>Armor Penetration</b> - the attacker's hits ignore part of the target's armour AND toughness, like Breach (cap
 * 60%). It never touches resistance genes.</li>
 * <li><b>Stealth</b> - mobs notice the carrier later: its visibility to them drops by up to half at 100%. (Silent
 * footsteps at 100% are in MixinEntity_GeneStealth.)</li>
 * <li><b>Forager</b> - a kill by a carrier has a chance (the gene's value) to roll the victim's loot table again.</li>
 * <li><b>Radiation / Toxin Resistance at 100%</b> - the matching effects can't be applied at all (radiation and
 * radiation sickness; poison and wither), the same way fire/cold/acid immunity work.</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity_GeneEffects {

    @Unique
    private static final ResourceLocation AVP_HUMAN$RADIATION = ResourceLocation.fromNamespaceAndPath("avp_human", "radiation");

    @Unique
    private static final ResourceLocation AVP_HUMAN$RADIATION_SICKNESS = ResourceLocation.fromNamespaceAndPath(
        "avp_alien",
        "radiation_sickness"
    );

    /** Re-entry guard: the second Forager roll must not roll a third. */
    @Unique
    private boolean avp_human$foragerRolling;

    @Shadow
    protected abstract void dropFromLootTable(DamageSource damageSource, boolean hitByPlayer);

    @Shadow
    protected boolean dead;

    @ModifyArg(
        method = "getDamageAfterArmorAbsorb",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"
        ),
        index = 3
    )
    private float avp_human$armorPenetrationArmor(
        LivingEntity target,
        float damage,
        DamageSource damageSource,
        float armor,
        float toughness
    ) {
        return armor * avp_human$armorKept(damageSource);
    }

    @ModifyArg(
        method = "getDamageAfterArmorAbsorb",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"
        ),
        index = 4
    )
    private float avp_human$armorPenetrationToughness(
        LivingEntity target,
        float damage,
        DamageSource damageSource,
        float armor,
        float toughness
    ) {
        return toughness * avp_human$armorKept(damageSource);
    }

    @Unique
    private static float avp_human$armorKept(DamageSource damageSource) {
        var penetration = GeneValues.get(damageSource.getEntity(), Genes.ARMOR_PENETRATION);

        if (penetration <= 0) {
            return 1.0F;
        }

        return (float) (1.0 - Math.min(penetration, Genes.cap(Genes.ARMOR_PENETRATION)));
    }

    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void avp_human$stealth(@Nullable Entity lookingEntity, CallbackInfoReturnable<Double> cir) {
        var self = (LivingEntity) (Object) this;
        var stealth = GeneValues.get(self, Genes.STEALTH);
        var visibility = cir.getReturnValue();

        if (stealth > 0) {
            visibility *= 1.0 - 0.5 * Math.min(stealth, 1.0);
        }

        // Ambusher (advanced): nothing notices the carrier until it is close.
        if (
            lookingEntity != null
                && self.distanceToSqr(lookingEntity) > AVP_HUMAN$AMBUSH_RANGE_SQR
                && GeneValues.hasAdvanced(self, Genes.AMBUSHER)
        ) {
            visibility *= 0.2;
        }

        if (visibility != cir.getReturnValue()) {
            cir.setReturnValue(visibility);
        }
    }

    @Unique
    private static final double AVP_HUMAN$AMBUSH_RANGE_SQR = 5.0 * 5.0;

    /** Berserker (advanced): up to +50% damage dealt as the carrier's health runs out. */
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float avp_human$berserker(float damage, DamageSource damageSource) {
        if (damageSource.getEntity() instanceof LivingEntity attacker && GeneValues.hasAdvanced(attacker, Genes.BERSERKER)) {
            var missing = 1.0F - attacker.getHealth() / Math.max(1.0F, attacker.getMaxHealth());
            return damage * (1.0F + 0.5F * Math.max(0.0F, missing));
        }
        return damage;
    }

    /** Hemophage (advanced): the attacker heals a fifth of the damage it deals. */
    @Inject(method = "hurt", at = @At("RETURN"))
    private void avp_human$hemophage(DamageSource damageSource, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (
            cir.getReturnValue()
                && damageSource.getEntity() instanceof LivingEntity attacker
                && attacker != (Object) this
                && GeneValues.hasAdvanced(attacker, Genes.HEMOPHAGE)
        ) {
            attacker.heal(damage * 0.2F);
        }
    }

    /**
     * Climbing (advanced, players): climbs walls like a spider - pushing into a wall counts as being on a ladder. Read
     * from the synced flags so the player's own client (which runs its movement) and the server agree.
     */
    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void avp_human$climbing(CallbackInfoReturnable<Boolean> cir) {
        var self = (LivingEntity) (Object) this;

        if (
            !cir.getReturnValue() && self.horizontalCollision && self instanceof net.minecraft.world.entity.player.Player
                && com.human.common.registry.init.HumanGeneSync.climbing(com.human.common.registry.init.HumanGeneSync.flags(self))
        ) {
            cir.setReturnValue(true);
        }
    }

    /** Amphibious (advanced): breathes underwater. */
    @Inject(method = "canBreatheUnderwater", at = @At("RETURN"), cancellable = true)
    private void avp_human$amphibious(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && GeneValues.hasAdvanced((Entity) (Object) this, Genes.AMPHIBIOUS)) {
            cir.setReturnValue(true);
        }
    }

    /** Haemophilia (defect): harmful effects last half again as long. */
    @ModifyVariable(
        method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("HEAD"),
        argsOnly = true
    )
    private MobEffectInstance avp_human$haemophilia(MobEffectInstance instance) {
        if (
            instance.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL
                && !instance.isInfiniteDuration()
                && GeneValues.carriesDefect((Entity) (Object) this, Genes.HAEMOPHILIA)
        ) {
            return new MobEffectInstance(
                instance.getEffect(),
                (int) (instance.getDuration() * 1.5),
                instance.getAmplifier(),
                instance.isAmbient(),
                instance.isVisible(),
                instance.showIcon()
            );
        }
        return instance;
    }

    /** Short-Lived (defect): every source of healing heals half as much. */
    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float avp_human$shortLived(float amount) {
        if (amount > 0 && GeneValues.carriesDefect((Entity) (Object) this, Genes.SHORT_LIVED)) {
            return amount * 0.5F;
        }
        return amount;
    }

    /** Whether this mob was already dead when die() was entered - so the splash fires once, on the real death. */
    @Unique
    private boolean avp_human$wasDeadBeforeDie;

    @Inject(method = "die", at = @At("HEAD"))
    private void avp_human$markDeathStart(DamageSource damageSource, CallbackInfo ci) {
        avp_human$wasDeadBeforeDie = dead;
    }

    /**
     * Volatile Acid (advanced): bursts in an acid splash when it dies. At the END of die(), and only if die() is what
     * killed it: a death cancelled by another mod (NeoForge's death event returns early, before the end) never
     * splashes, and a second die() call on an already-dead mob doesn't splash twice.
     */
    @Inject(method = "die", at = @At("TAIL"))
    private void avp_human$volatileAcid(DamageSource damageSource, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;

        if (!self.level().isClientSide && !avp_human$wasDeadBeforeDie && dead && GeneValues.hasAdvanced(self, Genes.VOLATILE_ACID)) {
            com.human.common.util.GeneAcidSplash.splash(self);
        }
    }

    @Inject(method = "dropFromLootTable", at = @At("TAIL"))
    private void avp_human$forager(DamageSource damageSource, boolean hitByPlayer, CallbackInfo ci) {
        if (avp_human$foragerRolling) {
            return;
        }

        var forager = GeneValues.get(damageSource.getEntity(), Genes.FORAGER);
        var self = (LivingEntity) (Object) this;

        if (forager > 0 && self.getRandom().nextDouble() < forager) {
            avp_human$foragerRolling = true;
            try {
                dropFromLootTable(damageSource, hitByPlayer);
            } finally {
                avp_human$foragerRolling = false;
            }
        }
    }

    /**
     * Loot genes: extra drops at death, right after the mob's own loot - so they obey doMobLoot exactly as its normal
     * drops do, and on NeoForge they ride the same living-drops capture as everything else.
     */
    @Inject(
        method = "dropAllDeathLoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;dropCustomDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V",
            shift = At.Shift.AFTER
        )
    )
    private void avp_human$lootGenes(net.minecraft.server.level.ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        com.human.common.gameplay.gene.LootGenes.dropExtra((LivingEntity) (Object) this);
    }

    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void avp_human$geneEffectImmunity(MobEffectInstance effectInstance, CallbackInfoReturnable<Boolean> cir) {
        var self = (Entity) (Object) this;

        if (GeneValues.hasNoGenes(self)) {
            return;
        }

        if (effectInstance.is(MobEffects.POISON) || effectInstance.is(MobEffects.WITHER)) {
            if (GeneImmunity.toxinImmune(self)) {
                cir.setReturnValue(false);
            }
            return;
        }

        var id = BuiltInRegistries.MOB_EFFECT.getKey(effectInstance.getEffect().value());

        if ((AVP_HUMAN$RADIATION.equals(id) || AVP_HUMAN$RADIATION_SICKNESS.equals(id)) && GeneImmunity.radiationImmune(self)) {
            cir.setReturnValue(false);
        }
    }
}
