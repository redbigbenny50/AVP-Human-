package com.human.common.gameplay.entity.machine;

import com.human.common.data.HumanTooltipTranslationKeys;
import com.human.common.gameplay.block.entity.AmmoChestBlockEntity;
import com.human.common.property.HumanProperties;
import com.human.common.property.HumanPropertyAccess;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.HumanSoundEvents;
import com.human.common.registry.key.HumanDamageTypeKeys;
import com.human.common.registry.tag.HumanDamageTypesTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SentryTurret extends Mob implements TraceableEntity {

    public static float DAMAGE = HumanPropertyAccess.INSTANCE.get(HumanProperties.Blocks.SentryTurret.DAMAGE);

    public static int RANGE = HumanPropertyAccess.INSTANCE.get(HumanProperties.Blocks.SentryTurret.RANGE);

    protected static int AMMO_CHEST_RANGE = HumanPropertyAccess.INSTANCE.get(HumanProperties.Blocks.SentryTurret.AMMO_CHEST_RANGE);

    protected static int FOV = HumanPropertyAccess.INSTANCE.get(HumanProperties.Blocks.SentryTurret.FOV);

    private static final int MAX_TURRET_FIRE_COOLDOWN_IN_TICKS = 2;

    /**
     * Ticks between ammo-chest searches.
     * <p>
     * ⚠ That search walks a cube of {@code AMMO_CHEST_RANGE}, which at the default 5 is 11x11x11 = 1331
     * {@code getBlockEntity} calls. It used to run EVERY tick and BEFORE the cooldown check, so an idle turret with
     * nothing to shoot still cost 26,620 block-entity lookups a second, and ten of them cost a quarter of a million.
     * Twenty ticks is far finer than the granularity anyone can perceive in "did my turret notice the chest yet".
     */
    private static final int AMMO_CHEST_SEARCH_INTERVAL_IN_TICKS = 20;

    /**
     * The chest found by the last search. Re-validated cheaply every tick, so a chest that is broken or emptied is
     * noticed immediately rather than at the end of the interval.
     */
    private @Nullable AmmoChestBlockEntity cachedAmmoChest;

    private static final String FIRE_COOLDOWN_KEY = "FireCooldown";

    private static final String OWNER_KEY = "Owner";

    @Nullable
    private UUID ownerUUID;

    @Nullable
    private Entity cachedOwner;

    private int fireCooldown = 0;

    protected final SentryTurretAnimDispatcher animDispatcher;

    public SentryTurret(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        animDispatcher = new SentryTurretAnimDispatcher(this);
    }

    public void setOwner(@Nullable Entity owner) {
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
            this.cachedOwner = owner;
        }
    }

    @Nullable
    @Override
    public Entity getOwner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        } else if (ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            cachedOwner = serverLevel.getEntity(ownerUUID);
            return cachedOwner;
        } else {
            return null;
        }
    }

    public static AttributeSupplier.Builder createSentryTurretAttributes() {
        return LivingEntity.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 16.0F)
            .add(Attributes.MOVEMENT_SPEED, 0.0F)
            .add(Attributes.KNOCKBACK_RESISTANCE, 100.0D)
            .add(Attributes.FOLLOW_RANGE, RANGE);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            if (!isPoweredByRedstone()) {
                animDispatcher.unpowered();
                return;
            } else if (getTarget() == null) {
                animDispatcher.idle();
            }

            var ammoChestBlockEntity = getAmmoChest();
            if (ammoChestBlockEntity == null || !ammoChestBlockEntity.hasAmmo()) {
                setTarget(null);
                animDispatcher.idle();
                return;
            }

            if (getTarget() != null && !getTarget().isAlive()) {
                setTarget(null);
                animDispatcher.idle();
                return;
            }

            if (fireCooldown > 0) {
                fireCooldown--;
                return;
            }

            if (ammoChestBlockEntity.hasAmmo()) {
                targetAndFire(ammoChestBlockEntity);
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID(OWNER_KEY)) {
            this.ownerUUID = compound.getUUID(OWNER_KEY);
            this.cachedOwner = null;
        }

        this.fireCooldown = compound.getInt(FIRE_COOLDOWN_KEY);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        if (ownerUUID != null) {
            compound.putUUID(OWNER_KEY, ownerUUID);
        }

        compound.putInt(FIRE_COOLDOWN_KEY, fireCooldown);
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return HumanBlocks.SENTRY_TURRET.get().asItem().getDefaultInstance();
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean canBeAffected(@NotNull MobEffectInstance effectInstance) {
        // Sentry turrets aren't affected by any effects, even positive ones. It doesn't make sense for a turret
        // to have regeneration or absorption as much as it doesn't make sense for them to have nausea or wither.
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is((HumanDamageTypesTags.DOES_NOT_HURT_SENTRY_TURRETS))) {
            return false;
        }

        return super.hurt(source, amount);
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        var isOwner = getOwner() != null && getOwner().is(player);

        // ⚠ PICKING THE TURRET UP NOW NEEDS A SNEAK. It used to be a plain right-click, but that is also the gesture
        // for "what is wrong with this thing", and asking a turret why it is silent should never pocket it mid-fight.
        // Sneaking is the deliberate action, which is the right way round for the destructive one.
        if (isOwner && player.isShiftKeyDown()) {
            dropTurretItem();
            discard();

            return InteractionResult.SUCCESS;
        }

        sendStatusReport(player);

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canBeCollidedWith() {
        return isAlive();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    private void dropTurretItem() {
        var turretItem = getPickResult();
        if (turretItem != null) {
            spawnAtLocation(turretItem, 0.5f);
        }
    }

    /**
     * What this turret is waiting on, as lines to show the player, or EMPTY when it is ready to fire.
     * <p>
     * Both conditions are reported, not just the first: a turret dropped in a bare field needs power AND a chest, and
     * being told about one at a time means two trips.
     * <p>
     * ⚠ Uses {@link #findNearbyAmmoChest} directly rather than the cached {@link #getAmmoChest()}. The cache only
     * refreshes on its own schedule and returns null in between, which is fine for firing - a tick's delay costs
     * nothing - but would make a status readout LIE to someone who just placed the chest and asked.
     */
    public List<Component> getStatusReport() {
        var report = new ArrayList<Component>();

        if (!isPoweredByRedstone()) {
            report.add(Component.translatable(HumanTooltipTranslationKeys.STATUS_NEEDS_REDSTONE));
        }

        var ammoChestBlockEntity = findNearbyAmmoChest(this, blockPosition());

        if (ammoChestBlockEntity == null) {
            report.add(Component.translatable(HumanTooltipTranslationKeys.STATUS_NEEDS_AMMO_CHEST, AMMO_CHEST_RANGE));
        } else if (!ammoChestBlockEntity.hasAmmo()) {
            report.add(Component.translatable(HumanTooltipTranslationKeys.STATUS_NEEDS_MEDIUM_BULLETS));
        }

        return report;
    }

    /**
     * Tells a player what the turret is waiting on, or that it is ready. Sent as an action-bar line rather than chat:
     * it is a glance-at-it status, not something worth keeping in the log.
     */
    public void sendStatusReport(Player player) {
        var report = getStatusReport();

        if (report.isEmpty()) {
            player.displayClientMessage(Component.translatable(HumanTooltipTranslationKeys.STATUS_READY), true);

            return;
        }

        var message = Component.empty();

        for (var index = 0; index < report.size(); index++) {
            if (index > 0) {
                message.append(Component.literal("  |  "));
            }

            message.append(report.get(index));
        }

        player.displayClientMessage(message, true);
    }

    private boolean isPoweredByRedstone() {
        return level().hasNeighborSignal(blockPosition());
    }

    private void targetAndFire(AmmoChestBlockEntity ammoChestBlockEntity) {
        var target = getTarget();

        if (target != null && (!target.isAlive() || target.isRemoved())) {
            setTarget(null);
            this.fireCooldown = 0;
            animDispatcher.idle();
            return;
        }

        if (getTarget() == null) {
            findTarget();
        }

        if (getTarget() != null) {
            fireAtTarget(ammoChestBlockEntity);
        }
    }

    private void findTarget() {
        var monsters = level()
            .getEntitiesOfClass(
                Monster.class,
                getBoundingBox().inflate(RANGE),
                this::canTargetMonster
            );

        if (!monsters.isEmpty()) {
            setTarget(monsters.getFirst());
        }
    }

    private void fireAtTarget(AmmoChestBlockEntity ammoChestBlockEntity) {
        var target = getTarget();

        if (
            target != null && target.isAlive() && isFacingTarget(blockPosition(), getLookAngle(), target) && getSensing()
                .hasLineOfSight(target)
        ) {
            animDispatcher.firing();
            level().playSound(null, blockPosition(), HumanSoundEvents.WEAPON_GENERIC_SHOOT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            target.hurt(damageSources().source(HumanDamageTypeKeys.BULLET, this), DAMAGE);
            target.setLastHurtMob(this);
            ammoChestBlockEntity.consumeAmmo(1);
            fireCooldown = MAX_TURRET_FIRE_COOLDOWN_IN_TICKS;
            target.invulnerableTime = 0;
        } else {
            setTarget(null);
        }
    }

    private boolean canTargetMonster(Monster monster) {
        return monster.isAlive()
            && distanceTo(monster) <= RANGE
            && isFacingTarget(blockPosition(), getLookAngle(), monster)
            && getSensing().hasLineOfSight(monster);
    }

    private static boolean isFacingTarget(BlockPos turretPos, Vec3 facingVec, LivingEntity target) {
        var turretCenter = Vec3.atCenterOf(turretPos);
        var entityPos = Vec3.atCenterOf(target.blockPosition());
        var directionToEntity = entityPos.subtract(turretCenter).normalize();

        var dotProduct = directionToEntity.dot(facingVec.normalize());
        return dotProduct > Math.cos(Math.toRadians(FOV));
    }

    /**
     * The turret's ammo chest, searched for at most once per {@link #AMMO_CHEST_SEARCH_INTERVAL_IN_TICKS}.
     * <p>
     * The cached chest is checked for still being loaded and still being a chest before it is trusted, which costs one
     * dereference - so breaking the chest stops the turret on the next tick, not twenty ticks later. Only the SEARCH is
     * throttled, and only when there is nothing cached to search for.
     */
    private @Nullable AmmoChestBlockEntity getAmmoChest() {
        if (cachedAmmoChest != null && !cachedAmmoChest.isRemoved()) {
            return cachedAmmoChest;
        }

        cachedAmmoChest = null;

        if (tickCount % AMMO_CHEST_SEARCH_INTERVAL_IN_TICKS != 0) {
            return null;
        }

        cachedAmmoChest = findNearbyAmmoChest(this, blockPosition());

        return cachedAmmoChest;
    }

    private static AmmoChestBlockEntity findNearbyAmmoChest(SentryTurret sentryTurret, BlockPos pos) {
        var level = sentryTurret.level();

        for (
            var searchRadius : BlockPos.betweenClosed(
                pos.offset(-AMMO_CHEST_RANGE, -AMMO_CHEST_RANGE, -AMMO_CHEST_RANGE),
                pos.offset(AMMO_CHEST_RANGE, AMMO_CHEST_RANGE, AMMO_CHEST_RANGE)
            )
        ) {
            var ammoEntity = level.getBlockEntity(searchRadius);
            if (ammoEntity instanceof AmmoChestBlockEntity ammoChestBlockEntity) {
                return ammoChestBlockEntity;
            }
        }

        return null;
    }

}
