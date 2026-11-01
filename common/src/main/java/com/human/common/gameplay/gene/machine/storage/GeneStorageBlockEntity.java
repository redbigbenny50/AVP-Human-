package com.human.common.gameplay.gene.machine.storage;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.gene.tube.GeneTubeItem;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.registry.init.HumanGeneStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * The Gene Storage's core. Holds the 90-slot rack, joins the storage network (same power grid, [stated] Oct 29) and
 * runs the lid.
 * <h2>POWER</h2> No idle draw. Once a second it reads whether its grid has supply, like the drive bay; unpowered, the
 * screen goes dark (the _off textures), the lid stays shut and it refuses to open - nothing stored is lost, and
 * machines on the grid still can't use it because a grid with no supply runs no machines.
 * <h2>LID</h2> Opening runs open_1 then open_2 with fog, a hiss and a freeze sound at each stage; closing runs close
 * with one hiss. The clips are played on the client from the synced {@code open} flag.
 * <h2>CAROUSEL</h2> The 32 carousel holders show rack slots 0-31: their gene colours are synced (one int each) and the
 * renderer draws the vials. Nothing else about the rack is synced; the screen gets it through its menu.
 */
public class GeneStorageBlockEntity extends PowerConsumerBlockEntity implements GeneStorageNetwork.Member {

    private static final int SUPPLY_CHECK_TICKS = 20;

    /** open_1 is 1 s; open_2 starts when it ends. */
    public static final int STAGE_TWO_DELAY = 20;

    private static final String NBT_RACK = "Rack";

    private static final String NBT_ISOLATED = "Isolated";

    private static final String NBT_FILL_ORDER = "FillOrder";

    private static final String NBT_SUPPLIED = "Supplied";

    private static final String NBT_OPEN = "Open";

    private static final String NBT_CAROUSEL = "Carousel";

    private static final String TRACK = "base_controller";

    private final GeneStorageInventory rack = new GeneStorageInventory();

    private boolean isolated;

    private int fillOrder;

    private boolean supplied;

    private boolean open;

    private int supplyTimer;

    private boolean joined;

    private int viewers;

    /** Server: ticks until stage two's fog and sound. */
    private int stageTwoTimer = -1;

    /** Synced: gene colour of each carousel holder, 0 for empty. */
    private final int[] carousel = new int[GeneStorageInventory.CAROUSEL];

    // Client animation state.
    private boolean shownOpen;

    private boolean animationSent = true;

    private int clientStageTwo = -1;

    private boolean clientSeen;

    public GeneStorageBlockEntity(BlockPos pos, BlockState state) {
        super(HumanGeneStorage.BLOCK_ENTITY.get(), pos, state);
        rack.addListener(container -> rackChanged());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Network
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public BlockPos storagePos() {
        return worldPosition;
    }

    /** The core is the power port, so it is also where the storage sits on its grid. */
    @Override
    public BlockPos gridPos() {
        return worldPosition;
    }

    @Override
    public boolean isIsolated() {
        return isolated;
    }

    public void setIsolated(boolean isolated) {
        this.isolated = isolated;
        setChanged();
    }

    @Override
    public int fillOrder() {
        return fillOrder;
    }

    /**
     * Placement order, for ties in distance. Game time when placed: always increases, and needs no grid - the storage
     * is usually placed before its cable is.
     */
    void assignFillOrder() {
        if (level != null) {
            fillOrder = (int) Math.min(Integer.MAX_VALUE, level.getGameTime());
            setChanged();
        }
    }

    @Override
    public GeneStorageInventory rack() {
        return rack;
    }

    @Override
    public void rackChanged() {
        setChanged();
        var changed = false;
        for (int slot = 0; slot < carousel.length; slot++) {
            var stack = rack.getItem(slot);
            var colour = stack.isEmpty() ? 0 : GeneTubeItem.colour(stack) | 0xFF000000;
            if (carousel[slot] != colour) {
                carousel[slot] = colour;
                changed = true;
            }
        }
        if (changed) {
            sync();
        }
    }

    @Override
    public void setRemoved() {
        joined = false;
        if (level != null) {
            GeneStorageNetwork.leave(level, worldPosition);
        }
        super.setRemoved();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Power
    // ------------------------------------------------------------------------------------------------------------

    /** [stated] Oct 31: power from underneath only. */
    @Override
    public boolean canConnectOn(Direction direction) {
        var state = getBlockState();
        // [stated] Oct 31: genetic machines take power from the BOTTOM ONLY - a back port would sit over the models'
        // counters. Every bottom block of the machine takes a cable for this core (see the block's coreThrough).
        return direction == Direction.DOWN;
    }

    @Override
    public long getRequestedPower() {
        return 0L;
    }

    @Override
    public void unpoweredTick(Level level, BlockPos pos, BlockState state) {
        tick(level);
    }

    @Override
    public void poweredTick(Level level, BlockPos pos, BlockState state) {
        tick(level);
    }

    private void tick(Level level) {
        // Joins on its first server tick (common code has no load hook); leaves in setRemoved, which chunk unloads call
        // too.
        if (!joined) {
            joined = true;
            GeneStorageNetwork.join(level, this);
        }

        if (stageTwoTimer >= 0 && --stageTwoTimer == 0) {
            stageTwoTimer = -1;
            lidEffects(level, true);
        }

        if (--supplyTimer > 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        supplyTimer = SUPPLY_CHECK_TICKS;

        var grid = PowerSystem.get(serverLevel).gridAt(worldPosition);
        var nowSupplied = grid != null && grid.hasSupply();
        if (nowSupplied != supplied) {
            supplied = nowSupplied;
            if (!supplied && open) {
                setOpen(false);
            }
            sync();
        }
    }

    public boolean isSupplied() {
        return supplied;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Lid
    // ------------------------------------------------------------------------------------------------------------

    public boolean isOpen() {
        return open;
    }

    /** Right-click with an empty hand opens the rack screen - and the lid, while anyone has it open. */
    public void use(Player player) {
        if (!supplied) {
            player.displayClientMessage(Component.translatable("gui.avp_human.gene_storage.no_power"), true);
            return;
        }
        player.openMenu(
            new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, opener) -> new GeneStorageMenu(id, inventory, this),
                Component.translatable(getBlockState().getBlock().getDescriptionId())
            )
        );
    }

    /** Server: a screen opened on this storage. Not saved - a reload closes every screen. */
    void startViewing() {
        if (viewers++ == 0) {
            setOpen(true);
        }
    }

    void stopViewing() {
        if (viewers > 0 && --viewers == 0) {
            setOpen(false);
        }
    }

    public void setOpen(boolean open) {
        if (this.open == open || level == null) {
            return;
        }
        this.open = open;
        lidEffects(level, open);
        stageTwoTimer = open ? STAGE_TWO_DELAY : -1;
        setChanged();
        sync();
    }

    /** Fog off the lift, a hiss, and on opening a freeze crackle - the cold rack meeting room air. */
    private void lidEffects(Level level, boolean opening) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        var centre = modelCentre();
        serverLevel.sendParticles(ParticleTypes.CLOUD, centre.x, centre.y + 1.2D, centre.z, 10, 0.45D, 0.1D, 0.45D, 0.01D);
        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, centre.x, centre.y + 1.2D, centre.z, 6, 0.4D, 0.1D, 0.4D, 0.0D);
        serverLevel.playSound(null, centre.x, centre.y + 1.0D, centre.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.4F);
        if (opening) {
            serverLevel.playSound(
                null,
                centre.x,
                centre.y + 1.0D,
                centre.z,
                SoundEvents.PLAYER_HURT_FREEZE,
                SoundSource.BLOCKS,
                0.4F,
                0.8F
            );
        }
    }

    /** The middle of the 2x2 footprint, at floor level. */
    public Vec3 modelCentre() {
        var facing = getBlockState().getValue(GeneStorageBlock.FACING);
        var side = facing.getClockWise();
        var back = facing.getOpposite();
        return Vec3.atBottomCenterOf(worldPosition)
            .add(
                (side.getStepX() + back.getStepX()) * 0.5D,
                0.0D,
                (side.getStepZ() + back.getStepZ()) * 0.5D
            );
    }

    // ------------------------------------------------------------------------------------------------------------
    // Client
    // ------------------------------------------------------------------------------------------------------------

    public int[] carouselColours() {
        return carousel;
    }

    private static AzCommand<GeneStorageBlockEntity> clip(String name) {
        return AzCommand.<GeneStorageBlockEntity>idempotent().play(AzTarget.track(TRACK), name, AzPlayBehaviors.HOLD_ON_LAST_FRAME).build();
    }

    private static final AzCommand<GeneStorageBlockEntity> OPEN_1 = clip("open_1");

    private static final AzCommand<GeneStorageBlockEntity> OPEN_2 = clip("open_2");

    private static final AzCommand<GeneStorageBlockEntity> CLOSE = clip("close");

    /**
     * Plays the lid clips when the synced flag changes; retries until the animator exists (it is made on first render).
     */
    public void clientTick() {
        if (clientStageTwo >= 0 && --clientStageTwo == 0) {
            clientStageTwo = -1;
            if (!dispatch(OPEN_2)) {
                clientStageTwo = 1;
            }
        }
        if (!animationSent) {
            animationSent = dispatch(shownOpen ? OPEN_1 : CLOSE);
            if (animationSent && shownOpen) {
                clientStageTwo = STAGE_TWO_DELAY;
            }
        }
    }

    private boolean dispatch(AzCommand<GeneStorageBlockEntity> command) {
        if (level == null || !level.isClientSide() || com.blib.internal.client.animation.AzAnimatorAccessor.getOrNull(this) == null) {
            return false;
        }
        command.dispatchForBlockEntity(this);
        return true;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Save / sync
    // ------------------------------------------------------------------------------------------------------------

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(NBT_RACK, rack.save(new CompoundTag(), registries));
        tag.putBoolean(NBT_ISOLATED, isolated);
        tag.putInt(NBT_FILL_ORDER, fillOrder);
        // The lid's open flag is not saved: it follows open screens, and a reload closes them all.
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NBT_RACK)) {
            rack.load(tag.getCompound(NBT_RACK), registries);
        }
        isolated = tag.getBoolean(NBT_ISOLATED);
        fillOrder = tag.getInt(NBT_FILL_ORDER);
        open = tag.getBoolean(NBT_OPEN);
        supplied = tag.getBoolean(NBT_SUPPLIED);

        if (tag.contains(NBT_CAROUSEL)) {
            var colours = tag.getIntArray(NBT_CAROUSEL);
            System.arraycopy(colours, 0, carousel, 0, Math.min(colours.length, carousel.length));
        } else if (level == null || !level.isClientSide) {
            rackChanged();
        }

        // Client: a changed open flag replays the lid.
        if (level != null && level.isClientSide && !clientSeen) {
            // Coming into view: no replay of the whole lid - an open storage goes straight to its open pose.
            clientSeen = true;
            shownOpen = open;
            clientStageTwo = open ? 1 : -1;
        } else if (level != null && level.isClientSide && open != shownOpen) {
            shownOpen = open;
            animationSent = false;
            clientStageTwo = -1;
        } else if (level == null) {
            shownOpen = open;
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.putBoolean(NBT_SUPPLIED, supplied);
        tag.putBoolean(NBT_OPEN, open);
        tag.putIntArray(NBT_CAROUSEL, carousel);
        return tag;
    }
}
