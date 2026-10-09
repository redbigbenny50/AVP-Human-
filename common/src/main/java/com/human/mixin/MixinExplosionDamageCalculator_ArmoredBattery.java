package com.human.mixin;

import com.human.common.gameplay.block.entity.power.impl.BatteryBlockEntity;
import com.human.common.gameplay.block.power.BatteryBlock;
import com.human.common.registry.init.HumanBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * [stated] Sep 29: a battery with the armoured upgrade has OBSIDIAN blast resistance.
 * <p>
 * Vanilla reads blast resistance from the BLOCK, one value for every battery, so an upgrade in one battery cannot
 * change it there. Every explosion asks {@code ExplosionDamageCalculator.getBlockExplosionResistance} per block - on
 * both loaders; NeoForge changes the method's body but keeps its signature (checked in both jars) - so the answer is
 * raised here, at RETURN, for an armoured battery only. Entity-specific calculators (a creeper's, a wither skull's)
 * extend this one and call it through super, so they are covered too.
 * </p>
 */
@Mixin(ExplosionDamageCalculator.class)
public abstract class MixinExplosionDamageCalculator_ArmoredBattery {

    @Inject(method = "getBlockExplosionResistance", at = @At("RETURN"), cancellable = true)
    private void avp_human$armoredBattery(
        Explosion explosion,
        BlockGetter reader,
        BlockPos pos,
        BlockState state,
        FluidState fluid,
        CallbackInfoReturnable<Optional<Float>> cir
    ) {
        if (!state.is(HumanBlocks.BATTERY.get())) {
            return;
        }

        if (reader.getBlockEntity(pos) instanceof BatteryBlockEntity battery && battery.isArmored()) {
            var current = cir.getReturnValue().orElse(0.0F);

            cir.setReturnValue(Optional.of(Math.max(current, BatteryBlock.ARMORED_BLAST_RESISTANCE)));
        }
    }
}
