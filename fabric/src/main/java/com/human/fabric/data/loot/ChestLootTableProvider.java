package com.human.fabric.data.loot;

import com.human.common.data.loot.AmmoChestBulletsLootTable;
import com.human.common.data.loot.BarrelBulletMaterialLootTable;
import com.human.common.data.loot.BarrelCasingsLootTable;
import com.human.common.data.loot.ChestRawMaterialLootTable;
import com.human.common.data.loot.CommanderChestPersonalLootTable;
import com.human.common.registry.key.HumanLootTableKeys;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class ChestLootTableProvider extends SimpleFabricLootTableProvider {

    private final HolderLookup.Provider provider;

    public ChestLootTableProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup, LootContextParamSets.CHEST);
        this.provider = registryLookup.join();
    }

    /**
     * ⚠⚠ MARINE_CHEST_PERSONAL IS NO LONGER GENERATED HERE. The structure datapack ships a hand-authored
     * {@code data/avp_human/loot_table/chests/marine_chest_personal.json}, and a generated file at the same path would
     * collide with it - two sources for one resource, with whichever the build happens to copy last winning silently.
     * The static file is the newer of the two, so the generator gives up ownership of it.
     * <p>
     * ⚠ {@code MarineChestPersonalLootTable} still exists and still compiles; it is simply no longer called. Delete it
     * only if you are sure the static table is the one you want to keep.
     * </p>
     */
    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> biConsumer) {
        biConsumer.accept(HumanLootTableKeys.BARREL_CASINGS, BarrelCasingsLootTable.LOOT_TABLE.apply(provider));
        biConsumer.accept(HumanLootTableKeys.BARREL_BULLET_MATERIAL, BarrelBulletMaterialLootTable.LOOT_TABLE.apply(provider));
        biConsumer.accept(HumanLootTableKeys.AMMO_CHEST_BULLETS, AmmoChestBulletsLootTable.LOOT_TABLE.apply(provider));
        biConsumer.accept(HumanLootTableKeys.CHEST_RAW_MATERIAL, ChestRawMaterialLootTable.LOOT_TABLE.apply(provider));
        biConsumer.accept(HumanLootTableKeys.COMMANDER_CHEST_PERSONAL, CommanderChestPersonalLootTable.LOOT_TABLE.apply(provider));
    }
}
