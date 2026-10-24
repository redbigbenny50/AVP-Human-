package com.human.common.gameplay.acid;

import com.human.common.registry.init.HumanMobEffects;
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

/**
 * What the acids do to anything standing in them - avp_alien's AcidEntityDamageUtil, rule for rule:
 * <ul>
 * <li>Every {@value #HIT_INTERVAL_TICKS} ticks, {@value #DAMAGE} acid damage (avp_alien's own {@code avp_alien:acid}
 * damage type, so its death message and every strain's immunity to it apply).</li>
 * <li>Boots soak the hit instead and take 3-5 durability. Boots in avp_alien's {@code acid_immune} item tag stop it
 * entirely, and mobs in its {@code acid_immune} entity tag are never touched.</li>
 * <li>NETHER acid also sets them alight for 5 seconds.</li>
 * <li>IRRADIATED acid never burns and never eats boots: it freezes, like powder snow, and - [stated] Oct 17 - it
 * irradiates, with avp_human's Radiation effect for 10 seconds, topped up while they stay in it.</li>
 * </ul>
 * Players in creative or spectator are left alone, as the blood leaves invulnerable players alone.
 */
public final class AcidEffects {

    public static final int HIT_INTERVAL_TICKS = 10;

    public static final float DAMAGE = 1.0F;

    public static final int IGNITE_TICKS = 5 * 20;

    public static final int RADIATION_TICKS = 10 * 20;

    private static final ResourceKey<DamageType> ALIEN_ACID = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath("avp_alien", "acid")
    );

    private static final TagKey<Item> ACID_IMMUNE_ITEMS = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("avp_alien", "acid_immune")
    );

    private static final TagKey<EntityType<?>> ACID_IMMUNE_ENTITIES = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("avp_alien", "acid_immune")
    );

    private AcidEffects() {}

    /** Server side; called by the liquid block once per tick for the one block that counts (see AcidLiquidBlock). */
    public static void tick(AcidKind kind, ServerLevel level, Entity entity) {
        if (!entity.isAlive() || entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }

        if (kind == AcidKind.IRRADIATED) {
            // [stated] Oct 19 - "Acid immune should not protect from irradiated blood": it is not an acid, it is cold.
            // It used to sit behind the acid immunities below, so every alien (all are acid_immune) and anyone in
            // chitin or predator boots waded through it unfrozen. Now only what cold cannot touch is spared - the
            // irradiated strain, cryo suits, cold-immune mobs and genes (FrozenSolid.isImmune); who the radiation
            // reaches is still the effect's own decision.
            // The cold and the radiation are separate: a cryo suit keeps out the freeze but not the radiation.
            var coldImmune = entity instanceof LivingEntity living && com.human.common.gameplay.freeze.FrozenSolid.isImmune(living);

            // Freezes exactly like powder snow, every tick it stands in it; never burns, never eats boots.
            if (!coldImmune) {
                entity.setIsInPowderSnow(true);
            }

            if (entity instanceof LivingEntity living && entity.tickCount % HIT_INTERVAL_TICKS == 0) {
                living.addEffect(new MobEffectInstance(HumanMobEffects.getRadiationHolder(), RADIATION_TICKS));
            }

            return;
        }

        // [stated] Oct 19 - a 100% acid resistance gene is acid immunity.
        if (entity.getType().is(ACID_IMMUNE_ENTITIES) || com.human.common.util.GeneImmunity.acidImmune(entity)) {
            return;
        }

        var boots = entity instanceof LivingEntity living ? living.getItemBySlot(EquipmentSlot.FEET) : null;

        if (boots != null && boots.is(ACID_IMMUNE_ITEMS)) {
            return;
        }

        if (entity.tickCount % HIT_INTERVAL_TICKS != 0) {
            return;
        }

        if (boots != null && !boots.isEmpty()) {
            var wear = level.random.nextInt(3) + 3;

            boots.hurtAndBreak(wear, level, null, item -> {});
            return;
        }

        if (kind == AcidKind.NETHER) {
            entity.igniteForTicks(IGNITE_TICKS);
        }

        entity.hurt(damageSource(level), DAMAGE);
    }

    /** avp_alien's acid damage type when it is installed; plain magic damage when it is not. */
    private static DamageSource damageSource(ServerLevel level) {
        var types = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var acid = types.getHolder(ALIEN_ACID);

        return acid.map(DamageSource::new).orElseGet(() -> level.damageSources().magic());
    }
}
