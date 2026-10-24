package com.human.common.gameplay.vapor;

import com.human.common.gameplay.acid.AcidEffects;
import com.human.common.gameplay.fluid.CryoFreezing;
import com.human.common.gameplay.freeze.FrozenSolid;
import com.human.common.registry.init.HumanMobEffects;
import com.human.common.registry.key.HumanDamageTypeKeys;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * ⭐ THE ONE PLACE EACH VAPOR'S EFFECT LIVES. The distributor's fog, the chem thrower's blast and its lingering cloud
 * all call {@link #apply}.
 * <h2>[stated] Oct 18 - "just like their liquids"</h2> "the only difference is it doesnt sit on a block its the blocks
 * the vapor is touching. lava vapor basically does lava damage as a mist." So each vapor does what its liquid does to
 * whatever stands in it, applied to whatever stands in the vapor:
 * <ul>
 * <li><b>CRYO</b> - cryofluid ({@link CryoFreezing}): vanilla's freeze meter climbs, freezing damage once fully frozen,
 * frozen solid by the shared rule, and it puts out anyone on fire ({@link FrozenSolid} - a hostile at 10 % health, any
 * passive mob). A full cryo suit or the cryofluid_immune tag protects. ⭐ From the CHEM THROWER it is the concentrated
 * cryomist [stated] Oct 18: it damages straight away instead of waiting for the meter, and on a mob that is ALREADY
 * frozen solid it does no damage and only resets the thaw timer to the full 5 minutes.</li>
 * <li><b>INFERNIUM</b> - lava: set alight for 15 s and lava's 4 damage. Fire-immune mobs, and anyone in a full MK50
 * suit, are not touched.</li>
 * <li><b>ACIDIUM</b> - xeno acid ({@link AcidEffects}): 1 acid damage every half second.</li>
 * <li><b>NETHER ACIDIUM</b> - the same, and it sets them alight for 5 s.</li>
 * <li><b>IRRADIUM</b> - [stated] Oct 18: "whatever freezing cryo does irradium does too it also deals radiation
 * damage". So it freezes exactly as CRYO does from the same machine (cryofluid's rules from the distributor, the
 * cryomist's from the chem thrower - frozen solid, thaw reset and all), and irradiates on top (Radiation, 10 s, topped
 * up while they stay in it; the effect deals the damage). Never burns.</li>
 * </ul>
 * The acid immunities carry over: avp_alien's acid_immune mobs, and acid-immune boots (chitin, the predator suit).
 * <h2>[stated] Oct 18 - VAPOR IGNORES ARMOUR</h2> A tester found the queen all but immune to cryomist: 2 damage per hit
 * against her 16 armour / 20 toughness landed about 0.74, so she lost ~1.5 health a second. He chose to have the vapor
 * ignore armour ("the mist reaches the whole body", the same reason ordinary boots don't soak it) and the gun's
 * cryomist to hit for one more. So each vapor has its OWN damage type, all three in {@code minecraft:bypasses_armor}:
 * {@code avp_human:cryo_vapor} (cryo, and irradium's freeze), {@code avp_human:infernium_vapor} (also {@code is_fire},
 * so fire resistance still protects) and {@code avp_human:acid_vapor} (both acidiums). The LIQUIDS keep their own types
 * and still respect armour. Protection enchantments and Resistance still count - only armour points and toughness are
 * skipped.
 * <h2>⚠ WHERE A MIST DIFFERS FROM A PUDDLE - my calls, flagged to him</h2>
 * <ul>
 * <li>Liquid acid lets ordinary BOOTS soak the hit (they wear down instead). A mist reaches the whole body, so here
 * ordinary boots do NOT soak it - otherwise an acid trap would never hurt anyone wearing shoes. Acid-IMMUNE boots still
 * protect.</li>
 * <li>Thin vapor - the fading edge of the fog or the cone - catches less often: each application lands with a chance
 * equal to the vapor's strength there, so the edges are patchy and the middle is certain.</li>
 * <li>BLOCKS: [stated] Oct 18 - only the chem thrower's vapor works on blocks, in {@link VaporBlockEffects}; the
 * distributor's fog never does.</li>
 * </ul>
 * <h2>CADENCE</h2> Callers do not run every tick (the fog every 10 ticks, the cloud every 5, the blast every tick), so
 * each call says how many ticks it stands for and the per-tick parts (the freeze meters) are scaled to match. Damage is
 * gated by its own interval and by vanilla's hurt cooldown, so it lands at the liquid's rate either way.
 */
public final class VaporEffects {

    /** Who released the vapor - the cryomist rule only applies to the gun. */
    public enum Origin {
        CHEM_THROWER,
        DISTRIBUTOR
    }

    /** [stated] lava's own numbers. */
    private static final float LAVA_DAMAGE = 4.0F;

    private static final float LAVA_IGNITE_SECONDS = 15.0F;

    /** The gun's cryomist: frost added each tick of spraying, so a sprayed player visibly ices over. */
    private static final int CRYOMIST_FROST_PER_TICK = 6;

    /**
     * [stated] Oct 18 - the chem thrower's cryomist hits for cryofluid's damage "plus 1 more": 3 at the cone's
     * thickest. The distributor's fog keeps cryofluid's own 2.
     */
    private static final float CRYOMIST_DAMAGE = CryoFreezing.DAMAGE + 1.0F;

    private static final TagKey<Item> ACID_IMMUNE_ITEMS = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("avp_alien", "acid_immune")
    );

    private static final TagKey<EntityType<?>> ACID_IMMUNE_ENTITIES = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("avp_alien", "acid_immune")
    );

    private VaporEffects() {}

    /**
     * @param level    the server level
     * @param kind     which vapor
     * @param target   the living thing in it
     * @param strength 0-1 - how strong the vapor is where the target is
     * @param source   who released it (the chem thrower's shooter), or null for a distributor
     * @param origin   which machine
     * @param ticks    how many ticks this call stands for (1 for every tick)
     */
    public static void apply(
        ServerLevel level,
        VaporKind kind,
        LivingEntity target,
        float strength,
        @Nullable Entity source,
        Origin origin,
        int ticks
    ) {
        apply(level, kind, target, strength, source, origin, ticks, 1.0F);
    }

    /**
     * As above, with a POTENCY: [stated] Oct 18 - the chem thrower's POWER enchantment makes its vapor stronger, 25 % a
     * level ({@code ChemCone.potency}). It multiplies everything the vapor does here - the damage, and how fast the
     * freeze meter climbs. 1 for a distributor.
     */
    public static void apply(
        ServerLevel level,
        VaporKind kind,
        LivingEntity target,
        float strength,
        @Nullable Entity source,
        Origin origin,
        int ticks,
        float potency
    ) {
        if (strength <= 0.0F || !target.isAlive() || target == source || target.isSpectator()) {
            return;
        }

        if (target instanceof Player player && player.isCreative()) {
            return;
        }

        // [stated] Oct 18: "whatever freezing cryo does irradium does too it also deals radiation damage".
        var freezes = kind == VaporKind.CRYO || kind == VaporKind.IRRADIUM;

        // ⚠⚠ Oct 19 (his report: "Irradiated vapor doesnt seem to freeze mobs"). Irradium used to stop here for
        // anything
        // ACID-immune - carried over from the irradiated acid LIQUID, which is an acid. But [stated] irradium is cryo
        // that also irradiates, not an acid: the gate meant it never froze a single xenomorph (every alien is in
        // avp_alien's acid_immune tag) or anyone in chitin or predator boots. It now freezes exactly as cryo does; the
        // cryo immunities (irradiated xenos, cryo suits) still apply through FrozenSolid.isImmune, and who the
        // radiation reaches is still decided by the effect itself (aliens refuse it unless aberrant; the radiation
        // suit blocks it).

        // [stated] Oct 19 - eggs: cryo freezes them solid at once (nether eggs die) and deals them no damage, since a
        // hurt egg opens. Any cryo - cryomist, the distributor's fog, irradium's freeze.
        // An egg that is ALREADY frozen goes the usual way below instead: its thaw clock is reset, and - [stated] - a
        // frozen nether egg is not killed by having its clock reset.
        if (
            freezes
                && !com.human.common.gameplay.freeze.FrozenSolid.isFrozen(target)
                && com.human.common.gameplay.freeze.FrozenSolid.reactsOnContact(target)
        ) {
            com.human.common.gameplay.freeze.FrozenSolid.tryFreeze(target);

            if (kind == VaporKind.IRRADIUM) {
                irradiate(target, ticks);
            }

            return;
        }

        if (freezes && origin == Origin.CHEM_THROWER) {
            // The gun's concentrated freeze: straight damage, freeze solid, thaw reset - never thinned by chance.
            cryomist(level, target, strength, source, ticks, potency);

            if (kind == VaporKind.IRRADIUM) {
                irradiate(target, ticks);
            }

            return;
        }

        // Thin vapor catches less often - patchy edges, a certain middle.
        if (level.random.nextFloat() >= strength) {
            return;
        }

        switch (kind) {
            case CRYO -> cryofluid(level, target, source, ticks, potency);
            case INFERNIUM -> lava(level, target, source, potency);
            case ACIDIUM -> acid(level, target, source, ticks, false, potency);
            case NETHER_ACIDIUM -> acid(level, target, source, ticks, true, potency);
            case IRRADIUM -> {
                cryofluid(level, target, source, ticks, potency);
                irradiate(target, ticks);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Cryo
    // ------------------------------------------------------------------------------------------------------------

    /** Cryofluid, rule for rule (CryoFreezing.tick), scaled to the ticks this call stands for. */
    private static void cryofluid(ServerLevel level, LivingEntity target, @Nullable Entity source, int ticks, float potency) {
        var damage = damageSource(level, HumanDamageTypeKeys.CRYO_VAPOR, source);

        // ⚠ FrozenSolid.isImmune, not just the vapor's own type: the alien strain rules (irradiated xenos) refuse
        // CRYOFLUID damage, and that is how their cryo immunity reaches us. The vapor now has its own damage type, so
        // asking only about that would quietly have stripped their immunity.
        if (FrozenSolid.isImmune(target) || target.isInvulnerableTo(damage)) {
            return;
        }

        // [stated] Oct 19 - already frozen solid: the fog only keeps it frozen (the thaw timer back to full), as the
        // chem thrower's cryomist does - never more damage, which used to grind frozen specimens to death.
        if (FrozenSolid.isFrozen(target)) {
            FrozenSolid.keepFrozen(target);
            return;
        }

        // Cryofluid puts fires out, on anyone it touches too.
        target.clearFire();

        var add = Math.round(CryoFreezing.FREEZE_PER_TICK * ticks * potency);
        var required = target.getTicksRequiredToFreeze();
        target.setTicksFrozen(Math.min(required + CryoFreezing.FREEZE_PER_TICK, target.getTicksFrozen() + add));

        if (target.isFullyFrozen() && dueThisCall(target, CryoFreezing.DAMAGE_INTERVAL_TICKS, ticks)) {
            target.hurt(damage, CryoFreezing.DAMAGE * potency);
        }

        if (target.isFullyFrozen()) {
            FrozenSolid.tryFreeze(target);
        }
    }

    /** [stated] Oct 18 - the chem thrower's cryomist. */
    private static void cryomist(
        ServerLevel level,
        LivingEntity target,
        float strength,
        @Nullable Entity source,
        int ticks,
        float potency
    ) {
        if (FrozenSolid.isFrozen(target)) {
            // [stated] no more damage - only the thaw timer goes back to full.
            FrozenSolid.keepFrozen(target);
            return;
        }

        target.clearFire();

        if (FrozenSolid.isImmune(target)) {
            return;
        }

        if (target.canFreeze()) {
            target.setTicksFrozen(
                Math.min(
                    target.getTicksRequiredToFreeze() + 20,
                    target.getTicksFrozen() + Math.round(CRYOMIST_FROST_PER_TICK * ticks * potency)
                )
            );
        }

        // Cryofluid's damage, scaled by the blast's strength; vanilla's hurt cooldown keeps it to two hits a second.
        target.hurt(damageSource(level, HumanDamageTypeKeys.CRYO_VAPOR, source), CRYOMIST_DAMAGE * strength * potency);
        FrozenSolid.tryFreeze(target);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Infernium
    // ------------------------------------------------------------------------------------------------------------

    /** [stated] "lava vapor basically does lava damage as a mist" - vanilla's lavaHurt, credited to the shooter. */
    private static void lava(ServerLevel level, LivingEntity target, @Nullable Entity source, float potency) {
        if (target.fireImmune() || inFullMk50(target)) {
            return;
        }

        target.igniteForSeconds(LAVA_IGNITE_SECONDS);
        target.hurt(damageSource(level, HumanDamageTypeKeys.INFERNIUM_VAPOR, source), LAVA_DAMAGE * potency);
    }

    /**
     * [stated] Oct 19 - "the mk50 suit should protect a user from the cryo and infernium vapors too". A full sealed
     * MK50 set: infernium vapor neither sets the wearer alight nor burns them. (Cryo and irradium's freeze were already
     * refused for it: the MK50 is in {@code cryo_protective_armor}, which FrozenSolid.isImmune reads.) Only the vapor -
     * ordinary fire and lava still burn.
     */
    private static boolean inFullMk50(LivingEntity target) {
        return com.blib.api.common.entity.v1.BLibEntityPredicates.hasFullArmorSetMatching(
            target,
            stack -> stack.is(com.human.common.registry.tag.HumanItemTags.MK50_ARMOR)
        );
    }

    // ------------------------------------------------------------------------------------------------------------
    // The acids
    // ------------------------------------------------------------------------------------------------------------

    private static boolean acidImmune(LivingEntity target) {
        return target.getType().is(ACID_IMMUNE_ENTITIES)
            || target.getItemBySlot(EquipmentSlot.FEET).is(ACID_IMMUNE_ITEMS)
            || com.human.common.util.GeneImmunity.acidImmune(target);
    }

    /** Xeno acid (and nether acid, which also ignites): AcidEffects' damage and interval. */
    private static void acid(
        ServerLevel level,
        LivingEntity target,
        @Nullable Entity source,
        int ticks,
        boolean nether,
        float potency
    ) {
        if (acidImmune(target) || !dueThisCall(target, AcidEffects.HIT_INTERVAL_TICKS, ticks)) {
            return;
        }

        if (nether) {
            target.igniteForTicks(AcidEffects.IGNITE_TICKS);
        }

        target.hurt(damageSource(level, HumanDamageTypeKeys.ACID_VAPOR, source), AcidEffects.DAMAGE * potency);
    }

    /**
     * Irradium's radiation: avp_human's Radiation effect for 10 s, topped up every half second while they stay in it -
     * the effect deals the radiation damage. Its freezing is cryo's, applied separately.
     */
    private static void irradiate(LivingEntity target, int ticks) {
        if (dueThisCall(target, AcidEffects.HIT_INTERVAL_TICKS, ticks)) {
            target.addEffect(new MobEffectInstance(HumanMobEffects.getRadiationHolder(), AcidEffects.RADIATION_TICKS));
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Whether an effect that fires every {@code interval} ticks is due in the {@code ticks} this call stands for - read
     * off the target's own tick count, as the liquids do, so two machines on one mob do not double it.
     */
    private static boolean dueThisCall(LivingEntity target, int interval, int ticks) {
        return ticks >= interval || target.tickCount % interval < ticks;
    }

    /** A damage type, credited to whoever released the vapor (so the kill and the anger are theirs). */
    private static DamageSource damageSource(ServerLevel level, ResourceKey<DamageType> type, @Nullable Entity source) {
        var holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type);
        return source == null ? new DamageSource(holder) : new DamageSource(holder, source);
    }

    /** avp_alien's acid damage type when it is installed; plain magic damage when it is not - as the liquid does. */
}
