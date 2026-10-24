package com.human.neoforge.data.loot;

import com.human.Human;
import com.human.neoforge.data.loot.modifier.BastionTreasureLootModifier;
import com.human.neoforge.data.loot.modifier.EarlyGameLootModifier;
import com.human.neoforge.data.loot.modifier.EndGameLootModifier;
import com.human.neoforge.data.loot.modifier.MidGameLootModifier;
import com.human.neoforge.data.loot.modifier.NetherBridgeLootModifier;
import com.human.neoforge.data.loot.modifier.VillageLootModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;

import java.util.concurrent.CompletableFuture;

public class HumanGlobalLootModifierProvider extends GlobalLootModifierProvider {

    public HumanGlobalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Human.MOD_ID);
    }

    @Override
    protected void start() {
        add(VillageLootModifier.NAME, new VillageLootModifier());
        add(EarlyGameLootModifier.NAME, new EarlyGameLootModifier());
        add(MidGameLootModifier.NAME, new MidGameLootModifier());
        add(EndGameLootModifier.NAME, new EndGameLootModifier());
        add(BastionTreasureLootModifier.NAME, new BastionTreasureLootModifier());
        add(NetherBridgeLootModifier.NAME, new NetherBridgeLootModifier());
        add(
            com.human.neoforge.data.loot.modifier.SpiderCobwebLootModifier.NAME,
            new com.human.neoforge.data.loot.modifier.SpiderCobwebLootModifier()
        );
    }
}
