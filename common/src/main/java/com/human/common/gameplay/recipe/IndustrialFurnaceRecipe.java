package com.human.common.gameplay.recipe;

import com.human.common.registry.init.HumanRecipes;
import com.human.common.registry.init.block.HumanPlasticBlocks;
import com.human.common.registry.tag.HumanItemTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class IndustrialFurnaceRecipe extends AbstractCookingRecipe {

    public static final Map<Item, Item> MELTING_RECIPES = new HashMap<>();

    /**
     * Melting recipes whose INPUT is a tag rather than a single item.
     * <p>
     * ⚠⚠ THIS EXISTS BECAUSE A TAG CANNOT LIVE IN {@code Map<Item, Item>}, AND THAT IS WHY THE RESIN RECIPE NEVER
     * WORKED. The datagen side already had a tag-input builder - IndustrialFurnaceRecipeProvider calls it for
     * RESIN_BLOCKS - but the runtime had nowhere to put the result, so the builder produced an object nobody ever
     * registered. No recipe JSON was generated and {@code fromItem} had nothing to match, so resin blocks simply sat in
     * the furnace doing nothing.
     * </p>
     * <p>
     * ⚠ A TAG IS THE ONLY SANE WAY TO EXPRESS THIS ONE. Resin comes from avp_alien, which is an OPTIONAL dependency,
     * and every strain has its own block - normal, aberrant, irradiated, nether, plus whatever is added later.
     * Enumerating them here would mean hard references to classes that may not exist at runtime; the tag already lists
     * them with {@code required: false} and simply comes back empty when the mod is absent.
     * </p>
     */
    public static final Map<TagKey<Item>, Supplier<Item>> MELTING_TAG_RECIPES = new LinkedHashMap<>();

    private static final float SPEED_MULTIPLIER = 0.5f; // 50% faster than regular furnace

    static {
        // Initialize melting recipes based on the provided list
        registerMeltingRecipe(Items.COBBLESTONE, Items.STONE);
        registerMeltingRecipe(Items.COBBLED_DEEPSLATE, Items.DEEPSLATE);
        registerMeltingRecipe(Items.WHITE_TERRACOTTA, Items.WHITE_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.ORANGE_TERRACOTTA, Items.ORANGE_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.MAGENTA_TERRACOTTA, Items.MAGENTA_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.LIGHT_BLUE_TERRACOTTA, Items.LIGHT_BLUE_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.YELLOW_TERRACOTTA, Items.YELLOW_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.LIME_TERRACOTTA, Items.LIME_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.PINK_TERRACOTTA, Items.PINK_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.GRAY_TERRACOTTA, Items.GRAY_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.LIGHT_GRAY_TERRACOTTA, Items.LIGHT_GRAY_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.CYAN_TERRACOTTA, Items.CYAN_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.PURPLE_TERRACOTTA, Items.PURPLE_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.BLUE_TERRACOTTA, Items.BLUE_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.BROWN_TERRACOTTA, Items.BROWN_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.GREEN_TERRACOTTA, Items.GREEN_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.RED_TERRACOTTA, Items.RED_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.BLACK_TERRACOTTA, Items.BLACK_GLAZED_TERRACOTTA);
        registerMeltingRecipe(Items.QUARTZ_BLOCK, Items.SMOOTH_QUARTZ);
        registerMeltingRecipe(Items.SANDSTONE, Items.SMOOTH_SANDSTONE);
        registerMeltingRecipe(Items.STONE, Items.SMOOTH_STONE);
        registerMeltingRecipe(Items.BASALT, Items.SMOOTH_BASALT);
        registerMeltingRecipe(Items.RED_SANDSTONE, Items.SMOOTH_RED_SANDSTONE);
        registerMeltingRecipe(Items.STONE_BRICKS, Items.CRACKED_STONE_BRICKS);
        registerMeltingRecipe(Items.DEEPSLATE_BRICKS, Items.CRACKED_DEEPSLATE_BRICKS);
        registerMeltingRecipe(Items.DEEPSLATE_TILES, Items.CRACKED_DEEPSLATE_TILES);
        registerMeltingRecipe(Items.POLISHED_BLACKSTONE_BRICKS, Items.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        registerMeltingRecipe(Items.NETHER_BRICKS, Items.CRACKED_NETHER_BRICKS);
        registerMeltingRecipe(Items.WET_SPONGE, Items.SPONGE);
        registerMeltingRecipe(Items.SAND, Items.GLASS);
        registerMeltingRecipe(Items.RED_SAND, Items.GLASS);
        registerMeltingRecipe(Items.MUD, Items.CLAY);
        registerMeltingRecipe(Items.CLAY, Items.TERRACOTTA);

        // ⭐ EVERY STRAIN'S RESIN BLOCK COOKS TO GREEN PLASTIC. The long way round is four resin balls smelted to
        // slime, smelted to polymer, then crafted into a plastic block - and since four resin balls are also one
        // resin block, cooking the block whole lands on the same plastic for the same resin. It buys steps, not
        // material.
        // ⚠ REGISTERED HERE, IN COMMON, RATHER THAN IN THE DATAGEN PROVIDER. The provider's builders only mirror this
        // map for the recipe book; they do not create anything. A recipe that exists only in the provider - which is
        // exactly what the resin one was - never reaches the furnace at all.
        // ⚠ LAMBDA, NOT A RESOLVED ITEM - see registerMeltingRecipe below. This line runs during class init.
        registerMeltingRecipe(
            HumanItemTags.RESIN_BLOCKS,
            () -> HumanPlasticBlocks.DYE_COLOR_TO_PLASTIC.get(DyeColor.GREEN).get().asItem()
        );
    }

    private static void registerMeltingRecipe(Item input, Item output) {
        MELTING_RECIPES.put(input, output);
    }

    /**
     * ⚠ ORDER MATTERS FOR THE RECIPE ID, which is why this is a LinkedHashMap - the generated JSON filename is derived
     * from iteration order, and a HashMap would shuffle it between builds for no reason.
     */
    /**
     * ⚠⚠ THE OUTPUT IS A SUPPLIER, NOT AN ITEM, AND THAT IS NOT TIDINESS. This map is filled from a STATIC INITIALISER,
     * and the plastic block is a registry holder - resolving it eagerly means calling {@code BLibHolder.get()} during
     * class init, before anything guarantees the block registry has been populated. Registry access during static init
     * is the trap that has to be avoided here: it boots fine in one load order and throws in another. Resolved lazily,
     * the lookup happens the first time a furnace actually runs, when the registries are certainly ready.
     * </p>
     * <p>
     * ⚠ ORDER MATTERS FOR THE RECIPE ID, which is why this is a LinkedHashMap - the generated JSON filename is derived
     * from iteration order, and a HashMap would shuffle it between builds for no reason.
     * </p>
     */
    public static void registerMeltingRecipe(TagKey<Item> input, Supplier<Item> output) {
        MELTING_TAG_RECIPES.put(input, output);
    }

    public IndustrialFurnaceRecipe(
        String group,
        CookingBookCategory cookingBookCategory,
        Ingredient ingredient,
        ItemStack result,
        float experience,
        int cookingTime
    ) {
        super(HumanRecipes.INDUSTRIAL_FURNACE_RECIPE_TYPE.get(), group, cookingBookCategory, ingredient, result, experience, cookingTime);
    }

    /**
     * Creates a recipe from an item if it has a registered melting recipe
     */
    public static IndustrialFurnaceRecipe fromItem(Item input) {
        var output = MELTING_RECIPES.get(input);

        // ⚠ ITEM FIRST, THEN TAGS. An exact item entry should always win over a tag that happens to contain it, so a
        // future one-off override of a single resin strain does not need the tag rewritten around it.
        if (output == null) {
            output = fromTag(input);
        }

        if (output == null) {
            return null;
        }

        var vanillaTime = 200;
        var processTime = (int) (vanillaTime * SPEED_MULTIPLIER);
        var recipeId = BuiltInRegistries.ITEM.getKey(input).getPath() + "_to_" +
            BuiltInRegistries.ITEM.getKey(output).getPath();

        return new IndustrialFurnaceRecipe(
            recipeId,
            determineRecipeCategory(input),
            Ingredient.of(input),
            new ItemStack(output),
            0.1f, // Default experience
            processTime
        );
    }

    /**
     * ⚠ USES {@code builtInRegistryHolder}, NOT AN ItemStack. Building a throwaway stack per lookup would allocate on
     * every furnace tick for every slot; the registry holder is the item's own tag membership and costs nothing.
     */
    private static Item fromTag(Item input) {
        var holder = input.builtInRegistryHolder();

        for (var entry : MELTING_TAG_RECIPES.entrySet()) {
            if (holder.is(entry.getKey())) {
                return entry.getValue().get();
            }
        }

        return null;
    }

    private static CookingBookCategory determineRecipeCategory(Item item) {
        return CookingBookCategory.BLOCKS;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return HumanRecipes.INDUSTRIAL_FURNACE_RECIPE_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
