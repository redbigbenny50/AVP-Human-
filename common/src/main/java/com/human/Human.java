package com.human;

import com.blib.api.BLibAPI;
import com.blib.api.common.entity.v1.BLibEntityPredicates;
import com.blib.api.common.mod.v1.BLibMod;
import com.human.common.data.HumanReloadListeners;
import com.human.common.data.fixer.migration.HumanDataMigrations;
import com.human.common.gameplay.entity.living.human.marine.ai.MarineGOAP;
import com.human.common.gameplay.gene.Genes;
import com.human.common.gameplay.level.patrol.impl.ApePatrolSpawnHandle;
import com.human.common.gameplay.level.patrol.impl.MarinePatrolSpawnHandle;
import com.human.common.gameplay.level.patrol.impl.TacticalMarinePatrolSpawnHandle;
import com.human.common.gameplay.level.patrol.impl.WYCPatrolSpawnHandle;
import com.human.common.gameplay.level.patrol.impl.WYEPatrolSpawnHandle;
import com.human.common.gameplay.level.patrol.impl.WYSOCPatrolSpawnHandle;
import com.human.common.gameplay.level.patrol.impl.WYSOEPatrolSpawnHandle;
import com.human.common.gameplay.power.PowerSystem;
import com.human.common.gameplay.worldgen.biome.NukedAshPlacement;
import com.human.common.gameplay.worldgen.structure.HumanCommissaryVillagerHouseInjector;
import com.human.common.network.HumanPacketDirectionRegistry;
import com.human.common.network.HumanServerPacketHandlerRegistry;
import com.human.common.property.HumanPropertyAccess;
import com.human.common.registry.GeneBonusDataRegistry;
import com.human.common.registry.init.HumanArmorMaterials;
import com.human.common.registry.init.HumanBlockEntityTypes;
import com.human.common.registry.init.HumanBlocks;
import com.human.common.registry.init.HumanCauldronInteractions;
import com.human.common.registry.init.HumanCommands;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.HumanDataSyncKeys;
import com.human.common.registry.init.HumanEntitySpawns;
import com.human.common.registry.init.HumanEntityTypes;
import com.human.common.registry.init.HumanFluids;
import com.human.common.registry.init.HumanFuels;
import com.human.common.registry.init.HumanMenuTypes;
import com.human.common.registry.init.HumanMobEffects;
import com.human.common.registry.init.HumanRecipes;
import com.human.common.registry.init.HumanSoundEvents;
import com.human.common.registry.init.HumanStructureTypes;
import com.human.common.registry.init.HumanVillagerPoiTypes;
import com.human.common.registry.init.HumanVillagerProfessions;
import com.human.common.registry.init.HumanVillagerTrades;
import com.human.common.registry.init.block.CoreBlocks;
import com.human.common.registry.init.block.HumanFerroaluminumBlocks;
import com.human.common.registry.init.block.HumanIndustrialConcreteBlocks;
import com.human.common.registry.init.block.HumanIndustrialGlassBlocks;
import com.human.common.registry.init.block.HumanPaddingBlocks;
import com.human.common.registry.init.block.HumanPlasticBlocks;
import com.human.common.registry.init.block.HumanSteelBlocks;
import com.human.common.registry.init.block.HumanTitaniumBlocks;
import com.human.common.registry.init.creative_mode_tab.HumanCreativeModeTabs;
import com.human.common.registry.init.item.HumanArmorItems;
import com.human.common.registry.init.item.HumanBlockItems;
import com.human.common.registry.init.item.HumanGunItems;
import com.human.common.registry.init.item.HumanItems;
import com.human.common.registry.init.item.HumanSpawnEggItems;
import com.human.common.registry.init.item.block.HumanFerroaluminumBlockItems;
import com.human.common.registry.init.item.block.HumanIndustrialConcreteBlockItems;
import com.human.common.registry.init.item.block.HumanIndustrialGlassBlockItems;
import com.human.common.registry.init.item.block.HumanPaddingBlockItems;
import com.human.common.registry.init.item.block.HumanPlasticBlockItems;
import com.human.common.registry.init.item.block.HumanSteelBlockItems;
import com.human.common.registry.init.item.block.HumanTitaniumBlockItems;
import com.human.common.registry.key.HumanVillagerGiftKeys;
import com.human.common.registry.tag.HumanItemTags;
import com.human.mixin.GiveGiftToHeroAccessor;
import com.human.util.NuclearExplosionUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Human {

    public static final String MOD_ID = "avp_human";

    public static final BLibMod MOD = BLibAPI.createMod(MOD_ID);

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final NukedAshPlacement NUKED_ASH_PLACEMENT = new NukedAshPlacement();

    public static void initialize() {
        LOGGER.info("Initializing AVP (Human) for mod loader '{}'", BLibAPI.getModLoaderType());

        HumanPropertyAccess.INSTANCE.save();

        MOD.initialize(Human::runInitialization);
    }

    private static void runInitialization() {
        HumanSoundEvents.initialize();
        HumanArmorMaterials.initialize();
        HumanDataComponents.initialize();
        HumanEntityTypes.initialize();

        // Blocks
        // ⚠ ORDER MATTERS: the latex liquid block's constructor takes the source fluid, so the fluids must exist
        // before any block registry runs.
        HumanFluids.initialize();
        CoreBlocks.initialize();
        HumanBlocks.initialize();
        HumanFerroaluminumBlocks.initialize();
        HumanIndustrialConcreteBlocks.initialize();
        HumanIndustrialGlassBlocks.initialize();
        HumanPaddingBlocks.initialize();
        HumanPlasticBlocks.initialize();
        HumanSteelBlocks.initialize();
        com.human.common.registry.init.block.HumanCompositeBlocks.initialize();
        HumanTitaniumBlocks.initialize();

        HumanBlockEntityTypes.initialize();

        // Items
        HumanItems.initialize();
        HumanGunItems.initialize();
        HumanBlockItems.initialize();
        HumanFerroaluminumBlockItems.initialize();
        HumanIndustrialConcreteBlockItems.initialize();
        HumanIndustrialGlassBlockItems.initialize();
        HumanPaddingBlockItems.initialize();
        HumanPlasticBlockItems.initialize();
        HumanSpawnEggItems.initialize();

        HumanSteelBlockItems.initialize();
        com.human.common.registry.init.item.block.HumanCompositeBlockItems.initialize();
        HumanTitaniumBlockItems.initialize();
        // Storage crates and containers: blocks, items, block entity, menu, item component, tab, packets.
        com.human.common.registry.init.HumanCrates.initialize();
        com.human.common.registry.init.HumanFabrication.initialize();
        // Quarry: the terminal (three editions) and the frame block.
        com.human.common.registry.init.HumanQuarry.initialize();
        // [stated] Oct 17 - the chem line: the xeno acids (fluids, liquid blocks, buckets) and the gas tanks. Fluids
        // first inside it; the vaporizer after it (its logic names the tanks).
        com.human.common.registry.init.HumanChemicals.initialize();
        com.human.common.registry.init.HumanVaporizer.initialize();
        // [stated] Oct 17 - the distributor (fog machine). After the vaporizer: it reads the same liquid-to-vapor
        // table.
        com.human.common.registry.init.HumanDistributor.initialize();
        // [stated] Sep 28 - the USCM base building set (50 blocks, their items, the locker block entity).
        com.human.common.registry.init.block.HumanUscmBlocks.initialize();
        // [stated] Oct 20 - the W-Y building set (51 blocks, their items, the W-Y locker block entity).
        com.human.common.registry.init.block.HumanWyBlocks.initialize();
        // [stated] Oct 20 - the creative test blocks (battery, fluid tank).
        com.human.common.registry.init.HumanCreativeBlocks.initialize();
        com.human.common.registry.init.HumanMetalFixtures.initialize();
        // [stated] Oct 23 - metal signs and hanging signs for the five chain metals.
        com.human.common.registry.init.HumanSigns.initialize();
        com.human.common.registry.init.HumanParticles.initialize();
        // Villagers
        HumanVillagerPoiTypes.initialize();
        HumanVillagerProfessions.initialize();
        HumanVillagerTrades.initialize();

        HumanDataSyncKeys.initialize();

        HumanPacketDirectionRegistry.initialize();
        HumanServerPacketHandlerRegistry.initialize();
        HumanMenuTypes.initialize();
        HumanMobEffects.initialize();

        HumanArmorItems.initialize();
        HumanRecipes.initialize();

        HumanEntitySpawns.initialize();
        HumanFuels.initialize();
        com.human.common.registry.init.HumanCompostables.initialize();

        HumanCreativeModeTabs.initialize();

        HumanCommands.initialize();

        HumanStructureTypes.initialize();

        // AI
        Genes.initialize();
        MarineGOAP.initialize();

        // Listeners / Events
        HumanReloadListeners.initialize();

        HumanDataMigrations.initialize();

        MOD.events().postLevelTick().register(Human::tickMarinePatrolSpawner);
        MOD.events().postLevelTick().register(Human::tickNukeAshPlacement);
        MOD.events().postLevelTick().register(Human::tickPowerSystem);
        MOD.events().postLevelTick().register(NuclearExplosionUtil::resumeUnfinishedExplosions);
        MOD.events().postLevelTick().register(com.human.common.gameplay.freeze.FrozenSolid::tickLevel);
        MOD.events().onEntityTick().register(Human::applyFullArmorSetBonuses);
        MOD.events().onTagsUpdated().register(($1, $2) -> GeneBonusDataRegistry.rebuildLookupMappings());
        // ⚠⚠ COMMON SETUP, NOT runInitialization. BLib's Fabric service DEFERS every registration -
        // BLibFabricRegistryServiceImpl.register calls deferRegistration, and nothing is bound until the container
        // runs them later. So a holder resolved anywhere inside runInitialization is unbound NO MATTER HOW LATE in
        // that method it sits; moving this call after the item registries did not help and could not have:
        // NullPointerException: Trying to access unbound value: ResourceKey[minecraft:item / avp_human:latex_bucket]
        // onCommonSetup fires AFTER the deferred registrations have run (see BLibFabricModContainer), which is the
        // first moment a CauldronInteraction map - keyed by the real Item INSTANCE - can be populated.
        MOD.events().onCommonSetup().register(HumanCauldronInteractions::initialize);

        MOD.events().onServerStarting().register(HumanCommissaryVillagerHouseInjector::inject);
        // The power grids hold block entities, which hold their level: without this every world left in single-player
        // stayed in memory for the rest of the session (PowerSystem's own note asked for exactly this, and nothing did
        // it).
        MOD.events().onServerStopped().register(server -> com.human.common.gameplay.power.PowerSystem.clear());
        MOD.events().onServerStarting().register(Human::injectVillagerGifts);
    }

    private static void applyFullArmorSetBonuses(Entity entity) {
        if (entity.level().isClientSide || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        // ⚠ This runs for EVERY living entity, EVERY tick, and every set below starts with a tag lookup on the helmet.
        // The overwhelming majority of entities in a world - every zombie, cow, alien and item frame - wear no helmet
        // at all, so one emptiness check retires them for the cost of an array read instead of three tag lookups.
        if (livingEntity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            return;
        }

        if (BLibEntityPredicates.hasFullArmorSetMatching(livingEntity, itemStack -> itemStack.is(HumanItemTags.WY_APE_ARMOR))) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 5, 0, true, false, true));
            // The weight of the plate, the same trade the MK50 makes. Five ticks and refreshed every tick, so it
            // lapses on its own the moment a piece comes off rather than needing to be cleared.
            livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 0, true, false, true));
        }

        var supplyAir = false;

        if (BLibEntityPredicates.hasFullArmorSetMatching(livingEntity, itemStack -> itemStack.is(HumanItemTags.MK50_ARMOR))) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 0, true, false, true));
            supplyAir = true;
        } else if (BLibEntityPredicates.hasFullArmorSetMatching(livingEntity, itemStack -> itemStack.is(HumanItemTags.PRESSURE_ARMOR))) {
            supplyAir = true;
        }

        if (supplyAir) {
            var newAirSupply = Math.min(livingEntity.getAirSupply() + 4, livingEntity.getMaxAirSupply());
            livingEntity.setAirSupply(newAirSupply);
        }
    }

    private static void injectVillagerGifts(MinecraftServer minecraftServer) {
        var gifts = GiveGiftToHeroAccessor.getGifts();
        gifts.put(HumanVillagerProfessions.COMMISSARY.get(), HumanVillagerGiftKeys.COMMISSARY_GIFT_LOOT_TABLE);
    }

    private static void tickMarinePatrolSpawner(Level level) {
        if (!level.isClientSide && level.dimension() == Level.OVERWORLD) {
            var serverLevel = (ServerLevel) level;

            MarinePatrolSpawnHandle.INSTANCE.tick(serverLevel);
            TacticalMarinePatrolSpawnHandle.INSTANCE.tick(serverLevel);
            ApePatrolSpawnHandle.INSTANCE.tick(serverLevel);
            WYCPatrolSpawnHandle.INSTANCE.tick(serverLevel);
            WYEPatrolSpawnHandle.INSTANCE.tick(serverLevel);
            WYSOCPatrolSpawnHandle.INSTANCE.tick(serverLevel);
            WYSOEPatrolSpawnHandle.INSTANCE.tick(serverLevel);
        }
    }

    private static void tickNukeAshPlacement(Level level) {
        if (!level.isClientSide) {
            Human.NUKED_ASH_PLACEMENT.tick((ServerLevel) level);
        }
    }

    private static void tickPowerSystem(Level level) {
        if (!level.isClientSide) {
            PowerSystem.get((ServerLevel) level).tick(level);
            // [stated] Sep 30: the fluid pipe networks, after the power grid.
            com.human.common.gameplay.fluid.pipe.FluidPipeSystem.get((ServerLevel) level).tick((ServerLevel) level);
            com.human.common.gameplay.item.pipe.ItemPipeSystem.get((ServerLevel) level).tick((ServerLevel) level);
        }
    }
}
