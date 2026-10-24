package com.human.mixin.client;

import com.human.client.input.AdsSightCalibration;
import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.old_painless.OldPainlessItem;
import com.human.common.registry.init.item.HumanGunItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer_AdjustHandRenderingForGun {

    private static final float ADS_HORIZONTAL_OFFSET = 0.56F;

    private static final float ADS_VERTICAL_OFFSET = 0.12F;

    private static final float ADS_DEPTH_OFFSET = -0.2F;

    @Mutable
    @Shadow
    @Final
    private final Minecraft minecraft;

    @Shadow
    private float mainHandHeight;

    @Shadow
    private float offHandHeight;

    @Shadow
    private ItemStack mainHandItem;

    @Shadow
    private ItemStack offHandItem;

    protected MixinItemInHandRenderer_AdjustHandRenderingForGun(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void cancelAnimation(CallbackInfo ci) {
        var clientPlayerEntity = this.minecraft.player;
        assert clientPlayerEntity != null;
        var itemStack = clientPlayerEntity.getMainHandItem();
        var itemStack2 = clientPlayerEntity.getOffhandItem();
        if ((this.mainHandItem.getItem() instanceof GunItem) && ItemStack.isSameItem(mainHandItem, itemStack)) {
            this.mainHandHeight = 1;
            this.mainHandItem = itemStack;
        }
        if ((this.offHandItem.getItem() instanceof GunItem) && ItemStack.isSameItem(offHandItem, itemStack2)) {
            this.offHandHeight = 1;
            this.offHandItem = itemStack2;
        }
    }

    @Inject(
        method = "renderArmWithItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            ordinal = 1
        )
    )
    private void avp_human$centerGunWhenAiming(
        AbstractClientPlayer player,
        float partialTicks,
        float pitch,
        InteractionHand hand,
        float swingProgress,
        ItemStack stack,
        float equippedProgress,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int combinedLight,
        CallbackInfo callbackInfo
    ) {
        if (
            !(stack.getItem() instanceof GunItem)
                || stack.getItem() instanceof OldPainlessItem
                || stack.getItem() == HumanGunItems.M6B_ROCKET_LAUNCHER.get()
                || !player.isUsingItem()
                || player.getUsedItemHand() != hand
        ) {
            return;
        }

        var arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        var side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        poseStack.translate(-ADS_HORIZONTAL_OFFSET * side, ADS_VERTICAL_OFFSET, ADS_DEPTH_OFFSET);
        applyGunSpecificAdsOffset(poseStack, stack, side);
    }

    private static void applyGunSpecificAdsOffset(PoseStack poseStack, ItemStack stack, float side) {
        var item = stack.getItem();

        if (item == HumanGunItems.F903WE_RIFLE.get()) {
            poseStack.translate(AdsSightCalibration.horizontalOffset(item) * side, -0.008F, 0.0F);
        } else if (item == HumanGunItems.ZX_76_SHOTGUN.get()) {
            poseStack.translate(0.020F * side, -0.012F, 0.0F);
        } else if (item == HumanGunItems.M37_12_SHOTGUN.get()) {
            poseStack.translate(AdsSightCalibration.horizontalOffset(item) * side, -0.010F, 0.0F);
        } else if (com.human.common.gameplay.item.gun.chem.ChemThrowerReloading.isChemThrower(item)) {
            poseStack.translate(CHEM_ADS_SIDEWAYS * side, CHEM_ADS_DOWN, CHEM_ADS_FORWARD);
        }
    }

    /**
     * [stated] Oct 19 - aiming the chem thrower, the camera ended up inside it and saw through the tank: "it needs to
     * move down and more center when aiming down sights you should be looking over the tank not just next to the gun".
     * On top of the shared aim offset: further toward the middle of the screen (so the tank sits under the crosshair),
     * lower (so the eye looks over the tank's top), and further forward (so the camera is no longer inside the model).
     * ⚠ First guesses, to be tuned in game - each is one number, in blocks.
     */
    private static final float CHEM_ADS_SIDEWAYS = -0.10F;

    private static final float CHEM_ADS_DOWN = -0.16F;

    private static final float CHEM_ADS_FORWARD = -0.18F;
}
