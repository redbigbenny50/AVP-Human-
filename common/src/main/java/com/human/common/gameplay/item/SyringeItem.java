package com.human.common.gameplay.item;

import com.human.common.gameplay.component.SyringeContents;
import com.human.common.gameplay.component.SyringeMode;
import com.human.common.gameplay.gene.GeneBonusDataEntry;
import com.human.common.gameplay.gene.GeneOperationType;
import com.human.common.gameplay.gene.Genes;
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

import java.util.List;

public class SyringeItem extends Item {

    private static final SyringeMode DEFAULT_SYRINGE_MODE = SyringeMode.EXTRACT;

    public SyringeItem() {
        super(
            new Item.Properties().stacksTo(1)
                .component(HumanDataComponents.SYRINGE_MODE.get(), DEFAULT_SYRINGE_MODE)
                .durability(20)
        );
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level level,
        @NotNull Player player,
        @NotNull InteractionHand hand
    ) {
        var stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                cycleSyringeMode(stack, player);
            }

            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            if (!level.isClientSide) {
                var mode = stack.getOrDefault(HumanDataComponents.SYRINGE_MODE.get(), DEFAULT_SYRINGE_MODE);

                // If mode is EMPTY, forcibly set SYRINGE_CONTENTS to its EMPTY value.
                if (mode == SyringeMode.EMPTY) {
                    emptySyringe(stack);
                    // TODO: Use proper translatable here.
                    player.displayClientMessage(Component.literal("Syringe has been emptied."), true);
                }

                // [stated] Oct 30: a player injects THEMSELVES with a filled syringe in INJECT mode, used in the air -
                // a temporary 3-day infusion in one slot (see PlayerGeneInfusion). The syringe empties as it would on
                // a mob; the integrity cost does not apply to a temporary dose. An empty CLEANSED syringe used the same
                // way is a pure cleanse shot: it ends the infusion.
                if (
                    mode == SyringeMode.INJECT
                        && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                        && com.human.common.gameplay.gene.infusion.PlayerGeneInfusion.injectSelf(serverPlayer, stack)
                ) {
                    emptySyringe(stack);
                    stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
                    player.setItemInHand(hand, stack);
                    return InteractionResultHolder.consume(stack);
                }
            }

            return super.use(level, player, hand);
        }
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        var player = context.getPlayer();

        if (player == null) {
            return super.useOn(context);
        }

        if (player.isShiftKeyDown()) {
            if (!context.getLevel().isClientSide()) {
                cycleSyringeMode(context.getItemInHand(), context.getPlayer());
            }

            return InteractionResult.SUCCESS;
        } else {
            if (!context.getLevel().isClientSide()) {
                var stack = context.getItemInHand();
                var mode = stack.getOrDefault(HumanDataComponents.SYRINGE_MODE.get(), DEFAULT_SYRINGE_MODE);

                if (mode == SyringeMode.EMPTY) {
                    emptySyringe(stack);
                    // TODO: Use proper translatable here.
                    player.displayClientMessage(Component.literal("Syringe has been emptied."), true);
                }
            }

            return super.useOn(context);
        }
    }

    /**
     * [stated] Oct 27 - an empty syringe defaults to EXTRACT, a full one to INJECT. Emptying (or injecting) therefore
     * always lands back on EXTRACT; shift + right click still cycles through every mode by hand.
     */
    /** Empties the syringe and forgets its donor; back to EXTRACT. Also used by the gene reader's CLEAR SAMPLE. */
    public static void emptySyringe(ItemStack stack) {
        forgetSample(stack);
        stack.set(HumanDataComponents.SYRINGE_CONTENTS.get(), SyringeContents.EMPTY);
        stack.set(HumanDataComponents.SYRINGE_MODE.get(), SyringeMode.EXTRACT);
        net.minecraft.world.item.component.CustomData.update(
            net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            stack,
            tag -> {
                tag.remove(DONOR_KEY);
                // A cleanse is used up with the syringe's contents ([stated] Oct 29: one cleanse per syringe).
                tag.remove(com.human.common.gameplay.gene.geneticist.GeneCleanser.KEY);
            }
        );
    }

    /** Where the syringe remembers which creature it was drawn from (entity type id), for the gene reader. */
    public static final String DONOR_KEY = "avp_human_syringe_donor";

    /** The entity type the sample in this syringe came from, or null. */
    // ------------------------------------------------------------------------------------------------------------
    // Analyzed - [stated] Nov 1: only the Gene Analyzer decodes a syringe; until then its genes are hidden everywhere
    // (tooltip, gene reader). A syringe the Splicer made counts as analyzed - its maker built it from known genes.
    // ------------------------------------------------------------------------------------------------------------

    private static final String ANALYZED = "avp_human_analyzed";

    public static boolean isAnalyzed(ItemStack stack) {
        if (com.human.common.gameplay.gene.machine.GeneSplicing.SPLICED.equals(donorOf(stack))) {
            return true;
        }
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(ANALYZED);
    }

    public static void markAnalyzed(ItemStack stack) {
        net.minecraft.world.item.component.CustomData.update(
            net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            stack,
            tag -> tag.putBoolean(ANALYZED, true)
        );
    }

    /**
     * [stated] Nov 1 - the name the gene reader gave this sample (SAMPLE-COW-3), which is also how the reader's history
     * knows a syringe it has read before. Null until a reader has read it.
     */
    private static final String SAMPLE_ID = "avp_human_sample_id";

    public static @org.jetbrains.annotations.Nullable String sampleIdOf(ItemStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        var tag = data.copyTag();
        return tag.contains(SAMPLE_ID) ? tag.getString(SAMPLE_ID) : null;
    }

    public static void setSampleId(ItemStack stack, String id) {
        net.minecraft.world.item.component.CustomData.update(
            net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            stack,
            tag -> tag.putString(SAMPLE_ID, id)
        );
    }

    /**
     * A new sample (or none): the old one's ANALYZED stamp and reader name go. So does the item's name, but only while
     * it is still the reader's auto-name - a name the player typed is theirs and stays.
     */
    private static void forgetSample(ItemStack stack) {
        var id = sampleIdOf(stack);
        var name = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (id != null && name != null && name.getString().equals(id)) {
            stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        }
        net.minecraft.world.item.component.CustomData.update(
            net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            stack,
            tag -> {
                tag.remove(ANALYZED);
                tag.remove(SAMPLE_ID);
            }
        );
    }

    public static @org.jetbrains.annotations.Nullable String donorOf(ItemStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        var tag = data.copyTag();
        return tag.contains(DONOR_KEY) ? tag.getString(DONOR_KEY) : null;
    }

    public static final double INTEGRITY_COST_PER_INJECTION = -0.10;

    public static final double INTEGRITY_COST_PER_EXTRA_GENE = -0.05;

    /** Integrity change one injection of this syringe causes (negative). */
    public static double injectionIntegrityCost(SyringeContents contents) {
        var genes = (int) contents.geneBonusDataEntries().stream().map(GeneBonusDataEntry::id).distinct().count();
        return INTEGRITY_COST_PER_INJECTION + INTEGRITY_COST_PER_EXTRA_GENE * Math.max(0, genes - 1);
    }

    private static boolean hasSample(SyringeContents contents) {
        return !contents.equals(SyringeContents.EMPTY) && !contents.geneBonusDataEntries().isEmpty();
    }

    private void cycleSyringeMode(ItemStack stack, Player player) {
        var current = stack.getOrDefault(HumanDataComponents.SYRINGE_MODE.get(), DEFAULT_SYRINGE_MODE);
        var next = nextMode(current);
        stack.set(HumanDataComponents.SYRINGE_MODE.get(), next);

        player.displayClientMessage(
            // TODO: Use proper translatable here.
            Component.literal("Switched mode to ")
                .append(
                    Component.literal(next.toString())
                        .withStyle(ChatFormatting.YELLOW)
                ),
            true
        );
    }

    private SyringeMode nextMode(SyringeMode current) {
        var values = SyringeMode.values();
        var nextOrdinal = (current.ordinal() + 1) % values.length;
        return values[nextOrdinal];
    }

    public static void interact(
        @NotNull ItemStack stack,
        @NotNull Player player,
        @NotNull LivingEntity interactionTarget,
        @NotNull InteractionHand usedHand
    ) {
        var syringeContents = stack.getOrDefault(HumanDataComponents.SYRINGE_CONTENTS.get(), SyringeContents.EMPTY);
        var syringeMode = stack.getOrDefault(HumanDataComponents.SYRINGE_MODE.get(), DEFAULT_SYRINGE_MODE);

        switch (syringeMode) {
            case EMPTY -> {
                emptySyringe(stack);
                // Assign stack back to player hand to update client-side.
                player.setItemInHand(usedHand, stack);
            }
            case EXTRACT -> {
                // [stated] Oct 27 - a syringe holds one sample. Extracting into a full one used to merge the new mob's
                // genes over the old ones; now it refuses and says why.
                if (hasSample(syringeContents)) {
                    // TODO: Use proper translatable here.
                    player.displayClientMessage(Component.literal("Syringe already contains a sample, please empty first."), true);
                    return;
                }

                // Extract genes from mob.
                var geneBonusMapData = GeneBonusDataRegistry.getOrDefault(interactionTarget.getType());

                // [stated] Oct 25 - the donor rests 2.5 minutes after a draw.
                var donorGeneManager = ((GeneCarrier) interactionTarget).getOrCreateGeneManager();
                var cooldown = donorGeneManager.getSampleCooldownRemaining();

                if (cooldown > 0) {
                    // TODO: Use proper translatable here.
                    player.displayClientMessage(
                        Component.literal("This creature needs to recover first (" + ((cooldown + 19) / 20) + "s)."),
                        true
                    );
                    return;
                }

                // [stated] Oct 27 - a draw extracts 100% of the donor's species value of each gene: one draw is one
                // dose, 8 from a Prime donor to reach a cap. Genetic integrity is the line's health, never drawn.
                var geneBonusDataEntries = new java.util.ArrayList<GeneBonusDataEntry>();
                geneBonusMapData.forEach((key, value) -> {
                    if (key.resourceLocation().equals(Genes.GENETIC_INTEGRITY.id()) || value == 0.0) {
                        return;
                    }
                    geneBonusDataEntries.add(new GeneBonusDataEntry(key.resourceLocation(), key.operation(), value));
                });

                // Loot genes: one per item the donor's loot table can drop, one dose (+10%) each per draw.
                if (
                    interactionTarget.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                        && com.human.common.gameplay.gene.LootGenes.canDonate(interactionTarget)
                ) {
                    for (var item : com.human.common.gameplay.gene.LootGenes.scan(serverLevel, interactionTarget, player)) {
                        geneBonusDataEntries.add(
                            new GeneBonusDataEntry(
                                com.human.common.gameplay.gene.LootGenes.geneFor(item).id(),
                                GeneOperationType.ADDITIVE,
                                com.human.common.gameplay.gene.LootGenes.DOSE
                            )
                        );
                    }
                }

                // [stated] Oct 28 - a draw also takes the donor's ACTIVE mutations (each as its own gene, so the
                // Separator
                // can pull it into a tube) and the defects it carries (so the Separator can filter them out).
                var donorState = donorGeneManager.getAdvancedState();
                var donorContainer = donorGeneManager.getGeneContainer();
                for (var definition : com.human.common.gameplay.gene.AdvancedGenes.definitions()) {
                    if (donorState.isActive(definition.gene())) {
                        geneBonusDataEntries.add(new GeneBonusDataEntry(definition.gene().id(), GeneOperationType.ADDITIVE, 1.0));
                    }
                }
                for (var defect : com.human.common.gameplay.gene.GeneDefects.all()) {
                    if (com.human.common.gameplay.gene.GeneDefects.carries(donorContainer, defect)) {
                        geneBonusDataEntries.add(
                            new GeneBonusDataEntry(
                                defect.id(),
                                GeneOperationType.ADDITIVE,
                                donorContainer.getActiveGeneMap().getValue(defect)
                            )
                        );
                    }
                }

                if (geneBonusDataEntries.isEmpty()) {
                    // TODO: Use proper translatable here.
                    player.displayClientMessage(Component.literal("This creature has no genetic data to extract."), true);
                    return;
                }

                donorGeneManager.startSampleCooldown();
                // [stated] Nov 1: drawing only counts the draw - the sample has to be analyzed to learn what is in it.
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    com.human.common.gameplay.gene.discovery.GeneDiscoveries.countDraw(serverPlayer, interactionTarget.getType());
                }
                // A fresh draw is a new, unanalyzed, unnamed sample.
                forgetSample(stack);
                net.minecraft.world.item.component.CustomData.update(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    stack,
                    tag -> tag.putString(
                        DONOR_KEY,
                        net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(interactionTarget.getType()).toString()
                    )
                );
                // Assign syringe contents back to syringe item stack.
                stack.set(HumanDataComponents.SYRINGE_CONTENTS.get(), new SyringeContents(geneBonusDataEntries));
                // [stated] Oct 27 - a filled syringe is ready to inject.
                stack.set(HumanDataComponents.SYRINGE_MODE.get(), SyringeMode.INJECT);
                stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
                // Assign stack back to player hand to update client-side.
                player.setItemInHand(usedHand, stack);
                // Hurt flash + sound only - no damage, not counted as a hurt (see GeneSamplePrick).
                GeneSamplePrick.prick(interactionTarget);
                // TODO: Use proper translatable here.
                player.displayClientMessage(Component.literal("Extracted genes from target."), true);
            }
            case INJECT -> {
                // An empty syringe used to "inject" nothing and still cost the mob integrity. [stated] Oct 29: a
                // CLEANSED
                // empty syringe is the exception - it is a pure cleansing shot.
                var cleanseBonus = com.human.common.gameplay.gene.geneticist.GeneCleanser.bonusOf(stack);
                if (!hasSample(syringeContents) && cleanseBonus <= 0.0) {
                    // TODO: Use proper translatable here.
                    player.displayClientMessage(Component.literal("Syringe is empty, extract a sample first."), true);
                    return;
                }

                // Injection keeps its old rule: only creatures with species gene data take genes.
                if (!GeneBonusDataRegistry.has(interactionTarget.getType())) {
                    // TODO: Use proper translatable here.
                    player.displayClientMessage(Component.literal("This creature cannot take genes."), true);
                    return;
                }

                var geneCarrier = (GeneCarrier) interactionTarget;
                var targetContainer = geneCarrier.getOrCreateGeneManager().getGeneContainer();

                // Injected genes sleep in the injected mob and wake in its offspring (each gene still capped).
                // [stated] Oct 28 - an injected MUTATION is different: it locks in as ENGINEERED on the mob straight
                // away,
                // replacing any engineered gene it already has in that category.
                var sleeping = new java.util.ArrayList<GeneBonusDataEntry>();
                for (var entry : syringeContents.geneBonusDataEntries()) {
                    var gene = com.human.common.gameplay.gene.GeneRegistry.getValueOrNull(entry.id());
                    if (gene != null && com.human.common.gameplay.gene.AdvancedGenes.definition(gene) != null) {
                        targetContainer.lockEngineered(gene);
                    } else {
                        sleeping.add(entry);
                    }
                }
                targetContainer.getDormantGeneMap().putAll(GeneDataUtil.toMap(sleeping));

                // [stated] Oct 25 - integrity costs -0.10 per injection plus -0.05 for each extra gene in the syringe,
                // and
                // it hits the injected mob itself (its own integrity level, e.g. a queen's aberrant-egg chance).
                // [stated] Oct 29: a cleansed syringe adds its bonus on top, never past full integrity.
                var integrityMap = targetContainer.getActiveGeneMap();
                var currentIntegrity = integrityMap.getValue(Genes.GENETIC_INTEGRITY, GeneOperationType.ADDITIVE);
                var newIntegrity = com.human.common.gameplay.gene.geneticist.GeneCleanser.applyTo(
                    currentIntegrity,
                    injectionIntegrityCost(syringeContents),
                    cleanseBonus
                );
                integrityMap.add(Genes.GENETIC_INTEGRITY.id(), GeneOperationType.ADDITIVE, newIntegrity - currentIntegrity);

                // The first injection makes the line MODIFIED; degradation is tracked from here on.
                targetContainer.setModified(true);

                emptySyringe(stack);
                stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
                // Assign stack back to player hand to update client-side.
                player.setItemInHand(usedHand, stack);
                // Hurt flash + sound only - no damage, not counted as a hurt (see GeneSamplePrick).
                GeneSamplePrick.prick(interactionTarget);
                // TODO: Use proper translatable here.
                player.displayClientMessage(
                    Component.literal(hasSample(syringeContents) ? "Injected target with genes." : "Injected a cleansing shot."),
                    true
                );
            }
        }
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

        var mode = itemStack.getOrDefault(HumanDataComponents.SYRINGE_MODE.get(), DEFAULT_SYRINGE_MODE);

        list.add(
            Component.literal("Current Mode: ")
                .append(
                    Component.literal(mode.toString())
                        .withStyle(ChatFormatting.YELLOW)
                )
        );

        if (com.human.common.gameplay.gene.geneticist.GeneCleanser.isCleansed(itemStack)) {
            list.add(
                Component.translatable(
                    "item.avp_human.syringe.cleansed",
                    Math.round(com.human.common.gameplay.gene.geneticist.GeneCleanser.bonusOf(itemStack) * 100)
                ).withStyle(ChatFormatting.GREEN)
            );
        }

        var syringeContents = itemStack.getOrDefault(HumanDataComponents.SYRINGE_CONTENTS.get(), SyringeContents.EMPTY);

        if (syringeContents.equals(SyringeContents.EMPTY) || syringeContents.geneBonusDataEntries().isEmpty()) {
            return;
        }

        list.add(CommonComponents.EMPTY);
        var donor = donorOf(itemStack);
        // [stated] Nov 1: one line per gene. A gene is NAMED (with its strength) only once a Gene Analyzer has decoded
        // the syringe AND this player knows the gene or has profiled the donor; otherwise it is ??? with no strength.
        var analyzed = isAnalyzed(itemStack);
        var profiled = analyzed && com.human.common.gameplay.gene.discovery.ClientGeneDiscoveries.hasProfile(donor);
        list.add(Component.literal(analyzed ? "Genes:" : "Unanalyzed sample:").withStyle(ChatFormatting.YELLOW));
        var hidden = false;
        for (var entry : syringeContents.geneBonusDataEntries()) {
            var named = analyzed && (profiled || com.human.common.gameplay.gene.discovery.ClientGeneDiscoveries.knowsGene(entry.id()));
            hidden |= !named;
            list.add(GeneTooltips.line(entry.id(), entry.operation(), entry.value(), named));
        }
        if (donor != null) {
            list.add(GeneTooltips.donor(donor));
        }
        if (!analyzed) {
            list.add(Component.literal("Analyze it in a Gene Analyzer to decode it").withStyle(ChatFormatting.DARK_GRAY));
        } else if (hidden) {
            list.add(Component.literal("Profile the donor with a gene reader to name them").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
