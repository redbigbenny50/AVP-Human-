package com.human.mixin;

import com.human.common.gameplay.fluid.FluidFallCushion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets our liquids change a landing: latex absorbs it and bounces, honey halves the damage. The rules themselves are in
 * {@link FluidFallCushion}; this only gives them the two moments vanilla offers.
 * <ol>
 * <li><b>HEAD of {@code checkFallDamage}</b> - the entity has just touched the ground and nothing has been dealt yet.
 * Every entity passes through here on landing, players included ({@code ServerPlayer.doCheckFallDamage} calls it via
 * super). The fall distance is adjusted before vanilla reads it.</li>
 * <li><b>After {@code Block.updateEntityAfterFallOn}</b> in {@code move} - the ground block has just zeroed the
 * vertical speed. A latex bounce is applied here, or it would be wiped the moment it was set.</li>
 * </ol>
 * Both hooks run on client and server alike, so a player's own predicted movement bounces the same as the server's.
 */
@Mixin(Entity.class)
public abstract class MixinEntity_FluidFallCushion {

    @Unique
    private double avp_human$pendingBounce;

    @Inject(method = "checkFallDamage", at = @At("HEAD"))
    private void avp_human$cushionLanding(double y, boolean onGround, BlockState state, BlockPos pos, CallbackInfo ci) {
        avp_human$pendingBounce = onGround ? FluidFallCushion.onLanding(Entity.class.cast(this)) : 0.0D;
    }

    @Inject(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V",
            shift = At.Shift.AFTER
        )
    )
    private void avp_human$applyBounce(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (avp_human$pendingBounce <= 0.0D) {
            return;
        }

        var self = Entity.class.cast(this);
        var motion = self.getDeltaMovement();

        self.setDeltaMovement(motion.x, avp_human$pendingBounce, motion.z);
        avp_human$pendingBounce = 0.0D;
    }
}
