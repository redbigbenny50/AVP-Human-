package com.human.common.gameplay.entity.living.dog;

import com.human.HumanResources;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.registry.init.HumanEntityTypes;
import com.human.mixin.MixinWolf_Accessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

    public MarineDog(EntityType<? extends MarineDog> entityType, Level level) {
        super(entityType, level);
    }

    public void assignMarineOwner(Marine marine) {
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

        var owner = getOwner();

        if (!(owner instanceof Marine) || !owner.isAlive() || owner.isRemoved()) {
            discard();
        }
    }

    @Override
    public ResourceLocation getTexture() {
        return TEXTURE;
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand interactionHand) {
        return InteractionResult.CONSUME;
    }

    @Override
    public void tame(Player player) {
        // Marine dogs are assigned to Marines only.
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
