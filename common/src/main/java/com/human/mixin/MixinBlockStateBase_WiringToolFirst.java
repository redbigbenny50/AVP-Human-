package com.human.mixin;

import com.human.common.gameplay.item.WiringToolItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * [stated] Oct 14: "when holding a wiring tool it should not open the block you are clicking on. it should be trying to
 * wire it or give you one of the messages why it cant be wired."
 * <p>
 * Vanilla asks the BLOCK first (useItemOn, then - only on PASS_TO_DEFAULT_BLOCK_INTERACTION - its plain right-click,
 * useWithoutItem, which opens screens) and only then the ITEM (useOn). Read in the 1.21.1 ServerPlayerGameMode
 * bytecode; the client's MultiPlayerGameMode runs the same order. So a fuse panel, a generator, the cryo controller,
 * the fabricator - any block with a screen, from any mod - opened it before the wiring tool was ever asked.
 * </p>
 * <p>
 * SKIP_DEFAULT_BLOCK_INTERACTION is vanilla's own "not the block - go straight to the item" result: the block's click
 * never runs and WiringToolItem.useOn always gets the click, on both sides. Sneaking already skipped the block in
 * vanilla, so the tool's sneak actions are unchanged.
 * </p>
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MixinBlockStateBase_WiringToolFirst {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void avp_human$wiringToolFirst(
        ItemStack stack,
        Level level,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult,
        CallbackInfoReturnable<ItemInteractionResult> callbackInfo
    ) {
        if (stack.getItem() instanceof WiringToolItem) {
            callbackInfo.setReturnValue(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION);
        }
    }
}
