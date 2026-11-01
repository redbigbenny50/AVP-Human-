package com.human.common.gameplay.gene.machine.analyzer;

import com.human.common.gameplay.gene.discovery.GeneDiscoveries;
import com.human.common.gameplay.gene.discovery.GeneticResearchItem;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.registry.init.HumanGeneAnalyzer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Gene Analyzer's database screen (approved genetic_analyzer_v2_ingame_scale3.png, 432x336, Oct 28).
 * <p>
 * Slots at the mockup's cells: the SAMPLE TRAY (filled syringes), PAPER and BLACK DYE, the DOCUMENT output, and the
 * player's inventory. Whoever puts a syringe in the tray owns that scan's discoveries. PRINT DOCUMENT arrives as a
 * packet with the ticked ids; one paper and one black dye make one Genetic Research, teaching only what the printing
 * player actually knows.
 * </p>
 */
public class GeneAnalyzerMenu extends AbstractContainerMenu {

    public static final int SCREEN_WIDTH = 432;

    public static final int SCREEN_HEIGHT = 336;

    /** Bottom panels start here (mockup BY = DY + 28, DY = LY + LH + 4, LY 72, LH 131). */
    public static final int BOTTOM_Y = 235;

    public static final int TRAY_X = 187;

    public static final int TRAY_Y = BOTTOM_Y + 7;

    public static final int PAPER_X = 185;

    public static final int DYE_X = 253;

    public static final int SUPPLY_Y = BOTTOM_Y + 64;

    public static final int DOCUMENT_X = 403;

    public static final int DOCUMENT_Y = BOTTOM_Y + 64;

    public static final int INVENTORY_X = 11;

    public static final int INVENTORY_Y = BOTTOM_Y + 5;

    public static final int HOTBAR_Y = BOTTOM_Y + 63;

    public static final int DATA_PROGRESS = 0;

    public static final int DATA_SUPPLIED = 1;

    /** Grid store in thousands of FE (-1 when the grid has no stores), and the remainder below a thousand. */
    public static final int DATA_STORED_K = 2;

    public static final int DATA_STORED_REST = 3;

    private static final int DATA_COUNT = 4;

    private static final int MACHINE_SLOTS = 4;

    private static final int POWER_REFRESH_TICKS = 20;

    private final Container items;

    private final ContainerData data;

    private final @Nullable GeneAnalyzerBlockEntity analyzer;

    private final Player player;

    private final int[] power = { -1, 0 };

    private int powerTimer;

    public GeneAnalyzerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(DATA_COUNT), null);
    }

    public GeneAnalyzerMenu(int containerId, Inventory inventory, GeneAnalyzerBlockEntity analyzer) {
        this(containerId, inventory, analyzer.items(), null, analyzer);
    }

    private GeneAnalyzerMenu(
        int containerId,
        Inventory inventory,
        Container items,
        @Nullable ContainerData clientData,
        @Nullable GeneAnalyzerBlockEntity analyzer
    ) {
        super(HumanGeneAnalyzer.MENU.get(), containerId);
        this.items = items;
        this.analyzer = analyzer;
        this.player = inventory.player;
        this.data = clientData != null ? clientData : new ServerData();

        addSlot(new Slot(items, GeneAnalyzerBlockEntity.SAMPLE_SLOT, TRAY_X, TRAY_Y) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return GeneAnalyzerBlockEntity.accepts(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void set(@NotNull ItemStack stack) {
                if (GeneAnalyzerMenu.this.analyzer != null && !stack.isEmpty()) {
                    GeneAnalyzerMenu.this.analyzer.setLoader(player);
                }
                super.set(stack);
            }
        });
        addSlot(new Slot(items, GeneAnalyzerBlockEntity.PAPER_SLOT, PAPER_X, SUPPLY_Y) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(Items.PAPER);
            }
        });
        addSlot(new Slot(items, GeneAnalyzerBlockEntity.DYE_SLOT, DYE_X, SUPPLY_Y) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(Items.BLACK_DYE);
            }
        });
        addSlot(new Slot(items, GeneAnalyzerBlockEntity.DOCUMENT_SLOT, DOCUMENT_X, DOCUMENT_Y) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
        addDataSlots(data);
        refreshPower();
    }

    public Container items() {
        return items;
    }

    public int data(int index) {
        return data.get(index);
    }

    private final class ServerData implements ContainerData {

        @Override
        public int get(int index) {
            if (analyzer == null) {
                return 0;
            }
            return switch (index) {
                case DATA_PROGRESS -> analyzer.progress();
                case DATA_SUPPLIED -> analyzer.isSupplied() ? 1 : 0;
                case DATA_STORED_K -> power[0];
                case DATA_STORED_REST -> power[1];
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    }

    @Override
    public void broadcastChanges() {
        if (analyzer != null && --powerTimer <= 0) {
            powerTimer = POWER_REFRESH_TICKS;
            refreshPower();
        }
        super.broadcastChanges();
    }

    /**
     * The header's POWER readout: what the grid's stores hold. Once a second while open - a snapshot walks the grid.
     */
    private void refreshPower() {
        if (analyzer == null || !(analyzer.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var grid = PowerSystem.get(level).gridAt(analyzer.getBlockPos());
        if (grid == null) {
            power[0] = -1;
            power[1] = 0;
            return;
        }
        var snapshot = grid.snapshot();
        if (snapshot.stores() == 0) {
            power[0] = -1;
            power[1] = 0;
        } else {
            // Container data travels as shorts.
            power[0] = (int) Math.min(Short.MAX_VALUE, snapshot.stored() / 1000L);
            power[1] = (int) (snapshot.stored() % 1000L);
        }
    }

    /** PRINT DOCUMENT. Only ids the player has discovered are printed; one paper and one black dye each. */
    public void print(ServerPlayer player, String geneList, String speciesList) {
        if (analyzer == null || player.getServer() == null) {
            return;
        }
        var paper = items.getItem(GeneAnalyzerBlockEntity.PAPER_SLOT);
        var dye = items.getItem(GeneAnalyzerBlockEntity.DYE_SLOT);
        if (!paper.is(Items.PAPER) || !dye.is(Items.BLACK_DYE) || !items.getItem(GeneAnalyzerBlockEntity.DOCUMENT_SLOT).isEmpty()) {
            return;
        }
        var known = GeneDiscoveries.get(player.getServer());
        var genes = new ArrayList<String>();
        for (var id : split(geneList)) {
            if (known.genesOf(player.getUUID()).contains(id)) {
                genes.add(id);
            }
        }
        var species = new ArrayList<String>();
        for (var id : split(speciesList)) {
            if (known.speciesOf(player.getUUID()).contains(id)) {
                species.add(id);
            }
        }
        if (genes.isEmpty() && species.isEmpty()) {
            return;
        }
        paper.shrink(1);
        dye.shrink(1);
        items.setItem(GeneAnalyzerBlockEntity.DOCUMENT_SLOT, GeneticResearchItem.of(genes, species));
        analyzer.getLevel()
            .playSound(
                null,
                analyzer.getBlockPos(),
                net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN,
                net.minecraft.sounds.SoundSource.BLOCKS,
                0.8F,
                1.0F
            );
    }

    private static List<String> split(String list) {
        return list.isEmpty() ? List.of() : Arrays.asList(list.split("\n"));
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        var stack = slot.getItem();
        var copy = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (GeneAnalyzerBlockEntity.accepts(stack)) {
            if (!slots.get(GeneAnalyzerBlockEntity.SAMPLE_SLOT).hasItem() && analyzer != null) {
                analyzer.setLoader(player);
            }
            if (!moveItemStackTo(stack, GeneAnalyzerBlockEntity.SAMPLE_SLOT, GeneAnalyzerBlockEntity.SAMPLE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.PAPER)) {
            if (!moveItemStackTo(stack, GeneAnalyzerBlockEntity.PAPER_SLOT, GeneAnalyzerBlockEntity.PAPER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.BLACK_DYE)) {
            if (!moveItemStackTo(stack, GeneAnalyzerBlockEntity.DYE_SLOT, GeneAnalyzerBlockEntity.DYE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return analyzer == null || !analyzer.isRemoved() && player.distanceToSqr(analyzer.getBlockPos().getCenter()) <= 64.0D;
    }
}
