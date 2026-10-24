package com.human.common.gameplay.storage;

import com.human.common.registry.init.HumanCrates;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.jetbrains.annotations.NotNull;

/**
 * A shaped recipe that turns a smaller crate into a bigger one and keeps whatever was inside.
 * <p>
 * The items never pass through the crafting grid. A full crate item only carries the id of its contents in
 * {@link CrateStorageData}, so the upgrade moves that id onto the new crate: the old crate is used up by the craft,
 * leaving exactly one item that points at the contents. The bigger crate always has more slots than the smaller one, so
 * every item keeps its slot when the new crate is placed.
 * <p>
 * The JSON is the same as {@code minecraft:crafting_shaped}, with {@code "type": "avp_human:crate_upgrade"}.
 */
public class CrateUpgradeRecipe extends ShapedRecipe {

    public static final MapCodec<CrateUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(CrateUpgradeRecipe::getGroup),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(CrateUpgradeRecipe::category),
            ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(CrateUpgradeRecipe::showNotification)
        ).apply(instance, CrateUpgradeRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, CrateUpgradeRecipe> STREAM_CODEC = StreamCodec.of(
        CrateUpgradeRecipe::write,
        CrateUpgradeRecipe::read
    );

    public static final RecipeSerializer<CrateUpgradeRecipe> SERIALIZER = new RecipeSerializer<>() {

        @Override
        public @NotNull MapCodec<CrateUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, CrateUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    // ShapedRecipe keeps these package-private, so this recipe keeps its own copies for the codecs.
    private final ShapedRecipePattern pattern;

    private final ItemStack result;

    public CrateUpgradeRecipe(
        String group,
        CraftingBookCategory category,
        ShapedRecipePattern pattern,
        ItemStack result,
        boolean showNotification
    ) {
        super(group, category, pattern, result, showNotification);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input, HolderLookup.@NotNull Provider registries) {
        var crafted = super.assemble(input, registries);

        if (!(crafted.getItem() instanceof CrateItem bigger)) {
            return crafted;
        }

        for (int i = 0; i < input.size(); i++) {
            var ingredient = input.getItem(i);

            if (!CrateItem.hasContents(ingredient)) {
                continue;
            }

            var contents = ingredient.get(HumanCrates.CRATE_CONTENTS.get());

            if (contents == null) {
                continue;
            }

            crafted.set(
                HumanCrates.CRATE_CONTENTS.get(),
                new CrateContents(
                    contents.id(),
                    contents.items(),
                    contents.usedSlots(),
                    bigger.type().size().slots(),
                    contents.preview()
                )
            );

            // Every upgrade recipe has exactly one crate in it.
            break;
        }

        return crafted;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return HumanCrates.CRATE_UPGRADE_SERIALIZER.get();
    }

    private static void write(RegistryFriendlyByteBuf buffer, CrateUpgradeRecipe recipe) {
        ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.getGroup());
        buffer.writeEnum(recipe.category());
        ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
        ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        buffer.writeBoolean(recipe.showNotification());
    }

    private static CrateUpgradeRecipe read(RegistryFriendlyByteBuf buffer) {
        var group = ByteBufCodecs.STRING_UTF8.decode(buffer);
        var category = buffer.readEnum(CraftingBookCategory.class);
        var pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
        var result = ItemStack.STREAM_CODEC.decode(buffer);
        var showNotification = buffer.readBoolean();

        return new CrateUpgradeRecipe(group, category, pattern, result, showNotification);
    }
}
