package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.cosmetic.CosmeticBlock;
import com.human.common.gameplay.cosmetic.CosmeticBlockEntity;
import com.human.common.gameplay.cosmetic.CosmeticCrafterBlock;
import com.human.common.gameplay.cosmetic.CosmeticCrafterBlockEntity;
import com.human.common.gameplay.cosmetic.CosmeticCrafterMenu;
import com.human.common.gameplay.cosmetic.CosmeticDesign;
import com.human.common.gameplay.cosmetic.CosmeticStyle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [stated] Oct 31 - the cosmetic furniture programme: the Cosmetic Crafter (standard, W-Y, USCM) and every cosmetic
 * design in every registered {@link CosmeticStyle}. Names: {@code <design>}, {@code wy_<design>},
 * {@code uscm_<design>}.
 */
public final class HumanCosmetics {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    private static final BLibRegistry<MenuType<?>> MENUS = Human.MOD.registries().create(BuiltInRegistries.MENU);

    // ------------------------------------------------------------------------------------------------------------
    // The crafter
    // ------------------------------------------------------------------------------------------------------------

    public static final BLibHolder<Block> COSMETIC_CRAFTER = BLOCKS.createHolder(
        "cosmetic_crafter",
        () -> new CosmeticCrafterBlock(machineProperties(), CosmeticStyle.INDUSTRIAL)
    );

    public static final BLibHolder<Block> WY_COSMETIC_CRAFTER = BLOCKS.createHolder(
        "wy_cosmetic_crafter",
        () -> new CosmeticCrafterBlock(machineProperties(), CosmeticStyle.WY)
    );

    public static final BLibHolder<Block> USCM_COSMETIC_CRAFTER = BLOCKS.createHolder(
        "uscm_cosmetic_crafter",
        () -> new CosmeticCrafterBlock(machineProperties(), CosmeticStyle.USCM)
    );

    public static final BLibHolder<Item> COSMETIC_CRAFTER_ITEM = ITEMS.createHolder(
        "cosmetic_crafter",
        () -> new BlockItem(COSMETIC_CRAFTER.get(), new Item.Properties())
    );

    public static final BLibHolder<Item> WY_COSMETIC_CRAFTER_ITEM = ITEMS.createHolder(
        "wy_cosmetic_crafter",
        () -> new BlockItem(WY_COSMETIC_CRAFTER.get(), new Item.Properties())
    );

    public static final BLibHolder<Item> USCM_COSMETIC_CRAFTER_ITEM = ITEMS.createHolder(
        "uscm_cosmetic_crafter",
        () -> new BlockItem(USCM_COSMETIC_CRAFTER.get(), new Item.Properties())
    );

    public static final BLibHolder<BlockEntityType<CosmeticCrafterBlockEntity>> CRAFTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "cosmetic_crafter",
        () -> BlockEntityType.Builder.of(
            CosmeticCrafterBlockEntity::new,
            COSMETIC_CRAFTER.get(),
            WY_COSMETIC_CRAFTER.get(),
            USCM_COSMETIC_CRAFTER.get()
        ).build(null)
    );

    public static final BLibHolder<MenuType<CosmeticCrafterMenu>> CRAFTER_MENU = MENUS.createHolder(
        "cosmetic_crafter",
        () -> new MenuType<>(CosmeticCrafterMenu::new, FeatureFlagSet.of())
    );

    // ------------------------------------------------------------------------------------------------------------
    // The designs - every design in every style, in catalogue order
    // ------------------------------------------------------------------------------------------------------------

    /** Every cosmetic block by registry name, in catalogue order. */
    public static final Map<String, BLibHolder<Block>> DECOR_BLOCKS = new LinkedHashMap<>();

    public static final Map<String, BLibHolder<Item>> DECOR_ITEMS = new LinkedHashMap<>();

    static {
        for (var design : CosmeticDesign.values()) {
            for (var style : CosmeticStyle.values()) {
                var name = design.blockName(style);
                BLibHolder<Block> block = BLOCKS.createHolder(
                    name,
                    () -> (Block) CosmeticBlock.create(decorProperties(), design, style)
                );

                DECOR_BLOCKS.put(name, block);
                DECOR_ITEMS.put(name, ITEMS.createHolder(name, () -> new BlockItem(block.get(), new Item.Properties())));
            }
        }
    }

    /** One block entity type for every ANIMATED design (the ones with glowing screens). */
    public static final BLibHolder<BlockEntityType<CosmeticBlockEntity>> DECOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "cosmetic_decor",
        () -> BlockEntityType.Builder.of(CosmeticBlockEntity::new, animatedBlocks()).build(null)
    );

    private HumanCosmetics() {}

    private static Block[] animatedBlocks() {
        var blocks = new ArrayList<Block>();

        for (var design : CosmeticDesign.values()) {
            if (design.animated() || com.human.common.gameplay.cosmetic.CosmeticStorage.has(design)) {
                for (var style : CosmeticStyle.values()) {
                    blocks.add(DECOR_BLOCKS.get(design.blockName(style)).get());
                }
            }
        }

        return blocks.toArray(new Block[0]);
    }

    /** Every cosmetic item of one style, in catalogue order - for that style's creative tab. */
    public static List<BLibHolder<Item>> decorItems(CosmeticStyle style) {
        var items = new ArrayList<BLibHolder<Item>>();

        for (var design : CosmeticDesign.values()) {
            items.add(DECOR_ITEMS.get(design.blockName(style)));
        }

        return items;
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(5.0F, 1200.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .pushReaction(PushReaction.BLOCK);
    }

    /** Furniture: metal, breaks like an iron door, never pushed (a piston would split a multi-block piece). */
    private static BlockBehaviour.Properties decorProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.5F, 6.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .pushReaction(PushReaction.BLOCK);
    }

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
        BLOCK_ENTITY_TYPES.registerAll();
        MENUS.registerAll();
    }
}
