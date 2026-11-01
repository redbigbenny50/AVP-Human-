package com.human.mixin;

import com.human.common.gameplay.harvest.HarvestGenes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Harvest genes: a bucket, bowl or shears used on a mob that carries the matching action gene. Runs in
 * {@code Entity.interact}, which {@code Mob.interact} reaches after leads/name tags and BEFORE the mob's own
 * interaction - so a gene harvest takes the click before vanilla's (vanilla-identical cases are left out of the tool
 * flag, see HarvestGenes.toolMask).
 * <p>
 * ⚠ Both sides answer the same: the client claims the click from the SYNCED tool flag (it has no genes), the server
 * decides from the genes. A client that let the click through would also run the bucket's own use and scoop whatever
 * fluid is behind the mob.
 * </p>
 */
@Mixin(Entity.class)
public abstract class MixinEntity_HarvestGenes {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void avp_human$harvestGenes(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!((Object) this instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }

        var tool = HarvestGenes.Tool.of(player.getItemInHand(hand));

        if (tool == null) {
            return;
        }

        if (target.level().isClientSide) {
            if ((HarvestGenes.syncedMask(target) & tool.bit) != 0) {
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            return;
        }

        if (HarvestGenes.interact(player, hand, target, tool)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
