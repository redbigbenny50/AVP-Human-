package com.human.common.gameplay.gene.discovery;

import com.human.common.registry.init.HumanGeneticist;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Genetic Research - the printout from the Analyzer ([stated] the Analyzer's PRINT tab, one paper + one black dye
 * each): the genes and species ticked when it was printed. Using it teaches all of them to whoever reads it - "THE
 * DOCUMENT TEACHES IT TO WHOEVER USES IT" - and it is used up. How discoveries get shared between players, since each
 * player's are their own.
 */
public class GeneticResearchItem extends Item {

    private static final String KEY = "avp_human_genetic_research";

    public GeneticResearchItem(Properties properties) {
        super(properties);
    }

    /** A document teaching these genes and species (ids as strings). */
    public static ItemStack of(Collection<String> genes, Collection<String> species) {
        var stack = new ItemStack(HumanGeneticist.GENETIC_RESEARCH.get());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            var tag = new net.minecraft.nbt.CompoundTag();
            tag.put("genes", list(genes));
            tag.put("species", list(species));
            root.put(KEY, tag);
        });
        return stack;
    }

    private static ListTag list(Collection<String> values) {
        var list = new ListTag();
        for (var value : values) {
            list.add(StringTag.valueOf(value));
        }
        return list;
    }

    private static List<String> read(ItemStack stack, String field) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return List.of();
        }
        var root = data.copyTag();
        if (!root.contains(KEY)) {
            return List.of();
        }
        var out = new ArrayList<String>();
        for (var element : root.getCompound(KEY).getList(field, Tag.TAG_STRING)) {
            out.add(element.getAsString());
        }
        return out;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var genes = read(stack, "genes");
        var species = read(stack, "species");

        if (genes.isEmpty() && species.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            var geneIds = genes.stream().map(ResourceLocation::tryParse).filter(java.util.Objects::nonNull).toList();
            GeneDiscoveries.discover(serverPlayer, geneIds, null);
            for (var typeId : species) {
                EntityType.byString(typeId).ifPresent(type -> GeneDiscoveries.discover(serverPlayer, List.of(), type));
            }
            player.displayClientMessage(
                Component.literal("Studied: " + genes.size() + " genes, " + species.size() + " species."),
                true
            );
            level.playSound(null, player, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.consume(1, player);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return !read(stack, "genes").isEmpty() || !read(stack, "species").isEmpty();
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @NotNull TooltipContext context,
        @NotNull List<Component> lines,
        @NotNull TooltipFlag flag
    ) {
        var genes = read(stack, "genes");
        var species = read(stack, "species");
        if (genes.isEmpty() && species.isEmpty()) {
            lines.add(Component.literal("Blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        lines.add(Component.literal(genes.size() + " genes, " + species.size() + " species").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Use to study it - it is used up").withStyle(ChatFormatting.DARK_GRAY));
    }
}
