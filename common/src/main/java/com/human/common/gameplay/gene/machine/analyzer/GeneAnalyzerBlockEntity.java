package com.human.common.gameplay.gene.machine.analyzer;

import com.blib.api.client.animation.v1.command.AzCommand;
import com.blib.api.client.animation.v1.command.AzTarget;
import com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehaviors;
import com.human.common.gameplay.block.entity.power.PowerConsumerBlockEntity;
import com.human.common.gameplay.gene.GeneColours;
import com.human.common.gameplay.gene.discovery.GeneDiscoveries;
import com.human.common.gameplay.item.SyringeItem;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.HumanGeneAnalyzer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;

/**
 * The Gene Analyzer's core: the sample tray, the scan, and the clips.
 * <h2>POWER</h2> [stated] Oct 28: its idle look (tray out, bars lit) shows whenever its grid has supply, checked once a
 * second; NO idle draw. It draws {@link #SCAN_POWER} per tick only while scanning, and a scan only advances on ticks it
 * was paid for - an under-supplied grid slows it, an unpowered one pauses it (nothing is lost).
 * <h2>SCAN</h2> Scans SYRINGES ([stated] Oct 29). When it finishes, the genes in the sample and the donor species are
 * discovered for the player who loaded it ([stated] Oct 28: a tray sample counts as reading it). The DNA screens come
 * up at 50% and 100% and hold until the sample is taken.
 * <h2>CLIPS</h2> Base track: off / power_on then idle / load then scan (at 50% speed, [stated] Oct 27) / finish. A
 * second track carries progress_50 and progress_100 over the top, and is cancelled when the sample leaves.
 */
public class GeneAnalyzerBlockEntity extends PowerConsumerBlockEntity {

    /** 12 seconds - two passes of the scanner at its 50%-speed clip. */
    public static final int SCAN_TICKS = 240;

    public static final long SCAN_POWER = 32L;

    public static final int SAMPLE_SLOT = 0;

    public static final int PAPER_SLOT = 1;

    public static final int DYE_SLOT = 2;

    public static final int DOCUMENT_SLOT = 3;

    private static final int SUPPLY_CHECK_TICKS = 20;

    private static final String NBT_ITEMS = "Items";

    private static final String NBT_PROGRESS = "Progress";

    private static final String NBT_LOADER = "Loader";

    private static final String NBT_SUPPLIED = "Supplied";

    private final SimpleContainer items = new SimpleContainer(4);

    private int progress;

    private @Nullable UUID loader;

    private ItemStack lastSample = ItemStack.EMPTY;

    private boolean supplied;

    private int supplyTimer;

    public GeneAnalyzerBlockEntity(BlockPos pos, BlockState state) {
        super(HumanGeneAnalyzer.BLOCK_ENTITY.get(), pos, state);
        items.addListener(container -> onItemsChanged());
    }

    public static boolean accepts(ItemStack stack) {
        if (!(stack.getItem() instanceof SyringeItem)) {
            return false;
        }
        var contents = stack.get(HumanDataComponents.SYRINGE_CONTENTS.get());
        return contents != null && !contents.geneBonusDataEntries().isEmpty();
    }

    public SimpleContainer items() {
        return items;
    }

    public ItemStack sample() {
        return items.getItem(SAMPLE_SLOT);
    }

    public int progress() {
        return progress;
    }

    public boolean isDone() {
        return !sample().isEmpty() && progress >= SCAN_TICKS;
    }

    public boolean isSupplied() {
        return supplied;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Use
    // ------------------------------------------------------------------------------------------------------------

    /** Right-click: the database screen. */
    public void use(Player player) {
        player.openMenu(
            new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, opener) -> new GeneAnalyzerMenu(id, inventory, this),
                Component.translatable(getBlockState().getBlock().getDescriptionId())
            )
        );
        if (player instanceof ServerPlayer serverPlayer) {
            AnalyzerDatabase.send(serverPlayer);
        }
    }

    /** Server: the player who last put something in the tray through the screen - the scan's discoveries are theirs. */
    void setLoader(@Nullable Player player) {
        loader = player == null ? null : player.getUUID();
    }

    /** A different sample in the tray restarts the scan; taking it out clears it. */
    private void onItemsChanged() {
        var sample = sample();
        if (!ItemStack.isSameItemSameComponents(sample, lastSample)) {
            lastSample = sample.copy();
            progress = 0;
            if (level != null && !level.isClientSide && !sample.isEmpty()) {
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            sync();
        }
        setChanged();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Power and scan
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
        return supplied && !sample().isEmpty() && progress < SCAN_TICKS ? SCAN_POWER : 0L;
    }

    @Override
    public void poweredTick(Level level, BlockPos pos, BlockState state) {
        checkSupply(level);
        if (!sample().isEmpty() && progress < SCAN_TICKS) {
            progress++;
            if (progress == SCAN_TICKS / 2) {
                sync();
            } else if (progress >= SCAN_TICKS) {
                finish(level);
            } else if (progress % 20 == 0) {
                setChanged();
            }
        }
    }

    @Override
    public void unpoweredTick(Level level, BlockPos pos, BlockState state) {
        checkSupply(level);
    }

    private void checkSupply(Level level) {
        if (--supplyTimer > 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        supplyTimer = SUPPLY_CHECK_TICKS;
        var grid = PowerSystem.get(serverLevel).gridAt(worldPosition);
        var nowSupplied = grid != null && grid.hasSupply();
        if (nowSupplied != supplied) {
            supplied = nowSupplied;
            sync();
        }
    }

    /** The scan is done: what the sample holds goes into the loading player's database. */
    private void finish(Level level) {
        level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6F, 1.6F);
        // [stated] Nov 1: the scan decodes the syringe - stamped ANALYZED whoever loaded it and whether or not they are
        // online. Its genes then show to anyone (names still per player: known genes, or a profiled donor).
        SyringeItem.markAnalyzed(sample());
        if (
            loader != null && level instanceof ServerLevel serverLevel && serverLevel.getServer()
                .getPlayerList()
                .getPlayer(loader) instanceof ServerPlayer player
        ) {
            var sample = sample();
            var contents = sample.get(HumanDataComponents.SYRINGE_CONTENTS.get());
            var genes = new ArrayList<net.minecraft.resources.ResourceLocation>();
            if (contents != null) {
                contents.geneBonusDataEntries().forEach(entry -> genes.add(entry.id()));
            }
            var donor = SyringeItem.donorOf(sample);
            EntityType<?> species = donor == null ? null : EntityType.byString(donor).orElse(null);
            // [stated] Nov 1: genes are taught only when the donor species is PROFILED (read alive with the gene
            // reader).
            // Otherwise the decode is partial: categories and strengths, names ???, until the species is profiled.
            if (GeneDiscoveries.hasProfile(serverLevel.getServer(), player.getUUID(), donor)) {
                GeneDiscoveries.discover(player, genes, species);
            }
            // Anyone looking at the database sees the new entries straight away.
            if (player.containerMenu instanceof GeneAnalyzerMenu) {
                AnalyzerDatabase.send(player);
            }
        }
        setChanged();
        sync();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Client
    // ------------------------------------------------------------------------------------------------------------

    private enum Base {
        NONE,
        OFF,
        IDLE,
        SCAN,
        DONE
    }

    private static final String BASE_TRACK = "base_controller";

    public static final String DNA_TRACK = "dna";

    private static AzCommand<GeneAnalyzerBlockEntity> clip(
        String track,
        String name,
        com.blib.api.client.animation.v1.command.play_behavior.AzPlayBehavior behavior
    ) {
        return AzCommand.<GeneAnalyzerBlockEntity>idempotent().play(AzTarget.track(track), name, behavior).build();
    }

    private static final AzCommand<GeneAnalyzerBlockEntity> OFF = clip(BASE_TRACK, "off", AzPlayBehaviors.LOOP);

    private static final AzCommand<GeneAnalyzerBlockEntity> POWER_ON = clip(BASE_TRACK, "power_on", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand<GeneAnalyzerBlockEntity> IDLE = clip(BASE_TRACK, "idle", AzPlayBehaviors.LOOP);

    private static final AzCommand<GeneAnalyzerBlockEntity> LOAD = clip(BASE_TRACK, "load", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    /** [stated] Oct 27: the scan clip plays at 50% (Blockbench does not save preview speed). */
    private static final AzCommand<GeneAnalyzerBlockEntity> SCAN = AzCommand.<GeneAnalyzerBlockEntity>idempotent()
        .play(AzTarget.track(BASE_TRACK), "scan", AzPlayBehaviors.LOOP)
        .setSpeed(AzTarget.track(BASE_TRACK), 0.5D)
        .build();

    private static final AzCommand<GeneAnalyzerBlockEntity> FINISH = clip(BASE_TRACK, "finish", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand<GeneAnalyzerBlockEntity> DNA_HALF = clip(DNA_TRACK, "progress_50", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    private static final AzCommand<GeneAnalyzerBlockEntity> DNA_FULL = clip(DNA_TRACK, "progress_100", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    /**
     * Both strands hidden. The DNA strands are animated by the dna track ONLY (the base clips no longer key them, so
     * the two tracks never fight over them). Hidden is a held clip rather than a cancel: a cancelled track lets BLib
     * ease the strands back to their rest pose - full size - within a tick.
     */
    private static final AzCommand<GeneAnalyzerBlockEntity> DNA_CLEAR = clip(DNA_TRACK, "progress_0", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

    /** Speed back to normal for every clip but scan. */
    private static final AzCommand<GeneAnalyzerBlockEntity> NORMAL_SPEED = AzCommand.<GeneAnalyzerBlockEntity>idempotent()
        .setSpeed(AzTarget.track(BASE_TRACK), 1.0D)
        .build();

    private Base shownBase = Base.NONE;

    private int shownDna = -1;

    /** Ticks until a chained clip follows (power_on into idle, load into scan). */
    private int chainTimer = -1;

    private @Nullable AzCommand<GeneAnalyzerBlockEntity> chained;

    public void clientTick() {
        if (level == null || com.blib.internal.client.animation.AzAnimatorAccessor.getOrNull(this) == null) {
            // No animator until the first render; the states are applied once it exists.
            return;
        }
        var hasSample = !sample().isEmpty();
        var base = !supplied ? Base.OFF : !hasSample ? Base.IDLE : progress >= SCAN_TICKS ? Base.DONE : Base.SCAN;

        if (base != shownBase) {
            var first = shownBase == Base.NONE;
            chainTimer = -1;
            chained = null;
            NORMAL_SPEED.dispatchForBlockEntity(this);
            switch (base) {
                case OFF -> OFF.dispatchForBlockEntity(this);
                case IDLE -> {
                    if (first || shownBase == Base.DONE || shownBase == Base.SCAN) {
                        IDLE.dispatchForBlockEntity(this);
                    } else {
                        POWER_ON.dispatchForBlockEntity(this);
                        chain(IDLE, 24);
                    }
                }
                case SCAN -> {
                    // The DNA strands are the dna track's alone (hidden until half way, see DNA_CLEAR). Coming into
                    // view
                    // mid-scan, load runs for a moment first.
                    LOAD.dispatchForBlockEntity(this);
                    chain(SCAN, first ? 2 : 30);
                }
                case DONE -> FINISH.dispatchForBlockEntity(this);
                default -> {}
            }
            shownBase = base;
        }

        var dna = !supplied || !hasSample ? 0 : progress >= SCAN_TICKS ? 2 : progress >= SCAN_TICKS / 2 ? 1 : 0;
        if (dna != shownDna) {
            switch (dna) {
                case 1 -> DNA_HALF.dispatchForBlockEntity(this);
                case 2 -> DNA_FULL.dispatchForBlockEntity(this);
                default -> DNA_CLEAR.dispatchForBlockEntity(this);
            }
            shownDna = dna;
        }

        if (chainTimer >= 0 && --chainTimer == 0 && chained != null) {
            chained.dispatchForBlockEntity(this);
            chained = null;
            chainTimer = -1;
        }
    }

    private void chain(AzCommand<GeneAnalyzerBlockEntity> next, int ticks) {
        chained = next;
        chainTimer = ticks;
    }

    /** The sample's fluid colour for the renderer (syringe blend), or 0 for none. */
    public int sampleColour() {
        var colour = GeneColours.syringe(sample());
        return (colour >>> 24) == 0 ? 0 : colour;
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
        tag.put(NBT_ITEMS, ContainerHelper.saveAllItems(new CompoundTag(), items.getItems(), true, registries));
        tag.putInt(NBT_PROGRESS, progress);
        if (loader != null) {
            tag.putUUID(NBT_LOADER, loader);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        items.clearContent();
        ContainerHelper.loadAllItems(tag.getCompound(NBT_ITEMS), items.getItems(), registries);
        lastSample = sample().copy();
        progress = tag.getInt(NBT_PROGRESS);
        loader = tag.hasUUID(NBT_LOADER) ? tag.getUUID(NBT_LOADER) : null;
        supplied = tag.getBoolean(NBT_SUPPLIED);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.put(NBT_ITEMS, ContainerHelper.saveAllItems(new CompoundTag(), items.getItems(), true, registries));
        tag.putInt(NBT_PROGRESS, progress);
        tag.putBoolean(NBT_SUPPLIED, supplied);
        return tag;
    }
}
