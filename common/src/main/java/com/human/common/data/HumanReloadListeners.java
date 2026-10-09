package com.human.common.data;

import com.blib.api.common.registry.v1.impl.BLibReloadListenerRegistry;
import com.human.Human;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public class HumanReloadListeners {

    private static final BLibReloadListenerRegistry REGISTRY = Human.MOD.registries().createReloadListenerRegistry();

    public static final PreparableReloadListener GENE_BONUS_DATA_RELOAD_LISTENER = new GeneBonusDataReloadListener();

    public static final PreparableReloadListener NUKE_CONVERSIONS_RELOAD_LISTENER = new NukeConversionsReloadListener();

    public static void initialize() {
        REGISTRY.register(GeneBonusDataReloadListener.DIRECTORY_NAME, GENE_BONUS_DATA_RELOAD_LISTENER, PackType.SERVER_DATA);
        REGISTRY.register(NukeConversionsReloadListener.DIRECTORY_NAME, NUKE_CONVERSIONS_RELOAD_LISTENER, PackType.SERVER_DATA);

        // [stated] Oct 2: sentry skins live in resource packs beside their textures. Client only - registered the way
        // BLib registers its own client listeners, behind the distribution check, so a dedicated server never loads
        // the client class.
        if (com.blib.api.BLibAPI.getDistributionType() == com.blib.api.common.mod.v1.model.DistributionEnvironmentType.CLIENT) {
            REGISTRY.register("sentry_skins", com.human.client.skin.SentrySkins.INSTANCE, PackType.CLIENT_RESOURCES);
        }
    }
}
