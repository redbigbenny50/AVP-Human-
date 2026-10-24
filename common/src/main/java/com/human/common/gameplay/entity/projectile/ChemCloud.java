package com.human.common.gameplay.entity.projectile;

import com.human.common.gameplay.entity.living.human.marine.MarineAllyUtil;
import com.human.common.gameplay.item.gun.chem.ChemCone;
import com.human.common.gameplay.vapor.VaporBlockEffects;
import com.human.common.gameplay.vapor.VaporEffects;
import com.human.common.gameplay.vapor.VaporKind;
import com.human.common.gameplay.vapor.VaporShape;
import com.human.common.registry.init.HumanEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The end of the chem thrower's blast, lingering. [stated] Oct 18: "the end of the cone kind of lingers for about 3-4
 * extra seconds before fading away (dispersing)."
 * <ul>
 * <li>A 3 x 3 x 3 cloud - the cone's end slice - with the distributor's rounded, fading edges ({@link VaporShape#box});
 * 5 x 5 x 5 from a gun with FLAME, as its cone ends that wide.</li>
 * <li>It lives {@value #LIFETIME_TICKS} ticks (3.5 s) after the last shot that fed it - a second longer per level of
 * PIERCING ([stated] Oct 18) - thinning as it goes: its strength falls to 0 over that time, so the particles thin out
 * and the effect weakens with it.</li>
 * <li>It keeps working on the blocks it touches ({@link VaporBlockEffects}), fading with it.</li>
 * <li>Whatever stands in it gets the vapor's effect ({@link VaporEffects}) every {@value #EFFECT_INTERVAL_TICKS} ticks,
 * credited to the shooter - so a cryomist cloud keeps topping up a frozen mob's thaw timer while it lasts.</li>
 * <li>One cloud per shooter is kept fed while the aim stays put; sweeping the aim leaves a short trail of clouds (see
 * {@code ChemBlastGunAttackAction}).</li>
 * </ul>
 * ⚠ Not saved with the world - a 3.5-second puff is not worth bringing back after a reload.
 */
public class ChemCloud extends Entity {

    public static final int LIFETIME_TICKS = 70;

    /** [stated] Oct 18 - PIERCING: "the longer linger time" - each level adds a second. */
    public static final int PIERCING_TICKS_PER_LEVEL = 20;

    private static final EntityDataAccessor<Float> HALF_SIZE = SynchedEntityData.defineId(ChemCloud.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(ChemCloud.class, EntityDataSerializers.INT);

    /** How long a cloud from this gun lingers: 3.5 s, plus a second per level of PIERCING. */
    public static int lifetimeFor(Level level, net.minecraft.world.item.ItemStack gun) {
        var piercing = com.blib.api.common.enchantment.v1.EnchantmentUtil.getLevel(
            level,
            gun,
            net.minecraft.world.item.enchantment.Enchantments.PIERCING
        );

        return LIFETIME_TICKS + PIERCING_TICKS_PER_LEVEL * Math.max(0, piercing);
    }

    private static final int EFFECT_INTERVAL_TICKS = 5;

    private static final EntityDataAccessor<Byte> VAPOR = SynchedEntityData.defineId(ChemCloud.class, EntityDataSerializers.BYTE);

    /** Ticks since it was last fed - synced so clients thin it at the same rate. */
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(ChemCloud.class, EntityDataSerializers.INT);

    /** POWER from the gun that made it - server only; it only matters to the effects, which run on the server. */
    private float potency = 1.0F;

    private @Nullable UUID ownerId;

    private @Nullable Entity cachedOwner;

    public ChemCloud(EntityType<? extends ChemCloud> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public ChemCloud(Level level, @Nullable Entity owner, VaporKind kind, double x, double y, double z, float half, int lifetime) {
        this(HumanEntityTypes.CHEM_CLOUD.get(), level);
        setPos(x, y, z);
        setVapor(kind);
        entityData.set(HALF_SIZE, half);
        entityData.set(LIFETIME, Math.max(1, lifetime));

        if (owner != null) {
            ownerId = owner.getUUID();
            cachedOwner = owner;
        }
    }

    @Override
    protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
        builder.define(VAPOR, (byte) 0);
        builder.define(AGE, 0);
        builder.define(HALF_SIZE, (float) ChemCone.FAR_HALF);
        builder.define(LIFETIME, LIFETIME_TICKS);
    }

    public VaporKind getVapor() {
        var kind = VaporKind.byOrdinal(entityData.get(VAPOR));
        return kind == null ? VaporKind.CRYO : kind;
    }

    public void setVapor(VaporKind kind) {
        entityData.set(VAPOR, (byte) kind.ordinal());
    }

    /** Half its width: the cone's end slice - 1.5 (3 x 3), or 2.5 (5 x 5) from a gun with FLAME. */
    public double getHalf() {
        return entityData.get(HALF_SIZE);
    }

    public int getLifetime() {
        return entityData.get(LIFETIME);
    }

    public void setPotency(float potency) {
        this.potency = potency;
    }

    public int getAge() {
        return entityData.get(AGE);
    }

    /** Fed by another shot: back to full strength, moved to where the blast now ends. */
    public void feed(double x, double y, double z) {
        setPos(x, y, z);
        entityData.set(AGE, 0);
    }

    /** 1 when fresh, falling to 0 as it disperses. */
    public float freshness() {
        return Math.max(0.0F, 1.0F - getAge() / (float) getLifetime());
    }

    public @Nullable Entity getOwner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }

        if (ownerId != null && level() instanceof ServerLevel serverLevel) {
            cachedOwner = serverLevel.getEntity(ownerId);
        }

        return cachedOwner;
    }

    @Override
    public void tick() {
        super.tick();

        var level = level();

        if (level.isClientSide) {
            spawnParticles();
            return;
        }

        var age = getAge() + 1;

        if (age >= getLifetime()) {
            discard();
            return;
        }

        entityData.set(AGE, age);

        if (age % EFFECT_INTERVAL_TICKS == 0 && level instanceof ServerLevel serverLevel) {
            applyEffects(serverLevel);
        }
    }

    private void applyEffects(ServerLevel level) {
        var owner = getOwner();
        var freshness = freshness();
        var kind = getVapor();
        var half = getHalf();

        // It is still the chem thrower's concentrated vapor: it keeps working on the blocks it touches as it thins.
        for (var sample = 0; sample < VaporBlockEffects.CLOUD_SAMPLES; sample++) {
            var dx = (random.nextDouble() * 2.0D - 1.0D) * half;
            var dy = (random.nextDouble() * 2.0D - 1.0D) * half;
            var dz = (random.nextDouble() * 2.0D - 1.0D) * half;
            var strength = VaporShape.box(dx, dy, dz, half) * freshness;

            VaporBlockEffects.touch(level, kind, BlockPos.containing(getX() + dx, getY() + dy, getZ() + dz), strength, random, potency);
        }

        for (
            var entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(half), LivingEntity::isAlive)
        ) {
            if (entity == owner || MarineAllyUtil.isMarineAlly(owner, entity)) {
                continue;
            }

            var centre = entity.getBoundingBox().getCenter();
            var strength = VaporShape.box(centre.x - getX(), centre.y - getY(), centre.z - getZ(), half) * freshness;

            VaporEffects.apply(level, kind, entity, strength, owner, VaporEffects.Origin.CHEM_THROWER, EFFECT_INTERVAL_TICKS, potency);
        }
    }

    /** The cloud: big, slow puffs in the vapor's cloud style through the 3 x 3 x 3, thinning as it disperses. */
    private void spawnParticles() {
        var freshness = freshness();
        var half = getHalf();
        var kind = getVapor();
        var style = com.human.common.gameplay.vapor.VaporLook.of(kind).cloud();

        // A wider cloud gets more puffs, so it reads as thick.
        var puffs = half > ChemCone.FAR_HALF ? 8 : 4;

        for (var i = 0; i < puffs; i++) {
            var dx = (random.nextDouble() * 2.0D - 1.0D) * half;
            var dy = (random.nextDouble() * 2.0D - 1.0D) * half;
            var dz = (random.nextDouble() * 2.0D - 1.0D) * half;

            if (random.nextFloat() < VaporShape.box(dx, dy, dz, half) * freshness) {
                // Big and slow; it rises or sinks as its vapor does (VaporLook), from the particle itself.
                var options = com.human.common.gameplay.vapor.VaporParticleOptions.of(kind, style, 0.9F, 1.8F, 30 + random.nextInt(20));

                level().addParticle(
                    options,
                    getX() + dx,
                    getY() + dy,
                    getZ() + dz,
                    (random.nextDouble() - 0.5D) * 0.02D,
                    0.0D,
                    (random.nextDouble() - 0.5D) * 0.02D
                );
            }
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {}
}
