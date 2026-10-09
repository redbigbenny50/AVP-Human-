package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;

/** The thermoelectric generator in hand, in the GUI and on the ground - fan still. */
public class ThermoelectricGeneratorItemRenderer extends AzItemRenderer {

    public static final String NAME = "thermoelectric_generator";

    public ThermoelectricGeneratorItemRenderer() {
        super(
            AzItemRendererConfig.builder(HumanResources.blockGeoModelLocation(NAME), HumanResources.blockTextureLocation(NAME))
                .useNewOffset(true)
                .build()
        );
    }
}
