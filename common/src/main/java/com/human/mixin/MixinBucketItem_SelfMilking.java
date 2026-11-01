package com.human.mixin;

import com.human.common.data.HumanAdvancements;
import com.human.common.gameplay.gene.Genes;
import com.human.common.gameplay.harvest.HarvestGenes;
import com.human.common.model.GeneCarrier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [stated] Oct 30: "if the player has the milkable gene if they right click air with a bucket they get milk and the
 * rare achievement challenge 'self milker'".
 * <p>
 * An EMPTY bucket used by a player carrying the Milkable gene (from a gene infusion) fills with milk - but only when
 * the bucket is pointed at nothing: aimed at water, lava or a block it does exactly what vanilla does, so scooping
 * water never turns into milking yourself. Server side only (genes are server data); vanilla's own client prediction
 * for an empty-air bucket is a pass, and the server's answer updates the hand. Each milking also grants the hidden Self
 * Milker challenge (granting an earned advancement again does nothing).
 * </p>
 */
@Mixin(BucketItem.class)
public abstract class MixinBucketItem_SelfMilking {

    @Shadow
    @Final
    private Fluid content;

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void avp_human$milkYourself(
        Level level,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
    ) {
        if (level.isClientSide || content != Fluids.EMPTY || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        var genes = ((GeneCarrier) player).getOrCreateGeneManager().getGeneContainer();
        if (!HarvestGenes.carries(genes, Genes.MILKABLE)) {
            return;
        }
        // Only into thin air: anything the bucket would normally scoop or place against keeps vanilla's behaviour.
        if (player.pick(player.blockInteractionRange(), 0.0F, true).getType() != HitResult.Type.MISS) {
            return;
        }

        var held = player.getItemInHand(hand);
        var filled = ItemUtils.createFilledResult(held, player, new ItemStack(Items.MILK_BUCKET));
        level.playSound(null, player.blockPosition(), SoundEvents.COW_MILK, SoundSource.PLAYERS, 1.0F, 1.0F);
        HumanAdvancements.SELF_MILKER.grant(serverPlayer);
        cir.setReturnValue(InteractionResultHolder.sidedSuccess(filled, false));
    }
}
