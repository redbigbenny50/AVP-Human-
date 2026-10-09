package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;

/**
 * The solar panel in hand, in the GUI and on the ground - the panel sits at its authored tilt; only placed blocks track
 * the sun.
 */
public class SolarPanelItemRenderer extends AzItemRenderer {

    public static final String NAME = "solar_panel";

    public SolarPanelItemRenderer() {
        super(
            AzItemRendererConfig.builder(HumanResources.blockGeoModelLocation(NAME), HumanResources.blockTextureLocation(NAME))
                .useNewOffset(true)
                // ⚠ AFTER removing the authored gui translation of -9.25 (which was what threw it off the bottom of
                // the frame), the panel sits correctly but its TOP is still proud: the geo spans y 0..18px against a
                // 16px block. 16/18 trims exactly that overhang. ITEM-ONLY - placed blocks render full size.
                .setScale(16.0F / 18.0F)
                .build()
        );
    }
}
