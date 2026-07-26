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
import com.human.common.registry.init.HumanCommands;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.registry.init.HumanDataSyncKeys;
import com.human.common.registry.init.HumanEntitySpawns;
import com.human.common.registry.init.HumanEntityTypes;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
        CoreBlocks.initialize();
        HumanBlocks.initialize();
        HumanFerroaluminumBlocks.initialize();
        HumanIndustrialConcreteBlocks.initialize();
        HumanIndustrialGlassBlocks.initialize();
        HumanPaddingBlocks.initialize();
        HumanPlasticBlocks.initialize();
        HumanSteelBlocks.initialize();
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
        HumanTitaniumBlockItems.initialize();

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
        MOD.events().onEntityTick().register(Human::applyFullArmorSetBonuses);
        MOD.events().onTagsUpdated().register(($1, $2) -> GeneBonusDataRegistry.rebuildLookupMappings());
        MOD.events().serverStarting().register(HumanCommissaryVillagerHouseInjector::inject);
        MOD.events().serverStarting().register(Human::injectVillagerGifts);
    }

    private static void applyFullArmorSetBonuses(Entity entity) {
        if (entity.level().isClientSide || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        if (BLibEntityPredicates.hasFullArmorSetMatching(livingEntity, itemStack -> itemStack.is(HumanItemTags.WY_APE_ARMOR))) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 5, 0, true, false, true));
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
            PowerSystem.get((ServerLevel) level).tick();
        }
    }
}
