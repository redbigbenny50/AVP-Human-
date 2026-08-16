package com.human.mixin.client;

import com.human.client.effect.NukedFoliageTint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blends every leaf toward the fallout foliage colour inside the nuked biome.
 * <p>
 * POST-PROCESSING THE LOOKUP rather than re-registering handlers, deliberately. Re-registering would mean naming the
 * offending blocks, capturing whatever handler each already had so it could still be used outside the biome, and doing
 * all of it during {@code createDefault} - before datapack tags exist and at a point where mod block registration
 * ordering is not something to rely on. Wrapping the RESULT sidesteps every one of those: the original colour is simply
 * the value being returned, whoever produced it.
 * </p>
 * <p>
 * That is what makes this work for THIRD-PARTY TREES with no per-mod support. Any block extending {@link LeavesBlock}
 * is covered, whether it tints from the biome, hardcodes a colour, or has no handler at all. Leaves that already tint
 * correctly blend from their own colour toward the same colour, so they come out unchanged.
 * </p>
 * <p>
 * ⚠ A colour can only be applied where the model declares a {@code tintindex}. Vanilla's azalea leaves use
 * {@code block/cube_all} and have none, which is why this mod also ships model overrides for those two putting them on
 * {@code block/leaves}. A third-party leaf without a tint index will not be affected by this and would need the same
 * treatment.
 * </p>
 */
@Mixin(net.minecraft.client.color.block.BlockColors.class)
public class MixinBlockColors_NukedFoliage {

    @Inject(
        method = "getColor(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;I)I",
        at = @At("RETURN"),
        cancellable = true
    )
    private void avp_human$blendLeavesTowardFallout(
        BlockState state,
        BlockAndTintGetter level,
        BlockPos pos,
        int tintIndex,
        CallbackInfoReturnable<Integer> callbackInfo
    ) {
        // level and pos are nullable on this path - the item/inventory lookups pass neither.
        if (level == null || pos == null || !(state.getBlock() instanceof LeavesBlock)) {
            return;
        }

        var original = callbackInfo.getReturnValueI();
        var tinted = NukedFoliageTint.applyTo(original, level, pos);

        if (tinted != original) {
            callbackInfo.setReturnValue(tinted);
        }
    }
}
