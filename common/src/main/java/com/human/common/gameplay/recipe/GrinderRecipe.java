package com.human.common.gameplay.recipe;

import com.human.common.registry.init.HumanRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A grinder recipe: one input, up to nine outputs, each with its own chance. [stated] Sep 29: ores give TWO dusts, coal
 * gives double what the crafting table does, gravel sometimes gives flint.
 * <h2>DATA, NOT CODE</h2> Every recipe is a JSON file under {@code data/<namespace>/recipe/}:
 *
 * <pre>
 * { "type": "avp_human:grinding",
 *   "ingredient": { "tag": "c:raw_materials/iron" },
 *   "results": [ { "id": "avp_human:iron_dust", "count": 2 }, { "id": "minecraft:flint", "chance": 0.1 } ],
 *   "time": 200 }
 * </pre>
 *
 * {@code count} on the ingredient (how many are used) and {@code time} (ticks) are optional; a result's {@code chance}
 * is optional and 1 by default. Pack makers can add their own the same way.
 * <h2>⚠ NO AUTOMATIC DECRAFTING</h2> Only listed recipes exist. Reverse-crafting whatever the crafting table knows is a
 * classic duplication exploit the moment another mod adds a cheaper recipe for the same item.
 */
public class GrinderRecipe implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_TIME = 200;

    public static final int MAX_RESULTS = 9;

    /** One output and the chance it drops, 0-1. */
    public record ChanceResult(
        ItemStack stack,
        float chance
    ) {

        public static final Codec<ChanceResult> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ChanceResult::stack),
                Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(ChanceResult::chance)
            ).apply(instance, ChanceResult::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ChanceResult> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            ChanceResult::stack,
            ByteBufCodecs.FLOAT,
            ChanceResult::chance,
            ChanceResult::new
        );
    }

    private final Ingredient ingredient;

    private final int inputCount;

    private final List<ChanceResult> results;

    private final int time;

    public GrinderRecipe(Ingredient ingredient, int inputCount, List<ChanceResult> results, int time) {
        this.ingredient = ingredient;
        this.inputCount = Math.max(1, inputCount);
        this.results = List.copyOf(results.subList(0, Math.min(MAX_RESULTS, results.size())));
        this.time = Math.max(1, time);
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getInputCount() {
        return inputCount;
    }

    public List<ChanceResult> getResults() {
        return results;
    }

    public int getTime() {
        return time;
    }

    /** What this grind actually produces - each chance rolled once. */
    public List<ItemStack> roll(RandomSource random) {
        var rolled = new ArrayList<ItemStack>(results.size());

        for (var result : results) {
            if (result.chance() >= 1.0F || random.nextFloat() < result.chance()) {
                rolled.add(result.stack().copy());
            }
        }

        return rolled;
    }

    /** Every output that COULD come out - what the machine checks for room before it starts. */
    public List<ItemStack> allPossibleResults() {
        var all = new ArrayList<ItemStack>(results.size());

        for (var result : results) {
            all.add(result.stack().copy());
        }

        return all;
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput input, @NotNull Level level) {
        return ingredient.test(input.item()) && input.item().getCount() >= inputCount;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull SingleRecipeInput input, HolderLookup.@NotNull Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).stack().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).stack();
    }

    /** ⚠ Keeps vanilla's recipe book out of it - otherwise the client logs "unknown recipe category" for each. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return HumanRecipes.GRINDING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return HumanRecipes.GRINDING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<GrinderRecipe> {

        private static final MapCodec<GrinderRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(GrinderRecipe::getIngredient),
                Codec.INT.optionalFieldOf("count", 1).forGetter(GrinderRecipe::getInputCount),
                ChanceResult.CODEC.listOf().fieldOf("results").forGetter(GrinderRecipe::getResults),
                Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(GrinderRecipe::getTime)
            ).apply(instance, GrinderRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, GrinderRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            GrinderRecipe::getIngredient,
            ByteBufCodecs.VAR_INT,
            GrinderRecipe::getInputCount,
            ChanceResult.STREAM_CODEC.apply(ByteBufCodecs.list()),
            GrinderRecipe::getResults,
            ByteBufCodecs.VAR_INT,
            GrinderRecipe::getTime,
            GrinderRecipe::new
        );

        @Override
        public @NotNull MapCodec<GrinderRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, GrinderRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
