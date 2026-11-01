package com.human.mixin;

import com.human.common.gameplay.gene.Genes;
import com.human.common.util.GeneValues;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sterile (defect): the animal can never fall in love, so it cannot breed. */
@Mixin(Animal.class)
public abstract class MixinAnimal_GeneDefects {

    @Inject(method = "canFallInLove", at = @At("RETURN"), cancellable = true)
    private void avp_human$sterile(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && GeneValues.carriesDefect((Animal) (Object) this, Genes.STERILE)) {
            cir.setReturnValue(false);
        }
    }
}
