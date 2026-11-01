package com.human.common.gameplay.gene.reader;

import com.human.common.gameplay.gene.AdvancedGenes;
import com.human.common.gameplay.gene.Genes;
import com.human.common.gameplay.harvest.HarvestGenes;
import com.human.common.model.GeneCarrier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the gene reader screen shows about the last mob sampled, captured on the SERVER when the mob is read and
 * stored on the reader item (in its custom data, under {@value #KEY}) so the client can draw it. A snapshot: the screen
 * shows the mob as it was when read; timers (donor rest, harvest) are stored as the game time they run out, so they
 * keep counting down on screen.
 */
public record GeneReaderSnapshot(
    String name,
    String type,
    boolean modified,
    int generation,
    double strain,
    float health,
    float maxHealth,
    double integrity,
    List<Line> active,
    List<Line> dormant,
    List<Advanced> advanced,
    long donorReadyAt,
    long[] harvestReadyAt,
    double aberrantChance
) {

    public static final String KEY = "avp_human_gene_reader";

    /** One gene value: id, GeneOperationType ordinal, value. */
    public record Line(
        String id,
        int operation,
        double value
    ) {}

    /** One advanced gene's state. */
    public record Advanced(
        String id,
        AdvancedGenes.Status status,
        boolean engineered
    ) {}

    private static final TagKey<EntityType<?>> QUEENS = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("avp_alien", "queens")
    );

    private static final TagKey<EntityType<?>> EMPRESSES = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("avp_alien", "empresses")
    );

    // ---------------------------------------------------------------- capture (server)

    public static GeneReaderSnapshot capture(LivingEntity target) {
        var gameTime = target.level().getGameTime();
        var manager = ((GeneCarrier) target).getOrCreateGeneManager();
        var container = manager.getGeneContainer();
        var activeMap = container.getActiveGeneMap();
        var state = manager.getAdvancedState();

        var advanced = new ArrayList<Advanced>();
        for (var definition : AdvancedGenes.definitions()) {
            var status = state.status(definition.gene());
            var onTheWay = definition.recipe().stream().allMatch(base -> activeMap.getValue(base) > 0);

            if (status != AdvancedGenes.Status.INACTIVE || onTheWay || state.isEngineered(definition.gene())) {
                advanced.add(new Advanced(definition.gene().id().toString(), status, state.isEngineered(definition.gene())));
            }
        }

        var harvest = new long[HarvestGenes.Tool.values().length];
        for (var tool : HarvestGenes.Tool.values()) {
            harvest[tool.ordinal()] = HarvestGenes.carries(container, tool.gene)
                ? gameTime + manager.getHarvestCooldownRemaining(tool)
                : -1L;
        }

        var integrity = activeMap.getValue(Genes.GENETIC_INTEGRITY);
        var aberrant = -1.0;
        if (target.getType().is(QUEENS) || target.getType().is(EMPRESSES)) {
            // Mirrors avp_alien's LayEggAction: stable never, volatile/fatal always, unstable by the fraction.
            var magnitude = Math.abs(integrity);
            aberrant = integrity < -2 ? 1.0 : integrity < -1 ? magnitude - Math.floor(magnitude) : 0.0;
        }

        return new GeneReaderSnapshot(
            target.getName().getString(),
            BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString(),
            container.isModified(),
            container.getGeneration(),
            container.getStrain(),
            target.getHealth(),
            target.getMaxHealth(),
            integrity,
            lines(activeMap.getBackingMap()),
            lines(container.getDormantGeneMap().getBackingMap()),
            advanced,
            gameTime + manager.getSampleCooldownRemaining(),
            harvest,
            aberrant
        );
    }

    private static List<Line> lines(java.util.Map<com.human.common.gameplay.gene.GeneModifierKey, Double> map) {
        var list = new ArrayList<Line>();
        map.forEach((key, value) -> {
            if (value != 0.0) {
                list.add(new Line(key.resourceLocation().toString(), key.operation().ordinal(), value));
            }
        });
        return list;
    }

    // ---------------------------------------------------------------- on the item

    public void writeTo(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(KEY, save()));
    }

    public static @Nullable GeneReaderSnapshot readFrom(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);

        if (data == null) {
            return null;
        }

        var tag = data.copyTag();
        return tag.contains(KEY, Tag.TAG_COMPOUND) ? load(tag.getCompound(KEY)) : null;
    }

    private CompoundTag save() {
        var tag = new CompoundTag();
        tag.putString("name", name);
        tag.putString("type", type);
        tag.putBoolean("modified", modified);
        tag.putInt("generation", generation);
        tag.putDouble("strain", strain);
        tag.putFloat("health", health);
        tag.putFloat("maxHealth", maxHealth);
        tag.putDouble("integrity", integrity);
        tag.put("active", saveLines(active));
        tag.put("dormant", saveLines(dormant));

        var advancedList = new ListTag();
        for (var entry : advanced) {
            var row = new CompoundTag();
            row.putString("id", entry.id());
            row.putInt("status", entry.status().ordinal());
            row.putBoolean("engineered", entry.engineered());
            advancedList.add(row);
        }
        tag.put("advanced", advancedList);

        tag.putLong("donorReadyAt", donorReadyAt);
        tag.putLongArray("harvestReadyAt", harvestReadyAt);
        tag.putDouble("aberrant", aberrantChance);
        return tag;
    }

    private static GeneReaderSnapshot load(CompoundTag tag) {
        var advanced = new ArrayList<Advanced>();
        var statuses = AdvancedGenes.Status.values();
        for (var element : tag.getList("advanced", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) element;
            var status = row.getInt("status");
            advanced.add(
                new Advanced(
                    row.getString("id"),
                    statuses[Math.clamp(status, 0, statuses.length - 1)],
                    row.getBoolean("engineered")
                )
            );
        }

        var harvest = tag.getLongArray("harvestReadyAt");
        if (harvest.length != HarvestGenes.Tool.values().length) {
            harvest = java.util.Arrays.copyOf(harvest, HarvestGenes.Tool.values().length);
        }

        return new GeneReaderSnapshot(
            tag.getString("name"),
            tag.getString("type"),
            tag.getBoolean("modified"),
            tag.getInt("generation"),
            tag.getDouble("strain"),
            tag.getFloat("health"),
            tag.getFloat("maxHealth"),
            tag.getDouble("integrity"),
            loadLines(tag.getList("active", Tag.TAG_COMPOUND)),
            loadLines(tag.getList("dormant", Tag.TAG_COMPOUND)),
            advanced,
            tag.getLong("donorReadyAt"),
            harvest,
            tag.contains("aberrant") ? tag.getDouble("aberrant") : -1.0
        );
    }

    private static ListTag saveLines(List<Line> lines) {
        var list = new ListTag();
        for (var line : lines) {
            var row = new CompoundTag();
            row.putString("id", line.id());
            row.putInt("op", line.operation());
            row.putDouble("value", line.value());
            list.add(row);
        }
        return list;
    }

    private static List<Line> loadLines(ListTag list) {
        var lines = new ArrayList<Line>();
        for (var element : list) {
            var row = (CompoundTag) element;
            lines.add(new Line(row.getString("id"), row.getInt("op"), row.getDouble("value")));
        }
        return lines;
    }
}
