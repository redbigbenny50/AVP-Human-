package com.human.common.gameplay.cosmetic;

import com.human.common.registry.init.HumanEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The invisible seat a player rides while sitting on cosmetic furniture ([stated] Oct 31: chairs, the sofa and other
 * seating actually seat you). It stands exactly at the cushion top: vanilla puts a rider's feet 0.6 below the seat
 * point (the player's vehicle attachment, read from the 1.21.1 jar), so the hips land on the cushion.
 * <ul>
 * <li>Never saved; it removes itself as soon as nobody sits on it, or when its furniture is gone.</li>
 * <li>Standing up puts the player in front of the furniture, not on top of it, when that spot is clear.</li>
 * </ul>
 */
public class CosmeticSeatEntity extends Entity {

    /** The furniture's core block; null on clients (they only follow the server's removal). */
    private @Nullable BlockPos furniture;

    /** Which way the seat faces - the rider is turned to it on sitting and stands up on this side. */
    private Direction front = Direction.NORTH;

    public CosmeticSeatEntity(EntityType<? extends CosmeticSeatEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static CosmeticSeatEntity create(Level level, BlockPos furniture, Vec3 seat, Direction front) {
        var entity = new CosmeticSeatEntity(HumanEntityTypes.COSMETIC_SEAT.get(), level);
        entity.furniture = furniture;
        entity.front = front;
        entity.setPos(seat);
        return entity;
    }

    /** Whether someone already sits at {@code seat}. */
    public static boolean occupied(Level level, Vec3 seat) {
        return !level.getEntitiesOfClass(CosmeticSeatEntity.class, new AABB(seat, seat).inflate(0.2), Entity::isVehicle).isEmpty();
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        var gone = furniture == null || !(level().getBlockState(furniture).getBlock() instanceof CosmeticBlock);

        if (!isVehicle() || gone) {
            ejectPassengers();
            discard();
        }
    }

    @Override
    protected @NotNull Vec3 getPassengerAttachmentPoint(@NotNull Entity passenger, @NotNull EntityDimensions dimensions, float scale) {
        return Vec3.ZERO;
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        // In front of the furniture at floor level, when the player fits there; otherwise vanilla's choice.
        var floor = Vec3.atBottomCenterOf(BlockPos.containing(position().relative(front, 0.9)));
        var box = passenger.getDimensions(passenger.getPose()).makeBoundingBox(floor);

        if (level().noCollision(passenger, box)) {
            return floor;
        }

        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {}

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {}

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
