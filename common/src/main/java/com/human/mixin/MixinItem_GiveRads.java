package com.human.mixin;

import com.human.common.model.RadiationExposure;
import com.human.common.registry.tag.HumanItemTags;
import com.human.util.HumanPredicates;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public class MixinItem_GiveRads {

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void giveRads(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected, CallbackInfo ci) {
        if (
            // Only want to run this logic server-side.
            level.isClientSide()
                // Only run this logic for radiation-emitting items.
                || !stack.is(HumanItemTags.RADIOACTIVE_ITEMS)
                // Only run this logic if the entity can be irradiated.
                || !HumanPredicates.canBeIrradiated(entity)
                // Sanity check + allow compiler to assert entity type to get livingEntity ref access.
                || !(entity instanceof LivingEntity livingEntity)
        ) {
            return;
        }

        // A hot item in the pocket is a BASELINE source: it marks the carrier as exposed for as long as it is
        // carried, and the exposure manager turns sustained contact into a rising sickness level. Drop the item
        // and the marking stops, so the level starts decaying back down.
        ((RadiationExposure) livingEntity).avp_human$markRadiationSource(1);
    }
}
