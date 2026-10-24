package com.human.common.gameplay.fluid;

import com.human.common.registry.key.HumanDamageTypeKeys;
import com.human.common.registry.tag.HumanEntityTypeTags;
import com.human.common.registry.tag.HumanItemTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * What cryofluid does to anything standing in it. [stated] Oct 1: it deals FREEZING DAMAGE while you are in it, and
 * only PROPER SUITS - the MK50 or another environmental hazard suit - protect you, as a FULL set.
 * <h2>How it freezes</h2> It drives vanilla's own freeze meter, so the frost creeping in at the screen edge and the
 * shivering are the ones players already know from powder snow. Vanilla drains the meter by 2 every tick an entity is
 * out of powder snow, so adding {@value #FREEZE_PER_TICK} nets +2 a tick: a player is fully frozen in about three and a
 * half seconds, twice as fast as powder snow. Once fully frozen it deals {@value #DAMAGE} every
 * {@value #DAMAGE_INTERVAL_TICKS} ticks with its own damage type.
 * <h2>Why leather does not help</h2> Vanilla's freeze damage is skipped for anyone in leather, but this is not that
 * check: [stated] cryofluid is much harsher than powder snow. Only a full set from the {@code cryo_protective_armor}
 * item tag counts, which holds the MK50 and pulls in Ad Astra's {@code freeze_resistant_armor} tag, so any other mod's
 * cold suit tagged for that works too.
 * <h2>Who is immune</h2> The {@code cryofluid_immune} entity tag (it includes vanilla's freeze-immune mobs), and
 * anything that is invulnerable to the cryofluid damage type. ⭐ That second check is how the alien strain rules reach
 * this: a strain that refuses the damage type in its own immunity table is also never frozen.
 */
public final class CryoFreezing {

    /** Added to the freeze meter each tick in the fluid. Vanilla takes 2 back off, so the net is +2. */
    public static final int FREEZE_PER_TICK = 4;

    public static final int DAMAGE_INTERVAL_TICKS = 20;

    public static final float DAMAGE = 2.0F;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    private CryoFreezing() {}

    /** Server side only. Called by the liquid block for every living entity inside it, every tick. */
    public static void tick(LivingEntity living) {
        if (living.level().isClientSide || living.isSpectator() || living.isDeadOrDying()) {
            return;
        }

        if (isImmune(living)) {
            return;
        }

        var source = damageSource(living);

        if (living.isInvulnerableTo(source)) {
            return;
        }

        // [stated] Oct 19 - eggs freeze (or, nether ones, die) the moment cryo touches them, and take no damage - a
        // hurt egg opens.
        if (com.human.common.gameplay.freeze.FrozenSolid.reactsOnContact(living)) {
            com.human.common.gameplay.freeze.FrozenSolid.tryFreeze(living);
            return;
        }

        // [stated] Oct 19 - frozen solid already: standing in cryofluid keeps it frozen (the thaw timer back to full)
        // and does no more damage.
        if (com.human.common.gameplay.freeze.FrozenSolid.isFrozen(living)) {
            com.human.common.gameplay.freeze.FrozenSolid.keepFrozen(living);
            return;
        }

        var required = living.getTicksRequiredToFreeze();
        living.setTicksFrozen(Math.min(required + FREEZE_PER_TICK, living.getTicksFrozen() + FREEZE_PER_TICK));

        if (living.isFullyFrozen() && living.tickCount % DAMAGE_INTERVAL_TICKS == 0) {
            living.hurt(source, DAMAGE);
        }

        // The shared rule: at 10% health, or any passive mob, it freezes solid.
        if (living.isFullyFrozen()) {
            com.human.common.gameplay.freeze.FrozenSolid.tryFreeze(living);
        }
    }

    /** A full set of cryo-protective armour, or a mob that is immune by type. */
    public static boolean isImmune(LivingEntity living) {
        // [stated] Oct 19 - a 100% cold resistance gene is cold immunity.
        if (living.getType().is(HumanEntityTypeTags.CRYOFLUID_IMMUNE) || com.human.common.util.GeneImmunity.coldImmune(living)) {
            return true;
        }

        for (var slot : ARMOR_SLOTS) {
            if (!living.getItemBySlot(slot).is(HumanItemTags.CRYO_PROTECTIVE_ARMOR)) {
                return false;
            }
        }

        return true;
    }

    public static DamageSource damageSource(LivingEntity living) {
        var registry = living.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);

        return new DamageSource(registry.getHolderOrThrow(HumanDamageTypeKeys.CRYOFLUID));
    }
}
