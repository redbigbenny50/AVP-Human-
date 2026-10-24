package com.human.common.registry.init;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.gameplay.block.entity.machine.ElectricMachineBlockEntity;
import com.human.common.gameplay.machine.ElectricMachineKind;
import com.human.common.gameplay.menu.machine.ElectricMachineMenu;
import com.human.common.gameplay.vaporizer.VaporizerBlock;
import com.human.common.gameplay.vaporizer.VaporizerTopBlock;
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

/**
 * The vaporizer. [stated] Oct 17: Industrial (crafted), Weyland-Yutani and USCM editions - one machine, three skins -
 * plus the one invisible top block they all share. It runs on the shared machine code as
 * {@link ElectricMachineKind#VAPORIZER}.
 */
public final class HumanVaporizer {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    private static final BLibRegistry<MenuType<?>> MENUS = Human.MOD.registries().create(BuiltInRegistries.MENU);

    public static final BLibHolder<Block> VAPORIZER = BLOCKS.createHolder("vaporizer", () -> new VaporizerBlock(properties()));

    public static final BLibHolder<Block> WY_VAPORIZER = BLOCKS.createHolder("wy_vaporizer", () -> new VaporizerBlock(properties()));

    public static final BLibHolder<Block> USCM_VAPORIZER = BLOCKS.createHolder("uscm_vaporizer", () -> new VaporizerBlock(properties()));

    /** The space above every vaporizer. No item; drops nothing; never drawn. */
    public static final BLibHolder<Block> VAPORIZER_TOP = BLOCKS.createHolder(
        "vaporizer_top",
        () -> new VaporizerTopBlock(properties().noLootTable())
    );

    public static final BLibHolder<Item> VAPORIZER_ITEM = ITEMS.createHolder(
        "vaporizer",
        () -> new BlockItem(VAPORIZER.get(), new Item.Properties())
    );

    public static final BLibHolder<Item> WY_VAPORIZER_ITEM = ITEMS.createHolder(
        "wy_vaporizer",
        () -> new BlockItem(WY_VAPORIZER.get(), new Item.Properties())
    );

    public static final BLibHolder<Item> USCM_VAPORIZER_ITEM = ITEMS.createHolder(
        "uscm_vaporizer",
        () -> new BlockItem(USCM_VAPORIZER.get(), new Item.Properties())
    );

    public static final BLibHolder<BlockEntityType<ElectricMachineBlockEntity>> VAPORIZER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "vaporizer",
        () -> BlockEntityType.Builder.of(
            (pos, state) -> new ElectricMachineBlockEntity(ElectricMachineKind.VAPORIZER, pos, state),
            VAPORIZER.get(),
            WY_VAPORIZER.get(),
            USCM_VAPORIZER.get()
        ).build(null)
    );

    public static final BLibHolder<MenuType<ElectricMachineMenu>> VAPORIZER_MENU = MENUS.createHolder(
        "vaporizer",
        () -> new MenuType<>((id, inventory) -> new ElectricMachineMenu(ElectricMachineKind.VAPORIZER, id, inventory), FeatureFlagSet.of())
    );

    private HumanVaporizer() {}

    /** The fabricator's: steel-strong, see-through for the model, and never pushed by a piston (it is two blocks). */
    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(5.0F, 6.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .requiresCorrectToolForDrops()
            .pushReaction(PushReaction.BLOCK);
    }

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
        BLOCK_ENTITY_TYPES.registerAll();
        MENUS.registerAll();
    }
}
