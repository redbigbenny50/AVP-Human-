package com.human.mixin;

import com.human.common.gameplay.gene.Genes;
import com.human.common.util.GeneValues;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Metabolism: hunger drains slower (-50% exhaustion at the cap). A weakness carrier (the pig line) drains faster.
 */
@Mixin(Player.class)
public abstract class MixinPlayer_GeneMetabolism {

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float avp_human$metabolism(float exhaustion) {
        var metabolism = GeneValues.get((Player) (Object) this, Genes.METABOLISM);

        if (metabolism == 0) {
            return exhaustion;
        }

        return (float) (exhaustion * Math.max(0.0, 1.0 - metabolism));
    }
}
