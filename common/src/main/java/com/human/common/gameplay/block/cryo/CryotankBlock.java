package com.human.common.gameplay.block.cryo;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * A cryotank block. Stack them into a solid box to make a bigger tank (see {@link CryotankFormation}). The cryoconduit
 * attaches to a tank's TOP or BOTTOM only.
 * <h2>Putting a specimen in</h2> [stated] Oct 1: right-click any block of the tank while a mob is on your lead or
 * titanium capture cord. It goes in only if the tank is free, the mob fits, and the tank is on a controller that has
 * cryofluid and power; otherwise you are told why and nothing happens. The lead or cord comes back to you.
 * <h2>Breaking it</h2> Breaking any block of an occupied tank breaks the seal: the specimen comes out awake and
 * hostile, and the rest of the blocks re-form.
 */
public class CryotankBlock extends BaseEntityBlock implements CryoConduitEndpoint {

    public static final MapCodec<CryotankBlock> CODEC = simpleCodec(CryotankBlock::new);

    /**
     * Where a block sits in its tank, top to bottom. [stated] Oct 2: the block you place shows the steel-and-aluminum
     * placeholder, and once it is part of a tank it turns into Brandon's pieces - which piece is decided by this and by
     * which of its sides face out. The blockstate does the rest; nothing here ticks or renders.
     */
    public enum Layer implements StringRepresentable {

        SINGLE("single"),
        BOTTOM("bottom"),
        MIDDLE("middle"),
        TOP("top");

        private final String name;

        Layer(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }

    /** False for a block that is not part of a proper box - it keeps the placeholder look. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    /**
     * [stated] Oct 2: the tank lights up while a specimen is inside - every block of it, light level 10. Set by the
     * master in {@link CryotankBlockEntity#setSpecimen}, the one place every specimen change goes through. A re-form
     * rebuilds each block's look from the default state (unlit) and then puts the specimens back, which relights them.
     */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /**
     * [stated] Oct 3: how frosted the glass is - 0 clear, 3 fully frosted. Frosted only while the tank is RUNNING; it
     * frosts over and thaws clear at the thaw rate (see {@link CryotankBlockEntity#tickFrost}). Only formed tanks show
     * it - "if it's not formed it's not a tank" - so the unformed block's model ignores this.
     */
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty FROST =
        net.minecraft.world.level.block.state.properties.IntegerProperty.create("frost", 0, 3);

    public static final EnumProperty<Layer> LAYER = EnumProperty.create("layer", Layer.class);

    /** Whether that side of this block is an OUTSIDE face of the tank. */
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;

    public static final BooleanProperty EAST = BlockStateProperties.EAST;

    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;

    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    /** How far from the player a leashed mob is looked for - the cord's own reach. */
    private static final double LEASH_SEARCH = 16.0;

    public CryotankBlock(Properties properties) {
        super(properties);
        registerDefaultState(
            stateDefinition.any()
                .setValue(FORMED, false)
                .setValue(LAYER, Layer.SINGLE)
                .setValue(NORTH, true)
                .setValue(EAST, true)
                .setValue(SOUTH, true)
                .setValue(WEST, true)
                .setValue(LIT, false)
                .setValue(FROST, 0)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, LAYER, NORTH, EAST, SOUTH, WEST, LIT, FROST);
    }

    /** The look of a block at this spot in a {@code sx x sy x sz} tank; {@code formed} false gives the placeholder. */
    public BlockState shapeState(boolean formed, int x, int y, int z, int sx, int sy, int sz) {
        if (!formed) {
            return defaultBlockState();
        }

        var layer = sy == 1 ? Layer.SINGLE : y == 0 ? Layer.BOTTOM : y == sy - 1 ? Layer.TOP : Layer.MIDDLE;

        return defaultBlockState()
            .setValue(FORMED, true)
            .setValue(LAYER, layer)
            .setValue(NORTH, z == 0)
            .setValue(SOUTH, z == sz - 1)
            .setValue(WEST, x == 0)
            .setValue(EAST, x == sx - 1);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CryotankBlockEntity(pos, state);
    }

    /** Oct 24 - server only: the unserved-tank thaw (CryotankBlockEntity.serverTick). */
    @Override
    public <T extends BlockEntity> @Nullable net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull net.minecraft.world.level.block.entity.BlockEntityType<T> type
    ) {
        return level.isClientSide
            ? null
            : createTickerHelper(
                type,
                com.human.common.registry.init.HumanBlockEntityTypes.CRYOTANK.get(),
                CryotankBlockEntity::serverTick
            );
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * ⚠ A block placed against an OCCUPIED tank would re-form it, and a specimen that no longer fits the new shape
     * would be thrown out awake - so the placement is refused instead, with a message. Release the specimen first to
     * resize its tank.
     */
    @Override
    public @Nullable BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();

        for (var direction : Direction.values()) {
            if (
                level.getBlockEntity(pos.relative(direction)) instanceof CryotankBlockEntity neighbour
                    && level.getBlockEntity(neighbour.getMasterPos()) instanceof CryotankBlockEntity master
                    && master.hasSpecimen()
            ) {
                if (!level.isClientSide && context.getPlayer() != null) {
                    tell(context.getPlayer(), "occupied_resize");
                }

                return null;
            }
        }

        return super.getStateForPlacement(context);
    }

    @Override
    public boolean acceptsCryoConduit(BlockState state, Direction face) {
        return face == Direction.UP || face == Direction.DOWN;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Forming
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void onPlace(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        // ⚠ Oct 21 - NOT when the block is being MOVED (Sable building or landing a ship, a Create contraption): the
        // whole tank moves block by block carrying its data, and re-forming mid-move works from a half-moved tank. The
        // move hook (ShipMoves) re-forms it once the whole batch is in place.
        if (level instanceof ServerLevel serverLevel && !oldState.is(this) && !movedByPiston) {
            CryotankFormation.reform(serverLevel, pos);
        }
    }

    @Override
    protected void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        CryotankBlockEntity stranded = null;

        // A MOVED block (see onPlace) breaks no seal and re-forms nothing: its specimen moves with its data.
        if (level instanceof ServerLevel serverLevel && !newState.is(this) && !movedByPiston) {
            // ⚠ BEFORE super: the block entity is still readable here and gone after.
            logBreak(serverLevel, pos);
            stranded = breakSeal(serverLevel, pos);
        }

        var strandedTag = stranded == null ? null : stranded.getSpecimen();
        var strandedWidth = stranded == null ? 0.0F : stranded.specimenWidth();
        var strandedHeight = stranded == null ? 0.0F : stranded.specimenHeight();

        super.onRemove(state, level, pos, newState, movedByPiston);

        if (level instanceof ServerLevel serverLevel && !newState.is(this) && !movedByPiston) {
            CryotankFormation.reformAround(serverLevel, pos);

            // A specimen whose release failed and whose master is THIS block would vanish with its block entity.
            if (strandedTag != null) {
                var around = new java.util.ArrayList<BlockPos>();

                for (var direction : Direction.values()) {
                    around.add(pos.relative(direction));
                }

                CryotankFormation.stash(serverLevel, around, strandedTag, strandedWidth, strandedHeight);
            }
        }
    }

    /**
     * Who broke which tank block - set by {@link #playerWillDestroy}, read once by {@link #onRemove}. Server thread.
     */
    private static final java.util.Map<BlockPos, String> BREAKERS = new java.util.HashMap<>();

    @Override
    public @NotNull BlockState playerWillDestroy(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState state,
        @NotNull Player player
    ) {
        if (!level.isClientSide) {
            BREAKERS.put(pos.immutable(), player.getGameProfile().getName());
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * [stated] Oct 2: the controller's log says when anything or anyone breaks a tube, with coordinates - like the
     * server log's "x killed a villager at ...". A player is named; anything else is reported with whatever living
     * thing was right beside it, since the game does not say who broke a block for anything but a player.
     */
    private static void logBreak(ServerLevel level, BlockPos pos) {
        var breaker = BREAKERS.remove(pos);

        if (
            !(level.getBlockEntity(pos) instanceof CryotankBlockEntity block)
                || !(level.getBlockEntity(block.getMasterPos()) instanceof CryotankBlockEntity master)
                || master.getOwner() == null
                || !level.isLoaded(master.getOwner())
                || !(level.getBlockEntity(master.getOwner()) instanceof CryotubeControllerBlockEntity controller)
        ) {
            return;
        }

        var where = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        var tube = controller.tubeLabel(master.getBlockPos());

        if (breaker != null) {
            controller.logEvent("broken_by", tube, breaker, where);
            return;
        }

        var near = level.getEntitiesOfClass(
            net.minecraft.world.entity.LivingEntity.class,
            new AABB(pos).inflate(4.0),
            entity -> entity.isAlive()
        ).stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(pos.getCenter())));

        if (near.isPresent()) {
            controller.logEvent("broken_near", tube, near.get().getName().getString(), where);
        } else {
            controller.logEvent("broken", tube, where);
        }
    }

    /**
     * The tank this block belonged to is no longer sealed: its specimen breaks out.
     *
     * @return the master, if its release FAILED and the master is the block being removed - the caller must re-home it
     */
    private static @Nullable CryotankBlockEntity breakSeal(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof CryotankBlockEntity tank)) {
            return null;
        }

        var masterPos = tank.getMasterPos();

        if (!(level.getBlockEntity(masterPos) instanceof CryotankBlockEntity master) || !master.hasSpecimen()) {
            return null;
        }

        // The broken block is already air by the time onRemove runs (the chunk swaps the state first), so the hole it
        // left is offered first; a mob too big for it comes out on top of or beside the tank instead.
        var escaped = master.specimenTypeId();

        if (CryoSpecimens.releaseFromTank(level, master, pos, true)) {
            if (
                master.getOwner() != null
                    && level.isLoaded(master.getOwner())
                    && level.getBlockEntity(master.getOwner()) instanceof CryotubeControllerBlockEntity controller
            ) {
                controller.logEvent(
                    "escaped",
                    controller.tubeLabel(master.getBlockPos()),
                    escaped == null
                        ? "?"
                        : net.minecraft.world.entity.EntityType.byString(escaped).map(t -> t.getDescription().getString()).orElse(escaped)
                );
            }

            master.setSpecimen(null, 1.0F, 0.0F, 0.0F);
            return null;
        }

        // Release failed. A master elsewhere keeps it safely; only one being removed needs rescuing.
        return masterPos.equals(pos) ? master : null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Capture
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected @NotNull ItemInteractionResult useItemOn(
        @NotNull ItemStack stack,
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hitResult
    ) {
        // Only when the player is actually holding a mob - otherwise a cryotank block in hand must still place, to
        // grow the tank. Both sides can see a lead, a cord AND a capture chain, so they agree.
        // [stated] Oct 19 - a lead, the titanium cord or a capture chain used on a tank that holds a specimen, with no
        // mob already in tow, pulls the specimen OUT. Both sides can see the item and whether the tank is occupied (the
        // tank syncs its specimen for the renderer), so they agree on the click.
        if (captive(level, player) == null && isTether(stack) && occupied(level, pos)) {
            if (level instanceof ServerLevel serverLevel) {
                tryExtract(serverLevel, pos, player, stack, hitResult);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (captive(level, player) == null) {
            // Vanilla calls this even with an EMPTY hand, and only goes on to useWithoutItem (the readout) on
            // PASS_TO_DEFAULT. ⚠ With anything IN hand it must be SKIP instead: passing would run the readout and
            // swallow the click, so a cryotank block could never be placed against a tank to grow it.
            return stack.isEmpty()
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        tryCapture(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hitResult
    ) {
        if (captive(level, player) == null) {
            // Nothing on a leash: an empty-handed click reads the tank out instead - its size, what is in it, and
            // whether a controller is looking after it. Nothing else on a tank is ever clickable.
            if (level instanceof ServerLevel serverLevel) {
                describe(serverLevel, pos, player);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        tryCapture(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Extraction - [stated] Oct 19
    // ------------------------------------------------------------------------------------------------------------

    /**
     * A lead, the titanium cord, or avp_alien's capture chain. ⚠ The chain is recognised by its registry name (an
     * avp_alien item whose id contains "capture_chain"), so avp_human needs no dependency on avp_alien to see it.
     */
    private static boolean isTether(ItemStack stack) {
        if (stack.is(Items.LEAD) || stack.is(com.human.common.registry.init.item.HumanItems.TITANIUM_CAPTURE_CORD.get())) {
            return true;
        }

        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());

        return "avp_alien".equals(id.getNamespace()) && id.getPath().contains("capture_chain");
    }

    private static boolean occupied(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CryotankBlockEntity block
            && level.getBlockEntity(block.getMasterPos()) instanceof CryotankBlockEntity master
            && master.hasSpecimen();
    }

    /**
     * [stated] Oct 19: "using a capture chain cord or leash on a cryotank with a mob in it should pull it out of cryo
     * deactivating the tank and starting a thaw timer on the mob. This is so you can take them out."
     * <ul>
     * <li>The specimen comes out beside the face you clicked (or the nearest free spot), FROZEN SOLID with a fresh
     * five-minute thaw timer, whatever its health - so it can be dragged away before it wakes.</li>
     * <li>The tank is emptied and goes dark; its controller logs the release and stops billing it.</li>
     * <li>On a lead or the cord it comes out tied to you, using up the lead or cord as tying one on does. A hostile on
     * a lead lets go when it thaws, as for any frozen hostile. ⚠ avp_alien's capture chain has no way for another mod
     * to start a hold, so a chained specimen comes out frozen beside you - use the chain on it to take it in tow.</li>
     * <li>It is never netted coming out.</li>
     * </ul>
     */
    private static void tryExtract(ServerLevel level, BlockPos pos, Player player, ItemStack stack, BlockHitResult hitResult) {
        if (
            !(level.getBlockEntity(pos) instanceof CryotankBlockEntity block)
                || !(level.getBlockEntity(block.getMasterPos()) instanceof CryotankBlockEntity master)
                || !master.hasSpecimen()
        ) {
            return;
        }

        var name = master.getCard() != null
            ? net.minecraft.world.entity.EntityType.byString(master.getCard().getString("type"))
                .map(t -> t.getDescription().getString())
                .orElse("?")
            : "?";

        // Out on the side that was clicked, if there is room there; otherwise the usual free spot around the tank.
        var entity = CryoSpecimens.releaseFromTankEntity(level, master, pos.relative(hitResult.getDirection()), false);

        if (entity == null) {
            tell(player, "extract_failed");
            return;
        }

        master.setSpecimen(null, 1.0F, 0.0F, 0.0F);

        var controller = CryoNetwork.controllerFor(level, master);
        var owner = master.getOwner();

        if (owner != null && level.isLoaded(owner) && level.getBlockEntity(owner) instanceof CryotubeControllerBlockEntity owning) {
            controller = owning;
        }

        if (controller != null) {
            controller.logEvent("released", controller.tubeLabel(master.getBlockPos()), name);
            controller.requestRescan();
            controller.sendSnapshotSoon();
        }

        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            com.human.common.gameplay.freeze.FrozenSolid.freezeOutOfCryo(living);
        }

        if (entity instanceof Mob mob) {
            if (stack.is(Items.LEAD)) {
                mob.setLeashedTo(player, true);
                stack.consume(1, player);
            } else if (
                stack.is(com.human.common.registry.init.item.HumanItems.TITANIUM_CAPTURE_CORD.get())
                    && !mob.getType().is(com.human.common.registry.tag.HumanEntityTypeTags.CAPTURE_CORD_BLACKLIST)
            ) {
                com.human.common.gameplay.item.CaptureCordTether.attach(mob, player);
                stack.consume(1, player);
            }
        }

        tell(player, "extracted");
    }

    /** The mob this player has brought: on a cord or lead (visible to both sides), or on avp_alien's capture chain. */
    private static @Nullable Mob captive(Level level, Player player) {
        // [stated] Oct 3: the cord is its own tether now, not a vanilla leash - looked for first.
        var corded = com.human.common.gameplay.item.CaptureCordTether.heldBy(level, player);

        if (corded != null) {
            return corded;
        }

        var leashed = leashedMob(level, player);

        if (leashed != null) {
            return leashed;
        }

        return level instanceof ServerLevel serverLevel
            ? com.human.compatibility.avp_alien.AlienCaptureChain.heldBy(serverLevel, player)
            : com.human.compatibility.avp_alien.AlienCaptureChain.heldByOnClient(level, player);
    }

    private static @Nullable Mob leashedMob(Level level, Player player) {
        var box = new AABB(player.blockPosition()).inflate(LEASH_SEARCH);

        return level.getEntitiesOfClass(Mob.class, box, mob -> mob.getLeashHolder() == player)
            .stream()
            .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)))
            .orElse(null);
    }

    private static void tryCapture(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        var mob = captive(level, player);

        if (
            mob == null
                || !(level.getBlockEntity(pos) instanceof CryotankBlockEntity block)
                || !(level.getBlockEntity(block.getMasterPos()) instanceof CryotankBlockEntity master)
        ) {
            return;
        }

        if (master.hasSpecimen()) {
            tell(player, "occupied");
            return;
        }

        if (mob.getType().is(com.human.common.registry.tag.HumanEntityTypeTags.CRYOTANK_BLACKLIST)) {
            tell(player, "refused");
            return;
        }

        var controller = CryoNetwork.controllerFor(serverLevel, master);
        var owner = master.getOwner();

        // The tank's own controller answers for it, if it has one - not whichever the walk happened to reach first.
        if (
            owner != null && serverLevel.isLoaded(owner) && serverLevel.getBlockEntity(
                owner
            ) instanceof CryotubeControllerBlockEntity owning
        ) {
            controller = owning;
        }

        if (controller == null) {
            tell(player, "no_controller");
            return;
        }

        if (!controller.hasFuel()) {
            tell(player, "no_fuel");
            return;
        }

        if (!controller.hasPowerSupply()) {
            tell(player, "no_power");
            return;
        }

        if (!controller.canServe(master)) {
            tell(player, "over_capacity");
            return;
        }

        // Oct 24 - the WIDTH is the body's, not the hitbox's (see CryoSpecimens.FROZEN_POSE_WIDTHS).
        // Oct 24 - only eggs, chestbursters and adolescents go in a 1x1 (CryoSpecimens.minimumAcross).
        var scale = CryoSpecimens.fitScale(
            master,
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()),
            CryoSpecimens.frozenWidth(mob),
            CryoSpecimens.frozenHeight(mob)
        );

        if (scale <= 0.0F) {
            tell(player, "too_big");
            return;
        }

        // The cord or lead goes back to the player, not on the floor. A capture chain is simply let go - the hold
        // never consumed it. [stated] Oct 3: the cord is its own tether, which hands the cord back itself.
        var corded = com.human.common.gameplay.item.CaptureCordTether.isHeldBy(mob, player);
        var leashed = !corded && mob.getLeashHolder() == player;

        if (corded) {
            com.human.common.gameplay.item.CaptureCordTether.release(mob, player);
        } else if (leashed) {
            mob.dropLeash(true, false);
        } else {
            com.human.compatibility.avp_alien.AlienCaptureChain.release(mob);
        }

        if (leashed && !player.getAbilities().instabuild) {
            var returned = new ItemStack(Items.LEAD);

            if (!player.getInventory().add(returned)) {
                player.drop(returned, false);
            }
        }

        // [stated] Oct 3: the specimen faces the player who loads it - turned toward them from the tank's centre, so it
        // faces the side they loaded it from. Saved with the specimen; the tank's renderer draws it this way.
        var master_ = master.getBlockPos();
        var dx = player.getX() - (master_.getX() + master.sizeX() / 2.0);
        var dz = player.getZ() - (master_.getZ() + master.sizeZ() / 2.0);
        var facing = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        mob.setYRot(facing);
        mob.setYBodyRot(facing);
        mob.setYHeadRot(facing);
        mob.setXRot(0.0F);

        if (!CryoSpecimens.freeze(master, mob, scale)) {
            tell(player, "cannot_freeze");
            return;
        }

        controller.requestRescan();
        controller.logEvent(
            "frozen",
            controller.tubeLabel(master.getBlockPos()),
            master.getCard() != null
                ? net.minecraft.world.entity.EntityType.byString(master.getCard().getString("type"))
                    .map(t -> t.getDescription().getString())
                    .orElse("?")
                : "?"
        );
        tell(player, "frozen");
    }

    private static void describe(ServerLevel level, BlockPos pos, Player player) {
        if (
            !(level.getBlockEntity(pos) instanceof CryotankBlockEntity block)
                || !(level.getBlockEntity(block.getMasterPos()) instanceof CryotankBlockEntity master)
        ) {
            return;
        }

        var size = master.sizeX() + "x" + master.sizeY() + "x" + master.sizeZ();
        var typeId = master.specimenTypeId();
        Component contents = !master.hasSpecimen()
            ? Component.translatable("gui.avp_human.cryo.empty")
            : typeId == null
                ? Component.translatable("gui.avp_human.cryo.unknown")
                : net.minecraft.world.entity.EntityType.byString(typeId)
                    .map(net.minecraft.world.entity.EntityType::getDescription)
                    .orElse(Component.translatable("gui.avp_human.cryo.unknown"));

        var connected = CryoNetwork.controllerFor(level, master) != null;

        player.displayClientMessage(
            Component.translatable(
                connected ? "message.avp_human.cryo.info" : "message.avp_human.cryo.info_unconnected",
                size,
                contents
            ),
            true
        );
    }

    private static void tell(Player player, String key) {
        player.displayClientMessage(Component.translatable("message.avp_human.cryo." + key), true);
    }
}
