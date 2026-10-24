package com.human.fabric;

import com.human.Human;
import com.human.common.data.loot.LootTableModifiers;
import com.human.fabric.common.worldgen.WorldGen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public class HumanFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // ⚠ BEFORE Human.initialize() is not required - the bridge is consulted at interaction time, long after
        // registration - but installing it first keeps it impossible to forget behind a later early-return.
        com.human.fabric.power.FabricItemEnergyBridge.install();
        // [stated] Sep 30: the FE adapter - cables see other mods' energy blocks.
        com.human.fabric.power.FabricExternalEnergy.install();
        // [stated] Sep 30: fluid pipes reach other mods' fluid blocks.
        com.human.fabric.fluid.FabricPipeFluidAccess.install();
        com.human.fabric.fluid.FabricPipeItemAccess.install();
        com.human.fabric.quarry.FabricQuarryProtection.install();
        com.human.fabric.fluid.FabricFluidConnections.install();

        Human.initialize();

        // After registration: the universal canister as Fabric fluid storage, for other mods' tanks and pipes.
        com.human.fabric.fluid.FabricCanisterFluidStorage.register();
        // [stated] Sep 30: the battery pack and nuclear battery as Team Reborn Energy items.
        com.human.fabric.power.FabricBatteryEnergy.register();
        // Storage crates as item storage, for hoppers and other mods.
        com.human.fabric.storage.FabricCrateItemStorage.register();
        // Fluid endpoints as blocks: the placed canister and all six tanks. Each face's rule comes from the endpoint.
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity(
            com.human.fabric.fluid.FabricFluidEndpointStorage::forSide,
            com.human.common.registry.init.HumanBlockEntityTypes.CANISTER.get()
        );
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity(
            com.human.fabric.fluid.FabricFluidEndpointStorage::forSide,
            com.human.common.registry.init.HumanBlockEntityTypes.FLUID_TANK.get()
        );
        // [stated] Oct 3: the cryotube controller takes cryofluid in through the port on its TOP. It was a fluid
        // endpoint all along but was never registered, so pipes could not see it and would not connect.
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity(
            com.human.fabric.fluid.FabricFluidEndpointStorage::forSide,
            com.human.common.registry.init.HumanBlockEntityTypes.CRYOTUBE_CONTROLLER.get()
        );
        // The machines with tanks: press and hydrator give fluid out of their bottom, the freezer takes it in on its
        // left.
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntities(
            (blockEntity, direction) -> blockEntity instanceof com.human.common.gameplay.fluid.FluidEndpoint endpoint
                ? com.human.fabric.fluid.FabricFluidEndpointStorage.forSide(endpoint, direction)
                : null,
            com.human.common.registry.init.HumanBlockEntityTypes.ELECTRIC_PRESS.get(),
            com.human.common.registry.init.HumanBlockEntityTypes.ELECTRIC_HYDRATOR.get(),
            com.human.common.registry.init.HumanBlockEntityTypes.ELECTRIC_FREEZER.get(),
            // [stated] Oct 17 - the vaporizer takes liquid in through the front of its bottom block.
            com.human.common.registry.init.HumanVaporizer.VAPORIZER_BLOCK_ENTITY.get(),
            // [stated] Oct 17 - the distributor takes liquid in through its TOP.
            com.human.common.registry.init.HumanDistributor.DISTRIBUTOR_BLOCK_ENTITY.get(),
            // ⚠ Oct 21 quality pass - the Creative Fluid Tank was never registered, so pipes (ours, which ask through
            // this same API, and every other mod's) could not see it at all: it could neither feed nor void a line.
            com.human.common.registry.init.HumanCreativeBlocks.CREATIVE_FLUID_TANK_BLOCK_ENTITY.get()
        );
        // Oct 15 - the quarry terminal's LIQUID port: the top of its back-row counter-clockwise block (no block entity
        // there), resolved to the terminal's tank.
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlocks(
            (level, pos, state, blockEntity, side) -> {
                var port = com.human.common.gameplay.quarry.QuarryTerminalBlock.liquidPort(level, pos, state, side);

                return port == null ? null : com.human.fabric.fluid.FabricFluidEndpointStorage.forSide(port, side);
            },
            com.human.common.registry.init.HumanQuarry.QUARRY_TERMINAL.get(),
            com.human.common.registry.init.HumanQuarry.WY_QUARRY_TERMINAL.get(),
            com.human.common.registry.init.HumanQuarry.USCM_QUARRY_TERMINAL.get()
        );

        // Shift + left-click with a canister places it as a block. SUCCESS on the client cancels the mining and still
        // sends the click to the server, where this same callback does the placing.
        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register(
            (player, level, hand, pos, direction) -> com.human.common.gameplay.item.canister.CanisterPlacement.tryPlace(
                player,
                level,
                hand,
                pos,
                direction
            ) ? net.minecraft.world.InteractionResult.SUCCESS : net.minecraft.world.InteractionResult.PASS
        );

        // Core
        WorldGen.initialize();

        // Functionality
        LootTableEvents.MODIFY.register(HumanFabric::applyLootModifiers);
    }

    private static void applyLootModifiers(
        ResourceKey<LootTable> key,
        LootTable.Builder builder,
        LootTableSource source,
        HolderLookup.Provider provider
    ) {
        if (!source.isBuiltin()) {
            return;
        }

        // Early-game loot
        LootTableModifiers.VILLAGE_LOOT_TABLE_MODIFIER.apply(key, builder);
        LootTableModifiers.EARLY_GAME_LOOT_TABLE_MODIFIER.apply(key, builder);

        // Mid-game loot
        LootTableModifiers.MID_GAME_LOOT_TABLE_MODIFIER.apply(key, builder);
        LootTableModifiers.NETHER_BRIDGE_LOOT_TABLE_MODIFIER.apply(key, builder);
        LootTableModifiers.BASTION_TREASURE_LOOT_TABLE_MODIFIER.apply(key, builder);

        // End-game loot
        LootTableModifiers.END_GAME_LOOT_TABLE_MODIFIER.apply(key, builder);

        // [stated] Oct 19 - spiders and cave spiders: a 20% cobweb.
        LootTableModifiers.SPIDER_COBWEB_LOOT_TABLE_MODIFIER.apply(key, builder);
    }
}
