package com.human.common.gameplay.recipe;

import com.human.common.gameplay.fluid.FluidContent;
import com.human.common.registry.init.HumanRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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

import java.util.Optional;

/**
 * An electric press recipe: one input, an ITEM result, a FLUID result, or both. [stated] readme + Sep 29/30 - jungle
 * log to latex (the log is used up), magma to lava and a block, mud to water and dirt.
 * <h2>DATA, NOT CODE</h2>
 *
 * <pre>
 * { "type": "avp_human:pressing",
 *   "ingredient": { "item": "minecraft:magma_block" },
 *   "result": { "id": "minecraft:netherrack" },
 *   "fluid": { "fluid": "minecraft:lava", "amount": 250 },
 *   "time": 200 }
 * </pre>
 *
 * {@code result} and {@code fluid} are each optional, but a recipe needs at least one. {@code count} (how many inputs
 * are used, default 1) and {@code time} (ticks, default {@value #DEFAULT_TIME}) are optional.
 */
public class PressRecipe implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_TIME = 200;

    /** An item result that may be absent - written as a missing field, never as an "empty" stack. */
    static final MapCodec<ItemStack> OPTIONAL_RESULT = ItemStack.STRICT_CODEC.optionalFieldOf("result")
        .xmap(result -> result.orElse(ItemStack.EMPTY), stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));

    /** A fluid that may be absent. */
    static final MapCodec<FluidContent> OPTIONAL_FLUID = FluidContent.CODEC.optionalFieldOf("fluid")
        .xmap(fluid -> fluid.orElse(FluidContent.EMPTY), fluid -> fluid.isEmpty() ? Optional.empty() : Optional.of(fluid));

    private final Ingredient ingredient;

    private final int inputCount;

    private final ItemStack result;

    private final FluidContent fluid;

    private final int time;

    public PressRecipe(Ingredient ingredient, int inputCount, ItemStack result, FluidContent fluid, int time) {
        this.ingredient = ingredient;
        this.inputCount = Math.max(1, inputCount);
        this.result = result;
        this.fluid = fluid;
        this.time = Math.max(1, time);
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getInputCount() {
        return inputCount;
    }

    /** The item result - EMPTY when this recipe only makes fluid (the jungle log). */
    public ItemStack getResult() {
        return result;
    }

    /** The fluid result - EMPTY when this recipe only makes an item. */
    public FluidContent getFluid() {
        return fluid;
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

    /** ⚠ Keeps vanilla's recipe book out of it - otherwise the client logs "unknown recipe category" for each. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return HumanRecipes.PRESSING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return HumanRecipes.PRESSING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<PressRecipe> {

        private static final MapCodec<PressRecipe> CODEC = RecordCodecBuilder.<PressRecipe>mapCodec(
            instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(PressRecipe::getIngredient),
                Codec.INT.optionalFieldOf("count", 1).forGetter(PressRecipe::getInputCount),
                OPTIONAL_RESULT.forGetter(PressRecipe::getResult),
                OPTIONAL_FLUID.forGetter(PressRecipe::getFluid),
                Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(PressRecipe::getTime)
            ).apply(instance, PressRecipe::new)
        )
            .validate(
                recipe -> recipe.getResult().isEmpty() && recipe.getFluid().isEmpty()
                    ? DataResult.error(() -> "A pressing recipe needs a \"result\", a \"fluid\", or both")
                    : DataResult.success(recipe)
            );

        private static final StreamCodec<RegistryFriendlyByteBuf, PressRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            PressRecipe::getIngredient,
            ByteBufCodecs.VAR_INT,
            PressRecipe::getInputCount,
            ItemStack.OPTIONAL_STREAM_CODEC,
            PressRecipe::getResult,
            FluidContent.STREAM_CODEC,
            PressRecipe::getFluid,
            ByteBufCodecs.VAR_INT,
            PressRecipe::getTime,
            PressRecipe::new
        );

        @Override
        public @NotNull MapCodec<PressRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, PressRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
