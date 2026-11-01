package com.human.common.gameplay.cosmetic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Storage in cosmetic cabinets, lockers, fridges, freezers and crates ([stated] Oct 31): vanilla's own chest screen, so
 * there is nothing new to learn.
 * <ul>
 * <li>Sized to the piece: a one-block piece holds 9, a two-block piece 27, anything bigger 54.</li>
 * <li>Like a chest, its doors (or lid) open while anyone has it open and close when the last viewer does.</li>
 * <li>The screen closes when the player walks away (vanilla's block-entity range check).</li>
 * <li>Everything inside drops when the piece is broken.</li>
 * </ul>
 */
public final class CosmeticStorage {

    /** design -> slots. */
    public static final Map<CosmeticDesign, Integer> SLOTS = Map.ofEntries(
        Map.entry(CosmeticDesign.STORAGE_CABINET, 27),
        Map.entry(CosmeticDesign.HABITAT_LOCKER, 27),
        Map.entry(CosmeticDesign.REFRIGERATOR, 27),
        Map.entry(CosmeticDesign.SPECIMEN_LOCKER, 27),
        Map.entry(CosmeticDesign.CRYOGENIC_SAMPLE_STORAGE, 27),
        Map.entry(CosmeticDesign.SPECIMEN_FREEZER, 27),
        Map.entry(CosmeticDesign.WALL_CABINET, 27),
        Map.entry(CosmeticDesign.FIELD_SUPPLY_CASE, 27),
        Map.entry(CosmeticDesign.HAZMAT_STORAGE, 9),
        Map.entry(CosmeticDesign.SAMPLE_FREEZER, 9),
        Map.entry(CosmeticDesign.TOOL_CHEST, 9),
        Map.entry(CosmeticDesign.CARGO_CRATE, 9),
        Map.entry(CosmeticDesign.TISSUE_STORAGE, 9),
        Map.entry(CosmeticDesign.GEAR_CAGE, 54)
    );

    private CosmeticStorage() {}

    public static boolean has(CosmeticDesign design) {
        return SLOTS.containsKey(design);
    }

    /**
     * The container a storage piece keeps: counts its viewers, and swings the piece's doors (its OPEN state) open while
     * anyone looks inside, shut when the last viewer leaves. Only ever valid within reach of its block.
     */
    public static final class Contents extends SimpleContainer {

        private final CosmeticBlockEntity owner;

        private int viewers;

        public Contents(CosmeticBlockEntity owner, int slots) {
            super(slots);
            this.owner = owner;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            owner.setChanged();
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return Container.stillValidBlockEntity(owner, player);
        }

        @Override
        public void startOpen(@NotNull Player player) {
            if (!player.isSpectator() && viewers++ == 0) {
                swing(true);
            }
        }

        @Override
        public void stopOpen(@NotNull Player player) {
            if (!player.isSpectator() && viewers > 0 && --viewers == 0) {
                swing(false);
            }
        }

        private void swing(boolean open) {
            Level level = owner.getLevel();

            if (level == null || level.isClientSide) {
                return;
            }

            var state = owner.getBlockState();
            BlockPos pos = owner.getBlockPos();

            if (state.getBlock() instanceof CosmeticBlock block && state.hasProperty(CosmeticBlock.OPEN)) {
                block.setOnEveryCell(level, pos, state, CosmeticBlock.OPEN, open);
            }

            level.playSound(
                null,
                pos,
                CosmeticBlock.doorSound(((CosmeticBlock) state.getBlock()).design(), open),
                SoundSource.BLOCKS,
                0.6F,
                open ? 1.0F : 1.1F
            );
        }
    }

    /** The chest screen for {@code storage}, titled with the piece's own name. */
    public static @Nullable SimpleMenuProvider menu(Contents storage, Component title) {
        int rows = storage.getContainerSize() / 9;
        MenuType<ChestMenu> type = rows <= 1 ? MenuType.GENERIC_9x1 : rows <= 3 ? MenuType.GENERIC_9x3 : MenuType.GENERIC_9x6;

        return new SimpleMenuProvider((id, inventory, player) -> new ChestMenu(type, id, inventory, storage, rows), title);
    }
}
