package com.human.common.gameplay.gene;

import com.human.HumanResources;
import com.human.common.model.GeneCarrier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loot genes (team-reviewed design, Oct 25/26): every mob's drops become genes automatically, one gene per item, read
 * from its loot table. A carrier drops extra of that item whenever it dies, on any death - no player kill needed.
 * <ul>
 * <li><b>Dose:</b> one draw gives +10% (0.1) to each loot gene. Each whole 1.0 is one guaranteed extra drop, the
 * remainder a chance at one more, capped at 3 extra per death (cap 3.0).</li>
 * <li><b>Cooked:</b> a food drop comes out cooked if the carrier dies on fire, as vanilla does.</li>
 * <li><b>Never a gene:</b> anything with durability, anything that stacks to 1, ingots / nuggets / gems / ores / raw
 * metals / dusts / arrows, anything Uncommon or rarer, any mob head, and the {@link #NO_LOOT_GENE} tag (jelly, plated
 * chitin, the elder guardian's sponge and trim, the warden's catalyst, and every piece of yautja gear - shards, scrap,
 * shurikens, darts, pellets, nets, grenades, cores). A carrier that already holds a gene for an item blocked later
 * drops nothing extra for it. {@link #LOOT_GENE_ALLOWED} lets a wrongly blocked item through. Bosses carry no genes at
 * all.</li>
 * </ul>
 * Genes are made on demand and named after their item, so modded mobs and items work with no data written.
 */
public final class LootGenes {

    public static final double DOSE = 0.1;

    public static final double CAP = 3.0;

    public static final int MAX_EXTRA_PER_DEATH = 3;

    /** How many times a donor's loot table is rolled to find everything it can drop. */
    private static final int SCAN_ROLLS = 48;

    private static final String PATH_PREFIX = "loot/";

    private static final String LOOT_NAMESPACE = HumanResources.location("loot").getNamespace();

    public static final TagKey<Item> NO_LOOT_GENE = TagKey.create(Registries.ITEM, HumanResources.location("no_loot_gene"));

    public static final TagKey<Item> LOOT_GENE_ALLOWED = TagKey.create(Registries.ITEM, HumanResources.location("loot_gene_allowed"));

    private static final List<TagKey<Item>> MATERIAL_TAGS = List.of(
        common("ingots"),
        common("nuggets"),
        common("gems"),
        common("ores"),
        common("raw_materials"),
        common("dusts"),
        ItemTags.ARROWS
    );

    private static final Map<ResourceKey<LootTable>, List<Item>> SCAN_CACHE = new ConcurrentHashMap<>();

    /**
     * The loot tables of the session the cache was filled from; a datapack reload replaces it and empties the cache.
     */
    private static @Nullable Object scannedRegistries;

    private LootGenes() {}

    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    // ---------------------------------------------------------------- the genes themselves

    public static ResourceLocation idFor(Item item) {
        var itemId = BuiltInRegistries.ITEM.getKey(item);
        return HumanResources.location(PATH_PREFIX + itemId.getNamespace() + "/" + itemId.getPath());
    }

    public static boolean isLootGene(Gene gene) {
        return gene instanceof Gene.Loot;
    }

    /** The loot gene for this item, made and registered the first time it is asked for. */
    public static Gene geneFor(Item item) {
        var id = idFor(item);
        var existing = GeneRegistry.getValueOrNull(id);
        return existing != null ? existing : register(id, BuiltInRegistries.ITEM.getKey(item));
    }

    /**
     * Makes the loot gene an unknown id names, if it is one ({@code avp_human:loot/<namespace>/<path>} for an item that
     * exists). Called by GeneRegistry for ids it has not seen yet. Returns null for anything else.
     */
    static @Nullable Gene resolve(ResourceLocation id) {
        if (!id.getNamespace().equals(LOOT_NAMESPACE) || !id.getPath().startsWith(PATH_PREFIX)) {
            return null;
        }

        var rest = id.getPath().substring(PATH_PREFIX.length());
        var slash = rest.indexOf('/');

        if (slash <= 0) {
            return null;
        }

        var itemId = ResourceLocation.tryBuild(rest.substring(0, slash), rest.substring(slash + 1));

        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return null;
        }

        return register(id, itemId);
    }

    private static synchronized Gene register(ResourceLocation id, ResourceLocation itemId) {
        var existing = GeneRegistry.getRegisteredOrNull(id);

        if (existing != null) {
            return existing;
        }

        var gene = GeneRegistry.register(
            () -> new Gene.Loot(id, itemId, (value, operation) -> Math.clamp(value, 0.0, CAP))
        );
        Genes.putSpec(gene, new Genes.Spec(GeneOperationType.ADDITIVE, CAP, Genes.Who.ALL, Genes.Category.LOOT));
        return gene;
    }

    // ---------------------------------------------------------------- which items count

    /** True when this item may become a loot gene. */
    public static boolean isEligible(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.AIR)) {
            return false;
        }

        if (stack.is(LOOT_GENE_ALLOWED)) {
            return true;
        }

        if (stack.is(NO_LOOT_GENE) || stack.isDamageableItem() || stack.getMaxStackSize() <= 1) {
            return false;
        }

        if (stack.getRarity() != Rarity.COMMON) {
            return false;
        }

        for (var tag : MATERIAL_TAGS) {
            if (stack.is(tag)) {
                return false;
            }
        }

        // Any mob head: a placeable block worn in the head slot (vanilla skulls, the xenomorph heads, modded heads).
        var equipable = Equipable.get(stack);
        if (equipable != null && equipable.getEquipmentSlot() == EquipmentSlot.HEAD && stack.getItem() instanceof BlockItem) {
            return false;
        }

        return !stack.is(ItemTags.SKULLS);
    }

    // ---------------------------------------------------------------- reading a donor's loot table

    /** Mobs that carry no genes at all, beyond the vanilla undead tag and the shared boss tag. */
    public static final TagKey<net.minecraft.world.entity.EntityType<?>> NO_GENES = TagKey.create(
        Registries.ENTITY_TYPE,
        HumanResources.location("no_genes")
    );

    private static final TagKey<net.minecraft.world.entity.EntityType<?>> COMMON_BOSSES = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("c", "bosses")
    );

    /** [stated] Undead, constructs, spirits, elementals and bosses carry no genes, loot genes included. */
    public static boolean canDonate(LivingEntity donor) {
        var type = donor.getType();
        return !type.is(net.minecraft.tags.EntityTypeTags.UNDEAD) && !type.is(COMMON_BOSSES) && !type.is(NO_GENES);
    }

    /**
     * Every eligible item the donor's loot table can drop. The table is rolled {@link #SCAN_ROLLS} times as if the
     * drawing player had killed it (so player-kill-only drops are found; loot genes themselves drop on any death).
     * Cached per loot table until the next datapack reload.
     */
    public static List<Item> scan(ServerLevel level, LivingEntity donor, @Nullable Player player) {
        var server = level.getServer();
        var key = donor.getLootTable();

        if (key == null || key == BuiltInLootTables.EMPTY) {
            return List.of();
        }

        var registries = server.reloadableRegistries();
        if (scannedRegistries != registries) {
            SCAN_CACHE.clear();
            scannedRegistries = registries;
        }

        var cached = SCAN_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        var table = registries.getLootTable(key);
        var builder = new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, donor)
            .withParameter(LootContextParams.ORIGIN, donor.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, player)
            .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player);

        if (player != null) {
            builder = builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withLuck(player.getLuck());
        }

        var params = builder.create(LootContextParamSets.ENTITY);
        var found = new LinkedHashSet<Item>();

        for (var i = 0; i < SCAN_ROLLS; i++) {
            table.getRandomItems(params, stack -> {
                if (isEligible(stack)) {
                    found.add(stack.getItem());
                }
            });
        }

        var result = List.copyOf(new ArrayList<>(found));
        SCAN_CACHE.put(key, result);
        return result;
    }

    // ---------------------------------------------------------------- dropping at death

    /**
     * Drops the extra items a dying carrier's loot genes give. Called from the death-loot path, so it obeys doMobLoot
     * and (on NeoForge) the drops go through the usual living-drops event with everything else.
     */
    public static void dropExtra(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !(entity instanceof GeneCarrier carrier)) {
            return;
        }

        var map = carrier.getOrCreateGeneManager().getGeneContainer().getActiveGeneMap();

        for (var entry : map.getBackingMap().entrySet()) {
            var gene = GeneRegistry.getValueOrNull(entry.getKey().resourceLocation());

            if (!(gene instanceof Gene.Loot loot) || entry.getValue() <= 0) {
                continue;
            }

            var item = BuiltInRegistries.ITEM.get(loot.itemId());

            // An item blocked after the gene was drawn (a filter added later, or a datapack change) drops nothing,
            // so syringes and mobs already carrying it stop handing out gear the moment the filter knows it.
            if (!isEligible(new ItemStack(item))) {
                continue;
            }

            var value = Math.min(entry.getValue(), CAP);
            var whole = (int) value;
            var count = whole + (entity.getRandom().nextDouble() < value - whole ? 1 : 0);
            count = Math.min(count, MAX_EXTRA_PER_DEATH);

            if (count <= 0) {
                continue;
            }

            var stack = new ItemStack(item, count);

            if (entity.isOnFire() && stack.has(DataComponents.FOOD)) {
                stack = cooked(level, stack);
            }

            entity.spawnAtLocation(stack);
        }
    }

    private static ItemStack cooked(ServerLevel level, ItemStack raw) {
        return level.getRecipeManager()
            .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(raw), level)
            .map(holder -> holder.value().getResultItem(level.registryAccess()).copyWithCount(raw.getCount()))
            .filter(result -> !result.isEmpty())
            .orElse(raw);
    }
}
