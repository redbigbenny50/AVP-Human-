package com.human.common.gameplay.cosmetic;

import com.human.common.registry.init.HumanCosmetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * The block entity of every animated cosmetic piece. It never ticks.
 * <ul>
 * <li>Whether a door is open or a fan spins lives in the BLOCK STATE, not here, so it survives relogs and reaches every
 * client with the chunk. The easing fields below are only the renderer's smoothing between frames.</li>
 * <li>A WEAPON RACK ([stated] Oct 31: real racks) also keeps the guns on its mounts - one stack per mount, listed in
 * {@link CosmeticModelTables#SLOTS}. They save with the world and are sent to clients so every player sees them.</li>
 * </ul>
 */
public class CosmeticBlockEntity extends BlockEntity {

    /** 0 shut .. 1 fully open, eased toward the OPEN state frame by frame (client only). */
    public float openProgress = Float.NaN;

    /** Current spinner angle, degrees (client only). */
    public float spinAngle;

    /** 0 still .. 1 full speed, so a fan winds up and coasts down instead of snapping (client only). */
    public float spinSpeed;

    /** When this was last drawn, in ticks (game time + partial tick); NaN before the first frame (client only). */
    public double lastDrawn = Double.NaN;

    /** The guns on a rack's mounts; empty for anything that is not a rack. */
    private final NonNullList<ItemStack> items;

    // ------------------------------------------------------------------------------------------------------------
    // Holotable - [stated] Oct 31: takes a filled map and projects the map's real terrain in green
    // ------------------------------------------------------------------------------------------------------------

    /** Projection resolution: 64 x 64 columns over the map's area. */
    public static final int HOLO = 64;

    /** A sampled column whose height was never read (its chunk was never loaded): drawn flat. */
    public static final int UNKNOWN = Integer.MIN_VALUE;

    /** How often the table re-reads its map while it holds one (ticks). */
    private static final int HOLO_REFRESH_TICKS = 100;

    private final boolean holotable;

    private ItemStack holoMap = ItemStack.EMPTY;

    /** Surface height of each column (world Y), or UNKNOWN. */
    private final int[] holoHeights = new int[HOLO * HOLO];

    /** 0 = unexplored on the map (not drawn), 1 = land, 2 = water. */
    private final byte[] holoKinds = new byte[HOLO * HOLO];

    private int holoRefreshCooldown;

    /**
     * Client only: what runs each client tick on a running machine piece - its working loop. Set by the client at
     * start-up (as for the powered machines); null on a dedicated server, where this never ticks.
     */
    public static java.util.function.@org.jetbrains.annotations.Nullable Consumer<CosmeticBlockEntity> clientTicker;

    /** Client only: this piece's playing loop sound, or null. */
    public @org.jetbrains.annotations.Nullable Object clientSound;

    /** A storage piece's contents ([stated] Oct 31: chest-style storage); null for anything else. */
    private final CosmeticStorage.@org.jetbrains.annotations.Nullable Contents storage;

    /**
     * Client only: the wall display's own random tactical map (heights 0..255, 0 = water), built once from its
     * position.
     */
    public byte[] randomMap;

    /**
     * Bumped whenever the projection data changes; the renderer rebuilds its cached arrays only then, never per frame.
     */
    public int holoVersion;

    /** Client only: the renderer's cached per-column arrays, and the data version they were built from. */
    public float @org.jetbrains.annotations.Nullable [] clientShade, clientColumns;

    public int clientCacheVersion = -1;

    public CosmeticBlockEntity(BlockPos pos, BlockState state) {
        super(HumanCosmetics.DECOR_BLOCK_ENTITY.get(), pos, state);
        var mounts = state.getBlock() instanceof CosmeticBlock block ? CosmeticModelTables.SLOTS.get(block.design().id()) : null;
        this.items = NonNullList.withSize(mounts == null ? 0 : mounts.length, ItemStack.EMPTY);
        this.holotable = state.getBlock() instanceof CosmeticBlock block && CosmeticBlock.isMapDisplay(block.design());
        this.storage = state.getBlock() instanceof CosmeticBlock block && CosmeticStorage.has(block.design())
            ? new CosmeticStorage.Contents(this, CosmeticStorage.SLOTS.get(block.design()))
            : null;
        java.util.Arrays.fill(holoHeights, UNKNOWN);
        this.holoRefreshCooldown = 1 + (int) (Math.floorMod(pos.asLong() * 31L, (long) HOLO_REFRESH_TICKS));
    }

    public CosmeticStorage.@org.jetbrains.annotations.Nullable Contents storage() {
        return storage;
    }

    public boolean isHolotable() {
        return holotable;
    }

    public ItemStack holoMap() {
        return holoMap;
    }

    public int[] holoHeights() {
        return holoHeights;
    }

    public byte[] holoKinds() {
        return holoKinds;
    }

    /** Puts a map on the table (or takes it off with EMPTY) and samples it straight away. */
    public void setHoloMap(ItemStack map) {
        holoMap = map;
        java.util.Arrays.fill(holoHeights, UNKNOWN);
        java.util.Arrays.fill(holoKinds, (byte) 0);

        if (!map.isEmpty() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            refreshHolo(serverLevel);
        }

        holoVersion++;
        setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Server tick - only the holotable has one: re-reads the map every few seconds while it holds one. */
    public void serverTick(net.minecraft.server.level.ServerLevel level) {
        if (holoMap.isEmpty() || --holoRefreshCooldown > 0) {
            return;
        }

        holoRefreshCooldown = HOLO_REFRESH_TICKS;

        if (refreshHolo(level)) {
            holoVersion++;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Samples the map's area: which columns the map has explored (and whether they are water) from the map's own
     * colours, and each column's real surface height from the world ([stated] Oct 31: real terrain). Only LOADED chunks
     * are read - a column whose chunk is not loaded keeps the height last seen, or stays flat if never seen - so this
     * never loads or generates a chunk. Returns whether anything changed (clients are only sent real changes).
     */
    private boolean refreshHolo(net.minecraft.server.level.ServerLevel level) {
        var data = net.minecraft.world.item.MapItem.getSavedData(holoMap, level);

        if (data == null) {
            return false;
        }

        var size = 128 << data.scale;
        var originX = data.centerX - size / 2;
        var originZ = data.centerZ - size / 2;
        var changed = false;
        var water = net.minecraft.world.level.material.MapColor.WATER.id;

        for (int j = 0; j < HOLO; j++) {
            for (int i = 0; i < HOLO; i++) {
                var index = j * HOLO + i;
                var colour = (data.colors[(i * 2) + (j * 2) * 128] & 0xFF) >> 2;
                byte kind = colour == 0 ? 0 : (byte) (colour == water ? 2 : 1);
                var x = originX + (int) ((i + 0.5) * size / HOLO);
                var z = originZ + (int) ((j + 0.5) * size / HOLO);
                var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                var height = chunk == null
                    ? holoHeights[index]
                    : chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);

                if (kind != holoKinds[index] || height != holoHeights[index]) {
                    holoKinds[index] = kind;
                    holoHeights[index] = height;
                    changed = true;
                }
            }
        }

        return changed;
    }

    /** The design and style this block entity draws. */
    public CosmeticBlock block() {
        return (CosmeticBlock) getBlockState().getBlock();
    }

    public boolean hasMounts() {
        return !items.isEmpty();
    }

    /** Whether there is anything to sync to clients (a rack's guns, a holotable's map and projection). */
    private boolean syncs() {
        return hasMounts() || holotable;
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public ItemStack getItem(int mount) {
        return mount >= 0 && mount < items.size() ? items.get(mount) : ItemStack.EMPTY;
    }

    /** Puts {@code stack} on a mount and tells every client. */
    public void setItem(int mount, ItemStack stack) {
        if (mount < 0 || mount >= items.size()) {
            return;
        }

        items.set(mount, stack);
        setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        if (hasMounts()) {
            ContainerHelper.saveAllItems(tag, items, true, registries);
        }

        if (storage != null) {
            tag.put("Storage", storage.createTag(registries));
        }

        if (holotable && !holoMap.isEmpty()) {
            tag.put("HoloMap", holoMap.save(registries));
            tag.putIntArray("HoloHeights", holoHeights);
            tag.putByteArray("HoloKinds", holoKinds);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        if (hasMounts()) {
            for (int i = 0; i < items.size(); i++) {
                items.set(i, ItemStack.EMPTY);
            }

            ContainerHelper.loadAllItems(tag, items, registries);
        }

        if (storage != null && tag.contains("Storage")) {
            storage.clearContent();
            storage.fromTag(tag.getList("Storage", net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
        }

        if (holotable) {
            holoMap = tag.contains("HoloMap") ? ItemStack.parse(registries, tag.get("HoloMap")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
            var heights = tag.getIntArray("HoloHeights");
            var kinds = tag.getByteArray("HoloKinds");
            java.util.Arrays.fill(holoHeights, UNKNOWN);
            java.util.Arrays.fill(holoKinds, (byte) 0);

            if (heights.length == holoHeights.length && kinds.length == holoKinds.length) {
                System.arraycopy(heights, 0, holoHeights, 0, heights.length);
                System.arraycopy(kinds, 0, holoKinds, 0, kinds.length);
            }

            holoVersion++;
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        // Only what clients DRAW: rack/shelf items and a map display's projection. Storage contents stay on the server
        // -
        // with a room full of lockers, sending every inventory with every chunk would be pure waste.
        var tag = new CompoundTag();

        if (hasMounts()) {
            ContainerHelper.saveAllItems(tag, items, true, registries);
        }

        if (holotable && !holoMap.isEmpty()) {
            tag.put("HoloMap", holoMap.save(registries));
            tag.putIntArray("HoloHeights", holoHeights);
            tag.putByteArray("HoloKinds", holoKinds);
        }

        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return syncs() ? ClientboundBlockEntityDataPacket.create(this) : null;
    }
}
