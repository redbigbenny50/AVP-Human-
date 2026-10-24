package com.human.common.gameplay.quarry;

import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.item.pipe.PipeItemAccess;
import com.human.common.gameplay.machine.MachineUpgrades;
import com.human.common.gameplay.power.upgrade.UpgradeCard;
import com.human.common.registry.init.HumanQuarry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The quarry terminal's brain. It sits on the BACK_TOP block - the one whose top face is the POWER port ([stated] Oct
 * 13: power top middle), because the power grid only talks to a block entity on the block the cable touches. The
 * renderer still draws the model from the CORE block.
 * <h2>PHASE 2A</h2> Power, the ITEM port, the 128 internal slots, the 9 frame slots, the 4 upgrade slots, the settings
 * and the screen. The work area, frame building, the liquid port and tank, and mining follow in the next phases.
 * <h2>POWER</h2> [stated] Sep 22 / Sep 29: machines store no power and draw ONLY while in use - a terminal draws only
 * while someone has its screen open or while it runs. So the screen is dark when nobody is using it, the normal display
 * while it is in use and powered, and the moving columns while it works ([stated] Oct 13).
 * <h2>ITEMS</h2> [stated] Oct 15: mined items go to connected storage first; the 128 internal slots are only a buffer.
 * The item port GIVES from the internal slots, so a pipe on it carries them away at the pipe's rate; a pipe can also
 * feed FRAME BLOCKS in, which land in the frame slots. When connected and internal storage are both full the quarry
 * stops until room is made.
 * <h2>DATA SAFETY</h2> Every change calls setChanged(); unloading touches nothing; breaking the terminal drops every
 * stack it holds at its feet, so nothing is ever lost.
 */
public class QuarryTerminalBlockEntity extends PowerConsumerBlockEntity {

    public static final int STORAGE_SLOTS = 128;

    public static final int FRAME_SLOTS = 9;

    public static final int UPGRADE_SLOTS = UpgradeCard.SLOTS;

    /** Draw while only the screen is open - a terminal in use ([stated] Sep 22 terminal rule). */
    public static final long SCREEN_POWER = 20L;

    /** How often the screen state is re-checked against the last grid delivery, in ticks. */
    private static final int SCREEN_CHECK_TICKS = 10;

    /** [stated] Oct 13: blank with no power, the normal display when powered, the columns moving while it works. */
    public enum Screen {

        OFF("off"),
        ON("on"),
        WORKING("working");

        private final String key;

        Screen(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    /** What the PROGRESS tab's status line says. */
    public enum Status {
        STOPPED,
        NO_POWER,
        IDLE,
        MINING,
        REFILLING,
        STORAGE_FULL,
        NO_AREA,
        BUILDING_FRAME,
        NEED_FRAMES,
        FRAME_BLOCKED,
        WAITING,
        RETRACTING,
        FRAME_SLOTS_FULL,
        READY,
        FINISHED,
        REDSTONE,
        TANK_FULL,
        FILLED,
        NO_FILL
    }

    /** Where the frame is up to. */
    public enum FrameState {
        NONE,
        BUILDING,
        BUILT,
        RETRACTING
    }

    /** What the WORK AREA map shows in each cell. */
    public static final int CELL_AVAILABLE = 0;

    public static final int CELL_SELECTED = 1;

    public static final int CELL_OUT_OF_RANGE = 2;

    public static final int CELL_CLAIMED = 3;

    public static final int CELL_XENO = 4;

    public static final int CELL_NEAR_QUARRY = 5;

    public static final int CELL_OTHER_QUARRY = 6;

    public static final int CELL_TERMINAL = 7;

    public static final int CELL_BEHIND = 8;

    public static final int MAP_SIZE = 7;

    /** Selection problems, for the WORK AREA status line. */
    public static final String[] PROBLEMS = { "ready", "nothing", "rectangle", "too_big", "front", "frame_claimed" };

    /** Draw while building or taking down the frame. */
    /** Placing or taking down the frame ([stated] Oct 15: "make it 5 rf a tick"). */
    public static final long FRAME_POWER = 5L;

    /** One frame position every this many ticks. */
    private static final int FRAME_STEP_TICKS = 2;

    /**
     * Frame blocks already in place are skipped this many at a time per step, so a rebuild over a whole frame is quick.
     */
    private static final int MAX_SKIPS_PER_STEP = 32;

    /** A frame stopped by a blocker tries again this often, and shows the blockers in red. */
    private static final int BLOCKED_RETRY_TICKS = 40;

    /** The map and frame counts refresh this often while a screen is open. */
    private static final int MAP_REFRESH_TICKS = 20;

    private final SimpleContainer storage = new SimpleContainer(STORAGE_SLOTS);

    private final SimpleContainer frames = new SimpleContainer(FRAME_SLOTS) {

        @Override
        public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
            return isFrame(stack);
        }
    };

    private final SimpleContainer upgrades = new SimpleContainer(UPGRADE_SLOTS);

    /**
     * ONE enchantment slot ([stated] Oct 15: "it should actually only have one enchantment slot for either fortune or
     * silk touch since they aren't compatible"): an enchanted book with FORTUNE at any level, or SILK TOUCH.
     */
    public static final int ENCHANT_SLOTS = 1;

    private final SimpleContainer enchants = new SimpleContainer(ENCHANT_SLOTS) {

        @Override
        public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
            return mayPlaceEnchant(stack);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            // The pickaxe the drops are rolled with is rebuilt only when a book goes in or out - never per block.
            miningTool = null;
            QuarryTerminalBlockEntity.this.setChanged();
        }
    };

    /** The pickaxe drops are rolled with, carrying the books' enchantments. Cached; rebuilt when the books change. */
    private @Nullable ItemStack miningTool;

    private final QuarrySettings settings = new QuarrySettings();

    /** The dig itself (phase 4a). */
    private final QuarryMiner miner = new QuarryMiner(this);

    /** REFILL HOLE (phase 4c). Never runs at the same time as the dig. */
    private final QuarryFiller filler = new QuarryFiller(this);

    /** The FILL WITH grid: 3 x 3, used in slot order. Plain placeable blocks only. */
    public static final int FILL_SLOTS = 9;

    private final SimpleContainer fills = new SimpleContainer(FILL_SLOTS) {

        @Override
        public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
            return QuarryFiller.isFill(stack);
        }
    };

    private boolean fillRunning;

    /** Refill MIN Y / MAX Y; MIN_VALUE = the default (the dig's bottom / the quarry's level). */
    private int refillMin = Integer.MIN_VALUE;

    private int refillMax = Integer.MIN_VALUE;

    public SimpleContainer fills() {
        return fills;
    }

    public boolean isFillRunning() {
        return fillRunning;
    }

    /** REFILL HOLE is ON in SETTINGS (choice 0). Its panel is greyed out and it cannot start while OFF. */
    public boolean refillEnabled() {
        return settings.get(QuarrySettings.Option.REFILL) == 0;
    }

    private int worldFloor() {
        return level == null ? -64 : level.getMinBuildHeight();
    }

    /** Lowest Y it fills from: down to the world's floor, defaulting to the dig's bottom. */
    public int refillMin() {
        var wanted = refillMin == Integer.MIN_VALUE ? bottomY() : refillMin;

        return Math.clamp(wanted, worldFloor(), refillMax());
    }

    /** Highest Y it fills to: never above the quarry's own level ([stated] Oct 15: max up to quarry level). */
    public int refillMax() {
        var wanted = refillMax == Integer.MIN_VALUE ? startY() : refillMax;

        return Math.clamp(wanted, worldFloor(), startY());
    }

    /** Only while not refilling: the range is fixed once a refill is under way. */
    public void moveRefillMin(int by) {
        if (!fillRunning) {
            refillMin = Math.clamp(refillMin() + by, worldFloor(), refillMax());
            setChanged();
        }
    }

    public void moveRefillMax(int by) {
        if (!fillRunning) {
            refillMax = Math.clamp(refillMax() + by, refillMin(), startY());
            setChanged();
        }
    }

    /**
     * The refill's own START / STOP ([stated] Oct 15: "mining and refilling can never run at the same time - starting
     * one stops the other"). Only while REFILL HOLE is ON. Starting a finished refill fills the area again from the
     * bottom.
     */
    public void setRefilling(boolean start) {
        if (start && (!refillEnabled() || frameState == FrameState.RETRACTING)) {
            return;
        }

        if (start) {
            running = false;

            if (filler.finished()) {
                filler.reset();
            }
        }

        if (fillRunning != start) {
            fillRunning = start;
            syncToClients();
        }
    }

    /** The refill is done: stop, and take the frame down unless WHEN FINISHED says leave it built. */
    void refillFinished() {
        fillRunning = false;

        if (settings.get(QuarrySettings.Option.WHEN_FINISHED) == 0) {
            retract();
        }

        syncToClients();
    }

    // ---- refill progress, for the GUI ----

    public int fillChunk() {
        return Math.min(filler.chunkIndex() + 1, areaChunks().size());
    }

    public int fillLayerY() {
        return filler.layerY();
    }

    public int fillLayerOfChunk() {
        return Math.clamp(filler.layerY() - refillMin() + 1, 1, Math.max(1, filler.layersPerChunk()));
    }

    public int fillLayersPerChunk() {
        return filler.layersPerChunk();
    }

    public int fillLayersLeft() {
        return filler.layersLeft(areaChunks().size());
    }

    public int fillLayersTotal() {
        return filler.layersPerChunk() * areaChunks().size();
    }

    public int blocksPlaced() {
        return filler.placed();
    }

    /** The fill slot in use (1-9), or 0 for none yet - FILLING WITH. */
    public int fillSlot() {
        return filler.lastSlot() + 1;
    }

    /** Blocks on hand across the FILL WITH grid - ON HAND. */
    public int fillOnHand() {
        var total = 0;

        for (int slot = 0; slot < FILL_SLOTS; slot++) {
            total += fills.getItem(slot).getCount();
        }

        return total;
    }

    /** Liquid tank capacity: 128 buckets ([stated] Oct 15). */
    public static final int TANK_CAPACITY = 128 * com.human.common.gameplay.fluid.FluidContent.BUCKET;

    /**
     * The internal liquid tank (phase 4b). DRAIN LIQUIDS = COLLECT fills it; pipes empty it through the LIQUID port on
     * top of the back-row counter-clockwise block ([stated] Oct 13: liquid port top right). Pipes cannot fill it.
     */
    private final QuarryTank tank = new QuarryTank();

    /** Where the item port's next offer starts looking (not saved: any start is fine). */
    private int peekCursor;

    public final class QuarryTank implements com.human.common.gameplay.fluid.FluidEndpoint {

        private com.human.common.gameplay.fluid.FluidContent content = com.human.common.gameplay.fluid.FluidContent.EMPTY;

        @Override
        public com.human.common.gameplay.fluid.FluidContent getContent() {
            return content;
        }

        @Override
        public int getCapacity() {
            return TANK_CAPACITY;
        }

        @Override
        public int insert(net.minecraft.world.level.material.Fluid fluid, int maxAmount, boolean simulate) {
            if (maxAmount <= 0 || !content.isEmpty() && !content.accepts(fluid)) {
                return 0;
            }

            var room = TANK_CAPACITY - content.amount();
            var taken = Math.min(room, maxAmount);

            if (taken > 0 && !simulate) {
                content = com.human.common.gameplay.fluid.FluidContent.of(
                    content.isEmpty() ? com.human.common.gameplay.fluid.FluidContent.sourceOf(fluid) : content.fluid(),
                    content.amount() + taken
                );
                notifyContentChanged();
            }

            return taken;
        }

        @Override
        public int extract(@Nullable net.minecraft.world.level.material.Fluid fluid, int maxAmount, boolean simulate) {
            if (content.isEmpty() || maxAmount <= 0 || fluid != null && !content.accepts(fluid)) {
                return 0;
            }

            var given = Math.min(content.amount(), maxAmount);

            if (!simulate) {
                var left = content.amount() - given;

                content = left <= 0 ? com.human.common.gameplay.fluid.FluidContent.EMPTY : content.withAmount(left);
                notifyContentChanged();
            }

            return given;
        }

        @Override
        public void setContentSilently(com.human.common.gameplay.fluid.FluidContent content) {
            this.content = content;
        }

        @Override
        public void notifyContentChanged() {
            setChanged();
        }

        /** Output only: pipes take liquid out through the port's top face and can never put any in. */
        @Override
        public boolean canFill(@Nullable net.minecraft.core.Direction side) {
            return false;
        }

        @Override
        public boolean canDrain(@Nullable net.minecraft.core.Direction side) {
            return side == net.minecraft.core.Direction.UP;
        }
    }

    public QuarryTank tank() {
        return tank;
    }

    /** Tank fill for the GUI in tenths of a bucket (0-1280) - fits a 16-bit data slot. */
    public int tankTenths() {
        return tank.getContent().amount() / 100;
    }

    /** The tank's liquid as a registry id for the GUI, or -1 when empty. */
    public int tankFluidId() {
        return tank.getContent().isEmpty() ? -1 : net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(tank.getContent().fluid());
    }

    /** Power while digging, per tick; each speed card adds a quarter more (faster digging costs more). */
    public static final long MINE_POWER = 80L;

    /** How often the redstone setting looks at its signal, in ticks. */
    private static final int REDSTONE_CHECK_TICKS = 10;

    private boolean redstoneAllows = true;

    private boolean running;

    /** The player who placed it - may mine their own claims and their factions' ([stated] Oct 5 claim rule). */
    private @Nullable java.util.UUID owner;

    /** Chosen chunks as a bit mask over the depth 1-3 x lateral -2..2 candidates; see {@link QuarryArea}. */
    private int selection = 1 << QuarryArea.bit(1, 0);

    /** The area is locked in: registered for spacing and ready for the frame. */
    private boolean confirmed;

    /** Lowest layer to mine; {@link Integer#MIN_VALUE} = the world's bottom. */
    private int bottomY = Integer.MIN_VALUE;

    private FrameState frameState = FrameState.NONE;

    private int frameCursor;

    private int blockedCount;

    private int problem = 1;

    // Not saved - rebuilt when needed.
    private final int[] map = new int[MAP_SIZE * MAP_SIZE];

    private int framesNeeded;

    private int framesInPlace;

    private @Nullable java.util.List<net.minecraft.core.BlockPos> framePositions;

    private final java.util.List<net.minecraft.core.BlockPos> blocked = new java.util.ArrayList<>();

    private Status status = Status.STOPPED;

    private Screen screen = Screen.OFF;

    /** Players with the screen open - the terminal draws power only while this is above zero or it runs. */
    private int viewers;

    /** Game time of the last tick the grid paid for; the screen goes dark a moment after it stops. */
    private long lastPoweredTick = Long.MIN_VALUE;

    public QuarryTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(HumanQuarry.QUARRY_TERMINAL_BLOCK_ENTITY.get(), pos, state);
        storage.addListener(container -> setChanged());
        frames.addListener(container -> setChanged());
        upgrades.addListener(container -> setChanged());
        // ⚠ Final-pass DATA-LOSS fix: the FILL WITH grid had no listener, so blocks put in it (by hand or by pipe)
        // never marked the terminal for saving - a chunk unload before anything else changed would lose them.
        fills.addListener(container -> setChanged());
    }

    public QuarryEdition edition() {
        return getBlockState().getBlock() instanceof QuarryTerminalBlock terminal ? terminal.edition() : QuarryEdition.BARE;
    }

    public static boolean isFrame(ItemStack stack) {
        return stack.is(HumanQuarry.QUARRY_FRAME_ITEM.get());
    }

    /** [stated] Oct 13: power comes in on the TOP of this block (the top middle of the terminal) and nowhere else. */
    @Override
    public boolean canConnectOn(net.minecraft.core.Direction direction) {
        return direction == net.minecraft.core.Direction.UP;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Power - draws only while in use
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public long getRequestedPower() {
        // ⚠ Final pass - NO DRAW WHILE STUCK ([stated] Sep 22: machines draw power ONLY WHILE RUNNING). Storage or tank
        // full, out of fill blocks, frame blocked, short of frames, a chunk unloaded: nothing is being done, so nothing
        // is asked for - only the screen, while someone has it open. Those states re-check once a second unpowered
        // (see unpoweredTick) and ask again the moment they clear.
        if (waiting()) {
            return viewers > 0 ? SCREEN_POWER : 0L;
        }

        return (framing() ? FRAME_POWER : 0L) + (digging() ? minePower() : 0L) + (viewers > 0 ? SCREEN_POWER : 0L);
    }

    /** How often a stuck quarry re-checks whether it can carry on, unpowered. */
    private static final int WAIT_RECHECK_TICKS = 20;

    /** Stuck on something outside itself - waiting, not working. */
    private boolean waiting() {
        // Only while it is meant to be doing something: a STOPPED quarry is never "waiting", so pressing STOP always
        // gets the status back to normal on the next tick.
        if (!running && !fillRunning && frameState != FrameState.RETRACTING) {
            return false;
        }

        return switch (status) {
            case STORAGE_FULL, TANK_FULL, NO_FILL, WAITING, FRAME_BLOCKED, NEED_FRAMES, FRAME_SLOTS_FULL -> true;
            default -> false;
        };
    }

    /** Running a finished frame over an unfinished area, with redstone allowing it - the only time digging draws. */
    private boolean digging() {
        return confirmed && frameState == FrameState.BUILT && redstoneAllows
            && (running && !miner.finished() || fillRunning && !filler.finished());
    }

    private long minePower() {
        return MINE_POWER + MINE_POWER * (20 - secondsPerLayer()) / 16L;
    }

    /** Building or taking down the frame right now - the only work phase 2 does, and what it draws power for. */
    private boolean framing() {
        return frameState == FrameState.RETRACTING || (running || fillRunning) && confirmed && frameState != FrameState.BUILT;
    }

    // ---- ENERGY: how much of the power the quarry asked for it actually got, over the last few seconds ----
    // The grid is all-or-nothing per tick (no buffer), so the honest readout is the share of ticks it was fed.

    private static final int ENERGY_WINDOW_TICKS = 100;

    private int energyAsked;

    private int energyGot;

    private int energyPercent = 100;

    private void countEnergy(boolean got) {
        if (getRequestedPower() <= 0L) {
            return;
        }

        energyAsked++;

        if (got) {
            energyGot++;
        }

        if (energyAsked >= ENERGY_WINDOW_TICKS) {
            energyPercent = Math.round(100.0F * energyGot / energyAsked);
            energyAsked = 0;
            energyGot = 0;
        }
    }

    public int energyPercent() {
        return energyPercent;
    }

    public int blocksMined() {
        return miner.mined();
    }

    /** Which layer of the current chunk (1 = the top) and how many there are. */
    public int layerOfChunk() {
        return Math.clamp(startY() - miner.layerY() + 1, 1, Math.max(1, miner.layersPerChunk()));
    }

    public int layersPerChunk() {
        return miner.layersPerChunk();
    }

    @Override
    public void poweredTick(Level level, BlockPos pos, BlockState state) {
        lastPoweredTick = level.getGameTime();
        countEnergy(true);
        work(level);
        afterTick(level);
    }

    /** One step of whatever the quarry is doing - retracting, refilling, building its frame or digging. */
    private void work(Level level) {
        if (frameState == FrameState.RETRACTING) {
            stepRetract(level);
        } else if (fillRunning) {
            // REFILL: needs the area and the frame (it builds the frame first, just as digging does).
            if (!confirmed) {
                status = Status.NO_AREA;
            } else if (frameState != FrameState.BUILT) {
                stepBuild(level);
            } else if (!redstoneAllows) {
                status = Status.REDSTONE;
            } else if (level instanceof net.minecraft.server.level.ServerLevel server) {
                status = filler.tick(server, areaChunks());
            }
        } else if (running && !confirmed) {
            status = Status.NO_AREA;
        } else if (running && frameState != FrameState.BUILT) {
            stepBuild(level);
        } else if (running && !redstoneAllows) {
            status = Status.REDSTONE;
        } else if (running && level instanceof net.minecraft.server.level.ServerLevel server) {
            status = miner.finished() ? Status.FINISHED : miner.tick(server, areaChunks());
        } else if (filler.finished() && !running) {
            status = Status.FILLED;
        } else if (miner.finished()) {
            status = Status.FINISHED;
        } else {
            status = frameState == FrameState.BUILT ? Status.READY : Status.STOPPED;
        }
    }

    @Override
    public void unpoweredTick(Level level, BlockPos pos, BlockState state) {
        if (waiting()) {
            // Stuck, asking for no power: look once a second whether it can carry on. The step it takes is bounded
            // like any other (a few blocks at most); when it clears, the status changes and the next tick asks for
            // power again.
            if (level.getGameTime() % WAIT_RECHECK_TICKS == 0 && (running || fillRunning || frameState == FrameState.RETRACTING)) {
                work(level);
            }

            afterTick(level);
            return;
        }

        countEnergy(false);
        if (framing() || digging()) {
            status = Status.NO_POWER;
        } else if ((running || fillRunning) && !confirmed) {
            status = Status.NO_AREA;
        } else if (!running) {
            status = frameState == FrameState.BUILT ? Status.READY : Status.STOPPED;
        }

        afterTick(level);
    }

    private void afterTick(Level level) {
        refreshScreen(level);

        // Containment field of finished chunks comes down a few blocks a tick, powered or not.
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            miner.tickClearing(server);
        }

        if (level.getGameTime() % REDSTONE_CHECK_TICKS == 0) {
            refreshRedstone(level);
        }

        if (moveSyncPending && level.getGameTime() % MOVE_SYNC_TICKS == 0) {
            moveSyncPending = false;
            syncToClients();
        }

        if (viewers > 0 && level.getGameTime() % MAP_REFRESH_TICKS == 0 && level instanceof net.minecraft.server.level.ServerLevel server) {
            refreshMap(server);
        }
    }

    /** REDSTONE setting: 0 ignore, 1 needs a signal, 2 a signal stops it. Any block of the terminal can be powered. */
    private void refreshRedstone(Level level) {
        var mode = settings.get(QuarrySettings.Option.REDSTONE);

        if (mode == 0) {
            redstoneAllows = true;
            return;
        }

        var powered = level.hasNeighborSignal(getBlockPos()) || level.hasNeighborSignal(core());

        redstoneAllows = mode == 1 ? powered : !powered;
    }

    /** Keeps the model's screen in step, telling clients only when it actually changes. */
    private void refreshScreen(Level level) {
        if (level.getGameTime() % SCREEN_CHECK_TICKS != 0) {
            return;
        }

        // [stated] Oct 15: the panel shows it HAS power even when idle. An idle quarry asks for no power (no idle
        // draw), so being fed recently is not enough: also ask the grid - from numbers its last tick already measured,
        // never a fresh poll, the same check the electric machines use for their ON light. Through the fuse panel's
        // grid if it is bound to one.
        var powered = level.getGameTime() - lastPoweredTick <= SCREEN_CHECK_TICKS || gridHasSupply(level);
        var next = !powered ? Screen.OFF : status == Status.MINING || status == Status.REFILLING ? Screen.WORKING : Screen.ON;

        if (next != screen) {
            screen = next;
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public Screen screen() {
        return screen;
    }

    private boolean gridHasSupply(Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) {
            return false;
        }

        var source = getFusePanel() != null ? getFusePanel() : getBlockPos();
        var grid = com.human.common.gameplay.power.PowerSystem.get(server).gridAt(source);

        return grid != null && grid.hasSupply();
    }

    // ---- atomizer pulse ----

    /** Block event: the atomizer just fired. */
    public static final int EVENT_ATOMIZE = 1;

    /** Client only: the game time of the last shot. */
    private long atomizeTick = Long.MIN_VALUE / 2;

    /**
     * Both sides: the server must answer true for the event to be sent on to clients; the client records the shot.
     */
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == EVENT_ATOMIZE) {
            if (level != null && level.isClientSide) {
                atomizeTick = level.getGameTime();
            }

            return true;
        }

        return super.triggerEvent(id, param);
    }

    /**
     * The emitter's flare, 0 to 1: full on each shot, fading over half a second, with a fast flicker on top so a sweep
     * reads as a working laser. Zero when it has not fired for a while.
     */
    public float emitterPulse(float partialTick) {
        if (level == null) {
            return 0.0F;
        }

        var since = level.getGameTime() - atomizeTick + partialTick;

        if (since < 0.0F || since > 10.0F) {
            return 0.0F;
        }

        var fade = 1.0F - since / 10.0F;
        var flicker = 0.75F + 0.25F * net.minecraft.util.Mth.sin((level.getGameTime() + partialTick) * 1.7F);

        return fade * flicker;
    }

    /** Tells clients the frame or area changed, so the renderer draws the right gantry. Only on real changes. */
    private void syncToClients() {
        setChanged();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public Status status() {
        return status;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        if (running && frameState == FrameState.RETRACTING) {
            return;
        }

        if (running) {
            // Starting the dig stops the refill.
            fillRunning = false;
        }

        if (running && miner.finished()) {
            // START on a finished area digs it again from the top - anything that fell back in, or a deeper bottom.
            miner.reset();
            syncToClients();
        }

        if (this.running != running) {
            this.running = running;
            setChanged();
        }
    }

    public void openedBy() {
        viewers++;

        // Fill the map at once, so the first click never acts on a blank map.
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            refreshMap(server);
        }
    }

    public void closedBy() {
        viewers = Math.max(0, viewers - 1);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Work area
    // ------------------------------------------------------------------------------------------------------------

    public void setOwner(@Nullable java.util.UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public net.minecraft.core.Direction facing() {
        return getBlockState().hasProperty(QuarryTerminalBlock.FACING)
            ? getBlockState().getValue(QuarryTerminalBlock.FACING)
            : net.minecraft.core.Direction.NORTH;
    }

    /** The front-bottom block - the quarry's level and the reference for the work area. */
    public BlockPos core() {
        return QuarryTerminalBlock.BRAIN.toCore(getBlockPos(), facing());
    }

    public int startY() {
        return core().getY();
    }

    public int bottomY() {
        var minimum = level != null ? level.getMinBuildHeight() : -64;

        return bottomY == Integer.MIN_VALUE ? minimum : Math.clamp(bottomY, minimum, startY());
    }

    public void moveBottom(int by) {
        if (frameState != FrameState.NONE) {
            return;
        }

        var minimum = level != null ? level.getMinBuildHeight() : -64;

        bottomY = Math.clamp(bottomY() + by, minimum, startY());
        setChanged();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public FrameState frameState() {
        return frameState;
    }

    public int selection() {
        return selection;
    }

    public int problem() {
        return problem;
    }

    public int blockedCount() {
        return blockedCount;
    }

    public int framesNeeded() {
        return framesNeeded;
    }

    public int framesInPlace() {
        return framesInPlace;
    }

    public int mapCell(int index) {
        return map[index];
    }

    /** The chunks of the area (client and server - the selection is in the update tag). */
    public java.util.List<net.minecraft.world.level.ChunkPos> areaChunksForRender() {
        return areaChunks();
    }

    /**
     * Where the drill head stands, in world coordinates of its axis (x, z) - over the chunk it is working, its bit
     * centred in the chunk ([stated] Oct 5). Until mining (phase 4) moves it, it parks over the chunk in front of the
     * terminal.
     */
    public net.minecraft.world.phys.Vec3 headPosition() {
        var chunks = areaChunks();
        var chunk = chunks.isEmpty()
            ? QuarryArea.chunkAt(core(), facing(), 1, 0)
            : chunks.get(Math.clamp(fillRunning ? filler.chunkIndex() : miner.chunkIndex(), 0, chunks.size() - 1));

        return new net.minecraft.world.phys.Vec3(chunk.getMinBlockX() + 8.0D, startY() + 2.0D, chunk.getMinBlockZ() + 8.0D);
    }

    /** How far the cutter should be below the parked head, in blocks: down to the layer being dug, or 0 when parked. */
    public double cutterDepth() {
        if (frameState != FrameState.BUILT) {
            return 0.0D;
        }

        // Refilling: the bit rides just above the layer it is filling.
        if (fillRunning) {
            return filler.finished() ? 0.0D : Math.max(0.0D, startY() + 1.0D - filler.layerY());
        }

        if (miner.finished()) {
            return 0.0D;
        }

        // Oct 16 fix: the bit rides on TOP of the layer it is cutting (bottom at layerY + 1), one block higher than
        // before - it sat inside the layer, so the layer's uncut blocks stuck up above the bit. Matches the refill.
        return Math.max(0.0D, startY() + 1.0D - miner.layerY());
    }

    // ---- client-side smoothing, so the head glides with no per-tick packets ----

    private double shownX = Double.NaN;

    private double shownZ;

    private double shownDepth;

    private double shownAt = Double.NaN;

    /**
     * Moves the shown head toward where it should be, at the same speeds the server waits for: the cutter rises (or
     * sinks) first, and only once it is up does the head travel. Called by the renderer once per frame, client only.
     */
    public void animate(float partialTick) {
        if (level == null) {
            return;
        }

        var now = level.getGameTime() + partialTick;
        var target = headPosition();
        var depth = cutterDepth();

        if (Double.isNaN(shownX) || Double.isNaN(shownAt)) {
            shownX = target.x;
            shownZ = target.z;
            shownDepth = depth;
            shownAt = now;
            return;
        }

        var ticks = Math.clamp(now - shownAt, 0.0D, 10.0D);

        shownAt = now;

        var travelling = Math.abs(target.x - shownX) > 1.0E-3D || Math.abs(target.z - shownZ) > 1.0E-3D;

        if (travelling && shownDepth > 1.0E-3D) {
            shownDepth = approach(shownDepth, 0.0D, QuarryMiner.RISE_PER_TICK * ticks);
        } else if (travelling) {
            var step = QuarryMiner.TRAVEL_PER_TICK * ticks;

            shownX = approach(shownX, target.x, step);
            shownZ = approach(shownZ, target.z, step);
        } else {
            // While digging the cutter eases down through the layer instead of jumping a block at a time.
            // ...but drops quickly through skipped empty layers, so it never trails far behind the dig.
            var behind = depth - shownDepth;
            var rate = behind > 1.5D
                ? QuarryMiner.RISE_PER_TICK
                : depth > shownDepth ? QuarryMiner.RISE_PER_TICK * 0.1D : QuarryMiner.RISE_PER_TICK;

            shownDepth = approach(shownDepth, depth, rate * ticks);
        }
    }

    private static double approach(double from, double to, double step) {
        return from < to ? Math.min(to, from + step) : Math.max(to, from - step);
    }

    public net.minecraft.world.phys.Vec3 shownHead() {
        return Double.isNaN(shownX) ? headPosition() : new net.minecraft.world.phys.Vec3(shownX, startY() + 2.0D, shownZ);
    }

    private long effectTick = Long.MIN_VALUE;

    /** True once per game tick - the renderer runs every frame, effects only want one go a tick. */
    public boolean claimEffectTick(long gameTime) {
        if (gameTime == effectTick) {
            return false;
        }

        effectTick = gameTime;
        return true;
    }

    public double shownDepth() {
        return Double.isNaN(shownX) ? cutterDepth() : shownDepth;
    }

    /** The cutter's spin in radians - turning only while the screen says WORKING. One turn every three seconds. */
    public float cutterSpin(float partialTick) {
        if (screen != Screen.WORKING || level == null) {
            return 0.0F;
        }

        return (float) Math.toRadians(((level.getGameTime() % 60L) + partialTick) * 6.0D);
    }

    // ---- progress, for the GUI ----

    public int miningChunk() {
        return Math.min(miner.chunkIndex() + 1, areaChunks().size());
    }

    public int miningLayerY() {
        return miner.layerY();
    }

    public int layersLeft() {
        return miner.layersLeft(areaChunks().size());
    }

    public int layersTotal() {
        return miner.layersPerChunk() * areaChunks().size();
    }

    // ---- hooks the miner calls ----

    @Nullable
    java.util.UUID ownerId() {
        return owner;
    }

    void contentsChanged() {
        setChanged();
    }

    /**
     * A new layer or chunk. ⚠ Empty layers now pass in a tick, so several can go by at once - the update is only
     * flagged here and sent at most once every {@link #MOVE_SYNC_TICKS} ticks, never once per layer.
     */
    void miningMoved() {
        moveSyncPending = true;
        setChanged();
    }

    private static final int MOVE_SYNC_TICKS = 10;

    private boolean moveSyncPending;

    /** The last chunk is done: stop, and take the frame down unless WHEN FINISHED says leave it built. */
    void miningFinished() {
        running = false;

        if (settings.get(QuarrySettings.Option.WHEN_FINISHED) == 0) {
            retract();
        }

        syncToClients();
    }

    /**
     * The area's chunks. ⚠ Final-pass cost fix: this was rebuilt from the selection on every call - every server tick,
     * about ten times a tick from the GUI's readouts while it is open, and several times a frame from the renderer. It
     * is now built once and reused until the selection (or the block's facing) changes.
     */
    private java.util.List<net.minecraft.world.level.ChunkPos> areaChunks() {
        var facing = facing();

        if (cachedChunks == null || cachedSelection != selection || cachedFacing != facing) {
            cachedChunks = java.util.List.copyOf(QuarryArea.chunks(core(), facing, selection));
            cachedSelection = selection;
            cachedFacing = facing;
        }

        return cachedChunks;
    }

    private java.util.@Nullable List<net.minecraft.world.level.ChunkPos> cachedChunks;

    private int cachedSelection;

    private net.minecraft.core.@Nullable Direction cachedFacing;

    private java.util.List<BlockPos> framePositions() {
        if (framePositions == null) {
            framePositions = QuarryArea.framePositions(areaChunks(), startY());
        }

        return framePositions;
    }

    /** A map cell clicked: add or take away that chunk. Only while nothing is built. */
    public void toggleCell(int row, int column) {
        if (frameState != FrameState.NONE || confirmed || row < 0 || row >= MAP_SIZE - 1 || column < 0 || column >= MAP_SIZE) {
            return;
        }

        var depth = MAP_SIZE - 1 - row;
        var lateral = column - MAP_SIZE / 2;
        var code = map[row * MAP_SIZE + column];

        if (!QuarryArea.isCandidate(depth, lateral) || code != CELL_AVAILABLE && code != CELL_SELECTED) {
            return;
        }

        selection ^= 1 << QuarryArea.bit(depth, lateral);
        framePositions = null;
        problem = PROBLEM_INDEX.get(QuarryArea.shape(selection).problem());
        setChanged();

        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            refreshMap(server);
        }
    }

    private static final java.util.Map<String, Integer> PROBLEM_INDEX = java.util.Map.of(
        "ready",
        0,
        "nothing",
        1,
        "rectangle",
        2,
        "too_big",
        3,
        "front",
        4,
        "frame_claimed",
        5
    );

    /** CONFIRM AREA: lock the selection in, if it is a valid shape and every chunk it and its frame touch is free. */
    public void confirm() {
        if (confirmed || !(level instanceof net.minecraft.server.level.ServerLevel server)) {
            return;
        }

        var shape = QuarryArea.shape(selection);

        problem = PROBLEM_INDEX.get(shape.problem());

        if (!shape.valid()) {
            return;
        }

        var registry = QuarryRegistry.get(server);

        for (var chunk : areaChunks()) {
            if (
                QuarryClaims.check(server, chunk, owner, startY()) != QuarryClaims.Verdict.FREE || registry.nearby(
                    getBlockPos(),
                    chunk
                ) != QuarryRegistry.Nearby.NONE
            ) {
                problem = PROBLEM_INDEX.get("frame_claimed");
                return;
            }
        }

        for (var chunk : QuarryArea.frameChunks(areaChunks())) {
            if (
                QuarryClaims.check(server, chunk, owner, startY()) != QuarryClaims.Verdict.FREE || registry.nearby(
                    getBlockPos(),
                    chunk
                ) == QuarryRegistry.Nearby.INSIDE
            ) {
                problem = PROBLEM_INDEX.get("frame_claimed");
                return;
            }
        }

        confirmed = true;
        framePositions = null;
        registry.put(getBlockPos(), areaChunks());
        miner.reset();
        syncToClients();
        refreshMap(server);
    }

    /** EDIT AREA: unlock the selection again - only while no frame stands. */
    public void unconfirm() {
        if (!confirmed || frameState != FrameState.NONE) {
            return;
        }

        confirmed = false;
        running = false;

        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            QuarryRegistry.get(server).remove(getBlockPos());
            refreshMap(server);
        }

        setChanged();
    }

    /** RETRACT FRAMES: take the frame down into the frame slots. */
    public void retract() {
        if (frameState == FrameState.NONE) {
            return;
        }

        running = false;
        // A retract ends a refill too - otherwise the frame would be rebuilt the moment it came down.
        fillRunning = false;
        frameState = FrameState.RETRACTING;
        frameCursor = framePositions().size() - 1;
        // The drill leaves: whatever containment it was holding comes down behind it.
        miner.releaseFields();
        syncToClients();
    }

    /**
     * Called when the terminal is broken: its area no longer reserves the chunks around it. The frame stays standing.
     */
    public void forget(Level level) {
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            QuarryRegistry.get(server).remove(getBlockPos());
            // Nothing would ever take this quarry's containment field down once the terminal is gone.
            miner.dropAllFields(server);
        }
    }

    private void refreshMap(net.minecraft.server.level.ServerLevel server) {
        var core = core();
        var facing = facing();
        var registry = QuarryRegistry.get(server);

        for (int row = 0; row < MAP_SIZE; row++) {
            for (int column = 0; column < MAP_SIZE; column++) {
                var index = row * MAP_SIZE + column;

                if (row == MAP_SIZE - 1) {
                    map[index] = column == MAP_SIZE / 2 ? CELL_TERMINAL : CELL_BEHIND;
                    continue;
                }

                var depth = MAP_SIZE - 1 - row;
                var lateral = column - MAP_SIZE / 2;
                var chunk = QuarryArea.chunkAt(core, facing, depth, lateral);
                var verdict = QuarryClaims.check(server, chunk, owner, core.getY());
                var nearby = registry.nearby(getBlockPos(), chunk);

                if (verdict == QuarryClaims.Verdict.XENO) {
                    map[index] = CELL_XENO;
                } else if (verdict == QuarryClaims.Verdict.CLAIMED) {
                    map[index] = CELL_CLAIMED;
                } else if (nearby == QuarryRegistry.Nearby.INSIDE) {
                    map[index] = CELL_OTHER_QUARRY;
                } else if (nearby == QuarryRegistry.Nearby.NEAR) {
                    map[index] = CELL_NEAR_QUARRY;
                } else if (!QuarryArea.isCandidate(depth, lateral)) {
                    map[index] = CELL_OUT_OF_RANGE;
                } else if ((selection & (1 << QuarryArea.bit(depth, lateral))) != 0) {
                    map[index] = CELL_SELECTED;
                } else {
                    // Oct 17 - only a chunk that can still be ADDED shows as available: one that would make the area
                    // wider than 3 (or push the front chunk out of it) is drawn dim, like one out of reach, and a click
                    // on it does nothing (toggleCell only takes AVAILABLE and SELECTED cells).
                    map[index] = QuarryArea.canAdd(selection, depth, lateral) ? CELL_AVAILABLE : CELL_OUT_OF_RANGE;
                }
            }
        }

        // A chunk that has since become blocked drops out of an unconfirmed selection.
        if (!confirmed) {
            for (int depth = 1; depth <= QuarryArea.MAX_DEPTH; depth++) {
                for (int lateral = -QuarryArea.MAX_LATERAL; lateral <= QuarryArea.MAX_LATERAL; lateral++) {
                    var code = map[(MAP_SIZE - 1 - depth) * MAP_SIZE + lateral + MAP_SIZE / 2];

                    if (code != CELL_SELECTED && code != CELL_AVAILABLE) {
                        selection &= ~(1 << QuarryArea.bit(depth, lateral));
                    }
                }
            }
        }

        problem = confirmed ? 0 : PROBLEM_INDEX.get(QuarryArea.shape(selection).problem());

        // Frame counts - only loaded chunks are read; an unloaded spot counts as still needed.
        var inPlace = 0;
        var positions = framePositions();

        for (var spot : positions) {
            if (server.isLoaded(spot) && server.getBlockState(spot).is(HumanQuarry.QUARRY_FRAME.get())) {
                inPlace++;
            }
        }

        framesInPlace = inPlace;
        framesNeeded = positions.size() - inPlace;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Building and taking down the frame
    // ------------------------------------------------------------------------------------------------------------

    /**
     * One step of the frame build. [stated] Oct 5 / Oct 15: frame blocks already in place are kept (reattaching a
     * quarry places no doubles); with the ATOMIZER on, whatever stands where the frame goes is deleted, not dropped,
     * except blocks that hold anything or carry anything (anything with a block entity - storage, machines, cables,
     * pipes - and unbreakable blocks), which stop the build and are shown in red.
     */
    private void stepBuild(Level level) {
        if (frameState == FrameState.NONE) {
            frameState = FrameState.BUILDING;
            frameCursor = 0;
            blocked.clear();
            setChanged();
        }

        var positions = framePositions();

        if (frameCursor >= positions.size()) {
            if (blocked.isEmpty()) {
                frameState = FrameState.BUILT;
                status = Status.READY;
                syncToClients();
                level.playSound(
                    null,
                    getBlockPos(),
                    net.minecraft.sounds.SoundEvents.ANVIL_USE,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    0.6F,
                    1.4F
                );
                setChanged();
                return;
            }

            status = Status.FRAME_BLOCKED;

            if (level.getGameTime() % BLOCKED_RETRY_TICKS == 0) {
                showBlocked(level);
                blocked.clear();
                frameCursor = 0;
            }

            return;
        }

        if (level.getGameTime() % FRAME_STEP_TICKS != 0) {
            // ⚠ Oct 16 fix: only say BUILDING if it is not stuck. Setting it on every in-between tick made a stuck
            // build flash NEED FRAMES / BUILDING FRAME every tick (it only re-checks on step ticks).
            if (status != Status.NEED_FRAMES && status != Status.WAITING) {
                status = Status.BUILDING_FRAME;
            }

            return;
        }

        var atomize = settings.get(QuarrySettings.Option.ATOMIZER) == 0;

        for (int skips = 0; skips < MAX_SKIPS_PER_STEP && frameCursor < positions.size(); skips++) {
            var spot = positions.get(frameCursor);

            // Never load a chunk to build into it.
            if (!level.isLoaded(spot)) {
                status = Status.WAITING;
                return;
            }

            var state = level.getBlockState(spot);

            if (state.is(HumanQuarry.QUARRY_FRAME.get())) {
                frameCursor++;
                continue;
            }

            var free = state.isAir() || state.canBeReplaced();

            // Checked again at the block itself: a claim made after the area was confirmed still stops the build.
            var allowed = !(level instanceof net.minecraft.server.level.ServerLevel server)
                || QuarryProtection.check(server, spot, owner);

            if (!allowed || !free && (!atomize || isProtected(level, spot, state))) {
                blocked.add(spot);
                blockedCount = blocked.size();
                frameCursor++;
                continue;
            }

            var frame = takeFrame();

            if (frame.isEmpty()) {
                status = Status.NEED_FRAMES;
                return;
            }

            if (!free && level instanceof net.minecraft.server.level.ServerLevel server) {
                // Atomized: gone, nothing dropped - with the laser from the emitter and a burst where it hit, and a
                // pulse of the emitter itself (one tiny block event, sent only to players nearby).
                atomizerLaser(server, spot);
                level.blockEvent(getBlockPos(), getBlockState().getBlock(), EVENT_ATOMIZE, 0);
            }

            level.setBlock(spot, HumanQuarry.QUARRY_FRAME.get().defaultBlockState(), Block.UPDATE_ALL);
            frameCursor++;
            status = Status.BUILDING_FRAME;
            setChanged();
            return;
        }

        status = Status.BUILDING_FRAME;
    }

    /** Laser colour - the emitter lens's cyan. */
    private static final net.minecraft.core.particles.DustParticleOptions LASER = new net.minecraft.core.particles.DustParticleOptions(
        new org.joml.Vector3f(0.3F, 0.9F, 1.0F),
        0.8F
    );

    /** Spacing of the laser's dots, in blocks, and the most dots one shot draws. */
    private static final double LASER_STEP = 0.4D;

    private static final int LASER_MAX_DOTS = 96;

    /**
     * The atomizer sweep ([stated] Oct 5: "shown with a sweeping laser from the machine"; Oct 15: the lasers come out
     * of the BACK, the blue emitter, which faces the chunk). A line of cyan dots from the emitter to the block, a burst
     * of sparks where it lands and a short zap. Server-sent particles, so every nearby player sees the same sweep; one
     * shot per atomized block, at most one every {@link #FRAME_STEP_TICKS} ticks, so its cost is bounded.
     */
    private void atomizerLaser(net.minecraft.server.level.ServerLevel server, BlockPos target) {
        var back = facing().getOpposite();
        var from = getBlockPos().getCenter().add(back.getStepX() * 0.55D, 0.1D, back.getStepZ() * 0.55D);
        var to = target.getCenter();
        var path = to.subtract(from);
        var dots = Math.min(LASER_MAX_DOTS, (int) Math.ceil(path.length() / LASER_STEP));

        for (int dot = 0; dot <= dots; dot++) {
            var at = from.add(path.scale(dot / (double) Math.max(1, dots)));

            server.sendParticles(LASER, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 8, 0.3D, 0.3D, 0.3D, 0.05D);
        server.playSound(
            null,
            target,
            net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,
            net.minecraft.sounds.SoundSource.BLOCKS,
            0.25F,
            1.9F
        );
    }

    /**
     * Oct 17 - the canopy pass (QuarryMiner): one laser shot at {@code target}, with the emitter's pulse, exactly as
     * the frame build's atomizer fires.
     */
    void fireLaser(net.minecraft.server.level.ServerLevel server, BlockPos target) {
        atomizerLaser(server, target);
        server.blockEvent(getBlockPos(), getBlockState().getBlock(), EVENT_ATOMIZE, 0);
    }

    /**
     * Never atomized: anything with a block entity (storage, machines, cables, pipes), unbreakable blocks, terminals.
     */
    static boolean isProtected(Level level, BlockPos pos, BlockState state) {
        return state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0 || state.getBlock() instanceof QuarryTerminalBlock;
    }

    private ItemStack takeFrame() {
        for (int slot = 0; slot < FRAME_SLOTS; slot++) {
            if (!frames.getItem(slot).isEmpty()) {
                return frames.removeItem(slot, 1);
            }
        }

        return ItemStack.EMPTY;
    }

    private static final int MAX_BLOCKERS_SHOWN = 32;

    /** Red dust over each blocker, so the player can find it ([stated] Oct 5: "a clear RED indicator"). */
    private void showBlocked(Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) {
            return;
        }

        var red = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0F, 0.1F, 0.1F), 1.5F);

        // Capped: a frame line through a storage room could have dozens of blockers, and each marker is a packet.
        var shown = 0;

        for (var spot : blocked) {
            if (shown++ >= MAX_BLOCKERS_SHOWN) {
                break;
            }

            server.sendParticles(red, spot.getX() + 0.5, spot.getY() + 0.5, spot.getZ() + 0.5, 12, 0.35, 0.35, 0.35, 0.0);
        }
    }

    /** One step of taking the frame down, last-built first, into the frame slots. Only frame blocks are touched. */
    private void stepRetract(Level level) {
        var positions = framePositions();

        if (frameCursor < 0) {
            frameState = FrameState.NONE;
            status = Status.STOPPED;
            blocked.clear();
            blockedCount = 0;
            syncToClients();
            return;
        }

        // ⚠ Oct 16 fix, same flash as the build: a stuck retract keeps its FRAME SLOTS FULL / WAITING between steps.
        if (status != Status.FRAME_SLOTS_FULL && status != Status.WAITING) {
            status = Status.RETRACTING;
        }

        if (level.getGameTime() % FRAME_STEP_TICKS != 0) {
            return;
        }

        for (int skips = 0; skips < MAX_SKIPS_PER_STEP && frameCursor >= 0; skips++) {
            var spot = positions.get(frameCursor);

            if (!level.isLoaded(spot)) {
                status = Status.WAITING;
                return;
            }

            if (!level.getBlockState(spot).is(HumanQuarry.QUARRY_FRAME.get())) {
                frameCursor--;
                continue;
            }

            var left = frames.addItem(new ItemStack(HumanQuarry.QUARRY_FRAME_ITEM.get()));

            if (!left.isEmpty()) {
                status = Status.FRAME_SLOTS_FULL;
                return;
            }

            level.setBlock(spot, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            frameCursor--;
            setChanged();
            return;
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Contents
    // ------------------------------------------------------------------------------------------------------------

    public SimpleContainer storage() {
        return storage;
    }

    public SimpleContainer frames() {
        return frames;
    }

    public SimpleContainer upgrades() {
        return upgrades;
    }

    public SimpleContainer enchants() {
        return enchants;
    }

    /** The two enchantments the slot takes. */
    private static final java.util.List<net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>> BOOK_ENCHANTS =
        java.util.List.of(
            net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,
            net.minecraft.world.item.enchantment.Enchantments.FORTUNE
        );

    /** The level of {@code wanted} on a book, or 0. Reads the book's STORED enchantments (an enchanted book). */
    private static int bookLevel(
        ItemStack stack,
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> wanted
    ) {
        var stored = stack.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);

        if (stored == null) {
            return 0;
        }

        for (var holder : stored.keySet()) {
            if (holder.is(wanted)) {
                return stored.getLevel(holder);
            }
        }

        return 0;
    }

    /** Only an enchanted book carrying FORTUNE (any level) or SILK TOUCH. */
    public static boolean mayPlaceEnchant(ItemStack stack) {
        if (!stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) {
            return false;
        }

        for (var enchant : BOOK_ENCHANTS) {
            if (bookLevel(stack, enchant) > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * A diamond pickaxe with the book's enchantment on it. Only ONE of the two is ever applied: a book carrying both
     * (possible with commands or other mods) counts as silk touch, matching vanilla where silk touch wins.
     */
    ItemStack miningTool() {
        if (miningTool != null) {
            return miningTool;
        }

        var tool = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);

        if (level != null) {
            var registry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

            var book = enchants.getItem(0);

            for (var enchant : BOOK_ENCHANTS) {
                var levelOnBook = bookLevel(book, enchant);

                if (levelOnBook > 0) {
                    registry.get(enchant).ifPresent(holder -> tool.enchant(holder, levelOnBook));
                    break;
                }
            }
        }

        miningTool = tool;

        return tool;
    }

    public QuarrySettings settings() {
        return settings;
    }

    public void settingsChanged() {
        // REFILL HOLE switched OFF stops a refill in progress.
        if (!refillEnabled() && fillRunning) {
            fillRunning = false;
            syncToClients();
        }

        setChanged();
    }

    public boolean mayPlaceUpgrade(int slot, ItemStack stack) {
        return MachineUpgrades.mayPlace(upgrades, slot, stack);
    }

    /** [stated] Oct 15: each speed card takes 4 s off the 20 s per chunk layer, down to 4 s. */
    public int secondsPerLayer() {
        return Math.max(4, 20 - 4 * MachineUpgrades.count(upgrades, UpgradeCard.SPEED));
    }

    public int usedStorageSlots() {
        var used = 0;

        for (int slot = 0; slot < STORAGE_SLOTS; slot++) {
            if (!storage.getItem(slot).isEmpty()) {
                used++;
            }
        }

        return used;
    }

    public int frameCount() {
        var count = 0;

        for (int slot = 0; slot < FRAME_SLOTS; slot++) {
            count += frames.getItem(slot).getCount();
        }

        return count;
    }

    /** Drops everything the terminal holds - called when the terminal is broken. */
    public void dropContents(Level level, BlockPos at) {
        Containers.dropContents(level, at, storage);
        Containers.dropContents(level, at, frames);
        Containers.dropContents(level, at, upgrades);
        Containers.dropContents(level, at, enchants);
        Containers.dropContents(level, at, fills);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Item port - gives from the internal slots, takes frame blocks into the frame slots
    // ------------------------------------------------------------------------------------------------------------

    private final PipeItemAccess.Port pipePort = new PipeItemAccess.Port() {

        @Override
        public boolean present() {
            return !isRemoved();
        }

        @Override
        public boolean canInsert() {
            return true;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public @Nullable PipeItemAccess.Offer peek(int max, java.util.function.Predicate<ItemStack> allowed) {
            // ⚠ Oct 16 - offers take turns round the slots instead of always starting at slot 0. A pipe network offers
            // ONE stack per pull; if the first stack is something no destination will take (a filtered crate end, a
            // crate with no room for that item), always offering it first blocked every other item behind it while
            // the quarry kept digging - and the 128 slots filled. Now a refused item only costs its own turn.
            for (int step = 0; step < STORAGE_SLOTS; step++) {
                var slot = (peekCursor + step) % STORAGE_SLOTS;
                var held = storage.getItem(slot);

                if (held.isEmpty() || !allowed.test(held)) {
                    continue;
                }

                var index = slot;
                var offered = held.copyWithCount(Math.min(max, held.getCount()));

                // Next pull starts one slot on - the same stack again only after every other one has had a turn.
                peekCursor = (slot + 1) % STORAGE_SLOTS;

                return new PipeItemAccess.Offer() {

                    @Override
                    public ItemStack stack() {
                        return offered;
                    }

                    @Override
                    public int take(int count) {
                        var current = storage.getItem(index);

                        if (!ItemStack.isSameItemSameComponents(current, offered)) {
                            return 0;
                        }

                        var took = Math.min(count, current.getCount());

                        storage.removeItem(index, took);

                        return took;
                    }
                };
            }

            return null;
        }

        /**
         * Frame blocks to the frame slots; plain blocks to the FILL WITH grid, so a refill can be fed by pipe - but
         * ONLY while REFILL HOLE is ON (final pass: otherwise a pipe network would quietly fill the grid with up to 576
         * of any block it carries, for a refill nobody asked for). The pipe system never routes the quarry's own output
         * back into it (it skips moving items into the same storage), so mined stone cannot loop into the grid.
         */
        @Override
        public int insert(ItemStack stack) {
            var target = isFrame(stack) ? frames : refillEnabled() && QuarryFiller.isFill(stack) ? fills : null;

            if (target == null) {
                return 0;
            }

            var left = target.addItem(stack.copy());

            return stack.getCount() - left.getCount();
        }

        /**
         * Undoes an insert (the pipe system's anti-duplication step). ⚠ Final-pass fix: this only knew the frame slots,
         * so a pipe insert into the FILL WITH grid could never be taken back - the duplication the step exists to stop.
         * Takes back from whichever grid {@link #insert} would have used, matching item AND components.
         */
        @Override
        public int retract(ItemStack like, int count) {
            var source = isFrame(like) ? frames : QuarryFiller.isFill(like) ? fills : null;

            if (source == null) {
                return 0;
            }

            var removed = 0;

            for (int slot = 0; slot < source.getContainerSize() && removed < count; slot++) {
                var held = source.getItem(slot);

                if (ItemStack.isSameItemSameComponents(held, like)) {
                    var take = Math.min(count - removed, held.getCount());

                    source.removeItem(slot, take);
                    removed += take;
                }
            }

            return removed;
        }

        @Override
        public Object storage() {
            return QuarryTerminalBlockEntity.this;
        }

        @Override
        public boolean isMachine() {
            return true;
        }

        @Override
        public void listContents(java.util.function.ObjIntConsumer<ItemStack> sink) {
            for (int slot = 0; slot < STORAGE_SLOTS; slot++) {
                var held = storage.getItem(slot);

                if (!held.isEmpty()) {
                    sink.accept(held, held.getCount());
                }
            }
        }
    };

    public PipeItemAccess.Port pipePort() {
        return pipePort;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Saving and syncing
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Storage", storage.createTag(registries));
        tag.put("Frames", frames.createTag(registries));
        tag.put("Upgrades", upgrades.createTag(registries));
        tag.put("Enchants", enchants.createTag(registries));
        tag.put("Fills", fills.createTag(registries));
        tag.putBoolean("FillRunning", fillRunning);
        tag.putInt("RefillMin", refillMin);
        tag.putInt("RefillMax", refillMax);
        tag.put("Filler", filler.save());
        tag.put("Settings", settings.save());
        tag.putBoolean("Running", running);
        tag.putInt("Selection", selection);
        tag.putBoolean("Confirmed", confirmed);
        tag.putInt("BottomY", bottomY);
        tag.putString("FrameState", frameState.name());
        tag.putInt("FrameCursor", frameCursor);

        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putString("Screen", screen.name());
        tag.put("Miner", miner.save(registries));

        // Saved the same way as the canister: the liquid's id and the millibuckets.
        if (!tank.getContent().isEmpty()) {
            tag.putString("TankFluid", net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(tank.getContent().fluid()).toString());
            tag.putInt("TankAmount", tank.getContent().amount());
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        storage.fromTag(tag.getList("Storage", net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
        frames.fromTag(tag.getList("Frames", net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
        upgrades.fromTag(tag.getList("Upgrades", net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
        enchants.fromTag(tag.getList("Enchants", net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
        fills.fromTag(tag.getList("Fills", net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
        fillRunning = tag.getBoolean("FillRunning");
        refillMin = tag.contains("RefillMin") ? tag.getInt("RefillMin") : Integer.MIN_VALUE;
        refillMax = tag.contains("RefillMax") ? tag.getInt("RefillMax") : Integer.MIN_VALUE;
        filler.load(tag.getCompound("Filler"));
        miningTool = null;
        settings.load(tag.getCompound("Settings"));
        running = tag.getBoolean("Running");
        selection = tag.contains("Selection") ? tag.getInt("Selection") : 1 << QuarryArea.bit(1, 0);
        confirmed = tag.getBoolean("Confirmed");
        bottomY = tag.contains("BottomY") ? tag.getInt("BottomY") : Integer.MIN_VALUE;
        frameCursor = tag.getInt("FrameCursor");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        framePositions = null;
        miner.load(tag.getCompound("Miner"), registries);
        // An unknown liquid (its mod removed since) loads as empty rather than failing, as the canister does.
        var tankId = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("TankFluid"));
        var tankContent = tankId == null
            ? com.human.common.gameplay.fluid.FluidContent.EMPTY
            : com.human.common.gameplay.fluid.FluidContent.of(
                net.minecraft.core.registries.BuiltInRegistries.FLUID.get(tankId),
                Math.clamp(tag.getInt("TankAmount"), 0, TANK_CAPACITY)
            );

        // FluidContent counts only the amount as "empty", so an unknown id (read back as the EMPTY fluid) is caught
        // here.
        tank.setContentSilently(
            tankContent.isEmpty() || tankContent.fluid() == net.minecraft.world.level.material.Fluids.EMPTY
                ? com.human.common.gameplay.fluid.FluidContent.EMPTY
                : tankContent
        );

        try {
            frameState = FrameState.valueOf(tag.getString("FrameState"));
        } catch (IllegalArgumentException missing) {
            frameState = FrameState.NONE;
        }

        try {
            screen = Screen.valueOf(tag.getString("Screen"));
        } catch (IllegalArgumentException missing) {
            screen = Screen.OFF;
        }
    }

    /**
     * ⚠ Oct 15 - SLIM. The power node's default update tag is the WHOLE save, which here means all 128 storage slots in
     * every update packet. Clients only need what the renderer draws: the power node's own state, the screen, the area,
     * the frame and where the head is. The GUI gets everything else through the menu.
     */
    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        var tag = new CompoundTag();

        super.saveAdditional(tag, registries);
        tag.putString("Screen", screen.name());
        tag.putInt("Selection", selection);
        tag.putBoolean("Confirmed", confirmed);
        tag.putInt("BottomY", bottomY);
        tag.putString("FrameState", frameState.name());

        var miner = new CompoundTag();

        this.miner.writeClient(miner);
        tag.put("Miner", miner);

        // The refill state too, so the head goes where the refill is working.
        tag.putBoolean("FillRunning", fillRunning);
        tag.putInt("RefillMin", refillMin);
        tag.putInt("RefillMax", refillMax);

        var filler = new CompoundTag();

        this.filler.writeClient(filler);
        tag.put("Filler", filler);

        return tag;
    }
}
