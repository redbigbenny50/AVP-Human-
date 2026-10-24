package com.human.common.data.loot;

import com.human.common.registry.init.item.HumanItems;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;

public class LootTableModifiers {

    public static final BLibLootModifier VILLAGE_LOOT_TABLE_MODIFIER = BLibLootModifier.builder()
        .addInjectionTargets(
            BuiltInLootTables.VILLAGE_WEAPONSMITH,
            BuiltInLootTables.VILLAGE_TOOLSMITH,
            BuiltInLootTables.VILLAGE_ARMORER,
            BuiltInLootTables.VILLAGE_CARTOGRAPHER,
            BuiltInLootTables.VILLAGE_MASON,
            BuiltInLootTables.VILLAGE_SHEPHERD,
            BuiltInLootTables.VILLAGE_BUTCHER,
            BuiltInLootTables.VILLAGE_FLETCHER,
            BuiltInLootTables.VILLAGE_FISHER,
            BuiltInLootTables.VILLAGE_TANNERY,
            BuiltInLootTables.VILLAGE_TEMPLE,
            BuiltInLootTables.VILLAGE_DESERT_HOUSE,
            BuiltInLootTables.VILLAGE_PLAINS_HOUSE,
            BuiltInLootTables.VILLAGE_TAIGA_HOUSE,
            BuiltInLootTables.VILLAGE_SNOWY_HOUSE,
            BuiltInLootTables.VILLAGE_SAVANNA_HOUSE
        )
        .addPool(() -> {
            var pistolWeight = 20;

            return LootPool.lootPool()
                .add(EmptyLootItem.emptyItem().setWeight(100 - pistolWeight))
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_M88MOD4_COMBAT_PISTOL.get())
                        .setWeight(pistolWeight)
                );
        })
        .build();

    public static final BLibLootModifier EARLY_GAME_LOOT_TABLE_MODIFIER = BLibLootModifier.builder()
        .addInjectionTargets(
            BuiltInLootTables.DESERT_PYRAMID,
            BuiltInLootTables.JUNGLE_TEMPLE,
            BuiltInLootTables.PILLAGER_OUTPOST,
            BuiltInLootTables.SIMPLE_DUNGEON
        )
        .addPool(
            LootPool.lootPool()
                .add(EmptyLootItem.emptyItem().setWeight(50))
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_M88MOD4_COMBAT_PISTOL.get())
                        .setWeight(10)
                )
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_M37_12_SHOTGUN.get())
                        .setWeight(20)
                )
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_ZX_76_SHOTGUN.get())
                        .setWeight(20)
                )
        )
        .build();

    public static final BLibLootModifier MID_GAME_LOOT_TABLE_MODIFIER = BLibLootModifier.builder()
        .addInjectionTargets(
            BuiltInLootTables.ANCIENT_CITY,
            BuiltInLootTables.ABANDONED_MINESHAFT,
            BuiltInLootTables.STRONGHOLD_CROSSING,
            BuiltInLootTables.STRONGHOLD_CORRIDOR,
            BuiltInLootTables.STRONGHOLD_LIBRARY,
            BuiltInLootTables.WOODLAND_MANSION
        )
        .addPool(
            LootPool.lootPool()
                .add(EmptyLootItem.emptyItem().setWeight(50))
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_M88MOD4_COMBAT_PISTOL.get())
                        .setWeight(4)
                )
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_M37_12_SHOTGUN.get())
                        .setWeight(8)
                )
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_ZX_76_SHOTGUN.get())
                        .setWeight(8)
                )
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_F903WE_RIFLE.get())
                        .setWeight(15)
                )
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_M4RA_BATTLE_RIFLE.get())
                        .setWeight(15)
                )
        )
        .build();

    public static final BLibLootModifier END_GAME_LOOT_TABLE_MODIFIER = BLibLootModifier.builder()
        .addInjectionTarget(BuiltInLootTables.END_CITY_TREASURE)
        .addPool(
            LootPool.lootPool()
                .add(EmptyLootItem.emptyItem().setWeight(50))
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_M41A_PULSE_RIFLE.get())
                        .setWeight(15)
                )
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_M42A3_SNIPER_RIFLE.get())
                        .setWeight(15)
                )
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_M6B_ROCKET_LAUNCHER.get())
                        .setWeight(10)
                )
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_M56_SMARTGUN.get())
                        .setWeight(5)
                )
                .add(
                    LootItem.lootTableItem(HumanItems.BLUEPRINT_OLD_PAINLESS.get())
                        .setWeight(5)
                )
        )
        .build();

    public static final BLibLootModifier BASTION_TREASURE_LOOT_TABLE_MODIFIER = BLibLootModifier.builder()
        .addInjectionTarget(BuiltInLootTables.BASTION_TREASURE)
        .addPool(
            LootPool.lootPool()
                .add(LootItem.lootTableItem(HumanItems.BLUEPRINT_FLAMETHROWER_SEVASTOPOL.get()))
        )
        .build();

    public static final BLibLootModifier NETHER_BRIDGE_LOOT_TABLE_MODIFIER = BLibLootModifier.builder()
        .addInjectionTarget(BuiltInLootTables.NETHER_BRIDGE)
        .addPool(
            LootPool.lootPool()
                .add(EmptyLootItem.emptyItem().setWeight(66))
                .add(LootItem.lootTableItem(HumanItems.BLUEPRINT_FLAMETHROWER_SEVASTOPOL.get()).setWeight(33))
        )
        .build();

    /**
     * [stated] Oct 19 - "Killing spiders and poison spiders has a 20% chance to drop a cob web". Spider and cave
     * spider: one cobweb, one time in five, alongside their usual string and eyes.
     */
    public static final BLibLootModifier SPIDER_COBWEB_LOOT_TABLE_MODIFIER = BLibLootModifier.builder()
        .addInjectionTargets(
            net.minecraft.world.entity.EntityType.SPIDER.getDefaultLootTable(),
            net.minecraft.world.entity.EntityType.CAVE_SPIDER.getDefaultLootTable()
        )
        .addPool(
            LootPool.lootPool()
                .add(EmptyLootItem.emptyItem().setWeight(80))
                .add(LootItem.lootTableItem(net.minecraft.world.item.Items.COBWEB).setWeight(20))
        )
        .build();
}
