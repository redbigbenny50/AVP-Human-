package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;

/** A vaporizer in the hand, the inventory or on the ground - the machine's geo model in its edition's skin. */
public class VaporizerItemRenderer extends AzItemRenderer {

    public VaporizerItemRenderer(String texture) {
        super(
            AzItemRendererConfig.builder(HumanResources.blockGeoModelLocation("vaporizer"), HumanResources.blockTextureLocation(texture))
                .useNewOffset(true)
                .build()
        );
    }
}
