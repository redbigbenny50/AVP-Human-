package com.human.mixin;

import com.blib.api.common.entity.v1.BLibEntityPredicates;
import com.human.common.registry.tag.HumanItemTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hazards the sealed suits shrug off that no armor tag can express. [stated] Sep 22: "mk50 and pressure suit should
 * protect from acid rain."
 * <p>
 * Ad Astra's acid rain is gated by ENTITY-type tags only ({@code can_survive_in_acid_rain} / {@code
 * can_survive_in_space}) and then dealt as its own {@code ad_astra:acid_rain} damage through {@code hurt} - read from
 * the 1.16.26 {@code LivingEntityMixin}; there is no armor tag for it the way there is for oxygen, heat and cold. So
 * the answer is at the damage: with a full MK50 or pressure set on, that damage type is refused here, inside
 * {@code isInvulnerableTo}, where vanilla refuses fire for fire-immune things - before any hurt sound or knockback.
 * <p>
 * Matched by damage-type KEY, so Ad Astra is not needed at compile time for this and an absent mod never matches. The
 * oxygen, heat and cold protection of the same suits goes through Ad Astra's own item tags (see the item tag provider)
 * and never reaches here.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity_SuitHazardImmunity {

    @Unique
    private static final ResourceKey<DamageType> AVP_HUMAN$ACID_RAIN = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath("ad_astra", "acid_rain")
    );

    /**
     * Oct 7 - [stated] "our suits the mk50 and pressure suit should protect from black goo and spores." Gigeresque's
     * black goo works through its {@code gigeresque:dna_disintegration} effect and its spores through
     * {@code gigeresque:neo_spore} (ids read from the 0.8.16 jar); every source - the fluid, goo blobs, ampoules, spore
     * blocks, poppers - ends in addEffect or forceAddEffect, and both ask canBeAffected first. Refusing the two effects
     * here covers all of them. Matched by registry id, so Gigeresque is not needed at compile time.
     */
    @Unique
    private static final ResourceLocation AVP_HUMAN$GIG_BLACK_GOO = ResourceLocation.fromNamespaceAndPath(
        "gigeresque",
        "dna_disintegration"
    );

    @Unique
    private static final ResourceLocation AVP_HUMAN$GIG_SPORE = ResourceLocation.fromNamespaceAndPath("gigeresque", "neo_spore");

    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void avp_human$refuseGooAndSporesInSealedSuit(
        net.minecraft.world.effect.MobEffectInstance effectInstance,
        CallbackInfoReturnable<Boolean> cir
    ) {
        var id = effectInstance.getEffect().unwrapKey().map(ResourceKey::location).orElse(null);

        if (!AVP_HUMAN$GIG_BLACK_GOO.equals(id) && !AVP_HUMAN$GIG_SPORE.equals(id)) {
            return;
        }

        if (avp_human$inSealedSuit((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean avp_human$inSealedSuit(LivingEntity self) {
        return BLibEntityPredicates.hasFullArmorSetMatching(self, itemStack -> itemStack.is(HumanItemTags.MK50_ARMOR))
            || BLibEntityPredicates.hasFullArmorSetMatching(self, itemStack -> itemStack.is(HumanItemTags.PRESSURE_ARMOR));
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void avp_human$refuseAcidRainInSealedSuit(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!source.typeHolder().unwrapKey().map(AVP_HUMAN$ACID_RAIN::equals).orElse(false)) {
            return;
        }

        var self = (LivingEntity) (Object) this;

        if (
            BLibEntityPredicates.hasFullArmorSetMatching(self, itemStack -> itemStack.is(HumanItemTags.MK50_ARMOR))
                || BLibEntityPredicates.hasFullArmorSetMatching(self, itemStack -> itemStack.is(HumanItemTags.PRESSURE_ARMOR))
        ) {
            cir.setReturnValue(true);
        }
    }
}
