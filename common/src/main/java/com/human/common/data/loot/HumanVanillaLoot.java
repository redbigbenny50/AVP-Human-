package com.human.common.data.loot;

import com.human.common.registry.init.item.HumanItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;

/**
 * The M42A3 sniper rifle blueprint in RUINS, in one place for both loaders.
 * <p>
 * [stated] Oct 24 - "the M42A3 should also be found in ruins", then "do ruins have chests? if not add the one gun
 * blueprint they should have to suspicious sand and gravel ruins have." Vanilla has two kinds of ruins:
 * </p>
 * <ul>
 * <li><b>Ocean ruins have chests</b> - the blueprint goes in them as a pool of its own: a {@value #BIG_RUIN_CHANCE}
 * chance in a big ruin's chest, {@value #SMALL_RUIN_CHANCE} in a small one's.</li>
 * <li><b>Trail ruins have no chests</b> - only suspicious gravel - so the blueprint is one more entry in their RARE
 * archaeology pool (twelve weight-1 entries in vanilla): about 1 in 13 rare finds. ⚠ It must be an entry IN the
 * existing pool, never a pool of its own: brushing keeps only the first item a roll gives, so an extra pool's item
 * would never come out (and would log a warning every time).</li>
 * </ul>
 */
public final class HumanVanillaLoot {

    public static final float BIG_RUIN_CHANCE = 0.10F;

    public static final float SMALL_RUIN_CHANCE = 0.05F;

    private HumanVanillaLoot() {}

    /** Extra pools for chest tables. */
    public static List<LootPool.Builder> extraPools(ResourceKey<LootTable> key) {
        if (key.equals(BuiltInLootTables.UNDERWATER_RUIN_BIG)) {
            return List.of(blueprint(BIG_RUIN_CHANCE));
        }
        if (key.equals(BuiltInLootTables.UNDERWATER_RUIN_SMALL)) {
            return List.of(blueprint(SMALL_RUIN_CHANCE));
        }
        return List.of();
    }

    /** Extra weight-1 entries for the EXISTING pools of archaeology tables. */
    public static List<Item> extraArchaeologyEntries(ResourceKey<LootTable> key) {
        if (key.equals(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE)) {
            return List.of(HumanItems.BLUEPRINT_M42A3_SNIPER_RIFLE.get());
        }
        return List.of();
    }

    private static LootPool.Builder blueprint(float chance) {
        return LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1))
            .when(LootItemRandomChanceCondition.randomChance(chance))
            .add(LootItem.lootTableItem(HumanItems.BLUEPRINT_M42A3_SNIPER_RIFLE.get()));
    }
}
