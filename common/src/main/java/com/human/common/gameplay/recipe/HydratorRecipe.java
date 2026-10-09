package com.human.common.gameplay.recipe;

import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.registry.init.HumanRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * An electric hydrator recipe: floods one input with the water the hydrator makes itself. [stated] readme + Sep 29/30 -
 * concrete powder to concrete, dirt to mud, obsidian to crying obsidian, dead coral back to living coral.
 *
 * <pre>
 * { "type": "avp_human:hydrating",
 *   "ingredient": { "item": "minecraft:dirt" },
 *   "result": { "id": "minecraft:mud" },
 *   "water": 250,
 *   "time": 100 }
 * </pre>
 *
 * {@code water} (millibuckets taken from the hydrator's tank, default {@value #DEFAULT_WATER}), {@code count} and
 * {@code time} are optional.
 */
public class HydratorRecipe implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_TIME = 100;

    /** A quarter bucket - [stated] Sep 29 "uses ¼ bucket per item". */
    public static final int DEFAULT_WATER = FluidContent.BUCKET / 4;

    private final Ingredient ingredient;

    private final int inputCount;

    private final ItemStack result;

    private final int water;

    private final int time;

    public HydratorRecipe(Ingredient ingredient, int inputCount, ItemStack result, int water, int time) {
        this.ingredient = ingredient;
        this.inputCount = Math.max(1, inputCount);
        this.result = result;
        this.water = Math.max(0, water);
        this.time = Math.max(1, time);
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getInputCount() {
        return inputCount;
    }

    public ItemStack getResult() {
        return result;
    }

    /** Millibuckets of water one job uses. */
    public int getWater() {
        return water;
    }

    public int getTime() {
        return time;
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput input, @NotNull Level level) {
        return ingredient.test(input.item()) && input.item().getCount() >= inputCount;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull SingleRecipeInput input, HolderLookup.@NotNull Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return result;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return HumanRecipes.HYDRATING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return HumanRecipes.HYDRATING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<HydratorRecipe> {

        private static final MapCodec<HydratorRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(HydratorRecipe::getIngredient),
                Codec.INT.optionalFieldOf("count", 1).forGetter(HydratorRecipe::getInputCount),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(HydratorRecipe::getResult),
                Codec.INT.optionalFieldOf("water", DEFAULT_WATER).forGetter(HydratorRecipe::getWater),
                Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(HydratorRecipe::getTime)
            ).apply(instance, HydratorRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, HydratorRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            HydratorRecipe::getIngredient,
            ByteBufCodecs.VAR_INT,
            HydratorRecipe::getInputCount,
            ItemStack.STREAM_CODEC,
            HydratorRecipe::getResult,
            ByteBufCodecs.VAR_INT,
            HydratorRecipe::getWater,
            ByteBufCodecs.VAR_INT,
            HydratorRecipe::getTime,
            HydratorRecipe::new
        );

        @Override
        public @NotNull MapCodec<HydratorRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, HydratorRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
