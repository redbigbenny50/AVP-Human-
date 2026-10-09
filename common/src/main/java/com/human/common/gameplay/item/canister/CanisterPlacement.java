package com.human.common.gameplay.item.canister;

import com.human.common.gameplay.block.entity.CanisterBlockEntity;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * [stated] Sep 29: "shift + left-click places the canister down as a block".
 * <h2>⚠ WHY THIS IS AN EVENT AND NOT THE ITEM</h2> Vanilla asks the held item about a left-click only through
 * {@code Item.canAttackBlock}, and only at the moment a block would actually BREAK (read in
 * {@code ServerPlayerGameMode.destroyBlock} and {@code MultiPlayerGameMode.destroyBlock}) - in survival, after the
 * whole mining time. So each loader's own left-click event calls {@link #tryPlace} the instant the click lands:
 * Fabric's {@code AttackBlockCallback}, NeoForge's {@code LeftClickBlock}. Both fire on the client and the server; on
 * both, a cancelled client click still reaches the server (checked in each loader's code), which is where the block is
 * actually placed.
 * <h2>THE RULE</h2> Sneaking, canister in the MAIN hand, clicking a block face: the canister goes down against that
 * face like any block, carrying its fluid. Anything else - not sneaking, no canister, nowhere to put it - returns false
 * and the click mines as normal.
 */
public final class CanisterPlacement {

    private CanisterPlacement() {}

    /** @return true when the click was used to place a canister and must not also start mining */
    public static boolean tryPlace(Player player, Level level, InteractionHand hand, BlockPos clicked, Direction face) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || player.isSpectator()) {
            return false;
        }

        var stack = player.getMainHandItem();

        if (!stack.is(HumanItems.CANISTER.get())) {
            return false;
        }

        var target = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(face);
        var state = HumanBlocks.CANISTER_BLOCK.get().defaultBlockState();

        if (
            !level.getBlockState(target).canBeReplaced()
                || !state.canSurvive(level, target)
                || !level.isUnobstructed(state, target, CollisionContext.of(player))
                || !level.mayInteract(player, target)
                || !player.mayUseItemAt(target, face, stack)
        ) {
            return false;
        }

        if (level.isClientSide) {
            // The server places it; the client only stops the mining from starting.
            return true;
        }

        if (!level.setBlock(target, state, 3)) {
            return false;
        }

        if (level.getBlockEntity(target) instanceof CanisterBlockEntity canister) {
            canister.setContent(UniversalCanisterItem.getContent(stack));
        }

        var sound = state.getSoundType();

        level.playSound(
            null,
            target,
            sound.getPlaceSound(),
            SoundSource.BLOCKS,
            (sound.getVolume() + 1.0F) / 2.0F,
            sound.getPitch() * 0.8F
        );
        level.gameEvent(player, GameEvent.BLOCK_PLACE, target);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return true;
    }
}
