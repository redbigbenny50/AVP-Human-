package com.human.common.registry.init;

import com.blib.api.common.data_sync.v1.model.DataSyncKey;
import com.blib.api.common.network.v1.NetworkHandler;
import com.blib.api.common.network.v1.PacketDirection;
import com.blib.api.common.registry.v1.BLibBuiltInRegistries;
import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.blib.api.common.registry.v1.impl.BLibNetworkRegistry;
import com.human.Human;
import com.human.common.gameplay.harvest.HarvestGenes;
import com.human.common.gameplay.harvest.SubstanceBucketItem;
import com.human.common.gameplay.harvest.SubstanceBucketUnpackRecipe;
import com.human.common.network.packet.C2SHarvestPickPayload;
import com.human.common.network.packet.S2COpenHarvestWheelPayload;
import com.just.codec.stream.impl.StreamCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

/**
 * Everything the harvest genes need, in one place (genetics phase 2, harvest pass): the substance bucket item and the
 * component naming what it holds, the crafting recipe that empties it, the synced flag that tells clients which tools a
 * mob answers to, and the selection-wheel packets.
 */
public final class HumanHarvest {

    private static final BLibRegistry<DataComponentType<?>> COMPONENTS = Human.MOD.registries()
        .create(BuiltInRegistries.DATA_COMPONENT_TYPE);

    private static final BLibRegistry<Item> ITEMS = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    private static final BLibRegistry<RecipeSerializer<?>> RECIPE_SERIALIZERS = Human.MOD.registries()
        .create(BuiltInRegistries.RECIPE_SERIALIZER);

    private static final BLibRegistry<DataSyncKey<?>> SYNC_KEYS = Human.MOD.registries().create(BLibBuiltInRegistries.DATA_SYNC_KEYS);

    private static final BLibNetworkRegistry DIRECTIONS = Human.MOD.registries().createNetworkRegistry();

    private static final BLibNetworkRegistry HANDLERS = Human.MOD.registries().createNetworkRegistry();

    /** The item a substance bucket holds (e.g. minecraft:ink_sac). */
    public static final BLibHolder<DataComponentType<ResourceLocation>> SUBSTANCE = COMPONENTS.createHolder(
        "substance",
        () -> DataComponentType.<ResourceLocation>builder()
            .persistent(ResourceLocation.CODEC)
            .networkSynchronized(ResourceLocation.STREAM_CODEC)
            .build()
    );

    /**
     * [stated] Oct 25 - a substance with no fluid of its own comes out as a bucket of that item. It places the item's
     * block if the item packs 9-into-a-block (slime), otherwise it can't be placed; in a crafting grid it gives the 9
     * items back plus the bucket.
     */
    public static final BLibHolder<Item> SUBSTANCE_BUCKET = ITEMS.createHolder(
        "substance_bucket",
        () -> new SubstanceBucketItem(new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET))
    );

    public static final BLibHolder<SimpleCraftingRecipeSerializer<SubstanceBucketUnpackRecipe>> UNPACK_SERIALIZER =
        RECIPE_SERIALIZERS.createHolder(
            "substance_bucket_unpack",
            () -> new SimpleCraftingRecipeSerializer<>(SubstanceBucketUnpackRecipe::new)
        );

    /**
     * Which harvest tools a mob answers to, as {@link HarvestGenes.Tool} bits. Synced so a client claims the click the
     * same way the server does - otherwise a dedicated-server client would let the click fall through to the bucket's
     * own use and scoop the water behind the mob as well.
     */
    public static final BLibHolder<DataSyncKey<Integer>> ENTITY_HARVEST_TOOLS = SYNC_KEYS.createHolder(
        "entity_harvest_tools",
        () -> new DataSyncKey.Builder<Integer>(com.human.HumanResources.location("entity_harvest_tools"))
            .networkSynchronized(StreamCodecs.INT)
            .build(0)
    );

    private HumanHarvest() {}

    public static void initialize() {
        COMPONENTS.registerAll();
        ITEMS.registerAll();
        RECIPE_SERIALIZERS.registerAll();
        SYNC_KEYS.registerAll();
        registerNetwork();
    }

    private static void registerNetwork() {
        DIRECTIONS.registerPacketDirection(
            new PacketDirection.S2C<>(S2COpenHarvestWheelPayload.TYPE, S2COpenHarvestWheelPayload.CODEC)
        );
        DIRECTIONS.registerPacketDirection(new PacketDirection.C2S<>(C2SHarvestPickPayload.TYPE, C2SHarvestPickPayload.CODEC));

        HANDLERS.registerPacketHandler(
            new NetworkHandler.FromServer<>(
                S2COpenHarvestWheelPayload.TYPE,
                S2COpenHarvestWheelPayload.CODEC,
                (payload, player) -> com.human.client.screen.HarvestWheelScreen.open(payload)
            )
        );
        HANDLERS.registerPacketHandler(
            new NetworkHandler.FromClient<>(C2SHarvestPickPayload.TYPE, C2SHarvestPickPayload.CODEC, HarvestGenes::handlePick)
        );
    }
}
