package com.human.client.render.item;

import com.blib.api.client.render.v1.item.AzItemRenderer;
import com.blib.api.client.render.v1.item.AzItemRendererConfig;
import com.human.HumanResources;

/**
 * A fabricator in the hand, the inventory or on the ground. Shrunk and centred by the item model's display transforms
 * (models/item/*fabricator.json), as the crates are - the geo is centred on its two-block width, not on one block.
 */
public class FabricatorItemRenderer extends AzItemRenderer {

    public FabricatorItemRenderer(String texture) {
        super(
            AzItemRendererConfig.builder(HumanResources.blockGeoModelLocation("fabricator"), HumanResources.blockTextureLocation(texture))
                .useNewOffset(true)
                .build()
        );
    }
}
