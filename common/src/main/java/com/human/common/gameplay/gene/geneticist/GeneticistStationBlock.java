package com.human.common.gameplay.gene.geneticist;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Geneticist Station - the Geneticist colonist's job site ([stated] Oct 28: concept model approved, metal matched
 * to the electronics workstation). A steel lab bench: microscope, tube rack, a syringe on the bench, the cleanser
 * beaker on its shelf. Faces the player when placed.
 * <p>
 * [stated] Oct 29: using it opens the cleanser screen - a syringe and a cleanser in, a cleansed syringe out after five
 * seconds (see {@link GeneticistStationMenu}). Like the blueprint table it keeps nothing; the slots empty back to the
 * player when the screen closes.
 * </p>
 */
public class GeneticistStationBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<GeneticistStationBlock> CODEC = simpleCodec(GeneticistStationBlock::new);

    /** The bench (up to the worktop) plus the backsplash along the back, for the model facing north. */
    private static final VoxelShape NORTH = Shapes.or(Block.box(0, 0, 0, 16, 10, 16), Block.box(0, 10, 14, 16, 16, 16));

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.NORTH, NORTH);
        SHAPES.put(Direction.SOUTH, Shapes.or(Block.box(0, 0, 0, 16, 10, 16), Block.box(0, 10, 0, 16, 16, 2)));
        SHAPES.put(Direction.EAST, Shapes.or(Block.box(0, 0, 0, 16, 10, 16), Block.box(0, 10, 0, 2, 16, 16)));
        SHAPES.put(Direction.WEST, Shapes.or(Block.box(0, 0, 0, 16, 10, 16), Block.box(14, 10, 0, 16, 16, 16)));
    }

    public GeneticistStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private static final net.minecraft.network.chat.Component TITLE =
        net.minecraft.network.chat.Component.translatable("container.avp_human.geneticist_station");

    @Override
    protected @NotNull net.minecraft.world.InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull net.minecraft.world.level.Level level,
        @NotNull BlockPos pos,
        @NotNull net.minecraft.world.entity.player.Player player,
        @NotNull net.minecraft.world.phys.BlockHitResult hit
    ) {
        if (!level.isClientSide) {
            player.openMenu(
                new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, opener) -> new GeneticistStationMenu(
                        id,
                        inventory,
                        net.minecraft.world.inventory.ContainerLevelAccess.create(level, pos)
                    ),
                    TITLE
                )
            );
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return SHAPES.getOrDefault(state.getValue(FACING), NORTH);
    }
}
