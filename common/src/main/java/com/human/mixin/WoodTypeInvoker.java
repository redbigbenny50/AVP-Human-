package com.human.mixin;

import net.minecraft.world.level.block.state.properties.WoodType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Opens {@code WoodType.register}, which is private in 1.21.1.
 * <p>
 * [stated] Oct 23 - metal signs. A sign's look (its texture, its model layer, its hanging-sign edit screen) is keyed by
 * its WoodType, and the sign renderers build their models from {@code WoodType.values()} - so a WoodType that is not
 * registered there has no model at all and the renderer fails on it.
 * </p>
 */
@Mixin(WoodType.class)
public interface WoodTypeInvoker {

    @Invoker("register")
    static WoodType avp_human$register(WoodType woodType) {
        throw new AssertionError();
    }
}
