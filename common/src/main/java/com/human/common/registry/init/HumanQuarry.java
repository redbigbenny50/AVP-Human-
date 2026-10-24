package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.quarry.QuarryEdition;
import com.human.common.gameplay.quarry.QuarryFrameBlock;
import com.human.common.gameplay.quarry.QuarryTerminalBlock;
import com.human.common.gameplay.quarry.QuarryTerminalBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * The quarry: the terminal in its three editions and the frame block. The drill head, shaft and gantry are not blocks -
 * the terminal's renderer draws them - so nothing else registers here yet.
 * <p>
 * [stated] Oct 15: the bare terminal is crafted (diamond, desk terminal, diamond / three machine blocks / piston,
 * canister, piston); the W-Y and USCM terminals come from the fabricator, like the other faction editions. The frame is
 * five steel ingots in an X, making 16.
 * </p>
 */
public final class HumanQuarry {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<net.minecraft.world.inventory.MenuType<?>> MENUS = Human.MOD.registries()
        .create(BuiltInRegistries.MENU);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    public static final BLibHolder<Block> QUARRY_TERMINAL = BLOCKS.createHolder(
        "quarry_terminal",
        () -> new QuarryTerminalBlock(QuarryEdition.BARE, terminalProperties())
    );

    public static final BLibHolder<Block> WY_QUARRY_TERMINAL = BLOCKS.createHolder(
        "wy_quarry_terminal",
        () -> new QuarryTerminalBlock(QuarryEdition.WY, terminalProperties())
    );

    public static final BLibHolder<Block> USCM_QUARRY_TERMINAL = BLOCKS.createHolder(
        "uscm_quarry_terminal",
        () -> new QuarryTerminalBlock(QuarryEdition.USCM, terminalProperties())
    );

    public static final BLibHolder<Block> QUARRY_FRAME = BLOCKS.createHolder("quarry_frame", () -> new QuarryFrameBlock(frameProperties()));

    /** Phase 4b - the containment field that holds liquid back from the dig. No item: only the quarry places it. */
    public static final BLibHolder<Block> QUARRY_CONTAINMENT_FIELD = BLOCKS.createHolder(
        "quarry_containment_field",
        () -> new com.human.common.gameplay.quarry.QuarryContainmentBlock(fieldProperties())
    );

    public static final BLibHolder<Item> QUARRY_TERMINAL_ITEM = ITEMS.createHolder(
        "quarry_terminal",
        () -> new BlockItem(QUARRY_TERMINAL.get(), new Item.Properties())
    );

    public static final BLibHolder<Item> WY_QUARRY_TERMINAL_ITEM = ITEMS.createHolder(
        "wy_quarry_terminal",
        () -> new BlockItem(WY_QUARRY_TERMINAL.get(), new Item.Properties())
    );

    public static final BLibHolder<Item> USCM_QUARRY_TERMINAL_ITEM = ITEMS.createHolder(
        "uscm_quarry_terminal",
        () -> new BlockItem(USCM_QUARRY_TERMINAL.get(), new Item.Properties())
    );

    public static final BLibHolder<Item> QUARRY_FRAME_ITEM = ITEMS.createHolder(
        "quarry_frame",
        () -> new BlockItem(QUARRY_FRAME.get(), new Item.Properties())
    );

    public static final BLibHolder<BlockEntityType<QuarryTerminalBlockEntity>> QUARRY_TERMINAL_BLOCK_ENTITY = BLOCK_ENTITY_TYPES
        .createHolder(
            "quarry_terminal",
            () -> BlockEntityType.Builder.of(
                QuarryTerminalBlockEntity::new,
                QUARRY_TERMINAL.get(),
                WY_QUARRY_TERMINAL.get(),
                USCM_QUARRY_TERMINAL.get()
            ).build(null)
        );

    public static final BLibHolder<net.minecraft.world.inventory.MenuType<com.human.common.gameplay.quarry.QuarryTerminalMenu>> QUARRY_TERMINAL_MENU =
        MENUS
            .createHolder(
                "quarry_terminal",
                () -> new net.minecraft.world.inventory.MenuType<>(
                    com.human.common.gameplay.quarry.QuarryTerminalMenu::new,
                    net.minecraft.world.flag.FeatureFlagSet.of()
                )
            );

    private HumanQuarry() {}

    /** Same as the fabricator: a heavy machine, explosion-proof, never pushed by a piston. */
    private static BlockBehaviour.Properties terminalProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(5.0F, 1200.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .pushReaction(PushReaction.BLOCK);
    }

    /**
     * Steel-strength, but see-through: light passes, nothing spawns on it, nobody suffocates in it and it blocks no
     * view - the frame is a lattice, not a wall.
     */
    private static BlockBehaviour.Properties frameProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.0F, 6.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .isValidSpawn((state, level, pos, type) -> false)
            .isRedstoneConductor((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false);
    }

    /**
     * Glowing, see-through and easy to break by hand (nobody gets trapped); drops nothing; nothing spawns on it; it
     * blocks no view and smothers nobody. Solid and NOT replaceable - what stops liquid flowing into it.
     */
    private static BlockBehaviour.Properties fieldProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN)
            .strength(0.3F, 6.0F)
            .sound(SoundType.AMETHYST)
            .lightLevel(state -> 6)
            .noOcclusion()
            .noLootTable()
            .pushReaction(PushReaction.BLOCK)
            .isValidSpawn((state, level, pos, type) -> false)
            .isRedstoneConductor((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false);
    }

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
        BLOCK_ENTITY_TYPES.registerAll();
        MENUS.registerAll();
    }
}
