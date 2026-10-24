package com.human.common.gameplay.storage;

import com.human.common.registry.init.HumanDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Oct 13 - whether an item is holding something: a full crate, drive bay or fabricator, a filled shulker box or bundle,
 * a tank or controller carrying fluid or batteries. Anything that USES UP items (the fabricator's planner, the grinder)
 * must never take one of these unless it carries the contents on - the contents would vanish with it.
 * <p>
 * Wider than {@link StoredContents#isProtected}, which only decides which DROPPED items are indestructible and is kept
 * to our own blocks so vanilla shulker boxes behave as vanilla on the ground.
 */
public final class ContentsRules {

    private ContentsRules() {}

    public static boolean holdsContents(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (StoredContents.isProtected(stack)) {
            return true;
        }

        var container = stack.get(DataComponents.CONTAINER);

        if (container != null && container.nonEmptyStream().findAny().isPresent()) {
            return true;
        }

        var bundle = stack.get(DataComponents.BUNDLE_CONTENTS);

        if (bundle != null && !bundle.isEmpty()) {
            return true;
        }

        var fluid = stack.get(HumanDataComponents.FLUID_CONTENT.get());

        return fluid != null && !fluid.isEmpty();
    }
}
