package com.human.client.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.human.Human;
import com.human.common.data.loot.BLibLootModifier;
import com.human.common.data.loot.LootTableModifiers;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where every blueprint can be found, for JEI and REI.
 * <p>
 * [stated] Oct 24 - "Make sure rei and jei can properly show where to find blueprints whatever hooks they need or
 * identifiers to see the loot tables". Recipe viewers cannot see loot tables on their own: loot is server data the
 * client never receives, and four of avp_human's blueprints are not in any loot table FILE at all - they are injected
 * into vanilla chests by code (LootTableModifiers). So this reads both, on the client, the same way every time:
 * </p>
 * <ul>
 * <li><b>Our structure chests</b> - read straight from the mod jar's own loot table files (the client has the jar),
 * walking pools, weights, chances and nested loot tables. Every table any avp_human structure fills is listed below, so
 * a blueprint added to any of them shows up without touching this class.</li>
 * <li><b>Vanilla chests</b> - the loot modifiers' own pools, turned into the same JSON through the game's loot codec,
 * so the weights shown are exactly the ones the game rolls.</li>
 * </ul>
 * <p>
 * Each source is shown with the chance of at least one of each item per chest opened. Collected: every blueprint
 * ({@code blueprint_...}) and, since Oct 24, the music disc fragments ([stated] "both music disc fragments, alien and
 * predator ... these are in loot tables") - which our chests give through item TAGS
 * ({@code avp_human:loot/alien_music_disc_fragments}), so tag entries are expanded too, through this client's synced
 * tags. ⚠ A server's data pack can override a loot table; the viewers show the mod's own tables.
 * </p>
 */
public final class BlueprintSources {

    /** One place to look, and the blueprints found there with their chance per chest (0-1). */
    public record Source(
        Component name,
        ItemStack icon,
        Map<Item, Float> blueprints
    ) {}

    /** Every chest loot table an avp_human structure uses, with where players meet it. */
    private static final Map<String, String> STRUCTURE_CHESTS = new LinkedHashMap<>();

    static {
        STRUCTURE_CHESTS.put("chests/commander_chest_comm", "Comms Outpost - commander's chest");
        STRUCTURE_CHESTS.put("chests/commander_chest_munition", "Munitions Outpost - commander's chest");
        STRUCTURE_CHESTS.put("chests/commander_chest_supply", "Supply Outpost - commander's chest");
        STRUCTURE_CHESTS.put("chests/marine_chest_campspecial", "Marine Camp & Comms Outpost - special chest");
        STRUCTURE_CHESTS.put("chests/marine_chest_personal", "Marine Camp & Outposts - personal chest");
        STRUCTURE_CHESTS.put("chests/mining_camp/foreman_chest_swamp", "Swamp Mining Camp - foreman's chest");
        STRUCTURE_CHESTS.put("chests/mining_camp/foreman_chest_jungle", "Jungle Mining Camp - foreman's chest");
        STRUCTURE_CHESTS.put("chests/mining_camp/personal_chest_swamp", "Swamp Mining Camp - personal chest");
        STRUCTURE_CHESTS.put("chests/mining_camp/personal_chest_jungle", "Jungle Mining Camp - personal chest");
        STRUCTURE_CHESTS.put("chests/supplies", "Supply Outposts & Desert Commissary - supply chest");
        STRUCTURE_CHESTS.put("chests/lead_chest_radioactive", "Supply Outposts - lead chest");
        STRUCTURE_CHESTS.put("chests/ammo_chest_small", "Camps & Outposts - small ammo chest");
        STRUCTURE_CHESTS.put("chests/ammo_chest_medium", "Outposts - medium ammo chest");
        STRUCTURE_CHESTS.put("chests/ammo_chest_heavy", "Outposts - heavy ammo chest");
        STRUCTURE_CHESTS.put("chests/ammo_chest_shotgun", "Outposts - shotgun ammo chest");
        STRUCTURE_CHESTS.put("chests/ammo_chest_caseless", "Outposts - caseless ammo chest");
        STRUCTURE_CHESTS.put("chests/ammo_chest_fuel", "Outposts - fuel chest");
        STRUCTURE_CHESTS.put("chests/ammo_chest_rocket", "Comms Outpost - rocket chest");
        STRUCTURE_CHESTS.put("chests/munition_caseless", "Munitions Outpost - caseless munitions");
        STRUCTURE_CHESTS.put("chests/munition_shell", "Munitions Outpost - shell munitions");
        STRUCTURE_CHESTS.put("chests/book/mobile_lab_human", "Mobile Lab - chest");
    }

    /** How deep nested loot tables are followed. */
    private static final int MAX_NESTING = 4;

    private BlueprintSources() {}

    /** Every source holding at least one blueprint. Built fresh each call (once per viewer reload). */
    public static List<Source> all() {
        var sources = new ArrayList<Source>();

        for (var entry : STRUCTURE_CHESTS.entrySet()) {
            try {
                var json = readOwnTable(entry.getKey());
                if (json != null) {
                    add(sources, Component.literal(entry.getValue()), new ItemStack(Items.CHEST), json);
                }
            } catch (RuntimeException exception) {
                Human.LOGGER.warn("Blueprint sources: skipped {}", entry.getKey(), exception);
            }
        }

        addModifier(sources, "Village chests", LootTableModifiers.VILLAGE_LOOT_TABLE_MODIFIER);
        addModifier(
            sources,
            "Desert pyramids, jungle temples, pillager outposts & dungeons",
            LootTableModifiers.EARLY_GAME_LOOT_TABLE_MODIFIER
        );
        addModifier(
            sources,
            "Ancient cities, mineshafts, strongholds & woodland mansions",
            LootTableModifiers.MID_GAME_LOOT_TABLE_MODIFIER
        );
        addModifier(sources, "End city treasure", LootTableModifiers.END_GAME_LOOT_TABLE_MODIFIER);
        addModifier(sources, "Bastion treasure", LootTableModifiers.BASTION_TREASURE_LOOT_TABLE_MODIFIER);
        addModifier(sources, "Nether fortresses", LootTableModifiers.NETHER_BRIDGE_LOOT_TABLE_MODIFIER);

        // Oct 24 - the M42A3 in ruins (HumanVanillaLoot): ocean ruin chests, and trail ruins' rare suspicious gravel.
        addPools(sources, "Ocean ruins - big ruin chest", net.minecraft.world.level.storage.loot.BuiltInLootTables.UNDERWATER_RUIN_BIG);
        addPools(sources, "Ocean ruins - small ruin chest", net.minecraft.world.level.storage.loot.BuiltInLootTables.UNDERWATER_RUIN_SMALL);
        try {
            var found = new LinkedHashMap<Item, Float>();
            for (
                var item : com.human.common.data.loot.HumanVanillaLoot.extraArchaeologyEntries(
                    net.minecraft.world.level.storage.loot.BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE
                )
            ) {
                // One weight-1 entry beside vanilla's twelve: 1 in 13 of the rare finds.
                found.put(item, 1.0F / 13.0F);
            }
            if (!found.isEmpty()) {
                sources.add(
                    new Source(Component.literal("Trail ruins - rare suspicious gravel (brush)"), new ItemStack(Items.BRUSH), found)
                );
            }
        } catch (RuntimeException exception) {
            Human.LOGGER.warn("Blueprint sources: skipped the trail ruins page", exception);
        }

        return sources;
    }

    private static void add(List<Source> sources, Component name, ItemStack icon, JsonObject table) {
        var blueprints = new LinkedHashMap<Item, Float>();
        collectTable(table, blueprints, 0);
        if (!blueprints.isEmpty()) {
            sources.add(new Source(name, icon, blueprints));
        }
    }

    private static void addModifier(List<Source> sources, String name, BLibLootModifier modifier) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        try {
            var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
            var encoded = LootTable.DIRECT_CODEC.encodeStart(ops, modifier.getLootTable()).result();
            if (encoded.isPresent() && encoded.get().isJsonObject()) {
                add(sources, Component.literal(name), new ItemStack(Items.CHEST), encoded.get().getAsJsonObject());
            }
        } catch (RuntimeException exception) {
            Human.LOGGER.warn("Blueprint sources: could not read the '{}' loot modifier", name, exception);
        }
    }

    /** A page for the extra pools avp_human adds to a vanilla chest table (HumanVanillaLoot). */
    private static void addPools(List<Source> sources, String name, net.minecraft.resources.ResourceKey<LootTable> key) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        try {
            var builder = LootTable.lootTable();
            var pools = com.human.common.data.loot.HumanVanillaLoot.extraPools(key);
            if (pools.isEmpty()) {
                return;
            }
            pools.forEach(builder::withPool);
            var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
            var encoded = LootTable.DIRECT_CODEC.encodeStart(ops, builder.build()).result();
            if (encoded.isPresent() && encoded.get().isJsonObject()) {
                add(sources, Component.literal(name), new ItemStack(Items.CHEST), encoded.get().getAsJsonObject());
            }
        } catch (RuntimeException exception) {
            Human.LOGGER.warn("Blueprint sources: could not read the extra loot for {}", key.location(), exception);
        }
    }

    private static JsonObject readOwnTable(String path) {
        var resource = "/data/" + Human.MOD_ID + "/loot_table/" + path + ".json";
        try (var stream = BlueprintSources.class.getResourceAsStream(resource)) {
            if (stream == null) {
                return null;
            }
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return json.isJsonObject() ? json.getAsJsonObject() : null;
        } catch (Exception exception) {
            Human.LOGGER.warn("Blueprint sources: could not read loot table {}", resource, exception);
            return null;
        }
    }

    // ---- the loot maths: chance of at least one of each blueprint per chest ----

    private static void collectTable(JsonObject table, Map<Item, Float> into, int depth) {
        if (!table.has("pools") || depth > MAX_NESTING) {
            return;
        }
        for (var poolElement : table.getAsJsonArray("pools")) {
            if (!poolElement.isJsonObject()) {
                continue;
            }
            var pool = poolElement.getAsJsonObject();
            var rolls = averageRolls(pool.get("rolls"));
            var poolChance = randomChance(pool.getAsJsonArray("conditions"));
            var entries = pool.getAsJsonArray("entries");
            if (entries == null || rolls <= 0.0F || poolChance <= 0.0F) {
                continue;
            }

            var totalWeight = 0.0F;
            for (var entry : entries) {
                totalWeight += weight(entry);
            }
            if (totalWeight <= 0.0F) {
                continue;
            }

            var perRoll = new LinkedHashMap<Item, Float>();
            for (var entry : entries) {
                if (entry.isJsonObject()) {
                    var share = weight(entry) / totalWeight * randomChance(entry.getAsJsonObject().getAsJsonArray("conditions"));
                    collectEntry(entry.getAsJsonObject(), share, perRoll, depth);
                }
            }

            for (var found : perRoll.entrySet()) {
                var pool1 = 1.0F - (float) Math.pow(1.0F - Math.min(1.0F, found.getValue()), rolls);
                combine(into, found.getKey(), poolChance * pool1);
            }
        }
    }

    private static void collectEntry(JsonObject entry, float share, Map<Item, Float> into, int depth) {
        var type = entry.has("type") ? entry.get("type").getAsString() : "";

        switch (type) {
            case "minecraft:item" -> {
                var id = entry.has("name") ? ResourceLocation.tryParse(entry.get("name").getAsString()) : null;
                if (id != null && isRareFind(id)) {
                    var item = BuiltInRegistries.ITEM.get(id);
                    if (item != Items.AIR) {
                        combine(into, item, share);
                    }
                }
            }
            case "minecraft:tag" -> {
                // A tag entry gives ONE of the tag's items (expand) or all of them; either way each listed item is
                // shown with the tag's share - shared out evenly when it picks one.
                var id = entry.has("name") ? ResourceLocation.tryParse(entry.get("name").getAsString()) : null;
                if (id != null) {
                    var tag = BuiltInRegistries.ITEM.getTag(
                        net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, id)
                    );
                    if (tag.isPresent()) {
                        var expand = entry.has("expand") && entry.get("expand").getAsBoolean();
                        var size = Math.max(1, tag.get().size());
                        for (var holder : tag.get()) {
                            var itemId = BuiltInRegistries.ITEM.getKey(holder.value());
                            if (isRareFind(itemId)) {
                                combine(into, holder.value(), expand ? share / size : share);
                            }
                        }
                    }
                }
            }
            case "minecraft:loot_table" -> {
                var value = entry.get("value");
                if (value != null && value.isJsonPrimitive()) {
                    var id = ResourceLocation.tryParse(value.getAsString());
                    if (id != null && Human.MOD_ID.equals(id.getNamespace())) {
                        var nested = readOwnTable(id.getPath());
                        if (nested != null) {
                            var inner = new LinkedHashMap<Item, Float>();
                            collectTable(nested, inner, depth + 1);
                            inner.forEach((item, chance) -> combine(into, item, chance * share));
                        }
                    }
                }
            }
            case "minecraft:alternatives", "minecraft:group", "minecraft:sequence" -> {
                var children = entry.getAsJsonArray("children");
                if (children != null) {
                    for (var child : children) {
                        if (child.isJsonObject()) {
                            collectEntry(child.getAsJsonObject(), share, into, depth);
                        }
                    }
                }
            }
            default -> {}
        }
    }

    /** What this tab lists: blueprints and music disc fragments. */
    private static boolean isRareFind(ResourceLocation id) {
        var path = id.getPath();
        return path.startsWith("blueprint_") || (path.contains("music_disc") && path.endsWith("fragment"));
    }

    /** At least one of two independent chances. */
    private static void combine(Map<Item, Float> into, Item item, float chance) {
        into.merge(item, chance, (a, b) -> 1.0F - (1.0F - a) * (1.0F - b));
    }

    private static float weight(JsonElement entry) {
        if (!entry.isJsonObject()) {
            return 0.0F;
        }
        var object = entry.getAsJsonObject();
        return object.has("weight") ? object.get("weight").getAsFloat() : 1.0F;
    }

    private static float averageRolls(JsonElement rolls) {
        if (rolls == null) {
            return 1.0F;
        }
        if (rolls.isJsonPrimitive()) {
            return rolls.getAsFloat();
        }
        var object = rolls.getAsJsonObject();
        if (object.has("value")) {
            return averageRolls(object.get("value"));
        }
        if (object.has("min") && object.has("max")) {
            return (averageRolls(object.get("min")) + averageRolls(object.get("max"))) / 2.0F;
        }
        return 1.0F;
    }

    private static float randomChance(JsonArray conditions) {
        if (conditions == null) {
            return 1.0F;
        }
        var chance = 1.0F;
        for (var condition : conditions) {
            if (!condition.isJsonObject()) {
                continue;
            }
            var object = condition.getAsJsonObject();
            var type = object.has("condition") ? object.get("condition").getAsString() : "";
            if (type.equals("minecraft:random_chance") && object.has("chance")) {
                var value = object.get("chance");
                chance *= value.isJsonPrimitive() ? value.getAsFloat() : averageRolls(value);
            }
        }
        return chance;
    }

    /** "12%" or "<1%". */
    public static net.minecraft.network.chat.MutableComponent chanceText(float chance) {
        var percent = chance * 100.0F;
        var text = percent >= 1.0F ? Math.round(percent) + "% per chest" : "<1% per chest";
        return Component.literal(text);
    }
}
