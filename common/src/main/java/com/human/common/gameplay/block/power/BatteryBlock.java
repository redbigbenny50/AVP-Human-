package com.human.common.gameplay.block.power;

import com.human.common.gameplay.block.entity.power.impl.BatteryBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The battery: a plain full block, front gauge, power ports on the other five faces.
 * <p>
 * ⚠ A BAKED MODEL, NOT A GEO MODEL. Unlike the charging hub it renders as an ordinary block model, so it keeps the
 * default {@link RenderShape#MODEL}, culls against neighbours like any cube, and costs nothing per frame. The gauge
 * bars are an overlay face selected by {@link #CHARGE}.
 * </p>
 */
public class BatteryBlock extends PowerNodeEntityBlock<BatteryBlockEntity> {

    public static final MapCodec<BatteryBlock> CODEC = simpleCodec(BatteryBlock::new);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Gauge bars lit on the front: 0 empty, 1-4 one per started quarter. */
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 4);

    public BatteryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CHARGE);
    }

    /** The gauge faces the player who placed it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NotNull BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /**
     * ⚠ BaseEntityBlock defaults this to INVISIBLE. Every block entity block has to say MODEL explicitly or its baked
     * model is never drawn.
     */
    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Bare hand: the battery's screen (upgrades, store mode, module settings). Sneaking still goes to the base block,
     * which toggles the clicked face's breaker - unchanged.
     */
    @Override
    protected @NotNull net.minecraft.world.InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull net.minecraft.world.level.Level level,
        @NotNull BlockPos pos,
        @NotNull net.minecraft.world.entity.player.Player player,
        @NotNull net.minecraft.world.phys.BlockHitResult hitResult
    ) {
        if (player.isShiftKeyDown()) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BatteryBlockEntity battery) {
            player.openMenu(battery);
        }

        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Whoever places the battery hears its alarm module. */
    @Override
    public void setPlacedBy(
        @NotNull net.minecraft.world.level.Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @Nullable net.minecraft.world.entity.LivingEntity placer,
        @NotNull net.minecraft.world.item.ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (
            !level.isClientSide
                && placer instanceof net.minecraft.world.entity.player.Player player
                && level.getBlockEntity(pos) instanceof BatteryBlockEntity battery
        ) {
            battery.setOwner(player.getUUID());
        }
    }

    /**
     * [stated] Sep 29: the armoured upgrade gives OBSIDIAN hardness. Mining progress is inversely proportional to
     * hardness (vanilla {@code BlockBehaviour.getDestroyProgress} divides by it), so scaling it by base / obsidian is
     * exactly as slow as a block of hardness {@value #ARMORED_HARDNESS} - with every tool, enchantment and effect still
     * counted by vanilla. Blast resistance is {@code MixinExplosionDamageCalculator_ArmoredBattery}.
     */
    @Override
    protected float getDestroyProgress(
        @NotNull BlockState state,
        @NotNull net.minecraft.world.entity.player.Player player,
        @NotNull net.minecraft.world.level.BlockGetter level,
        @NotNull BlockPos pos
    ) {
        var progress = super.getDestroyProgress(state, player, level, pos);
        var hardness = state.getDestroySpeed(level, pos);

        if (hardness > 0.0F && level.getBlockEntity(pos) instanceof BatteryBlockEntity battery && battery.isArmored()) {
            return progress * hardness / ARMORED_HARDNESS;
        }

        return progress;
    }

    /** Obsidian's numbers. */
    public static final float ARMORED_HARDNESS = 50.0F;

    public static final float ARMORED_BLAST_RESISTANCE = 1200.0F;

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new BatteryBlockEntity(blockPos, blockState);
    }
}
