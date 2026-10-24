package com.human.common.gameplay.storage;

import com.human.common.gameplay.item.pipe.PipeItemAccess;
import com.human.common.registry.init.HumanCrates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Every block of a crate has one of these. The anchor block is the MASTER: it owns the inventory and is the only one
 * the renderer draws. Every other block only remembers where its master is, and answers pipes and other mods with the
 * master's inventory.
 */
public class CrateBlockEntity extends BlockEntity {

    private BlockPos masterOffset = BlockPos.ZERO;

    private @Nullable CrateInventory inventory;

    private @Nullable Object loaderView;

    /** Set once the crate has been turned into an item, so its other blocks do not drop it again. */
    private boolean dismantled;

    /** Creative players breaking an EMPTY crate get nothing, as with shulker boxes. */
    private boolean skipDrop;

    /**
     * A loot table still waiting to fill this crate, or null.
     * <p>
     * [stated] Oct 23 - "add loot table support for the crates." The mining camps keep their grinder dusts and smelter
     * ingots in crates, and crates could not carry a loot table. Now one works as it does on a chest: set
     * {@code LootTable} (and optionally {@code LootTableSeed}) on the crate - by command or in a structure - and it
     * fills the first time anything reaches into the crate: a player opening it, a pipe, another mod's storage view, or
     * breaking it. A seed of 0 (what a structure leaves) rolls fresh loot for every copy of the structure.
     * </p>
     * <p>
     * Only the MASTER block's table counts; the crate's other blocks have no inventory of their own.
     * </p>
     */
    private net.minecraft.resources.@Nullable ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootTable;

    private long lootTableSeed;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(HumanCrates.CRATE_BLOCK_ENTITY.get(), pos, state);
    }

    public CrateType type() {
        return ((CrateBlock) getBlockState().getBlock()).type();
    }

    public Direction facing() {
        return getBlockState().getValue(CrateBlock.FACING);
    }

    public boolean isMaster() {
        return masterOffset.equals(BlockPos.ZERO);
    }

    public BlockPos masterPos() {
        return worldPosition.offset(masterOffset);
    }

    public void setMasterPos(BlockPos master) {
        masterOffset = master.subtract(worldPosition);
        setChanged();
    }

    public @Nullable CrateBlockEntity master() {
        if (isMaster()) {
            return this;
        }

        if (level == null) {
            return null;
        }

        return level.getBlockEntity(masterPos()) instanceof CrateBlockEntity crate && crate.isMaster() ? crate : null;
    }

    /** The master's inventory. Only meaningful on the master. */
    public CrateInventory inventory() {
        if (inventory == null) {
            inventory = new CrateInventory(type().size().slots());
            // Only mark the chunk for saving. Plain setChanged() would also wake the neighbouring blocks for
            // comparators on EVERY item moved; crates have no comparator output, so that is pure tick cost.
            inventory.setOnChange(() -> {
                if (level != null) {
                    level.blockEntityChanged(worldPosition);
                }
            });
        }

        // Every way in - the menu, pipes, the loader storage view, breaking - goes through here, so this is the one
        // place a pending loot table needs to be rolled.
        unpackLootTable();

        return inventory;
    }

    /** Sets (or clears, with null) the loot table this crate fills from. Call on the master. */
    public void setLootTable(
        net.minecraft.resources.@Nullable ResourceKey<net.minecraft.world.level.storage.loot.LootTable> table,
        long seed
    ) {
        lootTable = table;
        lootTableSeed = seed;
        setChanged();
    }

    /**
     * Rolls the pending loot table into the inventory, once, on the server. Cleared BEFORE rolling: inserting the loot
     * calls back into the inventory, and a second pass must find nothing left to do. Anything that will not fit (a
     * table larger than the crate) is dropped on the ground rather than lost.
     */
    private void unpackLootTable() {
        if (
            lootTable == null
                || inventory == null
                || !isMaster()
                || !(level instanceof net.minecraft.server.level.ServerLevel server)
        ) {
            return;
        }

        var key = lootTable;
        var seed = lootTableSeed;
        lootTable = null;
        lootTableSeed = 0L;

        var table = server.getServer().reloadableRegistries().getLootTable(key);
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(server)
            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);

        for (var stack : table.getRandomItems(params, seed)) {
            var moved = inventory.insert(stack, false);

            if (moved < stack.getCount()) {
                net.minecraft.world.Containers.dropItemStack(
                    server,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5,
                    stack.copyWithCount(stack.getCount() - moved)
                );
            }
        }

        setChanged();
    }

    /** The loader's storage view of this crate (Fabric storage / NeoForge handler), built once and reused. */
    @SuppressWarnings("unchecked")
    public <T> T loaderView(java.util.function.Function<CrateInventory, T> factory) {
        if (loaderView == null) {
            loaderView = factory.apply(inventory());
        }

        return (T) loaderView;
    }

    /** Every block of this crate, the master first. Call on the master. */
    public List<BlockPos> parts() {
        return CrateFootprint.positions(worldPosition, facing(), type().size());
    }

    /** World position of the footprint's bottom centre. Call on the master. */
    public Vec3 centre() {
        return Vec3.atLowerCornerOf(worldPosition).add(CrateFootprint.centre(facing(), type().size()));
    }

    /** What a pipe on {@code side} of this block sees, or null if the master is gone. */
    public @Nullable PipeItemAccess.Port pipePort(Direction side) {
        var master = master();

        return master == null ? null : new CratePipePort(master.inventory(), side, master.getBlockPos());
    }

    boolean isDismantled() {
        return dismantled;
    }

    void setDismantled() {
        dismantled = true;
    }

    boolean skipDrop() {
        return skipDrop;
    }

    void setSkipDrop() {
        skipDrop = true;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        writeMaster(tag);

        if (isMaster() && inventory != null && !inventory.isEmpty()) {
            tag.put("Items", inventory.save(registries));
        }

        if (isMaster() && lootTable != null) {
            tag.putString("LootTable", lootTable.location().toString());

            if (lootTableSeed != 0L) {
                tag.putLong("LootTableSeed", lootTableSeed);
            }
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        masterOffset = new BlockPos(tag.getInt("MasterX"), tag.getInt("MasterY"), tag.getInt("MasterZ"));

        if (tag.contains("Items")) {
            inventory().load(CrateInventory.readList(tag, "Items"), registries);
        }

        // Read AFTER the items: loading them goes through inventory(), and a table set before that would roll into
        // a crate that is about to be overwritten. (It would not anyway - there is no level yet during a load - but
        // the order keeps it obviously safe.)
        if (tag.contains("LootTable", net.minecraft.nbt.Tag.TAG_STRING)) {
            lootTable = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.ResourceLocation.parse(tag.getString("LootTable"))
            );
            lootTableSeed = tag.getLong("LootTableSeed");
        } else {
            lootTable = null;
            lootTableSeed = 0L;
        }
    }

    /** The client only needs to know which block is the master. Items are never sent with the block. */
    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        var tag = new CompoundTag();

        writeMaster(tag);

        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void writeMaster(CompoundTag tag) {
        tag.putInt("MasterX", masterOffset.getX());
        tag.putInt("MasterY", masterOffset.getY());
        tag.putInt("MasterZ", masterOffset.getZ());
    }
}
