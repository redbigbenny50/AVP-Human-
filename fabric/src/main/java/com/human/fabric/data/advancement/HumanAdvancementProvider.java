package com.human.fabric.data.advancement;

import com.blib.mod.common.registry.init.BLibDataComponents;
import com.human.HumanResources;
import com.human.common.data.HumanAdvancements;
import com.human.common.registry.init.block.HumanSteelBlocks;
import com.human.common.registry.init.item.HumanGunItems;
import com.human.common.registry.init.item.HumanItems;
import com.human.common.registry.tag.HumanItemTags;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Consumer;

public class HumanAdvancementProvider {

    public static void generateAdvancements(HolderLookup.Provider registryLookup, Consumer<AdvancementHolder> consumer) {
        var root = Advancement.Builder.advancement()
            .display(
                HumanSteelBlocks.STEEL_BLOCK.get(),
                HumanAdvancements.ROOT.titleComponent(),
                HumanAdvancements.ROOT.descriptionComponent(),
                HumanResources.location("textures/gui/advancements/backgrounds/steel.png"),
                AdvancementType.TASK,
                false,
                false,
                false
            )
            .addCriterion("crafting_table", InventoryChangeTrigger.TriggerInstance.hasItems(Blocks.CRAFTING_TABLE))
            .save(consumer, HumanAdvancements.ROOT.resourceLocation().toString());

        var blastSteelAdvancement = addBlastSteelAdvancement(root, consumer);

        var smeltPlasticAdvancement = addSmeltPlasticAdvancement(blastSteelAdvancement, consumer);

        var hasGunAdvancement = addHasGunAdvancement(smeltPlasticAdvancement, consumer);

        var hireMarineAdvancement = addHireMarineAdvancement(hasGunAdvancement, consumer);

        var equipFullArmorSetWithArmorCaseAdvancement = addEquipFullArmorSetWithArmorCaseAdvancement(smeltPlasticAdvancement, consumer);

        var smeltBrassAdvancement = addSmeltBrassAdvancement(root, consumer);

        var smeltTitaniumAdvancement = addSmeltTitaniumAdvancement(blastSteelAdvancement, consumer);

        var fillCanisterAdvancement = addFillCanisterAdvancement(smeltTitaniumAdvancement, consumer);

        var selfMilkerAdvancement = addSelfMilkerAdvancement(root, consumer);
    }

    /** [stated] Oct 30: rare challenge, granted in code (SelfMilking) - hidden until earned. */
    private static AdvancementHolder addSelfMilkerAdvancement(AdvancementHolder parent, Consumer<AdvancementHolder> consumer) {
        return Advancement.Builder.advancement()
            .addCriterion("self_milker", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
            .parent(parent)
            .display(
                Items.MILK_BUCKET,
                HumanAdvancements.SELF_MILKER.titleComponent(),
                HumanAdvancements.SELF_MILKER.descriptionComponent(),
                null,
                AdvancementType.CHALLENGE,
                true,
                true,
                true
            )
            .save(consumer, HumanAdvancements.SELF_MILKER.resourceLocation().toString());
    }

    private static AdvancementHolder addBlastSteelAdvancement(AdvancementHolder parent, Consumer<AdvancementHolder> consumer) {
        return Advancement.Builder.advancement()
            .addCriterion("steel", InventoryChangeTrigger.TriggerInstance.hasItems(HumanItems.STEEL_INGOT.get()))
            .parent(parent)
            .display(
                HumanItems.STEEL_INGOT.get(),
                HumanAdvancements.BLAST_STEEL.titleComponent(),
                HumanAdvancements.BLAST_STEEL.descriptionComponent(),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .save(consumer, HumanAdvancements.BLAST_STEEL.resourceLocation().toString());
    }

    private static AdvancementHolder addEquipFullArmorSetWithArmorCaseAdvancement(
        AdvancementHolder parent,
        Consumer<AdvancementHolder> consumer
    ) {
        return Advancement.Builder.advancement()
            .addCriterion(
                "equip_full_armor_set_with_armor_case",
                CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance())
            )
            .parent(parent)
            .display(
                HumanItems.ARMOR_CASE.get(),
                HumanAdvancements.EQUIP_FULL_ARMOR_SET_WITH_ARMOR_CASE.titleComponent(),
                HumanAdvancements.EQUIP_FULL_ARMOR_SET_WITH_ARMOR_CASE.descriptionComponent(),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .save(consumer, HumanAdvancements.EQUIP_FULL_ARMOR_SET_WITH_ARMOR_CASE.resourceLocation().toString());
    }

    private static AdvancementHolder addFillCanisterAdvancement(AdvancementHolder parent, Consumer<AdvancementHolder> consumer) {
        return Advancement.Builder.advancement()
            .addCriterion("fill_canister", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
            .parent(parent)
            .display(
                HumanItems.LAVA_CANISTER.get(),
                HumanAdvancements.FILL_CANISTER.titleComponent(),
                HumanAdvancements.FILL_CANISTER.descriptionComponent(),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .save(consumer, HumanAdvancements.FILL_CANISTER.resourceLocation().toString());
    }

    private static AdvancementHolder addSmeltPlasticAdvancement(AdvancementHolder parent, Consumer<AdvancementHolder> consumer) {
        return Advancement.Builder.advancement()
            .addCriterion("polymer", InventoryChangeTrigger.TriggerInstance.hasItems(HumanItems.POLYMER.get()))
            .parent(parent)
            .display(
                HumanItems.POLYMER.get(),
                HumanAdvancements.SMELT_PLASTIC.titleComponent(),
                HumanAdvancements.SMELT_PLASTIC.descriptionComponent(),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .save(consumer, HumanAdvancements.SMELT_PLASTIC.resourceLocation().toString());
    }

    private static AdvancementHolder addHasGunAdvancement(AdvancementHolder parent, Consumer<AdvancementHolder> consumer) {
        var itemStack = new ItemStack(HumanGunItems.M41A_PULSE_RIFLE.get(), 1);

        itemStack.remove(BLibDataComponents.AZ_ID.get());

        return Advancement.Builder.advancement()
            .addCriterion(
                "gun",
                InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(HumanItemTags.GUNS).build())
            )
            .parent(parent)
            .display(
                itemStack,
                HumanAdvancements.HAS_GUN.titleComponent(),
                HumanAdvancements.HAS_GUN.descriptionComponent(),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .save(consumer, HumanAdvancements.HAS_GUN.resourceLocation().toString());
    }

    private static AdvancementHolder addHireMarineAdvancement(AdvancementHolder parent, Consumer<AdvancementHolder> consumer) {
        return Advancement.Builder.advancement()
            .addCriterion("hire_marine", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
            .parent(parent)
            .display(
                Items.DIAMOND,
                HumanAdvancements.HIRE_MARINE.titleComponent(),
                HumanAdvancements.HIRE_MARINE.descriptionComponent(),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .save(consumer, HumanAdvancements.HIRE_MARINE.resourceLocation().toString());
    }

    private static AdvancementHolder addSmeltBrassAdvancement(AdvancementHolder parent, Consumer<AdvancementHolder> consumer) {
        return Advancement.Builder.advancement()
            .addCriterion("brass", InventoryChangeTrigger.TriggerInstance.hasItems(HumanItems.BRASS_INGOT.get()))
            .parent(parent)
            .display(
                HumanItems.BRASS_INGOT.get(),
                HumanAdvancements.SMELT_BRASS.titleComponent(),
                HumanAdvancements.SMELT_BRASS.descriptionComponent(),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .save(consumer, HumanAdvancements.SMELT_BRASS.resourceLocation().toString());
    }

    private static AdvancementHolder addSmeltTitaniumAdvancement(AdvancementHolder parent, Consumer<AdvancementHolder> consumer) {
        return Advancement.Builder.advancement()
            .addCriterion("titanium", InventoryChangeTrigger.TriggerInstance.hasItems(HumanItems.TITANIUM_INGOT.get()))
            .parent(parent)
            .display(
                HumanItems.TITANIUM_INGOT.get(),
                HumanAdvancements.SMELT_TITANIUM.titleComponent(),
                HumanAdvancements.SMELT_TITANIUM.descriptionComponent(),
                null,
                AdvancementType.TASK,
                true,
                true,
                false
            )
            .save(consumer, HumanAdvancements.SMELT_TITANIUM.resourceLocation().toString());
    }
}
