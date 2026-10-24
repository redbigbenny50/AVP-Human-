package com.human.common.gameplay.block.power;

import com.human.common.gameplay.block.entity.power.PowerNodeBlockEntity;
import com.human.common.gameplay.block.entity.power.impl.ResonatorBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * [stated] Oct 20 - the resonator (see ResonatorBlockEntity). The old resin cleaning and its resin-ball store are gone;
 * the wiring tool's shift-click shows its field.
 * <h2>⚠ Oct 21 - IT NEVER SPUN (tester: "cant seem to get it to work")</h2> Its spin animation is played on the CLIENT,
 * but the only thing that ever asked for it was the server's tick - and the animation code returns straight away on the
 * server. So no resonator ever spun, powered or not, and nothing told the player whether it was working. Now:
 * <ul>
 * <li>{@link #ACTIVE} - set by the server when it is powered and cleared about a second after power stops - is sent to
 * every player with the block, and a CLIENT tick plays the spin while it is set and the wind-down when it is not.</li>
 * <li>[stated] Oct 21 - "you need to add a feedback message when right clicking it": a plain right-click says whether
 * it is running and, if not, exactly why (no avp_alien, nothing feeding its underside, not enough power).</li>
 * </ul>
 */
public class ResonatorBlock extends PowerNodeEntityBlock<ResonatorBlockEntity> {

    public static final MapCodec<ResonatorBlock> CODEC = simpleCodec(ResonatorBlock::new);

    /** Running: powered now, or within the last second. Drives the spin on every client. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public ResonatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new ResonatorBlockEntity(blockPos, blockState);
    }

    /** The server tick as for every power block, plus a CLIENT tick for the animation. */
    @Override
    @SuppressWarnings("unchecked")
    public <U extends BlockEntity> @Nullable BlockEntityTicker<U> getTicker(
        @NotNull Level level,
        @NotNull BlockState blockState,
        @NotNull BlockEntityType<U> blockEntityType
    ) {
        if (!level.isClientSide) {
            return super.getTicker(level, blockState, blockEntityType);
        }

        return (lvl, pos, state, blockEntity) -> {
            if (blockEntity instanceof ResonatorBlockEntity resonator) {
                resonator.clientTick(state);
            }
        };
    }

    /**
     * A plain right-click: whether it is running, and if not, why. A sneak-click shows its field sphere (on the
     * underside, its power face, it works the breaker instead).
     */
    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hitResult
    ) {
        if (player.isShiftKeyDown()) {
            // Its underside is its only power face, so a sneak-click THERE works its breaker like any power block.
            if (hitResult.getDirection() == Direction.DOWN) {
                return super.useWithoutItem(state, level, pos, player, hitResult);
            }

            // ⚠ Oct 21 (tester: "does not show me a sphere when he shift rightclicks it") - the sphere was only on the
            // wiring tool's sneak-click. [stated] Oct 20 "shift right click should show the sphere": a sneak-click with
            // an empty hand on any other face shows it too. Drawn on the client only, through the server-safe hook.
            if (level.isClientSide) {
                com.human.common.gameplay.block.resonator.ResonatorSphereHook.show.accept(pos, ResonatorBlockEntity.RADIUS);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ResonatorBlockEntity resonator) {
            player.displayClientMessage(status(level, pos, state, resonator), true);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Component status(Level level, BlockPos pos, BlockState state, ResonatorBlockEntity resonator) {
        if (!com.human.compatibility.avp_alien.AVPAlien.MOD.isLoaded()) {
            return Component.translatable("message.avp_human.resonator.no_alien");
        }

        if (state.getValue(ACTIVE)) {
            return Component.translatable("message.avp_human.resonator.active", ResonatorBlockEntity.RADIUS);
        }

        if (resonator.getFusePanel() != null) {
            return Component.translatable("message.avp_human.resonator.panel_unpowered", ResonatorBlockEntity.POWER_PER_TICK);
        }

        var below = pos.below();
        var feedsUnderside = level.getBlockState(below).getBlock() instanceof CableBlock
            || level.getBlockEntity(below) instanceof PowerNodeBlockEntity node && node.isFaceConnectable(Direction.UP);

        if (!feedsUnderside) {
            return Component.translatable("message.avp_human.resonator.nothing_below", ResonatorBlockEntity.POWER_PER_TICK);
        }

        return Component.translatable("message.avp_human.resonator.underpowered", ResonatorBlockEntity.POWER_PER_TICK);
    }
}
