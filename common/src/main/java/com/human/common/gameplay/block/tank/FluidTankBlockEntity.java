package com.human.common.gameplay.block.tank;

import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.gameplay.fluid.FluidEndpoint;
import com.human.common.registry.init.HumanBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One block of a multiblock fluid tank.
 * <h2>CONTROLLER AND MEMBERS</h2> Every tank block has one of these. The CONTROLLER - the tank's bottom corner, lowest
 * x and z - holds the fluid, the size and the windows setting. Every other block only stores the offset to its
 * controller and forwards to it. Nothing ticks: the tank changes only when {@link FluidTankFormation} re-forms it on
 * place or break.
 * <h2>PORTS</h2> [stated] Sep 29: fluids default with gravity - the TOP takes fluid in, the BOTTOM gives it out. SIDES
 * default to input for convenience and can each be switched to output with the wiring tool; that switch is per block
 * and per face, and it survives re-forming because it lives here, not in the controller.
 */
public class FluidTankBlockEntity extends BlockEntity implements FluidEndpoint {

    private static final String NBT_OFFSET = "controllerOffset";

    private static final String NBT_WIDTH = "width";

    private static final String NBT_HEIGHT = "height";

    private static final String NBT_WINDOWS = "windows";

    private static final String NBT_FLUID = "fluid";

    private static final String NBT_AMOUNT = "amount";

    private static final String NBT_OUTPUTS = "sideOutputs";

    private static final String NBT_PIPES = "pipes";

    /** From this block to its controller. Zero on the controller itself - and on a block that is in no tank yet. */
    private BlockPos controllerOffset = BlockPos.ZERO;

    // Controller only.
    private int width;

    private int height;

    private boolean windows = true;

    private FluidContent content = FluidContent.EMPTY;

    private int lastSignal = -1;

    /** Bit per horizontal direction (2D data value): set = that face is an OUTPUT. Clear = input, the default. */
    private byte sideOutputs;

    /**
     * Bit per direction (3D data value): a pipe is attached to that face, so a VALVE is drawn there. Recomputed only
     * when a neighbour changes or the block is placed - never per tick - and synced so the client can draw it.
     */
    private byte pipes;

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(HumanBlockEntityTypes.FLUID_TANK.get(), pos, state);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Tank membership
    // ------------------------------------------------------------------------------------------------------------

    public boolean isController() {
        return controllerOffset.equals(BlockPos.ZERO) && width > 0;
    }

    /** Whether this block belongs to a formed tank. */
    public boolean hasTank() {
        return !controllerOffset.equals(BlockPos.ZERO) || width > 0;
    }

    public BlockPos getControllerPos() {
        return worldPosition.offset(controllerOffset);
    }

    /** The controller, or null if the tank is not formed or its controller is not loaded. */
    public @Nullable FluidTankBlockEntity controller() {
        if (isController()) {
            return this;
        }

        if (level == null || controllerOffset.equals(BlockPos.ZERO)) {
            return null;
        }

        // A tank can span chunks. With the controller's chunk unloaded, this block simply has no tank for now - nothing
        // is changed or lost, and looking it up must not load that chunk (on the server it would).
        var controllerPos = getControllerPos();

        if (!level.isLoaded(controllerPos)) {
            return null;
        }

        return level.getBlockEntity(controllerPos) instanceof FluidTankBlockEntity tank && tank.isController() ? tank : null;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean hasWindows() {
        return windows;
    }

    /** Called by {@link FluidTankFormation} only. */
    void setMember(BlockPos controller) {
        controllerOffset = controller.subtract(worldPosition);
        width = 0;
        height = 0;
        content = FluidContent.EMPTY;
        lastSignal = -1;
        setChanged();
    }

    /** Called by {@link FluidTankFormation} only. */
    void setController(int width, int height, FluidContent content, boolean windows) {
        this.controllerOffset = BlockPos.ZERO;
        this.width = width;
        this.height = height;
        this.windows = windows;
        this.content = clamp(content, width, height);
        this.lastSignal = -1;
        setChanged();
    }

    /** Called by {@link FluidTankFormation} only - the tank-wide windows switch lives on the controller. */
    void setWindows(boolean windows) {
        this.windows = windows;
        setChanged();
        sync();
    }

    private static FluidContent clamp(FluidContent content, int width, int height) {
        var capacity = capacityOf(width, height);

        return content.isEmpty() ? FluidContent.EMPTY : content.withAmount(Math.min(content.amount(), capacity));
    }

    public static int capacityOf(int width, int height) {
        return width * width * height * FluidTankRules.MB_PER_BLOCK;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ports
    // ------------------------------------------------------------------------------------------------------------

    public boolean isOutput(Direction side) {
        return side.getAxis().isHorizontal() && (sideOutputs & (1 << side.get2DDataValue())) != 0;
    }

    /** Switches one side face between input and output. @return true if it is now an output */
    public boolean toggleSide(Direction side) {
        if (!side.getAxis().isHorizontal()) {
            return false;
        }

        sideOutputs ^= (byte) (1 << side.get2DDataValue());
        setChanged();
        // The valve on this face changes colour.
        sync();

        return isOutput(side);
    }

    public boolean hasPipe(Direction side) {
        return (pipes & (1 << side.get3DDataValue())) != 0;
    }

    /** Whether any valve is drawn on this block - lets the renderer skip the block at once. */
    public boolean hasAnyPipe() {
        return pipes != 0;
    }

    /**
     * Re-checks which faces have a pipe attached. A face counts when the block beside it carries fluid towards it and
     * is not another tank block. Called on placement and when a neighbour changes; syncs only if something changed.
     */
    public void refreshPipes() {
        if (level == null || level.isClientSide) {
            return;
        }

        byte found = 0;

        for (var direction : Direction.values()) {
            var neighbour = worldPosition.relative(direction);

            if (level.getBlockState(neighbour).getBlock() instanceof FluidTankBlock) {
                continue;
            }

            if (com.human.common.gameplay.fluid.FluidConnections.isConnected(level, worldPosition, direction)) {
                found |= (byte) (1 << direction.get3DDataValue());
            }
        }

        if (found != pipes) {
            pipes = found;
            setChanged();
            sync();
        }
    }

    @Override
    public boolean canFill(@Nullable Direction side) {
        if (side == null || side == Direction.UP) {
            return true;
        }

        return side != Direction.DOWN && !isOutput(side);
    }

    @Override
    public boolean canDrain(@Nullable Direction side) {
        if (side == null || side == Direction.DOWN) {
            return true;
        }

        return side != Direction.UP && isOutput(side);
    }

    // ------------------------------------------------------------------------------------------------------------
    // FluidEndpoint - members forward to the controller
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public FluidContent getContent() {
        var controller = controller();

        return controller == null ? FluidContent.EMPTY : controller.content;
    }

    @Override
    public int getCapacity() {
        var controller = controller();

        return controller == null ? 0 : capacityOf(controller.width, controller.height);
    }

    @Override
    public int insert(Fluid fluid, int maxAmount, boolean simulate) {
        var controller = controller();

        if (controller == null || maxAmount <= 0 || fluid == null || !controller.content.accepts(fluid)) {
            return 0;
        }

        var accepted = Math.min(maxAmount, controller.getCapacity() - controller.content.amount());

        if (accepted > 0 && !simulate) {
            controller.content = FluidContent.of(fluid, controller.content.amount() + accepted);
            controller.notifyContentChanged();
        }

        return Math.max(0, accepted);
    }

    @Override
    public int extract(@Nullable Fluid fluid, int maxAmount, boolean simulate) {
        var controller = controller();

        if (
            controller == null || maxAmount <= 0 || controller.content.isEmpty()
                || fluid != null && !controller.content.fluid().isSame(fluid)
        ) {
            return 0;
        }

        var taken = Math.min(maxAmount, controller.content.amount());

        if (!simulate) {
            controller.content = controller.content.withAmount(controller.content.amount() - taken);
            controller.notifyContentChanged();
        }

        return taken;
    }

    @Override
    public void setContentSilently(FluidContent newContent) {
        var controller = controller();

        if (controller != null) {
            controller.content = newContent.isEmpty() ? FluidContent.EMPTY : newContent;
        }
    }

    /**
     * Saves and syncs the controller. Comparators are told only when the 0-15 signal actually changes: a pipe filling
     * the tank every tick must not update the neighbours of all 800 blocks of a big tank every tick.
     */
    @Override
    public void notifyContentChanged() {
        var controller = controller();

        if (controller == null) {
            return;
        }

        controller.setChanged();
        controller.sync();

        var signal = controller.signal();

        if (signal != controller.lastSignal && controller.level != null && !controller.level.isClientSide) {
            controller.lastSignal = signal;
            FluidTankFormation.updateComparators(controller.level, controller.worldPosition, controller.width, controller.height);
        }
    }

    /** Comparator strength 0-15 from the fill level - 1 as soon as there is anything inside. */
    public int signal() {
        var controller = controller();

        if (controller == null || controller.content.isEmpty()) {
            return 0;
        }

        var capacity = capacityOf(controller.width, controller.height);

        return capacity <= 0 ? 0 : Math.max(1, (int) ((long) controller.content.amount() * 15L / capacity));
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Save / sync
    // ------------------------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putIntArray(NBT_OFFSET, new int[] { controllerOffset.getX(), controllerOffset.getY(), controllerOffset.getZ() });
        tag.putByte(NBT_OUTPUTS, sideOutputs);
        tag.putByte(NBT_PIPES, pipes);

        if (width > 0) {
            tag.putInt(NBT_WIDTH, width);
            tag.putInt(NBT_HEIGHT, height);
            tag.putBoolean(NBT_WINDOWS, windows);

            if (!content.isEmpty()) {
                tag.putString(NBT_FLUID, BuiltInRegistries.FLUID.getKey(content.fluid()).toString());
                tag.putInt(NBT_AMOUNT, content.amount());
            }
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        var wasController = isController();
        var oldWidth = width;
        var oldHeight = height;

        var offset = tag.getIntArray(NBT_OFFSET);

        controllerOffset = offset.length == 3 ? new BlockPos(offset[0], offset[1], offset[2]) : BlockPos.ZERO;
        sideOutputs = tag.getByte(NBT_OUTPUTS);
        pipes = tag.getByte(NBT_PIPES);
        width = Math.max(0, Math.min(FluidTankRules.MAX_WIDTH, tag.getInt(NBT_WIDTH)));
        height = Math.max(0, Math.min(FluidTankRules.MAX_HEIGHT, tag.getInt(NBT_HEIGHT)));
        windows = !tag.contains(NBT_WINDOWS) || tag.getBoolean(NBT_WINDOWS);

        var id = ResourceLocation.tryParse(tag.getString(NBT_FLUID));

        // ⚠ A fluid whose mod was removed loads as empty rather than failing.
        content = id == null || width <= 0
            ? FluidContent.EMPTY
            : clamp(FluidContent.of(BuiltInRegistries.FLUID.get(id), tag.getInt(NBT_AMOUNT)), width, height);

        // ⚠ CLIENT: whether this block draws the tank's fluid from anywhere on screen is decided when its chunk section
        // is rebuilt (the renderer's shouldRenderOffScreen, read in vanilla's SectionCompiler). Becoming or stopping
        // being the controller does not always change the block's look, so ask for that rebuild ourselves.
        if (level != null && level.isClientSide && (wasController != isController() || oldWidth != width || oldHeight != height)) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
