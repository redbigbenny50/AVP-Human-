package com.human.common.gameplay.block.cryo;

import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One block of a cryotank. Every block knows its MASTER - the lowest corner of the tank it belongs to - and only the
 * master holds anything: the tank's size and the frozen specimen.
 * <h2>The specimen is data, not an entity</h2> [stated] Oct 1: the mob's AI is off while frozen, and it must survive
 * unloads, relogs and crashes with all its NBT - genes, effects, names and so on. So it is saved here exactly as the
 * game saves an entity to disk ({@code saveAsPassenger}), and nothing of it exists in the world until it is released. A
 * frozen specimen costs the server nothing per tick. The client gets a copy only to draw it.
 * <h2>Nothing here ticks</h2> The controller drives the thaw; a tank only stores.
 */
public class CryotankBlockEntity extends BlockEntity {

    private static final String NBT_MASTER = "master";

    private static final String NBT_SIZE = "size";

    private static final String NBT_SPECIMEN = "specimen";

    private static final String NBT_SCALE = "scale";

    private static final String NBT_THAW = "thaw";

    private static final String NBT_DIMS = "dims";

    private @Nullable BlockPos master;

    private int sizeX = 1;

    private int sizeY = 1;

    private int sizeZ = 1;

    private @Nullable CompoundTag specimen;

    private float scale = 1.0F;

    /** The specimen's width and height at capture, so a re-formed tank can tell whether it still fits. */
    private float specimenWidth;

    private float specimenHeight;

    private int thawTicks;

    /**
     * [stated] Oct 3: how far the glass has frosted over, in ticks, out of {@link #thawTicksRequired()}. It rises while
     * the tank runs, follows the thaw down, and is 0 when empty - so frosting over and thawing clear both take exactly
     * a thaw's length.
     */
    private int frostTicks;

    /**
     * The controller that serves this tank. Two controllers on one conduit run would otherwise BOTH bill it and both
     * thaw it; the first to claim it keeps it until that controller is gone.
     */
    private @Nullable BlockPos owner;

    /** 1 = kept longest, 9 = first to go when power or batteries run short. [stated] Oct 2. */
    public static final int MIN_PRIORITY = 1;

    public static final int MAX_PRIORITY = 9;

    public static final int DEFAULT_PRIORITY = 5;

    private int priority = DEFAULT_PRIORITY;

    /**
     * [stated] Oct 2: an occupied tube is active; DEACTIVATING it from the controller starts the thaw, and when the
     * countdown ends the specimen is let out. Cleared when the tube empties.
     */
    private boolean deactivated;

    /** The specimen's data sheet - see {@link SpecimenCard}. */
    private @Nullable CompoundTag card;

    public CryotankBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.CRYOTANK.get(), pos, state);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Shape
    // ------------------------------------------------------------------------------------------------------------

    /** The master's position; a block that has never been formed is its own master. */
    public BlockPos getMasterPos() {
        return master == null ? worldPosition : master;
    }

    public boolean isMaster() {
        return getMasterPos().equals(worldPosition);
    }

    public @Nullable BlockPos getOwner() {
        return owner;
    }

    void setOwner(@Nullable BlockPos owner) {
        if (!java.util.Objects.equals(this.owner, owner)) {
            this.owner = owner == null ? null : owner.immutable();
            setChanged();
        }
    }

    /**
     * @return true if anything changed. ⚠ The caller only re-sends a block whose shape changed: building a 15x15x15
     *         tank block by block re-forms the cluster on every placement, and re-sending all 3,375 blocks each time
     *         would be millions of packets.
     */
    boolean setShape(BlockPos master, int sizeX, int sizeY, int sizeZ) {
        if (master.equals(this.master) && sizeX == this.sizeX && sizeY == this.sizeY && sizeZ == this.sizeZ) {
            return false;
        }

        // A re-formed tank is a new tank: whichever controller reaches it first claims it again.
        this.owner = null;
        this.master = master.immutable();
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        return true;
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    public int blockCount() {
        return sizeX * sizeY * sizeZ;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Specimen
    // ------------------------------------------------------------------------------------------------------------

    public boolean hasSpecimen() {
        return specimen != null;
    }

    public @Nullable CompoundTag getSpecimen() {
        return specimen;
    }

    public float getScale() {
        return scale;
    }

    public float specimenWidth() {
        return specimenWidth;
    }

    public float specimenHeight() {
        return specimenHeight;
    }

    void setSpecimen(@Nullable CompoundTag tag, float scale, float width, float height) {
        this.specimen = tag;
        this.scale = scale;
        this.specimenWidth = width;
        this.specimenHeight = height;
        this.thawTicks = 0;
        this.deactivated = false;

        if (tag == null) {
            this.card = null;
        }

        markUpdated();
        lightTank(tag != null);

        // A new specimen frosts over from clear; an empty tank is clear at once.
        frostTicks = 0;

        if (tag == null) {
            applyFrost(0);
        }
    }

    /**
     * One tick of frost, from the controller that keeps this tank. Running (a specimen, not deactivated, not thawing):
     * frosts over. Thawing - a deactivation, or power or cryofluid failing - the frost follows the thaw. Empty: clear.
     * ⚡ The blocks are only rewritten when the level STEPS (four levels), never every tick.
     */
    void tickFrost() {
        if (level == null || level.isClientSide || !isMaster()) {
            return;
        }

        var required = Math.max(1, thawTicksRequired());

        if (!hasSpecimen()) {
            frostTicks = 0;
        } else if (thawTicks > 0) {
            frostTicks = Math.min(frostTicks, Math.max(0, required - thawTicks));
        } else if (!deactivated && frostTicks < required) {
            frostTicks++;
        }

        applyFrost((int) Math.ceil(3.0 * frostTicks / required));
    }

    /** Sets every block of this tank to frost {@code frost} (0-3), only if it differs from what they show. */
    private void applyFrost(int frost) {
        if (level == null || level.isClientSide) {
            return;
        }

        var own = getBlockState();

        if (own.hasProperty(CryotankBlock.FROST) && own.getValue(CryotankBlock.FROST) == frost) {
            return;
        }

        var cursor = new BlockPos.MutableBlockPos();

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    cursor.set(worldPosition.getX() + x, worldPosition.getY() + y, worldPosition.getZ() + z);
                    var state = level.getBlockState(cursor);

                    if (state.getBlock() instanceof CryotankBlock && state.getValue(CryotankBlock.FROST) != frost) {
                        level.setBlock(
                            cursor,
                            state.setValue(CryotankBlock.FROST, frost),
                            net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                        );
                    }
                }
            }
        }
    }

    /**
     * Lights every block of this tank while it holds a specimen, darkens it when it does not. Runs on this block - the
     * master, the tank's lowest corner - and walks the tank's box. ⚡ Only when a specimen goes in or out, never per
     * tick, and only blocks whose light actually changes are re-sent.
     */
    private void lightTank(boolean lit) {
        if (level == null || level.isClientSide) {
            return;
        }

        var cursor = new BlockPos.MutableBlockPos();

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    cursor.set(worldPosition.getX() + x, worldPosition.getY() + y, worldPosition.getZ() + z);
                    var state = level.getBlockState(cursor);

                    if (state.getBlock() instanceof CryotankBlock && state.getValue(CryotankBlock.LIT) != lit) {
                        level.setBlock(
                            cursor,
                            state.setValue(CryotankBlock.LIT, lit),
                            net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                        );
                    }
                }
            }
        }
    }

    public int getPriority() {
        return priority;
    }

    void setPriority(int priority) {
        var clamped = Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY, priority));

        if (clamped != this.priority) {
            this.priority = clamped;
            setChanged();
        }
    }

    public boolean isDeactivated() {
        return deactivated;
    }

    void setDeactivated(boolean deactivated) {
        if (deactivated != this.deactivated) {
            this.deactivated = deactivated;
            setChanged();
        }
    }

    public @Nullable CompoundTag getCard() {
        return card;
    }

    void setCard(@Nullable CompoundTag card) {
        this.card = card;
        setChanged();
    }

    /** The entity type id of the specimen, e.g. {@code avp_alien:drone}, for the controller screen. */
    public @Nullable String specimenTypeId() {
        return specimen == null || !specimen.contains("id") ? null : specimen.getString("id");
    }

    public int getThawTicks() {
        return thawTicks;
    }

    /**
     * How long a thaw takes - and so how long an unpowered tank holds before the specimen breaks out, and how long the
     * glass takes to frost over or thaw clear.
     * <p>
     * [stated] Oct 3: it is HEAT LOSS. A bigger tank holds more cold and warms slower; a bigger creature has more body
     * mass and warms faster. So the time follows the ratio of tank volume to the creature's HITBOX volume (width x
     * width x height, as stored when it was frozen): {@code 10 s x cube root(tank / creature)}, between 5 seconds and 2
     * minutes. The slowest thaw is a small creature in a huge tank. [stated] "bigger = riskier, which is why those
     * should be high priority" - a big specimen in a snug tank breaks out in about 13 seconds without power.
     * </p>
     * The cube root keeps it sensible: doubling the tank adds about 26%, not 100%. Examples: a zombie in a 1x1x2 tube
     * ~14 s, a chicken there ~26 s, a queen-size hitbox in 5x5x7 ~13 s, a chicken in 15x15x15 the full 2 minutes.
     */
    public int thawTicksRequired() {
        var creature = (double) specimenWidth * specimenWidth * specimenHeight;

        // A specimen frozen before the hitbox was stored has no size: treat it as one block, never divide by zero.
        if (!(creature > 0.0)) {
            creature = 1.0;
        }

        var seconds = 10.0 * Math.cbrt(blockCount() / creature);
        return (int) Math.round(Math.max(5.0, Math.min(120.0, seconds)) * 20.0);
    }

    void setThawTicks(int ticks) {
        if (ticks != thawTicks) {
            thawTicks = ticks;
            setChanged();
        }
    }

    /** Saves AND tells watching clients - a specimen change has to reach the renderer. */
    void markUpdated() {
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        if (master != null) {
            tag.putLong(NBT_MASTER, master.asLong());
        }

        tag.putIntArray(NBT_SIZE, new int[] { sizeX, sizeY, sizeZ });

        if (owner != null) {
            tag.putLong("owner", owner.asLong());
        }

        tag.putInt("priority", priority);

        if (deactivated) {
            tag.putBoolean("deactivated", true);
        }

        if (card != null) {
            tag.put("card", card.copy());
        }

        if (specimen != null) {
            tag.put(NBT_SPECIMEN, specimen.copy());
            tag.putFloat(NBT_SCALE, scale);
            tag.putIntArray(NBT_DIMS, new int[] { Float.floatToIntBits(specimenWidth), Float.floatToIntBits(specimenHeight) });
            tag.putInt(NBT_THAW, thawTicks);
            tag.putInt("frostTicks", frostTicks);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        master = tag.contains(NBT_MASTER) ? BlockPos.of(tag.getLong(NBT_MASTER)) : null;

        owner = tag.contains("owner") ? BlockPos.of(tag.getLong("owner")) : null;
        priority = tag.contains("priority") ? tag.getInt("priority") : DEFAULT_PRIORITY;
        deactivated = tag.getBoolean("deactivated");
        card = tag.contains("card") ? tag.getCompound("card") : null;

        var size = tag.getIntArray(NBT_SIZE);

        if (size.length == 3) {
            sizeX = Math.max(1, size[0]);
            sizeY = Math.max(1, size[1]);
            sizeZ = Math.max(1, size[2]);
        }

        if (tag.contains(NBT_SPECIMEN)) {
            specimen = tag.getCompound(NBT_SPECIMEN);
            scale = tag.contains(NBT_SCALE) ? tag.getFloat(NBT_SCALE) : 1.0F;
            var dims = tag.getIntArray(NBT_DIMS);
            specimenWidth = dims.length == 2 ? Float.intBitsToFloat(dims[0]) : 0.0F;
            specimenHeight = dims.length == 2 ? Float.intBitsToFloat(dims[1]) : 0.0F;
            thawTicks = tag.getInt(NBT_THAW);
            frostTicks = tag.getInt("frostTicks");
        } else {
            specimen = null;
            thawTicks = 0;
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
