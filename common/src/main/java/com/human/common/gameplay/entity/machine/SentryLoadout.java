package com.human.common.gameplay.entity.machine;

import com.human.common.gameplay.power.upgrade.UpgradeCard;
import com.human.common.gameplay.power.upgrade.UpgradeCardItem;
import com.human.common.registry.init.item.HumanBlockItems;
import com.human.common.registry.init.item.HumanItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The sentry's loadout - every slot but the battery. [stated] Oct 2: nine ammo slots (loose rounds, rockets, or filled
 * ammo chests), four upgrade-card slots (one card each), and a skin, network-card and sensor-lens slot.
 * <p>
 * The rules live here so the turret's container and its menu agree. Ammo is drawn from the loadout ONLY - the old
 * nearby-chest search is gone.
 * </p>
 */
public final class SentryLoadout {

    public static final int AMMO_FIRST = 0;

    public static final int AMMO_SLOTS = 9;

    public static final int CARD_FIRST = AMMO_FIRST + AMMO_SLOTS;

    public static final int CARD_SLOTS = UpgradeCard.SLOTS;

    public static final int SKIN = CARD_FIRST + CARD_SLOTS;

    public static final int NETWORK = SKIN + 1;

    public static final int SENSOR = NETWORK + 1;

    public static final int SIZE = SENSOR + 1;

    /** The ammo chest holds 27 stacks. */
    private static final int CHEST_SLOTS = 27;

    private SentryLoadout() {}

    public static boolean isAmmoSlot(int slot) {
        return slot >= AMMO_FIRST && slot < AMMO_FIRST + AMMO_SLOTS;
    }

    public static boolean isCardSlot(int slot) {
        return slot >= CARD_FIRST && slot < CARD_FIRST + CARD_SLOTS;
    }

    /** Everything but ammo holds one item. */
    public static int maxStack(int slot) {
        return isAmmoSlot(slot) ? 64 : 1;
    }

    public static boolean isRound(ItemStack stack) {
        return stack.is(HumanItems.MEDIUM_BULLET.get()) || stack.is(HumanItems.CASELESS_BULLET.get()) || stack.is(HumanItems.ROCKET.get());
    }

    public static boolean isAmmoChest(ItemStack stack) {
        return stack.is(HumanBlockItems.AMMO_CHEST.get());
    }

    public static boolean isLens(ItemStack stack) {
        return stack.is(HumanItems.MOTION_SENSOR.get()) || stack.is(HumanItems.EM_SENSOR.get()) || stack.is(HumanItems.CAMERA_SENSOR.get());
    }

    /** Whether {@code stack} may go into {@code slot} of {@code loadout}. */
    public static boolean mayPlace(Container loadout, int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (isAmmoSlot(slot)) {
            return isRound(stack) || isAmmoChest(stack);
        }

        if (isCardSlot(slot)) {
            if (!(stack.getItem() instanceof UpgradeCardItem item) || !item.getCard().fits(UpgradeCard.Target.SENTRY)) {
                return false;
            }

            // [stated] Oct 2: one of each card; caseless and rocket ammo exclude each other.
            for (int other = CARD_FIRST; other < CARD_FIRST + CARD_SLOTS; other++) {
                if (other != slot && loadout.getItem(other).getItem() instanceof UpgradeCardItem fitted) {
                    var card = item.getCard();
                    var existing = fitted.getCard();

                    if (existing == card || card.group() != null && card.group().equals(existing.group())) {
                        return false;
                    }
                }
            }

            return true;
        }

        return switch (slot) {
            case SKIN -> stack.is(HumanItems.SKIN_KIT.get()) && com.human.common.gameplay.item.SkinKitItem.skinOf(stack) != null;
            case NETWORK -> stack.is(HumanItems.NETWORK_CARD.get());
            case SENSOR -> isLens(stack);
            default -> false;
        };
    }

    public static boolean hasCard(Container loadout, UpgradeCard card) {
        for (int slot = CARD_FIRST; slot < CARD_FIRST + CARD_SLOTS; slot++) {
            if (loadout.getItem(slot).getItem() instanceof UpgradeCardItem item && item.getCard() == card) {
                return true;
            }
        }

        return false;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ammo
    // ------------------------------------------------------------------------------------------------------------

    /** Whether any loose round of {@code type}, or any carried ammo chest holding one, is in the ammo slots. */
    public static boolean hasAmmo(Container loadout, Item type) {
        for (int slot = AMMO_FIRST; slot < AMMO_FIRST + AMMO_SLOTS; slot++) {
            var stack = loadout.getItem(slot);

            if (stack.is(type)) {
                return true;
            }

            if (isAmmoChest(stack)) {
                var contents = stack.get(DataComponents.CONTAINER);

                if (contents != null) {
                    for (var held : contents.nonEmptyItems()) {
                        if (held.is(type)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Takes ONE round of {@code type}: loose rounds first, then from inside a carried ammo chest. A chest is drawn down
     * in place - its stored contents are rewritten one round lighter - and an emptied chest stays in the slot for the
     * player to take back. Nothing is created or destroyed except the one round fired.
     *
     * @return whether a round was taken
     */
    public static boolean consumeAmmo(Container loadout, Item type) {
        for (int slot = AMMO_FIRST; slot < AMMO_FIRST + AMMO_SLOTS; slot++) {
            var stack = loadout.getItem(slot);

            if (stack.is(type)) {
                stack.shrink(1);
                loadout.setChanged();
                return true;
            }
        }

        for (int slot = AMMO_FIRST; slot < AMMO_FIRST + AMMO_SLOTS; slot++) {
            var stack = loadout.getItem(slot);

            if (!isAmmoChest(stack)) {
                continue;
            }

            var contents = stack.get(DataComponents.CONTAINER);

            if (contents == null) {
                continue;
            }

            var items = NonNullList.withSize(CHEST_SLOTS, ItemStack.EMPTY);
            contents.copyInto(items);

            for (int index = 0; index < items.size(); index++) {
                if (items.get(index).is(type)) {
                    items.get(index).shrink(1);
                    stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
                    loadout.setChanged();
                    return true;
                }
            }
        }

        return false;
    }
}
