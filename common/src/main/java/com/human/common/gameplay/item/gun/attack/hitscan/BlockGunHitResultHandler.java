package com.human.common.gameplay.item.gun.attack.hitscan;

import com.blib.api.common.block.v1.BlockBreakProgressManager;
import com.blib.api.common.enchantment.v1.EnchantmentUtil;
import com.blib.api.common.tag.v1.BLibBlockTags;
import com.human.Human;
import com.human.common.gameplay.item.gun.attack.GunAttackConfig;
import com.human.common.gameplay.item.gun.attack.GunHitResult;
import com.human.common.network.packet.S2CBulletHitBlockPayload;
import com.human.common.property.HumanProperties;
import com.human.common.property.HumanPropertyAccess;
import com.human.common.registry.init.HumanSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockGunHitResultHandler {

    public static void handle(GunAttackConfig gunAttackConfig, GunHitResult.Block gunHitResult, int pierceIndex) {
        var blockPos = gunHitResult.blockPos();
        var direction = gunHitResult.direction();
        var level = gunAttackConfig.shooter().level();
        var blockState = level.getBlockState(blockPos);
        var soundType = blockState.getSoundType();

        var ricochetSoundEvent = getRicochetSoundForSoundType(soundType);
        level.playSound(null, blockPos, ricochetSoundEvent, SoundSource.BLOCKS);

        damageBlock(gunAttackConfig, level, blockPos, blockState, pierceIndex);

        var payload = new S2CBulletHitBlockPayload(blockPos, direction);
        Human.MOD.networking().sendToAllClients(level.getServer(), payload);
    }

    private static SoundEvent getRicochetSoundForSoundType(SoundType soundType) {
        SoundEvent ricochetSfx;

        if (soundType == SoundType.GLASS) {
            ricochetSfx = HumanSoundEvents.WEAPON_FX_RICOCHET_GLASS.get();
        } else if (soundType == SoundType.GRAVEL) {
            ricochetSfx = HumanSoundEvents.WEAPON_FX_RICOCHET_DIRT.get();
        } else if (soundType == SoundType.METAL) {
            ricochetSfx = HumanSoundEvents.WEAPON_FX_RICOCHET_METAL.get();
        } else {
            ricochetSfx = HumanSoundEvents.WEAPON_FX_RICOCHET_GENERIC.get();
        }
        return ricochetSfx;
    }

    private static void damageBlock(
        GunAttackConfig gunAttackConfig,
        Level level,
        BlockPos blockPos,
        BlockState blockState,
        int pierceIndex
    ) {
        if (
            !HumanPropertyAccess.INSTANCE.get(HumanProperties.Weapons.BULLETS_DAMAGE_BLOCKS_ENABLED)
                || !level.getGameRules().getBoolean(GameRules.RULE_PROJECTILESCANBREAKBLOCKS)
                // Only damage blocks if they should be destroyed.
                || blockState.is(BLibBlockTags.SHOULD_NOT_BE_DESTROYED)
                || (gunAttackConfig.shooter() instanceof Player player
                    && !Human.MOD.events().preBlockBreak().dispatcher().invoke(level, player, blockPos, blockState))
        ) {
            return;
        }

        var powerLevel = EnchantmentUtil.getLevel(level, gunAttackConfig.gunItemStack(), Enchantments.POWER);
        var baseDamage = gunAttackConfig.fireModeConfig().damage() * (1 + (0.25F * powerLevel));
        var multiplier = 1.0F - (0.2F * pierceIndex);
        var damage = baseDamage * multiplier;

        BlockBreakProgressManager.damage(level, blockPos, damage);
    }
}
