package com.human.client.animation.item;

import com.blib.api.client.animation.v1.animator.AzAnimatorConfig;
import com.blib.api.client.animation.v1.animator.AzItemAnimator;
import com.human.common.gameplay.item.gun.animation.GunAnimationEvents;
import com.human.common.gameplay.item.gun.animation.dispatcher.GunAnimationDispatcher;
import com.human.common.gameplay.item.gun.animation.dispatcher.impl.OldPainlessAnimationDispatcher;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public abstract class GunItemAnimator extends AzItemAnimator {

    private int previousAnimationId = Integer.MIN_VALUE;

    protected GunItemAnimator() {
        super(AzAnimatorConfig.defaultConfig());
    }

    @Override
    public void setCustomAnimations(ItemStack animatable, float partialTicks) {
        super.setCustomAnimations(animatable, partialTicks);
    }

    protected void runGunAnimationEvents(ItemStack itemStack, GunAnimationDispatcher dispatcher) {
        var animationId = itemStack.getOrDefault(HumanDataComponents.GUN_ANIMATION_ID.get(), 0);

        if (animationId == previousAnimationId) {
            return;
        }

        previousAnimationId = animationId;

        if (animationId == 0) {
            return;
        }

        var player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        switch (itemStack.getOrDefault(HumanDataComponents.GUN_ANIMATION_TYPE.get(), GunAnimationEvents.NONE)) {
            case GunAnimationEvents.SHOOT -> dispatcher.shoot(player, itemStack);
            case GunAnimationEvents.RELOAD -> dispatcher.reload(player, itemStack);
            case GunAnimationEvents.OLD_PAINLESS_SPIN_LOOP -> OldPainlessAnimationDispatcher.INSTANCE.spinLoop(player, itemStack);
            case GunAnimationEvents.OLD_PAINLESS_SPIN_DOWN -> OldPainlessAnimationDispatcher.INSTANCE.spinDown(player, itemStack);
            case GunAnimationEvents.OLD_PAINLESS_SPIN_UP -> OldPainlessAnimationDispatcher.INSTANCE.spinUp(player, itemStack);
            default -> {
                // No animation event to run.
            }
        }
    }
}
