package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.block.property.HumanBlockProperties;
import com.human.common.gameplay.distributor.DistributorBlock;
import com.human.common.gameplay.distributor.DistributorBlockEntity;
import com.human.common.gameplay.machine.ElectricMachineKind;
import com.human.common.gameplay.menu.machine.ElectricMachineMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The distributor. [stated] Oct 17 - a 1x1 tier-1 machine (same family as the press, hydrator, freezer and grinder),
 * running on the shared machine code as {@link ElectricMachineKind#DISTRIBUTOR}. Three editions ([stated] Oct 18): the
 * standard one (crafted), and Weyland-Yutani and USCM (fabricator only) - one machine, three skins.
 * <p>
 * ⚠ The block entity type is typed as {@link ElectricMachineBlockEntity} - the shared machine code (ticker, renderer,
 * menu, pipes) is written against it - and builds a {@link DistributorBlockEntity}, which is one.
 * </p>
 */
public final class HumanDistributor {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    private static final BLibRegistry<MenuType<?>> MENUS = Human.MOD.registries().create(BuiltInRegistries.MENU);

    public static final BLibHolder<Block> DISTRIBUTOR = BLOCKS.createHolder(
        "electric_distributor",
        () -> new DistributorBlock(properties())
    );

    /**
     * [stated] Oct 18 - the Weyland-Yutani and USCM editions: the same machine in their palettes, made in the
     * fabricator.
     */
    public static final BLibHolder<Block> WY_DISTRIBUTOR = BLOCKS.createHolder(
        "wy_electric_distributor",
        () -> new DistributorBlock(properties())
    );

    public static final BLibHolder<Block> USCM_DISTRIBUTOR = BLOCKS.createHolder(
        "uscm_electric_distributor",
        () -> new DistributorBlock(properties())
    );

    public static final BLibHolder<Item> DISTRIBUTOR_ITEM = ITEMS.createHolder(
        "electric_distributor",
        () -> new BlockItem(DISTRIBUTOR.get(), new Item.Properties())
    );

    public static final BLibHolder<Item> WY_DISTRIBUTOR_ITEM = ITEMS.createHolder(
        "wy_electric_distributor",
        () -> new BlockItem(WY_DISTRIBUTOR.get(), new Item.Properties())
    );

    public static final BLibHolder<Item> USCM_DISTRIBUTOR_ITEM = ITEMS.createHolder(
        "uscm_electric_distributor",
        () -> new BlockItem(USCM_DISTRIBUTOR.get(), new Item.Properties())
    );

    public static final BLibHolder<BlockEntityType<ElectricMachineBlockEntity>> DISTRIBUTOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES
        .createHolder(
            "electric_distributor",
            // One block entity type for all three editions - only the skin differs, as with the vaporizer.
            () -> BlockEntityType.Builder.<ElectricMachineBlockEntity>of(
                DistributorBlockEntity::new,
                DISTRIBUTOR.get(),
                WY_DISTRIBUTOR.get(),
                USCM_DISTRIBUTOR.get()
            ).build(null)
        );

    public static final BLibHolder<MenuType<ElectricMachineMenu>> DISTRIBUTOR_MENU = MENUS.createHolder(
        "electric_distributor",
        () -> new MenuType<>(
            (id, inventory) -> new ElectricMachineMenu(ElectricMachineKind.DISTRIBUTOR, id, inventory),
            FeatureFlagSet.of()
        )
    );

    private HumanDistributor() {}

    /** Exactly the other tier-1 machines' properties (HumanBlocks: the electric freezer, press, grinder...). */
    private static BlockBehaviour.Properties properties() {
        return HumanBlockProperties.STEEL.build();
    }

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
        BLOCK_ENTITY_TYPES.registerAll();
        MENUS.registerAll();
    }
}
