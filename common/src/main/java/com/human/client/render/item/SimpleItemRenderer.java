package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;

/**
 * A geo item with no animator - the armor case.
 * <p>
 * ⚠⚠ {@code useNewOffset(true)} IS NOT OPTIONAL HERE. {@code AzItemRendererPipeline} lifts a geo model by 0.51 blocks
 * unless the config sets the new offset (read from BLib 0.3.11: {@code translate(0.5, useNewOffset ? 0 : 0.51, 0.5)}).
 * Every other avp_human item renderer - all the guns, the desk terminal, the resonator, the sentry turret - sets it,
 * and their Blockbench {@code display} values in {@code models/item} were authored against a model sitting at 0. This
 * class was the one renderer that did not, so the armor case alone drew half a block out of place in the hand and on
 * the ground while its GUI icon (which rotates the lift out of view) looked fine. Sep 22 field report: "model has come
 * back but its just in the wrong place."
 */
public class SimpleItemRenderer extends AzItemRenderer {

    public SimpleItemRenderer(String path) {
        super(
            AzItemRendererConfig.builder(HumanResources.itemGeoModelLocation(path), HumanResources.itemTextureLocation(path))
                .useNewOffset(true)
                .build()
        );
    }
}
