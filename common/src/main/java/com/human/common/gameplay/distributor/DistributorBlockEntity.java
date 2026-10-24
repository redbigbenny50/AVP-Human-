package com.human.common.gameplay.distributor;

import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.block.power.machine.ElectricMachineBlock;
import com.human.common.gameplay.machine.ElectricMachineKind;
import com.human.common.gameplay.vapor.VaporEffects;
import com.human.common.gameplay.vapor.VaporKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The distributor - an industrial fog machine. [stated] Oct 17, locked design (DISTRIBUTOR_SPEC.md).
 * <p>
 * Everything a tier-1 machine has comes from {@link ElectricMachineBlockEntity}: power from the BACK, 30 FE/t drawn
 * only while pouring, the off / on / working light (the fan spins while WORKING), priority, the upgrade cards, port
 * settings, the fuse panel and the wiring tool. The machine's own 8-bucket tank is the LIQUID IN on the TOP, and its
 * one slot is the TANK IN, fed from the LEFT or RIGHT. This class adds what only a distributor has.
 * </p>
 * <h2>THE CHAMBER - [stated] 1,000 vapor</h2>
 * <ul>
 * <li>Liquid in the tank refills it on its own: 1 bucket = 1,000 vapor, so 1 mB = 1 vapor, moved straight across as
 * soon as there is room.</li>
 * <li>A vapor tank in the slot fills it whole and is USED UP, nothing returned ([stated] like the fuel tank). ⚠ Only
 * into an EMPTY chamber - a tank put into a chamber with 999 left would throw 999 away.</li>
 * <li>One vapor at a time. A different vapor waits (in the tank or the slot) until the chamber runs dry.</li>
 * <li>It pours 1 vapor a tick: a full chamber is 50 seconds of fog.</li>
 * </ul>
 * <h2>WHEN IT POURS</h2> {@link DistributorMode} - ALWAYS, REDSTONE, or SENSOR - unless a MUTHUR command
 * ({@link DistributorRemote}) says otherwise.
 * <h2>THE ROOM</h2> {@link DistributorRoom}, scanned a budget of blocks per tick and cached. The server only scans
 * while it needs the room - while pouring (for the vapor effects) or in SENSOR mode (to know whether a hostile is
 * INSIDE the room rather than behind a wall). It rescans every {@value #RESCAN_TICKS} ticks while needed, and at once
 * when a block near it is placed, broken or changed, or a door opens or closes ({@link Listener}, vanilla's game events
 * - no mixin).
 * <h2>SAVED</h2> chamber amount + vapor, mode, and the MUTHUR command - every change calls setChanged(). The room is
 * NOT saved: it is rebuilt from the world in well under a second.
 */
public class DistributorBlockEntity extends ElectricMachineBlockEntity implements GameEventListener.Provider<DistributorBlockEntity.Listener> {

    /** [stated] one fuel tank's worth. */
    public static final int CHAMBER_CAPACITY = VaporKind.PER_TANK;

    /** [stated] "pours 1 vapor per tick". */
    public static final int POUR_PER_TICK = 1;

    /** Blocks the room scan may check per tick - a worst-case open area finishes in under a second. */
    public static final int SCAN_BUDGET = 2048;

    /** [stated] "re-read every few seconds". */
    public static final int RESCAN_TICKS = 100;

    /** How often the sensor looks for hostiles. */
    private static final int SENSOR_INTERVAL_TICKS = 10;

    /** [stated] "stops a few seconds after it clears": 5 s. */
    public static final int SENSOR_HOLD_TICKS = 100;

    /** How often whatever stands in the fog is handed to {@link VaporEffects}. */
    private static final int EFFECT_INTERVAL_TICKS = 10;

    /** The block-change listener's reach; vanilla's block events are announced 16 blocks out. */
    private static final int LISTENER_RADIUS = DistributorFog.RADIUS;

    /** avp_alien's xenomorph tag, built from strings so avp_human never names an avp_alien class. */
    private static final TagKey<EntityType<?>> ALIENS = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("avp_alien", "aliens")
    );

    // Menu data, after the shared machine values (ElectricMachineKind#extraDataCount).
    public static final int EXTRA_CHAMBER = 0;

    public static final int EXTRA_VAPOR = 1;

    public static final int EXTRA_MODE = 2;

    public static final int EXTRA_REMOTE = 3;

    public static final int EXTRA_SENSING = 4;

    public static final int EXTRA_COUNT = 5;

    /** Menu buttons: BUTTON_SET_MODE + mode ordinal. Clear of every id the shared menu uses (0-247). */
    public static final int BUTTON_SET_MODE = 300;

    private static final String NBT_CHAMBER = "chamber";

    private static final String NBT_VAPOR = "vapor";

    private static final String NBT_MODE = "mode";

    private static final String NBT_REMOTE = "remote";

    /** Marks the small tag clients are sent, so it is never mistaken for a full save. */
    private static final String NBT_CLIENT = "distributorSync";

    private int chamber;

    private @Nullable VaporKind vapor;

    private DistributorMode mode = DistributorMode.ALWAYS;

    private DistributorRemote remote = DistributorRemote.NONE;

    /** SENSOR: pour until this game time. Not saved - a reload re-senses within half a second. */
    private long sensorHoldUntil = Long.MIN_VALUE / 2;

    private final DistributorRoom room = new DistributorRoom();

    /** A nearby block changed: rescan when the room is next needed. */
    private boolean roomDirty = true;

    private @Nullable Direction scannedFront;

    private final Listener listener;

    /** The vapor the clients were last told about - they colour the fog with it. */
    private @Nullable VaporKind syncedVapor;

    /** CLIENT ONLY: the vapor the server says is in the chamber. */
    public @Nullable VaporKind clientVapor;

    /**
     * CLIENT ONLY: this client's own copy of the room, for the particles. Typed as the common class; no client types.
     */
    public final DistributorRoom clientRoom = new DistributorRoom();

    /** CLIENT ONLY: game time this client last started a room scan. */
    public long clientScanStarted = Long.MIN_VALUE / 2;

    public DistributorBlockEntity(BlockPos pos, BlockState state) {
        super(ElectricMachineKind.DISTRIBUTOR, pos, state);
        this.listener = new Listener(new BlockPositionSource(pos));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public void serverTick(Level level, BlockPos blockPos, BlockState blockState) {
        refillChamber();

        var now = level.getGameTime();

        // ⚡ The room is only kept up while something needs it: pouring, or a SENSOR with vapor to pour. An empty,
        // switched-off or waiting distributor scans nothing and searches for nothing.
        var sensing = mode == DistributorMode.SENSOR && isEnabled() && chamber > 0 && remote != DistributorRemote.STOP;

        if (sensing || isPouringSoon(level)) {
            tickRoom(level, blockPos, blockState, now);
        }

        if (sensing && now % SENSOR_INTERVAL_TICKS == 0 && senseHostile(level, blockPos)) {
            sensorHoldUntil = now + SENSOR_HOLD_TICKS;
        }

        // The shared machine tick: asks DistributorMachineLogic whether to pour, draws 30 FE/t, pours on the powered
        // tick, keeps the light (and so the fan) right.
        super.serverTick(level, blockPos, blockState);

        if (vapor != null && level instanceof ServerLevel serverLevel && now % EFFECT_INTERVAL_TICKS == 0 && isWorkingState()) {
            applyEffects(serverLevel, blockPos, vapor);
        }

        if (syncedVapor != vapor) {
            syncedVapor = vapor;
            level.sendBlockUpdated(blockPos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void tickRoom(Level level, BlockPos blockPos, BlockState blockState, long now) {
        var front = front(blockState);

        if (!room.isRunning()) {
            var stale = !room.hasResult() || roomDirty || front != scannedFront || now - room.finishedAt() >= RESCAN_TICKS;

            if (!stale) {
                return;
            }

            roomDirty = false;
            scannedFront = front;
            room.start(level, blockPos, front);
        }

        room.step(level, SCAN_BUDGET, now);
    }

    /**
     * Fills the chamber. Liquid first - it tops up by the millibucket and wastes nothing - then a tank, only into an
     * empty chamber.
     */
    private void refillChamber() {
        var liquid = getFluid();

        if (!liquid.isEmpty() && chamber < CHAMBER_CAPACITY) {
            var kind = VaporKind.fromFluid(liquid.fluid());

            if (kind != null && (vapor == null || vapor == kind || chamber == 0)) {
                var moved = Math.min(CHAMBER_CAPACITY - chamber, liquid.amount());

                if (moved > 0) {
                    removeFluid(moved);
                    vapor = kind;
                    chamber += moved;
                    setChanged();
                }
            }
        }

        if (chamber == 0) {
            var input = getInput();
            var kind = VaporKind.fromTank(input);

            if (kind != null) {
                // [stated] the tank is used up - nothing is returned.
                input.shrink(1);
                vapor = kind;
                chamber = CHAMBER_CAPACITY;
                setChanged();
            }
        }
    }

    /** Whether this machine would pour right now if it had power - what DistributorMachineLogic asks every tick. */
    public boolean wantsToPour(Level level) {
        if (chamber <= 0 || vapor == null || remote == DistributorRemote.STOP) {
            return false;
        }

        if (remote == DistributorRemote.POUR) {
            return true;
        }

        return switch (mode) {
            case ALWAYS -> true;
            case REDSTONE -> level.hasNeighborSignal(worldPosition);
            case SENSOR -> level.getGameTime() < sensorHoldUntil;
        };
    }

    /** Pouring, or about to: the room is worth keeping up. */
    private boolean isPouringSoon(Level level) {
        return isEnabled() && wantsToPour(level);
    }

    /** One tick of pouring - only ever called on a tick the grid paid for. */
    public void pourOne() {
        if (chamber <= 0) {
            return;
        }

        chamber = Math.max(0, chamber - POUR_PER_TICK);

        if (chamber == 0) {
            vapor = null;
        }

        setChanged();
    }

    private boolean isWorkingState() {
        var state = getBlockState();

        return state.hasProperty(ElectricMachineBlock.STATE)
            && state.getValue(ElectricMachineBlock.STATE) == com.human.common.gameplay.machine.MachineState.WORKING;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sensor and effects
    // ------------------------------------------------------------------------------------------------------------

    /** [stated] "a hostile mob or xenomorph is inside its room" - inside the ROOM, not just inside the cube. */
    private boolean senseHostile(Level level, BlockPos blockPos) {
        if (!room.hasResult()) {
            return false;
        }

        var found = level.getEntitiesOfClass(
            LivingEntity.class,
            area(blockPos),
            entity -> entity.isAlive() && isHostile(entity) && room.contains(entity.blockPosition())
        );

        return !found.isEmpty();
    }

    /** Monsters (anything vanilla counts as an enemy or files under MONSTER) and every avp_alien xenomorph. */
    public static boolean isHostile(LivingEntity entity) {
        if (entity instanceof Player) {
            return false;
        }

        var type = entity.getType();

        return entity instanceof Enemy || type.getCategory() == MobCategory.MONSTER || type.is(ALIENS);
    }

    /**
     * Hands everything standing in the fog to its vapor's effect ({@link VaporEffects}), with the fog's strength there.
     */
    private void applyEffects(ServerLevel level, BlockPos blockPos, VaporKind kind) {
        if (!room.hasResult()) {
            return;
        }

        for (var entity : level.getEntitiesOfClass(LivingEntity.class, area(blockPos), LivingEntity::isAlive)) {
            var strength = room.strengthAt(entity.blockPosition());

            if (strength > 0.0F) {
                VaporEffects.apply(level, kind, entity, strength, null, VaporEffects.Origin.DISTRIBUTOR, EFFECT_INTERVAL_TICKS);
            }
        }
    }

    private static AABB area(BlockPos blockPos) {
        return new AABB(blockPos).inflate(DistributorFog.RADIUS);
    }

    /**
     * The fog's strength at a world position right now - 0 when not pouring. For anything else that wants to know
     * (MUTHUR, sensors).
     */
    public float fogStrengthAt(BlockPos pos) {
        return isWorkingState() && vapor != null ? room.strengthAt(pos) : 0.0F;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------------------------------------------------

    public DistributorMode getMode() {
        return mode;
    }

    public void setMode(DistributorMode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            setChanged();
        }
    }

    public DistributorRemote getRemote() {
        return remote;
    }

    /** MUTHUR's command - see {@link DistributorRemote}. */
    public void setRemote(DistributorRemote remote) {
        if (this.remote != remote) {
            this.remote = remote;
            setChanged();
        }
    }

    public int getChamber() {
        return chamber;
    }

    public @Nullable VaporKind getVapor() {
        return vapor;
    }

    @Override
    protected int extraData(int index) {
        return switch (index) {
            case EXTRA_CHAMBER -> chamber;
            case EXTRA_VAPOR -> vapor == null ? -1 : vapor.ordinal();
            case EXTRA_MODE -> mode.ordinal();
            case EXTRA_REMOTE -> remote.ordinal();
            case EXTRA_SENSING -> level != null && level.getGameTime() < sensorHoldUntil ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    public boolean handleMenuButton(Player player, int id) {
        if (id >= BUTTON_SET_MODE && id < BUTTON_SET_MODE + DistributorMode.values().length) {
            setMode(DistributorMode.byOrdinal(id - BUTTON_SET_MODE));
            return true;
        }

        return false;
    }

    private static Direction front(BlockState state) {
        return state.hasProperty(ElectricMachineBlock.FACING) ? state.getValue(ElectricMachineBlock.FACING) : Direction.NORTH;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Clients - only the vapor, for the fog colour. The working light comes with the block state.
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        var tag = new CompoundTag();

        tag.putBoolean(NBT_CLIENT, true);

        if (vapor != null) {
            tag.putString(NBT_VAPOR, vapor.id());
        }

        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Save
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(NBT_CHAMBER, chamber);

        if (vapor != null) {
            tag.putString(NBT_VAPOR, vapor.id());
        }

        tag.putString(NBT_MODE, mode.id());
        tag.putString(NBT_REMOTE, remote.id());
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        // The client's small update tag: the vapor alone. ⚠ Never fall through to the full load - it would wipe the
        // client copy's slots and tank with empties.
        if (tag.getBoolean(NBT_CLIENT)) {
            clientVapor = VaporKind.byId(tag.getString(NBT_VAPOR));
            return;
        }

        super.loadAdditional(tag, registries);
        vapor = VaporKind.byId(tag.getString(NBT_VAPOR));
        // ⚠ A chamber with no readable vapor (a tag from a future version) is treated as empty, never as poisoned.
        chamber = vapor == null ? 0 : Math.max(0, Math.min(CHAMBER_CAPACITY, tag.getInt(NBT_CHAMBER)));
        mode = DistributorMode.byId(tag.getString(NBT_MODE));
        remote = DistributorRemote.byId(tag.getString(NBT_REMOTE));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Block changes nearby - vanilla game events
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public @NotNull Listener getListener() {
        return listener;
    }

    /**
     * Marks the room for a rescan when a block within 16 blocks is placed, broken or changed, or a door or trapdoor
     * opens or closes. Costs one comparison per event; the rescan itself happens on the next tick that needs the room.
     * Blocks changed further out than vanilla announces (or by mods that skip the event) are caught by the periodic
     * rescan.
     */
    public final class Listener implements GameEventListener {

        private final PositionSource source;

        Listener(PositionSource source) {
            this.source = source;
        }

        @Override
        public @NotNull PositionSource getListenerSource() {
            return source;
        }

        @Override
        public int getListenerRadius() {
            return LISTENER_RADIUS;
        }

        @Override
        public boolean handleGameEvent(
            @NotNull ServerLevel level,
            @NotNull Holder<GameEvent> event,
            GameEvent.@NotNull Context context,
            @NotNull Vec3 position
        ) {
            if (
                event.is(GameEvent.BLOCK_PLACE.key()) || event.is(GameEvent.BLOCK_DESTROY.key())
                    || event.is(GameEvent.BLOCK_CHANGE.key()) || event.is(GameEvent.BLOCK_OPEN.key())
                    || event.is(GameEvent.BLOCK_CLOSE.key())
            ) {
                roomDirty = true;
            }

            return false;
        }
    }
}
