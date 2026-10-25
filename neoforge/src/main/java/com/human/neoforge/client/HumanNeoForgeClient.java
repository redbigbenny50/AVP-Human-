package com.human.neoforge.client;

import com.human.Human;
import com.human.client.HumanClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = Human.MOD_ID, dist = Dist.CLIENT)
public class HumanNeoForgeClient {

    public HumanNeoForgeClient(net.neoforged.bus.api.IEventBus modBus) {
        HumanClient.initialize();
        NeoForgeFluidClientInfo.install();

        // Oct 24 - the Create converters' turning gears. Only with Create installed, like the blocks themselves.
        if (net.neoforged.fml.ModList.get().isLoaded("create")) {
            com.human.neoforge.client.compat.create.HumanCreateClient.register(modBus);
        }
    }
}
