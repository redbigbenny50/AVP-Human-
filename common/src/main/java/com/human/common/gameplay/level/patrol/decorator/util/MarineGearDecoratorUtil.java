package com.human.common.gameplay.level.patrol.decorator.util;

import com.human.common.gameplay.entity.living.human.marine.Marine;
import com.human.common.gameplay.item.DyeItemColorUtil;
import com.human.common.gameplay.item.GunItem;
import com.human.common.registry.init.HumanDataComponents;
import com.just.core.functional.tuple.Tuple2;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public class MarineGearDecoratorUtil {

    /** Full reloads a marine is issued beyond the magazine already in the gun. */
    private static final int SPARE_RELOADS = 2;

    private static final EquipmentSlot[] SLOTS = new EquipmentSlot[] {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    public static void equipArmorItem(Marine marine, ArmorItem armorItem) {
        var slot = armorItem.getEquipmentSlot();
        var itemStack = new ItemStack(armorItem, 1);

        marine.setItemSlot(slot, itemStack);
    }

    public static void applyDyeColorToArmor(Marine marine, DyeColor dyeColor) {
        for (var equipmentSlot : SLOTS) {
            var itemStack = marine.getItemBySlot(equipmentSlot);

            if (!itemStack.is(ItemTags.DYEABLE)) {
                continue;
            }

            var dyes = List.of(DyeItem.byColor(dyeColor));
            itemStack = DyeItemColorUtil.applyDyesForced(itemStack, dyes);

            marine.setItemSlot(equipmentSlot, itemStack);
        }
    }

    public static void giveDroppableItem(Marine marine, Item item) {
        giveDroppableItem(marine, item, 1);
    }

    public static void giveDroppableItem(Marine marine, Item item, int count) {
        giveDroppableItem(marine, new ItemStack(item, count));
    }

    public static void giveDroppableItem(Marine marine, ItemStack itemStack) {
        giveItem(marine, itemStack, false);
    }

    public static void giveItem(Marine marine, Item item) {
        giveItem(marine, item, 1);
    }

    public static void giveItem(Marine marine, Item item, int count) {
        giveItem(marine, new ItemStack(item, count), true);
    }

    public static void giveItem(Marine marine, ItemStack itemStack) {
        giveItem(marine, itemStack, true);
    }

    public static void giveItem(Marine marine, ItemStack itemStack, boolean marineOwned) {
        if (itemStack.getItem() instanceof GunItem gunItem) {
            itemStack.set(HumanDataComponents.AMMUNITION.get(), gunItem.getGunConfig().maximumAmmunition());

            giveSpareAmmunition(marine, gunItem);
        }

        if (marineOwned) {
            itemStack.set(HumanDataComponents.MARINE_OWNED.get(), true);
        }

        marine.getInventory().addItemStack(itemStack);
    }

    /**
     * Issues spare magazines alongside a freshly handed-out gun.
     * <p>
     * A marine used to be given a full magazine and nothing else, which was invisible while they never reloaded. Now
     * that they do, a gunner with no spares simply falls silent partway through a fight. Machine gunners included —
     * their drums hold more, but they also empty them faster.
     */
    private static void giveSpareAmmunition(Marine marine, GunItem gunItem) {
        var gunConfig = gunItem.getGunConfig();
        var ammunitionItemSupplier = gunConfig.ammunitionItemSupplier();

        if (ammunitionItemSupplier == null) {
            return;
        }

        var reloadAmount = Math.max(1, gunConfig.reloadAmount());
        // Items per reload, exactly as GunReloading works it out: a drum is one item, loose rounds are many.
        var itemsPerReload = (int) Math.ceil(gunConfig.maximumAmmunition() / (float) reloadAmount);
        var spareItems = itemsPerReload * SPARE_RELOADS;

        if (spareItems <= 0) {
            return;
        }

        var spareStack = new ItemStack(ammunitionItemSupplier.get(), spareItems);

        // Marine-owned, so a player cannot farm drums off a corpse — issued kit stays issued.
        spareStack.set(HumanDataComponents.MARINE_OWNED.get(), true);

        marine.getInventory().addItemStack(spareStack);
    }

    public static void giveWeightedItemFromPool(Marine marine, List<Tuple2<Integer, Supplier<Item>>> pool) {
        var randomWeaponSupplier = selectFromWeightedList(pool, marine.getRandom());
        giveItem(marine, randomWeaponSupplier.get());
    }

    private static <T> T selectFromWeightedList(List<Tuple2<Integer, T>> weightedList, RandomSource randomSource) {
        if (weightedList.isEmpty()) {
            throw new IllegalArgumentException("Weighted list cannot be empty.");
        }

        var totalWeight = 0;

        for (var entry : weightedList) {
            totalWeight += entry.v1();
        }

        var randomValue = randomSource.nextInt(totalWeight);
        var cumulativeWeight = 0;

        for (var entry : weightedList) {
            cumulativeWeight += entry.v1();

            if (randomValue < cumulativeWeight) {
                return entry.v2();
            }
        }

        // Fallback to last entry (should not happen with valid weights).
        return weightedList.getLast().v2();
    }
}
