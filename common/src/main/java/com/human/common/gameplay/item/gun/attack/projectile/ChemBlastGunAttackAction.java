package com.human.common.gameplay.item.gun.attack.projectile;

import com.human.Human;
import com.human.common.gameplay.entity.living.human.marine.MarineAllyUtil;
import com.human.common.gameplay.entity.projectile.ChemCloud;
import com.human.common.gameplay.item.gun.attack.GunAttackAction;
import com.human.common.gameplay.item.gun.attack.GunAttackConfig;
import com.human.common.gameplay.item.gun.chem.ChemCone;
import com.human.common.gameplay.item.gun.chem.ChemThrowerReloading;
import com.human.common.gameplay.item.gun.pipeline.GunShootResult;
import com.human.common.gameplay.vapor.VaporBlockEffects;
import com.human.common.gameplay.vapor.VaporEffects;
import com.human.common.network.packet.S2CChemBlastPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The chem thrower's shot - one tick of the blast. [stated] Oct 18 - NOT the flamethrower's projectile: a directed
 * blast, a cone about 8 blocks long from the nozzle ({@link ChemCone}), that acts on everything caught in it at once,
 * and leaves its end lingering ({@link ChemCloud}).
 * <ol>
 * <li>How far it reaches before a wall.</li>
 * <li>Enchantments ([stated] Oct 18): FLAME widens the end to 5 x 5, PIERCING lengthens the linger, POWER makes the
 * vapor 25 % stronger a level, PUNCH pushes what it catches along the cone. (QUICK CHARGE is in the reload.)</li>
 * <li>The blocks it touches get the vapor's block effect ({@link VaporBlockEffects}) - [stated] the chem thrower's
 * vapor does, the distributor's does not.</li>
 * <li>Everything living in the cone gets the vapor's effect, by the cone's strength where it stands
 * ({@link VaporEffects}) - credited to the shooter; never the shooter, never their marines.</li>
 * <li>The cloud at the end: the shooter's current cloud is fed and moved to the new end while the aim stays within a
 * block of it; aimed further away, a new one starts (at most every {@value #NEW_CLOUD_TICKS} ticks, so a fast sweep
 * leaves a trail, not hundreds of clouds).</li>
 * <li>Every client watching is told where the blast is ({@link S2CChemBlastPayload}, one small packet a tick) and draws
 * it - so other players see it too.</li>
 * </ol>
 * ⚡ Per tick of firing: one wall march (32 block reads), one entity search around the cone, one packet per watcher.
 */
public class ChemBlastGunAttackAction implements GunAttackAction {

    public static final ChemBlastGunAttackAction INSTANCE = new ChemBlastGunAttackAction();

    private static final int NEW_CLOUD_TICKS = 4;

    private static final double FEED_DISTANCE_SQR = 1.0;

    /** Each shooter's cloud being fed. Weak, so a shooter who leaves drops out. */
    private static final Map<Entity, ChemCloud> CLOUDS = Collections.synchronizedMap(new WeakHashMap<>());

    /** When each shooter last started a NEW cloud (game time). */
    private static final Map<Entity, Long> LAST_SPAWN = Collections.synchronizedMap(new WeakHashMap<>());

    private ChemBlastGunAttackAction() {}

    @Override
    public GunShootResult shoot(GunAttackConfig gunAttackConfig) {
        var shooter = gunAttackConfig.shooter();

        if (!(shooter.level() instanceof ServerLevel level)) {
            return GunShootResult.SHOT;
        }

        var gun = gunAttackConfig.gunItemStack();

        // [stated] Oct 19 - an empty gun is empty, in creative too: it used to spray CRYO (the stand-in for a gun with
        // rounds but no vapor recorded) whenever creative let it fire with no tank on.
        var kind = ChemThrowerReloading.onGun(gun);

        if (kind == null) {
            return GunShootResult.FAILURE;
        }

        // [stated] Oct 18 - FLAME widens the end from 3 x 3 to 5 x 5; PIERCING makes the end linger longer.
        var farHalf = ChemCone.farHalf(level, gun);
        var lingerTicks = ChemCloud.lifetimeFor(level, gun);
        // POWER: a stronger vapor. PUNCH: the blast pushes what it catches along the cone.
        var potency = ChemCone.potency(level, gun);
        var punch = ChemCone.punch(level, gun);
        var nozzle = ChemCone.nozzle(shooter);
        var direction = shooter.getLookAngle().normalize();
        var reach = ChemCone.reach(level, nozzle, direction);
        var end = nozzle.add(direction.scale(reach));

        // Everything in the cone.
        var box = new AABB(nozzle, end).inflate(farHalf + 0.5D);

        for (var entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity == shooter || MarineAllyUtil.isMarineAlly(shooter, entity) || entity.isSpectator()) {
                continue;
            }

            var strength = ChemCone.strengthAt(
                entity.getBoundingBox().getCenter(),
                nozzle,
                direction,
                reach,
                farHalf,
                entity.getBbWidth() / 2.0D
            );

            if (strength > 0.0F) {
                VaporEffects.apply(level, kind, entity, strength, shooter, VaporEffects.Origin.CHEM_THROWER, 1, potency);

                if (punch > 0) {
                    push(entity, direction, punch * ChemCone.PUNCH_PUSH_PER_LEVEL * strength);
                }
            }
        }

        // [stated] Oct 18 - the blast is concentrated enough to act on the BLOCKS it touches (the distributor's fog is
        // not): a few points a tick, each touching one block beside it.
        for (var sample = 0; sample < VaporBlockEffects.BLAST_SAMPLES_PER_TICK; sample++) {
            var point = ChemCone.randomPoint(nozzle, direction, reach, farHalf, level.random);
            var strength = ChemCone.strengthAt(point, nozzle, direction, reach, farHalf, 0.0D);

            VaporBlockEffects.touch(level, kind, BlockPos.containing(point), strength, level.random, potency);
        }

        // The lingering end - pulled back a little from the wall so it does not sit inside it.
        var cloudAt = nozzle.add(direction.scale(Math.max(0.0D, reach - farHalf * 0.5D)));
        var cloud = CLOUDS.get(shooter);
        var now = level.getGameTime();
        var lastSpawn = LAST_SPAWN.getOrDefault(shooter, Long.MIN_VALUE / 2);

        // Fed (and moved to the new end) while the aim stays within a block of it, or while it is too soon for a new
        // one; aimed away, the old one is left to disperse where it is and a new one starts.
        if (
            cloud != null && !cloud.isRemoved() && cloud.level() == level && cloud.getVapor() == kind
                && (cloud.position().distanceToSqr(cloudAt) <= FEED_DISTANCE_SQR || now - lastSpawn < NEW_CLOUD_TICKS)
        ) {
            cloud.feed(cloudAt.x, cloudAt.y, cloudAt.z);
            cloud.setPotency(potency);
        } else {
            cloud = new ChemCloud(level, shooter, kind, cloudAt.x, cloudAt.y, cloudAt.z, (float) farHalf, lingerTicks);
            cloud.setPotency(potency);
            level.addFreshEntity(cloud);
            CLOUDS.put(shooter, cloud);
            LAST_SPAWN.put(shooter, now);
        }

        var payload = new S2CChemBlastPayload(
            (float) nozzle.x,
            (float) nozzle.y,
            (float) nozzle.z,
            (float) direction.x,
            (float) direction.y,
            (float) direction.z,
            (float) reach,
            (float) farHalf,
            kind.ordinal(),
            shooter.getId()
        );

        Human.MOD.networking().sendToAllClientsTrackingEntity(shooter, payload);

        if (shooter instanceof ServerPlayer serverPlayer) {
            Human.MOD.networking().sendToClient(serverPlayer, payload);
        }

        return GunShootResult.SHOT;
    }

    /**
     * PUNCH: a steady shove along the aim, like standing in a strong wind - added each tick it is caught, weaker where
     * the blast is thin, and less for anything that resists knockback (an iron golem, netherite). ⚠ hurtMarked so a
     * PLAYER's client is told about the push; mobs move on the server anyway.
     */
    private static void push(LivingEntity entity, net.minecraft.world.phys.Vec3 direction, double speed) {
        var resistance = entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
        var scaled = speed * Math.max(0.0, 1.0 - resistance);

        if (scaled <= 0.0) {
            return;
        }

        entity.push(direction.x * scaled, direction.y * scaled * 0.5, direction.z * scaled);
        entity.hurtMarked = true;
    }
}
