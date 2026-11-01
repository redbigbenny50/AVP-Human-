package com.human.mixin;

import com.human.common.gameplay.gene.infusion.PlayerGeneInfusion;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * [stated] Oct 30: a player respawns with their gene infusion still active - "another reason to be different from
 * potions". Vanilla builds the respawned player and copies what it keeps in {@code restoreFrom(old, keepEverything)};
 * effects are not among them and neither is the gene manager, so the infusion is copied here, after vanilla's copy,
 * with the time that was left. Covers both a death respawn and the return from the End.
 */
@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer_GeneInfusionRespawn {

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void avp_human$keepGeneInfusion(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo callback) {
        PlayerGeneInfusion.carryOver(oldPlayer, (ServerPlayer) (Object) this);
    }
}
