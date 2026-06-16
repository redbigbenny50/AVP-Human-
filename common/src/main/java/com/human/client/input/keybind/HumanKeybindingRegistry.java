package com.human.client.input.keybind;

import com.blib.api.client.input.v1.model.KeyInteractType;
import com.human.Human;
import com.human.client.HumanClient;
import com.human.common.model.Crawler;
import com.human.common.network.packet.C2SGunReloadPayload;
import com.human.common.network.packet.C2SPlayerToggleCrawlPayload;
import com.just.core.functional.tuple.Tuple2;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class HumanKeybindingRegistry {

    public static final Supplier<Tuple2<KeyMapping, Consumer<KeyInteractType>>> CRAWL = register(
        "crawl",
        "movement",
        GLFW.GLFW_KEY_LEFT_ALT,
        keyMapping -> {
            var player = Minecraft.getInstance().player;

            if (player != null) {
                var crawler = (Crawler) player;
                var shouldCrawl = keyMapping == KeyInteractType.PRESS;
                crawler.setCrawling(shouldCrawl);
                Human.MOD.networking().sendToServer(new C2SPlayerToggleCrawlPayload(shouldCrawl));
            }
        }
    );

    public static final Supplier<Tuple2<KeyMapping, Consumer<KeyInteractType>>> RELOAD = register(
        "reload",
        "weapons",
        GLFW.GLFW_KEY_R,
        keyMapping -> {
            var player = Minecraft.getInstance().player;

            if (player != null) {
                Human.MOD.networking().sendToServer(C2SGunReloadPayload.INSTANCE);
            }
        }
    );

    private static Supplier<Tuple2<KeyMapping, Consumer<KeyInteractType>>> register(
        String path,
        String category,
        int key,
        Consumer<KeyInteractType> onKeyMappingActivated
    ) {
        return HumanClient.MOD.registries()
            .registerKeyMapping(
                Human.MOD.resources().createLocation(path),
                category,
                key,
                onKeyMappingActivated
            );
    }

    public static void initialize() {}
}
