package com.human.common.gameplay.menu.marine;

import com.human.common.registry.init.HumanDataComponents;
import com.human.mixin.MixinSlot_MutablePosition;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A slot in a marine's inventory that refuses to give up issued gear.
 * <p>
 * Everything the marine spawned with is flagged {@code MARINE_OWNED}, which is already what keeps it out of the drops
 * on death. The same flag locks the slot here: the player can see the rifle and the armour the marine was issued, and
 * cannot take them or write over them. Anything the player handed over carries no flag and behaves like any chest slot,
 * including dropping when the marine dies.
 * <p>
 * The flag is a data component, so it travels with the stack through the ordinary container sync and the client can
 * make the same judgement the server does. That is what lets the lock be predicted client-side instead of the click
 * being accepted and then snapped back.
 */
public class MarineInventorySlot extends Slot {

    /**
     * Which row of the pack this slot belongs to. Fixed for the life of the menu - scrolling moves the slot, it never
     * reassigns it.
     */
    private final int packRow;

    private boolean visible = true;

    public MarineInventorySlot(Container container, int slotIndex, int packRow, int x, int y) {
        super(container, slotIndex, x, y);

        this.packRow = packRow;
    }

    public int getPackRow() {
        return packRow;
    }

    /**
     * Hides a slot that has scrolled out of the window.
     * <p>
     * An inactive slot is neither drawn nor hit-tested, but it is still visited by {@code moveItemStackTo} - so
     * shift-clicking still fills rows the player cannot currently see, which is what anyone would expect of a pack.
     */
    @Override
    public boolean isActive() {
        return visible;
    }

    public void placeAt(int x, int y, boolean visible) {
        this.visible = visible;

        ((MixinSlot_MutablePosition) (Object) this).setX(x);
        ((MixinSlot_MutablePosition) (Object) this).setY(y);
    }

    public static boolean isIssuedGear(ItemStack itemStack) {
        return !itemStack.isEmpty() && itemStack.getOrDefault(HumanDataComponents.MARINE_OWNED.get(), false);
    }

    public boolean isLocked() {
        return isIssuedGear(getItem());
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return !isLocked();
    }

    /**
     * Judged on what is ALREADY in the slot rather than on the incoming stack, because the harm to prevent is a player
     * swapping something over the top of issued gear - which would destroy it just as effectively as taking it.
     */
    @Override
    public boolean mayPlace(@NotNull ItemStack itemStack) {
        return !isLocked();
    }
}
