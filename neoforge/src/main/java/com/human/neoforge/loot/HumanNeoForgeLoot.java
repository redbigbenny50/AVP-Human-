package com.human.neoforge.loot;

import com.human.Human;
import com.human.common.data.loot.HumanVanillaLoot;
import com.human.neoforge.mixin.LootPoolEntriesAccessor;
import com.human.neoforge.mixin.LootTablePoolsAccessor;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * avp_human's Oct 24 ruin loot on NeoForge (HumanVanillaLoot; Fabric does the same through its loot event): the M42A3
 * blueprint as a pool of its own in ocean ruin chests, and as one more entry IN the existing pool of the trail ruins'
 * rare archaeology table - brushing keeps only the first item a roll gives, so a separate pool there would never pay
 * out. A failure is logged and skipped, never allowed to break the table's loading.
 */
public final class HumanNeoForgeLoot {

    private HumanNeoForgeLoot() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(HumanNeoForgeLoot::onLoad);
    }

    private static void onLoad(LootTableLoadEvent event) {
        var key = event.getKey();
        var table = event.getTable();
        if (key == null || table == null || table.isFrozen()) {
            return;
        }
        try {
            for (var pool : HumanVanillaLoot.extraPools(key)) {
                table.addPool(pool.build());
            }
            var items = HumanVanillaLoot.extraArchaeologyEntries(key);
            if (!items.isEmpty()) {
                for (var pool : ((LootTablePoolsAccessor) table).avp_human$pools()) {
                    var accessor = (LootPoolEntriesAccessor) pool;
                    List<LootPoolEntryContainer> entries = new ArrayList<>(accessor.avp_human$entries());
                    for (var item : items) {
                        entries.add(LootItem.lootTableItem(item).build());
                    }
                    accessor.avp_human$setEntries(List.copyOf(entries));
                }
            }
        } catch (RuntimeException exception) {
            Human.LOGGER.warn("avp_human: could not add ruin loot to {}", key.location(), exception);
        }
    }
}
