package com.human.neoforge.mixin.sable;

import com.human.common.gameplay.ship.ShipMoves;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * [stated] Oct 21 - Create Aeronautics ships: "I want to make sure it moving with power, fluid, or items systems wont
 * break them." Sable moves EVERY block of a ship - when it is built out of the world and when it lands back - through
 * {@code SubLevelAssemblyHelper.moveBlocks}: each block's data is saved, the block is placed at its new position
 * (turned by the move's rotation), the data is loaded back, and the old position is cleared. Verified from the Sable
 * 2.0.6 jar.
 * <p>
 * Around that one call: {@link ShipMoves#begin} before, {@link ShipMoves#finish} after, with the move's own mapping and
 * rotation - so a moved fuse panel still finds its devices, a cryotank its master, a pipe its flange settings on the
 * right faces, a cable its port modes. In flight nothing moves (only the ship's pose changes), so nothing else is
 * needed.
 * </p>
 * Applied only when Sable is installed ({@code SableMixinPlugin}); targeted by name so nothing here loads without it. A
 * stack, not a field, in case one move ever starts another.
 */
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper", remap = false)
public abstract class MixinSubLevelAssemblyHelper_ShipMove {

    @Unique
    private static final Deque<ShipMoves.Batch> avp_human$batches = new ArrayDeque<>();

    @Inject(method = "moveBlocks", at = @At("HEAD"), remap = false)
    private static void avp_human$beginShipMove(
        ServerLevel level,
        SubLevelAssemblyHelper.AssemblyTransform transform,
        Iterable<BlockPos> blocks,
        CallbackInfo callbackInfo
    ) {
        avp_human$batches.push(ShipMoves.begin(level, blocks));
    }

    @Inject(method = "moveBlocks", at = @At("RETURN"), remap = false)
    private static void avp_human$finishShipMove(
        ServerLevel level,
        SubLevelAssemblyHelper.AssemblyTransform transform,
        Iterable<BlockPos> blocks,
        CallbackInfo callbackInfo
    ) {
        var batch = avp_human$batches.poll();

        if (batch != null) {
            ShipMoves.finish(batch, transform.getLevel(), transform.getRotation(), transform::apply);
        }
    }
}
