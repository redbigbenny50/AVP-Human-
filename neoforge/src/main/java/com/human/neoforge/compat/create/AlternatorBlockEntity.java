package com.human.neoforge.compat.create;

import com.human.common.gameplay.machine.MachineUpgrades;
import com.human.common.gameplay.power.PowerFaces;
import com.human.common.gameplay.power.PowerNode;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.gameplay.power.upgrade.BatteryUpgrade;
import com.human.common.gameplay.power.upgrade.BatteryUpgradeItem;
import com.human.common.gameplay.power.upgrade.UpgradeCard;
import com.human.common.gameplay.power.upgrade.UpgradeCardItem;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The alternator: turns Create rotation into avp_human power.
 * <h2>Why a GearboxBlockEntity</h2> Create's speed logic gives gearboxes their own rule - the direction flips between
 * faces on different axes - and it checks for this exact class. Extending it is what makes the three gear sides (back,
 * left, right) share one rotation correctly, without copying any of Create's propagation code.
 * <h2>The numbers</h2> Out: {@code alternator_fe_per_rpm} (1.5) FE/t per RPM - 384 FE/t at 256 RPM - times the speed
 * cards' multiplier (1.5x per card, 4 = 6x). Load on Create: {@code stress_per_rpm} (64) SU per RPM, times the same
 * speed multiplier, times 0.75 per efficiency card. Output is whole FE; at Create's speeds it lands on multiples of 8.
 * <h2>Power</h2> A producer with a buffer: each tick's output goes into it, and the grid draws from it. Top and bottom
 * faces only.
 */
public class AlternatorBlockEntity extends GearboxBlockEntity implements PowerNode.PowerProducer, PowerFaces, MenuProvider {

    /** [stated] Oct 24 - the alternator's four upgrade slots: speed and efficiency cards. */
    public static final int UPGRADE_SLOTS = 4;

    private static final String NBT_STORED = "Stored";

    private static final String NBT_UPGRADES = "Upgrades";

    private static final String NBT_REDSTONE = "RedstoneMode";

    /** With a redstone card fitted: when it may generate. Same three modes as every machine (IGNORE / RUNS / STOPS). */
    private MachineUpgrades.RedstoneMode redstoneMode = MachineUpgrades.RedstoneMode.IGNORE;

    /** False while a redstone card holds it off: it then makes nothing AND puts no load on Create. */
    private boolean enabled = true;

    private final ContainerData menuData = new ContainerData() {

        @Override
        public int get(int index) {
            return switch (index) {
                case AlternatorMenu.SPEED -> Math.round(Math.abs(getSpeed()));
                case AlternatorMenu.LOAD -> ConverterData.low(currentLoad());
                case AlternatorMenu.LOAD + 1 -> ConverterData.high(currentLoad());
                case AlternatorMenu.OUTPUT -> ConverterData.low(lastOutput);
                case AlternatorMenu.OUTPUT + 1 -> ConverterData.high(lastOutput);
                case AlternatorMenu.STORED -> ConverterData.low(stored);
                case AlternatorMenu.STORED + 1 -> ConverterData.high(stored);
                case AlternatorMenu.CAPACITY -> ConverterData.low(ConverterSettings.buffer());
                case AlternatorMenu.CAPACITY + 1 -> ConverterData.high(ConverterSettings.buffer());
                case AlternatorMenu.STATUS -> status();
                case AlternatorMenu.MODE -> redstoneMode.ordinal();
                case AlternatorMenu.HAS_REDSTONE -> MachineUpgrades.hasRedstone(upgrades) ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return AlternatorMenu.DATA_SIZE;
        }
    };

    private final SimpleContainer upgrades = new SimpleContainer(UPGRADE_SLOTS) {

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return acceptsUpgrade(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };

    private long stored;

    /** Output last tick, for the readouts. */
    private long lastOutput;

    private boolean linked;

    private boolean loading;

    public AlternatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        upgrades.addListener(container -> onUpgradesChanged());
    }

    /**
     * Speed and efficiency cards, and the redstone card. Not the hub chip: it ties a machine to the fuse panel that
     * FEEDS it, which a generator is not.
     */
    public static boolean acceptsUpgrade(ItemStack stack) {
        return stack.getItem() instanceof UpgradeCardItem card
            && (card.getCard() == UpgradeCard.SPEED || card.getCard() == UpgradeCard.EFFICIENCY)
            || stack.getItem() instanceof BatteryUpgradeItem battery && battery.getUpgrade() == BatteryUpgrade.REDSTONE;
    }

    public ContainerData menuData() {
        return menuData;
    }

    public void cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        setChanged();
    }

    /** SU this alternator is loading Create with right now (per-RPM impact times its speed). */
    private long currentLoad() {
        return enabled ? (long) Math.floor(calculateStressApplied() * Math.abs(getSpeed())) : 0L;
    }

    private int status() {
        if (!enabled) {
            return AlternatorMenu.STATUS_REDSTONE;
        }

        if (getSpeed() != 0F && isOverStressed()) {
            return AlternatorMenu.STATUS_OVERSTRESSED;
        }

        return lastOutput > 0 ? AlternatorMenu.STATUS_GENERATING : AlternatorMenu.STATUS_STOPPED;
    }

    private boolean redstoneAllows() {
        return level == null
            || !MachineUpgrades.hasRedstone(upgrades)
            || redstoneMode.allows(level.hasNeighborSignal(worldPosition));
    }

    public SimpleContainer getUpgrades() {
        return upgrades;
    }

    public long getStored() {
        return stored;
    }

    public long getLastOutput() {
        return lastOutput;
    }

    private double speedMultiplier() {
        return ConverterSettings.alternatorUpgrades()
            ? UpgradeCard.speedMultiplier(MachineUpgrades.count(upgrades, UpgradeCard.SPEED))
            : 1.0;
    }

    private double efficiencyMultiplier() {
        return ConverterSettings.alternatorUpgrades()
            ? Math.pow(UpgradeCard.EFFICIENCY_KEEP_PER_CARD, MachineUpgrades.count(upgrades, UpgradeCard.EFFICIENCY))
            : 1.0;
    }

    /** FE this tick: nothing while still, or while Create says the network is overstressed. */
    private long currentOutput() {
        var speed = Math.abs(getSpeed());

        if (!enabled || speed <= 0F || isOverStressed()) {
            return 0L;
        }

        return (long) Math.floor(ConverterSettings.alternatorFePerRpm() * speed * speedMultiplier());
    }

    /** SU per RPM this alternator puts on its Create network. */
    @Override
    public float calculateStressApplied() {
        var impact = enabled ? (float) (ConverterSettings.stressPerRpm() * speedMultiplier() * efficiencyMultiplier()) : 0F;
        this.lastStressApplied = impact;
        return impact;
    }

    /** A card went in or out: the load on Create changes NOW, through Create's own network update. */
    private void onUpgradesChanged() {
        if (loading) {
            return;
        }

        setChanged();

        if (level != null && !level.isClientSide && hasNetwork()) {
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
        }
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

        // A redstone card holding it off takes its load off Create too - through Create's own update, only on a change.
        var allowed = redstoneAllows();

        if (allowed != enabled) {
            enabled = allowed;
            setChanged();

            if (hasNetwork()) {
                getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
            }
        }

        var output = currentOutput();
        var capacity = ConverterSettings.buffer();
        stored = Math.min(capacity, stored + output);

        if (output != lastOutput) {
            lastOutput = output;
            setChanged();
        }

        // [stated] Oct 24 - the LED is lit while it generates. Only written when it changes.
        var lit = output > 0;
        var state = getBlockState();

        if (state.hasProperty(AlternatorBlock.LIT) && state.getValue(AlternatorBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(AlternatorBlock.LIT, lit), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public long getAvailablePower() {
        return level == null || level.isClientSide ? 0L : stored;
    }

    @Override
    public long extractPower(long maxAmount) {
        var taken = Math.max(0L, Math.min(maxAmount, stored));
        stored -= taken;
        return taken;
    }

    /** [stated] Oct 24 - power out of the top and bottom only. */
    @Override
    public boolean isFaceConnectable(Direction direction) {
        return direction != null && direction.getAxis() == Direction.Axis.Y;
    }

    /**
     * Removed OR unloaded (SmartBlockEntity calls this from setRemoved): off the grid either way, as every power block.
     */
    @Override
    public void invalidate() {
        super.invalidate();

        if (level instanceof ServerLevel serverLevel) {
            PowerSystem.get(serverLevel).unregisterNode(worldPosition, this);
        }

        linked = false;
    }

    /** Broken: the cards drop. */
    @Override
    public void destroy() {
        super.destroy();

        if (level != null && !level.isClientSide) {
            Containers.dropContents(level, worldPosition, upgrades);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putLong(NBT_STORED, stored);
        tag.put(NBT_UPGRADES, upgrades.createTag(registries));
        tag.putInt(NBT_REDSTONE, redstoneMode.ordinal());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        stored = Math.max(0L, tag.getLong(NBT_STORED));
        redstoneMode = MachineUpgrades.RedstoneMode.byOrdinal(tag.getInt(NBT_REDSTONE));

        if (tag.contains(NBT_UPGRADES, Tag.TAG_LIST)) {
            loading = true;

            try {
                upgrades.clearContent();
                upgrades.fromTag(tag.getList(NBT_UPGRADES, Tag.TAG_COMPOUND), registries);
            } finally {
                loading = false;
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AlternatorMenu(id, inventory, this);
    }

    /** Create's goggles: its own stress lines, then what this makes. */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(
            Component.literal("    ")
                .append(Component.translatable("avp_human.create.goggle.generating", currentOutput()))
                .withStyle(ChatFormatting.GRAY)
        );
        return true;
    }
}
