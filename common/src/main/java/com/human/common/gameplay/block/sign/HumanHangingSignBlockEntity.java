package com.human.common.gameplay.block.sign;

import com.human.common.registry.init.HumanSigns;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A hanging sign of ours.
 * <p>
 * ⚠ It EXTENDS vanilla's hanging sign rather than rebuilding one, because the client picks the hanging-sign edit screen
 * (the narrow board with its chains) by {@code instanceof HangingSignBlockEntity}, and the shorter text lines come from
 * the same class. Vanilla's only constructor files it under vanilla's own hanging-sign type, so {@link #getType}
 * answers ours instead - the type the renderer is looked up by, the save id and the update packet all read it from
 * there.
 * </p>
 */
public class HumanHangingSignBlockEntity extends HangingSignBlockEntity {

    public HumanHangingSignBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return HumanSigns.HANGING_SIGN_BLOCK_ENTITY.get();
    }

    /**
     * ⚠ The chunk checks a block entity against the block under it on every state change, by the TYPE FIELD (vanilla's
     * hanging-sign type here, which lists only vanilla's blocks) - and throws away and rebuilds the block entity when
     * it says no, wiping the text. Asked of our own type instead.
     */
    @Override
    public boolean isValidBlockState(net.minecraft.world.level.block.state.BlockState state) {
        return getType().isValid(state);
    }
}
