package com.human.common.data.loot;

import com.blib.api.common.loot.v1.condition.item.BLibLootItemModLoadedCondition;
import com.human.common.registry.init.item.HumanItems;
import com.human.common.registry.tag.HumanItemTags;
import com.human.compatibility.avp_alien.AVPAlien;
import com.human.compatibility.avp_predator.AVPPredator;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.Function;

/**
 * ⚠⚠ THE SIBLING MODS' ITEMS ARE REACHED THROUGH A TAG, NOT BY NAME, AND THAT IS NOT A STYLE CHOICE.
 * <p>
 * {@code LootItem} parses its {@code name} through {@code BuiltInRegistries.ITEM.holderByNameCodec()}, which returns
 * "Unknown registry key" for an id that is not registered. That error propagates up through the pool list and fails
 * {@code LootTable.DIRECT_CODEC}, so ONE unknown id drops the ENTIRE TABLE at load - not just that entry, not just that
 * pool. avp_alien and avp_predator are OPTIONAL dependencies, so naming their items directly meant this table silently
 * ceased to exist for anyone running avp_human without them.
 * </p>
 * <p>
 * ⚠⚠ {@code BLibLootItemModLoadedCondition} DOES NOT PROTECT AGAINST THIS, and it reads as though it should. Verified
 * in blib-common 0.3.7-fork: it is a plain {@code LootItemCondition} whose {@code test} calls
 * {@code BLibAPI.isModLoaded} at LOOT-GENERATION time. It has no custom {@code DynamicOps} and does no decode-time
 * stripping the way NeoForge's ConditionalOps does, so the pool - entries and all - is fully decoded long before any
 * condition is ever asked.
 * </p>
 * <p>
 * ⭐ THE CONDITION IS KEPT ANYWAY, deliberately. The tag makes the table PARSE; the condition makes the pool not roll at
 * all when the mod is absent, so its weight is not quietly wasted on an entry that can produce nothing. They solve two
 * different halves.
 * </p>
 * <p>
 * ⚠ TAG MEMBERS ARE OPTIONAL ({@code required: false}) - see HumanItemTagProvider. An absent mod leaves the tag empty,
 * and an empty tag yields no stack. ⚠ Do NOT switch these to {@code TagEntry.expandTag}: expanded entries are built as
 * bare EntryBase instances that never call {@code decorate}, so the set-count function would be silently dropped.
 * </p>
 */
public class ChestRawMaterialLootTable {

    public static final Function<HolderLookup.Provider, LootTable.Builder> LOOT_TABLE = provider -> LootTable.lootTable()
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(
                    LootItem.lootTableItem(HumanItems.RAW_ZINC.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4)))
                        .setWeight(50)
                )
        )
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(
                    LootItem.lootTableItem(Items.COPPER_ORE)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4)))
                        .setWeight(50)
                )
        )
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(
                    LootItem.lootTableItem(Items.RAW_COPPER)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 8)))
                        .setWeight(40)
                )
        )
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(
                    LootItem.lootTableItem(HumanItems.RAW_ZINC.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 8)))
                        .setWeight(40)
                )
        )
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(
                    LootItem.lootTableItem(HumanItems.RAW_BRASS.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6)))
                        .setWeight(25)
                )
        )
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(
                    LootItem.lootTableItem(HumanItems.RAW_MONAZITE.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 6)))
                        .setWeight(5)
                )
        )
        .withPool(
            LootPool.lootPool()
                .when(BLibLootItemModLoadedCondition.isModLoaded(AVPAlien.MOD.id()))
                .setRolls(ConstantValue.exactly(1))
                .add(
                    TagEntry.tagContents(HumanItemTags.ALIEN_MUSIC_DISC_FRAGMENTS)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4)))
                        .setWeight(5)
                )
        )
        .withPool(
            LootPool.lootPool()
                .when(BLibLootItemModLoadedCondition.isModLoaded(AVPPredator.MOD.id()))
                .setRolls(ConstantValue.exactly(1))
                .add(
                    TagEntry.tagContents(HumanItemTags.PREDATOR_MUSIC_DISC_FRAGMENTS)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4)))
                        .setWeight(5)
                )
        );
}
