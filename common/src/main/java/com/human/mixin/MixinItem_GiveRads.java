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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public class MixinItem_GiveRads {

    /** Stack size is folded in at this granularity: every sixteen items adds one more multiple of the base rate. */
    @Unique
    private static final int STACK_SCALING_DIVISOR = 16;

    /** Ceiling on a single stack's contribution, so a shulker of uranium cannot outrun the ladder entirely. */
    @Unique
    private static final int MAX_ITEM_RATE = 20;

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

        // A hot item in the pocket marks the carrier as exposed for as long as it is carried; the exposure manager
        // turns sustained contact into a rising sickness level. Drop it and the marking stops, so the level decays.
        //
        // Strength scales with BOTH what it is and HOW MUCH of it there is: a single nugget is a manageable risk,
        // a stack of solid blocks is a death sentence. Items nested inside a lead chest never reach this code at
        // all - they are not loose inventory slots - so shielded hauling stays completely safe.
        ((RadiationExposure) livingEntity).avp_human$markRadiationSource(avp_human$radiationRateFor(stack));
    }

    @Unique
    private static int avp_human$radiationRateFor(ItemStack stack) {
        var potency = 1;

        if (stack.is(HumanItemTags.EXTREMELY_RADIOACTIVE_ITEMS)) {
            potency = 4;
        } else if (stack.is(HumanItemTags.HIGHLY_RADIOACTIVE_ITEMS)) {
            potency = 2;
        }

        // A full stack is worth roughly five singles rather than sixty-four, so hauling is punishing but not absurd.
        var rate = potency * (STACK_SCALING_DIVISOR + stack.getCount()) / STACK_SCALING_DIVISOR;

        return Math.min(MAX_ITEM_RATE, rate);
    }
}
