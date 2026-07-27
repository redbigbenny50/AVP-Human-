package com.human.client.render.item.gun.muzzled.impl;

import com.blib.api.client.render.v1.AzRendererPipelineContext;
import com.human.client.animation.item.OldPainlessAnimator;
import com.human.client.render.item.gun.muzzled.MuzzledGunItemRenderer;
import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class OldPainlessItemRenderer extends MuzzledGunItemRenderer {

    private static final String BARREL_BONE_NAME = "gBarrel";

    private static final double BARREL_SPIN_RADIANS_PER_TICK = 2.8D;

    private static final List<String> MUZZLE_FLASH_BONE_NAME_LIST = List.of(
        "gFlash",
        "gFlash2",
        "gFlash3",
        "gFlash4",
        "gFlash5",
        "gFlash6"
    );

    public OldPainlessItemRenderer(String name) {
        super(
            name,
            MUZZLE_FLASH_BONE_NAME_LIST,
            createOldPainlessPrerender(),
            config -> config
                .setAnimatorProvider(OldPainlessAnimator::new)
        );
    }

    private static Function<AzRendererPipelineContext<UUID, ItemStack>, AzRendererPipelineContext<UUID, ItemStack>> createOldPainlessPrerender() {
        return context -> {
            var itemStack = context.animatable();
            var gameTime = getGameTime();
            var isFiring = itemStack.getOrDefault(HumanDataComponents.IS_FIRING.get(), false);
            var muzzleFlashDuration = itemStack.getOrDefault(HumanDataComponents.MUZZLE_FLASH_DURATION_IN_TICKS.get(), 0);
            var shouldShowMuzzleFlash = isFiring
                && muzzleFlashDuration > 0
                && gameTime != Long.MIN_VALUE
                && gameTime % 2L == 0L;
            var barrel = context.bakedModel().getBoneOrNull(BARREL_BONE_NAME);

            if (barrel != null) {
                var barrelRotation = isFiring && gameTime != Long.MIN_VALUE
                    ? (float) ((gameTime * BARREL_SPIN_RADIANS_PER_TICK) % (Math.PI * 2.0D))
                    : 0.0F;

                barrel.setRotZ(barrelRotation);
            }

            MUZZLE_FLASH_BONE_NAME_LIST.forEach(muzzleFlashBoneName -> {
                var maybeBone = context.bakedModel().getBoneOrNull(muzzleFlashBoneName);

                if (maybeBone != null) {
                    maybeBone.setHidden(!shouldShowMuzzleFlash);
                }
            });

            return context;
        };
    }

    private static long getGameTime() {
        var level = Minecraft.getInstance().level;

        return level == null ? Long.MIN_VALUE : level.getGameTime();
    }
}
