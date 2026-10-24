package com.human.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PRESERVE SPAWNERS ([stated] Oct 15: "spawners collected keeping their type"). The quarry hands out a spawner item
 * carrying its block-entity data, but vanilla only lets OPERATORS place a spawner with data
 * (SpawnerBlockEntity.onlyOpCanSetNbt) - so in survival the spawner the quarry dug up went down EMPTY, its mob lost.
 * <p>
 * This lets the data through for spawners the quarry itself marked ({@link #QUARRIED}, in the stack's custom data), and
 * nothing else: a spawner given with data by any other route still needs an operator, exactly as vanilla.
 * </p>
 */
@Mixin(BlockItem.class)
public class MixinBlockItem_QuarriedSpawner {

    /** The custom-data flag the quarry puts on a spawner it dug up. */
    @org.spongepowered.asm.mixin.Unique
    private static final String QUARRIED = "avp_human_quarried";

    @Inject(
        method = "updateCustomBlockEntityTag(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"), cancellable = true
    )
    private static void avp_human$quarriedSpawner(
        Level level,
        @Nullable Player player,
        BlockPos pos,
        ItemStack stack,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (level.isClientSide || level.getServer() == null) {
            return;
        }

        var marker = stack.get(DataComponents.CUSTOM_DATA);
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);

        if (marker == null || data == null || !marker.copyTag().getBoolean(QUARRIED)) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
            callback.setReturnValue(data.loadInto(spawner, level.registryAccess()));
        }
    }
}
