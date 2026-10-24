package com.human.common.registry.init;

import com.blib.api.common.network.v1.NetworkHandler;
import com.blib.api.common.network.v1.PacketDirection;
import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.blib.api.common.registry.v1.impl.BLibNetworkRegistry;
import com.human.Human;
import com.human.common.gameplay.fabrication.BlueprintChipItem;
import com.human.common.gameplay.fabrication.BlueprintTableMenu;
import com.human.common.gameplay.fabrication.DriveBayBlock;
import com.human.common.gameplay.fabrication.DriveBayBlockEntity;
import com.human.common.gameplay.fabrication.DriveBayItem;
import com.human.common.gameplay.fabrication.DriveBayMenu;
import com.human.common.gameplay.fabrication.FabricatingRecipe;
import com.human.common.gameplay.fabrication.FabricatorBlock;
import com.human.common.gameplay.fabrication.FabricatorBlockEntity;
import com.human.common.gameplay.fabrication.FabricatorItem;
import com.human.common.gameplay.fabrication.FabricatorMenu;
import com.human.common.gameplay.fabrication.Faction;
import com.human.common.gameplay.fabrication.FactionLicenseItem;
import com.human.common.network.packet.S2CFabricatorPayload;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * The fabrication system: blueprint chips, faction licenses and the blueprint table's screen. The drive bay and the
 * fabricator itself register here too as they are built.
 */
public final class HumanFabrication {

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<DataComponentType<?>> COMPONENTS = Human.MOD.registries()
        .create(BuiltInRegistries.DATA_COMPONENT_TYPE);

    private static final BLibRegistry<net.minecraft.world.inventory.MenuType<?>> MENUS = Human.MOD.registries()
        .create(BuiltInRegistries.MENU);

    private static final BLibRegistry<Block> BLOCKS = Human.MOD.registries().create(BuiltInRegistries.BLOCK);

    private static final BLibRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = Human.MOD.registries()
        .create(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    private static final BLibRegistry<RecipeType<?>> RECIPE_TYPES = Human.MOD.registries().create(BuiltInRegistries.RECIPE_TYPE);

    private static final BLibRegistry<RecipeSerializer<?>> RECIPE_SERIALIZERS = Human.MOD.registries()
        .create(BuiltInRegistries.RECIPE_SERIALIZER);

    private static final BLibNetworkRegistry DIRECTIONS = Human.MOD.registries().createNetworkRegistry();

    private static final BLibNetworkRegistry HANDLERS = Human.MOD.registries().createNetworkRegistry();

    /**
     * The item a blueprint chip prints, as a PLAIN ID - never a registry reference and never a copy of the item.
     * <p>
     * ⚠ A registry-checked codec fails to load a chip naming an item whose mod was removed, and a component that fails
     * to load can take the whole stack with it. A plain id always loads; the chip just shows the id and prints nothing
     * until the item exists again.
     * </p>
     */
    public static final BLibHolder<DataComponentType<net.minecraft.resources.ResourceLocation>> BLUEPRINT = COMPONENTS.createHolder(
        "blueprint",
        () -> DataComponentType.<net.minecraft.resources.ResourceLocation>builder()
            .persistent(net.minecraft.resources.ResourceLocation.CODEC)
            .networkSynchronized(net.minecraft.resources.ResourceLocation.STREAM_CODEC)
            .cacheEncoding()
            .build()
    );

    public static final BLibHolder<Item> BLUEPRINT_CHIP = ITEMS.createHolder(
        "blueprint_chip",
        () -> new BlueprintChipItem(new Item.Properties().stacksTo(16))
    );

    public static final BLibHolder<Item> USCM_FACTION_LICENSE = ITEMS.createHolder(
        "uscm_faction_license",
        () -> new FactionLicenseItem(Faction.USCM, new Item.Properties().stacksTo(1).rarity(Rarity.RARE))
    );

    public static final BLibHolder<Item> WY_FACTION_LICENSE = ITEMS.createHolder(
        "wy_faction_license",
        () -> new FactionLicenseItem(Faction.WY, new Item.Properties().stacksTo(1).rarity(Rarity.RARE))
    );

    public static final BLibHolder<MenuType<BlueprintTableMenu>> BLUEPRINT_TABLE = MENUS.createHolder(
        "blueprint_table",
        () -> new MenuType<>(BlueprintTableMenu::new, FeatureFlagSet.of())
    );

    // ------------------------------------------------------------------------------------------------------------
    // Drive bay - [stated] Oct 13: standard (crafted), W-Y and USCM (printed in a fabricator with their license)
    // ------------------------------------------------------------------------------------------------------------

    public static final BLibHolder<Block> DRIVE_BAY = BLOCKS.createHolder("drive_bay", () -> new DriveBayBlock(driveBayProperties()));

    public static final BLibHolder<Block> WY_DRIVE_BAY = BLOCKS.createHolder("wy_drive_bay", () -> new DriveBayBlock(driveBayProperties()));

    public static final BLibHolder<Block> USCM_DRIVE_BAY = BLOCKS.createHolder(
        "uscm_drive_bay",
        () -> new DriveBayBlock(driveBayProperties())
    );

    public static final BLibHolder<Item> DRIVE_BAY_ITEM = ITEMS.createHolder(
        "drive_bay",
        () -> new DriveBayItem(DRIVE_BAY.get(), new Item.Properties())
    );

    public static final BLibHolder<Item> WY_DRIVE_BAY_ITEM = ITEMS.createHolder(
        "wy_drive_bay",
        () -> new DriveBayItem(WY_DRIVE_BAY.get(), new Item.Properties())
    );

    public static final BLibHolder<Item> USCM_DRIVE_BAY_ITEM = ITEMS.createHolder(
        "uscm_drive_bay",
        () -> new DriveBayItem(USCM_DRIVE_BAY.get(), new Item.Properties())
    );

    public static final BLibHolder<BlockEntityType<DriveBayBlockEntity>> DRIVE_BAY_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "drive_bay",
        () -> BlockEntityType.Builder.of(DriveBayBlockEntity::new, DRIVE_BAY.get(), WY_DRIVE_BAY.get(), USCM_DRIVE_BAY.get()).build(null)
    );

    public static final BLibHolder<MenuType<DriveBayMenu>> DRIVE_BAY_MENU = MENUS.createHolder(
        "drive_bay",
        () -> new MenuType<>(DriveBayMenu::new, FeatureFlagSet.of())
    );

    // ------------------------------------------------------------------------------------------------------------
    // Fabricator - [stated] Oct 13: standard (crafted), W-Y and USCM (printed in a fabricator with their license)
    // ------------------------------------------------------------------------------------------------------------

    public static final BLibHolder<Block> FABRICATOR = BLOCKS.createHolder("fabricator", () -> new FabricatorBlock(fabricatorProperties()));

    public static final BLibHolder<Block> WY_FABRICATOR = BLOCKS.createHolder(
        "wy_fabricator",
        () -> new FabricatorBlock(fabricatorProperties())
    );

    public static final BLibHolder<Block> USCM_FABRICATOR = BLOCKS.createHolder(
        "uscm_fabricator",
        () -> new FabricatorBlock(fabricatorProperties())
    );

    public static final BLibHolder<Item> FABRICATOR_ITEM = ITEMS.createHolder(
        "fabricator",
        () -> new FabricatorItem(FABRICATOR.get(), new Item.Properties())
    );

    public static final BLibHolder<Item> WY_FABRICATOR_ITEM = ITEMS.createHolder(
        "wy_fabricator",
        () -> new FabricatorItem(WY_FABRICATOR.get(), new Item.Properties())
    );

    public static final BLibHolder<Item> USCM_FABRICATOR_ITEM = ITEMS.createHolder(
        "uscm_fabricator",
        () -> new FabricatorItem(USCM_FABRICATOR.get(), new Item.Properties())
    );

    public static final BLibHolder<BlockEntityType<FabricatorBlockEntity>> FABRICATOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.createHolder(
        "fabricator",
        () -> BlockEntityType.Builder.of(FabricatorBlockEntity::new, FABRICATOR.get(), WY_FABRICATOR.get(), USCM_FABRICATOR.get())
            .build(null)
    );

    public static final BLibHolder<MenuType<FabricatorMenu>> FABRICATOR_MENU = MENUS.createHolder(
        "fabricator",
        () -> new MenuType<>(FabricatorMenu::new, FeatureFlagSet.of())
    );

    /** {@code avp_human:fabricating} - ingredient lists for fabricator-only items (the faction editions). */
    public static final BLibHolder<RecipeType<FabricatingRecipe>> FABRICATING_TYPE = RECIPE_TYPES.createHolder(
        "fabricating",
        () -> new RecipeType<>() {

            @Override
            public String toString() {
                return "avp_human:fabricating";
            }
        }
    );

    public static final BLibHolder<RecipeSerializer<FabricatingRecipe>> FABRICATING_SERIALIZER = RECIPE_SERIALIZERS.createHolder(
        "fabricating",
        () -> FabricatingRecipe.SERIALIZER
    );

    private HumanFabrication() {}

    private static BlockBehaviour.Properties fabricatorProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(5.0F, 1200.0F)
            .sound(SoundType.METAL)
            .noOcclusion()
            .pushReaction(PushReaction.BLOCK);
    }

    /** Blast resistance like obsidian: a bay holds blueprints, and an explosion must never be how they are lost. */
    private static BlockBehaviour.Properties driveBayProperties() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.5F, 1200.0F)
            .sound(SoundType.METAL)
            .pushReaction(PushReaction.BLOCK);
    }

    public static void initialize() {
        COMPONENTS.registerAll();
        BLOCKS.registerAll();
        ITEMS.registerAll();
        BLOCK_ENTITY_TYPES.registerAll();
        MENUS.registerAll();
        RECIPE_TYPES.registerAll();
        RECIPE_SERIALIZERS.registerAll();
        registerNetwork();
    }

    private static void registerNetwork() {
        DIRECTIONS.registerPacketDirection(new PacketDirection.S2C<>(S2CFabricatorPayload.TYPE, S2CFabricatorPayload.CODEC));
        HANDLERS.registerPacketHandler(
            new NetworkHandler.FromServer<>(
                S2CFabricatorPayload.TYPE,
                S2CFabricatorPayload.CODEC,
                (payload, player) -> com.human.client.screen.FabricatorScreen.handle(payload)
            )
        );
    }
}
