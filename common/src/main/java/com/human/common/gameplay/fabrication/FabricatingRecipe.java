package com.human.common.gameplay.fabrication;

import com.human.common.registry.init.HumanFabrication;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@code avp_human:fabricating} - an ingredient LIST for things only a fabricator makes: the W-Y and USCM editions,
 * which have no crafting-table recipe at all. Only the fabricator reads these; no grid ever matches one.
 *
 * <pre>
 * { "type": "avp_human:fabricating",
 *   "ingredients": [ { "ingredient": { "item": "minecraft:chest" }, "count": 1 }, ... ],
 *   "result": { "id": "avp_human:uscm_pallet_load_1x1", "count": 1 } }
 * </pre>
 */
public class FabricatingRecipe implements Recipe<RecipeInput> {

    public record Part(
        Ingredient ingredient,
        int count
    ) {

        public static final Codec<Part> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(Part::ingredient),
                Codec.intRange(1, 576).optionalFieldOf("count", 1).forGetter(Part::count)
            ).apply(instance, Part::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, Part> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            Part::ingredient,
            ByteBufCodecs.VAR_INT,
            Part::count,
            Part::new
        );
    }

    public static final MapCodec<FabricatingRecipe> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Part.CODEC.listOf().fieldOf("ingredients").forGetter(FabricatingRecipe::parts),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
        ).apply(instance, FabricatingRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FabricatingRecipe> STREAM_CODEC = StreamCodec.composite(
        Part.STREAM_CODEC.apply(ByteBufCodecs.list()),
        FabricatingRecipe::parts,
        ItemStack.STREAM_CODEC,
        recipe -> recipe.result,
        FabricatingRecipe::new
    );

    public static final RecipeSerializer<FabricatingRecipe> SERIALIZER = new RecipeSerializer<>() {

        @Override
        public @NotNull MapCodec<FabricatingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, FabricatingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    private final List<Part> parts;

    private final ItemStack result;

    public FabricatingRecipe(List<Part> parts, ItemStack result) {
        this.parts = List.copyOf(parts);
        this.result = result;
    }

    public List<Part> parts() {
        return parts;
    }

    @Override
    public boolean matches(@NotNull RecipeInput input, @NotNull Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput input, HolderLookup.@NotNull Provider registries) {
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
    public @NotNull NonNullList<Ingredient> getIngredients() {
        var list = NonNullList.<Ingredient>create();

        parts.forEach(part -> list.add(part.ingredient()));

        return list;
    }

    @Override
    public boolean isSpecial() {
        // Keeps it out of the vanilla recipe book, which could never fill it.
        return true;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return HumanFabrication.FABRICATING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return HumanFabrication.FABRICATING_TYPE.get();
    }
}
