package com.human.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Makes a slot's screen position writable.
 * <p>
 * {@code Slot.x} and {@code Slot.y} are declared final, which is the one thing standing between us and a scrolling
 * container that is safe on a server. The alternative - keeping a fixed grid of slots and remapping which container
 * index each one reads - changes what a menu slot MEANS as the player scrolls, and a click packet carries only the menu
 * slot index. Any lag between scrolling and clicking then lands the item somewhere else entirely.
 * <p>
 * Moving the slots instead keeps every menu index bound to the same inventory index for the whole life of the screen,
 * so the server can never misread a click, and scrolling stays a purely client-side concern. This widens two fields and
 * changes no behaviour.
 */
@Mixin(Slot.class)
public interface MixinSlot_MutablePosition {

    @Mutable
    @Accessor("x")
    void setX(int x);

    @Mutable
    @Accessor("y")
    void setY(int y);
}
