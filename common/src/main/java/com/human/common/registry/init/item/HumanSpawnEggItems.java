package com.human.common.registry.init.item;

import com.blib.api.common.registry.v1.BLibHolder;
import com.blib.api.common.registry.v1.BLibRegistry;
import com.human.Human;
import com.human.common.registry.init.HumanEntityTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class HumanSpawnEggItems {

    public static final BLibRegistry<Item> REGISTRY = Human.MOD.registries().create(BuiltInRegistries.ITEM);

    public static final BLibHolder<SpawnEggItem> MARINE_SPAWN_EGG = create("marine", HumanEntityTypes.MARINE);

    public static final BLibHolder<SpawnEggItem> MARINE_DOG_SPAWN_EGG = create("marine_dog", HumanEntityTypes.MARINE_DOG);

    private static <E extends Mob> BLibHolder<SpawnEggItem> create(String path, BLibHolder<EntityType<E>> holder) {
        return REGISTRY.createHolder(
            path + "_spawn_egg",
            Human.MOD.factories().createSpawnEggSupplier(holder, 0xFFFFFF, 0xFFFFFF, new Item.Properties())
        );
    }

    public static void initialize() {
        REGISTRY.registerAll();
    }
}
