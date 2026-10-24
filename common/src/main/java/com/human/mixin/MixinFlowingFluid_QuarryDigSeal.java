package com.human.mixin;

import com.human.common.gameplay.quarry.QuarryDigSeal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Oct 18: liquid may not spread into a chunk a quarry is digging - the cell is sealed with a containment field instead.
 * {@code spreadTo} is the single step every downward and sideways flow of every flowing liquid (water, lava, modded)
 * goes through. See QuarryDigSeal.
 */
@Mixin(FlowingFluid.class)
public abstract class MixinFlowingFluid_QuarryDigSeal {

    @Inject(at = @At("HEAD"), method = "spreadTo", cancellable = true)
    private void avp_human$sealQuarryDig(
        LevelAccessor level,
        BlockPos pos,
        BlockState blockState,
        Direction direction,
        FluidState fluidState,
        CallbackInfo callbackInfo
    ) {
        if (QuarryDigSeal.refuseSpread(level, pos)) {
            callbackInfo.cancel();
        }
    }
}
