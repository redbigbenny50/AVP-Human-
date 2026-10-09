package com.human.client.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Every colonist jumpsuit texture present, per sex - [stated] Sep 30, colonists "randomly spawn with" one of a few.
 * Listed from the loaded resources, so a new texture in {@code textures/entity/colonist/<male|female>/outfit/} joins
 * the pool with no code; sorted, so every client orders them the same and a colonist looks the same to everyone. Listed
 * again after a resource reload.
 */
public final class ColonistOutfits {

    private static List<ResourceLocation> male = List.of();

    private static List<ResourceLocation> female = List.of();

    private static Object listedFor;

    private ColonistOutfits() {}

    public static synchronized List<ResourceLocation> forSex(boolean isMale) {
        var manager = Minecraft.getInstance().getResourceManager();

        if (listedFor != manager) {
            male = list(manager, "male");
            female = list(manager, "female");
            listedFor = manager;
        }

        return isMale ? male : female;
    }

    private static List<ResourceLocation> list(net.minecraft.server.packs.resources.ResourceManager manager, String sex) {
        var found = new ArrayList<ResourceLocation>();

        for (
            var location : manager.listResources("textures/entity/colonist/" + sex + "/outfit", path -> path.getPath().endsWith(".png"))
                .keySet()
        ) {
            if (location.getNamespace().equals(com.human.Human.MOD_ID)) {
                found.add(location);
            }
        }

        found.sort(Comparator.comparing(ResourceLocation::toString));

        return List.copyOf(found);
    }
}
