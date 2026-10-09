package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;

/** The turbine in hand, in the GUI and on the ground - stationary; only placed ones turn. */
public class AerokineticGeneratorItemRenderer extends AzItemRenderer {

    public static final String NAME = "aerokinetic_generator";

    public AerokineticGeneratorItemRenderer() {
        super(
            AzItemRendererConfig.builder(HumanResources.blockGeoModelLocation(NAME), HumanResources.blockTextureLocation(NAME))
                .useNewOffset(true)
                .build()
        );
    }
}
