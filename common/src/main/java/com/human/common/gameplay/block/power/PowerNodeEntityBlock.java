package com.human.common.gameplay.block.power;

import com.human.common.gameplay.block.entity.power.PowerNodeBlockEntity;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.gameplay.power.grid.PowerGridExploreUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PowerNodeEntityBlock<T extends PowerNodeBlockEntity & PowerNode> extends BaseEntityBlock {

    /**
     * SNEAK + right-click a face to open or close its breaker.
     * <p>
     * ⭐ [stated] Sep 25: the wiring tool should be able to toggle ports "instead just relying on the gui". This is that
     * interaction, on the shared base so EVERY power machine gets it rather than the battery alone - and it is what
     * makes the port state testable before either the screen or the tool exists.
     * </p>
     * <p>
     * ⚠ SNEAK is required so a plain right-click stays free for the machine's own screen. A toggle is a topology
     * change, so {@code setPortClosed} routes it through the queued split the block-removal path already uses; the face
     * the player clicked is the one that changes, which is the only unambiguous meaning of "this port".
     * </p>
     */
    /**
     * Oct 10 - THE WIRING TOOL GETS THROUGH. [reported] "the terminal wont connect to the fuse panel no matter what
     * they do with the wiring tool". Minecraft runs a block's own right-click (useWithoutItem) BEFORE the held item's
     * useOn, and stops there if the block consumes the click. The desk terminal consumes it to open its screen, so with
     * the tool in hand the click opened (or tried to open) the terminal and the tool never saw it: binding to a fuse
     * panel was impossible. Sneaking skips the block but sends the tool down its breaker-toggle branch instead of
     * binding. The same swallow applied to every power block with a screen - machines (which bind with a hub chip), the
     * resonator, the charging hub, the cryotube controller, batteries and the fuse panel itself.
     * <p>
     * Holding the wiring tool now returns SKIP_DEFAULT_BLOCK_INTERACTION, which tells vanilla to skip the block's own
     * click and go straight to the tool. Blocks with their own item handling ask {@link #isWiringTool} first.
     */
    @Override
    protected @NotNull net.minecraft.world.ItemInteractionResult useItemOn(
        @NotNull net.minecraft.world.item.ItemStack stack,
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull net.minecraft.world.entity.player.Player player,
        @NotNull net.minecraft.world.InteractionHand hand,
        @NotNull net.minecraft.world.phys.BlockHitResult hitResult
    ) {
        if (isWiringTool(stack)) {
            return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    /** Oct 10 - {@return whether this is the wiring tool, whose clicks must always reach the tool} */
    protected static boolean isWiringTool(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof com.human.common.gameplay.item.WiringToolItem;
    }

    @Override
    protected @NotNull net.minecraft.world.InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull net.minecraft.world.entity.player.Player player,
        @NotNull net.minecraft.world.phys.BlockHitResult hitResult
    ) {
        if (!player.isShiftKeyDown()) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (!(level.getBlockEntity(pos) instanceof PowerNodeBlockEntity node)) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        var face = hitResult.getDirection();

        // ⚠ A face that can never carry power has no breaker - a machine's sides, a battery's front. That click belongs
        // to the block's own interaction, so hand it back rather than swallowing it with a silent no-op.
        if (!node.canConnectOn(face)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (!level.isClientSide) {
            var closed = !node.getPorts().isClosed(face);

            node.setPortClosed(face, closed);
            level.playSound(
                null,
                pos,
                closed ? net.minecraft.sounds.SoundEvents.LEVER_CLICK : net.minecraft.sounds.SoundEvents.LEVER_CLICK,
                net.minecraft.sounds.SoundSource.BLOCKS,
                0.6F,
                closed ? 0.6F : 1.2F
            );
            player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                    closed ? "message.avp_human.port_closed" : "message.avp_human.port_opened",
                    net.minecraft.network.chat.Component.translatable(
                        "message.avp_human.direction." + face.getSerializedName()
                    )
                ),
                true
            );
        }

        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static final Direction[] VALUES = Direction.values();

    public PowerNodeEntityBlock(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <U extends BlockEntity> BlockEntityTicker<U> getTicker(
        @NotNull Level level,
        @NotNull BlockState blockState,
        @NotNull BlockEntityType<U> blockEntityType
    ) {
        return level.isClientSide
            ? null
            : createTickerHelper(
                blockEntityType,
                (BlockEntityType<T>) blockEntityType,
                (lvl, pos, state, be) -> be.serverTick(lvl, pos, state)
            );
    }

    @Override
    protected void onPlace(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (level.isClientSide) {
            return;
        }

        var manager = PowerSystem.get((ServerLevel) level);

        for (var direction : VALUES) {
            var neighbor = pos.relative(direction);

            // ⚠⚠ PORTS DECIDE, ON BOTH SIDES OF THE FACE. This used to union with any cable or power block touching it,
            // so a machine placed sideways against a battery would join its grid through a face that can never carry
            // power, and a block placed against a CLOSED breaker merged straight through it until the next rebuild.
            // The grid walk already asked the ports; placement now asks exactly the same question.
            if (PowerGridExploreUtil.portsOpen(level, pos, direction) && PowerGridExploreUtil.isConnectable(level, neighbor)) {
                manager.union(pos, neighbor);
            }
        }
    }

    @Override
    protected void onRemove(
        @NotNull BlockState state,
        Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            // Queued, not rebuilt now: one explosion can remove hundreds of blocks in a single tick, and each
            // immediate rebuild walked the whole network up to six times.
            PowerSystem.get((ServerLevel) level).queueSplit(pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
