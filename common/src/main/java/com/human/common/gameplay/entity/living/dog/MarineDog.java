package com.human.common.gameplay.entity.living.dog;

import com.human.HumanResources;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.registry.init.HumanEntityTypes;
import com.human.common.registry.init.item.HumanItems;
import com.human.mixin.MixinWolf_Accessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class MarineDog extends Wolf {

    private static final ResourceLocation TEXTURE = HumanResources.entityTextureLocation("marine_dog");

    private static final String NBT_ASSIGNED_TO_MARINE = "AssignedToMarine";

    /**
     * True once a Marine has claimed this dog. Only THEN does losing its owner mean anything.
     * <p>
     * The despawn rule used to read "no Marine owner" as "invalid", which was right for the patrol dogs it was written
     * for but killed a spawn-egg dog on its first tick, before a player could even look at it. A dog that never
     * belonged to a Marine is not an orphan; it is just a dog.
     */
    private boolean assignedToMarine;

    public MarineDog(EntityType<? extends MarineDog> entityType, Level level) {
        super(entityType, level);
    }

    public void assignMarineOwner(Marine marine) {
        assignedToMarine = true;

        setTame(true, true);
        setOwnerUUID(marine.getUUID());
        getNavigation().stop();
        setOrderedToSit(false);
        setInSittingPose(false);
        entityData.set(MixinWolf_Accessor.getDataCollarColor(), DyeColor.GREEN.getId());

        if (getItemBySlot(EquipmentSlot.BODY).isEmpty()) {
            setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.WOLF_ARMOR));
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        if (!assignedToMarine) {
            // Player-owned or still stray: it lives or dies on its own terms, like any other wolf.
            return;
        }

        var owner = getOwner();

        // A Marine's dog outlives neither its Marine nor a reassignment away from one. This is what stops patrol dogs
        // accumulating in the world forever, which is the reason the rule exists at all.
        if (!(owner instanceof Marine) || !owner.isAlive() || owner.isRemoved()) {
            discard();
        }
    }

    @Override
    public ResourceLocation getTexture() {
        return TEXTURE;
    }

    /**
     * A Marine's dog cannot be won over — it already has a handler. A stray one is tamed with CORNBREAD rather than the
     * bones a vanilla wolf wants: these are working dogs raised on rations, not wild animals.
     */
    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand interactionHand) {
        if (assignedToMarine) {
            return InteractionResult.CONSUME;
        }

        var heldItem = player.getItemInHand(interactionHand);

        if (isTame() || !heldItem.is(HumanItems.CORNBREAD.get())) {
            // Already someone's dog, or the player is holding something else: hand back to Wolf, which covers sitting,
            // healing, dyeing the collar and fitting armour.
            return super.mobInteract(player, interactionHand);
        }

        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        heldItem.consume(1, player);

        // Same one-in-three vanilla wolves use, so it takes a few pieces and feels like winning it over.
        if (getRandom().nextInt(3) == 0) {
            tame(player);
            setOrderedToSit(true);
            level().broadcastEntityEvent(this, EntityEvent.TAMING_SUCCEEDED);
        } else {
            level().broadcastEntityEvent(this, EntityEvent.TAMING_FAILED);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFood(@NotNull ItemStack itemStack) {
        return itemStack.is(HumanItems.CORNBREAD.get());
    }

    @Override
    public void tame(Player player) {
        if (assignedToMarine) {
            // Assigned dogs answer to their Marine and to nobody else.
            return;
        }

        super.tame(player);
        getNavigation().stop();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(NBT_ASSIGNED_TO_MARINE, assignedToMarine);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        assignedToMarine = tag.getBoolean(NBT_ASSIGNED_TO_MARINE);
    }

    @Override
    public boolean canMate(Animal animal) {
        return false;
    }

    public static MarineDog convertFromWolf(Wolf wolf, Marine marine) {
        if (!(wolf.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        var marineDog = HumanEntityTypes.MARINE_DOG.get().create(serverLevel);

        if (marineDog == null) {
            return null;
        }

        marineDog.moveTo(wolf.getX(), wolf.getY(), wolf.getZ(), wolf.getYRot(), wolf.getXRot());
        marineDog.setDeltaMovement(wolf.getDeltaMovement());
        marineDog.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(wolf.blockPosition()), MobSpawnType.CONVERSION, null);
        marineDog.assignMarineOwner(marine);

        var bodyArmor = wolf.getItemBySlot(EquipmentSlot.BODY);

        if (!bodyArmor.isEmpty()) {
            marineDog.setItemSlot(EquipmentSlot.BODY, bodyArmor.copy());
        }

        if (wolf.hasCustomName()) {
            marineDog.setCustomName(wolf.getCustomName());
            marineDog.setCustomNameVisible(wolf.isCustomNameVisible());
        }

        marineDog.setHealth(Math.min(marineDog.getMaxHealth(), Math.max(wolf.getHealth(), 1.0F)));
        serverLevel.addFreshEntityWithPassengers(marineDog);
        wolf.discard();

        return marineDog;
    }
}
