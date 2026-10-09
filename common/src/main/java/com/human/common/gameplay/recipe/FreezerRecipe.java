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
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * An electric freezer recipe: freezes EITHER an item from the input slot OR fluid from the freezer's tank. [stated]
 * readme + Sep 29 - water to ice, ice to packed ice to blue ice, lava to obsidian, honey to a honey block, latex to a
 * latex block.
 *
 * <pre>
 * { "type": "avp_human:freezing", "ingredient": { "item": "minecraft:ice" }, "result": { "id": "minecraft:packed_ice" } }
 * { "type": "avp_human:freezing", "fluid": { "fluid": "minecraft:water", "amount": 1000 }, "result": { "id": "minecraft:ice" } }
 * </pre>
 *
 * Exactly one of {@code ingredient} and {@code fluid}. {@code count} (items used, default 1) and {@code time} (ticks,
 * default {@value #DEFAULT_TIME}) are optional.
 * <p>
 * ⚠ Ice to packed ice and packed ice to blue ice use NINE, as vanilla's crafting does - one for one would turn each ice
 * block into a blue ice block, 81 times what vanilla gives.
 * </p>
 */
public class FreezerRecipe implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_TIME = 200;

    private static final MapCodec<Ingredient> OPTIONAL_INGREDIENT = Ingredient.CODEC_NONEMPTY.optionalFieldOf("ingredient")
        .xmap(
            ingredient -> ingredient.orElse(Ingredient.EMPTY),
            ingredient -> ingredient.isEmpty() ? Optional.empty() : Optional.of(ingredient)
        );

    private final Ingredient ingredient;

    private final int inputCount;

    private final FluidContent fluid;

    private final ItemStack result;

    private final int time;

    public FreezerRecipe(Ingredient ingredient, int inputCount, FluidContent fluid, ItemStack result, int time) {
        this.ingredient = ingredient;
        this.inputCount = Math.max(1, inputCount);
        this.fluid = fluid;
        this.result = result;
        this.time = Math.max(1, time);
    }

    /** The item frozen - EMPTY for a fluid recipe. */
    public Ingredient getIngredient() {
        return ingredient;
    }

    /** How many input items one job uses (item recipes). */
    public int getInputCount() {
        return inputCount;
    }

    /** The fluid frozen, and how much per result - EMPTY for an item recipe. */
    public FluidContent getFluid() {
        return fluid;
    }

    public boolean isFluidRecipe() {
        return !fluid.isEmpty();
    }

    public boolean usesFluid(Fluid other) {
        return isFluidRecipe() && fluid.fluid().isSame(other);
    }

    public ItemStack getResult() {
        return result;
    }

    public int getTime() {
        return time;
    }

    /** Item recipes only. Fluid recipes are looked up by the fluid in the tank, not by the input slot. */
    @Override
    public boolean matches(@NotNull SingleRecipeInput input, @NotNull Level level) {
        return !ingredient.isEmpty() && ingredient.test(input.item()) && input.item().getCount() >= inputCount;
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
        return HumanRecipes.FREEZING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return HumanRecipes.FREEZING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<FreezerRecipe> {

        private static final MapCodec<FreezerRecipe> CODEC = RecordCodecBuilder.<FreezerRecipe>mapCodec(
            instance -> instance.group(
                OPTIONAL_INGREDIENT.forGetter(FreezerRecipe::getIngredient),
                Codec.INT.optionalFieldOf("count", 1).forGetter(FreezerRecipe::getInputCount),
                PressRecipe.OPTIONAL_FLUID.forGetter(FreezerRecipe::getFluid),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(FreezerRecipe::getResult),
                Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(FreezerRecipe::getTime)
            ).apply(instance, FreezerRecipe::new)
        )
            .validate(
                recipe -> recipe.getIngredient().isEmpty() == recipe.getFluid().isEmpty()
                    ? DataResult.error(() -> "A freezing recipe needs exactly one of \"ingredient\" and \"fluid\"")
                    : DataResult.success(recipe)
            );

        private static final StreamCodec<RegistryFriendlyByteBuf, FreezerRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            FreezerRecipe::getIngredient,
            ByteBufCodecs.VAR_INT,
            FreezerRecipe::getInputCount,
            FluidContent.STREAM_CODEC,
            FreezerRecipe::getFluid,
            ItemStack.STREAM_CODEC,
            FreezerRecipe::getResult,
            ByteBufCodecs.VAR_INT,
            FreezerRecipe::getTime,
            FreezerRecipe::new
        );

        @Override
        public @NotNull MapCodec<FreezerRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, FreezerRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
