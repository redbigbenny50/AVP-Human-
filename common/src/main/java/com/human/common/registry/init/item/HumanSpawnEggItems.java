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

    public static final BLibHolder<SpawnEggItem> COLONIST_SPAWN_EGG = create("colonist", HumanEntityTypes.COLONIST);

    // [stated] Oct 13 - one egg per soldier type (art supplied; the spec ops eggs recoloured from his commando/elite).
    public static final BLibHolder<SpawnEggItem> TACTICAL_MARINE_SPAWN_EGG = create("tactical_marine", HumanEntityTypes.TACTICAL_MARINE);

    public static final BLibHolder<SpawnEggItem> WY_COMMANDO_SPAWN_EGG = create("wy_commando", HumanEntityTypes.WY_COMMANDO);

    public static final BLibHolder<SpawnEggItem> WY_ELITE_SPAWN_EGG = create("wy_elite", HumanEntityTypes.WY_ELITE);

    public static final BLibHolder<SpawnEggItem> WY_SPEC_OPS_COMMANDO_SPAWN_EGG = create(
        "wy_spec_ops_commando",
        HumanEntityTypes.WY_SPEC_OPS_COMMANDO
    );

    public static final BLibHolder<SpawnEggItem> WY_SPEC_OPS_ELITE_SPAWN_EGG = create(
        "wy_spec_ops_elite",
        HumanEntityTypes.WY_SPEC_OPS_ELITE
    );

    public static final BLibHolder<SpawnEggItem> WY_APE_TROOPER_SPAWN_EGG = create("wy_ape_trooper", HumanEntityTypes.WY_APE_TROOPER);

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
