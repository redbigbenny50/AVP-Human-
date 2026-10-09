package com.human.neoforge;

import com.human.Human;
import com.human.neoforge.data.loot.modifier.BastionTreasureLootModifier;
import com.human.neoforge.data.loot.modifier.EarlyGameLootModifier;
import com.human.neoforge.data.loot.modifier.EndGameLootModifier;
import com.human.neoforge.data.loot.modifier.MidGameLootModifier;
import com.human.neoforge.data.loot.modifier.NetherBridgeLootModifier;
import com.human.neoforge.data.loot.modifier.VillageLootModifier;
import com.human.neoforge.fluid.NeoForgeLatexFluid;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

@Mod(Human.MOD_ID)
public class HumanNeoForge {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Human.MOD_ID);

    public static final Supplier<MapCodec<VillageLootModifier>> VILLAGE_LOOT_MODIFIER =
        GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(VillageLootModifier.NAME, () -> VillageLootModifier.CODEC);

    public static final Supplier<MapCodec<EarlyGameLootModifier>> EARLY_GAME_LOOT_MODIFIER =
        GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(EarlyGameLootModifier.NAME, () -> EarlyGameLootModifier.CODEC);

    public static final Supplier<MapCodec<MidGameLootModifier>> MID_GAME_LOOT_MODIFIER =
        GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(MidGameLootModifier.NAME, () -> MidGameLootModifier.CODEC);

    public static final Supplier<MapCodec<EndGameLootModifier>> END_GAME_LOOT_MODIFIER =
        GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(EndGameLootModifier.NAME, () -> EndGameLootModifier.CODEC);

    public static final Supplier<MapCodec<BastionTreasureLootModifier>> BASTION_TREASURE_LOOT_MODIFIER =
        GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(BastionTreasureLootModifier.NAME, () -> BastionTreasureLootModifier.CODEC);

    public static final Supplier<MapCodec<NetherBridgeLootModifier>> NETHER_BRIDGE_LOOT_MODIFIER =
        GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(NetherBridgeLootModifier.NAME, () -> NetherBridgeLootModifier.CODEC);

    public HumanNeoForge(IEventBus modBus) {
        GLOBAL_LOOT_MODIFIER_SERIALIZERS.register(modBus);

        // ⚠⚠ BOTH OF THESE MUST HAPPEN BEFORE Human.initialize(). BLib stores a holder's supplier and only calls it
        // during registerAll(), which runs inside initialize() - so the factory has to be installed first or the
        // plain common fluid gets registered with no FluidType, leaving latex invisible on NeoForge while it works
        // fine on Fabric.
        NeoForgeLatexFluid.register(modBus);
        NeoForgeLatexFluid.installFactory();

        // ⚠ No ordering constraint here, unlike the fluid factory - the bridge is consulted at interaction time, long
        // after registration. It just has to be installed at all, or the charging pad silently transfers nothing.
        com.human.neoforge.power.NeoForgeItemEnergyBridge.install();
        // [stated] Sep 30: the FE adapter - cables see other mods' energy blocks.
        com.human.neoforge.power.NeoForgeExternalEnergy.install();
        // [stated] Sep 30: fluid pipes reach other mods' fluid blocks.
        com.human.neoforge.fluid.NeoForgePipeFluidAccess.install();
        com.human.neoforge.fluid.NeoForgePipeItemAccess.install();
        com.human.neoforge.fluid.NeoForgeFluidConnections.install();
        com.human.neoforge.fluid.NeoForgePortRefresh.install();

        // The universal canister as a NeoForge fluid container, so other mods' tanks and pipes can fill and drain it.
        modBus.addListener(com.human.neoforge.fluid.NeoForgeCanisterFluidHandler::register);
        // [stated] Sep 30: the battery pack and nuclear battery as energy items.
        modBus.addListener(com.human.neoforge.power.NeoForgeBatteryEnergy::register);

        // Shift + left-click with a canister places it as a block. Fires on client and server; cancelling on the client
        // stops the mining but still sends the click, and the server-side call does the placing.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
            (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) -> {
                if (
                    event.getAction() == net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START
                        && event.getFace() != null
                        && com.human.common.gameplay.item.canister.CanisterPlacement.tryPlace(
                            event.getEntity(),
                            event.getLevel(),
                            event.getHand(),
                            event.getPos(),
                            event.getFace()
                        )
                ) {
                    event.setCanceled(true);
                }
            }
        );

        Human.initialize();
    }
}
