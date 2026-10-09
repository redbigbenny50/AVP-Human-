package com.human.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.human.common.gameplay.explosion.nuke.NukeConversions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Loads {@code data/<namespace>/avp_human/nuke_conversions/*.json} into {@link NukeConversions}, on start-up and on
 * every {@code /reload}. The format is described on {@link NukeConversions}.
 */
public class NukeConversionsReloadListener extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY_NAME = "avp_human/nuke_conversions";

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    public NukeConversionsReloadListener() {
        super(GSON, DIRECTORY_NAME);
    }

    @Override
    protected void apply(
        @NotNull Map<ResourceLocation, JsonElement> files,
        @NotNull ResourceManager resourceManager,
        @NotNull ProfilerFiller profilerFiller
    ) {
        // Name order, so "which file wins" never depends on hash order.
        var sorted = new TreeMap<>(files);
        var parsed = new ArrayList<NukeConversions.FileEntries>();

        for (var entry : sorted.entrySet()) {
            parsed.add(NukeConversions.parse(entry.getKey(), entry.getValue()));
        }

        // One swap: a blast mid-reload sees the old table or the new one, never half of each.
        NukeConversions.install(parsed);
    }
}
