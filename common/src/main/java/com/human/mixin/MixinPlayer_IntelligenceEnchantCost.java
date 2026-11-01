package com.human.mixin;

import com.human.common.gameplay.gene.PlayerIntelligence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * [stated] Oct 30: the Intelligence gene lowers the levels a player pays to enchant (see PlayerIntelligence). The
 * table's level REQUIREMENT is unchanged; only what is taken off is lowered.
 */
@Mixin(Player.class)
public abstract class MixinPlayer_IntelligenceEnchantCost {

    @ModifyVariable(method = "onEnchantmentPerformed", at = @At("HEAD"), argsOnly = true)
    private int avp_human$intelligenceEnchantCost(int levels, ItemStack stack) {
        return PlayerIntelligence.cutEnchantCost((Player) (Object) this, levels);
    }
}
