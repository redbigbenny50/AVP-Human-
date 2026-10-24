package com.human.common.registry.init;

import com.blib.api.common.network.v1.NetworkHandler;
import com.blib.api.common.network.v1.PacketDirection;
import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.blib.api.common.registry.v1.impl.BLibNetworkRegistry;
import com.human.Human;
import com.human.common.gameplay.storage.CrateBlock;
import com.human.common.gameplay.storage.CrateBlockEntity;
import com.human.common.gameplay.storage.CrateContents;
import com.human.common.gameplay.storage.CrateItem;
import com.human.common.gameplay.storage.CrateMenu;
import com.human.common.gameplay.storage.CrateSize;
import com.human.common.gameplay.storage.CrateType;
import com.human.common.gameplay.storage.CrateUpgradeRecipe;
import com.human.common.network.packet.C2SCrateInsertPayload;
import com.human.common.network.packet.C2SCrateTakePayload;
import com.human.common.network.packet.S2CCrateContentsPayload;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Everything the storage crates register: 168 blocks and items (6 families x 3 sizes + containers, in every colour or
 * metal), one block entity type shared by all of them, the crate menu, the item component that marks a full crate, a
 * Storage creative tab, and the crate packets.
 */
public final class HumanCrates {

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    private static final BLibRegistry<MenuType<?>> MENUS = Human.MOD.registries().create(BuiltInRegistries.MENU);

    private static final BLibRegistry<DataComponentType<?>> COMPONENTS = Human.MOD.registries()
        .create(BuiltInRegistries.DATA_COMPONENT_TYPE);

    private static final BLibRegistry<CreativeModeTab> TABS = Human.MOD.registries().create(BuiltInRegistries.CREATIVE_MODE_TAB);

    private static final BLibRegistry<RecipeSerializer<?>> RECIPE_SERIALIZERS = Human.MOD.registries()
        .create(BuiltInRegistries.RECIPE_SERIALIZER);

    private static final BLibNetworkRegistry DIRECTIONS = Human.MOD.registries().createNetworkRegistry();

    private static final BLibNetworkRegistry HANDLERS = Human.MOD.registries().createNetworkRegistry();

    /** Crate blocks by type, in {@link CrateType#ALL} order. */
    public static final Map<CrateType, BLibHolder<Block>> BLOCK_BY_TYPE;

    /** Crate items by type, in {@link CrateType#ALL} order. */
    public static final Map<CrateType, BLibHolder<Item>> ITEM_BY_TYPE;

    static {
        var blocks = new LinkedHashMap<CrateType, BLibHolder<Block>>();
        var items = new LinkedHashMap<CrateType, BLibHolder<Item>>();

        for (var type : CrateType.ALL) {
            var block = BLOCKS.<Block>createHolder(type.id(), () -> new CrateBlock(type, properties(type)));

            blocks.put(type, block);
            items.put(type, ITEMS.<Item>createHolder(type.id(), () -> new CrateItem(block.get(), type, new Item.Properties())));
        }

        BLOCK_BY_TYPE = Collections.unmodifiableMap(blocks);
        ITEM_BY_TYPE = Collections.unmodifiableMap(items);
    }

    public static final BLibHolder<BlockEntityType<CrateBlockEntity>> CRATE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "crate",
        () -> BlockEntityType.Builder.of(
            CrateBlockEntity::new,
            BLOCK_BY_TYPE.values().stream().map(BLibHolder::get).toArray(Block[]::new)
        ).build(null)
    );

    public static final BLibHolder<MenuType<CrateMenu>> CRATE_MENU = MENUS.createHolder(
        "crate",
        () -> new MenuType<>(CrateMenu::new, FeatureFlagSet.of())
    );

    public static final BLibHolder<DataComponentType<CrateContents>> CRATE_CONTENTS = COMPONENTS.createHolder(
        "crate_contents",
        () -> DataComponentType.<CrateContents>builder()
            .persistent(CrateContents.CODEC)
            .networkSynchronized(CrateContents.STREAM_CODEC)
            .build()
    );

    /** {@code avp_human:crate_upgrade} - a shaped recipe that carries a crate's contents into the bigger crate. */
    public static final BLibHolder<RecipeSerializer<CrateUpgradeRecipe>> CRATE_UPGRADE_SERIALIZER = RECIPE_SERIALIZERS.createHolder(
        "crate_upgrade",
        () -> CrateUpgradeRecipe.SERIALIZER
    );

    public static final BLibHolder<CreativeModeTab> STORAGE_TAB = TABS.createHolder(
        "storage",
        () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .icon(() -> new ItemStack(ITEM_BY_TYPE.get(CrateType.ALL.get(4)).get()))
            .title(Component.translatable("creativeModeTab.avp_human.storage"))
            // Sep 28 - USCM and W-Y crates live in their faction tabs.
            .displayItems((parameters, output) -> ITEM_BY_TYPE.forEach((type, item) -> {
                if (
                    type.family() != com.human.common.gameplay.storage.CrateFamily.USCM
                        && type.family() != com.human.common.gameplay.storage.CrateFamily.WY
                ) {
                    output.accept(item.get());
                }
            }))
            .build()
    );

    private HumanCrates() {}

    private static BlockBehaviour.Properties properties(CrateType type) {
        var container = type.size() == CrateSize.CONTAINER;
        var plastic = type.family().isPlastic();

        return BlockBehaviour.Properties.of()
            .mapColor(plastic ? MapColor.COLOR_LIGHT_GRAY : MapColor.METAL)
            .strength(container ? 5.0F : 2.5F, container ? 1200.0F : 6.0F)
            .sound(plastic ? SoundType.STONE : SoundType.METAL)
            .noOcclusion()
            .noLootTable()
            .pushReaction(PushReaction.BLOCK);
    }

    public static void initialize() {
        BLOCKS.registerAll();
        ITEMS.registerAll();
        BLOCK_ENTITY_TYPES.registerAll();
        MENUS.registerAll();
        COMPONENTS.registerAll();
        TABS.registerAll();
        RECIPE_SERIALIZERS.registerAll();
        registerNetwork();
    }

    private static void registerNetwork() {
        DIRECTIONS.registerPacketDirection(new PacketDirection.S2C<>(S2CCrateContentsPayload.TYPE, S2CCrateContentsPayload.CODEC));
        DIRECTIONS.registerPacketDirection(new PacketDirection.C2S<>(C2SCrateTakePayload.TYPE, C2SCrateTakePayload.CODEC));
        DIRECTIONS.registerPacketDirection(new PacketDirection.C2S<>(C2SCrateInsertPayload.TYPE, C2SCrateInsertPayload.CODEC));

        HANDLERS.registerPacketHandler(
            new NetworkHandler.FromClient<>(
                C2SCrateTakePayload.TYPE,
                C2SCrateTakePayload.CODEC,
                (payload, player) -> {
                    if (
                        player.containerMenu instanceof CrateMenu menu && menu.containerId == payload.containerId()
                            && menu.stillValid(player)
                    ) {
                        menu.take(payload.prototype(), payload.action());
                    }
                }
            )
        );
        HANDLERS.registerPacketHandler(
            new NetworkHandler.FromClient<>(
                C2SCrateInsertPayload.TYPE,
                C2SCrateInsertPayload.CODEC,
                (payload, player) -> {
                    if (
                        player.containerMenu instanceof CrateMenu menu && menu.containerId == payload.containerId()
                            && menu.stillValid(player)
                    ) {
                        menu.insertCarried(payload.single());
                    }
                }
            )
        );
        HANDLERS.registerPacketHandler(
            new NetworkHandler.FromServer<>(
                S2CCrateContentsPayload.TYPE,
                S2CCrateContentsPayload.CODEC,
                (payload, player) -> com.human.client.screen.CrateScreen.handleContents(payload)
            )
        );
    }
}
