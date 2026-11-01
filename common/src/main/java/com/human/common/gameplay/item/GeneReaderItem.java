package com.human.common.gameplay.item;

import com.human.common.gameplay.component.GeneReaderContents;
import com.human.common.gameplay.component.GeneReaderMode;
import com.human.common.gameplay.gene.GeneBonusDataEntry;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.gene.GeneRegistry;
import com.human.common.model.GeneCarrier;
import com.human.common.registry.GeneBonusDataRegistry;
import com.human.common.registry.init.HumanDataComponents;
import com.human.common.util.GeneDataUtil;
import com.human.common.util.GeneSamplePrick;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;

public class GeneReaderItem extends Item {

    private static final GeneReaderMode DEFAULT_MODE = GeneReaderMode.BONUS_GENES;

    public GeneReaderItem() {
        super(
            new Properties().stacksTo(1)
                .component(HumanDataComponents.GENE_READER_MODE.get(), DEFAULT_MODE)
                .durability(1024)
        );
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level level,
        @NotNull Player player,
        @NotNull InteractionHand hand
    ) {
        var stack = player.getItemInHand(hand);

        // [stated] Oct 27 approved screen footer: RIGHT-CLICK MOB: SAMPLE / SHIFT-CLICK: SELF / RIGHT-CLICK AIR: OPEN.
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                interact(stack, player, player, hand);
            } else {
                player.openMenu(
                    new net.minecraft.world.SimpleMenuProvider(
                        (containerId, inventory, opener) -> new com.human.common.gameplay.gene.reader.GeneReaderMenu(
                            containerId,
                            inventory
                        ),
                        Component.translatable("gui.avp_human.gene_reader")
                    )
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        // Clicking a block does what clicking the air does (open the screen, or sample yourself when sneaking).
        return InteractionResult.PASS;
    }

    public static void interact(
        @NotNull ItemStack stack,
        @NotNull Player player,
        @NotNull LivingEntity interactionTarget,
        @NotNull InteractionHand usedHand
    ) {
        // ⚠ Used to stop here for any creature without a species gene file, so the reader did nothing at all on it.
        // Every living thing is read now: its active and dormant genes are real either way, and the species list is
        // simply empty ("No gene data found." in the tooltip).
        var geneBonusMapData = GeneBonusDataRegistry.getOrDefault(interactionTarget.getType());

        var geneContainer = ((GeneCarrier) interactionTarget).getOrCreateGeneManager().getGeneContainer();
        var geneReaderContents = new EnumMap<GeneReaderMode, List<GeneBonusDataEntry>>(GeneReaderMode.class);

        geneReaderContents.put(GeneReaderMode.ACTIVE_GENES, GeneDataUtil.toList(geneContainer.getActiveGeneMap().getBackingMap()));
        geneReaderContents.put(GeneReaderMode.DORMANT_GENES, GeneDataUtil.toList(geneContainer.getDormantGeneMap().getBackingMap()));
        geneReaderContents.put(GeneReaderMode.BONUS_GENES, GeneDataUtil.toList(geneBonusMapData));

        // Assign gene reader contents back to gene reader item stack.
        stack.set(HumanDataComponents.GENE_READER_CONTENTS.get(), new GeneReaderContents(geneReaderContents));
        // Everything the reader SCREEN shows (genes, advanced states, integrity, timers), captured now.
        var snapshot = com.human.common.gameplay.gene.reader.GeneReaderSnapshot.capture(interactionTarget);
        snapshot.writeTo(stack);
        // [stated] Nov 1: reading a living creature PROFILES its species (and records the species as met). It no longer
        // discovers genes - only the Gene Analyzer teaches genes, and fully only for species profiled here.
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.human.common.gameplay.gene.discovery.GeneDiscoveries.profile(serverPlayer, interactionTarget.getType());
            com.human.common.gameplay.gene.discovery.GeneDiscoveries.discover(
                serverPlayer,
                java.util.List.of(),
                interactionTarget.getType()
            );
        }
        stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
        // Assign stack back to player hand to update client-side.
        player.setItemInHand(usedHand, stack);
        // Hurt flash + sound only - no damage, not counted as a hurt (see GeneSamplePrick).
        GeneSamplePrick.prick(interactionTarget);
        // TODO: Use proper translatable here.
        player.displayClientMessage(
            Component.literal(
                geneBonusMapData.isEmpty() ? "Processed gene data from target (no species data)." : "Processed gene data from target."
            ),
            true
        );
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack itemStack,
        @NotNull TooltipContext tooltipContext,
        @NotNull List<Component> list,
        @NotNull TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(itemStack, tooltipContext, list, tooltipFlag);
        // TODO: Use translatable here.
        list.add(Component.literal("Shift + Right Click to change mode.").withStyle(ChatFormatting.GRAY));
        list.add(CommonComponents.EMPTY);

        var geneReaderMode = itemStack.getOrDefault(HumanDataComponents.GENE_READER_MODE.get(), DEFAULT_MODE);

        list.add(
            Component.literal("Current Mode: ")
                .append(
                    Component.literal(geneReaderMode.toString())
                        .withStyle(ChatFormatting.YELLOW)
                )
        );

        var geneReaderContents = itemStack.getOrDefault(HumanDataComponents.GENE_READER_CONTENTS.get(), GeneReaderContents.EMPTY);

        if (geneReaderContents.equals(GeneReaderContents.EMPTY) || geneReaderMode == GeneReaderMode.CLEAR) {
            return;
        }

        var geneEntries = geneReaderContents.geneBonusDataEntriesByMode().getOrDefault(geneReaderMode, List.of());

        list.add(CommonComponents.EMPTY);
        // TODO: Make this translatable.
        list.add(
            Component.literal("Genes:")
                .withStyle(ChatFormatting.YELLOW)
        );

        if (geneEntries.isEmpty()) {
            list.add(
                Component.literal("No gene data found.")
                    .withStyle(ChatFormatting.GRAY)
            );
        } else {
            // TODO: Pretty this up later.
            geneEntries.stream()
                .filter(entry -> GeneRegistry.getValue(entry.id()).isSome())
                .forEach(
                    entry -> list.add(
                        Component.translatable(GeneRegistry.getValue(entry.id()).unwrap().getTranslationKey())
                            .append(Component.literal(": "))
                            .append(
                                Component.literal(format(entry.operation(), entry.value()))
                                    .withStyle(getColorForValue(entry.value()))
                            )
                    )
                );
        }
    }

    private String format(GeneOperationType operation, double value) {
        var suffix = operation == GeneOperationType.MULTIPLICATIVE ? "%" : "";
        var sign = value > 0 ? "+" : "";
        var formattedValue = String.format("%.2f", operation == GeneOperationType.MULTIPLICATIVE ? value * 100 : value);

        return sign + formattedValue + suffix;
    }

    private ChatFormatting getColorForValue(double value) {
        if (value > 0) {
            return ChatFormatting.GREEN;
        } else if (value < 0) {
            return ChatFormatting.RED;
        }

        return ChatFormatting.GRAY;
    }
}
