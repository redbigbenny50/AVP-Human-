package com.human.common.gameplay.storage;

import com.human.common.registry.init.HumanCrates;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A crate in item form. Placing it puts down the WHOLE crate (every block, facing the player) or nothing at all; a full
 * crate's contents come back out of {@link CrateStorageData} as it lands.
 * <p>
 * A full crate item can't burn, can't be blown up or hurt, and never despawns (see the ItemEntity mixin), so a crate
 * knocked loose by an explosion or acid can never take its contents with it.
 */
public class CrateItem extends BlockItem {

    private final CrateType type;

    public CrateItem(Block block, CrateType type, Properties properties) {
        super(block, properties);
        this.type = type;
    }

    public CrateType type() {
        return type;
    }

    public static boolean hasContents(ItemStack stack) {
        return stack.getItem() instanceof CrateItem && stack.has(HumanCrates.CRATE_CONTENTS.get());
    }

    /** Marks a crate stack as full: its contents id, and the protections a full crate item gets. */
    static void markFull(ItemStack stack, CrateContents contents) {
        stack.set(HumanCrates.CRATE_CONTENTS.get(), contents);
        stack.set(DataComponents.FIRE_RESISTANT, Unit.INSTANCE);
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
    }

    @Override
    public @NotNull InteractionResult place(@NotNull BlockPlaceContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var anchor = context.getClickedPos();
        var facing = context.getHorizontalDirection();
        var state = getBlock().defaultBlockState().setValue(CrateBlock.FACING, facing);
        var positions = CrateFootprint.positions(anchor, facing, type.size());

        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        }

        for (var pos : positions) {
            var blocking = !level.isInWorldBounds(pos)
                || !level.getBlockState(pos).canBeReplaced()
                || !level.isUnobstructed(state, pos, CollisionContext.empty());

            if (blocking) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                        Component.translatable("message.avp_human.crate.no_room").withStyle(ChatFormatting.RED),
                        true
                    );
                }

                return InteractionResult.FAIL;
            }
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        var placed = new java.util.ArrayList<net.minecraft.core.BlockPos>(positions.size());

        for (var pos : positions) {
            if (!serverLevel.setBlock(pos, state, Block.UPDATE_CLIENTS)) {
                // Another mod or a protected area refused a block: undo the ones already placed and keep the
                // item (and its contents) untouched. EVERY placed block is marked dismantled first: none of them
                // know their master yet, so each would otherwise act as its own master and drop an empty crate.
                for (var undo : placed) {
                    if (serverLevel.getBlockEntity(undo) instanceof CrateBlockEntity half) {
                        half.setDismantled();
                    }
                }

                for (var undo : placed) {
                    if (serverLevel.getBlockState(undo).is(getBlock())) {
                        serverLevel.removeBlock(undo, false);
                    }
                }

                return InteractionResult.FAIL;
            }

            placed.add(pos);
        }

        for (var pos : positions) {
            if (serverLevel.getBlockEntity(pos) instanceof CrateBlockEntity part) {
                part.setMasterPos(anchor);
            }
        }

        if (serverLevel.getBlockEntity(anchor) instanceof CrateBlockEntity master) {
            var stack = context.getItemInHand();
            var contents = stack.get(HumanCrates.CRATE_CONTENTS.get());

            if (contents != null) {
                var items = CrateStorageData.get(serverLevel.getServer()).take(contents.id());

                if (items != null) {
                    master.inventory().load(items, serverLevel.registryAccess());
                    master.setChanged();
                }
            }
        }

        for (var pos : positions) {
            serverLevel.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            serverLevel.updateNeighborsAt(pos, state.getBlock());
        }

        var sound = state.getSoundType();

        serverLevel.playSound(
            null,
            anchor,
            getPlaceSound(state),
            SoundSource.BLOCKS,
            (sound.getVolume() + 1.0F) / 2.0F,
            sound.getPitch() * 0.8F
        );
        serverLevel.gameEvent(GameEvent.BLOCK_PLACE, anchor, GameEvent.Context.of(player, state));

        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("tooltip.avp_human.crate.capacity", type.size().slots()).withStyle(ChatFormatting.GRAY));

        var contents = stack.get(HumanCrates.CRATE_CONTENTS.get());

        if (contents == null) {
            return;
        }

        tooltip.add(
            Component.translatable(
                "tooltip.avp_human.crate.contents",
                contents.items(),
                contents.usedSlots(),
                contents.totalSlots()
            ).withStyle(ChatFormatting.GOLD)
        );

        for (var line : contents.preview()) {
            tooltip.add(
                Component.literal("  " + line.count() + "x ")
                    .append(line.stack().getHoverName())
                    .withStyle(ChatFormatting.DARK_GRAY)
            );
        }
    }
}
