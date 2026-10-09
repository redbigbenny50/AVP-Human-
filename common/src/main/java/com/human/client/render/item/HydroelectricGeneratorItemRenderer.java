package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;

/** The wheel in hand, in the GUI and on the ground: both side plates on, stationary. */
public class HydroelectricGeneratorItemRenderer extends AzItemRenderer {

    public static final String NAME = "hydroelectric_generator";

    public HydroelectricGeneratorItemRenderer() {
        super(
            AzItemRendererConfig.builder(HumanResources.blockGeoModelLocation(NAME), HumanResources.blockTextureLocation(NAME))
                .useNewOffset(true)
                .build()
        );
    }
}
