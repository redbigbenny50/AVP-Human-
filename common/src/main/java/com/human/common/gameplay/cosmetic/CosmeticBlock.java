package com.human.common.gameplay.cosmetic;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A piece of cosmetic furniture: one {@link CosmeticDesign} in one {@link CosmeticStyle}.
 * <p>
 * Baked designs draw through their blockstate model on the core cell. Animated designs (glow, see-through glass,
 * contents, doors or spinners) draw through {@link CosmeticBlockEntity}'s AzureLib renderer instead. Every other cell
 * is invisible and only gives the footprint its collision.
 * </p>
 * <h2>Per-design properties</h2> A block carries only what its design uses:
 * <ul>
 * <li>{@link #FABRIC} - dyeable fabric ([stated] Oct 31: right-click with a dye; not used up; no reset);</li>
 * <li>{@link #MOUNT} - wall pieces: wall, floor or ceiling ([stated] Oct 31: the six faces);</li>
 * <li>{@link #OPEN} - doors and lids, toggled by right-clicking;</li>
 * <li>{@link #SPINNING} - fans and rotors, toggled by right-clicking ([stated] Oct 31);</li>
 * <li>contents - see {@link CosmeticSpecimenBlock}.</li>
 * </ul>
 * Vanilla builds a block's state definition inside the Block constructor, before any field is set - so the design is
 * handed over through {@link #BUILDING} by {@link #create}, the one way these blocks are made.
 */
public class CosmeticBlock extends CosmeticMultiblock implements EntityBlock {

    public static final EnumProperty<CosmeticFabric> FABRIC = EnumProperty.create("fabric", CosmeticFabric.class);

    public static final EnumProperty<CosmeticMount> MOUNT = EnumProperty.create("mount", CosmeticMount.class);

    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public static final BooleanProperty SPINNING = BooleanProperty.create("spinning");

    /** The design whose block is being constructed on this thread - read by {@link #createBlockStateDefinition}. */
    private static final ThreadLocal<CosmeticDesign> BUILDING = new ThreadLocal<>();

    private final CosmeticDesign design;

    private final CosmeticStyle style;

    protected CosmeticBlock(Properties properties, CosmeticDesign design, CosmeticStyle style) {
        super(properties, design.blockName(style));
        this.design = design;
        this.style = style;
        var state = defaultBlockState();

        // A boolean property's first value is true; these start shut and still.
        if (state.hasProperty(OPEN)) {
            state = state.setValue(OPEN, false);
        }

        if (state.hasProperty(SPINNING)) {
            state = state.setValue(SPINNING, false);
        }

        registerDefaultState(state);
    }

    /**
     * Makes the block for a design: the only way these blocks are created, so the state definition knows the design.
     */
    public static CosmeticBlock create(Properties properties, CosmeticDesign design, CosmeticStyle style) {
        BUILDING.set(design);

        try {
            return design.hasContents()
                ? new CosmeticSpecimenBlock(properties, design, style)
                : new CosmeticBlock(properties, design, style);
        } finally {
            BUILDING.remove();
        }
    }

    /** Pieces that take a filled map: the holotable (3D projection) and the wall display (flat map). */
    public static boolean isMapDisplay(CosmeticDesign design) {
        return design == CosmeticDesign.MAPPING_TABLE || design == CosmeticDesign.WALL_DISPLAY_ARRAY
            || design == CosmeticDesign.BRIEFING_TABLE;
    }

    public static boolean dyeable(CosmeticDesign design) {
        return CosmeticModelTables.FABRIC.containsKey(design.id());
    }

    public static boolean hasDoors(CosmeticDesign design) {
        return CosmeticModelTables.MOTIONS.getOrDefault(design.id(), List.of()).stream().anyMatch(motion -> !motion.spin());
    }

    public static boolean spins(CosmeticDesign design) {
        return CosmeticModelTables.MOTIONS.getOrDefault(design.id(), List.of()).stream().anyMatch(CosmeticMotion::spin);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        var building = BUILDING.get();

        if (building == null) {
            return;
        }

        if (dyeable(building)) {
            builder.add(FABRIC);
        }

        if (building.mount() == CosmeticDesign.Mount.WALL) {
            builder.add(MOUNT);
        }

        if (hasDoors(building)) {
            builder.add(OPEN);
        }

        if (spins(building)) {
            builder.add(SPINNING);
        }
    }

    public CosmeticDesign design() {
        return design;
    }

    public CosmeticStyle style() {
        return style;
    }

    @Override
    public int[][] cells() {
        return design.cells();
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(properties -> create(properties, design, style));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Placement - wall pieces mount on any face
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Wall pieces face out of the wall that was clicked; on a floor or ceiling they face back toward the player, so a
     * panel lying flat reads the right way up from where it was placed. Everything else faces the player.
     */
    @Override
    protected @Nullable Direction placementFacing(BlockPlaceContext context) {
        var face = context.getClickedFace();

        if (design.mount() == CosmeticDesign.Mount.WALL && face.getAxis().isHorizontal()) {
            return face;
        }

        return super.placementFacing(context);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);

        if (state == null || !state.hasProperty(MOUNT)) {
            return state;
        }

        var mount = switch (context.getClickedFace()) {
            case UP -> CosmeticMount.FLOOR;
            case DOWN -> CosmeticMount.CEILING;
            default -> CosmeticMount.WALL;
        };

        return state.setValue(MOUNT, mount);
    }

    @Override
    protected int mountIndex(BlockState state) {
        return state.hasProperty(MOUNT) ? state.getValue(MOUNT).ordinal() : 0;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Interaction - dye the fabric; open doors / start spinners
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected @NotNull ItemInteractionResult useItemOn(
        @NotNull ItemStack stack,
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hit
    ) {
        // Holotable / wall display: a filled map goes onto an empty one.
        if (isMapDisplay(design) && stack.is(net.minecraft.world.item.Items.FILLED_MAP)) {
            var table = level.getBlockEntity(corePos(pos, state));

            if (table instanceof CosmeticBlockEntity holo && holo.holoMap().isEmpty()) {
                if (!level.isClientSide) {
                    holo.setHoloMap(stack.copyWithCount(1));

                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }

                    level.playSound(
                        null,
                        pos,
                        com.human.common.registry.init.HumanSoundEvents.BLOCK_COSMETIC_MAP_IN.get(),
                        SoundSource.BLOCKS,
                        0.4F,
                        1.6F
                    );
                }

                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        var racked = useMount(stack, state, level, pos, player, hand, hit);

        if (racked != null) {
            return racked;
        }

        if (state.hasProperty(FABRIC) && stack.getItem() instanceof DyeItem dye) {
            var fabric = CosmeticFabric.of(dye.getDyeColor());

            if (state.getValue(FABRIC) == fabric) {
                return ItemInteractionResult.CONSUME;
            }

            if (!level.isClientSide) {
                setOnEveryCell(level, pos, state, FABRIC, fabric);
                level.playSound(
                    null,
                    pos,
                    com.human.common.registry.init.HumanSoundEvents.BLOCK_COSMETIC_DYE.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
                );
            }

            // [stated] Oct 31: the dye is not used up.
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        if (CosmeticBunks.isBunk(design)) {
            return sleep(state, level, pos, player, hit);
        }

        // Holotable: sneak toggles the rotation; otherwise (and on the wall display) an empty hand takes the map back.
        if (isMapDisplay(design) && !(design == CosmeticDesign.MAPPING_TABLE && player.isShiftKeyDown())) {
            var table = level.getBlockEntity(corePos(pos, state));

            if (table instanceof CosmeticBlockEntity holo && !holo.holoMap().isEmpty()) {
                if (!level.isClientSide) {
                    var map = holo.holoMap();
                    holo.setHoloMap(ItemStack.EMPTY);

                    if (!player.getInventory().add(map)) {
                        player.drop(map, false);
                    }

                    level.playSound(
                        null,
                        pos,
                        com.human.common.registry.init.HumanSoundEvents.BLOCK_COSMETIC_MAP_OUT.get(),
                        SoundSource.BLOCKS,
                        0.4F,
                        1.6F
                    );
                }

                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            return InteractionResult.PASS;
        }

        var sat = sit(state, level, pos, player, hit);

        if (sat != null) {
            return sat;
        }

        // Storage: the chest screen; its doors follow the viewers (see CosmeticStorage), not the click.
        if (CosmeticStorage.has(design)) {
            if (
                !level.isClientSide && level.getBlockEntity(corePos(pos, state)) instanceof CosmeticBlockEntity decor && decor
                    .storage() != null
            ) {
                player.openMenu(CosmeticStorage.menu(decor.storage(), getName()));
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (state.hasProperty(OPEN)) {
            var open = !state.getValue(OPEN);

            if (!level.isClientSide) {
                setOnEveryCell(level, pos, state, OPEN, open);
                level.playSound(null, pos, doorSound(design, open), SoundSource.BLOCKS, 0.8F, open ? 1.0F : 1.1F);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (state.hasProperty(SPINNING)) {
            var spinning = !state.getValue(SPINNING);

            if (!level.isClientSide) {
                setOnEveryCell(level, pos, state, SPINNING, spinning);
                level.playSound(
                    null,
                    pos,
                    com.human.common.registry.init.HumanSoundEvents.BLOCK_COSMETIC_MACHINE_SWITCH.get(),
                    SoundSource.BLOCKS,
                    0.6F,
                    spinning ? 0.6F : 0.5F
                );
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Weapon racks - [stated] Oct 31: right-click the mount you are looking at to put a gun in or take it out; no lock
    // ------------------------------------------------------------------------------------------------------------

    /** The rack result, or null when this is not a rack or no mount was aimed at (so other interactions run). */
    private @Nullable ItemInteractionResult useMount(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        var mounts = CosmeticModelTables.SLOTS.get(design.id());

        if (mounts == null) {
            return null;
        }

        // A case or locker has to be opened first; the click then opens it (useWithoutItem).
        if (state.hasProperty(OPEN) && !state.getValue(OPEN)) {
            return null;
        }

        var core = corePos(pos, state);

        if (!(level.getBlockEntity(core) instanceof CosmeticBlockEntity rack) || !rack.hasMounts()) {
            return null;
        }

        var mount = mountAt(mounts, core, state, hit.getLocation());

        if (mount < 0) {
            return null;
        }

        var held = rack.getItem(mount);

        if (held.isEmpty()) {
            if (stack.isEmpty()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!level.isClientSide) {
                rack.setItem(mount, stack.copyWithCount(1));

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                level.playSound(
                    null,
                    pos,
                    com.human.common.registry.init.HumanSoundEvents.BLOCK_COSMETIC_ITEM_PLACE.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    0.8F
                );
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            rack.setItem(mount, ItemStack.EMPTY);

            if (stack.isEmpty()) {
                player.setItemInHand(hand, held);
            } else if (!player.getInventory().add(held)) {
                player.drop(held, false);
            }

            level.playSound(
                null,
                pos,
                com.human.common.registry.init.HumanSoundEvents.BLOCK_COSMETIC_ITEM_TAKE.get(),
                SoundSource.BLOCKS,
                1.0F,
                0.8F
            );
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * The mount under {@code hit}, or -1. The hit is turned back into the model's own pixels - the exact inverse of how
     * the piece is drawn (facing turn about Y, then for floor/ceiling mounts the X turn about the block middle) - and
     * the nearest mount whose clickable area holds it wins.
     */
    private int mountAt(float[][] mounts, BlockPos core, BlockState state, net.minecraft.world.phys.Vec3 hit) {
        double x = (hit.x - core.getX()) * 16 - 8, y = (hit.y - core.getY()) * 16, z = (hit.z - core.getZ()) * 16 - 8;

        // undo the facing turn (BLib: north 0, south 180, west 90, east 270 degrees about +Y)
        double a = Math.toRadians(switch (state.getValue(FACING)) {
            case SOUTH -> 180;
            case WEST -> 90;
            case EAST -> 270;
            default -> 0;
        });
        double mx = x * Math.cos(a) - z * Math.sin(a), mz = x * Math.sin(a) + z * Math.cos(a);
        double my = y;

        // undo a floor (+90) / ceiling (-90) X turn about the block middle
        var mount = state.hasProperty(MOUNT) ? state.getValue(MOUNT) : CosmeticMount.WALL;

        if (mount != CosmeticMount.WALL) {
            double p = Math.toRadians(mount == CosmeticMount.FLOOR ? 90 : -90);
            double yy = my - 8;
            double ny = yy * Math.cos(p) + mz * Math.sin(p), nz = -yy * Math.sin(p) + mz * Math.cos(p);
            my = ny + 8;
            mz = nz;
        }

        int best = -1;
        double bestDistance = Double.MAX_VALUE;

        for (int i = 0; i < mounts.length; i++) {
            var m = mounts[i];
            double dx = Math.abs(mx - m[0]), dy = Math.abs(my - m[1]);

            if (dx <= m[5] + 1 && dy <= m[6] + 1 && dx + dy < bestDistance) {
                best = i;
                bestDistance = dx + dy;
            }
        }

        return best;
    }

    /** A rack's guns drop when its core goes - broken, blown up or replaced - so nothing is ever lost. */
    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CosmeticBlockEntity decor) {
            if (decor.hasMounts()) {
                net.minecraft.world.Containers.dropContents(level, pos, decor.items());
            }

            if (!decor.holoMap().isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, decor.holoMap());
            }

            if (decor.storage() != null) {
                net.minecraft.world.Containers.dropContents(level, pos, decor.storage());
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Bunk beds - [stated] Oct 31: work as beds (sleep through the night, set the respawn point), two sleepers
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Sleeps the player in the bunk they clicked: the top one when the click is on the upper block. Every refusal
     * (daytime, monsters, too far, wrong dimension) is vanilla's own, with vanilla's message. On success the respawn
     * point becomes the floor in front of the bunk, FORCED - vanilla's /spawnpoint route, so respawning needs no hook.
     */
    private InteractionResult sleep(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        var core = corePos(pos, state);
        var facing = state.getValue(FACING);
        var sleepingPos = CosmeticBunks.sleepingPos(this, core, facing, hit.getLocation().y);

        if (CosmeticBunks.occupied(level, sleepingPos)) {
            player.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
            return InteractionResult.CONSUME;
        }

        player.startSleepInBed(sleepingPos).ifLeft(problem -> {
            if (problem.getMessage() != null) {
                player.displayClientMessage(problem.getMessage(), true);
            }
        }).ifRight(slept -> {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.setRespawnPosition(
                    level.dimension(),
                    CosmeticBunks.respawnPoint(sleepingPos, core, facing),
                    facing.getOpposite().toYRot(),
                    true,
                    false
                );
            }
        });

        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Seating - [stated] Oct 31: chairs, the sofa and other seating actually seat the player
    // ------------------------------------------------------------------------------------------------------------

    /** Sits the player on the free seat nearest the click; null when this piece has no seats (or the player sneaks). */
    private @Nullable InteractionResult sit(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var seats = CosmeticModelTables.SEATS.get(design.id());

        if (seats == null || player.isShiftKeyDown() || player.isPassenger()) {
            return null;
        }

        var core = corePos(pos, state);
        var facing = state.getValue(FACING);
        Vec3 best = null;
        float bestTurn = 0;
        var bestDistance = Double.MAX_VALUE;

        for (var seat : seats) {
            var at = modelToWorld(core, facing, seat[0], seat[1], seat[2]);
            var distance = at.distanceToSqr(hit.getLocation());

            if (distance < bestDistance && !CosmeticSeatEntity.occupied(level, at)) {
                best = at;
                bestTurn = seat[3];
                bestDistance = distance;
            }
        }

        if (best == null) {
            return InteractionResult.sidedSuccess(level.isClientSide); // every seat taken: swallow the click
        }

        if (!level.isClientSide) {
            var seat = CosmeticSeatEntity.create(level, core, best, facing);
            level.addFreshEntity(seat);
            player.setYRot(facing.toYRot() + bestTurn);
            player.setYHeadRot(player.getYRot());
            player.startRiding(seat);
            level.playSound(
                null,
                pos,
                com.human.common.registry.init.HumanSoundEvents.BLOCK_COSMETIC_SIT.get(),
                SoundSource.BLOCKS,
                0.8F,
                0.9F
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * A point in the model's own pixels, in the world: the same turn the piece is drawn with (BLib: north 0, south 180,
     * west 90, east 270 degrees about +Y, right-handed) about the core block's bottom centre.
     */
    public static Vec3 modelToWorld(BlockPos core, Direction facing, double x, double y, double z) {
        var a = Math.toRadians(switch (facing) {
            case SOUTH -> 180;
            case WEST -> 90;
            case EAST -> 270;
            default -> 0;
        });
        var wx = x * Math.cos(a) + z * Math.sin(a);
        var wz = -x * Math.sin(a) + z * Math.cos(a);

        return new Vec3(core.getX() + 0.5 + wx / 16.0, core.getY() + y / 16.0, core.getZ() + 0.5 + wz / 16.0);
    }

    /** Pieces that open with a lid or a case latch rather than a door - they sound like a case, not a cabinet. */
    private static final java.util.Set<CosmeticDesign> LIDDED = java.util.EnumSet.of(
        CosmeticDesign.TOOL_CHEST,
        CosmeticDesign.SAMPLE_FREEZER,
        CosmeticDesign.CARGO_CRATE,
        CosmeticDesign.FIELD_SUPPLY_CASE,
        CosmeticDesign.TISSUE_STORAGE
    );

    /** The opening or closing sound for a piece: a case for lidded pieces, a cabinet door for everything else. */
    public static SoundEvent doorSound(CosmeticDesign design, boolean open) {
        if (LIDDED.contains(design)) {
            return open
                ? com.human.common.registry.init.HumanSoundEvents.BLOCK_COSMETIC_LID_OPEN.get()
                : com.human.common.registry.init.HumanSoundEvents.BLOCK_COSMETIC_LID_CLOSE.get();
        }

        return open
            ? com.human.common.registry.init.HumanSoundEvents.BLOCK_COSMETIC_DOOR_OPEN.get()
            : com.human.common.registry.init.HumanSoundEvents.BLOCK_COSMETIC_DOOR_CLOSE.get();
    }

    /** Every cell carries the same value, so the whole piece agrees whichever block is looked at. */
    protected <V extends Comparable<V>> void setOnEveryCell(Level level, BlockPos pos, BlockState state, Property<V> property, V value) {
        var core = corePos(pos, state);
        var coreState = level.getBlockState(core);

        if (!coreState.is(this)) {
            return;
        }

        for (var cell : cells()) {
            var cellPos = cellPos(core, coreState.getValue(FACING), cell);
            var cellState = level.getBlockState(cellPos);

            if (cellState.is(this) && cellState.hasProperty(property)) {
                level.setBlock(cellPos, cellState.setValue(property, value), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** The fabric tint of this block in {@code state} - read by the block-colour hook. */
    public int fabricTint(BlockState state) {
        var fabric = state.hasProperty(FABRIC) ? state.getValue(FABRIC) : CosmeticFabric.DEFAULT;

        return fabric.tint(design.blockName(style));
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        Item.@NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);

        if (dyeable(design)) {
            tooltip.add(Component.translatable("tooltip.avp_human.cosmetic.dye_hint").withStyle(ChatFormatting.GRAY));
        }

        if (CosmeticStorage.has(design)) {
            tooltip.add(
                Component.translatable("tooltip.avp_human.cosmetic.storage_hint", CosmeticStorage.SLOTS.get(design))
                    .withStyle(ChatFormatting.GRAY)
            );
        } else if (hasDoors(design)) {
            tooltip.add(Component.translatable("tooltip.avp_human.cosmetic.door_hint").withStyle(ChatFormatting.GRAY));
        }

        if (spins(design) && design != CosmeticDesign.MAPPING_TABLE) {
            tooltip.add(Component.translatable("tooltip.avp_human.cosmetic.spin_hint").withStyle(ChatFormatting.GRAY));
        }

        if (CosmeticModelTables.SLOT_BONES.containsKey(design.id())) {
            tooltip.add(Component.translatable("tooltip.avp_human.cosmetic.shelf_hint").withStyle(ChatFormatting.GRAY));
        } else if (CosmeticModelTables.SLOTS.containsKey(design.id())) {
            tooltip.add(Component.translatable("tooltip.avp_human.cosmetic.rack_hint").withStyle(ChatFormatting.GRAY));
        }

        if (CosmeticModelTables.SEATS.containsKey(design.id())) {
            tooltip.add(Component.translatable("tooltip.avp_human.cosmetic.seat_hint").withStyle(ChatFormatting.GRAY));
        }

        if (CosmeticBunks.isBunk(design)) {
            tooltip.add(Component.translatable("tooltip.avp_human.cosmetic.bunk_hint").withStyle(ChatFormatting.GRAY));
        }

        if (design == CosmeticDesign.MAPPING_TABLE) {
            tooltip.add(Component.translatable("tooltip.avp_human.cosmetic.holo_hint").withStyle(ChatFormatting.GRAY));
        }

        if (design == CosmeticDesign.WALL_DISPLAY_ARRAY || design == CosmeticDesign.BRIEFING_TABLE) {
            tooltip.add(Component.translatable("tooltip.avp_human.cosmetic.wall_map_hint").withStyle(ChatFormatting.GRAY));
        }

        if (design.mount() == CosmeticDesign.Mount.WALL) {
            tooltip.add(Component.translatable("tooltip.avp_human.cosmetic.mount_hint").withStyle(ChatFormatting.GRAY));
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        if (state.getValue(CELL) != 0) {
            return RenderShape.INVISIBLE;
        }

        return design.animated() ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    /** Only the holotable ticks (on the server, to keep its projection up to date); every other piece never does. */
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.@Nullable BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        net.minecraft.world.level.block.entity.@NotNull BlockEntityType<T> type
    ) {
        // Client: a machine piece (fan, saw, lathe, ...) plays its working loop while it runs.
        if (level.isClientSide) {
            var hook = CosmeticBlockEntity.clientTicker;

            if (hook == null || !spins(design)) {
                return null;
            }

            return (tickLevel, tickPos, tickState, entity) -> {
                if (entity instanceof CosmeticBlockEntity decor) {
                    hook.accept(decor);
                }
            };
        }

        if (!isMapDisplay(design)) {
            return null;
        }

        return (tickLevel, tickPos, tickState, entity) -> {
            if (entity instanceof CosmeticBlockEntity holo && tickLevel instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                holo.serverTick(serverLevel);
            }
        };
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        // Animated pieces, and storage on any piece (a baked crate keeps a block entity just for its contents).
        return (design.animated() || CosmeticStorage.has(design)) && state.getValue(CELL) == 0 ? new CosmeticBlockEntity(pos, state) : null;
    }
}
