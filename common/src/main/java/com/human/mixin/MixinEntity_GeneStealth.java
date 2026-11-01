package com.human.mixin;

import com.human.common.gameplay.gene.Genes;
import com.human.common.util.GeneValues;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stealth at 100% = silent footsteps (Silent Steps was merged into Stealth). Hooks vanilla's single private step
 * emitter rather than playStepSound, which dozens of mobs override without calling super. The step vibration is
 * dampened too, so sculk and wardens don't hear the carrier walk.
 */
@Mixin(Entity.class)
public abstract class MixinEntity_GeneStealth {

    @Unique
    private static final double AVP_HUMAN$SILENT = 1.0 - 1.0E-6;

    @Inject(method = "walkingStepSound", at = @At("HEAD"), cancellable = true)
    private void avp_human$silentSteps(BlockPos pos, BlockState state, CallbackInfo ci) {
        var self = (Entity) (Object) this;
        // Your own steps are played by your own client, which has no genes - it reads the synced value instead.
        var stealth = self.level().isClientSide
            ? com.human.common.registry.init.HumanGeneSync.stealth(com.human.common.registry.init.HumanGeneSync.flags(self))
            : GeneValues.get(self, Genes.STEALTH);

        if (stealth >= AVP_HUMAN$SILENT) {
            ci.cancel();
        }
    }

    @Inject(method = "dampensVibrations", at = @At("RETURN"), cancellable = true)
    private void avp_human$silentVibrations(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && GeneValues.get((Entity) (Object) this, Genes.STEALTH) >= AVP_HUMAN$SILENT) {
            cir.setReturnValue(true);
        }
    }
}
