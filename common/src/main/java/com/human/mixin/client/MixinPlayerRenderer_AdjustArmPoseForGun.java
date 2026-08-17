package com.human.mixin.client;

import com.human.common.gameplay.item.GunItem;
import com.human.common.gameplay.item.gun.GunData;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public class MixinPlayerRenderer_AdjustArmPoseForGun {

    @Inject(method = "getArmPose", at = @At(value = "TAIL"), cancellable = true)
    private static void tryItemPose(AbstractClientPlayer player, InteractionHand hand, CallbackInfoReturnable<HumanoidModel.ArmPose> ci) {
        var itemstack = player.getItemInHand(hand);

        if (!(itemstack.getItem() instanceof GunItem gunItem)) {
            return;
        }

        // ⭐⭐ THE HEAVY WEAPONS ARE ALWAYS SHOULDERED WHILE HELD — no aiming or firing condition at all.
        // [stated] "i think that as long as you hold it that its always raised. this is a big gun and its silly if
        // its at your side." A minigun, a flamethrower and a smartgun are two-handed weapons; letting them hang at
        // the hip between bursts looked wrong from every other player's point of view.
        // <p>
        // ⚠ EVERY OTHER GUN STILL NEEDS A REASON TO COME UP, or a pistol would be permanently levelled. Those keep
        // the aiming-or-firing test below.
        // </p>
        var isHeavyWeapon = gunItem.getGunConfig() == GunData.OLD_PAINLESS
            || gunItem.getGunConfig() == GunData.FLAMETHROWER_SEVASTOPOL
            || gunItem.getGunConfig() == GunData.M56_SMARTGUN;

        if (isHeavyWeapon) {
            ci.setReturnValue(HumanoidModel.ArmPose.BOW_AND_ARROW);
            return;
        }

        // ⚠⚠ `isUsingItem()` IS AIMING, NOT FIRING — gating on it alone is why the arms never came up while shooting.
        // [stated] "the 3rd person view still doesnt raise its arms when firing or holding old painless which means
        // when people look at the player they arent."
        // <p>
        // A player FIRES with the ATTACK button (GunInputHandler streams C2SGunFirePayload); `isUsingItem()` is only
        // true while RIGHT-CLICK is held, i.e. while aiming.
        // </p>
        // <p>
        // ⭐ IS_FIRING IS THE RIGHT SIGNAL AND IT ALREADY SYNCS. GunItem.fire sets it on the STACK, and the component
        // is networkSynchronized, so a held stack carries it to every tracking client — which is exactly what a
        // THIRD-PERSON observer needs. Reading a client-local input flag here would pose only your own player.
        // </p>
        var isAiming = player.isUsingItem() && player.getUsedItemHand() == hand;
        var isFiring = itemstack.getOrDefault(HumanDataComponents.IS_FIRING.get(), false)
            && hand == InteractionHand.MAIN_HAND;

        if (!isAiming && !isFiring) {
            return;
        }

        ci.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_HOLD);
    }
}
