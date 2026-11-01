package com.human.mixin;

import com.human.common.gameplay.gene.PlayerIntelligence;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * [stated] Oct 30: the Intelligence gene raises the XP a player gets from orbs (see PlayerIntelligence). Wraps the one
 * call in {@code playerTouch} that hands the orb's points (after any Mending repair) to the player.
 */
@Mixin(ExperienceOrb.class)
public abstract class MixinExperienceOrb_IntelligenceGain {

    @Redirect(
        method = "playerTouch",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V")
    )
    private void avp_human$intelligenceGain(Player player, int amount) {
        player.giveExperiencePoints(PlayerIntelligence.scaleGain(player, amount));
    }
}
