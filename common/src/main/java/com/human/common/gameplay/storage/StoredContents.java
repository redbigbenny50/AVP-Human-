package com.human.common.gameplay.storage;

import com.human.common.gameplay.fabrication.DriveBayItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * One rule for every item that carries a block's contents with it: a full storage crate, a fabricator, and a drive bay
 * holding SSDs or blueprints. Such an item is never destroyed as a dropped item (fire, lava, explosions, cactus,
 * despawning) and no machine may use it up, because the contents would go with it.
 * <p>
 * Ask here rather than checking each item type, so a new storage block only has to be added once.
 */
public final class StoredContents {

    private StoredContents() {}

    public static boolean isProtected(ItemStack stack) {
        if (CrateItem.hasContents(stack)) {
            return true;
        }

        if (stack.getItem() instanceof DriveBayItem || stack.getItem() instanceof com.human.common.gameplay.fabrication.FabricatorItem) {
            var container = stack.get(DataComponents.CONTAINER);

            return container != null && container.nonEmptyStream().findAny().isPresent();
        }

        return false;
    }
}
