package com.human.common.gameplay.item.gun.attack.hitscan;

import com.alien.common.gameplay.entity.living.alien.Alien;
import com.blib.api.common.enchantment.v1.EnchantmentUtil;
import com.blib.api.common.entity.v1.BLibEntityPredicates;
import com.human.Human;
import com.human.common.gameplay.entity.living.human.marine.MarineAllyUtil;
import com.human.common.gameplay.item.gun.GunAccuracyState;
import com.human.common.gameplay.item.gun.attack.GunAttackAction;
import com.human.common.gameplay.item.gun.attack.GunAttackConfig;
import com.human.common.gameplay.item.gun.attack.GunHitResult;
import com.human.common.gameplay.item.gun.debug.BulletTrajectoryDebug;
import com.human.common.gameplay.item.gun.pipeline.GunShootResult;
import com.human.common.network.packet.S2CGunKillEffectPayload;
import com.human.common.network.packet.S2CGunRecoilPayload;
import com.human.common.network.packet.S2CGunVoxelEffectPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** A single server-owned ray pipeline for every conventional firearm. */
public final class HitScanGunAttackAction implements GunAttackAction {

    public static final HitScanGunAttackAction INSTANCE = new HitScanGunAttackAction();

    private static final double TRACE_EPSILON = 0.01D;

    private HitScanGunAttackAction() {}

    @Override
    public GunShootResult shoot(GunAttackConfig gunAttackConfig) {
        var shooter = gunAttackConfig.shooter();
        if (shooter.level().isClientSide) {
            return GunShootResult.SHOT;
        }

        var level = (ServerLevel) shooter.level();
        var fireMode = gunAttackConfig.fireModeConfig();
        var accuracyShot = GunAccuracyState.nextShot(shooter, fireMode);
        var origin = shooter.getEyePosition();
        var allHits = new ArrayList<GunHitResult>();
        var visualImpacts = new ArrayList<Impact>();
        var primaryImpact = Impact.miss(origin.add(accuracyShot.direction().scale(fireMode.range())));
        Impact visualImpact = primaryImpact;
        var pelletDamage = 1.0F / fireMode.pelletCount();

        for (int pellet = 0; pellet < fireMode.pelletCount(); pellet++) {
            var direction = pellet == 0
                ? accuracyShot.direction()
                : GunAccuracyState.scatter(
                    accuracyShot.direction(),
                    Math.max(fireMode.pelletSpreadDegrees(), accuracyShot.spread()),
                    shooter.getRandom()
                );
            var impact = tracePellet(gunAttackConfig, direction, pelletDamage, allHits);
            if (pellet == 0) {
                primaryImpact = impact;
                visualImpact = impact;
            }
            if (impact.hasVisualImpact()) {
                visualImpacts.add(impact);
            }
            // The center pellet remains the debug/primary visual point of aim.
            if (visualImpact.isMiss() && impact.entityImpact()) {
                visualImpact = impact;
            }
        }

        for (var impact : visualImpacts) {
            var effectPayload = new S2CGunVoxelEffectPayload(
                origin,
                impact.position(),
                impact.impactNormal(),
                shooter.getRandom().nextInt(),
                impact.entityImpact(),
                impact.fluidType()
            );
            level.players()
                .stream()
                .filter(player -> canSeeImpact(level, player, impact))
                .forEach(player -> Human.MOD.networking().sendToClient(player, effectPayload));
        }

        BulletTrajectoryDebug.renderShot(
            gunAttackConfig,
            origin,
            accuracyShot.direction(),
            origin.distanceTo(primaryImpact.position()),
            allHits.size(),
            EnchantmentUtil.getLevel(level, gunAttackConfig.gunItemStack(), Enchantments.PIERCING) + 1,
            allHits
        );

        if (shooter instanceof ServerPlayer player) {
            Human.MOD.networking()
                .sendToClient(
                    player,
                    new S2CGunRecoilPayload(accuracyShot.verticalKick(), accuracyShot.horizontalKick())
                );
        }

        return GunShootResult.SHOT;
    }

    private static Impact tracePellet(
        GunAttackConfig baseConfig,
        Vec3 direction,
        float damageMultiplier,
        List<GunHitResult> allHits
    ) {
        var shooter = baseConfig.shooter();
        var level = shooter.level();
        var config = new GunAttackConfig(
            baseConfig.gunConfig(),
            baseConfig.fireModeConfig(),
            shooter,
            baseConfig.gunItemStack(),
            damageMultiplier
        );
        var current = shooter.getEyePosition();
        var remainingDistance = (double) config.fireModeConfig().range();
        var hitEntities = new HashSet<UUID>();
        var remainingPierces = EnchantmentUtil.getLevel(level, config.gunItemStack(), Enchantments.PIERCING) + 1;
        Impact lastImpact = null;

        while (remainingDistance > TRACE_EPSILON && remainingPierces > 0) {
            var requestedEnd = current.add(direction.scale(remainingDistance));
            var blockHit = level.clip(new ClipContext(current, requestedEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, shooter));
            var blockEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : requestedEnd;
            var entityHit = findNearestEntityHit(level, shooter, current, blockEnd, hitEntities);

            if (entityHit != null) {
                var entity = entityHit.entity();
                var hitLocation = entityHit.location();
                hitEntities.add(entity.getUUID());
                var hitDistance = shooter.getEyePosition().distanceTo(hitLocation);
                var impactConfig = withDamageFalloff(config, hitDistance);
                var damageResult = EntityGunHitResultHandler.handle(impactConfig, entity, hitEntities.size() - 1);
                allHits.add(new GunHitResult.Entity(entity.getUUID()));
                if (damageResult.lethal()) {
                    var burstCount = config.fireModeConfig().pelletCount() > 1 ? 24 : 18;
                    sendBloodBurst(level, shooter, entity, hitLocation, direction, burstCount);
                }
                lastImpact = new Impact(
                    hitLocation,
                    true,
                    Vec3.ZERO,
                    killFluidType(entity)
                );
                remainingPierces--;
                var traveled = current.distanceTo(hitLocation) + TRACE_EPSILON;
                current = hitLocation.add(direction.scale(TRACE_EPSILON));
                remainingDistance -= traveled;
                continue;
            }

            if (blockHit.getType() == HitResult.Type.BLOCK) {
                var impactConfig = withDamageFalloff(config, shooter.getEyePosition().distanceTo(blockHit.getLocation()));
                BlockGunHitResultHandler.handle(impactConfig, new GunHitResult.Block(blockHit.getBlockPos(), blockHit.getDirection()), 0);
                allHits.add(new GunHitResult.Block(blockHit.getBlockPos(), blockHit.getDirection()));
                return lastImpact == null
                    ? new Impact(blockHit.getLocation(), false, Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()), 0)
                    : lastImpact;
            }

            return lastImpact == null ? Impact.miss(requestedEnd) : lastImpact;
        }

        return lastImpact == null ? Impact.miss(current) : lastImpact;
    }

    private static EntityTraceHit findNearestEntityHit(
        net.minecraft.world.level.Level level,
        LivingEntity shooter,
        Vec3 rayStart,
        Vec3 rayEnd,
        HashSet<UUID> ignored
    ) {
        EntityTraceHit nearest = null;
        var nearestDistance = Double.MAX_VALUE;
        var searchBounds = new AABB(rayStart, rayEnd).inflate(12.0D);
        for (
            var entity : level.getEntities(
                shooter,
                searchBounds,
                candidate -> !ignored.contains(candidate.getUUID())
                    && !MarineAllyUtil.isMarineAlly(shooter, candidate)
                    && (candidate.getType() == EntityType.END_CRYSTAL || BLibEntityPredicates.isAlive(candidate))
            )
        ) {
            Vec3 location = null;
            var bodyLocation = entity.getBoundingBox().inflate(0.3D).clip(rayStart, rayEnd).orElse(null);
            if (bodyLocation != null) {
                location = bodyLocation;
            }
            if (location == null) {
                continue;
            }
            var distance = rayStart.distanceToSqr(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = new EntityTraceHit(entity, location);
            }
        }
        return nearest;
    }

    private static void sendBloodBurst(
        net.minecraft.world.level.Level level,
        LivingEntity shooter,
        Entity entity,
        Vec3 hitLocation,
        Vec3 direction,
        int burstCount
    ) {
        Human.MOD.networking()
            .sendToAllClients(
                level.getServer(),
                new S2CGunKillEffectPayload(
                    hitLocation,
                    direction,
                    killFluidType(entity),
                    burstCount,
                    shooter.getRandom().nextInt()
                )
            );
    }

    private static int killFluidType(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof Alien alien) {
            if (alien.isIrradiated()) {
                return 4;
            }
            if (alien.isNetherAfflicted()) {
                return 3;
            }
            return 1;
        }
        var key = entity.getType().builtInRegistryHolder().key().location();
        var namespace = key.getNamespace();
        var path = key.getPath();
        if (path.contains("irradiated")) {
            return 4;
        }
        if (path.contains("nether")) {
            return 3;
        }
        if (namespace.contains("alien") || path.contains("xenomorph") || path.contains("facehugger")) {
            return 1;
        }
        if (path.contains("android") || path.contains("synthetic") || path.contains("robot")) {
            return 2;
        }
        return 0;
    }

    private record Impact(
        Vec3 position,
        boolean entityImpact,
        Vec3 impactNormal,
        int fluidType
    ) {

        private boolean isMiss() {
            return !entityImpact && impactNormal.equals(Vec3.ZERO);
        }

        private boolean hasVisualImpact() {
            return !isMiss();
        }

        private static Impact miss(Vec3 position) {
            return new Impact(position, false, Vec3.ZERO, 0);
        }
    }

    private record EntityTraceHit(
        Entity entity,
        Vec3 location
    ) {}

    private static boolean canSeeImpact(ServerLevel level, ServerPlayer viewer, Impact impact) {
        if (!impact.entityImpact() && impact.impactNormal().equals(Vec3.ZERO)) {
            return false;
        }

        var visiblePoint = impact.position().add(impact.impactNormal().scale(0.015D));
        var obstruction = level.clip(
            new ClipContext(viewer.getEyePosition(), visiblePoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer)
        );
        return obstruction.getType() != HitResult.Type.BLOCK
            || obstruction.getLocation().distanceToSqr(viewer.getEyePosition()) >= visiblePoint.distanceToSqr(viewer.getEyePosition())
                - 0.001D;
    }

    private static GunAttackConfig withDamageFalloff(GunAttackConfig config, double distance) {
        var fireMode = config.fireModeConfig();
        var startDistance = fireMode.range() * fireMode.damageFalloffStartFraction();
        var falloffSpan = Math.max(0.001D, fireMode.range() - startDistance);
        var progress = Math.clamp((distance - startDistance) / falloffSpan, 0.0D, 1.0D);
        var multiplier = (float) (1.0D - ((1.0D - fireMode.minimumDamageMultiplier()) * progress));
        return new GunAttackConfig(
            config.gunConfig(),
            fireMode,
            config.shooter(),
            config.gunItemStack(),
            config.damageMultiplier() * multiplier
        );
    }

}
