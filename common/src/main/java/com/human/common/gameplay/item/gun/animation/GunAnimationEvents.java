package com.human.common.gameplay.item.gun.animation;

import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.world.item.ItemStack;

public final class GunAnimationEvents {

    public static final int NONE = 0;

    public static final int SHOOT = 1;

    public static final int RELOAD = 2;

    public static final int OLD_PAINLESS_SPIN_LOOP = 3;

    public static final int OLD_PAINLESS_SPIN_DOWN = 4;

    public static final int OLD_PAINLESS_SPIN_UP = 5;

    public static void trigger(ItemStack itemStack, int animationType) {
        var animationId = itemStack.getOrDefault(HumanDataComponents.GUN_ANIMATION_ID.get(), 0);

        itemStack.set(HumanDataComponents.GUN_ANIMATION_TYPE.get(), animationType);
        itemStack.set(HumanDataComponents.GUN_ANIMATION_ID.get(), animationId + 1);
    }

    private GunAnimationEvents() {
        throw new UnsupportedOperationException();
    }
}
