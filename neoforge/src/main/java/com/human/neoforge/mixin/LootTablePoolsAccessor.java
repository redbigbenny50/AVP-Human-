package com.human.neoforge.mixin;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** A loot table's pools, so HumanNeoForgeLoot can add to them (as Fabric's {@code modifyPools} does). */
@Mixin(LootTable.class)
public interface LootTablePoolsAccessor {

    @Accessor("pools")
    List<LootPool> avp_human$pools();
}
