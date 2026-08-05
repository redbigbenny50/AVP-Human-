package com.human.common.gameplay.item.gun.attack;

import com.blib.api.common.dismemberment.v1.hitbox.LimbHitPrediction;
import com.human.common.gameplay.item.gun.FireModeConfig;
import com.human.common.gameplay.item.gun.GunConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public record GunAttackConfig(
    GunConfig gunConfig,
    FireModeConfig fireModeConfig,
    LivingEntity shooter,
    ItemStack gunItemStack,
    float damageMultiplier,
    LimbHitPrediction prediction
) {

    public GunAttackConfig(GunConfig gunConfig, FireModeConfig fireModeConfig, LivingEntity shooter, ItemStack gunItemStack) {
        this(gunConfig, fireModeConfig, shooter, gunItemStack, 1.0F, null);
    }

    public GunAttackConfig(
        GunConfig gunConfig,
        FireModeConfig fireModeConfig,
        LivingEntity shooter,
        ItemStack gunItemStack,
        LimbHitPrediction prediction
    ) {
        this(gunConfig, fireModeConfig, shooter, gunItemStack, 1.0F, prediction);
    }
}
