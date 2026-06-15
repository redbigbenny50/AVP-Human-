package com.human.common.gameplay.entity.living.human.ai;

import com.blib.api.common.inventory.v1.BLibInventoryHolder;
import com.human.common.gameplay.entity.living.human.ai.model.ArmorSet;
import com.human.common.gameplay.entity.living.human.ai.model.ArmorSetTarget;
import com.human.common.gameplay.entity.living.human.ai.model.ItemTarget;
import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.just.ai.goap.state.ReadableWorldState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public class ItemSenseUtil {

    public static @NotNull ArmorSetTarget findFullArmorSetInWorldState(
        Marine marine,
        ReadableWorldState worldState,
        ArmorSet armorSet
    ) {
        var helmetTarget = findItemInWorldState(marine, worldState, armorSet.helmet().get());

        if (helmetTarget.location() == ItemTarget.Location.NONE) {
            return ArmorSetTarget.EMPTY;
        }

        var chestplateTarget = findItemInWorldState(marine, worldState, armorSet.chestplate().get());

        if (chestplateTarget.location() == ItemTarget.Location.NONE) {
            return ArmorSetTarget.EMPTY;
        }

        var chitinLeggingsTarget = findItemInWorldState(marine, worldState, armorSet.leggings().get());

        if (chitinLeggingsTarget.location() == ItemTarget.Location.NONE) {
            return ArmorSetTarget.EMPTY;
        }

        var chitinBootsTarget = findItemInWorldState(marine, worldState, armorSet.boots().get());

        if (chitinBootsTarget.location() == ItemTarget.Location.NONE) {
            return ArmorSetTarget.EMPTY;
        }

        return new ArmorSetTarget(
            helmetTarget,
            chestplateTarget,
            chitinLeggingsTarget,
            chitinBootsTarget
        );
    }

    public static ItemTarget findItemInWorldState(Marine marine, ReadableWorldState worldState, Item targetItem) {
        // If an armor item, check to see if it's equipped in an equipment slot.
        if (targetItem instanceof ArmorItem armorItem) {
            var equipmentSlot = armorItem.getEquipmentSlot();

            var itemStackInEquipmentSlot = marine.getItemBySlot(equipmentSlot);

            if (itemStackInEquipmentSlot.is(targetItem)) {
                return new ItemTarget.Equipped(equipmentSlot);
            }
        }

        // Check to see if the item is equipped in the entity's hands.
        // TODO: If the item is an armor item, we should check armor cases in our hands for it.
        // TODO: If the item is generic, we should also check shulker boxes in our hands.
        // TODO: If the item is ammo, we should check ammo chests in our hands.
        if (marine.getMainHandItem().is(targetItem)) {
            return new ItemTarget.Equipped(EquipmentSlot.MAINHAND);
        } else if (marine.getOffhandItem().is(targetItem)) {
            return new ItemTarget.Equipped(EquipmentSlot.OFFHAND);
        }

        // At this point we may be able to reasonably assume the entity doesn't have the item on them. So we move
        // on to checking the entity's inventory.
        if (marine instanceof BLibInventoryHolder inventoryHolder) {
            var inventory = inventoryHolder.getInventory();

            var entriesWithItem = inventory.selectEntries(targetItem);

            if (!entriesWithItem.isEmpty()) {
                return new ItemTarget.Inventory(entriesWithItem.getFirst());
            }

            // TODO: If the item is an armor item, we should check armor cases in our inventory for it.
            // TODO: If the item is generic, we should also check shulker boxes in our inventory.
            // TODO: If the item is ammo, we should check ammo chests.
        }

        // At this point we can assume the item is not in the entity's inventory. So, time to check the environment.
        var nearbyItemEntities = marine.getEntitySenseCache()
            .getByItem(targetItem);

        if (!nearbyItemEntities.isEmpty()) {
            // TODO: What if we want the closest based on distance?
            return new ItemTarget.World(nearbyItemEntities.getFirst());
        }

        // The item did not exist in the world state, so, return NONE - we couldn't find it!
        return ItemTarget.None.INSTANCE;
    }
}
