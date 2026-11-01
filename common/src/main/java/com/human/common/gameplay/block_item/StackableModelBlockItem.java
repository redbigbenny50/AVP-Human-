package com.human.common.gameplay.block_item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * A block item drawn with a 3D model that must still STACK.
 * <p>
 * Oct 29 BUG: model block items did not stack - in the player's inventory or in Refined Storage. They were registered
 * as AzureLib "identity" items, and BLib stamps every new stack of an identity item with its own RANDOM animation id
 * ({@code AZ_ID}), so no two stacks were ever equal. The id only exists so each held stack can play its own animation;
 * these items have no item animation at all (their renderers have no animator), so they no longer register for one.
 * </p>
 * <p>
 * Stacks made before the fix still carry an id. {@link #stripAnimationId} removes it whenever such a stack sits in a
 * player's inventory, so old stacks join new ones the first time a player carries them.
 * </p>
 */
public class StackableModelBlockItem extends BlockItem {

    public StackableModelBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        stripAnimationId(stack, level);
    }

    /** Server side only, so the client copy follows by the normal slot sync. */
    public static void stripAnimationId(ItemStack stack, Level level) {
        if (!level.isClientSide && stack.has(com.blib.mod.common.registry.init.BLibDataComponents.AZ_ID.get())) {
            stack.remove(com.blib.mod.common.registry.init.BLibDataComponents.AZ_ID.get());
        }
    }
}
