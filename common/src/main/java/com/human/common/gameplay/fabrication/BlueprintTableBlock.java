package com.human.common.gameplay.fabrication;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * [stated] Oct 13: the blueprint table - the same block as the commissary villager's job site. It needs no power. It
 * keeps nothing of its own: like a stonecutter, whatever is in its slots goes back to the player when the screen
 * closes, so there is nothing in the block to lose when it is broken or its chunk unloads.
 */
public class BlueprintTableBlock extends Block {

    private static final Component TITLE = Component.translatable("container.avp_human.blueprint_table");

    public BlueprintTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull BlockHitResult hit
    ) {
        if (!level.isClientSide) {
            player.openMenu(
                new SimpleMenuProvider(
                    (id, inventory, opener) -> new BlueprintTableMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
                    TITLE
                )
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
