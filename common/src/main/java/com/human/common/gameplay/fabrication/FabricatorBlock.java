package com.human.common.gameplay.fabrication;

import com.human.common.registry.init.HumanFabrication;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The fabricator: standard, W-Y and USCM editions on fabricator_v2. Four blocks - see {@link FabricatorPart}.
 * <h2>PLACING</h2> Like a bed: all four spaces are checked before anything is placed, so it places whole or not at all.
 * <h2>BREAKING</h2> Breaking ANY part breaks the core properly, so the one item with everything in it drops. Parts
 * whose core is gone tidy themselves away; the core itself NEVER removes itself on a neighbour change - it holds the
 * contents, and a block that turns to air drops nothing.
 */
public class FabricatorBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<FabricatorBlock> CODEC = simpleCodec(FabricatorBlock::new);

    public static final EnumProperty<FabricatorPart> PART = EnumProperty.create("part", FabricatorPart.class);

    public FabricatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, FabricatorPart.CORE));
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    public static BlockPos corePos(BlockPos pos, BlockState state) {
        return state.getValue(PART).toCore(pos, state.getValue(FACING));
    }

    /** The core of the fabricator at {@code pos}, or {@code pos} itself when there is no fabricator there. */
    public static BlockPos corePosOrSelf(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        var state = level.getBlockState(pos);

        return state.getBlock() instanceof FabricatorBlock ? corePos(pos, state) : pos;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        var facing = context.getHorizontalDirection().getOpposite();
        var core = context.getClickedPos();
        var level = context.getLevel();

        for (var part : FabricatorPart.values()) {
            var pos = part.fromCore(core, facing);

            if (part == FabricatorPart.CORE) {
                continue;
            }

            if (
                !level.getBlockState(pos).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(pos) || pos.getY() >= level
                    .getMaxBuildHeight()
            ) {
                return null;
            }

            // Nobody standing in the space - a part placed into a player or mob would trap them inside it.
            if (!level.isUnobstructed(defaultBlockState(), pos, net.minecraft.world.phys.shapes.CollisionContext.empty())) {
                return null;
            }
        }

        return defaultBlockState().setValue(FACING, facing).setValue(PART, FabricatorPart.CORE);
    }

    @Override
    public void setPlacedBy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @Nullable LivingEntity placer,
        @NotNull ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide) {
            return;
        }

        var facing = state.getValue(FACING);

        for (var part : FabricatorPart.values()) {
            if (part != FabricatorPart.CORE) {
                level.setBlock(part.fromCore(pos, facing), state.setValue(PART, part), Block.UPDATE_ALL);
            }
        }
    }

    /** A part with no core next to it goes away. The core stays put whatever happens to its parts. */
    @Override
    protected @NotNull BlockState updateShape(
        @NotNull BlockState state,
        @NotNull Direction direction,
        @NotNull BlockState neighbor,
        @NotNull LevelAccessor level,
        @NotNull BlockPos pos,
        @NotNull BlockPos neighborPos
    ) {
        if (state.getValue(PART) != FabricatorPart.CORE) {
            var core = level.getBlockState(corePos(pos, state));

            if (!core.is(this) || core.getValue(PART) != FabricatorPart.CORE || core.getValue(FACING) != state.getValue(FACING)) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** Breaking a part breaks the core properly, so its item - with everything in it - drops. */
    @Override
    public @NotNull BlockState playerWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Player player
    ) {
        if (!level.isClientSide) {
            var core = corePos(pos, state);

            if (state.getValue(PART) != FabricatorPart.CORE && level.getBlockState(core).is(this)) {
                dropCreativeContents(level, core, player);
                level.destroyBlock(core, !player.isCreative(), player);
            } else if (state.getValue(PART) == FabricatorPart.CORE) {
                dropCreativeContents(level, core, player);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Creative mode skips loot tables; a fabricator with anything in it still drops, like a shulker box. */
    private void dropCreativeContents(Level level, BlockPos core, Player player) {
        if (player.isCreative() && level.getBlockEntity(core) instanceof FabricatorBlockEntity fabricator && !fabricator.isEmpty()) {
            var stack = new ItemStack(this);

            stack.applyComponents(fabricator.collectComponents());

            var drop = new ItemEntity(level, core.getX() + 0.5D, core.getY() + 0.5D, core.getZ() + 0.5D, stack);

            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        if (!level.isClientSide && level.getBlockEntity(corePos(pos, state)) instanceof FabricatorBlockEntity fabricator) {
            player.openMenu(
                new SimpleMenuProvider(
                    (id, inventory, opener) -> new FabricatorMenu(id, inventory, fabricator),
                    // [stated] Oct 16: the screen's top right names only the TYPE (no "Fabricator"); same words as the
                    // quarry.
                    Component.translatable(brandKey())
                )
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return state.getValue(PART) == FabricatorPart.CORE ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return state.getValue(PART) == FabricatorPart.CORE ? new FabricatorBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockEntityType<T> type
    ) {
        if (type != HumanFabrication.FABRICATOR_BLOCK_ENTITY.get() || state.getValue(PART) != FabricatorPart.CORE) {
            return null;
        }

        if (level.isClientSide) {
            return (tickLevel, pos, tickState, entity) -> ((FabricatorBlockEntity) entity).clientTick();
        }

        return (tickLevel, pos, tickState, entity) -> ((FabricatorBlockEntity) entity).serverTick(tickLevel, pos, tickState);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return new ItemStack(this);
    }

    /** The edition's name for the screen's top right: INDUSTRIAL, WEYLAND-YUTANI or USCM ENGINEERING. */
    private String brandKey() {
        if (this == HumanFabrication.WY_FABRICATOR.get()) {
            return "gui.avp_human.fabricator.brand.wy";
        }

        return this == HumanFabrication.USCM_FABRICATOR.get()
            ? "gui.avp_human.fabricator.brand.uscm"
            : "gui.avp_human.fabricator.brand.bare";
    }
}
