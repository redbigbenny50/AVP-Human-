package com.human.mixin.compat;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lost Cities compat. [stated] Oct 28 - "is there a way to force our structure to place despite lost cities".
 * <p>
 * Why our structures went missing: Lost Cities builds its cities as a feature in the FIRST generation step
 * (raw_generation), reshaping the ground to city level. Our outposts, camps and the mobile lab are placed later and had
 * already chosen their height from the natural terrain, so they ended up buried under the streets or inside buildings -
 * while /locate still pointed at them.
 * </p>
 * <p>
 * The fix uses Lost Cities' own rule: structures on its "avoidStructures" list (mansions, temples, igloos, pillager
 * outposts by default) make the CITY give way - no buildings and no flattening in the chunks the structure covers. This
 * adds every avp_human structure to that list, without touching anyone's config file. A server that adds them by hand
 * gets the same result; one that doesn't still gets them.
 * </p>
 * <p>
 * ⚠ {@code @Pseudo} with the class named as a string, so avp_human never needs Lost Cities to build or run: without
 * Lost Cities the class never loads and this does nothing. {@code require = 0} so an older or newer Lost Cities that
 * renamed these methods is simply left alone instead of crashing the game.
 * </p>
 */
@Pseudo
@Mixin(targets = "mcjty.lostcities.setup.Config", remap = false)
public abstract class MixinLostCitiesConfig_KeepOurStructures {

    private static final String OUR_NAMESPACE = "avp_human";

    @Inject(method = "isAvoidedStructure", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void avp_human$avoidOurStructures(ResourceLocation id, CallbackInfoReturnable<Boolean> cir) {
        if (id != null && OUR_NAMESPACE.equals(id.getNamespace())) {
            cir.setReturnValue(true);
        }
    }

    /** Lost Cities skips the whole check when its list is empty; ours are always on it. */
    @Inject(method = "hasAvoidedStructures", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void avp_human$alwaysCheck(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
