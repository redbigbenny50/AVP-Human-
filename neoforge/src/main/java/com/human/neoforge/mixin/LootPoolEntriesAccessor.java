package com.human.neoforge.mixin;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Lets avp_human add entries to an existing loot pool on NeoForge, the way Fabric's {@code modifyPools} does - see
 * HumanNeoForgeLoot. The pool reads {@code entries} fresh on every roll, so a longer list is all it takes.
 */
@Mixin(LootPool.class)
public interface LootPoolEntriesAccessor {

    @Accessor("entries")
    List<LootPoolEntryContainer> avp_human$entries();

    @Mutable
    @Accessor("entries")
    void avp_human$setEntries(List<LootPoolEntryContainer> entries);
}
