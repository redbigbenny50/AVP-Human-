package com.human.neoforge.compat.create;

import com.human.common.gameplay.power.PowerFaces;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.gameplay.power.PowerSystem;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The electric gearbox: turns avp_human power into Create rotation.
 * <h2>Speed</h2> Set with Create's own value box on the blank front (scroll while looking at it), -256 to 256 RPM like
 * the creative motor - negative turns it the other way. 0 is off.
 * <h2>The numbers</h2> Draws {@code electric_gearbox_fe_per_rpm} (2.0) FE/t per RPM - 512 FE/t at 256 RPM - and never
 * less than {@code electric_gearbox_min_draw} (8) while running. Supplies {@code stress_per_rpm} (64) SU per RPM -
 * 16,384 SU at 256.
 * <h2>Running and stopping</h2> It runs off its buffer. It STARTS only with two ticks' draw stored and STOPS when a
 * tick's draw is not there, so a grid that is short does not make it flick between turning and stopped every tick -
 * each flip re-forms the whole Create network.
 */
public class ElectricGearboxBlockEntity extends GeneratingKineticBlockEntity implements PowerNode.PowerConsumer, PowerFaces, MenuProvider {

    public static final int DEFAULT_RPM = 64;

    private static final String NBT_STORED = "Stored";

    private static final String NBT_RUNNING = "Running";

    private ScrollValueBehaviour targetSpeed;

    private long stored;

    private boolean running;

    private boolean linked;

    private final ContainerData menuData = new ContainerData() {

        @Override
        public int get(int index) {
            return switch (index) {
                case ElectricGearboxMenu.RPM -> getTargetRpm() + 256;
                case ElectricGearboxMenu.DRAW -> ConverterData.low(running ? drawPerTick() : 0L);
                case ElectricGearboxMenu.DRAW + 1 -> ConverterData.high(running ? drawPerTick() : 0L);
                case ElectricGearboxMenu.SUPPLY -> ConverterData.low(supplied());
                case ElectricGearboxMenu.SUPPLY + 1 -> ConverterData.high(supplied());
                case ElectricGearboxMenu.STORED -> ConverterData.low(stored);
                case ElectricGearboxMenu.STORED + 1 -> ConverterData.high(stored);
                case ElectricGearboxMenu.CAPACITY -> ConverterData.low(ConverterSettings.buffer());
                case ElectricGearboxMenu.CAPACITY + 1 -> ConverterData.high(ConverterSettings.buffer());
                case ElectricGearboxMenu.STATUS -> status();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return ElectricGearboxMenu.DATA_SIZE;
        }
    };

    public ContainerData menuData() {
        return menuData;
    }

    /** SU it is supplying: its per-RPM capacity at its set speed, while it runs. */
    private long supplied() {
        return running ? (long) Math.floor(ConverterSettings.stressPerRpm() * Math.abs(getTargetRpm())) : 0L;
    }

    private int status() {
        if (getTargetRpm() == 0) {
            return ElectricGearboxMenu.STATUS_OFF;
        }

        if (!running) {
            return ElectricGearboxMenu.STATUS_NO_POWER;
        }

        return isOverStressed() ? ElectricGearboxMenu.STATUS_OVERSTRESSED : ElectricGearboxMenu.STATUS_RUNNING;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ElectricGearboxMenu(id, inventory, this);
    }

    public ElectricGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        targetSpeed = new KineticScrollValueBehaviour(
            Component.translatable("avp_human.create.electric_gearbox.speed"),
            this,
            new ConverterValueBox()
        );
        targetSpeed.between(-256, 256);
        targetSpeed.value = DEFAULT_RPM;
        targetSpeed.withCallback(value -> onSpeedSettingChanged());
        behaviours.add(targetSpeed);
    }

    public int getTargetRpm() {
        return targetSpeed == null ? 0 : targetSpeed.getValue();
    }

    public boolean isRunning() {
        return running;
    }

    public long getStored() {
        return stored;
    }

    private Direction facing() {
        var state = getBlockState();
        return state.hasProperty(HorizontalKineticBlock.HORIZONTAL_FACING)
            ? state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING)
            : Direction.NORTH;
    }

    /** FE this gearbox needs each tick at its set speed; 0 when set to 0. */
    public long drawPerTick() {
        var rpm = Math.abs(getTargetRpm());

        if (rpm == 0) {
            return 0L;
        }

        return Math.max(ConverterSettings.gearboxMinDraw(), (long) Math.ceil(ConverterSettings.gearboxFePerRpm() * rpm));
    }

    @Override
    public void initialize() {
        super.initialize();

        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed()) {
            updateGeneratedRotation();
        }
    }

    /**
     * Its shaft is on the BACK, so it turns as seen from there - the same rule Create's motor uses for its shaft side.
     */
    @Override
    public float getGeneratedSpeed() {
        if (!running || targetSpeed == null) {
            return 0F;
        }

        return convertToDirection(targetSpeed.getValue(), facing().getOpposite());
    }

    @Override
    public float calculateAddedStressCapacity() {
        var capacity = ConverterSettings.stressPerRpm();
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    private void onSpeedSettingChanged() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (getTargetRpm() == 0) {
            setRunning(false);
        } else if (running) {
            updateGeneratedRotation();
        }

        setChanged();
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide) {
            return;
        }

        if (!linked && level instanceof ServerLevel serverLevel) {
            linked = true;
            PowerSystem.get(serverLevel).linkOnLoad(serverLevel, worldPosition, this);
        }

        // While Create holds the network overstressed nothing turns, so it idles at the minimum draw instead of burning
        // its full rate on a stalled network.
        var draw = running && isOverStressed() ? Math.min(drawPerTick(), ConverterSettings.gearboxMinDraw()) : drawPerTick();

        if (running) {
            if (draw > 0 && stored >= draw) {
                stored -= draw;
            } else {
                setRunning(false);
            }
        } else if (draw > 0 && stored >= draw * 2) {
            stored -= draw;
            setRunning(true);
        }
    }

    private void setRunning(boolean value) {
        if (running == value || level == null) {
            return;
        }

        running = value;
        updateGeneratedRotation();
        setChanged();

        // [stated] Oct 24 - the LED: on while it has power and turns.
        var state = getBlockState();

        if (state.hasProperty(ElectricGearboxBlock.LIT) && state.getValue(ElectricGearboxBlock.LIT) != value) {
            level.setBlock(
                worldPosition,
                state.setValue(ElectricGearboxBlock.LIT, value),
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS
            );
        }
    }

    @Override
    public long getRequestedPower() {
        return level == null || level.isClientSide ? 0L : Math.max(0L, ConverterSettings.buffer() - stored);
    }

    @Override
    public long receivePower(long maxAmount) {
        var accepted = Math.max(0L, Math.min(maxAmount, ConverterSettings.buffer() - stored));
        stored += accepted;
        return accepted;
    }

    /** [stated] Oct 24 - power in through left, right, top and bottom: everything but the front and the shaft. */
    @Override
    public boolean isFaceConnectable(Direction direction) {
        return direction != null && direction.getAxis() != facing().getAxis();
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (level instanceof ServerLevel serverLevel) {
            PowerSystem.get(serverLevel).unregisterNode(worldPosition, this);
        }

        linked = false;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putLong(NBT_STORED, stored);
        tag.putBoolean(NBT_RUNNING, running);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        stored = Math.max(0L, tag.getLong(NBT_STORED));
        running = tag.getBoolean(NBT_RUNNING);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(
            Component.literal("    ")
                .append(
                    running
                        ? Component.translatable("avp_human.create.goggle.drawing", drawPerTick())
                        : Component.translatable("avp_human.create.goggle.no_power")
                )
                .withStyle(ChatFormatting.GRAY)
        );
        return true;
    }
}
