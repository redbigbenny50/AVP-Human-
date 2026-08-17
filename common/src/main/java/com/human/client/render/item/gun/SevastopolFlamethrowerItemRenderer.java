package com.human.client.render.item.gun;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;
import com.human.client.animation.item.SevastopolFlamethrowerAnimator;
import net.minecraft.world.item.ItemDisplayContext;

public class SevastopolFlamethrowerItemRenderer extends AzItemRenderer {

    public SevastopolFlamethrowerItemRenderer(String name) {
        super(
            AzItemRendererConfig.builder(
                HumanResources.itemGeoModelLocation(name),
                HumanResources.itemTextureLocation(name)
            )
                .setAnimatorProvider(SevastopolFlamethrowerAnimator::new)
                // ⚠⚠ THE FLAMETHROWER DOES NOT EXTEND MuzzledGunItemRenderer — it builds its config directly on
                // AzItemRenderer, so it does NOT inherit the base's static-outside-hands default and needs its own
                // copy. ⭐ Any future gun that also bypasses MuzzledGunItemRenderer needs this line too.
                .setShouldAnimateInContext(SevastopolFlamethrowerItemRenderer::animatesInContext)
                .useNewOffset(true)
                .build()
        );
    }

    /** In-hand transforms only — see MuzzledGunItemRenderer.animatesInContext for the reasoning. */
    private static boolean animatesInContext(ItemDisplayContext transformType) {
        return switch (transformType) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND ->
                true;
            default -> false;
        };
    }
}
