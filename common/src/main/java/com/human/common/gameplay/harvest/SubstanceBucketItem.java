package com.human.common.gameplay.harvest;

import com.human.common.registry.init.HumanHarvest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A bucket of a substance that has no fluid of its own (harvest genes). One item for every substance: the
 * {@link HumanHarvest#SUBSTANCE} component names the item inside and the name reads "Bucket of Slimeball". The fill is
 * a small overlay on vanilla's bucket, picked per substance by the model's custom-model-data overrides.
 * <ul>
 * <li>[stated] Oct 25 - if the item packs 9-into-a-block (slime balls into a slime block), the bucket places that
 * block, the way vanilla's powder snow bucket does, and comes back empty.</li>
 * <li>Otherwise it can't be placed.</li>
 * <li>In a crafting grid it gives back 9 of the item and the empty bucket (SubstanceBucketUnpackRecipe).</li>
 * </ul>
 */
public class SubstanceBucketItem extends Item {

    public static final int ITEMS_PER_BUCKET = 9;

    /** Item → the block 9 of it pack into (or none), found from the crafting recipes. Per recipe-manager session. */
    private static final Map<Item, Block> PACKED_BLOCK = new ConcurrentHashMap<>();

    private static @Nullable Object packedFor;

    public SubstanceBucketItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Item substance) {
        var stack = new ItemStack(HumanHarvest.SUBSTANCE_BUCKET.get());
        stack.set(HumanHarvest.SUBSTANCE.get(), BuiltInRegistries.ITEM.getKey(substance));

        // Picks the fill colour through the item model's overrides (no client code): 1 slime, 2 ink, none = the plain
        // fill. A new substance gets a fill texture and an override line in models/item/substance_bucket.json.
        var look = substance == Items.SLIME_BALL ? 1 : substance == Items.INK_SAC ? 2 : 0;
        if (look != 0) {
            stack.set(
                net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                new net.minecraft.world.item.component.CustomModelData(look)
            );
        }

        return stack;
    }

    /** The item inside, or AIR. */
    public static Item substanceOf(ItemStack stack) {
        ResourceLocation id = stack.get(HumanHarvest.SUBSTANCE.get());
        return id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        var substance = substanceOf(stack);

        if (substance == Items.AIR) {
            return super.getName(stack);
        }

        return Component.translatable("item.avp_human.substance_bucket.of", substance.getDescription());
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        var level = context.getLevel();
        var stack = context.getItemInHand();
        var block = packedBlock(level, substanceOf(stack));

        if (block == null) {
            return InteractionResult.PASS;
        }

        var placeContext = new BlockPlaceContext(context);
        var pos = placeContext.getClickedPos();

        if (!placeContext.canPlace()) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            var state = block.getStateForPlacement(placeContext);
            if (state == null || !level.setBlock(pos, state, Block.UPDATE_ALL)) {
                return InteractionResult.FAIL;
            }

            var sound = state.getSoundType();
            level.playSound(
                null,
                pos,
                sound.getPlaceSound(),
                SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F
            );
            level.gameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, pos);

            var player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild) {
                player.setItemInHand(context.getHand(), new ItemStack(Items.BUCKET));
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * The block 9 of this item pack into, found by asking the crafting recipes what a 3x3 grid full of it makes - so it
     * works for modded items too. Null when nothing packs.
     */
    public static @Nullable Block packedBlock(Level level, Item item) {
        if (item == Items.AIR) {
            return null;
        }

        var recipes = level.getRecipeManager();
        if (packedFor != recipes) {
            PACKED_BLOCK.clear();
            packedFor = recipes;
        }

        var cached = PACKED_BLOCK.get(item);
        if (cached != null) {
            return cached == net.minecraft.world.level.block.Blocks.AIR ? null : cached;
        }

        var grid = CraftingInput.of(3, 3, Collections.nCopies(ITEMS_PER_BUCKET, new ItemStack(item)));
        var result = recipes.getRecipeFor(RecipeType.CRAFTING, grid, level)
            .map(holder -> holder.value().assemble(grid, level.registryAccess()))
            .orElse(ItemStack.EMPTY);
        var block = result.getCount() == 1 && result.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;

        PACKED_BLOCK.put(item, block == null ? net.minecraft.world.level.block.Blocks.AIR : block);
        return block;
    }

    /** Every substance that comes out in this bucket today (for recipe viewers). */
    public static List<Item> knownSubstances() {
        return List.of(Items.SLIME_BALL, Items.INK_SAC);
    }
}
